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
 * Enhanced async condition waiting utility that provides robust condition checking
 * with proper timeout handling, polling mechanisms, and eventual consistency support.
 * 
 * Addresses Requirements 7.2 and 7.6:
 * - WHEN tests use mock data THEN mock implementations SHALL behave consistently
 * - WHEN tests involve async operations THEN proper synchronization SHALL be implemented
 */
class AsyncConditionWaiter(
    private val testDispatcher: TestDispatcher,
    private val testScope: TestScope
) {
    
    /**
     * Configuration for condition waiting behavior
     */
    data class WaitConfiguration(
        val defaultTimeout: Duration = 5.seconds,
        val defaultPollingInterval: Duration = 100.milliseconds,
        val maxPollingInterval: Duration = 1.seconds,
        val minPollingInterval: Duration = 10.milliseconds,
        val exponentialBackoffMultiplier: Double = 1.5,
        val enableAdaptivePolling: Boolean = true,
        val enableDetailedLogging: Boolean = false
    )
    
    /**
     * Result of a condition wait operation
     */
    sealed class WaitResult {
        object Success : WaitResult()
        data class Timeout(
            val duration: Duration,
            val attempts: Int,
            val lastError: Throwable? = null,
            val diagnostics: String = ""
        ) : WaitResult()
        data class Error(
            val error: Throwable,
            val attempts: Int,
            val duration: Duration
        ) : WaitResult()
    }
    
    /**
     * Context information for condition evaluation
     */
    data class ConditionContext(
        val attempt: Int,
        val elapsedTime: Duration,
        val remainingTime: Duration,
        val currentPollingInterval: Duration
    )
    
    private val configuration = WaitConfiguration()
    private val diagnosticsCollector = DiagnosticsCollector()
    
    /**
     * Wait for a condition to become true with comprehensive timeout and error handling
     */
    suspend fun waitForCondition(
        condition: suspend (ConditionContext) -> Boolean,
        timeout: Duration = configuration.defaultTimeout,
        pollingInterval: Duration = configuration.defaultPollingInterval,
        errorMessage: String = "Condition not met within timeout",
        diagnostics: suspend () -> String = { "" }
    ): WaitResult {
        val startTime = Clock.System.now()
        var attempt = 0
        var currentInterval = pollingInterval
        var lastError: Throwable? = null
        
        diagnosticsCollector.reset()
        diagnosticsCollector.log("Starting condition wait: timeout=$timeout, polling=$pollingInterval")
        
        while (true) {
            attempt++
            val elapsedTime = Clock.System.now() - startTime
            val remainingTime = timeout - elapsedTime
            
            if (elapsedTime >= timeout) {
                val diagnosticInfo = try {
                    diagnostics()
                } catch (e: Exception) {
                    "Failed to collect diagnostics: ${e.message}"
                }
                
                diagnosticsCollector.log("Timeout reached after $attempt attempts in $elapsedTime")
                
                return WaitResult.Timeout(
                    duration = elapsedTime,
                    attempts = attempt,
                    lastError = lastError,
                    diagnostics = buildTimeoutDiagnostics(errorMessage, elapsedTime, attempt, diagnosticInfo)
                )
            }
            
            try {
                val context = ConditionContext(
                    attempt = attempt,
                    elapsedTime = elapsedTime,
                    remainingTime = remainingTime,
                    currentPollingInterval = currentInterval
                )
                
                diagnosticsCollector.log("Attempt $attempt: checking condition (elapsed: $elapsedTime)")
                
                if (condition(context)) {
                    diagnosticsCollector.log("Condition met after $attempt attempts in $elapsedTime")
                    return WaitResult.Success
                }
                
            } catch (e: Exception) {
                lastError = e
                diagnosticsCollector.log("Attempt $attempt failed with error: ${e.message}")
                
                // If we're close to timeout, don't continue
                if (remainingTime < currentInterval * 2) {
                    return WaitResult.Error(
                        error = e,
                        attempts = attempt,
                        duration = elapsedTime
                    )
                }
            }
            
            // Adaptive polling: increase interval if we've been waiting a while
            if (configuration.enableAdaptivePolling && attempt > 10) {
                currentInterval = minOf(
                    currentInterval * configuration.exponentialBackoffMultiplier,
                    configuration.maxPollingInterval
                ).coerceAtLeast(configuration.minPollingInterval)
            }
            
            // Use test dispatcher for controlled timing
            testDispatcher.scheduler.advanceTimeBy(currentInterval.inWholeMilliseconds)
            delay(currentInterval)
        }
    }
    
    /**
     * Wait for a simple boolean condition
     */
    suspend fun waitForCondition(
        condition: suspend () -> Boolean,
        timeout: Duration = configuration.defaultTimeout,
        pollingInterval: Duration = configuration.defaultPollingInterval,
        errorMessage: String = "Condition not met within timeout",
        diagnostics: suspend () -> String = { "" }
    ): WaitResult {
        return waitForCondition(
            condition = { _ -> condition() },
            timeout = timeout,
            pollingInterval = pollingInterval,
            errorMessage = errorMessage,
            diagnostics = diagnostics
        )
    }
    
    /**
     * Wait for eventual consistency - keeps checking until condition is stable
     */
    suspend fun waitForEventualConsistency(
        condition: suspend () -> Boolean,
        stabilityDuration: Duration = 500.milliseconds,
        timeout: Duration = configuration.defaultTimeout,
        pollingInterval: Duration = configuration.defaultPollingInterval,
        errorMessage: String = "Eventual consistency not achieved within timeout"
    ): WaitResult {
        var lastSuccessTime: Instant? = null
        
        return waitForCondition(
            condition = { context ->
                val conditionMet = condition()
                val now = Clock.System.now()
                
                if (conditionMet) {
                    if (lastSuccessTime == null) {
                        lastSuccessTime = now
                        false // First success, need to wait for stability
                    } else {
                        val stableFor = now - lastSuccessTime!!
                        stableFor >= stabilityDuration // Stable for required duration
                    }
                } else {
                    lastSuccessTime = null // Reset stability timer
                    false
                }
            },
            timeout = timeout,
            pollingInterval = pollingInterval,
            errorMessage = errorMessage,
            diagnostics = {
                "Eventual consistency check: lastSuccessTime=$lastSuccessTime, stabilityRequired=$stabilityDuration"
            }
        )
    }
    
    /**
     * Wait for multiple conditions to all be true
     */
    suspend fun waitForAllConditions(
        conditions: List<Pair<String, suspend () -> Boolean>>,
        timeout: Duration = configuration.defaultTimeout,
        pollingInterval: Duration = configuration.defaultPollingInterval,
        errorMessage: String = "Not all conditions met within timeout"
    ): WaitResult {
        return waitForCondition(
            condition = { context ->
                val results = conditions.map { (name, condition) ->
                    try {
                        name to condition()
                    } catch (e: Exception) {
                        name to false
                    }
                }
                
                val allMet = results.all { it.second }
                
                if (configuration.enableDetailedLogging) {
                    diagnosticsCollector.log("Attempt ${context.attempt}: ${results.map { "${it.first}=${it.second}" }}")
                }
                
                allMet
            },
            timeout = timeout,
            pollingInterval = pollingInterval,
            errorMessage = errorMessage,
            diagnostics = {
                val results = conditions.map { (name, condition) ->
                    try {
                        "$name: ${condition()}"
                    } catch (e: Exception) {
                        "$name: ERROR(${e.message})"
                    }
                }
                "Condition states: ${results.joinToString(", ")}"
            }
        )
    }
    
    /**
     * Wait for any of the conditions to be true
     */
    suspend fun waitForAnyCondition(
        conditions: List<Pair<String, suspend () -> Boolean>>,
        timeout: Duration = configuration.defaultTimeout,
        pollingInterval: Duration = configuration.defaultPollingInterval,
        errorMessage: String = "None of the conditions met within timeout"
    ): WaitResult {
        return waitForCondition(
            condition = { context ->
                val results = conditions.map { (name, condition) ->
                    try {
                        name to condition()
                    } catch (e: Exception) {
                        name to false
                    }
                }
                
                val anyMet = results.any { it.second }
                
                if (configuration.enableDetailedLogging) {
                    diagnosticsCollector.log("Attempt ${context.attempt}: ${results.map { "${it.first}=${it.second}" }}")
                }
                
                anyMet
            },
            timeout = timeout,
            pollingInterval = pollingInterval,
            errorMessage = errorMessage,
            diagnostics = {
                val results = conditions.map { (name, condition) ->
                    try {
                        "$name: ${condition()}"
                    } catch (e: Exception) {
                        "$name: ERROR(${e.message})"
                    }
                }
                "Condition states: ${results.joinToString(", ")}"
            }
        )
    }
    
    /**
     * Wait for a state change to occur
     */
    suspend fun <T> waitForStateChange(
        stateProvider: suspend () -> T,
        initialState: T? = null,
        timeout: Duration = configuration.defaultTimeout,
        pollingInterval: Duration = configuration.defaultPollingInterval,
        errorMessage: String = "State did not change within timeout"
    ): WaitResult {
        val startState = initialState ?: stateProvider()
        
        return waitForCondition(
            condition = { _ ->
                val currentState = stateProvider()
                currentState != startState
            },
            timeout = timeout,
            pollingInterval = pollingInterval,
            errorMessage = errorMessage,
            diagnostics = {
                val currentState = try {
                    stateProvider()
                } catch (e: Exception) {
                    "ERROR: ${e.message}"
                }
                "Initial state: $startState, Current state: $currentState"
            }
        )
    }
    
    /**
     * Wait for a value to reach a specific target
     */
    suspend fun <T> waitForValue(
        valueProvider: suspend () -> T,
        expectedValue: T,
        timeout: Duration = configuration.defaultTimeout,
        pollingInterval: Duration = configuration.defaultPollingInterval,
        errorMessage: String = "Value did not reach expected target within timeout"
    ): WaitResult {
        return waitForCondition(
            condition = { _ ->
                val currentValue = valueProvider()
                currentValue == expectedValue
            },
            timeout = timeout,
            pollingInterval = pollingInterval,
            errorMessage = errorMessage,
            diagnostics = {
                val currentValue = try {
                    valueProvider()
                } catch (e: Exception) {
                    "ERROR: ${e.message}"
                }
                "Expected: $expectedValue, Current: $currentValue"
            }
        )
    }
    
    /**
     * Wait for a numeric value to be within a range
     */
    suspend fun waitForValueInRange(
        valueProvider: suspend () -> Number,
        minValue: Number,
        maxValue: Number,
        timeout: Duration = configuration.defaultTimeout,
        pollingInterval: Duration = configuration.defaultPollingInterval,
        errorMessage: String = "Value did not reach expected range within timeout"
    ): WaitResult {
        return waitForCondition(
            condition = { _ ->
                val currentValue = valueProvider().toDouble()
                val min = minValue.toDouble()
                val max = maxValue.toDouble()
                currentValue >= min && currentValue <= max
            },
            timeout = timeout,
            pollingInterval = pollingInterval,
            errorMessage = errorMessage,
            diagnostics = {
                val currentValue = try {
                    valueProvider()
                } catch (e: Exception) {
                    "ERROR: ${e.message}"
                }
                "Expected range: [$minValue, $maxValue], Current: $currentValue"
            }
        )
    }
    
    /**
     * Execute an operation and wait for its effects to be visible
     */
    suspend fun executeAndWaitForEffect(
        operation: suspend () -> Unit,
        effectCondition: suspend () -> Boolean,
        timeout: Duration = configuration.defaultTimeout,
        pollingInterval: Duration = configuration.defaultPollingInterval,
        errorMessage: String = "Operation effect not visible within timeout"
    ): WaitResult {
        // Execute the operation first
        try {
            operation()
        } catch (e: Exception) {
            return WaitResult.Error(
                error = e,
                attempts = 0,
                duration = Duration.ZERO
            )
        }
        
        // Then wait for the effect
        return waitForCondition(
            condition = effectCondition,
            timeout = timeout,
            pollingInterval = pollingInterval,
            errorMessage = errorMessage
        )
    }
    
    /**
     * Throw AssertionError if wait result is not successful
     */
    fun assertWaitSuccess(
        result: WaitResult,
        additionalContext: String = ""
    ) {
        when (result) {
            is WaitResult.Success -> return
            is WaitResult.Timeout -> {
                val message = buildString {
                    appendLine("Condition wait timed out:")
                    appendLine("  Duration: ${result.duration}")
                    appendLine("  Attempts: ${result.attempts}")
                    if (result.lastError != null) {
                        appendLine("  Last error: ${result.lastError.message}")
                    }
                    if (result.diagnostics.isNotEmpty()) {
                        appendLine("  Diagnostics: ${result.diagnostics}")
                    }
                    if (additionalContext.isNotEmpty()) {
                        appendLine("  Context: $additionalContext")
                    }
                    appendLine("  Full diagnostics:")
                    appendLine(diagnosticsCollector.getFullLog())
                }
                throw AssertionError(message)
            }
            is WaitResult.Error -> {
                val message = buildString {
                    appendLine("Condition wait failed with error:")
                    appendLine("  Error: ${result.error.message}")
                    appendLine("  Duration: ${result.duration}")
                    appendLine("  Attempts: ${result.attempts}")
                    if (additionalContext.isNotEmpty()) {
                        appendLine("  Context: $additionalContext")
                    }
                    appendLine("  Full diagnostics:")
                    appendLine(diagnosticsCollector.getFullLog())
                }
                throw AssertionError(message, result.error)
            }
        }
    }
    
    /**
     * Get diagnostic information about the waiter state
     */
    fun getDiagnostics(): String {
        return buildString {
            appendLine("AsyncConditionWaiter Diagnostics:")
            appendLine("  Configuration: $configuration")
            appendLine("  Recent activity:")
            appendLine(diagnosticsCollector.getRecentLog(10))
        }
    }
    
    private fun buildTimeoutDiagnostics(
        errorMessage: String,
        elapsedTime: Duration,
        attempts: Int,
        additionalDiagnostics: String
    ): String {
        return buildString {
            appendLine(errorMessage)
            appendLine("Timeout details:")
            appendLine("  Elapsed time: ${elapsedTime.inWholeMilliseconds}ms")
            appendLine("  Total attempts: $attempts")
            if (additionalDiagnostics.isNotEmpty()) {
                appendLine("  Additional diagnostics:")
                appendLine("    $additionalDiagnostics")
            }
        }
    }
    
    /**
     * Internal diagnostics collector
     */
    private class DiagnosticsCollector {
        private val logs = mutableListOf<Pair<Instant, String>>()
        private val maxLogs = 100
        
        fun log(message: String) {
            logs.add(Clock.System.now() to message)
            if (logs.size > maxLogs) {
                logs.removeAt(0)
            }
        }
        
        fun reset() {
            logs.clear()
        }
        
        fun getFullLog(): String {
            return logs.joinToString("\n") { (timestamp, message) ->
                "${timestamp}: $message"
            }
        }
        
        fun getRecentLog(count: Int): String {
            return logs.takeLast(count).joinToString("\n") { (timestamp, message) ->
                "${timestamp}: $message"
            }
        }
    }
}

/**
 * Extension functions for easier usage with BaseIntegrationTest
 */
suspend fun AsyncConditionWaiter.waitForConditionOrThrow(
    condition: suspend () -> Boolean,
    timeout: Duration = 5.seconds,
    pollingInterval: Duration = 100.milliseconds,
    errorMessage: String = "Condition not met within timeout",
    diagnostics: suspend () -> String = { "" }
) {
    val result = waitForCondition(condition, timeout, pollingInterval, errorMessage, diagnostics)
    assertWaitSuccess(result)
}

suspend fun AsyncConditionWaiter.waitForEventualConsistencyOrThrow(
    condition: suspend () -> Boolean,
    stabilityDuration: Duration = 500.milliseconds,
    timeout: Duration = 5.seconds,
    pollingInterval: Duration = 100.milliseconds,
    errorMessage: String = "Eventual consistency not achieved within timeout"
) {
    val result = waitForEventualConsistency(condition, stabilityDuration, timeout, pollingInterval, errorMessage)
    assertWaitSuccess(result)
}

suspend fun <T> AsyncConditionWaiter.waitForValueOrThrow(
    valueProvider: suspend () -> T,
    expectedValue: T,
    timeout: Duration = 5.seconds,
    pollingInterval: Duration = 100.milliseconds,
    errorMessage: String = "Value did not reach expected target within timeout"
) {
    val result = waitForValue(valueProvider, expectedValue, timeout, pollingInterval, errorMessage)
    assertWaitSuccess(result)
}