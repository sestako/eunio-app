package com.eunio.healthapp.domain.usecase.auth

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.manager.UnitSystemInitializer
import com.eunio.healthapp.domain.model.HealthGoal
import com.eunio.healthapp.domain.model.UnitSystem
import com.eunio.healthapp.domain.model.User
import com.eunio.healthapp.domain.util.MockUnitSystemInitializer
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CompleteOnboardingUseCaseTest {
    
    private val mockUserRepository = MockUserRepository()
    private val mockUnitSystemInitializer = MockUnitSystemInitializer()
    private val completeOnboardingUseCase = CompleteOnboardingUseCase(mockUserRepository, mockUnitSystemInitializer)
    
    @Test
    fun `completeOnboarding with valid data should succeed`() = runTest {
        // Given
        val userId = "test-user-id"
        val primaryGoal = HealthGoal.CYCLE_TRACKING
        val incompleteUser = createTestUser(onboardingComplete = false)
        val completedUser = incompleteUser.copy(onboardingComplete = true, primaryGoal = primaryGoal)
        
        mockUserRepository.getCurrentUserResult = Result.success(incompleteUser)
        mockUserRepository.completeOnboardingResult = Result.success(Unit)
        
        // When
        val result = completeOnboardingUseCase(userId, primaryGoal)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(userId, mockUserRepository.lastOnboardingUserId)
        assertEquals(primaryGoal, mockUserRepository.lastOnboardingGoal)
    }
    
    @Test
    fun `completeOnboarding with blank userId should fail`() = runTest {
        // Given
        val userId = ""
        val primaryGoal = HealthGoal.CYCLE_TRACKING
        
        // When
        val result = completeOnboardingUseCase(userId, primaryGoal)
        
        // Then
        assertTrue(result.isError)
        assertTrue(result.errorOrNull()?.message?.contains("Validation failed") == true)
    }
    
    @Test
    fun `completeOnboarding when no current user should fail`() = runTest {
        // Given
        val userId = "test-user-id"
        val primaryGoal = HealthGoal.CYCLE_TRACKING
        
        mockUserRepository.getCurrentUserResult = Result.success(null)
        
        // When
        val result = completeOnboardingUseCase(userId, primaryGoal)
        
        // Then
        assertTrue(result.isError)
        assertTrue(result.errorOrNull()?.message?.contains("No authenticated user found") == true)
    }
    
    @Test
    fun `completeOnboarding with mismatched userId should fail`() = runTest {
        // Given
        val userId = "test-user-id"
        val differentUserId = "different-user-id"
        val primaryGoal = HealthGoal.CYCLE_TRACKING
        val user = createTestUser(id = differentUserId, onboardingComplete = false)
        
        mockUserRepository.getCurrentUserResult = Result.success(user)
        
        // When
        val result = completeOnboardingUseCase(userId, primaryGoal)
        
        // Then
        assertTrue(result.isError)
        assertTrue(result.errorOrNull()?.message?.contains("User ID mismatch") == true)
    }
    
    @Test
    fun `completeOnboarding when already completed should fail`() = runTest {
        // Given
        val userId = "test-user-id"
        val primaryGoal = HealthGoal.CYCLE_TRACKING
        val completedUser = createTestUser(onboardingComplete = true)
        
        mockUserRepository.getCurrentUserResult = Result.success(completedUser)
        
        // When
        val result = completeOnboardingUseCase(userId, primaryGoal)
        
        // Then
        assertTrue(result.isError)
        assertTrue(result.errorOrNull()?.message?.contains("Onboarding already completed") == true)
    }
    
    @Test
    fun `completeOnboarding when repository fails should fail`() = runTest {
        // Given
        val userId = "test-user-id"
        val primaryGoal = HealthGoal.CYCLE_TRACKING
        val incompleteUser = createTestUser(onboardingComplete = false)
        
        mockUserRepository.getCurrentUserResult = Result.success(incompleteUser)
        mockUserRepository.completeOnboardingResult = Result.error(AppError.DatabaseError("Repository error"))
        
        // When
        val result = completeOnboardingUseCase(userId, primaryGoal)
        
        // Then
        assertTrue(result.isError)
        assertTrue(result.errorOrNull()?.message?.contains("Failed to complete onboarding") == true)
    }
    
    @Test
    fun `isOnboardingComplete with completed user should return true`() = runTest {
        // Given
        val userId = "test-user-id"
        val completedUser = createTestUser(onboardingComplete = true)
        
        mockUserRepository.getCurrentUserResult = Result.success(completedUser)
        
        // When
        val result = completeOnboardingUseCase.isOnboardingComplete(userId)
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
    }
    
    @Test
    fun `isOnboardingComplete with incomplete user should return false`() = runTest {
        // Given
        val userId = "test-user-id"
        val incompleteUser = createTestUser(onboardingComplete = false)
        
        mockUserRepository.getCurrentUserResult = Result.success(incompleteUser)
        
        // When
        val result = completeOnboardingUseCase.isOnboardingComplete(userId)
        
        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() == true)
    }
    
    @Test
    fun `isOnboardingComplete with blank userId should fail`() = runTest {
        // Given
        val userId = ""
        
        // When
        val result = completeOnboardingUseCase.isOnboardingComplete(userId)
        
        // Then
        assertTrue(result.isError)
        assertTrue(result.errorOrNull()?.message?.contains("User ID cannot be blank") == true)
    }
    
    @Test
    fun `isOnboardingComplete with no current user should fail`() = runTest {
        // Given
        val userId = "test-user-id"
        
        mockUserRepository.getCurrentUserResult = Result.success(null)
        
        // When
        val result = completeOnboardingUseCase.isOnboardingComplete(userId)
        
        // Then
        assertTrue(result.isError)
        assertTrue(result.errorOrNull()?.message?.contains("No authenticated user found") == true)
    }
    
    @Test
    fun `isOnboardingComplete with mismatched userId should fail`() = runTest {
        // Given
        val userId = "test-user-id"
        val differentUserId = "different-user-id"
        val user = createTestUser(id = differentUserId)
        
        mockUserRepository.getCurrentUserResult = Result.success(user)
        
        // When
        val result = completeOnboardingUseCase.isOnboardingComplete(userId)
        
        // Then
        assertTrue(result.isError)
        assertTrue(result.errorOrNull()?.message?.contains("User ID mismatch") == true)
    }
    
    private fun createTestUser(
        id: String = "test-user-id",
        onboardingComplete: Boolean = false
    ): User {
        val now = Clock.System.now()
        return User(
            id = id,
            email = "test@example.com",
            name = "Test User",
            onboardingComplete = onboardingComplete,
            primaryGoal = HealthGoal.CYCLE_TRACKING,
            createdAt = now,
            updatedAt = now
        )
    }
}