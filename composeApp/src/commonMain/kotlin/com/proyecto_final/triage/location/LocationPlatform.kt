package com.proyecto_final.triage.location

expect suspend fun requestLocationPermission(): Boolean
expect suspend fun getCurrentLocation(): CurrentLocation?
expect suspend fun geocodeAddress(query: String): CurrentLocation?
expect suspend fun reverseGeocode(location: CurrentLocation): String?
expect suspend fun fetchAddressSuggestions(query: String, near: CurrentLocation?): List<AddressSuggestion>