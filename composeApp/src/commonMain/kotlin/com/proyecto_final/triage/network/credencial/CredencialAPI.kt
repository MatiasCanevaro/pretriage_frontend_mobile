package com.proyecto_final.triage.network.credencial

import com.proyecto_final.triage.config.AppConfig
import com.proyecto_final.triage.network.httpClient
import com.proyecto_final.triage.utils.parsearMensajeError
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode

suspend fun obtenerObrasSociales(): Result<List<ObraSocialResponse>> {
    return try {

        val response = httpClient.get("${AppConfig.baseUrl}/api/obrasocial")

        if (response.status == HttpStatusCode.OK) {
            val obrasSociales = response.body<List<ObraSocialResponse>>()
            if (obrasSociales.isEmpty()) {
                Result.failure(Exception("No hay obras sociales disponibles."))
            } else {
                Result.success(obrasSociales)
            }
        } else {
            val errorBody = response.bodyAsText()
            Result.failure(Exception(parsearMensajeError(errorBody)))
        }

    } catch (e: Exception) {
        Result.failure(e)
    }
}