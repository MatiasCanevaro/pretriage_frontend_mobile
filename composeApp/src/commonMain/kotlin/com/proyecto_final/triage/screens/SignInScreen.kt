package com.proyecto_final.triage.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.proyecto_final.triage.theme.AppTheme
import org.jetbrains.compose.resources.painterResource
import triage.composeapp.generated.resources.Res
import triage.composeapp.generated.resources.logo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.proyecto_final.triage.components.InputTextField
import com.proyecto_final.triage.config.AppConfig
import com.proyecto_final.triage.config.Environment
import com.proyecto_final.triage.theme.Spacing
import com.proyecto_final.triage.viewmodels.SignInState
import com.proyecto_final.triage.viewmodels.SignInViewModel
import kotlinx.coroutines.launch

class SignInScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = remember { SignInViewModel() }
        val state by viewModel.state.collectAsState()

        LaunchedEffect(state) {
            if (state is SignInState.Success) {
                navigator.replace(HomeScreen())
            }
        }

        SignInContent(  onForgotPassword = { navigator.push(ForgotPasswordScreen()) },
                        onSignUp = { navigator.push(SignUpScreen()) },
                        onSignIn = { navigator.push(HomeScreen()) },
                        viewModel = viewModel,
                        state = state)
    }
}

@Preview
@Composable
fun SignInPreview() {
    AppTheme {
        SignInContent(
            onForgotPassword = { },
            onSignUp = { },
            onSignIn = { },
            viewModel = SignInViewModel(),
            state = SignInState.Idle
        )
    }
}

@Composable
fun SignInContent( onForgotPassword: () -> Unit,
                   onSignUp: () -> Unit,
                   onSignIn: () -> Unit,
                   viewModel: SignInViewModel,
                   state: SignInState
) {
    // variables
    var showErrors by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(Spacing.xxl))

        Image(
            painter = painterResource(Res.drawable.logo),
            contentDescription = "Logo",
            modifier = Modifier.size(100.dp)
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        Text(
            text = "Iniciá sesión",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(64.dp))

        // Correo Electronico
        val isEmailValid = isValidEmail(email)
        InputTextField( label = "Correo electrónico", value = email, onValueChange = { email = it }, leadingIcon = Icons.Filled.Email,
            isError = showErrors && (email.isBlank() || !isEmailValid),
            errorMessage = if (email.isBlank()) "Este campo es obligatorio" else "Ingresá un correo válido")
        Spacer(modifier = Modifier.height(Spacing.md))

        // Contraseña
        InputTextField(
            label = "Contraseña",
            value = password,
            onValueChange = { password = it },
            leadingIcon = Icons.Filled.Lock,
            isPassword = true,
            isError = showErrors && password.isBlank()
        )

        Text(
            text = "¿Olvidaste tu contraseña?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.End)
                .clickable { onForgotPassword() }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Recordarme
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.align(Alignment.Start).offset(x = (-12).dp)
        ) {
            Checkbox(
                checked = rememberMe,
                onCheckedChange = { rememberMe = it }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Recordarme",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Botón
        Button(
            onClick = {
                showErrors = true
                if(AppConfig.environment == Environment.DEV){
                    onSignIn()
                }
                else if (email.isNotBlank() && password.isNotBlank()) {
                    viewModel.login(email, password, rememberMe)
                }
            },
            enabled = state !is SignInState.Loading,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (state is SignInState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    text = "Iniciar sesión",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        if (state is SignInState.Error) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = state.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Registro
        Text(
            text = buildAnnotatedString {
                withStyle(
                    style = MaterialTheme.typography.bodyMedium.toSpanStyle().copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    append("¿No tenés cuenta? ")
                }
                withStyle(
                    style = MaterialTheme.typography.bodyMedium.toSpanStyle().copy(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.SemiBold
                    )
                ) {
                    append("Creá una")
                }
            },
            modifier = Modifier.clickable { onSignUp() }
        )
    }
}