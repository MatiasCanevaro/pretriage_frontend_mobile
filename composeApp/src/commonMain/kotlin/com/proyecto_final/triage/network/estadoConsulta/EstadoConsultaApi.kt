package com.proyecto_final.triage.network.estadoConsulta

import com.proyecto_final.triage.config.AppConfig
import com.proyecto_final.triage.network.httpClient
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
            Result.failure(Exception("Error ${response.status.value}: $errorBody"))
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
            Result.failure(Exception("Error ${response.status.value}: $errorBody"))
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
            Result.failure(Exception("Error ${response.status.value}: $errorBody"))
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
            Result.failure(Exception("Error ${response.status.value}: $errorBody"))
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
            Result.failure(Exception("Error ${response.status.value}: $errorBody"))
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
            Result.failure(Exception("Error ${response.status.value}: $errorBody"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}