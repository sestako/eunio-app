package com.eunio.healthapp.platform.haptic

import com.eunio.healthapp.domain.model.settings.HapticIntensity
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType

/**
 * iOS implementation of HapticFeedbackManager using UIKit feedback generators.
 */
class IOSHapticFeedbackManager : HapticFeedbackManager {
    
    private val lightGenerator = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight)
    private val mediumGenerator = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium)
    private val heavyGenerator = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy)
    private val notificationGenerator = UINotificationFeedbackGenerator()
    
    override suspend fun performHapticFeedback(intensity: HapticIntensity) {
        when (intensity) {
            HapticIntensity.DISABLED -> return
            HapticIntensity.LIGHT -> performLightHaptic()
            HapticIntensity.MEDIUM -> performMediumHaptic()
            HapticIntensity.STRONG -> performStrongHaptic()
        }
    }
    
    override suspend fun performLightHaptic() {
        if (isSystemHapticEnabled()) {
            lightGenerator.impactOccurred()
        }
    }
    
    override suspend fun performMediumHaptic() {
        if (isSystemHapticEnabled()) {
            mediumGenerator.impactOccurred()
        }
    }
    
    override suspend fun performStrongHaptic() {
        if (isSystemHapticEnabled()) {
            heavyGenerator.impactOccurred()
        }
    }
    
    override suspend fun performSuccessHaptic() {
        if (isSystemHapticEnabled()) {
            notificationGenerator.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)
        }
    }
    
    override suspend fun performErrorHaptic() {
        if (isSystemHapticEnabled()) {
            notificationGenerator.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeError)
        }
    }
    
    override suspend fun performWarningHaptic() {
        if (isSystemHapticEnabled()) {
            notificationGenerator.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeWarning)
        }
    }
    
    override fun isHapticFeedbackAvailable(): Boolean {
        // Haptic feedback is available on iPhone 6s and later
        return true // For simplicity, assume it's available
    }
    
    override fun isSystemHapticEnabled(): Boolean {
        // For now, assume haptic feedback is enabled
        // In a real implementation, you would check system settings
        return true
    }
}