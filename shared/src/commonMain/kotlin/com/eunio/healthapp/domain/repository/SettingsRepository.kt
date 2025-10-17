package com.eunio.healthapp.domain.repository

import com.eunio.healthapp.domain.model.settings.UserSettings
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for settings-related data operations.
 * Handles settings persistence, synchronization, backup, and offline-first data access.
 */
interface SettingsRepository {
    
    /**
     * Retrieves user settings for the current user.
     * Uses offline-first strategy: local cache first, then remote if needed.
     * 
     * @return Result containing UserSettings if found, null if not found
     */
    suspend fun getUserSettings(): Result<UserSettings?>
    
    /**
     * Retrieves user settings for a specific user.
     * 
     * @param userId The user ID to get settings for
     * @return Result containing UserSettings if found, null if not found
     */
    suspend fun getUserSettings(userId: String): Result<UserSettings?>
    
    /**
     * Saves user settings with automatic sync handling.
     * Saves locally first, then syncs to remote when connectivity allows.
     * 
     * @param settings The settings to save
     * @return Result indicating success or failure
     */
    suspend fun saveUserSettings(settings: UserSettings): Result<Unit>
    
    /**
     * Updates specific sections of user settings without affecting other sections.
     * 
     * @param userId The user ID to update settings for
     * @param updateFunction Function that transforms the current settings
     * @return Result containing the updated UserSettings
     */
    suspend fun updateUserSettings(
        userId: String,
        updateFunction: (UserSettings) -> UserSettings
    ): Result<UserSettings>
    
    /**
     * Forces synchronization of settings between local and remote storage.
     * Handles conflict resolution automatically based on last modified timestamps.
     * 
     * @return Result indicating sync success or failure
     */
    suspend fun syncSettings(): Result<Unit>
    
    /**
     * Forces synchronization for a specific user.
     * 
     * @param userId The user ID to sync settings for
     * @return Result indicating sync success or failure
     */
    suspend fun syncSettings(userId: String): Result<Unit>
    
    /**
     * Resolves sync conflicts by choosing the most recent version.
     * 
     * @param userId The user ID with conflicting settings
     * @param localSettings The local version of settings
     * @param remoteSettings The remote version of settings
     * @return Result containing the resolved UserSettings
     */
    suspend fun resolveSettingsConflict(
        userId: String,
        localSettings: UserSettings,
        remoteSettings: UserSettings
    ): Result<UserSettings>
    
    /**
     * Creates a backup of user settings for export or migration.
     * 
     * @param userId The user ID to backup settings for
     * @return Result containing the backup data as JSON string
     */
    suspend fun backupUserSettings(userId: String): Result<String>
    
    /**
     * Restores user settings from a backup.
     * 
     * @param userId The user ID to restore settings for
     * @param backupData The backup data as JSON string
     * @return Result indicating restore success or failure
     */
    suspend fun restoreUserSettings(userId: String, backupData: String): Result<Unit>
    
    /**
     * Exports user settings data for data portability.
     * Includes all settings and metadata in a user-readable format.
     * 
     * @param userId The user ID to export data for
     * @return Result containing the export data as formatted string
     */
    suspend fun exportUserData(userId: String): Result<String>
    
    /**
     * Deletes all user settings and associated data.
     * Used for account deletion or data cleanup.
     * 
     * @param userId The user ID to delete settings for
     * @return Result indicating deletion success or failure
     */
    suspend fun deleteUserSettings(userId: String): Result<Unit>
    
    /**
     * Clears all settings from local storage.
     * Used for sign-out or data cleanup scenarios.
     * 
     * @return Result indicating success or failure
     */
    suspend fun clearLocalSettings(): Result<Unit>
    
    /**
     * Resets user settings to default values.
     * 
     * @param userId The user ID to reset settings for
     * @param locale Optional locale for unit preferences
     * @return Result containing the reset UserSettings
     */
    suspend fun resetToDefaults(userId: String, locale: String? = null): Result<UserSettings>
    
    /**
     * Observes changes to user settings for reactive UI updates.
     * 
     * @param userId The user ID to observe settings for
     * @return Flow of UserSettings changes
     */
    fun observeUserSettings(userId: String): Flow<UserSettings?>
    
    /**
     * Observes settings sync status for UI feedback.
     * 
     * @return Flow of sync status updates
     */
    fun observeSyncStatus(): Flow<Boolean>
    
    /**
     * Gets all settings that have pending sync status.
     * Used for background sync operations.
     * 
     * @return Result containing list of UserSettings with pending sync
     */
    suspend fun getPendingSyncSettings(): Result<List<UserSettings>>
    
    /**
     * Marks settings as successfully synced.
     * 
     * @param userId The user ID to mark as synced
     * @return Result indicating success or failure
     */
    suspend fun markAsSynced(userId: String): Result<Unit>
    
    /**
     * Marks settings as having sync errors.
     * 
     * @param userId The user ID to mark as failed
     * @return Result indicating success or failure
     */
    suspend fun markAsSyncFailed(userId: String): Result<Unit>
    
    /**
     * Checks if settings exist for a user.
     * 
     * @param userId The user ID to check
     * @return Result containing true if settings exist, false otherwise
     */
    suspend fun settingsExist(userId: String): Result<Boolean>
    
    /**
     * Gets the last modified timestamp for user settings.
     * Used for conflict resolution and sync optimization.
     * 
     * @param userId The user ID to get timestamp for
     * @return Result containing the timestamp in epoch milliseconds, null if not found
     */
    suspend fun getLastModifiedTimestamp(userId: String): Result<Long?>
    
    /**
     * Validates user settings before saving.
     * 
     * @param settings The settings to validate
     * @return Result indicating validation success or containing validation errors
     */
    suspend fun validateSettings(settings: UserSettings): Result<Unit>
    
    /**
     * Migrates settings from an older version to the current version.
     * 
     * @param settings The settings to migrate
     * @param targetVersion The target version to migrate to
     * @return Result containing the migrated UserSettings
     */
    suspend fun migrateSettings(settings: UserSettings, targetVersion: Int): Result<UserSettings>
}