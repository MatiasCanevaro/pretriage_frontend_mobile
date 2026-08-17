package com.proyecto_final.triage.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto_final.triage.network.EstadoConsultaPacienteDTO
import com.proyecto_final.triage.network.EstadoEntradaCola
import com.proyecto_final.triage.network.TiempoEstimadoAtencionResponse
import com.proyecto_final.triage.network.TipoPausaCola
import com.proyecto_final.triage.network.ausentarme
import com.proyecto_final.triage.network.estoyAtrasado
import com.proyecto_final.triage.network.llegue
import com.proyecto_final.triage.network.obtenerEstadoConsulta
import com.proyecto_final.triage.network.sigoAsistiendo
import com.proyecto_final.triage.network.suscribirseATiempoEstimado
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface ConsultaActivaState {
    object Loading : ConsultaActivaState
    data class Success(val estado: EstadoConsultaPacienteDTO) : ConsultaActivaState
    data class Error(val message: String) : ConsultaActivaState
}

fun EstadoConsultaPacienteDTO.estadoColaEnum(): EstadoEntradaCola? =
    estadoEntradaCola?.let { runCatching { EstadoEntradaCola.valueOf(it) }.getOrNull() }

fun EstadoConsultaPacienteDTO.tipoPausaEnum(): TipoPausaCola? =
    tipoPausa?.let { runCatching { TipoPausaCola.valueOf(it) }.getOrNull() }

class ConsultaActivaViewModel : ViewModel() {

    var state by mutableStateOf<ConsultaActivaState>(ConsultaActivaState.Loading)
        private set

    // Errores puntuales de una acción, sin tirar abajo toda la pantalla
    var actionError by mutableStateOf<String?>(null)
        private set

    var isAusentarmeLoading by mutableStateOf(false)
        private set

    var isEstoyAtrasadoLoading by mutableStateOf(false)
        private set

    var isSigoAsistiendoLoading by mutableStateOf(false)
        private set

    var isLlegueLoading by mutableStateOf(false)
        private set

    private var sseJob: Job? = null
    private var sseConsultaId: Long? = null

    fun cargarEstado(mostrarLoading: Boolean = true) {
        viewModelScope.launch {

            if (mostrarLoading) {
                state = ConsultaActivaState.Loading
            }

            obtenerEstadoConsulta()
                .onSuccess { estado ->
                    state = ConsultaActivaState.Success(estado)
                    sincronizarSse(estado)
                }
                .onFailure { error ->
                    // Si ya teníamos datos cargados (ej: falló el polling silencioso),
                    // no pisamos la pantalla con un error, solo si no había nada aún.
                    if (mostrarLoading || state !is ConsultaActivaState.Success) {
                        state = ConsultaActivaState.Error(
                            error.message ?: "No se pudo obtener el estado de la consulta."
                        )
                    }
                }
        }
    }

    fun ausentarme() {
        viewModelScope.launch {
            actionError = null
            isAusentarmeLoading = true

            ausentarmeRequest()

            isAusentarmeLoading = false
        }
    }

    fun estoyAtrasado() {
        viewModelScope.launch {
            actionError = null
            isEstoyAtrasadoLoading = true

            estoyAtrasadoRequest()

            isEstoyAtrasadoLoading = false
        }
    }

    fun sigoAsistiendo() {
        viewModelScope.launch {
            actionError = null
            isSigoAsistiendoLoading = true

            sigoAsistiendoRequest()

            isSigoAsistiendoLoading = false
        }
    }

    fun llegue() {
        viewModelScope.launch {
            actionError = null
            isLlegueLoading = true

            llegueRequest()

            isLlegueLoading = false
        }
    }

    private suspend fun ausentarmeRequest() {
        com.proyecto_final.triage.network.ausentarme()
            .onSuccess { estado -> aplicarNuevoEstado(estado) }
            .onFailure { error -> actionError = error.message ?: "No se pudo procesar la acción." }
    }

    private suspend fun estoyAtrasadoRequest() {
        com.proyecto_final.triage.network.estoyAtrasado()
            .onSuccess { estado -> aplicarNuevoEstado(estado) }
            .onFailure { error -> actionError = error.message ?: "No se pudo procesar la acción." }
    }

    private suspend fun sigoAsistiendoRequest() {
        com.proyecto_final.triage.network.sigoAsistiendo()
            .onSuccess { estado -> aplicarNuevoEstado(estado) }
            .onFailure { error -> actionError = error.message ?: "No se pudo procesar la acción." }
    }

    private suspend fun llegueRequest() {
        com.proyecto_final.triage.network.llegue()
            .onSuccess { estado -> aplicarNuevoEstado(estado) }
            .onFailure { error -> actionError = error.message ?: "No se pudo procesar la acción." }
    }

    private fun aplicarNuevoEstado(estado: EstadoConsultaPacienteDTO) {
        state = ConsultaActivaState.Success(estado)
        sincronizarSse(estado)
    }

    /**
     * Abre (o mantiene) la conexión SSE cuando la consulta está EN_COLA,
     * y la corta apenas deja de estarlo. Reintenta la conexión sola si se cae
     * mientras sigamos EN_COLA (el servidor cierra el emitter en cualquier error de IO).
     */
    private fun sincronizarSse(estado: EstadoConsultaPacienteDTO) {
        val consultaId = estado.consultaId
        val enCola = estado.estadoColaEnum() == EstadoEntradaCola.EN_COLA

        if (!enCola || consultaId == null) {
            detenerSse()
            return
        }

        if (sseJob?.isActive == true && sseConsultaId == consultaId) return

        detenerSse()
        sseConsultaId = consultaId
        sseJob = viewModelScope.launch {
            while (isActive) {
                runCatching {
                    suscribirseATiempoEstimado(consultaId) { estimacion ->
                        actualizarEstimacion(estimacion)
                    }
                }
                if (isActive) delay(3_000) // se cortó la conexión: reintentar
            }
        }
    }

    private fun detenerSse() {
        sseJob?.cancel()
        sseJob = null
        sseConsultaId = null
    }

    /** Llamar explícitamente al salir de la pantalla (el ViewModel se crea con `remember`,
     *  por lo que `onCleared()` no se dispara solo al hacer pop de la Screen). */
    fun detenerActualizaciones() {
        detenerSse()
    }

    private fun actualizarEstimacion(estimacion: TiempoEstimadoAtencionResponse) {
        val actual = state
        if (actual is ConsultaActivaState.Success) {
            state = ConsultaActivaState.Success(actual.estado.copy(tiempoEstimadoAtencion = estimacion))
        }
    }

    override fun onCleared() {
        detenerSse()
        super.onCleared()
    }
}