package com.proyecto_final.triage.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto_final.triage.network.EspecialidadMedicaDTO
import com.proyecto_final.triage.network.obtenerEspecialidades
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
class SelectTypeGuardViewModel : ViewModel() {

    private val _state = MutableStateFlow<EspecialidadesState>(
        EspecialidadesState.Loading
    )

    val state: StateFlow<EspecialidadesState> = _state

    fun cargarEspecialidades() {
        viewModelScope.launch {

            _state.value = EspecialidadesState.Loading

            val result = obtenerEspecialidades()

            result  .onSuccess { especialidades -> _state.value = EspecialidadesState.Success(especialidades)}
                .onFailure { error -> _state.value = EspecialidadesState.Error(error.message ?: "Error desconocido")}
        }
    }
}
sealed class EspecialidadesState {

    object Loading : EspecialidadesState()

    data class Success(val especialidades: List<EspecialidadMedicaDTO>) : EspecialidadesState()

    data class Error(val message: String) : EspecialidadesState()
}