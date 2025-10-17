package com.eunio.healthapp.testutil

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

/**
 * Base class for Android integration tests that provides proper test state management,
 * async operation handling, and cleanup mechanisms.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
abstract class BaseAndroidIntegrationTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    protected lateinit var testDispatcher: TestDispatcher
    protected lateinit var testScope: TestScope
    protected lateinit var mockServices: MockServices
    protected lateinit var timingManager: TestTimingManager
    
    // Track resources for cleanup
    private val registeredMockServices = mutableListOf<Any>()
    private val coroutineJobs = mutableListOf<kotlinx.coroutines.Job>()
    private val testStateManager = TestStateManager()
    
    @BeforeTest
    open fun baseSetup() {
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        
        // Initialize timing manager with test dispatcher
        timingManager = TestTimingManager(testDispatcher, testScope)
        
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
        
        // Ensure all async operations complete
        testDispatcher.scheduler.advanceUntilIdle()
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
}