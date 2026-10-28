package com.proyecto_final.triage.viewmodels

import com.proyecto_final.triage.network.ChatResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

internal fun ChatUiState.esperandoResultadoCierre(): Boolean = finalizado &&
    (estadoClasificacion.isNullOrBlank() || estadoClasificacion.equals("PENDIENTE", ignoreCase = true))

/** A GET during voice closure may still see the transaction's earlier history. */
internal fun ChatUiState.aplicarActualizacionTrasVoz(chat: ChatResponse): ChatUiState {
    if (chat.id != null && chatId != null && chat.id != chatId) return this
    val estadoActual = estadoClasificacion?.uppercase()
    val estadoRemoto = chat.estadoClasificacion?.uppercase()
    val estadoActualEsTerminal = estadoActual == "COMPLETADA" || estadoActual == "FALLIDA" ||
        (finalizado && estadoActual == "NO_SOLICITADA")
    val remotoCompleto = estadoRemoto == "COMPLETADA"
    val falloAvanzaACompletada = estadoActual == "FALLIDA" && remotoCompleto
    if (estadoActualEsTerminal && estadoRemoto != estadoActual && !falloAvanzaACompletada) return this
    val terminal = estadoRemoto == "COMPLETADA" || estadoRemoto == "FALLIDA" ||
        (chat.finalizado && estadoRemoto == "NO_SOLICITADA")
    // A pre-closure GET can race with the saved closure transaction.
    val preservarCierre = finalizado && !chat.finalizado &&
        estadoRemoto == "NO_SOLICITADA"
    return copy(
        chatId = chat.id ?: chatId,
        mensajes = if (terminal || chat.mensajes.size >= mensajes.size) chat.mensajes else mensajes,
        finalizado = finalizado || chat.finalizado || terminal,
        estadoClasificacion = if (preservarCierre) estadoClasificacion else chat.estadoClasificacion,
        respuestaPendiente = if (terminal) false else respuestaPendiente,
        estadoVoz = if (terminal) EstadoVoz.INACTIVA else estadoVoz,
        error = if (terminal) null else error,
        errorVoz = if (terminal) false else errorVoz,
        actualizacionCierreDisponible = if (terminal) false else actualizacionCierreDisponible
    )
}

/** A completed polling budget ends the loading state even if the voice socket never closes. */
internal fun ChatUiState.aplicarLimiteEsperaCierre(ultimaLectura: ChatResponse?): ChatUiState {
    val agotado = copy(
        estadoVoz = EstadoVoz.INACTIVA,
        actualizacionCierreDisponible = true,
        procesandoRespuestaVoz = false,
        progresoVozControladoPorServidor = false,
        asistenteHablandoVoz = false
    )
    val confirmado = ultimaLectura?.let(agotado::aplicarActualizacionTrasVoz) ?: agotado
    return if (confirmado.esperandoResultadoCierre()) confirmado.copy(estadoClasificacion = null) else confirmado
}

/** Only GETs; no patient message or classification request is repeated. */
internal suspend fun seguirResultadoCierre(
    chatId: Long,
    pendiente: () -> Boolean,
    obtener: suspend (Long, Long) -> Result<ChatResponse>,
    aplicar: (ChatResponse) -> Unit,
    esperar: suspend (Long) -> Unit = { delay(it) }
) {
    repeat(80) {
        if (!pendiente()) return
        obtener(chatId, 2_000L).onSuccess { aplicar(it) }.onFailure {
            if (it is CancellationException) throw it
        }
        if (pendiente()) esperar(1_500L)
    }
}
