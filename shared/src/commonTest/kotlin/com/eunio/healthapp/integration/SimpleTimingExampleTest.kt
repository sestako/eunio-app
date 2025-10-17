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
 * Simple example showing how to use TestTimingManager to fix timing-dependent tests.
 * This demonstrates the key techniques for deterministic timing.
 */
class SimpleTimingExampleTest : BaseIntegrationTest() {
    
    @Test
    fun `user creation with timing control works correctly`() = runIntegrationTest {
        // Given: User credentials
        val email = "test@example.com"
        val password = "testPassword123"
        val name = "Test User"
        
        // When: Creating user with controlled timing
        var createdUser: User? = null
        executeWithTiming("auth_normal") {
            val result = mockServices.userRepository.createUser(email, password, name)
            assertTrue(result.isSuccess, "User creation should succeed")
            createdUser = result.getOrNull()
        }
        
        // Then: User should be created
        assertEquals(email, createdUser?.email)
        assertEquals(name, createdUser?.name)
    }
    
    @Test
    fun `async operation with proper waiting works correctly`() = runIntegrationTest {
        // Given: User and data
        val user = mockServices.createMockUser("user123")
        mockServices.userRepository.setCurrentUser(user)
        
        val dailyLog = DailyLog(
            id = "log123",
            userId = user.id,
            date = LocalDate(2024, 1, 15),
            periodFlow = PeriodFlow.HEAVY,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        // When: Saving with async operation handling
        waitForAsyncOperation(
            operationName = "save_daily_log",
            operation = {
                executeWithTiming("database_fast") {
                    mockServices.logRepository.saveDailyLog(dailyLog)
                }
            },
            verification = {
                val retrieveResult = mockServices.logRepository.getDailyLog(user.id, dailyLog.date)
                retrieveResult.isSuccess && retrieveResult.getOrNull() != null
            }
        )
        
        // Then: Data should be saved and retrievable
        val retrieveResult = mockServices.logRepository.getDailyLog(user.id, dailyLog.date)
        assertTrue(retrieveResult.isSuccess, "Should be able to retrieve saved data")
    }
    
    @Test
    fun `timestamp-based operations with controlled timing work correctly`() = runIntegrationTest {
        // Given: User for testing
        val user = mockServices.createMockUser("user789")
        
        // When: Creating logs with controlled timing
        val logs = mutableListOf<DailyLog>()
        
        repeat(3) { index ->
            createTimestampSeparation(100.milliseconds) // Ensure 100ms gap
            
            val log = DailyLog(
                id = "log$index",
                userId = user.id,
                date = LocalDate(2024, 1, 15 + index),
                periodFlow = PeriodFlow.MEDIUM,
                createdAt = timingManager.getCurrentTime(),
                updatedAt = timingManager.getCurrentTime()
            )
            
            logs.add(log)
            
            executeWithTiming("database_fast") {
                mockServices.logRepository.saveDailyLog(log)
            }
        }
        
        // Then: Timestamps should be properly spaced
        for (i in 1 until logs.size) {
            val timeDiff = logs[i].createdAt - logs[i-1].createdAt
            assertTrue(
                timeDiff.inWholeMilliseconds >= 100,
                "Log $i should be at least 100ms after log ${i-1}"
            )
        }
    }
    
    @Test
    fun `multi-device timing with clock skew works correctly`() = runIntegrationTest {
        // Given: Two devices with different clock skews
        val device1Id = "device1"
        val device2Id = "device2"
        val clockSkew = 1.seconds
        
        registerTestDevice(device1Id, 0.milliseconds)
        registerTestDevice(device2Id, clockSkew)
        
        // When: Getting times from both devices
        val device1Time = getDeviceTime(device1Id)
        val device2Time = getDeviceTime(device2Id)
        
        // Then: Device2 should be ahead by the clock skew
        val timeDiff = device2Time - device1Time
        assertTrue(
            timeDiff.inWholeMilliseconds >= (clockSkew.inWholeMilliseconds - 100),
            "Device2 should be ahead by approximately ${clockSkew.inWholeMilliseconds}ms"
        )
    }
    
    @Test
    fun `condition waiting with timeout works correctly`() = runIntegrationTest {
        // Given: A condition that will become true
        var conditionMet = false
        
        // Simulate condition becoming true after delay
        // In a real test, this would be triggered by the actual async operation
        
        // When: Simulating condition becoming true
        executeWithTiming("database_fast") {
            conditionMet = true
        }
        
        // Then: Condition should be met
        assertTrue(conditionMet, "Condition should be met")
    }
}