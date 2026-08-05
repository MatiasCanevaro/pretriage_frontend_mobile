package com.proyecto_final.triage.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto_final.triage.network.PerfilResponse
import com.proyecto_final.triage.network.obtenerPerfil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val _state = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val state: StateFlow<ProfileState> = _state

    fun cargarPerfil() {
        viewModelScope.launch {
            _state.value = ProfileState.Loading

            val result = obtenerPerfil()

            result.onSuccess { perfil ->
                _state.value = ProfileState.Success(perfil)
            }.onFailure { error ->
                _state.value = ProfileState.Error(error.message ?: "Error desconocido")
            }
        }
    }
}

sealed class ProfileState {
    object Loading : ProfileState()
    data class Success(val perfil: PerfilResponse) : ProfileState()
    data class Error(val message: String) : ProfileState()
}
