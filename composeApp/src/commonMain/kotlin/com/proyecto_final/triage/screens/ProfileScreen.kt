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
import com.proyecto_final.triage.components.SelectableOption
import com.proyecto_final.triage.components.SelectableOptionsGrid
import com.proyecto_final.triage.network.TokenStorage
import com.proyecto_final.triage.theme.AppTheme
import com.proyecto_final.triage.theme.Spacing
import org.jetbrains.compose.resources.painterResource
import triage.composeapp.generated.resources.Res
import triage.composeapp.generated.resources.credential
import triage.composeapp.generated.resources.profile

class ProfileScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        ProfileContent(
            onHealthPlan = { navigator?.push(HealthPlanScreen()) },
            onMyStudies = { navigator?.push(MyStudiesScreen()) },
            onLogOut = {
                TokenStorage.clearToken()
                navigator?.popUntilRoot()
                navigator?.replace(SignInScreen())
            },
            onBack = { navigator?.pop() }
        )
    }
}

@Preview
@Composable
fun ProfilePreview() {
    AppTheme {
        ProfileContent(
            onHealthPlan = { },
            onMyStudies = { },
            onLogOut = { },
            onBack = { }
        )
    }
}

@Composable
fun ProfileContent(
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

        // Card de perfil con foto y nombre
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
                        text = "Rocio Pérez",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Grid de opciones
        SelectableOptionsGrid(
            options = opciones,
            selectedOption = null,
            onOptionSelected = { }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Campos de información del perfil
        ProfileField(icon = Icons.Default.Person, label = "Nombre", value = "Rocio")
        ProfileField(icon = Icons.Default.Person, label = "Apellido", value = "Perez")
        ProfileFieldWithArrow(label = "Tipo de documento", value = "DNI")
        ProfileField(icon = Icons.Default.Badge, label = "Nº de documento", value = "42.123.456")
        ProfileField(icon = Icons.Default.CalendarMonth, label = "Fecha de nacimiento", value = "15/05/2001")
        ProfileFieldWithArrow(label = "Sexo", value = "Masculino")
        ProfileFieldWithArrow(label = "Genero", value = "Hombre")
        ProfileField(icon = Icons.Default.FitnessCenter, label = "Peso", value = "72 kg")
        ProfileField(icon = Icons.Default.Height, label = "Altura", value = "1.78 m")

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { },
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