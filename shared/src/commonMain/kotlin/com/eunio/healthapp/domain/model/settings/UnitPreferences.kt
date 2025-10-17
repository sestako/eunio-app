package com.eunio.healthapp.domain.model.settings

import kotlinx.serialization.Serializable

@Serializable
data class UnitPreferences(
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val weightUnit: WeightUnit = WeightUnit.KILOGRAMS,
    val isManuallySet: Boolean = false
) {
    /**
     * Validates that the unit preferences are in a valid state
     * 
     * @return true if preferences are valid, false otherwise
     */
    fun isValid(): Boolean {
        // All enum values are valid by definition
        return true
    }
    
    /**
     * Creates unit preferences based on device locale
     * 
     * @param locale The device locale string
     * @return UnitPreferences configured for the given locale
     */
    companion object {
        fun fromLocale(locale: String): UnitPreferences {
            return UnitPreferences(
                temperatureUnit = TemperatureUnit.fromLocale(locale),
                weightUnit = WeightUnit.fromLocale(locale),
                isManuallySet = false
            )
        }
        
        fun default(): UnitPreferences {
            return UnitPreferences()
        }
    }
}