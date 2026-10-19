package com.proyecto_final.triage.network.cambioContrasenia

import kotlinx.serialization.Serializable

@Serializable
data class CambiarContraseniaAutenticadaRequest(
    val contraseniaActual: String,
    val contraseniaNueva: String
)