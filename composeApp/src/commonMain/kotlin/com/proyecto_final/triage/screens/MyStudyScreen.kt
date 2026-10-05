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
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import com.proyecto_final.triage.components.CommonHeader
import com.proyecto_final.triage.components.ErrorMessage
import com.proyecto_final.triage.filesaver.FileSaver
import com.proyecto_final.triage.filesaver.FileViewer
import com.proyecto_final.triage.filesaver.rememberPlatformContext
import com.proyecto_final.triage.network.EstudioClinicoDTO
import com.proyecto_final.triage.theme.Spacing
import com.proyecto_final.triage.viewmodels.LocalStudiesViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.number

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

        // Error de cualquiera de las acciones (ver, descargar, eliminar). null = sin error
        var errorAccion by remember { mutableStateOf<ErrorAccion?>(null) }

        val nombreArchivo = estudio.nombreArchivo
            ?: "estudio_${estudio.id}.${estudio.extensionArchivo ?: "pdf"}"

        /*
         * ================================================================
         * ACCIONES
         * Cada acción es una función, así el "Reintentar" del error
         * vuelve a ejecutar exactamente la misma acción que falló.
         * ================================================================
         */

        fun mostrarError(mensaje: String, error: Throwable? = null, reintentar: () -> Unit) {
            error?.let { println("MyStudyScreen - $mensaje: ${it.stackTraceToString()}") }
            errorAccion = ErrorAccion(mensaje = mensaje, reintentar = reintentar)
        }

        fun verArchivo() {
            errorAccion = null
            cargandoVer = true

            scope.launch {
                val existe = FileViewer.archivoExisteLocalmente(platformContext, nombreArchivo)

                // Ya está en el celular: solo lo abro
                if (existe) {
                    FileViewer.abrirArchivo(platformContext, nombreArchivo)
                        .onFailure { error ->
                            mostrarError(
                                "No se pudo abrir el archivo. Verificá que tengas una app para abrir este tipo de archivo.",
                                error
                            ) { verArchivo() }
                        }
                    cargandoVer = false
                    return@launch
                }

                // No está en el celular: lo descargo, lo guardo y lo abro
                viewModel.descargarEstudioClinico(
                    idEstudio = estudio.id,
                    onSuccess = { bytes ->
                        scope.launch {
                            FileViewer.guardarLocalmente(platformContext, nombreArchivo, bytes)
                                .onSuccess {
                                    FileViewer.abrirArchivo(platformContext, nombreArchivo)
                                        .onFailure { error ->
                                            mostrarError(
                                                "No se pudo abrir el archivo. Verificá que tengas una app para abrir este tipo de archivo.",
                                                error
                                            ) { verArchivo() }
                                        }
                                }
                                .onFailure { error ->
                                    mostrarError("No se pudo guardar el archivo en el celular.", error) { verArchivo() }
                                }
                            cargandoVer = false
                        }
                    },
                    onError = { mensaje ->
                        cargandoVer = false
                        mostrarError(mensaje) { verArchivo() }
                    }
                )
            }
        }

        fun descargar() {
            errorAccion = null
            cargandoDescarga = true

            viewModel.descargarEstudioClinico(
                idEstudio = estudio.id,
                onSuccess = { bytes ->
                    scope.launch {
                        val resultado = FileSaver.saveToDownloads(
                            context = platformContext,
                            fileName = nombreArchivo,
                            bytes = bytes
                        )
                        // Apago el spinner ANTES del snackbar: showSnackbar espera
                        // a que el snackbar se cierre, y el botón quedaría cargando
                        cargandoDescarga = false

                        resultado
                            .onSuccess {
                                snackbarHostState.showSnackbar("Archivo guardado en Descargas")
                            }
                            .onFailure { error ->
                                mostrarError("No se pudo guardar el archivo en Descargas.", error) { descargar() }
                            }
                    }
                },
                onError = { mensaje ->
                    cargandoDescarga = false
                    mostrarError(mensaje) { descargar() }
                }
            )
        }

        fun eliminar() {
            errorAccion = null

            viewModel.eliminarEstudioClinico(
                idEstudio = estudio.id,
                onSuccess = { navigator?.pop() },
                onError = { mensaje ->
                    mostrarError(mensaje) { eliminar() }
                }
            )
        }

        /*
         * ================================================================
         * PANTALLA
         * ================================================================
         */

        Box(modifier = Modifier.fillMaxSize()) {

            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { paddingValues ->

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .verticalScroll(rememberScrollState())
                ) {

                    CommonHeader(
                        title = "Detalle del estudio",
                        onBack = { navigator?.pop() }
                    )

                    Spacer(modifier = Modifier.height(Spacing.lg))

                    EstudioDetalleContent(
                        estudio = estudio,
                        cargandoVer = cargandoVer,
                        cargandoDescarga = cargandoDescarga,
                        onVerArchivo = { verArchivo() },
                        onDownload = { descargar() }
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
                                contentColor = Color(0xFFE05353),
                                disabledContainerColor = Color(0xFFFCEAEA),
                                disabledContentColor = Color(0xFFE05353)
                            ),
                            border = BorderStroke(1.dp, Color(0xFFF2A6A6)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp)
                        ) {
                            if (eliminando) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color(0xFFE05353),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Eliminar estudio")
                            }
                        }
                    }
                }
            }

            // Overlay de error: va al final del Box para quedar por encima de todo.
            // "Reintentar" vuelve a ejecutar la acción que falló.
            errorAccion?.let { accion ->
                ErrorMessage(
                    errorText = accion.mensaje,
                    onClick = accion.reintentar
                )
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
                            eliminar()
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

/**
 * Error de una acción de la pantalla, con lo que hay que hacer al tocar "Reintentar".
 */
private data class ErrorAccion(
    val mensaje: String,
    val reintentar: () -> Unit
)

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
    return if (kb < 1024) {
        "${kb.toInt()} KB"
    } else {
        "${(kb / 1024).toInt()} MB"
    }
}