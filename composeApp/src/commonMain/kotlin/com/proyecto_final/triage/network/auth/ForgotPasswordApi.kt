package com.proyecto_final.triage.network.auth

import com.proyecto_final.triage.config.AppConfig
import com.proyecto_final.triage.network.httpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.encodeURLParameter
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

@Serializable
data class SolicitarTokenRequest(
    val email: String
)

@Serializable
data class SolicitarTokenResponse(
    val message: String? = null,
    val error: String? = null
)

@Serializable
data class ValidarTokenResponse(
    val valido: Boolean? = null,
    val message: String? = null,
    val error: String? = null
)

@Serializable
data class CambiarContraseniaRequest(
    val token: String,
    val nuevaContrasenia: String
)

@Serializable
data class CambiarContraseniaResponse(
    val message: String? = null,
    val error: String? = null
)

suspend fun solicitarToken(email: String): Result<String> {
    return try {
        println("SOLICITAR TOKEN REQUEST: $email")
        val response = httpClient.post("${AppConfig.baseUrl}/api/auth/cambio-contrasenia/solicitar-token") {
            contentType(ContentType.Application.Json)
            setBody(SolicitarTokenRequest(email))
        }
        println("SOLICITAR TOKEN STATUS: ${response.status}")
        val bodyText = response.bodyAsText()
        println("SOLICITAR TOKEN BODY: $bodyText")

        if (response.status == HttpStatusCode.OK || response.status.value in 200..299) {
            val body = runCatching { json.decodeFromString<SolicitarTokenResponse>(bodyText) }.getOrNull()
            return Result.success(body?.message ?: "Token enviado correctamente")
        }

        val errorBody = runCatching { json.decodeFromString<SolicitarTokenResponse>(bodyText) }.getOrNull()
        val msg = errorBody?.error ?: errorBody?.message ?: "No se pudo enviar el token"
        Result.failure(Exception(msg))
    } catch (e: Exception) {
        println("SOLICITAR TOKEN EXCEPTION: ${e.message}")
        e.printStackTrace()
        Result.failure(e)
    }
}

suspend fun validarToken(token: String): Result<String> {
    return try {
        val encoded = token.encodeURLParameter()
        println("VALIDAR TOKEN REQUEST: $token")
        val response = httpClient.get("${AppConfig.baseUrl}/api/auth/cambio-contrasenia/validar?token=$encoded")
        println("VALIDAR TOKEN STATUS: ${response.status}")
        val bodyText = response.bodyAsText()
        println("VALIDAR TOKEN BODY: $bodyText")

        val body = runCatching { json.decodeFromString<ValidarTokenResponse>(bodyText) }.getOrNull()

        if (response.status == HttpStatusCode.OK || response.status.value in 200..299) {
            if (body?.valido == true) {
                return Result.success(body.message ?: "Token válido")
            }
            // 200 pero valido false -> tratar como error
            if (body?.valido == false) {
                return Result.failure(Exception(body.error ?: body.message ?: "Token inválido o expirado"))
            }
            // sin campo valido pero status 2xx -> asumir válido
            return Result.success(body?.message ?: "Token válido")
        }

        val msg = body?.error ?: body?.message ?: "Token inválido o expirado"
        Result.failure(Exception(msg))
    } catch (e: Exception) {
        println("VALIDAR TOKEN EXCEPTION: ${e.message}")
        e.printStackTrace()
        Result.failure(e)
    }
}

suspend fun cambiarContrasenia(token: String, nuevaContrasenia: String): Result<String> {
    return try {
        println("CAMBIAR CONTRASENIA REQUEST token=$token")
        val response = httpClient.post("${AppConfig.baseUrl}/api/auth/cambio-contrasenia") {
            contentType(ContentType.Application.Json)
            setBody(CambiarContraseniaRequest(token, nuevaContrasenia))
        }
        println("CAMBIAR CONTRASENIA STATUS: ${response.status}")
        val bodyText = response.bodyAsText()
        println("CAMBIAR CONTRASENIA BODY: $bodyText")

        if (response.status == HttpStatusCode.OK || response.status.value in 200..299) {
            val body = runCatching { json.decodeFromString<CambiarContraseniaResponse>(bodyText) }.getOrNull()
            return Result.success(body?.message ?: "Contraseña cambiada con éxito")
        }

        val errorBody = runCatching { json.decodeFromString<CambiarContraseniaResponse>(bodyText) }.getOrNull()
        val msg = errorBody?.error ?: errorBody?.message ?: "No se pudo cambiar la contraseña"
        Result.failure(Exception(msg))
    } catch (e: Exception) {
        println("CAMBIAR CONTRASENIA EXCEPTION: ${e.message}")
        e.printStackTrace()
        Result.failure(e)
    }
}
