package com.proyecto_final.triage.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun InputMonthYearField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorMessage: String = "Este campo es obligatorio",
    placeholder: String? = null
) {
    var showDialog by remember { mutableStateOf(false) }

    CalendarOutlinedField(
        label = label,
        value = value,
        modifier = modifier,
        enabled = enabled,
        placeholder = placeholder,
        isError = isError,
        errorMessage = errorMessage,
        onShowDialog = { showDialog = true }
    )

    if (showDialog) {
        MonthYearPickerDialog(
            label = label,
            initialValue = value,
            onConfirm = { mesAnio ->
                onValueChange(mesAnio)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

private val mesesDelAnio = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
private fun MonthYearPickerDialog(
    label: String,
    initialValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val anioActual = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault()).year

    val anios = (anioActual - 5..anioActual + 20).toList()

    val (mesInicial, anioInicial) = parsearMesAnioInicial(initialValue, anioActual)

    var mes by remember { mutableStateOf(mesInicial) }
    var anio by remember { mutableStateOf(anioInicial) }

    var mesExpanded by remember { mutableStateOf(false) }
    var anioExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(label) },
        text = {
            Column {
                ExposedDropdownMenuBox(
                    expanded = mesExpanded,
                    onExpandedChange = { mesExpanded = it }
                ) {
                    OutlinedTextField(
                        value = mesesDelAnio[mes],
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = mesExpanded
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = mesExpanded,
                        onDismissRequest = { mesExpanded = false }
                    ) {
                        mesesDelAnio.forEachIndexed { index, nombre ->
                            DropdownMenuItem(
                                text = { Text(nombre) },
                                onClick = {
                                    mes = index
                                    mesExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = anioExpanded,
                    onExpandedChange = { anioExpanded = it }
                ) {
                    OutlinedTextField(
                        value = anio.toString(),
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = anioExpanded
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = anioExpanded,
                        onDismissRequest = { anioExpanded = false }
                    ) {
                        anios.forEach { numero ->
                            DropdownMenuItem(
                                text = { Text(numero.toString()) },
                                onClick = {
                                    anio = numero
                                    anioExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm("${(mes + 1).toString().padStart(2, '0')}/$anio")
            }) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

private fun parsearMesAnioInicial(value: String, anioPorDefecto: Int): Pair<Int, Int> {
    val partes = value.split("/")
    if (partes.size != 2) return 0 to anioPorDefecto

    val mes = partes[0].toIntOrNull() ?: return 0 to anioPorDefecto
    val anio = partes[1].toIntOrNull() ?: return 0 to anioPorDefecto

    if (mes !in 1..12) return 0 to anioPorDefecto

    return (mes - 1) to anio
}