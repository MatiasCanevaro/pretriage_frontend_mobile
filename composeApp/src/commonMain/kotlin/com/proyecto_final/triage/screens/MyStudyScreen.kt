package com.proyecto_final.triage.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import com.proyecto_final.triage.components.CommonHeader
import com.proyecto_final.triage.network.EstudioClinicoDTO
import com.proyecto_final.triage.theme.Spacing
import com.proyecto_final.triage.viewmodels.LocalStudiesViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.number
import com.proyecto_final.triage.filesaver.FileSaver
import com.proyecto_final.triage.filesaver.rememberPlatformContext
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.HorizontalDivider
import com.proyecto_final.triage.filesaver.FileViewer

class MyStudyScreen(
    private val estudio: EstudioClinicoDTO
) : Screen {

    @Composable
    override fun Content() {

        val navigator = LocalNavigator.current
        val viewModel = LocalStudiesViewModel.current
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }
        val platformContext = rememberPlatformContext()
        val eliminando by viewModel.eliminando.collectAsState()

        var mostrarConfirmacion by remember { mutableStateOf(false) }
        var cargandoVer by remember { mutableStateOf(false) }
        var cargandoDescarga by remember { mutableStateOf(false) }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {

                CommonHeader(
                    title = "Detalle del estudio",
                    onBack = { navigator?.pop() }
                )

                Spacer(modifier = Modifier.height(Spacing.lg))

                val nombreArchivo = estudio.nombreArchivo
                    ?: "estudio_${estudio.id}.${estudio.extensionArchivo ?: "pdf"}"

                EstudioDetalleContent(
                    estudio = estudio,
                    cargandoVer = cargandoVer,
                    cargandoDescarga = cargandoDescarga,
                    onVerArchivo = {
                        cargandoVer = true
                        scope.launch {
                            val existe = FileViewer.archivoExisteLocalmente(platformContext, nombreArchivo)

                            if (existe) {
                                FileViewer.abrirArchivo(platformContext, nombreArchivo)
                                    .onFailure {
                                        snackbarHostState.showSnackbar("No se pudo abrir el archivo")
                                    }
                                cargandoVer = false
                            } else {
                                viewModel.descargarEstudioClinico(
                                    idEstudio = estudio.id,
                                    onSuccess = { bytes ->
                                        scope.launch {
                                            FileViewer.guardarLocalmente(platformContext, nombreArchivo, bytes)
                                                .onSuccess {
                                                    FileViewer.abrirArchivo(platformContext, nombreArchivo)
                                                }
                                                .onFailure {
                                                    snackbarHostState.showSnackbar("No se pudo guardar el archivo")
                                                }
                                            cargandoVer = false
                                        }
                                    },
                                    onError = {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Error al descargar el estudio")
                                            cargandoVer = false
                                        }
                                    }
                                )
                            }
                        }
                    },
                    onDownload = { id ->
                        cargandoDescarga = true
                        viewModel.descargarEstudioClinico(
                            idEstudio = id,
                            onSuccess = { bytes ->
                                scope.launch {
                                    FileSaver.saveToDownloads(
                                        context = platformContext,
                                        fileName = nombreArchivo,
                                        bytes = bytes
                                    ).onSuccess {
                                        snackbarHostState.showSnackbar("Archivo guardado")
                                    }.onFailure {
                                        snackbarHostState.showSnackbar("No se pudo guardar el archivo")
                                    }
                                    cargandoDescarga = false
                                }
                            },
                            onError = {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Error al descargar")
                                    cargandoDescarga = false
                                }
                            }
                        )
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    OutlinedButton(
                        onClick = { mostrarConfirmacion = true },
                        enabled = !eliminando,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFFFCEAEA),
                            contentColor = Color(0xFFE05353)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFF2A6A6)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp)
                    ) {
                        if (eliminando) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color(0xFFE05353))
                        } else {
                            Text("Eliminar estudio")
                        }
                    }
                }
            }
        }

        if (mostrarConfirmacion) {
            AlertDialog(
                onDismissRequest = { mostrarConfirmacion = false },
                title = { Text("Eliminar estudio") },
                text = { Text("¿Estás seguro que querés eliminar este estudio?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            mostrarConfirmacion = false
                            viewModel.eliminarEstudioClinico(
                                idEstudio = estudio.id,
                                onSuccess = { navigator?.pop() },
                                onError = { /* TODO */ }
                            )
                        }
                    ) {
                        Text("Eliminar", color = Color(0xFFE05353))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { mostrarConfirmacion = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@Composable
private fun EstudioDetalleContent(
    estudio: EstudioClinicoDTO,
    cargandoVer: Boolean,
    cargandoDescarga: Boolean,
    onVerArchivo: () -> Unit,
    onDownload: (Long) -> Unit
) {
    val fecha = estudio.fechaSubida?.let { LocalDateTime.parse(it) }
    val fechaFormateada = fecha?.let {
        "${it.day.toString().padStart(2, '0')}/${it.month.number.toString().padStart(2, '0')}/${it.year}"
    } ?: ""

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {

            // --- Encabezado: ícono + título + fecha ---
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color.White, shape = RoundedCornerShape(50))
                        .border(1.dp, Color(0xFFEDEDED), shape = RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = null,
                        tint = Color(0xFF5BB8D4),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(estudio.tipoArchivo, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(fechaFormateada, color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // --- Descripción ---
            if (!estudio.descripcion.isNullOrBlank()) {
                HorizontalDivider(color = Color(0xFFEDEDED))
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Descripción", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(estudio.descripcion, style = MaterialTheme.typography.bodyMedium)
                }
            }

            // --- Archivo ---
            HorizontalDivider(color = Color(0xFFEDEDED))
            Column(modifier = Modifier.padding(18.dp)) {
                Text("Archivo", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFEDEDED)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFE05353), modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(estudio.nombreArchivo ?: "Sin nombre", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${estudio.extensionArchivo?.uppercase() ?: "-"} | ${formatearTamano(estudio.tamanoArchivo ?: 0)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onVerArchivo,
                        enabled = !cargandoVer,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        if (cargandoVer) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.RemoveRedEye, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ver archivo", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    OutlinedButton(
                        onClick = { onDownload(estudio.id) },
                        enabled = !cargandoDescarga,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        if (cargandoDescarga) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Descargar", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

private fun formatearTamano(bytes: Long?): String {
    if (bytes == null) return "-"
    val kb = bytes / 1024.0
    return if (kb < 1024) { "${kb.toInt()} KB" }
            else { "${(kb / 1024).toInt()} MB" }
}