package com.proyecto_final.triage.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

internal const val CHAT_TEXT_TURN_POLL_INTERVAL_MS = 1_500L
internal const val CHAT_TEXT_TURN_MAX_POLLS = 80

/** Polls durable turn state using GET only. The caller supplies the overall 120 second timeout. */
internal suspend fun seguirTurnoTexto(
    chatId: Long,
    siguePendiente: () -> Boolean,
    obtener: suspend (Long, Long) -> Result<ChatResponse>,
    aplicar: (ChatResponse) -> Unit,
    esperar: suspend (Long) -> Unit = { delay(it) }
): ChatResponse? {
    var ultimaLectura: ChatResponse? = null
    repeat(CHAT_TEXT_TURN_MAX_POLLS) { index ->
        if (!siguePendiente()) return ultimaLectura
        val resultado = try {
            obtener(chatId, 2_000L)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
        resultado?.onSuccess { chat ->
            ultimaLectura = chat
            aplicar(chat)
        }?.onFailure { error -> if (error is CancellationException) throw error }
        if (!siguePendiente()) return ultimaLectura
        if (index < CHAT_TEXT_TURN_MAX_POLLS - 1) esperar(CHAT_TEXT_TURN_POLL_INTERVAL_MS)
    }
    return ultimaLectura
}

internal fun ChatResponse.turnoTextoPendiente(): Boolean =
    estadoTurnoTexto.equals("PENDIENTE", ignoreCase = true)

internal fun ChatResponse.turnoTextoNoDisponible(): Boolean =
    estadoTurnoTexto.equals("NO_DISPONIBLE", ignoreCase = true)

