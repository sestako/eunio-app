package com.eunio.healthapp.domain.util

import com.eunio.healthapp.data.repository.CachedPreferencesRepository
import com.eunio.healthapp.domain.manager.CachedUnitSystemManager
import com.eunio.healthapp.domain.manager.UnitSystemManager
import com.eunio.healthapp.domain.repository.PreferencesRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

/**
 * Lazy loading factory for unit system components that provides performance optimization
 * by deferring component initialization until first access.
 * Includes singleton pattern and thread-safe initialization.
 */
class LazyUnitSystemComponents(
    private val preferencesRepositoryFactory: () -> PreferencesRepository,
    private val unitSystemManagerFactory: (PreferencesRepository) -> UnitSystemManager,
    private val unitConverterFactory: () -> UnitConverter = { UnitConverterImpl() }
) {
    
    private val initMutex = Mutex()
    
    // Lazy initialized components
    private var _cachedUnitConverter: CachedUnitConverter? = null
    private var _cachedPreferencesRepository: CachedPreferencesRepository? = null
    private var _cachedUnitSystemManager: CachedUnitSystemManager? = null
    
    /**
     * Gets or creates the cached unit converter instance
     */
    suspend fun getCachedUnitConverter(): CachedUnitConverter = initMutex.withLock {
        _cachedUnitConverter ?: run {
            val converter = CachedUnitConverter(
                delegate = unitConverterFactory(),
                maxCacheSize = 1000
            )
            _cachedUnitConverter = converter
            converter
        }
    }
    
    /**
     * Gets or creates the cached preferences repository instance
     */
    suspend fun getCachedPreferencesRepository(): CachedPreferencesRepository = initMutex.withLock {
        _cachedPreferencesRepository ?: run {
            val repository = CachedPreferencesRepository(
                delegate = preferencesRepositoryFactory()
            )
            _cachedPreferencesRepository = repository
            repository
        }
    }
    
    /**
     * Gets or creates the cached unit system manager instance
     */
    suspend fun getCachedUnitSystemManager(): CachedUnitSystemManager = initMutex.withLock {
        _cachedUnitSystemManager ?: run {
            // Get or create preferences repository without recursive lock
            val preferencesRepo = _cachedPreferencesRepository ?: run {
                val repository = CachedPreferencesRepository(
                    delegate = preferencesRepositoryFactory()
                )
                _cachedPreferencesRepository = repository
                repository
            }
            
            val baseManager = unitSystemManagerFactory(preferencesRepo)
            val manager = CachedUnitSystemManager(delegate = baseManager)
            _cachedUnitSystemManager = manager
            manager
        }
    }
    
    /**
     * Preloads all components for improved performance
     */
    suspend fun preloadComponents(): ComponentLoadResult {
        return try {
            val startTime = Clock.System.now()
            
            // Initialize all components
            val converter = getCachedUnitConverter()
            val repository = getCachedPreferencesRepository()
            val manager = getCachedUnitSystemManager()
            
            // Preload caches
            manager.preloadCache()
            
            val loadTime = (Clock.System.now() - startTime).inWholeMilliseconds
            
            ComponentLoadResult.Success(
                loadTimeMs = loadTime,
                componentsLoaded = 3
            )
        } catch (e: Exception) {
            ComponentLoadResult.Error(
                error = e,
                message = "Failed to preload components: ${e.message}"
            )
        }
    }
    
    /**
     * Clears all cached components and forces reinitialization
     */
    suspend fun clearComponents() = initMutex.withLock {
        _cachedUnitConverter?.clearCache()
        _cachedPreferencesRepository?.invalidateAllCache()
        _cachedUnitSystemManager?.clearCache()
        
        _cachedUnitConverter = null
        _cachedPreferencesRepository = null
        _cachedUnitSystemManager = null
    }
    
    /**
     * Gets performance statistics for all components
     */
    suspend fun getPerformanceStats(): PerformanceStats = initMutex.withLock {
        PerformanceStats(
            converterStats = _cachedUnitConverter?.getCacheStats(),
            repositoryStats = _cachedPreferencesRepository?.getCacheStats(),
            managerStats = _cachedUnitSystemManager?.getCacheStats(),
            isInitialized = _cachedUnitConverter != null && 
                           _cachedPreferencesRepository != null && 
                           _cachedUnitSystemManager != null
        )
    }
    
    sealed class ComponentLoadResult {
        data class Success(
            val loadTimeMs: Long,
            val componentsLoaded: Int
        ) : ComponentLoadResult()
        
        data class Error(
            val error: Throwable,
            val message: String
        ) : ComponentLoadResult()
    }
    
    data class PerformanceStats(
        val converterStats: CachedUnitConverter.CacheStats?,
        val repositoryStats: CachedPreferencesRepository.CacheStats?,
        val managerStats: CachedUnitSystemManager.CacheStats?,
        val isInitialized: Boolean
    )
}

/**
 * Global lazy components instance for singleton access
 */
object UnitSystemComponentsFactory {
    private var _instance: LazyUnitSystemComponents? = null
    private val factoryMutex = Mutex()
    
    suspend fun getInstance(
        preferencesRepositoryFactory: () -> PreferencesRepository,
        unitSystemManagerFactory: (PreferencesRepository) -> UnitSystemManager
    ): LazyUnitSystemComponents = factoryMutex.withLock {
        _instance ?: run {
            val components = LazyUnitSystemComponents(
                preferencesRepositoryFactory = preferencesRepositoryFactory,
                unitSystemManagerFactory = unitSystemManagerFactory
            )
            _instance = components
            components
        }
    }
    
    suspend fun clearInstance() = factoryMutex.withLock {
        _instance?.clearComponents()
        _instance = null
    }
    
    /**
     * Reset for testing purposes
     */
    suspend fun resetForTesting() = clearInstance()
}