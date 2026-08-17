package com.proyecto_final.triage.network

expect object ChatStorage {
    fun saveChatId(id: Long)
    fun getChatId(): Long?
    fun clearChatId()
}