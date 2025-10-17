package com.eunio.healthapp.domain.usecase.profile

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.HealthGoal
import com.eunio.healthapp.domain.model.UnitSystem
import com.eunio.healthapp.domain.model.User
import com.eunio.healthapp.domain.repository.UserRepository
import com.eunio.healthapp.domain.util.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UpdateHealthGoalUseCaseTest {
    
    private val userRepository = mockk<UserRepository>()
    private val useCase = UpdateHealthGoalUseCase(userRepository)
    
    private val mockUser = User(
        id = "user123",
        email = "test@example.com",
        name = "John Doe",
        onboardingComplete = true,
        primaryGoal = HealthGoal.CYCLE_TRACKING,
        unitSystem = UnitSystem.METRIC,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now()
    )
    
    @Test
    fun `invoke with different goal updates user successfully`() = runTest {
        // Given
        val newGoal = HealthGoal.CONCEPTION
        coEvery { userRepository.getCurrentUser() } returns Result.success(mockUser)
        coEvery { userRepository.updateUser(any()) } returns Result.success(Unit)
        
        // When
        val result = useCase("user123", newGoal)
        
        // Then
        assertTrue(result.isSuccess)
        val updatedUser = result.getOrNull()!!
        assertEquals(newGoal, updatedUser.primaryGoal)
        
        coVerify { userRepository.updateUser(match { it.primaryGoal == newGoal }) }
    }
    
    @Test
    fun `invoke with same goal returns current user without update`() = runTest {
        // Given
        val sameGoal = HealthGoal.CYCLE_TRACKING
        coEvery { userRepository.getCurrentUser() } returns Result.success(mockUser)
        
        // When
        val result = useCase("user123", sameGoal)
        
        // Then
        assertTrue(result.isSuccess)
        val returnedUser = result.getOrNull()!!
        assertEquals(mockUser.primaryGoal, returnedUser.primaryGoal)
        
        coVerify(exactly = 0) { userRepository.updateUser(any()) }
    }
    
    @Test
    fun `invoke when user not found returns error`() = runTest {
        // Given
        coEvery { userRepository.getCurrentUser() } returns Result.success(null)
        
        // When
        val result = useCase("user123", HealthGoal.CONCEPTION)
        
        // Then
        assertTrue(result.isError)
        assertEquals("User not found", result.errorOrNull()?.message)
    }
    
    @Test
    fun `invoke when repository fails returns error`() = runTest {
        // Given
        val errorMessage = "Database error"
        coEvery { userRepository.getCurrentUser() } returns Result.error(AppError.DatabaseError(errorMessage))
        
        // When
        val result = useCase("user123", HealthGoal.CONCEPTION)
        
        // Then
        assertTrue(result.isError)
        assertEquals(errorMessage, result.errorOrNull()?.message)
    }
    
    @Test
    fun `invoke when update fails returns error`() = runTest {
        // Given
        val errorMessage = "Update failed"
        coEvery { userRepository.getCurrentUser() } returns Result.success(mockUser)
        coEvery { userRepository.updateUser(any()) } returns Result.error(AppError.DatabaseError(errorMessage))
        
        // When
        val result = useCase("user123", HealthGoal.CONCEPTION)
        
        // Then
        assertTrue(result.isError)
        assertEquals(errorMessage, result.errorOrNull()?.message)
    }
    
    @Test
    fun `getGoalChangeImpact returns correct impact for conception`() {
        // When
        val impact = useCase.getGoalChangeImpact(HealthGoal.CYCLE_TRACKING, HealthGoal.CONCEPTION)
        
        // Then
        assertTrue(impact.contains("fertility window predictions"))
        assertTrue(impact.contains("optimal conception"))
    }
    
    @Test
    fun `getGoalChangeImpact returns correct impact for contraception`() {
        // When
        val impact = useCase.getGoalChangeImpact(HealthGoal.CYCLE_TRACKING, HealthGoal.CONTRACEPTION)
        
        // Then
        assertTrue(impact.contains("fertile window awareness"))
        assertTrue(impact.contains("contraceptive effectiveness"))
    }
    
    @Test
    fun `getGoalChangeImpact returns correct impact for cycle tracking`() {
        // When
        val impact = useCase.getGoalChangeImpact(HealthGoal.CONCEPTION, HealthGoal.CYCLE_TRACKING)
        
        // Then
        assertTrue(impact.contains("comprehensive cycle analysis"))
        assertTrue(impact.contains("Balance all tracking features equally"))
    }
    
    @Test
    fun `getGoalChangeImpact returns correct impact for general health`() {
        // When
        val impact = useCase.getGoalChangeImpact(HealthGoal.CYCLE_TRACKING, HealthGoal.GENERAL_HEALTH)
        
        // Then
        assertTrue(impact.contains("general wellness insights"))
        assertTrue(impact.contains("holistic health patterns"))
    }
    
    @Test
    fun `getGoalChangeImpact returns no changes message for same goal`() {
        // When
        val impact = useCase.getGoalChangeImpact(HealthGoal.CYCLE_TRACKING, HealthGoal.CYCLE_TRACKING)
        
        // Then
        assertEquals("No changes will be made to your current settings.", impact)
    }
    
    @Test
    fun `getAvailableGoals returns all health goals with descriptions`() {
        // When
        val goals = useCase.getAvailableGoals()
        
        // Then
        assertEquals(4, goals.size)
        
        val goalTypes = goals.map { it.first }.toSet()
        assertTrue(goalTypes.contains(HealthGoal.CONCEPTION))
        assertTrue(goalTypes.contains(HealthGoal.CONTRACEPTION))
        assertTrue(goalTypes.contains(HealthGoal.CYCLE_TRACKING))
        assertTrue(goalTypes.contains(HealthGoal.GENERAL_HEALTH))
        
        // Check that all descriptions are non-empty
        goals.forEach { (_, description) ->
            assertTrue(description.isNotBlank())
        }
    }
}