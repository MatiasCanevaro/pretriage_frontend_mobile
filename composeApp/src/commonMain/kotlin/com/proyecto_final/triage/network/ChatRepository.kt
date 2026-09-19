package com.proyecto_final.triage.network

import com.proyecto_final.triage.config.AppConfig
import com.proyecto_final.triage.storage.TokenStorage
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
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

private suspend inline fun <reified T> procesarRespuesta(
    tag: String,
    response: HttpResponse,
    mensajeError: String
): Result<T> {
    println("$tag STATUS: ${response.status}")
    val bodyText = response.bodyAsText()
    println("$tag BODY: $bodyText")

    if (response.status == HttpStatusCode.OK) {
        return Result.success(json.decodeFromString<T>(bodyText))
    }
    val errorResponse = runCatching {
        json.decodeFromString<ErrorResponse>(bodyText)
    }.getOrNull()
    return Result.failure(Exception(errorResponse?.error ?: mensajeError))
}

suspend fun iniciarChat(): Result<ChatResponse> {
    return try {
        println("CHAT INICIAR")
        val response = httpClient.post("${AppConfig.baseUrl}/api/chat") {
            contentType(ContentType.Application.Json)
        }
        procesarRespuesta("CHAT INICIAR", response, "No se pudo iniciar el chat")
    } catch (e: Exception) {
        println("CHAT INICIAR EXCEPTION: ${e.message}")
        Result.failure(e)
    }
}

suspend fun obtenerChat(id: Long): Result<ChatResponse> {
    return try {
        println("CHAT OBTENER ($id)")
        val response = httpClient.get("${AppConfig.baseUrl}/api/chat/$id") {
        }
        procesarRespuesta("CHAT OBTENER", response, "No se pudo recuperar el chat")
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
            setBody(EnviarMensajeRequest(contenido = contenido))
        }
        procesarRespuesta("CHAT ENVIAR", response, "No se pudo enviar el mensaje")
    } catch (e: Exception) {
        println("CHAT ENVIAR EXCEPTION: ${e.message}")
        Result.failure(e)
    }
}