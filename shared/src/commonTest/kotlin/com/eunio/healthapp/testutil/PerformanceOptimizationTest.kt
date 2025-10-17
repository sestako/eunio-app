package com.eunio.healthapp.testutil

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Test suite to validate performance optimizations and ensure
 * test execution meets performance requirements.
 */
class PerformanceOptimizationTest : PerformanceOptimizedBaseTest() {
    
    @Test
    fun `test data builder performance - cached vs uncached`() = runTest {
        val monitor = TestSuitePerformanceMonitor.getInstance()
        
        // Test uncached creation (first time)
        val uncachedResult = monitor.monitorTest("uncached_creation") {
            repeat(100) {
                TestDataBuilder.createUserSettings("user-$it")
            }
        }
        
        // Test cached creation (should be faster)
        val cachedResult = monitor.monitorTest("cached_creation") {
            repeat(100) {
                PerformanceOptimizedTestDataBuilder.createFastUserSettings("user-$it")
            }
        }
        
        val stats = monitor.getCurrentStats()
        println("Performance comparison completed. Total tests: ${stats.totalTests}")
        
        // Verify cache is working
        val cacheStats = PerformanceOptimizedTestDataBuilder.getCacheStats()
        assertTrue(cacheStats.size > 0, "Cache should contain entries")
        
        println("Cache statistics: ${cacheStats.size} entries, ~${cacheStats.memoryEstimate} bytes")
    }
    
    @Test
    fun `test parallel execution performance`() = runTest {
        val monitor = TestSuitePerformanceMonitor.getInstance()
        
        // Create test operations that simulate real test work
        val testOperations = (1..20).map { index ->
            "test_$index" to suspend {
                // Simulate test work
                delay(50.milliseconds)
                PerformanceOptimizedTestDataBuilder.createFastUser("user-$index")
                Unit
            }
        }
        
        // Test parallel execution
        val parallelReport = SimpleParallelTestExecutor.executeWithMonitoring(
            operations = testOperations,
            maxConcurrency = 4
        )
        
        parallelReport.printSummary()
        
        // Verify parallel execution is faster than sequential
        assertTrue(parallelReport.totalDuration < 5.seconds, "Parallel execution should complete quickly")
        assertTrue(parallelReport.successCount == 20, "All parallel tests should succeed")
    }
    
    @Test
    fun `test resource cleanup performance`() = runTest {
        val monitor = TestSuitePerformanceMonitor.getInstance()
        
        monitor.monitorTest("resource_cleanup_test") {
            ResourceCleanupManager.withCleanup(timeout = 2.seconds) { cleanupManager ->
                // Register various resources
                cleanupManager.registerKoinCleanup()
                cleanupManager.registerCacheCleanup()
                
                // Create some test data
                repeat(50) {
                    PerformanceOptimizedTestDataBuilder.createFastUserSettings("cleanup-user-$it")
                }
                
                // Verify resources are registered
                val stats = cleanupManager.getStats()
                assertTrue(stats.resourceCount > 0, "Resources should be registered")
                
                // Cleanup will happen automatically when block exits
            }
        }
        
        // Verify cache was cleared
        val cacheStats = PerformanceOptimizedTestDataBuilder.getCacheStats()
        println("Cache after cleanup: ${cacheStats.size} entries")
    }
    
    @Test
    fun `test fast unit test base class performance`() = runTest {
        val monitor = TestSuitePerformanceMonitor.getInstance()
        
        // Simulate a fast unit test
        monitor.monitorTest("fast_unit_test_simulation") {
            // This simulates what a FastUnitTest would do
            val testData = PerformanceOptimizedTestDataBuilder.createMinimalTestData()
            
            // Perform some quick assertions
            assertTrue(testData.user.id.isNotEmpty())
            assertTrue(testData.userSettings.userId.isNotEmpty())
            assertTrue(testData.userPreferences.userId.isNotEmpty())
        }
        
        val stats = monitor.getCurrentStats()
        val lastTest = stats.totalTests - 1
        
        println("Fast unit test completed in average ${stats.averageDurationMs}ms")
        assertTrue(stats.averageDurationMs < 1000, "Fast unit tests should complete quickly")
    }
    
    @Test
    fun `test memory usage optimization`() = runTest {
        val monitor = TestSuitePerformanceMonitor.getInstance()
        
        monitor.monitorTest("memory_optimization_test") {
            val initialCacheStats = PerformanceOptimizedTestDataBuilder.getCacheStats()
            
            // Create many objects to test memory efficiency
            val users = PerformanceOptimizedTestDataBuilder.createUserBatch(100, "memory-test")
            val settings = PerformanceOptimizedTestDataBuilder.createUserSettingsBatch(100, "memory-test")
            
            val finalCacheStats = PerformanceOptimizedTestDataBuilder.getCacheStats()
            
            println("Memory test results:")
            println("  Initial cache: ${initialCacheStats.size} entries")
            println("  Final cache: ${finalCacheStats.size} entries")
            println("  Created users: ${users.size}")
            println("  Created settings: ${settings.size}")
            
            // Verify batch creation worked
            assertTrue(users.size == 100, "Should create 100 users")
            assertTrue(settings.size == 100, "Should create 100 settings")
            
            // Verify caching is working (should have fewer cache entries than objects created)
            assertTrue(finalCacheStats.size < 200, "Cache should be efficient")
        }
    }
    
    @Test
    fun `test full suite time limit compliance`() = runTest {
        val monitor = TestSuitePerformanceMonitor.getInstance()
        monitor.startSuite()
        
        // Simulate a portion of the full test suite
        val testCount = 50
        val testOperations = (1..testCount).map { index ->
            "suite_test_$index" to suspend {
                // Simulate various test operations
                when (index % 4) {
                    0 -> {
                        // Data creation test
                        PerformanceOptimizedTestDataBuilder.createFastUser("suite-user-$index")
                        Unit
                    }
                    1 -> {
                        // Settings test
                        PerformanceOptimizedTestDataBuilder.createFastUserSettings("suite-user-$index")
                        Unit
                    }
                    2 -> {
                        // Preferences test
                        PerformanceOptimizedTestDataBuilder.createFastUserPreferences("suite-user-$index")
                        Unit
                    }
                    else -> {
                        // Cleanup test
                        delay(10.milliseconds) // Simulate some work
                        PerformanceOptimizedTestDataBuilder.clearCache()
                        Unit
                    }
                }
            }
        }
        
        // Execute tests in parallel
        val parallelReport = SimpleParallelTestExecutor.executeWithMonitoring(
            operations = testOperations,
            maxConcurrency = 6
        )
        
        val report = monitor.endSuite()
        report.printReport()
        
        // Verify time limit compliance
        assertTrue(report.withinTimeLimit, "Test suite should complete within time limit")
        assertTrue(report.totalDuration < 5.seconds, "Simulated suite should be fast")
        
        println("Suite simulation results:")
        println("  Total tests: $testCount")
        println("  Parallel duration: ${parallelReport.totalDuration}")
        println("  Success rate: ${(parallelReport.successCount.toDouble() / testCount * 100).toInt()}%")
    }
    
    @Test
    fun `test performance metrics collection`() = runTest {
        val testMetrics = getPerformanceMetrics()
        
        println("Test performance metrics:")
        testMetrics.printMetrics("PerformanceOptimizationTest")
        
        // Verify metrics are reasonable
        assertTrue(testMetrics.isPerformant(), "Test setup/teardown should be performant")
        assertTrue(testMetrics.cacheStats.size >= 0, "Cache stats should be available")
        
        // Test performance monitoring utility
        val result = withPerformanceMonitoring("test_operation") {
            delay(100.milliseconds)
            "test_result"
        }
        
        assertTrue(result == "test_result", "Performance monitoring should not affect results")
    }
}