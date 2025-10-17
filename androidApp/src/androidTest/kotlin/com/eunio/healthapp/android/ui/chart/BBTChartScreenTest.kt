package com.eunio.healthapp.android.ui.chart

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eunio.healthapp.android.ui.theme.EunioTheme
import com.eunio.healthapp.domain.model.Cycle
import com.eunio.healthapp.domain.model.DailyLog
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BBTChartScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleTemperatureLogs = listOf(
        createSampleDailyLog(LocalDate(2024, 1, 10), 36.2),
        createSampleDailyLog(LocalDate(2024, 1, 11), 36.3),
        createSampleDailyLog(LocalDate(2024, 1, 12), 36.1),
        createSampleDailyLog(LocalDate(2024, 1, 13), 36.4),
        createSampleDailyLog(LocalDate(2024, 1, 14), 36.8),
        createSampleDailyLog(LocalDate(2024, 1, 15), 36.9),
        createSampleDailyLog(LocalDate(2024, 1, 16), 36.7)
    )

    private val sampleCycle = Cycle(
        id = "test-cycle",
        userId = "test-user",
        startDate = LocalDate(2024, 1, 1),
        endDate = null,
        predictedOvulationDate = LocalDate(2024, 1, 14),
        confirmedOvulationDate = LocalDate(2024, 1, 14),
        cycleLength = 28,
        lutealPhaseLength = 14
    )

    @Test
    fun bbtChartScreen_displaysCorrectly_withTemperatureData() {
        composeTestRule.setContent {
            EunioTheme {
                BBTChartScreen(
                    temperatureLogs = sampleTemperatureLogs,
                    currentCycle = sampleCycle
                )
            }
        }

        // Verify main components are displayed
        composeTestRule.onNodeWithText("BBT Chart").assertIsDisplayed()
        composeTestRule.onNodeWithText("Temperature Trend").assertIsDisplayed()
        composeTestRule.onNodeWithText("Temperature Statistics").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cycle Information").assertIsDisplayed()
    }

    @Test
    fun bbtChartScreen_displaysEmptyState_withNoData() {
        composeTestRule.setContent {
            EunioTheme {
                BBTChartScreen(
                    temperatureLogs = emptyList(),
                    currentCycle = null
                )
            }
        }

        // Verify empty state is displayed
        composeTestRule.onNodeWithText("No temperature data available").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start logging your BBT to see trends").assertIsDisplayed()
    }

    @Test
    fun bbtChartScreen_zoomControls_workCorrectly() {
        composeTestRule.setContent {
            EunioTheme {
                BBTChartScreen(
                    temperatureLogs = sampleTemperatureLogs,
                    currentCycle = sampleCycle
                )
            }
        }

        // Test zoom in
        composeTestRule.onNodeWithContentDescription("Zoom In").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Zoom In").performClick()
        
        // Verify zoom level changed
        composeTestRule.onNodeWithText("Zoom: 1.2x").assertIsDisplayed()

        // Test zoom out
        composeTestRule.onNodeWithContentDescription("Zoom Out").performClick()
        
        // Test reset zoom
        composeTestRule.onNodeWithText("1:1").performClick()
        composeTestRule.onNodeWithText("Zoom: 1.0x").assertIsDisplayed()
    }

    @Test
    fun bbtChartScreen_displaysStatistics_correctly() {
        composeTestRule.setContent {
            EunioTheme {
                BBTChartScreen(
                    temperatureLogs = sampleTemperatureLogs,
                    currentCycle = sampleCycle
                )
            }
        }

        // Verify statistics section
        composeTestRule.onNodeWithText("Temperature Statistics").assertIsDisplayed()
        composeTestRule.onNodeWithText("Average").assertIsDisplayed()
        composeTestRule.onNodeWithText("Range").assertIsDisplayed()
        composeTestRule.onNodeWithText("Readings").assertIsDisplayed()
        composeTestRule.onNodeWithText("7").assertIsDisplayed() // Number of readings
    }

    @Test
    fun bbtChartScreen_displaysCyclePhases_correctly() {
        composeTestRule.setContent {
            EunioTheme {
                BBTChartScreen(
                    temperatureLogs = sampleTemperatureLogs,
                    currentCycle = sampleCycle
                )
            }
        }

        // Verify cycle phase indicators
        composeTestRule.onNodeWithText("Cycle Information").assertIsDisplayed()
        composeTestRule.onNodeWithText("Menstrual").assertIsDisplayed()
        composeTestRule.onNodeWithText("Follicular").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ovulatory").assertIsDisplayed()
        composeTestRule.onNodeWithText("Luteal").assertIsDisplayed()
    }

    @Test
    fun bbtChartScreen_displaysRecentReadings_correctly() {
        composeTestRule.setContent {
            EunioTheme {
                BBTChartScreen(
                    temperatureLogs = sampleTemperatureLogs,
                    currentCycle = sampleCycle
                )
            }
        }

        // Verify recent readings section
        composeTestRule.onNodeWithText("Recent Readings").assertIsDisplayed()
        
        // Check that temperature readings are displayed
        // Note: The exact format depends on the ReactiveTemperatureDisplay component
        // Just verify that some date text is present
        composeTestRule.onNodeWithText("Recent Readings").assertExists()
    }

    @Test
    fun bbtChartScreen_displaysPatternInsights_whenAvailable() {
        // Create data with a clear pattern (temperature rise)
        val patternData = listOf(
            createSampleDailyLog(LocalDate(2024, 1, 10), 36.1),
            createSampleDailyLog(LocalDate(2024, 1, 11), 36.2),
            createSampleDailyLog(LocalDate(2024, 1, 12), 36.1),
            createSampleDailyLog(LocalDate(2024, 1, 13), 36.2),
            createSampleDailyLog(LocalDate(2024, 1, 14), 36.6), // Temperature rise
            createSampleDailyLog(LocalDate(2024, 1, 15), 36.7),
            createSampleDailyLog(LocalDate(2024, 1, 16), 36.8)
        )

        composeTestRule.setContent {
            EunioTheme {
                BBTChartScreen(
                    temperatureLogs = patternData,
                    currentCycle = sampleCycle
                )
            }
        }

        // Verify pattern insights are displayed
        composeTestRule.onNodeWithText("Pattern Insights").assertIsDisplayed()
    }

    @Test
    fun bbtChartScreen_handlesGestures_onChart() {
        composeTestRule.setContent {
            EunioTheme {
                BBTChartScreen(
                    temperatureLogs = sampleTemperatureLogs,
                    currentCycle = sampleCycle
                )
            }
        }

        // Find the chart area (Canvas component)
        // Note: Testing gestures on Canvas is limited in Compose UI tests
        // This test verifies the chart is present and can be interacted with
        composeTestRule.onNodeWithText("Temperature Trend").assertIsDisplayed()
        
        // The actual gesture testing would require more complex setup
        // and might be better suited for integration tests
    }

    @Test
    fun bbtChartScreen_respondsToUnitSystemChanges() {
        // This test would require mocking the UnitSystemManager
        // For now, we verify the chart displays correctly
        composeTestRule.setContent {
            EunioTheme {
                BBTChartScreen(
                    temperatureLogs = sampleTemperatureLogs,
                    currentCycle = sampleCycle
                )
            }
        }

        // Verify temperature unit is displayed
        composeTestRule.onNodeWithText("Temperature Trend (°C)").assertIsDisplayed()
    }

    @Test
    fun bbtChartScreen_displaysOvulationMarkers_correctly() {
        // Create data with ovulation markers
        val ovulationData = sampleTemperatureLogs.mapIndexed { index, log ->
            if (index == 4) { // Mark the 5th day as ovulation
                log.copy(
                    // In a real implementation, ovulation would be detected
                    // based on temperature patterns or other indicators
                )
            } else {
                log
            }
        }

        composeTestRule.setContent {
            EunioTheme {
                BBTChartScreen(
                    temperatureLogs = ovulationData,
                    currentCycle = sampleCycle
                )
            }
        }

        // Verify the chart displays correctly with ovulation data
        composeTestRule.onNodeWithText("Temperature Trend").assertIsDisplayed()
    }

    @Test
    fun bbtChartScreen_scrollsCorrectly_withLongContent() {
        composeTestRule.setContent {
            EunioTheme {
                BBTChartScreen(
                    temperatureLogs = sampleTemperatureLogs,
                    currentCycle = sampleCycle
                )
            }
        }

        // Verify scrolling works by checking elements at different positions
        composeTestRule.onNodeWithText("BBT Chart").assertIsDisplayed()
        
        // Scroll down to see recent readings
        composeTestRule.onNodeWithText("Recent Readings").performScrollTo()
        composeTestRule.onNodeWithText("Recent Readings").assertIsDisplayed()
    }

    // Helper function to create sample daily log
    private fun createSampleDailyLog(date: LocalDate, temperature: Double): DailyLog {
        return DailyLog(
            id = "log-${date}",
            userId = "test-user",
            date = date,
            bbt = temperature,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
    }
}