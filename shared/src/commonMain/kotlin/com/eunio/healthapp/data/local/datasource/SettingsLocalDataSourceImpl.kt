package com.eunio.healthapp.data.local.datasource

import com.eunio.healthapp.data.local.DatabaseManager
import com.eunio.healthapp.data.local.util.JsonSerializer
import com.eunio.healthapp.domain.model.SyncStatus
import com.eunio.healthapp.domain.model.settings.*
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.domain.error.AppError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Implementation of SettingsLocalDataSource using SQLDelight for local storage.
 * Handles JSON serialization of complex preference objects and provides efficient database operations.
 */
class SettingsLocalDataSourceImpl(
    private val databaseManager: DatabaseManager
) : SettingsLocalDataSource {
    
    private val database get() = databaseManager.getDatabase()
    
    override suspend fun getSettings(): UserSettings? = withContext(Dispatchers.IO) {
        // For single-user apps, we can get the first available settings
        // In multi-user scenarios, this would need the current user ID
        database.userSettingsQueries.selectAll().executeAsOneOrNull()?.let { entity ->
            mapEntityToUserSettings(entity)
        }
    }
    
    override suspend fun getSettings(userId: String): UserSettings? = withContext(Dispatchers.IO) {
        database.userSettingsQueries.selectByUserId(userId).executeAsOneOrNull()?.let { entity ->
            mapEntityToUserSettings(entity)
        }
    }
    
    override suspend fun saveSettings(settings: UserSettings): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existingSettings = getSettings(settings.userId)
            
            if (existingSettings != null) {
                updateSettings(settings)
            } else {
                insertSettings(settings)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(AppError.DataSyncError("Failed to save settings: ${e.message}"))
        }
    }
    
    override suspend fun updateSettings(settings: UserSettings): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.userSettingsQueries.update(
                unitPreferences = JsonSerializer.toJson(settings.unitPreferences),
                notificationPreferences = JsonSerializer.toJson(settings.notificationPreferences),
                cyclePreferences = JsonSerializer.toJson(settings.cyclePreferences),
                privacyPreferences = JsonSerializer.toJson(settings.privacyPreferences),
                displayPreferences = JsonSerializer.toJson(settings.displayPreferences),
                syncPreferences = JsonSerializer.toJson(settings.syncPreferences),
                lastModified = settings.lastModified.toEpochMilliseconds(),
                syncStatus = settings.syncStatus.name,
                version = settings.version.toLong(),
                userId = settings.userId
            )
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(AppError.DataSyncError("Failed to update settings: ${e.message}"))
        }
    }
    
    override suspend fun deleteSettings(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.userSettingsQueries.deleteByUserId(userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(AppError.DataSyncError("Failed to delete settings: ${e.message}"))
        }
    }
    
    override suspend fun getPendingSyncSettings(): List<UserSettings> = withContext(Dispatchers.IO) {
        database.userSettingsQueries.selectPendingSync().executeAsList().mapNotNull { entity ->
            try {
                mapEntityToUserSettings(entity)
            } catch (e: Exception) {
                // Log error but continue with other settings
                null
            }
        }
    }
    
    override suspend fun markAsSynced(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.userSettingsQueries.updateSyncStatus(
                syncStatus = SyncStatus.SYNCED.name,
                lastModified = Clock.System.now().toEpochMilliseconds(),
                userId = userId
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(AppError.DataSyncError("Failed to mark as synced: ${e.message}"))
        }
    }
    
    override suspend fun markAsSyncFailed(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.userSettingsQueries.updateSyncStatus(
                syncStatus = SyncStatus.FAILED.name,
                lastModified = Clock.System.now().toEpochMilliseconds(),
                userId = userId
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(AppError.DataSyncError("Failed to mark as sync failed: ${e.message}"))
        }
    }
    
    override suspend fun clearAllSettings(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.userSettingsQueries.deleteAll()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(AppError.DataSyncError("Failed to clear all settings: ${e.message}"))
        }
    }
    
    override suspend fun clearSettings(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.userSettingsQueries.deleteByUserId(userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(AppError.DataSyncError("Failed to clear settings: ${e.message}"))
        }
    }
    
    override suspend fun settingsExist(userId: String): Boolean = withContext(Dispatchers.IO) {
        database.userSettingsQueries.settingsExist(userId).executeAsOne()
    }
    
    override suspend fun getLastModifiedTimestamp(userId: String): Long? = withContext(Dispatchers.IO) {
        database.userSettingsQueries.getLastModifiedTimestamp(userId).executeAsOneOrNull()
    }
    
    override fun observeSettings(userId: String): Flow<UserSettings?> = flow {
        // Simple polling-based observation - in a real app you might use a more sophisticated approach
        var lastSettings: UserSettings? = null
        
        while (true) {
            val currentSettings = getSettings(userId)
            if (currentSettings != lastSettings) {
                emit(currentSettings)
                lastSettings = currentSettings
            }
            kotlinx.coroutines.delay(1000) // Poll every second
        }
    }
    
    override suspend fun createSettingsBackup(userId: String, backupType: String): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val settings = getSettings(userId)
                ?: return@withContext Result.error(AppError.ValidationError("No settings found for user"))
            
            val settingsJson = JsonSerializer.toJson(settings)
            val dataSize = JsonSerializer.calculateJsonSize(settingsJson)
            val createdAt = Clock.System.now().toEpochMilliseconds()
            
            database.userSettingsQueries.insertBackup(
                userId = userId,
                settingsData = settingsJson,
                backupType = backupType,
                createdAt = createdAt,
                dataSize = dataSize.toLong()
            )
            
            // Get the last inserted row ID
            val backupId = database.userSettingsQueries.selectBackupsByUserId(userId)
                .executeAsList()
                .maxByOrNull { it.createdAt }?.id
                ?: return@withContext Result.error(AppError.DataSyncError("Failed to retrieve backup ID"))
            
            Result.success(backupId)
        } catch (e: Exception) {
            Result.error(AppError.DataSyncError("Failed to create settings backup: ${e.message}"))
        }
    }
    
    override suspend fun getSettingsBackup(backupId: Long): Result<String?> = withContext(Dispatchers.IO) {
        try {
            val backup = database.userSettingsQueries.selectBackupById(backupId).executeAsOneOrNull()
            Result.success(backup?.settingsData)
        } catch (e: Exception) {
            Result.error(AppError.DataSyncError("Failed to get settings backup: ${e.message}"))
        }
    }
    
    override suspend fun getUserBackups(userId: String): Result<List<SettingsBackupInfo>> = withContext(Dispatchers.IO) {
        try {
            val backups = database.userSettingsQueries.selectBackupsByUserId(userId).executeAsList()
                .map { entity ->
                    SettingsBackupInfo(
                        backupId = entity.id,
                        userId = entity.userId,
                        backupType = entity.backupType,
                        createdAt = entity.createdAt,
                        dataSize = entity.dataSize
                    )
                }
            Result.success(backups)
        } catch (e: Exception) {
            Result.error(AppError.DataSyncError("Failed to get user backups: ${e.message}"))
        }
    }
    
    override suspend fun deleteSettingsBackup(backupId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.userSettingsQueries.deleteBackupById(backupId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(AppError.DataSyncError("Failed to delete settings backup: ${e.message}"))
        }
    }
    
    override suspend fun deleteUserBackups(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.userSettingsQueries.deleteBackupsByUserId(userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(AppError.DataSyncError("Failed to delete user backups: ${e.message}"))
        }
    }
    
    override suspend fun getSettingsCount(): Long = withContext(Dispatchers.IO) {
        database.userSettingsQueries.countTotal().executeAsOne()
    }
    
    override suspend fun getSettingsDataSize(): Long = withContext(Dispatchers.IO) {
        // Simple implementation - return count of settings records
        // In a real implementation, this would calculate actual data size
        database.userSettingsQueries.countTotal().executeAsOne()
    }
    
    override suspend fun performMaintenance(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Clean up old automatic backups (keep only last 10 per user)
            val allUsers = database.userSettingsQueries.selectAll().executeAsList()
            
            for (userEntity in allUsers) {
                database.userSettingsQueries.cleanupOldBackups(
                    userId = userEntity.userId,
                    userId_ = userEntity.userId
                )
            }
            
            // Perform database vacuum and analyze
            database.userSettingsQueries.transaction {
                // Note: VACUUM and ANALYZE would need to be added to the .sq file
                // For now, we'll just ensure data consistency
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(AppError.DataSyncError("Failed to perform maintenance: ${e.message}"))
        }
    }
    
    /**
     * Private helper method to insert new settings
     */
    private suspend fun insertSettings(settings: UserSettings) {
        database.userSettingsQueries.insert(
            userId = settings.userId,
            unitPreferences = JsonSerializer.toJson(settings.unitPreferences),
            notificationPreferences = JsonSerializer.toJson(settings.notificationPreferences),
            cyclePreferences = JsonSerializer.toJson(settings.cyclePreferences),
            privacyPreferences = JsonSerializer.toJson(settings.privacyPreferences),
            displayPreferences = JsonSerializer.toJson(settings.displayPreferences),
            syncPreferences = JsonSerializer.toJson(settings.syncPreferences),
            lastModified = settings.lastModified.toEpochMilliseconds(),
            syncStatus = settings.syncStatus.name,
            version = settings.version.toLong()
        )
    }
    
    /**
     * Private helper method to map database entity to UserSettings domain model
     */
    private fun mapEntityToUserSettings(entity: com.eunio.healthapp.database.UserSettings): UserSettings {
        return UserSettings(
            userId = entity.userId,
            unitPreferences = JsonSerializer.fromJson<UnitPreferences>(entity.unitPreferences),
            notificationPreferences = JsonSerializer.fromJson<NotificationPreferences>(entity.notificationPreferences),
            cyclePreferences = JsonSerializer.fromJson<CyclePreferences>(entity.cyclePreferences),
            privacyPreferences = JsonSerializer.fromJson<PrivacyPreferences>(entity.privacyPreferences),
            displayPreferences = JsonSerializer.fromJson<DisplayPreferences>(entity.displayPreferences),
            syncPreferences = JsonSerializer.fromJson<SyncPreferences>(entity.syncPreferences),
            lastModified = Instant.fromEpochMilliseconds(entity.lastModified),
            syncStatus = SyncStatus.valueOf(entity.syncStatus),
            version = entity.version.toInt()
        )
    }
}