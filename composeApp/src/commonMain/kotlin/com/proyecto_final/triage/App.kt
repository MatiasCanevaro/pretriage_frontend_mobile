package com.proyecto_final.triage

import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import cafe.adriel.voyager.navigator.Navigator
import com.proyecto_final.triage.screens.SplashScreen
import com.proyecto_final.triage.theme.AppTheme

@Composable
@Preview
fun App() {
    AppTheme {
        Navigator(SplashScreen())
    }
}