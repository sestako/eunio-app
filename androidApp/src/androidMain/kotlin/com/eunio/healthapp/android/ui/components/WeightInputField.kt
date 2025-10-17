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
import com.eunio.healthapp.domain.model.settings.WeightUnit
import com.eunio.healthapp.domain.util.EnhancedUnitConverter
import org.koin.compose.koinInject

/**
 * A weight input field that respects user's weight unit preferences.
 * Automatically converts input values to Kilograms for storage while displaying
 * in the user's preferred weight unit.
 * 
 * @param value The weight value in Kilograms (storage format)
 * @param onValueChange Callback when weight changes (in Kilograms)
 * @param label Label for the input field
 * @param modifier Modifier for styling
 * @param isError Whether the input is in error state
 * @param errorMessage Error message to display
 * @param placeholder Placeholder text
 */
@Composable
fun WeightInputField(
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
    
    // Observe weight unit changes
    val unitPreferences by settingsAwareUnitManager.observeUnitPreferences().collectAsState(
        initial = com.eunio.healthapp.domain.model.settings.UnitPreferences.default()
    )
    
    // Convert stored Kilograms value to display unit
    val displayValue = remember(value, unitPreferences.weightUnit) {
        value?.let { kgValue ->
            enhancedConverter.convertWeightFromKilograms(
                kgValue = kgValue,
                targetUnit = unitPreferences.weightUnit
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
                    
                    // Convert input to Kilograms for storage
                    val inputValue = newText.toDoubleOrNull()
                    val kgValue = inputValue?.let { displayWeight ->
                        enhancedConverter.convertWeightToKilograms(
                            value = displayWeight,
                            sourceUnit = unitPreferences.weightUnit
                        )
                    }
                    onValueChange(kgValue)
                },
                label = { Text(label) },
                placeholder = placeholder?.let { { Text(it) } },
                trailingIcon = { 
                    Text(
                        text = unitPreferences.weightUnit.symbol,
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
                text = "Enter weight in ${unitPreferences.weightUnit.displayName}",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

/**
 * A specialized body weight input field with typical weight range validation
 */
@Composable
fun BodyWeightInputField(
    value: Double?,
    onValueChange: (Double?) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    val settingsAwareUnitManager = koinInject<SettingsAwareUnitSystemManager>()
    
    // Observe weight unit for range validation
    val unitPreferences by settingsAwareUnitManager.observeUnitPreferences().collectAsState(
        initial = com.eunio.healthapp.domain.model.settings.UnitPreferences.default()
    )
    
    // Define typical weight ranges based on unit
    val (minRange, maxRange, exampleWeight) = when (unitPreferences.weightUnit) {
        WeightUnit.KILOGRAMS -> Triple(30.0, 200.0, "65.0")
        WeightUnit.POUNDS -> Triple(66.0, 440.0, "143.0")
    }
    
    WeightInputField(
        value = value,
        onValueChange = onValueChange,
        label = "Body Weight",
        modifier = modifier,
        isError = isError,
        errorMessage = errorMessage,
        placeholder = "e.g., $exampleWeight"
    )
    
    // Additional weight-specific guidance
    if (!isError && value == null) {
        Text(
            text = "Typical range: $minRange - $maxRange ${unitPreferences.weightUnit.symbol}",
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.primary.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 16.dp, top = 2.dp)
        )
    }
}