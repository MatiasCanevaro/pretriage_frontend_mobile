package com.proyecto_final.triage.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.proyecto_final.triage.location.CurrentLocation

@Composable
expect fun PlatformMapView(
    location: CurrentLocation?,
    modifier: Modifier = Modifier
)