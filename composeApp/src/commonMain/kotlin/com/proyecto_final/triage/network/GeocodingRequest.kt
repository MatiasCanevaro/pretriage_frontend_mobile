package com.proyecto_final.triage.network

import kotlinx.serialization.Serializable

@Serializable
data class GeocodingResponse(
    val lat: String,
    val lon: String
)