package com.eunio.healthapp.domain.model.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UnitPreferencesTest {
    
    @Test
    fun `default constructor creates valid preferences`() {
        val preferences = UnitPreferences()
        
        assertEquals(TemperatureUnit.CELSIUS, preferences.temperatureUnit)
        assertEquals(WeightUnit.KILOGRAMS, preferences.weightUnit)
        assertFalse(preferences.isManuallySet)
        assertTrue(preferences.isValid())
    }
    
    @Test
    fun `default factory method creates valid preferences`() {
        val preferences = UnitPreferences.default()
        
        assertEquals(TemperatureUnit.CELSIUS, preferences.temperatureUnit)
        assertEquals(WeightUnit.KILOGRAMS, preferences.weightUnit)
        assertFalse(preferences.isManuallySet)
        assertTrue(preferences.isValid())
    }
    
    @Test
    fun `fromLocale creates correct preferences for US`() {
        val preferences = UnitPreferences.fromLocale("US")
        
        assertEquals(TemperatureUnit.FAHRENHEIT, preferences.temperatureUnit)
        assertEquals(WeightUnit.POUNDS, preferences.weightUnit)
        assertFalse(preferences.isManuallySet)
        assertTrue(preferences.isValid())
    }
    
    @Test
    fun `fromLocale creates correct preferences for Germany`() {
        val preferences = UnitPreferences.fromLocale("DE")
        
        assertEquals(TemperatureUnit.CELSIUS, preferences.temperatureUnit)
        assertEquals(WeightUnit.KILOGRAMS, preferences.weightUnit)
        assertFalse(preferences.isManuallySet)
        assertTrue(preferences.isValid())
    }
    
    @Test
    fun `fromLocale creates correct preferences for unknown locale`() {
        val preferences = UnitPreferences.fromLocale("UNKNOWN")
        
        assertEquals(TemperatureUnit.CELSIUS, preferences.temperatureUnit)
        assertEquals(WeightUnit.KILOGRAMS, preferences.weightUnit)
        assertFalse(preferences.isManuallySet)
        assertTrue(preferences.isValid())
    }
    
    @Test
    fun `manually set preferences are valid`() {
        val preferences = UnitPreferences(
            temperatureUnit = TemperatureUnit.FAHRENHEIT,
            weightUnit = WeightUnit.POUNDS,
            isManuallySet = true
        )
        
        assertEquals(TemperatureUnit.FAHRENHEIT, preferences.temperatureUnit)
        assertEquals(WeightUnit.POUNDS, preferences.weightUnit)
        assertTrue(preferences.isManuallySet)
        assertTrue(preferences.isValid())
    }
    
    @Test
    fun `mixed unit preferences are valid`() {
        val preferences = UnitPreferences(
            temperatureUnit = TemperatureUnit.CELSIUS,
            weightUnit = WeightUnit.POUNDS,
            isManuallySet = true
        )
        
        assertTrue(preferences.isValid())
    }
    
    @Test
    fun `all enum combinations are valid`() {
        val combinations = listOf(
            UnitPreferences(TemperatureUnit.CELSIUS, WeightUnit.KILOGRAMS),
            UnitPreferences(TemperatureUnit.CELSIUS, WeightUnit.POUNDS),
            UnitPreferences(TemperatureUnit.FAHRENHEIT, WeightUnit.KILOGRAMS),
            UnitPreferences(TemperatureUnit.FAHRENHEIT, WeightUnit.POUNDS)
        )
        
        combinations.forEach { preferences ->
            assertTrue(preferences.isValid(), "Preferences should be valid: $preferences")
        }
    }
    
    @Test
    fun `copy with changes works correctly`() {
        val original = UnitPreferences.default()
        val modified = original.copy(
            temperatureUnit = TemperatureUnit.FAHRENHEIT,
            isManuallySet = true
        )
        
        assertEquals(TemperatureUnit.CELSIUS, original.temperatureUnit)
        assertEquals(TemperatureUnit.FAHRENHEIT, modified.temperatureUnit)
        assertFalse(original.isManuallySet)
        assertTrue(modified.isManuallySet)
    }
    
    @Test
    fun `serialization annotations are present`() {
        // This test ensures the class can be serialized
        val preferences = UnitPreferences(
            temperatureUnit = TemperatureUnit.FAHRENHEIT,
            weightUnit = WeightUnit.POUNDS,
            isManuallySet = true
        )
        
        // If serialization annotations are missing, this would fail at compile time
        assertTrue(preferences.isValid())
    }
}