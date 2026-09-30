package com.proyecto_final.triage.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.core.content.ContextCompat
import com.proyecto_final.triage.appContext
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

private const val FRECUENCIA_GRABACION = 16_000
private const val FRECUENCIA_REPRODUCCION = 24_000
// 100 ms de PCM 16 bits mono a 16 kHz
private const val BYTES_POR_FRAGMENTO = 3_200

actual fun crearAudioVoz(): AudioVoz = AudioVozAndroid()

private fun tienePermisoMicrofono(): Boolean =
    ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED

@Composable
actual fun rememberSolicitudPermisoMicrofono(onResultado: (Boolean) -> Unit): () -> Unit {
    val callback by rememberUpdatedState(onResultado)
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido -> callback(concedido) }
    return remember(launcher) {
        {
            if (tienePermisoMicrofono()) callback(true)
            else launcher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}

private class AudioVozAndroid : AudioVoz {
    override val disponible = true

    @Volatile private var grabando = false
    private var grabador: AudioRecord? = null
    private var hiloGrabacion: Thread? = null
    private var cancelacionEco: AcousticEchoCanceler? = null

    @Volatile private var liberado = false
    private var reproductor: AudioTrack? = null
    private var hiloReproduccion: Thread? = null
    private val pendientes = LinkedBlockingQueue<ByteArray>()

    @SuppressLint("MissingPermission")
    @Synchronized
    override fun iniciarGrabacion(onFragmento: (ByteArray) -> Unit): Boolean {
        if (liberado || !tienePermisoMicrofono()) return false
        if (grabando) return true

        val minimo = AudioRecord.getMinBufferSize(
            FRECUENCIA_GRABACION, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        if (minimo <= 0) return false

        // VOICE_COMMUNICATION aplica el procesamiento de voz del dispositivo (eco / ruido).
        val nuevoGrabador = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                FRECUENCIA_GRABACION,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minimo, BYTES_POR_FRAGMENTO * 2)
            )
        } catch (e: Exception) {
            println("AUDIO VOZ: no se pudo crear AudioRecord: ${e.message}")
            return false
        }
        if (nuevoGrabador.state != AudioRecord.STATE_INITIALIZED) {
            nuevoGrabador.release()
            return false
        }
        if (AcousticEchoCanceler.isAvailable()) {
            cancelacionEco = AcousticEchoCanceler.create(nuevoGrabador.audioSessionId)
                ?.apply { setEnabled(true) }
        }

        try {
            nuevoGrabador.startRecording()
        } catch (e: IllegalStateException) {
            cancelacionEco?.release()
            cancelacionEco = null
            nuevoGrabador.release()
            return false
        }
        grabador = nuevoGrabador
        grabando = true
        hiloGrabacion = thread(name = "chat-voz-grabacion") {
            val buffer = ByteArray(BYTES_POR_FRAGMENTO)
            while (grabando) {
                val leidos = nuevoGrabador.read(buffer, 0, buffer.size)
                if (leidos > 0) {
                    onFragmento(buffer.copyOf(leidos))
                } else if (leidos < 0) {
                    break
                }
            }
        }
        return true
    }

    @Synchronized
    override fun detenerGrabacion() {
        grabando = false
        val actual = grabador ?: return
        grabador = null
        runCatching { actual.stop() }
        hiloGrabacion?.join(500)
        hiloGrabacion = null
        cancelacionEco?.release()
        cancelacionEco = null
        actual.release()
    }

    override fun reproducir(pcm: ByteArray) {
        if (liberado || pcm.isEmpty()) return
        asegurarReproductor() ?: return
        pendientes.offer(pcm)
    }

    @Synchronized
    private fun asegurarReproductor(): AudioTrack? {
        if (liberado) return null
        reproductor?.let { return it }

        val minimo = AudioTrack.getMinBufferSize(
            FRECUENCIA_REPRODUCCION, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val track = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(FRECUENCIA_REPRODUCCION)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minimo, FRECUENCIA_REPRODUCCION / 2))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (e: Exception) {
            println("AUDIO VOZ: no se pudo crear AudioTrack: ${e.message}")
            return null
        }
        track.play()
        reproductor = track
        // write() bloquea: se hace en un hilo propio para no frenar la lectura del WebSocket.
        hiloReproduccion = thread(name = "chat-voz-reproduccion") {
            try {
                while (!liberado) {
                    val pcm = pendientes.take()
                    if (liberado) break
                    track.write(pcm, 0, pcm.size)
                }
            } catch (e: InterruptedException) {
            } catch (e: IllegalStateException) {
            }
        }
        return track
    }

    @Synchronized
    override fun interrumpirReproduccion() {
        pendientes.clear()
        reproductor?.let { track ->
            runCatching {
                track.pause()
                track.flush()
                track.play()
            }
        }
    }

    @Synchronized
    override fun liberar() {
        if (liberado) return
        detenerGrabacion()
        liberado = true
        pendientes.clear()
        hiloReproduccion?.interrupt()
        hiloReproduccion = null
        reproductor?.let { track ->
            runCatching { track.stop() }
            track.release()
        }
        reproductor = null
    }
}
