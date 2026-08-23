package com.proyecto_final.triage.platform

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
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
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import java.util.Locale

private val ACCENT = Color(0xFF5BB8D4)

@SuppressLint("MissingPermission")
@Composable
actual fun PlatformMap(
    hospital: HospitalSeleccionadoResponse?
) {
    val context = LocalContext.current

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    var userLocation by remember {
        mutableStateOf<LatLng?>(null)
    }

    var hospitalLocation by remember(hospital?.direccion) {
        mutableStateOf<LatLng?>(null)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    userLocation = LatLng(
                        it.latitude,
                        it.longitude
                    )
                }
            }
        }
    }

    // Obtener ubicación del usuario
    LaunchedEffect(Unit) {
        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
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

    // Geocodificar hospital
    LaunchedEffect(hospital?.direccion) {
        val direccion = hospital?.direccion

        if (direccion.isNullOrBlank()) {
            hospitalLocation = null
            return@LaunchedEffect
        }

        hospitalLocation = withContext(Dispatchers.IO) {
            if (!Geocoder.isPresent()) {
                return@withContext null
            }

            try {
                @Suppress("DEPRECATION")
                Geocoder(context, Locale.getDefault())
                    .getFromLocationName(direccion, 1)
                    ?.firstOrNull()
                    ?.let {
                        LatLng(
                            it.latitude,
                            it.longitude
                        )
                    }
            } catch (e: Exception) {
                null
            }
        }
    }

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

    // Dibujar marcadores
    LaunchedEffect(
        userLocation,
        hospitalLocation,
        mapLibreMap
    ) {
        val map = mapLibreMap ?: return@LaunchedEffect

        map.getStyle { style ->

            userLocation?.let { location ->
                dibujarMarcadorEnMapa(
                    style = style,
                    sourceId = "consulta-user-location-source",
                    dotLayerId = "consulta-user-location-dot",
                    location = location,
                    color = ACCENT
                )
            }

            hospitalLocation?.let { location ->
                dibujarMarcadorEnMapa(
                    style = style,
                    sourceId = "consulta-hospital-location-source",
                    dotLayerId = "consulta-hospital-location-dot",
                    location = location,
                    color = Color(0xFFE57373)
                )
            }
        }

        when {
            userLocation != null && hospitalLocation != null -> {

                val bounds = LatLngBounds.Builder()
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

    AndroidView(
        factory = {
            mapView
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(12.dp))
    )
}

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

    val existingSource = style.getSourceAs<GeoJsonSource>(sourceId)

    if (existingSource != null) {
        existingSource.setGeoJson(geoJson)
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
            PropertyFactory.circleRadius(7f),
            PropertyFactory.circleColor(color.toArgb()),
            PropertyFactory.circleOpacity(1f),
            PropertyFactory.circleStrokeColor(
                Color.White.toArgb()
            ),
            PropertyFactory.circleStrokeWidth(2f)
        )
    )
}