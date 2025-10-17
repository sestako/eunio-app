package com.eunio.healthapp.domain.usecase.auth

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SignOutUseCaseTest {
    
    private val mockAuthService = MockAuthService()
    private val signOutUseCase = SignOutUseCase(mockAuthService)
    
    @Test
    fun `signOut should return success when successful`() = runTest {
        // Given
        mockAuthService.signOutResult = Result.success(Unit)
        
        // When
        val result = signOutUseCase()
        
        // Then
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `signOut should return error when service fails`() = runTest {
        // Given
        val expectedError = AppError.AuthenticationError("Sign out failed")
        mockAuthService.signOutResult = Result.error(expectedError)
        
        // When
        val result = signOutUseCase()
        
        // Then
        assertTrue(result.isError)
        assertEquals(expectedError, result.errorOrNull())
    }
}