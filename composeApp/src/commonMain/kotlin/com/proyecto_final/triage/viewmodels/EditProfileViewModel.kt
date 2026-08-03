package com.proyecto_final.triage.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto_final.triage.network.PerfilResponse
import com.proyecto_final.triage.network.PerfilUpdateResult
import com.proyecto_final.triage.network.PerfilUsuarioRequest
import com.proyecto_final.triage.network.actualizarPerfil
import com.proyecto_final.triage.screens.isValidEmail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditProfileViewModel : ViewModel() {

    private val _state = MutableStateFlow(EditProfileState())
    val state: StateFlow<EditProfileState> = _state

    fun prefill(perfil: PerfilResponse) {
        _state.value = EditProfileState(
            nombre = perfil.nombre,
            apellido = perfil.apellido,
            tipoDocumento = tipoDocumentoParaUi(perfil.tipoDocumento),
            numeroDocumento = perfil.numeroDocumento,

            fechaNacimiento = perfil.fechaNacimiento?.let { formatearFechaParaPicker(it) } ?: "",

            generoBiologico = perfil.generoBiologico.orEmpty(),
            generoConElQueSeIdentifica = perfil.generoConElQueSeIdentifica.orEmpty(),

            email = perfil.email.orEmpty(),
            telefono = perfil.telefono.orEmpty(),

            calle = perfil.calle.orEmpty(),
            alturaDireccion = perfil.alturaDireccion.orEmpty(),
            piso = perfil.piso.orEmpty(),
            codigoPostal = perfil.codigoPostal.orEmpty(),

            ciudad = perfil.ciudad.orEmpty(),
            provincia = perfil.provincia.orEmpty(),

            peso = extraerNumero(perfil.peso?.toString(), esAltura = false),
            alturaPersona = extraerNumero(perfil.alturaPersona?.toString(), esAltura = true)
        )
    }

    fun onFieldChange(campo: String, valor: String) {
        _state.update { estado ->
            when (campo) {
                "nombre" -> estado.copy(nombre = valor)
                "apellido" -> estado.copy(apellido = valor)
                "tipoDocumento" -> estado.copy(tipoDocumento = valor)
                "numeroDocumento" -> estado.copy(numeroDocumento = valor)
                "fechaNacimiento" -> estado.copy(fechaNacimiento = valor)

                "generoBiologico" -> estado.copy(generoBiologico = valor.uppercase())
                "generoConElQueSeIdentifica" -> if (valor == "Otro") {
                    estado.copy(generoConElQueSeIdentifica = "X")
                } else {
                    estado.copy(generoConElQueSeIdentifica = valor.uppercase())
                }

                "email" -> estado.copy(email = valor)
                "telefono" -> estado.copy(telefono = valor)

                "calle" -> estado.copy(calle = valor)
                "alturaDireccion" -> estado.copy(alturaDireccion = valor)
                "piso" -> estado.copy(piso = valor)
                "codigoPostal" -> estado.copy(codigoPostal = valor)
                "ciudad" -> estado.copy(ciudad = valor)
                "provincia" -> estado.copy(provincia = valor)

                "peso" -> estado.copy(peso = valor)
                "alturaPersona" -> estado.copy(alturaPersona = valor)
                else -> estado
            }
        }
    }

    fun guardar() {
        val errores = validar()
        if (errores.isNotEmpty()) {
            _state.update { it.copy(errors = errores) }
            return
        }

        println("Guardando perfil con datos (antes de update): ${_state.value}")

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errors = emptyMap(), generalError = null) }

            println("Guardando perfil con datos (despues de update): ${_state.value}")
            when (val result = actualizarPerfil(buildRequest())) {
                is PerfilUpdateResult.Success ->
                    _state.update { it.copy(isSaving = false, saved = true) }

                is PerfilUpdateResult.FieldErrors ->
                    _state.update { it.copy(isSaving = false, errors = result.errors) }

                is PerfilUpdateResult.Error ->
                    _state.update { it.copy(isSaving = false, generalError = result.message) }
            }
        }
    }

    private fun validar(): Map<String, String> {
        val s = _state.value
        val errores = mutableMapOf<String, String>()

        if (s.nombre.isBlank()) errores["nombre"] = "Este campo es obligatorio"
        if (s.apellido.isBlank()) errores["apellido"] = "Este campo es obligatorio"
        if (s.tipoDocumento.isBlank()) errores["tipoDocumento"] = "Este campo es obligatorio"
        if (s.numeroDocumento.isBlank()) errores["numeroDocumento"] = "Este campo es obligatorio"
        if (s.fechaNacimiento.isBlank()) errores["fechaNacimiento"] = "Este campo es obligatorio"
        if (s.generoBiologico.isBlank()) errores["generoBiologico"] = "Este campo es obligatorio"
        if (s.generoConElQueSeIdentifica.isBlank()) {
            errores["generoConElQueSeIdentifica"] = "Este campo es obligatorio"
        }

        if (s.email.isBlank()) {
            errores["email"] = "Este campo es obligatorio"
        } else if (!isValidEmail(s.email.trim())) {
            errores["email"] = "Ingresá un correo válido"
        }

        if (s.telefono.isBlank()) {
            errores["telefono"] = "Este campo es obligatorio"
        } else if (s.telefono.trim().any { !it.isDigit() && it !in "+-() " }) {
            errores["telefono"] = "Ingresá un teléfono válido"
        }

        if (s.calle.isBlank()) errores["calle"] = "Este campo es obligatorio"

        if (s.peso.isBlank()) {
            errores["peso"] = "Este campo es obligatorio"
        } else {
            val numero = s.peso.trim().replace(',', '.').toDoubleOrNull()
            if (numero == null || numero <= 0) {
                errores["peso"] = "Ingresá un peso válido"
            }
        }

        if (s.alturaPersona.isBlank()) {
            errores["alturaPersona"] = "Este campo es obligatorio"
        } else {
            val numero = s.alturaPersona.trim().toIntOrNull()
            if (numero == null || numero <= 0) {
                errores["alturaPersona"] = "Ingresá una altura válida"
            }
        }

        if (s.alturaDireccion.isBlank()) {
            errores["alturaDireccion"] = "Este campo es obligatorio"
        } else if (!s.alturaDireccion.trim().all { it.isDigit() }) {
            errores["alturaDireccion"] = "Ingresá un valor válido"
        }

        if (s.piso.isNotBlank() && !s.piso.trim().all { it.isDigit() }) {
            errores["piso"] = "Ingresá un valor válido"
        }

        if (s.codigoPostal.isBlank()) {
            errores["codigoPostal"] = "Este campo es obligatorio"
        } else if (!s.codigoPostal.trim().all { it.isDigit() }) {
            errores["codigoPostal"] = "Ingresá un código postal válido"
        }

        if (s.ciudad.isBlank()) errores["ciudad"] = "Este campo es obligatorio"
        if (s.provincia.isBlank()) errores["provincia"] = "Este campo es obligatorio"

        return errores
    }

    private fun buildRequest(): PerfilUsuarioRequest {
        val s = _state.value
        return PerfilUsuarioRequest(
            nombre = s.nombre.trim(),
            apellido = s.apellido.trim(),
            tipoDocumento = tipoDocumentoParaBackend(s.tipoDocumento),
            numeroDocumento = s.numeroDocumento.trim(),
            fechaNacimiento = fechaParaBackend(s.fechaNacimiento),
            generoBiologico = s.generoBiologico.ifBlank { "" },
            generoConElQueSeIdentifica = s.generoConElQueSeIdentifica.ifBlank { "" },
            email = s.email.trim().ifBlank { "" },
            telefono = s.telefono.trim().ifBlank { "" },
            calle = s.calle.trim().ifBlank { "" },
            alturaDireccion = s.alturaDireccion.trim().ifBlank { "" },
            piso = s.piso.trim().ifBlank { "" },
            codigoPostal = s.codigoPostal.trim().ifBlank { "" },
            ciudad = s.ciudad.trim().ifBlank { "" },
            provincia = s.provincia.trim().ifBlank { "" },
            peso = s.peso.trim().replace(',', '.').toDoubleOrNull() ?: 0.0,
            alturaPersona = s.alturaPersona.trim().toIntOrNull() ?: 0
        )
    }

    private fun tipoDocumentoParaBackend(tipo: String): String = when (tipo) {
        "Lib. Cívica" -> "LIBRETA_CIVICA"
        "Lib. Enrolamiento" -> "LIBRETA_ENROLAMIENTO"
        else -> tipo.uppercase()
    }

    private fun tipoDocumentoParaUi(tipo: String): String = when (tipo) {
        "LIBRETA_CIVICA" -> "Lib. Cívica"
        "LIBRETA_ENROLAMIENTO" -> "Lib. Enrolamiento"
        else -> tipo
    }

    private fun formatearFechaParaPicker(fecha: String): String {
        val partes = fecha.split("-")
        if (partes.size != 3) return fecha

        return try {
            val anio = partes[0].toInt()
            val mes = partes[1].toInt()
            val dia = partes[2].toInt()
            "${dia.toString().padStart(2, '0')}/${mes.toString().padStart(2, '0')}/$anio"
        } catch (e: NumberFormatException) {
            fecha
        }
    }

    private fun fechaParaBackend(fecha: String): String {
        val partes = fecha.split("/")
        if (partes.size != 3) return ""

        return try {
            val dia = partes[0].toInt()
            val mes = partes[1].toInt()
            val anio = partes[2].toInt()
            if (dia in 1..31 && mes in 1..12 && anio in 1900..2100) {
                "${anio}-${mes.toString().padStart(2, '0')}-${dia.toString().padStart(2, '0')}"
            } else {
                ""
            }
        } catch (e: NumberFormatException) {
            ""
        }
    }

    private fun extraerNumero(texto: String?, esAltura: Boolean): String {
        if (texto.isNullOrBlank()) return ""

        val valor = texto.trim().split(" ").firstOrNull()
            ?.replace(',', '.')
            ?.toDoubleOrNull()
            ?: return ""

        return if (esAltura) {
            val esMetros = texto.contains("m") && !texto.contains("cm")
            if (esMetros) (valor * 100).toInt().toString() else valor.toInt().toString()
        } else {
            if (valor % 1.0 == 0.0) valor.toInt().toString() else valor.toString()
        }
    }
}

data class EditProfileState(
    val nombre: String = "",
    val apellido: String = "",
    val tipoDocumento: String = "",
    val numeroDocumento: String = "",
    val fechaNacimiento: String = "",
    val generoBiologico: String = "",
    val generoConElQueSeIdentifica: String = "",
    val email: String = "",
    val telefono: String = "",
    val calle: String = "",
    val alturaDireccion: String = "",
    val piso: String = "",
    val codigoPostal: String = "",
    val ciudad: String = "",
    val provincia: String = "",
    val peso: String = "",
    val alturaPersona: String = "",
    val errors: Map<String, String> = emptyMap(),
    val isSaving: Boolean = false,
    val generalError: String? = null,
    val saved: Boolean = false
)
