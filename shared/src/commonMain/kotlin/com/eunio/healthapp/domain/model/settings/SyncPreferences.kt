package com.eunio.healthapp.domain.model.settings

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class SyncPreferences(
    val autoSyncEnabled: Boolean = true,
    val wifiOnlySync: Boolean = false,
    val cloudBackupEnabled: Boolean = true,
    val lastSyncTime: Instant? = null
) {
    /**
     * Validates that sync preferences are in a valid state
     * All boolean preferences are inherently valid
     * 
     * @return true (always valid)
     */
    fun isValid(): Boolean = true
    
    /**
     * Returns true if sync is effectively enabled
     */
    fun isSyncEnabled(): Boolean {
        return autoSyncEnabled && cloudBackupEnabled
    }
    
    /**
     * Returns true if sync should occur based on current network conditions
     * 
     * @param isWifiConnected Whether device is connected to WiFi
     * @param isMobileConnected Whether device is connected to mobile data
     * @return true if sync should proceed
     */
    fun shouldSync(isWifiConnected: Boolean, isMobileConnected: Boolean): Boolean {
        if (!isSyncEnabled()) return false
        
        return when {
            isWifiConnected -> true
            wifiOnlySync -> false
            isMobileConnected -> true
            else -> false
        }
    }
    
    /**
     * Returns a copy with updated last sync time
     */
    fun withLastSyncTime(time: Instant): SyncPreferences {
        return copy(lastSyncTime = time)
    }
    
    /**
     * Returns true if sync has never occurred
     */
    fun isFirstSync(): Boolean {
        return lastSyncTime == null
    }
    
    companion object {
        fun default(): SyncPreferences {
            return SyncPreferences()
        }
        
        /**
         * Creates sync preferences optimized for data conservation
         */
        fun dataConservative(): SyncPreferences {
            return SyncPreferences(
                autoSyncEnabled = true,
                wifiOnlySync = true,
                cloudBackupEnabled = true,
                lastSyncTime = null
            )
        }
        
        /**
         * Creates sync preferences for offline-first usage
         */
        fun offlineFirst(): SyncPreferences {
            return SyncPreferences(
                autoSyncEnabled = false,
                wifiOnlySync = true,
                cloudBackupEnabled = false,
                lastSyncTime = null
            )
        }
        
        /**
         * Creates sync preferences for maximum availability
         */
        fun maxAvailability(): SyncPreferences {
            return SyncPreferences(
                autoSyncEnabled = true,
                wifiOnlySync = false,
                cloudBackupEnabled = true,
                lastSyncTime = null
            )
        }
    }
}