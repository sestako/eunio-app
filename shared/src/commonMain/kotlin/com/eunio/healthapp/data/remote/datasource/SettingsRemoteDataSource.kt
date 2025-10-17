package com.eunio.healthapp.data.remote.datasource

import com.eunio.healthapp.domain.model.settings.UserSettings
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Remote data source interface for settings operations.
 * Handles cloud storage of user settings using Firestore.
 */
interface SettingsRemoteDataSource {
    
    /**
     * Retrieves user settings from remote storage.
     * 
     * @param userId The user ID to get settings for
     * @return Result containing UserSettings if found, null if not found
     */
    suspend fun getSettings(userId: String): Result<UserSettings?>
    
    /**
     * Saves user settings to remote storage.
     * Creates a new document if it doesn't exist, updates if it does.
     * 
     * @param settings The settings to save
     * @return Result indicating success or failure
     */
    suspend fun saveSettings(settings: UserSettings): Result<Unit>
    
    /**
     * Updates user settings in remote storage.
     * Only updates if the document already exists.
     * 
     * @param settings The settings to update
     * @return Result indicating success or failure
     */
    suspend fun updateSettings(settings: UserSettings): Result<Unit>
    
    /**
     * Performs a conditional update based on the last modified timestamp.
     * Used for conflict resolution during sync operations.
     * 
     * @param settings The settings to update
     * @param expectedLastModified The expected last modified timestamp
     * @return Result indicating success or failure, with conflict information if applicable
     */
    suspend fun conditionalUpdateSettings(
        settings: UserSettings,
        expectedLastModified: Long
    ): Result<Unit>
    
    /**
     * Deletes user settings from remote storage.
     * 
     * @param userId The user ID to delete settings for
     * @return Result indicating success or failure
     */
    suspend fun deleteSettings(userId: String): Result<Unit>
    
    /**
     * Clears all settings from remote storage.
     * This is typically used for account deletion or data cleanup.
     * 
     * @return Result indicating success or failure
     */
    suspend fun clearAllSettings(): Result<Unit>
    
    /**
     * Checks if settings exist for a user in remote storage.
     * 
     * @param userId The user ID to check
     * @return Result containing true if settings exist, false otherwise
     */
    suspend fun settingsExist(userId: String): Result<Boolean>
    
    /**
     * Gets the last modified timestamp for user settings from remote storage.
     * Used for conflict resolution during sync operations.
     * 
     * @param userId The user ID to get timestamp for
     * @return Result containing the timestamp in epoch milliseconds, null if not found
     */
    suspend fun getLastModifiedTimestamp(userId: String): Result<Long?>
    
    /**
     * Observes changes to user settings in real-time.
     * Uses Firestore listeners for live updates.
     * 
     * @param userId The user ID to observe settings for
     * @return Flow of UserSettings changes
     */
    fun observeSettings(userId: String): Flow<UserSettings?>
    
    /**
     * Creates a settings history entry for audit trail.
     * 
     * @param userId The user ID
     * @param settings The settings snapshot
     * @param changeType The type of change (UPDATE, RESET, EXPORT, DELETE)
     * @param deviceInfo Optional device information
     * @return Result containing the history entry ID
     */
    suspend fun createSettingsHistory(
        userId: String,
        settings: UserSettings,
        changeType: String,
        deviceInfo: String? = null
    ): Result<String>
    
    /**
     * Retrieves settings history for a user.
     * 
     * @param userId The user ID to get history for
     * @param limit Maximum number of history entries to retrieve
     * @return Result containing list of settings history entries
     */
    suspend fun getSettingsHistory(
        userId: String,
        limit: Int = 50
    ): Result<List<SettingsHistoryEntry>>
    
    /**
     * Deletes settings history for a user.
     * 
     * @param userId The user ID to delete history for
     * @return Result indicating success or failure
     */
    suspend fun deleteSettingsHistory(userId: String): Result<Unit>
    
    /**
     * Performs a batch operation to update multiple users' settings.
     * Used for administrative operations or migrations.
     * 
     * @param settingsList List of UserSettings to update
     * @return Result containing the number of successfully updated settings
     */
    suspend fun batchUpdateSettings(settingsList: List<UserSettings>): Result<Int>
    
    /**
     * Gets settings statistics for analytics.
     * 
     * @param userId The user ID to get statistics for
     * @return Result containing settings usage statistics
     */
    suspend fun getSettingsStatistics(userId: String): Result<SettingsStatistics>
    
    /**
     * Validates remote storage connectivity and permissions.
     * 
     * @return Result indicating if remote storage is accessible
     */
    suspend fun validateConnection(): Result<Unit>
    
    /**
     * Gets the current server timestamp from remote storage.
     * Used for time synchronization and conflict resolution.
     * 
     * @return Result containing the server timestamp in epoch milliseconds
     */
    suspend fun getServerTimestamp(): Result<Long>
    
    /**
     * Performs a health check on the remote storage service.
     * 
     * @return Result containing health status information
     */
    suspend fun healthCheck(): Result<RemoteStorageHealth>
}

/**
 * Data class representing a settings history entry
 */
data class SettingsHistoryEntry(
    val historyId: String,
    val userId: String,
    val settingsSnapshot: UserSettings,
    val changeType: String,
    val timestamp: Long,
    val deviceInfo: String?
)

/**
 * Data class representing settings usage statistics
 */
data class SettingsStatistics(
    val userId: String,
    val totalUpdates: Long,
    val lastUpdateTime: Long,
    val mostChangedSection: String,
    val syncFrequency: Double,
    val averageUpdateSize: Long
)

/**
 * Data class representing remote storage health status
 */
data class RemoteStorageHealth(
    val isHealthy: Boolean,
    val latency: Long,
    val lastSuccessfulOperation: Long,
    val errorCount: Int,
    val version: String
)