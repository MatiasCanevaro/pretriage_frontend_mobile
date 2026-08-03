package com.proyecto_final.triage.network

import com.proyecto_final.triage.config.AppConfig
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

suspend fun obtenerEspecialidades(): Result<List<EspecialidadResponse>> {
    return try {

        val response = httpClient.get("${AppConfig.baseUrl}/api/especialidades")
        println("ESPECIALIDADES STATUS: ${response.status}")

        val body = response.bodyAsText()
        println("ESPECIALIDADES BODY: $body")

        if (response.status == HttpStatusCode.OK) {
            val especialidades = json.decodeFromString<List<EspecialidadResponse>>(body)
            Result.success(especialidades)
        } else {
            Result.failure(
                Exception("No se pudieron obtener las especialidades")
            )
        }

    } catch (e: Exception) {
        println("ESPECIALIDADES EXCEPTION: ${e.message}")
        Result.failure(e)
    }
}