package com.proyecto_final.triage.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeUIViewController
import androidx.compose.ui.unit.dp
import com.proyecto_final.triage.location.CurrentLocation
import platform.MapKit.MKCoordinateRegion
import platform.MapKit.MKMapView
import platform.MapKit.MKPointAnnotation
import platform.UIKit.UIColor
import platform.UIKit.UIView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
actual fun PlatformMapView(
    location: CurrentLocation?,
    modifier: Modifier = Modifier
) {
    val mapView = remember { MKMapView() }
    mapView.showsUserLocation = false
    mapView.isZoomEnabled = false
    mapView.isScrollEnabled = false
    mapView.isRotateEnabled = false
    mapView.isPitchEnabled = false

    val uiViewController = ComposeUIViewController.current

    DisposableEffect(mapView) {
        onDispose {
            mapView.removeFromSuperview()
        }
    }

    LaunchedEffect(location) {
        withContext(Dispatchers.Main) {
            mapView.removeAnnotations(mapView.annotations)

            location?.let { loc ->
                val annotation = MKPointAnnotation.alloc().init()
                annotation.coordinate = platform.CoreLocation.CLLocationCoordinate2D(loc.latitude, loc.longitude)
                annotation.title = "Tu ubicación"
                mapView.addAnnotation(annotation)

                val region = MKCoordinateRegion(
                    platform.CoreLocation.CLLocationCoordinate2D(loc.latitude, loc.longitude),
                    platform.MapKit.MKCoordinateSpan(0.01, 0.01)
                )
                mapView.setRegion(region, animated = true)
            }
        }
    }

    androidx.compose.ui.viewinterop.UiView(
        factory = { mapView },
        modifier = modifier
            .background(Color(0xFFE0E0E0))
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
    )
}