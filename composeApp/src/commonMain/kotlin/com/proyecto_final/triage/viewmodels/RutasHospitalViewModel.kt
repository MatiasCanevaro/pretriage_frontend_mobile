package com.proyecto_final.triage.viewmodels

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.proyecto_final.triage.network.RutasHospitalState
import com.proyecto_final.triage.network.obtenerTiempoArriboHospital
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch


class RutasHospitalViewModel : ViewModel() {

    var state by mutableStateOf<RutasHospitalState>(
        RutasHospitalState.Loading
    )
        private set

    fun cargarRutas(
        idHospital: Long,
        transporte: String,
        latitud: Double,
        longitud: Double
    ) {
        viewModelScope.launch {

            state = RutasHospitalState.Loading

            val resultado = obtenerTiempoArriboHospital(
                idHospital = idHospital,
                transporte = transporte,
                latitud = latitud,
                longitud = longitud
            )

            resultado
                .onSuccess { rutas ->
                    println("========== RUTAS HOSPITAL ==========")
                    println("Cantidad de rutas: ${rutas.size}")

                    rutas.forEachIndexed { index, ruta ->
                        println("---- RUTA $index ----")
                        println("Transporte: ${ruta.transporte}")
                        println("Tiempo: ${ruta.tiempoEstimadoArribo}")
                        println("Hospital: ${ruta.idHospital}")
                        println("Distancia: ${ruta.distanciaMetros}")
                        println("Polyline: ${ruta.PolylineCode}")

                        println("Combinaciones:")
                        ruta.combinacionesLineas.forEach { combinacion ->
                            println("  Línea: ${combinacion.nombreLinea}")
                            println("  Transporte: ${combinacion.tipoTransporte}")
                            println("  Indicaciones: ${combinacion.indicaciones}")
                        }
                    }

                    println("====================================")

                    state = RutasHospitalState.Success(rutas)
                }
                .onFailure { error ->
                    state = RutasHospitalState.Error(
                        error.message
                            ?: "No pudimos obtener las rutas al hospital."
                    )
                }
        }
    }
}