package com.eunio.healthapp.presentation.viewmodel

import com.eunio.healthapp.domain.model.HealthGoal
import com.eunio.healthapp.domain.usecase.auth.CompleteOnboardingUseCase
import com.eunio.healthapp.domain.usecase.auth.GetCurrentUserUseCase
import com.eunio.healthapp.presentation.navigation.NavigationDestination
import com.eunio.healthapp.presentation.navigation.NavigationEvent
import com.eunio.healthapp.presentation.state.OnboardingStep
import com.eunio.healthapp.presentation.state.OnboardingUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing onboarding flow.
 */
class OnboardingViewModel(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val completeOnboardingUseCase: CompleteOnboardingUseCase,
    dispatcher: kotlinx.coroutines.CoroutineDispatcher = kotlinx.coroutines.Dispatchers.Main
) : BaseViewModel<OnboardingUiState>(dispatcher) {
    
    override val initialState = OnboardingUiState()
    
    private val _navigationEvents = MutableSharedFlow<NavigationEvent>()
    val navigationEvents: SharedFlow<NavigationEvent> = _navigationEvents.asSharedFlow()
    
    init {
        checkOnboardingStatus()
    }
    
    /**
     * Checks if user has already completed onboarding.
     */
    private fun checkOnboardingStatus() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }
            
            getCurrentUserUseCase()
                .onSuccess { user ->
                    if (user?.onboardingComplete == true) {
                        updateState { it.copy(isCompleted = true, isLoading = false) }
                        _navigationEvents.emit(
                            NavigationEvent.NavigateToWithClearStack(NavigationDestination.Calendar)
                        )
                    } else {
                        updateState { it.copy(isLoading = false) }
                    }
                }
                .onError { error ->
                    updateState { 
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed to check onboarding status: ${error.message}"
                        )
                    }
                }
        }
    }
    
    /**
     * Proceeds to the next onboarding step.
     */
    fun nextStep() {
        val currentStep = uiState.value.currentStep
        val nextStep = when (currentStep) {
            OnboardingStep.WELCOME -> OnboardingStep.GOAL_SELECTION
            OnboardingStep.GOAL_SELECTION -> OnboardingStep.COMPLETION
            OnboardingStep.COMPLETION -> return
        }
        
        updateState { it.copy(currentStep = nextStep, errorMessage = null) }
    }
    
    /**
     * Goes back to the previous onboarding step.
     */
    fun previousStep() {
        val currentStep = uiState.value.currentStep
        val previousStep = when (currentStep) {
            OnboardingStep.WELCOME -> return
            OnboardingStep.GOAL_SELECTION -> OnboardingStep.WELCOME
            OnboardingStep.COMPLETION -> OnboardingStep.GOAL_SELECTION
        }
        
        updateState { it.copy(currentStep = previousStep, errorMessage = null) }
    }
    
    /**
     * Selects a health goal.
     */
    fun selectGoal(goal: HealthGoal) {
        updateState { it.copy(selectedGoal = goal, errorMessage = null) }
    }
    
    /**
     * Completes the onboarding process.
     */
    fun completeOnboarding() {
        val selectedGoal = uiState.value.selectedGoal
        if (selectedGoal == null) {
            updateState { it.copy(errorMessage = "Please select a health goal") }
            return
        }
        
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            
            completeOnboardingUseCase("current_user", selectedGoal)
                .onSuccess { user ->
                    updateState { 
                        it.copy(
                            isLoading = false,
                            isCompleted = true,
                            currentStep = OnboardingStep.COMPLETION
                        )
                    }
                    
                    // Navigate to main app after a brief delay
                    kotlinx.coroutines.delay(1500)
                    _navigationEvents.emit(
                        NavigationEvent.NavigateToWithClearStack(NavigationDestination.Calendar)
                    )
                }
                .onError { error ->
                    updateState { 
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed to complete onboarding: ${error.message}"
                        )
                    }
                }
        }
    }
    
    /**
     * Clears any error messages.
     */
    fun clearError() {
        updateState { it.copy(errorMessage = null) }
    }
}