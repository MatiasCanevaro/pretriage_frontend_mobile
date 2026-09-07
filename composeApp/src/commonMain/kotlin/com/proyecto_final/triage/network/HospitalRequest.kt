package com.proyecto_final.triage.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class HospitalCercanoDTO(
    val idHospital: Long,
    val placeId: String?,
    val nombre: String,
    val direccion: String,
    val especialidades: List<EspecialidadMedicaDTO>,
    val tiempoEstimadoArriboMejorRuta: String? = null,
    val pacientesEnCola: Int = 0,
    val disponible: Boolean = true,
    val minutosEsperaEstimados: Long? = null,
    val fechaHoraAtencionEstimada: String? = null
)

@Serializable
data class CombinacionRutasDTO(
    val nombreLinea: String?,
    val tipoTransporte: String,
    val indicaciones: String
)

@Serializable
data class TiempoEstimadoArriboHospitalResponse(
    val transporte: String,
    val tiempoEstimadoArribo: String?,
    val idHospital: Long,
    val distanciaMetros: Int,
    @SerialName("PolylineCode")
    val polylineCode: String?,
    val combinacionesLineas: List<CombinacionRutasDTO>
)