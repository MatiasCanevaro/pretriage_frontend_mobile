package com.proyecto_final.triage.utils

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

private val json = Json { ignoreUnknownKeys = true }

/**
* extrae los mensajes de error de cada [campos] del la response [bodyText] y 
* los convierte en un Map
*
* @param bodyText body json de la respuesta en formato texto.
* @param campos conjunto de campos que puede llegar a devovler el body
* @return un Map<campo, mensajeError> o null.
*/
fun parsearErroresDeCampo(bodyText: String, campos: Set<String>): Map<String, String>? {
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