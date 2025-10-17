package com.eunio.healthapp.testutil

import com.eunio.healthapp.domain.model.HealthGoal
import com.eunio.healthapp.domain.util.Result
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test to validate that BaseIntegrationTest is working correctly
 */
class BaseIntegrationTestValidation : BaseIntegrationTest() {
    
    @Test
    fun `test state management and isolation works correctly`() = runIntegrationTest {
        // Create isolated test state
        val testState = createTestState("test-state-management")
        
        assertNotNull(testState)
        assertEquals("test-state-management", testState.testName)
        assertTrue(testState.userId.startsWith("test-user-"))
        assertTrue(testState.sessionId.startsWith("session-"))
        
        // Verify mock services are initialized
        assertNotNull(mockServices)
        assertNotNull(mockServices.userRepository)
        assertNotNull(mockServices.logRepository)
        
        // Test that mock services are properly reset
        assertEquals(0, mockServices.userRepository.getUserCount())
        assertEquals(0, mockServices.userRepository.getActiveSessionCount())
    }
    
    @Test
    fun `test async operation handling works correctly`() = runIntegrationTest {
        // Test async operation with proper error handling
        executeWithDiagnostics("user-creation") {
            val result = mockServices.userRepository.createUser(
                email = "test@example.com",
                password = "password123",
                name = "Test User"
            )
            assertTrue(result is Result.Success)
        }
        
        // Verify user was created
        assertEquals(1, mockServices.userRepository.getUserCount())
    }
    
    @Test
    fun `test wait for async operation works correctly`() = runIntegrationTest {
        waitForAsyncOperation(
            operationName = "user-onboarding",
            operation = {
                // Create user first
                val createResult = mockServices.userRepository.createUser(
                    email = "test2@example.com",
                    password = "password123",
                    name = "Test User 2"
                )
                assertTrue(createResult is Result.Success)
                
                // Complete onboarding
                val user = createResult.data
                val onboardingResult = mockServices.userRepository.completeOnboarding(
                    userId = user.id,
                    primaryGoal = HealthGoal.CYCLE_TRACKING
                )
                assertTrue(onboardingResult is Result.Success)
            },
            verification = {
                // Verify user exists and onboarding is complete
                val currentUserResult = mockServices.userRepository.getCurrentUser()
                if (currentUserResult is Result.Success && currentUserResult.data != null) {
                    currentUserResult.data!!.onboardingComplete
                } else {
                    false
                }
            }
        )
    }
    
    @Test
    fun `test diagnostic information is available`() = runIntegrationTest {
        // Create some test data
        val result = mockServices.userRepository.createUser(
            email = "diagnostic@example.com",
            password = "password123",
            name = "Diagnostic User"
        )
        assertTrue(result is Result.Success)
        
        // Get diagnostics
        val diagnostics = getMockServiceDiagnostics()
        
        assertTrue(diagnostics.contains("Mock Services"))
        assertTrue(diagnostics.contains("User Repository"))
        assertTrue(diagnostics.contains("Active Jobs"))
        assertTrue(diagnostics.contains("Test Dispatcher"))
        assertTrue(diagnostics.contains("Test State Manager"))
    }
    
    @Test
    fun `test proper cleanup between tests`() = runIntegrationTest {
        // This test verifies that state is properly isolated between tests
        // If the previous tests didn't clean up properly, this would fail
        
        assertEquals(0, mockServices.userRepository.getUserCount())
        assertEquals(0, mockServices.userRepository.getActiveSessionCount())
        
        // Create a user in this test
        val result = mockServices.userRepository.createUser(
            email = "cleanup@example.com",
            password = "password123",
            name = "Cleanup User"
        )
        assertTrue(result is Result.Success)
        
        assertEquals(1, mockServices.userRepository.getUserCount())
    }
}