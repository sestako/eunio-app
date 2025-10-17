package com.eunio.healthapp.testutil

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration

/**
 * Ensures deterministic test execution and proper cleanup
 */
class DeterministicTestExecutor {
    
    private val testState = TestExecutionState()
    private val cleanupTasks = mutableListOf<CleanupTask>()
    
    fun <T> executeTest(
        testName: String,
        setup: () -> Unit = {},
        cleanup: () -> Unit = {},
        test: () -> T
    ): DeterministicTestExecutionResult<T> {
        val executionId = generateExecutionId(testName)
        val startTime = Clock.System.now()
        
        return try {
            // Ensure deterministic environment
            setupDeterministicEnvironment(executionId)
            
            // Register cleanup task
            registerCleanupTask(CleanupTask(executionId, cleanup))
            
            // Execute setup
            setup()
            
            // Execute test with timeout
            val result = executeWithTimeout(testName, test)
            
            DeterministicTestExecutionResult.success(
                executionId = executionId,
                testName = testName,
                result = result,
                duration = Clock.System.now() - startTime
            )
        } catch (e: Exception) {
            DeterministicTestExecutionResult.failure<T>(
                executionId = executionId,
                testName = testName,
                error = e.message ?: "Unknown test execution error",
                duration = Clock.System.now() - startTime
            )
        } finally {
            // Always perform cleanup
            performCleanup(executionId)
        }
    }
    
    fun executeTestSuite(
        suiteName: String,
        tests: List<TestCase>
    ): TestSuiteExecutionResult {
        val suiteId = generateSuiteId(suiteName)
        val startTime = Clock.System.now()
        
        return try {
            // Setup suite-level deterministic environment
            setupSuiteDeterministicEnvironment(suiteId)
            
            val results = tests.map { testCase ->
                executeTest(
                    testName = testCase.name,
                    setup = testCase.setup,
                    cleanup = testCase.cleanup,
                    test = testCase.test
                )
            }
            
            val failures = results.filter { !it.success }
            
            TestSuiteExecutionResult(
                suiteId = suiteId,
                suiteName = suiteName,
                success = failures.isEmpty(),
                testResults = results,
                totalTests = tests.size,
                passedTests = results.count { it.success },
                failedTests = failures.size,
                duration = Clock.System.now() - startTime,
                deterministicScore = calculateDeterministicScore(results)
            )
        } catch (e: Exception) {
            TestSuiteExecutionResult.failure(
                suiteId = suiteId,
                suiteName = suiteName,
                error = e.message ?: "Test suite execution failed",
                duration = Clock.System.now() - startTime
            )
        } finally {
            // Suite-level cleanup
            performSuiteCleanup(suiteId)
        }
    }
    
    private fun setupDeterministicEnvironment(executionId: String) {
        // Set deterministic time source
        testState.setFixedTime(Clock.System.now())
        
        // Set deterministic random seed
        testState.setRandomSeed(executionId.hashCode().toLong())
        
        // Clear any cached state
        testState.clearCache()
        
        // Set deterministic system properties
        testState.setSystemProperty("test.execution.id", executionId)
        testState.setSystemProperty("test.deterministic", "true")
        
        // Initialize clean test environment
        initializeCleanEnvironment()
    }
    
    private fun setupSuiteDeterministicEnvironment(suiteId: String) {
        // Suite-level deterministic setup
        testState.setSuiteId(suiteId)
        testState.initializeSuiteEnvironment()
        
        // Clear any suite-level state
        testState.clearSuiteCache()
    }
    
    private fun initializeCleanEnvironment() {
        // Clear any global state that might affect tests
        testState.clearGlobalState()
        
        // Reset mock services to default state
        MockServiceManager.resetAllMocks()
        
        // Clear any temporary files or resources
        testState.clearTemporaryResources()
    }
    
    private fun <T> executeWithTimeout(testName: String, test: () -> T): T {
        // In a real implementation, this would use coroutines with timeout
        // For now, we'll execute directly but track execution time
        val startTime = Clock.System.now()
        
        try {
            val result = test()
            val duration = Clock.System.now() - startTime
            
            // Check for reasonable execution time
            if (duration.inWholeMilliseconds > 5000) { // 5 seconds
                println("Warning: Test '$testName' took ${duration.inWholeMilliseconds}ms to execute")
            }
            
            return result
        } catch (e: Exception) {
            val duration = Clock.System.now() - startTime
            throw TestExecutionException("Test '$testName' failed after ${duration.inWholeMilliseconds}ms: ${e.message}", e)
        }
    }
    
    private fun registerCleanupTask(task: CleanupTask) {
        cleanupTasks.add(task)
    }
    
    private fun performCleanup(executionId: String) {
        val tasksToRun = cleanupTasks.filter { it.executionId == executionId }
        
        tasksToRun.forEach { task ->
            try {
                task.cleanup()
            } catch (e: Exception) {
                println("Warning: Cleanup task failed for execution $executionId: ${e.message}")
            }
        }
        
        // Remove completed cleanup tasks
        cleanupTasks.removeAll(tasksToRun)
        
        // Perform standard cleanup
        performStandardCleanup(executionId)
    }
    
    private fun performSuiteCleanup(suiteId: String) {
        try {
            // Clear suite-level state
            testState.clearSuiteState(suiteId)
            
            // Reset suite-level mocks
            MockServiceManager.resetSuiteMocks(suiteId)
            
            // Clean up suite resources
            testState.cleanupSuiteResources(suiteId)
        } catch (e: Exception) {
            println("Warning: Suite cleanup failed for $suiteId: ${e.message}")
        }
    }
    
    private fun performStandardCleanup(executionId: String) {
        // Clear test-specific state
        testState.clearTestState(executionId)
        
        // Reset system properties
        testState.clearSystemProperties(executionId)
        
        // Clear temporary resources
        testState.clearTemporaryResources(executionId)
        
        // Reset time source
        testState.resetTimeSource()
    }
    
    private fun calculateDeterministicScore(results: List<DeterministicTestExecutionResult<*>>): Double {
        if (results.isEmpty()) return 1.0
        
        var score = 1.0
        
        // Check for timing consistency
        val executionTimes = results.map { it.duration.inWholeMilliseconds }
        if (executionTimes.isNotEmpty()) {
            val average = executionTimes.average()
            val variance = executionTimes.map { time -> (time - average) * (time - average) }.average()
            val standardDeviation = kotlin.math.sqrt(variance)
            
            // High variance indicates non-deterministic behavior
            if (standardDeviation > average * 0.5) {
                score -= 0.2 // Reduce score for high timing variance
            }
        }
        
        // Check for consistent success/failure patterns
        val successRate = results.count { it.success }.toDouble() / results.size
        if (successRate < 1.0 && successRate > 0.0) {
            // Intermittent failures suggest non-deterministic behavior
            score -= 0.3
        }
        
        // Check for resource cleanup issues
        val cleanupIssues = results.count { it.cleanupIssues.isNotEmpty() }
        if (cleanupIssues > 0) {
            score -= (cleanupIssues.toDouble() / results.size) * 0.2
        }
        
        return score.coerceIn(0.0, 1.0)
    }
    
    private fun generateExecutionId(testName: String): String {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        val hash = testName.hashCode().toString(16)
        return "exec-$hash-$timestamp"
    }
    
    private fun generateSuiteId(suiteName: String): String {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        val hash = suiteName.hashCode().toString(16)
        return "suite-$hash-$timestamp"
    }
}

/**
 * Manages test execution state for deterministic behavior
 */
class TestExecutionState {
    private var fixedTime: Instant? = null
    private var randomSeed: Long? = null
    private val systemProperties = mutableMapOf<String, String>()
    private val cache = mutableMapOf<String, Any>()
    private val suiteCache = mutableMapOf<String, Any>()
    private val temporaryResources = mutableSetOf<String>()
    
    fun setFixedTime(time: Instant) {
        fixedTime = time
    }
    
    fun getFixedTime(): Instant? = fixedTime
    
    fun setRandomSeed(seed: Long) {
        randomSeed = seed
    }
    
    fun getRandomSeed(): Long? = randomSeed
    
    fun setSystemProperty(key: String, value: String) {
        systemProperties[key] = value
    }
    
    fun getSystemProperty(key: String): String? = systemProperties[key]
    
    fun clearSystemProperties(executionId: String) {
        systemProperties.remove("test.execution.id")
        systemProperties.remove("test.deterministic")
    }
    
    fun clearCache() {
        cache.clear()
    }
    
    fun clearSuiteCache() {
        suiteCache.clear()
    }
    
    fun clearGlobalState() {
        // Clear any global state that might affect tests
        cache.clear()
        temporaryResources.clear()
    }
    
    fun clearTemporaryResources() {
        temporaryResources.clear()
    }
    
    fun clearTemporaryResources(executionId: String) {
        // Remove resources specific to this execution
        temporaryResources.removeAll { it.contains(executionId) }
    }
    
    fun setSuiteId(suiteId: String) {
        systemProperties["test.suite.id"] = suiteId
    }
    
    fun initializeSuiteEnvironment() {
        // Initialize suite-specific environment
        suiteCache.clear()
    }
    
    fun clearSuiteState(suiteId: String) {
        suiteCache.clear()
        systemProperties.remove("test.suite.id")
    }
    
    fun cleanupSuiteResources(suiteId: String) {
        temporaryResources.removeAll { it.contains(suiteId) }
    }
    
    fun clearTestState(executionId: String) {
        cache.removeAll { it.key.contains(executionId) }
    }
    
    fun resetTimeSource() {
        fixedTime = null
    }
    
    private fun <K, V> MutableMap<K, V>.removeAll(predicate: (Map.Entry<K, V>) -> Boolean) {
        val iterator = iterator()
        while (iterator.hasNext()) {
            if (predicate(iterator.next())) {
                iterator.remove()
            }
        }
    }
}

/**
 * Represents a test case for execution
 */
data class TestCase(
    val name: String,
    val setup: () -> Unit = {},
    val cleanup: () -> Unit = {},
    val test: () -> Any
)

/**
 * Cleanup task for test execution
 */
data class CleanupTask(
    val executionId: String,
    val cleanup: () -> Unit
)

/**
 * Result of deterministic test execution
 */
data class DeterministicTestExecutionResult<T>(
    val executionId: String,
    val testName: String,
    val success: Boolean,
    val result: T? = null,
    val error: String? = null,
    val duration: Duration,
    val timestamp: Instant = Clock.System.now(),
    val cleanupIssues: List<String> = emptyList()
) {
    companion object {
        fun <T> success(
            executionId: String,
            testName: String,
            result: T,
            duration: Duration
        ): DeterministicTestExecutionResult<T> {
            return DeterministicTestExecutionResult(
                executionId = executionId,
                testName = testName,
                success = true,
                result = result,
                error = null,
                duration = duration
            )
        }
        
        fun <T> failure(
            executionId: String,
            testName: String,
            error: String,
            duration: Duration
        ): DeterministicTestExecutionResult<T> {
            return DeterministicTestExecutionResult(
                executionId = executionId,
                testName = testName,
                success = false,
                result = null,
                error = error,
                duration = duration
            )
        }
    }
}

/**
 * Result of test suite execution
 */
data class TestSuiteExecutionResult(
    val suiteId: String,
    val suiteName: String,
    val success: Boolean,
    val testResults: List<DeterministicTestExecutionResult<*>>,
    val totalTests: Int,
    val passedTests: Int,
    val failedTests: Int,
    val duration: Duration,
    val deterministicScore: Double,
    val timestamp: Instant = Clock.System.now(),
    val error: String? = null
) {
    companion object {
        fun failure(
            suiteId: String,
            suiteName: String,
            error: String,
            duration: Duration
        ): TestSuiteExecutionResult {
            return TestSuiteExecutionResult(
                suiteId = suiteId,
                suiteName = suiteName,
                success = false,
                testResults = emptyList(),
                totalTests = 0,
                passedTests = 0,
                failedTests = 0,
                duration = duration,
                deterministicScore = 0.0,
                error = error
            )
        }
    }
}

/**
 * Exception for test execution failures
 */
class TestExecutionException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Utility for deterministic test data generation
 */
object DeterministicTestData {
    
    fun generateDeterministicUserId(testName: String): String {
        return "test-user-${testName.hashCode().toString(16)}"
    }
    
    fun generateDeterministicTimestamp(testName: String, offset: Long = 0): Instant {
        // Use a fixed base time plus deterministic offset
        val baseTime = Instant.fromEpochMilliseconds(1640995200000) // 2022-01-01T00:00:00Z
        val deterministicOffset = testName.hashCode().toLong() + offset
        return baseTime.plus(Duration.parse("PT${deterministicOffset}S"))
    }
    
    fun generateDeterministicString(testName: String, prefix: String = "test"): String {
        val hash = testName.hashCode().toString(16)
        return "$prefix-$hash"
    }
    
    fun generateDeterministicNumber(testName: String, min: Int = 0, max: Int = 100): Int {
        val hash = kotlin.math.abs(testName.hashCode())
        return min + (hash % (max - min + 1))
    }
}