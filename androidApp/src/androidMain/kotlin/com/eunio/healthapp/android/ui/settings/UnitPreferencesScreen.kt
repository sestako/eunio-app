package com.eunio.healthapp.android.ui.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eunio.healthapp.domain.model.settings.TemperatureUnit
import com.eunio.healthapp.domain.model.settings.UnitPreferences
import com.eunio.healthapp.domain.model.settings.WeightUnit
import com.eunio.healthapp.presentation.state.UnitPreferencesUiState
import com.eunio.healthapp.presentation.viewmodel.UnitPreferencesViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * Unit preferences screen with temperature and weight unit selection.
 * Provides immediate preview of unit changes with sample conversions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitPreferencesScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // For now, we'll create a mock implementation since the full DI setup isn't complete
    UnitPreferencesScreenContent(
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitPreferencesScreenContent(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Mock state for demonstration - in real implementation this would come from ViewModel
    var uiState by remember { 
        mutableStateOf(
            UnitPreferencesUiState(
                preferences = UnitPreferences.default(),
                loadingState = com.eunio.healthapp.presentation.state.LoadingState.Success(UnitPreferences.default())
            )
        )
    }
    val snackbarHostState = remember { SnackbarHostState() }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Unit Preferences",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.testTag("unit_preferences_screen")
    ) { paddingValues ->
        UnitPreferencesContent(
            uiState = uiState,
            onTemperatureUnitChange = { newUnit ->
                uiState = uiState.copy(
                    preferences = uiState.preferences.copy(temperatureUnit = newUnit),
                    showConversionPreview = true
                )
            },
            onWeightUnitChange = { newUnit ->
                uiState = uiState.copy(
                    preferences = uiState.preferences.copy(weightUnit = newUnit),
                    showConversionPreview = true
                )
            },
            onShowConversionPreview = {
                uiState = uiState.copy(showConversionPreview = true)
            },
            onHideConversionPreview = {
                uiState = uiState.copy(showConversionPreview = false)
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}

/**
 * Content for the unit preferences screen.
 */
@Composable
internal fun UnitPreferencesContent(
    uiState: UnitPreferencesUiState,
    onTemperatureUnitChange: (TemperatureUnit) -> Unit,
    onWeightUnitChange: (WeightUnit) -> Unit,
    onShowConversionPreview: () -> Unit,
    onHideConversionPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        uiState.isLoading -> {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.testTag("loading_indicator")
                )
            }
        }
        
        !uiState.errorMessage.isNullOrEmpty() -> {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = uiState.errorMessage ?: "Unknown error",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("error_message")
                    )
                }
            }
        }
        
        else -> {
            Column(
                modifier = modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Temperature Unit Section
                PreferenceSection(
                    title = "Temperature",
                    subtitle = "Choose your preferred temperature unit for body temperature tracking",
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        UnitOption(
                            unit = TemperatureUnit.CELSIUS,
                            isSelected = uiState.preferences.temperatureUnit == TemperatureUnit.CELSIUS,
                            onSelected = { 
                                onTemperatureUnitChange(TemperatureUnit.CELSIUS)
                                onShowConversionPreview()
                            },
                            enabled = uiState.isEnabled,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("celsius_option")
                        )
                        
                        UnitOption(
                            unit = TemperatureUnit.FAHRENHEIT,
                            isSelected = uiState.preferences.temperatureUnit == TemperatureUnit.FAHRENHEIT,
                            onSelected = { 
                                onTemperatureUnitChange(TemperatureUnit.FAHRENHEIT)
                                onShowConversionPreview()
                            },
                            enabled = uiState.isEnabled,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("fahrenheit_option")
                        )
                    }
                }
                
                // Weight Unit Section
                PreferenceSection(
                    title = "Weight",
                    subtitle = "Choose your preferred weight unit for weight tracking",
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        UnitOption(
                            unit = WeightUnit.KILOGRAMS,
                            isSelected = uiState.preferences.weightUnit == WeightUnit.KILOGRAMS,
                            onSelected = { 
                                onWeightUnitChange(WeightUnit.KILOGRAMS)
                                onShowConversionPreview()
                            },
                            enabled = uiState.isEnabled,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("kilograms_option")
                        )
                        
                        UnitOption(
                            unit = WeightUnit.POUNDS,
                            isSelected = uiState.preferences.weightUnit == WeightUnit.POUNDS,
                            onSelected = { 
                                onWeightUnitChange(WeightUnit.POUNDS)
                                onShowConversionPreview()
                            },
                            enabled = uiState.isEnabled,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pounds_option")
                        )
                    }
                }
                
                // Conversion Preview Section
                AnimatedVisibility(
                    visible = uiState.showConversionPreview,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                    modifier = Modifier.testTag("conversion_preview_section")
                ) {
                    ConversionPreviewCard(
                        uiState = uiState,
                        onDismiss = onHideConversionPreview,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Loading indicator for updates
                AnimatedVisibility(
                    visible = uiState.isUpdating,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("updating_indicator"),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Updating preferences...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}