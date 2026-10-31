package com.proyecto_final.triage.storage

import com.proyecto_final.triage.network.AtencionEstimada
import com.proyecto_final.triage.network.ChatResponse

/** One in-memory navigation snapshot. Never writes transcripts to preferences or disk. */
internal class ChatConversationCache {
    private var chat: ChatResponse? = null
    private var atencion: AtencionEstimada? = null
    private var solicitudChatId: Long? = null
    private var solicitudTexto: Pair<String, String>? = null

    fun guardar(snapshot: ChatResponse, estimacion: AtencionEstimada?) {
        chat = snapshot
        atencion = estimacion
    }

    fun recuperar(id: Long): Pair<ChatResponse, AtencionEstimada?>? =
        chat?.takeIf { it.id == id }?.let { it to atencion }

    fun guardarSolicitudTexto(chatId: Long, solicitudId: String?, contenido: String?) {
        solicitudChatId = chatId
        solicitudTexto = if (solicitudId != null && contenido != null) solicitudId to contenido else null
    }

    fun recuperarSolicitudTexto(chatId: Long): Pair<String, String>? =
        solicitudTexto?.takeIf { solicitudChatId == chatId }

    fun limpiar() {
        chat = null
        atencion = null
        solicitudChatId = null
        solicitudTexto = null
    }
}

internal val chatConversationCache = ChatConversationCache()
