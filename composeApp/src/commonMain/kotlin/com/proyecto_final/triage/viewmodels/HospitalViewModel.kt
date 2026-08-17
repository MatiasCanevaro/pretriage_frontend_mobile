package com.proyecto_final.triage.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto_final.triage.network.HospitalCercanoDTO
import com.proyecto_final.triage.network.elegirHospital
import com.proyecto_final.triage.network.obtenerHospitalesCercanos
import com.proyecto_final.triage.screens.Hospital
import kotlinx.coroutines.launch

class HospitalesViewModel : ViewModel() {

    var state by mutableStateOf<HospitalesState>(
        HospitalesState.Loading
    )
        private set

    var hospitalSeleccionado by mutableStateOf(false)
        private set

    fun buscarHospitalesCercanos(
        latitud: Double,
        longitud: Double,
        codigoEspecialidad: String,
        transporte: String = "transporte-publico"
    ) {
        viewModelScope.launch {

            state = HospitalesState.Loading

            val resultado = obtenerHospitalesCercanos(
                latitud = latitud,
                longitud = longitud,
                codigoEspecialidad = codigoEspecialidad,
                transporte = transporte
            )

            resultado.onSuccess { hospitales -> state = HospitalesState.Success(hospitales) }
                     .onFailure { state = HospitalesState.Error("No pudimos cargar los hospitales cercanos.")
                }
        }
    }

    fun seleccionarHospital(
        hospital: Hospital,
        codigoEspecialidad: String
    ) {
        println("HOSPITAL SELECCIONADO: ${hospital.nombre}")
        println("PLACE ID: ${hospital.placeId}")
        println("ESPECIALIDAD: $codigoEspecialidad")

        viewModelScope.launch {

            val placeId = hospital.placeId
                ?: return@launch

            val resultado = elegirHospital(
                placeId = placeId,
                codigoEspecialidad = codigoEspecialidad
            )

            resultado
                .onSuccess {
                    hospitalSeleccionado = true
                }
                .onFailure {
                    // Error
                }
        }
    }
}