package com.proyecto_final.triage.network

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val token: String? = null,
    val error: String? = null
)