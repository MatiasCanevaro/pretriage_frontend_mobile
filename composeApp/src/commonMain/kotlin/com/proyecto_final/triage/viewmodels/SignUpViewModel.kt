package com.proyecto_final.triage.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto_final.triage.network.RegisterRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.proyecto_final.triage.network.register

class SignUpViewModel : ViewModel() {

    private val _state = MutableStateFlow<SignUpState>(SignUpState.Idle)
    val state: StateFlow<SignUpState> = _state

    fun createAccount(request: RegisterRequest) {
        viewModelScope.launch {
            println("REQUEST: $request")
            _state.value = SignUpState.Loading
            val result = register(request)
            result.onSuccess { token ->
                println("SUCCESS TOKEN: $token")
                _state.value = SignUpState.Success(token)
            }.onFailure { error ->
                println("ERROR: ${error.message}")
                _state.value = SignUpState.Error(error.message ?: "Error desconocido")
            }
        }
    }
}

sealed class SignUpState {
    object Idle : SignUpState()
    object Loading : SignUpState()
    data class Success(val token: String) : SignUpState()
    data class Error(val message: String) : SignUpState()
}