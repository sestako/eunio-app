package com.eunio.healthapp.testutil

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Performance-optimized base test class that provides:
 * - Fast setup and teardown
 * - Resource cleanup management
 * - Cached test data
 * - Minimal Koin overhead
 */
abstract class PerformanceOptimizedBaseTest {
    
    private var testScope: CoroutineScope? = null
    private var cleanupManager: ResourceCleanupManager? = null
    private var setupTime: Long = 0
    private var teardownTime: Long = 0
    
    /**
     * Override to provide custom test modules (default: minimal module)
     */
    protected open fun getTestModules(): List<Module> = listOf(minimalTestModule)
    
    /**
     * Override to disable Koin for tests that don't need DI
     */
    protected open fun useKoin(): Boolean = true
    
    /**
     * Override to provide custom cleanup timeout
     */
    protected open fun getCleanupTimeout(): Duration = 5.seconds
    
    /**
     * Override for custom setup after base setup
     */
    protected open suspend fun customSetup() {}
    
    /**
     * Override for custom teardown before base teardown
     */
    protected open suspend fun customTeardown() {}
    
    /**
     * Get cached test data for performance
     */
    protected fun getTestData(): PerformanceOptimizedTestDataBuilder.TestDataSet {
        return PerformanceOptimizedTestDataBuilder.createMinimalTestData()
    }
    
    /**
     * Get test scope for coroutine operations
     */
    protected fun getTestScope(): CoroutineScope {
        return testScope ?: throw IllegalStateException("Test scope not initialized")
    }
    
    @BeforeTest
    fun performanceOptimizedSetup() {
        val startTime = Clock.System.now().toEpochMilliseconds()
        
        try {
            // Initialize cleanup manager
            cleanupManager = ResourceCleanupManager()
            
            // Create test scope
            testScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            runBlocking {
                cleanupManager?.registerCoroutineCleanup(testScope!!)
            }
            
            // Setup Koin if needed
            if (useKoin()) {
                setupKoinFast()
            }
            
            // Register cache cleanup
            runBlocking {
                cleanupManager?.registerCacheCleanup()
            }
            
        } catch (e: Exception) {
            println("Warning: Setup failed: ${e.message}")
            throw e
        } finally {
            setupTime = Clock.System.now().toEpochMilliseconds() - startTime
        }
    }
    
    @AfterTest
    fun performanceOptimizedTeardown() {
        val startTime = Clock.System.now().toEpochMilliseconds()
        
        try {
            // Custom teardown first
            kotlinx.coroutines.runBlocking {
                customTeardown()
            }
            
            // Cleanup all resources
            kotlinx.coroutines.runBlocking {
                cleanupManager?.cleanupAll(getCleanupTimeout())
            }
            
        } catch (e: Exception) {
            println("Warning: Teardown failed: ${e.message}")
            // Continue with cleanup even if custom teardown fails
        } finally {
            // Force cleanup critical resources
            try {
                testScope?.cancel()
                testScope = null
                
                if (useKoin()) {
                    stopKoin()
                }
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
            
            cleanupManager = null
            teardownTime = Clock.System.now().toEpochMilliseconds() - startTime
        }
    }
    
    /**
     * Fast Koin setup with minimal overhead
     */
    private fun setupKoinFast() {
        try {
            // Stop any existing Koin instance quickly
            stopKoin()
        } catch (e: Exception) {
            // Ignore if no instance was running
        }
        
        // Start with minimal configuration
        startKoin {
            modules(getTestModules())
        }
        
        runBlocking {
            cleanupManager?.registerKoinCleanup()
        }
    }
    
    /**
     * Get performance metrics for this test
     */
    protected fun getPerformanceMetrics(): TestPerformanceMetrics {
        return TestPerformanceMetrics(
            setupTimeMs = setupTime,
            teardownTimeMs = teardownTime,
            cacheStats = PerformanceOptimizedTestDataBuilder.getCacheStats(),
            cleanupStats = cleanupManager?.getStats()
        )
    }
    
    /**
     * Execute operation with performance monitoring
     */
    protected suspend fun <T> withPerformanceMonitoring(
        operationName: String,
        operation: suspend () -> T
    ): T {
        val startTime = Clock.System.now().toEpochMilliseconds()
        return try {
            operation()
        } finally {
            val duration = Clock.System.now().toEpochMilliseconds() - startTime
            if (duration > 1000) { // Log slow operations
                println("Performance warning: $operationName took ${duration}ms")
            }
        }
    }
    
    /**
     * Create test data with caching
     */
    protected fun createCachedTestUser(id: String = "test-user") = 
        PerformanceOptimizedTestDataBuilder.createFastUser(id)
    
    protected fun createCachedUserSettings(userId: String = "test-user") = 
        PerformanceOptimizedTestDataBuilder.createFastUserSettings(userId)
    
    protected fun createCachedUserPreferences(userId: String = "test-user") = 
        PerformanceOptimizedTestDataBuilder.createFastUserPreferences(userId)
}

/**
 * Ultra-fast base test for simple unit tests that don't need DI
 */
abstract class FastUnitTest : PerformanceOptimizedBaseTest() {
    override fun useKoin(): Boolean = false
    override fun getCleanupTimeout(): Duration = 1.seconds
}

/**
 * Performance metrics for test execution
 */
data class TestPerformanceMetrics(
    val setupTimeMs: Long,
    val teardownTimeMs: Long,
    val cacheStats: PerformanceOptimizedTestDataBuilder.CacheStats,
    val cleanupStats: CleanupStats?
) {
    val totalTimeMs: Long get() = setupTimeMs + teardownTimeMs
    
    fun printMetrics(testName: String) {
        println("Performance metrics for $testName:")
        println("  Setup: ${setupTimeMs}ms")
        println("  Teardown: ${teardownTimeMs}ms")
        println("  Total: ${totalTimeMs}ms")
        println("  Cache size: ${cacheStats.size}")
        println("  Cache memory: ${cacheStats.memoryEstimate} bytes")
    }
    
    fun isPerformant(): Boolean {
        return setupTimeMs < 100 && teardownTimeMs < 100
    }
}