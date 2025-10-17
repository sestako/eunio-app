package com.eunio.healthapp.android.ui.insights

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eunio.healthapp.android.ui.theme.EunioTheme
import com.eunio.healthapp.domain.model.Insight
import com.eunio.healthapp.domain.model.InsightType
import kotlinx.datetime.Clock
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InsightCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun insightCard_displaysInsightText() {
        val insight = createTestInsight(
            text = "Your cycle length has been consistent at 28 days"
        )

        composeTestRule.setContent {
            EunioTheme {
                InsightCard(
                    insight = insight,
                    isRead = false,
                    onDismiss = {},
                    onMarkAsRead = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("Your cycle length has been consistent at 28 days")
            .assertIsDisplayed()
    }

    @Test
    fun insightCard_displaysInsightType() {
        val insight = createTestInsight(
            type = InsightType.PATTERN_RECOGNITION
        )

        composeTestRule.setContent {
            EunioTheme {
                InsightCard(
                    insight = insight,
                    isRead = false,
                    onDismiss = {},
                    onMarkAsRead = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("Pattern Recognition")
            .assertIsDisplayed()
    }

    @Test
    fun insightCard_displaysConfidenceLevel() {
        val insight = createTestInsight(
            confidence = 0.87
        )

        composeTestRule.setContent {
            EunioTheme {
                InsightCard(
                    insight = insight,
                    isRead = false,
                    onDismiss = {},
                    onMarkAsRead = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("87% confidence")
            .assertIsDisplayed()
    }

    @Test
    fun insightCard_displaysActionableBadge_whenActionable() {
        val insight = createTestInsight(
            actionable = true
        )

        composeTestRule.setContent {
            EunioTheme {
                InsightCard(
                    insight = insight,
                    isRead = false,
                    onDismiss = {},
                    onMarkAsRead = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("Actionable")
            .assertIsDisplayed()
    }

    @Test
    fun insightCard_hidesActionableBadge_whenNotActionable() {
        val insight = createTestInsight(
            actionable = false
        )

        composeTestRule.setContent {
            EunioTheme {
                InsightCard(
                    insight = insight,
                    isRead = false,
                    onDismiss = {},
                    onMarkAsRead = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("Actionable")
            .assertDoesNotExist()
    }

    @Test
    fun insightCard_displaysDismissButton() {
        val insight = createTestInsight()

        composeTestRule.setContent {
            EunioTheme {
                InsightCard(
                    insight = insight,
                    isRead = false,
                    onDismiss = {},
                    onMarkAsRead = {}
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Dismiss insight")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun insightCard_handlesDismissClick() {
        var dismissCalled = false
        val insight = createTestInsight()

        composeTestRule.setContent {
            EunioTheme {
                InsightCard(
                    insight = insight,
                    isRead = false,
                    onDismiss = { dismissCalled = true },
                    onMarkAsRead = {}
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Dismiss insight")
            .performClick()

        assert(dismissCalled)
    }

    @Test
    fun insightCard_handlesMarkAsReadClick_whenUnread() {
        var markAsReadCalled = false
        val insight = createTestInsight()

        composeTestRule.setContent {
            EunioTheme {
                InsightCard(
                    insight = insight,
                    isRead = false,
                    onDismiss = {},
                    onMarkAsRead = { markAsReadCalled = true }
                )
            }
        }

        // Click on the card itself
        composeTestRule
            .onNodeWithText(insight.insightText)
            .performClick()

        assert(markAsReadCalled)
    }

    @Test
    fun insightCard_doesNotCallMarkAsRead_whenAlreadyRead() {
        var markAsReadCalled = false
        val insight = createTestInsight()

        composeTestRule.setContent {
            EunioTheme {
                InsightCard(
                    insight = insight,
                    isRead = true,
                    onDismiss = {},
                    onMarkAsRead = { markAsReadCalled = true }
                )
            }
        }

        // Click on the card
        composeTestRule
            .onNodeWithText(insight.insightText)
            .performClick()

        assert(!markAsReadCalled)
    }

    @Test
    fun insightCard_displaysDateFormatted() {
        val insight = createTestInsight()

        composeTestRule.setContent {
            EunioTheme {
                InsightCard(
                    insight = insight,
                    isRead = false,
                    onDismiss = {},
                    onMarkAsRead = {}
                )
            }
        }

        // Should display "Today" for current date
        composeTestRule
            .onNodeWithText("Today")
            .assertIsDisplayed()
    }

    @Test
    fun insightCard_displaysEarlyWarningType() {
        val insight = createTestInsight(
            type = InsightType.EARLY_WARNING,
            text = "Unusual temperature pattern detected"
        )

        composeTestRule.setContent {
            EunioTheme {
                InsightCard(
                    insight = insight,
                    isRead = false,
                    onDismiss = {},
                    onMarkAsRead = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("Health Alert")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Unusual temperature pattern detected")
            .assertIsDisplayed()
    }

    @Test
    fun insightCard_displaysCyclePredictionType() {
        val insight = createTestInsight(
            type = InsightType.CYCLE_PREDICTION,
            text = "Your next period is predicted for March 15"
        )

        composeTestRule.setContent {
            EunioTheme {
                InsightCard(
                    insight = insight,
                    isRead = false,
                    onDismiss = {},
                    onMarkAsRead = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("Cycle Prediction")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Your next period is predicted for March 15")
            .assertIsDisplayed()
    }

    @Test
    fun insightCard_displaysFertilityWindowType() {
        val insight = createTestInsight(
            type = InsightType.FERTILITY_WINDOW,
            text = "You are entering your fertile window"
        )

        composeTestRule.setContent {
            EunioTheme {
                InsightCard(
                    insight = insight,
                    isRead = false,
                    onDismiss = {},
                    onMarkAsRead = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("Fertility Window")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("You are entering your fertile window")
            .assertIsDisplayed()
    }

    @Test
    fun insightCard_displaysHighConfidenceBadge() {
        val insight = createTestInsight(
            confidence = 0.95
        )

        composeTestRule.setContent {
            EunioTheme {
                InsightCard(
                    insight = insight,
                    isRead = false,
                    onDismiss = {},
                    onMarkAsRead = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("95% confidence")
            .assertIsDisplayed()
    }

    @Test
    fun insightCard_displaysLowConfidenceBadge() {
        val insight = createTestInsight(
            confidence = 0.45
        )

        composeTestRule.setContent {
            EunioTheme {
                InsightCard(
                    insight = insight,
                    isRead = false,
                    onDismiss = {},
                    onMarkAsRead = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("45% confidence")
            .assertIsDisplayed()
    }

    private fun createTestInsight(
        id: String = "test-insight",
        text: String = "Test insight text",
        type: InsightType = InsightType.PATTERN_RECOGNITION,
        confidence: Double = 0.85,
        actionable: Boolean = false
    ) = Insight(
        id = id,
        userId = "test-user",
        generatedDate = Clock.System.now(),
        insightText = text,
        type = type,
        isRead = false,
        relatedLogIds = emptyList(),
        confidence = confidence,
        actionable = actionable
    )
}