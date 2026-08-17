package com.proyecto_final.triage.network

import com.proyecto_final.triage.config.AppConfig
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val AUTOR_BOT = "BOT"
const val AUTOR_PACIENTE = "PACIENTE"

fun esAutorBot(autor: String?): Boolean = autor == AUTOR_BOT

@Serializable
data class ChatMensaje(
    val contenido: String,
    val autor: String? = null,
    val fechaHoraEnvio: String? = null
)

@Serializable
data class ChatResponse(
    val id: Long? = null,
    val mensajes: List<ChatMensaje> = emptyList(),
    val timestamp: String? = null,
    val finalizado: Boolean = false
)

@Serializable
data class EnviarMensajeRequest(
    val contenido: String
)

@Serializable
data class AtencionEstimada(
    val consultaId: Long? = null,
    val fechaHoraAtencionEstimada: String? = null,
    val hayMedicosActivos: Boolean = false,
    val medicosActivos: Int = 0,
    val medicosParaEstimacion: Int = 0,
    val posicionEnCola: Int = 0,
    val pacientesAntes: Int = 0,
    val minutosPromedioAtencion: Int = 0,
    val mensaje: String? = null
)

@Serializable
data class RespuestaChatResponse(
    val respuesta: ChatMensaje,
    val atencionEstimada: AtencionEstimada? = null
)

private val json = Json { ignoreUnknownKeys = true }

suspend fun iniciarChat(): Result<ChatResponse> {
    return try {
        println("CHAT INICIAR")
        val response = httpClient.post("${AppConfig.baseUrl}/api/chat") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer ${TokenStorage.getToken()}")
        }

        println("CHAT INICIAR STATUS: ${response.status}")
        val bodyText = response.bodyAsText()
        println("CHAT INICIAR BODY: $bodyText")

        if (response.status == HttpStatusCode.OK) {
            val chat = json.decodeFromString<ChatResponse>(bodyText)
            Result.success(chat)
        } else {
            val errorResponse = runCatching {
                json.decodeFromString<ErrorResponse>(bodyText)
            }.getOrNull()
            Result.failure(Exception(errorResponse?.error ?: "No se pudo iniciar el chat"))
        }
    } catch (e: Exception) {
        println("CHAT INICIAR EXCEPTION: ${e.message}")
        Result.failure(e)
    }
}

suspend fun obtenerChat(id: Long): Result<ChatResponse> {
    return try {
        println("CHAT OBTENER ($id)")
        val response = httpClient.get("${AppConfig.baseUrl}/api/chat/$id") {
            header(HttpHeaders.Authorization, "Bearer ${TokenStorage.getToken()}")
        }

        println("CHAT OBTENER STATUS: ${response.status}")
        val bodyText = response.bodyAsText()
        println("CHAT OBTENER BODY: $bodyText")

        if (response.status == HttpStatusCode.OK) {
            val chat = json.decodeFromString<ChatResponse>(bodyText)
            Result.success(chat)
        } else {
            val errorResponse = runCatching {
                json.decodeFromString<ErrorResponse>(bodyText)
            }.getOrNull()
            Result.failure(Exception(errorResponse?.error ?: "No se pudo recuperar el chat"))
        }
    } catch (e: Exception) {
        println("CHAT OBTENER EXCEPTION: ${e.message}")
        Result.failure(e)
    }
}

suspend fun enviarMensaje(id: Long, contenido: String): Result<RespuestaChatResponse> {
    return try {
        println("CHAT ENVIAR ($id): $contenido")
        val response = httpClient.post("${AppConfig.baseUrl}/api/chat/$id/mensajes") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer ${TokenStorage.getToken()}")
            setBody(EnviarMensajeRequest(contenido = contenido))
        }

        println("CHAT ENVIAR STATUS: ${response.status}")
        val bodyText = response.bodyAsText()
        println("CHAT ENVIAR BODY: $bodyText")

        if (response.status == HttpStatusCode.OK) {
            val respuesta = json.decodeFromString<RespuestaChatResponse>(bodyText)
            Result.success(respuesta)
        } else {
            val errorResponse = runCatching {
                json.decodeFromString<ErrorResponse>(bodyText)
            }.getOrNull()
            Result.failure(Exception(errorResponse?.error ?: "No se pudo enviar el mensaje"))
        }
    } catch (e: Exception) {
        println("CHAT ENVIAR EXCEPTION: ${e.message}")
        Result.failure(e)
    }
}