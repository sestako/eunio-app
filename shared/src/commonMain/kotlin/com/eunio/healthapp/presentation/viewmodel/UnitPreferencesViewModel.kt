package com.eunio.healthapp.presentation.viewmodel

import com.eunio.healthapp.domain.manager.SettingsManager
import com.eunio.healthapp.domain.model.settings.TemperatureUnit
import com.eunio.healthapp.domain.model.settings.UnitPreferences
import com.eunio.healthapp.domain.model.settings.WeightUnit
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.domain.util.UnitPreferencesConverter
import com.eunio.healthapp.presentation.state.LoadingState
import com.eunio.healthapp.presentation.state.UnitPreferencesUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * ViewModel for unit preferences screen with immediate conversion updates.
 * Handles unit selection, conversion previews, and immediate UI feedback.
 */
class UnitPreferencesViewModel(
    private val settingsManager: SettingsManager,
    private val unitConverter: UnitPreferencesConverter
) : BaseViewModel<UnitPreferencesUiState>() {
    
    override val initialState = UnitPreferencesUiState()
    
    private val _showSuccessMessage = MutableSharedFlow<String>()
    val showSuccessMessage: SharedFlow<String> = _showSuccessMessage.asSharedFlow()
    
    private val _showErrorMessage = MutableSharedFlow<String>()
    val showErrorMessage: SharedFlow<String> = _showErrorMessage.asSharedFlow()
    
    private val _unitsChanged = MutableSharedFlow<UnitPreferences>()
    val unitsChanged: SharedFlow<UnitPreferences> = _unitsChanged.asSharedFlow()
    
    init {
        loadUnitPreferences()
        observeSettingsChanges()
    }
    
    /**
     * Loads the current unit preferences.
     */
    private fun loadUnitPreferences() {
        viewModelScope.launch {
            updateState { it.copy(loadingState = LoadingState.Loading) }
            
            settingsManager.getUserSettings()
                .onSuccess { settings ->
                    updateState { 
                        it.copy(
                            preferences = settings.unitPreferences,
                            loadingState = LoadingState.Success(settings.unitPreferences)
                        )
                    }
                }
                .onError { error ->
                    updateState { 
                        it.copy(
                            loadingState = LoadingState.Error("Failed to load unit preferences: ${error.message}")
                        )
                    }
                    _showErrorMessage.emit("Failed to load unit preferences")
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
                            preferences = settings.unitPreferences,
                            loadingState = LoadingState.Success(settings.unitPreferences)
                        )
                    }
                }
        }
    }
    
    /**
     * Updates the temperature unit preference with immediate feedback.
     * 
     * @param temperatureUnit The new temperature unit
     */
    fun updateTemperatureUnit(temperatureUnit: TemperatureUnit) {
        viewModelScope.launch {
            val currentPreferences = uiState.value.preferences
            val newPreferences = currentPreferences.copy(
                temperatureUnit = temperatureUnit,
                isManuallySet = true
            )
            
            // Update UI immediately for responsive feedback
            updateState { 
                it.copy(
                    preferences = newPreferences,
                    isUpdating = true,
                    showConversionPreview = true
                )
            }
            
            // Save to repository
            settingsManager.updateUnitPreferences(newPreferences)
                .onSuccess {
                    updateState { it.copy(isUpdating = false) }
                    _unitsChanged.emit(newPreferences)
                    _showSuccessMessage.emit("Temperature unit changed to ${temperatureUnit.displayName}")
                }
                .onError { error ->
                    // Revert UI state on error
                    updateState { 
                        it.copy(
                            preferences = currentPreferences,
                            isUpdating = false
                        )
                    }
                    _showErrorMessage.emit("Failed to update temperature unit: ${error.message}")
                }
        }
    }
    
    /**
     * Updates the weight unit preference with immediate feedback.
     * 
     * @param weightUnit The new weight unit
     */
    fun updateWeightUnit(weightUnit: WeightUnit) {
        viewModelScope.launch {
            val currentPreferences = uiState.value.preferences
            val newPreferences = currentPreferences.copy(
                weightUnit = weightUnit,
                isManuallySet = true
            )
            
            // Update UI immediately for responsive feedback
            updateState { 
                it.copy(
                    preferences = newPreferences,
                    isUpdating = true,
                    showConversionPreview = true
                )
            }
            
            // Save to repository
            settingsManager.updateUnitPreferences(newPreferences)
                .onSuccess {
                    updateState { it.copy(isUpdating = false) }
                    _unitsChanged.emit(newPreferences)
                    _showSuccessMessage.emit("Weight unit changed to ${weightUnit.displayName}")
                }
                .onError { error ->
                    // Revert UI state on error
                    updateState { 
                        it.copy(
                            preferences = currentPreferences,
                            isUpdating = false
                        )
                    }
                    _showErrorMessage.emit("Failed to update weight unit: ${error.message}")
                }
        }
    }
    
    /**
     * Updates both temperature and weight units atomically.
     * 
     * @param temperatureUnit The new temperature unit
     * @param weightUnit The new weight unit
     */
    fun updateBothUnits(temperatureUnit: TemperatureUnit, weightUnit: WeightUnit) {
        viewModelScope.launch {
            val currentPreferences = uiState.value.preferences
            val newPreferences = currentPreferences.copy(
                temperatureUnit = temperatureUnit,
                weightUnit = weightUnit,
                isManuallySet = true
            )
            
            // Update UI immediately for responsive feedback
            updateState { 
                it.copy(
                    preferences = newPreferences,
                    isUpdating = true,
                    showConversionPreview = true
                )
            }
            
            // Save to repository
            settingsManager.updateUnitPreferences(newPreferences)
                .onSuccess {
                    updateState { it.copy(isUpdating = false) }
                    _unitsChanged.emit(newPreferences)
                    _showSuccessMessage.emit("Units updated successfully")
                }
                .onError { error ->
                    // Revert UI state on error
                    updateState { 
                        it.copy(
                            preferences = currentPreferences,
                            isUpdating = false
                        )
                    }
                    _showErrorMessage.emit("Failed to update units: ${error.message}")
                }
        }
    }
    
    /**
     * Updates the preview temperature for conversion examples.
     * 
     * @param temperature The temperature value to preview
     */
    fun updatePreviewTemperature(temperature: Double) {
        updateState { it.copy(previewTemperature = temperature) }
    }
    
    /**
     * Updates the preview weight for conversion examples.
     * 
     * @param weight The weight value to preview
     */
    fun updatePreviewWeight(weight: Double) {
        updateState { it.copy(previewWeight = weight) }
    }
    
    /**
     * Toggles the conversion preview visibility.
     */
    fun toggleConversionPreview() {
        updateState { it.copy(showConversionPreview = !it.showConversionPreview) }
    }
    
    /**
     * Shows the conversion preview.
     */
    fun showConversionPreview() {
        updateState { it.copy(showConversionPreview = true) }
    }
    
    /**
     * Hides the conversion preview.
     */
    fun hideConversionPreview() {
        updateState { it.copy(showConversionPreview = false) }
    }
    
    /**
     * Gets the converted temperature value using the unit converter.
     * 
     * @param value The temperature value to convert
     * @param fromUnit The source temperature unit
     * @param toUnit The target temperature unit
     * @return The converted temperature value
     */
    fun convertTemperature(value: Double, fromUnit: TemperatureUnit, toUnit: TemperatureUnit): Double {
        return unitConverter.convertTemperature(value, fromUnit, toUnit)
    }
    
    /**
     * Gets the converted weight value using the unit converter.
     * 
     * @param value The weight value to convert
     * @param fromUnit The source weight unit
     * @param toUnit The target weight unit
     * @return The converted weight value
     */
    fun convertWeight(value: Double, fromUnit: WeightUnit, toUnit: WeightUnit): Double {
        return unitConverter.convertWeight(value, fromUnit, toUnit)
    }
    
    /**
     * Gets formatted temperature with unit symbol.
     * 
     * @param value The temperature value
     * @param unit The temperature unit
     * @return Formatted temperature string
     */
    fun formatTemperature(value: Double, unit: TemperatureUnit): String {
        return unitConverter.formatTemperature(value, unit)
    }
    
    /**
     * Gets formatted weight with unit symbol.
     * 
     * @param value The weight value
     * @param unit The weight unit
     * @return Formatted weight string
     */
    fun formatWeight(value: Double, unit: WeightUnit): String {
        return unitConverter.formatWeight(value, unit)
    }
    
    /**
     * Resets unit preferences to system defaults based on locale.
     */
    fun resetToDefaults() {
        viewModelScope.launch {
            updateState { it.copy(isUpdating = true) }
            
            val defaultPreferences = UnitPreferences.default()
            
            settingsManager.updateUnitPreferences(defaultPreferences)
                .onSuccess {
                    updateState { 
                        it.copy(
                            preferences = defaultPreferences,
                            isUpdating = false
                        )
                    }
                    _unitsChanged.emit(defaultPreferences)
                    _showSuccessMessage.emit("Unit preferences reset to defaults")
                }
                .onError { error ->
                    updateState { it.copy(isUpdating = false) }
                    _showErrorMessage.emit("Failed to reset preferences: ${error.message}")
                }
        }
    }
    
    /**
     * Refreshes unit preferences from the repository.
     */
    fun refresh() {
        loadUnitPreferences()
    }
}