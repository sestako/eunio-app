package com.eunio.healthapp.testutil

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.*
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.milliseconds
import com.eunio.healthapp.domain.model.*

/**
 * Base class for integration tests that provides proper test state management,
 * async operation handling, and cleanup mechanisms.
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class BaseIntegrationTest {
    
    protected lateinit var testDispatcher: TestDispatcher
    protected lateinit var testScope: TestScope
    protected lateinit var mockServices: MockServices
    protected lateinit var timingManager: TestTimingManager
    protected lateinit var asyncWaiter: AsyncConditionWaiter
    protected lateinit var performanceOptimizer: TestPerformanceOptimizer
    protected lateinit var resourceManager: MockServiceResourceManager
    
    // Track resources for cleanup
    private val registeredMockServices = mutableListOf<Any>()
    private val coroutineJobs = mutableListOf<kotlinx.coroutines.Job>()
    private val testStateManager = TestStateManager()
    
    @BeforeTest
    open fun baseSetup() {
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        
        // Initialize performance optimizer
        performanceOptimizer = TestPerformanceOptimizer(testDispatcher, testScope)
        
        // Initialize resource manager
        resourceManager = MockServiceResourceManager()
        
        // Initialize timing manager with test dispatcher
        timingManager = TestTimingManager(testDispatcher, testScope)
        
        // Initialize async condition waiter
        asyncWaiter = AsyncConditionWaiter(testDispatcher, testScope)
        
        // Initialize mock services with proper state isolation
        mockServices = MockServices()
        mockServices.reset()
        
        // Clear any previous state
        registeredMockServices.clear()
        coroutineJobs.clear()
        testStateManager.resetAllStates()
        
        // Register the main mock services for cleanup
        registerMockService(mockServices)
        
        // Call subclass setup
        setup()
    }
    
    @AfterTest
    open fun baseTeardown() {
        // Call subclass teardown first to allow custom cleanup
        teardown()
        
        // Perform performance optimization cleanup
        runBlocking {
            performanceOptimizer.performPeriodicCleanup()
            performanceOptimizer.reset()
        }
        
        // Clean up resource manager
        resourceManager.performCleanup()
        resourceManager.shutdown()
        
        // Cancel all tracked jobs
        coroutineJobs.forEach { job ->
            if (job.isActive) {
                job.cancel()
            }
        }
        
        // Clean up mock services
        registeredMockServices.forEach { service ->
            when (service) {
                is MockServices -> service.reset()
                is MockUserRepository -> service.reset()
                is MockLogRepository -> service.reset()
                is MockCycleRepository -> service.reset()
                is MockInsightRepository -> service.reset()
                is MockHealthReportRepository -> service.reset()
                // Add other mock service types as needed
            }
        }
        
        // Clear all tracking collections
        registeredMockServices.clear()
        coroutineJobs.clear()
        testStateManager.resetAllStates()
        
        // Reset timing manager
        timingManager.reset()
        
        // Reset UserNotificationService to prevent test interference
        com.eunio.healthapp.domain.util.UserNotificationService.resetForTesting()
        
        // Reset UnitSystemComponentsFactory to prevent test interference
        runBlocking {
            com.eunio.healthapp.domain.util.UnitSystemComponentsFactory.resetForTesting()
        }
        
        // Ensure all async operations complete
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Force cleanup any remaining coroutines to prevent interference
        try {
            // Cancel any remaining jobs in the test scope
            testScope.cancel()
            
            // Wait a bit for cleanup to complete
            testDispatcher.scheduler.advanceTimeBy(100)
            testDispatcher.scheduler.runCurrent()
        } catch (e: Exception) {
            // Ignore cleanup exceptions to prevent test interference
        }
        
        // Generate performance report if needed (for debugging)
        if (com.eunio.healthapp.platform.SystemUtils.getProperty("test.performance.report") == "true") {
            println(performanceOptimizer.generatePerformanceReport())
        }
    }
    
    /**
     * Subclasses should override this for their specific setup
     */
    protected open fun setup() {}
    
    /**
     * Subclasses should override this for their specific teardown
     */
    protected open fun teardown() {}
    
    /**
     * Register a mock service for cleanup tracking
     */
    protected fun <T : Any> registerMockService(service: T): T {
        registeredMockServices.add(service)
        return service
    }
    
    /**
     * Register a coroutine job for cleanup tracking
     */
    protected fun registerJob(job: kotlinx.coroutines.Job): kotlinx.coroutines.Job {
        coroutineJobs.add(job)
        return job
    }
    
    /**
     * Execute a test with proper async handling and timeout
     */
    protected fun runIntegrationTest(
        testBody: suspend TestScope.() -> Unit
    ) = testScope.runTest {
        try {
            testBody()
        } finally {
            // Ensure all async operations complete
            testDispatcher.scheduler.advanceUntilIdle()
        }
    }
    
    /**
     * Wait for a condition to be true with timeout and better error messages
     * Uses the timing manager for deterministic timing
     */
    protected suspend fun waitForCondition(
        timeoutMs: Long = 5000,
        intervalMs: Long = 100,
        condition: suspend () -> Boolean,
        errorMessage: () -> String = { "Condition was not met within timeout" }
    ) {
        val timeout = kotlin.time.Duration.parse("PT${timeoutMs / 1000}S")
        val interval = kotlin.time.Duration.parse("PT${intervalMs / 1000}S")
        
        try {
            timingManager.waitForCondition(
                condition = condition,
                timeout = timeout,
                pollingInterval = interval,
                errorMessage = errorMessage()
            )
        } catch (e: TestTimingException) {
            val diagnostics = getTimingDiagnostics()
            throw AssertionError("${e.message}\n$diagnostics", e)
        }
    }
    
    /**
     * Wait for condition using timing manager with better defaults
     */
    protected suspend fun waitForConditionWithTiming(
        condition: suspend () -> Boolean,
        timeout: kotlin.time.Duration = kotlin.time.Duration.parse("PT5S"),
        errorMessage: String = "Condition not met within timeout"
    ) {
        try {
            timingManager.waitForCondition(
                condition = condition,
                timeout = timeout,
                errorMessage = errorMessage
            )
        } catch (e: TestTimingException) {
            val diagnostics = getTimingDiagnostics()
            throw AssertionError("${e.message}\n$diagnostics", e)
        }
    }
    
    /**
     * Enhanced assertion with diagnostic information
     */
    protected fun assertWithDiagnostics(
        condition: Boolean,
        message: String,
        diagnostics: () -> String = { "" }
    ) {
        if (!condition) {
            val diagnosticInfo = try {
                diagnostics()
            } catch (e: Exception) {
                "Failed to collect diagnostics: ${e.message}"
            }
            
            val fullMessage = if (diagnosticInfo.isNotEmpty()) {
                "$message\nDiagnostics: $diagnosticInfo"
            } else {
                message
            }
            
            throw AssertionError(fullMessage)
        }
    }
    
    /**
     * Get diagnostic information about mock services
     */
    protected fun getMockServiceDiagnostics(): String {
        return buildString {
            appendLine("Mock Services (${registeredMockServices.size}):")
            registeredMockServices.forEachIndexed { index, service ->
                appendLine("  $index: ${service::class.simpleName}")
                when (service) {
                    is MockServices -> {
                        appendLine("    - User Repository: ${service.userRepository.getUserCount()} users")
                        appendLine("    - Active Sessions: ${service.userRepository.getActiveSessionCount()}")
                        // Note: Using suspend function requires runBlocking, so we'll skip this for now
                        appendLine("    - Log Repository: available")
                    }
                    is MockUserRepository -> {
                        appendLine("    - Users: ${service.getUserCount()}")
                        appendLine("    - Active Sessions: ${service.getActiveSessionCount()}")
                    }
                }
            }
            appendLine("Active Jobs: ${coroutineJobs.count { it.isActive }}")
            appendLine("Test Dispatcher: ${testDispatcher::class.simpleName}")
            appendLine("Test State Manager: ${testStateManager.getActiveStateCount()} active states")
        }
    }
    
    /**
     * Advance test time and wait for operations to complete
     */
    protected suspend fun advanceTimeAndWait(timeMs: Long = 1000) {
        val duration = kotlin.time.Duration.parse("PT${timeMs / 1000}S")
        timingManager.advanceTime(duration)
        testDispatcher.scheduler.advanceUntilIdle()
    }
    
    /**
     * Advance time using duration
     */
    protected fun advanceTime(duration: kotlin.time.Duration) {
        timingManager.advanceTime(duration)
    }
    
    /**
     * Execute operation with timing scenario
     */
    protected suspend fun executeWithTiming(
        scenarioName: String,
        operation: suspend () -> Unit
    ) {
        try {
            timingManager.executeWithDelay(scenarioName, operation)
        } catch (e: TestTimingException) {
            val diagnostics = getTimingDiagnostics()
            throw AssertionError("Timing operation '$scenarioName' failed: ${e.message}\n$diagnostics", e)
        }
    }
    
    /**
     * Execute operation with retry and timing
     */
    protected suspend fun executeWithRetryTiming(
        scenarioName: String,
        operation: suspend () -> Unit,
        maxRetries: Int? = null
    ) {
        try {
            timingManager.executeWithRetry(scenarioName, operation, maxRetries)
        } catch (e: TestTimingException) {
            val diagnostics = getTimingDiagnostics()
            throw AssertionError("Retry operation '$scenarioName' failed: ${e.message}\n$diagnostics", e)
        }
    }
    
    /**
     * Create isolated test state for this test
     */
    protected fun createTestState(testName: String): TestState {
        return testStateManager.createIsolatedState(testName)
    }
    
    /**
     * Clean up test state for a specific test
     */
    protected fun cleanupTestState(testName: String) {
        testStateManager.cleanupState(testName)
    }
    
    /**
     * Execute async operation with proper error handling and diagnostics
     */
    protected suspend fun executeWithDiagnostics(
        operationName: String,
        operation: suspend () -> Unit
    ) {
        try {
            operation()
        } catch (e: Exception) {
            val diagnostics = getMockServiceDiagnostics()
            throw AssertionError("Operation '$operationName' failed: ${e.message}\n$diagnostics", e)
        }
    }
    
    /**
     * Wait for async operation to complete with proper error handling
     */
    protected suspend fun waitForAsyncOperation(
        operationName: String,
        timeoutMs: Long = 5000,
        operation: suspend () -> Unit,
        verification: suspend () -> Boolean
    ) {
        try {
            val timeout = kotlin.time.Duration.parse("PT${timeoutMs / 1000}S")
            timingManager.waitForAsyncOperation(
                operationName = operationName,
                operation = operation,
                verification = verification,
                timeout = timeout
            )
        } catch (e: TestTimingException) {
            val diagnostics = getMockServiceDiagnostics() + "\n" + getTimingDiagnostics()
            throw AssertionError("Async operation '$operationName' failed: ${e.message}\n$diagnostics", e)
        } catch (e: Exception) {
            val diagnostics = getMockServiceDiagnostics() + "\n" + getTimingDiagnostics()
            throw AssertionError("Async operation '$operationName' failed: ${e.message}\n$diagnostics", e)
        }
    }
    
    /**
     * Create timestamp separation for deterministic timestamp-based tests
     */
    protected suspend fun createTimestampSeparation(minimumGap: kotlin.time.Duration = kotlin.time.Duration.parse("PT0.01S")) {
        timingManager.createTimestampSeparation(minimumGap)
    }
    
    /**
     * Assert timestamp is within expected range accounting for clock skew
     */
    protected fun assertTimestampWithinRange(
        actual: kotlinx.datetime.Instant,
        expected: kotlinx.datetime.Instant,
        tolerance: kotlin.time.Duration = kotlin.time.Duration.parse("PT1S"),
        message: String = "Timestamp not within expected range"
    ) {
        timingManager.assertTimestampWithinRange(actual, expected, tolerance, message)
    }
    
    /**
     * Register device for multi-device timing tests
     */
    protected fun registerTestDevice(deviceId: String, clockSkew: kotlin.time.Duration = kotlin.time.Duration.ZERO) {
        timingManager.registerDevice(deviceId, clockSkew)
    }
    
    /**
     * Get device time with clock skew
     */
    protected fun getDeviceTime(deviceId: String): kotlinx.datetime.Instant {
        return timingManager.getDeviceTime(deviceId)
    }
    
    /**
     * Synchronize device clocks for multi-device tests
     */
    protected fun synchronizeDeviceClocks(masterDeviceId: String) {
        timingManager.synchronizeDevices(masterDeviceId)
    }
    
    /**
     * Get timing diagnostics
     */
    protected fun getTimingDiagnostics(): String {
        return timingManager.getDiagnostics()
    }
    
    // Enhanced Async Condition Waiting Methods
    
    /**
     * Wait for a condition with enhanced error handling and diagnostics
     */
    protected suspend fun waitForConditionEnhanced(
        condition: suspend () -> Boolean,
        timeout: kotlin.time.Duration = 5.seconds,
        pollingInterval: kotlin.time.Duration = 100.milliseconds,
        errorMessage: String = "Condition not met within timeout",
        diagnostics: suspend () -> String = { getMockServiceDiagnostics() }
    ) {
        val result = asyncWaiter.waitForCondition(
            condition = condition,
            timeout = timeout,
            pollingInterval = pollingInterval,
            errorMessage = errorMessage,
            diagnostics = diagnostics
        )
        asyncWaiter.assertWaitSuccess(result, "Enhanced condition wait failed")
    }
    
    /**
     * Wait for eventual consistency - condition must remain true for a period
     */
    protected suspend fun waitForEventualConsistency(
        condition: suspend () -> Boolean,
        stabilityDuration: kotlin.time.Duration = 500.milliseconds,
        timeout: kotlin.time.Duration = 5.seconds,
        pollingInterval: kotlin.time.Duration = 100.milliseconds,
        errorMessage: String = "Eventual consistency not achieved within timeout"
    ) {
        val result = asyncWaiter.waitForEventualConsistency(
            condition = condition,
            stabilityDuration = stabilityDuration,
            timeout = timeout,
            pollingInterval = pollingInterval,
            errorMessage = errorMessage
        )
        asyncWaiter.assertWaitSuccess(result, "Eventual consistency check failed")
    }
    
    /**
     * Wait for multiple conditions to all be true
     */
    protected suspend fun waitForAllConditions(
        conditions: List<Pair<String, suspend () -> Boolean>>,
        timeout: kotlin.time.Duration = 5.seconds,
        pollingInterval: kotlin.time.Duration = 100.milliseconds,
        errorMessage: String = "Not all conditions met within timeout"
    ) {
        val result = asyncWaiter.waitForAllConditions(
            conditions = conditions,
            timeout = timeout,
            pollingInterval = pollingInterval,
            errorMessage = errorMessage
        )
        asyncWaiter.assertWaitSuccess(result, "Multiple conditions wait failed")
    }
    
    /**
     * Wait for any of the conditions to be true
     */
    protected suspend fun waitForAnyCondition(
        conditions: List<Pair<String, suspend () -> Boolean>>,
        timeout: kotlin.time.Duration = 5.seconds,
        pollingInterval: kotlin.time.Duration = 100.milliseconds,
        errorMessage: String = "None of the conditions met within timeout"
    ) {
        val result = asyncWaiter.waitForAnyCondition(
            conditions = conditions,
            timeout = timeout,
            pollingInterval = pollingInterval,
            errorMessage = errorMessage
        )
        asyncWaiter.assertWaitSuccess(result, "Any condition wait failed")
    }
    
    /**
     * Wait for a state change to occur
     */
    protected suspend fun <T> waitForStateChange(
        stateProvider: suspend () -> T,
        initialState: T? = null,
        timeout: kotlin.time.Duration = 5.seconds,
        pollingInterval: kotlin.time.Duration = 100.milliseconds,
        errorMessage: String = "State did not change within timeout"
    ) {
        val result = asyncWaiter.waitForStateChange(
            stateProvider = stateProvider,
            initialState = initialState,
            timeout = timeout,
            pollingInterval = pollingInterval,
            errorMessage = errorMessage
        )
        asyncWaiter.assertWaitSuccess(result, "State change wait failed")
    }
    
    /**
     * Wait for a value to reach a specific target
     */
    protected suspend fun <T> waitForValue(
        valueProvider: suspend () -> T,
        expectedValue: T,
        timeout: kotlin.time.Duration = 5.seconds,
        pollingInterval: kotlin.time.Duration = 100.milliseconds,
        errorMessage: String = "Value did not reach expected target within timeout"
    ) {
        val result = asyncWaiter.waitForValue(
            valueProvider = valueProvider,
            expectedValue = expectedValue,
            timeout = timeout,
            pollingInterval = pollingInterval,
            errorMessage = errorMessage
        )
        asyncWaiter.assertWaitSuccess(result, "Value wait failed")
    }
    
    /**
     * Wait for a numeric value to be within a range
     */
    protected suspend fun waitForValueInRange(
        valueProvider: suspend () -> Number,
        minValue: Number,
        maxValue: Number,
        timeout: kotlin.time.Duration = 5.seconds,
        pollingInterval: kotlin.time.Duration = 100.milliseconds,
        errorMessage: String = "Value did not reach expected range within timeout"
    ) {
        val result = asyncWaiter.waitForValueInRange(
            valueProvider = valueProvider,
            minValue = minValue,
            maxValue = maxValue,
            timeout = timeout,
            pollingInterval = pollingInterval,
            errorMessage = errorMessage
        )
        asyncWaiter.assertWaitSuccess(result, "Value range wait failed")
    }
    
    /**
     * Execute an operation and wait for its effects to be visible
     */
    protected suspend fun executeAndWaitForEffect(
        operationName: String,
        operation: suspend () -> Unit,
        effectCondition: suspend () -> Boolean,
        timeout: kotlin.time.Duration = 5.seconds,
        pollingInterval: kotlin.time.Duration = 100.milliseconds,
        errorMessage: String = "Operation effect not visible within timeout"
    ) {
        val result = asyncWaiter.executeAndWaitForEffect(
            operation = operation,
            effectCondition = effectCondition,
            timeout = timeout,
            pollingInterval = pollingInterval,
            errorMessage = "$errorMessage (operation: $operationName)"
        )
        asyncWaiter.assertWaitSuccess(result, "Execute and wait for effect failed for operation: $operationName")
    }
    
    /**
     * Enhanced async operation waiting with better error handling
     */
    protected suspend fun waitForAsyncOperationEnhanced(
        operationName: String,
        operation: suspend () -> Unit,
        verification: suspend () -> Boolean,
        timeout: kotlin.time.Duration = 5.seconds,
        pollingInterval: kotlin.time.Duration = 100.milliseconds
    ) {
        executeAndWaitForEffect(
            operationName = operationName,
            operation = operation,
            effectCondition = verification,
            timeout = timeout,
            pollingInterval = pollingInterval,
            errorMessage = "Async operation '$operationName' did not complete successfully"
        )
    }
    
    /**
     * Wait for mock service state to reach expected condition
     */
    protected suspend fun waitForMockServiceState(
        serviceName: String,
        condition: suspend () -> Boolean,
        timeout: kotlin.time.Duration = 5.seconds,
        pollingInterval: kotlin.time.Duration = 100.milliseconds
    ) {
        waitForConditionEnhanced(
            condition = condition,
            timeout = timeout,
            pollingInterval = pollingInterval,
            errorMessage = "Mock service '$serviceName' did not reach expected state",
            diagnostics = {
                buildString {
                    appendLine("Mock service state diagnostics for '$serviceName':")
                    appendLine(getMockServiceDiagnostics())
                    appendLine("Async waiter diagnostics:")
                    appendLine(asyncWaiter.getDiagnostics())
                }
            }
        )
    }
    
    /**
     * Get comprehensive async diagnostics
     */
    protected fun getAsyncDiagnostics(): String {
        return buildString {
            appendLine("=== Async Condition Waiter Diagnostics ===")
            appendLine(asyncWaiter.getDiagnostics())
            appendLine()
            appendLine("=== Timing Manager Diagnostics ===")
            appendLine(getTimingDiagnostics())
            appendLine()
            appendLine("=== Mock Service Diagnostics ===")
            appendLine(getMockServiceDiagnostics())
        }
    }
    
    // Performance Optimization Methods
    
    /**
     * Execute test with performance optimization
     */
    protected suspend fun <T> runOptimizedTest(
        testName: String,
        testBody: suspend TestPerformanceOptimizer.TestPerformanceContext.() -> T
    ): T {
        return performanceOptimizer.executeOptimizedTest(testName, testBody)
    }
    
    /**
     * Create optimized test user using resource manager
     */
    protected fun createOptimizedUser(
        userId: String = "test-user-${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}",
        template: EfficientTestDataFactory.DataTemplate = EfficientTestDataFactory.DataTemplate.STANDARD
    ): User {
        return resourceManager.getOrCreateUser(userId) {
            EfficientTestDataFactory.createOptimizedUser(
                userId,
                EfficientTestDataFactory.DataGenerationConfig(template = template)
            )
        }
    }
    
    /**
     * Create optimized daily log using resource manager
     */
    protected fun createOptimizedDailyLog(
        userId: String,
        date: kotlinx.datetime.LocalDate = kotlinx.datetime.LocalDate(2024, 1, 15),
        template: EfficientTestDataFactory.DataTemplate = EfficientTestDataFactory.DataTemplate.STANDARD
    ): DailyLog {
        val logKey = "$userId-$date"
        return resourceManager.getOrCreateDailyLog(logKey) {
            EfficientTestDataFactory.createOptimizedDailyLog(
                userId,
                date,
                EfficientTestDataFactory.DataGenerationConfig(template = template)
            )
        }
    }
    
    /**
     * Create optimized cycle using resource manager
     */
    protected fun createOptimizedCycle(
        userId: String,
        template: EfficientTestDataFactory.DataTemplate = EfficientTestDataFactory.DataTemplate.STANDARD
    ): Cycle {
        val cycleKey = "$userId-${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}"
        return resourceManager.getOrCreateCycle(cycleKey) {
            EfficientTestDataFactory.createOptimizedCycle(
                userId,
                EfficientTestDataFactory.DataGenerationConfig(template = template)
            )
        }
    }
    
    /**
     * Create batch of test data efficiently
     */
    protected fun createTestDataBatch(
        userId: String,
        logCount: Int = 7,
        cycleCount: Int = 3,
        template: EfficientTestDataFactory.DataTemplate = EfficientTestDataFactory.DataTemplate.STANDARD
    ): TestDataBatch {
        val config = EfficientTestDataFactory.DataGenerationConfig(template = template)
        
        return TestDataBatch(
            user = createOptimizedUser(userId, template),
            dailyLogs = EfficientTestDataFactory.createDailyLogBatch(userId, logCount, config = config),
            cycles = EfficientTestDataFactory.createCycleBatch(userId, cycleCount, config = config)
        )
    }
    
    data class TestDataBatch(
        val user: User,
        val dailyLogs: List<DailyLog>,
        val cycles: List<Cycle>
    )
    
    /**
     * Execute test with resource scoping for automatic cleanup
     */
    protected suspend fun <T> withResourceScope(
        testBody: suspend (ScopedResourceManager) -> T
    ): T {
        val scope = resourceManager.createScope()
        return try {
            testBody(scope)
        } finally {
            scope.cleanup()
        }
    }
    
    /**
     * Get performance statistics for the current test
     */
    protected fun getPerformanceStats(): TestPerformanceOptimizer.PerformanceStats {
        return performanceOptimizer.getPerformanceStats()
    }
    
    /**
     * Validate test performance and resource usage
     */
    protected fun validateTestPerformance(): String {
        val optimizerValidation = performanceOptimizer.validateTestSuitePerformance()
        val resourceValidation = resourceManager.validateResourceUsage()
        
        return buildString {
            appendLine("=== Test Performance Validation ===")
            
            if (!optimizerValidation.isValid) {
                appendLine("Performance Issues:")
                optimizerValidation.issues.forEach { issue ->
                    appendLine("  - $issue")
                }
                appendLine("Recommendations:")
                optimizerValidation.recommendations.forEach { rec ->
                    appendLine("  - $rec")
                }
            } else {
                appendLine("Performance: GOOD")
            }
            
            if (!resourceValidation.isValid) {
                appendLine("\nResource Issues:")
                resourceValidation.issues.forEach { issue ->
                    appendLine("  - $issue")
                }
                appendLine("Recommendations:")
                resourceValidation.recommendations.forEach { rec ->
                    appendLine("  - $rec")
                }
            } else {
                appendLine("Resource Usage: GOOD")
            }
            
            appendLine("\nPerformance Stats:")
            val stats = optimizerValidation.performanceStats
            appendLine("  Memory: ${stats.totalAllocatedMemoryMB}MB")
            appendLine("  Active Resources: ${stats.activeResourceCount}")
            appendLine("  Average Test Time: ${stats.averageTestExecutionTime.inWholeMilliseconds}ms")
            
            appendLine("\nResource Stats:")
            val resourceStats = resourceValidation.resourceStats
            appendLine("  Cache Size: ${resourceStats.totalCacheSize}")
            appendLine("  Pool Size: ${resourceStats.totalPoolSize}")
            appendLine("  Memory Usage: ${resourceStats.totalMemoryUsageBytes / 1024}KB")
        }
    }
    
    /**
     * Force cleanup of resources if test is taking too long
     */
    protected suspend fun forceResourceCleanup() {
        performanceOptimizer.performPeriodicCleanup()
        resourceManager.performCleanup()
    }
    
    /**
     * Check if test execution is within performance limits
     */
    protected fun checkPerformanceLimits(): Boolean {
        val stats = getPerformanceStats()
        val validation = performanceOptimizer.validateTestSuitePerformance()
        
        return validation.isValid && 
               stats.totalAllocatedMemoryMB < 100 && // 100MB limit
               stats.averageTestExecutionTime < kotlin.time.Duration.parse("PT10S") // 10 second limit
    }
    
    /**
     * Execute test with automatic performance monitoring
     */
    protected suspend fun runMonitoredTest(
        testName: String,
        testBody: suspend () -> Unit
    ) {
        val startStats = getPerformanceStats()
        
        try {
            performanceOptimizer.optimizeTestExecution(testName, testBody)
        } finally {
            val endStats = getPerformanceStats()
            
            // Check for performance degradation
            if (endStats.totalAllocatedMemoryMB > startStats.totalAllocatedMemoryMB * 2) {
                println("Warning: Memory usage doubled during test '$testName'")
                println("Before: ${startStats.totalAllocatedMemoryMB}MB, After: ${endStats.totalAllocatedMemoryMB}MB")
            }
            
            // Check for resource leaks
            if (endStats.memoryLeaks.isNotEmpty()) {
                println("Warning: Potential memory leaks detected in test '$testName':")
                endStats.memoryLeaks.forEach { leak ->
                    println("  - $leak")
                }
            }
        }
    }
}