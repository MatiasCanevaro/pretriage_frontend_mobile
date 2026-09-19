package com.proyecto_final.triage.location

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreLocation.CLGeocoder
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationCoordinate2D
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.NSError
import platform.Foundation.NSString
import platform.Foundation.NSNumber
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.net.URL
import java.net.URLEncoder

private val ACCURACY_THRESHOLD = 100.0
private val PHOTON_BBOX_CABA = "-58.5315,-34.7051,-58.3354,-34.5265"

actual suspend fun requestLocationPermission(): Boolean {
    return suspendCancellableCoroutine { continuation ->
        val locationManager = CLLocationManager.alloc().init()
        val status = CLLocationManager.authorizationStatus()

        when (status) {
            3, 4 -> {
                continuation.resume(true)
            }
            0 -> {
                locationManager.requestWhenInUseAuthorization()
                val observer = object : platform.CoreLocation.CLLocationManagerDelegate() {
                    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
                        val newStatus = CLLocationManager.authorizationStatus()
                        if (newStatus == 3 || newStatus == 4) {
                            continuation.resume(true)
                        } else if (newStatus == 2) {
                            continuation.resume(false)
                        }
                    }
                }
                locationManager.delegate = observer
            }
            else -> {
                continuation.resume(false)
            }
        }
    }
}

actual suspend fun getCurrentLocation(): CurrentLocation? {
    return suspendCancellableCoroutine { continuation ->
        val locationManager = CLLocationManager.alloc().init()
        locationManager.desiredAccuracy = kCLLocationAccuracyBest

        val hasPermission = CLLocationManager.authorizationStatus() == 3 || CLLocationManager.authorizationStatus() == 4
        if (!hasPermission) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        locationManager.requestLocation()
        val delegate = object : platform.CoreLocation.CLLocationManagerDelegate() {
            override fun locationManager(manager: CLLocationManager, didUpdateLocations: platform.Foundation.NSArray<out CLLocation>) {
                val location = didUpdateLocations.lastObject()
                if (location != null && location.horizontalAccuracy >= 0 && location.horizontalAccuracy <= ACCURACY_THRESHOLD) {
                    continuation.resume(CurrentLocation(location.coordinate.latitude, location.coordinate.longitude))
                } else if (location != null) {
                    continuation.resume(CurrentLocation(location.coordinate.latitude, location.coordinate.longitude))
                } else {
                    continuation.resume(null)
                }
            }

            override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
                continuation.resume(null)
            }
        }
        locationManager.delegate = delegate
    }
}

actual suspend fun geocodeAddress(query: String): CurrentLocation? {
    return withContext(Dispatchers.IO) {
        val geocoder = CLGeocoder.alloc().init()
        val encodedQuery = query

        suspendCancellableCoroutine<CurrentLocation?> { continuation ->
            geocoder.geocodeAddressString(encodedQuery) { placemarks, error ->
                if (error != null) {
                    continuation.resume(null)
                    return@geocodeAddressString
                }
                val placemark = placemarks?.firstOrNull()
                placemark?.location?.let { loc ->
                    continuation.resume(CurrentLocation(loc.coordinate.latitude, loc.coordinate.longitude))
                } ?: continuation.resume(null)
            }
        }
    }
}

actual suspend fun reverseGeocode(location: CurrentLocation): String? {
    return withContext(Dispatchers.IO) {
        val geocoder = CLGeocoder.alloc().init()
        val clLocation = CLLocation.alloc().initWithLatitude(
            location.latitude,
            location.longitude
        )

        suspendCancellableCoroutine<String?> { continuation ->
            geocoder.reverseGeocodeLocation(clLocation) { placemarks, error ->
                if (error != null) {
                    continuation.resume(null)
                    return@reverseGeocodeLocation
                }
                val placemark = placemarks?.firstOrNull()
                val address = placemark?.let { pm ->
                    val thoroughfare = pm.thoroughfare
                    val subThoroughfare = pm.subThoroughfare
                    when {
                        thoroughfare != null && subThoroughfare != null && thoroughfare.isNotEmpty() && subThoroughfare.isNotEmpty() ->
                            "$thoroughfare $subThoroughfare"
                        thoroughfare != null && thoroughfare.isNotEmpty() ->
                            thoroughfare
                        else ->
                            pm.name ?: ""
                    }
                }
                continuation.resume(address)
            }
        }
    }
}

@kotlinx.serialization.Serializable
private data class PhotonFeature(
    val properties: PhotonProperties,
    val geometry: PhotonGeometry
)

@kotlinx.serialization.Serializable
private data class PhotonProperties(
    val name: String? = null,
    val street: String? = null,
    val housenumber: String? = null,
    val city: String? = null,
    val locality: String? = null,
    val state: String? = null,
    val country: String? = null
)

@kotlinx.serialization.Serializable
private data class PhotonGeometry(
    val coordinates: List<Double>
)

@kotlinx.serialization.Serializable
private data class PhotonResponse(
    val features: List<PhotonFeature>
)

actual suspend fun fetchAddressSuggestions(query: String, near: CurrentLocation?): List<AddressSuggestion> {
    return withContext(Dispatchers.IO) {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        var urlStr = "https://photon.komoot.io/api/?q=$encodedQuery&limit=5&countrycode=AR&bbox=$PHOTON_BBOX_CABA"

        if (near != null) {
            urlStr += "&lat=${near.latitude}&lon=${near.longitude}&location_bias_scale=2"
        }

        val url = URL(urlStr)
        val connection = url.openConnection()
        connection.connectTimeout = 6000
        connection.readTimeout = 6000
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "TriageApp/1.0 (iOS)")

        try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                return@withContext emptyList()
            }

            val inputStream = connection.inputStream
            val body = inputStream.bufferedReader().use { it.readText() }
            inputStream.close()

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
            connection.inputStream?.close()
        }
    }
}