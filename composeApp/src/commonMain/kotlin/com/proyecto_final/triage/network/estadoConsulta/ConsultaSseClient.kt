package com.proyecto_final.triage.network.estadoConsulta

import com.proyecto_final.triage.config.AppConfig
import com.proyecto_final.triage.network.httpClient
import io.ktor.client.plugins.sse.sse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json

sealed class ConsultaSseEvento {
    data class TiempoEstimado(val data: TiempoEstimadoAtencionResponse) : ConsultaSseEvento()
    data object Heartbeat : ConsultaSseEvento()
}

/**
 * Solo tiene sentido suscribirse mientras estadoEntradaCola == "EN_COLA".
 * El backend deja de mandar "tiempo-estimado" (y solo manda heartbeat)
 * apenas la entrada sale de EN_COLA, sin avisar del cambio de estado.
 * Para enterarte de LLAMADO / CANCELADA / etc seguís necesitando
 * obtenerEstadoConsulta() por polling o al volver a la pantalla.
 */
object ConsultaSseClient {

    private val json = Json { ignoreUnknownKeys = true }
    private const val RECONNECT_DELAY_MS = 3000L

    fun suscribirse(consultaId: Long): Flow<ConsultaSseEvento> = callbackFlow {
        var activo = true

        while (activo) {
            try {
                httpClient.sse(
                    urlString = "${AppConfig.baseUrl}/suscribirse/$consultaId"
                ) {
                    incoming.collect { event ->
                        val data = event.data ?: return@collect
                        when (event.event) {
                            "tiempo-estimado" -> runCatching {
                                json.decodeFromString<TiempoEstimadoAtencionResponse>(data)
                            }.onSuccess { trySend(ConsultaSseEvento.TiempoEstimado(it)) }

                            "heartbeat" -> trySend(ConsultaSseEvento.Heartbeat)

                            else -> Unit // evento desconocido, se ignora
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                delay(RECONNECT_DELAY_MS)
            }
        }

        awaitClose { activo = false }
    }
}