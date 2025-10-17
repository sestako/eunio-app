package com.eunio.healthapp.android.ui.logging

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eunio.healthapp.android.ui.theme.EunioTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DailyLoggingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dailyLoggingScreen_displaysCorrectly() {
        composeTestRule.setContent {
            EunioTheme {
                DailyLoggingScreen()
            }
        }

        // Verify main components are displayed
        composeTestRule.onNodeWithText("Daily Log").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
        
        // Verify form sections are displayed
        composeTestRule.onNodeWithText("Period Flow").assertIsDisplayed()
        composeTestRule.onNodeWithText("Symptoms").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mood").assertIsDisplayed()
        composeTestRule.onNodeWithText("Basal Body Temperature").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cervical Mucus").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ovulation Test (OPK)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sexual Activity").assertIsDisplayed()
        composeTestRule.onNodeWithText("Notes").assertIsDisplayed()
    }

    @Test
    fun dateNavigation_worksCorrectly() {
        composeTestRule.setContent {
            EunioTheme {
                DailyLoggingScreen()
            }
        }

        // Test previous day navigation
        composeTestRule.onNodeWithContentDescription("Previous day").performClick()
        
        // Test next day navigation
        composeTestRule.onNodeWithContentDescription("Next day").performClick()
    }

    @Test
    fun periodFlowSelection_worksCorrectly() {
        composeTestRule.setContent {
            EunioTheme {
                DailyLoggingScreen()
            }
        }

        // Test selecting different period flows
        composeTestRule.onNodeWithText("Light").performClick()
        composeTestRule.onNodeWithText("Medium").performClick()
        composeTestRule.onNodeWithText("Heavy").performClick()
        composeTestRule.onAllNodesWithText("None")[0].performClick() // Period flow "None"
    }

    @Test
    fun symptomSelection_allowsMultipleSelections() {
        composeTestRule.setContent {
            EunioTheme {
                DailyLoggingScreen()
            }
        }

        // Test selecting multiple symptoms
        composeTestRule.onNodeWithText("Cramps").performClick()
        composeTestRule.onNodeWithText("Headache").performClick()
        composeTestRule.onNodeWithText("Bloating").performClick()
        
        // Test deselecting a symptom
        composeTestRule.onNodeWithText("Cramps").performClick()
    }

    @Test
    fun moodSelection_worksCorrectly() {
        composeTestRule.setContent {
            EunioTheme {
                DailyLoggingScreen()
            }
        }

        // Test mood selection
        composeTestRule.onNodeWithText("😊 Happy").performClick()
        composeTestRule.onNodeWithText("😢 Sad").performClick()
        composeTestRule.onNodeWithText("😌 Calm").performClick()
    }

    @Test
    fun bbtInput_acceptsValidInput() {
        composeTestRule.setContent {
            EunioTheme {
                DailyLoggingScreen()
            }
        }

        // Find BBT input field and enter valid temperature
        composeTestRule.onNodeWithText("Temperature (°F)").performTextInput("98.2")
        
        // Verify the input was accepted
        composeTestRule.onNodeWithText("98.2").assertIsDisplayed()
    }

    @Test
    fun cervicalMucusSelection_worksCorrectly() {
        composeTestRule.setContent {
            EunioTheme {
                DailyLoggingScreen()
            }
        }

        // Test cervical mucus selection
        composeTestRule.onNodeWithText("Dry").performClick()
        composeTestRule.onNodeWithText("Creamy").performClick()
        composeTestRule.onNodeWithText("Egg White").performClick()
    }

    @Test
    fun opkResultSelection_worksCorrectly() {
        composeTestRule.setContent {
            EunioTheme {
                DailyLoggingScreen()
            }
        }

        // Test OPK result selection
        composeTestRule.onNodeWithText("Negative").performClick()
        composeTestRule.onNodeWithText("Positive").performClick()
        composeTestRule.onNodeWithText("Peak").performClick()
    }

    @Test
    fun sexualActivitySelection_worksCorrectly() {
        composeTestRule.setContent {
            EunioTheme {
                DailyLoggingScreen()
            }
        }

        // Test selecting "Yes" for sexual activity
        composeTestRule.onAllNodesWithText("Yes")[0].performClick() // Sexual activity "Yes"

        // After selecting "Yes", protection options should appear
        composeTestRule.onNodeWithText("Protection Used:").assertIsDisplayed()
        
        // Test protection selection
        composeTestRule.onNodeWithText("Condom").performClick()
    }

    @Test
    fun notesInput_worksCorrectly() {
        composeTestRule.setContent {
            EunioTheme {
                DailyLoggingScreen()
            }
        }

        // Test notes input
        composeTestRule.onNodeWithText("Add any additional notes...")
            .performTextInput("Feeling great today!")
        
        // Verify the input was accepted
        composeTestRule.onNodeWithText("Feeling great today!").assertIsDisplayed()
    }

    @Test
    fun saveButton_appearsWhenChangesExist() {
        composeTestRule.setContent {
            EunioTheme {
                DailyLoggingScreen()
            }
        }

        // Make a change to trigger save button
        composeTestRule.onNodeWithText("Light").performClick()
        
        // Save button should be visible
        composeTestRule.onNodeWithText("Save").assertIsDisplayed()

        // Test save button click
        composeTestRule.onNodeWithText("Save").performClick()
        
        // Success message should appear
        composeTestRule.onNodeWithText("Log saved successfully").assertIsDisplayed()
    }

    @Test
    fun formValidation_showsErrorForInvalidBBT() {
        composeTestRule.setContent {
            EunioTheme {
                DailyLoggingScreen()
            }
        }

        // Enter invalid BBT
        composeTestRule.onNodeWithText("Temperature (°F)").performTextInput("110.0")
        
        // Error message should be displayed
        composeTestRule.onNodeWithText("Temperature must be between 95.0 and 105.0°F")
            .assertIsDisplayed()
    }

    @Test
    fun quickDateSelection_worksCorrectly() {
        composeTestRule.setContent {
            EunioTheme {
                DailyLoggingScreen()
            }
        }

        // Test clicking on a quick date selection
        composeTestRule.onNodeWithText("30").performClick()
    }

    @Test
    fun allFormSections_areScrollable() {
        composeTestRule.setContent {
            EunioTheme {
                DailyLoggingScreen()
            }
        }

        // Verify all sections are accessible by scrolling
        composeTestRule.onNodeWithText("Period Flow").assertIsDisplayed()
        
        // Scroll to bottom to see notes section
        composeTestRule.onNodeWithText("Notes").performScrollTo()
        composeTestRule.onNodeWithText("Notes").assertIsDisplayed()
    }
}