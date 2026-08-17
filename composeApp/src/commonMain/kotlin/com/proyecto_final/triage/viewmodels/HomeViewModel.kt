package com.proyecto_final.triage.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto_final.triage.network.obtenerEstadoConsulta
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    var state by mutableStateOf<HomeState>(HomeState.Loading)
        private set

    val tieneHospitalSeleccionado: Boolean
        get() {
            val estado = (state as? HomeState.Success)
                ?.estado
                ?.estadoConsulta

            return estado == "HOSPITAL_SELECCIONADO" ||
                    estado == "PRETRIAGE_EN_PROCESO" ||
                    estado == "PRETRIAGE_FINALIZADO"
        }

    fun cargarEstadoConsulta() {

        viewModelScope.launch {

            state = HomeState.Loading

            obtenerEstadoConsulta()
                .onSuccess { estado ->

                    println("===== ESTADO CONSULTA =====")
                    println("Consulta ID: ${estado.consultaId}")
                    println("Estado: ${estado.estadoConsulta}")
                    println("Estado cola: ${estado.estadoEntradaCola}")
                    println("Tipo pausa: ${estado.tipoPausa}")
                    println("Estimación: ${estado.tiempoEstimadoAtencion}")
                    println("===========================")

                    state = HomeState.Success(estado)
                }
                .onFailure { error ->

                    state = HomeState.Error(
                        error.message
                            ?: "No se pudo obtener el estado de la consulta."
                    )
                }
        }
    }
}