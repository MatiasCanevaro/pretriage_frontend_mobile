package com.proyecto_final.triage.audio

import androidx.compose.runtime.Composable

/**
 * Captura y reproducción de audio para el chat de voz.
 * Grabación: PCM 16 bits mono little-endian a 16 kHz.
 * Reproducción: PCM 16 bits mono little-endian a 24 kHz.
 */
interface AudioVoz {
    /** false en plataformas sin soporte: la UI oculta el chat de voz. */
    val disponible: Boolean

    /** Empieza a grabar; [onFragmento] se llama desde un hilo de audio. Devuelve false si no pudo. */
    fun iniciarGrabacion(onFragmento: (ByteArray) -> Unit): Boolean
    fun detenerGrabacion()

    /** Encola audio para reproducir sin bloquear al llamador. */
    fun reproducir(pcm: ByteArray)

    /** Corta lo que se está reproduciendo y descarta el audio pendiente. */
    fun interrumpirReproduccion()

    /** Libera los recursos nativos. La instancia no se puede reutilizar. */
    fun liberar()
}

expect fun crearAudioVoz(): AudioVoz

/** Devuelve una acción que pide el permiso de micrófono y llama a [onResultado] con el resultado. */
@Composable
expect fun rememberSolicitudPermisoMicrofono(onResultado: (Boolean) -> Unit): () -> Unit
