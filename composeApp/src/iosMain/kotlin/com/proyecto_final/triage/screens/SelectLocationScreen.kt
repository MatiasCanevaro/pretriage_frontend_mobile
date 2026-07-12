package com.proyecto_final.triage.screens

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen

actual fun getSelectLocationScreen(type: String): Screen = SelectLocationScreen(type)
class SelectLocationScreen(private val type: String) : Screen {
    @Composable
    override fun Content() {
        //TODO
    }
}