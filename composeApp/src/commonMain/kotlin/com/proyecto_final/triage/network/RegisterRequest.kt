package com.proyecto_final.triage.network

import kotlinx.serialization.Serializable

/*TODO: Faltan los siguientes campos
                            val fechaNacimiento: String,
                            val genero: String,
                            val sexo: String,
                            val peso: String,
                            val altura: String*/

@Serializable
data class RegisterRequest( val nombre: String,
                            val apellido: String,
                            val numeroDocumento: String,
                            val tipoDocumento: String,
                            val tipoUsuario: String,
                            val matricula: String? = null,
                            val email: String,
                            val password: String
)
@Serializable
data class RegisterResponse(
    val token: String? = null,
    val error: String? = null
)