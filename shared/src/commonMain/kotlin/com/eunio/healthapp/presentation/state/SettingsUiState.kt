package com.eunio.healthapp.presentation.state

import com.eunio.healthapp.domain.model.settings.UserSettings
import com.eunio.healthapp.presentation.viewmodel.UiState

/**
 * UI state for the main settings screen.
 */
data class SettingsUiState(
    val settings: UserSettings? = null,
    val searchQuery: String = "",
    val filteredSections: List<SettingSection> = emptyList(),
    val loadingState: LoadingState = LoadingState.Idle,
    val isRefreshing: Boolean = false,
    val syncStatus: Boolean = false
) : UiState {
    
    /**
     * Whether the settings are currently loading.
     */
    val isLoading: Boolean
        get() = loadingState is LoadingState.Loading
    
    /**
     * Whether the settings are enabled for interaction.
     */
    val isEnabled: Boolean
        get() = !isLoading && !isRefreshing
    
    /**
     * Error message if loading failed.
     */
    val errorMessage: String?
        get() = (loadingState as? LoadingState.Error)?.message
    
    /**
     * Whether search is active.
     */
    val isSearching: Boolean
        get() = searchQuery.isNotBlank()
    
    /**
     * Whether settings have been loaded successfully.
     */
    val hasSettings: Boolean
        get() = settings != null && loadingState is LoadingState.Success<*>
}

/**
 * Represents a settings section for organization and search.
 */
data class SettingSection(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: String,
    val items: List<SettingItem> = emptyList(),
    val isVisible: Boolean = true
)

/**
 * Represents an individual setting item.
 */
data class SettingItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val keywords: List<String> = emptyList(),
    val isVisible: Boolean = true
)