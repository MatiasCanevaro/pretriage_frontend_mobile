package com.proyecto_final.triage.network

import com.proyecto_final.triage.config.AppConfig
import com.proyecto_final.triage.screens.Credencial
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

private val camposDeCredencial = setOf(
    "nombreObraSocial", "numeroAfiliado", "plan", "fechaVencimiento"
)

sealed class CredencialUpdateResult {
    data class Success(val mensaje: String) : CredencialUpdateResult()
    data class FieldErrors(val errors: Map<String, String>) : CredencialUpdateResult()
    data class Error(val message: String) : CredencialUpdateResult()
}

suspend fun cargarCredencial(request: CredencialRequest): CredencialUpdateResult {
    return try {
        println("CREDENCIAL REQUEST: $request")

        val response = httpClient.post("${AppConfig.baseUrl}/api/obrasocial/credenciales") {
            contentType(ContentType.Application.Json)
            println("TOKEN: ${TokenStorage.getToken()}" )
            header(HttpHeaders.Authorization, "Bearer ${TokenStorage.getToken()}")
            setBody(request)
        }

        println("CREDENCIAL STATUS: ${response.status}")

        val bodyText = response.bodyAsText()
        println("CREDENCIAL BODY: $bodyText")

        if (response.status.value in 200..299) {
            val body = json.decodeFromString<CredencialResponse>(bodyText)
            return CredencialUpdateResult.Success(body.mensaje)
        }

        val erroresDeCampo = parsearErroresDeCampo(bodyText, camposDeCredencial)
        if (erroresDeCampo != null) {
            return CredencialUpdateResult.FieldErrors(erroresDeCampo)
        }

        val errorResponse = runCatching { json.decodeFromString<ErrorResponse>(bodyText) }.getOrNull()
        CredencialUpdateResult.Error(errorResponse?.error ?: "No se pudo cargar la credencial")

    } catch (e: Exception) {
        println("CREDENCIAL EXCEPTION: ${e.message}")
        CredencialUpdateResult.Error(e.message ?: "Error de conexión")
    }
}

suspend fun obtenerCredenciales(): Result<List<Credencial>> {
    return try {
        println("ENTRÉ AL GET")
        val response = httpClient.get("${AppConfig.baseUrl}/api/obrasocial/credenciales") {
            header(
                HttpHeaders.Authorization,
                "Bearer ${TokenStorage.getToken()}"
            )
        }
        println("GET STATUS: ${response.status}")
        println("GET BODY: ${response.bodyAsText()}")
        if (response.status == HttpStatusCode.OK) {
            val bodyText = response.bodyAsText()
            println("GET BODY: $bodyText")
            val credenciales = json.decodeFromString<List<Credencial>>(bodyText)
            Result.success(credenciales)
        } else {
            val bodyText = response.bodyAsText()
            val errorResponse = runCatching { json.decodeFromString<ErrorResponse>(bodyText) }.getOrNull()
            Result.failure(Exception(errorResponse?.error ?: "No se pudieron obtener las credenciales"))
        }

    } catch (e: Exception) {
        Result.failure(e)
    }
}