package com.eunio.healthapp.data.local.util

import com.eunio.healthapp.domain.model.settings.*
import kotlinx.datetime.LocalTime
import kotlin.test.*

/**
 * Unit tests for JsonSerializer utility.
 * Tests JSON serialization and deserialization functionality.
 */
class JsonSerializerTest {
    
    @Test
    fun `toJson should serialize simple objects correctly`() {
        // Given
        val unitPrefs = UnitPreferences(
            temperatureUnit = TemperatureUnit.FAHRENHEIT,
            weightUnit = WeightUnit.POUNDS,
            isManuallySet = true
        )
        
        // When
        val json = JsonSerializer.toJson(unitPrefs)
        
        // Then
        assertNotNull(json)
        assertTrue(json.contains("FAHRENHEIT"))
        assertTrue(json.contains("POUNDS"))
        assertTrue(json.contains("true"))
    }
    
    @Test
    fun `fromJson should deserialize simple objects correctly`() {
        // Given
        val originalPrefs = UnitPreferences(
            temperatureUnit = TemperatureUnit.FAHRENHEIT,
            weightUnit = WeightUnit.POUNDS,
            isManuallySet = true
        )
        val json = JsonSerializer.toJson(originalPrefs)
        
        // When
        val deserializedPrefs = JsonSerializer.fromJson<UnitPreferences>(json)
        
        // Then
        assertEquals(originalPrefs, deserializedPrefs)
    }
    
    @Test
    fun `toJson should serialize complex objects with nested structures`() {
        // Given
        val notificationPrefs = NotificationPreferences(
            dailyLoggingReminder = NotificationSetting(
                enabled = true,
                time = LocalTime(20, 30),
                daysInAdvance = 1
            ),
            periodPredictionAlert = NotificationSetting(
                enabled = false,
                time = LocalTime(9, 0),
                daysInAdvance = 2
            ),
            globalNotificationsEnabled = true
        )
        
        // When
        val json = JsonSerializer.toJson(notificationPrefs)
        
        // Then
        assertNotNull(json)
        assertTrue(json.contains("dailyLoggingReminder"))
        assertTrue(json.contains("20:30"))
        assertTrue(json.contains("periodPredictionAlert"))
        assertTrue(json.contains("09:00"))
    }
    
    @Test
    fun `fromJson should deserialize complex objects with nested structures`() {
        // Given
        val originalPrefs = NotificationPreferences(
            dailyLoggingReminder = NotificationSetting(
                enabled = true,
                time = LocalTime(20, 30),
                daysInAdvance = 1
            ),
            periodPredictionAlert = NotificationSetting(
                enabled = false,
                time = LocalTime(9, 0),
                daysInAdvance = 2
            ),
            globalNotificationsEnabled = true
        )
        val json = JsonSerializer.toJson(originalPrefs)
        
        // When
        val deserializedPrefs = JsonSerializer.fromJson<NotificationPreferences>(json)
        
        // Then
        assertEquals(originalPrefs, deserializedPrefs)
        assertEquals(LocalTime(20, 30), deserializedPrefs.dailyLoggingReminder.time)
        assertEquals(LocalTime(9, 0), deserializedPrefs.periodPredictionAlert.time)
    }
    
    @Test
    fun `toJson should serialize enums correctly`() {
        // Given
        val displayPrefs = DisplayPreferences(
            textSizeScale = 1.5f,
            highContrastMode = true,
            hapticFeedbackEnabled = false,
            hapticIntensity = HapticIntensity.STRONG
        )
        
        // When
        val json = JsonSerializer.toJson(displayPrefs)
        
        // Then
        assertNotNull(json)
        assertTrue(json.contains("STRONG"))
        assertTrue(json.contains("1.5"))
    }
    
    @Test
    fun `fromJson should deserialize enums correctly`() {
        // Given
        val originalPrefs = DisplayPreferences(
            textSizeScale = 1.5f,
            highContrastMode = true,
            hapticFeedbackEnabled = false,
            hapticIntensity = HapticIntensity.STRONG
        )
        val json = JsonSerializer.toJson(originalPrefs)
        
        // When
        val deserializedPrefs = JsonSerializer.fromJson<DisplayPreferences>(json)
        
        // Then
        assertEquals(originalPrefs, deserializedPrefs)
        assertEquals(HapticIntensity.STRONG, deserializedPrefs.hapticIntensity)
    }
    
    @Test
    fun `fromJsonOrNull should return null for invalid JSON`() {
        // Given
        val invalidJson = "{ invalid json }"
        
        // When
        val result = JsonSerializer.fromJsonOrNull<UnitPreferences>(invalidJson)
        
        // Then
        assertNull(result)
    }
    
    @Test
    fun `fromJsonOrNull should return null for null input`() {
        // When
        val result = JsonSerializer.fromJsonOrNull<UnitPreferences>(null)
        
        // Then
        assertNull(result)
    }
    
    @Test
    fun `fromJsonOrNull should return null for blank input`() {
        // When
        val result = JsonSerializer.fromJsonOrNull<UnitPreferences>("")
        
        // Then
        assertNull(result)
    }
    
    @Test
    fun `fromJsonOrNull should return object for valid JSON`() {
        // Given
        val originalPrefs = UnitPreferences.default()
        val json = JsonSerializer.toJson(originalPrefs)
        
        // When
        val result = JsonSerializer.fromJsonOrNull<UnitPreferences>(json)
        
        // Then
        assertNotNull(result)
        assertEquals(originalPrefs, result)
    }
    
    @Test
    fun `toJsonOrNull should return null for null input`() {
        // When
        val result = JsonSerializer.toJsonOrNull<UnitPreferences>(null)
        
        // Then
        assertNull(result)
    }
    
    @Test
    fun `toJsonOrNull should return JSON for valid object`() {
        // Given
        val prefs = UnitPreferences.default()
        
        // When
        val result = JsonSerializer.toJsonOrNull(prefs)
        
        // Then
        assertNotNull(result)
        assertTrue(result.contains("CELSIUS"))
    }
    
    @Test
    fun `isValidJson should return true for valid JSON`() {
        // Given
        val validJson = """{"temperatureUnit":"CELSIUS","weightUnit":"KILOGRAMS","isManuallySet":false}"""
        
        // When
        val isValid = JsonSerializer.isValidJson(validJson)
        
        // Then
        assertTrue(isValid)
    }
    
    @Test
    fun `isValidJson should return false for invalid JSON`() {
        // Given
        val invalidJson = "{ invalid json }"
        
        // When
        val isValid = JsonSerializer.isValidJson(invalidJson)
        
        // Then
        assertFalse(isValid)
    }
    
    @Test
    fun `isValidJson should return false for null input`() {
        // When
        val isValid = JsonSerializer.isValidJson(null)
        
        // Then
        assertFalse(isValid)
    }
    
    @Test
    fun `isValidJson should return false for blank input`() {
        // When
        val isValid = JsonSerializer.isValidJson("")
        
        // Then
        assertFalse(isValid)
    }
    
    @Test
    fun `calculateJsonSize should return correct size`() {
        // Given
        val json = """{"test":"value"}"""
        
        // When
        val size = JsonSerializer.calculateJsonSize(json)
        
        // Then
        assertEquals(json.toByteArray(Charsets.UTF_8).size, size)
    }
    
    @Test
    fun `calculateJsonSize should return 0 for null input`() {
        // When
        val size = JsonSerializer.calculateJsonSize(null)
        
        // Then
        assertEquals(0, size)
    }
    
    @Test
    fun `serialization should handle default values correctly`() {
        // Given
        val defaultPrefs = UnitPreferences.default()
        
        // When
        val json = JsonSerializer.toJson(defaultPrefs)
        val deserializedPrefs = JsonSerializer.fromJson<UnitPreferences>(json)
        
        // Then
        assertEquals(defaultPrefs, deserializedPrefs)
        assertEquals(TemperatureUnit.CELSIUS, deserializedPrefs.temperatureUnit)
        assertEquals(WeightUnit.KILOGRAMS, deserializedPrefs.weightUnit)
        assertFalse(deserializedPrefs.isManuallySet)
    }
    
    @Test
    fun `serialization should preserve all data types`() {
        // Given
        val cyclePrefs = CyclePreferences(
            averageCycleLength = 28,
            averageLutealPhaseLength = 14,
            periodDuration = 5,
            isCustomized = true
        )
        
        // When
        val json = JsonSerializer.toJson(cyclePrefs)
        val deserializedPrefs = JsonSerializer.fromJson<CyclePreferences>(json)
        
        // Then
        assertEquals(cyclePrefs, deserializedPrefs)
        assertEquals(28, deserializedPrefs.averageCycleLength)
        assertEquals(14, deserializedPrefs.averageLutealPhaseLength)
        assertEquals(5, deserializedPrefs.periodDuration)
        assertTrue(deserializedPrefs.isCustomized)
    }
    
    @Test
    fun `serialization should handle nullable fields correctly`() {
        // Given
        val notificationSetting = NotificationSetting(
            enabled = true,
            time = null, // nullable field
            daysInAdvance = 1
        )
        
        // When
        val json = JsonSerializer.toJson(notificationSetting)
        val deserializedSetting = JsonSerializer.fromJson<NotificationSetting>(json)
        
        // Then
        assertEquals(notificationSetting, deserializedSetting)
        assertTrue(deserializedSetting.enabled)
        assertNull(deserializedSetting.time)
        assertEquals(1, deserializedSetting.daysInAdvance)
    }
}