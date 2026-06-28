package com.proyecto_final.triage.network

import com.proyecto_final.triage.config.AppConfig
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.client.call.*
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json

suspend fun register(request: RegisterRequest): Result<String> {
    return try {
        println("ANTES DEL POST")
        val response = httpClient.post("${AppConfig.baseUrl}/api/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        println("DESPUES DEL POST: ${response.status}")
        val body = response.body<RegisterResponse>()
        println("BODY: $body")
        if (response.status == HttpStatusCode.OK && body.token != null) {
            Result.success(body.token)
        } else {
            Result.failure(Exception(body.error ?: "Error al registrar"))
        }
    } catch (e: Exception) {
        println("EXCEPTION EN REGISTER: ${e.message}")
        e.printStackTrace()
        Result.failure(e)
    }
}

private val json = Json { ignoreUnknownKeys = true }

suspend fun login(request: LoginRequest): Result<String> {
    return try {
        println("LOGIN REQUEST: $request")
        val response = httpClient.post("${AppConfig.baseUrl}/api/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        println("LOGIN STATUS: ${response.status}")
        val bodyText = response.bodyAsText()
        println("LOGIN BODY TEXT: $bodyText")

        if (response.status == HttpStatusCode.OK) {
            val body = json.decodeFromString<LoginResponse>(bodyText)
            if (body.token != null) {
                Result.success(body.token)
            } else {
                Result.failure(Exception("No se recibió token"))
            }
        } else {
            Result.failure(Exception("Credenciales inválidas"))
        }
    } catch (e: Exception) {
        println("LOGIN EXCEPTION: ${e.message}")
        Result.failure(e)
    }
}