package com.proyecto_final.triage.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto_final.triage.network.auth.cambiarContrasenia as cambiarContraseniaApi
import com.proyecto_final.triage.network.auth.solicitarToken as solicitarTokenApi
import com.proyecto_final.triage.network.auth.validarToken as validarTokenApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ForgotPasswordStep {
    Email,
    Token,
    NewPassword
}

sealed class ForgotPasswordState {
    object Idle : ForgotPasswordState()
    object Loading : ForgotPasswordState()
    data class Error(val message: String) : ForgotPasswordState()
    data class Success(val message: String) : ForgotPasswordState()
}

class ForgotPasswordViewModel : ViewModel() {

    companion object {
        const val DEFAULT_EXPIRATION_SEC = 15 * 60 // fallback si backend no envía tiempo
    }

    private val _step = MutableStateFlow(ForgotPasswordStep.Email)
    val step: StateFlow<ForgotPasswordStep> = _step.asStateFlow()

    private val _state = MutableStateFlow<ForgotPasswordState>(ForgotPasswordState.Idle)
    val state: StateFlow<ForgotPasswordState> = _state.asStateFlow()

    private val _countdownSec = MutableStateFlow(DEFAULT_EXPIRATION_SEC)
    val countdownSec: StateFlow<Int> = _countdownSec.asStateFlow()

    private var countdownJob: Job? = null

    var email: String = ""
        private set

    var token: String = ""
        private set

    fun setEmail(value: String) {
        email = value
    }

    fun setToken(value: String) {
        token = value
    }

    fun solicitarToken(emailInput: String) {
        viewModelScope.launch {
            _state.value = ForgotPasswordState.Loading
            val result = solicitarTokenApi(emailInput)
            result.onSuccess { res ->
                email = emailInput
                _state.value = ForgotPasswordState.Idle
                _step.value = ForgotPasswordStep.Token
                startCountdown(res.expiracionSec)
            }.onFailure { e ->
                _state.value = ForgotPasswordState.Error(e.message ?: "No se pudo enviar el correo")
            }
        }
    }

    fun validarToken(tokenInput: String) {
        viewModelScope.launch {
            _state.value = ForgotPasswordState.Loading
            val result = validarTokenApi(tokenInput)
            result.onSuccess { msg ->
                token = tokenInput
                _state.value = ForgotPasswordState.Idle
                _step.value = ForgotPasswordStep.NewPassword
                stopCountdown()
            }.onFailure { e ->
                _state.value = ForgotPasswordState.Error(e.message ?: "Token inválido o expirado")
            }
        }
    }

    fun cambiarContrasenia(nuevaContrasenia: String) {
        viewModelScope.launch {
            _state.value = ForgotPasswordState.Loading
            // usar token guardado
            val currentToken = token
            val result = cambiarContraseniaApi(currentToken, nuevaContrasenia)
            result.onSuccess { msg ->
                _state.value = ForgotPasswordState.Success(msg)
                stopCountdown()
            }.onFailure { e ->
                _state.value = ForgotPasswordState.Error(e.message ?: "No se pudo cambiar la contraseña")
            }
        }
    }

    fun goBack(): Boolean {
        return when (_step.value) {
            ForgotPasswordStep.Token -> {
                _step.value = ForgotPasswordStep.Email
                _state.value = ForgotPasswordState.Idle
                stopCountdown()
                true
            }
            ForgotPasswordStep.NewPassword -> {
                _step.value = ForgotPasswordStep.Token
                _state.value = ForgotPasswordState.Idle
                // no reiniciar countdown, queda el restante; si ya expiró se mostrará error
                if (_countdownSec.value <= 0) {
                    _state.value = ForgotPasswordState.Error("El código expiró. Volvé a solicitarlo.")
                }
                true
            }
            ForgotPasswordStep.Email -> false
        }
    }

    fun clearError() {
        if (_state.value is ForgotPasswordState.Error) {
            _state.value = ForgotPasswordState.Idle
        }
    }

    private fun startCountdown(expirationSec: Int = DEFAULT_EXPIRATION_SEC) {
        countdownJob?.cancel()
        _countdownSec.value = expirationSec
        countdownJob = viewModelScope.launch {
            while (_countdownSec.value > 0) {
                delay(1000)
                _countdownSec.value = _countdownSec.value - 1
            }
            // al llegar a 0, si sigue en paso Token, mostrar error
            if (_step.value == ForgotPasswordStep.Token) {
                _state.value = ForgotPasswordState.Error("El código expiró. Volvé atrás y solicitá uno nuevo.")
            }
        }
    }

    private fun stopCountdown() {
        countdownJob?.cancel()
        countdownJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopCountdown()
    }

    fun reset() {
        _step.value = ForgotPasswordStep.Email
        _state.value = ForgotPasswordState.Idle
        _countdownSec.value = DEFAULT_EXPIRATION_SEC
        stopCountdown()
        email = ""
        token = ""
    }
}
