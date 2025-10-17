package com.eunio.healthapp.android.ui.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eunio.healthapp.domain.model.settings.TemperatureUnit
import com.eunio.healthapp.domain.model.settings.WeightUnit
import com.eunio.healthapp.presentation.state.UnitPreferencesUiState

/**
 * A card that shows conversion previews for temperature and weight units.
 * Provides immediate feedback when unit preferences change.
 */
@Composable
fun ConversionPreviewCard(
    uiState: UnitPreferencesUiState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag("conversion_preview_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with dismiss button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("preview_header"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Conversion Preview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("dismiss_preview_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss preview",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            // Conversion examples
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Temperature conversion
                ConversionExample(
                    title = "Temperature",
                    currentValue = uiState.getFormattedPreviewTemperature(),
                    convertedValue = getTemperatureConversion(uiState),
                    icon = Icons.Default.Settings, // Using Settings as placeholder for temperature
                    modifier = Modifier.testTag("temperature_conversion")
                )
                
                // Weight conversion
                ConversionExample(
                    title = "Weight",
                    currentValue = uiState.getFormattedPreviewWeight(),
                    convertedValue = getWeightConversion(uiState),
                    icon = Icons.Default.Favorite,
                    modifier = Modifier.testTag("weight_conversion")
                )
            }
            
            // Info text
            Text(
                text = "These are sample conversions. All your existing data will be automatically converted to your preferred units.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("conversion_info_text")
            )
        }
    }
}

/**
 * Individual conversion example showing current and converted values.
 */
@Composable
private fun ConversionExample(
    title: String,
    currentValue: String,
    convertedValue: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Icon
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        
        // Conversion display
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Current value
                Text(
                    text = currentValue,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                // Arrow
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "converts to",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                
                // Converted value
                Text(
                    text = convertedValue,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Gets temperature conversion preview text.
 */
private fun getTemperatureConversion(uiState: UnitPreferencesUiState): String {
    val currentTemp = uiState.previewTemperature
    val currentUnit = uiState.preferences.temperatureUnit
    val otherUnit = if (currentUnit == TemperatureUnit.CELSIUS) {
        TemperatureUnit.FAHRENHEIT
    } else {
        TemperatureUnit.CELSIUS
    }
    
    val convertedTemp = TemperatureUnit.convert(currentTemp, currentUnit, otherUnit)
    return "${String.format("%.1f", convertedTemp)}${otherUnit.symbol}"
}

/**
 * Gets weight conversion preview text.
 */
private fun getWeightConversion(uiState: UnitPreferencesUiState): String {
    val currentWeight = uiState.previewWeight
    val currentUnit = uiState.preferences.weightUnit
    val otherUnit = if (currentUnit == WeightUnit.KILOGRAMS) {
        WeightUnit.POUNDS
    } else {
        WeightUnit.KILOGRAMS
    }
    
    val convertedWeight = WeightUnit.convert(currentWeight, currentUnit, otherUnit)
    return "${String.format("%.1f", convertedWeight)} ${otherUnit.symbol}"
}