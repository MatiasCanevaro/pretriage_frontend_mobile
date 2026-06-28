package com.proyecto_final.triage.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import com.proyecto_final.triage.components.InputTextField
import com.proyecto_final.triage.theme.AppTheme
import com.proyecto_final.triage.theme.Spacing
import kotlinx.coroutines.launch

data class Credencial(
    val nombreObraSocial: String,
    val nombreAfiliado: String,
    val numeroAfiliado: String
)

val coloresCards = listOf(
    Color(0xFF192DAD),
    Color(0xFF66BB6A),
    Color(0xFFFF8A65),
    Color(0xFF9575CD)
)

class HealthPlanScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        HealthPlanContent(
            onBack = { navigator?.pop() }
        )
    }
}

@Preview
@Composable
fun HealthPlanPreview() {
    AppTheme {
        HealthPlanContent(onBack = {})
    }
}

@Composable
fun HealthPlanContent(onBack: () -> Unit) {

    var showErrors by remember { mutableStateOf(false) }
    var nombreObraSocial by remember { mutableStateOf("") }
    var esPrepaga by remember { mutableStateOf(true) }
    var numeroAfiliado by remember { mutableStateOf("") }

    val credenciales = remember {
        listOf(
            Credencial("OSDE", "Matias G. Pérez", "12345678"),
            Credencial("Swiss Medical", "Matias G. Pérez", "87654321")
        )
    }

    // +1 para la card de "agregar nueva"
    val pagerState = rememberPagerState(pageCount = { credenciales.size + 1 })
    val scope = rememberCoroutineScope()

    val showForm = pagerState.currentPage == credenciales.size

    val isNombreValid = nombreObraSocial.isNotBlank() && nombreObraSocial.all { it.isLetter() || it.isWhitespace() }
    val isNumeroValid = numeroAfiliado.length >= 6 && numeroAfiliado.all { it.isDigit() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(Spacing.lg))

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Volver",
            modifier = Modifier.clickable { onBack() }
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        Text(
            text = "Mis Credenciales",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Carrusel
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) { page ->
            if (page < credenciales.size) {
                val credencial = credenciales[page]
                val colorCard = coloresCards[page % coloresCards.size]

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = colorCard),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(horizontal = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = credencial.nombreObraSocial,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Text(
                            text = credencial.nombreAfiliado,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White
                        )
                        Text(
                            text = "Afiliado: ${credencial.numeroAfiliado}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White
                        )
                    }
                }
            } else {
                // Card gris de "agregar nueva"
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE0E0E0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(horizontal = 4.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Agregar credencial",
                            tint = Color(0xFF9E9E9E),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Indicador de puntos
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(credenciales.size + 1) { index ->
                val isLast = index == credenciales.size
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (index == pagerState.currentPage) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                index == pagerState.currentPage && isLast -> Color(0xFF9E9E9E)
                                index == pagerState.currentPage -> coloresCards[index % coloresCards.size]
                                else -> Color(0xFFD0D0D0)
                            }
                        )
                        .clickable {
                            scope.launch { pagerState.animateScrollToPage(index) }
                        }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Formulario, solo visible si showForm es true
        if (showForm) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    InputTextField(
                        label = "Nombre de la obra social",
                        value = nombreObraSocial,
                        onValueChange = { nombreObraSocial = it },
                        isError = showErrors && !isNombreValid,
                        errorMessage = if (nombreObraSocial.isBlank()) "Este campo es obligatorio" else "Solo se permiten letras"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Tipo (Obra Social o Prepaga)",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.background)
                            .padding(4.dp)
                    ) {
                        val opciones = listOf("Obra Social" to false, "Prepaga" to true)
                        opciones.forEach { (label, esPrepagaOpcion) ->
                            val seleccionado = esPrepaga == esPrepagaOpcion
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (seleccionado) Color(0xFF192DAD) else Color.Transparent)
                                    .clickable { esPrepaga = esPrepagaOpcion }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (seleccionado) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    InputTextField(
                        label = "Número de afiliado",
                        value = numeroAfiliado,
                        onValueChange = { numeroAfiliado = it },
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                        isError = showErrors && !isNumeroValid,
                        errorMessage = if (numeroAfiliado.isBlank()) "Este campo es obligatorio" else "Mínimo 6 caracteres"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    showErrors = true
                    if (isNombreValid && isNumeroValid) {
                        // TODO: guardar credencial
                    }
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF192DAD)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "Aceptar",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(16.dp))
    }
}