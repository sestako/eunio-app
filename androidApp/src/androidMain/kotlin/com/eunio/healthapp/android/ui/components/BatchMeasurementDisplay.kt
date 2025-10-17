package com.eunio.healthapp.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.eunio.healthapp.domain.manager.CachedUnitSystemManager
import com.eunio.healthapp.domain.model.UnitSystem
import com.eunio.healthapp.domain.util.CachedUnitConverter
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * A performance-optimized composable for displaying multiple measurements using batch operations.
 * Uses cached converters and batch processing for improved performance with large datasets.
 */

data class MeasurementData(
    val id: String,
    val value: Double,
    val type: MeasurementType,
    val label: String? = null
)

enum class MeasurementType {
    WEIGHT, DISTANCE, TEMPERATURE
}

@Composable
fun BatchMeasurementDisplay(
    measurements: List<MeasurementData>,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.body2,
    color: Color = Color.Unspecified
) {
    val converter = koinInject<CachedUnitConverter>()
    val unitSystemManager = koinInject<CachedUnitSystemManager>()
    val coroutineScope = rememberCoroutineScope()
    
    // Observe unit system changes reactively
    val unitSystem by unitSystemManager.observeUnitSystemChanges().collectAsState(
        initial = UnitSystem.METRIC
    )
    
    // State for converted measurements
    var convertedMeasurements by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    
    // Perform batch conversion when measurements or unit system changes
    LaunchedEffect(measurements, unitSystem) {
        if (measurements.isNotEmpty()) {
            isLoading = true
            coroutineScope.launch {
                try {
                    val converted = convertMeasurementsBatch(measurements, unitSystem, converter)
                    convertedMeasurements = converted
                } finally {
                    isLoading = false
                }
            }
        } else {
            convertedMeasurements = emptyList()
        }
    }
    
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (isLoading) {
            item {
                Text(
                    text = "Converting measurements...",
                    style = style.copy(color = Color.Gray),
                    modifier = Modifier.padding(8.dp)
                )
            }
        } else {
            items(measurements.zip(convertedMeasurements)) { (measurement, converted) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    measurement.label?.let { label ->
                        Text(
                            text = label,
                            style = style,
                            color = color,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        text = converted,
                        style = style,
                        color = color
                    )
                }
            }
        }
    }
}

/**
 * Batch measurement display for weight values specifically
 */
@Composable
fun BatchWeightDisplay(
    weights: List<Pair<String, Double>>, // (label, weightInKg)
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.body2,
    color: Color = Color.Unspecified
) {
    val measurements = weights.map { (label, weight) ->
        MeasurementData(
            id = label,
            value = weight,
            type = MeasurementType.WEIGHT,
            label = label
        )
    }
    
    BatchMeasurementDisplay(
        measurements = measurements,
        modifier = modifier,
        style = style,
        color = color
    )
}

/**
 * Batch measurement display for distance values specifically
 */
@Composable
fun BatchDistanceDisplay(
    distances: List<Pair<String, Double>>, // (label, distanceInKm)
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.body2,
    color: Color = Color.Unspecified
) {
    val measurements = distances.map { (label, distance) ->
        MeasurementData(
            id = label,
            value = distance,
            type = MeasurementType.DISTANCE,
            label = label
        )
    }
    
    BatchMeasurementDisplay(
        measurements = measurements,
        modifier = modifier,
        style = style,
        color = color
    )
}

/**
 * Batch measurement display for temperature values specifically
 */
@Composable
fun BatchTemperatureDisplay(
    temperatures: List<Pair<String, Double>>, // (label, temperatureInCelsius)
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.body2,
    color: Color = Color.Unspecified
) {
    val measurements = temperatures.map { (label, temperature) ->
        MeasurementData(
            id = label,
            value = temperature,
            type = MeasurementType.TEMPERATURE,
            label = label
        )
    }
    
    BatchMeasurementDisplay(
        measurements = measurements,
        modifier = modifier,
        style = style,
        color = color
    )
}

/**
 * Converts measurements using batch operations for improved performance
 */
private suspend fun convertMeasurementsBatch(
    measurements: List<MeasurementData>,
    unitSystem: UnitSystem,
    converter: CachedUnitConverter
): List<String> {
    // Group measurements by type for batch processing
    val weightMeasurements = measurements.filter { it.type == MeasurementType.WEIGHT }
    val distanceMeasurements = measurements.filter { it.type == MeasurementType.DISTANCE }
    val temperatureMeasurements = measurements.filter { it.type == MeasurementType.TEMPERATURE }
    
    // Perform batch conversions
    val convertedWeights = if (weightMeasurements.isNotEmpty()) {
        val values = weightMeasurements.map { it.value }
        converter.convertWeightBatch(values, UnitSystem.METRIC, unitSystem)
            .zip(weightMeasurements.map { it.id })
    } else emptyList()
    
    val convertedDistances = if (distanceMeasurements.isNotEmpty()) {
        val values = distanceMeasurements.map { it.value }
        converter.convertDistanceBatch(values, UnitSystem.METRIC, unitSystem)
            .zip(distanceMeasurements.map { it.id })
    } else emptyList()
    
    val convertedTemperatures = if (temperatureMeasurements.isNotEmpty()) {
        val values = temperatureMeasurements.map { it.value }
        converter.convertTemperatureBatch(values, UnitSystem.METRIC, unitSystem)
            .zip(temperatureMeasurements.map { it.id })
    } else emptyList()
    
    // Perform batch formatting
    val formattedWeights = if (convertedWeights.isNotEmpty()) {
        val values = convertedWeights.map { it.first }
        converter.formatWeightBatch(values, unitSystem)
            .zip(convertedWeights.map { it.second })
    } else emptyList()
    
    val formattedDistances = if (convertedDistances.isNotEmpty()) {
        val values = convertedDistances.map { it.first }
        converter.formatDistanceBatch(values, unitSystem)
            .zip(convertedDistances.map { it.second })
    } else emptyList()
    
    val formattedTemperatures = if (convertedTemperatures.isNotEmpty()) {
        val values = convertedTemperatures.map { it.first }
        converter.formatTemperatureBatch(values, unitSystem)
            .zip(convertedTemperatures.map { it.second })
    } else emptyList()
    
    // Combine results and maintain original order
    val resultMap = mutableMapOf<String, String>()
    formattedWeights.forEach { (formatted, id) -> resultMap[id] = formatted }
    formattedDistances.forEach { (formatted, id) -> resultMap[id] = formatted }
    formattedTemperatures.forEach { (formatted, id) -> resultMap[id] = formatted }
    
    return measurements.map { measurement ->
        resultMap[measurement.id] ?: "N/A"
    }
}