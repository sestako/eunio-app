package com.eunio.healthapp.domain.model.settings

import com.eunio.healthapp.domain.model.SyncStatus
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class UserSettings(
    val userId: String,
    val unitPreferences: UnitPreferences = UnitPreferences.default(),
    val notificationPreferences: NotificationPreferences = NotificationPreferences.default(),
    val cyclePreferences: CyclePreferences = CyclePreferences.default(),
    val privacyPreferences: PrivacyPreferences = PrivacyPreferences.default(),
    val displayPreferences: DisplayPreferences = DisplayPreferences.default(),
    val syncPreferences: SyncPreferences = SyncPreferences.default(),
    val lastModified: Instant = Clock.System.now(),
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val version: Int = CURRENT_VERSION
) {
    /**
     * Validates that all user settings are in a valid state
     * 
     * @return true if all settings are valid, false otherwise
     */
    fun isValid(): Boolean {
        return userId.isNotBlank() &&
                unitPreferences.isValid() &&
                notificationPreferences.isValid() &&
                cyclePreferences.isValid() &&
                privacyPreferences.isValid() &&
                displayPreferences.isValid() &&
                syncPreferences.isValid()
    }
    
    /**
     * Returns all validation errors across all preference sections
     */
    fun getValidationErrors(): List<String> {
        val errors = mutableListOf<String>()
        
        if (userId.isBlank()) {
            errors.add("User ID cannot be blank")
        }
        
        if (!unitPreferences.isValid()) {
            errors.add("Unit preferences are invalid")
        }
        
        if (!notificationPreferences.isValid()) {
            errors.add("Notification preferences are invalid")
        }
        
        errors.addAll(cyclePreferences.getValidationErrors())
        errors.addAll(displayPreferences.getValidationErrors())
        
        return errors
    }
    
    /**
     * Returns a copy with updated last modified time and pending sync status
     */
    fun withUpdate(): UserSettings {
        return copy(
            lastModified = Clock.System.now(),
            syncStatus = SyncStatus.PENDING
        )
    }
    
    /**
     * Returns a copy marked as synced
     */
    fun markAsSynced(): UserSettings {
        return copy(syncStatus = SyncStatus.SYNCED)
    }
    
    /**
     * Returns a copy marked as having sync error
     */
    fun markAsSyncError(): UserSettings {
        return copy(syncStatus = SyncStatus.FAILED)
    }
    
    /**
     * Returns true if settings need to be synced
     */
    fun needsSync(): Boolean {
        return syncStatus == SyncStatus.PENDING
    }
    
    /**
     * Returns true if any customizations have been made from defaults
     */
    fun hasCustomizations(): Boolean {
        return unitPreferences.isManuallySet ||
                cyclePreferences.isCustomized ||
                notificationPreferences.hasEnabledNotifications() ||
                displayPreferences.hasAccessibilityFeaturesEnabled() ||
                !privacyPreferences.hasDataCollectionEnabled()
    }
    
    companion object {
        const val CURRENT_VERSION = 1
        
        /**
         * Creates default user settings for a given user ID
         * 
         * @param userId The user ID
         * @param locale Optional device locale for unit preferences
         * @return Default UserSettings
         */
        fun createDefault(userId: String, locale: String? = null): UserSettings {
            val unitPrefs = if (locale != null) {
                UnitPreferences.fromLocale(locale)
            } else {
                UnitPreferences.default()
            }
            
            return UserSettings(
                userId = userId,
                unitPreferences = unitPrefs,
                notificationPreferences = NotificationPreferences.default(),
                cyclePreferences = CyclePreferences.default(),
                privacyPreferences = PrivacyPreferences.default(),
                displayPreferences = DisplayPreferences.default(),
                syncPreferences = SyncPreferences.default(),
                lastModified = Clock.System.now(),
                syncStatus = SyncStatus.PENDING
            )
        }
        
        /**
         * Creates user settings with commonly used defaults
         * 
         * @param userId The user ID
         * @param locale Optional device locale for unit preferences
         * @return UserSettings with practical defaults
         */
        fun createWithDefaults(userId: String, locale: String? = null): UserSettings {
            val unitPrefs = if (locale != null) {
                UnitPreferences.fromLocale(locale)
            } else {
                UnitPreferences.default()
            }
            
            return UserSettings(
                userId = userId,
                unitPreferences = unitPrefs,
                notificationPreferences = NotificationPreferences.withDefaults(),
                cyclePreferences = CyclePreferences.default(),
                privacyPreferences = PrivacyPreferences.balanced(),
                displayPreferences = DisplayPreferences.default(),
                syncPreferences = SyncPreferences.default(),
                lastModified = Clock.System.now(),
                syncStatus = SyncStatus.PENDING
            )
        }
        
        /**
         * Creates user settings optimized for privacy
         * 
         * @param userId The user ID
         * @param locale Optional device locale for unit preferences
         * @return Privacy-focused UserSettings
         */
        fun createPrivacyFocused(userId: String, locale: String? = null): UserSettings {
            val unitPrefs = if (locale != null) {
                UnitPreferences.fromLocale(locale)
            } else {
                UnitPreferences.default()
            }
            
            return UserSettings(
                userId = userId,
                unitPreferences = unitPrefs,
                notificationPreferences = NotificationPreferences.default(),
                cyclePreferences = CyclePreferences.default(),
                privacyPreferences = PrivacyPreferences.maxPrivacy(),
                displayPreferences = DisplayPreferences.default(),
                syncPreferences = SyncPreferences.offlineFirst(),
                lastModified = Clock.System.now(),
                syncStatus = SyncStatus.PENDING
            )
        }
    }
}