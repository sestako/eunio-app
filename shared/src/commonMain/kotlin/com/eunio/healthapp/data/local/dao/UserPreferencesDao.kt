package com.eunio.healthapp.data.local.dao

import com.eunio.healthapp.database.EunioDatabase
import com.eunio.healthapp.database.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class UserPreferencesDao(private val database: EunioDatabase) {
    
    suspend fun getAllPreferences(): List<UserPreferences> = withContext(Dispatchers.IO) {
        database.userPreferencesQueries.selectAll().executeAsList()
    }
    
    suspend fun getPreferencesByUserId(userId: String): UserPreferences? = withContext(Dispatchers.IO) {
        database.userPreferencesQueries.selectByUserId(userId).executeAsOneOrNull()
    }
    
    suspend fun getPendingSyncPreferences(): List<UserPreferences> = withContext(Dispatchers.IO) {
        database.userPreferencesQueries.selectPendingSync().executeAsList()
    }
    
    suspend fun insertPreferences(
        userId: String,
        unitSystem: String,
        isManuallySet: Long,
        lastModified: Long,
        syncStatus: String
    ) = withContext(Dispatchers.IO) {
        database.userPreferencesQueries.insert(
            userId = userId,
            unitSystem = unitSystem,
            isManuallySet = isManuallySet,
            lastModified = lastModified,
            syncStatus = syncStatus
        )
    }
    
    suspend fun updatePreferences(
        userId: String,
        unitSystem: String,
        isManuallySet: Long,
        lastModified: Long,
        syncStatus: String
    ) = withContext(Dispatchers.IO) {
        database.userPreferencesQueries.update(
            unitSystem = unitSystem,
            isManuallySet = isManuallySet,
            lastModified = lastModified,
            syncStatus = syncStatus,
            userId = userId
        )
    }
    
    suspend fun updateSyncStatus(userId: String, syncStatus: String) = withContext(Dispatchers.IO) {
        database.userPreferencesQueries.updateSyncStatus(syncStatus, userId)
    }
    
    suspend fun deletePreferencesByUserId(userId: String) = withContext(Dispatchers.IO) {
        database.userPreferencesQueries.deleteByUserId(userId)
    }
    
    suspend fun deleteAllPreferences() = withContext(Dispatchers.IO) {
        database.userPreferencesQueries.deleteAll()
    }
}