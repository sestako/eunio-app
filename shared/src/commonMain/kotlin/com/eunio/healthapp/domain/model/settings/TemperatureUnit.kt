package com.eunio.healthapp.domain.model.settings

import kotlinx.serialization.Serializable

@Serializable
enum class TemperatureUnit(val displayName: String, val symbol: String) {
    CELSIUS("Celsius", "°C"),
    FAHRENHEIT("Fahrenheit", "°F");
    
    companion object {
        /**
         * Determines the appropriate temperature unit based on device locale.
         * Uses Fahrenheit for US, Liberia, Myanmar, and some Caribbean countries; Celsius for all others.
         * 
         * @param locale The device locale string (e.g., "US", "GB", "DE")
         * @return The appropriate TemperatureUnit for the given locale
         */
        fun fromLocale(locale: String): TemperatureUnit {
            return when (locale.uppercase()) {
                "US", "LR", "MM", "BS", "BZ", "KY", "PW" -> FAHRENHEIT
                else -> CELSIUS
            }
        }
        
        /**
         * Converts temperature from one unit to another
         * 
         * @param value The temperature value to convert
         * @param from The source temperature unit
         * @param to The target temperature unit
         * @return The converted temperature value
         */
        fun convert(value: Double, from: TemperatureUnit, to: TemperatureUnit): Double {
            if (from == to) return value
            
            return when {
                from == CELSIUS && to == FAHRENHEIT -> (value * 9.0 / 5.0) + 32.0
                from == FAHRENHEIT && to == CELSIUS -> (value - 32.0) * 5.0 / 9.0
                else -> value
            }
        }
    }
}