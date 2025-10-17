package com.eunio.healthapp.data.local

import com.eunio.healthapp.data.local.util.JsonSerializer
import com.eunio.healthapp.domain.model.settings.*
import kotlinx.datetime.Clock
import kotlin.test.*

/**
 * Simple integration test for settings functionality.
 * Tests JSON serialization and basic data model functionality.
 */
class SettingsIntegrationTest {
    
    @Test
    fun `UserSettings should serialize and deserialize correctly`() {
        // Given
        val originalSettings = UserSettings(
            userId = "test-user",
            unitPreferences = UnitPreferences(
                temperatureUnit = TemperatureUnit.FAHRENHEIT,
                weightUnit = WeightUnit.POUNDS,
                isManuallySet = true
            ),
            notificationPreferences = NotificationPreferences(
                dailyLoggingReminder = NotificationSetting(
                    enabled = true,
                    time = kotlinx.datetime.LocalTime(20, 30),
                    daysInAdvance = 1
                ),
                globalNotificationsEnabled = true
            ),
            cyclePreferences = CyclePreferences(
                averageCycleLength = 30,
                averageLutealPhaseLength = 12,
                periodDuration = 6,
                isCustomized = true
            ),
            displayPreferences = DisplayPreferences(
                textSizeScale = 1.2f,
                highContrastMode = true,
                hapticFeedbackEnabled = false,
                hapticIntensity = HapticIntensity.DISABLED
            ),
            lastModified = Clock.System.now()
        )
        
        // When
        val json = JsonSerializer.toJson(originalSettings)
        val deserializedSettings = JsonSerializer.fromJson<UserSettings>(json)
        
        // Then
        assertEquals(originalSettings.userId, deserializedSettings.userId)
        assertEquals(originalSettings.unitPreferences, deserializedSettings.unitPreferences)
        assertEquals(originalSettings.notificationPreferences, deserializedSettings.notificationPreferences)
        assertEquals(originalSettings.cyclePreferences, deserializedSettings.cyclePreferences)
        assertEquals(originalSettings.displayPreferences, deserializedSettings.displayPreferences)
        
        // Verify specific values
        assertEquals(TemperatureUnit.FAHRENHEIT, deserializedSettings.unitPreferences.temperatureUnit)
        assertEquals(WeightUnit.POUNDS, deserializedSettings.unitPreferences.weightUnit)
        assertTrue(deserializedSettings.unitPreferences.isManuallySet)
        
        assertTrue(deserializedSettings.notificationPreferences.dailyLoggingReminder.enabled)
        assertEquals(kotlinx.datetime.LocalTime(20, 30), deserializedSettings.notificationPreferences.dailyLoggingReminder.time)
        
        assertEquals(30, deserializedSettings.cyclePreferences.averageCycleLength)
        assertTrue(deserializedSettings.cyclePreferences.isCustomized)
        
        assertEquals(1.2f, deserializedSettings.displayPreferences.textSizeScale)
        assertTrue(deserializedSettings.displayPreferences.highContrastMode)
        assertEquals(HapticIntensity.DISABLED, deserializedSettings.displayPreferences.hapticIntensity)
    }
    
    @Test
    fun `UserSettings validation should work correctly`() {
        // Given - valid settings
        val validSettings = UserSettings.createDefault("test-user")
        
        // When
        val isValid = validSettings.isValid()
        
        // Then
        assertTrue(isValid)
        assertTrue(validSettings.getValidationErrors().isEmpty())
    }
    
    @Test
    fun `UserSettings should handle customizations correctly`() {
        // Given
        val defaultSettings = UserSettings.createDefault("test-user")
        val customizedSettings = defaultSettings.copy(
            unitPreferences = defaultSettings.unitPreferences.copy(isManuallySet = true),
            cyclePreferences = defaultSettings.cyclePreferences.copy(isCustomized = true)
        )
        
        // When
        val defaultHasCustomizations = defaultSettings.hasCustomizations()
        val customizedHasCustomizations = customizedSettings.hasCustomizations()
        
        // Then
        assertFalse(defaultHasCustomizations)
        assertTrue(customizedHasCustomizations)
    }
    
    @Test
    fun `UserSettings should handle sync status correctly`() {
        // Given
        val settings = UserSettings.createDefault("test-user")
        
        // When
        val needsSyncBefore = settings.needsSync()
        val syncedSettings = settings.markAsSynced()
        val needsSyncAfter = syncedSettings.needsSync()
        
        // Then
        assertTrue(needsSyncBefore)
        assertFalse(needsSyncAfter)
        assertEquals(com.eunio.healthapp.domain.model.SyncStatus.SYNCED, syncedSettings.syncStatus)
    }
    
    @Test
    fun `all settings models should have working default factories`() {
        // When
        val unitPrefs = UnitPreferences.default()
        val notificationPrefs = NotificationPreferences.default()
        val cyclePrefs = CyclePreferences.default()
        val privacyPrefs = PrivacyPreferences.default()
        val displayPrefs = DisplayPreferences.default()
        val syncPrefs = SyncPreferences.default()
        
        // Then
        assertNotNull(unitPrefs)
        assertNotNull(notificationPrefs)
        assertNotNull(cyclePrefs)
        assertNotNull(privacyPrefs)
        assertNotNull(displayPrefs)
        assertNotNull(syncPrefs)
        
        // Verify default values
        assertEquals(TemperatureUnit.CELSIUS, unitPrefs.temperatureUnit)
        assertEquals(WeightUnit.KILOGRAMS, unitPrefs.weightUnit)
        assertFalse(unitPrefs.isManuallySet)
        
        assertTrue(notificationPrefs.globalNotificationsEnabled)
        assertEquals(28, cyclePrefs.averageCycleLength)
        assertEquals(1.0f, displayPrefs.textSizeScale)
        assertTrue(syncPrefs.autoSyncEnabled)
    }
    
    @Test
    fun `settings should handle locale-based defaults correctly`() {
        // When
        val usSettings = UserSettings.createDefault("test-user", "en_US")
        val ukSettings = UserSettings.createDefault("test-user", "en_GB")
        val deSettings = UserSettings.createDefault("test-user", "de_DE")
        
        // Then - all should be created successfully
        assertNotNull(usSettings)
        assertNotNull(ukSettings)
        assertNotNull(deSettings)
        
        // Verify they all have the same user ID
        assertEquals("test-user", usSettings.userId)
        assertEquals("test-user", ukSettings.userId)
        assertEquals("test-user", deSettings.userId)
    }
}