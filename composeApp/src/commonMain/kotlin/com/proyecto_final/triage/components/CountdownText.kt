package com.proyecto_final.triage.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun CountdownText(
    totalSeconds: Int,
    onFinish: () -> Unit = {},
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle(fontSize = 14.sp),
    warningThresholdSeconds: Int = 120,
    warningColor: Color = Color(0xFFB3261E),
    normalColor: Color = Color(0xFF757575)
) {
    var secondsRemaining by remember(totalSeconds) { mutableStateOf(totalSeconds) }
    var hasFinished by remember(totalSeconds) { mutableStateOf(false) }

    LaunchedEffect(totalSeconds) {
        secondsRemaining = totalSeconds
        hasFinished = false
        if (totalSeconds <= 0) return@LaunchedEffect

        while (secondsRemaining > 0) {
            delay(1000.milliseconds)
            secondsRemaining--
        }
        hasFinished = true
        onFinish()
    }

    val minutes = secondsRemaining / 60
    val seconds = secondsRemaining % 60
    val formatted = "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    val color = if (secondsRemaining <= warningThresholdSeconds) warningColor else normalColor

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        androidx.compose.material3.Text(
            text = "Minutos restantes: ",
            style = textStyle,
            color = color
        )
        androidx.compose.material3.Text(
            text = formatted,
            style = textStyle.copy(fontWeight = FontWeight.Bold),
            color = color
        )
    }
}