package com.eunio.healthapp.domain.util

import com.eunio.healthapp.domain.model.settings.TemperatureUnit
import com.eunio.healthapp.domain.model.settings.WeightUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpecificUnitConverterTest {
    
    private val converter = SpecificUnitConverterImpl()
    
    // Temperature Conversion Tests
    
    @Test
    fun `convertTemperature - celsius to fahrenheit conversion`() {
        // Test common temperature conversions
        assertEquals(32.0, converter.convertTemperature(0.0, TemperatureUnit.CELSIUS, TemperatureUnit.FAHRENHEIT), 0.01)
        assertEquals(122.0, converter.convertTemperature(50.0, TemperatureUnit.CELSIUS, TemperatureUnit.FAHRENHEIT), 0.01)
        assertEquals(98.6, converter.convertTemperature(37.0, TemperatureUnit.CELSIUS, TemperatureUnit.FAHRENHEIT), 0.01)
        assertEquals(-40.0, converter.convertTemperature(-40.0, TemperatureUnit.CELSIUS, TemperatureUnit.FAHRENHEIT), 0.01)
    }
    
    @Test
    fun `convertTemperature - fahrenheit to celsius conversion`() {
        // Test common temperature conversions
        assertEquals(0.0, converter.convertTemperature(32.0, TemperatureUnit.FAHRENHEIT, TemperatureUnit.CELSIUS), 0.01)
        assertEquals(50.0, converter.convertTemperature(122.0, TemperatureUnit.FAHRENHEIT, TemperatureUnit.CELSIUS), 0.01)
        assertEquals(37.0, converter.convertTemperature(98.6, TemperatureUnit.FAHRENHEIT, TemperatureUnit.CELSIUS), 0.01)
        assertEquals(-40.0, converter.convertTemperature(-40.0, TemperatureUnit.FAHRENHEIT, TemperatureUnit.CELSIUS), 0.01)
    }
    
    @Test
    fun `convertTemperature - same unit returns original value`() {
        assertEquals(25.0, converter.convertTemperature(25.0, TemperatureUnit.CELSIUS, TemperatureUnit.CELSIUS))
        assertEquals(77.0, converter.convertTemperature(77.0, TemperatureUnit.FAHRENHEIT, TemperatureUnit.FAHRENHEIT))
    }
    
    @Test
    fun `convertTemperature - precision test with decimal values`() {
        // Test precise conversions with decimal values
        assertEquals(98.96, converter.convertTemperature(37.2, TemperatureUnit.CELSIUS, TemperatureUnit.FAHRENHEIT), 0.01)
        assertEquals(36.67, converter.convertTemperature(98.0, TemperatureUnit.FAHRENHEIT, TemperatureUnit.CELSIUS), 0.01)
    }
    
    @Test
    fun `convertTemperature - throws exception for invalid temperature`() {
        assertFailsWith<IllegalArgumentException> {
            converter.convertTemperature(Double.NaN, TemperatureUnit.CELSIUS, TemperatureUnit.FAHRENHEIT)
        }
        
        assertFailsWith<IllegalArgumentException> {
            converter.convertTemperature(Double.POSITIVE_INFINITY, TemperatureUnit.CELSIUS, TemperatureUnit.FAHRENHEIT)
        }
        
        assertFailsWith<IllegalArgumentException> {
            converter.convertTemperature(-100.0, TemperatureUnit.CELSIUS, TemperatureUnit.FAHRENHEIT)
        }
        
        assertFailsWith<IllegalArgumentException> {
            converter.convertTemperature(200.0, TemperatureUnit.FAHRENHEIT, TemperatureUnit.CELSIUS)
        }
    }
    
    // Weight Conversion Tests
    
    @Test
    fun `convertWeight - kilograms to pounds conversion`() {
        assertEquals(2.20, converter.convertWeight(1.0, WeightUnit.KILOGRAMS, WeightUnit.POUNDS), 0.01)
        assertEquals(220.46, converter.convertWeight(100.0, WeightUnit.KILOGRAMS, WeightUnit.POUNDS), 0.01)
        assertEquals(154.32, converter.convertWeight(70.0, WeightUnit.KILOGRAMS, WeightUnit.POUNDS), 0.01)
        assertEquals(0.22, converter.convertWeight(0.1, WeightUnit.KILOGRAMS, WeightUnit.POUNDS), 0.01)
    }
    
    @Test
    fun `convertWeight - pounds to kilograms conversion`() {
        assertEquals(0.45, converter.convertWeight(1.0, WeightUnit.POUNDS, WeightUnit.KILOGRAMS), 0.01)
        assertEquals(45.36, converter.convertWeight(100.0, WeightUnit.POUNDS, WeightUnit.KILOGRAMS), 0.01)
        assertEquals(31.75, converter.convertWeight(70.0, WeightUnit.POUNDS, WeightUnit.KILOGRAMS), 0.01)
        assertEquals(68.04, converter.convertWeight(150.0, WeightUnit.POUNDS, WeightUnit.KILOGRAMS), 0.01)
    }
    
    @Test
    fun `convertWeight - same unit returns original value`() {
        assertEquals(70.0, converter.convertWeight(70.0, WeightUnit.KILOGRAMS, WeightUnit.KILOGRAMS))
        assertEquals(154.0, converter.convertWeight(154.0, WeightUnit.POUNDS, WeightUnit.POUNDS))
    }
    
    @Test
    fun `convertWeight - precision test with decimal values`() {
        assertEquals(154.32, converter.convertWeight(70.0, WeightUnit.KILOGRAMS, WeightUnit.POUNDS), 0.01)
        assertEquals(70.0, converter.convertWeight(154.32, WeightUnit.POUNDS, WeightUnit.KILOGRAMS), 0.01)
    }
    
    @Test
    fun `convertWeight - throws exception for invalid weight`() {
        assertFailsWith<IllegalArgumentException> {
            converter.convertWeight(Double.NaN, WeightUnit.KILOGRAMS, WeightUnit.POUNDS)
        }
        
        assertFailsWith<IllegalArgumentException> {
            converter.convertWeight(Double.POSITIVE_INFINITY, WeightUnit.KILOGRAMS, WeightUnit.POUNDS)
        }
        
        assertFailsWith<IllegalArgumentException> {
            converter.convertWeight(-10.0, WeightUnit.KILOGRAMS, WeightUnit.POUNDS)
        }
        
        assertFailsWith<IllegalArgumentException> {
            converter.convertWeight(0.0, WeightUnit.KILOGRAMS, WeightUnit.POUNDS)
        }
        
        assertFailsWith<IllegalArgumentException> {
            converter.convertWeight(2000.0, WeightUnit.KILOGRAMS, WeightUnit.POUNDS)
        }
    }
    
    // Temperature Formatting Tests
    
    @Test
    fun `formatTemperature - celsius formatting with different decimal places`() {
        assertEquals("37°C", converter.formatTemperature(37.0, TemperatureUnit.CELSIUS, 0))
        assertEquals("37.0°C", converter.formatTemperature(37.0, TemperatureUnit.CELSIUS, 1))
        assertEquals("37.00°C", converter.formatTemperature(37.0, TemperatureUnit.CELSIUS, 2))
        assertEquals("36.5°C", converter.formatTemperature(36.5, TemperatureUnit.CELSIUS, 1))
        assertEquals("36.67°C", converter.formatTemperature(36.666, TemperatureUnit.CELSIUS, 2))
    }
    
    @Test
    fun `formatTemperature - fahrenheit formatting with different decimal places`() {
        assertEquals("98°F", converter.formatTemperature(98.6, TemperatureUnit.FAHRENHEIT, 0))
        assertEquals("98.6°F", converter.formatTemperature(98.6, TemperatureUnit.FAHRENHEIT, 1))
        assertEquals("98.60°F", converter.formatTemperature(98.6, TemperatureUnit.FAHRENHEIT, 2))
        assertEquals("97.7°F", converter.formatTemperature(97.7, TemperatureUnit.FAHRENHEIT, 1))
    }
    
    @Test
    fun `formatTemperature - throws exception for negative decimal places`() {
        assertFailsWith<IllegalArgumentException> {
            converter.formatTemperature(37.0, TemperatureUnit.CELSIUS, -1)
        }
    }
    
    // Weight Formatting Tests
    
    @Test
    fun `formatWeight - kilograms formatting with different decimal places`() {
        assertEquals("70 kg", converter.formatWeight(70.0, WeightUnit.KILOGRAMS, 0))
        assertEquals("70.0 kg", converter.formatWeight(70.0, WeightUnit.KILOGRAMS, 1))
        assertEquals("70.00 kg", converter.formatWeight(70.0, WeightUnit.KILOGRAMS, 2))
        assertEquals("70.5 kg", converter.formatWeight(70.5, WeightUnit.KILOGRAMS, 1))
        assertEquals("70.25 kg", converter.formatWeight(70.25, WeightUnit.KILOGRAMS, 2))
    }
    
    @Test
    fun `formatWeight - pounds formatting with different decimal places`() {
        assertEquals("154 lbs", converter.formatWeight(154.3, WeightUnit.POUNDS, 0))
        assertEquals("154.3 lbs", converter.formatWeight(154.3, WeightUnit.POUNDS, 1))
        assertEquals("154.30 lbs", converter.formatWeight(154.3, WeightUnit.POUNDS, 2))
        assertEquals("154.32 lbs", converter.formatWeight(154.324, WeightUnit.POUNDS, 2))
    }
    
    @Test
    fun `formatWeight - throws exception for negative decimal places`() {
        assertFailsWith<IllegalArgumentException> {
            converter.formatWeight(70.0, WeightUnit.KILOGRAMS, -1)
        }
    }
    
    // Temperature Validation Tests
    
    @Test
    fun `isValidTemperature - valid temperature ranges`() {
        // Valid Celsius temperatures
        assertTrue(converter.isValidTemperature(0.0, TemperatureUnit.CELSIUS))
        assertTrue(converter.isValidTemperature(37.0, TemperatureUnit.CELSIUS))
        assertTrue(converter.isValidTemperature(-10.0, TemperatureUnit.CELSIUS))
        assertTrue(converter.isValidTemperature(50.0, TemperatureUnit.CELSIUS))
        
        // Valid Fahrenheit temperatures
        assertTrue(converter.isValidTemperature(32.0, TemperatureUnit.FAHRENHEIT))
        assertTrue(converter.isValidTemperature(98.6, TemperatureUnit.FAHRENHEIT))
        assertTrue(converter.isValidTemperature(14.0, TemperatureUnit.FAHRENHEIT))
        assertTrue(converter.isValidTemperature(122.0, TemperatureUnit.FAHRENHEIT))
    }
    
    @Test
    fun `isValidTemperature - invalid temperature ranges`() {
        // Invalid Celsius temperatures
        assertFalse(converter.isValidTemperature(-60.0, TemperatureUnit.CELSIUS))
        assertFalse(converter.isValidTemperature(70.0, TemperatureUnit.CELSIUS))
        assertFalse(converter.isValidTemperature(Double.NaN, TemperatureUnit.CELSIUS))
        assertFalse(converter.isValidTemperature(Double.POSITIVE_INFINITY, TemperatureUnit.CELSIUS))
        
        // Invalid Fahrenheit temperatures
        assertFalse(converter.isValidTemperature(-80.0, TemperatureUnit.FAHRENHEIT))
        assertFalse(converter.isValidTemperature(200.0, TemperatureUnit.FAHRENHEIT))
        assertFalse(converter.isValidTemperature(Double.NaN, TemperatureUnit.FAHRENHEIT))
        assertFalse(converter.isValidTemperature(Double.NEGATIVE_INFINITY, TemperatureUnit.FAHRENHEIT))
    }
    
    // Weight Validation Tests
    
    @Test
    fun `isValidWeight - valid weight ranges`() {
        // Valid kilogram weights
        assertTrue(converter.isValidWeight(0.1, WeightUnit.KILOGRAMS))
        assertTrue(converter.isValidWeight(70.0, WeightUnit.KILOGRAMS))
        assertTrue(converter.isValidWeight(100.0, WeightUnit.KILOGRAMS))
        assertTrue(converter.isValidWeight(500.0, WeightUnit.KILOGRAMS))
        
        // Valid pound weights
        assertTrue(converter.isValidWeight(0.5, WeightUnit.POUNDS))
        assertTrue(converter.isValidWeight(154.0, WeightUnit.POUNDS))
        assertTrue(converter.isValidWeight(220.0, WeightUnit.POUNDS))
        assertTrue(converter.isValidWeight(1000.0, WeightUnit.POUNDS))
    }
    
    @Test
    fun `isValidWeight - invalid weight ranges`() {
        // Invalid kilogram weights
        assertFalse(converter.isValidWeight(0.0, WeightUnit.KILOGRAMS))
        assertFalse(converter.isValidWeight(-10.0, WeightUnit.KILOGRAMS))
        assertFalse(converter.isValidWeight(1500.0, WeightUnit.KILOGRAMS))
        assertFalse(converter.isValidWeight(Double.NaN, WeightUnit.KILOGRAMS))
        assertFalse(converter.isValidWeight(Double.POSITIVE_INFINITY, WeightUnit.KILOGRAMS))
        
        // Invalid pound weights
        assertFalse(converter.isValidWeight(0.0, WeightUnit.POUNDS))
        assertFalse(converter.isValidWeight(-5.0, WeightUnit.POUNDS))
        assertFalse(converter.isValidWeight(3000.0, WeightUnit.POUNDS))
        assertFalse(converter.isValidWeight(Double.NaN, WeightUnit.POUNDS))
        assertFalse(converter.isValidWeight(Double.NEGATIVE_INFINITY, WeightUnit.POUNDS))
    }
    
    // Edge Cases and Boundary Tests
    
    @Test
    fun `convertTemperature - boundary values`() {
        // Test boundary values
        assertEquals(-58.0, converter.convertTemperature(-50.0, TemperatureUnit.CELSIUS, TemperatureUnit.FAHRENHEIT), 0.01)
        assertEquals(122.0, converter.convertTemperature(50.0, TemperatureUnit.CELSIUS, TemperatureUnit.FAHRENHEIT), 0.01)
        assertEquals(-50.0, converter.convertTemperature(-58.0, TemperatureUnit.FAHRENHEIT, TemperatureUnit.CELSIUS), 0.01)
        assertEquals(50.0, converter.convertTemperature(122.0, TemperatureUnit.FAHRENHEIT, TemperatureUnit.CELSIUS), 0.01)
    }
    
    @Test
    fun `convertWeight - boundary values`() {
        // Test boundary values
        assertEquals(0.22, converter.convertWeight(0.1, WeightUnit.KILOGRAMS, WeightUnit.POUNDS), 0.01)
        assertEquals(1984.16, converter.convertWeight(900.0, WeightUnit.KILOGRAMS, WeightUnit.POUNDS), 0.01)
        assertEquals(0.11, converter.convertWeight(0.25, WeightUnit.POUNDS, WeightUnit.KILOGRAMS), 0.01)
        assertEquals(408.23, converter.convertWeight(900.0, WeightUnit.POUNDS, WeightUnit.KILOGRAMS), 0.01)
    }
    
    @Test
    fun `roundtrip conversions maintain precision`() {
        // Temperature roundtrip
        val originalTemp = 37.5
        val fahrenheit = converter.convertTemperature(originalTemp, TemperatureUnit.CELSIUS, TemperatureUnit.FAHRENHEIT)
        val backToCelsius = converter.convertTemperature(fahrenheit, TemperatureUnit.FAHRENHEIT, TemperatureUnit.CELSIUS)
        assertEquals(originalTemp, backToCelsius, 0.01)
        
        // Weight roundtrip
        val originalWeight = 70.5
        val pounds = converter.convertWeight(originalWeight, WeightUnit.KILOGRAMS, WeightUnit.POUNDS)
        val backToKg = converter.convertWeight(pounds, WeightUnit.POUNDS, WeightUnit.KILOGRAMS)
        assertEquals(originalWeight, backToKg, 0.01)
    }
}