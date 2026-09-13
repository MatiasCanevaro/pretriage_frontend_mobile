package com.proyecto_final.triage.storage

actual object TokenStorage {

    actual fun saveTokens(token: String, refreshToken: String, remember: Boolean) {
        // Implementación específica para iOS
    }

    actual fun getToken(): String? {
        // Implementación específica para iOS
        return null
    }

    actual fun getRefreshToken(): String? {
        // Implementación específica para iOS
        return null
    }

    actual fun clearTokens() {
        // Implementación específica para iOS
    }
}