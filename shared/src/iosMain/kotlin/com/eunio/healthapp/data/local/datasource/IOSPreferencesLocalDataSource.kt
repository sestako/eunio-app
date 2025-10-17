package com.eunio.healthapp.data.local.datasource

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
import platform.Foundation.*

/**
 * iOS-specific implementation of PreferencesLocalDataSource with NSUserDefaults caching.
 * Provides fast access to frequently used preferences while maintaining database persistence.
 */
class IOSPreferencesLocalDataSource(
    private val databaseManager: DatabaseManager
) : PreferencesLocalDataSource {
    
    companion object {
        private const val KEY_CACHED_PREFERENCES = "eunio_cached_preferences"
        private const val KEY_CACHE_TIMESTAMP = "eunio_cache_timestamp"
        private const val CACHE_VALIDITY_MS = 5 * 60 * 1000L // 5 minutes
    }
    
    private val userDefaults = NSUserDefaults.standardUserDefaults
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
     * iOS-specific method to preload preferences into cache.
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
     * iOS-specific method to get cache statistics for debugging.
     */
    fun getCacheStats(): Map<String, Any> {
        return mapOf(
            "hasCachedData" to (userDefaults.objectForKey(KEY_CACHED_PREFERENCES) != null),
            "cacheValid" to isCacheValid(),
            "cacheTimestamp" to userDefaults.doubleForKey(KEY_CACHE_TIMESTAMP)
        )
    }
    
    /**
     * iOS-specific method to sync preferences with system settings.
     * Integrates with iOS system measurement preferences when available.
     */
    suspend fun syncWithSystemSettings(userId: String): Result<Unit> {
        return try {
            // Get current preferences
            val currentPrefs = getPreferences(userId)
            
            // Check if user has manually set preferences
            if (currentPrefs?.isManuallySet == true) {
                return Result.success(Unit) // Don't override manual settings
            }
            
            // Try to get system measurement preference
            val locale = NSLocale.currentLocale
            val measurementSystem = locale.objectForKey(NSLocaleMeasurementSystem) as? String
            
            val systemUnitSystem = when (measurementSystem) {
                "Metric" -> UnitSystem.METRIC
                "U.S." -> UnitSystem.IMPERIAL
                else -> null
            }
            
            if (systemUnitSystem != null && systemUnitSystem != currentPrefs?.unitSystem) {
                val updatedPrefs = UserPreferences(
                    userId = userId,
                    unitSystem = systemUnitSystem,
                    isManuallySet = false,
                    lastModified = Clock.System.now(),
                    syncStatus = SyncStatus.PENDING
                )
                savePreferences(updatedPrefs)
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.error(AppError.ValidationError("Failed to sync with system settings: ${e.message}"))
        }
    }
    
    private fun getCachedPreferences(): UserPreferences? {
        return try {
            val prefsString = userDefaults.stringForKey(KEY_CACHED_PREFERENCES)
            prefsString?.let { json.decodeFromString<UserPreferences>(it) }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun cachePreferences(preferences: UserPreferences) {
        try {
            val prefsJson = json.encodeToString(preferences)
            userDefaults.setObject(prefsJson, KEY_CACHED_PREFERENCES)
            userDefaults.setDouble(Clock.System.now().toEpochMilliseconds().toDouble(), KEY_CACHE_TIMESTAMP)
            userDefaults.synchronize()
        } catch (e: Exception) {
            // Ignore cache failures
        }
    }
    
    private fun isCacheValid(): Boolean {
        val cacheTimestamp = userDefaults.doubleForKey(KEY_CACHE_TIMESTAMP).toLong()
        val currentTime = Clock.System.now().toEpochMilliseconds()
        return (currentTime - cacheTimestamp) < CACHE_VALIDITY_MS
    }
    
    private fun clearCache() {
        userDefaults.removeObjectForKey(KEY_CACHED_PREFERENCES)
        userDefaults.removeObjectForKey(KEY_CACHE_TIMESTAMP)
        userDefaults.synchronize()
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