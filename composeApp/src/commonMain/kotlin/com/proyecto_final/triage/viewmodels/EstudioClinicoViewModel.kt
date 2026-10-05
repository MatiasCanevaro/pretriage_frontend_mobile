package com.proyecto_final.triage.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto_final.triage.network.EstudioClinicoDTO
import com.proyecto_final.triage.network.descargarEstudio
import com.proyecto_final.triage.network.eliminarEstudio
import com.proyecto_final.triage.network.obtenerEstudios
import com.proyecto_final.triage.network.subirEstudio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class StudiesViewModel : ViewModel() {

    // Lista de estudios (pantalla "Mis estudios")
    private val _state = MutableStateFlow<EstudiosState>(EstudiosState.Loading)
    val state: StateFlow<EstudiosState> = _state.asStateFlow()

    // Subida (pantalla "Agregar estudio")
    private val _subiendo = MutableStateFlow(false)
    val subiendo: StateFlow<Boolean> = _subiendo.asStateFlow()

    private val _errorSubida = MutableStateFlow<String?>(null)
    val errorSubida: StateFlow<String?> = _errorSubida.asStateFlow()

    // Eliminación (pantalla "Detalle del estudio")
    private val _eliminando = MutableStateFlow(false)
    val eliminando: StateFlow<Boolean> = _eliminando.asStateFlow()

    // Mensaje de éxito (snackbar en "Mis estudios")
    private val _mensajeExito = MutableStateFlow<String?>(null)
    val mensajeExito: StateFlow<String?> = _mensajeExito.asStateFlow()

    // Carga de la lista en curso
    private var cargaJob: Job? = null

    fun notificarEstudioAgregado(nombreTipo: String) {
        _mensajeExito.value = "Se agregó \"$nombreTipo\" correctamente"
    }

    fun limpiarMensajeExito() {
        _mensajeExito.value = null
    }

    fun limpiarErrorSubida() {
        _errorSubida.value = null
    }

    /*
     * ================================================================
     * LISTA
     * ================================================================
     */

    fun cargarEstudios() {

        // Si ya hay una carga en curso, no lanzo otra.
        // No la cancelo: cancelar un request de Ktor a mitad de camino
        // crashea con el engine Android (NetworkOnMainThreadException).
        if (cargaJob?.isActive == true)
            return

        _state.value = EstudiosState.Loading

        cargaJob = viewModelScope.launch {
            // La llamada y el timeout corren fuera del hilo principal:
            // si el timeout corta el request, el cierre no pasa por el main thread
            val resultado = withContext(Dispatchers.Default) {
                withTimeoutOrNull(TIMEOUT_MS) { obtenerEstudios() }
            } ?: Result.failure(Exception("Timeout de $TIMEOUT_MS ms al cargar estudios"))

            resultado
                .onSuccess { estudios -> _state.value = EstudiosState.Success(estudios) }
                .onFailure { error ->
                    println("StudiesViewModel - Error al cargar estudios: ${error.stackTraceToString()}")
                    _state.value = EstudiosState.Error(MENSAJE_ERROR_CARGA)
                }
        }
    }

    /*
     * ================================================================
     * SUBIR
     * El error se guarda en errorSubida (estado), porque la pantalla
     * solo necesita saber si falló.
     * ================================================================
     */

    fun subirNuevoEstudio(
        fileBytes: ByteArray,
        fileName: String,
        tipoArchivo: String,
        descripcion: String?,
        onSuccess: () -> Unit = {}
    ) {
        // Evita subir dos veces si tocan el botón rápido
        if (_subiendo.value) return

        viewModelScope.launch {
            _subiendo.value = true
            _errorSubida.value = null

            try {
                subirEstudio(
                    fileBytes = fileBytes,
                    fileName = fileName,
                    tipoArchivo = tipoArchivo,
                    descripcion = descripcion
                )
                    .onSuccess {
                        cargarEstudios()
                        onSuccess()
                    }
                    .onFailure { error ->
                        // No toco _state: la lista de "Mis estudios" queda intacta
                        println("StudiesViewModel - Error al subir estudio: ${error.stackTraceToString()}")
                        _errorSubida.value = MENSAJE_ERROR_SUBIDA
                    }
            } finally {
                _subiendo.value = false
            }
        }
    }

    /*
     * ================================================================
     * ELIMINAR
     * Mismo criterio que la subida: detalle técnico al log y un mensaje
     * claro para el usuario. El error va por onError porque la pantalla
     * de detalle lo muestra en su ErrorMessage junto con los de ver y
     * descargar (con "Reintentar").
     * ================================================================
     */

    fun eliminarEstudioClinico(
        idEstudio: Long,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        // Evita eliminar dos veces si tocan el botón rápido
        if (_eliminando.value) return

        viewModelScope.launch {
            _eliminando.value = true

            try {
                eliminarEstudio(idEstudio)
                    .onSuccess {
                        cargarEstudios()
                        onSuccess()
                    }
                    .onFailure { error ->
                        println("StudiesViewModel - Error al eliminar estudio: ${error.stackTraceToString()}")
                        onError(MENSAJE_ERROR_ELIMINACION)
                    }
            } finally {
                _eliminando.value = false
            }
        }
    }

    /*
     * ================================================================
     * DESCARGAR
     * Devuelve los bytes por onSuccess porque cada pantalla hace algo
     * distinto con ellos (abrirlo o guardarlo en Descargas).
     * ================================================================
     */

    fun descargarEstudioClinico(
        idEstudio: Long,
        onSuccess: (ByteArray) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            descargarEstudio(idEstudio)
                .onSuccess { bytes -> onSuccess(bytes) }
                .onFailure { error ->
                    println("StudiesViewModel - Error al descargar estudio: ${error.stackTraceToString()}")
                    onError(MENSAJE_ERROR_DESCARGA)
                }
        }
    }

    companion object {
        private const val TIMEOUT_MS = 20_000L

        // Mensajes para el usuario (el detalle técnico va al log)
        private const val MENSAJE_ERROR_CARGA = "No se pudieron cargar los estudios. Intentá de nuevo."
        private const val MENSAJE_ERROR_SUBIDA = "No se pudo subir el estudio. Intentá de nuevo."
        private const val MENSAJE_ERROR_ELIMINACION = "No se pudo eliminar el estudio. Intentá de nuevo."
        private const val MENSAJE_ERROR_DESCARGA = "No se pudo descargar el estudio. Intentá de nuevo."
    }
}

sealed class EstudiosState {
    object Loading : EstudiosState()
    data class Success(val estudios: List<EstudioClinicoDTO>) : EstudiosState()
    data class Error(val message: String) : EstudiosState()
}