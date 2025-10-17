package com.eunio.healthapp.android.ui.components

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.eunio.healthapp.domain.model.UnitSystem
import com.eunio.healthapp.domain.util.UnitConverter
import org.koin.compose.koinInject

/**
 * A composable that displays distance measurements with automatic unit conversion.
 * Converts from the stored metric value (km) to the user's preferred unit system.
 * 
 * @param distanceInKm The distance value in kilometers (stored format)
 * @param unitSystem The user's preferred unit system for display
 * @param modifier Modifier for styling the component
 * @param style Text style for the display
 * @param color Text color for the display
 */
@Composable
fun DistanceDisplay(
    distanceInKm: Double,
    unitSystem: UnitSystem,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.body1,
    color: Color = Color.Unspecified
) {
    val converter = koinInject<UnitConverter>()
    
    // Convert and format the distance value reactively
    val displayText = remember(distanceInKm, unitSystem) {
        val convertedValue = converter.convertDistance(
            value = distanceInKm,
            from = UnitSystem.METRIC,
            to = unitSystem
        )
        converter.formatDistance(convertedValue, unitSystem)
    }
    
    Text(
        text = displayText,
        modifier = modifier,
        style = style,
        color = color
    )
}