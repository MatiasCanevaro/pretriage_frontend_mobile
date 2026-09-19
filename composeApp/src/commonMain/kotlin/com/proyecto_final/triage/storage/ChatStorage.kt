package com.proyecto_final.triage.storage

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object ChatStorage {
    fun saveChatId(id: Long)
    fun getChatId(): Long?
    fun clearChatId()
}