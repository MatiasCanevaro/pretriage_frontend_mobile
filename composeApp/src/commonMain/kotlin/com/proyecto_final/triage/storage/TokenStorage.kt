package com.proyecto_final.triage.storage

interface TokenStorage {

    fun saveTokens(token: String, refreshToken: String, remember: Boolean)

    fun getToken(): String?

    fun getRefreshToken(): String?

    fun clearTokens()
}