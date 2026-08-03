package com.proyecto_final.triage.screens

import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.proyecto_final.triage.viewmodels.SelectTypeGuardViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import com.proyecto_final.triage.components.ProgressBar
import com.proyecto_final.triage.network.EspecialidadResponse
import com.proyecto_final.triage.theme.Spacing
import com.proyecto_final.triage.viewmodels.EspecialidadesState

class SelectTypeGuardScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val viewModel = remember {SelectTypeGuardViewModel()}
        SelectTypeGuardContent(
            viewModel = viewModel,
            onBack = { navigator?.pop() },
            onContinue = { codigoEspecialidad -> navigator?.push(getSelectLocationScreen(codigoEspecialidad)) }
        )
    }
}

@Preview
@Composable
fun SelectTypeGuardPreview() {
    AppTheme {
        SelectTypeGuardContent(viewModel = SelectTypeGuardViewModel(), onBack = { }, onContinue = { })
    }
}

@Composable
fun SelectTypeGuardContent(viewModel: SelectTypeGuardViewModel, onBack: () -> Unit, onContinue: (String) -> Unit) {

    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.cargarEspecialidades()
    }

    var selectedEspecialidad by remember { mutableStateOf<EspecialidadResponse?>(null) }

    Column( modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp)
    ) {
        CommonHeader(title = "1. Elegí el tipo de guardia",
            subtitle = "Seleccioná el tipo de guardia al que deseas acceder",
            showLogo = true,
            onBack = { onBack() })

        ProgressBar(currentStep = 1, totalSteps = 3)

        Spacer(modifier = Modifier.height(Spacing.lg))

        when (state) {
            is EspecialidadesState.Loading -> {
                Text("Cargando especialidades...")
            }

            is EspecialidadesState.Success -> {
                val especialidades = (state as EspecialidadesState.Success).especialidades

                val options = especialidades.map {
                    SelectableOption(
                        it.nombre,
                        Icons.Default.LocalHospital
                    )
                }

                SelectableOptionsGrid(
                    options = options,
                    selectedOption = selectedEspecialidad?.nombre,
                    onOptionSelected = { nombre ->
                        selectedEspecialidad = especialidades.find { it.nombre == nombre }
                    }
                )
            }

            is EspecialidadesState.Error -> {
                Text((state as EspecialidadesState.Error).message)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button( onClick = {
                selectedEspecialidad?.let { onContinue(it.codigo) }
        },
            enabled = selectedEspecialidad != null,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(54.dp)
            ) { Text("Continuar") }

        Spacer(modifier = Modifier.height(24.dp))
    }
}