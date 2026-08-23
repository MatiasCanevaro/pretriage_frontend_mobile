package com.proyecto_final.triage.network.auth

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

@Serializable
data class RegisterRequest(
    val nombre: String,
    val apellido: String,
    val numeroDocumento: String,
    val tipoDocumento: String,
    val email: String,
    val password: String,
    val tipoUsuario: String,
    val rol: String
)

@Serializable
data class RegisterResponse(
    val token: String? = null,
    val message: String? = null,
    val error: String? = null
)