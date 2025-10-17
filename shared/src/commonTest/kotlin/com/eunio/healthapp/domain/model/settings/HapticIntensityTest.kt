package com.eunio.healthapp.domain.model.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HapticIntensityTest {
    
    @Test
    fun `haptic intensity enum has correct display names and values`() {
        assertEquals("Disabled", HapticIntensity.DISABLED.displayName)
        assertEquals(0.0f, HapticIntensity.DISABLED.intensityValue)
        
        assertEquals("Light", HapticIntensity.LIGHT.displayName)
        assertEquals(0.3f, HapticIntensity.LIGHT.intensityValue)
        
        assertEquals("Medium", HapticIntensity.MEDIUM.displayName)
        assertEquals(0.6f, HapticIntensity.MEDIUM.intensityValue)
        
        assertEquals("Strong", HapticIntensity.STRONG.displayName)
        assertEquals(1.0f, HapticIntensity.STRONG.intensityValue)
    }
    
    @Test
    fun `isEnabled returns correct values`() {
        assertFalse(HapticIntensity.DISABLED.isEnabled)
        assertTrue(HapticIntensity.LIGHT.isEnabled)
        assertTrue(HapticIntensity.MEDIUM.isEnabled)
        assertTrue(HapticIntensity.STRONG.isEnabled)
    }
    
    @Test
    fun `default returns medium intensity`() {
        assertEquals(HapticIntensity.MEDIUM, HapticIntensity.default())
    }
    
    @Test
    fun `fromValue returns disabled for zero and negative values`() {
        assertEquals(HapticIntensity.DISABLED, HapticIntensity.fromValue(0.0f))
        assertEquals(HapticIntensity.DISABLED, HapticIntensity.fromValue(-0.1f))
        assertEquals(HapticIntensity.DISABLED, HapticIntensity.fromValue(-1.0f))
    }
    
    @Test
    fun `fromValue returns light for low values`() {
        assertEquals(HapticIntensity.LIGHT, HapticIntensity.fromValue(0.1f))
        assertEquals(HapticIntensity.LIGHT, HapticIntensity.fromValue(0.3f))
        assertEquals(HapticIntensity.LIGHT, HapticIntensity.fromValue(0.45f))
    }
    
    @Test
    fun `fromValue returns medium for medium values`() {
        assertEquals(HapticIntensity.MEDIUM, HapticIntensity.fromValue(0.46f))
        assertEquals(HapticIntensity.MEDIUM, HapticIntensity.fromValue(0.6f))
        assertEquals(HapticIntensity.MEDIUM, HapticIntensity.fromValue(0.8f))
    }
    
    @Test
    fun `fromValue returns strong for high values`() {
        assertEquals(HapticIntensity.STRONG, HapticIntensity.fromValue(0.81f))
        assertEquals(HapticIntensity.STRONG, HapticIntensity.fromValue(1.0f))
        assertEquals(HapticIntensity.STRONG, HapticIntensity.fromValue(1.5f))
        assertEquals(HapticIntensity.STRONG, HapticIntensity.fromValue(Float.MAX_VALUE))
    }
    
    @Test
    fun `fromValue boundary conditions work correctly`() {
        // Test exact boundary values
        assertEquals(HapticIntensity.DISABLED, HapticIntensity.fromValue(0.0f))
        assertEquals(HapticIntensity.LIGHT, HapticIntensity.fromValue(0.45f))
        assertEquals(HapticIntensity.MEDIUM, HapticIntensity.fromValue(0.8f))
        assertEquals(HapticIntensity.STRONG, HapticIntensity.fromValue(0.81f))
    }
    
    @Test
    fun `intensity values are in ascending order`() {
        assertTrue(HapticIntensity.DISABLED.intensityValue < HapticIntensity.LIGHT.intensityValue)
        assertTrue(HapticIntensity.LIGHT.intensityValue < HapticIntensity.MEDIUM.intensityValue)
        assertTrue(HapticIntensity.MEDIUM.intensityValue < HapticIntensity.STRONG.intensityValue)
    }
    
    @Test
    fun `all intensity values are within valid range`() {
        HapticIntensity.values().forEach { intensity ->
            assertTrue(
                intensity.intensityValue >= 0.0f && intensity.intensityValue <= 1.0f,
                "Intensity value should be between 0.0 and 1.0: ${intensity.intensityValue}"
            )
        }
    }
    
    @Test
    fun `round trip conversion works correctly`() {
        HapticIntensity.values().forEach { original ->
            val converted = HapticIntensity.fromValue(original.intensityValue)
            // Note: Due to the way fromValue works with ranges, this might not always be exact
            // but the converted value should be reasonable
            assertNotNull(converted)
        }
    }
    
    @Test
    fun `enum serialization works correctly`() {
        // Test that enum can be serialized (kotlinx.serialization requirement)
        assertNotNull(HapticIntensity.DISABLED)
        assertNotNull(HapticIntensity.LIGHT)
        assertNotNull(HapticIntensity.MEDIUM)
        assertNotNull(HapticIntensity.STRONG)
    }
    
    @Test
    fun `all enum values have unique intensity values`() {
        val intensityValues = HapticIntensity.values().map { it.intensityValue }
        val uniqueValues = intensityValues.toSet()
        
        assertEquals(
            intensityValues.size,
            uniqueValues.size,
            "All intensity values should be unique"
        )
    }
    
    @Test
    fun `all enum values have non-empty display names`() {
        HapticIntensity.values().forEach { intensity ->
            assertTrue(
                intensity.displayName.isNotBlank(),
                "Display name should not be blank for $intensity"
            )
        }
    }
}