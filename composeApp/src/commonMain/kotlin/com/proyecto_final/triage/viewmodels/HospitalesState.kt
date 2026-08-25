package com.proyecto_final.triage.viewmodels

import com.proyecto_final.triage.network.HospitalCercanoDTO

sealed interface HospitalesState {

    data object Loading : HospitalesState

    data class Success(val hospitales: List<HospitalCercanoDTO>) : HospitalesState

    data class Error(val mensaje: String) : HospitalesState
}