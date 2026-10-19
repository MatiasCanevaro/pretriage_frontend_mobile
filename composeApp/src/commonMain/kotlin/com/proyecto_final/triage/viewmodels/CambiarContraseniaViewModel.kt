package com.proyecto_final.triage.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto_final.triage.network.cambioContrasenia.CambioContraseniaException
import com.proyecto_final.triage.network.cambioContrasenia.cambiarContrasenia
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/* ============================================================
   REGLAS DE LA CONTRASEÑA NUEVA
   Mismas reglas que CambiarContraseniaAutenticadaRequest (backend)
   ============================================================ */

data class ReglaContrasenia(val descripcion: String, val cumple: Boolean)

fun validarContraseniaNueva(contrasenia: String): List<ReglaContrasenia> {
    // Igual que el backend: los emojis cuentan como 1 carácter, no 2
    val largo = contrasenia.length - contrasenia.count { it.isLowSurrogate() }

    return listOf(
        ReglaContrasenia("Entre 8 y 32 caracteres", largo in 8..32),
        ReglaContrasenia("Al menos una mayúscula", contrasenia.any { it.isUpperCase() }),
        ReglaContrasenia("Al menos una minúscula", contrasenia.any { it.isLowerCase() }),
        ReglaContrasenia(
            "Al menos un número o carácter especial",
            contrasenia.any { it.isDigit() || (!it.isLetterOrDigit() && !it.isWhitespace()) }
        )
    )
}

/* ============================================================
   ESTADO
   ============================================================ */

data class CambiarContraseniaState(
    val actual: String = "",
    val nueva: String = "",
    val repetir: String = "",
    val enviando: Boolean = false,
    val exito: Boolean = false,
    val bloqueado: Boolean = false,
    val error: String? = null
) {
    // Reglas de fortaleza + que sea distinta de la actual.
    // Esta última la valida el backend, pero si llega a él cuenta como
    // intento fallido (3 intentos = 24 h de bloqueo), así que la frenamos acá.
    val reglas: List<ReglaContrasenia>
        get() = validarContraseniaNueva(nueva) + ReglaContrasenia(
            "Distinta de la contraseña actual",
            nueva.isNotEmpty() && nueva != actual
        )

    val coinciden: Boolean
        get() = repetir.isNotEmpty() && repetir == nueva

    val puedeEnviar: Boolean
        get() = actual.isNotBlank() &&
                reglas.all { it.cumple } &&
                coinciden &&
                !enviando &&
                !bloqueado
}

/* ============================================================
   VIEWMODEL
   ============================================================ */

class CambiarContraseniaViewModel : ViewModel() {

    private val _state = MutableStateFlow(CambiarContraseniaState())
    val state: StateFlow<CambiarContraseniaState> = _state.asStateFlow()

    fun onActualChange(valor: String) =
        _state.update { it.copy(actual = valor, error = null) }

    fun onNuevaChange(valor: String) =
        _state.update { it.copy(nueva = valor, error = null) }

    fun onRepetirChange(valor: String) =
        _state.update { it.copy(repetir = valor, error = null) }

    fun cambiar() {
        val actual = _state.value
        if (!actual.puedeEnviar) return

        viewModelScope.launch {
            _state.update { it.copy(enviando = true, error = null) }

            cambiarContrasenia(
                contraseniaActual = actual.actual,
                contraseniaNueva = actual.nueva
            )
                .onSuccess {
                    _state.update { it.copy(enviando = false, exito = true) }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            enviando = false,
                            bloqueado = (e as? CambioContraseniaException)?.bloqueado == true,
                            error = when (e) {
                                // Mensaje del backend (ya viene en español y para el usuario)
                                is CambioContraseniaException -> e.message
                                // Error de red u otro inesperado
                                else -> "No pudimos conectarnos. Revisá tu conexión e intentá nuevamente."
                            }
                        )
                    }
                }
        }
    }
}