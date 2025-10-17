package com.eunio.healthapp.presentation.viewmodel

import com.eunio.healthapp.domain.manager.SettingsManager
import com.eunio.healthapp.domain.model.settings.PrivacyPreferences
import com.eunio.healthapp.domain.repository.SettingsRepository
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.presentation.state.LoadingState
import com.eunio.healthapp.presentation.state.PrivacyPreferencesUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * ViewModel for privacy preferences screen with data export functionality.
 * Manages privacy settings, data export, and account deletion workflows.
 */
class PrivacyPreferencesViewModel(
    private val settingsManager: SettingsManager,
    private val settingsRepository: SettingsRepository
) : BaseViewModel<PrivacyPreferencesUiState>() {
    
    override val initialState = PrivacyPreferencesUiState()
    
    private val _showSuccessMessage = MutableSharedFlow<String>()
    val showSuccessMessage: SharedFlow<String> = _showSuccessMessage.asSharedFlow()
    
    private val _showErrorMessage = MutableSharedFlow<String>()
    val showErrorMessage: SharedFlow<String> = _showErrorMessage.asSharedFlow()
    
    private val _dataExported = MutableSharedFlow<String>()
    val dataExported: SharedFlow<String> = _dataExported.asSharedFlow()
    
    private val _accountDeleted = MutableSharedFlow<Unit>()
    val accountDeleted: SharedFlow<Unit> = _accountDeleted.asSharedFlow()
    
    private val _showDeleteConfirmation = MutableSharedFlow<Unit>()
    val showDeleteConfirmation: SharedFlow<Unit> = _showDeleteConfirmation.asSharedFlow()
    
    init {
        loadPrivacyPreferences()
        observeSettingsChanges()
    }
    
    /**
     * Loads the current privacy preferences.
     */
    private fun loadPrivacyPreferences() {
        viewModelScope.launch {
            updateState { it.copy(loadingState = LoadingState.Loading) }
            
            settingsManager.getUserSettings()
                .onSuccess { settings ->
                    updateState { 
                        it.copy(
                            preferences = settings.privacyPreferences,
                            loadingState = LoadingState.Success(settings.privacyPreferences)
                        )
                    }
                }
                .onError { error ->
                    updateState { 
                        it.copy(
                            loadingState = LoadingState.Error("Failed to load privacy preferences: ${error.message}")
                        )
                    }
                    _showErrorMessage.emit("Failed to load privacy preferences")
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
                            preferences = settings.privacyPreferences,
                            loadingState = LoadingState.Success(settings.privacyPreferences)
                        )
                    }
                }
        }
    }
    
    /**
     * Toggles data sharing preference.
     * 
     * @param enabled Whether data sharing should be enabled
     */
    fun toggleDataSharing(enabled: Boolean) {
        viewModelScope.launch {
            val currentPreferences = uiState.value.preferences
            val newPreferences = currentPreferences.copy(dataSharingEnabled = enabled)
            
            updatePrivacyPreferences(newPreferences)
        }
    }
    
    /**
     * Toggles anonymous insights preference.
     * 
     * @param enabled Whether anonymous insights should be enabled
     */
    fun toggleAnonymousInsights(enabled: Boolean) {
        viewModelScope.launch {
            val currentPreferences = uiState.value.preferences
            val newPreferences = currentPreferences.copy(anonymousInsightsEnabled = enabled)
            
            updatePrivacyPreferences(newPreferences)
        }
    }
    
    /**
     * Toggles crash reporting preference.
     * 
     * @param enabled Whether crash reporting should be enabled
     */
    fun toggleCrashReporting(enabled: Boolean) {
        viewModelScope.launch {
            val currentPreferences = uiState.value.preferences
            val newPreferences = currentPreferences.copy(crashReportingEnabled = enabled)
            
            updatePrivacyPreferences(newPreferences)
        }
    }
    
    /**
     * Toggles analytics preference.
     * 
     * @param enabled Whether analytics should be enabled
     */
    fun toggleAnalytics(enabled: Boolean) {
        viewModelScope.launch {
            val currentPreferences = uiState.value.preferences
            val newPreferences = currentPreferences.copy(analyticsEnabled = enabled)
            
            updatePrivacyPreferences(newPreferences)
        }
    }
    
    /**
     * Sets privacy preferences to maximum privacy mode.
     */
    fun setMaximumPrivacy() {
        viewModelScope.launch {
            val maxPrivacyPreferences = PrivacyPreferences.maxPrivacy()
            updatePrivacyPreferences(maxPrivacyPreferences)
        }
    }
    
    /**
     * Sets privacy preferences to balanced mode.
     */
    fun setBalancedPrivacy() {
        viewModelScope.launch {
            val balancedPreferences = PrivacyPreferences.balanced()
            updatePrivacyPreferences(balancedPreferences)
        }
    }
    
    /**
     * Sets privacy preferences to default mode.
     */
    fun setDefaultPrivacy() {
        viewModelScope.launch {
            val defaultPreferences = PrivacyPreferences.default()
            updatePrivacyPreferences(defaultPreferences)
        }
    }
    
    /**
     * Updates privacy preferences.
     */
    private suspend fun updatePrivacyPreferences(preferences: PrivacyPreferences) {
        updateState { it.copy(isUpdating = true) }
        
        settingsManager.updatePrivacyPreferences(preferences)
            .onSuccess {
                updateState { 
                    it.copy(
                        preferences = preferences,
                        isUpdating = false
                    )
                }
                _showSuccessMessage.emit("Privacy preferences updated")
            }
            .onError { error ->
                updateState { it.copy(isUpdating = false) }
                _showErrorMessage.emit("Failed to update privacy preferences: ${error.message}")
            }
    }
    
    /**
     * Exports user data for data portability.
     */
    fun exportUserData() {
        viewModelScope.launch {
            updateState { 
                it.copy(
                    isExportingData = true,
                    exportProgress = 0f
                )
            }
            
            try {
                // Simulate export progress
                for (progress in 1..10) {
                    kotlinx.coroutines.delay(200)
                    updateState { it.copy(exportProgress = progress / 10f) }
                }
                
                // Get current user ID (this would come from auth service in real implementation)
                val userId = getCurrentUserId()
                
                settingsRepository.exportUserData(userId)
                    .onSuccess { exportData ->
                        val currentTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                        val exportSize = "${exportData.length / 1024}KB" // Approximate size
                        
                        updateState { 
                            it.copy(
                                isExportingData = false,
                                exportProgress = 1f,
                                exportedDataSize = exportSize,
                                lastExportDate = "${currentTime.date} ${currentTime.time}"
                            )
                        }
                        
                        _dataExported.emit(exportData)
                        _showSuccessMessage.emit("Data export completed successfully")
                    }
                    .onError { error ->
                        updateState { 
                            it.copy(
                                isExportingData = false,
                                exportProgress = 0f
                            )
                        }
                        _showErrorMessage.emit("Failed to export data: ${error.message}")
                    }
            } catch (e: Exception) {
                updateState { 
                    it.copy(
                        isExportingData = false,
                        exportProgress = 0f
                    )
                }
                _showErrorMessage.emit("Export failed: ${e.message}")
            }
        }
    }
    
    /**
     * Initiates the account deletion process.
     */
    fun initiateAccountDeletion() {
        updateState { 
            it.copy(
                showDeleteConfirmation = true,
                deleteConfirmationStep = 0
            )
        }
        viewModelScope.launch {
            _showDeleteConfirmation.emit(Unit)
        }
    }
    
    /**
     * Proceeds to the next step in account deletion confirmation.
     */
    fun proceedDeleteConfirmation() {
        val currentStep = uiState.value.deleteConfirmationStep
        if (currentStep < 2) {
            updateState { it.copy(deleteConfirmationStep = currentStep + 1) }
        } else {
            executeAccountDeletion()
        }
    }
    
    /**
     * Cancels the account deletion process.
     */
    fun cancelAccountDeletion() {
        updateState { 
            it.copy(
                showDeleteConfirmation = false,
                deleteConfirmationStep = 0
            )
        }
    }
    
    /**
     * Executes the account deletion.
     */
    private fun executeAccountDeletion() {
        viewModelScope.launch {
            updateState { 
                it.copy(
                    isDeletingAccount = true,
                    showDeleteConfirmation = false
                )
            }
            
            try {
                // Get current user ID (this would come from auth service in real implementation)
                val userId = getCurrentUserId()
                
                settingsRepository.deleteUserSettings(userId)
                    .onSuccess {
                        updateState { it.copy(isDeletingAccount = false) }
                        _accountDeleted.emit(Unit)
                        _showSuccessMessage.emit("Account deleted successfully")
                    }
                    .onError { error ->
                        updateState { it.copy(isDeletingAccount = false) }
                        _showErrorMessage.emit("Failed to delete account: ${error.message}")
                    }
            } catch (e: Exception) {
                updateState { it.copy(isDeletingAccount = false) }
                _showErrorMessage.emit("Account deletion failed: ${e.message}")
            }
        }
    }
    
    /**
     * Gets the current user ID (placeholder implementation).
     */
    private fun getCurrentUserId(): String {
        // This would be implemented with actual auth service
        return "current_user_id"
    }
    
    /**
     * Refreshes privacy preferences from the repository.
     */
    fun refresh() {
        loadPrivacyPreferences()
    }
    
    /**
     * Gets the privacy impact description for a specific setting.
     */
    fun getPrivacyImpactDescription(setting: String): String {
        return when (setting) {
            "dataSharing" -> "Allows sharing of anonymized health data with research partners to improve health insights for all users."
            "anonymousInsights" -> "Uses your data to generate personalized insights while keeping your identity completely anonymous."
            "crashReporting" -> "Automatically sends crash reports to help us fix bugs and improve app stability."
            "analytics" -> "Collects usage statistics to help us understand how you use the app and improve the user experience."
            else -> "This setting affects how your data is used and shared."
        }
    }
    
    /**
     * Gets the data types affected by a specific privacy setting.
     */
    fun getAffectedDataTypes(setting: String): List<String> {
        return when (setting) {
            "dataSharing" -> listOf("Cycle patterns", "Symptom trends", "Health metrics")
            "anonymousInsights" -> listOf("Personal health data", "Usage patterns", "Preferences")
            "crashReporting" -> listOf("App crash logs", "Device information", "Error reports")
            "analytics" -> listOf("App usage statistics", "Feature usage", "Performance metrics")
            else -> emptyList()
        }
    }
    
    /**
     * Checks if the current privacy settings are considered secure.
     */
    fun arePrivacySettingsSecure(): Boolean {
        val preferences = uiState.value.preferences
        return !preferences.dataSharingEnabled && 
               (!preferences.analyticsEnabled || !preferences.crashReportingEnabled)
    }
    
    /**
     * Gets recommendations for improving privacy.
     */
    fun getPrivacyRecommendations(): List<String> {
        val preferences = uiState.value.preferences
        val recommendations = mutableListOf<String>()
        
        if (preferences.dataSharingEnabled) {
            recommendations.add("Consider disabling data sharing for maximum privacy")
        }
        
        if (preferences.analyticsEnabled) {
            recommendations.add("Disable analytics to prevent usage tracking")
        }
        
        if (preferences.crashReportingEnabled) {
            recommendations.add("Turn off crash reporting to avoid sending error data")
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("Your privacy settings are already optimized")
        }
        
        return recommendations
    }
}