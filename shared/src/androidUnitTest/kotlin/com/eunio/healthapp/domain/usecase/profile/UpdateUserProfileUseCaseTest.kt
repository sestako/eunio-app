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

class UpdateUserProfileUseCaseTest {
    
    private val userRepository = mockk<UserRepository>()
    private val useCase = UpdateUserProfileUseCase(userRepository)
    
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
    fun `invoke with valid name updates user successfully`() = runTest {
        // Given
        val newName = "Jane Doe"
        coEvery { userRepository.getCurrentUser() } returns Result.success(mockUser)
        coEvery { userRepository.updateUser(any()) } returns Result.success(Unit)
        
        // When
        val result = useCase("user123", name = newName)
        
        // Then
        assertTrue(result.isSuccess)
        val updatedUser = result.getOrNull()!!
        assertEquals(newName, updatedUser.name)
        assertEquals(mockUser.email, updatedUser.email)
        
        coVerify { userRepository.updateUser(match { it.name == newName }) }
    }
    
    @Test
    fun `invoke with valid email updates user successfully`() = runTest {
        // Given
        val newEmail = "jane@example.com"
        coEvery { userRepository.getCurrentUser() } returns Result.success(mockUser)
        coEvery { userRepository.updateUser(any()) } returns Result.success(Unit)
        
        // When
        val result = useCase("user123", email = newEmail)
        
        // Then
        assertTrue(result.isSuccess)
        val updatedUser = result.getOrNull()!!
        assertEquals(newEmail, updatedUser.email)
        assertEquals(mockUser.name, updatedUser.name)
        
        coVerify { userRepository.updateUser(match { it.email == newEmail }) }
    }
    
    @Test
    fun `invoke with both name and email updates both fields`() = runTest {
        // Given
        val newName = "Jane Doe"
        val newEmail = "jane@example.com"
        coEvery { userRepository.getCurrentUser() } returns Result.success(mockUser)
        coEvery { userRepository.updateUser(any()) } returns Result.success(Unit)
        
        // When
        val result = useCase("user123", name = newName, email = newEmail)
        
        // Then
        assertTrue(result.isSuccess)
        val updatedUser = result.getOrNull()!!
        assertEquals(newName, updatedUser.name)
        assertEquals(newEmail, updatedUser.email)
        
        coVerify { 
            userRepository.updateUser(match { 
                it.name == newName && it.email == newEmail 
            }) 
        }
    }
    
    @Test
    fun `invoke with empty name returns validation error`() = runTest {
        // Given
        coEvery { userRepository.getCurrentUser() } returns Result.success(mockUser)
        
        // When
        val result = useCase("user123", name = "")
        
        // Then
        assertTrue(result.isError)
        assertEquals("Name cannot be empty", result.errorOrNull()?.message)
    }
    
    @Test
    fun `invoke with short name returns validation error`() = runTest {
        // Given
        coEvery { userRepository.getCurrentUser() } returns Result.success(mockUser)
        
        // When
        val result = useCase("user123", name = "A")
        
        // Then
        assertTrue(result.isError)
        assertEquals("Name must be at least 2 characters", result.errorOrNull()?.message)
    }
    
    @Test
    fun `invoke with long name returns validation error`() = runTest {
        // Given
        val longName = "A".repeat(51)
        coEvery { userRepository.getCurrentUser() } returns Result.success(mockUser)
        
        // When
        val result = useCase("user123", name = longName)
        
        // Then
        assertTrue(result.isError)
        assertEquals("Name cannot exceed 50 characters", result.errorOrNull()?.message)
    }
    
    @Test
    fun `invoke with invalid email returns validation error`() = runTest {
        // Given
        coEvery { userRepository.getCurrentUser() } returns Result.success(mockUser)
        
        // When
        val result = useCase("user123", email = "invalid-email")
        
        // Then
        assertTrue(result.isError)
        assertEquals("Please enter a valid email address", result.errorOrNull()?.message)
    }
    
    @Test
    fun `invoke with empty email returns validation error`() = runTest {
        // Given
        coEvery { userRepository.getCurrentUser() } returns Result.success(mockUser)
        
        // When
        val result = useCase("user123", email = "")
        
        // Then
        assertTrue(result.isError)
        assertEquals("Email cannot be empty", result.errorOrNull()?.message)
    }
    
    @Test
    fun `invoke when user not found returns error`() = runTest {
        // Given
        coEvery { userRepository.getCurrentUser() } returns Result.success(null)
        
        // When
        val result = useCase("user123", name = "New Name")
        
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
        val result = useCase("user123", name = "New Name")
        
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
        val result = useCase("user123", name = "New Name")
        
        // Then
        assertTrue(result.isError)
        assertEquals(errorMessage, result.errorOrNull()?.message)
    }
    
    @Test
    fun `invoke with no changes returns current user`() = runTest {
        // Given
        coEvery { userRepository.getCurrentUser() } returns Result.success(mockUser)
        coEvery { userRepository.updateUser(any()) } returns Result.success(Unit)
        
        // When
        val result = useCase("user123", name = null, email = null)
        
        // Then
        assertTrue(result.isSuccess)
        val updatedUser = result.getOrNull()!!
        assertEquals(mockUser.name, updatedUser.name)
        assertEquals(mockUser.email, updatedUser.email)
        
        coVerify { userRepository.updateUser(any()) }
    }
}