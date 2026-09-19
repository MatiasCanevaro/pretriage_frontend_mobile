package com.proyecto_final.triage.screens

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import com.proyecto_final.triage.viewmodels.SelectLocationViewModel

actual fun getSelectLocationScreen(type: String): Screen = SelectLocationScreen(type)

class SelectLocationScreen(private val type: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val viewModel = viewModel<SelectLocationViewModel>()

        com.proyecto_final.triage.screens.SelectLocationContent(
            type = type,
            viewModel = viewModel,
            onBack = { navigator?.pop() },
            onContinue = { type, ubicacion ->
                navigator?.push(HospitalesScreen(type, ubicacion))
            }
        )
    }
}