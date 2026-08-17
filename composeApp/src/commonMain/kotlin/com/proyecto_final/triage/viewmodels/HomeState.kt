package com.proyecto_final.triage.viewmodels

import com.proyecto_final.triage.network.EstadoConsultaPacienteDTO

sealed class HomeState {

    data object Loading : HomeState()

    data class Success(
        val estado: EstadoConsultaPacienteDTO
    ) : HomeState()

    data class Error(
        val message: String
    ) : HomeState()
}