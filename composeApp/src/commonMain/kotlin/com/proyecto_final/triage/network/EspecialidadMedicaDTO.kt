package com.proyecto_final.triage.network

import kotlinx.serialization.Serializable

@Serializable
data class EspecialidadMedicaDTO(
    val codigo: String,
    val nombre: String
)