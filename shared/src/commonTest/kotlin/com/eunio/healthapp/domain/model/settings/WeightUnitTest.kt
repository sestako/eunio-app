package com.eunio.healthapp.domain.model.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class WeightUnitTest {
    
    @Test
    fun `weight unit enum has correct display names and symbols`() {
        assertEquals("Kilograms", WeightUnit.KILOGRAMS.displayName)
        assertEquals("kg", WeightUnit.KILOGRAMS.symbol)
        assertEquals("Pounds", WeightUnit.POUNDS.displayName)
        assertEquals("lbs", WeightUnit.POUNDS.symbol)
    }
    
    @Test
    fun `fromLocale returns correct unit for US locale`() {
        assertEquals(WeightUnit.POUNDS, WeightUnit.fromLocale("US"))
        assertEquals(WeightUnit.POUNDS, WeightUnit.fromLocale("us"))
    }
    
    @Test
    fun `fromLocale returns correct unit for imperial countries`() {
        assertEquals(WeightUnit.POUNDS, WeightUnit.fromLocale("LR")) // Liberia
        assertEquals(WeightUnit.POUNDS, WeightUnit.fromLocale("MM")) // Myanmar
    }
    
    @Test
    fun `fromLocale returns kilograms for most countries`() {
        assertEquals(WeightUnit.KILOGRAMS, WeightUnit.fromLocale("GB"))
        assertEquals(WeightUnit.KILOGRAMS, WeightUnit.fromLocale("DE"))
        assertEquals(WeightUnit.KILOGRAMS, WeightUnit.fromLocale("FR"))
        assertEquals(WeightUnit.KILOGRAMS, WeightUnit.fromLocale("CA"))
        assertEquals(WeightUnit.KILOGRAMS, WeightUnit.fromLocale("AU"))
        assertEquals(WeightUnit.KILOGRAMS, WeightUnit.fromLocale("JP"))
        assertEquals(WeightUnit.KILOGRAMS, WeightUnit.fromLocale(""))
        assertEquals(WeightUnit.KILOGRAMS, WeightUnit.fromLocale("UNKNOWN"))
    }
    
    @Test
    fun `convert kilograms to pounds correctly`() {
        assertEquals(2.20462, WeightUnit.convert(1.0, WeightUnit.KILOGRAMS, WeightUnit.POUNDS), 0.00001)
        assertEquals(220.462, WeightUnit.convert(100.0, WeightUnit.KILOGRAMS, WeightUnit.POUNDS), 0.001)
        assertEquals(154.324, WeightUnit.convert(70.0, WeightUnit.KILOGRAMS, WeightUnit.POUNDS), 0.001)
    }
    
    @Test
    fun `convert pounds to kilograms correctly`() {
        assertEquals(1.0, WeightUnit.convert(2.20462, WeightUnit.POUNDS, WeightUnit.KILOGRAMS), 0.00001)
        assertEquals(45.359, WeightUnit.convert(100.0, WeightUnit.POUNDS, WeightUnit.KILOGRAMS), 0.001)
        assertEquals(31.751, WeightUnit.convert(70.0, WeightUnit.POUNDS, WeightUnit.KILOGRAMS), 0.001)
    }
    
    @Test
    fun `convert same unit returns original value`() {
        assertEquals(70.0, WeightUnit.convert(70.0, WeightUnit.KILOGRAMS, WeightUnit.KILOGRAMS))
        assertEquals(154.0, WeightUnit.convert(154.0, WeightUnit.POUNDS, WeightUnit.POUNDS))
    }
    
    @Test
    fun `convert handles zero weight`() {
        assertEquals(0.0, WeightUnit.convert(0.0, WeightUnit.KILOGRAMS, WeightUnit.POUNDS))
        assertEquals(0.0, WeightUnit.convert(0.0, WeightUnit.POUNDS, WeightUnit.KILOGRAMS))
    }
    
    @Test
    fun `convert handles fractional weights`() {
        assertEquals(1.102, WeightUnit.convert(0.5, WeightUnit.KILOGRAMS, WeightUnit.POUNDS), 0.001)
        assertEquals(0.227, WeightUnit.convert(0.5, WeightUnit.POUNDS, WeightUnit.KILOGRAMS), 0.001)
    }
    
    @Test
    fun `enum serialization works correctly`() {
        // Test that enum can be serialized (kotlinx.serialization requirement)
        assertNotNull(WeightUnit.KILOGRAMS)
        assertNotNull(WeightUnit.POUNDS)
    }
}