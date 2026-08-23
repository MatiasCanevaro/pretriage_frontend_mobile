package com.proyecto_final.triage.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import com.proyecto_final.triage.theme.AppTheme
import com.proyecto_final.triage.storage.TokenStorage
import com.proyecto_final.triage.viewmodels.HomeState
import com.proyecto_final.triage.viewmodels.HomeViewModel
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import triage.composeapp.generated.resources.*

class HomeScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val viewModel = remember { HomeViewModel() }

        val state = viewModel.state

        if (TokenStorage.getToken() == null) {
            navigator?.push(SignInScreen())
            return
        }

        LaunchedEffect(Unit) {
            viewModel.cargarEstadoConsulta()
        }

        DisposableEffect(Unit) {
            onDispose { viewModel.detener() }
        }

        HomeContent(
            state = state,
            onEmergency = { },
            onSolicitarAtencion = { navigator?.push(SelectTypeGuardScreen()) },
            onChat = { navigator?.push(ChatScreen()) },
            onCargarEstudios = { },
            onMiPerfil = { navigator?.push(ProfileScreen()) },
            onConsultaActiva = { navigator?.push(ConsultaActivaScreen()) }
        )
    }
}

@Preview
@Composable
fun HomePreview() {
    AppTheme {
        HomeContent(
            state = HomeState.Loading,
            onEmergency = { },
            onSolicitarAtencion = { },
            onChat = { },
            onCargarEstudios = { },
            onMiPerfil = { },
            onConsultaActiva = {}
        )
    }
}

@Composable
fun HomeContent(
    state: HomeState,
    onEmergency: () -> Unit,
    onSolicitarAtencion: () -> Unit,
    onChat: () -> Unit,
    onCargarEstudios: () -> Unit,
    onMiPerfil: () -> Unit,
    onConsultaActiva: () -> Unit
) {
    val carouselImages = listOf(
        Res.drawable.image1,
        Res.drawable.image2,
        Res.drawable.image3
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {

        Spacer(modifier = Modifier.height(36.dp))

        // HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(Res.drawable.logo),
                contentDescription = "Logo",
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Hola Matias!",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "¿Como podemos ayudarte?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "Perfil",
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { onMiPerfil() }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // CARRUSEL
        Carousel(carouselImages)

        Spacer(modifier = Modifier.height(8.dp))

        // MENU
        Text(
            text = "Menú",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ─────────────────────────────────────────
        // SOLICITAR ATENCIÓN
        // ─────────────────────────────────────────
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSolicitarAtencion() },
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFBBDEFB)
            ),
            border = BorderStroke(
                width = 1.dp,
                color = Color(0xFF90CAF9)
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 6.dp
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Filled.LocalHospital,
                    contentDescription = "Solicitar atención",
                    tint = Color(0xFF6B7280),
                    modifier = Modifier.size(44.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Solicitar atención de guardia",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Elegí el hospital según su tiempo de espera y describí tus síntomas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = "Continuar",
                    tint = Color(0xFF6B7280),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ─────────────────────────────────────────
        // HISTORIAL DE CONSULTAS
        // ─────────────────────────────────────────
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { },
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFBBDEFB)
            ),
            border = BorderStroke(
                width = 1.dp,
                color = Color(0xFF90CAF9)
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 6.dp
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.History,
                    contentDescription = "Historial de consultas",
                    tint = Color(0xFF6B7280),
                    modifier = Modifier.size(44.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Historial de consultas",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Consultá tus atenciones anteriores",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = "Ver historial",
                    tint = Color(0xFF6B7280),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // ESPACIO ENTRE MENÚ Y ATENCIÓN EN CURSO
        Spacer(modifier = Modifier.height(20.dp))

        // ATENCIÓN EN CURSO
        Text(
            text = "Atención en curso",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        when (state) {
            is HomeState.Success -> ConsultaActivaCard(
                estado = state.estado,
                hospital = state.hospital,
                onClick = onConsultaActiva
            )

            is HomeState.SinConsultaActiva,
            is HomeState.Error -> SinConsultaCard()

            HomeState.Loading -> SinConsultaCard()
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Carousel(carouselImages: List<DrawableResource>) {

    val pagerState = rememberPagerState(
        pageCount = { carouselImages.size }
    )

    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            val nextPage = (pagerState.currentPage + 1) % carouselImages.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    Column {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(carouselImages[page]),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(carouselImages.size) { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (index == pagerState.currentPage) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == pagerState.currentPage) Color(0xFF5BB8D4) else Color(0xFFD0D0D0)
                        )
                        .clickable { }
                )
            }
        }
    }
}

@Composable
fun ConsultaActivaCard(
    estado: com.proyecto_final.triage.network.estadoConsulta.EstadoConsultaPacienteDTO,
    hospital: com.proyecto_final.triage.network.estadoConsulta.HospitalSeleccionadoResponse?,
    onClick: () -> Unit
) {
    val (etiqueta, colorEstado) = estadoVisual(
        estado.estadoEntradaCola,
        estado.tipoPausa,
        estado.estadoConsulta
    )

    val estimacion = estado.tiempoEstimadoAtencion
    val minutosRestantes = estimacion?.let {
        minutosHasta(it.fechaHoraAtencionEstimada)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE3F2FD)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = Color(0xFFBBDEFB)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(min = 120.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Ícono
            Icon(
                imageVector = Icons.Filled.People,
                contentDescription = "Personas en espera",
                tint = Color(0xFF6B7280),
                modifier = Modifier.size(44.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Información
            Column(
                modifier = Modifier.weight(1f)
            ) {

                // Nombre del hospital
                Text(
                    text = hospital?.nombre ?: "Tu consulta",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF3AA76D)
                )

                // Dirección
                Text(
                    text = hospital?.direccion
                        ?.substringBefore(",")
                        ?: "Dirección no disponible",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Estado
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(colorEstado)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = etiqueta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                when {
                    estimacion == null -> Unit

                    !estimacion.hayMedicosActivos -> {
                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = estimacion.mensaje
                                ?: "Sin médicos disponibles por el momento",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE8A33D)
                        )
                    }

                    else -> {
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.People,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color(0xFF6B7280)
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            Text(
                                text = "${estimacion.pacientesAntes} adelante",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (minutosRestantes != null) {
                                Spacer(modifier = Modifier.width(12.dp))

                                Icon(
                                    imageVector = Icons.Filled.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = Color(0xFF6B7280)
                                )

                                Spacer(modifier = Modifier.width(4.dp))

                                Text(
                                    text = "$minutosRestantes min",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Flecha
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "Ver detalle",
                tint = Color(0xFF6B7280),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun SinConsultaCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFD6D9DE)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No tenés consultas activas",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF6B7280)
        )
    }
}

private fun estadoVisual(
    estadoEntradaCola: String?,
    tipoPausa: String?,
    estadoConsulta: String?
): Pair<String, Color> = when {
    estadoEntradaCola == "EN_COLA" -> "En cola" to Color(0xFF3AA76D)
    estadoEntradaCola == "LLAMADO" -> "Te están llamando" to Color(0xFF5BB8D4)
    estadoEntradaCola == "EN_ATENCION" -> "En atención" to Color(0xFF3AA76D)
    estadoEntradaCola == "EN_ESPERA" && tipoPausa == "ESPERA_MANUAL" -> "Te ausentaste" to Color(0xFFE8A33D)
    estadoEntradaCola == "EN_ESPERA" && tipoPausa == "AUSENTE_AL_LLAMADO" -> "Confirmá tu llegada" to Color(0xFFD9534F)
    estadoEntradaCola == "ATRASADO" -> "Atrasado, confirmá" to Color(0xFFD9534F)
    estadoConsulta == "HOSPITAL_SELECCIONADO" -> "Completá el pretriage" to Color(0xFF5BB8D4)
    estadoConsulta == "PRETRIAGE_EN_PROCESO" -> "Pretriage en curso" to Color(0xFF5BB8D4)
    estadoConsulta == "PRETRIAGE_FINALIZADO" -> "Preparando tu turno" to Color(0xFF5BB8D4)
    else -> "Consulta activa" to Color(0xFF5BB8D4)
}

fun minutosHasta(fechaHoraIso: String?): Int? {
    if (fechaHoraIso.isNullOrBlank()) return null
    return runCatching {
        val objetivo = LocalDateTime.parse(fechaHoraIso).toInstant(TimeZone.currentSystemDefault())
        val ahora = kotlin.time.Clock.System.now()
        val minutos = (objetivo - ahora).inWholeMinutes
        if (minutos < 0) 0 else minutos.toInt()
    }.getOrNull()
}