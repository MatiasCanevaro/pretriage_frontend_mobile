package com.proyecto_final.triage.network

import com.proyecto_final.triage.config.AppConfig
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode

suspend fun obtenerEstadoConsulta(): Result<EstadoConsultaPacienteDTO> {
    return try {

        val response = httpClient.get(
            "${AppConfig.baseUrl}/api/paciente/consulta/estado"
        )

        if (response.status == HttpStatusCode.OK) {

            Result.success(
                response.body<EstadoConsultaPacienteDTO>()
            )

        } else {

            val errorBody = response.bodyAsText()

            Result.failure(
                Exception(
                    "Error ${response.status.value}: $errorBody"
                )
            )
        }

    } catch (e: Exception) {
        Result.failure(e)
    }
}