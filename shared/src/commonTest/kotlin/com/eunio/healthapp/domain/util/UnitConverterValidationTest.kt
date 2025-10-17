package com.eunio.healthapp.domain.util

import com.eunio.healthapp.domain.error.UnitSystemError
import com.eunio.healthapp.domain.model.UnitSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class UnitConverterValidationTest {
    
    private val converter = UnitConverterImpl()
    
    @Test
    fun `convertWeight should validate negative values`() {
        val exception = assertFailsWith<UnitSystemError.UnitValidationError> {
            converter.convertWeight(-10.0, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        }
        
        assertTrue(exception.message.contains("Weight cannot be negative"))
        assertEquals("weight", exception.field)
        assertEquals(-10.0, exception.inputValue)
    }
    
    @Test
    fun `convertWeight should validate infinite values`() {
        val exception = assertFailsWith<UnitSystemError.UnitValidationError> {
            converter.convertWeight(Double.POSITIVE_INFINITY, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        }
        
        assertTrue(exception.message.contains("Weight value must be a finite number"))
        assertEquals("weight", exception.field)
    }
    
    @Test
    fun `convertWeight should validate NaN values`() {
        val exception = assertFailsWith<UnitSystemError.UnitValidationError> {
            converter.convertWeight(Double.NaN, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        }
        
        assertTrue(exception.message.contains("Weight value must be a finite number"))
        assertEquals("weight", exception.field)
    }
    
    @Test
    fun `convertWeight should validate maximum values for metric`() {
        val exception = assertFailsWith<UnitSystemError.UnitValidationError> {
            converter.convertWeight(2000.0, UnitSystem.METRIC, UnitSystem.IMPERIAL) // Over 1000kg limit
        }
        
        assertTrue(exception.message.contains("Weight must be between"))
        assertTrue(exception.message.contains("kg"))
        assertEquals("weight", exception.field)
    }
    
    @Test
    fun `convertWeight should allow zero values`() {
        // Zero should be allowed
        val result = converter.convertWeight(0.0, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        assertEquals(0.0, result)
    }
    
    @Test
    fun `convertDistance should validate negative values`() {
        val exception = assertFailsWith<UnitSystemError.UnitValidationError> {
            converter.convertDistance(-5.0, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        }
        
        assertTrue(exception.message.contains("Distance cannot be negative"))
        assertEquals("distance", exception.field)
        assertEquals(-5.0, exception.inputValue)
    }
    
    @Test
    fun `convertDistance should validate infinite values`() {
        val exception = assertFailsWith<UnitSystemError.UnitValidationError> {
            converter.convertDistance(Double.NEGATIVE_INFINITY, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        }
        
        assertTrue(exception.message.contains("Distance value must be a finite number"))
        assertEquals("distance", exception.field)
    }
    
    @Test
    fun `convertDistance should allow zero values`() {
        // Zero should be allowed
        val result = converter.convertDistance(0.0, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        assertEquals(0.0, result)
    }
    
    @Test
    fun `convertDistance should validate maximum values`() {
        val exception = assertFailsWith<UnitSystemError.UnitValidationError> {
            converter.convertDistance(100000.0, UnitSystem.METRIC, UnitSystem.IMPERIAL) // Over 50,000km limit
        }
        
        assertTrue(exception.message.contains("Distance must be between"))
        assertTrue(exception.message.contains("km"))
        assertEquals("distance", exception.field)
    }
    
    @Test
    fun `convertTemperature should validate below absolute zero in Celsius`() {
        val exception = assertFailsWith<UnitSystemError.UnitValidationError> {
            converter.convertTemperature(-300.0, UnitSystem.METRIC, UnitSystem.IMPERIAL) // Below -273.15°C
        }
        
        assertTrue(exception.message.contains("Temperature must be between"))
        assertTrue(exception.message.contains("°C"))
        assertEquals("temperature", exception.field)
    }
    
    @Test
    fun `convertTemperature should validate below absolute zero in Fahrenheit`() {
        val exception = assertFailsWith<UnitSystemError.UnitValidationError> {
            converter.convertTemperature(-500.0, UnitSystem.IMPERIAL, UnitSystem.METRIC) // Below -459.67°F
        }
        
        assertTrue(exception.message.contains("Temperature must be between"))
        assertTrue(exception.message.contains("°F"))
        assertEquals("temperature", exception.field)
    }
    
    @Test
    fun `convertTemperature should validate maximum temperature`() {
        val exception = assertFailsWith<UnitSystemError.UnitValidationError> {
            converter.convertTemperature(2000.0, UnitSystem.METRIC, UnitSystem.IMPERIAL) // Over 1000°C limit
        }
        
        assertTrue(exception.message.contains("Temperature must be between"))
        assertTrue(exception.message.contains("°C"))
        assertEquals("temperature", exception.field)
    }
    
    @Test
    fun `formatWeight should validate input values`() {
        val exception = assertFailsWith<UnitSystemError.UnitValidationError> {
            converter.formatWeight(-10.0, UnitSystem.METRIC)
        }
        
        assertTrue(exception.message.contains("Invalid weight value for formatting"))
        assertEquals("weight", exception.field)
        assertEquals(-10.0, exception.inputValue)
    }
    
    @Test
    fun `formatDistance should validate input values`() {
        val exception = assertFailsWith<UnitSystemError.UnitValidationError> {
            converter.formatDistance(Double.NaN, UnitSystem.METRIC)
        }
        
        assertTrue(exception.message.contains("Invalid distance value for formatting"))
        assertEquals("distance", exception.field)
    }
    
    @Test
    fun `formatTemperature should validate input values`() {
        val exception = assertFailsWith<UnitSystemError.UnitValidationError> {
            converter.formatTemperature(-500.0, UnitSystem.IMPERIAL)
        }
        
        assertTrue(exception.message.contains("Invalid temperature value for formatting"))
        assertEquals("temperature", exception.field)
    }
    
    @Test
    fun `valid conversions should work without errors`() {
        // Test valid weight conversion
        val weight = converter.convertWeight(70.0, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        assertTrue(weight > 0)
        
        // Test valid distance conversion
        val distance = converter.convertDistance(10.0, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        assertTrue(distance > 0)
        
        // Test valid temperature conversion
        val temperature = converter.convertTemperature(25.0, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        assertTrue(temperature > 0)
    }
    
    @Test
    fun `valid formatting should work without errors`() {
        // Test valid weight formatting
        val weightStr = converter.formatWeight(70.0, UnitSystem.METRIC)
        assertTrue(weightStr.contains("kg"))
        
        // Test valid distance formatting
        val distanceStr = converter.formatDistance(10.0, UnitSystem.METRIC)
        assertTrue(distanceStr.contains("km"))
        
        // Test valid temperature formatting
        val temperatureStr = converter.formatTemperature(25.0, UnitSystem.METRIC)
        assertTrue(temperatureStr.contains("°C"))
    }
    
    @Test
    fun `edge case values should be handled correctly`() {
        // Test zero values
        val zeroWeight = converter.convertWeight(0.0, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        assertEquals(0.0, zeroWeight)
        
        val zeroDistance = converter.convertDistance(0.0, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        assertEquals(0.0, zeroDistance)
        
        // Test small valid values
        val smallWeight = converter.convertWeight(0.01, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        assertTrue(smallWeight > 0)
        
        val smallDistance = converter.convertDistance(0.001, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        assertTrue(smallDistance >= 0)
        
        val absoluteZeroC = converter.convertTemperature(-273.15, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        assertEquals(-459.67, absoluteZeroC, 0.01)
        
        // Test maximum valid values
        val maxWeight = converter.convertWeight(1000.0, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        assertTrue(maxWeight > 0)
        
        val maxDistance = converter.convertDistance(50000.0, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        assertTrue(maxDistance > 0)
        
        val maxTemp = converter.convertTemperature(1000.0, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        assertTrue(maxTemp > 0)
    }
}