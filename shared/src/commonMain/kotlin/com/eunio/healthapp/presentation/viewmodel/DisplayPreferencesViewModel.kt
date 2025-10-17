package com.eunio.healthapp.presentation.viewmodel

import com.eunio.healthapp.domain.model.settings.DisplayPreferences
import com.eunio.healthapp.domain.model.settings.HapticIntensity
import com.eunio.healthapp.domain.usecase.settings.GetDisplayPreferencesUseCase
import com.eunio.healthapp.domain.usecase.settings.UpdateDisplayPreferencesUseCase
import com.eunio.healthapp.platform.haptic.HapticFeedbackManager
import com.eunio.healthapp.platform.accessibility.AccessibilityManager
import com.eunio.healthapp.presentation.state.AccessibilityInfo
import com.eunio.healthapp.presentation.state.DisplayPreferencesUiState
import com.eunio.healthapp.presentation.state.LoadingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * ViewModel for managing display preferences.
 */
class DisplayPreferencesViewModel(
    private val getDisplayPreferencesUseCase: GetDisplayPreferencesUseCase,
    private val updateDisplayPreferencesUseCase: UpdateDisplayPreferencesUseCase,
    private val hapticFeedbackManager: HapticFeedbackManager,
    private val accessibilityManager: AccessibilityManager
) : BaseViewModel<DisplayPreferencesUiState>() {
    
    override val initialState = DisplayPreferencesUiState()
    
    init {
        loadDisplayPreferences()
        loadAccessibilityInfo()
    }
    
    /**
     * Loads display preferences from the repository.
     */
    fun loadDisplayPreferences() {
        viewModelScope.launch {
            updateState { it.copy(loadingState = LoadingState.Loading) }
            
            val result = getDisplayPreferencesUseCase()
            when (result) {
                is com.eunio.healthapp.domain.util.Result.Success -> {
                    updateState { 
                        it.copy(
                            preferences = result.data,
                            loadingState = LoadingState.Success(result.data),
                            validationErrors = result.data.getValidationErrors()
                        )
                    }
                }
                is com.eunio.healthapp.domain.util.Result.Error -> {
                    updateState { 
                        it.copy(
                            loadingState = LoadingState.Error(result.error.message ?: "Unknown error")
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Updates the text size scale.
     */
    fun updateTextSizeScale(scale: Float) {
        val clampedScale = scale.coerceIn(
            DisplayPreferences.MIN_TEXT_SCALE,
            DisplayPreferences.MAX_TEXT_SCALE
        )
        
        val updatedPreferences = uiState.value.preferences.copy(
            textSizeScale = clampedScale
        )
        
        updatePreferences(updatedPreferences)
    }
    
    /**
     * Toggles high contrast mode.
     */
    fun toggleHighContrastMode() {
        val currentPreferences = uiState.value.preferences
        val updatedPreferences = currentPreferences.copy(
            highContrastMode = !currentPreferences.highContrastMode
        )
        
        updatePreferences(updatedPreferences)
        
        // Provide haptic feedback for the toggle
        if (currentPreferences.hapticFeedbackEnabled) {
            viewModelScope.launch {
                hapticFeedbackManager.performHapticFeedback(currentPreferences.hapticIntensity)
            }
        }
    }
    
    /**
     * Toggles haptic feedback.
     */
    fun toggleHapticFeedback() {
        val currentPreferences = uiState.value.preferences
        val updatedPreferences = currentPreferences.withHapticFeedback(
            enabled = !currentPreferences.hapticFeedbackEnabled,
            intensity = if (!currentPreferences.hapticFeedbackEnabled) {
                HapticIntensity.MEDIUM
            } else {
                currentPreferences.hapticIntensity
            }
        )
        
        updatePreferences(updatedPreferences)
        
        // Test haptic feedback if being enabled
        if (updatedPreferences.hapticFeedbackEnabled) {
            testHapticFeedback(updatedPreferences.hapticIntensity)
        }
    }
    
    /**
     * Updates haptic intensity.
     */
    fun updateHapticIntensity(intensity: HapticIntensity) {
        val updatedPreferences = uiState.value.preferences.copy(
            hapticIntensity = intensity,
            hapticFeedbackEnabled = intensity != HapticIntensity.DISABLED
        )
        
        updatePreferences(updatedPreferences)
        
        // Test the new intensity
        if (intensity != HapticIntensity.DISABLED) {
            testHapticFeedback(intensity)
        }
    }
    
    /**
     * Tests haptic feedback with the specified intensity.
     */
    fun testHapticFeedback(intensity: HapticIntensity = uiState.value.preferences.hapticIntensity) {
        if (intensity == HapticIntensity.DISABLED) return
        
        viewModelScope.launch {
            updateState { 
                it.copy(
                    isTesting = true,
                    testingHapticIntensity = intensity
                )
            }
            
            try {
                hapticFeedbackManager.performHapticFeedback(intensity)
                delay(100) // Brief delay to show testing state
            } catch (e: Exception) {
                // Handle haptic feedback error
                handleError(e)
            } finally {
                updateState { 
                    it.copy(
                        isTesting = false,
                        testingHapticIntensity = null
                    )
                }
            }
        }
    }
    
    /**
     * Resets preferences to default values.
     */
    fun resetToDefaults() {
        updatePreferences(DisplayPreferences.default())
    }
    
    /**
     * Applies accessibility-optimized settings.
     */
    fun applyAccessibilityOptimizedSettings() {
        updatePreferences(DisplayPreferences.accessibilityOptimized())
        
        // Provide feedback
        viewModelScope.launch {
            delay(100)
            hapticFeedbackManager.performHapticFeedback(HapticIntensity.STRONG)
        }
    }
    
    /**
     * Applies minimal settings (reduced visual effects).
     */
    fun applyMinimalSettings() {
        updatePreferences(DisplayPreferences.minimal())
    }
    
    /**
     * Updates the preview text for text size demonstration.
     */
    fun updatePreviewText(text: String) {
        updateState { it.copy(previewText = text) }
    }
    
    /**
     * Loads accessibility information from the system.
     */
    private fun loadAccessibilityInfo() {
        viewModelScope.launch {
            try {
                val accessibilityInfo = AccessibilityInfo(
                    screenReaderEnabled = accessibilityManager.isScreenReaderEnabled(),
                    voiceOverEnabled = accessibilityManager.isVoiceOverEnabled(),
                    talkBackEnabled = accessibilityManager.isTalkBackEnabled(),
                    systemTextSizeScale = accessibilityManager.getSystemTextSizeScale(),
                    systemHighContrastEnabled = accessibilityManager.isSystemHighContrastEnabled(),
                    systemReduceMotionEnabled = accessibilityManager.isReduceMotionEnabled()
                )
                
                updateState { it.copy(accessibilityInfo = accessibilityInfo) }
            } catch (e: Exception) {
                // Handle accessibility info loading error
                handleError(e)
            }
        }
    }
    
    /**
     * Updates preferences and saves them.
     */
    private fun updatePreferences(preferences: DisplayPreferences) {
        viewModelScope.launch {
            updateState { 
                it.copy(
                    isUpdating = true,
                    validationErrors = preferences.getValidationErrors()
                )
            }
            
            if (preferences.isValid()) {
                val result = updateDisplayPreferencesUseCase(preferences)
                when (result) {
                    is com.eunio.healthapp.domain.util.Result.Success -> {
                        updateState { 
                            it.copy(
                                preferences = preferences,
                                isUpdating = false,
                                validationErrors = emptyList()
                            )
                        }
                    }
                    is com.eunio.healthapp.domain.util.Result.Error -> {
                        updateState { it.copy(isUpdating = false) }
                        handleError(Exception(result.error.message ?: "Unknown error"))
                    }
                }
            } else {
                updateState { it.copy(isUpdating = false) }
            }
        }
    }
    
    /**
     * Handles errors and updates UI state accordingly.
     */
    private fun handleError(error: Throwable) {
        val errorMessage = error.message ?: "An unexpected error occurred"
        updateState { 
            it.copy(loadingState = LoadingState.Error(errorMessage))
        }
    }
    
    /**
     * Clears any error state.
     */
    fun clearError() {
        if (uiState.value.loadingState is LoadingState.Error) {
            updateState { 
                it.copy(loadingState = LoadingState.Idle)
            }
        }
    }
    
    /**
     * Gets text size scale options for the slider.
     */
    fun getTextSizeScaleOptions(): List<Float> {
        return listOf(0.8f, 0.9f, 1.0f, 1.1f, 1.2f, 1.3f, 1.5f, 1.7f, 2.0f)
    }
    
    /**
     * Gets haptic intensity options.
     */
    fun getHapticIntensityOptions(): List<HapticIntensity> {
        return HapticIntensity.values().toList()
    }
}