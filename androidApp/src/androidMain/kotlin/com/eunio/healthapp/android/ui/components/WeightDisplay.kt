package com.eunio.healthapp.android.ui.components

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.eunio.healthapp.domain.model.settings.WeightUnit
import com.eunio.healthapp.domain.util.EnhancedUnitConverter
import org.koin.compose.koinInject

/**
 * A composable that displays weight measurements with automatic unit conversion.
 * Converts from the stored metric value (kg) to the specified weight unit.
 * 
 * @param weightInKg The weight value in kilograms (stored format)
 * @param weightUnit The weight unit for display
 * @param modifier Modifier for styling the component
 * @param style Text style for the display
 * @param color Text color for the display
 */
@Composable
fun WeightDisplay(
    weightInKg: Double,
    weightUnit: WeightUnit,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.body1,
    color: Color = Color.Unspecified
) {
    val enhancedConverter = koinInject<EnhancedUnitConverter>()
    
    // Convert and format the weight value
    val displayText = remember(weightInKg, weightUnit) {
        val convertedValue = enhancedConverter.convertWeightFromKilograms(
            kgValue = weightInKg,
            targetUnit = weightUnit
        )
        enhancedConverter.formatWeight(convertedValue, weightUnit)
    }
    
    Text(
        text = displayText,
        modifier = modifier,
        style = style,
        color = color
    )
}