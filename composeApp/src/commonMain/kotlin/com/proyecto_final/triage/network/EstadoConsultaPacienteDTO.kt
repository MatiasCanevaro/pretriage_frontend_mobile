package com.proyecto_final.triage.network

import kotlinx.serialization.Serializable

@Serializable
data class EstadoConsultaPacienteDTO(
    val consultaId: Long? = null,
    val estadoConsulta: String? = null,
    val estadoEntradaCola: String? = null,
    val tipoPausa: String? = null,
    val fechaHoraLimiteRespuesta: String? = null,
    val tiempoEstimadoAtencion: TiempoEstimadoAtencionResponse? = null
)

@Serializable
enum class EstadoConsulta {
    PENDIENTE,
    HOSPITAL_SELECCIONADO,
    PRETRIAGE_FINALIZADO,
    PRETRIAGE_EN_PROCESO,
    EN_COLA,
    LLAMADO,
    EN_ESPERA,
    ATRASADO,
    EN_ATENCION,
    FINALIZADA,
    CANCELADA
}

@Serializable
enum class EstadoEntradaCola {
    EN_COLA,
    LLAMADO,
    EN_ESPERA,
    ATRASADO,
    EN_ATENCION,
    FINALIZADA,
    CANCELADA
}

@Serializable
enum class TipoPausaCola {
    AUSENTE_AL_LLAMADO,
    ESPERA_MANUAL,
    ATRASADO_CONFIRMADO
}

@Serializable
data class TiempoEstimadoAtencionResponse(
    val consultaId: Long,
    val fechaHoraAtencionEstimada: String?,
    val hayMedicosActivos: Boolean,
    val medicosActivos: Int,
    val medicosParaEstimacion: Int,
    val posicionEnCola: Int,
    val pacientesAntes: Int,
    val minutosPromedioAtencion: Int,
    val mensaje: String
)