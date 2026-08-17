package com.proyecto_final.triage.network

import kotlinx.serialization.Serializable
import kotlinx.datetime.LocalTime

@Serializable
data class HospitalCercanoDTO(
    val idHospital: Long,
    val placeId: String?,
    val nombre: String,
    val direccion: String,
    val especialidades: List<EspecialidadMedicaDTO>,
    val tiempoEstimadoArriboMejorRuta: String?
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
    val PolylineCode: String?,
    val combinacionesLineas: List<CombinacionRutasDTO>
)