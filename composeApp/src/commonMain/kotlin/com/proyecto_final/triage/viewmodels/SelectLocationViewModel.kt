package com.proyecto_final.triage.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto_final.triage.location.AddressSuggestion
import com.proyecto_final.triage.location.CurrentLocation
import com.proyecto_final.triage.location.LocationState
import com.proyecto_final.triage.location.fetchAddressSuggestions
import com.proyecto_final.triage.location.geocodeAddress
import com.proyecto_final.triage.location.getCurrentLocation
import com.proyecto_final.triage.location.requestLocationPermission
import com.proyecto_final.triage.location.reverseGeocode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class SelectLocationViewModel : ViewModel() {

    var state: LocationState by mutableStateOf(LocationState())
        private set

    fun toggleLocationMode(useCurrent: Boolean) {
        state = state.copy(
            useCurrentLocation = useCurrent,
            addressSearchError = null,
            foundAddress = null,
            suggestions = emptyList(),
            suggestionsError = null
        )
        if (useCurrent) {
            loadCurrentLocation()
        }
    }

    fun updateManualAddress(address: String) {
        state = state.copy(
            manualAddress = address,
            addressSearchError = null,
            foundAddress = null,
            suggestionsError = null
        )
        if (address.length >= 3 && !state.useCurrentLocation) {
            debouncedSuggestionsSearch()
        } else {
            state = state.copy(suggestions = emptyList(), isLoadingSuggestions = false)
        }
    }

    fun searchAddress() {
        val query = state.manualAddress.trim()
        if (query.isBlank()) return

        state = state.copy(
            isSearchingAddress = true,
            addressSearchError = null,
            foundAddress = null
        )

        viewModelScope.launch(Dispatchers.IO) {
            val result = geocodeAddress(query)
            val newState = if (result != null) {
                state.copy(
                    isSearchingAddress = false,
                    currentLocation = result,
                    foundAddress = query,
                    addressSearchError = null,
                    suggestions = emptyList()
                )
            } else {
                state.copy(
                    isSearchingAddress = false,
                    addressSearchError = "No pudimos encontrar esa dirección. Probá con calle, altura y localidad.",
                    foundAddress = null
                )
            }
            state = newState
        }
    }

    fun selectSuggestion(suggestion: AddressSuggestion) {
        state = state.copy(
            manualAddress = suggestion.label,
            currentLocation = CurrentLocation(suggestion.latitude, suggestion.longitude),
            foundAddress = suggestion.label,
            addressSearchError = null,
            suggestions = emptyList(),
            suggestionsError = null
        )
    }

    fun initializeLocation() {
        loadCurrentLocation()
    }

    private fun loadCurrentLocation() {
        viewModelScope.launch {
            state = state.copy(isLoadingLocation = true)
            val hasPermission = requestLocationPermission()
            if (hasPermission) {
                val location = getCurrentLocation()
                val address = if (location != null) reverseGeocode(location) else null
                state = state.copy(
                    isLoadingLocation = false,
                    currentLocation = location,
                    currentAddressLabel = address ?: ""
                )
            } else {
                state = state.copy(isLoadingLocation = false)
            }
        }
    }

    private fun debouncedSuggestionsSearch() {
        viewModelScope.launch {
            delay(400.milliseconds)
            val query = state.manualAddress
            if (query.length < 3 || state.useCurrentLocation || state.foundAddress != null) {
                state = state.copy(suggestions = emptyList(), isLoadingSuggestions = false)
                return@launch
            }

            state = state.copy(isLoadingSuggestions = true, suggestionsError = null)

            try {
                val results = fetchAddressSuggestions(query, state.currentLocation)
                state = if (results.isEmpty()) {
                    state.copy(
                        isLoadingSuggestions = false,
                        suggestions = emptyList(),
                        suggestionsError = "Sin resultados"
                    )
                } else {
                    state.copy(
                        isLoadingSuggestions = false,
                        suggestions = results,
                        suggestionsError = null
                    )
                }
            } catch (e: Exception) {
                state = state.copy(
                    isLoadingSuggestions = false,
                    suggestions = emptyList(),
                    suggestionsError = "No pudimos buscar sugerencias. Revisá tu conexión."
                )
            }
        }
    }

    fun getSelectedLocation(): String? {
        return state.currentLocation?.let { "${it.latitude},${it.longitude}" }
    }

    fun isContinueEnabled(): Boolean {
        return if (state.useCurrentLocation) {
            state.currentLocation != null
        } else {
            state.manualAddress.isNotBlank() && state.foundAddress != null
        }
    }
}