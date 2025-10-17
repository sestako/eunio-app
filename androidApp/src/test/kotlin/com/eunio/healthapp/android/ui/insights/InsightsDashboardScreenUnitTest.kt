package com.eunio.healthapp.android.ui.insights

import com.eunio.healthapp.domain.model.Insight
import com.eunio.healthapp.domain.model.InsightType
import com.eunio.healthapp.presentation.state.InsightsUiState
import kotlinx.datetime.Clock
import org.junit.Test
import org.junit.Assert.*

class InsightsDashboardScreenUnitTest {

    @Test
    fun insightsUiState_showsCorrectUnreadCount() {
        val unreadInsights = listOf(
            createTestInsight(id = "1"),
            createTestInsight(id = "2"),
            createTestInsight(id = "3")
        )
        
        val state = InsightsUiState(unreadInsights = unreadInsights)

        assertEquals(3, state.unreadCount)
    }

    @Test
    fun insightsUiState_showsCorrectReadCount() {
        val readInsights = listOf(
            createTestInsight(id = "1", isRead = true),
            createTestInsight(id = "2", isRead = true)
        )
        
        val state = InsightsUiState(readInsights = readInsights)

        assertEquals(2, state.readInsights.size)
    }

    @Test
    fun insightsUiState_combinesAllInsights() {
        val unreadInsights = listOf(createTestInsight(id = "unread"))
        val readInsights = listOf(createTestInsight(id = "read", isRead = true))
        
        val state = InsightsUiState(
            unreadInsights = unreadInsights,
            readInsights = readInsights
        )

        assertEquals(2, state.allInsights.size)
        assertTrue(state.hasInsights)
    }

    @Test
    fun insightsUiState_hasInsights_returnsFalse_whenEmpty() {
        val state = InsightsUiState()

        assertFalse(state.hasInsights)
        assertEquals(0, state.unreadCount)
    }

    @Test
    fun insightsUiState_hasInsights_returnsTrue_whenHasInsights() {
        val state = InsightsUiState(
            unreadInsights = listOf(createTestInsight(id = "test"))
        )

        assertTrue(state.hasInsights)
        assertEquals(1, state.unreadCount)
    }

    @Test
    fun insightsUiState_dismissedInsights_areTracked() {
        val state = InsightsUiState(
            dismissedInsightIds = setOf("dismissed1", "dismissed2")
        )

        assertTrue(state.dismissedInsightIds.contains("dismissed1"))
        assertTrue(state.dismissedInsightIds.contains("dismissed2"))
        assertEquals(2, state.dismissedInsightIds.size)
    }

    @Test
    fun insightsUiState_loadingState_isTracked() {
        val loadingState = InsightsUiState(isLoading = true)
        val notLoadingState = InsightsUiState(isLoading = false)

        assertTrue(loadingState.isLoading)
        assertFalse(notLoadingState.isLoading)
    }

    @Test
    fun insightsUiState_errorMessage_isTracked() {
        val errorState = InsightsUiState(errorMessage = "Test error")
        val noErrorState = InsightsUiState(errorMessage = null)

        assertEquals("Test error", errorState.errorMessage)
        assertNull(noErrorState.errorMessage)
    }

    private fun createTestInsight(
        id: String,
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