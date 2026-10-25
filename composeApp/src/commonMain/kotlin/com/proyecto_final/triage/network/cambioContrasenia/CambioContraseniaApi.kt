package com.proyecto_final.triage.network.cambioContrasenia

import com.proyecto_final.triage.config.AppConfig
import com.proyecto_final.triage.network.httpClient
import com.proyecto_final.triage.utils.parsearMensajeError
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess

/**
 * Error devuelto por el backend al cambiar la contraseña.
 * bloqueado = true cuando responde 423 (superó los intentos fallidos).
 */
class CambioContraseniaException(
    message: String,
    val bloqueado: Boolean = false
) : Exception(message)

suspend fun cambiarContrasenia(
    contraseniaActual: String,
    contraseniaNueva: String
): Result<Unit> {
    return try {

        val response = httpClient.post("${AppConfig.baseUrl}/api/auth/cambio-contrasenia/actual") {
            contentType(ContentType.Application.Json)
            setBody(
                CambiarContraseniaAutenticadaRequest(
                    contraseniaActual = contraseniaActual,
                    contraseniaNueva = contraseniaNueva
                )
            )
        }

        when {
            response.status.isSuccess() -> Result.success(Unit)

            // 423: bloqueado por intentos fallidos
            response.status == HttpStatusCode.Locked -> Result.failure(
                CambioContraseniaException(
                    message = parsearMensajeError(response.bodyAsText()),
                    bloqueado = true
                )
            )

            // 400: contraseña actual incorrecta, igual a la actual, etc.
            else -> Result.failure(
                CambioContraseniaException(parsearMensajeError(response.bodyAsText()))
            )
        }

    } catch (e: Exception) {
        Result.failure(e)
    }
}