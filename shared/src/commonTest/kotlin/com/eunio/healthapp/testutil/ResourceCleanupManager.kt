package com.eunio.healthapp.testutil

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import org.koin.core.context.stopKoin
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Comprehensive resource cleanup manager to prevent memory leaks and ensure
 * proper resource disposal in test environments.
 */
class ResourceCleanupManager {
    
    private val resources = mutableListOf<CleanupResource>()
    private val cleanupTasks = mutableListOf<suspend () -> Unit>()
    private val resourcesMutex = Mutex()
    private val tasksMutex = Mutex()
    private var isShutdown = false
    
    /**
     * Register a resource for cleanup
     */
    suspend fun registerResource(resource: CleanupResource) {
        if (!isShutdown) {
            resourcesMutex.withLock {
                resources.add(resource)
            }
        }
    }
    
    /**
     * Register a cleanup task
     */
    suspend fun registerCleanupTask(task: suspend () -> Unit) {
        if (!isShutdown) {
            tasksMutex.withLock {
                cleanupTasks.add(task)
            }
        }
    }
    
    /**
     * Register multiple resources at once
     */
    suspend fun registerResources(vararg resources: CleanupResource) {
        resources.forEach { registerResource(it) }
    }
    
    /**
     * Clean up all registered resources
     */
    suspend fun cleanupAll(timeout: Duration = 10.seconds) {
        if (isShutdown) return
        
        isShutdown = true
        
        try {
            withTimeout(timeout) {
                // Clean up custom tasks first
                cleanupTasks.forEach { task ->
                    try {
                        task()
                    } catch (e: Exception) {
                        println("Warning: Cleanup task failed: ${e.message}")
                    }
                }
                
                // Clean up registered resources
                resources.forEach { resource ->
                    try {
                        resource.cleanup()
                    } catch (e: Exception) {
                        println("Warning: Resource cleanup failed for ${resource.name}: ${e.message}")
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            println("Warning: Resource cleanup timed out after $timeout")
        } finally {
            resources.clear()
            cleanupTasks.clear()
        }
    }
    
    /**
     * Force cleanup without timeout (use with caution)
     */
    suspend fun forceCleanup() {
        isShutdown = true
        
        // Clean up custom tasks
        cleanupTasks.forEach { task ->
            try {
                task()
            } catch (e: Exception) {
                // Ignore errors in force cleanup
            }
        }
        
        // Clean up registered resources
        resources.forEach { resource ->
            try {
                resource.cleanup()
            } catch (e: Exception) {
                // Ignore errors in force cleanup
            }
        }
        
        resources.clear()
        cleanupTasks.clear()
    }
    
    /**
     * Get cleanup statistics
     */
    fun getStats(): CleanupStats {
        return CleanupStats(
            resourceCount = resources.size,
            taskCount = cleanupTasks.size,
            isShutdown = isShutdown
        )
    }
    
    companion object {
        /**
         * Create a scoped cleanup manager that automatically cleans up after the block
         */
        suspend fun <T> withCleanup(
            timeout: Duration = 10.seconds,
            block: suspend (ResourceCleanupManager) -> T
        ): T {
            val manager = ResourceCleanupManager()
            return try {
                block(manager)
            } finally {
                manager.cleanupAll(timeout)
            }
        }
        
        /**
         * Global cleanup for test infrastructure
         */
        suspend fun globalCleanup() {
            try {
                // Stop Koin if running
                stopKoin()
            } catch (e: Exception) {
                // Ignore if Koin wasn't running
            }
            
            try {
                // Clear test data builder cache
                PerformanceOptimizedTestDataBuilder.clearCache()
            } catch (e: Exception) {
                println("Warning: Failed to clear test data cache: ${e.message}")
            }
            
            try {
                // Force garbage collection (platform-specific)
                // Note: GC is not available on all platforms, so we skip this
            } catch (e: Exception) {
                // Ignore GC errors
            }
        }
    }
}

/**
 * Interface for resources that need cleanup
 */
interface CleanupResource {
    val name: String
    suspend fun cleanup()
}

/**
 * Cleanup statistics
 */
data class CleanupStats(
    val resourceCount: Int,
    val taskCount: Int,
    val isShutdown: Boolean
)

/**
 * Common cleanup resources
 */
class KoinCleanupResource : CleanupResource {
    override val name = "Koin DI Container"
    
    override suspend fun cleanup() {
        try {
            stopKoin()
        } catch (e: Exception) {
            // Koin might not be running
        }
    }
}

class CacheCleanupResource : CleanupResource {
    override val name = "Test Data Cache"
    
    override suspend fun cleanup() {
        PerformanceOptimizedTestDataBuilder.clearCache()
    }
}

class CoroutineCleanupResource(
    private val scope: CoroutineScope
) : CleanupResource {
    override val name = "Coroutine Scope"
    
    override suspend fun cleanup() {
        scope.cancel()
        scope.coroutineContext[Job]?.join()
    }
}

class MockServiceCleanupResource(
    private val mockServices: MockServices
) : CleanupResource {
    override val name = "Mock Services"
    
    override suspend fun cleanup() {
        mockServices.reset()
    }
}

/**
 * Extension functions for easy resource registration
 */
suspend fun ResourceCleanupManager.registerKoinCleanup() {
    registerResource(KoinCleanupResource())
}

suspend fun ResourceCleanupManager.registerCacheCleanup() {
    registerResource(CacheCleanupResource())
}

suspend fun ResourceCleanupManager.registerCoroutineCleanup(scope: CoroutineScope) {
    registerResource(CoroutineCleanupResource(scope))
}

suspend fun ResourceCleanupManager.registerMockServiceCleanup(mockServices: MockServices) {
    registerResource(MockServiceCleanupResource(mockServices))
}