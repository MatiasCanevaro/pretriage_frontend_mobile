package com.proyecto_final.triage.network

import com.proyecto_final.triage.config.AppConfig
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.client.plugins.sse.sse
import kotlinx.serialization.json.Json

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

suspend fun ausentarme(): Result<EstadoConsultaPacienteDTO> =
    postAccionConsulta("ausentarme")

suspend fun estoyAtrasado(): Result<EstadoConsultaPacienteDTO> =
    postAccionConsulta("estoy-atrasado")

suspend fun sigoAsistiendo(): Result<EstadoConsultaPacienteDTO> =
    postAccionConsulta("sigo-asistiendo")

suspend fun llegue(): Result<EstadoConsultaPacienteDTO> =
    postAccionConsulta("llegue")

private suspend fun postAccionConsulta(accion: String): Result<EstadoConsultaPacienteDTO> {
    return try {

        val response = httpClient.post(
            "${AppConfig.baseUrl}/api/paciente/consulta/$accion"
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

/**
 * Se suscribe al stream de tiempo estimado de una consulta EN_COLA.
 * Suspende hasta que la conexión se corta (fin de EN_COLA, error de red, o
 * cancelación del coroutine que la llama). El caller es responsable de
 * relanzarla con reintentos si quiere mantenerla viva.
 */
suspend fun suscribirseATiempoEstimado(
    consultaId: Long,
    onEstimacion: (TiempoEstimadoAtencionResponse) -> Unit
) {
    val json = Json { ignoreUnknownKeys = true }

    httpClient.sse(
        urlString = "${AppConfig.baseUrl}/api/atencion/tiempos/suscribirse/$consultaId"
    ) {
        incoming.collect { event ->
            when (event.event) {
                "tiempo-estimado" -> {
                    val data = event.data ?: return@collect
                    runCatching {
                        json.decodeFromString<TiempoEstimadoAtencionResponse>(data)
                    }.onSuccess(onEstimacion)
                }
                // "heartbeat" u otros eventos: no requieren acción, solo mantienen viva la conexión
                else -> Unit
            }
        }
    }
}