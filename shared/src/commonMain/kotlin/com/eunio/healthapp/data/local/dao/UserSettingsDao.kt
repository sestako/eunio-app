package com.eunio.healthapp.data.local.dao

import com.eunio.healthapp.database.EunioDatabase
import com.eunio.healthapp.data.local.util.JsonSerializer
import com.eunio.healthapp.domain.model.SyncStatus
import com.eunio.healthapp.domain.model.settings.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Data Access Object for UserSettings operations.
 * Provides a clean interface for database operations with proper error handling and type safety.
 */
class UserSettingsDao(private val database: EunioDatabase) {
    
    /**
     * Inserts new user settings into the database
     */
    suspend fun insertUserSettings(settings: UserSettings): Unit = withContext(Dispatchers.IO) {
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
     * Updates existing user settings in the database
     */
    suspend fun updateUserSettings(settings: UserSettings): Unit = withContext(Dispatchers.IO) {
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
    }
    
    /**
     * Retrieves user settings by user ID
     */
    suspend fun getUserSettingsById(userId: String): UserSettings? = withContext(Dispatchers.IO) {
        database.userSettingsQueries.selectByUserId(userId).executeAsOneOrNull()?.let { entity ->
            mapEntityToUserSettings(entity)
        }
    }
    
    /**
     * Retrieves all user settings
     */
    suspend fun getAllUserSettings(): List<UserSettings> = withContext(Dispatchers.IO) {
        database.userSettingsQueries.selectAll().executeAsList().mapNotNull { entity ->
            try {
                mapEntityToUserSettings(entity)
            } catch (e: Exception) {
                // Log error but continue with other settings
                null
            }
        }
    }
    
    /**
     * Retrieves settings with pending sync status
     */
    suspend fun getPendingSyncSettings(): List<UserSettings> = withContext(Dispatchers.IO) {
        database.userSettingsQueries.selectPendingSync().executeAsList().mapNotNull { entity ->
            try {
                mapEntityToUserSettings(entity)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Retrieves settings with failed sync status
     */
    suspend fun getFailedSyncSettings(): List<UserSettings> = withContext(Dispatchers.IO) {
        database.userSettingsQueries.selectFailedSync().executeAsList().mapNotNull { entity ->
            try {
                mapEntityToUserSettings(entity)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Updates sync status for a specific user
     */
    suspend fun updateSyncStatus(userId: String, syncStatus: SyncStatus): Unit = withContext(Dispatchers.IO) {
        database.userSettingsQueries.updateSyncStatus(
            syncStatus = syncStatus.name,
            lastModified = Clock.System.now().toEpochMilliseconds(),
            userId = userId
        )
    }
    
    /**
     * Deletes user settings by user ID
     */
    suspend fun deleteUserSettings(userId: String): Unit = withContext(Dispatchers.IO) {
        database.userSettingsQueries.deleteByUserId(userId)
    }
    
    /**
     * Deletes all user settings
     */
    suspend fun deleteAllUserSettings(): Unit = withContext(Dispatchers.IO) {
        database.userSettingsQueries.deleteAll()
    }
    
    /**
     * Checks if settings exist for a user
     */
    suspend fun settingsExist(userId: String): Boolean = withContext(Dispatchers.IO) {
        database.userSettingsQueries.settingsExist(userId).executeAsOne()
    }
    
    /**
     * Gets the last modified timestamp for user settings
     */
    suspend fun getLastModifiedTimestamp(userId: String): Long? = withContext(Dispatchers.IO) {
        database.userSettingsQueries.getLastModifiedTimestamp(userId).executeAsOneOrNull()
    }
    
    /**
     * Gets count of settings by sync status
     */
    suspend fun getCountByStatus(syncStatus: SyncStatus): Long = withContext(Dispatchers.IO) {
        database.userSettingsQueries.countByStatus(syncStatus.name).executeAsOne()
    }
    
    /**
     * Gets total count of settings
     */
    suspend fun getTotalCount(): Long = withContext(Dispatchers.IO) {
        database.userSettingsQueries.countTotal().executeAsOne()
    }
    
    /**
     * Inserts or updates user settings (upsert operation)
     */
    suspend fun insertOrUpdateUserSettings(settings: UserSettings): Unit = withContext(Dispatchers.IO) {
        val existingSettings = getUserSettingsById(settings.userId)
        if (existingSettings != null) {
            updateUserSettings(settings)
        } else {
            insertUserSettings(settings)
        }
    }
    
    /**
     * Marks settings as synced
     */
    suspend fun markAsSynced(userId: String): Unit = withContext(Dispatchers.IO) {
        updateSyncStatus(userId, SyncStatus.SYNCED)
    }
    
    /**
     * Marks settings as sync failed
     */
    suspend fun markAsSyncFailed(userId: String): Unit = withContext(Dispatchers.IO) {
        updateSyncStatus(userId, SyncStatus.FAILED)
    }
    
    /**
     * Creates a settings backup
     */
    suspend fun createBackup(userId: String, backupType: String): Long = withContext(Dispatchers.IO) {
        val settings = getUserSettingsById(userId)
            ?: throw IllegalArgumentException("No settings found for user $userId")
        
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
        
        // Return the backup ID (this is a simplified approach)
        database.userSettingsQueries.selectBackupsByUserId(userId)
            .executeAsList()
            .maxByOrNull { it.createdAt }?.id
            ?: throw IllegalStateException("Failed to retrieve backup ID")
    }
    
    /**
     * Gets a settings backup by ID
     */
    suspend fun getBackup(backupId: Long): String? = withContext(Dispatchers.IO) {
        database.userSettingsQueries.selectBackupById(backupId).executeAsOneOrNull()?.settingsData
    }
    
    /**
     * Gets all backups for a user
     */
    suspend fun getUserBackups(userId: String): List<com.eunio.healthapp.database.SettingsBackup> = withContext(Dispatchers.IO) {
        database.userSettingsQueries.selectBackupsByUserId(userId).executeAsList()
    }
    
    /**
     * Deletes a backup by ID
     */
    suspend fun deleteBackup(backupId: Long): Unit = withContext(Dispatchers.IO) {
        database.userSettingsQueries.deleteBackupById(backupId)
    }
    
    /**
     * Deletes all backups for a user
     */
    suspend fun deleteUserBackups(userId: String): Unit = withContext(Dispatchers.IO) {
        database.userSettingsQueries.deleteBackupsByUserId(userId)
    }
    
    /**
     * Performs cleanup of old automatic backups
     */
    suspend fun cleanupOldBackups(userId: String): Unit = withContext(Dispatchers.IO) {
        database.userSettingsQueries.cleanupOldBackups(userId, userId)
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