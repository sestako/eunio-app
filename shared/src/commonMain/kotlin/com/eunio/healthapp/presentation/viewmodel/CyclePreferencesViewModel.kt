package com.eunio.healthapp.presentation.viewmodel

import com.eunio.healthapp.domain.manager.SettingsManager
import com.eunio.healthapp.domain.model.settings.CyclePreferences
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.presentation.state.CyclePreferencesUiState
import com.eunio.healthapp.presentation.state.LoadingState
import com.eunio.healthapp.presentation.state.PredictionPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

/**
 * ViewModel for cycle preferences screen with validation and prediction updates.
 * Handles cycle parameter input, validation, and prediction recalculation.
 */
class CyclePreferencesViewModel(
    private val settingsManager: SettingsManager
) : BaseViewModel<CyclePreferencesUiState>() {
    
    override val initialState = CyclePreferencesUiState()
    
    private val _showSuccessMessage = MutableSharedFlow<String>()
    val showSuccessMessage: SharedFlow<String> = _showSuccessMessage.asSharedFlow()
    
    private val _showErrorMessage = MutableSharedFlow<String>()
    val showErrorMessage: SharedFlow<String> = _showErrorMessage.asSharedFlow()
    
    private val _predictionsRecalculated = MutableSharedFlow<CyclePreferences>()
    val predictionsRecalculated: SharedFlow<CyclePreferences> = _predictionsRecalculated.asSharedFlow()
    
    init {
        loadCyclePreferences()
        observeSettingsChanges()
    }
    
    /**
     * Loads the current cycle preferences.
     */
    private fun loadCyclePreferences() {
        viewModelScope.launch {
            updateState { it.copy(loadingState = LoadingState.Loading) }
            
            settingsManager.getUserSettings()
                .onSuccess { settings ->
                    updateState { 
                        it.copy(
                            preferences = settings.cyclePreferences,
                            loadingState = LoadingState.Success(settings.cyclePreferences)
                        )
                    }
                    generatePredictionPreview(settings.cyclePreferences)
                }
                .onError { error ->
                    updateState { 
                        it.copy(
                            loadingState = LoadingState.Error("Failed to load cycle preferences: ${error.message}")
                        )
                    }
                    _showErrorMessage.emit("Failed to load cycle preferences")
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
                            preferences = settings.cyclePreferences,
                            loadingState = LoadingState.Success(settings.cyclePreferences)
                        )
                    }
                    generatePredictionPreview(settings.cyclePreferences)
                }
        }
    }
    
    /**
     * Updates the average cycle length with validation.
     * 
     * @param length The new cycle length (21-45 days)
     */
    fun updateCycleLength(length: Int) {
        val validationErrors = validateCycleLength(length)
        updateValidationErrors("cycleLength", validationErrors)
        
        if (validationErrors == null) {
            val currentPreferences = uiState.value.preferences
            val newPreferences = currentPreferences.copy(
                averageCycleLength = length,
                isCustomized = true
            )
            
            updateState { it.copy(preferences = newPreferences) }
            generatePredictionPreview(newPreferences)
        }
    }
    
    /**
     * Updates the average luteal phase length with validation.
     * 
     * @param length The new luteal phase length (10-16 days)
     */
    fun updateLutealPhaseLength(length: Int) {
        val validationErrors = validateLutealPhaseLength(length)
        updateValidationErrors("lutealPhaseLength", validationErrors)
        
        if (validationErrors == null) {
            val currentPreferences = uiState.value.preferences
            val newPreferences = currentPreferences.copy(
                averageLutealPhaseLength = length,
                isCustomized = true
            )
            
            updateState { it.copy(preferences = newPreferences) }
            generatePredictionPreview(newPreferences)
        }
    }
    
    /**
     * Updates the period duration with validation.
     * 
     * @param duration The new period duration (3-10 days)
     */
    fun updatePeriodDuration(duration: Int) {
        val validationErrors = validatePeriodDuration(duration)
        updateValidationErrors("periodDuration", validationErrors)
        
        if (validationErrors == null) {
            val currentPreferences = uiState.value.preferences
            val newPreferences = currentPreferences.copy(
                periodDuration = duration,
                isCustomized = true
            )
            
            updateState { it.copy(preferences = newPreferences) }
            generatePredictionPreview(newPreferences)
        }
    }
    
    /**
     * Saves the current cycle preferences.
     */
    fun saveCyclePreferences() {
        viewModelScope.launch {
            val preferences = uiState.value.preferences
            
            // Validate all preferences before saving
            val allValidationErrors = validateAllPreferences(preferences)
            if (allValidationErrors.isNotEmpty()) {
                updateState { it.copy(validationErrors = allValidationErrors) }
                _showErrorMessage.emit("Please fix validation errors before saving")
                return@launch
            }
            
            updateState { it.copy(isUpdating = true) }
            
            settingsManager.updateCyclePreferences(preferences)
                .onSuccess {
                    updateState { 
                        it.copy(
                            isUpdating = false,
                            isRecalculatingPredictions = true
                        )
                    }
                    
                    // Simulate prediction recalculation delay
                    kotlinx.coroutines.delay(1000)
                    
                    updateState { it.copy(isRecalculatingPredictions = false) }
                    _predictionsRecalculated.emit(preferences)
                    _showSuccessMessage.emit("Cycle preferences updated and predictions recalculated")
                }
                .onError { error ->
                    updateState { it.copy(isUpdating = false) }
                    _showErrorMessage.emit("Failed to update cycle preferences: ${error.message}")
                }
        }
    }
    
    /**
     * Resets cycle preferences to default values.
     */
    fun resetToDefaults() {
        viewModelScope.launch {
            updateState { it.copy(isUpdating = true) }
            
            val defaultPreferences = CyclePreferences.default()
            
            settingsManager.updateCyclePreferences(defaultPreferences)
                .onSuccess {
                    updateState { 
                        it.copy(
                            preferences = defaultPreferences,
                            isUpdating = false,
                            validationErrors = emptyMap()
                        )
                    }
                    generatePredictionPreview(defaultPreferences)
                    _showSuccessMessage.emit("Cycle preferences reset to defaults")
                }
                .onError { error ->
                    updateState { it.copy(isUpdating = false) }
                    _showErrorMessage.emit("Failed to reset preferences: ${error.message}")
                }
        }
    }
    
    /**
     * Toggles the prediction preview visibility.
     */
    fun togglePredictionPreview() {
        updateState { it.copy(showPredictionPreview = !it.showPredictionPreview) }
    }
    
    /**
     * Shows the prediction preview.
     */
    fun showPredictionPreview() {
        updateState { it.copy(showPredictionPreview = true) }
        generatePredictionPreview(uiState.value.preferences)
    }
    
    /**
     * Hides the prediction preview.
     */
    fun hidePredictionPreview() {
        updateState { it.copy(showPredictionPreview = false) }
    }
    
    /**
     * Generates a prediction preview based on current preferences.
     */
    private fun generatePredictionPreview(preferences: CyclePreferences) {
        viewModelScope.launch {
            try {
                val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                
                // Calculate next period date (simplified calculation)
                val daysUntilPeriod = preferences.averageCycleLength - (today.dayOfMonth % preferences.averageCycleLength)
                val nextPeriodDate = today.plus(daysUntilPeriod, DateTimeUnit.DAY)
                
                // Calculate ovulation date (luteal phase before period)
                val nextOvulationDate = nextPeriodDate.plus(-preferences.averageLutealPhaseLength, DateTimeUnit.DAY)
                
                // Calculate fertile window (5 days before ovulation + ovulation day)
                val fertileWindowStart = nextOvulationDate.plus(-5, DateTimeUnit.DAY)
                val fertileWindowEnd = nextOvulationDate.plus(1, DateTimeUnit.DAY)
                
                // Calculate current cycle day (simplified)
                val cycleDay = (today.dayOfMonth % preferences.averageCycleLength) + 1
                val daysUntilOvulation = if (nextOvulationDate > today) {
                    (nextOvulationDate.toEpochDays() - today.toEpochDays()).toInt()
                } else {
                    0
                }
                
                val preview = PredictionPreview(
                    nextPeriodDate = nextPeriodDate,
                    nextOvulationDate = nextOvulationDate,
                    fertileWindowStart = fertileWindowStart,
                    fertileWindowEnd = fertileWindowEnd,
                    cycleDay = cycleDay,
                    daysUntilPeriod = daysUntilPeriod,
                    daysUntilOvulation = daysUntilOvulation
                )
                
                updateState { it.copy(predictionPreview = preview) }
            } catch (e: Exception) {
                // Log error but don't show to user as preview is not critical
                updateState { it.copy(predictionPreview = null) }
            }
        }
    }
    
    /**
     * Validates cycle length input.
     */
    private fun validateCycleLength(length: Int): String? {
        return when {
            length < CyclePreferences.MIN_CYCLE_LENGTH -> 
                "Cycle length must be at least ${CyclePreferences.MIN_CYCLE_LENGTH} days"
            length > CyclePreferences.MAX_CYCLE_LENGTH -> 
                "Cycle length cannot exceed ${CyclePreferences.MAX_CYCLE_LENGTH} days"
            else -> null
        }
    }
    
    /**
     * Validates luteal phase length input.
     */
    private fun validateLutealPhaseLength(length: Int): String? {
        val cycleLength = uiState.value.preferences.averageCycleLength
        return when {
            length < CyclePreferences.MIN_LUTEAL_PHASE -> 
                "Luteal phase must be at least ${CyclePreferences.MIN_LUTEAL_PHASE} days"
            length > CyclePreferences.MAX_LUTEAL_PHASE -> 
                "Luteal phase cannot exceed ${CyclePreferences.MAX_LUTEAL_PHASE} days"
            length >= cycleLength -> 
                "Luteal phase must be shorter than cycle length"
            else -> null
        }
    }
    
    /**
     * Validates period duration input.
     */
    private fun validatePeriodDuration(duration: Int): String? {
        return when {
            duration < CyclePreferences.MIN_PERIOD_DURATION -> 
                "Period duration must be at least ${CyclePreferences.MIN_PERIOD_DURATION} days"
            duration > CyclePreferences.MAX_PERIOD_DURATION -> 
                "Period duration cannot exceed ${CyclePreferences.MAX_PERIOD_DURATION} days"
            else -> null
        }
    }
    
    /**
     * Validates all cycle preferences.
     */
    private fun validateAllPreferences(preferences: CyclePreferences): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        
        validateCycleLength(preferences.averageCycleLength)?.let { 
            errors["cycleLength"] = it 
        }
        validateLutealPhaseLength(preferences.averageLutealPhaseLength)?.let { 
            errors["lutealPhaseLength"] = it 
        }
        validatePeriodDuration(preferences.periodDuration)?.let { 
            errors["periodDuration"] = it 
        }
        
        return errors
    }
    
    /**
     * Updates validation errors for a specific field.
     */
    private fun updateValidationErrors(field: String, error: String?) {
        val currentErrors = uiState.value.validationErrors.toMutableMap()
        if (error != null) {
            currentErrors[field] = error
        } else {
            currentErrors.remove(field)
        }
        updateState { it.copy(validationErrors = currentErrors) }
    }
    
    /**
     * Clears all validation errors.
     */
    fun clearValidationErrors() {
        updateState { it.copy(validationErrors = emptyMap()) }
    }
    
    /**
     * Refreshes cycle preferences from the repository.
     */
    fun refresh() {
        loadCyclePreferences()
    }
    
    /**
     * Gets the recommended cycle length range as a string.
     */
    fun getCycleLengthRange(): String {
        return "${CyclePreferences.MIN_CYCLE_LENGTH}-${CyclePreferences.MAX_CYCLE_LENGTH} days"
    }
    
    /**
     * Gets the recommended luteal phase length range as a string.
     */
    fun getLutealPhaseLengthRange(): String {
        return "${CyclePreferences.MIN_LUTEAL_PHASE}-${CyclePreferences.MAX_LUTEAL_PHASE} days"
    }
    
    /**
     * Gets the recommended period duration range as a string.
     */
    fun getPeriodDurationRange(): String {
        return "${CyclePreferences.MIN_PERIOD_DURATION}-${CyclePreferences.MAX_PERIOD_DURATION} days"
    }
}