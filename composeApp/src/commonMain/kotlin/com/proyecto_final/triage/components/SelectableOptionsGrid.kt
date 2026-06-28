package com.proyecto_final.triage.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

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
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (seleccionado) Color(0xFFE3F2FD)
                                else MaterialTheme.colorScheme.surface
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                                else MaterialTheme.colorScheme.onBackground
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