package com.proyecto_final.triage

import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import cafe.adriel.voyager.navigator.Navigator
import com.proyecto_final.triage.screens.SplashScreen
import com.proyecto_final.triage.theme.AppTheme
import com.proyecto_final.triage.viewmodels.LocalStudiesViewModel
import com.proyecto_final.triage.viewmodels.StudiesViewModel

@Composable
@Preview
fun App() {
    val studiesViewModel = remember { StudiesViewModel() }

    AppTheme {
        CompositionLocalProvider(LocalStudiesViewModel provides studiesViewModel) {
            Navigator(SplashScreen())
        }
    }
}