package com.proyecto_final.triage.storage

import android.content.Context
import androidx.core.content.edit
import com.proyecto_final.triage.appContext

class AndroidTokenStorage : TokenStorage {

    companion object {
        private const val PREFS_NAME = "auth_prefs"
        private const val TOKEN_KEY = "auth_token"
        private const val REFRESH_TOKEN_KEY = "refresh_token"
    }

    private val prefs by lazy {
        appContext.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
    }

    // Tokens en memoria cuando no se selecciona "Recuérdame"
    private var memoryToken: String? = null
    private var memoryRefreshToken: String? = null

    override fun saveTokens(
        token: String,
        refreshToken: String,
        remember: Boolean
    ) {
        if (remember) {
            prefs.edit {
                putString(TOKEN_KEY, token)
                putString(REFRESH_TOKEN_KEY, refreshToken)
            }

            // No necesitamos mantenerlos también en memoria
            memoryToken = null
            memoryRefreshToken = null
        } else {
            memoryToken = token
            memoryRefreshToken = refreshToken
        }
    }

    override fun getToken(): String? {
        return memoryToken
            ?: prefs.getString(TOKEN_KEY, null)
    }

    override fun getRefreshToken(): String? {
        return memoryRefreshToken
            ?: prefs.getString(REFRESH_TOKEN_KEY, null)
    }

    override fun clearTokens() {
        memoryToken = null
        memoryRefreshToken = null

        prefs.edit {
            remove(TOKEN_KEY)
            remove(REFRESH_TOKEN_KEY)
        }
    }
}