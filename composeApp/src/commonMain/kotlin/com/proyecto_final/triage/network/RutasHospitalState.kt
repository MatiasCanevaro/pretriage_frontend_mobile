package com.proyecto_final.triage.network

sealed class RutasHospitalState {

    data object Loading : RutasHospitalState()

    data class Success(
        val rutas: List<TiempoEstimadoArriboHospitalResponse>
    ) : RutasHospitalState()

    data class Error(
        val message: String
    ) : RutasHospitalState()
}