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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.proyecto_final.triage.theme.TextPrimary
import com.proyecto_final.triage.theme.TextSecondary

class SignInScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        SignInContent(  onForgotPassword = { navigator.push(ForgotPasswordScreen()) },
                        onSignIn = { navigator.replace(HomeScreen()) },
                        onSignUp = { navigator.push(SignUpScreen()) }
                    )
    }
}

@Composable
fun SignInContent( onForgotPassword: () -> Unit,
                   onSignIn: () -> Unit,
                   onSignUp: () -> Unit
) {
    // variables
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }

    Column( modifier = Modifier.fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(48.dp))

        Image(painter = painterResource(Res.drawable.logo), contentDescription = "Logo", modifier = Modifier.size(100.dp))

        Spacer(modifier = Modifier.height(12.dp))

        Text(text = "Iniciá sesión", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)

        Spacer(modifier = Modifier.height(64.dp))

        Text(text = "Correo electronico",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.align(Alignment.Start))

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text(   text = "Ejemplo@ejemplo.com",
                                    style = MaterialTheme.typography.bodyMedium
                            )},
            leadingIcon = { Icon(   imageVector = Icons.Filled.Email,
                                    contentDescription = "Email"
                            )},
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(  unfocusedBorderColor = TextSecondary,
                                                        unfocusedLeadingIconColor = TextSecondary,
                                                        unfocusedPlaceholderColor = TextSecondary,
                                                        unfocusedTextColor = TextPrimary,
                                                        focusedTextColor = TextPrimary
                                                    )
        )

        Spacer(modifier = Modifier.height(16.dp))
        
        Text(text = "Contraseña",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Contraseña"
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                    contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                    modifier = Modifier.clickable { passwordVisible = !passwordVisible }
                )
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(  unfocusedBorderColor = TextSecondary,
                                                        unfocusedLeadingIconColor = TextSecondary,
                                                        unfocusedTextColor = TextPrimary,
                                                        focusedTextColor = TextPrimary,
                                                        unfocusedTrailingIconColor = TextSecondary
                                                    )
        )

        Spacer(modifier = Modifier.height(8.dp))

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
            onClick = onSignIn,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                text = "Iniciar sesión",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary
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

@Preview
@Composable
fun SignInPreview() {
    AppTheme {
        SignInContent(  onForgotPassword = { },
                        onSignIn = { },
                        onSignUp = { }
                    )
    }
}
