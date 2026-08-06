package com.proyecto_final.triage.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import com.proyecto_final.triage.components.CommonHeader
import com.proyecto_final.triage.components.ErrorBanner
import com.proyecto_final.triage.components.InfoBanner
import com.proyecto_final.triage.network.AtencionEstimada
import com.proyecto_final.triage.network.ChatMensaje
import com.proyecto_final.triage.network.esAutorBot
import com.proyecto_final.triage.theme.AppTheme
import com.proyecto_final.triage.viewmodels.ChatViewModel
import kotlinx.coroutines.delay

class ChatScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val viewModel = remember { ChatViewModel() }
        ChatContent(
            onBack = { navigator?.pop() },
            viewModel = viewModel
        )
    }
}

@Preview
@Composable
fun ChatPreview() {
    AppTheme {
        ChatContent(
            onBack = {},
            viewModel = ChatViewModel()
        )
    }
}

@Composable
fun ChatContent(
    onBack: () -> Unit,
    viewModel: ChatViewModel
) {
    val state by viewModel.state.collectAsState()

    var showConfirmDialog by remember { mutableStateOf(false) }
    var mostrarExito by remember { mutableStateOf(false) }
    val listaState = rememberLazyListState()

    // El InfoBanner se muestra hasta que el usuario envía su primer mensaje.
    val mostrarInfo = state.mensajes.none { !esAutorBot(it.autor) }
    // Offset de items fijos antes de los mensajes: header + (banner cuando está visible)
    val offsetMensajes = if (mostrarInfo) 2 else 1

    LaunchedEffect(Unit) {
        viewModel.iniciar()
    }

    // Al enviar un mensaje: llevo la vista al mensaje recién agregado.
    LaunchedEffect(state.enviando) {
        if (state.enviando && state.mensajes.isNotEmpty()) {
            scrollAlFinal(listaState, offsetMensajes + state.mensajes.size - 1)
        }
    }

    // Al recibir mensajes: solo bajo al final si el usuario ya está cerca del fondo
    // (o en la primera carga del chat). Si está leyendo histórico, la vista no se mueve.
    var scrollInicialHecho by remember { mutableStateOf(false) }
    LaunchedEffect(state.mensajes.size) {
        if (state.mensajes.isNotEmpty()) {
            val ultimoVisible = listaState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val cercaDelFinal = ultimoVisible >= listaState.layoutInfo.totalItemsCount - 2
            if (!scrollInicialHecho || cercaDelFinal) {
                scrollInicialHecho = true
                scrollAlFinal(listaState, offsetMensajes + state.mensajes.size - 1)
            }
        }
    }

    // Feedback transitorio de éxito del envío
    LaunchedEffect(state.envioExitoso) {
        if (state.envioExitoso == true) {
            mostrarExito = true
            delay(2500)
            mostrarExito = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        // Error al iniciar el chat
        if (state.error != null && state.chatId == null) {
            ErrorBanner(
                message = state.error ?: "No se pudo iniciar el chat",
                buttonText = "Reintentar",
                icon = Icons.Filled.Backup,
                onButtonClick = { viewModel.iniciar() }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Lista de mensajes
        LazyColumn(
            state = listaState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                CommonHeader(title = "Chat interactivo", onBack = { onBack() })
            }

            if (mostrarInfo) {
                item {
                    InfoBanner(
                        text = "Describí tus síntomas por escrito y el asistente estimará una prioridad de atención."
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            if (state.mensajes.isEmpty() && state.iniciando) {
                item {
                    Box(
                        modifier = Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Cargando chat...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            itemsIndexed(state.mensajes) { index, mensaje ->
                BurbujaDeMensaje(
                    mensaje = mensaje,
                    noEnviado = state.mensajesNoEnviados.contains(mensaje.contenido)
                )
                if (index == state.mensajes.size - 1) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            if (state.enviando) {
                item {
                    BurbujaDeMensaje(
                        ChatMensaje(
                            contenido = "Escribiendo...",
                            autor = "BOT"
                        ),
                        escribiendo = true
                    )
                }
            }
        }

        // Card de atención estimada / prioridad
        state.atencionEstimada?.let { atencion ->
            CardAtencionEstimada(atencion)
        }

        // Chat finalizado
        if (state.finalizado) {
            InfoBanner(
                text = "Triage finalizado. Tu consulta fue derivada al equipo médico.",
                icon = Icons.Filled.Check
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Error al enviar (texto conservado en el campo)
        if (state.error != null && state.chatId != null) {
            Text(
                text = state.error ?: "Error",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
        }

        if (mostrarExito) {
            Text(
                text = "Mensaje enviado",
                color = Color(0xFF2E7D32),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            )
        }

        // Entrada de mensaje
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = state.borrador,
                onValueChange = viewModel::actualizarBorrador,
                enabled = state.chatId != null && !state.finalizado && !state.enviando,
                placeholder = {
                    Text("Escribí tus síntomas...")
                },
                minLines = 1,
                maxLines = 4,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            val puedeEnviar = state.borrador.isNotBlank() &&
                state.chatId != null &&
                !state.finalizado &&
                !state.enviando

            if (state.enviando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    strokeWidth = 3.dp
                )
            } else {
                IconButton(
                    onClick = {
                        if (puedeEnviar) {
                            showConfirmDialog = true
                        }
                    },
                    enabled = puedeEnviar,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = if (puedeEnviar) Color(0xFF5BB8D4) else MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Send,
                        contentDescription = "Enviar",
                        tint = if (puedeEnviar) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Confirmación: se permite editar la consulta antes de enviarla
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Revisá tu consulta") },
            text = {
                Text(
                    text = state.borrador,
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    viewModel.enviarBorrador()
                }) {
                    Text("Enviar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Editar")
                }
            }
        )
    }
}

@Composable
private fun BurbujaDeMensaje(
    mensaje: ChatMensaje,
    escribiendo: Boolean = false,
    noEnviado: Boolean = false
) {
    val esDelBot = esAutorBot(mensaje.autor) || escribiendo

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (esDelBot) Alignment.Start else Alignment.End
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (esDelBot)
                MaterialTheme.colorScheme.surfaceVariant
            else
                Color(0xFF5BB8D4),
            shadowElevation = 2.dp
        ) {
            if (escribiendo) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = mensaje.contenido,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = mensaje.contenido,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (esDelBot)
                        MaterialTheme.colorScheme.onSurface
                    else
                        Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }

        mensaje.fechaHoraEnvio?.takeIf { it.isNotBlank() }?.let { hora ->
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatearFechaHora(hora),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (noEnviado) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "No enviado",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun CardAtencionEstimada(atencion: AtencionEstimada) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.PriorityHigh,
                    contentDescription = null,
                    tint = Color(0xFF5BB8D4),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Prioridad triage asignada",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            atencion.mensaje?.takeIf { it.isNotBlank() }?.let { mensaje ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = mensaje,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (atencion.posicionEnCola > 0 || atencion.pacientesAntes > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Posición en cola",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${atencion.posicionEnCola}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pacientes antes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${atencion.pacientesAntes}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tiempo estimado",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${atencion.minutosPromedioAtencion} min",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            if (!atencion.hayMedicosActivos) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Groups,
                        contentDescription = null,
                        tint = Color(0xFF9E9E9E),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "No hay médicos activos para estimar la atención",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9E9E9E)
                    )
                }
            }
        }
    }
}

// Baja al final de la lista. Con mucha distancia usa scroll instantáneo para
// evitar animaciones largas que interfieren con el gesto del usuario.
private suspend fun scrollAlFinal(listaState: LazyListState, target: Int) {
    val distancia = target - listaState.firstVisibleItemIndex
    if (distancia > 20) {
        listaState.scrollToItem(target)
    } else {
        listaState.animateScrollToItem(target)
    }
}

// Formateo simple: "2024-06-15T14:30:00Z" -> "15/06/2024 14:30"
private fun formatearFechaHora(fechaHora: String): String {
    val partes = fechaHora.replace("T", " ").substringBeforeLast(":").split("-")
    
    val anio= partes[0]
    val mes = partes[1]
    val diaHora = partes[2]
    val partesDiaHora = diaHora.split(" ")

    return "${partesDiaHora[0]}/${mes}/${anio} ${partesDiaHora[1]}"
}