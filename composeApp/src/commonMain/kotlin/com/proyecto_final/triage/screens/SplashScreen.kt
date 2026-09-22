package com.proyecto_final.triage.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import com.proyecto_final.triage.components.ErrorMessage
import com.proyecto_final.triage.config.AppConfig
import com.proyecto_final.triage.network.httpClient
import com.proyecto_final.triage.storage.TokenStorageProvider
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
                    print("Error al conectar con el servidor: ${e.message}")
                } finally {
                    StartupState.loading = false
                }
            }
        }

        LaunchedEffect(Unit) {
            iniciar()
        }

        // Mensaje de error en caso de que no se pueda conectar con el servidor
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (showPopup) {
                ErrorMessage(
                    onClick = {
                        showPopup = false
                        iniciar()
                    }
                )

            }
        }
    }
}

private fun continuar(navigator: Navigator?) {
    if (TokenStorageProvider.instance.getToken() != null) {
        navigator?.replace(HomeScreen())
    } else {
        navigator?.replace(SignInScreen())
    }
}

object StartupState {
    @Volatile
    var loading = true
}