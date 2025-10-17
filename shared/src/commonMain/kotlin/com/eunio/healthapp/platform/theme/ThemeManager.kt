package com.eunio.healthapp.platform.theme

import com.eunio.healthapp.domain.util.Result

/**
 * Platform-specific theme manager interface.
 * Handles text size scaling, high contrast mode, and other display preferences.
 */
interface ThemeManager {
    
    /**
     * Updates the text size scale for the application.
     * 
     * @param scale The text size scale factor (0.5 to 2.0)
     */
    suspend fun updateTextSizeScale(scale: Float): Result<Unit>
    
    /**
     * Updates the high contrast mode setting.
     * 
     * @param enabled Whether high contrast mode should be enabled
     */
    suspend fun updateHighContrastMode(enabled: Boolean): Result<Unit>
    
    /**
     * Gets the current text size scale.
     * 
     * @return The current text size scale factor
     */
    fun getCurrentTextSizeScale(): Float
    
    /**
     * Checks if high contrast mode is currently enabled.
     * 
     * @return true if high contrast mode is enabled, false otherwise
     */
    fun isHighContrastModeEnabled(): Boolean
    
    /**
     * Applies the current theme settings to the application.
     */
    suspend fun applyCurrentTheme(): Result<Unit>
    
    /**
     * Resets theme settings to system defaults.
     */
    suspend fun resetToSystemDefaults(): Result<Unit>
}