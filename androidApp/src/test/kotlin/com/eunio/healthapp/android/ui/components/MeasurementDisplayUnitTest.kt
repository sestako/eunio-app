package com.eunio.healthapp.android.ui.components

import com.eunio.healthapp.domain.model.UnitSystem
import com.eunio.healthapp.domain.util.UnitConverterImpl
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for measurement display logic without UI dependencies.
 * Tests the underlying conversion and formatting logic.
 */
class MeasurementDisplayUnitTest {
    
    private val converter = UnitConverterImpl()
    
    @Test
    fun weightConversion_metricToImperial_isCorrect() {
        val result = converter.convertWeight(70.0, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        assertEquals(154.32, result, 0.01)
    }
    
    @Test
    fun weightConversion_imperialToMetric_isCorrect() {
        val result = converter.convertWeight(154.32, UnitSystem.IMPERIAL, UnitSystem.METRIC)
        assertEquals(70.0, result, 0.01)
    }
    
    @Test
    fun distanceConversion_metricToImperial_isCorrect() {
        val result = converter.convertDistance(5.0, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        assertEquals(3.11, result, 0.01)
    }
    
    @Test
    fun distanceConversion_imperialToMetric_isCorrect() {
        val result = converter.convertDistance(3.11, UnitSystem.IMPERIAL, UnitSystem.METRIC)
        assertEquals(5.0, result, 0.01)
    }
    
    @Test
    fun temperatureConversion_celsiusToFahrenheit_isCorrect() {
        val result = converter.convertTemperature(0.0, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        assertEquals(32.0, result, 0.01)
    }
    
    @Test
    fun temperatureConversion_fahrenheitToCelsius_isCorrect() {
        val result = converter.convertTemperature(32.0, UnitSystem.IMPERIAL, UnitSystem.METRIC)
        assertEquals(0.0, result, 0.01)
    }
    
    @Test
    fun weightFormatting_metric_isCorrect() {
        val result = converter.formatWeight(70.0, UnitSystem.METRIC)
        assertEquals("70 kg", result)
    }
    
    @Test
    fun weightFormatting_imperial_isCorrect() {
        val result = converter.formatWeight(154.32, UnitSystem.IMPERIAL)
        assertEquals("154.32 lbs", result)
    }
    
    @Test
    fun distanceFormatting_metric_isCorrect() {
        val result = converter.formatDistance(5.0, UnitSystem.METRIC)
        assertEquals("5 km", result)
    }
    
    @Test
    fun distanceFormatting_imperial_isCorrect() {
        val result = converter.formatDistance(3.11, UnitSystem.IMPERIAL)
        assertEquals("3.11 miles", result)
    }
    
    @Test
    fun temperatureFormatting_metric_isCorrect() {
        val result = converter.formatTemperature(36.5, UnitSystem.METRIC)
        assertEquals("36.5°C", result)
    }
    
    @Test
    fun temperatureFormatting_imperial_isCorrect() {
        val result = converter.formatTemperature(97.7, UnitSystem.IMPERIAL)
        assertEquals("97.7°F", result)
    }
    
    @Test
    fun conversion_sameUnitSystem_returnsOriginalValue() {
        assertEquals(70.0, converter.convertWeight(70.0, UnitSystem.METRIC, UnitSystem.METRIC), 0.01)
        assertEquals(5.0, converter.convertDistance(5.0, UnitSystem.IMPERIAL, UnitSystem.IMPERIAL), 0.01)
        assertEquals(36.5, converter.convertTemperature(36.5, UnitSystem.METRIC, UnitSystem.METRIC), 0.01)
    }
    
    @Test
    fun formatting_handlesZeroValues() {
        assertEquals("0 kg", converter.formatWeight(0.0, UnitSystem.METRIC))
        assertEquals("0 km", converter.formatDistance(0.0, UnitSystem.METRIC))
        assertEquals("0°C", converter.formatTemperature(0.0, UnitSystem.METRIC))
    }
    
    @Test
    fun conversion_handlesNegativeTemperature() {
        // Test the conversion step by step
        val fahrenheitValue = converter.convertTemperature(-10.0, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        // -10°C = (-10 * 9/5) + 32 = -18 + 32 = 14°F
        assertEquals(14.0, fahrenheitValue, 0.01)
    }
}