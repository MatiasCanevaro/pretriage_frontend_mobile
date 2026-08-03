package com.proyecto_final.triage.network

import kotlinx.serialization.Serializable
import kotlinx.datetime.LocalTime

@Serializable
data class HospitalResponse(
    val idHospital: Long,
    val placeId: String,
    val nombre: String,
    val direccion: String,
    val especialidades: List<EspecialidadResponse>
)

@Serializable
data class CombinacionRutasDTO(
    val nombreLinea: String
)

@Serializable
data class TiempoEstimadoArriboHospitalResponse(
    val transporte: String,
    val tiempoEstimadoArribo: LocalTime,
    val idHospital: Long,
    val distanciaMetros: Int,
    val polylineCode: String,
    val combinacionesLineas: List<CombinacionRutasDTO> = emptyList()
)
@Serializable
data class EspecialidadResponse(
    val codigo: String,
    val nombre: String
)