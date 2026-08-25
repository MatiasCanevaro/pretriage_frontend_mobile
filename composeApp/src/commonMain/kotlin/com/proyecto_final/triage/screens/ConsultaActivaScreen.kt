package com.proyecto_final.triage.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import com.proyecto_final.triage.location.obtenerUbicacionActual
import com.proyecto_final.triage.network.estadoConsulta.EstadoConsultaPacienteDTO
import com.proyecto_final.triage.network.estadoConsulta.EstadoEntradaCola
import com.proyecto_final.triage.network.estadoConsulta.HospitalSeleccionadoResponse
import com.proyecto_final.triage.network.estadoConsulta.TipoPausaCola
import com.proyecto_final.triage.platform.PlatformMap
import com.proyecto_final.triage.viewmodels.ConsultaActivaState
import com.proyecto_final.triage.viewmodels.ConsultaActivaViewModel
import com.proyecto_final.triage.viewmodels.estadoColaEnum
import com.proyecto_final.triage.viewmodels.tipoPausaEnum
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

private const val POLLING_INTERVAL_MS = 20_000L

private val ACCENT = Color(0xFF5BB8D4)

private val BUTTON_BACKGROUND = Color(0xFFE3F2FD)

private val BUTTON_BORDER = Color(0xFF5BB8D4)

private val CARD_BORDER = Color(0xFFE0E0E0)


class ConsultaActivaScreen : Screen {

    @Composable
    override fun Content() {

        val navigator = LocalNavigator.current

        val scope = rememberCoroutineScope()

        val viewModel = remember {
            ConsultaActivaViewModel()
        }

        LaunchedEffect(Unit) {

            viewModel.cargarEstado()
        }

        LaunchedEffect(Unit) {

            while (true) {

                delay(POLLING_INTERVAL_MS)

                viewModel.cargarEstado(
                    mostrarLoading = false
                )
            }
        }

        DisposableEffect(Unit) {

            onDispose {

                viewModel.detenerActualizaciones()
            }
        }

        ConsultaActivaContent(

            state = viewModel.state,

            actionError = viewModel.actionError,

            hospital = viewModel.hospital,

            isAusentarmeLoading =
                viewModel.isAusentarmeLoading,

            isEstoyAtrasadoLoading =
                viewModel.isEstoyAtrasadoLoading,

            isSigoAsistiendoLoading =
                viewModel.isSigoAsistiendoLoading,

            isLlegueLoading =
                viewModel.isLlegueLoading,


            onBack = {

                navigator?.pop()
            },


            onRetry = {

                viewModel.cargarEstado()
            },


            onAusentarme = {

                viewModel.ausentarme()
            },


            onEstoyAtrasado = {

                viewModel.estoyAtrasado()
            },


            onSigoAsistiendo = {

                viewModel.sigoAsistiendo()
            },


            onLlegue = {

                viewModel.llegue()
            },


            onComoLlegar = {

                scope.launch {

                    val ubicacion =
                        obtenerUbicacionActual()

                    val hospitalSeleccionado =
                        viewModel.hospital

                    if (
                        ubicacion != null &&
                        hospitalSeleccionado != null
                    ) {

                        val hospital = Hospital(

                            idHospital =
                                hospitalSeleccionado.idHospital,

                            placeId =
                                hospitalSeleccionado.placeId,

                            nombre =
                                hospitalSeleccionado.nombre,

                            direccion =
                                hospitalSeleccionado.direccion
                                    ?: "",

                            especialidades =
                                emptyList(),

                            tiempoEstimadoArriboMejorRuta =
                                null
                        )

                        navigator?.push(

                            RutasHospitalScreen(

                                hospital = hospital,

                                ubicacion =
                                    "${ubicacion.latitude},${ubicacion.longitude}"
                            )
                        )
                    }
                }
            },


            onChatInteractivo = {

                navigator?.push(
                    ChatScreen()
                )
            }
        )
    }
}


@Composable
fun ConsultaActivaContent(

    state: ConsultaActivaState,

    actionError: String?,

    hospital: HospitalSeleccionadoResponse?,

    isAusentarmeLoading: Boolean,

    isEstoyAtrasadoLoading: Boolean,

    isSigoAsistiendoLoading: Boolean,

    isLlegueLoading: Boolean,

    onBack: () -> Unit,

    onRetry: () -> Unit,

    onAusentarme: () -> Unit,

    onEstoyAtrasado: () -> Unit,

    onSigoAsistiendo: () -> Unit,

    onLlegue: () -> Unit,

    onComoLlegar: () -> Unit,

    onChatInteractivo: () -> Unit
) {

    Column(

        modifier = Modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.background
            )
            .verticalScroll(
                rememberScrollState()
            )
            .padding(
                horizontal = 16.dp
            )
    ) {

        Spacer(
            modifier = Modifier.height(36.dp)
        )


        Row(

            modifier = Modifier.fillMaxWidth(),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Icon(

                imageVector =
                    Icons.Filled.ArrowBack,

                contentDescription =
                    "Volver",

                modifier = Modifier
                    .size(28.dp)
                    .clickable {
                        onBack()
                    },

                tint =
                    MaterialTheme.colorScheme.onBackground
            )


            Spacer(
                modifier = Modifier.width(12.dp)
            )


            Text(

                text = "Tu atención",

                style =
                    MaterialTheme.typography.titleLarge,

                color =
                    MaterialTheme.colorScheme.onBackground
            )
        }


        Spacer(
            modifier = Modifier.height(16.dp)
        )


        when (state) {

            is ConsultaActivaState.Loading -> {

                LoadingSection()
            }


            is ConsultaActivaState.Error -> {

                ErrorSection(

                    message =
                        state.message,

                    onRetry =
                        onRetry
                )
            }


            is ConsultaActivaState.Success -> {

                EstadoConsultaSection(

                    estado =
                        state.estado,

                    hospital =
                        hospital,

                    actionError =
                        actionError,

                    isAusentarmeLoading =
                        isAusentarmeLoading,

                    isEstoyAtrasadoLoading =
                        isEstoyAtrasadoLoading,

                    isSigoAsistiendoLoading =
                        isSigoAsistiendoLoading,

                    isLlegueLoading =
                        isLlegueLoading,

                    onAusentarme =
                        onAusentarme,

                    onEstoyAtrasado =
                        onEstoyAtrasado,

                    onSigoAsistiendo =
                        onSigoAsistiendo,

                    onLlegue =
                        onLlegue,

                    onComoLlegar =
                        onComoLlegar,

                    onChatInteractivo =
                        onChatInteractivo
                )
            }
        }


        Spacer(
            modifier = Modifier.height(24.dp)
        )
    }
}


@Composable
private fun LoadingSection() {

    Box(

        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 64.dp
            ),

        contentAlignment =
            Alignment.Center
    ) {

        CircularProgressIndicator(
            color = ACCENT
        )
    }
}


@Composable
private fun ErrorSection(

    message: String,

    onRetry: () -> Unit
) {

    Card(

        shape =
            RoundedCornerShape(16.dp),

        modifier =
            Modifier.fillMaxWidth(),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surface
            )
    ) {

        Column(

            modifier =
                Modifier.padding(20.dp),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Icon(

                imageVector =
                    Icons.Filled.ErrorOutline,

                contentDescription =
                    null,

                tint =
                    Color(0xFFE57373),

                modifier =
                    Modifier.size(40.dp)
            )


            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )


            Text(

                text =
                    message,

                style =
                    MaterialTheme.typography.bodyMedium,

                color =
                    MaterialTheme.colorScheme.onSurfaceVariant,

                textAlign =
                    TextAlign.Center
            )


            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )


            Button(

                onClick =
                    onRetry,

                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            ACCENT
                    )
            ) {

                Text(
                    text = "Reintentar"
                )
            }
        }
    }
}


/*
 * ================================================================
 * ESTADO PRINCIPAL
 *
 * IMPORTANTE:
 *
 * Hospital
 * Dirección
 * Mapa
 *
 * SIEMPRE SE MUESTRAN PARA LOS ESTADOS ACTIVOS.
 *
 * Lo único que cambia es la card interior.
 * ================================================================
 */

@Composable
private fun EstadoConsultaSection(

    estado: EstadoConsultaPacienteDTO,

    hospital: HospitalSeleccionadoResponse?,

    actionError: String?,

    isAusentarmeLoading: Boolean,

    isEstoyAtrasadoLoading: Boolean,

    isSigoAsistiendoLoading: Boolean,

    isLlegueLoading: Boolean,

    onAusentarme: () -> Unit,

    onEstoyAtrasado: () -> Unit,

    onSigoAsistiendo: () -> Unit,

    onLlegue: () -> Unit,

    onComoLlegar: () -> Unit,

    onChatInteractivo: () -> Unit
) {

    val estadoCola =
        estado.estadoColaEnum()

    val tipoPausa =
        estado.tipoPausaEnum()


    /*
     * ============================================================
     * ERROR DE ACCIÓN
     * ============================================================
     */

    if (actionError != null) {

        Card(

            shape =
                RoundedCornerShape(12.dp),

            modifier =
                Modifier.fillMaxWidth(),

            colors =
                CardDefaults.cardColors(
                    containerColor =
                        Color(0xFFFDECEA)
                )
        ) {

            Text(

                text =
                    actionError,

                modifier =
                    Modifier.padding(12.dp),

                style =
                    MaterialTheme.typography.bodySmall,

                color =
                    Color(0xFFC62828)
            )
        }


        Spacer(
            modifier =
                Modifier.height(16.dp)
        )
    }


    /*
     * ============================================================
     * CARD DEL HOSPITAL
     *
     * ESTA PARTE NO CAMBIA AL PASAR DE:
     *
     * EN_COLA
     * EN_ESPERA
     * AUSENTE_AL_LLAMADO
     * ATRASADO
     * ============================================================
     */

    Card(

        shape =
            RoundedCornerShape(20.dp),

        modifier =
            Modifier.fillMaxWidth(),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color.White
            ),

        border =
            androidx.compose.foundation.BorderStroke(
                1.dp,
                CARD_BORDER
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                    6.dp
            )
    ) {

        Column(

            modifier =
                Modifier.padding(16.dp)
        ) {


            /*
             * ========================================================
             * HOSPITAL
             * ========================================================
             */

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(

                    modifier =
                        Modifier
                            .size(56.dp)
                            .clip(
                                RoundedCornerShape(16.dp)
                            )
                            .background(
                                Color(0xFFD8EFF9)
                            ),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(

                        imageVector =
                            Icons.Filled.LocalHospital,

                        contentDescription =
                            null,

                        tint =
                            ACCENT,

                        modifier =
                            Modifier.size(32.dp)
                    )
                }


                Spacer(
                    modifier =
                        Modifier.width(12.dp)
                )


                Column(

                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(

                        text =
                            hospital?.nombre
                                ?: "Hospital",

                        style =
                            MaterialTheme.typography.titleMedium,

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            MaterialTheme.colorScheme.onBackground
                    )


                    Spacer(
                        modifier =
                            Modifier.height(2.dp)
                    )


                    Text(

                        text =
                            hospital?.direccion
                                ?.substringBefore(",")
                                ?: "Dirección no disponible",

                        style =
                            MaterialTheme.typography.bodyMedium,

                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )


            /*
             * ========================================================
             * MAPA
             * ========================================================
             */

            PlatformMap(
                hospital =
                    hospital
            )


            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )


            /*
             * ========================================================
             * CARD INTERIOR
             *
             * SOLAMENTE ESTA PARTE CAMBIA
             * ========================================================
             */

            when {

                /*
                 * ====================================================
                 * EN COLA
                 * ====================================================
                 */

                estadoCola ==
                        EstadoEntradaCola.EN_COLA -> {

                    CardEstadoConsulta(

                        titulo =
                            "Estás en la cola",

                        subtitulo =
                            "Te iremos avisando cualquier novedad"
                    ) {

                        val tiempoEstimado =
                            estado.tiempoEstimadoAtencion


                        if (tiempoEstimado != null) {

                            if (
                                tiempoEstimado.hayMedicosActivos
                            ) {

                                Row(

                                    modifier =
                                        Modifier.fillMaxWidth()
                                ) {


                                    /*
                                     * TIEMPO DE ESPERA
                                     */

                                    Column(

                                        modifier =
                                            Modifier.weight(1f),

                                        horizontalAlignment =
                                            Alignment.CenterHorizontally
                                    ) {

                                        Text(

                                            text =
                                                "Tiempo de espera",

                                            style =
                                                MaterialTheme.typography
                                                    .bodySmall,

                                            color =
                                                MaterialTheme.colorScheme
                                                    .onSurfaceVariant
                                        )


                                        Spacer(
                                            modifier =
                                                Modifier.height(4.dp)
                                        )


                                        var minutosRestantes
                                                by remember(
                                                    tiempoEstimado
                                                        .fechaHoraAtencionEstimada
                                                ) {

                                                    mutableStateOf(
                                                        calcularMinutosRestantes(
                                                            tiempoEstimado
                                                                .fechaHoraAtencionEstimada
                                                        )
                                                    )
                                                }


                                        LaunchedEffect(
                                            tiempoEstimado
                                                .fechaHoraAtencionEstimada
                                        ) {

                                            while (true) {

                                                minutosRestantes =
                                                    calcularMinutosRestantes(
                                                        tiempoEstimado
                                                            .fechaHoraAtencionEstimada
                                                    )

                                                delay(1_000)
                                            }
                                        }


                                        Text(

                                            text =
                                                "$minutosRestantes min",

                                            style =
                                                MaterialTheme.typography
                                                    .headlineSmall,

                                            fontWeight =
                                                FontWeight.Bold,

                                            color =
                                                ACCENT
                                        )
                                    }


                                    Box(

                                        modifier =
                                            Modifier
                                                .width(1.dp)
                                                .height(58.dp)
                                                .background(
                                                    Color(0xFFE0E0E0)
                                                )
                                    )


                                    /*
                                     * PACIENTES ANTES
                                     */

                                    Column(

                                        modifier =
                                            Modifier.weight(1f),

                                        horizontalAlignment =
                                            Alignment.CenterHorizontally
                                    ) {

                                        Text(

                                            text =
                                                "Pacientes antes que vos",

                                            style =
                                                MaterialTheme.typography
                                                    .bodySmall,

                                            color =
                                                MaterialTheme.colorScheme
                                                    .onSurfaceVariant,

                                            textAlign =
                                                TextAlign.Center
                                        )


                                        Spacer(
                                            modifier =
                                                Modifier.height(4.dp)
                                        )


                                        Text(

                                            text =
                                                "${tiempoEstimado.pacientesAntes}",

                                            style =
                                                MaterialTheme.typography
                                                    .headlineSmall,

                                            fontWeight =
                                                FontWeight.Bold,

                                            color =
                                                ACCENT
                                        )
                                    }
                                }


                                Spacer(
                                    modifier =
                                        Modifier.height(14.dp)
                                )


                                HorizontalDivider(
                                    color =
                                        Color(0xFFE6E6E6)
                                )


                                Spacer(
                                    modifier =
                                        Modifier.height(12.dp)
                                )


                                /*
                                 * POSICIÓN
                                 */

                                Column(

                                    modifier =
                                        Modifier.fillMaxWidth(),

                                    horizontalAlignment =
                                        Alignment.CenterHorizontally
                                ) {

                                    Text(

                                        text =
                                            "Tu posición en la cola",

                                        style =
                                            MaterialTheme.typography
                                                .bodySmall,

                                        color =
                                            MaterialTheme.colorScheme
                                                .onSurfaceVariant
                                    )


                                    Spacer(
                                        modifier =
                                            Modifier.height(2.dp)
                                    )


                                    Text(

                                        text =
                                            "#${tiempoEstimado.posicionEnCola}",

                                        style =
                                            MaterialTheme.typography
                                                .headlineSmall,

                                        fontWeight =
                                            FontWeight.Bold,

                                        color =
                                            ACCENT
                                    )
                                }


                                /*
                                 * HORA ESTIMADA
                                 */

                                if (
                                    !tiempoEstimado
                                        .fechaHoraAtencionEstimada
                                        .isNullOrBlank()
                                ) {

                                    Spacer(
                                        modifier =
                                            Modifier.height(8.dp)
                                    )


                                    Text(

                                        text =
                                            "Alrededor de las ${
                                                formatHora(
                                                    tiempoEstimado
                                                        .fechaHoraAtencionEstimada
                                                )
                                            }",

                                        modifier =
                                            Modifier.fillMaxWidth(),

                                        textAlign =
                                            TextAlign.Center,

                                        style =
                                            MaterialTheme.typography
                                                .bodySmall,

                                        color =
                                            MaterialTheme.colorScheme
                                                .onSurfaceVariant
                                    )
                                }

                            } else {

                                Row(

                                    verticalAlignment =
                                        Alignment.CenterVertically
                                ) {

                                    Icon(

                                        imageVector =
                                            Icons.Filled.WarningAmber,

                                        contentDescription =
                                            null,

                                        tint =
                                            Color(0xFFFFA726),

                                        modifier =
                                            Modifier.size(20.dp)
                                    )


                                    Spacer(
                                        modifier =
                                            Modifier.width(6.dp)
                                    )


                                    Text(

                                        text =
                                            tiempoEstimado.mensaje
                                                ?: "No hay médicos disponibles por el momento.",

                                        style =
                                            MaterialTheme.typography
                                                .bodySmall,

                                        color =
                                            Color(0xFFE08A00)
                                    )
                                }
                            }
                        }
                    }
                }


                /*
                 * ====================================================
                 * ESPERA MANUAL
                 * ====================================================
                 */

                estadoCola ==
                        EstadoEntradaCola.EN_ESPERA &&
                        tipoPausa ==
                        TipoPausaCola.ESPERA_MANUAL -> {

                    CardEstadoConsulta(

                        titulo =
                            "Estás en pausa",

                        subtitulo =
                            "Te ausentaste temporalmente"
                    ) {

                        Text(

                            text =
                                "Tenés hasta 60 minutos para volver antes de que se cancele tu turno.",

                            style =
                                MaterialTheme.typography.bodySmall,

                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant
                        )


                        Spacer(
                            modifier =
                                Modifier.height(16.dp)
                        )


                        BotonCeleste(

                            texto =
                                "Llegué, volver a la cola",

                            icono =
                                Icons.Filled.CheckCircle,

                            loading =
                                isLlegueLoading,

                            onClick =
                                onLlegue
                        )
                    }
                }


                /*
                 * ====================================================
                 * AUSENTE AL LLAMADO
                 * ====================================================
                 */

                estadoCola ==
                        EstadoEntradaCola.EN_ESPERA &&
                        tipoPausa ==
                        TipoPausaCola.AUSENTE_AL_LLAMADO -> {

                    CardEstadoConsulta(

                        titulo =
                            "No estabas cuando te llamamos",

                        subtitulo =
                            "Tu turno sigue activo"
                    ) {

                        Text(

                            text =
                                "Tenés una hora para llegar. Si no llegás dentro de ese tiempo, tu turno se cancelará.",

                            style =
                                MaterialTheme.typography.bodySmall,

                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant
                        )


                        Spacer(
                            modifier =
                                Modifier.height(16.dp)
                        )


                        BotonCeleste(

                            texto =
                                "Estoy atrasado, sigo esperando",

                            loading =
                                isEstoyAtrasadoLoading,

                            onClick =
                                onEstoyAtrasado
                        )
                    }
                }


                /*
                 * ====================================================
                 * ATRASADO
                 * ====================================================
                 */

                estadoCola ==
                        EstadoEntradaCola.ATRASADO -> {

                    CardEstadoConsulta(

                        titulo =
                            "Turno atrasado",

                        subtitulo =
                            "Todavía podés llegar"
                    ) {

                        Text(

                            text =
                                "Tenés tiempo hasta las ${
                                    formatHora(
                                        estado.fechaHoraLimiteRespuesta
                                    )
                                } para llegar o confirmar que seguís en camino.",

                            style =
                                MaterialTheme.typography.bodySmall,

                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant
                        )


                        Spacer(
                            modifier =
                                Modifier.height(16.dp)
                        )


                        Row(

                            modifier =
                                Modifier.fillMaxWidth(),

                            horizontalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {

                            BotonCeleste(

                                texto =
                                    "Sigo en camino",

                                loading =
                                    isSigoAsistiendoLoading,

                                onClick =
                                    onSigoAsistiendo,

                                modifier =
                                    Modifier.weight(1f)
                            )


                            BotonCeleste(

                                texto =
                                    "Ya llegué",

                                icono =
                                    Icons.Filled.CheckCircle,

                                loading =
                                    isLlegueLoading,

                                onClick =
                                    onLlegue,

                                modifier =
                                    Modifier.weight(1f)
                            )
                        }
                    }
                }


                /*
                 * ====================================================
                 * LLAMADO
                 * ====================================================
                 */

                estadoCola ==
                        EstadoEntradaCola.LLAMADO -> {

                    CardEstadoConsulta(

                        titulo =
                            "¡Te están llamando!",

                        subtitulo =
                            "Acercate a recepción o a la sala indicada."
                    ) {

                        Icon(

                            imageVector =
                                Icons.Filled.NotificationsActive,

                            contentDescription =
                                null,

                            tint =
                                ACCENT,

                            modifier =
                                Modifier
                                    .size(42.dp)
                                    .align(
                                        Alignment.CenterHorizontally
                                    )
                        )
                    }
                }


                /*
                 * ====================================================
                 * EN ATENCIÓN
                 * ====================================================
                 */

                estadoCola ==
                        EstadoEntradaCola.EN_ATENCION -> {

                    CardEstadoConsulta(

                        titulo =
                            "Estás siendo atendido",

                        subtitulo =
                            "Tu consulta está en curso."
                    ) {

                        Icon(

                            imageVector =
                                Icons.Filled.MedicalServices,

                            contentDescription =
                                null,

                            tint =
                                ACCENT,

                            modifier =
                                Modifier
                                    .size(40.dp)
                                    .align(
                                        Alignment.CenterHorizontally
                                    )
                        )
                    }
                }


                /*
                 * ====================================================
                 * FINALIZADA
                 * ====================================================
                 */

                estadoCola ==
                        EstadoEntradaCola.FINALIZADA -> {

                    CardEstadoConsulta(

                        titulo =
                            "Consulta finalizada",

                        subtitulo =
                            "Esta atención ya no está activa."
                    ) {

                        Icon(

                            imageVector =
                                Icons.Filled.CheckCircle,

                            contentDescription =
                                null,

                            tint =
                                ACCENT,

                            modifier =
                                Modifier
                                    .size(40.dp)
                                    .align(
                                        Alignment.CenterHorizontally
                                    )
                        )
                    }
                }


                /*
                 * ====================================================
                 * CANCELADA
                 * ====================================================
                 */

                estadoCola ==
                        EstadoEntradaCola.CANCELADA -> {

                    CardEstadoConsulta(

                        titulo =
                            "Consulta cancelada",

                        subtitulo =
                            "Este turno ya no está activo."
                    ) {

                        Icon(

                            imageVector =
                                Icons.Filled.Cancel,

                            contentDescription =
                                null,

                            tint =
                                Color(0xFFE57373),

                            modifier =
                                Modifier
                                    .size(40.dp)
                                    .align(
                                        Alignment.CenterHorizontally
                                    )
                        )
                    }
                }


                /*
                 * ====================================================
                 * OTRO ESTADO
                 * ====================================================
                 */

                else -> {
                    CardEstadoConsulta(
                        titulo = "Estado de la consulta",
                        subtitulo = "Estamos actualizando la información."
                    ) {
                        Text(
                            text = "Estamos actualizando la información de tu turno.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }


    /*
     * ================================================================
     * BOTONES DEBAJO DE TODO
     *
     * ESTOS SE MANTIENEN PARA:
     *
     * EN_COLA
     * LLAMADO
     * EN_ESPERA
     * ATRASADO
     *
     * Por lo tanto, también aparecen cuando:
     *
     * - te ausentaste manualmente
     * - estás ausente al llamado
     * - estás atrasado
     * ================================================================
     */

    if (

        estadoCola ==
        EstadoEntradaCola.EN_COLA ||

        estadoCola ==
        EstadoEntradaCola.LLAMADO ||

        estadoCola ==
        EstadoEntradaCola.EN_ESPERA ||

        estadoCola ==
        EstadoEntradaCola.ATRASADO
    ) {

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )


        Row(

            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            /*
             * CÓMO LLEGAR
             */

            BotonCeleste(

                texto =
                    "Cómo llegar",

                icono =
                    Icons.Filled.Directions,

                onClick =
                    onComoLlegar,

                modifier =
                    Modifier.weight(1f)
            )


            /*
             * CHAT
             */

            BotonCeleste(

                texto =
                    "Chat interactivo",

                icono =
                    Icons.Filled.Chat,

                onClick =
                    onChatInteractivo,

                modifier =
                    Modifier.weight(1f)
            )
        }


        /*
         * AUSENTARME
         *
         * SOLO CUANDO ESTÁ EN COLA.
         */

        if (
            estadoCola ==
            EstadoEntradaCola.EN_COLA
        ) {

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )


            BotonCeleste(

                texto =
                    "Ausentarme un momento",

                icono =
                    Icons.Filled.ExitToApp,

                loading =
                    isAusentarmeLoading,

                onClick =
                    onAusentarme
            )
        }
    }
}


/*
 * ================================================================
 * CARD INTERIOR REUTILIZABLE
 * ================================================================
 */

@Composable
private fun CardEstadoConsulta(

    titulo: String,

    subtitulo: String,

    contenido: @Composable ColumnScope.() -> Unit
) {

    Card(

        shape =
            RoundedCornerShape(16.dp),

        modifier =
            Modifier.fillMaxWidth(),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color.White
            ),

        border =
            androidx.compose.foundation.BorderStroke(
                1.dp,
                CARD_BORDER
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                    0.dp
            )
    ) {

        Column(

            modifier =
                Modifier.padding(16.dp)
        ) {


            Row(

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(

                    modifier =
                        Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                Color(0xFFD8EFF9)
                            ),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(

                        imageVector =
                            Icons.Filled.Groups,

                        contentDescription =
                            null,

                        tint =
                            ACCENT,

                        modifier =
                            Modifier.size(25.dp)
                    )
                }


                Spacer(
                    modifier =
                        Modifier.width(10.dp)
                )


                Column(

                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(

                        text =
                            titulo,

                        style =
                            MaterialTheme.typography.titleMedium,

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            MaterialTheme.colorScheme
                                .onBackground
                    )


                    Text(

                        text =
                            subtitulo,

                        style =
                            MaterialTheme.typography.bodySmall,

                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )


            HorizontalDivider(
                color =
                    Color(0xFFE6E6E6)
            )


            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )


            contenido()
        }
    }
}


/*
 * ================================================================
 * BOTÓN CELESTE
 * ================================================================
 */

@Composable
private fun BotonCeleste(

    texto: String,

    icono: ImageVector? = null,

    loading: Boolean = false,

    onClick: () -> Unit,

    modifier: Modifier =
        Modifier.fillMaxWidth()
) {

    Box(

        contentAlignment =
            Alignment.Center,

        modifier =
            modifier
                .clip(
                    RoundedCornerShape(12.dp)
                )
                .heightIn(
                    min = 64.dp
                )
                .background(
                    BUTTON_BACKGROUND
                )
                .border(

                    width = 1.dp,

                    color =
                        BUTTON_BORDER,

                    shape =
                        RoundedCornerShape(12.dp)
                )
                .clickable(
                    enabled =
                        !loading
                ) {

                    onClick()
                }
                .padding(
                    horizontal = 14.dp,
                    vertical = 12.dp
                )
    ) {

        Row(

            verticalAlignment =
                Alignment.CenterVertically,

            horizontalArrangement =
                Arrangement.Center
        ) {

            if (loading) {

                CircularProgressIndicator(

                    modifier =
                        Modifier.size(20.dp),

                    strokeWidth =
                        2.dp,

                    color =
                        BUTTON_BORDER
                )

            } else {

                if (icono != null) {

                    Icon(

                        imageVector =
                            icono,

                        contentDescription =
                            null,

                        tint =
                            BUTTON_BORDER,

                        modifier =
                            Modifier.size(22.dp)
                    )


                    Spacer(
                        modifier =
                            Modifier.width(8.dp)
                    )
                }


                Text(

                    text =
                        texto,

                    color =
                        BUTTON_BORDER,

                    style =
                        MaterialTheme.typography.bodyMedium,

                    textAlign =
                        TextAlign.Center
                )
            }
        }
    }
}


/*
 * ================================================================
 * HORA
 * ================================================================
 */

private fun formatHora(
    iso: String?
): String {

    if (
        iso.isNullOrBlank()
    ) {

        return "--:--"
    }


    return try {

        val timePart =
            iso
                .substringAfter("T")
                .substringBefore(".")


        timePart.substring(
            0,
            5
        )

    } catch (
        e: Exception
    ) {

        "--:--"
    }
}


/*
 * ================================================================
 * MINUTOS RESTANTES
 * ================================================================
 */

fun calcularMinutosRestantes(

    fechaHoraAtencionEstimada:
    String?
): Int {

    if (
        fechaHoraAtencionEstimada
            .isNullOrBlank()
    ) {

        return 0
    }


    return try {

        val fechaEstimada =
            LocalDateTime.parse(
                fechaHoraAtencionEstimada
            )


        val fechaEstimadaInstant =
            fechaEstimada.toInstant(
                TimeZone.currentSystemDefault()
            )


        val ahora =
            kotlin.time.Clock.System.now()


        val minutos =
            (
                    fechaEstimadaInstant - ahora
                    ).inWholeMinutes


        minutos
            .coerceAtLeast(0)
            .toInt()

    } catch (
        e: Exception
    ) {

        0
    }
}