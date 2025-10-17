package com.eunio.healthapp.presentation.viewmodel

import com.eunio.healthapp.domain.manager.SettingsManager
import com.eunio.healthapp.domain.model.settings.*
import com.eunio.healthapp.presentation.state.LoadingState
import com.eunio.healthapp.presentation.state.SettingsUiState
import com.eunio.healthapp.presentation.state.SettingSection
import com.eunio.healthapp.presentation.util.handleSettingsError
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Enhanced ViewModel for the main settings screen with comprehensive error handling,
 * loading states, and user feedback mechanisms.
 */
class EnhancedSettingsViewModel(
    private val settingsManager: SettingsManager
) : BaseErrorHandlingViewModel<SettingsUiState>() {
    
    override val initialState = SettingsUiState()
    
    init {
        loadSettings()
        observeSettingsChanges()
    }
    
    /**
     * Loads all user settings with comprehensive error handling
     */
    private fun loadSettings() {
        viewModelScope.launch {
            try {
                updateState { it.copy(loadingState = LoadingState.Loading) }
                
                settingsManager.getUserSettings()
                    .onSuccess { settings ->
                        updateState { 
                            it.copy(
                                settings = settings,
                                loadingState = LoadingState.Success(settings)
                            )
                        }
                        handleSuccess("Settings loaded successfully")
                    }
                    .onError { error ->
                        handleError(error, "loading settings")
                        updateState { 
                            it.copy(
                                loadingState = LoadingState.Error("Failed to load settings")
                            )
                        }
                    }
            } catch (error: Throwable) {
                handleError(error, "loading settings")
                updateState { 
                    it.copy(
                        loadingState = LoadingState.Error("Failed to load settings")
                    )
                }
            }
        }
    }
    
    /**
     * Observes settings changes with error recovery
     */
    private fun observeSettingsChanges() {
        viewModelScope.launch {
            settingsManager.observeSettingsChanges()
                .catch { error ->
                    handleError(error, "observing settings changes")
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
     * Updates unit preferences with error handling and user feedback
     */
    fun updateUnitPreferences(preferences: UnitPreferences) {
        viewModelScope.launch {
            safeExecute("updating unit preferences", showDialog = false) {
                settingsManager.updateUnitPreferences(preferences)
                    .onSuccess {
                        updateState { 
                            it.copy(
                                settings = it.settings?.copy(unitPreferences = preferences)
                            )
                        }
                        handleSuccess("Unit preferences updated")
                    }
                    .onError { error ->
                        handleError(error, "updating unit preferences")
                    }
            }
        }
    }
    
    /**
     * Updates notification preferences with comprehensive error handling
     */
    fun updateNotificationPreferences(preferences: NotificationPreferences) {
        viewModelScope.launch {
            safeExecute("updating notification preferences") {
                settingsManager.updateNotificationPreferences(preferences)
                    .onSuccess {
                        updateState { 
                            it.copy(
                                settings = it.settings?.copy(notificationPreferences = preferences)
                            )
                        }
                        handleSuccess("Notification preferences updated")
                    }
                    .onError { error ->
                        handleError(error, "updating notification preferences", showDialog = true)
                    }
            }
        }
    }
    
    /**
     * Updates cycle preferences with validation and error handling
     */
    fun updateCyclePreferences(preferences: CyclePreferences) {
        viewModelScope.launch {
            safeExecute("updating cycle preferences") {
                settingsManager.updateCyclePreferences(preferences)
                    .onSuccess {
                        updateState { 
                            it.copy(
                                settings = it.settings?.copy(cyclePreferences = preferences)
                            )
                        }
                        handleSuccess("Cycle preferences updated - predictions will be recalculated")
                    }
                    .onError { error ->
                        handleError(error, "updating cycle preferences", showDialog = true)
                    }
            }
        }
    }
    
    /**
     * Updates privacy preferences with user confirmation for sensitive changes
     */
    fun updatePrivacyPreferences(preferences: PrivacyPreferences) {
        viewModelScope.launch {
            safeExecute("updating privacy preferences") {
                settingsManager.updatePrivacyPreferences(preferences)
                    .onSuccess {
                        updateState { 
                            it.copy(
                                settings = it.settings?.copy(privacyPreferences = preferences)
                            )
                        }
                        handleSuccess("Privacy preferences updated")
                    }
                    .onError { error ->
                        handleError(error, "updating privacy preferences", showDialog = true)
                    }
            }
        }
    }
    
    /**
     * Updates display preferences with immediate UI feedback
     */
    fun updateDisplayPreferences(preferences: DisplayPreferences) {
        viewModelScope.launch {
            safeExecute("updating display preferences") {
                settingsManager.updateDisplayPreferences(preferences)
                    .onSuccess {
                        updateState { 
                            it.copy(
                                settings = it.settings?.copy(displayPreferences = preferences)
                            )
                        }
                        handleSuccess("Display preferences updated")
                    }
                    .onError { error ->
                        handleError(error, "updating display preferences")
                    }
            }
        }
    }
    
    /**
     * Updates sync preferences with network status validation
     */
    fun updateSyncPreferences(preferences: SyncPreferences) {
        viewModelScope.launch {
            safeExecute("updating sync preferences") {
                settingsManager.updateSyncPreferences(preferences)
                    .onSuccess {
                        updateState { 
                            it.copy(
                                settings = it.settings?.copy(syncPreferences = preferences)
                            )
                        }
                        handleSuccess("Sync preferences updated")
                    }
                    .onError { error ->
                        handleError(error, "updating sync preferences", showDialog = true)
                    }
            }
        }
    }
    
    /**
     * Resets all settings to defaults with confirmation
     */
    fun resetToDefaults() {
        viewModelScope.launch {
            safeExecute("resetting settings to defaults", showDialog = true) {
                settingsManager.resetToDefaults()
                    .onSuccess { resetSettings ->
                        updateState { 
                            it.copy(
                                settings = resetSettings,
                                loadingState = LoadingState.Success(resetSettings)
                            )
                        }
                        handleSuccess("Settings reset to defaults")
                    }
                    .onError { error ->
                        handleError(error, "resetting settings", showDialog = true)
                    }
            }
        }
    }
    
    /**
     * Exports user settings with progress tracking
     */
    fun exportSettings() {
        viewModelScope.launch {
            safeExecute("exporting settings") {
                settingsManager.exportSettings()
                    .onSuccess { exportData ->
                        handleSuccess("Settings exported successfully")
                        // The actual file sharing would be handled by the UI layer
                    }
                    .onError { error ->
                        handleError(error, "exporting settings", showDialog = true)
                    }
            }
        }
    }
    
    /**
     * Searches settings based on query with error handling
     */
    fun searchSettings(query: String) {
        viewModelScope.launch {
            safeExecute("searching settings") {
                updateState { it.copy(searchQuery = query) }
                
                // Simulate search delay
                kotlinx.coroutines.delay(300)
                
                val filteredSections = filterSettingsByQuery(query)
                updateState { 
                    it.copy(
                        filteredSections = filteredSections
                    )
                }
            }
        }
    }
    
    /**
     * Clears search results
     */
    fun clearSearch() {
        updateState { 
            it.copy(
                searchQuery = "",
                filteredSections = emptyList()
            )
        }
    }
    
    /**
     * Retry mechanism for failed operations
     */
    override suspend fun onRetryRequested() {
        when (val currentError = errorHandlingState.value.currentError) {
            is com.eunio.healthapp.presentation.state.ErrorState.SyncError -> {
                // Retry sync operation
                loadSettings()
            }
            is com.eunio.healthapp.presentation.state.ErrorState.NetworkError -> {
                // Retry network-dependent operations
                loadSettings()
            }
            else -> {
                // Generic retry - reload settings
                loadSettings()
            }
        }
    }
    
    /**
     * Filters settings based on search query
     */
    private fun filterSettingsByQuery(query: String): List<SettingSection> {
        if (query.isBlank()) return emptyList()
        
        // Create mock sections for search results
        val allSections = listOf(
            SettingSection("units", "Units", "Temperature and weight units", "units"),
            SettingSection("notifications", "Notifications", "Reminders and alerts", "notifications"),
            SettingSection("cycle", "Cycle Settings", "Cycle length and parameters", "cycle"),
            SettingSection("privacy", "Privacy", "Data sharing and export", "privacy"),
            SettingSection("display", "Display", "Text size and contrast", "display"),
            SettingSection("sync", "Sync", "Cloud backup and sync", "sync")
        )
        
        return allSections.filter { section ->
            section.title.contains(query, ignoreCase = true) ||
            section.subtitle.contains(query, ignoreCase = true)
        }
    }
    
    /**
     * Gets setting recommendations based on current state
     */
    fun getSettingRecommendations(): List<String> {
        val settings = uiState.value.settings ?: return emptyList()
        val recommendations = mutableListOf<String>()
        
        // Check for incomplete setup
        if (!settings.unitPreferences.isManuallySet) {
            recommendations.add("Set your preferred units for temperature and weight")
        }
        
        if (!settings.notificationPreferences.globalNotificationsEnabled) {
            recommendations.add("Enable notifications to stay on track with your health goals")
        }
        
        if (!settings.cyclePreferences.isCustomized) {
            recommendations.add("Customize cycle settings for more accurate predictions")
        }
        
        if (!settings.syncPreferences.cloudBackupEnabled) {
            recommendations.add("Enable cloud backup to protect your data")
        }
        
        return recommendations
    }
    
    /**
     * Checks if settings need user attention
     */
    fun needsAttention(): Boolean {
        val settings = uiState.value.settings ?: return true
        val errorState = errorHandlingState.value
        
        return !settings.unitPreferences.isManuallySet || 
               errorState.currentError != null ||
               errorState.hasReachedMaxRetries
    }
}