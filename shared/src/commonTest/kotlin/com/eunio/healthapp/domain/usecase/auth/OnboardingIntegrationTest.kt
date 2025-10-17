package com.eunio.healthapp.domain.usecase.auth

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

/**
 * Integration test for the complete onboarding flow using both use cases together
 */
class OnboardingIntegrationTest {
    
    private val mockUserRepository = MockUserRepository()
    private val mockAuthService = MockAuthService()
    private val mockUnitSystemInitializer = MockUnitSystemInitializer()
    private val getCurrentUserUseCase = GetCurrentUserUseCase(mockUserRepository, mockAuthService)
    private val completeOnboardingUseCase = CompleteOnboardingUseCase(mockUserRepository, mockUnitSystemInitializer)
    
    @Test
    fun `complete onboarding flow should work end to end`() = runTest {
        // Given - User exists but hasn't completed onboarding
        val userId = "test-user-id"
        val primaryGoal = HealthGoal.CONCEPTION
        val incompleteUser = createTestUser(userId, onboardingComplete = false)
        val completedUser = incompleteUser.copy(onboardingComplete = true, primaryGoal = primaryGoal)
        
        // Setup initial state
        mockUserRepository.getCurrentUserResult = Result.success(incompleteUser)
        mockUserRepository.completeOnboardingResult = Result.success(Unit)
        
        // When - Check if onboarding is needed
        val needsOnboardingResult = getCurrentUserUseCase.needsOnboarding()
        
        // Then - Should need onboarding
        assertTrue(needsOnboardingResult.isSuccess)
        assertTrue(needsOnboardingResult.getOrNull() == true)
        
        // When - Complete onboarding
        val completeResult = completeOnboardingUseCase(userId, primaryGoal)
        
        // Then - Should succeed
        assertTrue(completeResult.isSuccess)
        assertEquals(userId, mockUserRepository.lastOnboardingUserId)
        assertEquals(primaryGoal, mockUserRepository.lastOnboardingGoal)
        
        // When - Update repository to return completed user and check again
        mockUserRepository.getCurrentUserResult = Result.success(completedUser)
        val needsOnboardingAfterResult = getCurrentUserUseCase.needsOnboarding()
        
        // Then - Should no longer need onboarding
        assertTrue(needsOnboardingAfterResult.isSuccess)
        assertFalse(needsOnboardingAfterResult.getOrNull() == true)
        
        // When - Get user with onboarding status
        val userWithStatusResult = getCurrentUserUseCase.getCurrentUserWithOnboardingStatus()
        
        // Then - Should return completed user and true status
        assertTrue(userWithStatusResult.isSuccess)
        val (user, onboardingComplete) = userWithStatusResult.getOrNull()!!
        assertEquals(completedUser, user)
        assertTrue(onboardingComplete)
    }
    
    @Test
    fun `user who already completed onboarding should not need onboarding`() = runTest {
        // Given - User has already completed onboarding
        val userId = "test-user-id"
        val completedUser = createTestUser(userId, onboardingComplete = true)
        
        mockUserRepository.getCurrentUserResult = Result.success(completedUser)
        
        // When - Check if onboarding is needed
        val needsOnboardingResult = getCurrentUserUseCase.needsOnboarding()
        
        // Then - Should not need onboarding
        assertTrue(needsOnboardingResult.isSuccess)
        assertFalse(needsOnboardingResult.getOrNull() == true)
        
        // When - Try to complete onboarding again
        val completeResult = completeOnboardingUseCase(userId, HealthGoal.CYCLE_TRACKING)
        
        // Then - Should fail
        assertTrue(completeResult.isError)
        assertTrue(completeResult.errorOrNull()?.message?.contains("Onboarding already completed") == true)
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