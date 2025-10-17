package com.eunio.healthapp.domain.util

import com.eunio.healthapp.domain.error.UnitSystemError
import com.eunio.healthapp.domain.model.UnitSystem
import kotlin.test.*

class UnitConverterImplTest {
    
    private lateinit var converter: UnitConverterImpl
    
    @BeforeTest
    fun setup() {
        converter = UnitConverterImpl()
    }
    
    // Weight Conversion Tests
    
    @Test
    fun `convertWeight from metric to imperial`() {
        // Test basic conversion
        assertEquals(2.20, converter.convertWeight(1.0, UnitSystem.METRIC, UnitSystem.IMPERIAL), 0.01)
        assertEquals(22.05, converter.convertWeight(10.0, UnitSystem.METRIC, UnitSystem.IMPERIAL), 0.01)
        assertEquals(220.46, converter.convertWeight(100.0, UnitSystem.METRIC, UnitSystem.IMPERIAL), 0.01)
    }
    
    @Test
    fun `convertWeight from imperial to metric`() {
        // Test basic conversion
        assertEquals(0.45, converter.convertWeight(1.0, UnitSystem.IMPERIAL, UnitSystem.METRIC), 0.01)
        assertEquals(4.54, converter.convertWeight(10.0, UnitSystem.IMPERIAL, UnitSystem.METRIC), 0.01)
        assertEquals(45.36, converter.convertWeight(100.0, UnitSystem.IMPERIAL, UnitSystem.METRIC), 0.01)
    }
    
    @Test
    fun `convertWeight returns same value when from and to are same`() {
        assertEquals(50.0, converter.convertWeight(50.0, UnitSystem.METRIC, UnitSystem.METRIC))
        assertEquals(50.0, converter.convertWeight(50.0, UnitSystem.IMPERIAL, UnitSystem.IMPERIAL))
    }
    
    @Test
    fun `convertWeight handles zero weight`() {
        assertEquals(0.0, converter.convertWeight(0.0, UnitSystem.METRIC, UnitSystem.IMPERIAL))
        assertEquals(0.0, converter.convertWeight(0.0, UnitSystem.IMPERIAL, UnitSystem.METRIC))
    }
    
    @Test
    fun `convertWeight throws exception for negative weight`() {
        assertFailsWith<UnitSystemError.UnitValidationError> {
            converter.convertWeight(-1.0, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        }
    }
    
    @Test
    fun `convertWeight throws exception for infinite weight`() {
        assertFailsWith<UnitSystemError.UnitValidationError> {
            converter.convertWeight(Double.POSITIVE_INFINITY, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        }
        
        assertFailsWith<UnitSystemError.UnitValidationError> {
            converter.convertWeight(Double.NaN, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        }
    }
    
    @Test
    fun `convertWeight throws exception for weight exceeding maximum`() {
        assertFailsWith<UnitSystemError.UnitValidationError> {
            converter.convertWeight(1001.0, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        }
        
        assertFailsWith<UnitSystemError.UnitValidationError> {
            converter.convertWeight(2205.0, UnitSystem.IMPERIAL, UnitSystem.METRIC)
        }
    }
    
    // Distance Conversion Tests
    
    @Test
    fun `convertDistance from metric to imperial`() {
        assertEquals(0.62, converter.convertDistance(1.0, UnitSystem.METRIC, UnitSystem.IMPERIAL), 0.01)
        assertEquals(6.21, converter.convertDistance(10.0, UnitSystem.METRIC, UnitSystem.IMPERIAL), 0.01)
        assertEquals(62.14, converter.convertDistance(100.0, UnitSystem.METRIC, UnitSystem.IMPERIAL), 0.01)
    }
    
    @Test
    fun `convertDistance from imperial to metric`() {
        assertEquals(1.61, converter.convertDistance(1.0, UnitSystem.IMPERIAL, UnitSystem.METRIC), 0.01)
        assertEquals(16.09, converter.convertDistance(10.0, UnitSystem.IMPERIAL, UnitSystem.METRIC), 0.01)
        assertEquals(160.93, converter.convertDistance(100.0, UnitSystem.IMPERIAL, UnitSystem.METRIC), 0.01)
    }
    
    @Test
    fun `convertDistance returns same value when from and to are same`() {
        assertEquals(25.0, converter.convertDistance(25.0, UnitSystem.METRIC, UnitSystem.METRIC))
        assertEquals(25.0, converter.convertDistance(25.0, UnitSystem.IMPERIAL, UnitSystem.IMPERIAL))
    }
    
    @Test
    fun `convertDistance handles zero distance`() {
        assertEquals(0.0, converter.convertDistance(0.0, UnitSystem.METRIC, UnitSystem.IMPERIAL))
        assertEquals(0.0, converter.convertDistance(0.0, UnitSystem.IMPERIAL, UnitSystem.METRIC))
    }
    
    @Test
    fun `convertDistance throws exception for negative distance`() {
        assertFailsWith<UnitSystemError.UnitValidationError> {
            converter.convertDistance(-1.0, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        }
    }
    
    @Test
    fun `convertDistance throws exception for distance exceeding maximum`() {
        assertFailsWith<UnitSystemError.UnitValidationError> {
            converter.convertDistance(50001.0, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        }
        
        assertFailsWith<UnitSystemError.UnitValidationError> {
            converter.convertDistance(31069.0, UnitSystem.IMPERIAL, UnitSystem.METRIC)
        }
    }
    
    // Temperature Conversion Tests
    
    @Test
    fun `convertTemperature from celsius to fahrenheit`() {
        assertEquals(32.0, converter.convertTemperature(0.0, UnitSystem.METRIC, UnitSystem.IMPERIAL), 0.01)
        assertEquals(212.0, converter.convertTemperature(100.0, UnitSystem.METRIC, UnitSystem.IMPERIAL), 0.01)
        assertEquals(98.6, converter.convertTemperature(37.0, UnitSystem.METRIC, UnitSystem.IMPERIAL), 0.01)
    }
    
    @Test
    fun `convertTemperature from fahrenheit to celsius`() {
        assertEquals(0.0, converter.convertTemperature(32.0, UnitSystem.IMPERIAL, UnitSystem.METRIC), 0.01)
        assertEquals(100.0, converter.convertTemperature(212.0, UnitSystem.IMPERIAL, UnitSystem.METRIC), 0.01)
        assertEquals(37.0, converter.convertTemperature(98.6, UnitSystem.IMPERIAL, UnitSystem.METRIC), 0.01)
    }
    
    @Test
    fun `convertTemperature returns same value when from and to are same`() {
        assertEquals(25.0, converter.convertTemperature(25.0, UnitSystem.METRIC, UnitSystem.METRIC))
        assertEquals(77.0, converter.convertTemperature(77.0, UnitSystem.IMPERIAL, UnitSystem.IMPERIAL))
    }
    
    @Test
    fun `convertTemperature throws exception for temperature below absolute zero`() {
        assertFailsWith<UnitSystemError.UnitValidationError> {
            converter.convertTemperature(-274.0, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        }
        
        assertFailsWith<UnitSystemError.UnitValidationError> {
            converter.convertTemperature(-460.0, UnitSystem.IMPERIAL, UnitSystem.METRIC)
        }
    }
    
    @Test
    fun `convertTemperature throws exception for temperature exceeding maximum`() {
        assertFailsWith<UnitSystemError.UnitValidationError> {
            converter.convertTemperature(1001.0, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        }
        
        assertFailsWith<UnitSystemError.UnitValidationError> {
            converter.convertTemperature(1833.0, UnitSystem.IMPERIAL, UnitSystem.METRIC)
        }
    }
    
    // Formatting Tests
    
    @Test
    fun `formatWeight returns correct format for metric`() {
        assertEquals("70 kg", converter.formatWeight(70.0, UnitSystem.METRIC))
        assertEquals("70.5 kg", converter.formatWeight(70.5, UnitSystem.METRIC))
        assertEquals("70.25 kg", converter.formatWeight(70.25, UnitSystem.METRIC))
    }
    
    @Test
    fun `formatWeight returns correct format for imperial`() {
        assertEquals("154 lbs", converter.formatWeight(154.0, UnitSystem.IMPERIAL))
        assertEquals("154.3 lbs", converter.formatWeight(154.3, UnitSystem.IMPERIAL))
        assertEquals("154.32 lbs", converter.formatWeight(154.32, UnitSystem.IMPERIAL))
    }
    
    @Test
    fun `formatWeight throws exception for invalid weight`() {
        assertFailsWith<UnitSystemError.UnitValidationError> {
            converter.formatWeight(-1.0, UnitSystem.METRIC)
        }
        
        assertFailsWith<UnitSystemError.UnitValidationError> {
            converter.formatWeight(Double.NaN, UnitSystem.IMPERIAL)
        }
    }
    
    @Test
    fun `formatDistance returns correct format for metric`() {
        assertEquals("5 km", converter.formatDistance(5.0, UnitSystem.METRIC))
        assertEquals("5.5 km", converter.formatDistance(5.5, UnitSystem.METRIC))
        assertEquals("5.25 km", converter.formatDistance(5.25, UnitSystem.METRIC))
    }
    
    @Test
    fun `formatDistance returns correct format for imperial`() {
        assertEquals("3 miles", converter.formatDistance(3.0, UnitSystem.IMPERIAL))
        assertEquals("3.1 miles", converter.formatDistance(3.1, UnitSystem.IMPERIAL))
        assertEquals("3.14 miles", converter.formatDistance(3.14, UnitSystem.IMPERIAL))
    }
    
    @Test
    fun `formatDistance throws exception for invalid distance`() {
        assertFailsWith<UnitSystemError.UnitValidationError> {
            converter.formatDistance(-1.0, UnitSystem.METRIC)
        }
        
        assertFailsWith<UnitSystemError.UnitValidationError> {
            converter.formatDistance(Double.POSITIVE_INFINITY, UnitSystem.IMPERIAL)
        }
    }
    
    @Test
    fun `formatTemperature returns correct format for metric`() {
        assertEquals("37°C", converter.formatTemperature(37.0, UnitSystem.METRIC))
        assertEquals("37.5°C", converter.formatTemperature(37.5, UnitSystem.METRIC))
        assertEquals("37.25°C", converter.formatTemperature(37.25, UnitSystem.METRIC))
    }
    
    @Test
    fun `formatTemperature returns correct format for imperial`() {
        assertEquals("98°F", converter.formatTemperature(98.0, UnitSystem.IMPERIAL))
        assertEquals("98.6°F", converter.formatTemperature(98.6, UnitSystem.IMPERIAL))
        assertEquals("98.65°F", converter.formatTemperature(98.65, UnitSystem.IMPERIAL))
    }
    
    @Test
    fun `formatTemperature throws exception for invalid temperature`() {
        assertFailsWith<UnitSystemError.UnitValidationError> {
            converter.formatTemperature(-300.0, UnitSystem.METRIC)
        }
        
        assertFailsWith<UnitSystemError.UnitValidationError> {
            converter.formatTemperature(Double.NaN, UnitSystem.IMPERIAL)
        }
    }
    
    // Edge Cases and Precision Tests
    
    @Test
    fun `conversion maintains precision within acceptable bounds`() {
        val originalKg = 75.5
        val convertedLbs = converter.convertWeight(originalKg, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        val backToKg = converter.convertWeight(convertedLbs, UnitSystem.IMPERIAL, UnitSystem.METRIC)
        
        // Should be within 0.01 due to rounding
        assertEquals(originalKg, backToKg, 0.01)
    }
    
    @Test
    fun `formatting handles integer values correctly`() {
        assertEquals("70 kg", converter.formatWeight(70.0, UnitSystem.METRIC))
        assertEquals("5 km", converter.formatDistance(5.0, UnitSystem.METRIC))
        assertEquals("37°C", converter.formatTemperature(37.0, UnitSystem.METRIC))
    }
    
    @Test
    fun `formatting handles single decimal place correctly`() {
        assertEquals("70.5 kg", converter.formatWeight(70.5, UnitSystem.METRIC))
        assertEquals("5.5 km", converter.formatDistance(5.5, UnitSystem.METRIC))
        assertEquals("37.5°C", converter.formatTemperature(37.5, UnitSystem.METRIC))
    }
    
    @Test
    fun `formatting handles two decimal places correctly`() {
        assertEquals("70.25 kg", converter.formatWeight(70.25, UnitSystem.METRIC))
        assertEquals("5.25 km", converter.formatDistance(5.25, UnitSystem.METRIC))
        assertEquals("37.25°C", converter.formatTemperature(37.25, UnitSystem.METRIC))
    }
}