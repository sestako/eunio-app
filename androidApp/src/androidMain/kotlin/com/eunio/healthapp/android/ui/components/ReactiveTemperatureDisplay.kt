package com.eunio.healthapp.android.ui.components

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.eunio.healthapp.domain.manager.SettingsAwareUnitSystemManager
import com.eunio.healthapp.domain.model.settings.TemperatureUnit
import com.eunio.healthapp.domain.util.EnhancedUnitConverter
import androidx.compose.runtime.collectAsState
import org.koin.compose.koinInject

/**
 * A reactive composable that displays temperature measurements with automatic unit conversion.
 * Automatically updates when the user's temperature unit preference changes.
 * Converts from the stored metric value (°C) to the user's current preferred temperature unit.
 * 
 * @param temperatureInCelsius The temperature value in Celsius (stored format)
 * @param modifier Modifier for styling the component
 * @param style Text style for the display
 * @param color Text color for the display
 */
@Composable
fun ReactiveTemperatureDisplay(
    temperatureInCelsius: Double,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.body1,
    color: Color = Color.Unspecified
) {
    val enhancedConverter = koinInject<EnhancedUnitConverter>()
    
    // Use a simple state instead of observing flow to avoid recomposition issues
    var unitPreferences by remember { 
        mutableStateOf(com.eunio.healthapp.domain.model.settings.UnitPreferences.default()) 
    }
    
    // Convert and format the temperature value
    val displayText = remember(temperatureInCelsius, unitPreferences.temperatureUnit) {
        val convertedValue = enhancedConverter.convertTemperatureFromCelsius(
            celsiusValue = temperatureInCelsius,
            targetUnit = unitPreferences.temperatureUnit
        )
        enhancedConverter.formatTemperature(convertedValue, unitPreferences.temperatureUnit)
    }
    
    Text(
        text = displayText,
        modifier = modifier,
        style = style,
        color = color
    )
}