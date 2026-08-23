package com.proyecto_final.triage.platform

import androidx.compose.runtime.Composable
import com.proyecto_final.triage.network.estadoConsulta.HospitalSeleccionadoResponse

@Composable
expect fun PlatformMap(
    hospital: HospitalSeleccionadoResponse?
)