package com.proyecto_final.triage.network

import com.proyecto_final.triage.config.AppConfig
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

suspend fun obtenerHospitalesCercanos(
    latitud: Double,
    longitud: Double,
    codigoEspecialidad: String,
    transporte: String = "transporte-publico"
): Result<List<HospitalCercanoDTO>> {
    return try {

        val response = httpClient.get("${AppConfig.baseUrl}/api/hospitales/cercanos") {
            parameter("latitud", latitud)
            parameter("longitud", longitud)
            parameter("codigoEspecialidad", codigoEspecialidad)
            parameter("transporte", transporte)
        }

        if (response.status == HttpStatusCode.OK) {
            val hospitales = response.body<List<HospitalCercanoDTO>>()
            Result.success(hospitales)
        } else {
            val errorBody = response.bodyAsText()
            Result.failure(Exception("Error ${response.status.value}: $errorBody"))
        }

    } catch (e: Exception) {
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

        val response = httpClient.get(
            "${AppConfig.baseUrl}/api/hospitales/$idHospital/tiempo-arribo"
        ) {
            parameter("transporte", transporte)
            parameter("latitud", latitud)
            parameter("longitud", longitud)
        }

        if (response.status == HttpStatusCode.OK) {

            Result.success(
                response.body<List<TiempoEstimadoArriboHospitalResponse>>()
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

suspend fun elegirHospital(
    placeId: String,
    codigoEspecialidad: String
): Result<Unit> {
    return try {

        val response = httpClient.post("${AppConfig.baseUrl}/api/atencion/hospital") {
            contentType(ContentType.Application.Json)
            setBody(SeleccionHospitalRequest(placeId = placeId, codigoEspecialidad = codigoEspecialidad))
        }

        if (response.status == HttpStatusCode.NoContent) {
            Result.success(Unit)
        } else {
            val errorBody = response.bodyAsText()
            Result.failure(Exception("Error ${response.status.value}: $errorBody"))
        }

    } catch (e: Exception) {
        Result.failure(e)
    }
}