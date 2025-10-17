package com.eunio.healthapp.platform.accessibility

import platform.UIKit.*
import platform.Foundation.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * iOS implementation of accessibility manager using UIAccessibility APIs.
 */
class IOSAccessibilityManager : AccessibilityManager {
    
    override fun isScreenReaderEnabled(): Boolean {
        return UIAccessibilityIsVoiceOverRunning()
    }
    
    override fun isVoiceOverEnabled(): Boolean {
        return UIAccessibilityIsVoiceOverRunning()
    }
    
    override fun isTalkBackEnabled(): Boolean {
        // TalkBack is Android-specific, always false on iOS
        return false
    }
    
    override fun getSystemTextSizeScale(): Float {
        val contentSizeCategory = UIApplication.sharedApplication.preferredContentSizeCategory
        
        return when (contentSizeCategory) {
            UIContentSizeCategoryExtraSmall -> 0.8f
            UIContentSizeCategorySmall -> 0.9f
            UIContentSizeCategoryMedium -> 1.0f
            UIContentSizeCategoryLarge -> 1.1f
            UIContentSizeCategoryExtraLarge -> 1.2f
            UIContentSizeCategoryExtraExtraLarge -> 1.3f
            UIContentSizeCategoryExtraExtraExtraLarge -> 1.4f
            UIContentSizeCategoryAccessibilityMedium -> 1.5f
            UIContentSizeCategoryAccessibilityLarge -> 1.7f
            UIContentSizeCategoryAccessibilityExtraLarge -> 1.9f
            UIContentSizeCategoryAccessibilityExtraExtraLarge -> 2.1f
            UIContentSizeCategoryAccessibilityExtraExtraExtraLarge -> 2.3f
            else -> 1.0f
        }
    }
    
    override fun isSystemHighContrastEnabled(): Boolean {
        return UIAccessibilityDarkerSystemColorsEnabled()
    }
    
    override fun isReduceMotionEnabled(): Boolean {
        return UIAccessibilityIsReduceMotionEnabled()
    }
    
    override fun isBoldTextEnabled(): Boolean {
        return UIAccessibilityIsBoldTextEnabled()
    }
    
    override fun isButtonShapesEnabled(): Boolean {
        return UIAccessibilityButtonShapesEnabled()
    }
    
    override fun announceForAccessibility(text: String, priority: AnnouncementPriority) {
        val announcement = when (priority) {
            AnnouncementPriority.LOW -> text
            AnnouncementPriority.NORMAL -> text
            AnnouncementPriority.HIGH -> "Important: $text"
            AnnouncementPriority.URGENT -> "Alert: $text"
        }
        
        UIAccessibilityPostNotification(
            UIAccessibilityAnnouncementNotification,
            announcement
        )
    }
    
    override fun postAccessibilityEvent(event: AccessibilityEvent) {
        when (event) {
            is AccessibilityEvent.ContentChanged -> {
                UIAccessibilityPostNotification(
                    UIAccessibilityLayoutChangedNotification,
                    event.description
                )
            }
            is AccessibilityEvent.ViewFocused -> {
                UIAccessibilityPostNotification(
                    UIAccessibilityScreenChangedNotification,
                    event.description
                )
            }
            is AccessibilityEvent.ViewSelected -> {
                announceForAccessibility("Selected: ${event.description}")
            }
            is AccessibilityEvent.StateChanged -> {
                announceForAccessibility(event.description)
            }
            is AccessibilityEvent.Notification -> {
                announceForAccessibility(event.message, AnnouncementPriority.HIGH)
            }
        }
    }
    
    override fun isAccessibilitySupported(): Boolean {
        return true // iOS always supports accessibility features
    }
}