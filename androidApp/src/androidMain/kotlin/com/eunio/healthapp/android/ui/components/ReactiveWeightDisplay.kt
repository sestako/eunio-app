package com.eunio.healthapp.android.ui.components

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.eunio.healthapp.domain.manager.SettingsAwareUnitSystemManager
import com.eunio.healthapp.domain.model.settings.WeightUnit
import com.eunio.healthapp.domain.util.EnhancedUnitConverter
import androidx.compose.runtime.collectAsState
import org.koin.compose.koinInject

/**
 * A reactive composable that displays weight measurements with automatic unit conversion.
 * Automatically updates when the user's weight unit preference changes.
 * Converts from the stored metric value (kg) to the user's current preferred weight unit.
 * 
 * @param weightInKg The weight value in kilograms (stored format)
 * @param modifier Modifier for styling the component
 * @param style Text style for the display
 * @param color Text color for the display
 */
@Composable
fun ReactiveWeightDisplay(
    weightInKg: Double,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.body1,
    color: Color = Color.Unspecified
) {
    val enhancedConverter = koinInject<EnhancedUnitConverter>()
    val settingsAwareUnitManager = koinInject<SettingsAwareUnitSystemManager>()
    
    // Observe weight unit changes reactively
    val unitPreferences by settingsAwareUnitManager.observeUnitPreferences().collectAsState(
        initial = com.eunio.healthapp.domain.model.settings.UnitPreferences.default()
    )
    
    // Convert and format the weight value reactively
    val displayText = remember(weightInKg, unitPreferences.weightUnit) {
        val convertedValue = enhancedConverter.convertWeightFromKilograms(
            kgValue = weightInKg,
            targetUnit = unitPreferences.weightUnit
        )
        enhancedConverter.formatWeight(convertedValue, unitPreferences.weightUnit)
    }
    
    Text(
        text = displayText,
        modifier = modifier,
        style = style,
        color = color
    )
}