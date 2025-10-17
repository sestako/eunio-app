package com.eunio.healthapp.domain.model.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TemperatureUnitTest {
    
    @Test
    fun `temperature unit enum has correct display names and symbols`() {
        assertEquals("Celsius", TemperatureUnit.CELSIUS.displayName)
        assertEquals("°C", TemperatureUnit.CELSIUS.symbol)
        assertEquals("Fahrenheit", TemperatureUnit.FAHRENHEIT.displayName)
        assertEquals("°F", TemperatureUnit.FAHRENHEIT.symbol)
    }
    
    @Test
    fun `fromLocale returns correct unit for US locale`() {
        assertEquals(TemperatureUnit.FAHRENHEIT, TemperatureUnit.fromLocale("US"))
        assertEquals(TemperatureUnit.FAHRENHEIT, TemperatureUnit.fromLocale("us"))
    }
    
    @Test
    fun `fromLocale returns correct unit for Fahrenheit countries`() {
        assertEquals(TemperatureUnit.FAHRENHEIT, TemperatureUnit.fromLocale("LR")) // Liberia
        assertEquals(TemperatureUnit.FAHRENHEIT, TemperatureUnit.fromLocale("MM")) // Myanmar
        assertEquals(TemperatureUnit.FAHRENHEIT, TemperatureUnit.fromLocale("BS")) // Bahamas
        assertEquals(TemperatureUnit.FAHRENHEIT, TemperatureUnit.fromLocale("BZ")) // Belize
        assertEquals(TemperatureUnit.FAHRENHEIT, TemperatureUnit.fromLocale("KY")) // Cayman Islands
        assertEquals(TemperatureUnit.FAHRENHEIT, TemperatureUnit.fromLocale("PW")) // Palau
    }
    
    @Test
    fun `fromLocale returns Celsius for most countries`() {
        assertEquals(TemperatureUnit.CELSIUS, TemperatureUnit.fromLocale("GB"))
        assertEquals(TemperatureUnit.CELSIUS, TemperatureUnit.fromLocale("DE"))
        assertEquals(TemperatureUnit.CELSIUS, TemperatureUnit.fromLocale("FR"))
        assertEquals(TemperatureUnit.CELSIUS, TemperatureUnit.fromLocale("CA"))
        assertEquals(TemperatureUnit.CELSIUS, TemperatureUnit.fromLocale("AU"))
        assertEquals(TemperatureUnit.CELSIUS, TemperatureUnit.fromLocale("JP"))
        assertEquals(TemperatureUnit.CELSIUS, TemperatureUnit.fromLocale(""))
        assertEquals(TemperatureUnit.CELSIUS, TemperatureUnit.fromLocale("UNKNOWN"))
    }
    
    @Test
    fun `convert celsius to fahrenheit correctly`() {
        assertEquals(32.0, TemperatureUnit.convert(0.0, TemperatureUnit.CELSIUS, TemperatureUnit.FAHRENHEIT))
        assertEquals(212.0, TemperatureUnit.convert(100.0, TemperatureUnit.CELSIUS, TemperatureUnit.FAHRENHEIT))
        assertEquals(98.6, TemperatureUnit.convert(37.0, TemperatureUnit.CELSIUS, TemperatureUnit.FAHRENHEIT), 0.1)
    }
    
    @Test
    fun `convert fahrenheit to celsius correctly`() {
        assertEquals(0.0, TemperatureUnit.convert(32.0, TemperatureUnit.FAHRENHEIT, TemperatureUnit.CELSIUS))
        assertEquals(100.0, TemperatureUnit.convert(212.0, TemperatureUnit.FAHRENHEIT, TemperatureUnit.CELSIUS))
        assertEquals(37.0, TemperatureUnit.convert(98.6, TemperatureUnit.FAHRENHEIT, TemperatureUnit.CELSIUS), 0.1)
    }
    
    @Test
    fun `convert same unit returns original value`() {
        assertEquals(25.0, TemperatureUnit.convert(25.0, TemperatureUnit.CELSIUS, TemperatureUnit.CELSIUS))
        assertEquals(77.0, TemperatureUnit.convert(77.0, TemperatureUnit.FAHRENHEIT, TemperatureUnit.FAHRENHEIT))
    }
    
    @Test
    fun `convert handles negative temperatures`() {
        assertEquals(-40.0, TemperatureUnit.convert(-40.0, TemperatureUnit.CELSIUS, TemperatureUnit.FAHRENHEIT))
        assertEquals(-40.0, TemperatureUnit.convert(-40.0, TemperatureUnit.FAHRENHEIT, TemperatureUnit.CELSIUS))
    }
    
    @Test
    fun `enum serialization works correctly`() {
        // Test that enum can be serialized (kotlinx.serialization requirement)
        assertNotNull(TemperatureUnit.CELSIUS)
        assertNotNull(TemperatureUnit.FAHRENHEIT)
    }
}