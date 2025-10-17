package com.eunio.healthapp.presentation.state

import com.eunio.healthapp.domain.model.settings.CyclePreferences
import com.eunio.healthapp.presentation.viewmodel.UiState
import kotlinx.datetime.LocalDate

/**
 * UI state for cycle preferences screen.
 */
data class CyclePreferencesUiState(
    val preferences: CyclePreferences = CyclePreferences.default(),
    val loadingState: LoadingState = LoadingState.Idle,
    val isUpdating: Boolean = false,
    val validationErrors: Map<String, String> = emptyMap(),
    val showPredictionPreview: Boolean = false,
    val predictionPreview: PredictionPreview? = null,
    val isRecalculatingPredictions: Boolean = false
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
        get() = !isLoading && !isUpdating && !isRecalculatingPredictions
    
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
     * Whether the current preferences are valid.
     */
    val isValid: Boolean
        get() = validationErrors.isEmpty() && preferences.isValid()
    
    /**
     * Whether there are any validation errors.
     */
    val hasValidationErrors: Boolean
        get() = validationErrors.isNotEmpty()
    
    /**
     * Gets validation error for a specific field.
     */
    fun getValidationError(field: String): String? = validationErrors[field]
    
    /**
     * Whether a specific field has validation error.
     */
    fun hasValidationError(field: String): Boolean = validationErrors.containsKey(field)
    
    /**
     * Whether preferences have been customized from defaults.
     */
    val hasCustomizations: Boolean
        get() = preferences.isCustomized
}

/**
 * Preview of cycle predictions based on current preferences.
 */
data class PredictionPreview(
    val nextPeriodDate: LocalDate,
    val nextOvulationDate: LocalDate,
    val fertileWindowStart: LocalDate,
    val fertileWindowEnd: LocalDate,
    val cycleDay: Int,
    val daysUntilPeriod: Int,
    val daysUntilOvulation: Int
) {
    /**
     * Whether currently in fertile window.
     */
    val isInFertileWindow: Boolean
        get() = cycleDay >= (28 - 14 - 5) && cycleDay <= (28 - 14 + 1) // Simplified calculation
    
    /**
     * Whether period is expected soon (within 3 days).
     */
    val isPeriodSoon: Boolean
        get() = daysUntilPeriod <= 3
    
    /**
     * Whether ovulation is expected soon (within 2 days).
     */
    val isOvulationSoon: Boolean
        get() = daysUntilOvulation <= 2
}