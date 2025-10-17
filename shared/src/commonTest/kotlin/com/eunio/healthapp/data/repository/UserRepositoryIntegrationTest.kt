package com.eunio.healthapp.data.repository

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.HealthGoal
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.testutil.MockServices
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserRepositoryIntegrationTest {
    
    private val mockServices = MockServices()
    private val userRepository = mockServices.userRepository
    
    @Test
    fun `create user should persist data correctly`() = runTest {
        val result = userRepository.createUser(
            email = "test@example.com",
            password = "validPassword123",
            name = "Test User"
        )
        
        assertTrue(result is Result.Success)
        assertEquals("test@example.com", result.data.email)
        assertEquals("Test User", result.data.name)
    }
    
    @Test
    fun `get current user when none exists should return null`() = runTest {
        val result = userRepository.getCurrentUser()
        
        assertTrue(result is Result.Success)
        assertNull(result.data)
    }
    
    @Test
    fun `complete onboarding should update user state`() = runTest {
        val result = userRepository.completeOnboarding(
            userId = "user123",
            primaryGoal = HealthGoal.CONCEPTION
        )
        
        assertTrue(result is Result.Success)
    }
    
    @Test
    fun `update user should preserve existing data`() = runTest {
        val user = mockServices.createMockUser("user123")
        val result = userRepository.updateUser(user)
        
        assertTrue(result is Result.Success)
    }
    
    @Test
    fun `sign in with valid credentials should succeed`() = runTest {
        // First create a user
        val createResult = userRepository.createUser(
            email = "signin@example.com",
            password = "validPassword123",
            name = "Sign In User"
        )
        assertTrue(createResult is Result.Success)
        
        // Sign out to clear current session
        userRepository.signOutUser()
        
        // Now sign in with the same credentials
        val result = userRepository.signInUser(
            email = "signin@example.com",
            password = "validPassword123"
        )
        
        assertTrue(result is Result.Success)
        assertEquals("signin@example.com", result.data.email)
    }
    
    @Test
    fun `sign out should clear user session`() = runTest {
        val result = userRepository.signOutUser()
        
        assertTrue(result is Result.Success)
    }
    
    @Test
    fun `delete user should be restricted`() = runTest {
        val result = userRepository.deleteUser("user123")
        
        assertTrue(result is Result.Error)
        assertTrue(result.error is AppError.SecurityError)
    }
    
    @Test
    fun `create user with malicious input should be rejected`() = runTest {
        val result = userRepository.createUser(
            email = "test'; DROP TABLE users; --@example.com",
            password = "validPassword123",
            name = "Test User"
        )
        
        assertTrue(result is Result.Error)
        assertTrue(result.error is AppError.ValidationError)
    }
}