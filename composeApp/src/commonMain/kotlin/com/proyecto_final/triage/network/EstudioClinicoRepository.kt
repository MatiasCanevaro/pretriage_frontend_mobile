package com.proyecto_final.triage.network

import com.proyecto_final.triage.config.AppConfig
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.setBody
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.append
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

suspend fun obtenerEstudios(): Result<List<EstudioClinicoDTO>> {
    return try {
        val response = httpClient.get("${AppConfig.baseUrl}/api/estudios")

        println("STATUS: ${response.status}")
        println("BODY: ${response.bodyAsText()}")

        if (response.status == HttpStatusCode.OK) {
            Result.success(response.body())
        } else {
            Result.failure(Exception("No se pudieron obtener los estudios."))
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Result.failure(e)
    }
}

suspend fun obtenerEstudio(idEstudio: Long): Result<EstudioClinicoDTO> {
    return try {
        val response = httpClient.get("${AppConfig.baseUrl}/api/estudios/$idEstudio")
        val body = response.bodyAsText()
        if (response.status == HttpStatusCode.OK) {
            Result.success(json.decodeFromString<EstudioClinicoDTO>(body))
        } else {
            Result.failure(Exception("No se pudo obtener el estudio"))
        }
    } catch (e: Exception) {
        println("ESTUDIO EXCEPTION ($idEstudio): ${e.message}")
        Result.failure(e)
    }
}

suspend fun subirEstudio(fileBytes: ByteArray,
                         fileName: String,
                         tipoArchivo: String,
                         descripcion: String?
): Result<Unit> {
    return try {
        val response = httpClient.post("${AppConfig.baseUrl}/api/estudios") {
            setBody(MultiPartFormDataContent(formData { append(
                "file",
                fileBytes,
                Headers.build {
                    append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                }
            )
                append("tipoArchivo", tipoArchivo)
                descripcion?.let { append("descripcion", it) }
            }
            )
            )
        }

        val body = response.bodyAsText()

        println("SUBIR ESTUDIO STATUS: ${response.status}")
        println("SUBIR ESTUDIO BODY: $body")

        if (response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Created) {
            Result.success(Unit)
        } else {
            Result.failure(Exception(body))
        }

    } catch (e: Exception) {
        e.printStackTrace()
        Result.failure(e)
    }
}

suspend fun eliminarEstudio(idEstudio: Long): Result<Unit> {
    return try {
        val response = httpClient.delete("${AppConfig.baseUrl}/api/estudios/$idEstudio")
        if (response.status == HttpStatusCode.OK) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("No se pudo eliminar el estudio"))
        }
    } catch (e: Exception) {
        println("ELIMINAR ESTUDIO EXCEPTION ($idEstudio): ${e.message}")
        Result.failure(e)
    }
}

suspend fun descargarEstudio(idEstudio: Long): Result<ByteArray> {
    return try {
        val response = httpClient.get("${AppConfig.baseUrl}/api/estudios/$idEstudio/file")
        if (response.status == HttpStatusCode.OK) {
            Result.success(response.bodyAsBytes())
        } else {
            Result.failure(Exception("No se pudo descargar el archivo"))
        }
    } catch (e: Exception) {
        println("DESCARGAR ESTUDIO EXCEPTION ($idEstudio): ${e.message}")
        Result.failure(e)
    }
}