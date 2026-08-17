package com.proyecto_final.triage.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import com.proyecto_final.triage.network.EstadoConsultaPacienteDTO
import com.proyecto_final.triage.network.EstadoEntradaCola
import com.proyecto_final.triage.network.TiempoEstimadoAtencionResponse
import com.proyecto_final.triage.network.TipoPausaCola
import com.proyecto_final.triage.viewmodels.ConsultaActivaState
import com.proyecto_final.triage.viewmodels.ConsultaActivaViewModel
import com.proyecto_final.triage.viewmodels.estadoColaEnum
import com.proyecto_final.triage.viewmodels.tipoPausaEnum
import kotlinx.coroutines.delay

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
            isAusentarmeLoading = viewModel.isAusentarmeLoading,
            isEstoyAtrasadoLoading = viewModel.isEstoyAtrasadoLoading,
            isSigoAsistiendoLoading = viewModel.isSigoAsistiendoLoading,
            isLlegueLoading = viewModel.isLlegueLoading,
            onBack = { navigator?.pop() },
            onRetry = { viewModel.cargarEstado() },
            onAusentarme = { viewModel.ausentarme() },
            onEstoyAtrasado = { viewModel.estoyAtrasado() },
            onSigoAsistiendo = { viewModel.sigoAsistiendo() },
            onLlegue = { viewModel.llegue() }
        )
    }
}

@Composable
fun ConsultaActivaContent(
    state: ConsultaActivaState,
    actionError: String?,
    isAusentarmeLoading: Boolean,
    isEstoyAtrasadoLoading: Boolean,
    isSigoAsistiendoLoading: Boolean,
    isLlegueLoading: Boolean,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onAusentarme: () -> Unit,
    onEstoyAtrasado: () -> Unit,
    onSigoAsistiendo: () -> Unit,
    onLlegue: () -> Unit
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
                    actionError = actionError,
                    isAusentarmeLoading = isAusentarmeLoading,
                    isEstoyAtrasadoLoading = isEstoyAtrasadoLoading,
                    isSigoAsistiendoLoading = isSigoAsistiendoLoading,
                    isLlegueLoading = isLlegueLoading,
                    onAusentarme = onAusentarme,
                    onEstoyAtrasado = onEstoyAtrasado,
                    onSigoAsistiendo = onSigoAsistiendo,
                    onLlegue = onLlegue
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

@Composable
private fun EstadoConsultaSection(
    estado: EstadoConsultaPacienteDTO,
    actionError: String?,
    isAusentarmeLoading: Boolean,
    isEstoyAtrasadoLoading: Boolean,
    isSigoAsistiendoLoading: Boolean,
    isLlegueLoading: Boolean,
    onAusentarme: () -> Unit,
    onEstoyAtrasado: () -> Unit,
    onSigoAsistiendo: () -> Unit,
    onLlegue: () -> Unit
) {
    val estadoCola = estado.estadoColaEnum()
    val tipoPausa = estado.tipoPausaEnum()

    if (actionError != null) {
        Card(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFDECEA))
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
        estadoCola == EstadoEntradaCola.EN_COLA -> EnColaCard(
            tiempoEstimado = estado.tiempoEstimadoAtencion,
            isAusentarmeLoading = isAusentarmeLoading,
            onAusentarme = onAusentarme
        )

        estadoCola == EstadoEntradaCola.LLAMADO -> LlamadoCard()

        estadoCola == EstadoEntradaCola.EN_ESPERA && tipoPausa == TipoPausaCola.ESPERA_MANUAL ->
            EsperaManualCard(isLlegueLoading = isLlegueLoading, onLlegue = onLlegue)

        estadoCola == EstadoEntradaCola.EN_ESPERA && tipoPausa == TipoPausaCola.AUSENTE_AL_LLAMADO ->
            AusenteAlLlamadoCard(
                isEstoyAtrasadoLoading = isEstoyAtrasadoLoading,
                onEstoyAtrasado = onEstoyAtrasado
            )

        estadoCola == EstadoEntradaCola.ATRASADO -> AtrasadoCard(
            fechaHoraLimiteRespuesta = estado.fechaHoraLimiteRespuesta,
            isSigoAsistiendoLoading = isSigoAsistiendoLoading,
            isLlegueLoading = isLlegueLoading,
            onSigoAsistiendo = onSigoAsistiendo,
            onLlegue = onLlegue
        )

        estadoCola == EstadoEntradaCola.EN_ATENCION -> EnAtencionCard()

        estadoCola == EstadoEntradaCola.FINALIZADA || estadoCola == EstadoEntradaCola.CANCELADA ->
            InfoCard(
                icon = Icons.Filled.CheckCircle,
                title = "Consulta finalizada",
                subtitle = "Esta atención ya no está activa."
            )

        else -> InfoCard(
            icon = Icons.Filled.HourglassEmpty,
            title = "Sin turno en cola",
            subtitle = "Todavía no ingresaste a la cola de espera de un hospital."
        )
    }
}

@Composable
private fun EnColaCard(
    tiempoEstimado: TiempoEstimadoAtencionResponse?,
    isAusentarmeLoading: Boolean,
    onAusentarme: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Groups,
                    contentDescription = null,
                    tint = ACCENT,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Estás en la cola",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (tiempoEstimado != null) {

                // Tiempo de espera destacado arriba de todo
                if (tiempoEstimado.hayMedicosActivos) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ACCENT.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
                            .padding(vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Tiempo de espera estimado",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.AccessTime,
                                contentDescription = null,
                                tint = ACCENT,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "~${tiempoEstimado.minutosPromedioAtencion} min",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        if (!tiempoEstimado.fechaHoraAtencionEstimada.isNullOrBlank()) {
                            Text(
                                text = "Alrededor de las ${formatHora(tiempoEstimado.fechaHoraAtencionEstimada)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Row(modifier = Modifier.fillMaxWidth()) {
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

                Spacer(modifier = Modifier.height(12.dp))

                if (!tiempoEstimado.hayMedicosActivos) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.WarningAmber,
                            contentDescription = null,
                            tint = Color(0xFFFFA726),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = tiempoEstimado.mensaje,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFFA726)
                        )
                    }
                } else {
                    Text(
                        text = tiempoEstimado.mensaje,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedButton(
                onClick = onAusentarme,
                enabled = !isAusentarmeLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isAusentarmeLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(imageVector = Icons.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ausentarme un momento")
                }
            }
        }
    }
}

@Composable
private fun EstimacionDato(label: String, valor: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
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