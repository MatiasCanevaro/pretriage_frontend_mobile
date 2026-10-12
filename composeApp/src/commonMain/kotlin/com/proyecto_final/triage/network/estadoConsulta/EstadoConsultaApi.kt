package com.proyecto_final.triage.network.estadoConsulta

import com.proyecto_final.triage.config.AppConfig
import com.proyecto_final.triage.network.httpClient
import com.proyecto_final.triage.storage.ChatStorage
import com.proyecto_final.triage.utils.parsearMensajeError
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.client.request.post

suspend fun obtenerEstadoConsulta(): Result<EstadoConsultaPacienteDTO> {
    return try {

        val response = httpClient.get("${AppConfig.baseUrl}/api/paciente/consulta/estado")

        if (response.status == HttpStatusCode.OK) {
            Result.success(response.body<EstadoConsultaPacienteDTO>())
        } else {
            val errorBody = response.bodyAsText()
            Result.failure(Exception(parsearMensajeError(errorBody)))
        }

    } catch (e: Exception) {
        Result.failure(e)
    }
}

suspend fun obtenerHospitalSeleccionado(): Result<HospitalSeleccionadoResponse> {
    return try {

        val response = httpClient.get("${AppConfig.baseUrl}/api/atencion/hospital")

        if (response.status == HttpStatusCode.OK) {
            Result.success(response.body<HospitalSeleccionadoResponse>())
        } else {
            val errorBody = response.bodyAsText()
            Result.failure(Exception(parsearMensajeError(errorBody)))
        }

    } catch (e: Exception) {
        Result.failure(e)
    }
}

suspend fun ausentarme(): Result<EstadoConsultaPacienteDTO> {
    return try {
        val response = httpClient.post("${AppConfig.baseUrl}/api/paciente/consulta/cola/pausa-manual")

        if (response.status == HttpStatusCode.OK) {
            Result.success(response.body<EstadoConsultaPacienteDTO>())
        } else {
            val errorBody = response.bodyAsText()
            Result.failure(Exception(parsearMensajeError(errorBody)))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}

suspend fun estoyAtrasado(): Result<EstadoConsultaPacienteDTO> {
    return try {
        val response = httpClient.post("${AppConfig.baseUrl}/api/paciente/consulta/cola/atraso/confirmar")

        if (response.status == HttpStatusCode.OK) {
            Result.success(response.body<EstadoConsultaPacienteDTO>())
        } else {
            val errorBody = response.bodyAsText()
            Result.failure(Exception(parsearMensajeError(errorBody)))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}

suspend fun sigoAsistiendo(): Result<EstadoConsultaPacienteDTO> {
    return try {
        val response = httpClient.post("${AppConfig.baseUrl}/api/paciente/consulta/cola/atraso/renovar")

        if (response.status == HttpStatusCode.OK) {
            Result.success(response.body<EstadoConsultaPacienteDTO>())
        } else {
            val errorBody = response.bodyAsText()
            Result.failure(Exception(parsearMensajeError(errorBody)))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}

suspend fun llegue(): Result<EstadoConsultaPacienteDTO> {
    return try {
        val response = httpClient.post("${AppConfig.baseUrl}/api/paciente/consulta/cola/reincorporar")

        if (response.status == HttpStatusCode.OK) {
            Result.success(response.body<EstadoConsultaPacienteDTO>())
        } else {
            val errorBody = response.bodyAsText()
            Result.failure(Exception(parsearMensajeError(errorBody)))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}

suspend fun cancelarSeleccionHospital(): Result<EstadoConsultaPacienteDTO> {
    return try {
        val response = httpClient.post("${AppConfig.baseUrl}/api/paciente/consulta/cancelar")

        if (response.status == HttpStatusCode.OK) {
            val estado = response.body<EstadoConsultaPacienteDTO>()
            // El backend conserva el historial y cierra el chat de la consulta cancelada.
            // Solo descartamos su referencia local tras confirmar la cancelacion.
            if (estado.estadoConsulta == "CANCELADA" || estado.estadoEntradaCola == "CANCELADA") {
                ChatStorage.clearChatId()
            }
            Result.success(estado)
        } else {
            val errorBody = response.bodyAsText()
            Result.failure(Exception(parsearMensajeError(errorBody)))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
