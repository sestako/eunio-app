package com.eunio.healthapp.platform.haptic

import com.eunio.healthapp.domain.model.settings.HapticIntensity

/**
 * Platform-specific haptic feedback manager interface.
 */
interface HapticFeedbackManager {
    
    /**
     * Performs haptic feedback with the specified intensity.
     * 
     * @param intensity The haptic feedback intensity
     */
    suspend fun performHapticFeedback(intensity: HapticIntensity)
    
    /**
     * Performs a light haptic feedback (e.g., for button taps).
     */
    suspend fun performLightHaptic()
    
    /**
     * Performs a medium haptic feedback (e.g., for selections).
     */
    suspend fun performMediumHaptic()
    
    /**
     * Performs a strong haptic feedback (e.g., for confirmations).
     */
    suspend fun performStrongHaptic()
    
    /**
     * Performs a success haptic pattern.
     */
    suspend fun performSuccessHaptic()
    
    /**
     * Performs an error haptic pattern.
     */
    suspend fun performErrorHaptic()
    
    /**
     * Performs a warning haptic pattern.
     */
    suspend fun performWarningHaptic()
    
    /**
     * Checks if haptic feedback is available on the device.
     * 
     * @return true if haptic feedback is supported, false otherwise
     */
    fun isHapticFeedbackAvailable(): Boolean
    
    /**
     * Checks if the user has enabled haptic feedback in system settings.
     * 
     * @return true if system haptic feedback is enabled, false otherwise
     */
    fun isSystemHapticEnabled(): Boolean
}