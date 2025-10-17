package com.eunio.healthapp.presentation.state

import com.eunio.healthapp.domain.model.support.*
import com.eunio.healthapp.presentation.viewmodel.UiState

data class HelpSupportUiState(
    val isLoading: Boolean = false,
    val helpCategories: List<HelpCategory> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<FAQ> = emptyList(),
    val selectedCategory: HelpCategory? = null,
    val tutorials: List<Tutorial> = emptyList(),
    val selectedTutorial: Tutorial? = null,
    val errorMessage: String? = null
) : UiState