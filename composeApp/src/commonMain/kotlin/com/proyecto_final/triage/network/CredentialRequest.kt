package com.proyecto_final.triage.network

import kotlinx.serialization.Serializable

@Serializable
data class CredencialRequest(
    val nombreObraSocial: String,
    val numeroAfiliado: String,
    val plan: String,
    val fechaVencimiento: String
)

@Serializable
data class CredencialResponse(
    val mensaje: String
)