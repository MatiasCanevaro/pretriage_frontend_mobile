package com.proyecto_final.triage.network

import kotlinx.serialization.Serializable


@Serializable
data class ErrorResponse(
    val error: String? = null
)