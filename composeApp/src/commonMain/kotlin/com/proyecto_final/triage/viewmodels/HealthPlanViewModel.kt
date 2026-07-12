package com.proyecto_final.triage.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto_final.triage.network.CredencialRequest
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

            result.onSuccess { mensaje ->
                _state.value = CredencialState.Success(mensaje)
                obtenerCredenciales()
            }.onFailure { error ->
                _state.value = CredencialState.Error(
                    error.message ?: "Error desconocido"
                )
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
    data class Error(val message: String) : CredencialState()
}

fun convertirFechaVencimiento(fecha: String): String {
    val (mesStr, anioStr) = fecha.split("/")

    val mes = mesStr.toInt()
    val anio = 2000 + anioStr.toInt()

    val esBisiesto = (anio % 4 == 0 && anio % 100 != 0) || (anio % 400 == 0)

    val ultimoDia = when (mes) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (esBisiesto) 29 else 28
        else -> throw IllegalArgumentException("Mes inválido")
    }

    return "${anio.toString().padStart(4, '0')}-${mes.toString().padStart(2, '0')}-${ultimoDia.toString().padStart(2, '0')}"
}