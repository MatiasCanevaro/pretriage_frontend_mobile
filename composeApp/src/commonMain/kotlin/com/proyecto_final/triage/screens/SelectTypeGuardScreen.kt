package com.proyecto_final.triage.screens

import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import com.proyecto_final.triage.components.CommonHeader
import com.proyecto_final.triage.components.SelectableOption
import com.proyecto_final.triage.components.SelectableOptionsGrid
import com.proyecto_final.triage.theme.AppTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class SelectTypeGuardScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        SelectTypeGuardContent(
            onBack = { navigator?.pop()},
            onContinue = { type -> navigator?.push(getSelectLocationScreen(type)) }
        )
    }
}

@Preview
@Composable
fun SelectTypeGuardPreview() {
    AppTheme {
        SelectTypeGuardContent(onBack = { }, onContinue = { })
    }
}

@Composable
fun SelectTypeGuardContent(onBack: () -> Unit, onContinue: (String) -> Unit) {

    var selectedOption by remember { mutableStateOf<String?>(null) }

    val options = listOf(
        SelectableOption("Clínica médica", Icons.Default.LocalHospital),
        SelectableOption("Oftalmología", Icons.Default.Visibility),
        SelectableOption("Cardiología", Icons.Default.Favorite),
        SelectableOption("Odontología", Icons.Default.MedicalServices),
        SelectableOption("Pediatría", Icons.Default.ChildCare),
        SelectableOption("Traumatología", Icons.Default.AccessibilityNew),
        SelectableOption("Otorrinolaringología", Icons.Default.Hearing),
        SelectableOption("Ginecología", Icons.Default.Female)
    )

    Column(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        CommonHeader(title = "1. Elegí el tipo de guardia", onBack = onBack)

        SelectableOptionsGrid(options = options, selectedOption = selectedOption, onOptionSelected = { selectedOption = it })

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                selectedOption?.let {
                    onContinue(it)
                }
            },
            enabled = selectedOption != null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text("Continuar")
        }

    }
}