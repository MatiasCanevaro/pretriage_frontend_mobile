package com.proyecto_final.triage.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import com.google.android.gms.location.LocationServices
import com.proyecto_final.triage.components.CommonHeader
import com.proyecto_final.triage.theme.Spacing
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import android.location.Geocoder
import androidx.compose.foundation.border
import com.proyecto_final.triage.components.ProgressBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

private data class SugerenciaDireccion(
    val etiqueta: String,
    val lat: Double,
    val lon: Double
)

actual fun getSelectLocationScreen(type: String): Screen = SelectLocationScreen(type)

class SelectLocationScreen(private val type: String) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current

        SelectLocationContent(
            type = type,
            onBack = { navigator?.pop() },
            onContinue = { type, ubicacion ->
                navigator?.push(HospitalesScreen(type, ubicacion))
            }
        )
    }
}

@SuppressLint("MissingPermission")
@Composable
fun SelectLocationContent(
    type: String,
    onBack: () -> Unit,
    onContinue: (String, String) -> Unit
) {

    var usarUbicacionActual by remember { mutableStateOf(true) }
    var direccion by remember { mutableStateOf("") }
    var cargandoUbicacion by remember { mutableStateOf(true) }

    // Dirección legible de la ubicación actual (calle y altura), obtenida por geocoding inverso
    var direccionActualLegible by remember { mutableStateOf("") }

    // Estado de la búsqueda manual de dirección
    var buscandoDireccion by remember { mutableStateOf(false) }
    var errorBusquedaDireccion by remember { mutableStateOf<String?>(null) }
    var direccionEncontrada by remember { mutableStateOf<String?>(null) }

    // Estado del autocompletado (sugerencias mientras el usuario escribe)
    var sugerencias by remember { mutableStateOf<List<SugerenciaDireccion>>(emptyList()) }
    var cargandoSugerencias by remember { mutableStateOf(false) }
    var errorSugerencias by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    userLocation = LatLng(it.latitude, it.longitude)
                }
                cargandoUbicacion = false
            }
        } else {
            cargandoUbicacion = false
        }
    }

    // Geocoding: dirección -> lat/lng (usada por "Buscar dirección")
    suspend fun geocodificarDireccion(query: String): LatLng? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null
        val geocoder = Geocoder(context, Locale.getDefault())
        try {
            @Suppress("DEPRECATION")
            val resultados = geocoder.getFromLocationName(query, 1)
            resultados?.firstOrNull()?.let { LatLng(it.latitude, it.longitude) }
        } catch (e: Exception) {
            println("ERROR GEOCODER (directo): ${e.message}")
            null
        }
    }

    // Geocoding inverso: lat/lng -> dirección legible (calle y altura)
    suspend fun direccionDesdeUbicacion(location: LatLng): String? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null
        val geocoder = Geocoder(context, Locale.getDefault())
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
            println("ERROR GEOCODER (inverso): ${e.message}")
            null
        }
    }

    fun buscarDireccion(query: String) {
        if (query.isBlank()) return
        buscandoDireccion = true
        errorBusquedaDireccion = null
        direccionEncontrada = null
    }

    LaunchedEffect(buscandoDireccion) {
        if (!buscandoDireccion) return@LaunchedEffect
        val resultado = geocodificarDireccion(direccion)
        if (resultado != null) {
            userLocation = resultado
            direccionEncontrada = direccion
        } else {
            errorBusquedaDireccion = "No pudimos encontrar esa dirección. Probá con calle, altura y localidad."
        }
        buscandoDireccion = false
    }

    // Autocompletado: consulta a Photon (photon.komoot.io), gratis y sin API key.
    // Prioriza resultados cerca de "cerca" si tenemos una ubicación de referencia.
    // Lanza excepción con detalle en vez de tragarse el error, para poder mostrarlo.
    suspend fun buscarSugerencias(query: String, cerca: LatLng?): List<SugerenciaDireccion> =
        withContext(Dispatchers.IO) {
            val q = URLEncoder.encode(query, "UTF-8")
            // Esta instancia de Photon soporta countrycode (ISO 3166-1 alpha-2) y bbox.
            // countrycode=AR + bbox de CABA para restringir a Ciudad Autónoma de Buenos Aires.
            // bbox = minLon,minLat,maxLon,maxLat
            // Ojo: también soporta lang=default|de|en|fr (NO "es").
            val bboxCABA = "-58.5315,-34.7051,-58.3354,-34.5265"
            var urlStr = "https://photon.komoot.io/api/?q=$q&limit=5&countrycode=AR&bbox=$bboxCABA"
            if (cerca != null) {
                urlStr += "&lat=${cerca.latitude}&lon=${cerca.longitude}&location_bias_scale=2"
            }
            val connection = URL(urlStr).openConnection() as HttpURLConnection
            connection.connectTimeout = 6000
            connection.readTimeout = 6000
            connection.requestMethod = "GET"
            // Algunos servicios públicos rechazan pedidos sin User-Agent identificable
            connection.setRequestProperty("User-Agent", "TriageApp/1.0 (Android)")

            try {
                val codigo = connection.responseCode
                if (codigo !in 200..299) {
                    val detalle = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    throw java.io.IOException("Photon respondió $codigo: $detalle")
                }

                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val features = json.optJSONArray("features") ?: return@withContext emptyList()

                (0 until features.length()).mapNotNull { i ->
                    val feature = features.getJSONObject(i)
                    val props = feature.optJSONObject("properties") ?: return@mapNotNull null
                    val geometry = feature.optJSONObject("geometry") ?: return@mapNotNull null
                    val coords = geometry.optJSONArray("coordinates") ?: return@mapNotNull null
                    val lon = coords.getDouble(0)
                    val lat = coords.getDouble(1)

                    val provincia = props.optString("state", "")
                    if (provincia.isNotBlank() &&
                        !provincia.contains("Autónoma", ignoreCase = true) &&
                        !provincia.contains("Autonoma", ignoreCase = true)
                    ) {
                        return@mapNotNull null
                    }

                    val calle = props.optString("street", "")
                    val altura = props.optString("housenumber", "")
                    val ciudad = props.optString("city", props.optString("locality", ""))
                    val nombre = props.optString("name", "")
                    val pais = props.optString("country", "")

                    // Si no hay calle ni nombre de lugar, no es una sugerencia útil
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
                    if (etiqueta.isBlank()) null else SugerenciaDireccion(etiqueta, lat, lon)
                }
            } finally {
                connection.disconnect()
            }
        }

    // Dispara la búsqueda de sugerencias con debounce mientras el usuario escribe
    LaunchedEffect(direccion, usarUbicacionActual) {
        if (usarUbicacionActual || direccion.isBlank() || direccion.length < 3 || direccionEncontrada != null) {
            sugerencias = emptyList()
            cargandoSugerencias = false
            errorSugerencias = null
            return@LaunchedEffect
        }
        cargandoSugerencias = true
        errorSugerencias = null
        delay(400) // debounce: espera a que el usuario deje de tipear
        try {
            sugerencias = buscarSugerencias(direccion, userLocation)
            if (sugerencias.isEmpty()) {
                errorSugerencias = "Sin resultados"
            }
        } catch (e: Exception) {
            println("ERROR AUTOCOMPLETE: ${e.javaClass.simpleName}: ${e.message}")
            sugerencias = emptyList()
            errorSugerencias = "No pudimos buscar sugerencias. Revisá tu conexión."
        }
        cargandoSugerencias = false
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    userLocation = LatLng(location.latitude, location.longitude)
                }
                cargandoUbicacion = false
            }
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Cuando cambia la ubicación actual (y estamos usando "mi ubicación actual"),
    // resolvemos la dirección legible para mostrarla en vez de "Ubicación detectada"
    LaunchedEffect(userLocation, usarUbicacionActual) {
        if (usarUbicacionActual) {
            val location = userLocation
            if (location != null) {
                direccionActualLegible = direccionDesdeUbicacion(location) ?: ""
            }
        }
    }

    val mapView = remember {
        MapView(context).apply {
            onCreate(null)
        }
    }

    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }

    DisposableEffect(mapView) {
        mapView.getMapAsync { map ->
            map.setStyle("https://tiles.openfreemap.org/styles/liberty") {
                mapLibreMap = map
                map.uiSettings.isLogoEnabled = false
                map.uiSettings.isAttributionEnabled = false
                map.uiSettings.isScrollGesturesEnabled = false
                map.uiSettings.isZoomGesturesEnabled = false
                map.uiSettings.isRotateGesturesEnabled = false
                map.uiSettings.isTiltGesturesEnabled = false
                map.uiSettings.isDoubleTapGesturesEnabled = false
                map.uiSettings.isQuickZoomGesturesEnabled = false
            }
        }

        onDispose {
            mapView.onDestroy()
        }
    }

    LaunchedEffect(userLocation, mapLibreMap) {
        val location = userLocation ?: return@LaunchedEffect
        val map = mapLibreMap ?: return@LaunchedEffect

        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(location, 15.0)
        )

        map.getStyle { style ->
            val sourceId = "user-location-source"
            val radiusLayerId = "user-location-circle"
            val dotLayerId = "user-location-dot"

            val geoJson = """
                {
                    "type": "FeatureCollection",
                    "features": [{
                        "type": "Feature",
                        "geometry": {
                            "type": "Point",
                            "coordinates": [${location.longitude}, ${location.latitude}]
                        }
                    }]
                }
            """.trimIndent()

            val existingSource = style.getSourceAs<GeoJsonSource>(sourceId)
            if (existingSource != null) {
                existingSource.setGeoJson(geoJson)
            } else {
                style.addSource(GeoJsonSource(sourceId, geoJson))

                val radiusLayer = CircleLayer(radiusLayerId, sourceId).withProperties(
                    PropertyFactory.circleRadius(50f),
                    PropertyFactory.circleColor(Color(0xFF5BB8D4).toArgb()),
                    PropertyFactory.circleOpacity(0.3f),
                    PropertyFactory.circleStrokeColor(Color(0xFF5BB8D4).toArgb()),
                    PropertyFactory.circleStrokeWidth(2f)
                )
                style.addLayer(radiusLayer)

                val dotLayer = CircleLayer(dotLayerId, sourceId).withProperties(
                    PropertyFactory.circleRadius(8f),
                    PropertyFactory.circleColor(Color(0xFF5BB8D4).toArgb()),
                    PropertyFactory.circleOpacity(1f),
                    PropertyFactory.circleStrokeColor(Color.White.toArgb()),
                    PropertyFactory.circleStrokeWidth(2f)
                )
                style.addLayer(dotLayer)
            }
        }
    }

    val mapaUbicacion = remember {
        movableContentOf {
            AndroidView(
                factory = { mapView },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        CommonHeader(title = "2. Ingresá tu ubicación",
                        subtitle = "Usaremos tu ubicación para mostrarte las mejores opciones",
                        showLogo = true,
                        onBack = { onBack() })

        ProgressBar(currentStep = 2, totalSteps = 3)

        Spacer(modifier = Modifier.height(Spacing.lg))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(16.dp))
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { usarUbicacionActual = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = if (usarUbicacionActual) Color(0xFF5BB8D4) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Usar mi ubicación actual",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (cargandoUbicacion) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF5BB8D4)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Obteniendo ubicación...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Text(
                                text = direccionActualLegible.ifBlank { "Ubicación detectada" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    RadioButton(
                        selected = usarUbicacionActual,
                        onClick = { usarUbicacionActual = true },
                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF5BB8D4))
                    )
                }

                if (usarUbicacionActual) {
                    mapaUbicacion()
                }

                HorizontalDivider(
                    color = Color(0xFFE0E0E0),
                    thickness = 1.dp
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { usarUbicacionActual = false }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = if (!usarUbicacionActual) Color(0xFF5BB8D4) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Ingresar otra dirección",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    RadioButton(
                        selected = !usarUbicacionActual,
                        onClick = { usarUbicacionActual = false },
                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF5BB8D4))
                    )
                }

                if (!usarUbicacionActual) {

                    OutlinedTextField(
                        value = direccion,
                        onValueChange = {
                            direccion = it
                            // si el usuario edita el texto, invalidamos el resultado anterior
                            errorBusquedaDireccion = null
                            errorSugerencias = null
                            direccionEncontrada = null
                        },
                        label = { Text("Dirección") },
                        singleLine = true,
                        isError = errorBusquedaDireccion != null,
                        trailingIcon = {
                            if (cargandoSugerencias || buscandoDireccion) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF5BB8D4)
                                )
                            } else {
                                IconButton(
                                    onClick = { buscarDireccion(direccion) },
                                    enabled = direccion.isNotBlank()
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = "Buscar dirección",
                                        tint = if (direccion.isNotBlank())
                                            Color(0xFF5BB8D4)
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp)
                    )

                    if (sugerencias.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(12.dp)
                                )
                        ) {
                            sugerencias.forEach { sugerencia ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            direccion = sugerencia.etiqueta
                                            userLocation = LatLng(sugerencia.lat, sugerencia.lon)
                                            direccionEncontrada = sugerencia.etiqueta
                                            errorBusquedaDireccion = null
                                            sugerencias = emptyList()
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = sugerencia.etiqueta,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    } else if (errorSugerencias != null && !cargandoSugerencias) {
                        Text(
                            text = errorSugerencias ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }

                    if (errorBusquedaDireccion != null) {
                        Text(
                            text = errorBusquedaDireccion ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    } else if (direccionEncontrada != null) {
                        Text(
                            text = "Dirección encontrada ✓",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    mapaUbicacion()
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            enabled = if (usarUbicacionActual) {
                userLocation != null
            } else {
                direccion.isNotBlank() && direccionEncontrada != null
            },
            onClick = {
                onContinue(type, "${userLocation?.latitude},${userLocation?.longitude}")
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(54.dp),
        ) {
            Text("Continuar", color = Color.White)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}