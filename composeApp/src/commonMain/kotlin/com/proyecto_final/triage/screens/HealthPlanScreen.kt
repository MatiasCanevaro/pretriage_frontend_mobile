package com.proyecto_final.triage.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import com.proyecto_final.triage.components.CommonHeader
import com.proyecto_final.triage.components.InputMonthYearField
import com.proyecto_final.triage.components.InputTextField
import com.proyecto_final.triage.theme.AppTheme
import com.proyecto_final.triage.theme.Spacing
import com.proyecto_final.triage.viewmodels.CredencialState
import com.proyecto_final.triage.viewmodels.CredencialesState
import com.proyecto_final.triage.viewmodels.HealthPlanViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.painterResource
import triage.composeapp.generated.resources.Res
import triage.composeapp.generated.resources.credential

@Serializable
data class Credencial(
    @SerialName("nombreObraSocial")
    val nombreObraSocial: String,

    @SerialName("numeroAfiliado")
    val numeroAfiliado: String,

    @SerialName("plan")
    val plan: String,

    @SerialName("fechaVencimiento")
    val fechaVencimiento: String
)

class HealthPlanScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val viewModel = remember { HealthPlanViewModel() }
        HealthPlanContent(
            onBack = { navigator?.pop() },
            viewModel = viewModel
        )
    }
}

@Preview
@Composable
fun HealthPlanPreview() {
    AppTheme {
        HealthPlanContent(onBack = {},
            viewModel = HealthPlanViewModel()
        )
    }
}

// Listas de ejemplo para los selects. Reemplazar por el origen de datos real.
private val obrasSocialesDisponibles = listOf("OSDE", "Swiss Medical", "Galeno", "Medifé", "IOMA")
private val planesDisponibles = listOf("210", "220", "310", "410", "Plan Único")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthPlanContent(onBack: () -> Unit,
                      viewModel: HealthPlanViewModel) {

    var showErrors by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    // isEditingExisting: si la card actual es una credencial ya cargada,
    // controla si el form está bloqueado (false) o habilitado para editar (true)
    var isEditingExisting by remember { mutableStateOf(false) }

    var nombreObraSocial by remember { mutableStateOf("") }
    var numeroAfiliado by remember { mutableStateOf("") }
    var plan by remember { mutableStateOf("") }
    var fechaVencimiento by remember { mutableStateOf("") }

    // OBTENGO LAS CREDENCIALES DEL SERVIDOR
    LaunchedEffect(Unit) {
        viewModel.obtenerCredenciales()
    }

    var nuevaCredencial by remember {
        mutableStateOf(
            Credencial("", "", "", "")
        )
    }

    val state by viewModel.credencialesState.collectAsState()
    val credencialState by viewModel.state.collectAsState()

    val backendFieldErrors =
        (credencialState as? CredencialState.Error)?.fieldErrors ?: emptyMap()

    val errorGeneral =
        (credencialState as? CredencialState.Error)?.message
            ?: (state as? CredencialesState.Error)?.message

    val credenciales = when (state) {
        is CredencialesState.Success ->
            (state as CredencialesState.Success).credenciales

        else -> emptyList<Credencial>()
    }
    val pagerState = rememberPagerState(pageCount = { credenciales.size + 1 })
    val scope = rememberCoroutineScope()

    val showForm = pagerState.currentPage == credenciales.size

    // Cada vez que cambia la página del carrusel, sincronizo el form:
    LaunchedEffect(credencialState) {
        if (credencialState is CredencialState.Success) {

            nuevaCredencial = Credencial("", "", "", "")
            nombreObraSocial = ""
            numeroAfiliado = ""
            plan = ""
            fechaVencimiento = ""

            showConfirmDialog = false
            isEditingExisting = false

            viewModel.resetState()
        }
    }

    LaunchedEffect(pagerState.currentPage, credenciales) {

        isEditingExisting = false
        showErrors = false
        viewModel.resetState()

        if (pagerState.currentPage < credenciales.size) {

            val credencial = credenciales[pagerState.currentPage]

            nombreObraSocial = credencial.nombreObraSocial
            numeroAfiliado = credencial.numeroAfiliado
            plan = credencial.plan
            fechaVencimiento = formatearFechaParaPicker(credencial.fechaVencimiento)

        } else {

            nombreObraSocial = nuevaCredencial.nombreObraSocial
            numeroAfiliado = nuevaCredencial.numeroAfiliado
            plan = nuevaCredencial.plan
            fechaVencimiento = nuevaCredencial.fechaVencimiento
        }
    }

    // El form está habilitado si estoy cargando una credencial nueva,
    // o si estoy editando una existente que fue desbloqueada con "Editar"
    val fieldsEnabled = showForm || isEditingExisting

    val mostrandoVistaPrevia =
        nuevaCredencial.nombreObraSocial.isNotBlank() ||
                nuevaCredencial.numeroAfiliado.isNotBlank() ||
                nuevaCredencial.plan.isNotBlank() ||
                nuevaCredencial.fechaVencimiento.isNotBlank()

    val isNombreValid = nombreObraSocial.isNotBlank() && nombreObraSocial.all { it.isLetter() || it.isWhitespace() }
    val isNumeroValid = numeroAfiliado.length >= 6 && numeroAfiliado.all { it.isDigit() }

    Column(modifier = Modifier.fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(horizontal = 16.dp)
    ) {
        //HEADER
        CommonHeader(title = "Mis Credenciales", onBack = { onBack() })

        // Contenido desplazable
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {

            //CARROUSEL
            HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) { page ->
            if (page < credenciales.size) {
                val credencial = credenciales[page]

                Card(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(horizontal = 4.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {

                        Image(
                            painter = painterResource(Res.drawable.credential),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {

                            Column {
                                Text(text = credencial.nombreObraSocial.uppercase(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White
                                )

                                Text(text = credencial.numeroAfiliado,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column {
                                    Text(
                                        text = "PLAN",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.75f)
                                    )
                                    Text(
                                        text = credencial.plan,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "VENCIMIENTO",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.75f)
                                    )
                                    Text(
                                        formatearVencimiento(credencial.fechaVencimiento),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Card gris/azul de "agregar nueva"
                Card(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(horizontal = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (mostrandoVistaPrevia)
                            Color.Transparent
                        else
                            Color(0xFFE0E0E0)
                    ),
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {

                        if (mostrandoVistaPrevia) {
                            Image(
                                painter = painterResource(Res.drawable.credential),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        if (mostrandoVistaPrevia) {

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {

                                Column {
                                    Text(
                                        text = nuevaCredencial.nombreObraSocial.uppercase(),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = nuevaCredencial.numeroAfiliado,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {

                                    Column {
                                        Text(
                                            text = "PLAN",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.75f)
                                        )

                                        Text(
                                            text = nuevaCredencial.plan,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White
                                        )
                                    }

                                    Column(
                                        modifier = Modifier.wrapContentWidth()
                                    ) {
                                        Text(
                                            text = "VENCIMIENTO",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.75f)
                                        )

                                        Text(
                                            text = formatearVencimientoForm(nuevaCredencial.fechaVencimiento),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                        } else {

                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
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
                                index == pagerState.currentPage -> Color(0xFF192DAD)
                                else -> Color(0xFFD0D0D0)
                            }
                        )
                        .clickable {
                            scope.launch { pagerState.animateScrollToPage(index) }
                        }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Form: ahora "suelto", sin Card contenedora, visible siempre
        // (ya sea mostrando una credencial existente bloqueada, o el alta de una nueva)
        Column(modifier = Modifier.fillMaxWidth()) {

            DropdownSelectField(
                label = "Obra social",
                value = nombreObraSocial,
                options = obrasSocialesDisponibles,
                enabled = fieldsEnabled,
                onValueChange = {
                    nombreObraSocial = it
                    if (showForm) {
                        nuevaCredencial = nuevaCredencial.copy(nombreObraSocial = it)
                    }
                },
                isError = (showErrors && !isNombreValid) || backendFieldErrors.containsKey("nombreObraSocial"),
                errorMessage = backendFieldErrors["nombreObraSocial"] ?: "Este campo es obligatorio",
                placeholder = "Seleccioná una obra social"
            )

            Spacer(modifier = Modifier.height(12.dp))

            InputTextField(
                label = "N° de Afiliado",
                value = numeroAfiliado,
                onValueChange = {
                    numeroAfiliado = it
                    if (showForm) {
                        nuevaCredencial = nuevaCredencial.copy(numeroAfiliado = it)
                    }
                },
                enabled = fieldsEnabled,
                keyboardType = KeyboardType.Number,
                placeholder = "Ingresá tu número",
                isError = (showErrors && !isNumeroValid) || backendFieldErrors.containsKey("numeroAfiliado"),
                errorMessage = backendFieldErrors["numeroAfiliado"]
                    ?: if (numeroAfiliado.isBlank()) "Este campo es obligatorio" else "Mínimo 6 caracteres"
            )

            Spacer(modifier = Modifier.height(12.dp))

            DropdownSelectField(
                label = "Plan",
                value = plan,
                options = planesDisponibles,
                enabled = fieldsEnabled,
                onValueChange = {
                    plan = it
                    if (showForm) {
                        nuevaCredencial = nuevaCredencial.copy(plan = it)
                    }
                },
                isError = (showErrors && plan.isBlank()) || backendFieldErrors.containsKey("plan"),
                errorMessage = backendFieldErrors["plan"] ?: "Este campo es obligatorio",
                placeholder = "Seleccioná un plan"
            )

            Spacer(modifier = Modifier.height(12.dp))

            InputMonthYearField(
                label = "Fecha de Vencimiento",
                value = fechaVencimiento,
                onValueChange = {
                    fechaVencimiento = it
                    if (showForm) {
                        nuevaCredencial = nuevaCredencial.copy(fechaVencimiento = it)
                    }
                },
                enabled = fieldsEnabled,
                isError = (showErrors && fechaVencimiento.isBlank()) || backendFieldErrors.containsKey("fechaVencimiento"),
                errorMessage = backendFieldErrors["fechaVencimiento"]
                    ?: if (fechaVencimiento.isBlank()) "Este campo es obligatorio" else "",
                placeholder = "MM/AAAA"
            )
        }

        errorGeneral?.let { mensaje ->
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
            onClick = {
                if (showForm) {
                    // Card nueva: pido confirmación antes de subir
                    showErrors = true
                    if (isNombreValid && isNumeroValid) {
                        showConfirmDialog = true
                    }
                } else if (!isEditingExisting) {
                    // Credencial existente bloqueada: la desbloqueo para editar
                    isEditingExisting = true
                } else {
                    // Credencial existente en edición: guardo cambios
                    showErrors = true
                    if (isNombreValid && isNumeroValid) {
                        viewModel.cargarCredencial(nombreObraSocial, numeroAfiliado, plan, fechaVencimiento)
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6FA8C7)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .height(52.dp)
        ) {
            Text(
                text = when {
                    showForm -> "Continuar"
                    isEditingExisting -> "Guardar"
                    else -> "Editar"
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        }
    }

    // Popup de confirmación antes de subir una credencial nueva
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirmar credencial") },
            text = { Text("¿Estás seguro de que querés subir esta credencial?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.cargarCredencial(nombreObraSocial, numeroAfiliado, plan, fechaVencimiento)
                    showConfirmDialog = false
                }) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

/**
 * Campo de selección con estilo similar a un InputTextField, pero con menú desplegable
 * (para "Obra social" y "Plan", tal como se ve en el diseño).
 * Reemplazar `options` por el origen de datos real de cada caso.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownSelectField(
    label: String,
    value: String,
    options: List<String>,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    isError: Boolean = false,
    errorMessage: String = "Este campo es obligatorio",
    placeholder: String? = null
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color(0xFF6B6B6B),
            modifier = Modifier.padding(bottom = 4.dp)
        )

        ExposedDropdownMenuBox(
            expanded = expanded && enabled,
            onExpandedChange = { if (enabled) expanded = it }
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                enabled = enabled,
                isError = isError,
                trailingIcon = {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = Color(0xFFD0D0D0),
                    disabledTextColor = Color(0xFF333333),
                    disabledTrailingIconColor = Color(0xFF9E9E9E)
                ),
                placeholder = if (placeholder != null) {
                    {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else null,
                supportingText = {
                    if (isError) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = expanded && enabled,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

private fun formatearVencimiento(fecha: String): String {
    return try {
        val partes = fecha.split("-")
        val anio = partes[0].takeLast(2)
        val mes = partes[1]
        "$mes/$anio"
    } catch (e: Exception) {
        fecha
    }
}

private fun formatearFechaParaPicker(fecha: String): String {
    val partes = fecha.split("-")
    if (partes.size != 3) return fecha

    return try {
        val anio = partes[0].toInt()
        val mes = partes[1].toInt()
        "${mes.toString().padStart(2, '0')}/$anio"
    } catch (e: NumberFormatException) {
        fecha
    }
}

private fun formatearVencimientoForm(fecha: String): String {
    val partes = fecha.split("/")
    if (partes.size != 2) return fecha
    return "${partes[0]}/${partes[1].takeLast(2)}"
}