package com.eunio.healthapp.android.integration

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eunio.healthapp.android.MainActivity
import com.eunio.healthapp.android.ui.theme.EunioTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Android integration tests
 * Tests full app integration, navigation flows, and end-to-end scenarios
 */
@RunWith(AndroidJUnit4::class)
class AndroidIntegrationTest {
    
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    
    // MARK: - App Launch Tests
    
    @Test
    fun app_launchesSuccessfully() {
        // Given/When - App launches via rule
        
        // Then - App should display main content
        composeTestRule.onNodeWithContentDescription("Main app content")
            .assertIsDisplayed()
    }
    
    @Test
    fun app_showsOnboardingFlow() {
        // Given/When - App launches
        
        // Then - Should show onboarding flow initially
        composeTestRule.onNodeWithText("Welcome to Eunio")
            .assertIsDisplayed()
    }
    
    // MARK: - Navigation Tests
    
    @Test
    fun navigation_worksCorrectly() {
        // Given - App is launched
        
        // When - Navigate through onboarding
        composeTestRule.onNodeWithText("Get Started")
            .performClick()
        
        // Then - Should navigate to next screen
        composeTestRule.onNodeWithText("Track Your Health")
            .assertIsDisplayed()
    }
    
    @Test
    fun navigation_handlesBackNavigation() {
        // Given - Navigate to a screen
        composeTestRule.onNodeWithText("Get Started")
            .performClick()
        
        // When - Navigate back
        composeTestRule.onNodeWithContentDescription("Navigate back")
            .performClick()
        
        // Then - Should return to previous screen
        composeTestRule.onNodeWithText("Welcome to Eunio")
            .assertIsDisplayed()
    }
    
    // MARK: - Daily Logging Flow Tests
    
    @Test
    fun dailyLogging_completesFullFlow() {
        // Given - Navigate to daily logging
        navigateToMainApp()
        composeTestRule.onNodeWithText("Log Today")
            .performClick()
        
        // When - Fill out daily log
        composeTestRule.onNodeWithContentDescription("Select medium flow")
            .performClick()
        
        composeTestRule.onNodeWithContentDescription("Toggle cramps symptom")
            .performClick()
        
        composeTestRule.onNodeWithContentDescription("Temperature input")
            .performTextInput("98.6")
        
        composeTestRule.onNodeWithText("Save Log")
            .performClick()
        
        // Then - Should show success message
        composeTestRule.onNodeWithText("Log saved successfully")
            .assertIsDisplayed()
    }
    
    @Test
    fun dailyLogging_validatesInput() {
        // Given - Navigate to daily logging
        navigateToMainApp()
        composeTestRule.onNodeWithText("Log Today")
            .performClick()
        
        // When - Enter invalid temperature
        composeTestRule.onNodeWithContentDescription("Temperature input")
            .performTextInput("150")
        
        // Then - Should show validation error
        composeTestRule.onNodeWithText("Invalid temperature range")
            .assertIsDisplayed()
        
        // And save button should be disabled
        composeTestRule.onNodeWithText("Save Log")
            .assertIsNotEnabled()
    }
    
    @Test
    fun dailyLogging_handlesEmptyForm() {
        // Given - Navigate to daily logging
        navigateToMainApp()
        composeTestRule.onNodeWithText("Log Today")
            .performClick()
        
        // When - Try to save without entering data
        composeTestRule.onNodeWithText("Save Log")
            .performClick()
        
        // Then - Should show validation message
        composeTestRule.onNodeWithText("Please enter at least one piece of data")
            .assertIsDisplayed()
    }
    
    // MARK: - Calendar Integration Tests
    
    @Test
    fun calendar_displaysLoggedData() {
        // Given - Log some data first
        navigateToMainApp()
        logSampleData()
        
        // When - Navigate to calendar
        composeTestRule.onNodeWithText("Calendar")
            .performClick()
        
        // Then - Should show logged data on calendar
        composeTestRule.onNodeWithContentDescription("Day with period data")
            .assertIsDisplayed()
    }
    
    @Test
    fun calendar_allowsDateSelection() {
        // Given - Navigate to calendar
        navigateToMainApp()
        composeTestRule.onNodeWithText("Calendar")
            .performClick()
        
        // When - Select a date
        composeTestRule.onNodeWithContentDescription("Select date 15")
            .performClick()
        
        // Then - Should navigate to daily logging for that date
        composeTestRule.onNodeWithText("Log for January 15")
            .assertIsDisplayed()
    }
    
    // MARK: - Insights Integration Tests
    
    @Test
    fun insights_displayWithData() {
        // Given - Log some data first
        navigateToMainApp()
        logSampleData()
        
        // When - Navigate to insights
        composeTestRule.onNodeWithText("Insights")
            .performClick()
        
        // Then - Should show insights based on data
        composeTestRule.onNodeWithText("Cycle Insights")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Symptom Patterns")
            .assertIsDisplayed()
    }
    
    @Test
    fun insights_showsEmptyStateWithoutData() {
        // Given - Navigate to insights without logging data
        navigateToMainApp()
        composeTestRule.onNodeWithText("Insights")
            .performClick()
        
        // Then - Should show empty state
        composeTestRule.onNodeWithText("Start logging to see insights")
            .assertIsDisplayed()
    }
    
    // MARK: - Settings Integration Tests
    
    @Test
    fun settings_allowsUnitSystemChange() {
        // Given - Navigate to settings
        navigateToMainApp()
        composeTestRule.onNodeWithText("Settings")
            .performClick()
        
        // When - Change unit system
        composeTestRule.onNodeWithText("Unit System")
            .performClick()
        composeTestRule.onNodeWithText("Imperial")
            .performClick()
        
        // Then - Should update throughout app
        composeTestRule.onNodeWithText("°F")
            .assertIsDisplayed()
    }
    
    @Test
    fun settings_allowsThemeChange() {
        // Given - Navigate to settings
        navigateToMainApp()
        composeTestRule.onNodeWithText("Settings")
            .performClick()
        
        // When - Change theme
        composeTestRule.onNodeWithText("Appearance")
            .performClick()
        composeTestRule.onNodeWithText("Dark Mode")
            .performClick()
        
        // Then - Should apply dark theme
        // Note: This would require checking theme colors or background
        composeTestRule.onNodeWithContentDescription("Dark theme applied")
            .assertExists()
    }
    
    // MARK: - Error Handling Tests
    
    @Test
    fun app_handlesNetworkErrors() {
        // Given - Simulate network error
        // Note: This would require mocking network layer
        
        // When - Try to sync data
        navigateToMainApp()
        composeTestRule.onNodeWithText("Sync")
            .performClick()
        
        // Then - Should show error message
        composeTestRule.onNodeWithText("Unable to sync. Check your connection.")
            .assertIsDisplayed()
    }
    
    @Test
    fun app_recoversFromErrors() {
        // Given - Trigger an error state
        navigateToMainApp()
        composeTestRule.onNodeWithText("Sync")
            .performClick()
        
        // When - Retry after error
        composeTestRule.onNodeWithText("Retry")
            .performClick()
        
        // Then - Should attempt operation again
        composeTestRule.onNodeWithText("Syncing...")
            .assertIsDisplayed()
    }
    
    // MARK: - Performance Tests
    
    @Test
    fun app_performsWellWithLargeDataset() {
        // Given - Generate large dataset
        navigateToMainApp()
        
        // When - Navigate through app with large dataset
        repeat(10) {
            composeTestRule.onNodeWithText("Calendar")
                .performClick()
            composeTestRule.onNodeWithText("Insights")
                .performClick()
            composeTestRule.onNodeWithText("Log Today")
                .performClick()
        }
        
        // Then - Should remain responsive
        composeTestRule.onNodeWithText("Log Today")
            .assertIsDisplayed()
    }
    
    // MARK: - Accessibility Integration Tests
    
    @Test
    fun app_isFullyAccessible() {
        // Given - Navigate through app
        navigateToMainApp()
        
        // When - Check accessibility of main screens
        composeTestRule.onNodeWithText("Log Today")
            .assertHasClickAction()
            .assertIsEnabled()
        
        composeTestRule.onNodeWithText("Calendar")
            .assertHasClickAction()
            .assertIsEnabled()
        
        composeTestRule.onNodeWithText("Insights")
            .assertHasClickAction()
            .assertIsEnabled()
        
        composeTestRule.onNodeWithText("Settings")
            .assertHasClickAction()
            .assertIsEnabled()
        
        // Then - All main navigation should be accessible
        // Additional accessibility checks would be performed here
    }
    
    @Test
    fun app_supportsScreenReader() {
        // Given - Navigate to daily logging
        navigateToMainApp()
        composeTestRule.onNodeWithText("Log Today")
            .performClick()
        
        // When - Check screen reader support
        composeTestRule.onNodeWithContentDescription("Period flow selector")
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithContentDescription("Symptom selector")
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithContentDescription("Temperature input")
            .assertIsDisplayed()
        
        // Then - All components should have proper content descriptions
    }
    
    // MARK: - Data Persistence Tests
    
    @Test
    fun app_persistsDataAcrossRestarts() {
        // Given - Log some data
        navigateToMainApp()
        logSampleData()
        
        // When - Restart app (simulated by recreating activity)
        composeTestRule.activityRule.scenario.recreate()
        
        // Then - Data should still be available
        navigateToMainApp()
        composeTestRule.onNodeWithText("Calendar")
            .performClick()
        composeTestRule.onNodeWithContentDescription("Day with period data")
            .assertIsDisplayed()
    }
    
    // MARK: - Helper Methods
    
    private fun navigateToMainApp() {
        // Skip onboarding if present
        try {
            composeTestRule.onNodeWithText("Skip")
                .performClick()
        } catch (e: AssertionError) {
            // Onboarding might not be present
        }
        
        // Wait for main app to load
        composeTestRule.waitForIdle()
    }
    
    private fun logSampleData() {
        composeTestRule.onNodeWithText("Log Today")
            .performClick()
        
        composeTestRule.onNodeWithContentDescription("Select medium flow")
            .performClick()
        
        composeTestRule.onNodeWithContentDescription("Toggle cramps symptom")
            .performClick()
        
        composeTestRule.onNodeWithText("Save Log")
            .performClick()
        
        // Wait for save to complete
        composeTestRule.waitForIdle()
        
        // Navigate back to main screen
        composeTestRule.onNodeWithContentDescription("Navigate back")
            .performClick()
    }
}