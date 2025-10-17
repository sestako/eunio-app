package com.eunio.healthapp.platform.accessibility

/**
 * Platform-specific accessibility manager interface.
 */
interface AccessibilityManager {
    
    /**
     * Checks if any screen reader is enabled.
     * 
     * @return true if screen reader is active, false otherwise
     */
    fun isScreenReaderEnabled(): Boolean
    
    /**
     * Checks if VoiceOver is enabled (iOS).
     * 
     * @return true if VoiceOver is active, false otherwise
     */
    fun isVoiceOverEnabled(): Boolean
    
    /**
     * Checks if TalkBack is enabled (Android).
     * 
     * @return true if TalkBack is active, false otherwise
     */
    fun isTalkBackEnabled(): Boolean
    
    /**
     * Gets the system text size scale factor.
     * 
     * @return the system text scale (1.0 = normal, >1.0 = larger, <1.0 = smaller)
     */
    fun getSystemTextSizeScale(): Float
    
    /**
     * Checks if system high contrast mode is enabled.
     * 
     * @return true if high contrast is enabled, false otherwise
     */
    fun isSystemHighContrastEnabled(): Boolean
    
    /**
     * Checks if reduce motion is enabled in system settings.
     * 
     * @return true if reduce motion is enabled, false otherwise
     */
    fun isReduceMotionEnabled(): Boolean
    
    /**
     * Checks if bold text is enabled in system settings.
     * 
     * @return true if bold text is enabled, false otherwise
     */
    fun isBoldTextEnabled(): Boolean
    
    /**
     * Checks if button shapes are enabled in system settings.
     * 
     * @return true if button shapes are enabled, false otherwise
     */
    fun isButtonShapesEnabled(): Boolean
    
    /**
     * Announces text to screen readers.
     * 
     * @param text The text to announce
     * @param priority The announcement priority
     */
    fun announceForAccessibility(text: String, priority: AnnouncementPriority = AnnouncementPriority.NORMAL)
    
    /**
     * Posts an accessibility event.
     * 
     * @param event The accessibility event to post
     */
    fun postAccessibilityEvent(event: AccessibilityEvent)
    
    /**
     * Checks if the device supports accessibility features.
     * 
     * @return true if accessibility features are supported, false otherwise
     */
    fun isAccessibilitySupported(): Boolean
}

/**
 * Priority levels for accessibility announcements.
 */
enum class AnnouncementPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}

/**
 * Accessibility events that can be posted.
 */
sealed class AccessibilityEvent {
    data class ContentChanged(val description: String) : AccessibilityEvent()
    data class ViewFocused(val description: String) : AccessibilityEvent()
    data class ViewSelected(val description: String) : AccessibilityEvent()
    data class StateChanged(val description: String) : AccessibilityEvent()
    data class Notification(val message: String) : AccessibilityEvent()
}