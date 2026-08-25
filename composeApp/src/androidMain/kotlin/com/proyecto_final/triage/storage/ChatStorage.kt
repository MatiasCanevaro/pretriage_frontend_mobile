package com.proyecto_final.triage.storage

import android.content.Context
import androidx.core.content.edit
import com.proyecto_final.triage.appContext

actual object ChatStorage {
    private const val PREFS_NAME = "chat_prefs"
    private const val CHAT_ID_KEY = "chat_id"

    private val prefs by lazy {
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun saveChatId(id: Long) {
        prefs.edit { putLong(CHAT_ID_KEY, id) }
    }

    actual fun getChatId(): Long? {
        val id = prefs.getLong(CHAT_ID_KEY, -1L)
        return if (id == -1L) null else id
    }

    actual fun clearChatId() {
        prefs.edit().remove(CHAT_ID_KEY).apply()
    }
}