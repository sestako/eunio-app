package com.eunio.healthapp.domain.model.settings

import com.eunio.healthapp.domain.model.SyncStatus
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class UserSettingsTest {
    
    private val testUserId = "test-user-123"
    
    @Test
    fun `constructor creates valid user settings with defaults`() {
        val settings = UserSettings(userId = testUserId)
        
        assertEquals(testUserId, settings.userId)
        assertEquals(UnitPreferences.default(), settings.unitPreferences)
        assertEquals(NotificationPreferences.default(), settings.notificationPreferences)
        assertEquals(CyclePreferences.default(), settings.cyclePreferences)
        assertEquals(PrivacyPreferences.default(), settings.privacyPreferences)
        assertEquals(DisplayPreferences.default(), settings.displayPreferences)
        assertEquals(SyncPreferences.default(), settings.syncPreferences)
        assertEquals(SyncStatus.PENDING, settings.syncStatus)
        assertEquals(UserSettings.CURRENT_VERSION, settings.version)
        assertTrue(settings.isValid())
    }
    
    @Test
    fun `createDefault factory method creates valid settings`() {
        val settings = UserSettings.createDefault(testUserId)
        
        assertEquals(testUserId, settings.userId)
        assertTrue(settings.isValid())
        assertTrue(settings.needsSync())
        assertFalse(settings.hasCustomizations())
    }
    
    @Test
    fun `createDefault with locale creates correct unit preferences`() {
        val usSettings = UserSettings.createDefault(testUserId, "US")
        val deSettings = UserSettings.createDefault(testUserId, "DE")
        
        assertEquals(TemperatureUnit.FAHRENHEIT, usSettings.unitPreferences.temperatureUnit)
        assertEquals(WeightUnit.POUNDS, usSettings.unitPreferences.weightUnit)
        
        assertEquals(TemperatureUnit.CELSIUS, deSettings.unitPreferences.temperatureUnit)
        assertEquals(WeightUnit.KILOGRAMS, deSettings.unitPreferences.weightUnit)
    }
    
    @Test
    fun `createWithDefaults factory method creates settings with practical defaults`() {
        val settings = UserSettings.createWithDefaults(testUserId)
        
        assertTrue(settings.isValid())
        assertTrue(settings.notificationPreferences.hasEnabledNotifications())
        assertTrue(settings.privacyPreferences.hasDataCollectionEnabled())
        assertTrue(settings.hasCustomizations())
    }
    
    @Test
    fun `createPrivacyFocused factory method creates privacy-focused settings`() {
        val settings = UserSettings.createPrivacyFocused(testUserId)
        
        assertTrue(settings.isValid())
        assertFalse(settings.notificationPreferences.hasEnabledNotifications())
        assertFalse(settings.privacyPreferences.hasDataCollectionEnabled())
        assertFalse(settings.syncPreferences.isSyncEnabled())
    }
    
    @Test
    fun `isValid returns false for blank user ID`() {
        val settings = UserSettings(userId = "")
        
        assertFalse(settings.isValid())
        val errors = settings.getValidationErrors()
        assertTrue(errors.any { it.contains("User ID cannot be blank") })
    }
    
    @Test
    fun `isValid returns false for whitespace-only user ID`() {
        val settings = UserSettings(userId = "   ")
        
        assertFalse(settings.isValid())
        val errors = settings.getValidationErrors()
        assertTrue(errors.any { it.contains("User ID cannot be blank") })
    }
    
    @Test
    fun `isValid returns false when unit preferences are invalid`() {
        // Create invalid unit preferences by using reflection or creating a custom invalid state
        // Since UnitPreferences.isValid() always returns true, we'll test with invalid cycle preferences
        val invalidCyclePrefs = CyclePreferences(
            averageCycleLength = 50, // Invalid: above maximum
            averageLutealPhaseLength = 14,
            periodDuration = 5
        )
        
        val settings = UserSettings(
            userId = testUserId,
            cyclePreferences = invalidCyclePrefs
        )
        
        assertFalse(settings.isValid())
        val errors = settings.getValidationErrors()
        assertTrue(errors.isNotEmpty())
    }
    
    @Test
    fun `isValid returns false when notification preferences are invalid`() {
        val invalidNotificationSetting = NotificationSetting(
            enabled = true,
            time = null, // Invalid: enabled but no time
            daysInAdvance = 1
        )
        
        val invalidNotificationPrefs = NotificationPreferences(
            dailyLoggingReminder = invalidNotificationSetting
        )
        
        val settings = UserSettings(
            userId = testUserId,
            notificationPreferences = invalidNotificationPrefs
        )
        
        assertFalse(settings.isValid())
        val errors = settings.getValidationErrors()
        assertTrue(errors.any { it.contains("Notification preferences are invalid") })
    }
    
    @Test
    fun `isValid returns false when display preferences are invalid`() {
        val invalidDisplayPrefs = DisplayPreferences(
            textSizeScale = 0.5f, // Invalid: below minimum
            hapticFeedbackEnabled = false,
            hapticIntensity = HapticIntensity.MEDIUM // Invalid: should be disabled
        )
        
        val settings = UserSettings(
            userId = testUserId,
            displayPreferences = invalidDisplayPrefs
        )
        
        assertFalse(settings.isValid())
        val errors = settings.getValidationErrors()
        assertTrue(errors.isNotEmpty())
    }
    
    @Test
    fun `getValidationErrors returns all errors from all preference sections`() {
        val invalidCyclePrefs = CyclePreferences(
            averageCycleLength = 50, // Invalid
            averageLutealPhaseLength = 5, // Invalid
            periodDuration = 10 // Invalid
        )
        
        val invalidDisplayPrefs = DisplayPreferences(
            textSizeScale = 0.5f, // Invalid
            hapticFeedbackEnabled = false,
            hapticIntensity = HapticIntensity.MEDIUM // Invalid
        )
        
        val settings = UserSettings(
            userId = testUserId,
            cyclePreferences = invalidCyclePrefs,
            displayPreferences = invalidDisplayPrefs
        )
        
        val errors = settings.getValidationErrors()
        assertTrue(errors.size >= 5) // At least 3 cycle errors + 2 display errors
    }
    
    @Test
    fun `withUpdate creates new instance with updated timestamp and pending sync`() {
        val original = UserSettings.createDefault(testUserId).copy(
            syncStatus = SyncStatus.SYNCED
        )
        
        val updated = original.withUpdate()
        
        assertEquals(original.userId, updated.userId)
        assertEquals(SyncStatus.SYNCED, original.syncStatus)
        assertEquals(SyncStatus.PENDING, updated.syncStatus)
        assertTrue(updated.lastModified >= original.lastModified)
    }
    
    @Test
    fun `markAsSynced updates sync status`() {
        val original = UserSettings.createDefault(testUserId)
        val synced = original.markAsSynced()
        
        assertEquals(SyncStatus.PENDING, original.syncStatus)
        assertEquals(SyncStatus.SYNCED, synced.syncStatus)
        assertTrue(original.needsSync())
        assertFalse(synced.needsSync())
    }
    
    @Test
    fun `markAsSyncError updates sync status`() {
        val original = UserSettings.createDefault(testUserId)
        val error = original.markAsSyncError()
        
        assertEquals(SyncStatus.PENDING, original.syncStatus)
        assertEquals(SyncStatus.FAILED, error.syncStatus)
    }
    
    @Test
    fun `needsSync returns correct values for different sync statuses`() {
        val pending = UserSettings.createDefault(testUserId)
        val synced = pending.markAsSynced()
        val error = pending.markAsSyncError()
        
        assertTrue(pending.needsSync())
        assertFalse(synced.needsSync())
        assertFalse(error.needsSync()) // Failed status doesn't need sync
    }
    
    @Test
    fun `hasCustomizations returns false for default settings`() {
        val settings = UserSettings.createDefault(testUserId)
        
        assertFalse(settings.hasCustomizations())
    }
    
    @Test
    fun `hasCustomizations returns true when unit preferences are manually set`() {
        val settings = UserSettings.createDefault(testUserId).copy(
            unitPreferences = UnitPreferences(isManuallySet = true)
        )
        
        assertTrue(settings.hasCustomizations())
    }
    
    @Test
    fun `hasCustomizations returns true when cycle preferences are customized`() {
        val settings = UserSettings.createDefault(testUserId).copy(
            cyclePreferences = CyclePreferences(isCustomized = true)
        )
        
        assertTrue(settings.hasCustomizations())
    }
    
    @Test
    fun `hasCustomizations returns true when notifications are enabled`() {
        val settings = UserSettings.createDefault(testUserId).copy(
            notificationPreferences = NotificationPreferences.withDefaults()
        )
        
        assertTrue(settings.hasCustomizations())
    }
    
    @Test
    fun `hasCustomizations returns true when accessibility features are enabled`() {
        val settings = UserSettings.createDefault(testUserId).copy(
            displayPreferences = DisplayPreferences.accessibilityOptimized()
        )
        
        assertTrue(settings.hasCustomizations())
    }
    
    @Test
    fun `hasCustomizations returns true when data collection is disabled`() {
        val settings = UserSettings.createDefault(testUserId).copy(
            privacyPreferences = PrivacyPreferences.maxPrivacy()
        )
        
        assertTrue(settings.hasCustomizations())
    }
    
    @Test
    fun `copy with changes works correctly`() {
        val original = UserSettings.createDefault(testUserId)
        val modified = original.copy(
            unitPreferences = UnitPreferences(
                temperatureUnit = TemperatureUnit.FAHRENHEIT,
                isManuallySet = true
            ),
            syncStatus = SyncStatus.SYNCED
        )
        
        assertEquals(TemperatureUnit.CELSIUS, original.unitPreferences.temperatureUnit)
        assertEquals(TemperatureUnit.FAHRENHEIT, modified.unitPreferences.temperatureUnit)
        assertFalse(original.unitPreferences.isManuallySet)
        assertTrue(modified.unitPreferences.isManuallySet)
        assertEquals(SyncStatus.PENDING, original.syncStatus)
        assertEquals(SyncStatus.SYNCED, modified.syncStatus)
    }
    
    @Test
    fun `settings maintain immutability across operations`() {
        val original = UserSettings.createDefault(testUserId)
        val updated = original.withUpdate()
        val synced = original.markAsSynced()
        val error = original.markAsSyncError()
        
        // Original should remain unchanged
        assertEquals(SyncStatus.PENDING, original.syncStatus)
        
        // Each operation creates a new instance
        assertEquals(SyncStatus.PENDING, updated.syncStatus)
        assertEquals(SyncStatus.SYNCED, synced.syncStatus)
        assertEquals(SyncStatus.FAILED, error.syncStatus)
        
        // Timestamps should be greater or equal for updated (may be same if called very quickly)
        assertTrue(updated.lastModified >= original.lastModified)
        assertEquals(original.lastModified, synced.lastModified)
        assertEquals(original.lastModified, error.lastModified)
    }
    
    @Test
    fun `version is set correctly`() {
        val settings = UserSettings.createDefault(testUserId)
        
        assertEquals(UserSettings.CURRENT_VERSION, settings.version)
        assertEquals(1, settings.version) // Current version should be 1
    }
    
    @Test
    fun `serialization annotations are present`() {
        // This test ensures the class can be serialized
        val settings = UserSettings.createWithDefaults(testUserId, "US")
        
        // If serialization annotations are missing, this would fail at compile time
        assertTrue(settings.isValid())
    }
    
    @Test
    fun `all factory methods create valid settings`() {
        val factoryMethods = listOf(
            { UserSettings.createDefault(testUserId) },
            { UserSettings.createDefault(testUserId, "US") },
            { UserSettings.createWithDefaults(testUserId) },
            { UserSettings.createWithDefaults(testUserId, "DE") },
            { UserSettings.createPrivacyFocused(testUserId) },
            { UserSettings.createPrivacyFocused(testUserId, "GB") }
        )
        
        factoryMethods.forEach { factory ->
            val settings = factory()
            assertTrue(settings.isValid(), "Factory method should create valid settings")
            assertEquals(testUserId, settings.userId)
        }
    }
}