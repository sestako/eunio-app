package com.eunio.healthapp.domain.usecase.auth

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.testutil.MockServices
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SignInUseCaseTest {
    
    private val mockServices = MockServices()
    private val signInUseCase = SignInUseCase(mockServices.authService)
    
    @Test
    fun `sign in with valid credentials should succeed`() = runTest {
        // First create a user to sign in with
        mockServices.authService.signUp("test@example.com", "validPassword123", "Test User")
        
        val result = signInUseCase(
            email = "test@example.com",
            password = "validPassword123"
        )
        
        assertTrue(result is Result.Success)
        assertEquals("test@example.com", result.data.email)
    }
    
    @Test
    fun `sign in with invalid credentials should fail`() = runTest {
        // First create a user
        mockServices.authService.signUp("test@example.com", "validPassword123", "Test User")
        
        val result = signInUseCase(
            email = "test@example.com",
            password = "wrongpassword"
        )
        
        assertTrue(result is Result.Error)
        assertTrue(result.error is AppError.AuthenticationError)
    }
    
    @Test
    fun `sign in with empty email should fail`() = runTest {
        val result = signInUseCase(
            email = "",
            password = "validPassword123"
        )
        
        assertTrue(result is Result.Error)
        assertTrue(result.error is AppError.ValidationError)
    }
    
    @Test
    fun `sign in with empty password should fail`() = runTest {
        val result = signInUseCase(
            email = "test@example.com",
            password = ""
        )
        
        assertTrue(result is Result.Error)
        assertTrue(result.error is AppError.ValidationError)
    }
    
    @Test
    fun `sign in with malformed email should fail`() = runTest {
        val result = signInUseCase(
            email = "not-an-email",
            password = "validPassword123"
        )
        
        assertTrue(result is Result.Error)
        assertTrue(result.error is AppError.ValidationError)
    }
}