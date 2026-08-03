package com.proyecto_final.triage.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto_final.triage.network.HospitalResponse
import com.proyecto_final.triage.network.TiempoEstimadoArriboHospitalResponse
import com.proyecto_final.triage.network.obtenerHospitalesCercanos
import com.proyecto_final.triage.network.obtenerTiempoArriboHospital
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class HospitalConArribo(
    val hospital: HospitalResponse,
    val arribo: TiempoEstimadoArriboHospitalResponse?
)

class HospitalesViewModel : ViewModel() {

    private val _state = MutableStateFlow<HospitalesState>(HospitalesState.Loading)
    val state: StateFlow<HospitalesState> = _state

    // TODO: reemplazar por el transporte que elija el usuario cuando exista ese selector en la UI
    private val transporteDefault = "transporte-publico"

    fun cargarHospitalesCercanos(
        latitud: Double,
        longitud: Double,
        codigoEspecialidad: String
    ) {
        viewModelScope.launch {
            _state.value = HospitalesState.Loading

            val resultHospitales = obtenerHospitalesCercanos(latitud, longitud, codigoEspecialidad)

            resultHospitales
                .onSuccess { hospitales ->
                    // Por cada hospital, pedimos su tiempo de arribo en paralelo (no secuencial)
                    val conArribo = hospitales.map { hospital ->
                        async {
                            val resultArribo = obtenerTiempoArriboHospital(
                                idHospital = hospital.idHospital,
                                transporte = transporteDefault,
                                latitud = latitud,
                                longitud = longitud
                            )
                            HospitalConArribo(
                                hospital = hospital,
                                arribo = resultArribo.getOrNull()?.firstOrNull()
                            )
                        }
                    }.awaitAll()

                    _state.value = HospitalesState.Success(conArribo)
                }
                .onFailure { error ->
                    _state.value = HospitalesState.Error(error.message ?: "Error desconocido")
                }
        }
    }
}

sealed class HospitalesState {
    object Loading : HospitalesState()
    data class Success(val hospitales: List<HospitalConArribo>) : HospitalesState()
    data class Error(val message: String) : HospitalesState()
}

// Calcula minutos entre ahora y una hora de arribo (asume que la hora es siempre "hoy", no cruza medianoche)
fun minutosHasta(horaArribo: LocalTime): Int {
    val tz = TimeZone.currentSystemDefault()
    val ahora = Clock.System.now().toLocalDateTime(tz).time
    val diffSegundos = horaArribo.toSecondOfDay() - ahora.toSecondOfDay()
    return (diffSegundos / 60).coerceAtLeast(0)
}