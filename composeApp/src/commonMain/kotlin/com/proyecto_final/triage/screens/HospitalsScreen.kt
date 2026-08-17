package com.proyecto_final.triage.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import com.proyecto_final.triage.components.CommonHeader
import com.proyecto_final.triage.components.ErrorBanner
import com.proyecto_final.triage.components.ProgressBar
import com.proyecto_final.triage.network.EspecialidadMedicaDTO
import com.proyecto_final.triage.network.HospitalCercanoDTO
import com.proyecto_final.triage.theme.Spacing
import com.proyecto_final.triage.viewmodels.HospitalesState
import com.proyecto_final.triage.viewmodels.HospitalesViewModel

class HospitalesScreen(private val type: String, private val ubicacion: String) : Screen {
    @Composable
    override fun Content() {

        val navigator = LocalNavigator.current
        val viewModel = remember { HospitalesViewModel() }
        val state = viewModel.state

        var selectedHospital by remember { mutableStateOf<Hospital?>(null) }

        // ENVIO LA REQUEST PARA BUSCAR HOSPITALES
        LaunchedEffect(ubicacion, type) {
            val (lat, lon) = ubicacion.split(",").map { it.trim().toDouble() }
            viewModel.buscarHospitalesCercanos(latitud = lat, longitud = lon, codigoEspecialidad = type)
        }

        LaunchedEffect(viewModel.hospitalSeleccionado) {
            if (viewModel.hospitalSeleccionado) {
                navigator?.popUntilRoot()
            }
        }

        when (state) {

            is HospitalesState.Loading -> {
                Box( modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF5BB8D4))
                }
            }

            is HospitalesState.Error -> {
                ErrorBanner( message = state.mensaje,
                             buttonText = "Reintentar",
                             icon = Icons.Filled.ErrorOutline,
                             onButtonClick = {
                                 val (lat, lon) = ubicacion.split(",").map { it.trim().toDouble() }
                                 viewModel.buscarHospitalesCercanos(
                                     latitud = lat,
                                     longitud = lon,
                                     codigoEspecialidad = type
                                 )
                    }
                )
            }

            is HospitalesState.Success -> {
                HospitalesContent( hospitales = state.hospitales.map { it.toHospital() },
                                   selectedHospital = selectedHospital,
                                   onBack = { navigator?.pop() },
                                   onHospitalSelected = { hospital -> selectedHospital = hospital },
                                   onContinue = {
                                        selectedHospital?.let { hospital ->
                                            viewModel.seleccionarHospital(
                                                hospital = hospital,
                                                codigoEspecialidad = type
                                            )
                                        }
                                    }
                )
            }
        }
    }
}

@Composable
fun HospitalesContent(
    hospitales: List<Hospital>,
    selectedHospital: Hospital?,
    onBack: () -> Unit,
    onHospitalSelected: (Hospital) -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {

        CommonHeader(
            title = "3. Elegí el hospital",
            subtitle = "Calculamos las mejores opciones para vos",
            showLogo = true,
            onBack = onBack
        )

        ProgressBar(
            currentStep = 3,
            totalSteps = 3
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        // SOLO ESTA PARTE SCROLLEA
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {

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
                        selected = selectedHospital?.idHospital == hospital.idHospital,
                        onClick = {
                            onHospitalSelected(hospital)
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        // STICKY
        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onContinue,
            enabled = selectedHospital != null,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Text("Continuar")
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun HospitalCard(
    hospital: Hospital,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) {
                Color(0xFF5BB8D4)
            } else {
                Color(0xFFD6D6D6)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            // Nombre del hospital
            Text(
                text = hospital.nombre,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Línea divisora
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = Color(0xFFE0E0E0),
                thickness = 1.dp
            )

            val direccionCorta = hospital.direccion.substringBefore(",")

            println("DIRECCION COMPLETA: ${hospital.direccion}")
            println("DIRECCION MOSTRADA: $direccionCorta")

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Row(
                    modifier = Modifier.weight(1.4f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = direccionCorta,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }

// Tiempo estimado - derecha
                hospital.tiempoEstimadoArriboMejorRuta?.let { tiempo ->

                    Row(
                        modifier = Modifier.weight(0.8f),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(modifier = Modifier.width(5.dp))

                        Text(
                            text = tiempo,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Especialidades
            Text(
                text = "Especialidades",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                hospital.especialidades.forEach { especialidad ->

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFA8D5A2)
                    ) {
                        Text(
                            text = especialidad.nombre,
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(
                                horizontal = 12.dp,
                                vertical = 6.dp
                            )
                        )
                    }
                }
            }
        }
    }
}

data class Hospital(
    val idHospital: Long,
    val placeId: String?,
    val nombre: String,
    val direccion: String,
    val especialidades: List<EspecialidadMedicaDTO>,
    val tiempoEstimadoArriboMejorRuta: String?
)

private fun HospitalCercanoDTO.toHospital(): Hospital {
    return Hospital(
        idHospital = idHospital,
        placeId = placeId,
        nombre = nombre,
        direccion = direccion,
        especialidades = especialidades,
        tiempoEstimadoArriboMejorRuta = tiempoEstimadoArriboMejorRuta
    )
}