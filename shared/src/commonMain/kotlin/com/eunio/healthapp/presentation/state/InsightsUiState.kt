package com.eunio.healthapp.presentation.state

import com.eunio.healthapp.domain.model.Insight
import com.eunio.healthapp.presentation.viewmodel.UiState

/**
 * UI state for insights screen.
 */
data class InsightsUiState(
    val unreadInsights: List<Insight> = emptyList(),
    val readInsights: List<Insight> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val dismissedInsightIds: Set<String> = emptySet()
) : UiState {
    
    /**
     * All insights combined.
     */
    val allInsights: List<Insight>
        get() = unreadInsights + readInsights
    
    /**
     * Whether there are any insights to display.
     */
    val hasInsights: Boolean
        get() = allInsights.isNotEmpty()
    
    /**
     * Number of unread insights.
     */
    val unreadCount: Int
        get() = unreadInsights.size
}