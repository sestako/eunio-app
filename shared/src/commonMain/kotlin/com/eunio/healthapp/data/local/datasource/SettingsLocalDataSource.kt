package com.eunio.healthapp.data.local.datasource

import com.eunio.healthapp.domain.model.settings.UserSettings
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Local data source interface for settings operations.
 * Handles local storage of user settings using SQLDelight.
 */
interface SettingsLocalDataSource {
    
    /**
     * Retrieves user settings for the current user from local storage.
     * 
     * @return UserSettings if found, null if not found
     */
    suspend fun getSettings(): UserSettings?
    
    /**
     * Retrieves user settings for a specific user from local storage.
     * 
     * @param userId The user ID to get settings for
     * @return UserSettings if found, null if not found
     */
    suspend fun getSettings(userId: String): UserSettings?
    
    /**
     * Saves user settings to local storage.
     * 
     * @param settings The settings to save
     * @return Result indicating success or failure
     */
    suspend fun saveSettings(settings: UserSettings): Result<Unit>
    
    /**
     * Updates existing user settings in local storage.
     * 
     * @param settings The settings to update
     * @return Result indicating success or failure
     */
    suspend fun updateSettings(settings: UserSettings): Result<Unit>
    
    /**
     * Deletes user settings from local storage.
     * 
     * @param userId The user ID to delete settings for
     * @return Result indicating success or failure
     */
    suspend fun deleteSettings(userId: String): Result<Unit>
    
    /**
     * Retrieves all settings that have pending sync status.
     * Used for background sync operations.
     * 
     * @return List of UserSettings with PENDING sync status
     */
    suspend fun getPendingSyncSettings(): List<UserSettings>
    
    /**
     * Marks settings as successfully synced for a specific user.
     * 
     * @param userId The user ID to mark as synced
     * @return Result indicating success or failure
     */
    suspend fun markAsSynced(userId: String): Result<Unit>
    
    /**
     * Marks settings as having sync errors for a specific user.
     * 
     * @param userId The user ID to mark as failed
     * @return Result indicating success or failure
     */
    suspend fun markAsSyncFailed(userId: String): Result<Unit>
    
    /**
     * Clears all settings from local storage.
     * Used for sign-out or data cleanup scenarios.
     * 
     * @return Result indicating success or failure
     */
    suspend fun clearAllSettings(): Result<Unit>
    
    /**
     * Clears settings for a specific user from local storage.
     * 
     * @param userId The user ID to clear settings for
     * @return Result indicating success or failure
     */
    suspend fun clearSettings(userId: String): Result<Unit>
    
    /**
     * Checks if settings exist for a user in local storage.
     * 
     * @param userId The user ID to check
     * @return true if settings exist, false otherwise
     */
    suspend fun settingsExist(userId: String): Boolean
    
    /**
     * Gets the last modified timestamp for user settings from local storage.
     * 
     * @param userId The user ID to get timestamp for
     * @return The timestamp in epoch milliseconds, null if not found
     */
    suspend fun getLastModifiedTimestamp(userId: String): Long?
    
    /**
     * Observes changes to user settings for reactive updates.
     * 
     * @param userId The user ID to observe settings for
     * @return Flow of UserSettings changes
     */
    fun observeSettings(userId: String): Flow<UserSettings?>
    
    /**
     * Creates a backup of user settings in local storage.
     * 
     * @param userId The user ID to backup settings for
     * @param backupType The type of backup (MANUAL, AUTOMATIC, EXPORT)
     * @return Result containing the backup ID
     */
    suspend fun createSettingsBackup(userId: String, backupType: String): Result<Long>
    
    /**
     * Retrieves a settings backup by ID.
     * 
     * @param backupId The backup ID to retrieve
     * @return Result containing the backup data as JSON string
     */
    suspend fun getSettingsBackup(backupId: Long): Result<String?>
    
    /**
     * Retrieves all backups for a specific user.
     * 
     * @param userId The user ID to get backups for
     * @return Result containing list of backup metadata
     */
    suspend fun getUserBackups(userId: String): Result<List<SettingsBackupInfo>>
    
    /**
     * Deletes a settings backup.
     * 
     * @param backupId The backup ID to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteSettingsBackup(backupId: Long): Result<Unit>
    
    /**
     * Deletes all backups for a specific user.
     * 
     * @param userId The user ID to delete backups for
     * @return Result indicating success or failure
     */
    suspend fun deleteUserBackups(userId: String): Result<Unit>
    
    /**
     * Gets the total count of settings records in local storage.
     * Used for analytics and debugging.
     * 
     * @return The total count of settings records
     */
    suspend fun getSettingsCount(): Long
    
    /**
     * Gets the total size of settings data in local storage.
     * Used for storage management and analytics.
     * 
     * @return The total size in bytes
     */
    suspend fun getSettingsDataSize(): Long
    
    /**
     * Performs database maintenance operations like vacuum and analyze.
     * 
     * @return Result indicating success or failure
     */
    suspend fun performMaintenance(): Result<Unit>
}

/**
 * Data class representing settings backup metadata
 */
data class SettingsBackupInfo(
    val backupId: Long,
    val userId: String,
    val backupType: String,
    val createdAt: Long,
    val dataSize: Long
)