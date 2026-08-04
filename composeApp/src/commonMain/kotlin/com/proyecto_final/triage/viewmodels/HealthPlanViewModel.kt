package com.proyecto_final.triage.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto_final.triage.network.CredencialRequest
import com.proyecto_final.triage.network.CredencialUpdateResult
import com.proyecto_final.triage.network.cargarCredencial
import com.proyecto_final.triage.screens.Credencial
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.YearMonth

class HealthPlanViewModel : ViewModel() {

    private val _state = MutableStateFlow<CredencialState>(CredencialState.Idle)
    val state: StateFlow<CredencialState> = _state

    private val _credencialesState = MutableStateFlow<CredencialesState>(CredencialesState.Loading)
    val credencialesState: StateFlow<CredencialesState> = _credencialesState

    fun cargarCredencial(nombreObraSocial: String, numeroAfiliado: String, plan: String, fechaVencimiento: String) {
        viewModelScope.launch {
            _state.value = CredencialState.Loading

            val result = cargarCredencial(
                CredencialRequest(
                    nombreObraSocial = nombreObraSocial,
                    numeroAfiliado = numeroAfiliado,
                    plan = plan,
                    fechaVencimiento = convertirFechaVencimiento(fechaVencimiento)
                )
            )

            when (result) {
                is CredencialUpdateResult.Success -> {
                    _state.value = CredencialState.Success(result.mensaje)
                    obtenerCredenciales()
                }

                is CredencialUpdateResult.FieldErrors ->
                    _state.value = CredencialState.Error(fieldErrors = result.errors)

                is CredencialUpdateResult.Error ->
                    _state.value = CredencialState.Error(message = result.message)
            }
        }
    }

    fun resetState() {
        _state.value = CredencialState.Idle
    }

    fun obtenerCredenciales() {
        viewModelScope.launch {

            val result = com.proyecto_final.triage.network.obtenerCredenciales()

            result.onSuccess { lista ->
                _credencialesState.value =
                    CredencialesState.Success(lista)
            }.onFailure {
                _credencialesState.value =
                    CredencialesState.Error(it.message ?: "Error")
            }
        }
    }
}

sealed class CredencialesState {
    object Loading : CredencialesState()
    data class Success(val credenciales: List<Credencial>) : CredencialesState()
    data class Error(val message: String) : CredencialesState()
}

sealed class CredencialState {
    object Idle : CredencialState()
    object Loading : CredencialState()
    data class Success(val mensaje: String) : CredencialState()
    data class Error(
        val message: String? = null,
        val fieldErrors: Map<String, String> = emptyMap()
    ) : CredencialState()
}

fun convertirFechaVencimiento(fecha: String): String {
    if (fecha.isBlank()) return ""
    val partes = fecha.split("/")
    if (partes.size != 2) return ""

    val mes = partes[0].toIntOrNull() ?: return ""
    val anio = partes[1].toIntOrNull() ?: return ""

    if (mes !in 1..12) return ""

    return "${anio.toString().padStart(4, '0')}-${mes.toString().padStart(2, '0')}-01"
}