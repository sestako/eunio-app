package com.eunio.healthapp.android.ui.insights

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eunio.healthapp.android.ui.theme.EunioTheme
import com.eunio.healthapp.domain.model.Insight
import com.eunio.healthapp.domain.model.InsightType
import com.eunio.healthapp.presentation.state.InsightsUiState
import kotlinx.datetime.Clock
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InsightsDashboardScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun insightsDashboardScreen_displaysLoadingState() {
        val loadingState = InsightsUiState(isLoading = true)

        composeTestRule.setContent {
            EunioTheme {
                InsightsDashboardContent(
                    uiState = loadingState,
                    onDismissInsight = {},
                    onMarkAsRead = {},
                    onClearError = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("Loading your insights...")
            .assertIsDisplayed()
    }

    @Test
    fun insightsDashboardScreen_displaysErrorState() {
        val errorState = InsightsUiState(
            errorMessage = "Network error occurred"
        )

        composeTestRule.setContent {
            EunioTheme {
                InsightsDashboardContent(
                    uiState = errorState,
                    onDismissInsight = {},
                    onMarkAsRead = {},
                    onClearError = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("Unable to load insights")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Network error occurred")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Try Again")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun insightsDashboardScreen_displaysEmptyState() {
        val emptyState = InsightsUiState()

        composeTestRule.setContent {
            EunioTheme {
                InsightsDashboardContent(
                    uiState = emptyState,
                    onDismissInsight = {},
                    onMarkAsRead = {},
                    onClearError = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("No insights yet")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Keep logging your health data and we'll start generating personalized insights for you.")
            .assertIsDisplayed()
    }

    @Test
    fun insightsDashboardScreen_displaysUnreadInsights() {
        val unreadInsights = listOf(
            createTestInsight(
                id = "1",
                text = "Your cycle length has been consistent",
                type = InsightType.PATTERN_RECOGNITION,
                isRead = false
            ),
            createTestInsight(
                id = "2", 
                text = "Temperature spike detected",
                type = InsightType.EARLY_WARNING,
                isRead = false
            )
        )
        
        val stateWithUnreadInsights = InsightsUiState(
            unreadInsights = unreadInsights
        )

        composeTestRule.setContent {
            EunioTheme {
                InsightsDashboardContent(
                    uiState = stateWithUnreadInsights,
                    onDismissInsight = {},
                    onMarkAsRead = {},
                    onClearError = {}
                )
            }
        }

        // Check section header
        composeTestRule
            .onNodeWithText("New Insights")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("2 new")
            .assertIsDisplayed()

        // Check insights are displayed
        composeTestRule
            .onNodeWithText("Your cycle length has been consistent")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Temperature spike detected")
            .assertIsDisplayed()
    }

    @Test
    fun insightsDashboardScreen_displaysReadInsights() {
        val readInsights = listOf(
            createTestInsight(
                id = "3",
                text = "Previous cycle prediction was accurate",
                type = InsightType.CYCLE_PREDICTION,
                isRead = true
            )
        )
        
        val stateWithReadInsights = InsightsUiState(
            readInsights = readInsights
        )

        composeTestRule.setContent {
            EunioTheme {
                InsightsDashboardContent(
                    uiState = stateWithReadInsights,
                    onDismissInsight = {},
                    onMarkAsRead = {},
                    onClearError = {}
                )
            }
        }

        // Check section header
        composeTestRule
            .onNodeWithText("Previous Insights")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("1 insights")
            .assertIsDisplayed()

        // Check insight is displayed
        composeTestRule
            .onNodeWithText("Previous cycle prediction was accurate")
            .assertIsDisplayed()
    }

    @Test
    fun insightsDashboardScreen_displaysMedicalDisclaimer() {
        val stateWithInsights = InsightsUiState(
            unreadInsights = listOf(
                createTestInsight(
                    id = "1",
                    text = "Test insight",
                    type = InsightType.PATTERN_RECOGNITION
                )
            )
        )

        composeTestRule.setContent {
            EunioTheme {
                InsightsDashboardContent(
                    uiState = stateWithInsights,
                    onDismissInsight = {},
                    onMarkAsRead = {},
                    onClearError = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("Medical Disclaimer")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("These insights are for informational purposes only and should not replace professional medical advice. Always consult with your healthcare provider for medical concerns.")
            .assertIsDisplayed()
    }

    @Test
    fun insightsDashboardScreen_handlesInsightDismissal() {
        var dismissedInsightId: String? = null
        
        val stateWithInsights = InsightsUiState(
            unreadInsights = listOf(
                createTestInsight(
                    id = "dismissible",
                    text = "Dismissible insight",
                    type = InsightType.PATTERN_RECOGNITION
                )
            )
        )

        composeTestRule.setContent {
            EunioTheme {
                InsightsDashboardContent(
                    uiState = stateWithInsights,
                    onDismissInsight = { dismissedInsightId = it },
                    onMarkAsRead = {},
                    onClearError = {}
                )
            }
        }

        // Find and click dismiss button
        composeTestRule
            .onNodeWithContentDescription("Dismiss insight")
            .assertIsDisplayed()
            .performClick()

        assert(dismissedInsightId == "dismissible")
    }

    @Test
    fun insightsDashboardScreen_handlesMarkAsRead() {
        var markedAsReadId: String? = null
        
        val stateWithInsights = InsightsUiState(
            unreadInsights = listOf(
                createTestInsight(
                    id = "readable",
                    text = "Readable insight",
                    type = InsightType.PATTERN_RECOGNITION
                )
            )
        )

        composeTestRule.setContent {
            EunioTheme {
                InsightsDashboardContent(
                    uiState = stateWithInsights,
                    onDismissInsight = {},
                    onMarkAsRead = { markedAsReadId = it },
                    onClearError = {}
                )
            }
        }

        // Click on the insight card to mark as read
        composeTestRule
            .onNodeWithText("Readable insight")
            .assertIsDisplayed()
            .performClick()

        assert(markedAsReadId == "readable")
    }

    @Test
    fun insightsDashboardScreen_refreshButtonIsDisplayed() {
        val stateWithInsights = InsightsUiState(
            unreadInsights = listOf(
                createTestInsight(
                    id = "1",
                    text = "Test insight",
                    type = InsightType.PATTERN_RECOGNITION
                )
            )
        )

        composeTestRule.setContent {
            EunioTheme {
                InsightsDashboardScreen()
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Refresh insights")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    private fun createTestInsight(
        id: String,
        text: String,
        type: InsightType,
        isRead: Boolean = false,
        confidence: Double = 0.85,
        actionable: Boolean = false
    ) = Insight(
        id = id,
        userId = "test-user",
        generatedDate = Clock.System.now(),
        insightText = text,
        type = type,
        isRead = isRead,
        relatedLogIds = emptyList(),
        confidence = confidence,
        actionable = actionable
    )
}