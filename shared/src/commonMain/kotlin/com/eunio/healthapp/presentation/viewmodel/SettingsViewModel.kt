package com.eunio.healthapp.presentation.viewmodel

import com.eunio.healthapp.domain.manager.SettingsManager
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.presentation.state.LoadingState
import com.eunio.healthapp.presentation.state.SettingItem
import com.eunio.healthapp.presentation.state.SettingSection
import com.eunio.healthapp.presentation.state.SettingsUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * ViewModel for the main settings screen with search functionality.
 * Manages settings state, search filtering, and navigation to sub-settings.
 */
class SettingsViewModel(
    private val settingsManager: SettingsManager
) : BaseViewModel<SettingsUiState>() {
    
    override val initialState = SettingsUiState()
    
    private val _showSuccessMessage = MutableSharedFlow<String>()
    val showSuccessMessage: SharedFlow<String> = _showSuccessMessage.asSharedFlow()
    
    private val _showErrorMessage = MutableSharedFlow<String>()
    val showErrorMessage: SharedFlow<String> = _showErrorMessage.asSharedFlow()
    
    private val _navigateToSetting = MutableSharedFlow<String>()
    val navigateToSetting: SharedFlow<String> = _navigateToSetting.asSharedFlow()
    
    private val allSettingSections = listOf(
        SettingSection(
            id = "health",
            title = "Health Settings",
            subtitle = "Customize your health tracking preferences",
            icon = "health",
            items = listOf(
                SettingItem(
                    id = "units",
                    title = "Units",
                    subtitle = "Temperature and weight units",
                    keywords = listOf("temperature", "weight", "celsius", "fahrenheit", "kg", "lbs", "units", "measurement")
                ),
                SettingItem(
                    id = "notifications",
                    title = "Notifications",
                    subtitle = "Reminders and alerts",
                    keywords = listOf("notifications", "reminders", "alerts", "period", "ovulation", "logging")
                ),
                SettingItem(
                    id = "cycle",
                    title = "Cycle Settings",
                    subtitle = "Cycle length and period duration",
                    keywords = listOf("cycle", "period", "length", "duration", "luteal", "phase", "prediction")
                )
            )
        ),
        SettingSection(
            id = "privacy",
            title = "Privacy & Security",
            subtitle = "Control your data and privacy settings",
            icon = "privacy",
            items = listOf(
                SettingItem(
                    id = "privacy",
                    title = "Privacy",
                    subtitle = "Data protection and sharing settings",
                    keywords = listOf("privacy", "data", "sharing", "protection", "anonymous", "analytics")
                ),
                SettingItem(
                    id = "sync",
                    title = "Data Sync",
                    subtitle = "Cloud backup and synchronization",
                    keywords = listOf("sync", "backup", "cloud", "synchronization", "devices")
                )
            )
        ),
        SettingSection(
            id = "accessibility",
            title = "Accessibility & Display",
            subtitle = "Customize your app experience",
            icon = "accessibility",
            items = listOf(
                SettingItem(
                    id = "display",
                    title = "Display",
                    subtitle = "Text size, contrast, and haptic feedback",
                    keywords = listOf("display", "text", "size", "contrast", "haptic", "feedback", "accessibility")
                )
            )
        ),
        SettingSection(
            id = "support",
            title = "Help & Support",
            subtitle = "Get help and contact support",
            icon = "support",
            items = listOf(
                SettingItem(
                    id = "help",
                    title = "Help & FAQ",
                    subtitle = "Frequently asked questions and tutorials",
                    keywords = listOf("help", "faq", "questions", "tutorials", "support", "guide")
                ),
                SettingItem(
                    id = "contact",
                    title = "Contact Support",
                    subtitle = "Get in touch with our support team",
                    keywords = listOf("contact", "support", "help", "bug", "report", "feedback")
                )
            )
        ),
        SettingSection(
            id = "account",
            title = "Account",
            subtitle = "Manage your profile and account settings",
            icon = "account",
            items = listOf(
                SettingItem(
                    id = "profile",
                    title = "Profile",
                    subtitle = "Edit your profile information",
                    keywords = listOf("profile", "name", "email", "account", "information", "goals")
                )
            )
        )
    )
    
    init {
        loadSettings()
        observeSettingsChanges()
        observeSyncStatus()
        updateFilteredSections("")
    }
    
    /**
     * Loads the current user settings.
     */
    private fun loadSettings() {
        viewModelScope.launch {
            updateState { it.copy(loadingState = LoadingState.Loading) }
            
            settingsManager.getUserSettings()
                .onSuccess { settings ->
                    updateState { 
                        it.copy(
                            settings = settings,
                            loadingState = LoadingState.Success(settings)
                        )
                    }
                }
                .onError { error ->
                    updateState { 
                        it.copy(
                            loadingState = LoadingState.Error("Failed to load settings: ${error.message}")
                        )
                    }
                    _showErrorMessage.emit("Failed to load settings")
                }
        }
    }
    
    /**
     * Observes settings changes for reactive updates.
     */
    private fun observeSettingsChanges() {
        viewModelScope.launch {
            settingsManager.observeSettingsChanges()
                .catch { error ->
                    _showErrorMessage.emit("Settings sync error: ${error.message}")
                }
                .collect { settings ->
                    updateState { 
                        it.copy(
                            settings = settings,
                            loadingState = LoadingState.Success(settings)
                        )
                    }
                }
        }
    }
    
    /**
     * Observes sync status for UI feedback.
     */
    private fun observeSyncStatus() {
        viewModelScope.launch {
            settingsManager.observeSyncStatus()
                .catch { error ->
                    // Log error but don't show to user as sync status is not critical
                }
                .collect { isSynced ->
                    updateState { it.copy(syncStatus = isSynced) }
                }
        }
    }
    
    /**
     * Updates the search query and filters settings sections.
     * 
     * @param query The search query
     */
    fun updateSearchQuery(query: String) {
        updateState { it.copy(searchQuery = query) }
        updateFilteredSections(query)
    }
    
    /**
     * Filters settings sections based on search query.
     */
    private fun updateFilteredSections(query: String) {
        val filteredSections = if (query.isBlank()) {
            allSettingSections
        } else {
            allSettingSections.mapNotNull { section ->
                val filteredItems = section.items.filter { item ->
                    matchesSearchQuery(item, query)
                }
                
                if (filteredItems.isNotEmpty() || matchesSearchQuery(section, query)) {
                    section.copy(
                        items = filteredItems,
                        isVisible = true
                    )
                } else {
                    null
                }
            }
        }
        
        updateState { it.copy(filteredSections = filteredSections) }
    }
    
    /**
     * Checks if a setting section matches the search query.
     */
    private fun matchesSearchQuery(section: SettingSection, query: String): Boolean {
        val searchTerms = query.lowercase().split(" ").filter { it.isNotBlank() }
        val searchableText = "${section.title} ${section.subtitle}".lowercase()
        
        return searchTerms.any { term ->
            searchableText.contains(term)
        }
    }
    
    /**
     * Checks if a setting item matches the search query.
     */
    private fun matchesSearchQuery(item: SettingItem, query: String): Boolean {
        val searchTerms = query.lowercase().split(" ").filter { it.isNotBlank() }
        val searchableText = "${item.title} ${item.subtitle} ${item.keywords.joinToString(" ")}".lowercase()
        
        return searchTerms.any { term ->
            searchableText.contains(term)
        }
    }
    
    /**
     * Navigates to a specific setting screen.
     * 
     * @param settingId The ID of the setting to navigate to
     */
    fun navigateToSetting(settingId: String) {
        viewModelScope.launch {
            _navigateToSetting.emit(settingId)
        }
    }
    
    /**
     * Refreshes the settings from the repository.
     */
    fun refresh() {
        viewModelScope.launch {
            updateState { it.copy(isRefreshing = true) }
            
            settingsManager.syncSettings()
                .onSuccess {
                    loadSettings()
                    _showSuccessMessage.emit("Settings refreshed")
                }
                .onError { error ->
                    _showErrorMessage.emit("Failed to refresh settings: ${error.message}")
                }
                .also {
                    updateState { it.copy(isRefreshing = false) }
                }
        }
    }
    
    /**
     * Clears the search query.
     */
    fun clearSearch() {
        updateSearchQuery("")
    }
    
    /**
     * Gets the count of settings sections that match the current search.
     */
    fun getSearchResultCount(): Int {
        return uiState.value.filteredSections.sumOf { it.items.size }
    }
    
    /**
     * Checks if a specific setting section is visible in current filter.
     */
    fun isSectionVisible(sectionId: String): Boolean {
        return uiState.value.filteredSections.any { it.id == sectionId && it.isVisible }
    }
    
    /**
     * Gets recently accessed settings (placeholder for future implementation).
     */
    fun getRecentlyAccessedSettings(): List<SettingItem> {
        // This would be implemented with actual usage tracking
        return emptyList()
    }
}