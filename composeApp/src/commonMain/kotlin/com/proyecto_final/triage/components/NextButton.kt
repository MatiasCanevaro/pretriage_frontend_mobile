package com.proyecto_final.triage.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Componente reutilizable para avanzar a la siguiente acción.
 *
 * @param onClick Acción que se ejecutará al presionar el botón.
 * @param enabled Indica si el botón está habilitado. Por defecto es true.
 * @param text Texto que se mostrará en el botón. Por defecto es "Continuar".
 */
@Composable
fun NextButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    text: String = "Continuar"
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF69B3D5))
    ) {
        Text(text)
    }
}
