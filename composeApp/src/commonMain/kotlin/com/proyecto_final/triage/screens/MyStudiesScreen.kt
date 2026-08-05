package com.proyecto_final.triage.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Science
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import com.proyecto_final.triage.theme.AppTheme
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import com.proyecto_final.triage.components.CommonHeader
import com.proyecto_final.triage.theme.Spacing
import com.proyecto_final.triage.viewmodels.StudiesViewModel
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import com.proyecto_final.triage.components.HeaderAlignment
import com.proyecto_final.triage.network.EstudioClinicoDTO
import com.proyecto_final.triage.viewmodels.EstudiosState
import com.proyecto_final.triage.viewmodels.LocalStudiesViewModel
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.number

@Composable
fun StudyCard(estudio: EstudioClinicoDTO, onClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() },
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Default.Science,
                    contentDescription = null,
                    tint = Color(0xFF5BB8D4),
                    modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)
            ) {
                Text(text = estudio.tipoArchivo, style = MaterialTheme.typography.titleMedium)

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))
                    val fecha = estudio.fechaSubida?.let { LocalDateTime.parse(it) }

                    Text(text = fecha?.let { "${it.day.toString().padStart(2, '0')}/" +
                                             "${it.month.number.toString().padStart(2, '0')}/" +
                                             it.year
                        } ?: "",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

            }

            Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.Gray
            )
        }
    }

}

class MyStudiesScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val viewModel = LocalStudiesViewModel.current

        MyStudiesContent(
            onBack = { navigator?.pop() },
            onAddStudy = { navigator?.push(AddStudyScreen()) },
            onStudyClick = { estudio -> navigator?.push(MyStudyScreen(estudio)) },
            viewModel = viewModel
        )
    }
}

@Preview
@Composable
fun MyStudiesPreview() {
    AppTheme {
        MyStudiesContent(onBack = {},
            onAddStudy = {},
            onStudyClick = {},
            viewModel = StudiesViewModel()
        )
    }
}

@Composable
fun MyStudiesContent(   onBack: () -> Unit,
                        onAddStudy: () -> Unit,
                        onStudyClick: (EstudioClinicoDTO) -> Unit,
                        viewModel: StudiesViewModel
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.cargarEstudios()
    }

    Column( modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp)
    ) {
        CommonHeader(title = "Mis estudios",
                     onBack = { onBack() })

        Spacer(modifier = Modifier.height(Spacing.lg))

        when(state) {

            is EstudiosState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            is EstudiosState.Error -> {
                Text((state as EstudiosState.Error).message)
            }

            is EstudiosState.Success -> {
                val estudios = (state as EstudiosState.Success).estudios

                if (estudios.isEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth().height(130.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F2F2)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "No hay estudios disponibles",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.Gray)

                        }
                    }
                } else {
                    estudios.forEach { estudio ->
                        StudyCard(
                            estudio = estudio,
                            onClick = { onStudyClick(estudio) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = onAddStudy,
                shape = RoundedCornerShape(50)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Agregar estudio")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}