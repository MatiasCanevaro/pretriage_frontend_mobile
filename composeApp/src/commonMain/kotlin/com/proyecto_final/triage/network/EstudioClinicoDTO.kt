package com.proyecto_final.triage.network

import kotlinx.serialization.Serializable
import com.proyecto_final.triage.utils.JavaSerializable

@Serializable
data class EstudioClinicoDTO(
    val id: Long,
    val pacienteId: Long? = null,
    val nombreArchivo: String? = null,
    val tipoArchivo: String,
    val extensionArchivo: String? = null,
    val descripcion: String? = null,
    val fechaSubida: String? = null,
    val tamanoArchivo: Long? = null,
    val rutaArchivo: String? = null
) : JavaSerializable