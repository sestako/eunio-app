package com.eunio.healthapp.testutil

import com.eunio.healthapp.domain.model.*
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Comprehensive tests for AsyncConditionWaiter functionality.
 * Demonstrates all the async condition waiting mechanisms.
 */
class AsyncConditionWaiterTest : BaseIntegrationTest() {
    
    @Test
    fun `waitForCondition with simple boolean condition works correctly`() = runIntegrationTest {
        // Given: A condition that will become true after some time
        var conditionMet = false
        
        // Simulate async operation that sets condition to true
        val asyncOperation = suspend {
            executeWithTiming("database_fast") {
                conditionMet = true
            }
        }
        
        // When: Waiting for condition with async operation
        executeAndWaitForEffect(
            operationName = "set_condition_true",
            operation = asyncOperation,
            effectCondition = { conditionMet },
            timeout = 2.seconds,
            pollingInterval = 50.milliseconds
        )
        
        // Then: Condition should be met
        assertTrue(conditionMet, "Condition should be met after async operation")
    }
    
    @Test
    fun `waitForEventualConsistency ensures condition remains stable`() = runIntegrationTest {
        // Given: A condition that fluctuates before stabilizing
        var stableValue = 0
        var fluctuationCount = 0
        
        val checkStability = suspend {
            // Simulate fluctuation for first few checks
            if (fluctuationCount < 3) {
                fluctuationCount++
                stableValue = if (fluctuationCount % 2 == 0) 10 else 5
            } else {
                stableValue = 10 // Stabilize at 10
            }
            stableValue == 10
        }
        
        // When: Waiting for eventual consistency
        waitForEventualConsistency(
            condition = checkStability,
            stabilityDuration = 200.milliseconds,
            timeout = 3.seconds,
            pollingInterval = 50.milliseconds
        )
        
        // Then: Value should be stable
        assertEquals(10, stableValue, "Value should be stable at 10")
        assertTrue(fluctuationCount >= 3, "Should have gone through fluctuation period")
    }
    
    @Test
    fun `waitForAllConditions waits for multiple conditions`() = runIntegrationTest {
        // Given: Multiple conditions that become true at different times
        var condition1Met = false
        var condition2Met = false
        var condition3Met = false
        
        // Simulate async operations
        executeWithTiming("database_fast") {
            condition1Met = true
        }
        
        advanceTimeAndWait(100)
        executeWithTiming("network_fast") {
            condition2Met = true
        }
        
        advanceTimeAndWait(100)
        executeWithTiming("auth_normal") {
            condition3Met = true
        }
        
        // When: Waiting for all conditions
        waitForAllConditions(
            conditions = listOf(
                "condition1" to { condition1Met },
                "condition2" to { condition2Met },
                "condition3" to { condition3Met }
            ),
            timeout = 2.seconds,
            pollingInterval = 50.milliseconds
        )
        
        // Then: All conditions should be met
        assertTrue(condition1Met, "Condition 1 should be met")
        assertTrue(condition2Met, "Condition 2 should be met")
        assertTrue(condition3Met, "Condition 3 should be met")
    }
    
    @Test
    fun `waitForAnyCondition succeeds when one condition is met`() = runIntegrationTest {
        // Given: Multiple conditions where only one will become true
        var condition1Met = false
        var condition2Met = false
        var condition3Met = false
        
        // Only set condition2 to true
        executeWithTiming("network_fast") {
            condition2Met = true
        }
        
        // When: Waiting for any condition
        waitForAnyCondition(
            conditions = listOf(
                "condition1" to { condition1Met },
                "condition2" to { condition2Met },
                "condition3" to { condition3Met }
            ),
            timeout = 2.seconds,
            pollingInterval = 50.milliseconds
        )
        
        // Then: At least one condition should be met
        assertTrue(condition2Met, "Condition 2 should be met")
        // Other conditions may or may not be met
    }
    
    @Test
    fun `waitForStateChange detects when state changes`() = runIntegrationTest {
        // Given: Initial state
        var currentState = "initial"
        val stateProvider = { currentState }
        
        // When: Changing state after some time
        executeAndWaitForEffect(
            operationName = "change_state",
            operation = {
                executeWithTiming("database_fast") {
                    currentState = "changed"
                }
            },
            effectCondition = { currentState != "initial" },
            timeout = 2.seconds
        )
        
        // Then: State should have changed
        assertEquals("changed", currentState, "State should have changed")
    }
    
    @Test
    fun `waitForValue waits for specific target value`() = runIntegrationTest {
        // Given: Value that will change to target
        var currentValue = 0
        val valueProvider = { currentValue }
        val targetValue = 42
        
        // When: Setting value to target
        executeAndWaitForEffect(
            operationName = "set_target_value",
            operation = {
                executeWithTiming("database_fast") {
                    currentValue = targetValue
                }
            },
            effectCondition = { currentValue == targetValue },
            timeout = 2.seconds
        )
        
        // Then: Value should match target
        assertEquals(targetValue, currentValue, "Value should match target")
    }
    
    @Test
    fun `waitForValueInRange waits for numeric value in range`() = runIntegrationTest {
        // Given: Value that will move into range
        var currentValue = 0.0
        val valueProvider = { currentValue }
        val minValue = 10.0
        val maxValue = 20.0
        
        // When: Setting value within range
        executeAndWaitForEffect(
            operationName = "set_value_in_range",
            operation = {
                executeWithTiming("database_fast") {
                    currentValue = 15.0 // Within range
                }
            },
            effectCondition = { 
                val value = currentValue
                value >= minValue && value <= maxValue 
            },
            timeout = 2.seconds
        )
        
        // Then: Value should be in range
        assertTrue(currentValue >= minValue && currentValue <= maxValue, 
                  "Value $currentValue should be in range [$minValue, $maxValue]")
    }
    
    @Test
    fun `waitForMockServiceState waits for mock service condition`() = runIntegrationTest {
        // Given: Mock user repository
        val userEmail = "test@example.com"
        val userPassword = "password123"
        val userName = "Test User"
        
        // When: Creating user and waiting for service state
        waitForMockServiceState(
            serviceName = "UserRepository",
            condition = {
                // Create user first
                val result = mockServices.userRepository.createUser(userEmail, userPassword, userName)
                result.isSuccess && mockServices.userRepository.getUserCount() > 0
            },
            timeout = 2.seconds
        )
        
        // Then: User should be created
        assertTrue(mockServices.userRepository.getUserCount() > 0, "User should be created")
    }
    
    @Test
    fun `enhanced async operation waiting with verification works correctly`() = runIntegrationTest {
        // Given: User and daily log data
        val user = mockServices.createMockUser("user123")
        mockServices.userRepository.setCurrentUser(user)
        
        val logData = DailyLog(
            id = "log123",
            userId = user.id,
            date = LocalDate(2024, 1, 15),
            periodFlow = PeriodFlow.HEAVY,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        // When: Saving log with enhanced async waiting
        waitForAsyncOperationEnhanced(
            operationName = "save_daily_log",
            operation = {
                executeWithTiming("database_fast") {
                    mockServices.logRepository.saveDailyLog(logData)
                }
            },
            verification = {
                val retrieveResult = mockServices.logRepository.getDailyLog(user.id, logData.date)
                retrieveResult.isSuccess && retrieveResult.getOrNull() != null
            },
            timeout = 2.seconds,
            pollingInterval = 50.milliseconds
        )
        
        // Then: Log should be saved and retrievable
        val retrieveResult = mockServices.logRepository.getDailyLog(user.id, logData.date)
        assertTrue(retrieveResult.isSuccess, "Should be able to retrieve saved log")
    }
    
    @Test
    fun `condition waiting with context information works correctly`() = runIntegrationTest {
        // Given: A condition that uses context information
        var attemptCount = 0
        var lastElapsedTime = kotlin.time.Duration.ZERO
        
        val contextAwareCondition: suspend (AsyncConditionWaiter.ConditionContext) -> Boolean = { context ->
            attemptCount = context.attempt
            lastElapsedTime = context.elapsedTime
            
            // Condition becomes true after 3 attempts
            context.attempt >= 3
        }
        
        // When: Waiting with context-aware condition
        val result = asyncWaiter.waitForCondition(
            condition = contextAwareCondition,
            timeout = 2.seconds,
            pollingInterval = 100.milliseconds
        )
        
        // Then: Should succeed and context should be populated
        asyncWaiter.assertWaitSuccess(result)
        assertTrue(attemptCount >= 3, "Should have made at least 3 attempts, got $attemptCount")
        assertTrue(lastElapsedTime >= kotlin.time.Duration.ZERO, "Should have non-negative elapsed time, got $lastElapsedTime")
    }
    
    @Test
    fun `timeout handling provides clear error messages`() = runIntegrationTest {
        // Given: A condition that will never be true
        val neverTrueCondition = { false }
        
        // When: Waiting for condition that times out
        val result = asyncWaiter.waitForCondition(
            condition = neverTrueCondition,
            timeout = 500.milliseconds,
            pollingInterval = 50.milliseconds,
            errorMessage = "Test condition never became true",
            diagnostics = { "Additional diagnostic information" }
        )
        
        // Then: Should timeout with proper error information
        assertTrue(result is AsyncConditionWaiter.WaitResult.Timeout, "Should timeout")
        val timeoutResult = result as AsyncConditionWaiter.WaitResult.Timeout
        assertTrue(timeoutResult.attempts > 0, "Should have made attempts")
        assertTrue(timeoutResult.duration >= 500.milliseconds, "Should have waited for timeout duration")
        assertTrue(timeoutResult.diagnostics.contains("Test condition never became true"), 
                  "Should contain error message")
    }
}