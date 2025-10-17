package com.eunio.healthapp.presentation.state

import com.eunio.healthapp.domain.model.settings.TemperatureUnit
import com.eunio.healthapp.domain.model.settings.UnitPreferences
import com.eunio.healthapp.domain.model.settings.WeightUnit
import com.eunio.healthapp.presentation.viewmodel.UiState

/**
 * UI state for unit preferences screen.
 */
data class UnitPreferencesUiState(
    val preferences: UnitPreferences = UnitPreferences.default(),
    val loadingState: LoadingState = LoadingState.Idle,
    val isUpdating: Boolean = false,
    val previewTemperature: Double = 37.0, // Sample temperature for preview
    val previewWeight: Double = 70.0, // Sample weight for preview
    val showConversionPreview: Boolean = false
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
        get() = !isLoading && !isUpdating
    
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
     * Gets formatted preview temperature in current unit.
     */
    fun getFormattedPreviewTemperature(): String {
        return "${previewTemperature}${preferences.temperatureUnit.symbol}"
    }
    
    /**
     * Gets formatted preview weight in current unit.
     */
    fun getFormattedPreviewWeight(): String {
        return "${previewWeight} ${preferences.weightUnit.symbol}"
    }
    
    /**
     * Gets conversion preview text for temperature.
     */
    fun getTemperatureConversionPreview(): String {
        val otherUnit = if (preferences.temperatureUnit == TemperatureUnit.CELSIUS) {
            TemperatureUnit.FAHRENHEIT
        } else {
            TemperatureUnit.CELSIUS
        }
        
        // Simple conversion for preview (would use UnitConverter in real implementation)
        val convertedValue = if (preferences.temperatureUnit == TemperatureUnit.CELSIUS) {
            (previewTemperature * 9.0 / 5.0) + 32.0
        } else {
            (previewTemperature - 32.0) * 5.0 / 9.0
        }
        
        return "${previewTemperature}${preferences.temperatureUnit.symbol} = ${convertedValue.toString().take(4)}${otherUnit.symbol}"
    }
    
    /**
     * Gets conversion preview text for weight.
     */
    fun getWeightConversionPreview(): String {
        val otherUnit = if (preferences.weightUnit == WeightUnit.KILOGRAMS) {
            WeightUnit.POUNDS
        } else {
            WeightUnit.KILOGRAMS
        }
        
        // Simple conversion for preview (would use UnitConverter in real implementation)
        val convertedValue = if (preferences.weightUnit == WeightUnit.KILOGRAMS) {
            previewWeight * 2.20462
        } else {
            previewWeight / 2.20462
        }
        
        return "${previewWeight} ${preferences.weightUnit.symbol} = ${convertedValue.toString().take(5)} ${otherUnit.symbol}"
    }
}