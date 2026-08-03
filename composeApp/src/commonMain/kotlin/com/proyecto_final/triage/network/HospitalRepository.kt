package com.proyecto_final.triage.network

import com.proyecto_final.triage.config.AppConfig
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

suspend fun obtenerHospitalesCercanos(
    latitud: Double,
    longitud: Double,
    codigoEspecialidad: String
): Result<List<HospitalResponse>> {
    return try {

        val response = httpClient.get("${AppConfig.baseUrl}/api/hospitales/cercanos") {
            parameter("latitud", latitud)
            parameter("longitud", longitud)
            parameter("codigoEspecialidad", codigoEspecialidad)
        }
        println("HOSPITALES CERCANOS STATUS: ${response.status}")

        val body = response.bodyAsText()
        println("HOSPITALES CERCANOS BODY: $body")

        if (response.status == HttpStatusCode.OK) {
            val hospitales = json.decodeFromString<List<HospitalResponse>>(body)
            Result.success(hospitales)
        } else {
            Result.failure(Exception("No se pudieron obtener los hospitales cercanos"))
        }

    } catch (e: Exception) {
        println("HOSPITALES CERCANOS EXCEPTION: ${e.message}")
        Result.failure(e)
    }
}

suspend fun obtenerTiempoArriboHospital(
    idHospital: Long,
    transporte: String,
    latitud: Double,
    longitud: Double
): Result<List<TiempoEstimadoArriboHospitalResponse>> {
    return try {
        val response = httpClient.get("${AppConfig.baseUrl}/api/hospitales/$idHospital/tiempo-arribo") {
            parameter("transporte", transporte)
            parameter("latitud", latitud)
            parameter("longitud", longitud)
        }
        val body = response.bodyAsText()

        if (response.status == HttpStatusCode.OK) {
            Result.success(json.decodeFromString<List<TiempoEstimadoArriboHospitalResponse>>(body))
        } else {
            Result.failure(Exception("No se pudo obtener el tiempo de arribo al hospital"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}