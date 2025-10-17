package com.eunio.healthapp.domain.model.settings

import kotlinx.serialization.Serializable

@Serializable
data class DisplayPreferences(
    val textSizeScale: Float = 1.0f,
    val highContrastMode: Boolean = false,
    val hapticFeedbackEnabled: Boolean = true,
    val hapticIntensity: HapticIntensity = HapticIntensity.MEDIUM
) {
    /**
     * Validates that display preferences are within acceptable ranges
     * 
     * @return true if preferences are valid, false otherwise
     */
    fun isValid(): Boolean {
        return when {
            textSizeScale < MIN_TEXT_SCALE || textSizeScale > MAX_TEXT_SCALE -> false
            !hapticFeedbackEnabled && hapticIntensity != HapticIntensity.DISABLED -> false
            else -> true
        }
    }
    
    /**
     * Returns validation errors if any exist
     */
    fun getValidationErrors(): List<String> {
        val errors = mutableListOf<String>()
        
        if (textSizeScale < MIN_TEXT_SCALE || textSizeScale > MAX_TEXT_SCALE) {
            errors.add("Text size scale must be between $MIN_TEXT_SCALE and $MAX_TEXT_SCALE")
        }
        
        if (!hapticFeedbackEnabled && hapticIntensity != HapticIntensity.DISABLED) {
            errors.add("Haptic intensity should be disabled when haptic feedback is disabled")
        }
        
        return errors
    }
    
    /**
     * Returns true if accessibility features are enabled
     */
    fun hasAccessibilityFeaturesEnabled(): Boolean {
        return highContrastMode || textSizeScale != 1.0f
    }
    
    /**
     * Creates a copy with haptic feedback properly configured
     */
    fun withHapticFeedback(enabled: Boolean, intensity: HapticIntensity = HapticIntensity.MEDIUM): DisplayPreferences {
        return copy(
            hapticFeedbackEnabled = enabled,
            hapticIntensity = if (enabled) intensity else HapticIntensity.DISABLED
        )
    }
    
    companion object {
        // Text scale limits for accessibility
        const val MIN_TEXT_SCALE = 0.8f
        const val MAX_TEXT_SCALE = 2.0f
        const val DEFAULT_TEXT_SCALE = 1.0f
        
        fun default(): DisplayPreferences {
            return DisplayPreferences()
        }
        
        /**
         * Creates display preferences optimized for accessibility
         */
        fun accessibilityOptimized(): DisplayPreferences {
            return DisplayPreferences(
                textSizeScale = 1.2f,
                highContrastMode = true,
                hapticFeedbackEnabled = true,
                hapticIntensity = HapticIntensity.STRONG
            )
        }
        
        /**
         * Creates display preferences with minimal visual effects
         */
        fun minimal(): DisplayPreferences {
            return DisplayPreferences(
                textSizeScale = 1.0f,
                highContrastMode = false,
                hapticFeedbackEnabled = false,
                hapticIntensity = HapticIntensity.DISABLED
            )
        }
        
        /**
         * Creates display preferences with validation
         * 
         * @param textScale The text size scale
         * @param highContrast Whether high contrast mode is enabled
         * @param hapticEnabled Whether haptic feedback is enabled
         * @param hapticIntensity The haptic feedback intensity
         * @return Valid DisplayPreferences or default if invalid
         */
        fun create(
            textScale: Float,
            highContrast: Boolean,
            hapticEnabled: Boolean,
            hapticIntensity: HapticIntensity
        ): DisplayPreferences {
            val preferences = DisplayPreferences(
                textSizeScale = textScale.coerceIn(MIN_TEXT_SCALE, MAX_TEXT_SCALE),
                highContrastMode = highContrast,
                hapticFeedbackEnabled = hapticEnabled,
                hapticIntensity = if (hapticEnabled) hapticIntensity else HapticIntensity.DISABLED
            )
            
            return if (preferences.isValid()) preferences else default()
        }
    }
}