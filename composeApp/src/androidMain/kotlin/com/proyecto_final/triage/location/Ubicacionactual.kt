package com.proyecto_final.triage.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

sealed interface UbicacionState {
    data object Cargando : UbicacionState
    data class Lista(val latitud: Double, val longitud: Double) : UbicacionState

    // permanente = el usuario rechazó con "No volver a preguntar" (o dos veces).
    // En ese caso Android ya no muestra el diálogo y hay que ir a Ajustes.
    data class PermisoDenegado(val permanente: Boolean) : UbicacionState
    data class Error(val mensaje: String) : UbicacionState
}

class UbicacionController internal constructor(
    val state: UbicacionState,
    val reintentar: () -> Unit
)

private val PERMISOS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

private const val MENSAJE_SIN_UBICACION =
    "No pudimos obtener tu ubicación. Revisá que la ubicación del dispositivo esté activada."

/**
 * Cada vez que la pantalla entra en composición:
 *  1. Verifica si el permiso está concedido.
 *  2. Si no, lo pide.
 *  3. Si está concedido, obtiene la ubicación actual.
 *
 * reintentar():
 *  - Si ahora hay permiso (por ej. lo activó desde Ajustes) → busca la ubicación.
 *  - Si fue rechazado de forma permanente → abre los ajustes de la app.
 *  - Si no → vuelve a mostrar el diálogo de permisos.
 */
@Composable
fun rememberUbicacionActual(): UbicacionController {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    var state by remember { mutableStateOf<UbicacionState>(UbicacionState.Cargando) }

    val obtener: () -> Unit = {
        state = UbicacionState.Cargando
        obtenerUbicacion(context) { resultado -> state = resultado }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultado ->
        if (resultado.values.any { it }) {
            obtener()
        } else {
            val permanente = activity != null &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(
                        activity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
            state = UbicacionState.PermisoDenegado(permanente)
        }
    }

    val iniciar: () -> Unit = {
        val actual = state
        when {
            tienePermiso(context) -> obtener()

            actual is UbicacionState.PermisoDenegado && actual.permanente ->
                abrirAjustesApp(context)

            else -> {
                state = UbicacionState.Cargando
                launcher.launch(PERMISOS)
            }
        }
    }

    // Siempre que se entra a la pantalla, se verifica el permiso antes de cargar
    LaunchedEffect(Unit) {
        iniciar()
    }

    return UbicacionController(state = state, reintentar = iniciar)
}

private fun tienePermiso(context: Context): Boolean =
    PERMISOS.any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

@SuppressLint("MissingPermission") // el permiso se verifica antes de llamar
private fun obtenerUbicacion(context: Context, onResultado: (UbicacionState) -> Unit) {
    val client = LocationServices.getFusedLocationProviderClient(context)

    try {
        // Pide la ubicación ACTUAL (lastLocation sola puede ser null, sobre todo en el emulador)
        client.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            CancellationTokenSource().token
        )
            .addOnSuccessListener { location ->
                if (location != null) {
                    onResultado(UbicacionState.Lista(location.latitude, location.longitude))
                } else {
                    // Respaldo: última ubicación conocida
                    client.lastLocation
                        .addOnSuccessListener { ultima ->
                            onResultado(
                                if (ultima != null) {
                                    UbicacionState.Lista(ultima.latitude, ultima.longitude)
                                } else {
                                    UbicacionState.Error(MENSAJE_SIN_UBICACION)
                                }
                            )
                        }
                        .addOnFailureListener {
                            onResultado(UbicacionState.Error(MENSAJE_SIN_UBICACION))
                        }
                }
            }
            .addOnFailureListener {
                onResultado(UbicacionState.Error(MENSAJE_SIN_UBICACION))
            }
    } catch (_: SecurityException) {
        onResultado(UbicacionState.PermisoDenegado(permanente = false))
    }
}

private fun abrirAjustesApp(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}