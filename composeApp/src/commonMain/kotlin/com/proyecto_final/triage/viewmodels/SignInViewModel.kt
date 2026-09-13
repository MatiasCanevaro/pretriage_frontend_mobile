package com.proyecto_final.triage.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto_final.triage.network.auth.LoginRequest
import com.proyecto_final.triage.network.auth.login
import com.proyecto_final.triage.storage.TokenStorageProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SignInViewModel : ViewModel() {

    private val _state = MutableStateFlow<SignInState>(SignInState.Idle)
    val state: StateFlow<SignInState> = _state

    fun login(email: String, password: String, rememberMe: Boolean) {
        viewModelScope.launch {
            _state.value = SignInState.Loading
            val result = login(LoginRequest(email, password))
            result.onSuccess { response ->
                val token = response.token
                val refreshToken = response.refreshToken
                if (token != null && refreshToken != null) {
                    TokenStorageProvider.instance.saveTokens(token, refreshToken, rememberMe)
                    _state.value = SignInState.Success(token)
                } else {
                    _state.value = SignInState.Error("No se recibió token")
                }
            }.onFailure { error ->
                _state.value = SignInState.Error(error.message ?: "Error desconocido")
            }
        }
    }
}

sealed class SignInState {
    object Idle : SignInState()
    object Loading : SignInState()
    data class Success(val token: String) : SignInState()
    data class Error(val message: String) : SignInState()
}

object AuthState {
    private val _sessionExpired = MutableStateFlow(false)
    val sessionExpired: StateFlow<Boolean> = _sessionExpired

    fun onSessionExpired() {
        _sessionExpired.value = true
    }

    fun reset() {
        _sessionExpired.value = false
    }
}