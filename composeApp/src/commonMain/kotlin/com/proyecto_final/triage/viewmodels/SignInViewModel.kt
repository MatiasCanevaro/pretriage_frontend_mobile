package com.proyecto_final.triage.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto_final.triage.network.LoginRequest
import com.proyecto_final.triage.network.TokenStorage
import com.proyecto_final.triage.network.login
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
            result.onSuccess { token ->
                TokenStorage.saveToken(token, rememberMe)
                _state.value = SignInState.Success(token)
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