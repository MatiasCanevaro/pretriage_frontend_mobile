package com.proyecto_final.triage.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

internal const val CHAT_RECONCILIATION_TIMEOUT_MS = 1_200L
private val reconciliationBackoffMs = listOf(250L, 500L, 1_000L)

internal sealed interface ResultadoEnvioChat {
    data class Respuesta(val respuesta: RespuestaChatResponse) : ResultadoEnvioChat
    data class Confirmado(val chat: ChatResponse, val procesando: Boolean = false) : ResultadoEnvioChat
    data class Desconocido(val solicitudId: String, val chat: ChatResponse? = null) : ResultadoEnvioChat
    data class NoConfirmado(val error: Throwable) : ResultadoEnvioChat
}

/**
 * Submit one patient turn. An ambiguous POST is reconciled by GET only; the same request ID is
 * retained for status recovery, and this function never replays the POST.
 */
internal suspend fun enviarConReconciliacion(
    chatId: Long,
    contenido: String,
    solicitudId: String,
    mensajesAntes: List<ChatMensaje>,
    enviar: suspend (Long, String, String) -> Result<ResultadoPostMensaje>,
    obtener: suspend (Long, Long) -> Result<ChatResponse>,
    esperar: suspend (Long) -> Unit = { delay(it) }
): ResultadoEnvioChat {
    val post = try {
        enviar(chatId, contenido, solicitudId)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }

    post.getOrNull()?.let { resultado ->
        when (resultado) {
            is ResultadoPostMensaje.RespuestaInmediata -> return ResultadoEnvioChat.Respuesta(resultado.respuesta)
            is ResultadoPostMensaje.TurnoAceptado -> {
                val idRemoto = resultado.chat.solicitudTurnoTextoId
                val estadoRemoto = resultado.chat.estadoTurnoTexto
                if ((idRemoto != null && idRemoto != solicitudId) ||
                    (estadoRemoto.equals("COMPLETADA", ignoreCase = true) && idRemoto != solicitudId)
                ) return ResultadoEnvioChat.Desconocido(solicitudId, resultado.chat)
                return ResultadoEnvioChat.Confirmado(
                    resultado.chat,
                    procesando = !estadoRemoto.equals("COMPLETADA", ignoreCase = true) &&
                        !estadoRemoto.equals("NO_DISPONIBLE", ignoreCase = true)
                )
            }
        }
    }

    val errorPost = post.exceptionOrNull() ?: IllegalStateException("No se pudo enviar el mensaje")
    if (errorPost is CancellationException) throw errorPost
    if (errorPost is ErrorHttpChat && errorPost.estado !in 500..599 && errorPost.estado != 409) {
        return ResultadoEnvioChat.NoConfirmado(errorPost)
    }

    var ultimoChat: ChatResponse? = null
    for (attempt in 0..reconciliationBackoffMs.size) {
        val lectura = try {
            obtener(chatId, CHAT_RECONCILIATION_TIMEOUT_MS)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }
        lectura.onSuccess { chat ->
            ultimoChat = chat
            val estadoTurno = chat.estadoTurnoTexto.orEmpty()
            if (chat.solicitudTurnoTextoId == solicitudId) {
                return ResultadoEnvioChat.Confirmado(
                    chat,
                    procesando = estadoTurno.equals("PENDIENTE", ignoreCase = true)
                )
            }
            if (estadoTurno.equals("NO_DISPONIBLE", ignoreCase = true) || chat.finalizado ||
                chat.estadoClasificacion.equals("COMPLETADA", ignoreCase = true) ||
                chat.estadoClasificacion.equals("FALLIDA", ignoreCase = true)
            ) return ResultadoEnvioChat.Confirmado(chat)

            // Backward compatibility with servers that do not expose durable turn IDs.
            val suffix = nuevoSufijoOrdenado(mensajesAntes, chat.mensajes)
            val patientIndex = suffix.indexOfFirst {
                it.autor?.trim() == AUTOR_PACIENTE && it.contenido.trim() == contenido.trim()
            }
            if (patientIndex >= 0) {
                return ResultadoEnvioChat.Confirmado(
                    chat,
                    procesando = !suffix.drop(patientIndex + 1).any { it.autor?.trim() == AUTOR_BOT }
                )
            }
        }.onFailure { error -> if (error is CancellationException) throw error }

        if (attempt < reconciliationBackoffMs.size) esperar(reconciliationBackoffMs[attempt])
    }

    // The caller keeps this request locked and switches to status-only polling. A missing GET
    // snapshot cannot prove that an in-flight POST was rejected.
    return ResultadoEnvioChat.Desconocido(solicitudId, ultimoChat)
}

/** Trim-only author/content comparison mirrors backend input trimming without ignoring order or duplicates. */
internal fun nuevoSufijoOrdenado(
    anteriores: List<ChatMensaje>,
    actuales: List<ChatMensaje>
): List<ChatMensaje> {
    if (actuales.size <= anteriores.size) return emptyList()
    val prefijoCoincide = anteriores.indices.all { index ->
        val anterior = anteriores[index]
        val actual = actuales[index]
        anterior.autor?.trim() == actual.autor?.trim() && anterior.contenido.trim() == actual.contenido.trim()
    }
    return if (prefijoCoincide) actuales.drop(anteriores.size) else emptyList()
}

internal fun hayRespuestaBotNuevaDesde(mensajesAntes: List<ChatMensaje>, mensajesActuales: List<ChatMensaje>): Boolean =
    nuevoSufijoOrdenado(mensajesAntes, mensajesActuales).any { it.autor?.trim() == AUTOR_BOT }
