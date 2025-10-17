package com.eunio.healthapp.domain.manager

import com.eunio.healthapp.domain.model.UnitSystem
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Cached implementation of UnitSystemManager that provides performance optimization
 * through in-memory caching of preferences and lazy loading of components.
 * Includes cache invalidation and refresh strategies.
 */
class CachedUnitSystemManager(
    private val delegate: UnitSystemManager
) : UnitSystemManager {
    
    private val _unitSystemFlow = MutableStateFlow<UnitSystem?>(null)
    private val cacheMutex = Mutex()
    
    // Cache state
    private var cachedUnitSystem: UnitSystem? = null
    private var cacheTimestamp: Instant? = null
    private var isInitialized = false
    
    // Cache configuration
    private val cacheExpirationMs = 5 * 60 * 1000L // 5 minutes
    private val maxRetryAttempts = 3
    
    override suspend fun getCurrentUnitSystem(): UnitSystem = cacheMutex.withLock {
        // Check if cache is valid
        if (isCacheValid()) {
            cachedUnitSystem?.let { return it }
        }
        
        // Cache miss or expired, fetch from delegate
        val unitSystem = delegate.getCurrentUnitSystem()
        updateCache(unitSystem)
        unitSystem
    }
    
    override suspend fun setUnitSystem(unitSystem: UnitSystem, isManuallySet: Boolean): Result<Unit> {
        val result = delegate.setUnitSystem(unitSystem, isManuallySet)
        
        if (result.isSuccess) {
            cacheMutex.withLock {
                updateCache(unitSystem)
            }
        }
        
        return result
    }
    
    override suspend fun initializeFromLocale(locale: String): UnitSystem {
        val unitSystem = delegate.initializeFromLocale(locale)
        cacheMutex.withLock {
            updateCache(unitSystem)
        }
        return unitSystem
    }
    
    override suspend fun initializeFromCurrentLocale(): UnitSystem {
        val unitSystem = delegate.initializeFromCurrentLocale()
        cacheMutex.withLock {
            updateCache(unitSystem)
        }
        return unitSystem
    }
    
    override fun observeUnitSystemChanges(): Flow<UnitSystem> {
        return _unitSystemFlow.asStateFlow()
            .let { flow ->
                kotlinx.coroutines.flow.flow {
                    // Emit cached value if available
                    val current = cacheMutex.withLock { 
                        cachedUnitSystem ?: getCurrentUnitSystem()
                    }
                    emit(current)
                    
                    // Continue with flow updates
                    flow.collect { value ->
                        value?.let { emit(it) }
                    }
                }
            }
    }
    
    override suspend fun clearCache() {
        cacheMutex.withLock {
            cachedUnitSystem = null
            cacheTimestamp = null
            isInitialized = false
            _unitSystemFlow.value = null
        }
        
        // Also clear delegate cache if it supports it
        delegate.clearCache()
    }
    
    /**
     * Lazy initialization that only loads when first accessed
     */
    suspend fun lazyInitialize(): UnitSystem = cacheMutex.withLock {
        if (!isInitialized) {
            val unitSystem = delegate.initializeFromCurrentLocale()
            updateCache(unitSystem)
            isInitialized = true
            unitSystem
        } else {
            cachedUnitSystem ?: getCurrentUnitSystem()
        }
    }
    
    /**
     * Preloads the unit system preference into cache
     */
    suspend fun preloadCache(): Result<Unit> {
        return try {
            val unitSystem = getCurrentUnitSystem()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(com.eunio.healthapp.domain.error.AppError.UnknownError(
                message = "Failed to preload cache: ${e.message}",
                cause = e
            ))
        }
    }
    
    /**
     * Refreshes cache from the underlying data source
     */
    suspend fun refreshCache(): Result<UnitSystem> {
        return try {
            cacheMutex.withLock {
                // Force cache invalidation
                cacheTimestamp = null
            }
            
            val unitSystem = getCurrentUnitSystem()
            Result.success(unitSystem)
        } catch (e: Exception) {
            Result.error(com.eunio.healthapp.domain.error.AppError.UnknownError(
                message = "Failed to refresh cache: ${e.message}",
                cause = e
            ))
        }
    }
    
    /**
     * Gets the cached unit system without triggering a fetch
     */
    suspend fun getCachedUnitSystem(): UnitSystem? = cacheMutex.withLock {
        if (isCacheValid()) cachedUnitSystem else null
    }
    
    /**
     * Checks if the current cache is still valid
     */
    private fun isCacheValid(): Boolean {
        val timestamp = cacheTimestamp ?: return false
        val now = Clock.System.now()
        return (now.toEpochMilliseconds() - timestamp.toEpochMilliseconds()) < cacheExpirationMs
    }
    
    /**
     * Updates the cache with new unit system value
     */
    private fun updateCache(unitSystem: UnitSystem) {
        cachedUnitSystem = unitSystem
        cacheTimestamp = Clock.System.now()
        _unitSystemFlow.value = unitSystem
        isInitialized = true
    }
    
    /**
     * Gets cache statistics for monitoring
     */
    suspend fun getCacheStats(): CacheStats = cacheMutex.withLock {
        CacheStats(
            isCached = cachedUnitSystem != null,
            isValid = isCacheValid(),
            cacheAge = cacheTimestamp?.let { 
                Clock.System.now().toEpochMilliseconds() - it.toEpochMilliseconds() 
            },
            isInitialized = isInitialized
        )
    }
    
    data class CacheStats(
        val isCached: Boolean,
        val isValid: Boolean,
        val cacheAge: Long?,
        val isInitialized: Boolean
    )
}