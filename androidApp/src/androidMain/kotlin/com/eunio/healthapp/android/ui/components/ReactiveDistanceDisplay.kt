package com.eunio.healthapp.android.ui.components

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.eunio.healthapp.domain.manager.CachedUnitSystemManager
import com.eunio.healthapp.domain.model.UnitSystem
import com.eunio.healthapp.domain.util.CachedUnitConverter
import androidx.compose.runtime.collectAsState
import org.koin.compose.koinInject

/**
 * A reactive composable that displays distance measurements with automatic unit conversion.
 * Automatically updates when the user's unit system preference changes.
 * Converts from the stored metric value (km) to the user's current preferred unit system.
 * 
 * @param distanceInKm The distance value in kilometers (stored format)
 * @param modifier Modifier for styling the component
 * @param style Text style for the display
 * @param color Text color for the display
 */
@Composable
fun ReactiveDistanceDisplay(
    distanceInKm: Double,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.body1,
    color: Color = Color.Unspecified
) {
    val converter = koinInject<CachedUnitConverter>()
    val unitSystemManager = koinInject<CachedUnitSystemManager>()
    
    // Observe unit system changes reactively
    val unitSystem by unitSystemManager.observeUnitSystemChanges().collectAsState(
        initial = UnitSystem.METRIC
    )
    
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