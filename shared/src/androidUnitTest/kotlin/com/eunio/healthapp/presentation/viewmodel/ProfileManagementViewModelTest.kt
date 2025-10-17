package com.eunio.healthapp.presentation.viewmodel

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.HealthGoal
import com.eunio.healthapp.domain.model.UnitSystem
import com.eunio.healthapp.domain.model.User
import com.eunio.healthapp.domain.usecase.auth.GetCurrentUserUseCase
import com.eunio.healthapp.domain.usecase.profile.GetUserStatisticsUseCase
import com.eunio.healthapp.domain.usecase.profile.UpdateHealthGoalUseCase
import com.eunio.healthapp.domain.usecase.profile.UpdateUserProfileUseCase
import com.eunio.healthapp.domain.usecase.profile.UserStatistics
import com.eunio.healthapp.domain.util.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileManagementViewModelTest {
    
    private val getCurrentUserUseCase = mockk<GetCurrentUserUseCase>()
    private val updateUserProfileUseCase = mockk<UpdateUserProfileUseCase>()
    private val updateHealthGoalUseCase = mockk<UpdateHealthGoalUseCase>()
    private val getUserStatisticsUseCase = mockk<GetUserStatisticsUseCase>()
    
    private lateinit var viewModel: ProfileManagementViewModel
    
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
    
    private val mockStatistics = UserStatistics(
        accountCreatedAt = Clock.System.now(),
        lastUpdatedAt = Clock.System.now(),
        daysSinceCreation = 30,
        daysSinceLastUpdate = 1,
        currentGoal = HealthGoal.CYCLE_TRACKING,
        onboardingCompleted = true,
        totalLogsEntered = 25,
        cyclesTracked = 3,
        insightsGenerated = 10,
        lastLoginDate = Clock.System.now()
    )
    
    @BeforeTest
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        viewModel = ProfileManagementViewModel(
            getCurrentUserUseCase,
            updateUserProfileUseCase,
            updateHealthGoalUseCase,
            getUserStatisticsUseCase
        )
    }
    
    @Test
    fun `loadProfile loads user and statistics successfully`() = runTest {
        // Given
        coEvery { getCurrentUserUseCase() } returns Result.success(mockUser)
        coEvery { getUserStatisticsUseCase() } returns Result.success(mockStatistics)
        
        // When
        viewModel.loadProfile()
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(mockUser, state.user)
        assertEquals(mockStatistics, state.statistics)
        assertEquals(mockUser.name, state.editedName)
        assertEquals(mockUser.email, state.editedEmail)
    }
    
    @Test
    fun `loadProfile handles user not found error`() = runTest {
        // Given
        coEvery { getCurrentUserUseCase() } returns Result.success(null)
        
        // When
        viewModel.loadProfile()
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("User not found", state.errorMessage)
        assertNull(state.user)
    }
    
    @Test
    fun `loadProfile handles repository error`() = runTest {
        // Given
        val errorMessage = "Network error"
        coEvery { getCurrentUserUseCase() } returns Result.error(AppError.DatabaseError(errorMessage))
        
        // When
        viewModel.loadProfile()
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.errorMessage!!.contains(errorMessage))
        assertNull(state.user)
    }
    
    @Test
    fun `startEditing sets editing mode and clears errors`() = runTest {
        // Given
        coEvery { getCurrentUserUseCase() } returns Result.success(mockUser)
        coEvery { getUserStatisticsUseCase() } returns Result.success(mockStatistics)
        viewModel.loadProfile()
        advanceUntilIdle()
        
        // When
        viewModel.startEditing()
        
        // Then
        val state = viewModel.uiState.value
        assertTrue(state.isEditing)
        assertNull(state.errorMessage)
        assertNull(state.successMessage)
        assertEquals(mockUser.name, state.editedName)
        assertEquals(mockUser.email, state.editedEmail)
    }
    
    @Test
    fun `cancelEditing reverts changes and exits editing mode`() = runTest {
        // Given
        coEvery { getCurrentUserUseCase() } returns Result.success(mockUser)
        coEvery { getUserStatisticsUseCase() } returns Result.success(mockStatistics)
        viewModel.loadProfile()
        advanceUntilIdle()
        
        viewModel.startEditing()
        viewModel.updateName("Changed Name")
        
        // When
        viewModel.cancelEditing()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isEditing)
        assertFalse(state.hasUnsavedChanges)
        assertEquals(mockUser.name, state.editedName)
        assertEquals(mockUser.email, state.editedEmail)
    }
    
    @Test
    fun `updateName validates and sets unsaved changes flag`() = runTest {
        // Given
        coEvery { getCurrentUserUseCase() } returns Result.success(mockUser)
        coEvery { getUserStatisticsUseCase() } returns Result.success(mockStatistics)
        viewModel.loadProfile()
        advanceUntilIdle()
        
        viewModel.startEditing()
        
        // When
        viewModel.updateName("Jane Doe")
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Jane Doe", state.editedName)
        assertTrue(state.hasUnsavedChanges)
        assertNull(state.nameError)
    }
    
    @Test
    fun `updateName with invalid name sets validation error`() = runTest {
        // Given
        coEvery { getCurrentUserUseCase() } returns Result.success(mockUser)
        coEvery { getUserStatisticsUseCase() } returns Result.success(mockStatistics)
        viewModel.loadProfile()
        advanceUntilIdle()
        
        viewModel.startEditing()
        
        // When
        viewModel.updateName("")
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("", state.editedName)
        assertEquals("Name cannot be empty", state.nameError)
    }
    
    @Test
    fun `updateEmail validates and sets unsaved changes flag`() = runTest {
        // Given
        coEvery { getCurrentUserUseCase() } returns Result.success(mockUser)
        coEvery { getUserStatisticsUseCase() } returns Result.success(mockStatistics)
        viewModel.loadProfile()
        advanceUntilIdle()
        
        viewModel.startEditing()
        
        // When
        viewModel.updateEmail("jane@example.com")
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("jane@example.com", state.editedEmail)
        assertTrue(state.hasUnsavedChanges)
        assertTrue(state.emailVerificationRequired)
        assertNull(state.emailError)
    }
    
    @Test
    fun `updateEmail with invalid email sets validation error`() = runTest {
        // Given
        coEvery { getCurrentUserUseCase() } returns Result.success(mockUser)
        coEvery { getUserStatisticsUseCase() } returns Result.success(mockStatistics)
        viewModel.loadProfile()
        advanceUntilIdle()
        
        viewModel.startEditing()
        
        // When
        viewModel.updateEmail("invalid-email")
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("invalid-email", state.editedEmail)
        assertEquals("Please enter a valid email address", state.emailError)
    }
    
    @Test
    fun `saveProfile updates user successfully`() = runTest {
        // Given
        val updatedUser = mockUser.copy(name = "Jane Doe", email = "jane@example.com")
        coEvery { getCurrentUserUseCase() } returns Result.success(mockUser)
        coEvery { getUserStatisticsUseCase() } returns Result.success(mockStatistics)
        coEvery { updateUserProfileUseCase(any(), any(), any()) } returns Result.success(updatedUser)
        
        viewModel.loadProfile()
        advanceUntilIdle()
        
        viewModel.startEditing()
        viewModel.updateName("Jane Doe")
        viewModel.updateEmail("jane@example.com")
        
        // When
        viewModel.saveProfile()
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isSaving)
        assertFalse(state.isEditing)
        assertFalse(state.hasUnsavedChanges)
        assertEquals(updatedUser, state.user)
        assertNotNull(state.successMessage)
        
        coVerify { 
            updateUserProfileUseCase(
                userId = mockUser.id,
                name = "Jane Doe",
                email = "jane@example.com"
            )
        }
    }
    
    @Test
    fun `saveProfile handles validation errors`() = runTest {
        // Given
        coEvery { getCurrentUserUseCase() } returns Result.success(mockUser)
        coEvery { getUserStatisticsUseCase() } returns Result.success(mockStatistics)
        
        viewModel.loadProfile()
        advanceUntilIdle()
        
        viewModel.startEditing()
        viewModel.updateName("") // Invalid name
        
        // When
        viewModel.saveProfile()
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isSaving)
        assertTrue(state.isEditing) // Still in editing mode
        
        coVerify(exactly = 0) { updateUserProfileUseCase(any(), any(), any()) }
    }
    
    @Test
    fun `selectHealthGoal shows confirmation for different goal`() = runTest {
        // Given
        coEvery { getCurrentUserUseCase() } returns Result.success(mockUser)
        coEvery { getUserStatisticsUseCase() } returns Result.success(mockStatistics)
        every { updateHealthGoalUseCase.getGoalChangeImpact(any(), any()) } returns "Impact description"
        
        viewModel.loadProfile()
        advanceUntilIdle()
        
        // When
        viewModel.selectHealthGoal(HealthGoal.CONCEPTION)
        
        // Then
        val state = viewModel.uiState.value
        assertEquals(HealthGoal.CONCEPTION, state.selectedGoal)
        assertTrue(state.showGoalConfirmation)
        assertEquals("Impact description", state.goalChangeImpact)
    }
    
    @Test
    fun `confirmGoalChange updates health goal successfully`() = runTest {
        // Given
        val updatedUser = mockUser.copy(primaryGoal = HealthGoal.CONCEPTION)
        coEvery { getCurrentUserUseCase() } returns Result.success(mockUser)
        coEvery { getUserStatisticsUseCase() } returns Result.success(mockStatistics)
        coEvery { updateHealthGoalUseCase(any(), any()) } returns Result.success(updatedUser)
        every { updateHealthGoalUseCase.getGoalChangeImpact(any(), any()) } returns "Impact description"
        
        viewModel.loadProfile()
        advanceUntilIdle()
        
        viewModel.selectHealthGoal(HealthGoal.CONCEPTION)
        
        // When
        viewModel.confirmGoalChange()
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isSaving)
        assertFalse(state.isSelectingGoal)
        assertFalse(state.showGoalConfirmation)
        assertEquals(updatedUser, state.user)
        assertNotNull(state.successMessage)
        
        coVerify { updateHealthGoalUseCase(mockUser.id, HealthGoal.CONCEPTION) }
    }
    
    @Test
    fun `clearMessages clears all error and success messages`() = runTest {
        // Given - trigger an error first to set error message
        coEvery { getUserStatisticsUseCase() } returns Result.error(
            com.eunio.healthapp.domain.error.AppError.UnknownError("Test error")
        )
        
        // Load user profile to trigger error state
        viewModel.loadProfile()
        advanceUntilIdle()
        
        // Verify error message is set
        var state = viewModel.uiState.value
        assertNotNull(state.errorMessage)
        
        // When
        viewModel.clearMessages()
        
        // Then
        state = viewModel.uiState.value
        assertNull(state.errorMessage)
        assertNull(state.successMessage)
        assertNull(state.emailVerificationError)
    }
}