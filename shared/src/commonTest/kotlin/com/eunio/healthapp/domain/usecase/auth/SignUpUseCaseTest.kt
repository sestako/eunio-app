package com.eunio.healthapp.domain.usecase.auth

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.testutil.MockServices
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SignUpUseCaseTest {
    
    private val mockServices = MockServices()
    private val signUpUseCase = SignUpUseCase(mockServices.authService)
    
    @Test
    fun `sign up with valid credentials should succeed`() = runTest {
        val result = signUpUseCase(
            email = "test@example.com",
            password = "validPassword123",
            name = "Test User"
        )
        
        assertTrue(result is Result.Success)
        assertEquals("test@example.com", result.data.email)
        assertEquals("Test User", result.data.name)
    }
    
    @Test
    fun `sign up with invalid email should fail`() = runTest {
        val result = signUpUseCase(
            email = "invalid-email",
            password = "validPassword123",
            name = "Test User"
        )
        
        assertTrue(result is Result.Error)
        assertTrue(result.error is AppError.ValidationError)
    }
    
    @Test
    fun `sign up with weak password should fail`() = runTest {
        val result = signUpUseCase(
            email = "test@example.com",
            password = "123",
            name = "Test User"
        )
        
        assertTrue(result is Result.Error)
        assertTrue(result.error is AppError.ValidationError)
    }
    
    @Test
    fun `sign up with empty name should fail`() = runTest {
        val result = signUpUseCase(
            email = "test@example.com",
            password = "validPassword123",
            name = ""
        )
        
        assertTrue(result is Result.Error)
        assertTrue(result.error is AppError.ValidationError)
    }
    
    @Test
    fun `sign up with malicious input should be rejected`() = runTest {
        val result = signUpUseCase(
            email = "test@example.com'; DROP TABLE users; --",
            password = "validPassword123",
            name = "Test User"
        )
        
        assertTrue(result is Result.Error)
        assertTrue(result.error is AppError.ValidationError)
    }
}