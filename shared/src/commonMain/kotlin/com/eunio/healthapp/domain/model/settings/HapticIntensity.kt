package com.eunio.healthapp.domain.model.settings

import kotlinx.serialization.Serializable

@Serializable
enum class HapticIntensity(val displayName: String, val intensityValue: Float) {
    DISABLED("Disabled", 0.0f),
    LIGHT("Light", 0.3f),
    MEDIUM("Medium", 0.6f),
    STRONG("Strong", 1.0f);
    
    /**
     * Returns true if haptic feedback is enabled (not disabled)
     */
    val isEnabled: Boolean
        get() = this != DISABLED
    
    companion object {
        /**
         * Gets the default haptic intensity
         */
        fun default(): HapticIntensity = MEDIUM
        
        /**
         * Creates haptic intensity from a float value
         * 
         * @param value The intensity value (0.0 to 1.0)
         * @return The closest HapticIntensity enum value
         */
        fun fromValue(value: Float): HapticIntensity {
            return when {
                value <= 0.0f -> DISABLED
                value <= 0.45f -> LIGHT
                value <= 0.8f -> MEDIUM
                else -> STRONG
            }
        }
    }
}