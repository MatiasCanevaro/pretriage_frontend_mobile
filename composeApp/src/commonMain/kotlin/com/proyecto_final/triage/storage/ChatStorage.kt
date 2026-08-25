package com.proyecto_final.triage.storage

expect object ChatStorage {
    fun saveChatId(id: Long)
    fun getChatId(): Long?
    fun clearChatId()
}