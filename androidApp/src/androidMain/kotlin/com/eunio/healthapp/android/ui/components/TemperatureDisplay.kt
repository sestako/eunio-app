package com.eunio.healthapp.android.ui.components

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.eunio.healthapp.domain.model.settings.TemperatureUnit
import com.eunio.healthapp.domain.util.EnhancedUnitConverter
import org.koin.compose.koinInject

/**
 * A composable that displays temperature measurements with automatic unit conversion.
 * Converts from the stored metric value (°C) to the specified temperature unit.
 * 
 * @param temperatureInCelsius The temperature value in Celsius (stored format)
 * @param temperatureUnit The temperature unit for display
 * @param modifier Modifier for styling the component
 * @param style Text style for the display
 * @param color Text color for the display
 */
@Composable
fun TemperatureDisplay(
    temperatureInCelsius: Double,
    temperatureUnit: TemperatureUnit,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.body1,
    color: Color = Color.Unspecified
) {
    val enhancedConverter = koinInject<EnhancedUnitConverter>()
    
    // Convert and format the temperature value
    val displayText = remember(temperatureInCelsius, temperatureUnit) {
        val convertedValue = enhancedConverter.convertTemperatureFromCelsius(
            celsiusValue = temperatureInCelsius,
            targetUnit = temperatureUnit
        )
        enhancedConverter.formatTemperature(convertedValue, temperatureUnit)
    }
    
    Text(
        text = displayText,
        modifier = modifier,
        style = style,
        color = color
    )
}