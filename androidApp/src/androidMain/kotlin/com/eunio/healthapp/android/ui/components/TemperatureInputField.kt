package com.eunio.healthapp.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.eunio.healthapp.domain.manager.SettingsAwareUnitSystemManager
import com.eunio.healthapp.domain.model.settings.TemperatureUnit
import com.eunio.healthapp.domain.util.EnhancedUnitConverter
import org.koin.compose.koinInject

/**
 * A temperature input field that respects user's temperature unit preferences.
 * Automatically converts input values to Celsius for storage while displaying
 * in the user's preferred temperature unit.
 * 
 * @param value The temperature value in Celsius (storage format)
 * @param onValueChange Callback when temperature changes (in Celsius)
 * @param label Label for the input field
 * @param modifier Modifier for styling
 * @param isError Whether the input is in error state
 * @param errorMessage Error message to display
 * @param placeholder Placeholder text
 */
@Composable
fun TemperatureInputField(
    value: Double?,
    onValueChange: (Double?) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null,
    placeholder: String? = null
) {
    val enhancedConverter = koinInject<EnhancedUnitConverter>()
    val settingsAwareUnitManager = koinInject<SettingsAwareUnitSystemManager>()
    
    // Observe temperature unit changes
    val unitPreferences by settingsAwareUnitManager.observeUnitPreferences().collectAsState(
        initial = com.eunio.healthapp.domain.model.settings.UnitPreferences.default()
    )
    
    // Convert stored Celsius value to display unit
    val displayValue = remember(value, unitPreferences.temperatureUnit) {
        value?.let { celsiusValue ->
            enhancedConverter.convertTemperatureFromCelsius(
                celsiusValue = celsiusValue,
                targetUnit = unitPreferences.temperatureUnit
            )
        }
    }
    
    // Local state for text input
    var textValue by remember(displayValue) {
        mutableStateOf(displayValue?.toString() ?: "")
    }
    
    // Update text when display value changes
    LaunchedEffect(displayValue) {
        textValue = displayValue?.toString() ?: ""
    }
    
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textValue,
                onValueChange = { newText ->
                    textValue = newText
                    
                    // Convert input to Celsius for storage
                    val inputValue = newText.toDoubleOrNull()
                    val celsiusValue = inputValue?.let { displayTemp ->
                        enhancedConverter.convertTemperatureToCelsius(
                            value = displayTemp,
                            sourceUnit = unitPreferences.temperatureUnit
                        )
                    }
                    onValueChange(celsiusValue)
                },
                label = { Text(label) },
                placeholder = placeholder?.let { { Text(it) } },
                trailingIcon = { 
                    Text(
                        text = unitPreferences.temperatureUnit.symbol,
                        color = LocalContentColor.current.copy(alpha = 0.6f)
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = isError,
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        
        // Error message
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colors.error,
                style = MaterialTheme.typography.caption,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
        
        // Helper text showing unit
        if (!isError) {
            Text(
                text = "Enter temperature in ${unitPreferences.temperatureUnit.displayName}",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

/**
 * A specialized BBT (Basal Body Temperature) input field that includes
 * validation for typical BBT ranges and helpful guidance.
 */
@Composable
fun BBTInputField(
    value: Double?,
    onValueChange: (Double?) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    val settingsAwareUnitManager = koinInject<SettingsAwareUnitSystemManager>()
    
    // Observe temperature unit for range validation
    val unitPreferences by settingsAwareUnitManager.observeUnitPreferences().collectAsState(
        initial = com.eunio.healthapp.domain.model.settings.UnitPreferences.default()
    )
    
    // Define typical BBT ranges based on unit
    val (minRange, maxRange) = when (unitPreferences.temperatureUnit) {
        TemperatureUnit.CELSIUS -> Pair(35.0, 38.0)
        TemperatureUnit.FAHRENHEIT -> Pair(95.0, 100.4)
    }
    
    TemperatureInputField(
        value = value,
        onValueChange = onValueChange,
        label = "Basal Body Temperature",
        modifier = modifier,
        isError = isError,
        errorMessage = errorMessage,
        placeholder = "e.g., ${if (unitPreferences.temperatureUnit == TemperatureUnit.CELSIUS) "36.5" else "97.7"}"
    )
    
    // Additional BBT-specific guidance
    if (!isError && value == null) {
        Text(
            text = "Typical BBT range: $minRange - $maxRange${unitPreferences.temperatureUnit.symbol}",
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.primary.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 16.dp, top = 2.dp)
        )
    }
}