package com.proyecto_final.triage.network

import com.proyecto_final.triage.config.AppConfig
import com.proyecto_final.triage.screens.Credencial
import com.proyecto_final.triage.storage.TokenStorage
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
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

private fun procesarCredencialResponse(status: HttpStatusCode, bodyText: String): CredencialUpdateResult {
    if (status.value in 200..299) {
        val body = json.decodeFromString<CredencialResponse>(bodyText)
        return CredencialUpdateResult.Success(body.mensaje)
    }

    val erroresDeCampo = parsearErroresDeCampo(bodyText, camposDeCredencial)
    if (erroresDeCampo != null) {
        return CredencialUpdateResult.FieldErrors(erroresDeCampo)
    }

    val errorResponse = runCatching { json.decodeFromString<ErrorResponse>(bodyText) }.getOrNull()
    return CredencialUpdateResult.Error(errorResponse?.error ?: "No se pudo procesar la credencial")
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

        procesarCredencialResponse(response.status, bodyText)

    } catch (e: Exception) {
        println("CREDENCIAL EXCEPTION: ${e.message}")
        CredencialUpdateResult.Error(e.message ?: "Error de conexión")
    }
}

suspend fun actualizarCredencial(idCredencial: Long, request: CredencialRequest): CredencialUpdateResult {
    return try {
        println("CREDENCIAL UPDATE REQUEST ($idCredencial): $request")

        val response = httpClient.put("${AppConfig.baseUrl}/api/obrasocial/credenciales/$idCredencial") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer ${TokenStorage.getToken()}")
            setBody(request)
        }

        println("CREDENCIAL UPDATE STATUS: ${response.status}")

        val bodyText = response.bodyAsText()
        println("CREDENCIAL UPDATE BODY: $bodyText")

        procesarCredencialResponse(response.status, bodyText)

    } catch (e: Exception) {
        println("CREDENCIAL UPDATE EXCEPTION: ${e.message}")
        CredencialUpdateResult.Error(e.message ?: "Error de conexión")
    }
}

suspend fun eliminarCredencial(idCredencial: Long): CredencialUpdateResult {
    return try {
        println("CREDENCIAL DELETE REQUEST ($idCredencial)")

        val response = httpClient.delete("${AppConfig.baseUrl}/api/obrasocial/credenciales/$idCredencial") {
            header(HttpHeaders.Authorization, "Bearer ${TokenStorage.getToken()}")
        }

        println("CREDENCIAL DELETE STATUS: ${response.status}")

        val bodyText = response.bodyAsText()
        println("CREDENCIAL DELETE BODY: $bodyText")

        procesarCredencialResponse(response.status, bodyText)

    } catch (e: Exception) {
        println("CREDENCIAL DELETE EXCEPTION: ${e.message}")
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