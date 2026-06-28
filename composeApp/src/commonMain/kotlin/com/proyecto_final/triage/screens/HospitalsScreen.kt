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

data class Hospital(
    val nombre: String,
    val guardia: String,
    val distanciaKm: Double,
    val tiempoMin: Int,
    val personasEspera: Int,
    val tiempoEsperaMin: Int,
    val tiempoEsperaMax: Int,
    val transporte: String,
    val linea: String,
    val paradaDesde: String,
    val esMasRecomendado: Boolean = false
)

class HospitalesScreen(
    private val ubicacion: String,
    private val transporte: String
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current

        // Función preparada para conectar con el back
        val hospitales = obtenerHospitalesEstaticos(transporte)

        HospitalesContent(
            ubicacion = ubicacion,
            transporte = transporte,
            hospitales = hospitales,
            onBack = { navigator?.pop() },
            onHospitalClick = { /* TODO: navegar a detalle */ }
        )
    }
}

// Función lista para reemplazar con llamada al back
fun obtenerHospitalesEstaticos(transporte: String): List<Hospital> {
    return listOf(
        Hospital(
            nombre = "Hospital Italiano",
            guardia = "Guardia - Clínica Médica",
            distanciaKm = 1.5,
            tiempoMin = 15,
            personasEspera = 8,
            tiempoEsperaMin = 20,
            tiempoEsperaMax = 30,
            transporte = transporte,
            linea = "Colectivo 59",
            paradaDesde = "Desde Av. Santa fe y Pueyrredón",
            esMasRecomendado = true
        ),
        Hospital(
            nombre = "Hospital Fernandez",
            guardia = "Guardia - Clínica Médica",
            distanciaKm = 2.8,
            tiempoMin = 25,
            personasEspera = 15,
            tiempoEsperaMin = 40,
            tiempoEsperaMax = 70,
            transporte = transporte,
            linea = "Colectivo 29",
            paradaDesde = "Desde Av. Santa fe y Pueyrredón"
        ),
        Hospital(
            nombre = "Hospital Británico",
            guardia = "Guardia - Clínica Médica",
            distanciaKm = 3.2,
            tiempoMin = 30,
            personasEspera = 20,
            tiempoEsperaMin = 50,
            tiempoEsperaMax = 90,
            transporte = transporte,
            linea = "Colectivo 12",
            paradaDesde = "Desde Av. Santa fe y Pueyrredón"
        )
    )
}

@Composable
fun HospitalesContent(
    ubicacion: String,
    transporte: String,
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
            text = "Segun tu ubicación y viaje en $transporte",
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

        // Lista de hospitales
        hospitales.forEach { hospital ->
            HospitalCard(
                hospital = hospital,
                onClick = { onHospitalClick(hospital) }
            )
            Spacer(modifier = Modifier.height(12.dp))
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
                        Text(
                            text = hospital.paradaDesde,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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