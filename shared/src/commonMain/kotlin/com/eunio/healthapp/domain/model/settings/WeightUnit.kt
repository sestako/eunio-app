package com.eunio.healthapp.domain.model.settings

import kotlinx.serialization.Serializable

@Serializable
enum class WeightUnit(val displayName: String, val symbol: String) {
    KILOGRAMS("Kilograms", "kg"),
    POUNDS("Pounds", "lbs");
    
    companion object {
        /**
         * Determines the appropriate weight unit based on device locale.
         * Uses Pounds for US, Liberia, and Myanmar; Kilograms for all other countries.
         * 
         * @param locale The device locale string (e.g., "US", "GB", "DE")
         * @return The appropriate WeightUnit for the given locale
         */
        fun fromLocale(locale: String): WeightUnit {
            return when (locale.uppercase()) {
                "US", "LR", "MM" -> POUNDS
                else -> KILOGRAMS
            }
        }
        
        /**
         * Converts weight from one unit to another
         * 
         * @param value The weight value to convert
         * @param from The source weight unit
         * @param to The target weight unit
         * @return The converted weight value
         */
        fun convert(value: Double, from: WeightUnit, to: WeightUnit): Double {
            if (from == to) return value
            
            return when {
                from == KILOGRAMS && to == POUNDS -> value * 2.20462
                from == POUNDS && to == KILOGRAMS -> value / 2.20462
                else -> value
            }
        }
    }
}