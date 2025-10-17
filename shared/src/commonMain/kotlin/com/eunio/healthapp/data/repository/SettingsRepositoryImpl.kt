package com.eunio.healthapp.data.repository

import com.eunio.healthapp.data.local.datasource.SettingsLocalDataSource
import com.eunio.healthapp.data.remote.datasource.SettingsRemoteDataSource
import com.eunio.healthapp.domain.error.SettingsError
import com.eunio.healthapp.domain.error.SettingsErrorHandler
import com.eunio.healthapp.domain.model.SyncStatus
import com.eunio.healthapp.domain.model.settings.UserSettings
import com.eunio.healthapp.domain.repository.SettingsRepository
import com.eunio.healthapp.domain.repository.UserRepository
import com.eunio.healthapp.domain.util.NetworkConnectivity
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Implementation of SettingsRepository with offline-first strategy.
 * Handles settings persistence, synchronization, backup, and validation.
 */
class SettingsRepositoryImpl(
    private val localDataSource: SettingsLocalDataSource,
    private val remoteDataSource: SettingsRemoteDataSource,
    private val networkConnectivity: NetworkConnectivity,
    private val userRepository: UserRepository,
    private val errorHandler: SettingsErrorHandler
) : SettingsRepository {
    
    private val _syncStatus = MutableStateFlow(false)
    private val syncStatus: StateFlow<Boolean> = _syncStatus.asStateFlow()
    
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    override suspend fun getUserSettings(): Result<UserSettings?> {
        return try {
            val currentUser = userRepository.getCurrentUser()
            when {
                currentUser.isSuccess -> {
                    val user = currentUser.getOrNull()
                    if (user != null) {
                        getUserSettings(user.id)
                    } else {
                        Result.success(null)
                    }
                }
                else -> {
                    val error = currentUser.errorOrNull()
                    Result.error(SettingsError.SyncError("Failed to get current user: ${error?.message}"))
                }
            }
        } catch (e: Exception) {
            Result.error(errorHandler.handleSettingsError(e, "getUserSettings"))
        }
    }
    
    override suspend fun getUserSettings(userId: String): Result<UserSettings?> {
        return try {
            // Offline-first strategy: try local first
            val localSettings = localDataSource.getSettings(userId)
            
            if (localSettings != null) {
                // Check if we should sync with remote
                if (networkConnectivity.isConnected() && shouldSyncSettings(localSettings)) {
                    syncSettingsInBackground(userId)
                }
                return Result.success(localSettings)
            }
            
            // No local settings, try remote if connected
            if (networkConnectivity.isConnected()) {
                val remoteResult = remoteDataSource.getSettings(userId)
                when {
                    remoteResult.isSuccess -> {
                        val remoteSettings = remoteResult.getOrNull()
                        if (remoteSettings != null) {
                            // Cache locally for offline access
                            localDataSource.saveSettings(remoteSettings.markAsSynced())
                            return Result.success(remoteSettings)
                        }
                    }
                    else -> {
                        // Remote failed, but we don't have local data either
                        val error = remoteResult.errorOrNull()
                        return Result.error(error ?: SettingsError.SyncError("Failed to get remote settings"))
                    }
                }
            }
            
            // No settings found anywhere
            Result.success(null)
        } catch (e: Exception) {
            Result.error(errorHandler.handleSettingsError(e, "getUserSettings"))
        }
    }
    
    override suspend fun saveUserSettings(settings: UserSettings): Result<Unit> {
        return try {
            // Validate settings before saving
            val validationResult = validateSettings(settings)
            if (validationResult.isError) {
                return validationResult
            }
            
            val settingsToSave = settings.withUpdate()
            
            // Always save locally first (offline-first)
            val localResult = localDataSource.saveSettings(settingsToSave)
            if (localResult.isError) {
                return localResult
            }
            
            // Try to sync to remote if connected
            if (networkConnectivity.isConnected()) {
                val remoteResult = remoteDataSource.saveSettings(settingsToSave)
                when {
                    remoteResult.isSuccess -> {
                        // Mark as synced in local storage
                        localDataSource.markAsSynced(settings.userId)
                        _syncStatus.value = true
                    }
                    else -> {
                        // Remote save failed, but local succeeded
                        // Settings will be synced later when connectivity is restored
                        _syncStatus.value = false
                    }
                }
            } else {
                _syncStatus.value = false
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(errorHandler.handleSettingsError(e, "saveUserSettings"))
        }
    }
    
    override suspend fun updateUserSettings(
        userId: String,
        updateFunction: (UserSettings) -> UserSettings
    ): Result<UserSettings> {
        return try {
            val currentSettings = getUserSettings(userId)
            when {
                currentSettings.isSuccess -> {
                    val settings = currentSettings.getOrNull()
                        ?: UserSettings.createDefault(userId)
                    
                    val updatedSettings = updateFunction(settings)
                    val saveResult = saveUserSettings(updatedSettings)
                    
                    when {
                        saveResult.isSuccess -> Result.success(updatedSettings)
                        else -> {
                            val error = saveResult.errorOrNull()
                            Result.error(error ?: SettingsError.SyncError("Failed to save updated settings"))
                        }
                    }
                }
                else -> {
                    val error = currentSettings.errorOrNull()
                    Result.error(error ?: SettingsError.SyncError("Failed to get current settings"))
                }
            }
        } catch (e: Exception) {
            Result.error(errorHandler.handleSettingsError(e, "updateUserSettings"))
        }
    }
    
    override suspend fun syncSettings(): Result<Unit> {
        return try {
            val currentUser = userRepository.getCurrentUser()
            when {
                currentUser.isSuccess -> {
                    val user = currentUser.getOrNull()
                    if (user != null) {
                        syncSettings(user.id)
                    } else {
                        Result.error(SettingsError.SyncError("No current user found"))
                    }
                }
                else -> {
                    val error = currentUser.errorOrNull()
                    Result.error(SettingsError.SyncError("Failed to get current user: ${error?.message}"))
                }
            }
        } catch (e: Exception) {
            Result.error(errorHandler.handleSettingsError(e, "syncSettings"))
        }
    }
    
    override suspend fun syncSettings(userId: String): Result<Unit> {
        return try {
            if (!networkConnectivity.isConnected()) {
                return Result.error(SettingsError.SyncError("No network connection available"))
            }
            
            _syncStatus.value = true
            
            val localSettings = localDataSource.getSettings(userId)
            val remoteResult = remoteDataSource.getSettings(userId)
            
            val syncResult = when {
                remoteResult.isSuccess -> {
                    val remoteSettings = remoteResult.getOrNull()
                    
                    when {
                        localSettings == null && remoteSettings == null -> {
                            // No settings anywhere, nothing to sync
                            Result.success(Unit)
                        }
                        localSettings == null && remoteSettings != null -> {
                            // Only remote settings exist, save locally
                            localDataSource.saveSettings(remoteSettings.markAsSynced())
                            Result.success(Unit)
                        }
                        localSettings != null && remoteSettings == null -> {
                            // Only local settings exist, save remotely
                            val saveResult = remoteDataSource.saveSettings(localSettings)
                            when {
                                saveResult.isSuccess -> {
                                    localDataSource.markAsSynced(userId)
                                    Result.success(Unit)
                                }
                                else -> {
                                    val error = saveResult.errorOrNull()
                                    Result.error(error ?: SettingsError.SyncError("Failed to save to remote"))
                                }
                            }
                        }
                        localSettings != null && remoteSettings != null -> {
                            // Both exist, resolve conflicts
                            val conflictResult = resolveSettingsConflict(userId, localSettings, remoteSettings)
                            when {
                                conflictResult.isSuccess -> Result.success(Unit)
                                else -> {
                                    val error = conflictResult.errorOrNull()
                                    Result.error(error ?: SettingsError.SyncError("Failed to resolve conflict"))
                                }
                            }
                        }
                        else -> {
                            Result.success(Unit)
                        }
                    }
                }
                else -> {
                    val error = remoteResult.errorOrNull()
                    Result.error(error ?: SettingsError.SyncError("Failed to get remote settings"))
                }
            }
            
            syncResult
        } catch (e: Exception) {
            Result.error(errorHandler.handleSettingsError(e, "syncSettings"))
        } finally {
            _syncStatus.value = false
        }
    }
    
    override suspend fun resolveSettingsConflict(
        userId: String,
        localSettings: UserSettings,
        remoteSettings: UserSettings
    ): Result<UserSettings> {
        return try {
            // Use last modified timestamp to resolve conflicts
            val resolvedSettings = if (localSettings.lastModified > remoteSettings.lastModified) {
                // Local is newer, use local and update remote
                val saveResult = remoteDataSource.saveSettings(localSettings)
                when {
                    saveResult.isSuccess -> {
                        localDataSource.markAsSynced(userId)
                        localSettings.markAsSynced()
                    }
                    else -> {
                        return Result.error(SettingsError.ConflictResolutionError(
                            "Failed to save local settings to remote during conflict resolution"
                        ))
                    }
                }
            } else {
                // Remote is newer, use remote and update local
                val saveResult = localDataSource.saveSettings(remoteSettings.markAsSynced())
                when {
                    saveResult.isSuccess -> remoteSettings.markAsSynced()
                    else -> {
                        return Result.error(SettingsError.ConflictResolutionError(
                            "Failed to save remote settings to local during conflict resolution"
                        ))
                    }
                }
            }
            
            Result.success(resolvedSettings)
        } catch (e: Exception) {
            Result.error(errorHandler.handleSettingsError(e, "resolveSettingsConflict"))
        }
    } 
   
    override suspend fun backupUserSettings(userId: String): Result<String> {
        return try {
            val settings = getUserSettings(userId)
            when {
                settings.isSuccess -> {
                    val userSettings = settings.getOrNull()
                    if (userSettings != null) {
                        // Create backup in local storage
                        val backupResult = localDataSource.createSettingsBackup(userId, "MANUAL")
                        when {
                            backupResult.isSuccess -> {
                                val backupJson = json.encodeToString(userSettings)
                                Result.success(backupJson)
                            }
                            else -> {
                                val error = backupResult.errorOrNull()
                                Result.error(SettingsError.BackupError(
                                    "Failed to create backup: ${error?.message}",
                                    "MANUAL"
                                ))
                            }
                        }
                    } else {
                        Result.error(SettingsError.BackupError("No settings found to backup", "MANUAL"))
                    }
                }
                else -> {
                    val error = settings.errorOrNull()
                    Result.error(SettingsError.BackupError(
                        "Failed to get settings for backup: ${error?.message}",
                        "MANUAL"
                    ))
                }
            }
        } catch (e: Exception) {
            Result.error(errorHandler.handleBackupError("Failed to backup settings: ${e.message}", "MANUAL", e))
        }
    }
    
    override suspend fun restoreUserSettings(userId: String, backupData: String): Result<Unit> {
        return try {
            // Parse backup data
            val restoredSettings = json.decodeFromString<UserSettings>(backupData)
            
            // Validate restored settings
            val validationResult = validateSettings(restoredSettings)
            if (validationResult.isError) {
                return Result.error(SettingsError.BackupError(
                    "Backup data contains invalid settings",
                    "RESTORE"
                ))
            }
            
            // Update user ID to current user (in case backup is from different user)
            val settingsToRestore = restoredSettings.copy(
                userId = userId,
                lastModified = Clock.System.now(),
                syncStatus = SyncStatus.PENDING
            )
            
            // Save restored settings
            val saveResult = saveUserSettings(settingsToRestore)
            when {
                saveResult.isSuccess -> {
                    // Create history entry for restore operation
                    if (networkConnectivity.isConnected()) {
                        remoteDataSource.createSettingsHistory(
                            userId = userId,
                            settings = settingsToRestore,
                            changeType = "RESTORE",
                            deviceInfo = "Settings restored from backup"
                        )
                    }
                    Result.success(Unit)
                }
                else -> {
                    val error = saveResult.errorOrNull()
                    Result.error(SettingsError.BackupError(
                        "Failed to save restored settings: ${error?.message}",
                        "RESTORE"
                    ))
                }
            }
        } catch (e: Exception) {
            Result.error(errorHandler.handleBackupError("Failed to restore settings: ${e.message}", "RESTORE", e))
        }
    }
    
    override suspend fun exportUserData(userId: String): Result<String> {
        return try {
            val settings = getUserSettings(userId)
            when {
                settings.isSuccess -> {
                    val userSettings = settings.getOrNull()
                    if (userSettings != null) {
                        // Create comprehensive export data
                        val exportData = createExportData(userSettings)
                        
                        // Create backup entry for export
                        localDataSource.createSettingsBackup(userId, "EXPORT")
                        
                        // Create history entry if connected
                        if (networkConnectivity.isConnected()) {
                            remoteDataSource.createSettingsHistory(
                                userId = userId,
                                settings = userSettings,
                                changeType = "EXPORT",
                                deviceInfo = "Data exported by user"
                            )
                        }
                        
                        Result.success(exportData)
                    } else {
                        Result.error(SettingsError.ExportError("No settings found to export", "USER_DATA"))
                    }
                }
                else -> {
                    val error = settings.errorOrNull()
                    Result.error(SettingsError.ExportError(
                        "Failed to get settings for export: ${error?.message}",
                        "USER_DATA"
                    ))
                }
            }
        } catch (e: Exception) {
            Result.error(errorHandler.handleSettingsError(e, "exportUserData"))
        }
    }
    
    override suspend fun deleteUserSettings(userId: String): Result<Unit> {
        return try {
            // Create history entry before deletion if connected
            val currentSettings = getUserSettings(userId)
            if (networkConnectivity.isConnected() && currentSettings.isSuccess) {
                currentSettings.getOrNull()?.let { settings ->
                    remoteDataSource.createSettingsHistory(
                        userId = userId,
                        settings = settings,
                        changeType = "DELETE",
                        deviceInfo = "Settings deleted by user"
                    )
                }
            }
            
            // Delete from local storage
            val localResult = localDataSource.deleteSettings(userId)
            
            // Delete from remote storage if connected
            if (networkConnectivity.isConnected()) {
                val remoteResult = remoteDataSource.deleteSettings(userId)
                // Continue even if remote deletion fails
            }
            
            // Delete all backups for the user
            localDataSource.deleteUserBackups(userId)
            
            when {
                localResult.isSuccess -> Result.success(Unit)
                else -> {
                    val error = localResult.errorOrNull()
                    Result.error(SettingsError.SyncError("Failed to delete local settings: ${error?.message}"))
                }
            }
        } catch (e: Exception) {
            Result.error(errorHandler.handleSettingsError(e, "deleteUserSettings"))
        }
    }
    
    override suspend fun clearLocalSettings(): Result<Unit> {
        return try {
            val result = localDataSource.clearAllSettings()
            when {
                result.isSuccess -> Result.success(Unit)
                else -> {
                    val error = result.errorOrNull()
                    Result.error(SettingsError.PersistenceError(
                        "Failed to clear local settings: ${error?.message}",
                        "clearLocalSettings"
                    ))
                }
            }
        } catch (e: Exception) {
            Result.error(errorHandler.handleSettingsError(e, "clearLocalSettings"))
        }
    }
    
    override suspend fun resetToDefaults(userId: String, locale: String?): Result<UserSettings> {
        return try {
            val defaultSettings = if (locale != null) {
                UserSettings.createDefault(userId, locale)
            } else {
                UserSettings.createDefault(userId)
            }
            
            val saveResult = saveUserSettings(defaultSettings)
            when {
                saveResult.isSuccess -> {
                    // Create history entry for reset if connected
                    if (networkConnectivity.isConnected()) {
                        remoteDataSource.createSettingsHistory(
                            userId = userId,
                            settings = defaultSettings,
                            changeType = "RESET",
                            deviceInfo = "Settings reset to defaults"
                        )
                    }
                    Result.success(defaultSettings)
                }
                else -> {
                    val error = saveResult.errorOrNull()
                    Result.error(SettingsError.SyncError("Failed to save default settings: ${error?.message}"))
                }
            }
        } catch (e: Exception) {
            Result.error(errorHandler.handleSettingsError(e, "resetToDefaults"))
        }
    }
    
    override fun observeUserSettings(userId: String): Flow<UserSettings?> {
        return combine(
            localDataSource.observeSettings(userId),
            networkConnectivity.observeConnectivity()
        ) { localSettings, isConnected ->
            // Trigger sync if connected and settings need sync
            if (isConnected && localSettings?.needsSync() == true) {
                // Launch sync in background (fire and forget)
                kotlinx.coroutines.GlobalScope.launch {
                    syncSettings(userId)
                }
            }
            localSettings
        }.distinctUntilChanged()
    }
    
    override fun observeSyncStatus(): Flow<Boolean> {
        return syncStatus
    }
    
    override suspend fun getPendingSyncSettings(): Result<List<UserSettings>> {
        return try {
            val pendingSettings = localDataSource.getPendingSyncSettings()
            Result.success(pendingSettings)
        } catch (e: Exception) {
            Result.error(errorHandler.handleSettingsError(e, "getPendingSyncSettings"))
        }
    }
    
    override suspend fun markAsSynced(userId: String): Result<Unit> {
        return try {
            val result = localDataSource.markAsSynced(userId)
            when {
                result.isSuccess -> Result.success(Unit)
                else -> {
                    val error = result.errorOrNull()
                    Result.error(SettingsError.SyncError("Failed to mark as synced: ${error?.message}"))
                }
            }
        } catch (e: Exception) {
            Result.error(errorHandler.handleSettingsError(e, "markAsSynced"))
        }
    }
    
    override suspend fun markAsSyncFailed(userId: String): Result<Unit> {
        return try {
            val result = localDataSource.markAsSyncFailed(userId)
            when {
                result.isSuccess -> Result.success(Unit)
                else -> {
                    val error = result.errorOrNull()
                    Result.error(SettingsError.SyncError("Failed to mark as sync failed: ${error?.message}"))
                }
            }
        } catch (e: Exception) {
            Result.error(errorHandler.handleSettingsError(e, "markAsSyncFailed"))
        }
    }
    
    override suspend fun settingsExist(userId: String): Result<Boolean> {
        return try {
            val localExists = localDataSource.settingsExist(userId)
            
            if (localExists) {
                Result.success(true)
            } else if (networkConnectivity.isConnected()) {
                // Check remote if not found locally
                val remoteResult = remoteDataSource.settingsExist(userId)
                when {
                    remoteResult.isSuccess -> remoteResult
                    else -> Result.success(false) // Assume doesn't exist if remote check fails
                }
            } else {
                Result.success(false)
            }
        } catch (e: Exception) {
            Result.error(errorHandler.handleSettingsError(e, "settingsExist"))
        }
    }
    
    override suspend fun getLastModifiedTimestamp(userId: String): Result<Long?> {
        return try {
            val localTimestamp = localDataSource.getLastModifiedTimestamp(userId)
            
            if (localTimestamp != null) {
                Result.success(localTimestamp)
            } else if (networkConnectivity.isConnected()) {
                // Check remote if not found locally
                val remoteResult = remoteDataSource.getLastModifiedTimestamp(userId)
                when {
                    remoteResult.isSuccess -> remoteResult
                    else -> Result.success(null)
                }
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.error(errorHandler.handleSettingsError(e, "getLastModifiedTimestamp"))
        }
    }
    
    override suspend fun validateSettings(settings: UserSettings): Result<Unit> {
        return try {
            if (!settings.isValid()) {
                val errors = settings.getValidationErrors()
                return Result.error(SettingsError.ValidationError(
                    "Settings validation failed: ${errors.joinToString(", ")}"
                ))
            }
            
            // Additional business logic validation
            if (settings.cyclePreferences.averageCycleLength < 21 || 
                settings.cyclePreferences.averageCycleLength > 45) {
                return Result.error(SettingsError.ValidationError(
                    "Average cycle length must be between 21 and 45 days",
                    "averageCycleLength",
                    settings.cyclePreferences.averageCycleLength
                ))
            }
            
            if (settings.cyclePreferences.averageLutealPhaseLength < 10 || 
                settings.cyclePreferences.averageLutealPhaseLength > 16) {
                return Result.error(SettingsError.ValidationError(
                    "Average luteal phase length must be between 10 and 16 days",
                    "averageLutealPhaseLength",
                    settings.cyclePreferences.averageLutealPhaseLength
                ))
            }
            
            if (settings.displayPreferences.textSizeScale < 0.5f || 
                settings.displayPreferences.textSizeScale > 3.0f) {
                return Result.error(SettingsError.ValidationError(
                    "Text size scale must be between 0.5 and 3.0",
                    "textSizeScale",
                    settings.displayPreferences.textSizeScale
                ))
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(errorHandler.handleValidationError("Settings validation failed: ${e.message}", cause = e))
        }
    }
    
    override suspend fun migrateSettings(settings: UserSettings, targetVersion: Int): Result<UserSettings> {
        return try {
            if (settings.version == targetVersion) {
                return Result.success(settings)
            }
            
            if (settings.version > targetVersion) {
                return Result.error(SettingsError.MigrationError(
                    "Cannot migrate to older version",
                    settings.version,
                    targetVersion
                ))
            }
            
            var migratedSettings = settings
            
            // Perform version-specific migrations
            for (version in (settings.version + 1)..targetVersion) {
                migratedSettings = when (version) {
                    1 -> {
                        // Migration to version 1 (current version)
                        migratedSettings.copy(version = 1)
                    }
                    else -> {
                        return Result.error(SettingsError.MigrationError(
                            "Unknown target version: $version",
                            settings.version,
                            targetVersion
                        ))
                    }
                }
            }
            
            Result.success(migratedSettings)
        } catch (e: Exception) {
            Result.error(errorHandler.handleMigrationError(
                "Settings migration failed: ${e.message}",
                settings.version,
                targetVersion,
                e
            ))
        }
    }
    
    // Private helper methods
    
    private suspend fun shouldSyncSettings(settings: UserSettings): Boolean {
        return settings.needsSync() || 
               (Clock.System.now().toEpochMilliseconds() - settings.lastModified.toEpochMilliseconds()) > SYNC_INTERVAL_MS
    }
    
    private suspend fun syncSettingsInBackground(userId: String) {
        try {
            syncSettings(userId)
        } catch (e: Exception) {
            // Log error but don't propagate - this is background sync
        }
    }
    
    private fun createExportData(settings: UserSettings): String {
        return try {
            val exportMap = mapOf(
                "exportInfo" to mapOf(
                    "exportDate" to Clock.System.now().toString(),
                    "appVersion" to "1.0.0",
                    "dataVersion" to settings.version
                ),
                "userSettings" to json.encodeToString(UserSettings.serializer(), settings),
                "metadata" to mapOf(
                    "hasCustomizations" to settings.hasCustomizations(),
                    "lastModified" to settings.lastModified.toString(),
                    "syncStatus" to settings.syncStatus.name
                )
            )
            
            json.encodeToString(exportMap)
        } catch (e: Exception) {
            // Fallback to simple JSON structure if serialization fails
            """
            {
                "exportInfo": {
                    "exportDate": "${Clock.System.now()}",
                    "appVersion": "1.0.0",
                    "dataVersion": ${settings.version}
                },
                "userSettings": {
                    "userId": "${settings.userId}",
                    "version": ${settings.version}
                },
                "metadata": {
                    "hasCustomizations": ${settings.hasCustomizations()},
                    "lastModified": "${settings.lastModified}",
                    "syncStatus": "${settings.syncStatus.name}"
                }
            }
            """.trimIndent()
        }
    }
    
    companion object {
        private const val SYNC_INTERVAL_MS = 5 * 60 * 1000L // 5 minutes
    }
}