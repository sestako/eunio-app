package com.eunio.healthapp.android.ui.onboarding

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eunio.healthapp.android.ui.theme.EunioTheme
import com.eunio.healthapp.domain.model.HealthGoal
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for individual onboarding screens.
 */
@RunWith(AndroidJUnit4::class)
class OnboardingScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun welcomeScreen_displaysCorrectContent() {
        composeTestRule.setContent {
            EunioTheme {
                WelcomeScreen(
                    onNext = {},
                    isLoading = false
                )
            }
        }
        
        // Check main content
        composeTestRule.onNodeWithText("Welcome to Eunio").assertIsDisplayed()
        composeTestRule.onNodeWithText("Your personal health companion for understanding your body and tracking your wellness journey.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Get Started").assertIsDisplayed()
        
        // Check feature items
        composeTestRule.onNodeWithText("Smart Tracking").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cycle Prediction").assertIsDisplayed()
        composeTestRule.onNodeWithText("Privacy First").assertIsDisplayed()
    }
    
    @Test
    fun welcomeScreen_getStartedButtonWorks() {
        var nextCalled = false
        
        composeTestRule.setContent {
            EunioTheme {
                WelcomeScreen(
                    onNext = { nextCalled = true },
                    isLoading = false
                )
            }
        }
        
        composeTestRule.onNodeWithText("Get Started").performClick()
        assert(nextCalled)
    }
    
    @Test
    fun goalSelectionScreen_displaysAllGoalOptions() {
        composeTestRule.setContent {
            EunioTheme {
                GoalSelectionScreen(
                    selectedGoal = null,
                    onGoalSelected = {},
                    onNext = {},
                    onBack = {},
                    isLoading = false,
                    errorMessage = null
                )
            }
        }
        
        // Check title and description
        composeTestRule.onNodeWithText("What's your primary goal?").assertIsDisplayed()
        composeTestRule.onNodeWithText("This helps us personalize your experience and provide relevant insights.").assertIsDisplayed()
        
        // Check all goal options
        composeTestRule.onNodeWithText("Trying to Conceive").assertIsDisplayed()
        composeTestRule.onNodeWithText("Natural Contraception").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cycle Tracking").assertIsDisplayed()
        composeTestRule.onNodeWithText("General Health").assertIsDisplayed()
        
        // Continue button should be disabled initially
        composeTestRule.onNodeWithText("Continue").assertIsNotEnabled()
    }
    
    @Test
    fun goalSelectionScreen_canSelectGoal() {
        var selectedGoal: HealthGoal? = null
        
        composeTestRule.setContent {
            EunioTheme {
                GoalSelectionScreen(
                    selectedGoal = selectedGoal,
                    onGoalSelected = { selectedGoal = it },
                    onNext = {},
                    onBack = {},
                    isLoading = false,
                    errorMessage = null
                )
            }
        }
        
        // Select a goal
        composeTestRule.onNodeWithText("Cycle Tracking").performClick()
        
        // Verify goal was selected
        assert(selectedGoal == HealthGoal.CYCLE_TRACKING)
    }
    
    @Test
    fun goalSelectionScreen_continueButtonEnabledAfterSelection() {
        composeTestRule.setContent {
            EunioTheme {
                GoalSelectionScreen(
                    selectedGoal = HealthGoal.CYCLE_TRACKING,
                    onGoalSelected = {},
                    onNext = {},
                    onBack = {},
                    isLoading = false,
                    errorMessage = null
                )
            }
        }
        
        // Continue button should be enabled when goal is selected
        composeTestRule.onNodeWithText("Continue").assertIsEnabled()
    }
    
    @Test
    fun goalSelectionScreen_showsErrorMessage() {
        composeTestRule.setContent {
            EunioTheme {
                GoalSelectionScreen(
                    selectedGoal = null,
                    onGoalSelected = {},
                    onNext = {},
                    onBack = {},
                    isLoading = false,
                    errorMessage = "Please select a health goal"
                )
            }
        }
        
        composeTestRule.onNodeWithText("Please select a health goal").assertIsDisplayed()
    }
    
    @Test
    fun completionScreen_displaysCorrectContent() {
        composeTestRule.setContent {
            EunioTheme {
                CompletionScreen(
                    onComplete = {},
                    onBack = {},
                    isLoading = false,
                    isCompleted = false
                )
            }
        }
        
        // Check content
        composeTestRule.onNodeWithText("Ready to get started?").assertIsDisplayed()
        composeTestRule.onNodeWithText("What happens next:").assertIsDisplayed()
        composeTestRule.onNodeWithText("Complete Setup").assertIsDisplayed()
        
        // Check setup steps
        composeTestRule.onNodeWithText("Start logging").assertIsDisplayed()
        composeTestRule.onNodeWithText("Get insights").assertIsDisplayed()
        composeTestRule.onNodeWithText("Track progress").assertIsDisplayed()
    }
    
    @Test
    fun completionScreen_showsSuccessStateWhenCompleted() {
        composeTestRule.setContent {
            EunioTheme {
                CompletionScreen(
                    onComplete = {},
                    onBack = {},
                    isLoading = false,
                    isCompleted = true
                )
            }
        }
        
        // Should show success message
        composeTestRule.onNodeWithText("Welcome to Eunio!").assertIsDisplayed()
        composeTestRule.onNodeWithText("Your account is set up and ready to go. Start tracking your health journey today!").assertIsDisplayed()
        
        // Complete button should not be visible when completed
        composeTestRule.onNodeWithText("Complete Setup").assertDoesNotExist()
    }
}