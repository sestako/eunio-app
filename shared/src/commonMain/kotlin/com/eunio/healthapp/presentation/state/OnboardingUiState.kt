package com.eunio.healthapp.presentation.state

import com.eunio.healthapp.domain.model.HealthGoal
import com.eunio.healthapp.presentation.viewmodel.UiState

/**
 * UI state for onboarding screens.
 */
data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.WELCOME,
    val selectedGoal: HealthGoal? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isCompleted: Boolean = false
) : UiState

enum class OnboardingStep {
    WELCOME,
    GOAL_SELECTION,
    COMPLETION
}