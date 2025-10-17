package com.eunio.healthapp.testutil

import com.eunio.healthapp.domain.model.settings.*
import com.eunio.healthapp.domain.model.UnitSystem
import com.eunio.healthapp.domain.model.SyncStatus
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Test to verify that TestDataBuilder creates valid models with correct constructor parameters
 */
class TestDataBuilderTest {
    
    @Test
    fun `createDisplayPreferences creates valid DisplayPreferences with correct parameters`() {
        // Test default creation
        val defaultPrefs = TestDataBuilder.createDisplayPreferences()
        
        assertNotNull(defaultPrefs)
        assertEquals(1.0f, defaultPrefs.textSizeScale)
        assertEquals(false, defaultPrefs.highContrastMode)
        assertEquals(true, defaultPrefs.hapticFeedbackEnabled)
        assertEquals(HapticIntensity.MEDIUM, defaultPrefs.hapticIntensity)
        assertTrue(defaultPrefs.isValid())
    }
    
    @Test
    fun `createDisplayPreferences creates valid DisplayPreferences with custom parameters`() {
        // Test custom creation
        val customPrefs = TestDataBuilder.createDisplayPreferences(
            textSizeScale = 1.5f,
            highContrastMode = true,
            hapticFeedbackEnabled = false,
            hapticIntensity = HapticIntensity.DISABLED
        )
        
        assertNotNull(customPrefs)
        assertEquals(1.5f, customPrefs.textSizeScale)
        assertEquals(true, customPrefs.highContrastMode)
        assertEquals(false, customPrefs.hapticFeedbackEnabled)
        assertEquals(HapticIntensity.DISABLED, customPrefs.hapticIntensity)
        assertTrue(customPrefs.isValid())
    }
    
    @Test
    fun `createUserSettings creates valid UserSettings with correct parameters`() {
        // Test default creation
        val defaultSettings = TestDataBuilder.createUserSettings()
        
        assertNotNull(defaultSettings)
        assertEquals("test-user", defaultSettings.userId)
        assertNotNull(defaultSettings.unitPreferences)
        assertNotNull(defaultSettings.notificationPreferences)
        assertNotNull(defaultSettings.cyclePreferences)
        assertNotNull(defaultSettings.privacyPreferences)
        assertNotNull(defaultSettings.displayPreferences)
        assertNotNull(defaultSettings.syncPreferences)
        assertNotNull(defaultSettings.lastModified)
        assertTrue(defaultSettings.isValid())
    }
    
    @Test
    fun `createUserSettings creates valid UserSettings with custom parameters`() {
        // Test custom creation
        val customDisplayPrefs = TestDataBuilder.createDisplayPreferences(
            textSizeScale = 1.2f,
            highContrastMode = true
        )
        
        val customSettings = TestDataBuilder.createUserSettings(
            userId = "custom-user",
            displayPreferences = customDisplayPrefs
        )
        
        assertNotNull(customSettings)
        assertEquals("custom-user", customSettings.userId)
        assertEquals(1.2f, customSettings.displayPreferences.textSizeScale)
        assertEquals(true, customSettings.displayPreferences.highContrastMode)
        assertTrue(customSettings.isValid())
    }
    
    @Test
    fun `createAccessibilityDisplayPreferences creates valid accessibility-focused preferences`() {
        val accessibilityPrefs = TestDataBuilder.createAccessibilityDisplayPreferences()
        
        assertNotNull(accessibilityPrefs)
        assertEquals(1.4f, accessibilityPrefs.textSizeScale)
        assertEquals(true, accessibilityPrefs.highContrastMode)
        assertEquals(true, accessibilityPrefs.hapticFeedbackEnabled)
        assertEquals(HapticIntensity.STRONG, accessibilityPrefs.hapticIntensity)
        assertTrue(accessibilityPrefs.isValid())
        assertTrue(accessibilityPrefs.hasAccessibilityFeaturesEnabled())
    }
    
    @Test
    fun `createMinimalDisplayPreferences creates valid minimal preferences`() {
        val minimalPrefs = TestDataBuilder.createMinimalDisplayPreferences()
        
        assertNotNull(minimalPrefs)
        assertEquals(1.0f, minimalPrefs.textSizeScale)
        assertEquals(false, minimalPrefs.highContrastMode)
        assertEquals(false, minimalPrefs.hapticFeedbackEnabled)
        assertEquals(HapticIntensity.DISABLED, minimalPrefs.hapticIntensity)
        assertTrue(minimalPrefs.isValid())
    }
    
    @Test
    fun `createFullyCustomizedUserSettings creates valid customized settings`() {
        val customizedSettings = TestDataBuilder.createFullyCustomizedUserSettings("custom-user")
        
        assertNotNull(customizedSettings)
        assertEquals("custom-user", customizedSettings.userId)
        assertEquals(TemperatureUnit.FAHRENHEIT, customizedSettings.unitPreferences.temperatureUnit)
        assertEquals(WeightUnit.POUNDS, customizedSettings.unitPreferences.weightUnit)
        assertTrue(customizedSettings.unitPreferences.isManuallySet)
        assertTrue(customizedSettings.notificationPreferences.globalNotificationsEnabled)
        assertTrue(customizedSettings.cyclePreferences.isCustomized)
        assertTrue(customizedSettings.displayPreferences.hasAccessibilityFeaturesEnabled())
        assertTrue(customizedSettings.isValid())
    }
    
    @Test
    fun `createInvalidUserSettings creates invalid settings for testing validation`() {
        val invalidSettings = TestDataBuilder.createInvalidUserSettings()
        
        assertNotNull(invalidSettings)
        assertEquals("", invalidSettings.userId) // Invalid: blank user ID
        assertEquals(-1.0f, invalidSettings.displayPreferences.textSizeScale) // Invalid: below minimum
        assertEquals(false, invalidSettings.displayPreferences.hapticFeedbackEnabled)
        assertEquals(HapticIntensity.MEDIUM, invalidSettings.displayPreferences.hapticIntensity) // Invalid: should be DISABLED
        
        // Should be invalid due to blank user ID and invalid display preferences
        assertTrue(!invalidSettings.isValid())
        
        val errors = invalidSettings.getValidationErrors()
        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it.contains("User ID cannot be blank") })
    }
    
    @Test
    fun `createUserPreferences creates valid UserPreferences with current constructor parameters`() {
        // Test default creation
        val defaultPrefs = TestDataBuilder.createUserPreferences()
        
        assertNotNull(defaultPrefs)
        assertEquals("test-user", defaultPrefs.userId)
        assertEquals(UnitSystem.METRIC, defaultPrefs.unitSystem)
        assertEquals(false, defaultPrefs.isManuallySet)
        assertEquals(SyncStatus.PENDING, defaultPrefs.syncStatus)
        assertNotNull(defaultPrefs.lastModified)
    }
    
    @Test
    fun `createUserPreferences creates valid UserPreferences with custom parameters`() {
        // Test custom creation
        val customPrefs = TestDataBuilder.createUserPreferences(
            userId = "custom-user",
            unitSystem = UnitSystem.IMPERIAL,
            isManuallySet = true,
            syncStatus = SyncStatus.SYNCED
        )
        
        assertNotNull(customPrefs)
        assertEquals("custom-user", customPrefs.userId)
        assertEquals(UnitSystem.IMPERIAL, customPrefs.unitSystem)
        assertEquals(true, customPrefs.isManuallySet)
        assertEquals(SyncStatus.SYNCED, customPrefs.syncStatus)
        assertNotNull(customPrefs.lastModified)
    }
    
    @Test
    fun `createNotificationSettings creates valid NotificationPreferences with enum-based configuration`() {
        // Test default creation
        val defaultNotifications = TestDataBuilder.createNotificationSettings()
        
        assertNotNull(defaultNotifications)
        assertTrue(defaultNotifications.globalNotificationsEnabled)
        assertTrue(defaultNotifications.dailyLoggingReminder.enabled)
        assertTrue(defaultNotifications.periodPredictionAlert.enabled)
        assertFalse(defaultNotifications.ovulationAlert.enabled)
        assertFalse(defaultNotifications.insightNotifications.enabled)
        assertTrue(defaultNotifications.isValid())
        assertTrue(defaultNotifications.hasEnabledNotifications())
    }
    
    @Test
    fun `createNotificationSettings creates valid NotificationPreferences with custom configuration`() {
        // Test custom creation
        val customNotifications = TestDataBuilder.createNotificationSettings(
            dailyLoggingEnabled = false,
            periodPredictionEnabled = false,
            ovulationEnabled = true,
            ovulationTime = LocalTime(8, 30),
            insightNotificationsEnabled = true,
            globalNotificationsEnabled = true
        )
        
        assertNotNull(customNotifications)
        assertTrue(customNotifications.globalNotificationsEnabled)
        assertFalse(customNotifications.dailyLoggingReminder.enabled)
        assertFalse(customNotifications.periodPredictionAlert.enabled)
        assertTrue(customNotifications.ovulationAlert.enabled)
        assertEquals(LocalTime(8, 30), customNotifications.ovulationAlert.time)
        assertTrue(customNotifications.insightNotifications.enabled)
        assertTrue(customNotifications.isValid())
        assertTrue(customNotifications.hasEnabledNotifications())
    }
    
    @Test
    fun `createAllNotificationsEnabled creates NotificationPreferences with all notifications enabled`() {
        val allEnabled = TestDataBuilder.createAllNotificationsEnabled()
        
        assertNotNull(allEnabled)
        assertTrue(allEnabled.globalNotificationsEnabled)
        assertTrue(allEnabled.dailyLoggingReminder.enabled)
        assertTrue(allEnabled.periodPredictionAlert.enabled)
        assertTrue(allEnabled.ovulationAlert.enabled)
        assertTrue(allEnabled.insightNotifications.enabled)
        assertTrue(allEnabled.isValid())
        assertTrue(allEnabled.hasEnabledNotifications())
    }
    
    @Test
    fun `createAllNotificationsDisabled creates NotificationPreferences with all notifications disabled`() {
        val allDisabled = TestDataBuilder.createAllNotificationsDisabled()
        
        assertNotNull(allDisabled)
        assertFalse(allDisabled.globalNotificationsEnabled)
        assertFalse(allDisabled.dailyLoggingReminder.enabled)
        assertFalse(allDisabled.periodPredictionAlert.enabled)
        assertFalse(allDisabled.ovulationAlert.enabled)
        assertFalse(allDisabled.insightNotifications.enabled)
        assertTrue(allDisabled.isValid())
        assertFalse(allDisabled.hasEnabledNotifications())
    }
    
    @Test
    fun `createEssentialNotificationsOnly creates NotificationPreferences with only essential notifications`() {
        val essentialOnly = TestDataBuilder.createEssentialNotificationsOnly()
        
        assertNotNull(essentialOnly)
        assertTrue(essentialOnly.globalNotificationsEnabled)
        assertTrue(essentialOnly.dailyLoggingReminder.enabled)
        assertTrue(essentialOnly.periodPredictionAlert.enabled)
        assertFalse(essentialOnly.ovulationAlert.enabled)
        assertFalse(essentialOnly.insightNotifications.enabled)
        assertTrue(essentialOnly.isValid())
        assertTrue(essentialOnly.hasEnabledNotifications())
    }
    
    @Test
    fun `createImperialUserPreferences creates UserPreferences with Imperial unit system`() {
        val imperialPrefs = TestDataBuilder.createImperialUserPreferences("imperial-test")
        
        assertNotNull(imperialPrefs)
        assertEquals("imperial-test", imperialPrefs.userId)
        assertEquals(UnitSystem.IMPERIAL, imperialPrefs.unitSystem)
        assertTrue(imperialPrefs.isManuallySet)
    }
    
    @Test
    fun `createMetricUserPreferences creates UserPreferences with Metric unit system`() {
        val metricPrefs = TestDataBuilder.createMetricUserPreferences("metric-test")
        
        assertNotNull(metricPrefs)
        assertEquals("metric-test", metricPrefs.userId)
        assertEquals(UnitSystem.METRIC, metricPrefs.unitSystem)
        assertTrue(metricPrefs.isManuallySet)
    }
    
    @Test
    fun `createSyncedUserPreferences creates UserPreferences with synced status`() {
        val syncedPrefs = TestDataBuilder.createSyncedUserPreferences("synced-test")
        
        assertNotNull(syncedPrefs)
        assertEquals("synced-test", syncedPrefs.userId)
        assertEquals(SyncStatus.SYNCED, syncedPrefs.syncStatus)
    }
    
    @Test
    fun `createFailedSyncUserPreferences creates UserPreferences with failed sync status`() {
        val failedPrefs = TestDataBuilder.createFailedSyncUserPreferences("failed-test")
        
        assertNotNull(failedPrefs)
        assertEquals("failed-test", failedPrefs.userId)
        assertEquals(SyncStatus.FAILED, failedPrefs.syncStatus)
    }
}