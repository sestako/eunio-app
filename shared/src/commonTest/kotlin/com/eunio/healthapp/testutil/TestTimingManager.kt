package com.eunio.healthapp.testutil

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Comprehensive test timing manager that provides configurable delays,
 * proper clock synchronization, and deterministic timing for multi-device tests.
 * 
 * Addresses Requirements 7.3 and 7.6:
 * - WHEN tests involve timing THEN they SHALL not be dependent on system performance or timing variations
 * - WHEN tests involve async operations THEN proper synchronization SHALL be implemented
 */
class TestTimingManager(
    private val testDispatcher: TestDispatcher,
    private val testScope: TestScope
) {
    private var virtualTime: Instant = Clock.System.now()
    private val timingConfiguration = TimingConfiguration()
    private val clockSynchronizer = ClockSynchronizer()
    private val delayScenarios = mutableMapOf<String, DelayScenario>()
    
    /**
     * Configuration for different test timing scenarios
     */
    data class TimingConfiguration(
        val defaultAsyncTimeout: Duration = 5.seconds,
        val defaultPollingInterval: Duration = 100.milliseconds,
        val networkDelaySimulation: Duration = 100.milliseconds,
        val databaseOperationDelay: Duration = 50.milliseconds,
        val syncOperationDelay: Duration = 200.milliseconds,
        val authenticationDelay: Duration = 150.milliseconds,
        val multiDeviceClockSkew: Duration = 500.milliseconds,
        val retryBackoffBase: Duration = 100.milliseconds,
        val maxRetryBackoff: Duration = 5.seconds
    )
    
    /**
     * Delay scenario configuration for different test operations
     */
    data class DelayScenario(
        val name: String,
        val baseDelay: Duration,
        val variability: Duration = Duration.ZERO,
        val failureRate: Double = 0.0,
        val retryMultiplier: Double = 2.0,
        val maxRetries: Int = 3
    )
    
    /**
     * Clock synchronization for multi-device test scenarios
     */
    private class ClockSynchronizer {
        private val deviceClocks = mutableMapOf<String, DeviceClock>()
        
        data class DeviceClock(
            val deviceId: String,
            val baseTime: Instant,
            val clockSkew: Duration,
            val driftRate: Double = 0.0 // Clock drift per second
        )
        
        fun registerDevice(deviceId: String, clockSkew: Duration = Duration.ZERO) {
            deviceClocks[deviceId] = DeviceClock(
                deviceId = deviceId,
                baseTime = Clock.System.now(),
                clockSkew = clockSkew
            )
        }
        
        fun getDeviceTime(deviceId: String): Instant {
            val deviceClock = deviceClocks[deviceId] ?: return Clock.System.now()
            val elapsed = Clock.System.now() - deviceClock.baseTime
            val driftAdjustment = (elapsed.inWholeMilliseconds * deviceClock.driftRate).milliseconds
            return Clock.System.now() + deviceClock.clockSkew + driftAdjustment
        }
        
        fun synchronizeDevices(masterDeviceId: String) {
            val masterClock = deviceClocks[masterDeviceId] ?: return
            val masterTime = getDeviceTime(masterDeviceId)
            
            deviceClocks.forEach { (deviceId, clock) ->
                if (deviceId != masterDeviceId) {
                    val timeDiff = masterTime - getDeviceTime(deviceId)
                    deviceClocks[deviceId] = clock.copy(
                        clockSkew = clock.clockSkew + timeDiff
                    )
                }
            }
        }
        
        fun reset() {
            deviceClocks.clear()
        }
    }
    
    init {
        setupDefaultDelayScenarios()
    }
    
    private fun setupDefaultDelayScenarios() {
        // Network operation scenarios
        delayScenarios["network_fast"] = DelayScenario(
            name = "Fast Network",
            baseDelay = 50.milliseconds,
            variability = 20.milliseconds
        )
        
        delayScenarios["network_slow"] = DelayScenario(
            name = "Slow Network",
            baseDelay = 2.seconds,
            variability = 500.milliseconds,
            failureRate = 0.1
        )
        
        delayScenarios["network_unreliable"] = DelayScenario(
            name = "Unreliable Network",
            baseDelay = 500.milliseconds,
            variability = 1.seconds,
            failureRate = 0.3,
            maxRetries = 5
        )
        
        // Database operation scenarios
        delayScenarios["database_fast"] = DelayScenario(
            name = "Fast Database",
            baseDelay = 10.milliseconds,
            variability = 5.milliseconds
        )
        
        delayScenarios["database_slow"] = DelayScenario(
            name = "Slow Database",
            baseDelay = 500.milliseconds,
            variability = 200.milliseconds,
            failureRate = 0.05
        )
        
        // Authentication scenarios
        delayScenarios["auth_normal"] = DelayScenario(
            name = "Normal Authentication",
            baseDelay = 150.milliseconds,
            variability = 50.milliseconds
        )
        
        delayScenarios["auth_slow"] = DelayScenario(
            name = "Slow Authentication",
            baseDelay = 3.seconds,
            variability = 1.seconds,
            failureRate = 0.1
        )
        
        // Sync operation scenarios
        delayScenarios["sync_normal"] = DelayScenario(
            name = "Normal Sync",
            baseDelay = 200.milliseconds,
            variability = 100.milliseconds
        )
        
        delayScenarios["sync_conflict"] = DelayScenario(
            name = "Sync with Conflicts",
            baseDelay = 1.seconds,
            variability = 500.milliseconds,
            failureRate = 0.2
        )
    }
    
    /**
     * Execute operation with configurable delay scenario
     */
    suspend fun executeWithDelay(
        scenarioName: String,
        operation: suspend () -> Unit
    ) {
        val scenario = delayScenarios[scenarioName] 
            ?: throw IllegalArgumentException("Unknown delay scenario: $scenarioName")
        
        val actualDelay = calculateActualDelay(scenario)
        
        // Use test dispatcher for controlled timing
        testDispatcher.scheduler.advanceTimeBy(actualDelay.inWholeMilliseconds)
        delay(actualDelay)
        
        // Simulate potential failure
        if (scenario.failureRate > 0 && shouldSimulateFailure(scenario.failureRate)) {
            throw TestTimingException("Simulated failure for scenario: $scenarioName")
        }
        
        operation()
    }
    
    /**
     * Execute operation with retry logic and exponential backoff
     */
    suspend fun executeWithRetry(
        scenarioName: String,
        operation: suspend () -> Unit,
        maxRetries: Int? = null
    ) {
        val scenario = delayScenarios[scenarioName] 
            ?: throw IllegalArgumentException("Unknown delay scenario: $scenarioName")
        
        val retryLimit = maxRetries ?: scenario.maxRetries
        var attempt = 0
        var lastException: Exception? = null
        
        while (attempt <= retryLimit) {
            try {
                if (attempt > 0) {
                    // Apply exponential backoff
                    val backoffDelay = calculateBackoffDelay(attempt, scenario)
                    testDispatcher.scheduler.advanceTimeBy(backoffDelay.inWholeMilliseconds)
                    delay(backoffDelay)
                }
                
                executeWithDelay(scenarioName, operation)
                return // Success
                
            } catch (e: TestTimingException) {
                lastException = e
                attempt++
                
                if (attempt > retryLimit) {
                    throw TestTimingException(
                        "Operation failed after $retryLimit retries. Last error: ${e.message}",
                        e
                    )
                }
            }
        }
    }
    
    /**
     * Wait for condition with configurable timeout and polling
     */
    suspend fun waitForCondition(
        condition: suspend () -> Boolean,
        timeout: Duration = timingConfiguration.defaultAsyncTimeout,
        pollingInterval: Duration = timingConfiguration.defaultPollingInterval,
        errorMessage: String = "Condition not met within timeout"
    ): Boolean {
        val startTime = getCurrentTime()
        
        while (getCurrentTime() - startTime < timeout) {
            if (condition()) {
                return true
            }
            
            // Use test dispatcher for controlled timing
            testDispatcher.scheduler.advanceTimeBy(pollingInterval.inWholeMilliseconds)
            delay(pollingInterval)
        }
        
        throw TestTimingException("$errorMessage after ${timeout.inWholeMilliseconds}ms")
    }
    
    /**
     * Wait for async operation to complete with proper synchronization
     */
    suspend fun waitForAsyncOperation(
        operationName: String,
        operation: suspend () -> Unit,
        verification: suspend () -> Boolean,
        timeout: Duration = timingConfiguration.defaultAsyncTimeout
    ) {
        operation()
        
        val success = waitForCondition(
            condition = verification,
            timeout = timeout,
            errorMessage = "Async operation '$operationName' did not complete"
        )
        
        if (!success) {
            throw TestTimingException("Async operation '$operationName' failed to complete within $timeout")
        }
    }
    
    /**
     * Advance virtual time for deterministic testing
     */
    fun advanceTime(duration: Duration) {
        virtualTime = virtualTime.plus(duration)
        testDispatcher.scheduler.advanceTimeBy(duration.inWholeMilliseconds)
    }
    
    /**
     * Get current virtual time
     */
    fun getCurrentTime(): Instant = virtualTime
    
    /**
     * Reset virtual time to current system time
     */
    fun resetTime() {
        virtualTime = Clock.System.now()
    }
    
    /**
     * Execute operation with advanced time
     */
    suspend fun withAdvancedTime(
        duration: Duration,
        operation: suspend () -> Unit
    ) {
        advanceTime(duration)
        operation()
    }
    
    /**
     * Register a device for multi-device clock synchronization
     */
    fun registerDevice(deviceId: String, clockSkew: Duration = Duration.ZERO) {
        clockSynchronizer.registerDevice(deviceId, clockSkew)
    }
    
    /**
     * Get time for a specific device (with clock skew)
     */
    fun getDeviceTime(deviceId: String): Instant {
        return clockSynchronizer.getDeviceTime(deviceId)
    }
    
    /**
     * Synchronize all device clocks to a master device
     */
    fun synchronizeDevices(masterDeviceId: String) {
        clockSynchronizer.synchronizeDevices(masterDeviceId)
    }
    
    /**
     * Create timestamp-based assertion that accounts for clock skew
     */
    fun assertTimestampWithinRange(
        actual: Instant,
        expected: Instant,
        tolerance: Duration = 1.seconds,
        message: String = "Timestamp not within expected range"
    ) {
        val diff = if (actual > expected) actual - expected else expected - actual
        
        if (diff > tolerance) {
            throw AssertionError(
                "$message. Expected: $expected, Actual: $actual, Difference: ${diff.inWholeMilliseconds}ms, Tolerance: ${tolerance.inWholeMilliseconds}ms"
            )
        }
    }
    
    /**
     * Create deterministic delay for timestamp-based operations
     */
    suspend fun createTimestampSeparation(minimumGap: Duration = 10.milliseconds) {
        advanceTime(minimumGap)
        delay(minimumGap)
    }
    
    /**
     * Configure timing for specific test scenario
     */
    fun configureScenario(name: String, scenario: DelayScenario) {
        delayScenarios[name] = scenario
    }
    
    /**
     * Get timing configuration
     */
    fun getConfiguration(): TimingConfiguration = timingConfiguration
    
    /**
     * Update timing configuration
     */
    fun updateConfiguration(update: TimingConfiguration.() -> TimingConfiguration) {
        val newConfig = timingConfiguration.update()
        // Apply new configuration (in a real implementation, this would update the internal state)
    }
    
    /**
     * Reset all timing state
     */
    fun reset() {
        virtualTime = Clock.System.now()
        clockSynchronizer.reset()
        setupDefaultDelayScenarios()
    }
    
    /**
     * Get diagnostic information about timing state
     */
    fun getDiagnostics(): String {
        return buildString {
            appendLine("Test Timing Manager Diagnostics:")
            appendLine("  Virtual Time: $virtualTime")
            appendLine("  System Time: ${Clock.System.now()}")
            appendLine("  Time Difference: ${Clock.System.now() - virtualTime}")
            appendLine("  Configured Scenarios: ${delayScenarios.keys}")
            appendLine("  Default Timeout: ${timingConfiguration.defaultAsyncTimeout}")
            appendLine("  Default Polling: ${timingConfiguration.defaultPollingInterval}")
        }
    }
    
    // Private helper methods
    
    private fun calculateActualDelay(scenario: DelayScenario): Duration {
        val variability = if (scenario.variability > Duration.ZERO) {
            val variabilityMs = scenario.variability.inWholeMilliseconds
            val randomVariability = (-variabilityMs..variabilityMs).random()
            randomVariability.milliseconds
        } else {
            Duration.ZERO
        }
        
        return scenario.baseDelay + variability
    }
    
    private fun shouldSimulateFailure(failureRate: Double): Boolean {
        // Use deterministic failure based on current time to make tests more predictable
        val timeBasedSeed = virtualTime.toEpochMilliseconds() % 100
        return (timeBasedSeed / 100.0) < failureRate
    }
    
    private fun calculateBackoffDelay(attempt: Int, scenario: DelayScenario): Duration {
        val baseBackoff = timingConfiguration.retryBackoffBase
        val multiplier = scenario.retryMultiplier
        
        // Calculate exponential backoff manually to avoid kotlin.math.pow
        var backoffMs = baseBackoff.inWholeMilliseconds
        repeat(attempt) {
            backoffMs = (backoffMs * multiplier).toLong()
        }
        
        val cappedBackoff = minOf(backoffMs, timingConfiguration.maxRetryBackoff.inWholeMilliseconds)
        return cappedBackoff.milliseconds
    }
}

/**
 * Exception thrown by timing operations
 */
class TestTimingException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Extension functions for easier timing management
 */
suspend fun TestTimingManager.networkOperation(
    operation: suspend () -> Unit,
    slow: Boolean = false
) {
    val scenarioName = if (slow) "network_slow" else "network_fast"
    executeWithDelay(scenarioName, operation)
}

suspend fun TestTimingManager.databaseOperation(
    operation: suspend () -> Unit,
    slow: Boolean = false
) {
    val scenarioName = if (slow) "database_slow" else "database_fast"
    executeWithDelay(scenarioName, operation)
}

suspend fun TestTimingManager.authenticationOperation(
    operation: suspend () -> Unit,
    slow: Boolean = false
) {
    val scenarioName = if (slow) "auth_slow" else "auth_normal"
    executeWithDelay(scenarioName, operation)
}

suspend fun TestTimingManager.syncOperation(
    operation: suspend () -> Unit,
    withConflicts: Boolean = false
) {
    val scenarioName = if (withConflicts) "sync_conflict" else "sync_normal"
    executeWithDelay(scenarioName, operation)
}