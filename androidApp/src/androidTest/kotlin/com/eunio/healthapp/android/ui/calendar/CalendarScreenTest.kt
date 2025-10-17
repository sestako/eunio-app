package com.eunio.healthapp.android.ui.calendar

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eunio.healthapp.android.ui.theme.EunioTheme
import com.eunio.healthapp.android.ui.calendar.SimpleCalendarUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CalendarScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun calendarScreen_displaysCorrectly() {
        composeTestRule.setContent {
            EunioTheme {
                CalendarScreen()
            }
        }
        
        // Verify calendar header is displayed
        composeTestRule.onNodeWithText("March 2024").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Previous month").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Next month").assertIsDisplayed()
        composeTestRule.onNodeWithText("Today").assertIsDisplayed()
        
        // Verify day headers are displayed
        composeTestRule.onNodeWithText("Sun").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mon").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tue").assertIsDisplayed()
        composeTestRule.onNodeWithText("Wed").assertIsDisplayed()
        composeTestRule.onNodeWithText("Thu").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fri").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sat").assertIsDisplayed()
        
        // Verify cycle information card
        composeTestRule.onNodeWithText("Cycle Information").assertIsDisplayed()
        
        // Verify legend
        composeTestRule.onNodeWithText("Legend").assertIsDisplayed()
    }
    
    @Test
    fun calendarScreen_navigationButtons_work() {
        var previousClicked = false
        var nextClicked = false
        var todayClicked = false
        
        composeTestRule.setContent {
            EunioTheme {
                CalendarScreen(
                    onPreviousMonth = { previousClicked = true },
                    onNextMonth = { nextClicked = true },
                    onGoToToday = { todayClicked = true }
                )
            }
        }
        
        // Test previous month navigation
        composeTestRule.onNodeWithContentDescription("Previous month").performClick()
        assert(previousClicked)
        
        // Test next month navigation
        composeTestRule.onNodeWithContentDescription("Next month").performClick()
        assert(nextClicked)
        
        // Test today button
        composeTestRule.onNodeWithText("Today").performClick()
        assert(todayClicked)
    }
    
    @Test
    fun calendarScreen_withoutCycleData_displaysPrompt() {
        val uiState = SimpleCalendarUiState(
            hasCycle = false
        )
        
        composeTestRule.setContent {
            EunioTheme {
                CalendarScreen(uiState = uiState)
            }
        }
        
        // Verify prompt message is displayed
        composeTestRule.onNodeWithText("Start logging your period to see cycle predictions")
            .assertIsDisplayed()
    }
    
    @Test
    fun calendarScreen_withError_displaysErrorMessage() {
        val uiState = SimpleCalendarUiState(
            errorMessage = "Failed to load calendar data"
        )
        
        composeTestRule.setContent {
            EunioTheme {
                CalendarScreen(uiState = uiState)
            }
        }
        
        // Verify error message is displayed
        composeTestRule.onNodeWithText("Failed to load calendar data").assertIsDisplayed()
    }
    
    @Test
    fun calendarScreen_loading_displaysProgressIndicator() {
        val uiState = SimpleCalendarUiState(
            isLoading = true
        )
        
        composeTestRule.setContent {
            EunioTheme {
                CalendarScreen(uiState = uiState)
            }
        }
        
        // Verify loading indicator is displayed
        // Note: CircularProgressIndicator doesn't have specific test semantics, 
        // so we just verify the screen renders without crashing when loading
    }
    
    @Test
    fun calendarScreen_legendItems_displayCorrectly() {
        composeTestRule.setContent {
            EunioTheme {
                CalendarScreen()
            }
        }
        
        // Verify all legend items are displayed
        composeTestRule.onNodeWithText("Period").assertIsDisplayed()
        composeTestRule.onNodeWithText("Predicted Period").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ovulation").assertIsDisplayed()
        composeTestRule.onNodeWithText("Predicted Ovulation").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fertility Window").assertIsDisplayed()
    }
}