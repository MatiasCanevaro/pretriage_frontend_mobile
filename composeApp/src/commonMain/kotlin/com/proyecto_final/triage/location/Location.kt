package com.proyecto_final.triage.location

data class CurrentLocation(
    val latitude: Double,
    val longitude: Double
)

expect suspend fun obtenerUbicacionActual(): CurrentLocation?