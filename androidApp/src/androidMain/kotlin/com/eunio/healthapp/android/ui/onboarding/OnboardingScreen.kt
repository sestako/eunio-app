package com.eunio.healthapp.android.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eunio.healthapp.presentation.state.OnboardingStep
import com.eunio.healthapp.presentation.viewmodel.OnboardingViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * Main onboarding screen that manages the onboarding flow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onNavigateToMain: () -> Unit,
    viewModel: OnboardingViewModel? = null
) {
    // For now, create a simple state management without ViewModel
    var currentStep by remember { mutableStateOf(com.eunio.healthapp.presentation.state.OnboardingStep.WELCOME) }
    var selectedGoal by remember { mutableStateOf<com.eunio.healthapp.domain.model.HealthGoal?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isCompleted by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (currentStep) {
            OnboardingStep.WELCOME -> {
                WelcomeScreen(
                    onNext = { 
                        currentStep = OnboardingStep.GOAL_SELECTION
                    },
                    isLoading = isLoading
                )
            }
            OnboardingStep.GOAL_SELECTION -> {
                GoalSelectionScreen(
                    selectedGoal = selectedGoal,
                    onGoalSelected = { goal -> 
                        selectedGoal = goal
                        errorMessage = null
                    },
                    onNext = { 
                        if (selectedGoal != null) {
                            currentStep = OnboardingStep.COMPLETION
                        } else {
                            errorMessage = "Please select a health goal"
                        }
                    },
                    onBack = { 
                        currentStep = OnboardingStep.WELCOME
                    },
                    isLoading = isLoading,
                    errorMessage = errorMessage
                )
            }
            OnboardingStep.COMPLETION -> {
                CompletionScreen(
                    onComplete = { 
                        isLoading = true
                        // Simulate completion
                        isCompleted = true
                        // Navigate to main app after delay
                        onNavigateToMain()
                    },
                    onBack = { 
                        currentStep = OnboardingStep.GOAL_SELECTION
                    },
                    isLoading = isLoading,
                    isCompleted = isCompleted
                )
            }
        }
    }
}