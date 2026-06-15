package com.proyecto_final.triage.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
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
        Font(
            resource = Res.font.Poppins_Medium,
            weight = FontWeight.Medium
        ),
        Font(
            resource = Res.font.Poppins_SemiBold,
            weight = FontWeight.SemiBold
        )
    )

    return Typography(

        headlineLarge = TextStyle(
            fontFamily = poppins,
            fontWeight = FontWeight.SemiBold,
            fontSize = 32.sp
        ),

        headlineMedium = TextStyle(
            fontFamily = poppins,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp
        ),

        titleLarge = TextStyle(
            fontFamily = poppins,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp
        ),

        bodyLarge = TextStyle(
            fontFamily = poppins,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp
        ),

        bodyMedium = TextStyle(
            fontFamily = poppins,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
        ),

        labelLarge = TextStyle(
            fontFamily = poppins,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp
        )
    )
}