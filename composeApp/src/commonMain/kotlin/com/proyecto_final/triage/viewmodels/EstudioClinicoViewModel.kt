package com.proyecto_final.triage.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto_final.triage.network.EstudioClinicoDTO
import com.proyecto_final.triage.network.descargarEstudio
import com.proyecto_final.triage.network.eliminarEstudio
import com.proyecto_final.triage.network.obtenerEstudios
import com.proyecto_final.triage.network.subirEstudio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StudiesViewModel : ViewModel() {

    private val _state = MutableStateFlow<EstudiosState>(EstudiosState.Loading)
    val state: StateFlow<EstudiosState> = _state

    private val _subiendo = MutableStateFlow(false)
    val subiendo: StateFlow<Boolean> = _subiendo

    private val _eliminando = MutableStateFlow(false)
    val eliminando: StateFlow<Boolean> = _eliminando

    private val _mensajeExito = MutableStateFlow<String?>(null)
    val mensajeExito: StateFlow<String?> = _mensajeExito.asStateFlow()

    fun notificarEstudioAgregado(nombreTipo: String) {
        _mensajeExito.value = "Se agregó \"$nombreTipo\" correctamente"
    }

    fun limpiarMensajeExito() {
        _mensajeExito.value = null
    }

    fun cargarEstudios() {
        viewModelScope.launch {
            _state.value = EstudiosState.Loading
            obtenerEstudios()
                .onSuccess { estudios -> _state.value = EstudiosState.Success(estudios) }
                .onFailure { error -> _state.value = EstudiosState.Error(error.message ?: "Error desconocido") }
        }
    }

    fun subirNuevoEstudio(
        fileBytes: ByteArray,
        fileName: String,
        tipoArchivo: String,
        descripcion: String?,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _subiendo.value = true
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
                    val mensaje = error.message ?: "Error al subir el estudio."
                    _state.value = EstudiosState.Error(mensaje)
                    onError(mensaje)
                }
            _subiendo.value = false
        }
    }

    fun eliminarEstudioClinico(
        idEstudio: Long,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _eliminando.value = true
            eliminarEstudio(idEstudio)
                .onSuccess {
                    cargarEstudios()
                    onSuccess()
                }
                .onFailure { error ->
                    onError(error.message ?: "Error al eliminar")
                }
            _eliminando.value = false
        }
    }

    fun descargarEstudioClinico(idEstudio: Long,
                                onSuccess: (ByteArray) -> Unit,
                                onError: (String) -> Unit
    ) {
        viewModelScope.launch { descargarEstudio(idEstudio)
                .onSuccess { bytes -> onSuccess(bytes) }
                .onFailure { error -> onError(error.message ?: "No se pudo descargar el estudio") }
        }
    }
}

sealed class EstudiosState {
    object Loading : EstudiosState()
    data class Success(val estudios: List<EstudioClinicoDTO>) : EstudiosState()
    data class Error(val message: String) : EstudiosState()
}