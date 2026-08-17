package com.proyecto_final.triage.network

import kotlinx.serialization.Serializable

@Serializable
data class SeleccionHospitalRequest(
    val placeId: String,
    val codigoEspecialidad: String
)