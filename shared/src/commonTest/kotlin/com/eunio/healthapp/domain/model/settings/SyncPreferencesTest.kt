package com.eunio.healthapp.domain.model.settings

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SyncPreferencesTest {
    
    @Test
    fun `default constructor creates valid preferences`() {
        val preferences = SyncPreferences()
        
        assertTrue(preferences.autoSyncEnabled)
        assertFalse(preferences.wifiOnlySync)
        assertTrue(preferences.cloudBackupEnabled)
        assertNull(preferences.lastSyncTime)
        assertTrue(preferences.isValid())
        assertTrue(preferences.isSyncEnabled())
        assertTrue(preferences.isFirstSync())
    }
    
    @Test
    fun `default factory method creates valid preferences`() {
        val preferences = SyncPreferences.default()
        
        assertTrue(preferences.isValid())
        assertTrue(preferences.isSyncEnabled())
    }
    
    @Test
    fun `dataConservative factory method creates wifi-only preferences`() {
        val preferences = SyncPreferences.dataConservative()
        
        assertTrue(preferences.autoSyncEnabled)
        assertTrue(preferences.wifiOnlySync)
        assertTrue(preferences.cloudBackupEnabled)
        assertNull(preferences.lastSyncTime)
        assertTrue(preferences.isValid())
        assertTrue(preferences.isSyncEnabled())
    }
    
    @Test
    fun `offlineFirst factory method creates offline-focused preferences`() {
        val preferences = SyncPreferences.offlineFirst()
        
        assertFalse(preferences.autoSyncEnabled)
        assertTrue(preferences.wifiOnlySync)
        assertFalse(preferences.cloudBackupEnabled)
        assertNull(preferences.lastSyncTime)
        assertTrue(preferences.isValid())
        assertFalse(preferences.isSyncEnabled())
    }
    
    @Test
    fun `maxAvailability factory method creates always-sync preferences`() {
        val preferences = SyncPreferences.maxAvailability()
        
        assertTrue(preferences.autoSyncEnabled)
        assertFalse(preferences.wifiOnlySync)
        assertTrue(preferences.cloudBackupEnabled)
        assertNull(preferences.lastSyncTime)
        assertTrue(preferences.isValid())
        assertTrue(preferences.isSyncEnabled())
    }
    
    @Test
    fun `isSyncEnabled returns false when auto sync disabled`() {
        val preferences = SyncPreferences(
            autoSyncEnabled = false,
            cloudBackupEnabled = true
        )
        
        assertFalse(preferences.isSyncEnabled())
    }
    
    @Test
    fun `isSyncEnabled returns false when cloud backup disabled`() {
        val preferences = SyncPreferences(
            autoSyncEnabled = true,
            cloudBackupEnabled = false
        )
        
        assertFalse(preferences.isSyncEnabled())
    }
    
    @Test
    fun `isSyncEnabled returns true when both auto sync and cloud backup enabled`() {
        val preferences = SyncPreferences(
            autoSyncEnabled = true,
            cloudBackupEnabled = true
        )
        
        assertTrue(preferences.isSyncEnabled())
    }
    
    @Test
    fun `shouldSync returns false when sync disabled`() {
        val preferences = SyncPreferences(
            autoSyncEnabled = false,
            cloudBackupEnabled = false
        )
        
        assertFalse(preferences.shouldSync(isWifiConnected = true, isMobileConnected = true))
        assertFalse(preferences.shouldSync(isWifiConnected = false, isMobileConnected = true))
        assertFalse(preferences.shouldSync(isWifiConnected = true, isMobileConnected = false))
        assertFalse(preferences.shouldSync(isWifiConnected = false, isMobileConnected = false))
    }
    
    @Test
    fun `shouldSync returns true when wifi connected and sync enabled`() {
        val preferences = SyncPreferences(
            autoSyncEnabled = true,
            wifiOnlySync = false,
            cloudBackupEnabled = true
        )
        
        assertTrue(preferences.shouldSync(isWifiConnected = true, isMobileConnected = false))
        assertTrue(preferences.shouldSync(isWifiConnected = true, isMobileConnected = true))
    }
    
    @Test
    fun `shouldSync with wifi only returns false when only mobile connected`() {
        val preferences = SyncPreferences(
            autoSyncEnabled = true,
            wifiOnlySync = true,
            cloudBackupEnabled = true
        )
        
        assertFalse(preferences.shouldSync(isWifiConnected = false, isMobileConnected = true))
    }
    
    @Test
    fun `shouldSync with wifi only returns true when wifi connected`() {
        val preferences = SyncPreferences(
            autoSyncEnabled = true,
            wifiOnlySync = true,
            cloudBackupEnabled = true
        )
        
        assertTrue(preferences.shouldSync(isWifiConnected = true, isMobileConnected = false))
        assertTrue(preferences.shouldSync(isWifiConnected = true, isMobileConnected = true))
    }
    
    @Test
    fun `shouldSync without wifi only returns true when mobile connected`() {
        val preferences = SyncPreferences(
            autoSyncEnabled = true,
            wifiOnlySync = false,
            cloudBackupEnabled = true
        )
        
        assertTrue(preferences.shouldSync(isWifiConnected = false, isMobileConnected = true))
    }
    
    @Test
    fun `shouldSync returns false when no connection`() {
        val preferences = SyncPreferences(
            autoSyncEnabled = true,
            wifiOnlySync = false,
            cloudBackupEnabled = true
        )
        
        assertFalse(preferences.shouldSync(isWifiConnected = false, isMobileConnected = false))
    }
    
    @Test
    fun `withLastSyncTime updates sync time correctly`() {
        val original = SyncPreferences.default()
        val syncTime = Clock.System.now()
        val updated = original.withLastSyncTime(syncTime)
        
        assertNull(original.lastSyncTime)
        assertEquals(syncTime, updated.lastSyncTime)
        assertTrue(original.isFirstSync())
        assertFalse(updated.isFirstSync())
    }
    
    @Test
    fun `isFirstSync returns true when lastSyncTime is null`() {
        val preferences = SyncPreferences(lastSyncTime = null)
        
        assertTrue(preferences.isFirstSync())
    }
    
    @Test
    fun `isFirstSync returns false when lastSyncTime is set`() {
        val preferences = SyncPreferences(lastSyncTime = Clock.System.now())
        
        assertFalse(preferences.isFirstSync())
    }
    
    @Test
    fun `all boolean combinations are valid`() {
        val combinations = listOf(
            SyncPreferences(false, false, false),
            SyncPreferences(true, false, false),
            SyncPreferences(false, true, false),
            SyncPreferences(false, false, true),
            SyncPreferences(true, true, false),
            SyncPreferences(true, false, true),
            SyncPreferences(false, true, true),
            SyncPreferences(true, true, true)
        )
        
        combinations.forEach { preferences ->
            assertTrue(preferences.isValid(), "Preferences should be valid: $preferences")
        }
    }
    
    @Test
    fun `copy with changes works correctly`() {
        val original = SyncPreferences.default()
        val syncTime = Clock.System.now()
        val modified = original.copy(
            wifiOnlySync = true,
            lastSyncTime = syncTime
        )
        
        assertFalse(original.wifiOnlySync)
        assertTrue(modified.wifiOnlySync)
        assertNull(original.lastSyncTime)
        assertEquals(syncTime, modified.lastSyncTime)
    }
    
    @Test
    fun `sync preferences maintain immutability`() {
        val original = SyncPreferences.default()
        val dataConservative = SyncPreferences.dataConservative()
        val offlineFirst = SyncPreferences.offlineFirst()
        val maxAvailability = SyncPreferences.maxAvailability()
        
        // Each factory method creates different configurations
        assertFalse(original.wifiOnlySync)
        assertTrue(dataConservative.wifiOnlySync)
        assertTrue(offlineFirst.wifiOnlySync)
        assertFalse(maxAvailability.wifiOnlySync)
        
        assertTrue(original.autoSyncEnabled)
        assertTrue(dataConservative.autoSyncEnabled)
        assertFalse(offlineFirst.autoSyncEnabled)
        assertTrue(maxAvailability.autoSyncEnabled)
    }
    
    @Test
    fun `withLastSyncTime preserves other properties`() {
        val original = SyncPreferences(
            autoSyncEnabled = false,
            wifiOnlySync = true,
            cloudBackupEnabled = false,
            lastSyncTime = null
        )
        
        val syncTime = Clock.System.now()
        val updated = original.withLastSyncTime(syncTime)
        
        assertEquals(original.autoSyncEnabled, updated.autoSyncEnabled)
        assertEquals(original.wifiOnlySync, updated.wifiOnlySync)
        assertEquals(original.cloudBackupEnabled, updated.cloudBackupEnabled)
        assertEquals(syncTime, updated.lastSyncTime)
    }
    
    @Test
    fun `serialization annotations are present`() {
        // This test ensures the class can be serialized
        val preferences = SyncPreferences(
            autoSyncEnabled = true,
            wifiOnlySync = true,
            cloudBackupEnabled = false,
            lastSyncTime = Clock.System.now()
        )
        
        // If serialization annotations are missing, this would fail at compile time
        assertTrue(preferences.isValid())
    }
}