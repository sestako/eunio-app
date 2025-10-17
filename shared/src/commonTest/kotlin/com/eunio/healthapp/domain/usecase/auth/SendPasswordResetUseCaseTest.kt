package com.eunio.healthapp.domain.usecase.auth

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SendPasswordResetUseCaseTest {
    
    private val mockAuthService = TestMockAuthService()
    private val sendPasswordResetUseCase = SendPasswordResetUseCase(mockAuthService)
    
    @Test
    fun `sendPasswordReset with valid email should return success`() = runTest {
        // Given
        val email = "test@example.com"
        mockAuthService.sendPasswordResetResult = Result.Success(Unit)
        
        // When
        val result = sendPasswordResetUseCase(email)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals("test@example.com", mockAuthService.lastPasswordResetEmail)
    }
    
    @Test
    fun `sendPasswordReset with invalid email should return validation error`() = runTest {
        // Given
        val email = "invalid-email"
        
        // When
        val result = sendPasswordResetUseCase(email)
        
        // Then
        assertTrue(result.isError)
        val error = result.errorOrNull() as? AppError.ValidationError
        assertEquals("email", error?.field)
    }
    
    @Test
    fun `sendPasswordReset with empty email should return validation error`() = runTest {
        // Given
        val email = ""
        
        // When
        val result = sendPasswordResetUseCase(email)
        
        // Then
        assertTrue(result.isError)
        val error = result.errorOrNull() as? AppError.ValidationError
        assertEquals("email", error?.field)
    }
    
    @Test
    fun `sendPasswordReset should trim email`() = runTest {
        // Given
        val email = "  test@example.com  "
        mockAuthService.sendPasswordResetResult = Result.Success(Unit)
        
        // When
        val result = sendPasswordResetUseCase(email)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals("test@example.com", mockAuthService.lastPasswordResetEmail)
    }
    
    @Test
    fun `sendPasswordReset with network error should return error`() = runTest {
        // Given
        val email = "test@example.com"
        mockAuthService.sendPasswordResetResult = Result.Error(AppError.NetworkError("Network error"))
        
        // When
        val result = sendPasswordResetUseCase(email)
        
        // Then
        assertTrue(result.isError)
        val error = result.errorOrNull() as? AppError.NetworkError
        assertEquals("Network error", error?.message)
    }
}

class TestMockAuthService : com.eunio.healthapp.data.remote.auth.AuthService {
    var sendPasswordResetResult: Result<Unit> = Result.Success(Unit)
    var lastPasswordResetEmail: String? = null
    
    override val authState: kotlinx.coroutines.flow.Flow<com.eunio.healthapp.domain.model.User?> = 
        kotlinx.coroutines.flow.flowOf(null)
    
    override suspend fun getCurrentUser(): Result<com.eunio.healthapp.domain.model.User?> = Result.Success(null)
    
    override suspend fun signUp(email: String, password: String, name: String): Result<com.eunio.healthapp.domain.model.User> {
        return Result.Error(AppError.AuthenticationError("Not implemented"))
    }
    
    override suspend fun signIn(email: String, password: String): Result<com.eunio.healthapp.domain.model.User> {
        return Result.Error(AppError.AuthenticationError("Not implemented"))
    }
    
    override suspend fun signOut(): Result<Unit> = Result.Success(Unit)
    
    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        lastPasswordResetEmail = email
        return sendPasswordResetResult
    }
    
    override fun isAuthenticated(): Boolean = false
}