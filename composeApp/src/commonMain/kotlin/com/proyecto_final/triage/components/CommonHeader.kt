package com.proyecto_final.triage.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.proyecto_final.triage.theme.Spacing
import org.jetbrains.compose.resources.painterResource
import triage.composeapp.generated.resources.Res
import triage.composeapp.generated.resources.logo

enum class HeaderAlignment { START, CENTER, END }

@Composable
fun CommonHeader(
    title: String,
    subtitle: String? = null,
    alignment: HeaderAlignment = HeaderAlignment.START,
    showLogo: Boolean = false,
    onBack: (() -> Unit)? = null
) {
    Column {
        Spacer(modifier = Modifier.height(Spacing.lg))

        // FLECHA + LOGO
        if (onBack != null || showLogo) {
            Box(modifier = Modifier.fillMaxWidth()) {
                if (onBack != null) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .clickable { onBack() }
                    )
                }
                if (showLogo) {
                    Image(
                        painter = painterResource(Res.drawable.logo),
                        contentDescription = "Logo",
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.Center)
                            .padding(top = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (showLogo) 4.dp else Spacing.md))
        }

        // TITULO Y SUBTITULO
        val textAlign = when (alignment) {
            HeaderAlignment.START -> TextAlign.Start
            HeaderAlignment.CENTER -> TextAlign.Center
            HeaderAlignment.END -> TextAlign.End
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth()
        )

        if (subtitle != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = textAlign,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(Spacing.md))
    }
}