package com.eunio.healthapp.android.ui.onboarding

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eunio.healthapp.android.ui.theme.EunioTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for the onboarding flow.
 */
@RunWith(AndroidJUnit4::class)
class OnboardingFlowTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun onboardingFlow_startsWithAuthenticationScreen() {
        composeTestRule.setContent {
            EunioTheme {
                OnboardingFlow()
            }
        }
        
        // Should show authentication screen initially
        composeTestRule.onNodeWithText("Welcome Back").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sign in to continue your health journey").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sign In").assertIsDisplayed()
    }
    
    @Test
    fun authenticationScreen_canToggleBetweenSignInAndSignUp() {
        composeTestRule.setContent {
            EunioTheme {
                AuthenticationScreen(
                    onSignInSuccess = {},
                    onSignUpSuccess = {}
                )
            }
        }
        
        // Initially shows sign in
        composeTestRule.onNodeWithText("Welcome Back").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sign In").assertIsDisplayed()
        
        // Toggle to sign up
        composeTestRule.onNodeWithText("Sign Up").performClick()
        
        // Should show sign up screen
        composeTestRule.onNodeWithText("Create Account").assertIsDisplayed()
        composeTestRule.onNodeWithText("Full Name").assertIsDisplayed()
        composeTestRule.onNodeWithText("Confirm Password").assertIsDisplayed()
        
        // Toggle back to sign in
        composeTestRule.onNodeWithText("Sign In").performClick()
        
        // Should show sign in screen again
        composeTestRule.onNodeWithText("Welcome Back").assertIsDisplayed()
    }
    
    @Test
    fun authenticationScreen_showsValidationErrors() {
        composeTestRule.setContent {
            EunioTheme {
                AuthenticationScreen(
                    onSignInSuccess = {},
                    onSignUpSuccess = {}
                )
            }
        }
        
        // Try to sign in without filling fields
        composeTestRule.onNodeWithText("Sign In").performClick()
        
        // Should show validation error
        composeTestRule.onNodeWithText("Please fill in all fields").assertIsDisplayed()
    }
    
    @Test
    fun authenticationScreen_signUpValidatesPasswordMatch() {
        composeTestRule.setContent {
            EunioTheme {
                AuthenticationScreen(
                    onSignInSuccess = {},
                    onSignUpSuccess = {}
                )
            }
        }
        
        // Switch to sign up
        composeTestRule.onNodeWithText("Sign Up").performClick()
        
        // Fill in fields with mismatched passwords
        composeTestRule.onNodeWithText("Full Name").performTextInput("Test User")
        composeTestRule.onNodeWithText("Email").performTextInput("test@example.com")
        composeTestRule.onNodeWithText("Password").performTextInput("password123")
        composeTestRule.onNodeWithText("Confirm Password").performTextInput("different")
        
        // Try to create account
        composeTestRule.onNodeWithText("Create Account").performClick()
        
        // Should show password mismatch error
        composeTestRule.onNodeWithText("Passwords do not match").assertIsDisplayed()
    }
}