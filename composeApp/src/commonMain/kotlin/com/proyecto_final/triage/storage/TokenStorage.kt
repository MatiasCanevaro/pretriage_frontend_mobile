package com.proyecto_final.triage.storage

expect object TokenStorage {
    fun saveToken(token: String, remember: Boolean)
    fun getToken(): String?
    fun clearToken()
}