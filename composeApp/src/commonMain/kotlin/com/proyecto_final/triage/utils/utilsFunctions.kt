package com.proyecto_final.triage.utils

import com.proyecto_final.triage.AppConstants
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlin.time.Clock

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

/**
 * Extrae el mensaje de error del json [bodyText] y devuelve el campo "mensaje"
 *
 * @param bodyText body json de la respuesta en formato texto con atributo "mensaje".
 * @return un String con el mensaje de error que devolvió la request
 */
fun parsearMensajeError(bodyText: String): String {
    if (bodyText.isBlank()) return AppConstants.ERROR_GENERICO

    val jsonResponse = runCatching { json.parseToJsonElement(bodyText).jsonObject }.getOrNull()
        ?: return AppConstants.ERROR_GENERICO

    val mensajeError = when(val mensajeJson = jsonResponse["error"]){
        is JsonPrimitive -> mensajeJson.content
        else -> AppConstants.ERROR_GENERICO
    }

    return mensajeError.ifEmpty { AppConstants.ERROR_GENERICO }
}

/**
 * Devuelve los minutos restantes despecto a la fechaHora [fechaHoraAtencionEstimada]
 *
 * @param fechaHoraAtencionEstimada string de fechaHora con formato `AAAA-MM-DDTHH:MM:SS.MS`.
 * @return minutos restantes hasta alcanzar la [fechaHoraAtencionEstimada]
 */
fun calcularMinutosRestantes(

    fechaHoraAtencionEstimada:
    String?
): Int {

    if (
        fechaHoraAtencionEstimada
            .isNullOrBlank()
    ) {

        return 0
    }


    return try {

        val fechaEstimada =
            LocalDateTime.parse(
                fechaHoraAtencionEstimada
            )


        val fechaEstimadaInstant =
            fechaEstimada.toInstant(
                TimeZone.currentSystemDefault()
            )

        val ahora = Clock.System.now()

        val minutos = (fechaEstimadaInstant - ahora).inWholeMinutes

        minutos
            .coerceAtLeast(0)
            .toInt()

    } catch (
        e: Exception
    ) {
        0
    }
}

/**
 * Devuelve los segundos restantes despecto a la fechaHora [iso]
 *
 * @param iso string de fechaHora con formato `AAAA-MM-DDTHH:MM:SS.MS`.
 * @return segundos restantes hasta alcanzar la [iso]
 */
fun segundosRestantes(
    iso: String?
): Int {
    if (iso.isNullOrBlank()) return 0

    try{

        val fechaHoraInstant = LocalDateTime.parse(iso)
            .toInstant(TimeZone.currentSystemDefault())
        val ahoraInstant = Clock.System.now()

        val segundos = (fechaHoraInstant - ahoraInstant).inWholeSeconds

        return segundos
            .coerceAtLeast(0)
            .toInt()
    }catch (e: Exception){
        return 0
    }
}

/**
 * Extrae los minutos y segundos de la fechaHora [iso]
 *
 * @param iso string de fechaHora con formato `AAAA-MM-DDTHH:MM:SS.MS`.
 * @return string de hora y minutos extraidos de [iso]
 */
fun formatHora(
    iso: String?
): String {

    if (
        iso.isNullOrBlank()
    ) {

        return "--:--"
    }


    return try {

        val timePart =
            iso
                .substringAfter("T")
                .substringBefore(".")


        timePart.substring(
            0,
            5
        )

    } catch (
        e: Exception
    ) {

        "--:--"
    }
}