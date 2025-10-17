package com.eunio.healthapp.presentation.viewmodel

import com.eunio.healthapp.domain.model.support.*
import com.eunio.healthapp.domain.usecase.support.*
import com.eunio.healthapp.presentation.state.HelpSupportUiState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HelpSupportViewModel(
    private val getHelpCategoriesUseCase: GetHelpCategoriesUseCase,
    private val searchFAQsUseCase: SearchFAQsUseCase,
    private val getTutorialsUseCase: GetTutorialsUseCase
) : BaseViewModel<HelpSupportUiState>() {

    override val initialState = HelpSupportUiState()

    init {
        loadHelpCategories()
        loadTutorials()
    }

    fun updateSearchQuery(query: String) {
        updateState { it.copy(searchQuery = query) }
        if (query.isNotBlank()) {
            searchFAQs(query)
        } else {
            updateState { it.copy(searchResults = emptyList()) }
        }
    }

    fun selectCategory(category: HelpCategory) {
        updateState { 
            it.copy(
                selectedCategory = category,
                searchQuery = "",
                searchResults = emptyList()
            ) 
        }
    }

    fun selectTutorial(tutorial: Tutorial) {
        updateState { it.copy(selectedTutorial = tutorial) }
    }

    fun toggleFAQExpansion(faqId: String) {
        updateState { state ->
            val updatedCategories = state.helpCategories.map { category ->
                category.copy(
                    faqs = category.faqs.map { faq ->
                        if (faq.id == faqId) {
                            faq.copy(isExpanded = !faq.isExpanded)
                        } else {
                            faq
                        }
                    }
                )
            }
            
            val updatedSearchResults = state.searchResults.map { faq ->
                if (faq.id == faqId) {
                    faq.copy(isExpanded = !faq.isExpanded)
                } else {
                    faq
                }
            }
            
            state.copy(
                helpCategories = updatedCategories,
                searchResults = updatedSearchResults
            )
        }
    }

    fun clearError() {
        updateState { it.copy(errorMessage = null) }
    }

    private fun loadHelpCategories() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }
            
            getHelpCategoriesUseCase()
                .onSuccess { categories ->
                    updateState { 
                        it.copy(
                            helpCategories = categories,
                            isLoading = false
                        ) 
                    }
                }
                .onFailure { error ->
                    updateState { 
                        it.copy(
                            errorMessage = error.message ?: "Failed to load help categories",
                            isLoading = false
                        ) 
                    }
                }
        }
    }

    private fun loadTutorials() {
        viewModelScope.launch {
            getTutorialsUseCase()
                .onSuccess { tutorials ->
                    updateState { it.copy(tutorials = tutorials) }
                }
                .onFailure { error ->
                    updateState { 
                        it.copy(errorMessage = error.message ?: "Failed to load tutorials") 
                    }
                }
        }
    }

    private fun searchFAQs(query: String) {
        viewModelScope.launch {
            searchFAQsUseCase(query)
                .onSuccess { results ->
                    updateState { it.copy(searchResults = results) }
                }
                .onFailure { error ->
                    updateState { 
                        it.copy(errorMessage = error.message ?: "Search failed") 
                    }
                }
        }
    }
}