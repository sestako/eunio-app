package com.eunio.healthapp.domain.usecase.auth

import com.eunio.healthapp.domain.model.HealthGoal
import com.eunio.healthapp.domain.model.User
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GetCurrentUserUseCaseTest {
    
    private val mockUserRepository = MockUserRepository()
    private val mockAuthService = MockAuthService()
    private val getCurrentUserUseCase = GetCurrentUserUseCase(mockUserRepository, mockAuthService)
    
    @Test
    fun `getCurrentUser when authenticated should return user`() = runTest {
        // Given
        val expectedUser = createTestUser()
        mockUserRepository.getCurrentUserResult = Result.success(expectedUser)
        
        // When
        val result = getCurrentUserUseCase()
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedUser, result.getOrNull())
    }
    
    @Test
    fun `getCurrentUser when not authenticated should return null`() = runTest {
        // Given
        mockUserRepository.getCurrentUserResult = Result.success(null)
        
        // When
        val result = getCurrentUserUseCase()
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(null, result.getOrNull())
    }
    
    @Test
    fun `isAuthenticated when user is authenticated should return true`() {
        // Given
        mockAuthService.isAuthenticatedValue = true
        
        // When
        val result = getCurrentUserUseCase.isAuthenticated()
        
        // Then
        assertTrue(result)
    }
    
    @Test
    fun `isAuthenticated when user is not authenticated should return false`() {
        // Given
        mockAuthService.isAuthenticatedValue = false
        
        // When
        val result = getCurrentUserUseCase.isAuthenticated()
        
        // Then
        assertFalse(result)
    }
    
    @Test
    fun `observeAuthState should return auth state flow`() = runTest {
        // Given
        val expectedUser = createTestUser()
        mockAuthService.authStateValue = expectedUser
        
        // When
        val authStateFlow = getCurrentUserUseCase.observeAuthState()
        
        // Then
        // Note: In a real test, you would collect from the flow and verify the emitted values
        // For this simple test, we just verify the flow is returned
        assertEquals(mockAuthService.authState, authStateFlow)
    }
    
    @Test
    fun `getCurrentUserWithOnboardingStatus with completed user should return user and true`() = runTest {
        // Given
        val completedUser = createTestUser(onboardingComplete = true)
        mockUserRepository.getCurrentUserResult = Result.success(completedUser)
        
        // When
        val result = getCurrentUserUseCase.getCurrentUserWithOnboardingStatus()
        
        // Then
        assertTrue(result.isSuccess)
        val (user, onboardingComplete) = result.getOrNull()!!
        assertEquals(completedUser, user)
        assertTrue(onboardingComplete)
    }
    
    @Test
    fun `getCurrentUserWithOnboardingStatus with incomplete user should return user and false`() = runTest {
        // Given
        val incompleteUser = createTestUser(onboardingComplete = false)
        mockUserRepository.getCurrentUserResult = Result.success(incompleteUser)
        
        // When
        val result = getCurrentUserUseCase.getCurrentUserWithOnboardingStatus()
        
        // Then
        assertTrue(result.isSuccess)
        val (user, onboardingComplete) = result.getOrNull()!!
        assertEquals(incompleteUser, user)
        assertFalse(onboardingComplete)
    }
    
    @Test
    fun `getCurrentUserWithOnboardingStatus with no user should return null and false`() = runTest {
        // Given
        mockUserRepository.getCurrentUserResult = Result.success(null)
        
        // When
        val result = getCurrentUserUseCase.getCurrentUserWithOnboardingStatus()
        
        // Then
        assertTrue(result.isSuccess)
        val (user, onboardingComplete) = result.getOrNull()!!
        assertEquals(null, user)
        assertFalse(onboardingComplete)
    }
    
    @Test
    fun `needsOnboarding with incomplete user should return true`() = runTest {
        // Given
        val incompleteUser = createTestUser(onboardingComplete = false)
        mockUserRepository.getCurrentUserResult = Result.success(incompleteUser)
        
        // When
        val result = getCurrentUserUseCase.needsOnboarding()
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
    }
    
    @Test
    fun `needsOnboarding with completed user should return false`() = runTest {
        // Given
        val completedUser = createTestUser(onboardingComplete = true)
        mockUserRepository.getCurrentUserResult = Result.success(completedUser)
        
        // When
        val result = getCurrentUserUseCase.needsOnboarding()
        
        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() == true)
    }
    
    @Test
    fun `needsOnboarding with no user should return false`() = runTest {
        // Given
        mockUserRepository.getCurrentUserResult = Result.success(null)
        
        // When
        val result = getCurrentUserUseCase.needsOnboarding()
        
        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() == true)
    }
    
    @Test
    fun `needsOnboarding when repository fails should return error`() = runTest {
        // Given
        mockUserRepository.getCurrentUserResult = Result.error(com.eunio.healthapp.domain.error.AppError.DatabaseError("Repository error"))
        
        // When
        val result = getCurrentUserUseCase.needsOnboarding()
        
        // Then
        assertTrue(result.isError)
    }
    
    private fun createTestUser(onboardingComplete: Boolean = true): User {
        val now = Clock.System.now()
        return User(
            id = "test-id",
            email = "test@example.com",
            name = "Test User",
            onboardingComplete = onboardingComplete,
            primaryGoal = HealthGoal.CYCLE_TRACKING,
            createdAt = now,
            updatedAt = now
        )
    }
}