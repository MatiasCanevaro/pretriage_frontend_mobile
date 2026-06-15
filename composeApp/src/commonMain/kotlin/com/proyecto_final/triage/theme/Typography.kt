// Theme.kt
package com.proyecto_final.triage.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import triage.composeapp.generated.resources.Poppins_Medium
import triage.composeapp.generated.resources.Poppins_SemiBold
import triage.composeapp.generated.resources.Res

@Composable
fun appTypography(): Typography {
    val poppins = FontFamily(
        Font(Res.font.Poppins_Medium, FontWeight.Medium),
        Font(Res.font.Poppins_SemiBold, FontWeight.SemiBold)
    )

    return remember(poppins) {
        Typography(
            headlineLarge  = AppTextStyles.headlineLarge.copy(fontFamily = poppins),
            headlineMedium = AppTextStyles.headlineMedium.copy(fontFamily = poppins),
            titleLarge     = AppTextStyles.titleLarge.copy(fontFamily = poppins),
            bodyLarge      = AppTextStyles.bodyLarge.copy(fontFamily = poppins),
            bodyMedium     = AppTextStyles.bodyMedium.copy(fontFamily = poppins),
            labelLarge     = AppTextStyles.labelLarge.copy(fontFamily = poppins),
        )
    }
}


object AppTextStyles {
    val headlineLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp
    )
    val headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp
    )
    val titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp
    )
    val bodyLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    )
    val bodyMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    )
    val labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp
    )
}