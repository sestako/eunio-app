package com.eunio.healthapp.integration

import com.eunio.healthapp.domain.model.*
import com.eunio.healthapp.testutil.BaseIntegrationTest
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Example test demonstrating the enhanced async condition waiting mechanisms.
 * Shows practical usage of the new AsyncConditionWaiter utilities.
 */
class AsyncConditionWaitingExampleTest : BaseIntegrationTest() {
    
    @Test
    fun `user authentication with enhanced async waiting`() = runIntegrationTest {
        // Given: User credentials
        val email = "test@example.com"
        val password = "testPassword123"
        val name = "Test User"
        
        // When: Creating user with enhanced async waiting
        waitForAsyncOperationEnhanced(
            operationName = "create_user",
            operation = {
                executeWithTiming("auth_normal") {
                    mockServices.userRepository.createUser(email, password, name)
                }
            },
            verification = {
                mockServices.userRepository.getUserCount() > 0
            },
            timeout = 3.seconds
        )
        
        // Then: User should be created
        assertTrue(mockServices.userRepository.getUserCount() > 0, "User should be created")
        
        // When: Signing in with enhanced waiting
        waitForAsyncOperationEnhanced(
            operationName = "sign_in_user",
            operation = {
                executeWithTiming("auth_normal") {
                    mockServices.userRepository.signInUser(email, password)
                }
            },
            verification = {
                mockServices.userRepository.hasActiveSession(mockServices.userRepository.getStoredUser(email)?.id ?: "")
            },
            timeout = 3.seconds
        )
        
        // Then: User should have active session
        val user = mockServices.userRepository.getStoredUser(email)
        assertTrue(user != null, "User should exist")
        assertTrue(mockServices.userRepository.hasActiveSession(user!!.id), "User should have active session")
    }
    
    @Test
    fun `data persistence with eventual consistency`() = runIntegrationTest {
        // Given: User and data
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
        
        // When: Saving data and waiting for eventual consistency
        executeAndWaitForEffect(
            operationName = "save_daily_log",
            operation = {
                executeWithTiming("database_fast") {
                    mockServices.logRepository.saveDailyLog(logData)
                }
            },
            effectCondition = {
                val retrieveResult = mockServices.logRepository.getDailyLog(user.id, logData.date)
                retrieveResult.isSuccess && retrieveResult.getOrNull() != null
            },
            timeout = 2.seconds
        )
        
        // Then: Wait for eventual consistency (data remains stable)
        waitForEventualConsistency(
            condition = {
                val retrieveResult = mockServices.logRepository.getDailyLog(user.id, logData.date)
                retrieveResult.isSuccess && retrieveResult.getOrNull()?.id == logData.id
            },
            stabilityDuration = 300.milliseconds,
            timeout = 3.seconds,
            errorMessage = "Data did not achieve eventual consistency"
        )
        
        // Verify final state
        val finalResult = mockServices.logRepository.getDailyLog(user.id, logData.date)
        assertTrue(finalResult.isSuccess, "Data should be consistently available")
        assertEquals(logData.id, finalResult.getOrNull()?.id, "Data should match saved log")
    }
    
    @Test
    fun `multiple conditions with enhanced waiting`() = runIntegrationTest {
        // Given: User credentials
        val email = "user456@test.com"
        val password = "password123"
        val name = "Test User 456"
        
        // When: Creating user and waiting for multiple conditions
        executeAndWaitForEffect(
            operationName = "create_and_setup_user",
            operation = {
                executeWithTiming("auth_normal") {
                    mockServices.userRepository.createUser(email, password, name)
                }
            },
            effectCondition = {
                val userCount = mockServices.userRepository.getUserCount() > 0
                val userData = mockServices.userRepository.getStoredUser(email) != null
                val hasUser = mockServices.userRepository.hasUser(email)
                userCount && userData && hasUser
            },
            timeout = 5.seconds
        )
        
        // Then: All conditions should be satisfied
        assertTrue(mockServices.userRepository.getUserCount() > 0, "User should be created")
        assertTrue(mockServices.userRepository.hasUser(email), "User should exist in repository")
        
        val storedUser = mockServices.userRepository.getStoredUser(email)
        assertTrue(storedUser != null, "User data should be available")
        assertEquals(email, storedUser.email, "User email should match")
        assertEquals(name, storedUser.name, "User name should match")
    }
    
    @Test
    fun `state change detection with enhanced waiting`() = runIntegrationTest {
        // Given: Initial state
        val user = mockServices.createMockUser("user789")
        val initialLogCount = 0
        
        val logsToAdd = listOf(
            DailyLog(
                id = "log1",
                userId = user.id,
                date = LocalDate(2024, 1, 15),
                periodFlow = PeriodFlow.LIGHT,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            ),
            DailyLog(
                id = "log2",
                userId = user.id,
                date = LocalDate(2024, 1, 16),
                periodFlow = PeriodFlow.MEDIUM,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            )
        )
        
        // When: Adding logs and waiting for state change
        executeAndWaitForEffect(
            operationName = "add_logs",
            operation = {
                executeWithTiming("database_fast") {
                    logsToAdd.forEach { log ->
                        mockServices.logRepository.saveDailyLog(log)
                    }
                }
            },
            effectCondition = {
                val result = mockServices.logRepository.getLogCount(user.id)
                val count = result.getOrNull() ?: 0
                count > initialLogCount
            },
            timeout = 3.seconds
        )
        
        // Then: State should have changed
        val finalLogCountResult = mockServices.logRepository.getLogCount(user.id)
        assertTrue(finalLogCountResult.isSuccess, "Should be able to get log count")
        val finalLogCount = finalLogCountResult.getOrNull() ?: 0
        assertTrue(finalLogCount > initialLogCount, "Log count should have increased")
        assertEquals(logsToAdd.size, finalLogCount, "Should have added all logs")
    }
    
    @Test
    fun `value range waiting with enhanced diagnostics`() = runIntegrationTest {
        // Given: User and initial state
        val user = mockServices.createMockUser("user999")
        mockServices.userRepository.setCurrentUser(user)
        
        // When: Performing operations that affect a numeric value
        waitForValueInRange(
            valueProvider = { mockServices.userRepository.getUserCount().toDouble() },
            minValue = 1.0,
            maxValue = 5.0,
            timeout = 3.seconds,
            pollingInterval = 100.milliseconds,
            errorMessage = "User count did not reach expected range"
        )
        
        // Trigger value changes
        val additionalUsers = listOf("user1@test.com", "user2@test.com")
        additionalUsers.forEach { email ->
            executeWithTiming("auth_normal") {
                mockServices.userRepository.createUser(email, "password123", "Test User")
            }
        }
        
        // Then: Value should be in expected range
        val finalUserCount = mockServices.userRepository.getUserCount()
        assertTrue(finalUserCount >= 1, "Should have at least 1 user")
        assertTrue(finalUserCount <= 5, "Should not have more than 5 users")
    }
    
    @Test
    fun `mock service state monitoring with comprehensive diagnostics`() = runIntegrationTest {
        // Given: Clean state
        val serviceName = "UserRepository"
        
        // When: Performing operations and monitoring state
        waitForMockServiceState(
            serviceName = serviceName,
            condition = {
                // Create test users
                val testUsers = listOf(
                    "user1@test.com" to "User One",
                    "user2@test.com" to "User Two"
                )
                
                testUsers.forEach { (email, name) ->
                    executeWithTiming("auth_normal") {
                        mockServices.userRepository.createUser(email, "password123", name)
                    }
                }
                
                // Check if users are created and have active sessions
                mockServices.userRepository.getUserCount() >= 2
            },
            timeout = 5.seconds
        )
        
        // Then: Service should have expected state
        assertTrue(mockServices.userRepository.getUserCount() >= 2, "Should have created users")
        
        // Verify diagnostic information is available
        val diagnostics = getAsyncDiagnostics()
        assertTrue(diagnostics.isNotEmpty(), "Should have diagnostic information")
        assertTrue(diagnostics.contains("Mock Service"), "Should contain mock service diagnostics")
        assertTrue(diagnostics.contains("Async Condition Waiter"), "Should contain async waiter diagnostics")
    }
    
    @Test
    fun `timeout handling with clear error messages`() = runIntegrationTest {
        // Given: A condition that will timeout
        var attemptCount = 0
        
        // When: Using enhanced condition waiting with timeout
        try {
            waitForConditionEnhanced(
                condition = {
                    attemptCount++
                    false // Never becomes true
                },
                timeout = 500.milliseconds,
                pollingInterval = 50.milliseconds,
                errorMessage = "Test condition never became true",
                diagnostics = {
                    "Attempts made: $attemptCount, Mock service state: ${getMockServiceDiagnostics()}"
                }
            )
            
            // Should not reach here
            assertTrue(false, "Should have thrown AssertionError due to timeout")
            
        } catch (e: AssertionError) {
            // Then: Should have clear error message with diagnostics
            assertTrue(e.message?.contains("Test condition never became true") == true, 
                      "Should contain custom error message")
            assertTrue(e.message?.contains("Attempts made:") == true, 
                      "Should contain diagnostic information")
            assertTrue(attemptCount > 0, "Should have made attempts")
        }
    }
}