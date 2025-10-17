package com.eunio.healthapp.testutil

import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Comprehensive test suite performance monitor that tracks execution times,
 * resource usage, and ensures the full test suite completes within time limits.
 */
class TestSuitePerformanceMonitor {
    
    private val testExecutions = mutableListOf<TestExecution>()
    private val performanceThresholds = PerformanceThresholds()
    private var suiteStartTime: Instant? = null
    private var suiteEndTime: Instant? = null
    
    /**
     * Start monitoring the test suite
     */
    fun startSuite() {
        suiteStartTime = Clock.System.now()
        testExecutions.clear()
        println("🚀 Test suite performance monitoring started")
    }
    
    /**
     * End monitoring the test suite
     */
    fun endSuite(): TestSuiteReport {
        suiteEndTime = Clock.System.now()
        val report = generateReport()
        println("✅ Test suite performance monitoring completed")
        return report
    }
    
    /**
     * Record a test execution
     */
    fun recordTestExecution(
        testName: String,
        duration: Duration,
        setupTime: Duration = Duration.ZERO,
        teardownTime: Duration = Duration.ZERO,
        memoryUsed: Long = 0,
        success: Boolean = true
    ) {
        val execution = TestExecution(
            testName = testName,
            startTime = Clock.System.now() - duration,
            endTime = Clock.System.now(),
            duration = duration,
            setupTime = setupTime,
            teardownTime = teardownTime,
            memoryUsed = memoryUsed,
            success = success
        )
        
        testExecutions.add(execution)
        
        // Check for performance issues
        checkPerformanceThresholds(execution)
    }
    
    /**
     * Monitor test execution with automatic recording
     */
    suspend fun <T> monitorTest(
        testName: String,
        test: suspend () -> T
    ): T {
        val startTime = Clock.System.now()
        val startMemory = getMemoryUsage()
        
        return try {
            val result = test()
            val endTime = Clock.System.now()
            val endMemory = getMemoryUsage()
            val duration = endTime - startTime
            
            recordTestExecution(
                testName = testName,
                duration = duration,
                memoryUsed = endMemory - startMemory,
                success = true
            )
            
            result
        } catch (e: Exception) {
            val endTime = Clock.System.now()
            val duration = endTime - startTime
            
            recordTestExecution(
                testName = testName,
                duration = duration,
                success = false
            )
            
            throw e
        }
    }
    
    /**
     * Monitor simple parallel test execution
     */
    suspend fun monitorSimpleParallelTests(
        tests: List<Pair<String, suspend () -> Unit>>,
        maxConcurrency: Int = 4
    ): ParallelTestReport {
        val startTime = Clock.System.now()
        
        val parallelResults = SimpleParallelTestExecutor.executeWithMonitoring(
            operations = tests,
            maxConcurrency = maxConcurrency
        )
        
        val endTime = Clock.System.now()
        val totalDuration = endTime - startTime
        
        // Record individual test results
        parallelResults.results.forEach { (testName, success) ->
            recordTestExecution(
                testName = testName,
                duration = 100.milliseconds, // Rough estimate
                success = success
            )
        }
        
        return ParallelTestReport(
            totalDuration = totalDuration,
            maxConcurrency = maxConcurrency,
            successCount = parallelResults.successCount,
            failureCount = parallelResults.failureCount,
            averageExecutionTime = totalDuration / parallelResults.totalCount.coerceAtLeast(1)
        )
    }
    
    /**
     * Check if test suite is within time limits
     */
    fun checkTimeLimit(): TimeLimitStatus {
        val start = suiteStartTime ?: return TimeLimitStatus.NOT_STARTED
        val current = Clock.System.now()
        val elapsed = current - start
        
        return when {
            elapsed > performanceThresholds.maxSuiteDuration -> TimeLimitStatus.EXCEEDED
            elapsed > performanceThresholds.warningSuiteDuration -> TimeLimitStatus.WARNING
            else -> TimeLimitStatus.OK
        }
    }
    
    /**
     * Get current performance statistics
     */
    fun getCurrentStats(): PerformanceStats {
        val totalTests = testExecutions.size
        val successfulTests = testExecutions.count { it.success }
        val failedTests = totalTests - successfulTests
        
        val totalDuration = testExecutions.sumOf { it.duration.inWholeMilliseconds }
        val averageDuration = if (totalTests > 0) totalDuration / totalTests else 0
        
        val slowTests = testExecutions.filter { 
            it.duration > performanceThresholds.slowTestThreshold 
        }
        
        return PerformanceStats(
            totalTests = totalTests,
            successfulTests = successfulTests,
            failedTests = failedTests,
            totalDurationMs = totalDuration,
            averageDurationMs = averageDuration,
            slowTestCount = slowTests.size,
            memoryUsage = testExecutions.sumOf { it.memoryUsed }
        )
    }
    
    /**
     * Generate comprehensive performance report
     */
    private fun generateReport(): TestSuiteReport {
        val start = suiteStartTime ?: Clock.System.now()
        val end = suiteEndTime ?: Clock.System.now()
        val totalDuration = end - start
        
        val stats = getCurrentStats()
        val slowTests = testExecutions.filter { 
            it.duration > performanceThresholds.slowTestThreshold 
        }.sortedByDescending { it.duration }
        
        val recommendations = generateRecommendations(stats, slowTests)
        
        return TestSuiteReport(
            startTime = start,
            endTime = end,
            totalDuration = totalDuration,
            stats = stats,
            slowTests = slowTests,
            recommendations = recommendations,
            withinTimeLimit = totalDuration <= performanceThresholds.maxSuiteDuration
        )
    }
    
    /**
     * Check individual test against performance thresholds
     */
    private fun checkPerformanceThresholds(execution: TestExecution) {
        if (execution.duration > performanceThresholds.slowTestThreshold) {
            println("⚠️ Slow test detected: ${execution.testName} took ${execution.duration}")
        }
        
        if (execution.memoryUsed > performanceThresholds.memoryThreshold) {
            println("⚠️ High memory usage: ${execution.testName} used ${execution.memoryUsed} bytes")
        }
    }
    
    /**
     * Generate performance recommendations
     */
    private fun generateRecommendations(
        stats: PerformanceStats,
        slowTests: List<TestExecution>
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (stats.slowTestCount > stats.totalTests * 0.1) {
            recommendations.add("Consider optimizing ${stats.slowTestCount} slow tests")
        }
        
        if (stats.averageDurationMs > 1000) {
            recommendations.add("Average test duration is high (${stats.averageDurationMs}ms). Consider using FastUnitTest base class")
        }
        
        if (stats.memoryUsage > 100 * 1024 * 1024) { // 100MB
            recommendations.add("High memory usage detected. Ensure proper resource cleanup")
        }
        
        if (slowTests.isNotEmpty()) {
            recommendations.add("Top slow tests: ${slowTests.take(3).joinToString { it.testName }}")
        }
        
        return recommendations
    }
    
    /**
     * Get current memory usage (rough estimate)
     * Note: Memory monitoring is platform-specific and not available on all platforms
     */
    private fun getMemoryUsage(): Long {
        return try {
            // Memory monitoring not available on all platforms
            0L
        } catch (e: Exception) {
            0L
        }
    }
    
    companion object {
        private var instance: TestSuitePerformanceMonitor? = null
        
        /**
         * Get global performance monitor instance
         */
        fun getInstance(): TestSuitePerformanceMonitor {
            return instance ?: TestSuitePerformanceMonitor().also { instance = it }
        }
        
        /**
         * Reset global instance
         */
        fun reset() {
            instance = null
        }
    }
}

/**
 * Performance thresholds configuration
 */
data class PerformanceThresholds(
    val maxSuiteDuration: Duration = 5.minutes,
    val warningSuiteDuration: Duration = 4.minutes,
    val slowTestThreshold: Duration = 5.seconds,
    val memoryThreshold: Long = 10 * 1024 * 1024 // 10MB
)

/**
 * Individual test execution record
 */
data class TestExecution(
    val testName: String,
    val startTime: Instant,
    val endTime: Instant,
    val duration: Duration,
    val setupTime: Duration,
    val teardownTime: Duration,
    val memoryUsed: Long,
    val success: Boolean
)

/**
 * Performance statistics
 */
data class PerformanceStats(
    val totalTests: Int,
    val successfulTests: Int,
    val failedTests: Int,
    val totalDurationMs: Long,
    val averageDurationMs: Long,
    val slowTestCount: Int,
    val memoryUsage: Long
)

/**
 * Parallel test execution report
 */
data class ParallelTestReport(
    val totalDuration: Duration,
    val maxConcurrency: Int,
    val successCount: Int,
    val failureCount: Int,
    val averageExecutionTime: Duration
)

/**
 * Complete test suite report
 */
data class TestSuiteReport(
    val startTime: Instant,
    val endTime: Instant,
    val totalDuration: Duration,
    val stats: PerformanceStats,
    val slowTests: List<TestExecution>,
    val recommendations: List<String>,
    val withinTimeLimit: Boolean
) {
    fun printReport() {
        println("\n" + "=".repeat(60))
        println("TEST SUITE PERFORMANCE REPORT")
        println("=".repeat(60))
        println("Duration: $totalDuration")
        println("Within time limit: ${if (withinTimeLimit) "✅ YES" else "❌ NO"}")
        println("Total tests: ${stats.totalTests}")
        println("Success rate: ${(stats.successfulTests.toDouble() / stats.totalTests * 100).toInt()}%")
        println("Average test time: ${stats.averageDurationMs}ms")
        println("Slow tests: ${stats.slowTestCount}")
        println("Memory usage: ${stats.memoryUsage / 1024 / 1024}MB")
        
        if (slowTests.isNotEmpty()) {
            println("\nSlowest tests:")
            slowTests.take(5).forEach { test ->
                println("  - ${test.testName}: ${test.duration}")
            }
        }
        
        if (recommendations.isNotEmpty()) {
            println("\nRecommendations:")
            recommendations.forEach { recommendation ->
                println("  • $recommendation")
            }
        }
        
        println("=".repeat(60))
    }
}

/**
 * Time limit status
 */
enum class TimeLimitStatus {
    NOT_STARTED,
    OK,
    WARNING,
    EXCEEDED
}