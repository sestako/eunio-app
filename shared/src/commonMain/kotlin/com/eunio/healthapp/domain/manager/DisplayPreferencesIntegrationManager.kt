package com.eunio.healthapp.domain.manager

import com.eunio.healthapp.domain.model.settings.DisplayPreferences
import com.eunio.healthapp.domain.model.settings.HapticIntensity
import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Manager that integrates display preferences with UI components throughout the app.
 * Applies text size, contrast, and haptic feedback settings to all UI components.
 */
interface DisplayPreferencesIntegrationManager {
    
    /**
     * Applies display preferences to all UI components
     */
    suspend fun applyDisplayPreferences(): Result<Unit>
    
    /**
     * Observes display preference changes and applies them immediately
     */
    fun observeAndApplyDisplayChanges(): Flow<DisplayPreferences>
    
    /**
     * Gets current text size scale for UI components
     */
    suspend fun getCurrentTextSizeScale(): Float
    
    /**
     * Gets current high contrast mode setting
     */
    suspend fun isHighContrastModeEnabled(): Boolean
    
    /**
     * Gets current haptic feedback settings
     */
    suspend fun getCurrentHapticSettings(): HapticIntensity
    
    /**
     * Applies text size scaling to the app theme
     */
    suspend fun applyTextSizeScaling(scale: Float): Result<Unit>
    
    /**
     * Applies high contrast mode to the app theme
     */
    suspend fun applyHighContrastMode(enabled: Boolean): Result<Unit>
    
    /**
     * Applies haptic feedback settings to platform services
     */
    suspend fun applyHapticSettings(intensity: HapticIntensity): Result<Unit>
}

/**
 * Implementation that integrates display preferences with platform-specific services
 */
class DisplayPreferencesIntegrationManagerImpl(
    private val settingsManager: SettingsManager,
    private val hapticFeedbackManager: com.eunio.healthapp.platform.haptic.HapticFeedbackManager,
    private val themeManager: com.eunio.healthapp.platform.theme.ThemeManager
) : DisplayPreferencesIntegrationManager {
    
    override suspend fun applyDisplayPreferences(): Result<Unit> {
        return try {
            val settings = settingsManager.getUserSettings().getOrNull()
                ?: return Result.error(AppError.ValidationError("No user settings found"))
            
            val displayPrefs = settings.displayPreferences
            
            // Apply all display preferences and check results
            val textScaleResult = applyTextSizeScaling(displayPrefs.textSizeScale)
            if (textScaleResult.isError) return textScaleResult
            
            val contrastResult = applyHighContrastMode(displayPrefs.highContrastMode)
            if (contrastResult.isError) return contrastResult
            
            val hapticResult = applyHapticSettings(displayPrefs.hapticIntensity)
            if (hapticResult.isError) return hapticResult
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(AppError.ValidationError("Failed to apply display preferences: ${e.message}"))
        }
    }
    
    override fun observeAndApplyDisplayChanges(): Flow<DisplayPreferences> {
        return settingsManager.observeSettingsChanges()
            .map { settings ->
                val displayPrefs = settings.displayPreferences
                
                // Apply changes immediately when preferences change
                applyTextSizeScaling(displayPrefs.textSizeScale)
                applyHighContrastMode(displayPrefs.highContrastMode)
                applyHapticSettings(displayPrefs.hapticIntensity)
                
                displayPrefs
            }
    }
    
    override suspend fun getCurrentTextSizeScale(): Float {
        return settingsManager.getUserSettings()
            .getOrNull()?.displayPreferences?.textSizeScale ?: 1.0f
    }
    
    override suspend fun isHighContrastModeEnabled(): Boolean {
        return settingsManager.getUserSettings()
            .getOrNull()?.displayPreferences?.highContrastMode ?: false
    }
    
    override suspend fun getCurrentHapticSettings(): HapticIntensity {
        return settingsManager.getUserSettings()
            .getOrNull()?.displayPreferences?.hapticIntensity ?: HapticIntensity.MEDIUM
    }
    
    override suspend fun applyTextSizeScaling(scale: Float): Result<Unit> {
        return try {
            themeManager.updateTextSizeScale(scale)
        } catch (e: Exception) {
            Result.error(AppError.ValidationError("Failed to apply text size scaling: ${e.message}"))
        }
    }
    
    override suspend fun applyHighContrastMode(enabled: Boolean): Result<Unit> {
        return try {
            themeManager.updateHighContrastMode(enabled)
        } catch (e: Exception) {
            Result.error(AppError.ValidationError("Failed to apply high contrast mode: ${e.message}"))
        }
    }
    
    override suspend fun applyHapticSettings(intensity: HapticIntensity): Result<Unit> {
        return try {
            hapticFeedbackManager.performHapticFeedback(intensity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(AppError.ValidationError("Failed to apply haptic settings: ${e.message}"))
        }
    }
}

/**
 * Types of haptic feedback
 */
enum class HapticFeedbackType {
    LIGHT,
    MEDIUM,
    HEAVY,
    SUCCESS,
    WARNING,
    ERROR
}