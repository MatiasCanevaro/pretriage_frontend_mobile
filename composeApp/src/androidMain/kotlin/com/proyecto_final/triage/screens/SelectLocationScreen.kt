package com.proyecto_final.triage.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import com.google.android.gms.location.LocationServices
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource

actual fun getSelectLocationScreen(): Screen = SelectLocationScreen()

class SelectLocationScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current

        SelectLocationContent(
            onBack = { navigator?.pop() },
            onContinue = { ubicacion, transporte ->
                navigator?.push(HospitalesScreen(ubicacion, transporte))
            }
        )
    }
}

@SuppressLint("MissingPermission")
@Composable
fun SelectLocationContent(
    onBack: () -> Unit,
    onContinue: (String, String) -> Unit
) {

    var usarUbicacionActual by remember { mutableStateOf(true) }
    var transporteSeleccionado by remember { mutableStateOf("Transporte público") }

    val context = LocalContext.current
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    userLocation = LatLng(it.latitude, it.longitude)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    userLocation = LatLng(it.latitude, it.longitude)
                }
            }
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    val mapView = remember {
        MapView(context).apply {
            onCreate(null)
        }
    }

    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }

    DisposableEffect(mapView) {
        mapView.getMapAsync { map ->
            map.setStyle("https://tiles.openfreemap.org/styles/liberty") {
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

    LaunchedEffect(userLocation, mapLibreMap) {
        val location = userLocation ?: return@LaunchedEffect
        val map = mapLibreMap ?: return@LaunchedEffect

        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(location, 15.0)
        )

        map.getStyle { style ->
            val sourceId = "user-location-source"
            val radiusLayerId = "user-location-circle"
            val dotLayerId = "user-location-dot"

            val geoJson = """
                {
                    "type": "FeatureCollection",
                    "features": [{
                        "type": "Feature",
                        "geometry": {
                            "type": "Point",
                            "coordinates": [${location.longitude}, ${location.latitude}]
                        }
                    }]
                }
            """.trimIndent()

            val existingSource = style.getSourceAs<GeoJsonSource>(sourceId)
            if (existingSource != null) {
                existingSource.setGeoJson(geoJson)
            } else {
                style.addSource(GeoJsonSource(sourceId, geoJson))

                val radiusLayer = CircleLayer(radiusLayerId, sourceId).withProperties(
                    PropertyFactory.circleRadius(50f),
                    PropertyFactory.circleColor(Color(0xFF5BB8D4).toArgb()),
                    PropertyFactory.circleOpacity(0.3f),
                    PropertyFactory.circleStrokeColor(Color(0xFF5BB8D4).toArgb()),
                    PropertyFactory.circleStrokeWidth(2f)
                )
                style.addLayer(radiusLayer)

                val dotLayer = CircleLayer(dotLayerId, sourceId).withProperties(
                    PropertyFactory.circleRadius(8f),
                    PropertyFactory.circleColor(Color(0xFF5BB8D4).toArgb()),
                    PropertyFactory.circleOpacity(1f),
                    PropertyFactory.circleStrokeColor(Color.White.toArgb()),
                    PropertyFactory.circleStrokeWidth(2f)
                )
                style.addLayer(dotLayer)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {

        item {
            Spacer(modifier = Modifier.height(24.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                modifier = Modifier.clickable { onBack() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Encontrá el hospital más cercano",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Usamos tu ubicación y medio de transporte para mostrarte las mejores opciones",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "1. Ingresá tu ubicación",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            // Card que contiene: opción ubicación actual + mapa + opción otra dirección
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // Opción 1: ubicación actual
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { usarUbicacionActual = true }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF5BB8D4)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Usar mi ubicación actual",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Av. Santa Fe 1234, Caba",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        RadioButton(
                            selected = usarUbicacionActual,
                            onClick = { usarUbicacionActual = true },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFF5BB8D4)
                            )
                        )
                    }

                    // El mapa, dentro de la misma Card
                    AndroidView(
                        factory = { mapView },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )

                    // Opción 2: otra dirección
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { usarUbicacionActual = false }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Ingresar otra dirección",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        RadioButton(
                            selected = !usarUbicacionActual,
                            onClick = { usarUbicacionActual = false },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFF5BB8D4)
                            )
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "2. Elegí tu medio de transporte",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(12.dp))

            val transportes = listOf(
                "Auto" to Icons.Default.DirectionsCar,
                "Transporte público" to Icons.Default.DirectionsBus,
                "A pie" to Icons.AutoMirrored.Filled.DirectionsWalk,
                "Bicicleta" to Icons.Default.PedalBike
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                transportes.chunked(2).forEach { fila ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        fila.forEach { (nombre, icono) ->
                            val seleccionado = transporteSeleccionado == nombre

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor =
                                        if (seleccionado) Color(0xFFE3F2FD)
                                        else MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { transporteSeleccionado = nombre }
                                    .then(
                                        if (seleccionado)
                                            Modifier.border(1.dp, Color(0xFF5BB8D4), RoundedCornerShape(12.dp))
                                        else Modifier
                                    )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = icono,
                                        contentDescription = null,
                                        tint = if (seleccionado) Color(0xFF5BB8D4)
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        nombre,
                                        color = if (seleccionado) Color(0xFF5BB8D4)
                                        else MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }

                        if (fila.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onContinue(
                        if (usarUbicacionActual)
                            "${userLocation?.latitude}, ${userLocation?.longitude}"
                        else
                            "otra",
                        transporteSeleccionado
                    )
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5BB8D4)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Continuar", color = Color.White)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}