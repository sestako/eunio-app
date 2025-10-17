package com.eunio.healthapp.domain.manager

import com.eunio.healthapp.domain.model.settings.*
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Interface for centralized settings management.
 * Provides reactive state management, validation, and business logic enforcement for user preferences.
 * Handles automatic updates to dependent systems when settings change.
 */
interface SettingsManager {
    
    /**
     * Retrieves current user settings with reactive updates.
     * 
     * @return Result containing current UserSettings
     */
    suspend fun getUserSettings(): Result<UserSettings>
    
    /**
     * Observes changes to user settings for reactive UI updates.
     * 
     * @return Flow of UserSettings changes
     */
    fun observeSettingsChanges(): Flow<UserSettings>
    
    /**
     * Updates unit preferences and applies changes throughout the app.
     * Triggers immediate UI updates for all measurement displays.
     * 
     * @param preferences The new unit preferences
     * @return Result indicating success or validation errors
     */
    suspend fun updateUnitPreferences(preferences: UnitPreferences): Result<Unit>
    
    /**
     * Updates notification preferences and reconfigures system notifications.
     * Automatically schedules/cancels notifications based on new preferences.
     * 
     * @param preferences The new notification preferences
     * @return Result indicating success or notification configuration errors
     */
    suspend fun updateNotificationPreferences(preferences: NotificationPreferences): Result<Unit>
    
    /**
     * Updates cycle preferences and triggers prediction recalculation.
     * Recalculates all future cycle predictions based on new parameters.
     * 
     * @param preferences The new cycle preferences
     * @return Result indicating success or validation errors
     */
    suspend fun updateCyclePreferences(preferences: CyclePreferences): Result<Unit>
    
    /**
     * Updates privacy preferences and applies data sharing changes.
     * Handles data collection consent and sharing preference changes.
     * 
     * @param preferences The new privacy preferences
     * @return Result indicating success or privacy configuration errors
     */
    suspend fun updatePrivacyPreferences(preferences: PrivacyPreferences): Result<Unit>
    
    /**
     * Updates display preferences and applies UI changes immediately.
     * Triggers immediate updates to text size, contrast, and haptic feedback.
     * 
     * @param preferences The new display preferences
     * @return Result indicating success or validation errors
     */
    suspend fun updateDisplayPreferences(preferences: DisplayPreferences): Result<Unit>
    
    /**
     * Updates sync preferences and reconfigures data synchronization.
     * Changes sync behavior and backup settings immediately.
     * 
     * @param preferences The new sync preferences
     * @return Result indicating success or sync configuration errors
     */
    suspend fun updateSyncPreferences(preferences: SyncPreferences): Result<Unit>
    
    /**
     * Updates multiple preference sections atomically.
     * Ensures all changes are applied together or none at all.
     * 
     * @param updateFunction Function that transforms current settings
     * @return Result containing updated UserSettings or validation errors
     */
    suspend fun updateSettings(updateFunction: (UserSettings) -> UserSettings): Result<UserSettings>
    
    /**
     * Validates settings before applying changes.
     * Checks all business rules and constraints.
     * 
     * @param settings The settings to validate
     * @return Result indicating validation success or containing error details
     */
    suspend fun validateSettings(settings: UserSettings): Result<Unit>
    
    /**
     * Resets all settings to default values.
     * Optionally preserves certain preferences like unit system based on locale.
     * 
     * @param preserveUnitPreferences Whether to keep current unit preferences
     * @return Result containing reset UserSettings
     */
    suspend fun resetToDefaults(preserveUnitPreferences: Boolean = false): Result<UserSettings>
    
    /**
     * Forces synchronization of settings across devices.
     * Handles conflict resolution and ensures consistency.
     * 
     * @return Result indicating sync success or failure
     */
    suspend fun syncSettings(): Result<Unit>
    
    /**
     * Exports user settings for backup or migration.
     * 
     * @return Result containing settings data as JSON string
     */
    suspend fun exportSettings(): Result<String>
    
    /**
     * Imports settings from backup data.
     * Validates imported data before applying changes.
     * 
     * @param backupData The backup data as JSON string
     * @return Result indicating import success or validation errors
     */
    suspend fun importSettings(backupData: String): Result<Unit>
    
    /**
     * Gets the current sync status of settings.
     * 
     * @return true if settings are synced, false if pending sync
     */
    suspend fun isSynced(): Boolean
    
    /**
     * Observes sync status changes for UI feedback.
     * 
     * @return Flow of sync status updates
     */
    fun observeSyncStatus(): Flow<Boolean>
}