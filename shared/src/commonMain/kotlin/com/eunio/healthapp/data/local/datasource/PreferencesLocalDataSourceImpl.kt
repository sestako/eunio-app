package com.eunio.healthapp.data.local.datasource

import com.eunio.healthapp.data.local.DatabaseManager
import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.SyncStatus
import com.eunio.healthapp.domain.model.UnitSystem
import com.eunio.healthapp.domain.model.UserPreferences
import com.eunio.healthapp.domain.util.Result
import kotlinx.datetime.Instant

/**
 * Implementation of PreferencesLocalDataSource using SQLDelight.
 * Handles local storage operations for user preferences.
 */
class PreferencesLocalDataSourceImpl(
    private val databaseManager: DatabaseManager
) : PreferencesLocalDataSource {
    
    override suspend fun getPreferences(): UserPreferences? {
        return try {
            val dao = databaseManager.getUserPreferencesDao()
            // Get the first available preferences (assuming single user context)
            val allPrefs = dao.getAllPreferences()
            allPrefs.firstOrNull()?.let { dbPrefs ->
                mapToUserPreferences(dbPrefs)
            }
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun getPreferences(userId: String): UserPreferences? {
        return try {
            val dao = databaseManager.getUserPreferencesDao()
            val dbPrefs = dao.getPreferencesByUserId(userId)
            dbPrefs?.let { mapToUserPreferences(it) }
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun savePreferences(preferences: UserPreferences): Result<Unit> {
        return try {
            val dao = databaseManager.getUserPreferencesDao()
            
            // Check if preferences already exist
            val existing = dao.getPreferencesByUserId(preferences.userId)
            
            if (existing != null) {
                // Update existing preferences
                dao.updatePreferences(
                    userId = preferences.userId,
                    unitSystem = preferences.unitSystem.name,
                    isManuallySet = if (preferences.isManuallySet) 1L else 0L,
                    lastModified = preferences.lastModified.epochSeconds,
                    syncStatus = preferences.syncStatus.name
                )
            } else {
                // Insert new preferences
                dao.insertPreferences(
                    userId = preferences.userId,
                    unitSystem = preferences.unitSystem.name,
                    isManuallySet = if (preferences.isManuallySet) 1L else 0L,
                    lastModified = preferences.lastModified.epochSeconds,
                    syncStatus = preferences.syncStatus.name
                )
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(AppError.DatabaseError("Failed to save preferences: ${e.message}"))
        }
    }
    
    override suspend fun getPendingSyncPreferences(): List<UserPreferences> {
        return try {
            val dao = databaseManager.getUserPreferencesDao()
            val pendingPrefs = dao.getPendingSyncPreferences()
            pendingPrefs.mapNotNull { dbPrefs ->
                try {
                    mapToUserPreferences(dbPrefs)
                } catch (e: Exception) {
                    null // Skip invalid entries
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    override suspend fun markAsSynced(userId: String): Result<Unit> {
        return try {
            val dao = databaseManager.getUserPreferencesDao()
            dao.updateSyncStatus(userId, SyncStatus.SYNCED.name)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(AppError.DatabaseError("Failed to mark as synced: ${e.message}"))
        }
    }
    
    override suspend fun markAsFailed(userId: String): Result<Unit> {
        return try {
            val dao = databaseManager.getUserPreferencesDao()
            dao.updateSyncStatus(userId, SyncStatus.FAILED.name)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(AppError.DatabaseError("Failed to mark as failed: ${e.message}"))
        }
    }
    
    override suspend fun clearPreferences(): Result<Unit> {
        return try {
            val dao = databaseManager.getUserPreferencesDao()
            dao.deleteAllPreferences()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(AppError.DatabaseError("Failed to clear all preferences: ${e.message}"))
        }
    }
    
    override suspend fun clearPreferences(userId: String): Result<Unit> {
        return try {
            val dao = databaseManager.getUserPreferencesDao()
            dao.deletePreferencesByUserId(userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(AppError.DatabaseError("Failed to clear preferences for user: ${e.message}"))
        }
    }
    
    /**
     * Maps database UserPreferences entity to domain UserPreferences model.
     */
    private fun mapToUserPreferences(dbPrefs: com.eunio.healthapp.database.UserPreferences): UserPreferences {
        return UserPreferences(
            userId = dbPrefs.userId,
            unitSystem = UnitSystem.valueOf(dbPrefs.unitSystem),
            isManuallySet = dbPrefs.isManuallySet == 1L,
            lastModified = Instant.fromEpochSeconds(dbPrefs.lastModified),
            syncStatus = SyncStatus.valueOf(dbPrefs.syncStatus)
        )
    }
}