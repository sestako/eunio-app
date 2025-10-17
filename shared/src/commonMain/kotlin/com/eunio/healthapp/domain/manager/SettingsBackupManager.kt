package com.eunio.healthapp.domain.manager

import com.eunio.healthapp.domain.model.settings.UserSettings
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Interface for managing settings backup and restore operations.
 * Handles automatic backups, conflict resolution, and data migration.
 */
interface SettingsBackupManager {
    
    /**
     * Creates an automatic backup when settings change.
     * Called internally by SettingsManager on every settings update.
     * 
     * @param settings The settings to backup
     * @return Result containing backup ID or error
     */
    suspend fun createAutomaticBackup(settings: UserSettings): Result<Long>
    
    /**
     * Creates a manual backup requested by the user.
     * 
     * @param userId The user ID to backup settings for
     * @return Result containing backup data as JSON string
     */
    suspend fun createManualBackup(userId: String): Result<String>
    
    /**
     * Restores settings from backup data on new device login.
     * Automatically called during user authentication flow.
     * 
     * @param userId The user ID to restore settings for
     * @param backupData Optional backup data, if null will fetch from remote
     * @return Result indicating restore success or failure
     */
    suspend fun restoreOnNewDevice(userId: String, backupData: String? = null): Result<Unit>
    
    /**
     * Resolves settings conflicts with user choice options.
     * Presents conflict resolution strategies to the user.
     * 
     * @param userId The user ID with conflicting settings
     * @param localSettings The local version of settings
     * @param remoteSettings The remote version of settings
     * @param strategy The resolution strategy chosen by user
     * @return Result containing resolved settings
     */
    suspend fun resolveConflictWithUserChoice(
        userId: String,
        localSettings: UserSettings,
        remoteSettings: UserSettings,
        strategy: ConflictResolutionStrategy
    ): Result<UserSettings>
    
    /**
     * Imports settings from backup file for device migration.
     * Validates and applies imported settings with conflict resolution.
     * 
     * @param userId The user ID to import settings for
     * @param backupData The backup data as JSON string
     * @param mergeStrategy How to handle conflicts with existing settings
     * @return Result indicating import success or validation errors
     */
    suspend fun importSettings(
        userId: String,
        backupData: String,
        mergeStrategy: ImportMergeStrategy = ImportMergeStrategy.REPLACE_ALL
    ): Result<Unit>
    
    /**
     * Exports settings for data portability.
     * Creates a comprehensive export including metadata.
     * 
     * @param userId The user ID to export settings for
     * @param includeMetadata Whether to include backup metadata
     * @return Result containing export data
     */
    suspend fun exportSettings(userId: String, includeMetadata: Boolean = true): Result<String>
    
    /**
     * Gets available backup history for a user.
     * 
     * @param userId The user ID to get backup history for
     * @return Result containing list of backup metadata
     */
    suspend fun getBackupHistory(userId: String): Result<List<BackupMetadata>>
    
    /**
     * Cleans up old automatic backups to manage storage.
     * Keeps configurable number of recent backups per user.
     * 
     * @param userId The user ID to cleanup backups for
     * @param keepCount Number of recent backups to keep (default: 10)
     * @return Result indicating cleanup success
     */
    suspend fun cleanupOldBackups(userId: String, keepCount: Int = 10): Result<Unit>
    
    /**
     * Observes backup operations for UI feedback.
     * 
     * @return Flow of backup operation status
     */
    fun observeBackupOperations(): Flow<BackupOperation>
}

/**
 * Strategies for resolving settings conflicts
 */
enum class ConflictResolutionStrategy {
    /** Use the most recently modified settings */
    LAST_WRITE_WINS,
    /** Always prefer local settings */
    LOCAL_WINS,
    /** Always prefer remote settings */
    REMOTE_WINS,
    /** Merge settings field by field, preferring non-default values */
    MERGE_FIELDS,
    /** Present options to user for manual resolution */
    MANUAL_RESOLUTION
}

/**
 * Strategies for importing settings
 */
enum class ImportMergeStrategy {
    /** Replace all existing settings with imported ones */
    REPLACE_ALL,
    /** Merge imported settings with existing ones */
    MERGE_WITH_EXISTING,
    /** Only import settings that don't exist locally */
    IMPORT_MISSING_ONLY
}

/**
 * Metadata about a settings backup
 */
data class BackupMetadata(
    val backupId: Long,
    val userId: String,
    val backupType: String, // AUTOMATIC, MANUAL, EXPORT
    val createdAt: Long,
    val dataSize: Long,
    val settingsVersion: Int,
    val deviceInfo: String? = null
)

/**
 * Represents a backup operation in progress
 */
data class BackupOperation(
    val type: BackupOperationType,
    val userId: String,
    val status: BackupOperationStatus,
    val progress: Float = 0f,
    val error: String? = null
)

enum class BackupOperationType {
    AUTOMATIC_BACKUP,
    MANUAL_BACKUP,
    RESTORE,
    IMPORT,
    EXPORT,
    CLEANUP
}

enum class BackupOperationStatus {
    STARTED,
    IN_PROGRESS,
    COMPLETED,
    FAILED
}