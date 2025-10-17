package com.eunio.healthapp.integration

import com.eunio.healthapp.domain.model.settings.*
import com.eunio.healthapp.domain.util.EnhancedUnitConverterImpl
import kotlin.test.*

/**
 * Simple integration tests for settings functionality.
 * Tests core unit conversion and settings integration without complex dependencies.
 */
class SimpleSettingsIntegrationTest {
    
    private val enhancedConverter = EnhancedUnitConverterImpl()
    
    @Test
    fun `enhanced unit converter handles temperature conversions correctly`() {
        // Test Celsius to Fahrenheit
        val celsius = 36.5
        val fahrenheit = enhancedConverter.convertTemperature(
            value = celsius,
            from = TemperatureUnit.CELSIUS,
            to = TemperatureUnit.FAHRENHEIT
        )
        assertEquals(97.7, fahrenheit, 0.1)
        
        // Test Fahrenheit to Celsius
        val fahrenheitInput = 98.6
        val celsiusOutput = enhancedConverter.convertTemperature(
            value = fahrenheitInput,
            from = TemperatureUnit.FAHRENHEIT,
            to = TemperatureUnit.CELSIUS
        )
        assertEquals(37.0, celsiusOutput, 0.1)
        
        // Test same unit conversion
        val sameUnit = enhancedConverter.convertTemperature(
            value = 37.0,
            from = TemperatureUnit.CELSIUS,
            to = TemperatureUnit.CELSIUS
        )
        assertEquals(37.0, sameUnit, 0.01)
    }
    
    @Test
    fun `enhanced unit converter handles weight conversions correctly`() {
        // Test Kilograms to Pounds
        val kg = 70.0
        val lbs = enhancedConverter.convertWeight(
            value = kg,
            from = WeightUnit.KILOGRAMS,
            to = WeightUnit.POUNDS
        )
        assertEquals(154.32, lbs, 0.1)
        
        // Test Pounds to Kilograms
        val lbsInput = 150.0
        val kgOutput = enhancedConverter.convertWeight(
            value = lbsInput,
            from = WeightUnit.POUNDS,
            to = WeightUnit.KILOGRAMS
        )
        assertEquals(68.04, kgOutput, 0.1)
        
        // Test same unit conversion
        val sameUnit = enhancedConverter.convertWeight(
            value = 70.0,
            from = WeightUnit.KILOGRAMS,
            to = WeightUnit.KILOGRAMS
        )
        assertEquals(70.0, sameUnit, 0.01)
    }
    
    @Test
    fun `enhanced unit converter formats values correctly`() {
        // Test temperature formatting
        val tempCelsius = enhancedConverter.formatTemperature(36.5, TemperatureUnit.CELSIUS)
        assertEquals("36.5°C", tempCelsius)
        
        val tempFahrenheit = enhancedConverter.formatTemperature(98.6, TemperatureUnit.FAHRENHEIT)
        assertEquals("98.6°F", tempFahrenheit)
        
        // Test weight formatting
        val weightKg = enhancedConverter.formatWeight(70.0, WeightUnit.KILOGRAMS)
        assertEquals("70 kg", weightKg)
        
        val weightLbs = enhancedConverter.formatWeight(154.32, WeightUnit.POUNDS)
        assertEquals("154.32 lbs", weightLbs)
    }
    
    @Test
    fun `unit preferences can be created from locale`() {
        // Test US locale (should use Imperial units)
        val usPreferences = UnitPreferences.fromLocale("US")
        assertEquals(TemperatureUnit.FAHRENHEIT, usPreferences.temperatureUnit)
        assertEquals(WeightUnit.POUNDS, usPreferences.weightUnit)
        assertFalse(usPreferences.isManuallySet)
        
        // Test German locale (should use Metric units)
        val dePreferences = UnitPreferences.fromLocale("DE")
        assertEquals(TemperatureUnit.CELSIUS, dePreferences.temperatureUnit)
        assertEquals(WeightUnit.KILOGRAMS, dePreferences.weightUnit)
        assertFalse(dePreferences.isManuallySet)
    }
    
    @Test
    fun `temperature unit conversion methods work correctly`() {
        // Test static conversion methods
        val celsiusToFahrenheit = TemperatureUnit.convert(
            value = 0.0,
            from = TemperatureUnit.CELSIUS,
            to = TemperatureUnit.FAHRENHEIT
        )
        assertEquals(32.0, celsiusToFahrenheit, 0.01)
        
        val fahrenheitToCelsius = TemperatureUnit.convert(
            value = 32.0,
            from = TemperatureUnit.FAHRENHEIT,
            to = TemperatureUnit.CELSIUS
        )
        assertEquals(0.0, fahrenheitToCelsius, 0.01)
    }
    
    @Test
    fun `weight unit conversion methods work correctly`() {
        // Test static conversion methods
        val kgToPounds = WeightUnit.convert(
            value = 1.0,
            from = WeightUnit.KILOGRAMS,
            to = WeightUnit.POUNDS
        )
        assertEquals(2.20462, kgToPounds, 0.001)
        
        val poundsToKg = WeightUnit.convert(
            value = 2.20462,
            from = WeightUnit.POUNDS,
            to = WeightUnit.KILOGRAMS
        )
        assertEquals(1.0, poundsToKg, 0.001)
    }
    
    @Test
    fun `enhanced converter storage format conversions work correctly`() {
        // Test temperature storage conversions
        val fahrenheitInput = 98.6
        val storedCelsius = enhancedConverter.convertTemperatureToCelsius(
            value = fahrenheitInput,
            sourceUnit = TemperatureUnit.FAHRENHEIT
        )
        assertEquals(37.0, storedCelsius, 0.1)
        
        val displayFahrenheit = enhancedConverter.convertTemperatureFromCelsius(
            celsiusValue = storedCelsius,
            targetUnit = TemperatureUnit.FAHRENHEIT
        )
        assertEquals(fahrenheitInput, displayFahrenheit, 0.1)
        
        // Test weight storage conversions
        val poundsInput = 154.32
        val storedKg = enhancedConverter.convertWeightToKilograms(
            value = poundsInput,
            sourceUnit = WeightUnit.POUNDS
        )
        assertEquals(70.0, storedKg, 0.1)
        
        val displayPounds = enhancedConverter.convertWeightFromKilograms(
            kgValue = storedKg,
            targetUnit = WeightUnit.POUNDS
        )
        assertEquals(poundsInput, displayPounds, 0.1)
    }
}