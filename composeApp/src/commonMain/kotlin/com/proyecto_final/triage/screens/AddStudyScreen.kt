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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import com.proyecto_final.triage.viewmodels.StudiesViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.proyecto_final.triage.components.CommonHeader
import com.proyecto_final.triage.components.InputDropdownField
import com.proyecto_final.triage.components.InputTextField
import com.proyecto_final.triage.theme.Spacing
import multiplatform.network.cmpfilepicker.MediaPicker
import multiplatform.network.cmpfilepicker.rememberMediaPickerState
import multiplatform.network.cmpfilepicker.utils.MediaResult
import androidx.compose.runtime.collectAsState
import com.proyecto_final.triage.viewmodels.LocalStudiesViewModel

class AddStudyScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val viewModel = LocalStudiesViewModel.current
        val subiendo by viewModel.subiendo.collectAsState()

        AddStudyContent(
            onBack = { navigator?.pop() },
            onUpload = { tipo, descripcion, fileBytes, fileName ->
                viewModel.subirNuevoEstudio(
                    fileBytes = fileBytes,
                    fileName = fileName,
                    tipoArchivo = tipo,
                    descripcion = descripcion,
                    onSuccess = { navigator?.pop() },
                    onError = { /* opcional: snackbar */ }
                )
            },
            subiendo = subiendo
        )
    }
}

@Composable
fun UploadFileCard(fileName: String?, onClick: () -> Unit) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()
        .height(120.dp)
        .clickable(onClick = onClick),
        border = BorderStroke(1.dp, Color.Gray),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (fileName == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.CloudUpload,
                        null,
                        modifier = Modifier.size(48.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Seleccionar archivo")
                    Text("PDF, PNG o JPG", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }

            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Description,
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
fun AddStudyContent(onBack: () -> Unit,
                    onUpload: (tipo: String,
                               descripcion: String,
                               fileBytes: ByteArray,
                               fileName: String) -> Unit,
                    subiendo: Boolean = false
) {
    var expanded            by remember { mutableStateOf(false) }
    var tipo                by remember { mutableStateOf("") }
    var descripcion         by remember { mutableStateOf("") }
    var selectedFileName    by remember { mutableStateOf<String?>(null) }
    var selectedFileBytes   by remember { mutableStateOf<ByteArray?>(null) }

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

    val tipos = listOf("Análisis de laboratorio",
        "Radiografía",
        "Resonancia",
        "Tomografía",
        "Ecografía",
        "Otro"
    )

    Column(modifier = Modifier.fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp)
    ) {

        CommonHeader(
            title = "Agregar estudio",
            onBack = onBack
        )

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

        Spacer(modifier = Modifier.weight(1f))

        Button(
            enabled = tipo.isNotBlank() && selectedFileBytes != null && !subiendo,
            onClick = { onUpload(tipo, descripcion, selectedFileBytes!!, selectedFileName!!) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (subiendo) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Subir estudio")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}