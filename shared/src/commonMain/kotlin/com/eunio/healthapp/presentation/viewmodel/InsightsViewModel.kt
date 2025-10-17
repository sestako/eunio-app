package com.eunio.healthapp.presentation.viewmodel

import com.eunio.healthapp.domain.model.Insight
import com.eunio.healthapp.domain.repository.InsightRepository
import com.eunio.healthapp.presentation.state.InsightsUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing insights functionality.
 */
class InsightsViewModel(
    private val insightRepository: InsightRepository,
    dispatcher: kotlinx.coroutines.CoroutineDispatcher = kotlinx.coroutines.Dispatchers.Main
) : BaseViewModel<InsightsUiState>(dispatcher) {
    
    override val initialState = InsightsUiState()
    
    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages.asSharedFlow()
    
    init {
        loadInsights()
    }
    
    /**
     * Loads all insights (unread and read).
     */
    private fun loadInsights() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                // Load unread insights
                val unreadInsights = insightRepository.getUnreadInsights("current_user")
                    .getOrNull() ?: emptyList()
                
                // Load insight history (read insights)
                val readInsights = insightRepository.getInsightHistory("current_user", limit = 50)
                    .getOrNull()?.filter { it.isRead } ?: emptyList()
                
                updateState { 
                    it.copy(
                        isLoading = false,
                        unreadInsights = unreadInsights,
                        readInsights = readInsights
                    )
                }
            } catch (e: Exception) {
                updateState { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load insights: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Refreshes insights from the server.
     */
    fun refresh() {
        viewModelScope.launch {
            updateState { it.copy(isRefreshing = true, errorMessage = null) }
            
            try {
                // Load unread insights
                val unreadInsights = insightRepository.getUnreadInsights("current_user")
                    .getOrNull() ?: emptyList()
                
                // Load insight history (read insights)
                val readInsights = insightRepository.getInsightHistory("current_user", limit = 50)
                    .getOrNull()?.filter { it.isRead } ?: emptyList()
                
                updateState { 
                    it.copy(
                        isRefreshing = false,
                        unreadInsights = unreadInsights,
                        readInsights = readInsights
                    )
                }
                
                _messages.emit("Insights refreshed")
            } catch (e: Exception) {
                updateState { 
                    it.copy(
                        isRefreshing = false,
                        errorMessage = "Failed to refresh insights: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Marks an insight as read.
     */
    fun markInsightAsRead(insightId: String) {
        viewModelScope.launch {
            insightRepository.markInsightAsRead(insightId)
                .onSuccess {
                    // Move insight from unread to read list
                    val currentState = uiState.value
                    val insight = currentState.unreadInsights.find { it.id == insightId }
                    
                    if (insight != null) {
                        val updatedInsight = insight.copy(isRead = true)
                        val newUnreadInsights = currentState.unreadInsights.filter { it.id != insightId }
                        val newReadInsights = listOf(updatedInsight) + currentState.readInsights
                        
                        updateState { 
                            it.copy(
                                unreadInsights = newUnreadInsights,
                                readInsights = newReadInsights
                            )
                        }
                    }
                }
                .onError { error ->
                    updateState { 
                        it.copy(errorMessage = "Failed to mark insight as read: ${error.message}")
                    }
                }
        }
    }
    
    /**
     * Dismisses an insight (marks as read and adds to dismissed set).
     */
    fun dismissInsight(insightId: String) {
        viewModelScope.launch {
            // First mark as read
            markInsightAsRead(insightId)
            
            // Add to dismissed set for UI purposes
            updateState { 
                it.copy(dismissedInsightIds = it.dismissedInsightIds + insightId)
            }
            
            _messages.emit("Insight dismissed")
        }
    }
    
    /**
     * Gets insights filtered by type.
     */
    fun getInsightsByType(type: com.eunio.healthapp.domain.model.InsightType): List<Insight> {
        return uiState.value.allInsights.filter { it.type == type }
    }
    
    /**
     * Gets actionable insights only.
     */
    fun getActionableInsights(): List<Insight> {
        return uiState.value.allInsights.filter { it.actionable }
    }
    
    /**
     * Gets insights with high confidence (>= 0.8).
     */
    fun getHighConfidenceInsights(): List<Insight> {
        return uiState.value.allInsights.filter { it.confidence >= 0.8 }
    }
    
    /**
     * Searches insights by text content.
     */
    fun searchInsights(query: String): List<Insight> {
        if (query.isBlank()) return uiState.value.allInsights
        
        val lowercaseQuery = query.lowercase()
        return uiState.value.allInsights.filter { insight ->
            insight.insightText.lowercase().contains(lowercaseQuery) ||
            insight.type.name.lowercase().contains(lowercaseQuery)
        }
    }
    
    /**
     * Clears any error messages.
     */
    fun clearError() {
        updateState { it.copy(errorMessage = null) }
    }
}