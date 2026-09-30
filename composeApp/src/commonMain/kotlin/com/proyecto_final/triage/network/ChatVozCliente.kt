package com.proyecto_final.triage.network

import com.proyecto_final.triage.config.AppConfig
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Protocolo del WebSocket /api/chat/{id}/voz del backend (Gemini Live).
// Cliente -> servidor: audio PCM 16 bits mono little-endian a 16 kHz (frames binarios)
// y eventos de control JSON. Servidor -> cliente: audio PCM 24 kHz y eventos JSON.

sealed interface MensajeVozCliente {
    class Audio(val pcm: ByteArray) : MensajeVozCliente
    data object FinAudio : MensajeVozCliente
    data object Cerrar : MensajeVozCliente
}

sealed interface EventoVoz {
    class Audio(val pcm: ByteArray) : EventoVoz
    data object Listo : EventoVoz
    data class TranscripcionPaciente(val texto: String) : EventoVoz
    data class TranscripcionBot(val texto: String) : EventoVoz
    data object Interrumpido : EventoVoz
    data object TurnoCompleto : EventoVoz
    data object SesionPorExpirar : EventoVoz
    data object EntrevistaFinalizada : EventoVoz
    data class TriageFinalizado(
        val respuesta: ChatMensaje?,
        val atencionEstimada: AtencionEstimada?
    ) : EventoVoz
    data class Error(val mensaje: String) : EventoVoz
    data object Fin : EventoVoz
}

@Serializable
private data class EventoVozDTO(
    val tipo: String,
    val texto: String? = null,
    val mensaje: String? = null,
    val respuesta: ChatMensaje? = null,
    val atencionEstimada: AtencionEstimada? = null
)

private val jsonVoz = Json { ignoreUnknownKeys = true }

private fun parsearEvento(texto: String): EventoVoz? {
    val evento = runCatching { jsonVoz.decodeFromString<EventoVozDTO>(texto) }.getOrNull() ?: return null
    return when (evento.tipo) {
        "listo" -> EventoVoz.Listo
        "transcripcion_paciente" -> evento.texto?.let { EventoVoz.TranscripcionPaciente(it) }
        "transcripcion_bot" -> evento.texto?.let { EventoVoz.TranscripcionBot(it) }
        "interrumpido" -> EventoVoz.Interrumpido
        "turno_completo" -> EventoVoz.TurnoCompleto
        "sesion_por_expirar" -> EventoVoz.SesionPorExpirar
        "entrevista_finalizada" -> EventoVoz.EntrevistaFinalizada
        "triage_finalizado" -> EventoVoz.TriageFinalizado(evento.respuesta, evento.atencionEstimada)
        "error" -> EventoVoz.Error(evento.mensaje ?: "Error en el chat de voz")
        "fin" -> EventoVoz.Fin
        else -> null
    }
}

// http://host:8080 -> ws://host:8080 ; https -> wss
private fun urlChatVoz(chatId: Long): String =
    "${AppConfig.baseUrl.replaceFirst("http", "ws")}/api/chat/$chatId/voz"

/**
 * Abre el canal de voz del chat y lo mantiene hasta que el backend lo cierra.
 * El JWT viaja en el header Authorization (plugin Auth del httpClient).
 * [salida] se envía al backend; cada frame recibido se entrega a [onEvento].
 */
suspend fun conectarChatVoz(
    chatId: Long,
    salida: ReceiveChannel<MensajeVozCliente>,
    onEvento: suspend (EventoVoz) -> Unit
): Result<Unit> {
    return try {
        println("CHAT VOZ CONECTAR ($chatId)")
        httpClient.webSocket(urlString = urlChatVoz(chatId)) {
            val envio = launch {
                for (mensaje in salida) {
                    when (mensaje) {
                        is MensajeVozCliente.Audio -> send(Frame.Binary(true, mensaje.pcm))
                        MensajeVozCliente.FinAudio -> send(Frame.Text("""{"tipo":"fin_audio"}"""))
                        MensajeVozCliente.Cerrar -> send(Frame.Text("""{"tipo":"cerrar"}"""))
                    }
                }
            }
            try {
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Binary -> onEvento(EventoVoz.Audio(frame.data))
                        is Frame.Text -> {
                            val texto = frame.readText()
                            if (!texto.contains("\"transcripcion_")) println("CHAT VOZ EVENTO: $texto")
                            parsearEvento(texto)?.let { onEvento(it) }
                        }
                        else -> Unit
                    }
                }
            } finally {
                envio.cancel()
            }
        }
        println("CHAT VOZ CERRADO ($chatId)")
        Result.success(Unit)
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        println("CHAT VOZ EXCEPTION: ${e.message}")
        Result.failure(e)
    }
}
