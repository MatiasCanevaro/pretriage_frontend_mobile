package com.proyecto_final.triage.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val AppColorScheme = lightColorScheme(
    primary         = ButtonPrimary,
    secondary       = TextSecondary,
    background      = Background,
    surface         = InputBackground,
    error           = Warning,
    onPrimary       = InputBackground,
    onBackground    = TextPrimary,
    onSurface       = TextPrimary,
    onSurfaceVariant = TextSecondary
)

@Composable
fun AppTheme ( content: @Composable () -> Unit) {
    MaterialTheme(  colorScheme = AppColorScheme,
                    typography  = appTypography(),
                    content     = content,
        )
}
