package com.eunio.healthapp.android.ui.logging

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eunio.healthapp.android.ui.theme.EunioTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DailyLoggingUITest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun dailyLogging_displaysAllInputFields() {
        composeTestRule.setContent {
            EunioTheme {
                // DailyLoggingScreen() - Would need actual implementation
            }
        }
        
        // Verify all input fields are present
        composeTestRule
            .onNodeWithText("Period Flow")
            .assertExists()
        
        composeTestRule
            .onNodeWithText("Symptoms")
            .assertExists()
        
        composeTestRule
            .onNodeWithText("Mood")
            .assertExists()
        
        composeTestRule
            .onNodeWithText("BBT")
            .assertExists()
        
        composeTestRule
            .onNodeWithText("Cervical Mucus")
            .assertExists()
        
        composeTestRule
            .onNodeWithText("Sexual Activity")
            .assertExists()
    }
    
    @Test
    fun dailyLogging_allowsBBTInput() {
        composeTestRule.setContent {
            EunioTheme {
                // BBT input component - Would need actual implementation
            }
        }
        
        // Input BBT value
        composeTestRule
            .onNodeWithText("BBT")
            .performTextInput("98.6")
        
        // Verify input is accepted
        composeTestRule
            .onNodeWithText("98.6")
            .assertExists()
    }
    
    @Test
    fun dailyLogging_validatesBBTRange() {
        composeTestRule.setContent {
            EunioTheme {
                // BBT input with validation - Would need actual implementation
            }
        }
        
        // Input invalid BBT value
        composeTestRule
            .onNodeWithText("BBT")
            .performTextInput("150.0")
        
        // Verify error message
        composeTestRule
            .onNodeWithText("Please enter a valid temperature between 95°F and 105°F")
            .assertExists()
    }
    
    @Test
    fun dailyLogging_allowsSymptomSelection() {
        composeTestRule.setContent {
            EunioTheme {
                // Symptom selection component - Would need actual implementation
            }
        }
        
        // Select multiple symptoms
        composeTestRule
            .onNodeWithText("Cramps")
            .performClick()
        
        composeTestRule
            .onNodeWithText("Headache")
            .performClick()
        
        composeTestRule
            .onNodeWithText("Bloating")
            .performClick()
        
        // Verify selections are highlighted
        composeTestRule
            .onNode(hasText("Cramps") and isSelected())
            .assertExists()
    }
    
    @Test
    fun dailyLogging_savesDataSuccessfully() {
        composeTestRule.setContent {
            EunioTheme {
                // Complete daily logging form - Would need actual implementation
            }
        }
        
        // Fill out form
        composeTestRule
            .onNodeWithText("BBT")
            .performTextInput("98.6")
        
        composeTestRule
            .onNodeWithText("Cramps")
            .performClick()
        
        // Save data
        composeTestRule
            .onNodeWithText("Save")
            .performClick()
        
        // Verify success message
        composeTestRule
            .onNodeWithText("Daily log saved successfully")
            .assertExists()
    }
    
    @Test
    fun dailyLogging_allowsDateNavigation() {
        composeTestRule.setContent {
            EunioTheme {
                // Daily logging with date picker - Would need actual implementation
            }
        }
        
        // Navigate to previous day
        composeTestRule
            .onNodeWithContentDescription("Previous day")
            .performClick()
        
        // Navigate to next day
        composeTestRule
            .onNodeWithContentDescription("Next day")
            .performClick()
        
        // Open date picker
        composeTestRule
            .onNodeWithContentDescription("Select date")
            .performClick()
        
        // Verify date picker is displayed
        composeTestRule
            .onNodeWithText("Select Date")
            .assertExists()
    }
}