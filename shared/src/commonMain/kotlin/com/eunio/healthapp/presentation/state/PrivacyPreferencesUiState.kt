package com.eunio.healthapp.presentation.state

import com.eunio.healthapp.domain.model.settings.PrivacyPreferences
import com.eunio.healthapp.presentation.viewmodel.UiState

/**
 * UI state for privacy preferences screen.
 */
data class PrivacyPreferencesUiState(
    val preferences: PrivacyPreferences = PrivacyPreferences.default(),
    val loadingState: LoadingState = LoadingState.Idle,
    val isUpdating: Boolean = false,
    val isExportingData: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val exportProgress: Float = 0f,
    val showDeleteConfirmation: Boolean = false,
    val deleteConfirmationStep: Int = 0,
    val exportedDataSize: String? = null,
    val lastExportDate: String? = null
) : UiState {
    
    /**
     * Whether the preferences are currently loading.
     */
    val isLoading: Boolean
        get() = loadingState is LoadingState.Loading
    
    /**
     * Whether the preferences are enabled for interaction.
     */
    val isEnabled: Boolean
        get() = !isLoading && !isUpdating && !isExportingData && !isDeletingAccount
    
    /**
     * Error message if loading failed.
     */
    val errorMessage: String?
        get() = (loadingState as? LoadingState.Error)?.message
    
    /**
     * Whether preferences have been loaded successfully.
     */
    val hasPreferences: Boolean
        get() = loadingState is LoadingState.Success<*>
    
    /**
     * Whether data export is in progress.
     */
    val isDataExportInProgress: Boolean
        get() = isExportingData && exportProgress > 0f && exportProgress < 1f
    
    /**
     * Whether data export is complete.
     */
    val isDataExportComplete: Boolean
        get() = isExportingData && exportProgress >= 1f
    
    /**
     * Whether account deletion is in progress.
     */
    val isAccountDeletionInProgress: Boolean
        get() = isDeletingAccount || showDeleteConfirmation
    
    /**
     * Whether all data collection is disabled.
     */
    val isDataCollectionDisabled: Boolean
        get() = !preferences.analyticsEnabled && 
                !preferences.crashReportingEnabled && 
                !preferences.anonymousInsightsEnabled
    
    /**
     * Whether data sharing is enabled.
     */
    val isDataSharingEnabled: Boolean
        get() = preferences.dataSharingEnabled
    
    /**
     * Gets the privacy level description.
     */
    fun getPrivacyLevelDescription(): String {
        return when {
            isDataCollectionDisabled && !isDataSharingEnabled -> "Maximum Privacy"
            preferences.anonymousInsightsEnabled && !preferences.dataSharingEnabled -> "Balanced Privacy"
            !isDataSharingEnabled -> "High Privacy"
            else -> "Standard Privacy"
        }
    }
    
    /**
     * Gets the count of enabled data collection features.
     */
    fun getEnabledDataCollectionCount(): Int {
        var count = 0
        if (preferences.analyticsEnabled) count++
        if (preferences.crashReportingEnabled) count++
        if (preferences.anonymousInsightsEnabled) count++
        if (preferences.dataSharingEnabled) count++
        return count
    }
    
    /**
     * Whether the delete confirmation is at the final step.
     */
    val isDeleteConfirmationFinalStep: Boolean
        get() = deleteConfirmationStep >= 2
    
    /**
     * Gets the export progress percentage.
     */
    fun getExportProgressPercentage(): Int = (exportProgress * 100).toInt()
}