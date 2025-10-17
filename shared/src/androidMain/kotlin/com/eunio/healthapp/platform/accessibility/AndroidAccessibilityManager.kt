package com.eunio.healthapp.platform.accessibility

import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager as AndroidAccessibilityManager
import android.view.accessibility.AccessibilityEvent as AndroidAccessibilityEvent
import android.content.res.Configuration
import android.os.Build

/**
 * Android implementation of accessibility manager using Android accessibility APIs.
 */
class AndroidAccessibilityManager(
    private val context: Context
) : AccessibilityManager {
    
    private val accessibilityManager: AndroidAccessibilityManager by lazy {
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AndroidAccessibilityManager
    }
    
    override fun isScreenReaderEnabled(): Boolean {
        return accessibilityManager.isEnabled && accessibilityManager.isTouchExplorationEnabled
    }
    
    override fun isVoiceOverEnabled(): Boolean {
        // VoiceOver is iOS-specific, always false on Android
        return false
    }
    
    override fun isTalkBackEnabled(): Boolean {
        return isScreenReaderEnabled() // TalkBack is the primary screen reader on Android
    }
    
    override fun getSystemTextSizeScale(): Float {
        return context.resources.configuration.fontScale
    }
    
    override fun isSystemHighContrastEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                Settings.Secure.getInt(
                    context.contentResolver,
                    "high_text_contrast_enabled",
                    0
                ) == 1
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }
    
    override fun isReduceMotionEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            try {
                Settings.Global.getFloat(
                    context.contentResolver,
                    Settings.Global.ANIMATOR_DURATION_SCALE,
                    1.0f
                ) == 0.0f
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }
    
    override fun isBoldTextEnabled(): Boolean {
        // Android doesn't have a system-wide bold text setting like iOS
        // We can check if the font weight is increased through configuration (API 31+)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.resources.configuration.fontWeightAdjustment > 0
        } else {
            false // Not available on older Android versions
        }
    }
    
    override fun isButtonShapesEnabled(): Boolean {
        // Android doesn't have a direct equivalent to iOS button shapes
        // We'll return false as this is iOS-specific
        return false
    }
    
    override fun announceForAccessibility(text: String, priority: AnnouncementPriority) {
        if (!accessibilityManager.isEnabled) return
        
        val announcement = when (priority) {
            AnnouncementPriority.LOW -> text
            AnnouncementPriority.NORMAL -> text
            AnnouncementPriority.HIGH -> text // Android doesn't have priority levels for announcements
            AnnouncementPriority.URGENT -> text
        }
        
        val event = AndroidAccessibilityEvent.obtain(AndroidAccessibilityEvent.TYPE_ANNOUNCEMENT)
        event.text.add(announcement)
        event.className = this::class.java.name
        event.packageName = context.packageName
        
        accessibilityManager.sendAccessibilityEvent(event)
    }
    
    override fun postAccessibilityEvent(event: AccessibilityEvent) {
        if (!accessibilityManager.isEnabled) return
        
        val androidEvent = when (event) {
            is AccessibilityEvent.ContentChanged -> {
                AndroidAccessibilityEvent.obtain(AndroidAccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED).apply {
                    text.add(event.description)
                }
            }
            is AccessibilityEvent.ViewFocused -> {
                AndroidAccessibilityEvent.obtain(AndroidAccessibilityEvent.TYPE_VIEW_FOCUSED).apply {
                    text.add(event.description)
                }
            }
            is AccessibilityEvent.ViewSelected -> {
                AndroidAccessibilityEvent.obtain(AndroidAccessibilityEvent.TYPE_VIEW_SELECTED).apply {
                    text.add(event.description)
                }
            }
            is AccessibilityEvent.StateChanged -> {
                AndroidAccessibilityEvent.obtain(AndroidAccessibilityEvent.TYPE_WINDOW_STATE_CHANGED).apply {
                    text.add(event.description)
                }
            }
            is AccessibilityEvent.Notification -> {
                AndroidAccessibilityEvent.obtain(AndroidAccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED).apply {
                    text.add(event.message)
                }
            }
        }
        
        androidEvent.className = this::class.java.name
        androidEvent.packageName = context.packageName
        
        accessibilityManager.sendAccessibilityEvent(androidEvent)
    }
    
    override fun isAccessibilitySupported(): Boolean {
        return true // Android always supports accessibility features
    }
}