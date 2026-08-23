package com.proyecto_final.triage.platform

import androidx.compose.runtime.Composable
import com.proyecto_final.triage.network.estadoConsulta.HospitalSeleccionadoResponse

@Composable
actual fun PlatformMap(
    hospital: HospitalSeleccionadoResponse?,
    ubicacion: String,
    polylineCode: String?
) {
}