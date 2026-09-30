package com.proyecto_final.triage.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto_final.triage.audio.crearAudioVoz
import com.proyecto_final.triage.network.AUTOR_BOT
import com.proyecto_final.triage.network.AUTOR_PACIENTE
import com.proyecto_final.triage.network.AtencionEstimada
import com.proyecto_final.triage.network.ChatMensaje
import com.proyecto_final.triage.network.EventoVoz
import com.proyecto_final.triage.network.MensajeVozCliente
import com.proyecto_final.triage.network.conectarChatVoz
import com.proyecto_final.triage.storage.ChatStorage
import com.proyecto_final.triage.network.enviarMensaje
import com.proyecto_final.triage.network.iniciarChat
import com.proyecto_final.triage.network.obtenerChat
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class ChatUiState(
    val chatId: Long? = null,
    val iniciando: Boolean = false,
    val enviando: Boolean = false,
    val mensajes: List<ChatMensaje> = emptyList(),
    val mensajesNoEnviados: Set<String> = emptySet(),
    val finalizado: Boolean = false,
    val borrador: String = "",
    val atencionEstimada: AtencionEstimada? = null,
    val error: String? = null,
    val envioExitoso: Boolean? = null,
    // Chat de voz
    val vozDisponible: Boolean = false,
    val estadoVoz: EstadoVoz = EstadoVoz.INACTIVA,
    // Transcripción en curso (todavía no confirmada como burbuja)
    val transcripcionPaciente: String = "",
    val transcripcionBot: String = ""
)

enum class EstadoVoz {
    INACTIVA,
    CONECTANDO,
    // Gemini listo: se envía el audio del micrófono
    ESCUCHANDO,
    // Entrevista cerrada por Gemini: se espera la preclasificación
    PROCESANDO
}

class ChatViewModel : ViewModel() {

    private val audio = crearAudioVoz()

    private val _state = MutableStateFlow(ChatUiState(vozDisponible = audio.disponible))
    val state: StateFlow<ChatUiState> = _state

    private var sesionVoz: Job? = null
    private var salidaVoz: Channel<MensajeVozCliente>? = null

    fun iniciar() {
        if (_state.value.iniciando || _state.value.chatId != null) return

        val chatIdGuardado = ChatStorage.getChatId()

        _state.value = _state.value.copy(
            iniciando = true,
            error = null
        )

        viewModelScope.launch {
            if (chatIdGuardado != null) {
                recuperarChat(chatIdGuardado)
            } else {
                crearChat()
            }
        }
    }

    private suspend fun recuperarChat(id: Long) {
        println("CHAT RECUPERAR (id=$id)")
        obtenerChat(id)
            .onSuccess { chat ->
                println("CHAT RECUPERADO: id=${chat.id} finalizado=${chat.finalizado}")
                _state.value = _state.value.copy(
                    chatId = chat.id ?: id,
                    mensajes = chat.mensajes,
                    finalizado = chat.finalizado,
                    iniciando = false
                )
            }
            .onFailure { error ->
                println("CHAT ERROR AL RECUPERAR: ${error.message}")
                // El chat guardado ya no existe: lo descarto y creo uno nuevo.
                ChatStorage.clearChatId()
                if (_state.value.mensajes.isEmpty()) {
                    crearChat()
                } else {
                    _state.value = _state.value.copy(
                        iniciando = false,
                        error = error.message ?: "No se pudo recuperar el chat"
                    )
                }
            }
    }

    private suspend fun crearChat() {
        iniciarChat()
            .onSuccess { chat ->
                println("CHAT INICIADO: id=${chat.id} finalizado=${chat.finalizado}")
                chat.id?.let { ChatStorage.saveChatId(it) }
                _state.value = _state.value.copy(
                    chatId = chat.id,
                    mensajes = chat.mensajes,
                    finalizado = chat.finalizado,
                    iniciando = false
                )
            }
            .onFailure { error ->
                println("CHAT ERROR AL INICIAR: ${error.message}")
                _state.value = _state.value.copy(
                    iniciando = false,
                    error = error.message ?: "No se pudo iniciar el chat"
                )
            }
    }

    fun actualizarBorrador(texto: String) {
        _state.value = _state.value.copy(
            borrador = texto,
            envioExitoso = null
        )
    }

    fun enviarBorrador() {
        val chatId = _state.value.chatId
        val contenido = _state.value.borrador
        if (chatId == null || contenido.isBlank() || _state.value.enviando) return
        if (_state.value.estadoVoz != EstadoVoz.INACTIVA) return

        // El mensaje del paciente se muestra al instante (actualización optimista),
        // con la hora local para que la burbuja la muestre mientras llega la respuesta.
        // Si es un reintento de un mensaje fallido, se reutiliza la burbuja existente
        // y solo se actualiza su hora.
        val esReintento = _state.value.mensajesNoEnviados.contains(contenido)
        val hora = fechaHoraLocal()
        val mensajesActualizados = if (esReintento) {
            _state.value.mensajes.mapIndexed { index, mensaje ->
                if (index == _state.value.mensajes.lastIndex && mensaje.contenido == contenido) {
                    mensaje.copy(fechaHoraEnvio = hora)
                } else {
                    mensaje
                }
            }
        } else {
            _state.value.mensajes +
                ChatMensaje(contenido = contenido, autor = "PACIENTE", fechaHoraEnvio = hora)
        }

        _state.value = _state.value.copy(
            enviando = true,
            error = null,
            envioExitoso = null,
            borrador = "",
            mensajesNoEnviados = _state.value.mensajesNoEnviados - contenido,
            mensajes = mensajesActualizados
        )

        viewModelScope.launch {
            enviarMensaje(chatId, contenido)
                .onSuccess { respuesta ->
                    println("CHAT RESPUESTA: ${respuesta.respuesta}")
                    _state.value = _state.value.copy(
                        enviando = false,
                        envioExitoso = true,
                        error = null,
                        mensajes = _state.value.mensajes + respuesta.respuesta,
                        atencionEstimada = respuesta.atencionEstimada ?: _state.value.atencionEstimada
                    )
                }
                .onFailure { error ->
                    println("CHAT ERROR AL ENVIAR: ${error.message}")
                    _state.value = _state.value.copy(
                        enviando = false,
                        envioExitoso = false,
                        error = "No se pudo enviar el mensaje. Verificá tu conexión e intentá de nuevo.",
                        mensajesNoEnviados = _state.value.mensajesNoEnviados + contenido,
                        borrador = contenido
                    )
                }
        }
    }

    fun limpiarError() {
        _state.value = _state.value.copy(error = null)
    }

    // ---------------------------------------------------------------------
    // Chat de voz (WebSocket /api/chat/{id}/voz, entrevista con Gemini Live)
    // ---------------------------------------------------------------------

    fun permisoMicrofonoDenegado() {
        _state.value = _state.value.copy(
            error = "Necesitamos permiso para usar el micrófono. Podés seguir por texto."
        )
    }

    fun iniciarVoz() {
        val actual = _state.value
        val chatId = actual.chatId ?: return
        if (!actual.vozDisponible || actual.estadoVoz != EstadoVoz.INACTIVA ||
            actual.finalizado || actual.enviando
        ) return

        _state.value = actual.copy(
            estadoVoz = EstadoVoz.CONECTANDO,
            error = null,
            envioExitoso = null
        )

        // El micrófono corre en su propio hilo: si el socket se atrasa se descarta el audio más viejo.
        val salida = Channel<MensajeVozCliente>(
            capacity = 64,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        salidaVoz = salida

        sesionVoz = viewModelScope.launch {
            var triageRecibido = false
            try {
                // Consulta previa por HTTP: renueva el token si venció (el handshake no reintenta)
                // y evita abrir la voz sobre un chat ya finalizado (el backend respondería 409).
                val chat = obtenerChat(chatId).getOrNull()
                if (chat?.finalizado == true) {
                    _state.value = _state.value.copy(
                        mensajes = chat.mensajes,
                        finalizado = true
                    )
                    return@launch
                }

                val resultado = conectarChatVoz(chatId, salida) { evento ->
                    if (evento is EventoVoz.TriageFinalizado) triageRecibido = true
                    procesarEventoVoz(evento, salida)
                }
                resultado.onFailure {
                    _state.value = _state.value.copy(
                        error = "No se pudo conectar el chat de voz. Podés seguir por texto."
                    )
                }
            } finally {
                audio.detenerGrabacion()
                confirmarTranscripciones()
                salida.close()
                if (salidaVoz === salida) salidaVoz = null
                _state.value = _state.value.copy(estadoVoz = EstadoVoz.INACTIVA)
            }
            sincronizarChatTrasVoz(chatId, triageRecibido)
        }
    }

    /** El paciente corta la conversación de voz. */
    fun detenerVoz() {
        val estado = _state.value.estadoVoz
        // Tras entrevista_finalizada el backend clasifica igual: se espera el resultado.
        if (estado == EstadoVoz.INACTIVA || estado == EstadoVoz.PROCESANDO) return

        audio.detenerGrabacion()
        audio.interrumpirReproduccion()
        salidaVoz?.trySend(MensajeVozCliente.Cerrar)

        // El backend cierra el socket al recibir "cerrar"; si no lo hace, se corta desde acá.
        val sesion = sesionVoz
        viewModelScope.launch {
            delay(5_000)
            if (sesion?.isActive == true) sesion.cancel()
        }
    }

    private fun procesarEventoVoz(evento: EventoVoz, salida: Channel<MensajeVozCliente>) {
        when (evento) {
            is EventoVoz.Audio -> audio.reproducir(evento.pcm)

            EventoVoz.Listo -> {
                val grabando = audio.iniciarGrabacion { pcm ->
                    salida.trySend(MensajeVozCliente.Audio(pcm))
                }
                if (grabando) {
                    _state.value = _state.value.copy(estadoVoz = EstadoVoz.ESCUCHANDO)
                } else {
                    _state.value = _state.value.copy(
                        error = "No se pudo acceder al micrófono."
                    )
                    salida.trySend(MensajeVozCliente.Cerrar)
                }
            }

            is EventoVoz.TranscripcionPaciente -> {
                // Si el paciente empieza a hablar, el turno del asistente terminó.
                if (_state.value.transcripcionBot.isNotBlank()) confirmarTranscripcionBot()
                _state.value = _state.value.copy(
                    transcripcionPaciente = _state.value.transcripcionPaciente + evento.texto
                )
            }

            is EventoVoz.TranscripcionBot -> {
                if (_state.value.transcripcionPaciente.isNotBlank()) confirmarTranscripcionPaciente()
                _state.value = _state.value.copy(
                    transcripcionBot = _state.value.transcripcionBot + evento.texto
                )
            }

            EventoVoz.Interrumpido -> {
                audio.interrumpirReproduccion()
                confirmarTranscripcionBot()
            }

            EventoVoz.TurnoCompleto -> confirmarTranscripciones()

            EventoVoz.EntrevistaFinalizada -> {
                // El audio de despedida que ya llegó se sigue reproduciendo.
                audio.detenerGrabacion()
                confirmarTranscripciones()
                _state.value = _state.value.copy(estadoVoz = EstadoVoz.PROCESANDO)
            }

            is EventoVoz.TriageFinalizado -> {
                confirmarTranscripciones()
                _state.value = _state.value.copy(
                    mensajes = _state.value.mensajes + listOfNotNull(evento.respuesta),
                    atencionEstimada = evento.atencionEstimada ?: _state.value.atencionEstimada,
                    finalizado = true
                )
            }

            EventoVoz.SesionPorExpirar -> _state.value = _state.value.copy(
                error = "La sesión de voz está por terminar."
            )

            is EventoVoz.Error -> _state.value = _state.value.copy(error = evento.mensaje)

            // El backend cierra el socket después de "fin"; el cierre termina conectarChatVoz.
            EventoVoz.Fin -> Unit
        }
    }

    private fun confirmarTranscripciones() {
        confirmarTranscripcionPaciente()
        confirmarTranscripcionBot()
    }

    private fun confirmarTranscripcionPaciente() {
        val texto = _state.value.transcripcionPaciente.trim()
        _state.value = _state.value.copy(
            transcripcionPaciente = "",
            mensajes = if (texto.isEmpty()) _state.value.mensajes else
                _state.value.mensajes + ChatMensaje(texto, AUTOR_PACIENTE, fechaHoraLocal())
        )
    }

    private fun confirmarTranscripcionBot() {
        val texto = _state.value.transcripcionBot.trim()
        _state.value = _state.value.copy(
            transcripcionBot = "",
            mensajes = if (texto.isEmpty()) _state.value.mensajes else
                _state.value.mensajes + ChatMensaje(texto, AUTOR_BOT, fechaHoraLocal())
        )
    }

    // Las burbujas de voz son transcripciones locales: al terminar se reemplazan por lo que
    // persistió el backend. Sin triage el guardado es asíncrono, por eso se espera un poco y
    // solo se reemplaza si el backend no tiene menos mensajes que los que se muestran.
    private suspend fun sincronizarChatTrasVoz(chatId: Long, triageRecibido: Boolean) {
        if (!triageRecibido) delay(1_500)
        obtenerChat(chatId).onSuccess { chat ->
            val local = _state.value
            if (chat.finalizado || chat.mensajes.size >= local.mensajes.size) {
                _state.value = local.copy(
                    mensajes = chat.mensajes,
                    finalizado = chat.finalizado || local.finalizado
                )
            }
        }
    }

    /** Corta la voz y libera el audio. Llamar al salir de la pantalla. */
    fun liberarRecursos() {
        sesionVoz?.cancel()
        sesionVoz = null
        audio.liberar()
    }

    override fun onCleared() {
        liberarRecursos()
        super.onCleared()
    }

    // Hora local con el mismo formato que envía el backend ("2026-08-06T14:30:00")
    // para que formatearFechaHora la renderice sin cambios.
    @OptIn(ExperimentalTime::class)
    private fun fechaHoraLocal(): String {
        val ahora = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val hora = ahora.hour.toString().padStart(2, '0')
        val minuto = ahora.minute.toString().padStart(2, '0')
        return "${ahora.date}T$hora:$minuto:00"
    }
}