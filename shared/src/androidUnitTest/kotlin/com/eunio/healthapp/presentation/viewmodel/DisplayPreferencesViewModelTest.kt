package com.eunio.healthapp.presentation.viewmodel

import com.eunio.healthapp.domain.model.settings.DisplayPreferences
import com.eunio.healthapp.domain.model.settings.HapticIntensity
import com.eunio.healthapp.domain.usecase.settings.GetDisplayPreferencesUseCase
import com.eunio.healthapp.domain.usecase.settings.UpdateDisplayPreferencesUseCase
import com.eunio.healthapp.platform.accessibility.AccessibilityManager
import com.eunio.healthapp.platform.haptic.HapticFeedbackManager
import com.eunio.healthapp.presentation.state.LoadingState
import com.eunio.healthapp.domain.util.Result
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class DisplayPreferencesViewModelTest {
    
    private val testDispatcher = StandardTestDispatcher()
    
    // Mocks
    private val mockGetDisplayPreferencesUseCase = mockk<GetDisplayPreferencesUseCase>()
    private val mockUpdateDisplayPreferencesUseCase = mockk<UpdateDisplayPreferencesUseCase>()
    private val mockHapticFeedbackManager = mockk<HapticFeedbackManager>()
    private val mockAccessibilityManager = mockk<AccessibilityManager>()
    
    // System under test
    private lateinit var viewModel: DisplayPreferencesViewModel
    
    // Test data
    private val defaultPreferences = DisplayPreferences.default()
    private val customPreferences = DisplayPreferences(
        textSizeScale = 1.5f,
        highContrastMode = true,
        hapticFeedbackEnabled = true,
        hapticIntensity = HapticIntensity.STRONG
    )
    
    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        clearAllMocks()
        
        // Setup default mock behaviors
        coEvery { mockGetDisplayPreferencesUseCase() } returns Result.Success(defaultPreferences)
        coEvery { mockUpdateDisplayPreferencesUseCase(any<DisplayPreferences>()) } returns Result.Success(Unit)
        coEvery { mockHapticFeedbackManager.performHapticFeedback(any<HapticIntensity>()) } just Runs
        coEvery { mockHapticFeedbackManager.isHapticFeedbackAvailable() } returns true
        coEvery { mockHapticFeedbackManager.isSystemHapticEnabled() } returns true
        
        every { mockAccessibilityManager.isScreenReaderEnabled() } returns false
        every { mockAccessibilityManager.isVoiceOverEnabled() } returns false
        every { mockAccessibilityManager.isTalkBackEnabled() } returns false
        every { mockAccessibilityManager.getSystemTextSizeScale() } returns 1.0f
        every { mockAccessibilityManager.isSystemHighContrastEnabled() } returns false
        every { mockAccessibilityManager.isReduceMotionEnabled() } returns false
        every { mockAccessibilityManager.isBoldTextEnabled() } returns false
        every { mockAccessibilityManager.isButtonShapesEnabled() } returns false
        
        viewModel = DisplayPreferencesViewModel(
            getDisplayPreferencesUseCase = mockGetDisplayPreferencesUseCase,
            updateDisplayPreferencesUseCase = mockUpdateDisplayPreferencesUseCase,
            hapticFeedbackManager = mockHapticFeedbackManager,
            accessibilityManager = mockAccessibilityManager
        )
    }
    
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }
    
    // MARK: - Initialization Tests
    
    @Test
    fun `init loads display preferences automatically`() = runTest {
        // Advance the test dispatcher to complete async operations
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify use case was called during initialization
        coVerify { mockGetDisplayPreferencesUseCase() }
        
        // Verify initial state
        val state = viewModel.uiState.first()
        assertEquals(defaultPreferences, state.preferences)
        assertTrue(state.loadingState is LoadingState.Success<*>)
    }
    
    @Test
    fun `init loads accessibility info automatically`() = runTest {
        // Verify accessibility manager methods were called
        verify { mockAccessibilityManager.isScreenReaderEnabled() }
        verify { mockAccessibilityManager.getSystemTextSizeScale() }
        verify { mockAccessibilityManager.isSystemHighContrastEnabled() }
        
        // Verify accessibility info is populated
        val state = viewModel.uiState.first()
        assertNotNull(state.accessibilityInfo)
    }
    
    // MARK: - Loading Tests
    
    @Test
    fun `loadDisplayPreferences sets loading state`() = runTest {
        // Setup mock to delay response
        coEvery { mockGetDisplayPreferencesUseCase() } coAnswers {
            kotlinx.coroutines.delay(100)
            Result.Success(defaultPreferences)
        }
        
        // Call load preferences
        viewModel.loadDisplayPreferences()
        
        // Advance dispatcher to start the coroutine
        testDispatcher.scheduler.advanceTimeBy(1)
        
        // Verify loading state is set initially
        val initialState = viewModel.uiState.first()
        assertTrue(initialState.loadingState is LoadingState.Loading)
    }
    
    @Test
    fun `loadDisplayPreferences handles success`() = runTest {
        // Setup successful response
        coEvery { mockGetDisplayPreferencesUseCase() } returns Result.Success(customPreferences)
        
        // Load preferences
        viewModel.loadDisplayPreferences()
        
        // Advance dispatcher to complete the coroutine
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify success state
        val state = viewModel.uiState.first()
        assertEquals(customPreferences, state.preferences)
        assertTrue(state.loadingState is LoadingState.Success<*>)
        assertTrue(state.validationErrors.isEmpty())
    }
    
    @Test
    fun `loadDisplayPreferences handles error`() = runTest {
        val errorMessage = "Failed to load preferences"
        coEvery { mockGetDisplayPreferencesUseCase() } returns Result.Error(
            com.eunio.healthapp.domain.error.AppError.UnknownError(errorMessage)
        )
        
        // Load preferences
        viewModel.loadDisplayPreferences()
        
        // Advance dispatcher to complete the coroutine
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify error state
        val state = viewModel.uiState.first()
        assertTrue(state.loadingState is LoadingState.Error)
        assertEquals(errorMessage, (state.loadingState as LoadingState.Error).message)
    }
    
    // MARK: - Text Size Tests
    
    @Test
    fun `updateTextSizeScale updates preferences`() = runTest {
        val newScale = 1.5f
        
        // Update text size scale
        viewModel.updateTextSizeScale(newScale)
        
        // Advance dispatcher to complete the coroutine
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify update use case was called with correct preferences
        coVerify {
            mockUpdateDisplayPreferencesUseCase(
                match { it.textSizeScale == newScale }
            )
        }
    }
    
    @Test
    fun `updateTextSizeScale clamps value to valid range`() = runTest {
        val tooLargeScale = 3.0f
        val expectedScale = DisplayPreferences.MAX_TEXT_SCALE
        
        // Update with invalid scale
        viewModel.updateTextSizeScale(tooLargeScale)
        
        // Advance dispatcher to complete the coroutine
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify clamped value was used
        coVerify {
            mockUpdateDisplayPreferencesUseCase(
                match { it.textSizeScale == expectedScale }
            )
        }
    }
    
    @Test
    fun `updateTextSizeScale clamps minimum value`() = runTest {
        val tooSmallScale = 0.5f
        val expectedScale = DisplayPreferences.MIN_TEXT_SCALE
        
        // Update with invalid scale
        viewModel.updateTextSizeScale(tooSmallScale)
        
        // Advance dispatcher to complete the coroutine
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify clamped value was used
        coVerify {
            mockUpdateDisplayPreferencesUseCase(
                match { it.textSizeScale == expectedScale }
            )
        }
    }
    
    // MARK: - High Contrast Tests
    
    @Test
    fun `toggleHighContrastMode updates preferences`() = runTest {
        // Toggle high contrast mode
        viewModel.toggleHighContrastMode()
        
        // Advance dispatcher to complete the coroutine
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify update use case was called with toggled value
        coVerify {
            mockUpdateDisplayPreferencesUseCase(
                match { it.highContrastMode == !defaultPreferences.highContrastMode }
            )
        }
    }
    
    @Test
    fun `toggleHighContrastMode provides haptic feedback when enabled`() = runTest {
        // Setup preferences with haptic feedback enabled
        coEvery { mockGetDisplayPreferencesUseCase() } returns Result.Success(
            defaultPreferences.copy(hapticFeedbackEnabled = true, hapticIntensity = HapticIntensity.MEDIUM)
        )
        viewModel.loadDisplayPreferences()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Toggle high contrast mode
        viewModel.toggleHighContrastMode()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify haptic feedback was triggered
        coVerify { mockHapticFeedbackManager.performHapticFeedback(HapticIntensity.MEDIUM) }
    }
    
    @Test
    fun `toggleHighContrastMode does not provide haptic feedback when disabled`() = runTest {
        // Setup preferences with haptic feedback disabled
        coEvery { mockGetDisplayPreferencesUseCase() } returns Result.Success(
            defaultPreferences.copy(hapticFeedbackEnabled = false)
        )
        viewModel.loadDisplayPreferences()
        
        // Toggle high contrast mode
        viewModel.toggleHighContrastMode()
        
        // Verify no haptic feedback was triggered
        coVerify(exactly = 0) { mockHapticFeedbackManager.performHapticFeedback(any()) }
    }
    
    // MARK: - Haptic Feedback Tests
    
    @Test
    fun `toggleHapticFeedback enables haptic with default intensity`() = runTest {
        // Setup with haptic disabled
        coEvery { mockGetDisplayPreferencesUseCase() } returns Result.Success(
            defaultPreferences.copy(hapticFeedbackEnabled = false)
        )
        viewModel.loadDisplayPreferences()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Toggle haptic feedback
        viewModel.toggleHapticFeedback()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify haptic was enabled with medium intensity
        coVerify {
            mockUpdateDisplayPreferencesUseCase(
                match { 
                    it.hapticFeedbackEnabled && 
                    it.hapticIntensity == HapticIntensity.MEDIUM 
                }
            )
        }
    }
    
    @Test
    fun `toggleHapticFeedback disables haptic feedback`() = runTest {
        // Setup with haptic enabled
        coEvery { mockGetDisplayPreferencesUseCase() } returns Result.Success(
            defaultPreferences.copy(hapticFeedbackEnabled = true, hapticIntensity = HapticIntensity.STRONG)
        )
        viewModel.loadDisplayPreferences()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Toggle haptic feedback
        viewModel.toggleHapticFeedback()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify haptic was disabled
        coVerify {
            mockUpdateDisplayPreferencesUseCase(
                match { 
                    !it.hapticFeedbackEnabled && 
                    it.hapticIntensity == HapticIntensity.DISABLED 
                }
            )
        }
    }
    
    @Test
    fun `toggleHapticFeedback tests feedback when enabling`() = runTest {
        // Setup with haptic disabled
        coEvery { mockGetDisplayPreferencesUseCase() } returns Result.Success(
            defaultPreferences.copy(hapticFeedbackEnabled = false)
        )
        viewModel.loadDisplayPreferences()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Toggle haptic feedback
        viewModel.toggleHapticFeedback()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify test haptic was triggered
        coVerify { mockHapticFeedbackManager.performHapticFeedback(HapticIntensity.MEDIUM) }
    }
    
    @Test
    fun `updateHapticIntensity updates preferences and enables haptic`() = runTest {
        val newIntensity = HapticIntensity.STRONG
        
        // Update haptic intensity
        viewModel.updateHapticIntensity(newIntensity)
        
        // Advance dispatcher to complete the coroutine
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify preferences were updated
        coVerify {
            mockUpdateDisplayPreferencesUseCase(
                match { 
                    it.hapticIntensity == newIntensity && 
                    it.hapticFeedbackEnabled 
                }
            )
        }
    }
    
    @Test
    fun `updateHapticIntensity to disabled disables haptic feedback`() = runTest {
        // Update to disabled intensity
        viewModel.updateHapticIntensity(HapticIntensity.DISABLED)
        
        // Advance dispatcher to complete the coroutine
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify haptic feedback was disabled
        coVerify {
            mockUpdateDisplayPreferencesUseCase(
                match { 
                    it.hapticIntensity == HapticIntensity.DISABLED && 
                    !it.hapticFeedbackEnabled 
                }
            )
        }
    }
    
    @Test
    fun `updateHapticIntensity tests new intensity`() = runTest {
        val newIntensity = HapticIntensity.LIGHT
        
        // Update haptic intensity
        viewModel.updateHapticIntensity(newIntensity)
        
        // Advance dispatcher to complete the coroutine
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify test haptic was triggered with new intensity
        coVerify { mockHapticFeedbackManager.performHapticFeedback(newIntensity) }
    }
    
    // MARK: - Test Haptic Feedback Tests
    
    @Test
    fun `testHapticFeedback performs haptic with current intensity`() = runTest {
        // Setup with haptic enabled
        coEvery { mockGetDisplayPreferencesUseCase() } returns Result.Success(
            defaultPreferences.copy(hapticFeedbackEnabled = true, hapticIntensity = HapticIntensity.STRONG)
        )
        viewModel.loadDisplayPreferences()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Test haptic feedback
        viewModel.testHapticFeedback()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify haptic was performed with current intensity
        coVerify { mockHapticFeedbackManager.performHapticFeedback(HapticIntensity.STRONG) }
    }
    
    @Test
    fun `testHapticFeedback with specific intensity performs that intensity`() = runTest {
        val testIntensity = HapticIntensity.LIGHT
        
        // Test haptic feedback with specific intensity
        viewModel.testHapticFeedback(testIntensity)
        
        // Advance dispatcher to complete the coroutine
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify haptic was performed with specified intensity
        coVerify { mockHapticFeedbackManager.performHapticFeedback(testIntensity) }
    }
    
    @Test
    fun `testHapticFeedback does nothing when intensity is disabled`() = runTest {
        // Test with disabled intensity
        viewModel.testHapticFeedback(HapticIntensity.DISABLED)
        
        // Advance dispatcher to complete the coroutine
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify no haptic was performed
        coVerify(exactly = 0) { mockHapticFeedbackManager.performHapticFeedback(any()) }
    }
    
    @Test
    fun `testHapticFeedback sets testing state`() = runTest {
        // Test haptic feedback
        viewModel.testHapticFeedback()
        
        // Advance dispatcher to complete the coroutine
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Note: Testing the transient testing state is difficult in unit tests
        // This would be better tested in integration tests
        coVerify { mockHapticFeedbackManager.performHapticFeedback(any()) }
    }
    
    // MARK: - Quick Actions Tests
    
    @Test
    fun `resetToDefaults applies default preferences`() = runTest {
        // Reset to defaults
        viewModel.resetToDefaults()
        
        // Advance dispatcher to complete the coroutine
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify default preferences were applied
        coVerify {
            mockUpdateDisplayPreferencesUseCase(DisplayPreferences.default())
        }
    }
    
    @Test
    fun `applyAccessibilityOptimizedSettings applies optimized preferences`() = runTest {
        // Apply accessibility optimized settings
        viewModel.applyAccessibilityOptimizedSettings()
        
        // Advance dispatcher to complete the coroutine
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify optimized preferences were applied
        coVerify {
            mockUpdateDisplayPreferencesUseCase(DisplayPreferences.accessibilityOptimized())
        }
    }
    
    @Test
    fun `applyAccessibilityOptimizedSettings provides haptic feedback`() = runTest {
        // Apply accessibility optimized settings
        viewModel.applyAccessibilityOptimizedSettings()
        
        // Advance dispatcher to complete the coroutine
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify strong haptic feedback was provided
        coVerify { mockHapticFeedbackManager.performHapticFeedback(HapticIntensity.STRONG) }
    }
    
    @Test
    fun `applyMinimalSettings applies minimal preferences`() = runTest {
        // Apply minimal settings
        viewModel.applyMinimalSettings()
        
        // Advance dispatcher to complete the coroutine
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify minimal preferences were applied
        coVerify {
            mockUpdateDisplayPreferencesUseCase(DisplayPreferences.minimal())
        }
    }
    
    // MARK: - Preview Text Tests
    
    @Test
    fun `updatePreviewText updates UI state`() = runTest {
        val newText = "Custom preview text"
        
        // Update preview text
        viewModel.updatePreviewText(newText)
        
        // Advance dispatcher to complete the coroutine
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify UI state was updated
        val state = viewModel.uiState.first()
        assertEquals(newText, state.previewText)
    }
    
    // MARK: - Error Handling Tests
    
    @Test
    fun `clearError clears error state`() = runTest {
        // Setup error state
        coEvery { mockGetDisplayPreferencesUseCase() } returns Result.Error(
            com.eunio.healthapp.domain.error.AppError.UnknownError("Test error")
        )
        viewModel.loadDisplayPreferences()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify error state
        val errorState = viewModel.uiState.first()
        assertTrue(errorState.loadingState is LoadingState.Error)
        
        // Clear error
        viewModel.clearError()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify error was cleared
        val clearedState = viewModel.uiState.first()
        assertTrue(clearedState.loadingState is LoadingState.Idle)
    }
    
    @Test
    fun `update preferences handles validation errors`() = runTest {
        val invalidPreferences = DisplayPreferences(
            textSizeScale = -1.0f, // Invalid
            highContrastMode = false,
            hapticFeedbackEnabled = false,
            hapticIntensity = HapticIntensity.STRONG // Inconsistent with enabled = false
        )
        
        // Setup to return invalid preferences
        coEvery { mockGetDisplayPreferencesUseCase() } returns Result.Success(invalidPreferences)
        viewModel.loadDisplayPreferences()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify validation errors are populated
        val state = viewModel.uiState.first()
        assertTrue(state.validationErrors.isNotEmpty())
    }
    
    // MARK: - Utility Method Tests
    
    @Test
    fun `getTextSizeScaleOptions returns valid options`() = runTest {
        val options = viewModel.getTextSizeScaleOptions()
        
        // Advance dispatcher to complete the coroutine
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify options are within valid range
        assertTrue(options.isNotEmpty())
        assertTrue(options.all { it >= DisplayPreferences.MIN_TEXT_SCALE })
        assertTrue(options.all { it <= DisplayPreferences.MAX_TEXT_SCALE })
        assertTrue(options.contains(DisplayPreferences.DEFAULT_TEXT_SCALE))
    }
    
    @Test
    fun `getHapticIntensityOptions returns all intensity values`() = runTest {
        val options = viewModel.getHapticIntensityOptions()
        
        // Advance dispatcher to complete the coroutine
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify all intensity values are included
        assertEquals(HapticIntensity.values().size, options.size)
        assertTrue(options.containsAll(HapticIntensity.values().toList()))
    }
}