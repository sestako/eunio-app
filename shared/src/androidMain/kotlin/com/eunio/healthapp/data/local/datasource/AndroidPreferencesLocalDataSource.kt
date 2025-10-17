package com.eunio.healthapp.data.local.datasource

import android.content.Context
import android.content.SharedPreferences
import com.eunio.healthapp.data.local.DatabaseManager
import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.SyncStatus
import com.eunio.healthapp.domain.model.UnitSystem
import com.eunio.healthapp.domain.model.UserPreferences
import com.eunio.healthapp.domain.util.Result
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Android-specific implementation of PreferencesLocalDataSource with SharedPreferences caching.
 * Provides fast access to frequently used preferences while maintaining database persistence.
 */
class AndroidPreferencesLocalDataSource(
    private val databaseManager: DatabaseManager,
    private val context: Context
) : PreferencesLocalDataSource {
    
    companion object {
        private const val PREFS_NAME = "eunio_unit_preferences"
        private const val KEY_CACHED_PREFERENCES = "cached_preferences"
        private const val KEY_CACHE_TIMESTAMP = "cache_timestamp"
        private const val CACHE_VALIDITY_MS = 5 * 60 * 1000L // 5 minutes
    }
    
    private val sharedPrefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    private val json = Json { ignoreUnknownKeys = true }
    
    override suspend fun getPreferences(): UserPreferences? {
        // Try cache first for performance
        getCachedPreferences()?.let { cached ->
            if (isCacheValid()) {
                return cached
            }
        }
        
        // Fallback to database
        return try {
            val dao = databaseManager.getUserPreferencesDao()
            val allPrefs = dao.getAllPreferences()
            allPrefs.firstOrNull()?.let { dbPrefs ->
                val preferences = mapToUserPreferences(dbPrefs)
                // Update cache
                cachePreferences(preferences)
                preferences
            }
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun getPreferences(userId: String): UserPreferences? {
        // Try cache first
        getCachedPreferences()?.let { cached ->
            if (cached.userId == userId && isCacheValid()) {
                return cached
            }
        }
        
        // Fallback to database
        return try {
            val dao = databaseManager.getUserPreferencesDao()
            val dbPrefs = dao.getPreferencesByUserId(userId)
            dbPrefs?.let { 
                val preferences = mapToUserPreferences(it)
                // Update cache
                cachePreferences(preferences)
                preferences
            }
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
            
            // Update cache immediately for fast access
            cachePreferences(preferences)
            
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
                    null
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
            
            // Update cache if it's for the same user
            getCachedPreferences()?.let { cached ->
                if (cached.userId == userId) {
                    val updatedPrefs = cached.copy(syncStatus = SyncStatus.SYNCED)
                    cachePreferences(updatedPrefs)
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(AppError.DatabaseError("Failed to mark as synced: ${e.message}"))
        }
    }
    
    override suspend fun markAsFailed(userId: String): Result<Unit> {
        return try {
            val dao = databaseManager.getUserPreferencesDao()
            dao.updateSyncStatus(userId, SyncStatus.FAILED.name)
            
            // Update cache if it's for the same user
            getCachedPreferences()?.let { cached ->
                if (cached.userId == userId) {
                    val updatedPrefs = cached.copy(syncStatus = SyncStatus.FAILED)
                    cachePreferences(updatedPrefs)
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(AppError.DatabaseError("Failed to mark as failed: ${e.message}"))
        }
    }
    
    override suspend fun clearPreferences(): Result<Unit> {
        return try {
            val dao = databaseManager.getUserPreferencesDao()
            dao.deleteAllPreferences()
            
            // Clear cache
            clearCache()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(AppError.DatabaseError("Failed to clear all preferences: ${e.message}"))
        }
    }
    
    override suspend fun clearPreferences(userId: String): Result<Unit> {
        return try {
            val dao = databaseManager.getUserPreferencesDao()
            dao.deletePreferencesByUserId(userId)
            
            // Clear cache if it's for the same user
            getCachedPreferences()?.let { cached ->
                if (cached.userId == userId) {
                    clearCache()
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(AppError.DatabaseError("Failed to clear preferences for user: ${e.message}"))
        }
    }
    
    /**
     * Android-specific method to preload preferences into cache.
     * Useful for app startup optimization.
     */
    suspend fun preloadCache(userId: String) {
        try {
            getPreferences(userId) // This will load and cache the preferences
        } catch (e: Exception) {
            // Ignore preload failures
        }
    }
    
    /**
     * Android-specific method to get cache statistics for debugging.
     */
    fun getCacheStats(): Map<String, Any> {
        return mapOf(
            "hasCachedData" to sharedPrefs.contains(KEY_CACHED_PREFERENCES),
            "cacheValid" to isCacheValid(),
            "cacheTimestamp" to sharedPrefs.getLong(KEY_CACHE_TIMESTAMP, 0L)
        )
    }
    
    private fun getCachedPreferences(): UserPreferences? {
        return try {
            val prefsJson = sharedPrefs.getString(KEY_CACHED_PREFERENCES, null)
            prefsJson?.let { json.decodeFromString<UserPreferences>(it) }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun cachePreferences(preferences: UserPreferences) {
        try {
            val prefsJson = json.encodeToString(preferences)
            sharedPrefs.edit()
                .putString(KEY_CACHED_PREFERENCES, prefsJson)
                .putLong(KEY_CACHE_TIMESTAMP, Clock.System.now().toEpochMilliseconds())
                .apply()
        } catch (e: Exception) {
            // Ignore cache failures
        }
    }
    
    private fun isCacheValid(): Boolean {
        val cacheTimestamp = sharedPrefs.getLong(KEY_CACHE_TIMESTAMP, 0L)
        val currentTime = Clock.System.now().toEpochMilliseconds()
        return (currentTime - cacheTimestamp) < CACHE_VALIDITY_MS
    }
    
    private fun clearCache() {
        sharedPrefs.edit()
            .remove(KEY_CACHED_PREFERENCES)
            .remove(KEY_CACHE_TIMESTAMP)
            .apply()
    }
    
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