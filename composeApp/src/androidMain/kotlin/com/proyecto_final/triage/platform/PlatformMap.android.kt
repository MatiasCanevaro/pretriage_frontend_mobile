package com.proyecto_final.triage.platform

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.proyecto_final.triage.network.estadoConsulta.HospitalSeleccionadoResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import java.util.Locale

private val ACCENT = Color(0xFF5BB8D4)

@SuppressLint("MissingPermission")
@Composable
actual fun PlatformMap(
    hospital: HospitalSeleccionadoResponse?,
    ubicacion: String?,
    polylineCode: String?
) {

    val context = LocalContext.current

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    /*
     * ============================================================
     * UBICACIÓN DEL USUARIO
     * ============================================================
     */

    var userLocation by remember {
        mutableStateOf<LatLng?>(null)
    }

    /*
     * Primero intentamos utilizar la ubicación recibida.
     *
     * Formato:
     *
     * "latitud,longitud"
     */

    LaunchedEffect(ubicacion) {

        if (ubicacion.isNullOrBlank()) {
            return@LaunchedEffect
        }

        try {
            val coordenadas = ubicacion
                .split(",")
                .map { it.trim().toDouble() }

            if (coordenadas.size >= 2) {
                userLocation = LatLng(
                    coordenadas[0],
                    coordenadas[1]
                )
            }

        } catch (_: Exception) {
            // Si no hay ubicación válida,
            // se intenta obtener la ubicación del dispositivo.
        }
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {

                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->

                        location?.let {

                            userLocation = LatLng(
                                it.latitude,
                                it.longitude
                            )
                        }
                    }
            }
        }

    /*
     * Si no recibimos ubicación, intentamos obtenerla
     * directamente del dispositivo.
     */

    LaunchedEffect(Unit) {

        if (userLocation == null) {

            if (
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {

                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->

                        location?.let {

                            userLocation = LatLng(
                                it.latitude,
                                it.longitude
                            )
                        }
                    }

            } else {

                permissionLauncher.launch(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
        }
    }

    /*
     * ============================================================
     * UBICACIÓN DEL HOSPITAL
     * ============================================================
     */

    var hospitalLocation by remember(
        hospital?.direccion
    ) {
        mutableStateOf<LatLng?>(null)
    }

    LaunchedEffect(hospital?.direccion) {

        val direccion = hospital?.direccion

        if (direccion.isNullOrBlank()) {

            hospitalLocation = null

            return@LaunchedEffect
        }

        hospitalLocation = withContext(
            Dispatchers.IO
        ) {

            if (!Geocoder.isPresent()) {
                return@withContext null
            }

            try {

                @Suppress("DEPRECATION")
                Geocoder(
                    context,
                    Locale.getDefault()
                )
                    .getFromLocationName(
                        direccion,
                        1
                    )
                    ?.firstOrNull()
                    ?.let {

                        LatLng(
                            it.latitude,
                            it.longitude
                        )
                    }

            } catch (_: Exception) {

                null
            }
        }
    }

    /*
     * ============================================================
     * POLYLINE
     * ============================================================
     */

    var routePoints by remember {
        mutableStateOf<List<LatLng>>(emptyList())
    }

    LaunchedEffect(polylineCode) {

        routePoints =
            if (polylineCode.isNullOrBlank()) {
                emptyList()
            } else {
                decodePolyline(polylineCode)
            }
    }

    /*
     * ============================================================
     * MAP VIEW
     * ============================================================
     */

    val mapView = remember {

        MapView(context).apply {
            onCreate(null)
        }
    }

    var mapLibreMap by remember {
        mutableStateOf<MapLibreMap?>(null)
    }

    DisposableEffect(mapView) {

        mapView.getMapAsync { map ->

            map.setStyle(
                "https://tiles.openfreemap.org/styles/liberty"
            ) {

                mapLibreMap = map

                map.uiSettings.isLogoEnabled = false
                map.uiSettings.isAttributionEnabled = false

                map.uiSettings.isScrollGesturesEnabled = false
                map.uiSettings.isZoomGesturesEnabled = false
                map.uiSettings.isRotateGesturesEnabled = false
                map.uiSettings.isTiltGesturesEnabled = false
                map.uiSettings.isDoubleTapGesturesEnabled = false
                map.uiSettings.isQuickZoomGesturesEnabled = false
            }
        }

        onDispose {
            mapView.onDestroy()
        }
    }

    /*
     * ============================================================
     * DIBUJAR MAPA
     * ============================================================
     */

    LaunchedEffect(
        userLocation,
        hospitalLocation,
        routePoints,
        mapLibreMap
    ) {

        val map = mapLibreMap
            ?: return@LaunchedEffect

        map.getStyle { style ->

            /*
             * USUARIO
             */

            userLocation?.let { location ->

                dibujarMarcadorEnMapa(
                    style = style,
                    sourceId =
                        "consulta-user-location-source",
                    dotLayerId =
                        "consulta-user-location-dot",
                    location = location,
                    color = ACCENT
                )
            }

            /*
             * HOSPITAL
             */

            hospitalLocation?.let { location ->

                dibujarMarcadorEnMapa(
                    style = style,
                    sourceId =
                        "consulta-hospital-location-source",
                    dotLayerId =
                        "consulta-hospital-location-dot",
                    location = location,
                    color = Color(0xFFE57373)
                )
            }

            /*
             * RUTA
             */

            if (routePoints.isNotEmpty()) {

                dibujarRutaEnMapa(
                    style = style,
                    points = routePoints
                )
            }
        }

        /*
         * ========================================================
         * CÁMARA
         * ========================================================
         */

        if (routePoints.isNotEmpty()) {

            val boundsBuilder =
                LatLngBounds.Builder()

            routePoints.forEach {
                boundsBuilder.include(it)
            }

            userLocation?.let {
                boundsBuilder.include(it)
            }

            hospitalLocation?.let {
                boundsBuilder.include(it)
            }

            map.animateCamera(
                CameraUpdateFactory.newLatLngBounds(
                    boundsBuilder.build(),
                    80
                )
            )

        } else {

            when {

                userLocation != null &&
                        hospitalLocation != null -> {

                    val bounds =
                        LatLngBounds.Builder()
                            .include(userLocation!!)
                            .include(hospitalLocation!!)
                            .build()

                    map.animateCamera(
                        CameraUpdateFactory.newLatLngBounds(
                            bounds,
                            120
                        )
                    )
                }

                hospitalLocation != null -> {

                    map.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            hospitalLocation!!,
                            15.0
                        )
                    )
                }

                userLocation != null -> {

                    map.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            userLocation!!,
                            15.0
                        )
                    )
                }
            }
        }
    }

    AndroidView(
        factory = {
            mapView
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(
                RoundedCornerShape(12.dp)
            )
    )
}


/* ============================================================
   MARCADOR
   ============================================================ */

private fun dibujarMarcadorEnMapa(
    style: Style,
    sourceId: String,
    dotLayerId: String,
    location: LatLng,
    color: Color
) {

    val geoJson = """
        {
            "type": "FeatureCollection",
            "features": [{
                "type": "Feature",
                "geometry": {
                    "type": "Point",
                    "coordinates": [
                        ${location.longitude},
                        ${location.latitude}
                    ]
                }
            }]
        }
    """.trimIndent()

    val existingSource =
        style.getSourceAs<GeoJsonSource>(
            sourceId
        )

    if (existingSource != null) {

        existingSource.setGeoJson(
            geoJson
        )

        return
    }

    style.addSource(
        GeoJsonSource(
            sourceId,
            geoJson
        )
    )

    style.addLayer(
        CircleLayer(
            dotLayerId,
            sourceId
        ).withProperties(

            PropertyFactory.circleRadius(
                7f
            ),

            PropertyFactory.circleColor(
                color.toArgb()
            ),

            PropertyFactory.circleOpacity(
                1f
            ),

            PropertyFactory.circleStrokeColor(
                Color.White.toArgb()
            ),

            PropertyFactory.circleStrokeWidth(
                2f
            )
        )
    )
}


/* ============================================================
   DIBUJAR RUTA
   ============================================================ */

private fun dibujarRutaEnMapa(
    style: Style,
    points: List<LatLng>
) {

    if (points.size < 2) {
        return
    }

    val coordinates =
        points.joinToString(
            separator = ","
        ) {
            """
            [
                ${it.longitude},
                ${it.latitude}
            ]
            """.trimIndent()
        }

    val geoJson = """
        {
            "type": "Feature",
            "geometry": {
                "type": "LineString",
                "coordinates": [
                    $coordinates
                ]
            }
        }
    """.trimIndent()

    val sourceId =
        "consulta-route-source"

    val layerId =
        "consulta-route-line"

    val existingSource =
        style.getSourceAs<GeoJsonSource>(
            sourceId
        )

    if (existingSource != null) {

        existingSource.setGeoJson(
            geoJson
        )

        return
    }

    style.addSource(
        GeoJsonSource(
            sourceId,
            geoJson
        )
    )

    style.addLayer(
        LineLayer(
            layerId,
            sourceId
        ).withProperties(

            PropertyFactory.lineColor(
                ACCENT.toArgb()
            ),

            PropertyFactory.lineWidth(
                5f
            ),

            PropertyFactory.lineOpacity(
                0.9f
            ),

            PropertyFactory.lineCap(
                "round"
            ),

            PropertyFactory.lineJoin(
                "round"
            )
        )
    )
}


/* ============================================================
   DECODIFICAR ENCODED POLYLINE
   ============================================================ */

private fun decodePolyline(
    encoded: String
): List<LatLng> {

    val polyline = mutableListOf<LatLng>()

    var index = 0

    var latitude = 0
    var longitude = 0

    while (index < encoded.length) {

        var result = 0
        var shift = 0

        while (true) {

            val byte =
                encoded[index++].code - 63

            result =
                result or (
                        (byte and 0x1F)
                                shl shift
                        )

            shift += 5

            if (byte < 0x20) {
                break
            }
        }

        val deltaLatitude =
            if ((result and 1) != 0) {
                -(result shr 1) - 1
            } else {
                result shr 1
            }

        latitude += deltaLatitude

        result = 0
        shift = 0

        while (true) {

            val byte =
                encoded[index++].code - 63

            result =
                result or (
                        (byte and 0x1F)
                                shl shift
                        )

            shift += 5

            if (byte < 0x20) {
                break
            }
        }

        val deltaLongitude =
            if ((result and 1) != 0) {
                -(result shr 1) - 1
            } else {
                result shr 1
            }

        longitude += deltaLongitude

        polyline.add(
            LatLng(
                latitude / 1E5,
                longitude / 1E5
            )
        )
    }

    return polyline
}