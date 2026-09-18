package com.proyecto_final.triage.location

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.proyecto_final.triage.appContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.coroutines.resume
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

private val PHOTON_BBOX_CABA = "-58.5315,-34.7051,-58.3354,-34.5265"

@Serializable
private data class PhotonFeature(
    val properties: PhotonProperties,
    val geometry: PhotonGeometry
)

@Serializable
private data class PhotonProperties(
    val name: String? = null,
    val street: String? = null,
    val housenumber: String? = null,
    val city: String? = null,
    val locality: String? = null,
    val state: String? = null,
    val country: String? = null
)

@Serializable
private data class PhotonGeometry(
    val coordinates: List<Double>
)

@Serializable
private data class PhotonResponse(
    val features: List<PhotonFeature>
)

actual suspend fun requestLocationPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
        appContext,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

actual suspend fun getCurrentLocation(): CurrentLocation? {
    val tienePermiso = ContextCompat.checkSelfPermission(
        appContext,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    if (!tienePermiso) {
        return null
    }

    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(appContext)

    return suspendCancellableCoroutine { continuation ->

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    continuation.resume(
                        CurrentLocation(
                            latitude = location.latitude,
                            longitude = location.longitude
                        )
                    )
                } else {
                    continuation.resume(null)
                }
            }
            .addOnFailureListener {
                continuation.resume(null)
            }
    }
}

actual suspend fun geocodeAddress(query: String): CurrentLocation? {
    return withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null
        val geocoder = Geocoder(appContext, Locale.getDefault())
        try {
            @Suppress("DEPRECATION")
            val resultados = geocoder.getFromLocationName(query, 1)
            resultados?.firstOrNull()?.let { CurrentLocation(it.latitude, it.longitude) }
        } catch (e: Exception) {
            null
        }
    }
}

actual suspend fun reverseGeocode(location: CurrentLocation): String? {
    return withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null
        val geocoder = Geocoder(appContext, Locale.getDefault())
        try {
            @Suppress("DEPRECATION")
            val resultados = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            val direccion = resultados?.firstOrNull()
            direccion?.let {
                val calle = it.thoroughfare
                val altura = it.subThoroughfare
                when {
                    !calle.isNullOrBlank() && !altura.isNullOrBlank() -> "$calle $altura"
                    !calle.isNullOrBlank() -> calle
                    else -> it.getAddressLine(0)
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}

actual suspend fun fetchAddressSuggestions(query: String, near: CurrentLocation?): List<AddressSuggestion> {
    return withContext(Dispatchers.IO) {
        val q = URLEncoder.encode(query, "UTF-8")
        var urlStr = "https://photon.komoot.io/api/?q=$q&limit=5&countrycode=AR&bbox=$PHOTON_BBOX_CABA"
        if (near != null) {
            urlStr += "&lat=${near.latitude}&lon=${near.longitude}&location_bias_scale=2"
        }
        val connection = URL(urlStr).openConnection() as java.net.HttpURLConnection
        connection.connectTimeout = 6000
        connection.readTimeout = 6000
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "TriageApp/1.0 (Android)")

        try {
            val codigo = connection.responseCode
            if (codigo !in 200..299) {
                return@withContext emptyList()
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = Json { ignoreUnknownKeys = true }
            val response = json.decodeFromString<PhotonResponse>(body)

            response.features.mapNotNull { feature ->
                val props = feature.properties
                val coords = feature.geometry.coordinates
                if (coords.size < 2) return@mapNotNull null

                val lon = coords[0]
                val lat = coords[1]

                val provincia = props.state ?: ""
                if (provincia.isNotBlank() &&
                    !provincia.contains("Autónoma", ignoreCase = true) &&
                    !provincia.contains("Autonoma", ignoreCase = true)
                ) {
                    return@mapNotNull null
                }

                val calle = props.street ?: ""
                val altura = props.housenumber ?: ""
                val ciudad = props.city ?: props.locality ?: ""
                val nombre = props.name ?: ""
                val pais = props.country ?: ""

                val esLugar = calle.isBlank() && nombre.isNotBlank()
                if (calle.isBlank() && !esLugar) return@mapNotNull null

                val partes = mutableListOf<String>()
                if (calle.isNotBlank()) {
                    partes.add(if (altura.isNotBlank()) "$calle $altura" else calle)
                } else {
                    partes.add(nombre)
                }
                if (ciudad.isNotBlank()) partes.add(ciudad)
                if (pais.isNotBlank()) partes.add(pais)

                val etiqueta = partes.joinToString(", ")
                if (etiqueta.isBlank()) null else AddressSuggestion(etiqueta, lat, lon)
            }
        } finally {
            connection.disconnect()
        }
    }
}