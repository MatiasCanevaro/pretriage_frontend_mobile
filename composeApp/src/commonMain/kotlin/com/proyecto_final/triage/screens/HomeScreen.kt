package com.proyecto_final.triage.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.proyecto_final.triage.utils.dialEmergency

class HomeScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        HomeContent(  onEmergency = { dialEmergency() }
        )
    }
}

@Composable
fun HomeContent(onEmergency: () -> Unit) {
    Spacer(modifier = Modifier.height(48.dp))
    Button(onClick = onEmergency) {
        Text("Llamar al 911")
    }
}