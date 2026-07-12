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

suspend fun cargarCredencial(request: CredencialRequest): Result<String> {
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

        if (response.status == HttpStatusCode.OK) {
            val body = json.decodeFromString<CredencialResponse>(bodyText)
            Result.success(body.mensaje)
        } else {
            Result.failure(Exception("No se pudo cargar la credencial"))
        }

    } catch (e: Exception) {
        println("CREDENCIAL EXCEPTION: ${e.message}")
        Result.failure(e)
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
            Result.failure(Exception("No se pudieron obtener las credenciales"))
        }

    } catch (e: Exception) {
        Result.failure(e)
    }
}