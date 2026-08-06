package com.proyecto_final.triage.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto_final.triage.network.AtencionEstimada
import com.proyecto_final.triage.network.ChatMensaje
import com.proyecto_final.triage.network.ChatStorage
import com.proyecto_final.triage.network.enviarMensaje
import com.proyecto_final.triage.network.iniciarChat
import com.proyecto_final.triage.network.obtenerChat
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
    val envioExitoso: Boolean? = null
)

class ChatViewModel : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state

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