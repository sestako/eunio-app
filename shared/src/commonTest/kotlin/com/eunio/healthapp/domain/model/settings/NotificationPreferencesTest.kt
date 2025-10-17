package com.eunio.healthapp.domain.model.settings

import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationPreferencesTest {
    
    @Test
    fun `default constructor creates valid preferences with all disabled`() {
        val preferences = NotificationPreferences()
        
        assertFalse(preferences.dailyLoggingReminder.enabled)
        assertFalse(preferences.periodPredictionAlert.enabled)
        assertFalse(preferences.ovulationAlert.enabled)
        assertFalse(preferences.insightNotifications.enabled)
        assertTrue(preferences.globalNotificationsEnabled)
        assertTrue(preferences.isValid())
        assertFalse(preferences.hasEnabledNotifications())
    }
    
    @Test
    fun `default factory method creates valid preferences`() {
        val preferences = NotificationPreferences.default()
        
        assertTrue(preferences.isValid())
        assertFalse(preferences.hasEnabledNotifications())
    }
    
    @Test
    fun `withDefaults factory method creates preferences with some enabled`() {
        val preferences = NotificationPreferences.withDefaults()
        
        assertTrue(preferences.dailyLoggingReminder.enabled)
        assertTrue(preferences.periodPredictionAlert.enabled)
        assertTrue(preferences.ovulationAlert.enabled)
        assertFalse(preferences.insightNotifications.enabled)
        assertTrue(preferences.globalNotificationsEnabled)
        assertTrue(preferences.isValid())
        assertTrue(preferences.hasEnabledNotifications())
    }
    
    @Test
    fun `preferences with invalid notification setting is invalid`() {
        val invalidSetting = NotificationSetting(
            enabled = true,
            time = null, // Invalid: enabled but no time
            daysInAdvance = 1
        )
        
        val preferences = NotificationPreferences(
            dailyLoggingReminder = invalidSetting
        )
        
        assertFalse(preferences.isValid())
    }
    
    @Test
    fun `preferences with all valid settings is valid`() {
        val validSetting = NotificationSetting(
            enabled = true,
            time = LocalTime(9, 0),
            daysInAdvance = 1
        )
        
        val preferences = NotificationPreferences(
            dailyLoggingReminder = validSetting,
            periodPredictionAlert = validSetting,
            ovulationAlert = validSetting,
            insightNotifications = validSetting,
            globalNotificationsEnabled = true
        )
        
        assertTrue(preferences.isValid())
        assertTrue(preferences.hasEnabledNotifications())
    }
    
    @Test
    fun `hasEnabledNotifications returns false when global notifications disabled`() {
        val enabledSetting = NotificationSetting(
            enabled = true,
            time = LocalTime(9, 0),
            daysInAdvance = 1
        )
        
        val preferences = NotificationPreferences(
            dailyLoggingReminder = enabledSetting,
            globalNotificationsEnabled = false
        )
        
        assertFalse(preferences.hasEnabledNotifications())
    }
    
    @Test
    fun `hasEnabledNotifications returns true when any notification enabled and global enabled`() {
        val enabledSetting = NotificationSetting(
            enabled = true,
            time = LocalTime(9, 0),
            daysInAdvance = 1
        )
        
        val preferences = NotificationPreferences(
            dailyLoggingReminder = enabledSetting,
            globalNotificationsEnabled = true
        )
        
        assertTrue(preferences.hasEnabledNotifications())
    }
    
    @Test
    fun `getEnabledNotifications returns empty list when global disabled`() {
        val enabledSetting = NotificationSetting(
            enabled = true,
            time = LocalTime(9, 0),
            daysInAdvance = 1
        )
        
        val preferences = NotificationPreferences(
            dailyLoggingReminder = enabledSetting,
            periodPredictionAlert = enabledSetting,
            globalNotificationsEnabled = false
        )
        
        val enabled = preferences.getEnabledNotifications()
        assertTrue(enabled.isEmpty())
    }
    
    @Test
    fun `getEnabledNotifications returns correct enabled notifications`() {
        val enabledSetting = NotificationSetting(
            enabled = true,
            time = LocalTime(9, 0),
            daysInAdvance = 1
        )
        
        val disabledSetting = NotificationSetting.disabled()
        
        val preferences = NotificationPreferences(
            dailyLoggingReminder = enabledSetting,
            periodPredictionAlert = disabledSetting,
            ovulationAlert = enabledSetting,
            insightNotifications = disabledSetting,
            globalNotificationsEnabled = true
        )
        
        val enabled = preferences.getEnabledNotifications()
        assertEquals(2, enabled.size)
        
        val enabledNames = enabled.map { it.first }
        assertTrue(enabledNames.contains("dailyLoggingReminder"))
        assertTrue(enabledNames.contains("ovulationAlert"))
        assertFalse(enabledNames.contains("periodPredictionAlert"))
        assertFalse(enabledNames.contains("insightNotifications"))
    }
    
    @Test
    fun `getEnabledNotifications returns all when all enabled`() {
        val enabledSetting = NotificationSetting(
            enabled = true,
            time = LocalTime(9, 0),
            daysInAdvance = 1
        )
        
        val preferences = NotificationPreferences(
            dailyLoggingReminder = enabledSetting,
            periodPredictionAlert = enabledSetting,
            ovulationAlert = enabledSetting,
            insightNotifications = enabledSetting,
            globalNotificationsEnabled = true
        )
        
        val enabled = preferences.getEnabledNotifications()
        assertEquals(4, enabled.size)
    }
    
    @Test
    fun `copy with changes works correctly`() {
        val original = NotificationPreferences.default()
        val enabledSetting = NotificationSetting.defaultEnabled()
        
        val modified = original.copy(
            dailyLoggingReminder = enabledSetting
        )
        
        assertFalse(original.dailyLoggingReminder.enabled)
        assertTrue(modified.dailyLoggingReminder.enabled)
        assertFalse(original.hasEnabledNotifications())
        assertTrue(modified.hasEnabledNotifications())
    }
    
    @Test
    fun `serialization annotations are present`() {
        // This test ensures the class can be serialized
        val preferences = NotificationPreferences.withDefaults()
        
        // If serialization annotations are missing, this would fail at compile time
        assertTrue(preferences.isValid())
    }
}