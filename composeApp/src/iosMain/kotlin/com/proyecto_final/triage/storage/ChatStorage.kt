package com.proyecto_final.triage.storage

import platform.Foundation.NSUserDefaults

actual object ChatStorage {
    private const val CHAT_ID_KEY = "chat_id"

    private val defaults get() = NSUserDefaults.Companion.standardUserDefaults

    actual fun saveChatId(id: Long) {
        defaults.setObject(id.toString(), forKey = CHAT_ID_KEY)
    }

    actual fun getChatId(): Long? {
        return defaults.stringForKey(CHAT_ID_KEY)?.toLongOrNull()
    }

    actual fun clearChatId() {
        defaults.removeObjectForKey(CHAT_ID_KEY)
    }
}