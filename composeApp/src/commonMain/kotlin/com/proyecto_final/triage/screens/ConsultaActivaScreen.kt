package com.proyecto_final.triage.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import com.proyecto_final.triage.network.estadoConsulta.EstadoConsultaPacienteDTO
import com.proyecto_final.triage.network.estadoConsulta.EstadoEntradaCola
import com.proyecto_final.triage.network.estadoConsulta.HospitalSeleccionadoResponse
import com.proyecto_final.triage.network.estadoConsulta.TiempoEstimadoAtencionResponse
import com.proyecto_final.triage.network.estadoConsulta.TipoPausaCola
import com.proyecto_final.triage.viewmodels.ConsultaActivaState
import com.proyecto_final.triage.viewmodels.ConsultaActivaViewModel
import com.proyecto_final.triage.viewmodels.estadoColaEnum
import com.proyecto_final.triage.viewmodels.tipoPausaEnum
import kotlinx.coroutines.delay
import com.proyecto_final.triage.platform.PlatformMap

private const val POLLING_INTERVAL_MS = 20_000L
private val ACCENT = Color(0xFF5BB8D4)

class ConsultaActivaScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val viewModel = remember { ConsultaActivaViewModel() }

        LaunchedEffect(Unit) {
            viewModel.cargarEstado()
        }

        // Polling silencioso mientras la pantalla está activa
        LaunchedEffect(Unit) {
            while (true) {
                delay(POLLING_INTERVAL_MS)
                viewModel.cargarEstado(mostrarLoading = false)
            }
        }

        DisposableEffect(Unit) {
            onDispose { viewModel.detenerActualizaciones() }
        }

        ConsultaActivaContent(
            state = viewModel.state,
            actionError = viewModel.actionError,
            // TODO: agregar `hospital: HospitalSeleccionadoResponse?` al ConsultaActivaViewModel
            // (mismo patrón que HomeViewModel.obtenerHospitalSeleccionado()) y exponerlo acá.
            hospital = viewModel.hospital,
            isAusentarmeLoading = viewModel.isAusentarmeLoading,
            isEstoyAtrasadoLoading = viewModel.isEstoyAtrasadoLoading,
            isSigoAsistiendoLoading = viewModel.isSigoAsistiendoLoading,
            isLlegueLoading = viewModel.isLlegueLoading,
            onBack = { navigator?.pop() },
            onRetry = { viewModel.cargarEstado() },
            onAusentarme = { viewModel.ausentarme() },
            onEstoyAtrasado = { viewModel.estoyAtrasado() },
            onSigoAsistiendo = { viewModel.sigoAsistiendo() },
            onLlegue = { viewModel.llegue() },
            // TODO: reemplazar por navegación real (deep link a Maps / abrir ChatScreen con consultaId)
            onComoLlegar = { },
            onChatInteractivo = { }
        )
    }
}

@Composable
fun ConsultaActivaContent(
    state: ConsultaActivaState,
    actionError: String?,
    hospital: HospitalSeleccionadoResponse?,
    isAusentarmeLoading: Boolean,
    isEstoyAtrasadoLoading: Boolean,
    isSigoAsistiendoLoading: Boolean,
    isLlegueLoading: Boolean,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onAusentarme: () -> Unit,
    onEstoyAtrasado: () -> Unit,
    onSigoAsistiendo: () -> Unit,
    onLlegue: () -> Unit,
    onComoLlegar: () -> Unit,
    onChatInteractivo: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(36.dp))

        // HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Volver",
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onBack() },
                tint = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Tu atención",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        when (state) {
            is ConsultaActivaState.Loading -> LoadingSection()
            is ConsultaActivaState.Error -> ErrorSection(state.message, onRetry)
            is ConsultaActivaState.Success -> {
                EstadoConsultaSection(
                    estado = state.estado,
                    hospital = hospital,
                    actionError = actionError,
                    isAusentarmeLoading = isAusentarmeLoading,
                    isEstoyAtrasadoLoading = isEstoyAtrasadoLoading,
                    isSigoAsistiendoLoading = isSigoAsistiendoLoading,
                    isLlegueLoading = isLlegueLoading,
                    onAusentarme = onAusentarme,
                    onEstoyAtrasado = onEstoyAtrasado,
                    onSigoAsistiendo = onSigoAsistiendo,
                    onLlegue = onLlegue,
                    onComoLlegar = onComoLlegar,
                    onChatInteractivo = onChatInteractivo
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun LoadingSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 64.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = ACCENT)
    }
}

@Composable
private fun ErrorSection(message: String, onRetry: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = Color(0xFFE57373),
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = ACCENT)) {
                Text("Reintentar")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Grid de opciones seleccionables (mismo diseño que en otras pantallas)
// ─────────────────────────────────────────────────────────────
data class SelectableOption(
    val label: String,
    val icon: ImageVector,
    val onClick: (() -> Unit)? = null
)

@Composable
fun SelectableOptionsGrid(
    options: List<SelectableOption>,
    selectedOption: String?,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.chunked(2).forEach { fila ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                fila.forEach { option ->
                    val seleccionado = selectedOption == option.label

                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .heightIn(min = 80.dp)
                            .background(
                                if (seleccionado) Color(0xFFBBDEFB)
                                else Color(0xFFE3F2FD)
                            )
                            .then(
                                if (seleccionado)
                                    Modifier.border(1.dp, Color(0xFF5BB8D4), RoundedCornerShape(12.dp))
                                else Modifier
                            )
                            .clickable {
                                option.onClick?.invoke() ?: onOptionSelected(option.label)
                            }
                            .padding(16.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = null,
                                tint = if (seleccionado) Color(0xFF5BB8D4)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = option.label,
                                color = if (seleccionado) Color(0xFF5BB8D4)
                                else MaterialTheme.colorScheme.onBackground,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2
                            )
                        }
                    }
                }

                // Si la fila quedó con un solo elemento, ocupamos el espacio restante
                if (fila.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LlamadoCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ACCENT.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.NotificationsActive,
                contentDescription = null,
                tint = ACCENT,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "¡Te están llamando!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Acercate a recepción o a la sala indicada.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EsperaManualCard(isLlegueLoading: Boolean, onLlegue: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Filled.PauseCircle, contentDescription = null, tint = ACCENT, modifier = Modifier.size(36.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("En pausa", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Te ausentaste temporalmente. Tenés hasta 60 minutos para volver antes de que se cancele tu turno.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onLlegue,
                enabled = !isLlegueLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = ACCENT)
            ) {
                if (isLlegueLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Llegué, volver a la cola")
                }
            }
        }
    }
}

@Composable
private fun AusenteAlLlamadoCard(isEstoyAtrasadoLoading: Boolean, onEstoyAtrasado: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Filled.WarningAmber, contentDescription = null, tint = Color(0xFFEF6C00), modifier = Modifier.size(36.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("No estabas cuando te llamamos", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Confirmá que seguís esperando para que no se cancele tu turno.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onEstoyAtrasado,
                enabled = !isEstoyAtrasadoLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF6C00))
            ) {
                if (isEstoyAtrasadoLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text("Estoy atrasado, sigo esperando")
                }
            }
        }
    }
}

@Composable
private fun AtrasadoCard(
    fechaHoraLimiteRespuesta: String?,
    isSigoAsistiendoLoading: Boolean,
    isLlegueLoading: Boolean,
    onSigoAsistiendo: () -> Unit,
    onLlegue: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Filled.AccessTime, contentDescription = null, tint = Color(0xFFEF6C00), modifier = Modifier.size(36.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Turno atrasado", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tenés tiempo hasta las ${formatHora(fechaHoraLimiteRespuesta)} para llegar o confirmar que seguís en camino.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onSigoAsistiendo,
                    enabled = !isSigoAsistiendoLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isSigoAsistiendoLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Sigo en camino")
                    }
                }
                Button(
                    onClick = onLlegue,
                    enabled = !isLlegueLoading,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = ACCENT)
                ) {
                    if (isLlegueLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Text("Ya llegué")
                    }
                }
            }
        }
    }
}

@Composable
private fun EnAtencionCard() {
    InfoCard(
        icon = Icons.Filled.MedicalServices,
        title = "Estás siendo atendido",
        subtitle = "Tu consulta está en curso."
    )
}

@Composable
private fun InfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = ACCENT, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatHora(iso: String?): String {
    if (iso.isNullOrBlank()) return "--:--"
    return try {
        val timePart = iso.substringAfter("T").substringBefore(".")
        timePart.substring(0, 5)
    } catch (e: Exception) {
        "--:--"
    }
}

@Composable
private fun EstimacionDato(
    label: String,
    valor: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = valor,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun EstadoConsultaSection(
    estado: EstadoConsultaPacienteDTO,
    hospital: HospitalSeleccionadoResponse?,
    actionError: String?,
    isAusentarmeLoading: Boolean,
    isEstoyAtrasadoLoading: Boolean,
    isSigoAsistiendoLoading: Boolean,
    isLlegueLoading: Boolean,
    onAusentarme: () -> Unit,
    onEstoyAtrasado: () -> Unit,
    onSigoAsistiendo: () -> Unit,
    onLlegue: () -> Unit,
    onComoLlegar: () -> Unit,
    onChatInteractivo: () -> Unit
) {
    val estadoCola = estado.estadoColaEnum()
    val tipoPausa = estado.tipoPausaEnum()

    if (actionError != null) {
        Card(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFDECEA)
            )
        ) {
            Text(
                text = actionError,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFC62828)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    when {
        // ─────────────────────────────────────
        // EN COLA
        // ─────────────────────────────────────
        estadoCola == EstadoEntradaCola.EN_COLA -> {

            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 8.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {

                    // HOSPITAL
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalHospital,
                            contentDescription = null,
                            tint = ACCENT,
                            modifier = Modifier.size(30.dp)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = hospital?.nombre ?: "Hospital",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Text(
                                text = hospital?.direccion
                                    ?.substringBefore(",")
                                    ?: "Dirección no disponible",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // MAPA
                    PlatformMap(
                        hospital = hospital
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // ESTÁS EN LA COLA
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Groups,
                            contentDescription = null,
                            tint = ACCENT,
                            modifier = Modifier.size(32.dp)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = "Estás en la cola",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // INFORMACIÓN DE LA COLA
                    val tiempoEstimado = estado.tiempoEstimadoAtencion

                    if (tiempoEstimado != null) {

                        if (tiempoEstimado.hayMedicosActivos) {

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        ACCENT.copy(alpha = 0.10f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(vertical = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Tiempo de espera estimado",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AccessTime,
                                        contentDescription = null,
                                        tint = ACCENT,
                                        modifier = Modifier.size(22.dp)
                                    )

                                    Spacer(modifier = Modifier.width(6.dp))

                                    Text(
                                        text = "${tiempoEstimado.minutosPromedioAtencion} min",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }

                                if (!tiempoEstimado.fechaHoraAtencionEstimada.isNullOrBlank()) {
                                    Text(
                                        text = "Alrededor de las ${
                                            formatHora(
                                                tiempoEstimado.fechaHoraAtencionEstimada
                                            )
                                        }",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // PACIENTES ANTES / POSICIÓN
                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            EstimacionDato(
                                label = "Pacientes antes",
                                valor = "${tiempoEstimado.pacientesAntes}",
                                modifier = Modifier.weight(1f)
                            )

                            EstimacionDato(
                                label = "Tu posición",
                                valor = "#${tiempoEstimado.posicionEnCola}",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (!tiempoEstimado.hayMedicosActivos) {
                            Spacer(modifier = Modifier.height(12.dp))

                            tiempoEstimado.mensaje?.let {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.WarningAmber,
                                        contentDescription = null,
                                        tint = Color(0xFFFFA726),
                                        modifier = Modifier.size(18.dp)
                                    )

                                    Spacer(modifier = Modifier.width(4.dp))

                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFFFA726)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        Button(
                            onClick = onComoLlegar,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFBBDEFB),
                                contentColor = Color(0xFF6B7280)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Directions,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            Text("Cómo llegar")
                        }

                        Button(
                            onClick = onChatInteractivo,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFBBDEFB),
                                contentColor = Color(0xFF6B7280)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Chat,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            Text("Chat")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // AUSENTARME
                    OutlinedButton(
                        onClick = onAusentarme,
                        enabled = !isAusentarmeLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isAusentarmeLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.ExitToApp,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text("Ausentarme un momento")
                        }
                    }
                }
            }
        }

        // LLAMADO
        estadoCola == EstadoEntradaCola.LLAMADO -> {
            LlamadoCard()
        }

        // ESPERA MANUAL
        estadoCola == EstadoEntradaCola.EN_ESPERA &&
                tipoPausa == TipoPausaCola.ESPERA_MANUAL -> {
            EsperaManualCard(
                isLlegueLoading = isLlegueLoading,
                onLlegue = onLlegue
            )
        }

        // AUSENTE AL LLAMADO
        estadoCola == EstadoEntradaCola.EN_ESPERA &&
                tipoPausa == TipoPausaCola.AUSENTE_AL_LLAMADO -> {
            AusenteAlLlamadoCard(
                isEstoyAtrasadoLoading = isEstoyAtrasadoLoading,
                onEstoyAtrasado = onEstoyAtrasado
            )
        }

        // ATRASADO
        estadoCola == EstadoEntradaCola.ATRASADO -> {
            AtrasadoCard(
                fechaHoraLimiteRespuesta = estado.fechaHoraLimiteRespuesta,
                isSigoAsistiendoLoading = isSigoAsistiendoLoading,
                isLlegueLoading = isLlegueLoading,
                onSigoAsistiendo = onSigoAsistiendo,
                onLlegue = onLlegue
            )
        }

        // EN ATENCIÓN
        estadoCola == EstadoEntradaCola.EN_ATENCION -> {
            EnAtencionCard()
        }

        // FINALIZADA / CANCELADA
        estadoCola == EstadoEntradaCola.FINALIZADA ||
                estadoCola == EstadoEntradaCola.CANCELADA -> {
            InfoCard(
                icon = Icons.Filled.CheckCircle,
                title = "Consulta finalizada",
                subtitle = "Esta atención ya no está activa."
            )
        }

        // SIN TURNO
        else -> {
            InfoCard(
                icon = Icons.Filled.HourglassEmpty,
                title = "Sin turno en cola",
                subtitle = "Todavía no ingresaste a la cola de espera de un hospital."
            )
        }
    }
}