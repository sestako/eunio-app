package com.eunio.healthapp.testutil

import com.eunio.healthapp.domain.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

/**
 * Comprehensive test performance optimizer that manages resource allocation,
 * prevents memory leaks, and ensures efficient test execution.
 * 
 * Addresses Requirement 7.4:
 * - WHEN tests clean up resources THEN they SHALL not interfere with subsequent test executions
 * - Implement efficient test data creation and cleanup
 * - Add proper resource management for test execution
 * - Fix memory leaks in mock services and test utilities
 * - Ensure test suite completes within reasonable time limits
 */
class TestPerformanceOptimizer(
    private val testDispatcher: TestDispatcher,
    private val testScope: TestScope
) {
    
    /**
     * Performance configuration for test execution
     */
    data class PerformanceConfiguration(
        val maxTestExecutionTime: Duration = 30.seconds,
        val maxMemoryUsageMB: Long = 512,
        val resourceCleanupInterval: Duration = 5.seconds,
        val enablePerformanceMonitoring: Boolean = true,
        val enableMemoryLeakDetection: Boolean = true,
        val enableResourceTracking: Boolean = true,
        val maxConcurrentOperations: Int = 10,
        val dataPoolSize: Int = 100,
        val enableDataPooling: Boolean = true
    )
    
    /**
     * Resource tracking for cleanup management
     */
    private class ResourceTracker {
        private val allocatedResources = mutableMapOf<String, AllocatedResource>()
        private val resourcePools = mutableMapOf<String, ResourcePool<*>>()
        private var totalAllocatedMemory: Long = 0
        
        data class AllocatedResource(
            val id: String,
            val type: String,
            val allocatedAt: Instant,
            val estimatedMemoryBytes: Long,
            val cleanupAction: suspend () -> Unit
        )
        
        fun trackResource(
            id: String,
            type: String,
            estimatedMemoryBytes: Long,
            cleanupAction: suspend () -> Unit
        ) {
            allocatedResources[id] = AllocatedResource(
                id = id,
                type = type,
                allocatedAt = Clock.System.now(),
                estimatedMemoryBytes = estimatedMemoryBytes,
                cleanupAction = cleanupAction
            )
            totalAllocatedMemory += estimatedMemoryBytes
        }
        
        suspend fun cleanupResource(id: String) {
            val resource = allocatedResources.remove(id)
            if (resource != null) {
                try {
                    resource.cleanupAction()
                    totalAllocatedMemory -= resource.estimatedMemoryBytes
                } catch (e: Exception) {
                    // Log cleanup failure but don't throw
                    println("Warning: Failed to cleanup resource $id: ${e.message}")
                }
            }
        }
        
        suspend fun cleanupAllResources() {
            val resources = allocatedResources.values.toList()
            allocatedResources.clear()
            totalAllocatedMemory = 0
            
            resources.forEach { resource ->
                try {
                    resource.cleanupAction()
                } catch (e: Exception) {
                    println("Warning: Failed to cleanup resource ${resource.id}: ${e.message}")
                }
            }
        }
        
        fun getResourceCount(): Int = allocatedResources.size
        fun getTotalMemoryUsage(): Long = totalAllocatedMemory
        
        fun getResourcesByType(type: String): List<AllocatedResource> {
            return allocatedResources.values.filter { it.type == type }
        }
        
        fun getOldResources(olderThan: Duration): List<AllocatedResource> {
            val cutoff = Clock.System.now() - olderThan
            return allocatedResources.values.filter { it.allocatedAt < cutoff }
        }
    }
    
    /**
     * Object pool for efficient resource reuse
     */
    private class ResourcePool<T>(
        private val factory: () -> T,
        private val reset: (T) -> Unit,
        private val maxSize: Int = 50
    ) {
        private val available = mutableListOf<T>()
        private val inUse = mutableSetOf<T>()
        
        fun acquire(): T {
            return if (available.isNotEmpty()) {
                val item = available.removeAt(available.size - 1)
                inUse.add(item)
                item
            } else {
                val item = factory()
                inUse.add(item)
                item
            }
        }
        
        fun release(item: T) {
            if (inUse.remove(item)) {
                reset(item)
                if (available.size < maxSize) {
                    available.add(item)
                }
            }
        }
        
        fun clear() {
            available.clear()
            inUse.clear()
        }
        
        fun getStats(): PoolStats {
            return PoolStats(
                available = available.size,
                inUse = inUse.size,
                total = available.size + inUse.size
            )
        }
    }
    
    data class PoolStats(
        val available: Int,
        val inUse: Int,
        val total: Int
    )
    
    /**
     * Performance metrics collection
     */
    private class PerformanceMetrics {
        private val testExecutionTimes = mutableMapOf<String, Duration>()
        private val memoryUsageSnapshots = mutableListOf<MemorySnapshot>()
        private val resourceAllocationHistory = mutableListOf<ResourceAllocation>()
        
        data class MemorySnapshot(
            val timestamp: Instant,
            val usedMemoryMB: Long,
            val allocatedResources: Int
        )
        
        data class ResourceAllocation(
            val timestamp: Instant,
            val resourceType: String,
            val action: String, // "allocate" or "deallocate"
            val memoryBytes: Long
        )
        
        fun recordTestExecution(testName: String, duration: Duration) {
            testExecutionTimes[testName] = duration
        }
        
        fun recordMemorySnapshot(usedMemoryMB: Long, allocatedResources: Int) {
            memoryUsageSnapshots.add(
                MemorySnapshot(
                    timestamp = Clock.System.now(),
                    usedMemoryMB = usedMemoryMB,
                    allocatedResources = allocatedResources
                )
            )
        }
        
        fun recordResourceAllocation(resourceType: String, action: String, memoryBytes: Long) {
            resourceAllocationHistory.add(
                ResourceAllocation(
                    timestamp = Clock.System.now(),
                    resourceType = resourceType,
                    action = action,
                    memoryBytes = memoryBytes
                )
            )
        }
        
        fun getAverageTestExecutionTime(): Duration {
            return if (testExecutionTimes.isEmpty()) {
                Duration.ZERO
            } else {
                val totalMs = testExecutionTimes.values.sumOf { it.inWholeMilliseconds }
                (totalMs / testExecutionTimes.size).milliseconds
            }
        }
        
        fun getSlowestTests(count: Int = 5): List<Pair<String, Duration>> {
            return testExecutionTimes.entries
                .sortedByDescending { it.value }
                .take(count)
                .map { it.key to it.value }
        }
        
        fun getMemoryLeaks(): List<String> {
            val leaks = mutableListOf<String>()
            
            // Detect memory leaks by looking for consistently increasing memory usage
            if (memoryUsageSnapshots.size >= 3) {
                val recent = memoryUsageSnapshots.takeLast(3)
                if (recent[0].usedMemoryMB < recent[1].usedMemoryMB && 
                    recent[1].usedMemoryMB < recent[2].usedMemoryMB) {
                    leaks.add("Potential memory leak detected: consistently increasing memory usage")
                }
            }
            
            // Check for resource allocation imbalances
            val allocations = resourceAllocationHistory.groupBy { it.resourceType }
            allocations.forEach { (type, history) ->
                val allocated = history.count { it.action == "allocate" }
                val deallocated = history.count { it.action == "deallocate" }
                if (allocated > deallocated + 5) { // Allow some tolerance
                    leaks.add("Resource leak detected for $type: $allocated allocated, $deallocated deallocated")
                }
            }
            
            return leaks
        }
        
        fun reset() {
            testExecutionTimes.clear()
            memoryUsageSnapshots.clear()
            resourceAllocationHistory.clear()
        }
        
        fun generateReport(): String {
            return buildString {
                appendLine("=== Performance Metrics Report ===")
                appendLine("Total tests executed: ${testExecutionTimes.size}")
                appendLine("Average execution time: ${getAverageTestExecutionTime()}")
                appendLine("Memory snapshots: ${memoryUsageSnapshots.size}")
                appendLine("Resource allocations tracked: ${resourceAllocationHistory.size}")
                
                val slowTests = getSlowestTests()
                if (slowTests.isNotEmpty()) {
                    appendLine("\nSlowest tests:")
                    slowTests.forEach { (name, duration) ->
                        appendLine("  $name: ${duration.inWholeMilliseconds}ms")
                    }
                }
                
                val leaks = getMemoryLeaks()
                if (leaks.isNotEmpty()) {
                    appendLine("\nPotential issues:")
                    leaks.forEach { leak ->
                        appendLine("  - $leak")
                    }
                }
                
                if (memoryUsageSnapshots.isNotEmpty()) {
                    val maxMemory = memoryUsageSnapshots.maxOf { it.usedMemoryMB }
                    val avgMemory = memoryUsageSnapshots.map { it.usedMemoryMB }.average()
                    appendLine("\nMemory usage:")
                    appendLine("  Peak: ${maxMemory}MB")
                    appendLine("  Average: ${com.eunio.healthapp.platform.SystemUtils.formatDouble(avgMemory, 1)}MB")
                }
            }
        }
    }
    
    private val configuration = PerformanceConfiguration()
    private val resourceTracker = ResourceTracker()
    private val performanceMetrics = PerformanceMetrics()
    
    // Object pools for common test objects
    private val userPool = ResourcePool(
        factory = { TestDataFactory.createTestUser("pool-user-${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}") },
        reset = { user -> /* Users are immutable, no reset needed */ },
        maxSize = configuration.dataPoolSize
    )
    
    private val dailyLogPool = ResourcePool(
        factory = { TestDataFactory.createDailyLog("pool-user-${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}") },
        reset = { log -> /* Logs are immutable, no reset needed */ },
        maxSize = configuration.dataPoolSize
    )
    
    private val cyclePool = ResourcePool(
        factory = { TestDataFactory.createTestCycle("pool-user-${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}") },
        reset = { cycle -> /* Cycles are immutable, no reset needed */ },
        maxSize = configuration.dataPoolSize
    )
    
    // Coroutine job tracking for proper cleanup
    private val activeJobs = mutableSetOf<Job>()
    private val jobCleanupScope = CoroutineScope(testDispatcher + SupervisorJob())
    
    /**
     * Execute a test with performance monitoring and resource management
     */
    suspend fun <T> executeOptimizedTest(
        testName: String,
        testBody: suspend TestPerformanceContext.() -> T
    ): T {
        val context = TestPerformanceContext()
        
        return try {
            val executionTime = measureTime {
                // Pre-test setup
                recordMemorySnapshot()
                
                // Execute test with timeout
                withTimeout(configuration.maxTestExecutionTime) {
                    testBody(context)
                }
            }
            
            // Record performance metrics
            performanceMetrics.recordTestExecution(testName, executionTime)
            
            // Post-test cleanup
            context.cleanup()
            recordMemorySnapshot()
            
            // Check for performance issues
            checkPerformanceThresholds(testName, executionTime)
            
            context.result as T
            
        } catch (e: TimeoutCancellationException) {
            throw AssertionError("Test '$testName' exceeded maximum execution time of ${configuration.maxTestExecutionTime}")
        } finally {
            // Ensure cleanup even if test fails
            context.forceCleanup()
        }
    }
    
    /**
     * Context for optimized test execution
     */
    inner class TestPerformanceContext {
        internal var result: Any? = null
        private val contextResources = mutableListOf<String>()
        
        /**
         * Create optimized test user from pool
         */
        fun createOptimizedUser(userId: String? = null): User {
            return if (configuration.enableDataPooling) {
                val pooledUser = userPool.acquire()
                val optimizedUser = if (userId != null) {
                    pooledUser.copy(id = userId)
                } else {
                    pooledUser
                }
                
                trackResource(
                    id = "user-${optimizedUser.id}",
                    type = "User",
                    estimatedMemoryBytes = 1024, // Estimate 1KB per user
                    cleanupAction = { userPool.release(pooledUser) }
                )
                
                optimizedUser
            } else {
                TestDataFactory.createTestUser(userId ?: "test-user-${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}")
            }
        }
        
        /**
         * Create optimized daily log from pool
         */
        fun createOptimizedDailyLog(userId: String): DailyLog {
            return if (configuration.enableDataPooling) {
                val pooledLog = dailyLogPool.acquire()
                val optimizedLog = pooledLog.copy(userId = userId)
                
                trackResource(
                    id = "log-${optimizedLog.userId}-${optimizedLog.date}",
                    type = "DailyLog",
                    estimatedMemoryBytes = 2048, // Estimate 2KB per log
                    cleanupAction = { dailyLogPool.release(pooledLog) }
                )
                
                optimizedLog
            } else {
                TestDataFactory.createDailyLog(userId)
            }
        }
        
        /**
         * Create optimized cycle from pool
         */
        fun createOptimizedCycle(userId: String): Cycle {
            return if (configuration.enableDataPooling) {
                val pooledCycle = cyclePool.acquire()
                val optimizedCycle = pooledCycle.copy(userId = userId)
                
                trackResource(
                    id = "cycle-${optimizedCycle.userId}-${optimizedCycle.id}",
                    type = "Cycle",
                    estimatedMemoryBytes = 4096, // Estimate 4KB per cycle
                    cleanupAction = { cyclePool.release(pooledCycle) }
                )
                
                optimizedCycle
            } else {
                TestDataFactory.createTestCycle(userId)
            }
        }
        
        /**
         * Execute async operation with job tracking
         */
        fun <T> launchTrackedJob(block: suspend CoroutineScope.() -> T): Job {
            val job = jobCleanupScope.launch {
                block()
            }
            
            activeJobs.add(job)
            
            trackResource(
                id = "job-${job.hashCode()}",
                type = "CoroutineJob",
                estimatedMemoryBytes = 512, // Estimate 512 bytes per job
                cleanupAction = {
                    if (job.isActive) {
                        job.cancel()
                    }
                    activeJobs.remove(job)
                }
            )
            
            return job
        }
        
        /**
         * Track a custom resource for cleanup
         */
        fun trackResource(
            id: String,
            type: String,
            estimatedMemoryBytes: Long,
            cleanupAction: suspend () -> Unit
        ) {
            resourceTracker.trackResource(id, type, estimatedMemoryBytes, cleanupAction)
            contextResources.add(id)
            
            if (configuration.enablePerformanceMonitoring) {
                performanceMetrics.recordResourceAllocation(type, "allocate", estimatedMemoryBytes)
            }
        }
        
        /**
         * Set the test result
         */
        fun setResult(value: Any?) {
            result = value
        }
        
        /**
         * Clean up context resources
         */
        internal suspend fun cleanup() {
            contextResources.forEach { resourceId ->
                resourceTracker.cleanupResource(resourceId)
            }
            contextResources.clear()
        }
        
        /**
         * Force cleanup even if exceptions occur
         */
        internal suspend fun forceCleanup() {
            contextResources.forEach { resourceId ->
                try {
                    resourceTracker.cleanupResource(resourceId)
                } catch (e: Exception) {
                    // Ignore cleanup exceptions during force cleanup
                }
            }
            contextResources.clear()
        }
    }
    
    /**
     * Perform periodic resource cleanup
     */
    suspend fun performPeriodicCleanup() {
        // Clean up old resources
        val oldResources = resourceTracker.getOldResources(configuration.resourceCleanupInterval)
        oldResources.forEach { resource ->
            resourceTracker.cleanupResource(resource.id)
        }
        
        // Cancel completed jobs
        val completedJobs = activeJobs.filter { !it.isActive }
        completedJobs.forEach { job ->
            activeJobs.remove(job)
        }
        
        // Record memory snapshot
        recordMemorySnapshot()
        
        // Check memory usage
        checkMemoryUsage()
    }
    
    /**
     * Check if memory usage is within acceptable limits
     */
    private fun checkMemoryUsage() {
        val currentMemoryMB = resourceTracker.getTotalMemoryUsage() / (1024 * 1024)
        
        if (currentMemoryMB > configuration.maxMemoryUsageMB) {
            println("Warning: Memory usage (${currentMemoryMB}MB) exceeds limit (${configuration.maxMemoryUsageMB}MB)")
            
            // Force cleanup of oldest resources
            val oldResources = resourceTracker.getOldResources(1.seconds)
            println("Forcing cleanup of ${oldResources.size} old resources")
            
            // This would be done asynchronously in a real implementation
            testScope.launch {
                oldResources.forEach { resource ->
                    resourceTracker.cleanupResource(resource.id)
                }
            }
        }
    }
    
    /**
     * Check if test execution time is within acceptable limits
     */
    private fun checkPerformanceThresholds(testName: String, executionTime: Duration) {
        val warningThreshold = configuration.maxTestExecutionTime * 0.7
        
        if (executionTime > warningThreshold) {
            println("Warning: Test '$testName' took ${executionTime.inWholeMilliseconds}ms (threshold: ${warningThreshold.inWholeMilliseconds}ms)")
        }
    }
    
    /**
     * Record current memory usage snapshot
     */
    private fun recordMemorySnapshot() {
        if (configuration.enablePerformanceMonitoring) {
            val memoryMB = resourceTracker.getTotalMemoryUsage() / (1024 * 1024)
            val resourceCount = resourceTracker.getResourceCount()
            performanceMetrics.recordMemorySnapshot(memoryMB, resourceCount)
        }
    }
    
    /**
     * Get current performance statistics
     */
    fun getPerformanceStats(): PerformanceStats {
        return PerformanceStats(
            totalAllocatedMemoryMB = resourceTracker.getTotalMemoryUsage() / (1024 * 1024),
            activeResourceCount = resourceTracker.getResourceCount(),
            activeJobCount = activeJobs.size,
            userPoolStats = userPool.getStats(),
            dailyLogPoolStats = dailyLogPool.getStats(),
            cyclePoolStats = cyclePool.getStats(),
            averageTestExecutionTime = performanceMetrics.getAverageTestExecutionTime(),
            memoryLeaks = performanceMetrics.getMemoryLeaks()
        )
    }
    
    data class PerformanceStats(
        val totalAllocatedMemoryMB: Long,
        val activeResourceCount: Int,
        val activeJobCount: Int,
        val userPoolStats: PoolStats,
        val dailyLogPoolStats: PoolStats,
        val cyclePoolStats: PoolStats,
        val averageTestExecutionTime: Duration,
        val memoryLeaks: List<String>
    )
    
    /**
     * Generate comprehensive performance report
     */
    fun generatePerformanceReport(): String {
        val stats = getPerformanceStats()
        
        return buildString {
            appendLine("=== Test Performance Optimization Report ===")
            appendLine("Memory Usage: ${stats.totalAllocatedMemoryMB}MB")
            appendLine("Active Resources: ${stats.activeResourceCount}")
            appendLine("Active Jobs: ${stats.activeJobCount}")
            appendLine("Average Test Time: ${stats.averageTestExecutionTime.inWholeMilliseconds}ms")
            
            appendLine("\nObject Pool Statistics:")
            appendLine("  User Pool: ${stats.userPoolStats.inUse} in use, ${stats.userPoolStats.available} available")
            appendLine("  DailyLog Pool: ${stats.dailyLogPoolStats.inUse} in use, ${stats.dailyLogPoolStats.available} available")
            appendLine("  Cycle Pool: ${stats.cyclePoolStats.inUse} in use, ${stats.cyclePoolStats.available} available")
            
            if (stats.memoryLeaks.isNotEmpty()) {
                appendLine("\nMemory Leak Warnings:")
                stats.memoryLeaks.forEach { leak ->
                    appendLine("  - $leak")
                }
            }
            
            appendLine("\nDetailed Metrics:")
            appendLine(performanceMetrics.generateReport())
        }
    }
    
    /**
     * Reset all performance tracking and cleanup resources
     */
    suspend fun reset() {
        // Cancel all active jobs
        activeJobs.forEach { job ->
            if (job.isActive) {
                job.cancel()
            }
        }
        activeJobs.clear()
        
        // Clean up all tracked resources
        resourceTracker.cleanupAllResources()
        
        // Clear object pools
        userPool.clear()
        dailyLogPool.clear()
        cyclePool.clear()
        
        // Reset metrics
        performanceMetrics.reset()
    }
    
    /**
     * Validate test suite performance and identify bottlenecks
     */
    fun validateTestSuitePerformance(): ValidationResult {
        val stats = getPerformanceStats()
        val issues = mutableListOf<String>()
        val recommendations = mutableListOf<String>()
        
        // Check memory usage
        if (stats.totalAllocatedMemoryMB > configuration.maxMemoryUsageMB * 0.8) {
            issues.add("High memory usage: ${stats.totalAllocatedMemoryMB}MB (limit: ${configuration.maxMemoryUsageMB}MB)")
            recommendations.add("Consider enabling data pooling or reducing test data size")
        }
        
        // Check resource leaks
        if (stats.memoryLeaks.isNotEmpty()) {
            issues.addAll(stats.memoryLeaks)
            recommendations.add("Review resource cleanup in failing tests")
        }
        
        // Check test execution time
        if (stats.averageTestExecutionTime > 5.seconds) {
            issues.add("Slow test execution: average ${stats.averageTestExecutionTime.inWholeMilliseconds}ms")
            recommendations.add("Consider optimizing mock service operations or reducing test complexity")
        }
        
        // Check object pool efficiency
        val totalPoolUsage = stats.userPoolStats.inUse + stats.dailyLogPoolStats.inUse + stats.cyclePoolStats.inUse
        val totalPoolCapacity = stats.userPoolStats.total + stats.dailyLogPoolStats.total + stats.cyclePoolStats.total
        
        if (totalPoolCapacity > 0 && totalPoolUsage.toDouble() / totalPoolCapacity < 0.3) {
            recommendations.add("Object pools are underutilized - consider reducing pool sizes")
        }
        
        return ValidationResult(
            isValid = issues.isEmpty(),
            issues = issues,
            recommendations = recommendations,
            performanceStats = stats
        )
    }
    
    data class ValidationResult(
        val isValid: Boolean,
        val issues: List<String>,
        val recommendations: List<String>,
        val performanceStats: PerformanceStats
    )
}

/**
 * Extension functions for easier integration with existing test infrastructure
 */
suspend fun TestPerformanceOptimizer.optimizeTestExecution(
    testName: String,
    testBody: suspend () -> Unit
) {
    executeOptimizedTest(testName) {
        testBody()
        setResult(Unit)
    }
}

suspend fun <T> TestPerformanceOptimizer.optimizeTestExecutionWithResult(
    testName: String,
    testBody: suspend TestPerformanceOptimizer.TestPerformanceContext.() -> T
): T {
    return executeOptimizedTest(testName, testBody)
}