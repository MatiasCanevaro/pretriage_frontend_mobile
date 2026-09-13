package com.proyecto_final.triage.network

import com.proyecto_final.triage.config.AppConfig
import com.proyecto_final.triage.network.auth.LoginResponse
import com.proyecto_final.triage.network.auth.RefreshTokenRequest
import com.proyecto_final.triage.storage.TokenStorageProvider
import com.proyecto_final.triage.viewmodels.AuthState
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

val httpClient = HttpClient {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 60_000
        connectTimeoutMillis = 15_000
        socketTimeoutMillis = 60_000
    }
    install(SSE)
    install(Auth) {
        bearer {

            // Carga el token actual
            loadTokens {
                val token = TokenStorageProvider.instance.getToken()
                val refreshToken = TokenStorageProvider.instance.getRefreshToken()
                if (token != null && refreshToken != null) {
                    BearerTokens(token, refreshToken)
                } else null
            }

            // Cuando recibe 401, intenta renovar el token
            refreshTokens {

                println("REFRESH: intentando renovar token.")

                val refreshToken = TokenStorageProvider.instance.getRefreshToken()
                if (refreshToken == null) {
                    println("REFRESH: No hay refreshToken, sesión corrupta.")
                    TokenStorageProvider.instance.clearTokens()
                    AuthState.onSessionExpired()
                    return@refreshTokens null
                }

                try {
                    val response = client.post("${AppConfig.baseUrl}/api/renovar") {
                        contentType(ContentType.Application.Json)
                        setBody(RefreshTokenRequest(refreshToken))
                        markAsRefreshTokenRequest()
                    }

                    println("REFRESH STATUS: ${response.status}")

                    if (response.status == HttpStatusCode.OK) {
                        val body = response.body<LoginResponse>()
                        val newToken = body.token
                        val newRefreshToken = body.refreshToken

                        println("REFRESH: Se recibieron correctamente los nuevos tokens.")

                        if (newToken != null && newRefreshToken != null) {
                            // Guardamos los nuevos tokens manteniendo la preferencia de "recuérdame"
                            val remember = TokenStorageProvider.instance.getToken() != null
                            TokenStorageProvider.instance.saveTokens(newToken, newRefreshToken, remember)
                            BearerTokens(newToken, newRefreshToken)
                        } else null
                    } else {
                        // El refresh falló, limpiamos tokens
                        println("REFRESH: Falló la renovación del token. Limpiando tokens.")
                        TokenStorageProvider.instance.clearTokens()
                        null
                    }
                }  catch (e: Exception) {
                        println("REFRESH EXCEPTION: ${e.message}")
                        println("REFRESH EXCEPTION CAUSE: ${e.cause}")
                        TokenStorageProvider.instance.clearTokens()
                        null
                }
            }

            sendWithoutRequest { request ->
                // Mandamos el token en todas las requests excepto login y register
                !request.url.pathSegments.any { it == "login" || it == "register" || it == "renovar" }
            }
        }
    }
}