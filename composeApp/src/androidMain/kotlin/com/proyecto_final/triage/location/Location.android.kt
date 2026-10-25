package com.proyecto_final.triage.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.proyecto_final.triage.appContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@SuppressLint("MissingPermission") // el permiso se verifica antes
actual suspend fun obtenerUbicacionActual(): CurrentLocation? {

    // Alcanza con cualquiera de los dos (precisa o aproximada)
    val tienePermiso = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ).any {
        ContextCompat.checkSelfPermission(appContext, it) == PackageManager.PERMISSION_GRANTED
    }

    if (!tienePermiso) return null

    val client = LocationServices.getFusedLocationProviderClient(appContext)

    return suspendCancellableCoroutine { continuation ->

        val cancellationToken = CancellationTokenSource()
        continuation.invokeOnCancellation { cancellationToken.cancel() }

        // Ubicación ACTUAL; lastLocation queda como respaldo
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationToken.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    continuation.resume(CurrentLocation(location.latitude, location.longitude))
                } else {
                    client.lastLocation
                        .addOnSuccessListener { ultima ->
                            continuation.resume(
                                ultima?.let { CurrentLocation(it.latitude, it.longitude) }
                            )
                        }
                        .addOnFailureListener { continuation.resume(null) }
                }
            }
            .addOnFailureListener { continuation.resume(null) }
    }
}