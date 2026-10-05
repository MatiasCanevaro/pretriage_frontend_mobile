package com.proyecto_final.triage.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import com.proyecto_final.triage.components.CommonHeader
import com.proyecto_final.triage.components.ErrorMessage
import com.proyecto_final.triage.components.InputDropdownField
import com.proyecto_final.triage.components.InputTextField
import com.proyecto_final.triage.theme.Spacing
import com.proyecto_final.triage.viewmodels.LocalStudiesViewModel
import com.proyecto_final.triage.viewmodels.StudiesViewModel
import multiplatform.network.cmpfilepicker.MediaPicker
import multiplatform.network.cmpfilepicker.rememberMediaPickerState
import multiplatform.network.cmpfilepicker.utils.MediaResult

class AddStudyScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val viewModel = LocalStudiesViewModel.current

        AddStudyContent(
            viewModel = viewModel,
            onBack = { navigator?.pop() },
            onUpload = { tipo, descripcion, fileBytes, fileName ->
                viewModel.subirNuevoEstudio(
                    fileBytes = fileBytes,
                    fileName = fileName,
                    tipoArchivo = tipo,
                    descripcion = descripcion,
                    onSuccess = {
                        viewModel.notificarEstudioAgregado(tipo)
                        navigator?.pop()
                    }
                )
            }
        )
    }
}

@Composable
fun UploadFileCard(fileName: String?, onClick: () -> Unit) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable(onClick = onClick),
        border = BorderStroke(1.dp, Color.Gray),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (fileName == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CloudUpload,
                        null,
                        modifier = Modifier.size(48.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Seleccionar archivo")
                    Text("PDF, PNG o JPG", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Description,
                        null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(fileName)
                }
            }
        }
    }
}

@Composable
fun AddStudyContent(
    viewModel: StudiesViewModel,
    onBack: () -> Unit,
    onUpload: (
        tipo: String,
        descripcion: String,
        fileBytes: ByteArray,
        fileName: String
    ) -> Unit,
) {
    var expanded            by remember { mutableStateOf(false) }
    var tipo                by remember { mutableStateOf("") }
    var descripcion         by remember { mutableStateOf("") }
    var selectedFileName    by remember { mutableStateOf<String?>(null) }
    var selectedFileBytes   by remember { mutableStateOf<ByteArray?>(null) }

    val subiendo            by viewModel.subiendo.collectAsState()
    val errorSubida         by viewModel.errorSubida.collectAsState()

    // El ViewModel es compartido entre pantallas: limpio el error al salir,
    // así no aparece la próxima vez que se abra esta pantalla
    DisposableEffect(Unit) {
        onDispose { viewModel.limpiarErrorSubida() }
    }

    // Una sola forma de subir: la usan el botón "Subir estudio" y el "Reintentar"
    val subir: () -> Unit = {
        val bytes = selectedFileBytes
        val nombre = selectedFileName
        if (tipo.isNotBlank() && bytes != null && nombre != null) {
            onUpload(tipo, descripcion, bytes, nombre)
        }
    }

    // 1) el "estado" del picker
    val pickerState = rememberMediaPickerState()

    // 2) el componente que escucha el resultado. Tiene que estar SIEMPRE
    //    en composición, no dentro de un if / dialog condicional.
    MediaPicker(
        state = pickerState,
        onResult = { result ->
            when (result) {
                is MediaResult.Document -> {
                    val doc = result.document.firstOrNull()
                    if (doc != null) {
                        selectedFileName = doc.name
                        selectedFileBytes = doc.data
                    }
                }
                else -> Unit
            }
        },
        onPermissionDenied = { /* TODO */ }
    )

    val tipos = listOf(
        "Análisis de laboratorio",
        "Radiografía",
        "Resonancia",
        "Tomografía",
        "Ecografía",
        "Otro"
    )

    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {

            // Titulo de la pantalla
            CommonHeader(title = "Agregar estudio", onBack = onBack)

            // Formulario: scrollea solo esta parte, el botón queda fijo abajo
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                InputDropdownField(
                    label = "Tipo de estudio",
                    value = tipo,
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    options = tipos,
                    onOptionSelected = { tipo = it }
                )

                InputTextField(
                    label = "Descripción (opcional)",
                    value = descripcion,
                    onValueChange = { if (it.length <= 150) descripcion = it },
                    singleLine = false
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        "${descripcion.length}/150",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.md))

                Text(
                    "Archivo",
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                UploadFileCard(
                    fileName = selectedFileName,
                    onClick = {
                        // "*/*" = cualquier archivo. Filtrá por MIME si querés
                        // limitarlo a PDF/imágenes: listOf("application/pdf", "image/*")
                        pickerState.pickDocument(
                            mimeTypes = listOf("*/*"),
                            isSingle = true
                        )
                    },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                enabled = tipo.isNotBlank() && selectedFileBytes != null && !subiendo,
                onClick = subir,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                // Mientras sube, el botón conserva su color (más tenue) en vez de ponerse gris.
                // Si está deshabilitado porque falta completar algo, se ve gris como siempre.
                colors = if (subiendo) {
                    ButtonDefaults.buttonColors(
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        disabledContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                if (subiendo) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Subir estudio")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Overlay de error: va al final del Box para quedar por encima de todo.
        // "Reintentar" vuelve a subir el mismo archivo con los mismos datos.
        errorSubida?.let { mensaje ->
            ErrorMessage(
                errorText = mensaje,
                onClick = subir
            )
        }
    }
}