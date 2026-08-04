package com.proyecto_final.triage.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import com.proyecto_final.triage.components.InputDateField
import com.proyecto_final.triage.components.InputDropdownField
import com.proyecto_final.triage.components.InputTextField
import com.proyecto_final.triage.network.PerfilResponse
import com.proyecto_final.triage.theme.AppTheme
import com.proyecto_final.triage.theme.Spacing
import com.proyecto_final.triage.viewmodels.EditProfileState
import com.proyecto_final.triage.viewmodels.EditProfileViewModel


class EditProfileScreen(
    private val perfil: PerfilResponse,
    private val onProfileUpdated: () -> Unit
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val viewModel = remember { EditProfileViewModel() }
        val state by viewModel.state.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            viewModel.prefill(perfil)
        }

        LaunchedEffect(state.saved) {
            if (state.saved) {
                onProfileUpdated()
                navigator?.pop()
            }
        }

        EditProfileContent(
            state = state,
            viewModel = viewModel,
            onBack = { navigator?.pop() }
        )
    }
}

@Preview
@Composable
fun EditProfilePreview() {
    AppTheme {
        EditProfileContent(
            state = EditProfileState(),
            viewModel = EditProfileViewModel(),
            onBack = { }
        )
    }
}

@Composable
fun EditProfileContent(
    state: EditProfileState,
    viewModel: EditProfileViewModel,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(Spacing.lg))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                modifier = Modifier.clickable { onBack() }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Editar perfil",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        // DATOS PERSONALES
        SectionTitle("Datos personales")

        InputTextField(
            label = "Nombre",
            value = state.nombre,
            onValueChange = { viewModel.onFieldChange("nombre", it) },
            leadingIcon = Icons.Filled.Person,
            placeholder = "Ingresá tu nombre",
            isError = state.errors["nombre"] != null,
            errorMessage = state.errors["nombre"] ?: "Este campo es obligatorio"
        )
        Spacer(modifier = Modifier.height(Spacing.sm))

        InputTextField(
            label = "Apellido",
            value = state.apellido,
            onValueChange = { viewModel.onFieldChange("apellido", it) },
            leadingIcon = Icons.Filled.Person,
            placeholder = "Ingresá tu apellido",
            isError = state.errors["apellido"] != null,
            errorMessage = state.errors["apellido"] ?: "Este campo es obligatorio"
        )
        Spacer(modifier = Modifier.height(Spacing.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                var tipoDocumentoExpanded by remember { mutableStateOf(false) }
                InputDropdownField(
                    label = "Tipo de documento",
                    value = state.tipoDocumento,
                    expanded = tipoDocumentoExpanded,
                    onExpandedChange = { tipoDocumentoExpanded = it },
                    options = listOf("DNI", "Lib. Cívica", "Lib. Enrolamiento"),
                    onOptionSelected = { viewModel.onFieldChange("tipoDocumento", it) },
                    placeholder = "Seleccioná un tipo",
                    isError = state.errors["tipoDocumento"] != null,
                    errorMessage = state.errors["tipoDocumento"] ?: "Este campo es obligatorio"
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                InputTextField(
                    label = "N° de documento",
                    value = state.numeroDocumento,
                    onValueChange = { viewModel.onFieldChange("numeroDocumento", it) },
                    keyboardType = KeyboardType.Number,
                    placeholder = "Ingresá tu número",
                    isError = state.errors["numeroDocumento"] != null,
                    errorMessage = state.errors["numeroDocumento"] ?: "Este campo es obligatorio"
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        InputDateField(
            label = "Fecha de nacimiento",
            value = state.fechaNacimiento,
            onValueChange = { viewModel.onFieldChange("fechaNacimiento", it) },
            placeholder = "DD/MM/AAAA",
            isError = state.errors["fechaNacimiento"] != null,
            errorMessage = state.errors["fechaNacimiento"] ?: "Este campo es obligatorio"
        )

        // GENERO
        SectionTitle("Identidad de género")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                var sexoExpanded by remember { mutableStateOf(false) }
                InputDropdownField(
                    label = "Sexo biológico",
                    value = state.generoBiologico,
                    expanded = sexoExpanded,
                    onExpandedChange = { sexoExpanded = it },
                    options = listOf("Masculino", "Femenino"),
                    onOptionSelected = { viewModel.onFieldChange("generoBiologico", it) },
                    placeholder = "Seleccioná un sexo",
                    isError = state.errors["generoBiologico"] != null,
                    errorMessage = state.errors["generoBiologico"] ?: "Este campo es obligatorio"
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                var generoExpanded by remember { mutableStateOf(false) }
                InputDropdownField(
                    label = "Género",
                    value = state.generoConElQueSeIdentifica,
                    expanded = generoExpanded,
                    onExpandedChange = { generoExpanded = it },
                    options = listOf("Masculino", "Femenino", "Otro"),
                    onOptionSelected = { viewModel.onFieldChange("generoConElQueSeIdentifica", it) },
                    placeholder = "Seleccioná un género",
                    isError = state.errors["generoConElQueSeIdentifica"] != null,
                    errorMessage = state.errors["generoConElQueSeIdentifica"] ?: "Este campo es obligatorio"
                )
            }
        }

        // CONTACTO
        SectionTitle("Contacto")

        InputTextField(
            label = "Correo electrónico",
            value = state.email,
            onValueChange = { viewModel.onFieldChange("email", it) },
            leadingIcon = Icons.Filled.Email,
            keyboardType = KeyboardType.Email,
            placeholder = "nombre@ejemplo.com",
            isError = state.errors["email"] != null,
            errorMessage = state.errors["email"] ?: "Este campo es obligatorio"
        )
        Spacer(modifier = Modifier.height(Spacing.sm))

        InputTextField(
            label = "Teléfono",
            value = state.telefono,
            onValueChange = { viewModel.onFieldChange("telefono", it) },
            leadingIcon = Icons.Filled.Phone,
            keyboardType = KeyboardType.Phone,
            placeholder = "Ingresá tu teléfono",
            isError = state.errors["telefono"] != null,
            errorMessage = state.errors["telefono"] ?: "Ingresá un teléfono válido"
        )

        // DOMICILIO
        SectionTitle("Domicilio")

        InputTextField(
            label = "Calle",
            value = state.calle,
            onValueChange = { viewModel.onFieldChange("calle", it) },
            leadingIcon = Icons.Filled.Home,
            placeholder = "Ingresá tu calle",
            isError = state.errors["calle"] != null,
            errorMessage = state.errors["calle"] ?: "Este campo es obligatorio"
        )
        Spacer(modifier = Modifier.height(Spacing.sm))

        InputTextField(
            label = "Altura",
            value = state.alturaDireccion,
            onValueChange = { viewModel.onFieldChange("alturaDireccion", it) },
            keyboardType = KeyboardType.Number,
            placeholder = "Ingresá la altura",
            isError = state.errors["alturaDireccion"] != null,
            errorMessage = state.errors["alturaDireccion"] ?: "Ingresá un valor válido"
        )
    
        InputTextField(
            label = "Piso (Opcional)",
            value = state.piso,
            onValueChange = { viewModel.onFieldChange("piso", it) },
            keyboardType = KeyboardType.Number,
            placeholder = "Ingresá el piso",
            isError = state.errors["piso"] != null,
            errorMessage = state.errors["piso"] ?: "Ingresá un valor válido"
        )    
        

        Spacer(modifier = Modifier.height(Spacing.sm))
        
        InputTextField(
            label = "Código postal",
            value = state.codigoPostal,
            onValueChange = { viewModel.onFieldChange("codigoPostal", it) },
            keyboardType = KeyboardType.Number,
            placeholder = "Ingresá el código postal",
            isError = state.errors["codigoPostal"] != null,
            errorMessage = state.errors["codigoPostal"] ?: "Ingresá un código postal válido"
        )
    
        InputTextField(
            label = "Ciudad",
            value = state.ciudad,
            onValueChange = { viewModel.onFieldChange("ciudad", it) },
            leadingIcon = Icons.Filled.Place,
            placeholder = "Ingresá tu ciudad",
            isError = state.errors["ciudad"] != null,
            errorMessage = state.errors["ciudad"] ?: "Este campo es obligatorio"
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        InputTextField(
            label = "Provincia",
            value = state.provincia,
            onValueChange = { viewModel.onFieldChange("provincia", it) },
            leadingIcon = Icons.Filled.Map,
            placeholder = "Ingresá tu provincia",
            isError = state.errors["provincia"] != null,
            errorMessage = state.errors["provincia"] ?: "Este campo es obligatorio"
        )

        // SALUD
        SectionTitle("Salud")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                InputTextField(
                    label = "Peso (kg)",
                    value = state.peso,
                    onValueChange = { viewModel.onFieldChange("peso", it) },
                    leadingIcon = Icons.Filled.FitnessCenter,
                    keyboardType = KeyboardType.Decimal,
                    placeholder = "Ej: 70",
                    isError = state.errors["peso"] != null,
                    errorMessage = state.errors["peso"] ?: "Ingresá un peso válido"
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                InputTextField(
                    label = "Altura (cm)",
                    value = state.alturaPersona,
                    onValueChange = { viewModel.onFieldChange("alturaPersona", it) },
                    leadingIcon = Icons.Filled.Height,
                    keyboardType = KeyboardType.Number,
                    placeholder = "Ej: 170",
                    isError = state.errors["alturaPersona"] != null,
                    errorMessage = state.errors["alturaPersona"] ?: "Ingresá una altura válida"
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        state.generalError?.let { mensaje ->
            Text(
                text = mensaje,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.sm)
            )
        }

        Button(
            onClick = { viewModel.guardar() },
            enabled = !state.isSaving,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5BB8D4)),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Guardar cambios",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.lg))
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.md, bottom = Spacing.sm)
    )
}
