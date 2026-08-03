package com.proyecto_final.triage.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.proyecto_final.triage.components.SelectableOption
import com.proyecto_final.triage.components.SelectableOptionsGrid
import com.proyecto_final.triage.network.PerfilResponse
import com.proyecto_final.triage.network.TokenStorage
import com.proyecto_final.triage.theme.AppTheme
import com.proyecto_final.triage.theme.Spacing
import com.proyecto_final.triage.viewmodels.ProfileState
import com.proyecto_final.triage.viewmodels.ProfileViewModel
import org.jetbrains.compose.resources.painterResource
import triage.composeapp.generated.resources.Res
import triage.composeapp.generated.resources.credential
import triage.composeapp.generated.resources.profile

class ProfileScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val viewModel = remember { ProfileViewModel() }
        val state by viewModel.state.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            viewModel.cargarPerfil()
        }

        ProfileContent(
            state = state,
            onRetry = { viewModel.cargarPerfil() },
            onEditProfile = { perfil ->
                navigator?.push(
                    EditProfileScreen(
                        perfil = perfil,
                        onProfileUpdated = { viewModel.cargarPerfil() }
                    )
                )
            },
            onHealthPlan = { navigator?.push(HealthPlanScreen()) },
            onMyStudies = {
                //TODO
            },
            onLogOut = {
                TokenStorage.clearToken()
                navigator?.popUntilRoot()
                navigator?.replace(SignInScreen())
            },
            onBack = { navigator?.pop() }
        )
    }
}

private val perfilDeEjemplo = PerfilResponse(
    nombre = "Rocio",
    apellido = "Perez",
    tipoDocumento = "DNI",
    numeroDocumento = "42.123.456",
    fechaNacimiento = "2001-05-15",
    generoBiologico = "FEMENINO",
    generoConElQueSeIdentifica = "FEMENINO",
    peso = 72.0,
    alturaPersona = 178
)

@Preview
@Composable
fun ProfilePreview() {
    AppTheme {
        ProfileContent(
            state = ProfileState.Success(perfilDeEjemplo),
            onRetry = { },
            onEditProfile = { },
            onHealthPlan = { },
            onMyStudies = { },
            onLogOut = { },
            onBack = { }
        )
    }
}

@Composable
fun ProfileContent(
    state: ProfileState,
    onRetry: () -> Unit,
    onEditProfile: (PerfilResponse) -> Unit,
    onHealthPlan: () -> Unit,
    onMyStudies: () -> Unit,
    onLogOut: () -> Unit,
    onBack: () -> Unit
) {
    val opciones = listOf(
        SelectableOption("Mis estudios clínicos", Icons.Default.BarChart, onClick = onMyStudies),
        SelectableOption("Mis credenciales", Icons.Default.CreditCard, onClick = onHealthPlan)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(Spacing.lg))

        // Header con título y botón cerrar sesión
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
                text = "Mi perfil",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(
                onClick = onLogOut,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFE53935)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE53935))
            ) {
                Text("Cerrar sesión", color = Color(0xFFE53935))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (state) {
            is ProfileState.Loading -> ProfileLoading()

            is ProfileState.Error -> ProfileError(
                message = state.message,
                onRetry = onRetry
            )

            is ProfileState.Success -> {
                val perfil = state.perfil

                ProfileHeaderCard(
                    nombreCompleto = "${perfil.nombre} ${perfil.apellido}"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Grid de opciones
                SelectableOptionsGrid(
                    options = opciones,
                    selectedOption = null,
                    onOptionSelected = { }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Campos de información del perfil
                ProfileField(icon = Icons.Default.Person, label = "Nombre", value = perfil.nombre)
                ProfileField(icon = Icons.Default.Person, label = "Apellido", value = perfil.apellido)
                ProfileFieldWithArrow(label = "Tipo de documento", value = perfil.tipoDocumento)
                ProfileField(icon = Icons.Default.Badge, label = "Nº de documento", value = perfil.numeroDocumento)
                ProfileField(
                    icon = Icons.Default.CalendarMonth,
                    label = "Fecha de nacimiento",
                    value = formatearFechaNacimiento(perfil.fechaNacimiento ?: "")
                )

                ProfileFieldWithArrow(label = "Sexo", value = perfil.generoBiologico ?: "")
                ProfileFieldWithArrow(label = "Genero", value = perfil.generoConElQueSeIdentifica ?: "")
                ProfileField(
                    icon = Icons.Default.FitnessCenter,
                    label = "Peso",
                    value = perfil.peso?.let {
                        if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
                    } ?: ""
                )
                ProfileField(
                    icon = Icons.Default.Height,
                    label = "Altura",
                    value = perfil.alturaPersona?.let { "${it} cm" } ?: ""
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { onEditProfile(perfil) },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5BB8D4)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = "Editar",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ProfileLoading() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ProfileError(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE5E5)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFB3261E)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No se pudo cargar el perfil",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFB3261E)
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFB3261E)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onRetry,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Reintentar", color = Color(0xFFB3261E))
                }
            }
        }
    }
}

@Composable
private fun ProfileHeaderCard(nombreCompleto: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFD6EAF8)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(Res.drawable.profile),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF5BB8D4)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = nombreCompleto,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

private fun formatearFechaNacimiento(fecha: String): String {
    val partes = fecha.split("-")
    if (partes.size != 3) return fecha

    return try {
        val anio = partes[0].toInt()
        val mes = partes[1].toInt()
        val dia = partes[2].toInt()
        "${dia.toString().padStart(2, '0')}/${mes.toString().padStart(2, '0')}/$anio"
    } catch (e: NumberFormatException) {
        fecha
    }
}

@Composable
fun ProfileField(
    icon: ImageVector,
    label: String,
    value: String
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
fun ProfileFieldWithArrow(
    label: String,
    value: String
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}