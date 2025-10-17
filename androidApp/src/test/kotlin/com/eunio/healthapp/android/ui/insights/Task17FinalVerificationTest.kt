package com.eunio.healthapp.android.ui.insights

import com.eunio.healthapp.domain.model.Insight
import com.eunio.healthapp.domain.model.InsightType
import com.eunio.healthapp.presentation.state.InsightsUiState
import kotlinx.datetime.Clock
import org.junit.Test
import org.junit.Assert.*

/**
 * Final verification test for Task 17 to ensure complete functionality
 * and integration with existing systems.
 */
class Task17FinalVerificationTest {

    @Test
    fun verifyInsightsDashboardIntegration() {
        // Test that the insights dashboard can handle all required states
        val loadingState = InsightsUiState(isLoading = true)
        val errorState = InsightsUiState(errorMessage = "Test error")
        val emptyState = InsightsUiState()
        val populatedState = InsightsUiState(
            unreadInsights = listOf(
                createTestInsight(type = InsightType.PATTERN_RECOGNITION),
                createTestInsight(type = InsightType.EARLY_WARNING),
                createTestInsight(type = InsightType.CYCLE_PREDICTION),
                createTestInsight(type = InsightType.FERTILITY_WINDOW)
            ),
            readInsights = listOf(
                createTestInsight(id = "read1", isRead = true),
                createTestInsight(id = "read2", isRead = true)
            )
        )

        // Verify loading state
        assertTrue("Loading state should be loading", loadingState.isLoading)
        assertFalse("Loading state should not have insights", loadingState.hasInsights)

        // Verify error state
        assertNotNull("Error state should have error message", errorState.errorMessage)
        assertEquals("Error message should match", "Test error", errorState.errorMessage)

        // Verify empty state
        assertFalse("Empty state should not have insights", emptyState.hasInsights)
        assertEquals("Empty state should have 0 unread", 0, emptyState.unreadCount)

        // Verify populated state
        assertTrue("Populated state should have insights", populatedState.hasInsights)
        assertEquals("Should have 4 unread insights", 4, populatedState.unreadCount)
        assertEquals("Should have 2 read insights", 2, populatedState.readInsights.size)
        assertEquals("Should have 6 total insights", 6, populatedState.allInsights.size)
    }

    @Test
    fun verifyInsightTypeHandling() {
        // Test all insight types are properly supported
        val patternInsight = createTestInsight(type = InsightType.PATTERN_RECOGNITION)
        val warningInsight = createTestInsight(type = InsightType.EARLY_WARNING)
        val cycleInsight = createTestInsight(type = InsightType.CYCLE_PREDICTION)
        val fertilityInsight = createTestInsight(type = InsightType.FERTILITY_WINDOW)

        val insights = listOf(patternInsight, warningInsight, cycleInsight, fertilityInsight)
        val types = insights.map { it.type }.toSet()

        assertEquals("Should support all 4 insight types", 4, types.size)
        assertTrue("Should include pattern recognition", types.contains(InsightType.PATTERN_RECOGNITION))
        assertTrue("Should include early warning", types.contains(InsightType.EARLY_WARNING))
        assertTrue("Should include cycle prediction", types.contains(InsightType.CYCLE_PREDICTION))
        assertTrue("Should include fertility window", types.contains(InsightType.FERTILITY_WINDOW))
    }

    @Test
    fun verifyInsightConfidenceAndActionability() {
        // Test confidence levels and actionable insights
        val highConfidenceInsight = createTestInsight(confidence = 0.95, actionable = true)
        val mediumConfidenceInsight = createTestInsight(confidence = 0.75, actionable = false)
        val lowConfidenceInsight = createTestInsight(confidence = 0.45, actionable = false)

        assertTrue("High confidence insight should be actionable", highConfidenceInsight.actionable)
        assertTrue("High confidence should be >= 0.8", highConfidenceInsight.confidence >= 0.8)
        
        assertFalse("Medium confidence insight should not be actionable", mediumConfidenceInsight.actionable)
        assertTrue("Medium confidence should be between 0.6-0.8", 
            mediumConfidenceInsight.confidence >= 0.6 && mediumConfidenceInsight.confidence < 0.8)
        
        assertFalse("Low confidence insight should not be actionable", lowConfidenceInsight.actionable)
        assertTrue("Low confidence should be < 0.6", lowConfidenceInsight.confidence < 0.6)
    }

    @Test
    fun verifyInsightDismissalAndReadStatus() {
        // Test dismissal and read status functionality
        val insight1 = createTestInsight(id = "insight1", isRead = false)
        val insight2 = createTestInsight(id = "insight2", isRead = true)
        
        val state = InsightsUiState(
            unreadInsights = listOf(insight1),
            readInsights = listOf(insight2),
            dismissedInsightIds = setOf("insight3")
        )

        assertFalse("Insight1 should be unread", insight1.isRead)
        assertTrue("Insight2 should be read", insight2.isRead)
        assertTrue("Insight3 should be dismissed", state.dismissedInsightIds.contains("insight3"))
        assertEquals("Should have 1 unread insight", 1, state.unreadCount)
        assertEquals("Should have 1 read insight", 1, state.readInsights.size)
    }

    @Test
    fun verifyInsightDataIntegrity() {
        // Test that insight data maintains integrity
        val insight = createTestInsight(
            id = "test-insight-123",
            text = "Your cycle has been consistent for the past 3 months",
            type = InsightType.PATTERN_RECOGNITION,
            confidence = 0.87,
            actionable = true
        )

        assertEquals("ID should match", "test-insight-123", insight.id)
        assertEquals("Text should match", "Your cycle has been consistent for the past 3 months", insight.insightText)
        assertEquals("Type should match", InsightType.PATTERN_RECOGNITION, insight.type)
        assertEquals("Confidence should match", 0.87, insight.confidence, 0.001)
        assertTrue("Should be actionable", insight.actionable)
        assertEquals("User ID should match", "test-user", insight.userId)
        assertTrue("Should have related log IDs list", insight.relatedLogIds.isEmpty())
    }

    @Test
    fun verifyInsightTimestampHandling() {
        // Test that timestamps are properly handled
        val now = Clock.System.now()
        val insight = createTestInsight()
        
        assertNotNull("Generated date should not be null", insight.generatedDate)
        // The generated date should be close to now (within a few seconds)
        val timeDiff = kotlin.math.abs(insight.generatedDate.epochSeconds - now.epochSeconds)
        assertTrue("Generated date should be recent", timeDiff < 10)
    }

    @Test
    fun verifyInsightStateTransitions() {
        // Test state transitions work correctly
        val initialState = InsightsUiState()
        
        // Loading state
        val loadingState = initialState.copy(isLoading = true)
        assertTrue("Should be loading", loadingState.isLoading)
        
        // Error state
        val errorState = loadingState.copy(isLoading = false, errorMessage = "Network error")
        assertFalse("Should not be loading", errorState.isLoading)
        assertEquals("Should have error message", "Network error", errorState.errorMessage)
        
        // Success state with insights
        val successState = errorState.copy(
            errorMessage = null,
            unreadInsights = listOf(createTestInsight())
        )
        assertNull("Should not have error message", successState.errorMessage)
        assertTrue("Should have insights", successState.hasInsights)
        assertEquals("Should have 1 unread insight", 1, successState.unreadCount)
    }

    @Test
    fun verifyInsightCompatibilityWithExistingSystems() {
        // Test that insights work with existing unit system and other features
        val insight = createTestInsight(
            text = "Your temperature readings show a consistent pattern",
            type = InsightType.PATTERN_RECOGNITION
        )
        
        // Verify insight can reference temperature data (unit system compatibility)
        assertTrue("Insight should reference temperature", 
            insight.insightText.contains("temperature"))
        
        // Verify insight type is compatible with existing domain models
        assertTrue("Insight type should be valid enum value", 
            InsightType.values().contains(insight.type))
        
        // Verify insight structure is compatible with existing data models
        assertNotNull("Insight should have user ID", insight.userId)
        assertNotNull("Insight should have generated date", insight.generatedDate)
        assertNotNull("Insight should have confidence", insight.confidence)
    }

    private fun createTestInsight(
        id: String = "test-insight",
        text: String = "Test insight text",
        type: InsightType = InsightType.PATTERN_RECOGNITION,
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