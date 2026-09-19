package com.proyecto_final.triage.location

data class AddressSuggestion(
    val label: String,
    val latitude: Double,
    val longitude: Double
)

data class LocationState(
    val useCurrentLocation: Boolean = true,
    val manualAddress: String = "",
    val currentLocation: CurrentLocation? = null,
    val currentAddressLabel: String = "",
    val isLoadingLocation: Boolean = true,
    val isSearchingAddress: Boolean = false,
    val addressSearchError: String? = null,
    val foundAddress: String? = null,
    val suggestions: List<AddressSuggestion> = emptyList(),
    val isLoadingSuggestions: Boolean = false,
    val suggestionsError: String? = null
)