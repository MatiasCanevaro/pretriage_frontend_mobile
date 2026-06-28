package com.proyecto_final.triage.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
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
import com.proyecto_final.triage.theme.Spacing

@Composable
fun InfoBanner( text: String,
                modifier: Modifier = Modifier,
                icon: ImageVector = Icons.Filled.Info) {
    Row(modifier = modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE3F2FD))
                            .padding(Spacing.md),
        verticalAlignment = Alignment.CenterVertically ) {
        Icon(   imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF5BB8D4),
                modifier = Modifier.size(Spacing.lg) )
        Spacer( modifier = Modifier.width(12.dp))
        Text(  text = text, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF5BB8D4))
    }
}