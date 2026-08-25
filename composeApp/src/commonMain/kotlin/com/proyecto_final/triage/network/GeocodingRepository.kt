package com.proyecto_final.triage.network

import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json

suspend fun buscarDireccion(
    direccion: String
): Result<GeocodingResponse> {

    return try {

        val response = httpClient.get(
            "https://nominatim.openstreetmap.org/search"
        ) {
            parameter("q", direccion)
            parameter("format", "json")
            parameter("limit", 1)
        }

        val resultado =
            Json.decodeFromString<List<GeocodingResponse>>(
                response.bodyAsText()
            )

        if (resultado.isNotEmpty()) {
            Result.success(resultado.first())
        } else {
            Result.failure(
                Exception("No se encontró la dirección")
            )
        }

    } catch (e: Exception) {
        Result.failure(e)
    }
}