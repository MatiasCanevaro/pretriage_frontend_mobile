package com.proyecto_final.triage.network

import android.content.Context
import com.proyecto_final.triage.appContext

actual object TokenStorage {
    private const val PREFS_NAME = "auth_prefs"
    private const val TOKEN_KEY = "auth_token"

    private val prefs by lazy {
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Token en memoria, para cuando no se marca "Recuérdame"
    private var memoryToken: String? = null

    actual fun saveToken(token: String, remember: Boolean) {
        if (remember) {
            prefs.edit().putString(TOKEN_KEY, token).apply()
        } else {
            memoryToken = token
        }
    }

    actual fun getToken(): String? {
        return memoryToken ?: prefs.getString(TOKEN_KEY, null)
    }

    actual fun clearToken() {
        memoryToken = null
        prefs.edit().remove(TOKEN_KEY).apply()
    }
}

