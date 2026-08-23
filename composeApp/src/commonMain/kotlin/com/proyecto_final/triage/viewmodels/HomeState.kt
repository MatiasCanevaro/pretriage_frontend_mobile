package com.proyecto_final.triage.viewmodels

import com.proyecto_final.triage.network.estadoConsulta.EstadoConsultaPacienteDTO
import com.proyecto_final.triage.network.estadoConsulta.HospitalSeleccionadoResponse

sealed class HomeState {

    data object Loading : HomeState()

    data class Success(
        val estado: EstadoConsultaPacienteDTO,
        val hospital: HospitalSeleccionadoResponse? = null
    ) : HomeState()

    data class SinConsultaActiva(val estado: EstadoConsultaPacienteDTO) : HomeState()

    data class Error(val message: String) : HomeState()
}