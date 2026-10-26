package com.proyecto_final.triage.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

internal const val CHAT_RECONCILIATION_TIMEOUT_MS = 1_200L

private val reconciliationBackoffMs = listOf(250L, 500L, 1_000L)

internal sealed interface ResultadoEnvioChat {
    data class Respuesta(val respuesta: RespuestaChatResponse) : ResultadoEnvioChat
    data class Confirmado(val chat: ChatResponse, val procesando: Boolean = false) : ResultadoEnvioChat
    data class NoConfirmado(val error: Throwable) : ResultadoEnvioChat
}

/**
 * A failed POST has an unknown outcome. Check whether the exact new patient message appears
 * after the pre-send ordered snapshot before exposing it as unsent. A patient-only suffix may
 * still be processing, so poll a bounded number of times and never submit the POST again here.
 */
internal suspend fun enviarConReconciliacion(
    chatId: Long,
    contenido: String,
    mensajesAntes: List<ChatMensaje>,
    enviar: suspend (Long, String) -> Result<RespuestaChatResponse>,
    obtener: suspend (Long, Long) -> Result<ChatResponse>,
    esperar: suspend (Long) -> Unit = { delay(it) }
): ResultadoEnvioChat {
    val respuestaPost = try {
        enviar(chatId, contenido)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }

    respuestaPost.getOrNull()?.let { return ResultadoEnvioChat.Respuesta(it) }
    val errorPost = respuestaPost.exceptionOrNull() ?: IllegalStateException("No se pudo enviar el mensaje")
    if (errorPost is CancellationException) throw errorPost
    if (errorPost is ErrorHttpChat && errorPost.estado != 409 && errorPost.estado !in 500..599) {
        return ResultadoEnvioChat.NoConfirmado(errorPost)
    }

    var mensajePacienteGuardado: ChatResponse? = null

    for (attempt in 0..reconciliationBackoffMs.size) {
        val lectura = try {
            obtener(chatId, CHAT_RECONCILIATION_TIMEOUT_MS)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }

        lectura.onSuccess { chat ->
            if (chat.finalizado || chat.estadoClasificacion.equals("FALLIDA", ignoreCase = true)) {
                return ResultadoEnvioChat.Confirmado(chat)
            }

            val suffix = nuevoSufijoOrdenado(mensajesAntes, chat.mensajes)
            val patientIndex = suffix.indexOfFirst {
                it.autor?.trim() == AUTOR_PACIENTE && it.contenido.trim() == contenido.trim()
            }
            if (patientIndex >= 0) {
                if (suffix.drop(patientIndex + 1).any { it.autor?.trim() == AUTOR_BOT }) {
                    return ResultadoEnvioChat.Confirmado(chat)
                }
                mensajePacienteGuardado = chat
            }
        }.onFailure { error ->
            if (error is CancellationException) throw error
        }

        if (attempt < reconciliationBackoffMs.size) {
            esperar(reconciliationBackoffMs[attempt])
        }
    }

    mensajePacienteGuardado?.let {
        return ResultadoEnvioChat.Confirmado(it, procesando = true)
    }

    // A missing message in an open snapshot is not proof that the POST was rejected;
    // preserve the user's draft and surface the original send failure for an explicit retry.
    return ResultadoEnvioChat.NoConfirmado(errorPost)
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
