package com.eunio.healthapp.android.ui.logging

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eunio.healthapp.android.ui.theme.EunioTheme
import kotlinx.datetime.LocalDate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Comprehensive test suite for calendar date display functionality.
 * Tests Requirements: 1.1, 1.2, 1.3, 1.4, 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3, 3.4
 * 
 * This test suite verifies:
 * - Correct date display on October 10, 2025
 * - Previous/next day navigation updates calendar correctly
 * - Month boundary scenarios (October 1, October 31)
 * - Year boundary scenarios (January 1, December 31)
 * - Accessibility announcements with TalkBack
 */
@RunWith(AndroidJUnit4::class)
class CalendarDateDisplayTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Test Requirement 1.1, 1.2: Display correct dates on October 10, 2025
     * WHEN the selected date is October 10, 2025 
     * THEN the calendar SHALL show dates around October 10
     */
    @Test
    fun calendarDateDisplay_showsCorrectOctoberDates() {
        composeTestRule.setContent {
            EunioTheme {
                DailyLoggingScreen()
            }
        }

        // Wait for the screen to load
        composeTestRule.waitForIdle()

        // Verify the main selected date is displayed
        composeTestRule.onNodeWithText("2025-10-10", substring = true)
            .assertIsDisplayed()

        // Verify quick date selection shows October dates
        // The date range should be Oct 7, 8, 9, 10, 11, 12, 13 (3 days before to 3 days after)
        composeTestRule.onNodeWithText("Oct", substring = true)
            .assertIsDisplayed()
        
        // Verify specific dates in the range
        composeTestRule.onNodeWithText("7").assertExists()
        composeTestRule.onNodeWithText("8").assertExists()
        composeTestRule.onNodeWithText("9").assertExists()
        composeTestRule.onNodeWithText("10").assertExists()
        composeTestRule.onNodeWithText("11").assertExists()
        composeTestRule.onNodeWithText("12").assertExists()
        composeTestRule.onNodeWithText("13").assertExists()

        // Verify NO January dates are shown (the bug we fixed)
        composeTestRule.onNodeWithText("Jan", substring = true)
            .assertDoesNotExist()
    }

    /**
     * Test Requirement 1.3, 2.4: Previous day navigation updates calendar
     * WHEN the user navigates to a different date using the arrow buttons
     * THEN the quick date selection row SHALL update to show dates relative to the new selected date
     */
    @Test
    fun calendarDateDisplay_updatesOnPreviousDayNavigation() {
        composeTestRule.setContent {
            EunioTheme {
                DailyLoggingScreen()
            }
        }

        composeTestRule.waitForIdle()

        // Click previous day button
        composeTestRule.onNodeWithContentDescription("Go to previous day")
            .performClick()

        composeTestRule.waitForIdle()

        // Verify the selected date changed to October 9, 2025
        composeTestRule.onNodeWithText("2025-10-09", substring = true)
            .assertIsDisplayed()

        // Verify quick date selection updated to show dates around October 9
        // Should now show Oct 6, 7, 8, 9, 10, 11, 12
        composeTestRule.onNodeWithText("6").assertExists()
        composeTestRule.onNodeWithText("9").assertExists()
        composeTestRule.onNodeWithText("12").assertExists()
    }

    /**
     * Test Requirement 1.3, 2.4: Next day navigation updates calendar
     */
    @Test
    fun calendarDateDisplay_updatesOnNextDayNavigation() {
        composeTestRule.setContent {
            EunioTheme {
                DailyLoggingScreen()
            }
        }

        composeTestRule.waitForIdle()

        // Click next day button
        composeTestRule.onNodeWithContentDescription("Go to next day")
            .performClick()

        composeTestRule.waitForIdle()

        // Verify the selected date changed to October 11, 2025
        composeTestRule.onNodeWithText("2025-10-11", substring = true)
            .assertIsDisplayed()

        // Verify quick date selection updated to show dates around October 11
        // Should now show Oct 8, 9, 10, 11, 12, 13, 14
        composeTestRule.onNodeWithText("8").assertExists()
        composeTestRule.onNodeWithText("11").assertExists()
        composeTestRule.onNodeWithText("14").assertExists()
    }

    /**
     * Test Requirement 1.3: Multiple navigation clicks update correctly
     */
    @Test
    fun calendarDateDisplay_updatesOnMultipleNavigationClicks() {
        composeTestRule.setContent {
            EunioTheme {
                DailyLoggingScreen()
            }
        }

        composeTestRule.waitForIdle()

        // Click previous day 3 times
        repeat(3) {
            composeTestRule.onNodeWithContentDescription("Go to previous day")
                .performClick()
            composeTestRule.waitForIdle()
        }

        // Should now be on October 7, 2025
        composeTestRule.onNodeWithText("2025-10-07", substring = true)
            .assertIsDisplayed()

        // Click next day 5 times
        repeat(5) {
            composeTestRule.onNodeWithContentDescription("Go to next day")
                .performClick()
            composeTestRule.waitForIdle()
        }

        // Should now be on October 12, 2025
        composeTestRule.onNodeWithText("2025-10-12", substring = true)
            .assertIsDisplayed()
    }

    /**
     * Test Requirement 1.4, 2.1, 2.2: Month boundary - October 1st
     * WHEN the selected date is near the beginning of a month
     * THEN the system SHALL correctly display dates spanning multiple months
     */
    @Test
    fun calendarDateDisplay_handlesOctoberFirstBoundary() {
        composeTestRule.setContent {
            EunioTheme {
                DailyLoggingScreen()
            }
        }

        composeTestRule.waitForIdle()

        // Navigate to October 1, 2025 (9 days back from Oct 10)
        repeat(9) {
            composeTestRule.onNodeWithContentDescription("Go to previous day")
                .performClick()
            composeTestRule.waitForIdle()
        }

        // Verify we're on October 1
        composeTestRule.onNodeWithText("2025-10-01", substring = true)
            .assertIsDisplayed()

        // The date range should span September and October
        // Should show Sep 28, 29, 30, Oct 1, 2, 3, 4
        composeTestRule.onNodeWithText("Sep", substring = true)
            .assertExists()
        
        composeTestRule.onNodeWithText("Oct", substring = true)
            .assertExists()

        // Verify specific dates
        composeTestRule.onNodeWithText("28").assertExists() // Sep 28
        composeTestRule.onNodeWithText("1").assertExists()  // Oct 1
        composeTestRule.onNodeWithText("4").assertExists()  // Oct 4
    }

    /**
     * Test Requirement 1.4, 2.1, 2.2: Month boundary - October 31st
     * WHEN the selected date is near the end of a month
     * THEN the system SHALL correctly display dates spanning multiple months
     */
    @Test
    fun calendarDateDisplay_handlesOctoberLastBoundary() {
        composeTestRule.setContent {
            EunioTheme {
                DailyLoggingScreen()
            }
        }

        composeTestRule.waitForIdle()

        // Navigate to October 31, 2025 (21 days forward from Oct 10)
        repeat(21) {
            composeTestRule.onNodeWithContentDescription("Go to next day")
                .performClick()
            composeTestRule.waitForIdle()
        }

        // Verify we're on October 31
        composeTestRule.onNodeWithText("2025-10-31", substring = true)
            .assertIsDisplayed()

        // The date range should span October and November
        // Should show Oct 28, 29, 30, 31, Nov 1, 2, 3
        composeTestRule.onNodeWithText("Oct", substring = true)
            .assertExists()
        
        composeTestRule.onNodeWithText("Nov", substring = true)
            .assertExists()

        // Verify specific dates
        composeTestRule.onNodeWithText("28").assertExists() // Oct 28
        composeTestRule.onNodeWithText("31").assertExists() // Oct 31
        composeTestRule.onNodeWithText("3").assertExists()  // Nov 3
    }

    /**
     * Test Requirement 1.4, 2.1, 2.2: Year boundary - January 1st
     * WHEN the selected date is near the beginning of a year
     * THEN the system SHALL correctly display dates spanning multiple years
     */
    @Test
    fun calendarDateDisplay_handlesJanuaryFirstBoundary() {
        composeTestRule.setContent {
            EunioTheme {
                DailyLoggingScreen()
            }
        }

        composeTestRule.waitForIdle()

        // Navigate to January 1, 2025 (283 days back from Oct 10, 2025)
        // This is a lot of clicks, so we'll use a more efficient approach
        // Navigate back to October 1 first (9 clicks)
        repeat(9) {
            composeTestRule.onNodeWithContentDescription("Go to previous day")
                .performClick()
            composeTestRule.waitForIdle()
        }

        // Then navigate back 3 more months (approximately 92 days)
        repeat(92) {
            composeTestRule.onNodeWithContentDescription("Go to previous day")
                .performClick()
            if (it % 10 == 0) {
                composeTestRule.waitForIdle()
            }
        }

        // Verify we're on or near January 1, 2025
        composeTestRule.onNodeWithText("2025-01", substring = true)
            .assertIsDisplayed()

        // The date range should span December 2024 and January 2025
        composeTestRule.onNodeWithText("Dec", substring = true)
            .assertExists()
        
        composeTestRule.onNodeWithText("Jan", substring = true)
            .assertExists()

        // Verify the year boundary is handled correctly
        composeTestRule.onNodeWithText("2025", substring = true)
            .assertExists()
    }

    /**
     * Test Requirement 1.4, 2.1, 2.2: Year boundary - December 31st
     * WHEN the selected date is near the end of a year
     * THEN the system SHALL correctly display dates spanning multiple years
     */
    @Test
    fun calendarDateDisplay_handlesDecemberLastBoundary() {
        composeTestRule.setContent {
            EunioTheme {
                DailyLoggingScreen()
            }
        }

        composeTestRule.waitForIdle()

        // Navigate to December 31, 2025 (82 days forward from Oct 10, 2025)
        repeat(82) {
            composeTestRule.onNodeWithContentDescription("Go to next day")
                .performClick()
            if (it % 10 == 0) {
                composeTestRule.waitForIdle()
            }
        }

        // Verify we're on or near December 31, 2025
        composeTestRule.onNodeWithText("2025-12-31", substring = true)
            .assertIsDisplayed()

        // The date range should span December 2025 and January 2026
        composeTestRule.onNodeWithText("Dec", substring = true)
            .assertExists()
        
        // After Dec 31, we should see Jan dates from 2026
        composeTestRule.onNodeWithText("Jan", substring = true)
            .assertExists()
    }

    /**
     * Test Requirement 1.4, 2.3: Date formatting is correct
     * WHEN displaying dates THEN each date SHALL show the correct day number and month abbreviation
     */
    @Test
    fun calendarDateDisplay_formatsMonthAbbreviationsCorrectly() {
        composeTestRule.setContent {
            EunioTheme {
                DailyLoggingScreen()
            }
        }

        composeTestRule.waitForIdle()

        // Verify October abbreviation
        composeTestRule.onNodeWithText("Oct", substring = true)
            .assertExists()

        // Navigate to different months and verify abbreviations
        // Go back to September
        repeat(10) {
            composeTestRule.onNodeWithContentDescription("Go to previous day")
                .performClick()
            composeTestRule.waitForIdle()
        }

        composeTestRule.onNodeWithText("Sep", substring = true)
            .assertExists()

        // Go forward to November
        repeat(61) {
            composeTestRule.onNodeWithContentDescription("Go to next day")
                .performClick()
            if (it % 10 == 0) {
                composeTestRule.waitForIdle()
            }
        }

        composeTestRule.onNodeWithText("Nov", substring = true)
            .assertExists()
    }

    /**
     * Test Requirement 3.1, 3.2: Accessibility - Date descriptions
     * WHEN dates are displayed THEN each date SHALL have an accurate accessibility description
     */
    @Test
    fun calendarDateDisplay_hasCorrectAccessibilityDescriptions() {
        composeTestRule.setContent {
            EunioTheme {
                DailyLoggingScreen()
            }
        }

        composeTestRule.waitForIdle()

        // Verify the date navigation section has accessibility description
        composeTestRule.onNodeWithContentDescription("Date navigation section")
            .assertExists()

        // Verify navigation buttons have accessibility descriptions
        composeTestRule.onNodeWithContentDescription("Go to previous day")
            .assertExists()
        
        composeTestRule.onNodeWithContentDescription("Go to next day")
            .assertExists()

        // Verify the selected date has accessibility description
        composeTestRule.onNode(
            hasContentDescription("Selected date: 2025-10-10", substring = true)
        ).assertExists()
    }

    /**
     * Test Requirement 3.2: Accessibility - Selected date indication
     * WHEN the selected date is highlighted 
     * THEN the accessibility description SHALL indicate "currently selected"
     */
    @Test
    fun calendarDateDisplay_indicatesSelectedDateForAccessibility() {
        composeTestRule.setContent {
            EunioTheme {
                DailyLoggingScreen()
            }
        }

        composeTestRule.waitForIdle()

        // Find a date card with "currently selected" in its description
        composeTestRule.onNode(
            hasContentDescription("currently selected", substring = true)
        ).assertExists()
    }

    /**
     * Test Requirement 3.3: Accessibility - Date changes are announced
     * WHEN the selected date changes THEN screen readers SHALL announce the changes
     */
    @Test
    fun calendarDateDisplay_announcesDateChangesForAccessibility() {
        composeTestRule.setContent {
            EunioTheme {
                DailyLoggingScreen()
            }
        }

        composeTestRule.waitForIdle()

        // Click previous day
        composeTestRule.onNodeWithContentDescription("Go to previous day")
            .performClick()

        composeTestRule.waitForIdle()

        // Verify the new date is announced (the selected date description should update)
        composeTestRule.onNode(
            hasContentDescription("Selected date: 2025-10-09", substring = true)
        ).assertExists()

        // Click next day
        composeTestRule.onNodeWithContentDescription("Go to next day")
            .performClick()

        composeTestRule.waitForIdle()

        // Verify the date changed back
        composeTestRule.onNode(
            hasContentDescription("Selected date: 2025-10-10", substring = true)
        ).assertExists()
    }

    /**
     * Test Requirement 3.4: Accessibility - Touch target size
     * WHEN dates are displayed THEN all touch targets SHALL remain at minimum 48dp size
     */
    @Test
    fun calendarDateDisplay_maintainsMinimumTouchTargetSize() {
        composeTestRule.setContent {
            EunioTheme {
                DailyLoggingScreen()
            }
        }

        composeTestRule.waitForIdle()

        // Verify navigation buttons have minimum touch target
        composeTestRule.onNodeWithContentDescription("Go to previous day")
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
        
        composeTestRule.onNodeWithContentDescription("Go to next day")
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)

        // Verify date cards have minimum touch target
        // Find any date card with "Select" in its description
        composeTestRule.onNode(
            hasContentDescription("Select", substring = true)
        ).assertHeightIsAtLeast(48.dp)
    }

    /**
     * Test Requirement 3.3: Accessibility - Quick date selection description
     * WHEN the quick date selection is displayed 
     * THEN it SHALL have appropriate accessibility descriptions
     */
    @Test
    fun calendarDateDisplay_hasAccessibleQuickDateSelection() {
        composeTestRule.setContent {
            EunioTheme {
                DailyLoggingScreen()
            }
        }

        composeTestRule.waitForIdle()

        // Verify quick date selection has accessibility description
        composeTestRule.onNode(
            hasContentDescription("Quick date selection", substring = true)
        ).assertExists()
    }

    /**
     * Test Requirement 2.1, 2.2: Dynamic date calculation
     * WHEN the calendar renders THEN it SHALL calculate dates dynamically based on selected date
     */
    @Test
    fun calendarDateDisplay_calculatesDatesDynamically() {
        composeTestRule.setContent {
            EunioTheme {
                DailyLoggingScreen()
            }
        }

        composeTestRule.waitForIdle()

        // Store initial date display
        val initialDate = "2025-10-10"
        composeTestRule.onNodeWithText(initialDate, substring = true)
            .assertIsDisplayed()

        // Navigate and verify dates update dynamically
        composeTestRule.onNodeWithContentDescription("Go to next day")
            .performClick()

        composeTestRule.waitForIdle()

        // Verify the date changed
        val newDate = "2025-10-11"
        composeTestRule.onNodeWithText(newDate, substring = true)
            .assertIsDisplayed()

        // Verify the old date is no longer the selected date
        composeTestRule.onNode(
            hasContentDescription("Selected date: $initialDate", substring = true)
        ).assertDoesNotExist()
    }

    /**
     * Integration test: Complete navigation flow
     * Tests the entire user flow of navigating through dates
     */
    @Test
    fun calendarDateDisplay_completeNavigationFlow() {
        composeTestRule.setContent {
            EunioTheme {
                DailyLoggingScreen()
            }
        }

        composeTestRule.waitForIdle()

        // Start on October 10, 2025
        composeTestRule.onNodeWithText("2025-10-10", substring = true)
            .assertIsDisplayed()

        // Navigate backward 5 days
        repeat(5) {
            composeTestRule.onNodeWithContentDescription("Go to previous day")
                .performClick()
            composeTestRule.waitForIdle()
        }

        // Should be on October 5, 2025
        composeTestRule.onNodeWithText("2025-10-05", substring = true)
            .assertIsDisplayed()

        // Navigate forward 10 days
        repeat(10) {
            composeTestRule.onNodeWithContentDescription("Go to next day")
                .performClick()
            composeTestRule.waitForIdle()
        }

        // Should be on October 15, 2025
        composeTestRule.onNodeWithText("2025-10-15", substring = true)
            .assertIsDisplayed()

        // Verify October dates are still showing (not January)
        composeTestRule.onNodeWithText("Oct", substring = true)
            .assertExists()
        
        composeTestRule.onNodeWithText("Jan", substring = true)
            .assertDoesNotExist()
    }
}
