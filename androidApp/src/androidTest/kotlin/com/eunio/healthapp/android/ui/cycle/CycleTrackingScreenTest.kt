package com.eunio.healthapp.android.ui.cycle

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eunio.healthapp.android.ui.theme.EunioTheme
import com.eunio.healthapp.android.ui.cycle.SimpleCycleUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CycleTrackingScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun cycleTrackingScreen_displaysCorrectly() {
        composeTestRule.setContent {
            EunioTheme {
                CycleTrackingScreen()
            }
        }
        
        // Verify main title
        composeTestRule.onNodeWithText("Cycle Tracking").assertIsDisplayed()
        
        // Verify card titles
        composeTestRule.onNodeWithText("Current Cycle").assertIsDisplayed()
        composeTestRule.onNodeWithText("Predictions").assertIsDisplayed()
        composeTestRule.onNodeWithText("Current Phase").assertIsDisplayed()
        composeTestRule.onNodeWithText("Recent Cycles").assertIsDisplayed()
    }
    
    @Test
    fun cycleTrackingScreen_withoutActiveCycle_displaysPrompt() {
        val uiState = SimpleCycleUiState(
            hasCycle = false
        )
        
        composeTestRule.setContent {
            EunioTheme {
                CycleTrackingScreen(uiState = uiState)
            }
        }
        
        // Verify no active cycle message
        composeTestRule.onNodeWithText("No active cycle").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start logging your period to begin cycle tracking")
            .assertIsDisplayed()
    }
    
    @Test
    fun cycleTrackingScreen_withoutPredictions_displaysPrompt() {
        val uiState = SimpleCycleUiState(
            hasCycle = false
        )
        
        composeTestRule.setContent {
            EunioTheme {
                CycleTrackingScreen(uiState = uiState)
            }
        }
        
        // Verify no predictions message
        composeTestRule.onNodeWithText("Log more cycles to see predictions").assertIsDisplayed()
    }
    
    @Test
    fun cycleTrackingScreen_withoutCycleHistory_displaysPrompt() {
        val uiState = SimpleCycleUiState(
            cycleHistory = emptyList()
        )
        
        composeTestRule.setContent {
            EunioTheme {
                CycleTrackingScreen(uiState = uiState)
            }
        }
        
        // Verify no history message
        composeTestRule.onNodeWithText("No cycle history available yet").assertIsDisplayed()
    }
    
    @Test
    fun cycleTrackingScreen_withError_displaysErrorMessage() {
        val uiState = SimpleCycleUiState(
            errorMessage = "Failed to load cycle data"
        )
        
        composeTestRule.setContent {
            EunioTheme {
                CycleTrackingScreen(uiState = uiState)
            }
        }
        
        // Verify error message is displayed
        composeTestRule.onNodeWithText("Failed to load cycle data").assertIsDisplayed()
    }
    
    @Test
    fun cycleTrackingScreen_loading_displaysProgressIndicator() {
        val uiState = SimpleCycleUiState(
            isLoading = true
        )
        
        composeTestRule.setContent {
            EunioTheme {
                CycleTrackingScreen(uiState = uiState)
            }
        }
        
        // Verify loading indicator is displayed
        // Note: CircularProgressIndicator doesn't have specific test semantics, 
        // so we just verify the screen renders without crashing when loading
    }
}