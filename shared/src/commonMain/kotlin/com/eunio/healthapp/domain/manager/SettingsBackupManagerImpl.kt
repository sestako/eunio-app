package com.eunio.healthapp.domain.manager

import com.eunio.healthapp.data.local.datasource.SettingsLocalDataSource
import com.eunio.healthapp.data.remote.datasource.SettingsRemoteDataSource
import com.eunio.healthapp.domain.error.SettingsError
import com.eunio.healthapp.domain.model.SyncStatus
import com.eunio.healthapp.domain.model.settings.*
import com.eunio.healthapp.domain.repository.SettingsRepository
import com.eunio.healthapp.domain.util.NetworkConnectivity
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

/**
 * Implementation of SettingsBackupManager that handles automatic backups,
 * conflict resolution, and data migration for user settings.
 */
class SettingsBackupManagerImpl(
    private val settingsRepository: SettingsRepository,
    private val localDataSource: SettingsLocalDataSource,
    private val remoteDataSource: SettingsRemoteDataSource,
    private val networkConnectivity: NetworkConnectivity
) : SettingsBackupManager {
    
    private val _backupOperations = MutableStateFlow<BackupOperation?>(null)
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    override suspend fun createAutomaticBackup(settings: UserSettings): Result<Long> {
        return try {
            emitBackupOperation(
                BackupOperation(
                    type = BackupOperationType.AUTOMATIC_BACKUP,
                    userId = settings.userId,
                    status = BackupOperationStatus.STARTED
                )
            )
            
            val backupResult = localDataSource.createSettingsBackup(settings.userId, "AUTOMATIC")
            
            when {
                backupResult.isSuccess -> {
                    val backupId = backupResult.getOrThrow()
                    
                    // Cleanup old automatic backups to manage storage
                    cleanupOldBackups(settings.userId, keepCount = 10)
                    
                    emitBackupOperation(
                        BackupOperation(
                            type = BackupOperationType.AUTOMATIC_BACKUP,
                            userId = settings.userId,
                            status = BackupOperationStatus.COMPLETED,
                            progress = 1.0f
                        )
                    )
                    
                    Result.success(backupId)
                }
                else -> {
                    val error = backupResult.errorOrNull()
                    emitBackupOperation(
                        BackupOperation(
                            type = BackupOperationType.AUTOMATIC_BACKUP,
                            userId = settings.userId,
                            status = BackupOperationStatus.FAILED,
                            error = error?.message
                        )
                    )
                    Result.error(
                        SettingsError.BackupError(
                            "Failed to create automatic backup: ${error?.message}",
                            backupType = "AUTOMATIC"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            emitBackupOperation(
                BackupOperation(
                    type = BackupOperationType.AUTOMATIC_BACKUP,
                    userId = settings.userId,
                    status = BackupOperationStatus.FAILED,
                    error = e.message
                )
            )
            Result.error(
                SettingsError.BackupError(
                    "Automatic backup failed: ${e.message}",
                    backupType = "AUTOMATIC"
                )
            )
        }
    }
    
    override suspend fun createManualBackup(userId: String): Result<String> {
        return try {
            emitBackupOperation(
                BackupOperation(
                    type = BackupOperationType.MANUAL_BACKUP,
                    userId = userId,
                    status = BackupOperationStatus.STARTED
                )
            )
            
            val settingsResult = settingsRepository.getUserSettings(userId)
            when {
                settingsResult.isSuccess -> {
                    val settings = settingsResult.getOrNull()
                    if (settings != null) {
                        // Create backup in local storage
                        val backupResult = localDataSource.createSettingsBackup(userId, "MANUAL")
                        
                        when {
                            backupResult.isSuccess -> {
                                val backupJson = json.encodeToString(settings)
                                
                                emitBackupOperation(
                                    BackupOperation(
                                        type = BackupOperationType.MANUAL_BACKUP,
                                        userId = userId,
                                        status = BackupOperationStatus.COMPLETED,
                                        progress = 1.0f
                                    )
                                )
                                
                                Result.success(backupJson)
                            }
                            else -> {
                                val error = backupResult.errorOrNull()
                                emitBackupOperation(
                                    BackupOperation(
                                        type = BackupOperationType.MANUAL_BACKUP,
                                        userId = userId,
                                        status = BackupOperationStatus.FAILED,
                                        error = error?.message
                                    )
                                )
                                Result.error(
                                    SettingsError.BackupError(
                                        "Failed to create manual backup: ${error?.message}",
                                        backupType = "MANUAL"
                                    )
                                )
                            }
                        }
                    } else {
                        emitBackupOperation(
                            BackupOperation(
                                type = BackupOperationType.MANUAL_BACKUP,
                                userId = userId,
                                status = BackupOperationStatus.FAILED,
                                error = "No settings found to backup"
                            )
                        )
                        Result.error(
                            SettingsError.BackupError("No settings found to backup", backupType = "MANUAL")
                        )
                    }
                }
                else -> {
                    val error = settingsResult.errorOrNull()
                    emitBackupOperation(
                        BackupOperation(
                            type = BackupOperationType.MANUAL_BACKUP,
                            userId = userId,
                            status = BackupOperationStatus.FAILED,
                            error = error?.message
                        )
                    )
                    Result.error(
                        SettingsError.BackupError(
                            "Failed to get settings for backup: ${error?.message}",
                            "MANUAL"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            emitBackupOperation(
                BackupOperation(
                    type = BackupOperationType.MANUAL_BACKUP,
                    userId = userId,
                    status = BackupOperationStatus.FAILED,
                    error = e.message
                )
            )
            Result.error(
                SettingsError.BackupError(
                    "Manual backup failed: ${e.message}",
                    "MANUAL"
                )
            )
        }
    }
    
    override suspend fun restoreOnNewDevice(userId: String, backupData: String?): Result<Unit> {
        return try {
            emitBackupOperation(
                BackupOperation(
                    type = BackupOperationType.RESTORE,
                    userId = userId,
                    status = BackupOperationStatus.STARTED
                )
            )
            
            val dataToRestore = backupData ?: run {
                // Fetch latest backup from remote if no data provided
                if (networkConnectivity.isConnected()) {
                    val remoteSettings = remoteDataSource.getSettings(userId)
                    when {
                        remoteSettings.isSuccess -> {
                            val settings = remoteSettings.getOrNull()
                            if (settings != null) {
                                json.encodeToString(settings)
                            } else {
                                // No remote settings, create defaults
                                return createDefaultSettingsForNewDevice(userId)
                            }
                        }
                        else -> {
                            // Remote fetch failed, create defaults
                            return createDefaultSettingsForNewDevice(userId)
                        }
                    }
                } else {
                    // No connectivity, create defaults
                    return createDefaultSettingsForNewDevice(userId)
                }
            }
            
            emitBackupOperation(
                BackupOperation(
                    type = BackupOperationType.RESTORE,
                    userId = userId,
                    status = BackupOperationStatus.IN_PROGRESS,
                    progress = 0.5f
                )
            )
            
            // Parse and validate backup data
            val restoredSettings = json.decodeFromString<UserSettings>(dataToRestore)
            val validationResult = settingsRepository.validateSettings(restoredSettings)
            
            if (validationResult.isError) {
                emitBackupOperation(
                    BackupOperation(
                        type = BackupOperationType.RESTORE,
                        userId = userId,
                        status = BackupOperationStatus.FAILED,
                        error = "Invalid backup data"
                    )
                )
                return Result.error(
                    SettingsError.BackupError("Backup data contains invalid settings", backupType = "RESTORE")
                )
            }
            
            // Update user ID and timestamps for current device
            val settingsToRestore = restoredSettings.copy(
                userId = userId,
                lastModified = Clock.System.now(),
                syncStatus = SyncStatus.PENDING
            )
            
            // Save restored settings
            val saveResult = settingsRepository.saveUserSettings(settingsToRestore)
            when {
                saveResult.isSuccess -> {
                    // Create history entry for restore operation
                    if (networkConnectivity.isConnected()) {
                        remoteDataSource.createSettingsHistory(
                            userId = userId,
                            settings = settingsToRestore,
                            changeType = "RESTORE",
                            deviceInfo = "Settings restored on new device"
                        )
                    }
                    
                    emitBackupOperation(
                        BackupOperation(
                            type = BackupOperationType.RESTORE,
                            userId = userId,
                            status = BackupOperationStatus.COMPLETED,
                            progress = 1.0f
                        )
                    )
                    
                    Result.success(Unit)
                }
                else -> {
                    val error = saveResult.errorOrNull()
                    emitBackupOperation(
                        BackupOperation(
                            type = BackupOperationType.RESTORE,
                            userId = userId,
                            status = BackupOperationStatus.FAILED,
                            error = error?.message
                        )
                    )
                    Result.error(
                        SettingsError.BackupError(
                            "Failed to save restored settings: ${error?.message}",
                            "RESTORE"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            emitBackupOperation(
                BackupOperation(
                    type = BackupOperationType.RESTORE,
                    userId = userId,
                    status = BackupOperationStatus.FAILED,
                    error = e.message
                )
            )
            Result.error(
                SettingsError.BackupError(
                    "Settings restore failed: ${e.message}",
                    "RESTORE"
                )
            )
        }
    }
    
    override suspend fun resolveConflictWithUserChoice(
        userId: String,
        localSettings: UserSettings,
        remoteSettings: UserSettings,
        strategy: ConflictResolutionStrategy
    ): Result<UserSettings> {
        return try {
            val resolvedSettings = when (strategy) {
                ConflictResolutionStrategy.LAST_WRITE_WINS -> {
                    if (localSettings.lastModified > remoteSettings.lastModified) {
                        localSettings
                    } else {
                        remoteSettings
                    }
                }
                ConflictResolutionStrategy.LOCAL_WINS -> localSettings
                ConflictResolutionStrategy.REMOTE_WINS -> remoteSettings
                ConflictResolutionStrategy.MERGE_FIELDS -> {
                    mergeSettingsFields(localSettings, remoteSettings)
                }
                ConflictResolutionStrategy.MANUAL_RESOLUTION -> {
                    // This should be handled by the UI layer
                    return Result.error(
                        SettingsError.ConflictResolutionError(
                            "Manual resolution requires UI interaction"
                        )
                    )
                }
            }
            
            // Update timestamps and sync status
            val finalSettings = resolvedSettings.copy(
                lastModified = Clock.System.now(),
                syncStatus = SyncStatus.PENDING
            )
            
            // Save resolved settings
            val saveResult = settingsRepository.saveUserSettings(finalSettings)
            when {
                saveResult.isSuccess -> {
                    // Create history entry for conflict resolution
                    if (networkConnectivity.isConnected()) {
                        remoteDataSource.createSettingsHistory(
                            userId = userId,
                            settings = finalSettings,
                            changeType = "CONFLICT_RESOLVED",
                            deviceInfo = "Conflict resolved using strategy: $strategy"
                        )
                    }
                    Result.success(finalSettings)
                }
                else -> {
                    val error = saveResult.errorOrNull()
                    Result.error(
                        SettingsError.ConflictResolutionError(
                            "Failed to save resolved settings: ${error?.message}"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Result.error(
                SettingsError.ConflictResolutionError(
                    "Conflict resolution failed: ${e.message}"
                )
            )
        }
    }
    
    override suspend fun importSettings(
        userId: String,
        backupData: String,
        mergeStrategy: ImportMergeStrategy
    ): Result<Unit> {
        return try {
            emitBackupOperation(
                BackupOperation(
                    type = BackupOperationType.IMPORT,
                    userId = userId,
                    status = BackupOperationStatus.STARTED
                )
            )
            
            // Parse imported settings
            val importedSettings = json.decodeFromString<UserSettings>(backupData)
            val validationResult = settingsRepository.validateSettings(importedSettings)
            
            if (validationResult.isError) {
                emitBackupOperation(
                    BackupOperation(
                        type = BackupOperationType.IMPORT,
                        userId = userId,
                        status = BackupOperationStatus.FAILED,
                        error = "Invalid import data"
                    )
                )
                return Result.error(
                    SettingsError.BackupError("Import data contains invalid settings", backupType = "IMPORT")
                )
            }
            
            emitBackupOperation(
                BackupOperation(
                    type = BackupOperationType.IMPORT,
                    userId = userId,
                    status = BackupOperationStatus.IN_PROGRESS,
                    progress = 0.5f
                )
            )
            
            // Get existing settings for merge strategies
            val existingResult = settingsRepository.getUserSettings(userId)
            val finalSettings = when (mergeStrategy) {
                ImportMergeStrategy.REPLACE_ALL -> {
                    importedSettings.copy(
                        userId = userId,
                        lastModified = Clock.System.now(),
                        syncStatus = SyncStatus.PENDING
                    )
                }
                ImportMergeStrategy.MERGE_WITH_EXISTING -> {
                    if (existingResult.isSuccess && existingResult.getOrNull() != null) {
                        val existing = existingResult.getOrNull()!!
                        mergeSettingsFields(existing, importedSettings).copy(
                            userId = userId,
                            lastModified = Clock.System.now(),
                            syncStatus = SyncStatus.PENDING
                        )
                    } else {
                        importedSettings.copy(
                            userId = userId,
                            lastModified = Clock.System.now(),
                            syncStatus = SyncStatus.PENDING
                        )
                    }
                }
                ImportMergeStrategy.IMPORT_MISSING_ONLY -> {
                    if (existingResult.isSuccess && existingResult.getOrNull() != null) {
                        val existing = existingResult.getOrNull()!!
                        // Only import fields that are at default values in existing settings
                        mergeWithDefaults(existing, importedSettings).copy(
                            lastModified = Clock.System.now(),
                            syncStatus = SyncStatus.PENDING
                        )
                    } else {
                        importedSettings.copy(
                            userId = userId,
                            lastModified = Clock.System.now(),
                            syncStatus = SyncStatus.PENDING
                        )
                    }
                }
            }
            
            // Save imported settings
            val saveResult = settingsRepository.saveUserSettings(finalSettings)
            when {
                saveResult.isSuccess -> {
                    // Create history entry for import
                    if (networkConnectivity.isConnected()) {
                        remoteDataSource.createSettingsHistory(
                            userId = userId,
                            settings = finalSettings,
                            changeType = "IMPORT",
                            deviceInfo = "Settings imported using strategy: $mergeStrategy"
                        )
                    }
                    
                    emitBackupOperation(
                        BackupOperation(
                            type = BackupOperationType.IMPORT,
                            userId = userId,
                            status = BackupOperationStatus.COMPLETED,
                            progress = 1.0f
                        )
                    )
                    
                    Result.success(Unit)
                }
                else -> {
                    val error = saveResult.errorOrNull()
                    emitBackupOperation(
                        BackupOperation(
                            type = BackupOperationType.IMPORT,
                            userId = userId,
                            status = BackupOperationStatus.FAILED,
                            error = error?.message
                        )
                    )
                    Result.error(
                        SettingsError.BackupError(
                            "Failed to save imported settings: ${error?.message}",
                            "IMPORT"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            emitBackupOperation(
                BackupOperation(
                    type = BackupOperationType.IMPORT,
                    userId = userId,
                    status = BackupOperationStatus.FAILED,
                    error = e.message
                )
            )
            Result.error(
                SettingsError.BackupError(
                    "Settings import failed: ${e.message}",
                    "IMPORT"
                )
            )
        }
    }
    
    override suspend fun exportSettings(userId: String, includeMetadata: Boolean): Result<String> {
        return try {
            emitBackupOperation(
                BackupOperation(
                    type = BackupOperationType.EXPORT,
                    userId = userId,
                    status = BackupOperationStatus.STARTED
                )
            )
            
            val settingsResult = settingsRepository.getUserSettings(userId)
            when {
                settingsResult.isSuccess -> {
                    val settings = settingsResult.getOrNull()
                    if (settings != null) {
                        val exportData = if (includeMetadata) {
                            createExportWithMetadata(settings)
                        } else {
                            json.encodeToString(settings)
                        }
                        
                        // Create backup entry for export
                        localDataSource.createSettingsBackup(userId, "EXPORT")
                        
                        // Create history entry if connected
                        if (networkConnectivity.isConnected()) {
                            remoteDataSource.createSettingsHistory(
                                userId = userId,
                                settings = settings,
                                changeType = "EXPORT",
                                deviceInfo = "Settings exported by user"
                            )
                        }
                        
                        emitBackupOperation(
                            BackupOperation(
                                type = BackupOperationType.EXPORT,
                                userId = userId,
                                status = BackupOperationStatus.COMPLETED,
                                progress = 1.0f
                            )
                        )
                        
                        Result.success(exportData)
                    } else {
                        emitBackupOperation(
                            BackupOperation(
                                type = BackupOperationType.EXPORT,
                                userId = userId,
                                status = BackupOperationStatus.FAILED,
                                error = "No settings found to export"
                            )
                        )
                        Result.error(
                            SettingsError.ExportError("No settings found to export", exportType = "USER_DATA")
                        )
                    }
                }
                else -> {
                    val error = settingsResult.errorOrNull()
                    emitBackupOperation(
                        BackupOperation(
                            type = BackupOperationType.EXPORT,
                            userId = userId,
                            status = BackupOperationStatus.FAILED,
                            error = error?.message
                        )
                    )
                    Result.error(
                        SettingsError.ExportError(
                            "Failed to get settings for export: ${error?.message}",
                            exportType = "USER_DATA"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            emitBackupOperation(
                BackupOperation(
                    type = BackupOperationType.EXPORT,
                    userId = userId,
                    status = BackupOperationStatus.FAILED,
                    error = e.message
                )
            )
            Result.error(
                SettingsError.ExportError(
                    "Settings export failed: ${e.message}",
                    exportType = "USER_DATA"
                )
            )
        }
    }
    
    override suspend fun getBackupHistory(userId: String): Result<List<BackupMetadata>> {
        return try {
            val backupsResult = localDataSource.getUserBackups(userId)
            when {
                backupsResult.isSuccess -> {
                    val backups = backupsResult.getOrThrow()
                    val metadata = backups.map { backup ->
                        BackupMetadata(
                            backupId = backup.backupId,
                            userId = backup.userId,
                            backupType = backup.backupType,
                            createdAt = backup.createdAt,
                            dataSize = backup.dataSize,
                            settingsVersion = 1, // Current version
                            deviceInfo = null
                        )
                    }
                    Result.success(metadata)
                }
                else -> {
                    val error = backupsResult.errorOrNull()
                    Result.error(
                        SettingsError.BackupError(
                            "Failed to get backup history: ${error?.message}",
                            "HISTORY"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Result.error(
                SettingsError.BackupError(
                    "Failed to get backup history: ${e.message}",
                    "HISTORY"
                )
            )
        }
    }
    
    override suspend fun cleanupOldBackups(userId: String, keepCount: Int): Result<Unit> {
        return try {
            emitBackupOperation(
                BackupOperation(
                    type = BackupOperationType.CLEANUP,
                    userId = userId,
                    status = BackupOperationStatus.STARTED
                )
            )
            
            val backupsResult = localDataSource.getUserBackups(userId)
            when {
                backupsResult.isSuccess -> {
                    val backups = backupsResult.getOrThrow()
                    val automaticBackups = backups.filter { it.backupType == "AUTOMATIC" }
                    
                    if (automaticBackups.size > keepCount) {
                        // Sort by creation time and keep only the most recent ones
                        val backupsToDelete = automaticBackups
                            .sortedByDescending { it.createdAt }
                            .drop(keepCount)
                        
                        var deletedCount = 0
                        for (backup in backupsToDelete) {
                            val deleteResult = localDataSource.deleteSettingsBackup(backup.backupId)
                            if (deleteResult.isSuccess) {
                                deletedCount++
                            }
                            
                            emitBackupOperation(
                                BackupOperation(
                                    type = BackupOperationType.CLEANUP,
                                    userId = userId,
                                    status = BackupOperationStatus.IN_PROGRESS,
                                    progress = deletedCount.toFloat() / backupsToDelete.size
                                )
                            )
                        }
                    }
                    
                    emitBackupOperation(
                        BackupOperation(
                            type = BackupOperationType.CLEANUP,
                            userId = userId,
                            status = BackupOperationStatus.COMPLETED,
                            progress = 1.0f
                        )
                    )
                    
                    Result.success(Unit)
                }
                else -> {
                    val error = backupsResult.errorOrNull()
                    emitBackupOperation(
                        BackupOperation(
                            type = BackupOperationType.CLEANUP,
                            userId = userId,
                            status = BackupOperationStatus.FAILED,
                            error = error?.message
                        )
                    )
                    Result.error(
                        SettingsError.BackupError(
                            "Failed to cleanup old backups: ${error?.message}",
                            "CLEANUP"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            emitBackupOperation(
                BackupOperation(
                    type = BackupOperationType.CLEANUP,
                    userId = userId,
                    status = BackupOperationStatus.FAILED,
                    error = e.message
                )
            )
            Result.error(
                SettingsError.BackupError(
                    "Backup cleanup failed: ${e.message}",
                    "CLEANUP"
                )
            )
        }
    }
    
    override fun observeBackupOperations(): Flow<BackupOperation> {
        return _backupOperations.asStateFlow().mapNotNull { it }
    }
    
    // Private helper methods
    
    private suspend fun createDefaultSettingsForNewDevice(userId: String): Result<Unit> {
        return try {
            val defaultSettings = UserSettings.createDefault(userId)
            val saveResult = settingsRepository.saveUserSettings(defaultSettings)
            
            when {
                saveResult.isSuccess -> {
                    emitBackupOperation(
                        BackupOperation(
                            type = BackupOperationType.RESTORE,
                            userId = userId,
                            status = BackupOperationStatus.COMPLETED,
                            progress = 1.0f
                        )
                    )
                    Result.success(Unit)
                }
                else -> {
                    val error = saveResult.errorOrNull()
                    emitBackupOperation(
                        BackupOperation(
                            type = BackupOperationType.RESTORE,
                            userId = userId,
                            status = BackupOperationStatus.FAILED,
                            error = error?.message
                        )
                    )
                    Result.error(
                        SettingsError.BackupError(
                            "Failed to create default settings: ${error?.message}",
                            "RESTORE"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            emitBackupOperation(
                BackupOperation(
                    type = BackupOperationType.RESTORE,
                    userId = userId,
                    status = BackupOperationStatus.FAILED,
                    error = e.message
                )
            )
            Result.error(
                SettingsError.BackupError(
                    "Failed to create default settings: ${e.message}",
                    "RESTORE"
                )
            )
        }
    }
    
    private fun mergeSettingsFields(local: UserSettings, remote: UserSettings): UserSettings {
        // Merge strategy: prefer non-default values, use most recent for conflicts
        return UserSettings(
            userId = local.userId,
            unitPreferences = if (remote.unitPreferences.isManuallySet) {
                remote.unitPreferences
            } else {
                local.unitPreferences
            },
            notificationPreferences = mergeNotificationPreferences(
                local.notificationPreferences,
                remote.notificationPreferences
            ),
            cyclePreferences = if (remote.cyclePreferences.isCustomized) {
                remote.cyclePreferences
            } else {
                local.cyclePreferences
            },
            privacyPreferences = mergePrivacyPreferences(
                local.privacyPreferences,
                remote.privacyPreferences
            ),
            displayPreferences = mergeDisplayPreferences(
                local.displayPreferences,
                remote.displayPreferences
            ),
            syncPreferences = mergeSyncPreferences(
                local.syncPreferences,
                remote.syncPreferences
            ),
            lastModified = maxOf(local.lastModified, remote.lastModified),
            syncStatus = SyncStatus.PENDING,
            version = maxOf(local.version, remote.version)
        )
    }
    
    private fun mergeWithDefaults(existing: UserSettings, imported: UserSettings): UserSettings {
        val defaults = UserSettings.createDefault(existing.userId)
        
        return existing.copy(
            unitPreferences = if (existing.unitPreferences == defaults.unitPreferences) {
                imported.unitPreferences
            } else {
                existing.unitPreferences
            },
            notificationPreferences = if (existing.notificationPreferences == defaults.notificationPreferences) {
                imported.notificationPreferences
            } else {
                existing.notificationPreferences
            },
            cyclePreferences = if (existing.cyclePreferences == defaults.cyclePreferences) {
                imported.cyclePreferences
            } else {
                existing.cyclePreferences
            },
            privacyPreferences = if (existing.privacyPreferences == defaults.privacyPreferences) {
                imported.privacyPreferences
            } else {
                existing.privacyPreferences
            },
            displayPreferences = if (existing.displayPreferences == defaults.displayPreferences) {
                imported.displayPreferences
            } else {
                existing.displayPreferences
            },
            syncPreferences = if (existing.syncPreferences == defaults.syncPreferences) {
                imported.syncPreferences
            } else {
                existing.syncPreferences
            }
        )
    }
    
    private fun createExportWithMetadata(settings: UserSettings): String {
        @Serializable
        data class ExportData(
            val exportVersion: Int,
            val exportedAt: Long,
            val settingsVersion: Int,
            val settings: UserSettings
        )
        
        val exportData = ExportData(
            exportVersion = 1,
            exportedAt = Clock.System.now().toEpochMilliseconds(),
            settingsVersion = settings.version,
            settings = settings
        )
        return json.encodeToString(exportData)
    }
    
    private fun emitBackupOperation(operation: BackupOperation) {
        _backupOperations.value = operation
    }
    
    // Helper methods for merging specific preference sections
    private fun mergeNotificationPreferences(
        local: NotificationPreferences,
        remote: NotificationPreferences
    ): NotificationPreferences {
        return NotificationPreferences(
            dailyLoggingReminder = if (remote.dailyLoggingReminder.enabled) {
                remote.dailyLoggingReminder
            } else {
                local.dailyLoggingReminder
            },
            periodPredictionAlert = if (remote.periodPredictionAlert.enabled) {
                remote.periodPredictionAlert
            } else {
                local.periodPredictionAlert
            },
            ovulationAlert = if (remote.ovulationAlert.enabled) {
                remote.ovulationAlert
            } else {
                local.ovulationAlert
            },
            insightNotifications = if (remote.insightNotifications.enabled) {
                remote.insightNotifications
            } else {
                local.insightNotifications
            },
            globalNotificationsEnabled = remote.globalNotificationsEnabled || local.globalNotificationsEnabled
        )
    }
    
    private fun mergePrivacyPreferences(
        local: PrivacyPreferences,
        remote: PrivacyPreferences
    ): PrivacyPreferences {
        // For privacy, prefer more restrictive settings
        return PrivacyPreferences(
            dataSharingEnabled = local.dataSharingEnabled && remote.dataSharingEnabled,
            anonymousInsightsEnabled = local.anonymousInsightsEnabled && remote.anonymousInsightsEnabled,
            crashReportingEnabled = local.crashReportingEnabled && remote.crashReportingEnabled,
            analyticsEnabled = local.analyticsEnabled && remote.analyticsEnabled
        )
    }
    
    private fun mergeDisplayPreferences(
        local: DisplayPreferences,
        remote: DisplayPreferences
    ): DisplayPreferences {
        return DisplayPreferences(
            textSizeScale = if (remote.textSizeScale != 1.0f) remote.textSizeScale else local.textSizeScale,
            highContrastMode = remote.highContrastMode || local.highContrastMode,
            hapticFeedbackEnabled = remote.hapticFeedbackEnabled || local.hapticFeedbackEnabled,
            hapticIntensity = if (remote.hapticIntensity != HapticIntensity.MEDIUM) {
                remote.hapticIntensity
            } else {
                local.hapticIntensity
            }
        )
    }
    
    private fun mergeSyncPreferences(
        local: SyncPreferences,
        remote: SyncPreferences
    ): SyncPreferences {
        return SyncPreferences(
            autoSyncEnabled = remote.autoSyncEnabled || local.autoSyncEnabled,
            wifiOnlySync = remote.wifiOnlySync || local.wifiOnlySync,
            cloudBackupEnabled = remote.cloudBackupEnabled || local.cloudBackupEnabled,
            lastSyncTime = maxOf(
                local.lastSyncTime ?: kotlinx.datetime.Instant.DISTANT_PAST,
                remote.lastSyncTime ?: kotlinx.datetime.Instant.DISTANT_PAST
            )
        )
    }
}

