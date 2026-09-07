package com.proyecto_final.triage.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import com.proyecto_final.triage.components.InputTextField
import com.proyecto_final.triage.theme.Spacing
import com.proyecto_final.triage.viewmodels.ForgotPasswordState
import com.proyecto_final.triage.viewmodels.ForgotPasswordStep
import com.proyecto_final.triage.viewmodels.ForgotPasswordViewModel
import org.jetbrains.compose.resources.painterResource
import triage.composeapp.generated.resources.Res
import triage.composeapp.generated.resources.logo

class ForgotPasswordScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val viewModel = remember { ForgotPasswordViewModel() }

        val step by viewModel.step.collectAsState()
        val state by viewModel.state.collectAsState()
        val countdownSec by viewModel.countdownSec.collectAsState()

        // Redirigir al login cuando el cambio fue exitoso
        LaunchedEffect(state) {
            if (state is ForgotPasswordState.Success) {
                val msg = (state as ForgotPasswordState.Success).message
                RegistrationSuccessMessage.message = msg.ifBlank { "Contraseña cambiada con éxito" }
                navigator?.pop()
            }
        }

        ForgotPasswordContent(
            step = step,
            state = state,
            countdownSec = countdownSec,
            onBack = {
                val handled = viewModel.goBack()
                if (!handled) {
                    navigator?.pop()
                }
            },
            onSolicitarToken = { email ->
                viewModel.solicitarToken(email)
            },
            onValidarToken = { token ->
                viewModel.validarToken(token)
            },
            onCambiarContrasenia = { nueva ->
                viewModel.cambiarContrasenia(nueva)
            },
            onClearError = {
                viewModel.clearError()
            }
        )
    }
}

@Composable
fun ForgotPasswordContent(
    step: ForgotPasswordStep,
    state: ForgotPasswordState,
    countdownSec: Int,
    onBack: () -> Unit,
    onSolicitarToken: (String) -> Unit,
    onValidarToken: (String) -> Unit,
    onCambiarContrasenia: (String) -> Unit,
    onClearError: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var nuevaContrasenia by remember { mutableStateOf("") }
    var confirmarContrasenia by remember { mutableStateOf("") }

    var showEmailErrors by remember { mutableStateOf(false) }
    var showTokenErrors by remember { mutableStateOf(false) }
    var showPasswordErrors by remember { mutableStateOf(false) }

    val isLoading = state is ForgotPasswordState.Loading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(36.dp))

        // =====================================================
        // HEADER
        // =====================================================
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .clickable { onBack() }
            )

            Image(
                painter = painterResource(Res.drawable.logo),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = when (step) {
                ForgotPasswordStep.Email -> "Recuperar contraseña"
                ForgotPasswordStep.Token -> "Ingresá el código"
                ForgotPasswordStep.NewPassword -> "Nueva contraseña"
            },
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = when (step) {
                ForgotPasswordStep.Email -> "Ingresá tu correo para recibir el código"
                ForgotPasswordStep.Token -> "Revisá tu bandeja de entrada"
                ForgotPasswordStep.NewPassword -> "Definí tu nueva contraseña"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // =====================================================
        // CONTENIDO POR PASO
        // =====================================================
        when (step) {
            ForgotPasswordStep.Email -> {
                // Mensaje informativo leve
                // Input email
                val isEmailValid = isValidEmail(email)
                InputTextField(
                    label = "Correo electrónico",
                    value = email,
                    onValueChange = {
                        email = it
                        if (state is ForgotPasswordState.Error) onClearError()
                    },
                    leadingIcon = Icons.Filled.Email,
                    isError = showEmailErrors && (email.isBlank() || !isEmailValid),
                    errorMessage = if (email.isBlank()) "Este campo es obligatorio" else "Ingresá un correo válido",
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (state is ForgotPasswordState.Error) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            ForgotPasswordStep.Token -> {
                // Banner verde
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F5E9)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Revisá tu mail e ingresá el token que te llegó.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                InputTextField(
                    label = "Token",
                    value = token,
                    onValueChange = {
                        token = it
                        if (state is ForgotPasswordState.Error) onClearError()
                    },
                    leadingIcon = Icons.Filled.VpnKey,
                    isError = showTokenErrors && token.isBlank(),
                    errorMessage = "Este campo es obligatorio",
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Countdown
                val minutes = countdownSec / 60
                val seconds = countdownSec % 60
                val formatted = "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
                val countdownColor = if (countdownSec <= 120) Color(0xFFB3261E) else MaterialTheme.colorScheme.onSurfaceVariant

                Text(
                    text = "El código expira en: $formatted",
                    style = MaterialTheme.typography.bodyMedium,
                    color = countdownColor,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Si el tiempo expiró, volvé atrás y solicitá un nuevo código.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (state is ForgotPasswordState.Error) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            ForgotPasswordStep.NewPassword -> {
                val hasMinLength = nuevaContrasenia.length in 8..32
                val hasUppercase = nuevaContrasenia.any { it.isUpperCase() }
                val hasLowercase = nuevaContrasenia.any { it.isLowerCase() }
                val hasDigitOrSpecial = nuevaContrasenia.any { !it.isLetterOrDigit() || it.isDigit() }
                val isPasswordValid = hasMinLength && hasUppercase && hasLowercase && hasDigitOrSpecial
                val passwordsMatch = nuevaContrasenia == confirmarContrasenia

                InputTextField(
                    label = "Contraseña nueva",
                    value = nuevaContrasenia,
                    onValueChange = {
                        nuevaContrasenia = it
                        if (state is ForgotPasswordState.Error) onClearError()
                    },
                    leadingIcon = Icons.Filled.Lock,
                    isPassword = true,
                    isError = showPasswordErrors && (nuevaContrasenia.isBlank() || !isPasswordValid),
                    errorMessage = if (nuevaContrasenia.isBlank()) "Este campo es obligatorio" else "La contraseña no cumple los requisitos",
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(Spacing.md))

                Text(
                    text = "La contraseña debe tener:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFB0BEC5)
                )

                Spacer(modifier = Modifier.height(6.dp))

                PasswordRequirement(text = "Entre 8 y 32 caracteres", met = hasMinLength)
                PasswordRequirement(text = "Al menos una letra mayúscula", met = hasUppercase)
                PasswordRequirement(text = "Al menos una letra minúscula", met = hasLowercase)
                PasswordRequirement(text = "Al menos un número o carácter especial", met = hasDigitOrSpecial)

                Spacer(modifier = Modifier.height(16.dp))

                InputTextField(
                    label = "Confirmar contraseña nueva",
                    value = confirmarContrasenia,
                    onValueChange = {
                        confirmarContrasenia = it
                        if (state is ForgotPasswordState.Error) onClearError()
                    },
                    leadingIcon = Icons.Filled.Lock,
                    isPassword = true,
                    isError = showPasswordErrors && (confirmarContrasenia.isBlank() || !passwordsMatch),
                    errorMessage = if (confirmarContrasenia.isBlank()) "Este campo es obligatorio" else "Las contraseñas no coinciden",
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (state is ForgotPasswordState.Error) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // =====================================================
        // BOTÓN PRINCIPAL
        // =====================================================
        Button(
            onClick = {
                when (step) {
                    ForgotPasswordStep.Email -> {
                        showEmailErrors = true
                        if (email.isNotBlank() && isValidEmail(email)) {
                            onSolicitarToken(email)
                        }
                    }
                    ForgotPasswordStep.Token -> {
                        showTokenErrors = true
                        if (token.isNotBlank()) {
                            onValidarToken(token)
                        }
                    }
                    ForgotPasswordStep.NewPassword -> {
                        showPasswordErrors = true
                        val hasMinLength = nuevaContrasenia.length in 8..32
                        val hasUppercase = nuevaContrasenia.any { it.isUpperCase() }
                        val hasLowercase = nuevaContrasenia.any { it.isLowerCase() }
                        val hasDigitOrSpecial = nuevaContrasenia.any { !it.isLetterOrDigit() || it.isDigit() }
                        val isPasswordValid = hasMinLength && hasUppercase && hasLowercase && hasDigitOrSpecial
                        val passwordsMatch = nuevaContrasenia == confirmarContrasenia
                        if (nuevaContrasenia.isNotBlank() && isPasswordValid && confirmarContrasenia.isNotBlank() && passwordsMatch) {
                            onCambiarContrasenia(nuevaContrasenia)
                        }
                    }
                }
            },
            enabled = !isLoading,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    text = when (step) {
                        ForgotPasswordStep.Email -> "Continuar"
                        ForgotPasswordStep.Token -> "Continuar"
                        ForgotPasswordStep.NewPassword -> "Cambiar contraseña"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// Reutiliza validación de SignUpScreen para mantener consistencia
// Copia local para evitar dependencia cruzada frágil

private fun isValidEmailLocal(email: String): Boolean {
    val regex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    return regex.matches(email)
}
