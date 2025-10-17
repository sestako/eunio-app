package com.eunio.healthapp.presentation.state

import com.eunio.healthapp.domain.model.settings.DisplayPreferences
import com.eunio.healthapp.domain.model.settings.HapticIntensity
import com.eunio.healthapp.presentation.viewmodel.UiState

/**
 * UI state for display preferences screen.
 */
data class DisplayPreferencesUiState(
    val preferences: DisplayPreferences = DisplayPreferences.default(),
    val loadingState: LoadingState = LoadingState.Idle,
    val isUpdating: Boolean = false,
    val isTesting: Boolean = false,
    val testingHapticIntensity: HapticIntensity? = null,
    val validationErrors: List<String> = emptyList(),
    val previewText: String = "Sample text for size preview",
    val accessibilityInfo: AccessibilityInfo = AccessibilityInfo()
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
     * Whether there are validation errors.
     */
    val hasValidationErrors: Boolean
        get() = validationErrors.isNotEmpty()
    
    /**
     * Whether haptic feedback testing is available.
     */
    val canTestHaptic: Boolean
        get() = isEnabled && preferences.hapticFeedbackEnabled && !isTesting
    
    /**
     * Whether accessibility features are enabled.
     */
    val hasAccessibilityFeaturesEnabled: Boolean
        get() = preferences.hasAccessibilityFeaturesEnabled()
    
    /**
     * Gets the text size scale as a percentage.
     */
    fun getTextSizePercentage(): Int = (preferences.textSizeScale * 100).toInt()
    
    /**
     * Gets the text size description.
     */
    fun getTextSizeDescription(): String {
        return when {
            preferences.textSizeScale < 0.9f -> "Small"
            preferences.textSizeScale < 1.1f -> "Normal"
            preferences.textSizeScale < 1.3f -> "Large"
            preferences.textSizeScale < 1.6f -> "Extra Large"
            else -> "Accessibility Large"
        }
    }
    
    /**
     * Gets the haptic intensity description.
     */
    fun getHapticIntensityDescription(): String {
        return if (preferences.hapticFeedbackEnabled) {
            preferences.hapticIntensity.displayName
        } else {
            "Disabled"
        }
    }
    
    /**
     * Gets the contrast mode description.
     */
    fun getContrastModeDescription(): String {
        return if (preferences.highContrastMode) {
            "High contrast enabled"
        } else {
            "Standard contrast"
        }
    }
    
    /**
     * Gets accessibility compliance status.
     */
    fun getAccessibilityComplianceStatus(): AccessibilityComplianceStatus {
        val issues = mutableListOf<String>()
        
        // Check text size accessibility
        if (preferences.textSizeScale < DisplayPreferences.MIN_TEXT_SCALE) {
            issues.add("Text size too small for accessibility")
        }
        
        // Check contrast requirements
        if (!preferences.highContrastMode && hasAccessibilityFeaturesEnabled) {
            issues.add("Consider enabling high contrast mode")
        }
        
        // Check haptic feedback for accessibility
        if (!preferences.hapticFeedbackEnabled) {
            issues.add("Haptic feedback disabled - may affect accessibility")
        }
        
        return AccessibilityComplianceStatus(
            isCompliant = issues.isEmpty(),
            issues = issues,
            recommendations = getAccessibilityRecommendations()
        )
    }
    
    /**
     * Gets accessibility recommendations.
     */
    private fun getAccessibilityRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (preferences.textSizeScale == DisplayPreferences.DEFAULT_TEXT_SCALE) {
            recommendations.add("Consider increasing text size if you have vision difficulties")
        }
        
        if (!preferences.highContrastMode) {
            recommendations.add("Enable high contrast mode for better visibility")
        }
        
        if (preferences.hapticIntensity == HapticIntensity.LIGHT) {
            recommendations.add("Consider stronger haptic feedback for better tactile response")
        }
        
        return recommendations
    }
    
    /**
     * Whether the current settings are optimal for accessibility.
     */
    val isAccessibilityOptimized: Boolean
        get() = preferences.textSizeScale >= 1.2f && 
                preferences.highContrastMode && 
                preferences.hapticFeedbackEnabled &&
                preferences.hapticIntensity != HapticIntensity.LIGHT
}

/**
 * Accessibility information and compliance status.
 */
data class AccessibilityInfo(
    val screenReaderEnabled: Boolean = false,
    val voiceOverEnabled: Boolean = false,
    val talkBackEnabled: Boolean = false,
    val systemTextSizeScale: Float = 1.0f,
    val systemHighContrastEnabled: Boolean = false,
    val systemReduceMotionEnabled: Boolean = false
) {
    /**
     * Whether any system accessibility features are enabled.
     */
    val hasSystemAccessibilityEnabled: Boolean
        get() = screenReaderEnabled || voiceOverEnabled || talkBackEnabled ||
                systemTextSizeScale != 1.0f || systemHighContrastEnabled ||
                systemReduceMotionEnabled
    
    /**
     * Gets system accessibility description.
     */
    fun getSystemAccessibilityDescription(): String {
        val features = mutableListOf<String>()
        
        if (screenReaderEnabled || voiceOverEnabled || talkBackEnabled) {
            features.add("Screen reader")
        }
        if (systemTextSizeScale != 1.0f) {
            features.add("System text scaling")
        }
        if (systemHighContrastEnabled) {
            features.add("System high contrast")
        }
        if (systemReduceMotionEnabled) {
            features.add("Reduced motion")
        }
        
        return if (features.isEmpty()) {
            "No system accessibility features detected"
        } else {
            "System features: ${features.joinToString(", ")}"
        }
    }
}

/**
 * Accessibility compliance status.
 */
data class AccessibilityComplianceStatus(
    val isCompliant: Boolean,
    val issues: List<String>,
    val recommendations: List<String>
) {
    /**
     * Gets the compliance level.
     */
    fun getComplianceLevel(): ComplianceLevel {
        return when {
            isCompliant && recommendations.isEmpty() -> ComplianceLevel.EXCELLENT
            isCompliant -> ComplianceLevel.GOOD
            issues.size <= 1 -> ComplianceLevel.FAIR
            else -> ComplianceLevel.POOR
        }
    }
}

/**
 * Compliance level enumeration.
 */
enum class ComplianceLevel(val displayName: String, val color: String) {
    EXCELLENT("Excellent", "#4CAF50"),
    GOOD("Good", "#8BC34A"),
    FAIR("Fair", "#FF9800"),
    POOR("Poor", "#F44336")
}