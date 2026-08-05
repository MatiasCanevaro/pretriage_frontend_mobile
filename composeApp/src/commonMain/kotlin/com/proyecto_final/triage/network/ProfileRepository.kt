package com.proyecto_final.triage.network

import com.proyecto_final.triage.config.AppConfig
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

@Serializable
data class PerfilResponse(
    val nombre: String,
    val apellido: String,
    val tipoDocumento: String,
    val numeroDocumento: String,
    val fechaNacimiento: String? = null,
    val generoBiologico: String? = null,
    val generoConElQueSeIdentifica: String? = null,
    val peso: Double? = null,
    val alturaPersona: Int? = null,
    val email: String? = null,
    val telefono: String? = null,
    val calle: String? = null,
    val alturaDireccion: String? = null,
    val piso: String? = null,
    val codigoPostal: String? = null,
    val ciudad: String? = null,
    val provincia: String? = null
)

@Serializable
data class PerfilUsuarioRequest(
    val nombre: String,
    val apellido: String,
    val tipoDocumento: String,
    val numeroDocumento: String,
    val fechaNacimiento: String,
    val generoBiologico: String,
    val generoConElQueSeIdentifica: String,
    val email: String,
    val telefono: String,
    val calle: String,
    val alturaDireccion: String,
    val piso: String? = null,
    val codigoPostal: String,
    val ciudad: String,
    val provincia: String,
    val peso: Double,
    val alturaPersona: Int
)

sealed class PerfilUpdateResult {
    data class Success(val perfil: PerfilResponse) : PerfilUpdateResult()
    data class FieldErrors(val errors: Map<String, String>) : PerfilUpdateResult()
    data class Error(val message: String) : PerfilUpdateResult()
}

private val json = Json { ignoreUnknownKeys = true }

private val camposDelPerfil = setOf(
    "nombre", "apellido", "tipoDocumento", "numeroDocumento", "fechaNacimiento",
    "generoBiologico", "generoConElQueSeIdentifica", "email", "telefono", "calle",
    "alturaDireccion", "piso", "codigoPostal", "ciudad", "provincia", "peso", "alturaPersona"
)

internal fun parsearErroresDeCampo(bodyText: String, campos: Set<String>): Map<String, String>? {
    if (bodyText.isBlank()) return null

    val errores = mutableMapOf<String, String>()
    val root = runCatching { json.parseToJsonElement(bodyText) as? JsonObject }.getOrNull()
        ?: return null

    when (val errorsElement = root["errors"]) {
        is JsonObject -> errorsElement.forEach { (campo, valor) ->
            (valor as? JsonPrimitive)?.contentOrNull?.let { errores[campo] = it }
        }

        is JsonArray -> errorsElement.forEach { elemento ->
            val obj = elemento as? JsonObject ?: return@forEach
            val campo = (obj["field"] as? JsonPrimitive)?.contentOrNull
            val mensaje = (obj["message"] as? JsonPrimitive)?.contentOrNull
            if (campo != null && mensaje != null) {
                errores[campo] = mensaje
            }
        }

        else -> {}
    }

    root.forEach { (campo, valor) ->
        if (campo in campos) {
            (valor as? JsonPrimitive)?.contentOrNull?.let { errores[campo] = it }
        }
    }

    return errores.ifEmpty { null }
}

suspend fun obtenerPerfil(): Result<PerfilResponse> {
    return try {
        val response = httpClient.get("${AppConfig.baseUrl}/api/perfil") {
            header(HttpHeaders.Authorization, "Bearer ${TokenStorage.getToken()}")
        }
        if (response.status == HttpStatusCode.OK) {
            val bodyText = response.bodyAsText()
            val perfil = json.decodeFromString<PerfilResponse>(bodyText)
            Result.success(perfil)
        } else {
            val bodyText = response.bodyAsText()
            val errorResponse = json.decodeFromString<ErrorResponse>(bodyText)
            Result.failure(Exception(errorResponse.error ?: "No se pudo obtener el perfil"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}

suspend fun actualizarPerfil(request: PerfilUsuarioRequest): PerfilUpdateResult {

    return try {
        val response = httpClient.put("${AppConfig.baseUrl}/api/perfil") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer ${TokenStorage.getToken()}")
            setBody(request)
        }

        val bodyText = response.bodyAsText()

        if (response.status.value in 200..299) {
            val perfil = runCatching { json.decodeFromString<PerfilResponse>(bodyText) }.getOrNull()
            return PerfilUpdateResult.Success(
                perfil ?: PerfilResponse(
                    nombre = request.nombre,
                    apellido = request.apellido,
                    tipoDocumento = request.tipoDocumento,
                    numeroDocumento = request.numeroDocumento
                )
            )
        }

        val erroresDeCampo = parsearErroresDeCampo(bodyText, camposDelPerfil)
        if (erroresDeCampo != null) {
            return PerfilUpdateResult.FieldErrors(erroresDeCampo)
        }

        val errorResponse = runCatching { json.decodeFromString<ErrorResponse>(bodyText) }.getOrNull()
        PerfilUpdateResult.Error(errorResponse?.error ?: "No se pudo actualizar el perfil")
    } catch (e: Exception) {
        PerfilUpdateResult.Error(e.message ?: "Error de conexión")
    }
}
