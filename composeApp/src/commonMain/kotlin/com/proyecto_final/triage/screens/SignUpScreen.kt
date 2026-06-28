package com.proyecto_final.triage.screens

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import cafe.adriel.voyager.core.screen.Screen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import cafe.adriel.voyager.navigator.LocalNavigator
import com.proyecto_final.triage.theme.AppTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import triage.composeapp.generated.resources.Res
import triage.composeapp.generated.resources.logo
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.proyecto_final.triage.components.InfoBanner
import com.proyecto_final.triage.components.InputDropdownField
import com.proyecto_final.triage.components.InputTextField
import com.proyecto_final.triage.components.ProgressBar
import com.proyecto_final.triage.network.RegisterRequest
import com.proyecto_final.triage.theme.Spacing
import com.proyecto_final.triage.viewmodels.SignUpState
import com.proyecto_final.triage.viewmodels.SignUpViewModel

class SignUpScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val viewModel = remember { SignUpViewModel() }
        val state by viewModel.state.collectAsState()

        println("STATE ACTUAL: $state")

        LaunchedEffect(state) {
            println("LAUNCHED EFFECT STATE: $state")
            if (state is SignUpState.Success) {
                navigator?.replace(HomeScreen())
            }
        }

        SignUpContent(
            onBack = { navigator?.pop() },
            viewModel = viewModel,
            state = state
        )
    }
}

@Preview @Composable
fun SignUpPreview() {
    AppTheme {
        SignUpContent(onBack = {}, viewModel = SignUpViewModel(), state = SignUpState.Idle)
    }
}

@Composable
fun SignUpContent(onBack: () -> Unit, viewModel: SignUpViewModel, state: SignUpState) {

    var currentStep by remember { mutableStateOf(1) }
    var showStep1Errors by remember { mutableStateOf(false) }
    var showStep2Errors by remember { mutableStateOf(false) }
    var showStep3Errors by remember { mutableStateOf(false) }

    // PASO 1
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var repeatPassword by remember { mutableStateOf("") }

    // PASO 2
    var nombre by remember { mutableStateOf("") }
    var apellido by remember { mutableStateOf("") }
    var tipoDocumento by remember { mutableStateOf("") }
    var numeroDocumento by remember { mutableStateOf("") }
    var fechaNacimiento by remember { mutableStateOf("") }
    var genero by remember { mutableStateOf("") }
    var sexo by remember { mutableStateOf("") }

    // PASO 3
    var peso by remember { mutableStateOf("") }
    var altura by remember { mutableStateOf("") }

    val onCreateAccount = {
        val request = RegisterRequest(  nombre = nombre,
                                        apellido = apellido,
                                        numeroDocumento = numeroDocumento,
                                        tipoDocumento = tipoDocumento,
                                        tipoUsuario = "Paciente",
                                        email = email,
                                        password = password,
        )
        viewModel.createAccount(request)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(36.dp))

        // HEADER
        Box(modifier = Modifier.fillMaxWidth()) {
            Icon( imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                modifier =  Modifier.align(Alignment.CenterStart)
                                    .clickable { if (currentStep > 1) currentStep-- else onBack() })
            Image(painter = painterResource(Res.drawable.logo),
                contentDescription = "Logo",
                modifier = Modifier.size(48.dp)
                                    .align(Alignment.Center))
        }

        Spacer(modifier = Modifier.height(4.dp))

        // TITULO Y SUBTITULO
        Text( text = when (currentStep) {   1 -> "Creá tu cuenta"
                                            2 -> "Información personal"
                                            else -> "Información de salud"
            },
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text( text = when (currentStep) {   1 -> "Datos de acceso"
                                            else -> "Contanos un poco sobre vos" },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // PROGRESO
        ProgressBar(currentStep = currentStep, totalSteps = 3)
        Spacer(modifier = Modifier.height(Spacing.md))

        // Contenido según paso
        when (currentStep) {
            1 -> Step1Content(  email = email, onEmailChange = { email = it },
                                password = password, onPasswordChange = { password = it },
                                repeatPassword = repeatPassword, onRepeatPasswordChange = { repeatPassword = it },
                                showErrors = showStep1Errors)
            2 -> Step2Content(
                nombre = nombre,
                onNombreChange = { nombre = it },
                apellido = apellido,
                onApellidoChange = { apellido = it },
                tipoDocumento = tipoDocumento,
                onTipoDocumentoChange = { tipoDocumento = it },
                numeroDocumento = numeroDocumento,
                onNumeroDocumentoChange = { numeroDocumento = it },
                fechaNacimiento = fechaNacimiento,
                onFechaNacimientoChange = { fechaNacimiento = it },
                genero = genero,
                onGeneroChange = { genero = it },
                sexo = sexo,
                onSexoChange = { sexo = it }
            )
            3 -> Step3Content(
                peso = peso,
                onPesoChange = { peso = it },
                altura = altura,
                onAlturaChange = { altura = it },
                pesoError = showStep3Errors && peso.isBlank(),
                alturaError = showStep3Errors && altura.isBlank(),
                state = state
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button( onClick = { when (currentStep) {
            1 -> {
                showStep1Errors = true
                if(email.isNotBlank() && isValidEmail(email) && password.isNotBlank() && repeatPassword.isNotBlank() && password == repeatPassword)
                    currentStep = 2
            }
            2 -> {
                showStep2Errors = true
                if(nombre.isNotBlank() && apellido.isNotBlank())
                  currentStep = 3
            }
            3 -> {
                showStep3Errors = true
                if(peso.isNotBlank() && altura.isNotBlank())
                    onCreateAccount()
            }
        }},
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(54.dp)
        ) {
            Text(
                text = if (currentStep == 3) "Crear cuenta" else "Continuar",
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

    }
}

@Composable
fun Step1Content(   email: String, onEmailChange: (String) -> Unit,
                    password: String, onPasswordChange: (String) -> Unit,
                    repeatPassword: String, onRepeatPasswordChange: (String) -> Unit,
                    showErrors: Boolean
    ) {
    var passwordVisible by remember { mutableStateOf(false) }
    var repeatPasswordVisible by remember { mutableStateOf(false) }

    val hasMinLength = password.length in 8..32
    val hasUppercase = password.any { it.isUpperCase() }
    val hasLowercase = password.any { it.isLowerCase() }
    val hasSpecial = password.any { !it.isLetterOrDigit() }

    // Email
    val isEmailValid = isValidEmail(email)
    InputTextField( label = "Correo electrónico", value = email, onValueChange = onEmailChange, leadingIcon = Icons.Filled.Email,
                    isError = showErrors && (email.isBlank() || !isEmailValid),
                    errorMessage = if (email.isBlank()) "Este campo es obligatorio" else "Ingresá un correo válido")
    Spacer(modifier = Modifier.height(Spacing.sm))

    // Contraseña
    val isPasswordValid = hasMinLength && hasUppercase && hasLowercase && hasSpecial
    val passwordsMatch = password == repeatPassword
    InputTextField( label = "Contraseña", value = password, onValueChange = onPasswordChange, leadingIcon = Icons.Filled.Lock, isPassword = true,
        isError = showErrors && (password.isBlank() || !isPasswordValid),
        errorMessage = if (password.isBlank()) "Este campo es obligatorio" else "La contraseña no cumple los requisitos")
    Spacer(modifier = Modifier.height(Spacing.md))

    // Requisitos contraseña
    Text(text = "La contraseña debe tener:",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFB0BEC5)
    )
    Spacer(modifier = Modifier.height(6.dp))
    PasswordRequirement("Entre 8 y 32 caracteres", hasMinLength)
    PasswordRequirement("Al menos una letra mayúscula", hasUppercase)
    PasswordRequirement("Al menos una letra minúscula", hasLowercase)
    PasswordRequirement("Al menos un carácter especial", hasSpecial)

    Spacer(modifier = Modifier.height(16.dp))

    // Repetir contraseña
    InputTextField( label = "Repetir contraseña", value = repeatPassword, onValueChange = onRepeatPasswordChange, leadingIcon = Icons.Filled.Lock, isPassword = true,
        isError = showErrors && (repeatPassword.isBlank() || !passwordsMatch),
        errorMessage = if (repeatPassword.isBlank()) "Este campo es obligatorio" else "Las contraseñas no coinciden")
}

@Composable
fun Step2Content(nombre: String, onNombreChange: (String) -> Unit,
                 apellido: String, onApellidoChange: (String) -> Unit,
                 tipoDocumento: String, onTipoDocumentoChange: (String) -> Unit,
                 numeroDocumento: String, onNumeroDocumentoChange: (String) -> Unit,
                 fechaNacimiento: String, onFechaNacimientoChange: (String) -> Unit,
                 genero: String, onGeneroChange: (String) -> Unit,
                 sexo: String, onSexoChange: (String) -> Unit
) {
    // Nombre
    InputTextField(label = "Nombre", value = nombre, onValueChange = onNombreChange, leadingIcon = Icons.Filled.Person,
        isError = nombre.isBlank(),
        errorMessage = "Este campo es obligatorio")
    Spacer(modifier = Modifier.height(Spacing.sm))

    // Apellido
    InputTextField(label = "Apellido", value = apellido, onValueChange = onApellidoChange, leadingIcon = Icons.Filled.Person,
        isError = apellido.isBlank(),
        errorMessage = "Este campo es obligatorio")
    Spacer(modifier = Modifier.height(Spacing.sm))

    // Tipo y Número de documento
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            var expanded by remember { mutableStateOf(false) }
            InputDropdownField( label = "Tipo de documento", value = tipoDocumento, expanded = expanded, onExpandedChange = { expanded = it },
                options = listOf("DNI", "Lib. Cívica", "Lib. Enrolamiento"), onOptionSelected = { onTipoDocumentoChange(it) },
                displayMap = mapOf("Libreta Civica" to "LC", "Libreta de Enrolamiento" to "LE"), modifier = Modifier.fillMaxWidth()
            )
        }
        Column (modifier = Modifier.weight(1f)) {
            InputTextField(label = "N° de documento", value = numeroDocumento, onValueChange = onNumeroDocumentoChange, keyboardType = KeyboardType.Number)
        }
    }

    // Fecha de nacimiento
    Text("Fecha de nacimiento", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = fechaNacimiento, onValueChange = onFechaNacimientoChange,
        leadingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
        singleLine = true, shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(Spacing.md))

    // Género y Sexo
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Genero", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = genero, onValueChange = onGeneroChange,
                leadingIcon = { Icon(Icons.Filled.Transgender, contentDescription = null) },
                trailingIcon = { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null) },
                singleLine = true, shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("Sexo", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = sexo, onValueChange = onSexoChange,
                leadingIcon = { Icon(Icons.Filled.Transgender, contentDescription = null) },
                trailingIcon = { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null) },
                singleLine = true, shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun Step3Content( peso: String, onPesoChange: (String) -> Unit,
                  altura: String, onAlturaChange: (String) -> Unit,
                  pesoError: Boolean,
                  alturaError: Boolean,
                  state: SignUpState) {

    // Peso
    InputTextField(label = "Peso (kg)", value = peso, onValueChange = onPesoChange, leadingIcon = Icons.Filled.FitnessCenter, isError = pesoError, keyboardType = KeyboardType.Number)
    Spacer(modifier = Modifier.height(Spacing.sm))

    // Altura
    InputTextField(label = "Altura (cm)", value = altura, onValueChange = onAlturaChange, leadingIcon = Icons.Filled.Height, isError = alturaError, keyboardType = KeyboardType.Number)
    Spacer(modifier = Modifier.height(Spacing.sm))

    // Info
    InfoBanner( text = "Estos datos estarán protegidos y nos permitirán brindar una mejor experiencia" )

    if (state is SignUpState.Error) {
        Text(
            text = state.message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

fun isValidEmail(email: String): Boolean {
    val regex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    return regex.matches(email)
}

@Composable
fun PasswordRequirement(text: String, met: Boolean) {
    val color = if (met) Color(0xFF5BB8D4) else Color(0xFFB0BEC5)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}