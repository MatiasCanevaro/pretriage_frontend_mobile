package com.proyecto_final.triage.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Componente reutilizable que muestra un mensaje de éxito
 * centrado sobre la pantalla, junto con un botón para
 * ejecutar una acción.
 *
 * @param successText Texto que describe el resultado exitoso.
 * @param buttonText Texto que se mostrará en el botón.
 * @param onClick Acción que se ejecutará al presionar el botón.
 */
@Composable
fun SuccessMessage(
    successText: String = "La operación se realizó correctamente.",
    buttonText: String = "Aceptar",
    onClick: () -> Unit
) {
    val successColor = Color(0xFF64B5D9)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Color.Black.copy(alpha = 0.45f)
            ),
        contentAlignment = Alignment.Center
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(24.dp)
                )
                .background(
                    color = Color(0xFFDAF1FF),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(
                    horizontal = 24.dp,
                    vertical = 24.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Ícono de éxito
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Éxito",
                tint = successColor,
                modifier = Modifier.size(56.dp)
            )

            // Título
            Text(
                text = "¡Listo!",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A5C8B),
                textAlign = TextAlign.Center
            )

            // Mensaje de éxito
            Text(
                text = successText,
                fontSize = 15.sp,
                color = Color(0xFF30516D),
                textAlign = TextAlign.Center
            )

            // Botón
            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = successColor,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = buttonText,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}