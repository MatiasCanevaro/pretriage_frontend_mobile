package com.proyecto_final.triage.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.proyecto_final.triage.components.CommonHeader
import com.proyecto_final.triage.components.ProgressBar
import com.proyecto_final.triage.platform.PlatformMapView
import com.proyecto_final.triage.theme.Spacing
import com.proyecto_final.triage.viewmodels.SelectLocationViewModel

@Composable
fun SelectLocationContent(
    type: String,
    viewModel: SelectLocationViewModel = viewModel(),
    onBack: () -> Unit,
    onContinue: (String, String) -> Unit
) {
    val state = viewModel.state
    val accentColor = Color(0xFF5BB8D4)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        CommonHeader(
            title = "2. Ingresá tu ubicación",
            subtitle = "Usaremos tu ubicación para mostrarte las mejores opciones",
            showLogo = true,
            onBack = onBack
        )

        ProgressBar(currentStep = 2, totalSteps = 3)

        Spacer(modifier = Modifier.height(Spacing.lg))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Column {
                // Opción 1: Usar mi ubicación actual
                LocationOptionRow(
                    icon = LocationOn,
                    title = "Usar mi ubicación actual",
                    subtitle = if (state.isLoadingLocation) {
                        "Obteniendo ubicación..."
                    } else {
                        state.currentAddressLabel.ifBlank { "Ubicación detectada" }
                    },
                    isLoading = state.isLoadingLocation,
                    isSelected = state.useCurrentLocation,
                    accentColor = accentColor,
                    onClick = { viewModel.toggleLocationMode(true) }
                )

                if (state.useCurrentLocation) {
                    PlatformMapView(
                        location = state.currentLocation,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .padding(top = 8.dp)
                    )
                }

                androidx.compose.material3.Divider(
                    color = Color(0xFFE0E0E0),
                    thickness = 1.dp
                )

                // Opción 2: Ingresar otra dirección
                LocationOptionRow(
                    icon = LocationOn,
                    title = "Ingresar otra dirección",
                    subtitle = "",
                    isLoading = false,
                    isSelected = !state.useCurrentLocation,
                    accentColor = accentColor,
                    onClick = { viewModel.toggleLocationMode(false) }
                )

                if (!state.useCurrentLocation) {
                    AddressInputSection(
                        state = state,
                        viewModel = viewModel,
                        accentColor = accentColor
                    )

                    PlatformMapView(
                        location = state.currentLocation,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .padding(top = 8.dp, bottom = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            enabled = viewModel.isContinueEnabled(),
            onClick = {
                viewModel.getSelectedLocation()?.let { ubicacion ->
                    onContinue(type, ubicacion)
                }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Text("Continuar", color = Color.White, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun LocationOptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isLoading: Boolean,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (isLoading) {
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = accentColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        RadioButton(
            selected = isSelected,
            onClick = { onClick() },
            colors = RadioButtonDefaults.colors(selectedColor = accentColor)
        )
    }
}

@Composable
private fun AddressInputSection(
    state: com.proyecto_final.triage.location.LocationState,
    viewModel: SelectLocationViewModel,
    accentColor: Color
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
        OutlinedTextField(
            value = state.manualAddress,
            onValueChange = { viewModel.updateManualAddress(it) },
            label = { Text("Dirección") },
            singleLine = true,
            isError = state.addressSearchError != null,
            trailingIcon = {
                if (state.isLoadingSuggestions || state.isSearchingAddress) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = accentColor
                    )
                } else {
                    IconButton(
                        onClick = { viewModel.searchAddress() },
                        enabled = state.manualAddress.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Search,
                            contentDescription = "Buscar dirección",
                            tint = if (state.manualAddress.isNotBlank())
                                accentColor
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        if (state.suggestions.isNotEmpty()) {
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
            SuggestionsList(
                suggestions = state.suggestions,
                onSelect = { viewModel.selectSuggestion(it) }
            )
        } else if (state.suggestionsError != null && !state.isLoadingSuggestions) {
            Text(
                text = state.suggestionsError!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth()
            )
        }

        if (state.addressSearchError != null) {
            Text(
                text = state.addressSearchError!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth()
            )
        } else if (state.foundAddress != null) {
            Text(
                text = "Dirección encontrada ✓",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF2E7D32),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SuggestionsList(
    suggestions: List<com.proyecto_final.triage.location.AddressSuggestion>,
    onSelect: (com.proyecto_final.triage.location.AddressSuggestion) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(12.dp)
            )
    ) {
        suggestions.forEach { sugerencia ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(sugerencia) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = sugerencia.label,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}