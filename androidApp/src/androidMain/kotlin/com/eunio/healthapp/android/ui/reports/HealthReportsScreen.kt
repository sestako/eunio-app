package com.eunio.healthapp.android.ui.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eunio.healthapp.android.ui.components.ReactiveTemperatureDisplay
import com.eunio.healthapp.android.ui.components.ReactiveWeightDisplay
import com.eunio.healthapp.android.ui.components.ReactiveDistanceDisplay
import com.eunio.healthapp.domain.manager.UnitSystemManager
import com.eunio.healthapp.domain.model.UnitSystem
import com.eunio.healthapp.domain.model.DailyLog
import com.eunio.healthapp.domain.model.Insight
import com.eunio.healthapp.domain.model.InsightType
// Using simple date strings to avoid datetime dependency issues
import org.koin.compose.koinInject

/**
 * Health reports screen that displays comprehensive health data with unit system information.
 * Shows insights, statistics, and measurements using unit-aware display components.
 */
@Composable
internal fun HealthReportsScreen(
    modifier: Modifier = Modifier
) {
    val unitSystemManager = koinInject<UnitSystemManager>()
    
    // Observe unit system changes reactively
    val unitSystem by unitSystemManager.observeUnitSystemChanges().collectAsState(
        initial = UnitSystem.METRIC
    )
    
    // Sample data - in real app this would come from ViewModel/UseCase
    val healthStats = remember {
        HealthStatistics(
            averageTemperature = 36.5, // Stored in Celsius
            averageWeight = 65.0, // Stored in kg
            totalDistance = 45.2, // Stored in km
            cycleLength = 28,
            temperatureRange = Pair(36.1, 36.9)
        )
    }
    
    val insights = remember {
        listOf(
            SampleInsight(
                insightText = "Your temperature pattern suggests ovulation occurred around day 14",
                type = "Cycle Prediction",
                confidence = 0.85,
                actionable = true
            ),
            SampleInsight(
                insightText = "Your cycle length has been consistent at 28 days",
                type = "Pattern Recognition",
                confidence = 0.92,
                actionable = false
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Health Reports") }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                UnitSystemInfoCard(unitSystem = unitSystem)
            }
            
            item {
                HealthStatisticsCard(stats = healthStats)
            }
            
            item {
                Text(
                    text = "Health Insights",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold
                )
            }
            
            items(insights) { insight ->
                InsightCard(insight = insight)
            }
        }
    }
}

@Composable
private fun UnitSystemInfoCard(
    unitSystem: UnitSystem
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Unit System",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Current: ${unitSystem.name}",
                style = MaterialTheme.typography.body1,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            val unitInfo = when (unitSystem) {
                UnitSystem.METRIC -> "Temperature: °C, Weight: kg, Distance: km"
                UnitSystem.IMPERIAL -> "Temperature: °F, Weight: lbs, Distance: miles"
            }
            
            Text(
                text = unitInfo,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun HealthStatisticsCard(
    stats: HealthStatistics
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Health Statistics",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Average temperature with reactive display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Average BBT:",
                    style = MaterialTheme.typography.body1,
                    fontWeight = FontWeight.Medium
                )
                ReactiveTemperatureDisplay(
                    temperatureInCelsius = stats.averageTemperature,
                    style = MaterialTheme.typography.body1
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Temperature range
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Temperature Range:",
                    style = MaterialTheme.typography.body1,
                    fontWeight = FontWeight.Medium
                )
                Row {
                    ReactiveTemperatureDisplay(
                        temperatureInCelsius = stats.temperatureRange.first,
                        style = MaterialTheme.typography.body1
                    )
                    Text(
                        text = " - ",
                        style = MaterialTheme.typography.body1
                    )
                    ReactiveTemperatureDisplay(
                        temperatureInCelsius = stats.temperatureRange.second,
                        style = MaterialTheme.typography.body1
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Average weight with reactive display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Average Weight:",
                    style = MaterialTheme.typography.body1,
                    fontWeight = FontWeight.Medium
                )
                ReactiveWeightDisplay(
                    weightInKg = stats.averageWeight,
                    style = MaterialTheme.typography.body1
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Total distance with reactive display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Distance:",
                    style = MaterialTheme.typography.body1,
                    fontWeight = FontWeight.Medium
                )
                ReactiveDistanceDisplay(
                    distanceInKm = stats.totalDistance,
                    style = MaterialTheme.typography.body1
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Cycle length (no unit conversion needed)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Average Cycle Length:",
                    style = MaterialTheme.typography.body1,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${stats.cycleLength} days",
                    style = MaterialTheme.typography.body1
                )
            }
        }
    }
}

@Composable
private fun InsightCard(
    insight: SampleInsight
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = insight.type,
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary
                )
                
                Text(
                    text = "${(insight.confidence * 100).toInt()}% confidence",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = insight.insightText,
                style = MaterialTheme.typography.body2
            )
            
            if (insight.actionable) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Actionable",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.secondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private data class HealthStatistics(
    val averageTemperature: Double,
    val averageWeight: Double,
    val totalDistance: Double,
    val cycleLength: Int,
    val temperatureRange: Pair<Double, Double>
)

/**
 * Sample data class for demonstration purposes
 */
internal data class SampleInsight(
    val insightText: String,
    val type: String,
    val confidence: Double,
    val actionable: Boolean
)