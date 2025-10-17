package com.eunio.healthapp.data.repository

import com.eunio.healthapp.domain.model.SyncStatistics
import com.eunio.healthapp.domain.model.UserPreferences
import com.eunio.healthapp.domain.repository.PreferencesRepository
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Cached implementation of PreferencesRepository that provides performance optimization
 * through in-memory caching of user preferences with intelligent cache invalidation.
 */
class CachedPreferencesRepository(
    private val delegate: PreferencesRepository
) : PreferencesRepository {
    
    private val preferencesCache = mutableMapOf<String, CachedPreference>()
    private val cacheMutex = Mutex()
    
    // Cache configuration
    private val cacheExpirationMs = 10 * 60 * 1000L // 10 minutes
    private val maxCacheSize = 100
    
    data class CachedPreference(
        val preferences: UserPreferences?,
        val timestamp: Instant,
        val accessCount: Int = 0
    )
    
    override suspend fun getUserPreferences(): Result<UserPreferences?> {
        return delegate.getUserPreferences()
    }
    
    override suspend fun getUserPreferences(userId: String): Result<UserPreferences?> = cacheMutex.withLock {
        // Check cache first
        val cached = preferencesCache[userId]
        if (cached != null && isCacheValid(cached.timestamp)) {
            // Update access count for LRU
            preferencesCache[userId] = cached.copy(accessCount = cached.accessCount + 1)
            return Result.success(cached.preferences)
        }
        
        // Cache miss or expired, fetch from delegate
        val result = delegate.getUserPreferences(userId)
        if (result.isSuccess) {
            cachePreferences(userId, result.getOrNull())
        }
        
        result
    }
    
    override suspend fun saveUserPreferences(preferences: UserPreferences): Result<Unit> {
        val result = delegate.saveUserPreferences(preferences)
        
        if (result.isSuccess) {
            cacheMutex.withLock {
                cachePreferences(preferences.userId, preferences)
            }
        }
        
        return result
    }
    
    override suspend fun syncPreferences(): Result<Unit> {
        val result = delegate.syncPreferences()
        
        if (result.isSuccess) {
            // Invalidate cache after successful sync to ensure fresh data
            cacheMutex.withLock {
                preferencesCache.clear()
            }
        }
        
        return result
    }
    
    override suspend fun clearPreferences(): Result<Unit> {
        val result = delegate.clearPreferences()
        
        if (result.isSuccess) {
            cacheMutex.withLock {
                preferencesCache.clear()
            }
        }
        
        return result
    }
    
    override suspend fun clearPreferences(userId: String): Result<Unit> {
        val result = delegate.clearPreferences(userId)
        
        if (result.isSuccess) {
            cacheMutex.withLock {
                preferencesCache.remove(userId)
            }
        }
        
        return result
    }
    
    override suspend fun syncWithConflictResolution(userId: String): Result<Unit> {
        val result = delegate.syncWithConflictResolution(userId)
        
        if (result.isSuccess) {
            // Invalidate cache for this user to ensure fresh data
            cacheMutex.withLock {
                preferencesCache.remove(userId)
            }
        }
        
        return result
    }
    
    override suspend fun handleOfflineMode(): Result<Unit> {
        return delegate.handleOfflineMode()
    }
    
    override suspend fun getSyncStatistics(): Result<SyncStatistics> {
        return delegate.getSyncStatistics()
    }
    
    override suspend fun recoverFromSyncFailure(): Result<Unit> {
        val result = delegate.recoverFromSyncFailure()
        
        if (result.isSuccess) {
            // Invalidate cache after successful recovery to ensure fresh data
            cacheMutex.withLock {
                preferencesCache.clear()
            }
        }
        
        return result
    }
    
    /**
     * Preloads preferences for a user into cache
     */
    suspend fun preloadUserPreferences(userId: String): Result<Unit> {
        return try {
            getUserPreferences(userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(com.eunio.healthapp.domain.error.AppError.UnknownError(
                message = "Failed to preload preferences: ${e.message}",
                cause = e
            ))
        }
    }
    
    /**
     * Batch preloads preferences for multiple users
     */
    suspend fun preloadUserPreferencesBatch(userIds: List<String>): Result<Unit> {
        return try {
            userIds.forEach { userId ->
                getUserPreferences(userId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(com.eunio.healthapp.domain.error.AppError.UnknownError(
                message = "Failed to preload preferences batch: ${e.message}",
                cause = e
            ))
        }
    }
    
    /**
     * Gets cached preferences without triggering a fetch
     */
    suspend fun getCachedPreferences(userId: String): UserPreferences? = cacheMutex.withLock {
        val cached = preferencesCache[userId]
        if (cached != null && isCacheValid(cached.timestamp)) {
            cached.preferences
        } else {
            null
        }
    }
    
    /**
     * Invalidates cache for a specific user
     */
    suspend fun invalidateCache(userId: String) = cacheMutex.withLock {
        preferencesCache.remove(userId)
    }
    
    /**
     * Invalidates all cached preferences
     */
    suspend fun invalidateAllCache() = cacheMutex.withLock {
        preferencesCache.clear()
    }
    
    /**
     * Refreshes cache for a specific user
     */
    suspend fun refreshCache(userId: String): Result<UserPreferences?> = cacheMutex.withLock {
        // Remove from cache to force refresh
        preferencesCache.remove(userId)
        
        // Fetch fresh data
        val result = delegate.getUserPreferences(userId)
        if (result.isSuccess) {
            cachePreferences(userId, result.getOrNull())
        }
        
        result
    }
    
    private fun cachePreferences(userId: String, preferences: UserPreferences?) {
        // Evict least recently used if cache is full
        if (preferencesCache.size >= maxCacheSize) {
            evictLeastRecentlyUsed()
        }
        
        preferencesCache[userId] = CachedPreference(
            preferences = preferences,
            timestamp = Clock.System.now(),
            accessCount = 1
        )
    }
    
    private fun evictLeastRecentlyUsed() {
        val lruEntry = preferencesCache.entries.minByOrNull { it.value.accessCount }
        lruEntry?.let { preferencesCache.remove(it.key) }
    }
    
    private fun isCacheValid(timestamp: Instant): Boolean {
        val now = Clock.System.now()
        return (now.toEpochMilliseconds() - timestamp.toEpochMilliseconds()) < cacheExpirationMs
    }
    
    /**
     * Gets cache statistics for monitoring
     */
    suspend fun getCacheStats(): CacheStats = cacheMutex.withLock {
        val validEntries = preferencesCache.values.count { isCacheValid(it.timestamp) }
        val totalEntries = preferencesCache.size
        
        CacheStats(
            totalEntries = totalEntries,
            validEntries = validEntries,
            maxCacheSize = maxCacheSize,
            hitRate = if (totalEntries > 0) validEntries.toDouble() / totalEntries else 0.0,
            cacheUtilization = totalEntries.toDouble() / maxCacheSize
        )
    }
    
    data class CacheStats(
        val totalEntries: Int,
        val validEntries: Int,
        val maxCacheSize: Int,
        val hitRate: Double,
        val cacheUtilization: Double
    )
}