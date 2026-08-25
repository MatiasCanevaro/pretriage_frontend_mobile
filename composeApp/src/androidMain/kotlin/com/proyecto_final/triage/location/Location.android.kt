package com.proyecto_final.triage.location

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.proyecto_final.triage.appContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual suspend fun obtenerUbicacionActual(): CurrentLocation? {

    val tienePermiso = ContextCompat.checkSelfPermission(
        appContext,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    if (!tienePermiso) {
        return null
    }

    val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(appContext)

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