package com.proyecto_final.triage.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import com.proyecto_final.triage.viewmodels.HospitalConArribo
import com.proyecto_final.triage.viewmodels.HospitalesState
import com.proyecto_final.triage.viewmodels.HospitalesViewModel
import com.proyecto_final.triage.viewmodels.minutosHasta

data class Hospital(
    val nombre: String,
    val guardia: String,
    val distanciaKm: Double,
    val tiempoMin: Int,
    val personasEspera: Int,
    val tiempoEsperaMin: Int,
    val tiempoEsperaMax: Int,
    val linea: String,
    val paradaDesde: String,
    val esMasRecomendado: Boolean = false
)

class HospitalesScreen(private val type: String, private val ubicacion: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val viewModel = remember { HospitalesViewModel() }
        val state by viewModel.state.collectAsState()

        LaunchedEffect(ubicacion, type) {
            val (lat, lon) = ubicacion.split(",").map { it.trim().toDouble() }
            viewModel.cargarHospitalesCercanos(lat, lon, type)
        }

        when (val currentState = state) {
            is HospitalesState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF5BB8D4))
                }
            }

            is HospitalesState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currentState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            is HospitalesState.Success -> {
                HospitalesContent(
                    ubicacion = ubicacion,
                    hospitales = currentState.hospitales.map { it.toHospital() },
                    onBack = { navigator?.pop() },
                    onHospitalClick = { /* TODO: navegar a detalle */ }
                )
            }
        }
    }
}

// TODO: paradaDesde no existe en el back (CombinacionRutasDTO solo tiene nombreLinea)
// TODO: personasEspera, tiempoEsperaMin/Max, esMasRecomendado: falta definir de dónde salen en el back
private fun HospitalConArribo.toHospital(): Hospital {
    return Hospital(
        nombre = hospital.nombre,
        guardia = hospital.especialidades.joinToString(", ") { it.nombre }.ifBlank { "Guardia" },
        distanciaKm = (arribo?.distanciaMetros ?: 0) / 1000.0,
        tiempoMin = arribo?.tiempoEstimadoArribo?.let { minutosHasta(it) } ?: 0,
        personasEspera = 0,
        tiempoEsperaMin = 0,
        tiempoEsperaMax = 0,
        linea = arribo?.combinacionesLineas
            ?.joinToString(" → ") { it.nombreLinea }
            ?.ifBlank { "No disponible" }
            ?: "No disponible",
        paradaDesde = "",
        esMasRecomendado = false
    )
}

@Composable
fun HospitalesContent(
    ubicacion: String,
    hospitales: List<Hospital>,
    onBack: () -> Unit,
    onHospitalClick: (Hospital) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Volver",
            modifier = Modifier.clickable { onBack() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Hospitales cercanos",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Segun tu ubicación",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Info box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFE3F2FD))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = Color(0xFF5BB8D4),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Calculamos las mejores opciones para vos de acuerdo a la distancia y tiempo de espera en la guardia",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF5BB8D4)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (hospitales.isEmpty()) {
            Text(
                text = "No encontramos hospitales cercanos para esta ubicación.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        } else {
            hospitales.forEach { hospital ->
                HospitalCard(
                    hospital = hospital,
                    onClick = { onHospitalClick(hospital) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Info box abajo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFE3F2FD))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = Color(0xFF5BB8D4),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Los tiempos de espera son aproximados y pueden variar",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF5BB8D4)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun HospitalCard(hospital: Hospital, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = hospital.nombre,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = hospital.guardia,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (hospital.esMasRecomendado) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFF66BB6A))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Mas recomendado",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "${hospital.personasEspera} personas",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${hospital.tiempoEsperaMin}-${hospital.tiempoEsperaMax}min",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${hospital.distanciaKm}km",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(16.dp))
                Icon(
                    imageVector = Icons.Filled.AccessTime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${hospital.tiempoMin} min en transporte",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Card de transporte
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.DirectionsBus,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = hospital.linea,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (hospital.paradaDesde.isNotBlank()) {
                            Text(
                                text = hospital.paradaDesde,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}