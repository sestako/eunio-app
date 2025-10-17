package com.eunio.healthapp.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class UnitSystem(val displayName: String) {
    METRIC("Metric"),
    IMPERIAL("Imperial");
    
    companion object {
        /**
         * Determines the appropriate unit system based on device locale.
         * Uses Imperial for US, Liberia, and Myanmar; Metric for all other countries.
         * 
         * @param locale The device locale string (e.g., "US", "GB", "DE")
         * @return The appropriate UnitSystem for the given locale
         */
        fun fromLocale(locale: String): UnitSystem {
            return when (locale.uppercase()) {
                "US", "LR", "MM" -> IMPERIAL // United States, Liberia, Myanmar
                else -> METRIC
            }
        }
    }
}