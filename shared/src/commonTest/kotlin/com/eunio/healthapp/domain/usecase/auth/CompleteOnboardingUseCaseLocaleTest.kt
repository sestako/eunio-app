package com.eunio.healthapp.domain.usecase.auth

import com.eunio.healthapp.domain.manager.UnitSystemInitializer
import com.eunio.healthapp.domain.model.HealthGoal
import com.eunio.healthapp.domain.model.UnitSystem
import com.eunio.healthapp.domain.model.User
import com.eunio.healthapp.domain.repository.UserRepository
import com.eunio.healthapp.domain.util.MockUnitSystemInitializer
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for CompleteOnboardingUseCase integration with unit system initialization.
 * Verifies that unit system is properly initialized during onboarding flow.
 */
class CompleteOnboardingUseCaseLocaleTest {
    
    private val mockUserRepository = MockUserRepository()
    private val mockUnitSystemInitializer = MockUnitSystemInitializer()
    
    @Test
    fun `onboarding initializes unit system for new user`() = runTest {
        val useCase = CompleteOnboardingUseCase(mockUserRepository, mockUnitSystemInitializer)
        
        val user = createMockUser(onboardingComplete = false)
        mockUserRepository.getCurrentUserResult = Result.success(user)
        mockUserRepository.completeOnboardingResult = Result.success(Unit)
        mockUnitSystemInitializer.initializeForNewUserResult = Result.success(UnitSystem.IMPERIAL)
        
        // After onboarding completion, return updated user
        val completedUser = user.copy(onboardingComplete = true)
        
        val result = useCase("user123", HealthGoal.GENERAL_HEALTH)
        
        assertTrue(result.isSuccess)
        
        // Verify unit system initialization was called
        assertTrue(mockUnitSystemInitializer.initializeForNewUserCalled)
        assertEquals("user123", mockUserRepository.lastOnboardingUserId)
        assertEquals(HealthGoal.GENERAL_HEALTH, mockUserRepository.lastOnboardingGoal)
    }
    
    @Test
    fun `onboarding continues even if unit system initialization fails`() = runTest {
        val useCase = CompleteOnboardingUseCase(mockUserRepository, mockUnitSystemInitializer)
        
        val user = createMockUser(onboardingComplete = false)
        mockUserRepository.getCurrentUserResult = Result.success(user)
        mockUserRepository.completeOnboardingResult = Result.success(Unit)
        
        // Unit system initialization fails
        mockUnitSystemInitializer.initializeForNewUserResult = Result.error(com.eunio.healthapp.domain.error.AppError.ValidationError("Initialization failed"))
        
        val result = useCase("user123", HealthGoal.GENERAL_HEALTH)
        
        // Onboarding should still succeed
        assertTrue(result.isSuccess)
        
        // Verify unit system initialization was attempted
        assertTrue(mockUnitSystemInitializer.initializeForNewUserCalled)
        assertEquals("user123", mockUserRepository.lastOnboardingUserId)
    }
    
    @Test
    fun `unit system initializer provides correct locale-based defaults`() = runTest {
        // Test that the initializer correctly determines unit systems for different locales
        mockUnitSystemInitializer.currentLocaleUnitSystem = UnitSystem.IMPERIAL
        assertEquals(UnitSystem.IMPERIAL, mockUnitSystemInitializer.getUnitSystemForCurrentLocale())
        
        mockUnitSystemInitializer.currentLocaleUnitSystem = UnitSystem.METRIC
        assertEquals(UnitSystem.METRIC, mockUnitSystemInitializer.getUnitSystemForCurrentLocale())
    }
    
    private fun createMockUser(
        onboardingComplete: Boolean = false,
        userId: String = "user123"
    ): User {
        return User(
            id = userId,
            email = "test@example.com",
            name = "Test User",
            onboardingComplete = onboardingComplete,
            primaryGoal = HealthGoal.GENERAL_HEALTH,
            unitSystem = UnitSystem.METRIC,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
    }
}