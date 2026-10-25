package com.proyecto_final.triage.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import com.proyecto_final.triage.components.NextButton
import com.proyecto_final.triage.components.ScreenTitleBar
import com.proyecto_final.triage.theme.Spacing
import com.proyecto_final.triage.viewmodels.CambiarContraseniaState
import com.proyecto_final.triage.viewmodels.CambiarContraseniaViewModel

private val VERDE = Color(0xFF2E7D32)

class CambiarContraseniaScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val viewModel = remember { CambiarContraseniaViewModel() }
        val state by viewModel.state.collectAsStateWithLifecycle()

        CambiarContraseniaContent(
            state = state,
            onActualChange = viewModel::onActualChange,
            onNuevaChange = viewModel::onNuevaChange,
            onRepetirChange = viewModel::onRepetirChange,
            onCambiar = viewModel::cambiar,
            onBack = { navigator?.pop() }
        )
    }
}

@Composable
fun CambiarContraseniaContent(
    state: CambiarContraseniaState,
    onActualChange: (String) -> Unit,
    onNuevaChange: (String) -> Unit,
    onRepetirChange: (String) -> Unit,
    onCambiar: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        ScreenTitleBar(
            title = "Cambiar contraseña",
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        // SOLO ESTA PARTE SCROLLEA (ocupa todo el espacio disponible)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {

            if (state.exito) {
                CambioExitoso()
            } else {
                // CONTRASEÑA ACTUAL
                CampoContrasenia(
                    label = "Contraseña actual",
                    value = state.actual,
                    onValueChange = onActualChange,
                    enabled = !state.bloqueado
                )

                Spacer(modifier = Modifier.height(Spacing.md))

                // CONTRASEÑA NUEVA
                CampoContrasenia(
                    label = "Nueva contraseña",
                    value = state.nueva,
                    onValueChange = onNuevaChange,
                    enabled = !state.bloqueado
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                // CHECKLIST DE REGLAS
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.reglas.forEach { regla ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (regla.cumple) Icons.Filled.CheckCircle
                                else Icons.Filled.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (regla.cumple) VERDE
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = regla.descripcion,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (regla.cumple) VERDE
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.md))

                // REPETIR CONTRASEÑA NUEVA
                CampoContrasenia(
                    label = "Repetir nueva contraseña",
                    value = state.repetir,
                    onValueChange = onRepetirChange,
                    enabled = !state.bloqueado,
                    isError = state.repetir.isNotEmpty() && !state.coinciden,
                    errorMessage = "Las contraseñas no coinciden"
                )

                Spacer(modifier = Modifier.height(Spacing.md))

                // AVISO DE INTENTOS
                Text(
                    text = "Por seguridad, después de 3 intentos fallidos vas a tener que esperar 24 horas para volver a intentarlo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // ERROR / BLOQUEO
                state.error?.let { mensaje ->
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE5E5)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (state.bloqueado) Icons.Filled.Lock
                                else Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFB3261E),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = mensaje,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFB3261E)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.md))
            }
        }

        // STICKY: siempre abajo de todo
        Spacer(modifier = Modifier.height(12.dp))

        if (state.exito) {
            NextButton(
                onClick = onBack,
                text = "Volver"
            )
        } else {
            NextButton(
                onClick = onCambiar,
                enabled = state.puedeEnviar,
                text = "Cambiar contraseña",
                isLoading = state.enviando
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/* ============================================================
   CAMPO DE CONTRASEÑA CON MOSTRAR / OCULTAR
   ============================================================ */

@Composable
private fun CampoContrasenia(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    var visible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        isError = isError,
        supportingText = if (isError && errorMessage != null) {
            { Text(errorMessage) }
        } else null,
        leadingIcon = {
            Icon(imageVector = Icons.Filled.Lock, contentDescription = null)
        },
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Filled.VisibilityOff
                    else Icons.Filled.Visibility,
                    contentDescription = if (visible) "Ocultar contraseña"
                    else "Mostrar contraseña"
                )
            }
        },
        visualTransformation = if (visible) VisualTransformation.None
        else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

/* ============================================================
   ÉXITO
   ============================================================ */

@Composable
private fun CambioExitoso() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = VERDE,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            text = "¡Listo!",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            text = "Tu contraseña se cambió con éxito.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}