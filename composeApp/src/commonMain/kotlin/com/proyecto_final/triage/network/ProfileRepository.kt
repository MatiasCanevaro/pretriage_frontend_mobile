package com.proyecto_final.triage.network

import com.proyecto_final.triage.config.AppConfig
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class PerfilResponse(
    val nombre: String,
    val apellido: String,
    val tipoDocumento: String,
    val numeroDocumento: String,
    val fechaNacimiento: String? = null,
    val sexo: String? = null,
    val genero: String? = null,
    val peso: String? = null,
    val altura: String? = null
)

private val json = Json { ignoreUnknownKeys = true }

suspend fun obtenerPerfil(): Result<PerfilResponse> {
    return try {
        val response = httpClient.get("${AppConfig.baseUrl}/api/perfil") {
            header(HttpHeaders.Authorization, "Bearer ${TokenStorage.getToken()}")
        }
        if (response.status == HttpStatusCode.OK) {
            val bodyText = response.bodyAsText()
            val perfil = json.decodeFromString<PerfilResponse>(bodyText)
            Result.success(perfil)
        } else {
            val bodyText = response.bodyAsText()
            val errorResponse = json.decodeFromString<ErrorResponse>(bodyText)
            Result.failure(Exception(errorResponse.error ?: "No se pudo obtener el perfil"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
