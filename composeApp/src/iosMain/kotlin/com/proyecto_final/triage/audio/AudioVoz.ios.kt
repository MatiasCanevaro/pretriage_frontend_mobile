package com.proyecto_final.triage.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

// Chat de voz todavía no implementado en iOS (requiere AVAudioEngine): la UI oculta el botón.
actual fun crearAudioVoz(): AudioVoz = AudioVozNoDisponible

private object AudioVozNoDisponible : AudioVoz {
    override val disponible = false
    override fun iniciarGrabacion(onFragmento: (ByteArray) -> Unit): Boolean = false
    override fun detenerGrabacion() = Unit
    override fun reproducir(pcm: ByteArray) = Unit
    override fun interrumpirReproduccion() = Unit
    override fun liberar() = Unit
}

@Composable
actual fun rememberSolicitudPermisoMicrofono(onResultado: (Boolean) -> Unit): () -> Unit {
    val callback by rememberUpdatedState(onResultado)
    return remember { { callback(false) } }
}
