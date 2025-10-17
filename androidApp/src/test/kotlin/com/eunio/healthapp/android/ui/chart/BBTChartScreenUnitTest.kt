package com.eunio.healthapp.android.ui.chart

import com.eunio.healthapp.domain.model.UnitSystem
import kotlinx.datetime.LocalDate
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BBTChartScreenUnitTest {

    @Test
    fun `generateSampleBBTData creates correct number of data points`() {
        val sampleData = generateSampleBBTData()
        
        assertEquals(28, sampleData.size)
        assertTrue(sampleData.all { it.temperature > 35.0 && it.temperature < 40.0 })
    }

    @Test
    fun `generateSampleBBTData includes ovulation marker`() {
        val sampleData = generateSampleBBTData()
        
        val ovulationMarkers = sampleData.filter { it.hasOvulationMarker }
        assertEquals(1, ovulationMarkers.size)
        
        // Ovulation should be around day 14
        val ovulationDay = ovulationMarkers.first()
        assertEquals(LocalDate(2024, 1, 14), ovulationDay.date)
    }

    @Test
    fun `generateSampleBBTData simulates realistic cycle pattern`() {
        val sampleData = generateSampleBBTData()
        
        // Menstrual phase (days 1-5) should have lower temperatures
        val menstrualPhase = sampleData.take(5)
        val menstrualAvg = menstrualPhase.map { it.temperature }.average()
        
        // Luteal phase (days 15-28) should have higher temperatures
        val lutealPhase = sampleData.drop(14)
        val lutealAvg = lutealPhase.map { it.temperature }.average()
        
        assertTrue(lutealAvg > menstrualAvg, "Luteal phase should have higher average temperature")
    }

    @Test
    fun `calculateOvulationThreshold returns null for insufficient data`() {
        val shortList = listOf(36.1, 36.2, 36.3)
        val threshold = calculateOvulationThreshold(shortList)
        
        assertEquals(null, threshold)
    }

    @Test
    fun `calculateOvulationThreshold detects temperature shift`() {
        val temperatures = listOf(
            36.1, 36.2, 36.1, 36.2, 36.1, 36.2, // First half - lower temps
            36.5, 36.6, 36.7, 36.6, 36.5, 36.6  // Second half - higher temps
        )
        
        val threshold = calculateOvulationThreshold(temperatures)
        
        assertNotNull(threshold)
        assertTrue(threshold > 36.1 && threshold < 36.5)
    }

    @Test
    fun `calculateOvulationThreshold returns null for no significant shift`() {
        val temperatures = listOf(
            36.1, 36.2, 36.1, 36.2, 36.1, 36.2, // First half
            36.2, 36.1, 36.2, 36.1, 36.2, 36.1  // Second half - no significant rise
        )
        
        val threshold = calculateOvulationThreshold(temperatures)
        
        assertEquals(null, threshold)
    }

    @Test
    fun `generateTemperatureInsights detects elevated recent temperatures`() {
        val temperatureData = listOf(
            BBTDataPoint(LocalDate(2024, 1, 1), 36.1),
            BBTDataPoint(LocalDate(2024, 1, 2), 36.2),
            BBTDataPoint(LocalDate(2024, 1, 3), 36.1),
            BBTDataPoint(LocalDate(2024, 1, 4), 36.2),
            BBTDataPoint(LocalDate(2024, 1, 5), 36.6), // Recent elevated temps
            BBTDataPoint(LocalDate(2024, 1, 6), 36.7),
            BBTDataPoint(LocalDate(2024, 1, 7), 36.8)
        )
        
        val insights = generateTemperatureInsights(
            temperatureData, 
            UnitSystem.METRIC, 
            MockUnitConverter()
        )
        
        assertTrue(insights.any { it.contains("elevated") })
    }

    @Test
    fun `generateTemperatureInsights detects consistent temperature rise`() {
        val temperatureData = listOf(
            BBTDataPoint(LocalDate(2024, 1, 1), 36.1),
            BBTDataPoint(LocalDate(2024, 1, 2), 36.2),
            BBTDataPoint(LocalDate(2024, 1, 3), 36.1),
            BBTDataPoint(LocalDate(2024, 1, 4), 36.2),
            BBTDataPoint(LocalDate(2024, 1, 5), 36.3), // Consistent rise
            BBTDataPoint(LocalDate(2024, 1, 6), 36.4),
            BBTDataPoint(LocalDate(2024, 1, 7), 36.5)
        )
        
        val insights = generateTemperatureInsights(
            temperatureData, 
            UnitSystem.METRIC, 
            MockUnitConverter()
        )
        
        assertTrue(insights.any { it.contains("Consistent temperature rise") })
    }

    @Test
    fun `generateTemperatureInsights detects wide temperature variation`() {
        val temperatureData = listOf(
            BBTDataPoint(LocalDate(2024, 1, 1), 35.8), // Wide variation
            BBTDataPoint(LocalDate(2024, 1, 2), 36.2),
            BBTDataPoint(LocalDate(2024, 1, 3), 37.1),
            BBTDataPoint(LocalDate(2024, 1, 4), 36.0),
            BBTDataPoint(LocalDate(2024, 1, 5), 36.9),
            BBTDataPoint(LocalDate(2024, 1, 6), 36.1),
            BBTDataPoint(LocalDate(2024, 1, 7), 36.8)
        )
        
        val insights = generateTemperatureInsights(
            temperatureData, 
            UnitSystem.METRIC, 
            MockUnitConverter()
        )
        
        assertTrue(insights.any { it.contains("Wide temperature variation") })
    }

    @Test
    fun `generateTemperatureInsights returns empty for insufficient data`() {
        val temperatureData = listOf(
            BBTDataPoint(LocalDate(2024, 1, 1), 36.1),
            BBTDataPoint(LocalDate(2024, 1, 2), 36.2)
        )
        
        val insights = generateTemperatureInsights(
            temperatureData, 
            UnitSystem.METRIC, 
            MockUnitConverter()
        )
        
        assertTrue(insights.isEmpty())
    }

    @Test
    fun `BBTDataPoint stores correct values`() {
        val date = LocalDate(2024, 1, 15)
        val temperature = 36.5
        val hasMarker = true
        
        val dataPoint = BBTDataPoint(
            date = date,
            temperature = temperature,
            hasOvulationMarker = hasMarker
        )
        
        assertEquals(date, dataPoint.date)
        assertEquals(temperature, dataPoint.temperature)
        assertEquals(hasMarker, dataPoint.hasOvulationMarker)
    }

    @Test
    fun `BBTDataPoint defaults ovulation marker to false`() {
        val dataPoint = BBTDataPoint(
            date = LocalDate(2024, 1, 15),
            temperature = 36.5
        )
        
        assertEquals(false, dataPoint.hasOvulationMarker)
    }

    // Mock UnitConverter for testing
    private class MockUnitConverter : com.eunio.healthapp.domain.util.UnitConverter {
        override fun convertWeight(value: Double, from: UnitSystem, to: UnitSystem): Double = value
        override fun convertDistance(value: Double, from: UnitSystem, to: UnitSystem): Double = value
        override fun convertTemperature(value: Double, from: UnitSystem, to: UnitSystem): Double = value
        override fun formatWeight(value: Double, unitSystem: UnitSystem): String = "$value kg"
        override fun formatDistance(value: Double, unitSystem: UnitSystem): String = "$value km"
        override fun formatTemperature(value: Double, unitSystem: UnitSystem): String = "$value°C"
    }
}