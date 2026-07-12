package com.proyecto_final.triage.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import com.proyecto_final.triage.components.ErrorBanner
import com.proyecto_final.triage.config.AppConfig
import com.proyecto_final.triage.network.TokenStorage
import com.proyecto_final.triage.network.httpClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.get
import kotlinx.coroutines.launch
import kotlin.concurrent.Volatile

class SplashScreen : Screen {

    @Composable
    override fun Content() {

        val navigator = LocalNavigator.current
        val scope = rememberCoroutineScope()

        var showPopup by remember { mutableStateOf(false) }

        fun iniciar() {
            scope.launch {
                try {
                    StartupState.loading = true

                    httpClient.get("${AppConfig.baseUrl}/")

                    continuar(navigator)

                } catch (e: ClientRequestException) {
                    // 401, 403, 404...
                    continuar(navigator)

                } catch (e: ServerResponseException) {
                    // 5xx
                    continuar(navigator)

                } catch (e: Exception) {
                    showPopup = true
                    println("EXCEPCIÓN: ${e::class.qualifiedName}")
                    println("MENSAJE: ${e.message}")
                } finally {
                    StartupState.loading = false
                }
            }
        }

        LaunchedEffect(Unit) {
            iniciar()
        }

        Box(modifier = Modifier.fillMaxSize()
        ) {
            if (showPopup) {
                Box(modifier = Modifier.align(Alignment.TopCenter)) {
                    ErrorBanner(
                        message = "No se pudo conectar con el servidor.",
                        buttonText = "Reintentar",
                        icon = Icons.Default.Warning,
                        onButtonClick = {
                            showPopup = false
                            iniciar()
                        }
                    )
                }
            }
        }
        // TODO: falta validar que el token no esté vencido.
    }
}

private fun continuar(navigator: Navigator?) {
    if (TokenStorage.getToken() != null) {
        navigator?.replace(HomeScreen())
    } else {
        navigator?.replace(SignInScreen())
    }
}

object StartupState {
    @Volatile
    var loading = true
}