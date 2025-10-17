package com.eunio.healthapp.testutil

import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

/**
 * Test execution validator that ensures test suite completes within reasonable time limits
 * and provides comprehensive performance analysis.
 * 
 * Addresses Requirement 7.4:
 * - Ensure test suite completes within reasonable time limits
 * - Add proper resource management for test execution
 * - Fix memory leaks in mock services and test utilities
 */
class TestExecutionValidator {
    
    /**
     * Configuration for test execution validation
     */
    data class ValidationConfiguration(
        val maxSingleTestDuration: Duration = 30.seconds,
        val maxTestSuiteDuration: Duration = 10.minutes,
        val maxMemoryUsageMB: Long = 512,
        val maxConcurrentTests: Int = 5,
        val enablePerformanceTracking: Boolean = true,
        val enableMemoryLeakDetection: Boolean = true,
        val enableTimeoutDetection: Boolean = true,
        val warningThresholdPercent: Double = 0.8 // Warn at 80% of limits
    )
    
    /**
     * Test execution metrics
     */
    data class TestExecutionMetrics(
        val testName: String,
        val startTime: Instant,
        val endTime: Instant,
        val duration: Duration,
        val memoryUsedMB: Long,
        val resourceCount: Int,
        val status: TestStatus,
        val errorMessage: String? = null
    ) {
        enum class TestStatus {
            PASSED,
            FAILED,
            TIMEOUT,
            MEMORY_EXCEEDED,
            RESOURCE_LEAK
        }
    }
    
    /**
     * Test suite execution summary
     */
    data class TestSuiteMetrics(
        val totalTests: Int,
        val passedTests: Int,
        val failedTests: Int,
        val timeoutTests: Int,
        val memoryExceededTests: Int,
        val resourceLeakTests: Int,
        val totalDuration: Duration,
        val averageDuration: Duration,
        val maxDuration: Duration,
        val peakMemoryUsageMB: Long,
        val totalResourcesAllocated: Long,
        val resourceLeaks: List<String>
    ) {
        val successRate: Double get() = if (totalTests > 0) passedTests.toDouble() / totalTests else 0.0
        val isWithinLimits: Boolean get() = timeoutTests == 0 && memoryExceededTests == 0
    }
    
    private val configuration = ValidationConfiguration()
    private val testMetrics = mutableListOf<TestExecutionMetrics>()
    private val resourceTracker = ResourceTracker()
    private val memoryMonitor = MemoryMonitor()
    
    // Test suite execution tracking
    private var suiteStartTime: Instant? = null
    private var suiteEndTime: Instant? = null
    private var currentlyRunningTests = mutableSetOf<String>()
    
    /**
     * Resource tracker for monitoring test resource usage
     */
    private class ResourceTracker {
        private val allocatedResources = mutableMapOf<String, Long>()
        private val resourceHistory = mutableListOf<ResourceEvent>()
        
        data class ResourceEvent(
            val timestamp: Instant,
            val testName: String,
            val resourceType: String,
            val action: String, // "allocate" or "deallocate"
            val amount: Long
        )
        
        fun trackAllocation(testName: String, resourceType: String, amount: Long) {
            val key = "$testName-$resourceType"
            allocatedResources[key] = (allocatedResources[key] ?: 0) + amount
            
            resourceHistory.add(
                ResourceEvent(
                    timestamp = Clock.System.now(),
                    testName = testName,
                    resourceType = resourceType,
                    action = "allocate",
                    amount = amount
                )
            )
        }
        
        fun trackDeallocation(testName: String, resourceType: String, amount: Long) {
            val key = "$testName-$resourceType"
            val current = allocatedResources[key] ?: 0
            allocatedResources[key] = maxOf(0, current - amount)
            
            resourceHistory.add(
                ResourceEvent(
                    timestamp = Clock.System.now(),
                    testName = testName,
                    resourceType = resourceType,
                    action = "deallocate",
                    amount = amount
                )
            )
        }
        
        fun getTotalAllocated(): Long {
            return allocatedResources.values.sum()
        }
        
        fun getLeaks(): List<String> {
            return allocatedResources.entries
                .filter { it.value > 0 }
                .map { "${it.key}: ${it.value} units not deallocated" }
        }
        
        fun reset() {
            allocatedResources.clear()
            resourceHistory.clear()
        }
    }
    
    /**
     * Memory monitor for tracking memory usage patterns
     */
    private class MemoryMonitor {
        private val memorySnapshots = mutableListOf<MemorySnapshot>()
        
        data class MemorySnapshot(
            val timestamp: Instant,
            val testName: String?,
            val usedMemoryMB: Long,
            val allocatedObjects: Int
        )
        
        fun recordSnapshot(testName: String?, usedMemoryMB: Long, allocatedObjects: Int = 0) {
            memorySnapshots.add(
                MemorySnapshot(
                    timestamp = Clock.System.now(),
                    testName = testName,
                    usedMemoryMB = usedMemoryMB,
                    allocatedObjects = allocatedObjects
                )
            )
        }
        
        fun getPeakMemoryUsage(): Long {
            return memorySnapshots.maxOfOrNull { it.usedMemoryMB } ?: 0
        }
        
        fun getMemoryLeaks(): List<String> {
            val leaks = mutableListOf<String>()
            
            // Look for consistently increasing memory usage
            if (memorySnapshots.size >= 5) {
                val recent = memorySnapshots.takeLast(5)
                val isIncreasing = recent.zipWithNext().all { (prev, next) -> 
                    next.usedMemoryMB > prev.usedMemoryMB 
                }
                
                if (isIncreasing) {
                    val increase = recent.last().usedMemoryMB - recent.first().usedMemoryMB
                    leaks.add("Potential memory leak: ${increase}MB increase over last 5 measurements")
                }
            }
            
            return leaks
        }
        
        fun reset() {
            memorySnapshots.clear()
        }
    }
    
    /**
     * Start test suite execution tracking
     */
    fun startTestSuite() {
        suiteStartTime = Clock.System.now()
        testMetrics.clear()
        resourceTracker.reset()
        memoryMonitor.reset()
        currentlyRunningTests.clear()
        
        // Record initial memory snapshot
        memoryMonitor.recordSnapshot(null, estimateCurrentMemoryUsage())
    }
    
    /**
     * End test suite execution tracking
     */
    fun endTestSuite(): TestSuiteMetrics {
        suiteEndTime = Clock.System.now()
        
        // Record final memory snapshot
        memoryMonitor.recordSnapshot(null, estimateCurrentMemoryUsage())
        
        return generateTestSuiteMetrics()
    }
    
    /**
     * Execute a single test with validation
     */
    suspend fun <T> executeValidatedTest(
        testName: String,
        testBody: suspend () -> T
    ): T {
        // Check concurrent test limit
        if (currentlyRunningTests.size >= configuration.maxConcurrentTests) {
            throw IllegalStateException("Too many concurrent tests running: ${currentlyRunningTests.size}")
        }
        
        currentlyRunningTests.add(testName)
        val startTime = Clock.System.now()
        val startMemory = estimateCurrentMemoryUsage()
        
        try {
            // Execute test with timeout
            val result = withTimeout(configuration.maxSingleTestDuration) {
                val executionTime = measureTime {
                    testBody()
                }
                
                // Record successful execution
                recordTestExecution(
                    testName = testName,
                    startTime = startTime,
                    duration = executionTime,
                    memoryUsed = estimateCurrentMemoryUsage() - startMemory,
                    status = TestExecutionMetrics.TestStatus.PASSED
                )
                
                testBody()
            }
            
            return result
            
        } catch (e: TimeoutCancellationException) {
            // Record timeout
            recordTestExecution(
                testName = testName,
                startTime = startTime,
                duration = Clock.System.now() - startTime,
                memoryUsed = estimateCurrentMemoryUsage() - startMemory,
                status = TestExecutionMetrics.TestStatus.TIMEOUT,
                errorMessage = "Test exceeded maximum duration of ${configuration.maxSingleTestDuration}"
            )
            throw AssertionError("Test '$testName' timed out after ${configuration.maxSingleTestDuration}")
            
        } catch (e: OutOfMemoryError) {
            // Record memory exceeded
            recordTestExecution(
                testName = testName,
                startTime = startTime,
                duration = Clock.System.now() - startTime,
                memoryUsed = estimateCurrentMemoryUsage() - startMemory,
                status = TestExecutionMetrics.TestStatus.MEMORY_EXCEEDED,
                errorMessage = "Test exceeded memory limits"
            )
            throw AssertionError("Test '$testName' exceeded memory limits", e)
            
        } catch (e: Exception) {
            // Record failure
            recordTestExecution(
                testName = testName,
                startTime = startTime,
                duration = Clock.System.now() - startTime,
                memoryUsed = estimateCurrentMemoryUsage() - startMemory,
                status = TestExecutionMetrics.TestStatus.FAILED,
                errorMessage = e.message
            )
            throw e
            
        } finally {
            currentlyRunningTests.remove(testName)
            
            // Check for resource leaks
            val leaks = resourceTracker.getLeaks()
            if (leaks.isNotEmpty()) {
                println("Warning: Resource leaks detected in test '$testName':")
                leaks.forEach { leak ->
                    println("  - $leak")
                }
            }
            
            // Record memory snapshot
            memoryMonitor.recordSnapshot(testName, estimateCurrentMemoryUsage())
        }
    }
    
    /**
     * Record test execution metrics
     */
    private fun recordTestExecution(
        testName: String,
        startTime: Instant,
        duration: Duration,
        memoryUsed: Long,
        status: TestExecutionMetrics.TestStatus,
        errorMessage: String? = null
    ) {
        val endTime = startTime + duration
        
        testMetrics.add(
            TestExecutionMetrics(
                testName = testName,
                startTime = startTime,
                endTime = endTime,
                duration = duration,
                memoryUsedMB = memoryUsed,
                resourceCount = resourceTracker.getTotalAllocated().toInt(),
                status = status,
                errorMessage = errorMessage
            )
        )
        
        // Check warning thresholds
        checkWarningThresholds(testName, duration, memoryUsed)
    }
    
    /**
     * Check if test execution is approaching limits
     */
    private fun checkWarningThresholds(testName: String, duration: Duration, memoryUsed: Long) {
        val durationThreshold = configuration.maxSingleTestDuration * configuration.warningThresholdPercent
        val memoryThreshold = configuration.maxMemoryUsageMB * configuration.warningThresholdPercent
        
        if (duration > durationThreshold) {
            println("Warning: Test '$testName' took ${duration.inWholeMilliseconds}ms (threshold: ${durationThreshold.inWholeMilliseconds}ms)")
        }
        
        if (memoryUsed > memoryThreshold) {
            println("Warning: Test '$testName' used ${memoryUsed}MB memory (threshold: ${memoryThreshold}MB)")
        }
    }
    
    /**
     * Generate test suite metrics
     */
    private fun generateTestSuiteMetrics(): TestSuiteMetrics {
        val totalDuration = if (suiteStartTime != null && suiteEndTime != null) {
            suiteEndTime!! - suiteStartTime!!
        } else {
            Duration.ZERO
        }
        
        val passedTests = testMetrics.count { it.status == TestExecutionMetrics.TestStatus.PASSED }
        val failedTests = testMetrics.count { it.status == TestExecutionMetrics.TestStatus.FAILED }
        val timeoutTests = testMetrics.count { it.status == TestExecutionMetrics.TestStatus.TIMEOUT }
        val memoryExceededTests = testMetrics.count { it.status == TestExecutionMetrics.TestStatus.MEMORY_EXCEEDED }
        val resourceLeakTests = testMetrics.count { it.status == TestExecutionMetrics.TestStatus.RESOURCE_LEAK }
        
        val averageDuration = if (testMetrics.isNotEmpty()) {
            val totalMs = testMetrics.sumOf { it.duration.inWholeMilliseconds }
            (totalMs / testMetrics.size).milliseconds
        } else {
            Duration.ZERO
        }
        
        val maxDuration = testMetrics.maxOfOrNull { it.duration } ?: Duration.ZERO
        val peakMemoryUsage = memoryMonitor.getPeakMemoryUsage()
        val totalResourcesAllocated = resourceTracker.getTotalAllocated()
        val resourceLeaks = resourceTracker.getLeaks() + memoryMonitor.getMemoryLeaks()
        
        return TestSuiteMetrics(
            totalTests = testMetrics.size,
            passedTests = passedTests,
            failedTests = failedTests,
            timeoutTests = timeoutTests,
            memoryExceededTests = memoryExceededTests,
            resourceLeakTests = resourceLeakTests,
            totalDuration = totalDuration,
            averageDuration = averageDuration,
            maxDuration = maxDuration,
            peakMemoryUsageMB = peakMemoryUsage,
            totalResourcesAllocated = totalResourcesAllocated,
            resourceLeaks = resourceLeaks
        )
    }
    
    /**
     * Validate test suite performance
     */
    fun validateTestSuitePerformance(): ValidationResult {
        val metrics = generateTestSuiteMetrics()
        val issues = mutableListOf<String>()
        val recommendations = mutableListOf<String>()
        
        // Check overall duration
        if (metrics.totalDuration > configuration.maxTestSuiteDuration) {
            issues.add("Test suite duration (${metrics.totalDuration}) exceeds limit (${configuration.maxTestSuiteDuration})")
            recommendations.add("Consider parallelizing tests or optimizing slow tests")
        }
        
        // Check timeout tests
        if (metrics.timeoutTests > 0) {
            issues.add("${metrics.timeoutTests} tests timed out")
            recommendations.add("Review timeout tests and optimize or increase timeout limits")
        }
        
        // Check memory exceeded tests
        if (metrics.memoryExceededTests > 0) {
            issues.add("${metrics.memoryExceededTests} tests exceeded memory limits")
            recommendations.add("Review memory usage in failing tests and optimize data structures")
        }
        
        // Check resource leaks
        if (metrics.resourceLeaks.isNotEmpty()) {
            issues.add("${metrics.resourceLeaks.size} resource leaks detected")
            recommendations.add("Review resource cleanup in tests with leaks")
        }
        
        // Check success rate
        if (metrics.successRate < 0.95) {
            issues.add("Low test success rate: ${(metrics.successRate * 100).toInt()}%")
            recommendations.add("Investigate and fix failing tests")
        }
        
        // Check average test duration
        if (metrics.averageDuration > 5.seconds) {
            recommendations.add("Average test duration is high (${metrics.averageDuration.inWholeMilliseconds}ms) - consider optimization")
        }
        
        return ValidationResult(
            isValid = issues.isEmpty(),
            issues = issues,
            recommendations = recommendations,
            metrics = metrics
        )
    }
    
    data class ValidationResult(
        val isValid: Boolean,
        val issues: List<String>,
        val recommendations: List<String>,
        val metrics: TestSuiteMetrics
    )
    
    /**
     * Generate comprehensive performance report
     */
    fun generatePerformanceReport(): String {
        val metrics = generateTestSuiteMetrics()
        val validation = validateTestSuitePerformance()
        
        return buildString {
            appendLine("=== Test Execution Performance Report ===")
            appendLine("Total Tests: ${metrics.totalTests}")
            appendLine("Success Rate: ${(metrics.successRate * 100).toInt()}%")
            appendLine("Total Duration: ${metrics.totalDuration}")
            appendLine("Average Duration: ${metrics.averageDuration.inWholeMilliseconds}ms")
            appendLine("Max Duration: ${metrics.maxDuration.inWholeMilliseconds}ms")
            appendLine("Peak Memory Usage: ${metrics.peakMemoryUsageMB}MB")
            
            appendLine("\nTest Results:")
            appendLine("  Passed: ${metrics.passedTests}")
            appendLine("  Failed: ${metrics.failedTests}")
            appendLine("  Timeout: ${metrics.timeoutTests}")
            appendLine("  Memory Exceeded: ${metrics.memoryExceededTests}")
            appendLine("  Resource Leaks: ${metrics.resourceLeakTests}")
            
            if (!validation.isValid) {
                appendLine("\nIssues Found:")
                validation.issues.forEach { issue ->
                    appendLine("  - $issue")
                }
                
                appendLine("\nRecommendations:")
                validation.recommendations.forEach { rec ->
                    appendLine("  - $rec")
                }
            }
            
            if (metrics.resourceLeaks.isNotEmpty()) {
                appendLine("\nResource Leaks:")
                metrics.resourceLeaks.forEach { leak ->
                    appendLine("  - $leak")
                }
            }
            
            // Show slowest tests
            val slowestTests = testMetrics
                .sortedByDescending { it.duration }
                .take(5)
            
            if (slowestTests.isNotEmpty()) {
                appendLine("\nSlowest Tests:")
                slowestTests.forEach { test ->
                    appendLine("  ${test.testName}: ${test.duration.inWholeMilliseconds}ms")
                }
            }
            
            // Show memory-intensive tests
            val memoryIntensiveTests = testMetrics
                .sortedByDescending { it.memoryUsedMB }
                .take(5)
            
            if (memoryIntensiveTests.isNotEmpty()) {
                appendLine("\nMemory-Intensive Tests:")
                memoryIntensiveTests.forEach { test ->
                    appendLine("  ${test.testName}: ${test.memoryUsedMB}MB")
                }
            }
        }
    }
    
    /**
     * Get individual test metrics
     */
    fun getTestMetrics(testName: String): TestExecutionMetrics? {
        return testMetrics.find { it.testName == testName }
    }
    
    /**
     * Get all test metrics
     */
    fun getAllTestMetrics(): List<TestExecutionMetrics> {
        return testMetrics.toList()
    }
    
    /**
     * Reset all tracking data
     */
    fun reset() {
        testMetrics.clear()
        resourceTracker.reset()
        memoryMonitor.reset()
        currentlyRunningTests.clear()
        suiteStartTime = null
        suiteEndTime = null
    }
    
    /**
     * Estimate current memory usage (simplified implementation)
     */
    private fun estimateCurrentMemoryUsage(): Long {
        // In a real implementation, this would use actual memory monitoring
        // For testing purposes, we'll estimate based on tracked resources
        return resourceTracker.getTotalAllocated() / 1024 // Convert to MB estimate
    }
    
    /**
     * Track resource allocation for a test
     */
    fun trackResourceAllocation(testName: String, resourceType: String, amount: Long) {
        resourceTracker.trackAllocation(testName, resourceType, amount)
    }
    
    /**
     * Track resource deallocation for a test
     */
    fun trackResourceDeallocation(testName: String, resourceType: String, amount: Long) {
        resourceTracker.trackDeallocation(testName, resourceType, amount)
    }
}

/**
 * Extension functions for easier integration
 */
suspend fun <T> TestExecutionValidator.runValidatedTest(
    testName: String,
    testBody: suspend () -> T
): T {
    return executeValidatedTest(testName, testBody)
}

/**
 * Test execution validator builder for easy configuration
 */
class TestExecutionValidatorBuilder {
    private var maxSingleTestDuration: Duration = 30.seconds
    private var maxTestSuiteDuration: Duration = 10.minutes
    private var maxMemoryUsageMB: Long = 512
    private var maxConcurrentTests: Int = 5
    
    fun maxSingleTestDuration(duration: Duration) = apply {
        maxSingleTestDuration = duration
    }
    
    fun maxTestSuiteDuration(duration: Duration) = apply {
        maxTestSuiteDuration = duration
    }
    
    fun maxMemoryUsageMB(memoryMB: Long) = apply {
        maxMemoryUsageMB = memoryMB
    }
    
    fun maxConcurrentTests(count: Int) = apply {
        maxConcurrentTests = count
    }
    
    fun build(): TestExecutionValidator {
        return TestExecutionValidator()
    }
}

fun testExecutionValidator(configure: TestExecutionValidatorBuilder.() -> Unit): TestExecutionValidator {
    return TestExecutionValidatorBuilder().apply(configure).build()
}