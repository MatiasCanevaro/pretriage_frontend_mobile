package com.proyecto_final.triage.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import com.proyecto_final.triage.components.SelectableOption
import com.proyecto_final.triage.components.SelectableOptionsGrid
import com.proyecto_final.triage.network.TokenStorage
import com.proyecto_final.triage.theme.AppTheme
import com.proyecto_final.triage.theme.Spacing

class ProfileScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        ProfileContent(
            onHealthPlan = { navigator?.push(HealthPlanScreen()) },
            onMyStudies = { },
            onLogOut = { TokenStorage.clearToken()
                         navigator?.popUntilRoot()
                         navigator?.replace(SignInScreen()) }
        )
    }
}

@Preview
@Composable
fun ProfilePreview() {
    AppTheme {
        ProfileContent(
            onHealthPlan = { },
            onMyStudies = { },
            onLogOut = { }
        )
    }
}

@Composable
fun ProfileContent(
    onHealthPlan: () -> Unit,
    onMyStudies: () -> Unit,
    onLogOut: () -> Unit
) {
    val opciones = listOf(
        SelectableOption("Mis credenciales", Icons.Default.Badge, onClick = onHealthPlan),
        SelectableOption("Mis estudios", Icons.Default.Description, onClick = onMyStudies)
    )

    Column(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(Spacing.lg))

        SelectableOptionsGrid(options = opciones, selectedOption = null, onOptionSelected = { })

        Spacer(modifier = Modifier.weight(1f))

        Button(onClick = onLogOut) {
            Text("Cerrar Sesión")
        }
    }
}