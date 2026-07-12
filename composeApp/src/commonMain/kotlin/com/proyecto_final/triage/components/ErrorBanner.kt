package com.proyecto_final.triage.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun ErrorBanner(message: String, buttonText: String, icon: ImageVector, onButtonClick: () -> Unit) {

    val textColor = Color(0xFFB3261E)

    Surface(
            modifier =  Modifier.fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(top = 36.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFFFE5E5),
            border = BorderStroke(1.dp, textColor),
            shadowElevation = 6.dp
    ) {
        Row(
            modifier =  Modifier.fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Row(modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.size(8.dp))

                Text(
                    text = message,
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            TextButton(onClick = onButtonClick) {
                Text(
                    text = buttonText,
                    color = textColor
                )
            }
        }
    }
}