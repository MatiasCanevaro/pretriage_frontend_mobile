package com.proyecto_final.triage.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Spinner(
    color: Color = Color(0xFF0878D1),
    paddingVertical: Dp = 16.dp
) {
    Box(
        modifier = Modifier.fillMaxWidth()
            .padding(vertical = paddingVertical),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = color)
    }
}