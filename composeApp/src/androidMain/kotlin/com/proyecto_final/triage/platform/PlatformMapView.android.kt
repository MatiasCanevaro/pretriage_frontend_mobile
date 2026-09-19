package com.proyecto_final.triage.platform

import android.annotation.SuppressLint
import androidx.compose.foundation.background
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
import com.proyecto_final.triage.location.CurrentLocation
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource

private val ACCENT = Color(0xFF5BB8D4)

@SuppressLint("MissingPermission")
@Composable
actual fun PlatformMapView(
    location: CurrentLocation?,
    modifier: Modifier
) {
    val context = LocalContext.current

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

    LaunchedEffect(location, mapLibreMap) {
        val loc = location ?: return@LaunchedEffect
        val map = mapLibreMap ?: return@LaunchedEffect

        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 15.0)
        )

        map.getStyle { style ->
            val sourceId = "location-source"
            val layerId = "location-dot"

            val geoJson = """
                {
                    "type": "FeatureCollection",
                    "features": [{
                        "type": "Feature",
                        "geometry": {
                            "type": "Point",
                            "coordinates": [${loc.longitude}, ${loc.latitude}]
                        }
                    }]
                }
            """.trimIndent()

            val existingSource = style.getSourceAs<GeoJsonSource>(sourceId)
            if (existingSource != null) {
                existingSource.setGeoJson(geoJson)
            } else {
                style.addSource(GeoJsonSource(sourceId, geoJson))

                val dotLayer = CircleLayer(layerId, sourceId).withProperties(
                    PropertyFactory.circleRadius(8f),
                    PropertyFactory.circleColor(ACCENT.toArgb()),
                    PropertyFactory.circleOpacity(1f),
                    PropertyFactory.circleStrokeColor(Color.White.toArgb()),
                    PropertyFactory.circleStrokeWidth(2f)
                )
                style.addLayer(dotLayer)
            }
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier
            .background(Color(0xFFE0E0E0))
            .clip(RoundedCornerShape(12.dp))
    )
}