package com.eunio.healthapp.android.ui.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eunio.healthapp.android.ui.components.ReactiveTemperatureDisplay
import com.eunio.healthapp.android.ui.theme.EunioColors
import com.eunio.healthapp.domain.manager.UnitSystemManager
import com.eunio.healthapp.domain.model.Cycle
import com.eunio.healthapp.domain.model.DailyLog
import com.eunio.healthapp.domain.model.UnitSystem
import com.eunio.healthapp.domain.util.UnitConverter
import kotlinx.datetime.LocalDate
import org.koin.compose.koinInject
import kotlin.math.*

/**
 * BBT (Basal Body Temperature) charting screen with advanced features.
 * Displays temperature trends over cycle phases with ovulation detection,
 * zoom/pan functionality, and comprehensive statistics.
 */
@Composable
internal fun BBTChartScreen(
    temperatureLogs: List<DailyLog> = emptyList(),
    currentCycle: Cycle? = null,
    cycleHistory: List<Cycle> = emptyList(),
    modifier: Modifier = Modifier
) {
    val unitSystemManager = koinInject<UnitSystemManager>()
    val converter = koinInject<UnitConverter>()
    
    // Observe unit system changes reactively
    val unitSystem by unitSystemManager.observeUnitSystemChanges().collectAsState(
        initial = UnitSystem.METRIC
    )
    
    // Sample temperature data - in real app this would come from ViewModel/UseCase
    val sampleTemperatureData = remember {
        generateSampleBBTData()
    }
    
    // Use provided data or sample data
    val temperatureData = if (temperatureLogs.isNotEmpty()) {
        temperatureLogs.mapNotNull { log ->
            log.bbt?.let { temp ->
                BBTDataPoint(
                    date = log.date,
                    temperature = temp,
                    hasOvulationMarker = false // Will be calculated based on cycle data
                )
            }
        }
    } else {
        sampleTemperatureData
    }
    
    // Chart state
    var zoomLevel by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var selectedDataPoint by remember { mutableStateOf<BBTDataPoint?>(null) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with title and controls
        BBTChartHeader(
            zoomLevel = zoomLevel,
            onZoomIn = { zoomLevel = (zoomLevel * 1.2f).coerceAtMost(3f) },
            onZoomOut = { zoomLevel = (zoomLevel / 1.2f).coerceAtLeast(0.5f) },
            onResetZoom = { 
                zoomLevel = 1f
                panOffset = Offset.Zero
            }
        )
        
        // Main BBT Chart
        BBTChart(
            temperatureData = temperatureData,
            currentCycle = currentCycle,
            unitSystem = unitSystem,
            converter = converter,
            zoomLevel = zoomLevel,
            panOffset = panOffset,
            onPanOffsetChange = { panOffset = it },
            onDataPointSelected = { selectedDataPoint = it }
        )
        
        // Temperature Statistics
        BBTStatistics(
            temperatureData = temperatureData,
            unitSystem = unitSystem,
            converter = converter
        )
        
        // Cycle Phase Information
        if (currentCycle != null) {
            CyclePhaseInfo(
                currentCycle = currentCycle,
                selectedDataPoint = selectedDataPoint
            )
        }
        
        // Temperature Pattern Insights
        BBTPatternInsights(
            temperatureData = temperatureData,
            unitSystem = unitSystem,
            converter = converter
        )
        
        // Recent Temperature Readings
        RecentTemperatureReadings(
            temperatureData = temperatureData.takeLast(7),
            unitSystem = unitSystem,
            converter = converter
        )
    }
}

@Composable
private fun BBTChartHeader(
    zoomLevel: Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetZoom: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "BBT Chart",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Zoom: ${String.format("%.1f", zoomLevel)}x",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onZoomOut) {
                    Text(
                        text = "-",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onResetZoom) {
                    Text(
                        text = "1:1",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onZoomIn) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom In",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun BBTChart(
    temperatureData: List<BBTDataPoint>,
    currentCycle: Cycle?,
    unitSystem: UnitSystem,
    converter: UnitConverter,
    zoomLevel: Float,
    panOffset: Offset,
    onPanOffsetChange: (Offset) -> Unit,
    onDataPointSelected: (BBTDataPoint?) -> Unit
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    
    if (temperatureData.isEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "No temperature data available",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Start logging your BBT to see trends",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        return
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Chart title with unit system
            val unitLabel = if (unitSystem == UnitSystem.METRIC) "°C" else "°F"
            Text(
                text = "Temperature Trend ($unitLabel)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Chart canvas with gesture handling
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, _, _ ->
                            onPanOffsetChange(panOffset + pan)
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            // Find nearest data point to tap
                            val nearestPoint = findNearestDataPoint(
                                temperatureData, offset, Size(size.width.toFloat(), size.height.toFloat()), 
                                unitSystem, converter, zoomLevel, panOffset
                            )
                            onDataPointSelected(nearestPoint)
                        }
                    }
            ) {
                drawBBTChart(
                    temperatureData = temperatureData,
                    currentCycle = currentCycle,
                    unitSystem = unitSystem,
                    converter = converter,
                    zoomLevel = zoomLevel,
                    panOffset = panOffset,
                    textMeasurer = textMeasurer,
                    density = density
                )
            }
        }
    }
}

private fun DrawScope.drawBBTChart(
    temperatureData: List<BBTDataPoint>,
    currentCycle: Cycle?,
    unitSystem: UnitSystem,
    converter: UnitConverter,
    zoomLevel: Float,
    panOffset: Offset,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    density: androidx.compose.ui.unit.Density
) {
    if (temperatureData.isEmpty()) return
    
    val temperatures = temperatureData.map { data ->
        converter.convertTemperature(data.temperature, UnitSystem.METRIC, unitSystem)
    }
    
    if (temperatures.isEmpty()) return
    
    val minTemp = temperatures.minOrNull() ?: return
    val maxTemp = temperatures.maxOrNull() ?: return
    val tempRange = if (maxTemp - minTemp < 1.0) 1.0 else maxTemp - minTemp
    
    // Add padding to the range for better visualization
    val paddedMin = minTemp - (tempRange * 0.15)
    val paddedMax = maxTemp + (tempRange * 0.15)
    val paddedRange = paddedMax - paddedMin
    
    val margin = with(density) { 40.dp.toPx() }
    val chartWidth = (size.width - margin * 2) * zoomLevel
    val chartHeight = size.height - margin * 2
    val startX = margin + panOffset.x
    val startY = margin
    
    // Draw background grid
    drawTemperatureGrid(
        startX = startX,
        startY = startY,
        chartWidth = chartWidth,
        chartHeight = chartHeight,
        minTemp = paddedMin,
        maxTemp = paddedMax,
        unitSystem = unitSystem,
        textMeasurer = textMeasurer,
        density = density
    )
    
    // Calculate data points
    val points = temperatures.mapIndexed { index, temp ->
        val x = startX + (index.toFloat() / (temperatures.size - 1).toFloat()) * chartWidth
        val y = startY + chartHeight - ((temp - paddedMin) / paddedRange * chartHeight).toFloat()
        Offset(x, y)
    }
    
    // Draw cycle phase backgrounds
    drawCyclePhaseBackgrounds(
        temperatureData = temperatureData,
        currentCycle = currentCycle,
        points = points,
        startY = startY,
        chartHeight = chartHeight
    )
    
    // Draw temperature trend line
    drawTemperatureTrendLine(points)
    
    // Draw ovulation detection line
    drawOvulationDetectionLine(
        temperatureData = temperatureData,
        points = points,
        temperatures = temperatures,
        paddedMin = paddedMin,
        paddedRange = paddedRange,
        startY = startY,
        chartHeight = chartHeight
    )
    
    // Draw data points
    drawTemperatureDataPoints(
        temperatureData = temperatureData,
        points = points
    )
    
    // Draw ovulation markers
    drawOvulationMarkers(
        temperatureData = temperatureData,
        points = points,
        density = density
    )
}

private fun DrawScope.drawTemperatureGrid(
    startX: Float,
    startY: Float,
    chartWidth: Float,
    chartHeight: Float,
    minTemp: Double,
    maxTemp: Double,
    unitSystem: UnitSystem,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    density: androidx.compose.ui.unit.Density
) {
    val gridColor = EunioColors.Gray300.copy(alpha = 0.5f)
    val textColor = EunioColors.Gray600
    val textSize = with(density) { 12.sp.toPx() }
    
    // Draw horizontal grid lines (temperature)
    val tempStep = (maxTemp - minTemp) / 5
    for (i in 0..5) {
        val temp = minTemp + (tempStep * i)
        val y = startY + chartHeight - (i.toFloat() / 5f * chartHeight)
        
        // Grid line
        drawLine(
            color = gridColor,
            start = Offset(startX, y),
            end = Offset(startX + chartWidth, y),
            strokeWidth = 1.dp.toPx()
        )
        
        // Temperature label
        val tempText = String.format("%.1f", temp)
        val textResult = textMeasurer.measure(
            text = tempText,
            style = TextStyle(
                fontSize = 12.sp,
                color = textColor
            )
        )
        
        drawText(
            textLayoutResult = textResult,
            topLeft = Offset(
                startX - textResult.size.width - 8.dp.toPx(),
                y - textResult.size.height / 2
            )
        )
    }
    
    // Draw chart border
    drawRect(
        color = EunioColors.Gray400,
        topLeft = Offset(startX, startY),
        size = Size(chartWidth, chartHeight),
        style = Stroke(width = 2.dp.toPx())
    )
}

private fun DrawScope.drawCyclePhaseBackgrounds(
    temperatureData: List<BBTDataPoint>,
    currentCycle: Cycle?,
    points: List<Offset>,
    startY: Float,
    chartHeight: Float
) {
    if (currentCycle == null || points.isEmpty()) return
    
    // Draw different background colors for cycle phases
    val phaseColors = mapOf(
        "menstrual" to EunioColors.MenstrualPhase.copy(alpha = 0.1f),
        "follicular" to EunioColors.FollicularPhase.copy(alpha = 0.1f),
        "ovulatory" to EunioColors.OvulatoryPhase.copy(alpha = 0.1f),
        "luteal" to EunioColors.LutealPhase.copy(alpha = 0.1f)
    )
    
    // This would need cycle phase calculation logic
    // For now, just draw a subtle background
    drawRect(
        color = EunioColors.Primary.copy(alpha = 0.05f),
        topLeft = Offset(points.first().x, startY),
        size = Size(points.last().x - points.first().x, chartHeight)
    )
}

private fun DrawScope.drawTemperatureTrendLine(points: List<Offset>) {
    if (points.size < 2) return
    
    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        for (i in 1 until points.size) {
            lineTo(points[i].x, points[i].y)
        }
    }
    
    drawPath(
        path = path,
        color = EunioColors.Primary,
        style = Stroke(
            width = 3.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

private fun DrawScope.drawOvulationDetectionLine(
    temperatureData: List<BBTDataPoint>,
    points: List<Offset>,
    temperatures: List<Double>,
    paddedMin: Double,
    paddedRange: Double,
    startY: Float,
    chartHeight: Float
) {
    // Calculate potential ovulation based on temperature shift
    val ovulationThreshold = calculateOvulationThreshold(temperatures)
    if (ovulationThreshold != null) {
        val thresholdY = startY + chartHeight - 
            ((ovulationThreshold - paddedMin) / paddedRange * chartHeight).toFloat()
        
        drawLine(
            color = EunioColors.OvulatoryPhase.copy(alpha = 0.7f),
            start = Offset(points.first().x, thresholdY),
            end = Offset(points.last().x, thresholdY),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f))
        )
    }
}

private fun DrawScope.drawTemperatureDataPoints(
    temperatureData: List<BBTDataPoint>,
    points: List<Offset>
) {
    points.forEachIndexed { index, point ->
        val dataPoint = temperatureData.getOrNull(index)
        val color = when {
            dataPoint?.hasOvulationMarker == true -> EunioColors.OvulatoryPhase
            else -> EunioColors.Secondary
        }
        
        // Outer circle
        drawCircle(
            color = color,
            radius = 6.dp.toPx(),
            center = point
        )
        
        // Inner circle
        drawCircle(
            color = Color.White,
            radius = 3.dp.toPx(),
            center = point
        )
    }
}

private fun DrawScope.drawOvulationMarkers(
    temperatureData: List<BBTDataPoint>,
    points: List<Offset>,
    density: androidx.compose.ui.unit.Density
) {
    temperatureData.forEachIndexed { index, dataPoint ->
        if (dataPoint.hasOvulationMarker && index < points.size) {
            val point = points[index]
            val markerSize = with(density) { 12.dp.toPx() }
            
            // Draw ovulation marker (triangle)
            val path = Path().apply {
                moveTo(point.x, point.y - markerSize)
                lineTo(point.x - markerSize/2, point.y - markerSize - markerSize/2)
                lineTo(point.x + markerSize/2, point.y - markerSize - markerSize/2)
                close()
            }
            
            drawPath(
                path = path,
                color = EunioColors.OvulatoryPhase
            )
        }
    }
}

@Composable
private fun BBTStatistics(
    temperatureData: List<BBTDataPoint>,
    unitSystem: UnitSystem,
    converter: UnitConverter
) {
    if (temperatureData.isEmpty()) return
    
    val temperatures = temperatureData.map { 
        converter.convertTemperature(it.temperature, UnitSystem.METRIC, unitSystem)
    }
    
    val avgTemp = temperatures.average()
    val minTemp = temperatures.minOrNull() ?: 0.0
    val maxTemp = temperatures.maxOrNull() ?: 0.0
    val tempRange = maxTemp - minTemp
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Temperature Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem(
                    label = "Average",
                    value = converter.formatTemperature(avgTemp, unitSystem),
                    color = EunioColors.Primary
                )
                StatisticItem(
                    label = "Range",
                    value = String.format("%.1f°", tempRange),
                    color = EunioColors.Secondary
                )
                StatisticItem(
                    label = "Readings",
                    value = "${temperatureData.size}",
                    color = EunioColors.Info
                )
            }
        }
    }
}

@Composable
private fun StatisticItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CyclePhaseInfo(
    currentCycle: Cycle,
    selectedDataPoint: BBTDataPoint?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Cycle Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            if (selectedDataPoint != null) {
                Text(
                    text = "Selected: ${selectedDataPoint.date}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Cycle phase indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PhaseIndicator(
                    phase = "Menstrual",
                    color = EunioColors.MenstrualPhase,
                    isActive = false // Would calculate based on current date
                )
                PhaseIndicator(
                    phase = "Follicular",
                    color = EunioColors.FollicularPhase,
                    isActive = true
                )
                PhaseIndicator(
                    phase = "Ovulatory",
                    color = EunioColors.OvulatoryPhase,
                    isActive = false
                )
                PhaseIndicator(
                    phase = "Luteal",
                    color = EunioColors.LutealPhase,
                    isActive = false
                )
            }
        }
    }
}

@Composable
private fun PhaseIndicator(
    phase: String,
    color: Color,
    isActive: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (isActive) color else color.copy(alpha = 0.3f)
                )
        )
        Text(
            text = phase,
            style = MaterialTheme.typography.bodySmall,
            color = if (isActive) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun BBTPatternInsights(
    temperatureData: List<BBTDataPoint>,
    unitSystem: UnitSystem,
    converter: UnitConverter
) {
    if (temperatureData.size < 7) return
    
    val insights = generateTemperatureInsights(temperatureData, unitSystem, converter)
    
    if (insights.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Pattern Insights",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                insights.forEach { insight ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Text(
                            text = insight,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentTemperatureReadings(
    temperatureData: List<BBTDataPoint>,
    unitSystem: UnitSystem,
    converter: UnitConverter
) {
    if (temperatureData.isEmpty()) return
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Recent Readings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            temperatureData.forEach { dataPoint ->
                TemperatureReadingItem(
                    dataPoint = dataPoint,
                    unitSystem = unitSystem,
                    converter = converter
                )
            }
        }
    }
}

@Composable
private fun TemperatureReadingItem(
    dataPoint: BBTDataPoint,
    unitSystem: UnitSystem,
    converter: UnitConverter
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "${dataPoint.date.monthNumber}/${dataPoint.date.dayOfMonth}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (dataPoint.hasOvulationMarker) {
                Text(
                    text = "Ovulation detected",
                    style = MaterialTheme.typography.bodySmall,
                    color = EunioColors.OvulatoryPhase
                )
            }
        }
        
        ReactiveTemperatureDisplay(
            temperatureInCelsius = dataPoint.temperature,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

// Helper functions
internal fun calculateOvulationThreshold(temperatures: List<Double>): Double? {
    if (temperatures.size < 6) return null
    
    // Simple ovulation detection: look for sustained temperature rise
    val firstHalf = temperatures.take(temperatures.size / 2)
    val secondHalf = temperatures.drop(temperatures.size / 2)
    
    val firstHalfAvg = firstHalf.average()
    val secondHalfAvg = secondHalf.average()
    
    return if (secondHalfAvg - firstHalfAvg > 0.2) {
        firstHalfAvg + 0.1
    } else null
}

internal fun findNearestDataPoint(
    temperatureData: List<BBTDataPoint>,
    tapOffset: Offset,
    canvasSize: Size,
    unitSystem: UnitSystem,
    converter: UnitConverter,
    zoomLevel: Float,
    panOffset: Offset
): BBTDataPoint? {
    // Implementation would calculate which data point is closest to tap
    // For now, return null
    return null
}

internal fun generateTemperatureInsights(
    temperatureData: List<BBTDataPoint>,
    unitSystem: UnitSystem,
    converter: UnitConverter
): List<String> {
    val insights = mutableListOf<String>()
    
    if (temperatureData.size >= 7) {
        val temperatures = temperatureData.map { 
            converter.convertTemperature(it.temperature, UnitSystem.METRIC, unitSystem)
        }
        
        val recentAvg = temperatures.takeLast(3).average()
        val overallAvg = temperatures.average()
        
        if (recentAvg > overallAvg + 0.2) {
            insights.add("Recent temperatures are elevated, possibly indicating luteal phase")
        }
        
        val hasConsistentRise = temperatures.takeLast(3).zipWithNext().all { (a, b) -> b >= a }
        if (hasConsistentRise) {
            insights.add("Consistent temperature rise detected over last 3 days")
        }
        
        val tempRange = temperatures.maxOrNull()!! - temperatures.minOrNull()!!
        if (tempRange > 1.0) {
            insights.add("Wide temperature variation detected - ensure consistent measurement timing")
        }
    }
    
    return insights
}

internal fun generateSampleBBTData(): List<BBTDataPoint> {
    return (1..28).map { dayOfMonth ->
        val date = LocalDate(2024, 1, dayOfMonth)
        val baseTemp = 36.2
        val cycleDay = dayOfMonth
        
        // Simulate cycle pattern
        val temp = when {
            cycleDay <= 5 -> baseTemp + (0..2).random() * 0.1 // Menstrual
            cycleDay <= 13 -> baseTemp + (0..1).random() * 0.1 // Follicular
            cycleDay == 14 -> baseTemp + 0.4 // Ovulation
            else -> baseTemp + 0.3 + (0..2).random() * 0.1 // Luteal
        }
        
        BBTDataPoint(
            date = date,
            temperature = temp,
            hasOvulationMarker = cycleDay == 14
        )
    }
}

/**
 * Data class representing a BBT measurement point
 */
data class BBTDataPoint(
    val date: LocalDate,
    val temperature: Double, // Always in Celsius
    val hasOvulationMarker: Boolean = false
)