package com.eunio.healthapp.testutil

import android.content.Context
import android.content.SharedPreferences
import com.eunio.healthapp.platform.PlatformManager
import com.eunio.healthapp.platform.PlatformLifecycleManager
import com.eunio.healthapp.platform.PlatformNavigationManager
import com.eunio.healthapp.platform.PlatformPerformanceMetrics
import com.eunio.healthapp.platform.haptic.HapticFeedbackManager
import com.eunio.healthapp.platform.theme.ThemeManager
import com.eunio.healthapp.platform.accessibility.AccessibilityManager
import com.eunio.healthapp.domain.model.settings.HapticIntensity
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.presentation.navigation.NavigationDestination
import kotlinx.datetime.Instant
import kotlinx.datetime.Clock
import java.io.File

/**
 * Android-specific test context and utilities for creating mock Android components
 */
class AndroidTestContext {
    
    companion object {
        /**
         * Creates a mock Android Context for testing
         */
        fun createMockContext(): MockAndroidContext {
            return MockAndroidContext()
        }
        
        /**
         * Creates mock SharedPreferences for testing
         */
        fun createMockSharedPreferences(): MockSharedPreferences {
            return MockSharedPreferences()
        }
        
        /**
         * Creates a mock Android PlatformManager for testing
         */
        fun createMockPlatformManager(): MockAndroidPlatformManager {
            return MockAndroidPlatformManager()
        }
        
        /**
         * Creates a mock Android HapticFeedbackManager for testing
         */
        fun createMockHapticFeedbackManager(): MockAndroidHapticFeedbackManager {
            return MockAndroidHapticFeedbackManager()
        }
        
        /**
         * Creates a mock Android ThemeManager for testing
         */
        fun createMockThemeManager(): MockAndroidThemeManager {
            return MockAndroidThemeManager()
        }
        
        /**
         * Creates a mock Android AccessibilityManager for testing
         */
        fun createMockAccessibilityManager(): MockAndroidAccessibilityManager {
            return MockAndroidAccessibilityManager()
        }
        
        /**
         * Creates a mock Android LifecycleManager for testing
         */
        fun createMockLifecycleManager(): MockAndroidLifecycleManager {
            return MockAndroidLifecycleManager()
        }
        
        /**
         * Creates a mock Android NavigationManager for testing
         */
        fun createMockNavigationManager(): MockAndroidNavigationManager {
            return MockAndroidNavigationManager()
        }
    }
}

/**
 * Mock Android Context implementation for testing
 * Note: This is a simplified mock that doesn't extend Context to avoid Android API dependencies
 */
class MockAndroidContext {
    private val sharedPreferencesMap = mutableMapOf<String, MockSharedPreferences>()
    private val mockCacheDir = File("/tmp/test-cache")
    private val mockFilesDir = File("/tmp/test-files")
    
    init {
        // Ensure test directories exist
        mockCacheDir.mkdirs()
        mockFilesDir.mkdirs()
    }
    
    fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
        return sharedPreferencesMap.getOrPut(name) { MockSharedPreferences() }
    }
    
    fun getCacheDir(): File = mockCacheDir
    
    fun getFilesDir(): File = mockFilesDir
    
    fun getPackageName(): String = "com.eunio.healthapp.test"
    
    fun getApplicationContext(): MockAndroidContext = this
}

/**
 * Mock SharedPreferences implementation for testing
 */
class MockSharedPreferences : SharedPreferences {
    private val data = mutableMapOf<String, Any?>()
    private val listeners = mutableSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()
    
    override fun getAll(): Map<String, *> = data.toMap()
    
    override fun getString(key: String, defValue: String?): String? {
        return data[key] as? String ?: defValue
    }
    
    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? {
        @Suppress("UNCHECKED_CAST")
        return data[key] as? Set<String> ?: defValues
    }
    
    override fun getInt(key: String, defValue: Int): Int {
        return data[key] as? Int ?: defValue
    }
    
    override fun getLong(key: String, defValue: Long): Long {
        return data[key] as? Long ?: defValue
    }
    
    override fun getFloat(key: String, defValue: Float): Float {
        return data[key] as? Float ?: defValue
    }
    
    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        return data[key] as? Boolean ?: defValue
    }
    
    override fun contains(key: String): Boolean = data.containsKey(key)
    
    override fun edit(): SharedPreferences.Editor = MockEditor()
    
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        listeners.add(listener)
    }
    
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        listeners.remove(listener)
    }
    
    private inner class MockEditor : SharedPreferences.Editor {
        private val edits = mutableMapOf<String, Any?>()
        private val removals = mutableSetOf<String>()
        private var shouldClear = false
        
        override fun putString(key: String, value: String?): SharedPreferences.Editor {
            edits[key] = value
            return this
        }
        
        override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor {
            edits[key] = values
            return this
        }
        
        override fun putInt(key: String, value: Int): SharedPreferences.Editor {
            edits[key] = value
            return this
        }
        
        override fun putLong(key: String, value: Long): SharedPreferences.Editor {
            edits[key] = value
            return this
        }
        
        override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
            edits[key] = value
            return this
        }
        
        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
            edits[key] = value
            return this
        }
        
        override fun remove(key: String): SharedPreferences.Editor {
            removals.add(key)
            return this
        }
        
        override fun clear(): SharedPreferences.Editor {
            shouldClear = true
            return this
        }
        
        override fun commit(): Boolean {
            apply()
            return true
        }
        
        override fun apply() {
            if (shouldClear) {
                data.clear()
            }
            
            removals.forEach { key ->
                data.remove(key)
                notifyListeners(key)
            }
            
            edits.forEach { (key, value) ->
                data[key] = value
                notifyListeners(key)
            }
        }
        
        private fun notifyListeners(key: String) {
            listeners.forEach { listener ->
                listener.onSharedPreferenceChanged(this@MockSharedPreferences, key)
            }
        }
    }
}

/**
 * Mock Android PlatformManager implementation for testing
 */
class MockAndroidPlatformManager : PlatformManager {
    private var performanceOptimized = false
    private val performanceMetrics = MockAndroidPerformanceMetrics()
    
    override fun optimizePerformance() {
        performanceOptimized = true
    }
    
    override fun getPerformanceMetrics(): PlatformPerformanceMetrics = performanceMetrics
    
    override fun configureSecurityFeatures() {
        // Mock implementation - no-op for testing
    }
    
    override fun handleDeepLink(url: String): Boolean {
        // Mock implementation - always return true for testing
        return url.startsWith("eunio://") || url.startsWith("https://eunio.app/")
    }
    
    override fun shareContent(content: String, title: String) {
        // Mock implementation - no-op for testing
    }
    
    override fun openDocumentPicker() {
        // Mock implementation - no-op for testing
    }
    
    override fun getPlatformName(): String = "Android"
    
    override fun getOSVersion(): String = "14.0"
    
    override fun getDeviceModel(): String = "Test Device"
    
    override fun getScreenSize(): String = "1080x2400"
    
    override fun getLocale(): String = "en_US"
    
    override fun getAppVersion(): String = "1.0.0"
    
    override fun getBuildNumber(): String = "100"
    
    override fun getInstallDate(): Instant = Clock.System.now()
    
    override fun getLastUpdateDate(): Instant = Clock.System.now()
    
    // Test helper methods
    fun isPerformanceOptimized(): Boolean = performanceOptimized
    
    fun reset() {
        performanceOptimized = false
    }
}

/**
 * Mock Android PerformanceMetrics implementation for testing
 */
class MockAndroidPerformanceMetrics : PlatformPerformanceMetrics {
    override val deviceModel: String = "Test Android Device"
    override val systemVersion: String = "Android 14"
    override val availableMemory: Long = 2048L * 1024 * 1024 // 2GB
    override val totalMemory: Long = 8192L * 1024 * 1024 // 8GB
    override val processorCount: Int = 8
}

/**
 * Mock Android HapticFeedbackManager implementation for testing
 */
class MockAndroidHapticFeedbackManager : HapticFeedbackManager {
    private var hapticFeedbackAvailable = true
    private var systemHapticEnabled = true
    private val hapticHistory = mutableListOf<HapticEvent>()
    
    override suspend fun performHapticFeedback(intensity: HapticIntensity) {
        recordHapticEvent("performHapticFeedback", intensity.name)
    }
    
    override suspend fun performLightHaptic() {
        recordHapticEvent("performLightHaptic")
    }
    
    override suspend fun performMediumHaptic() {
        recordHapticEvent("performMediumHaptic")
    }
    
    override suspend fun performStrongHaptic() {
        recordHapticEvent("performStrongHaptic")
    }
    
    override suspend fun performSuccessHaptic() {
        recordHapticEvent("performSuccessHaptic")
    }
    
    override suspend fun performErrorHaptic() {
        recordHapticEvent("performErrorHaptic")
    }
    
    override suspend fun performWarningHaptic() {
        recordHapticEvent("performWarningHaptic")
    }
    
    override fun isHapticFeedbackAvailable(): Boolean = hapticFeedbackAvailable
    
    override fun isSystemHapticEnabled(): Boolean = systemHapticEnabled
    
    // Test helper methods
    fun setHapticFeedbackAvailable(available: Boolean) {
        hapticFeedbackAvailable = available
    }
    
    fun setSystemHapticEnabled(enabled: Boolean) {
        systemHapticEnabled = enabled
    }
    
    fun getHapticHistory(): List<HapticEvent> = hapticHistory.toList()
    
    fun clearHapticHistory() {
        hapticHistory.clear()
    }
    
    fun getLastHapticEvent(): HapticEvent? = hapticHistory.lastOrNull()
    
    fun getHapticEventCount(): Int = hapticHistory.size
    
    private fun recordHapticEvent(method: String, parameter: String? = null) {
        hapticHistory.add(HapticEvent(method, parameter, Clock.System.now()))
    }
    
    data class HapticEvent(
        val method: String,
        val parameter: String?,
        val timestamp: Instant
    )
}

/**
 * Mock Android ThemeManager implementation for testing
 */
class MockAndroidThemeManager : ThemeManager {
    private var textSizeScale: Float = 1.0f
    private var highContrastModeEnabled: Boolean = false
    private var themeApplied: Boolean = false
    
    override suspend fun updateTextSizeScale(scale: Float): Result<Unit> {
        return if (scale in 0.5f..2.0f) {
            textSizeScale = scale
            Result.Success(Unit)
        } else {
            Result.Error(com.eunio.healthapp.domain.error.AppError.ValidationError("Text size scale must be between 0.5 and 2.0"))
        }
    }
    
    override suspend fun updateHighContrastMode(enabled: Boolean): Result<Unit> {
        highContrastModeEnabled = enabled
        return Result.Success(Unit)
    }
    
    override fun getCurrentTextSizeScale(): Float = textSizeScale
    
    override fun isHighContrastModeEnabled(): Boolean = highContrastModeEnabled
    
    override suspend fun applyCurrentTheme(): Result<Unit> {
        themeApplied = true
        return Result.Success(Unit)
    }
    
    override suspend fun resetToSystemDefaults(): Result<Unit> {
        textSizeScale = 1.0f
        highContrastModeEnabled = false
        themeApplied = false
        return Result.Success(Unit)
    }
    
    // Test helper methods
    fun isThemeApplied(): Boolean = themeApplied
    
    fun reset() {
        textSizeScale = 1.0f
        highContrastModeEnabled = false
        themeApplied = false
    }
}

/**
 * Mock Android AccessibilityManager implementation for testing
 */
class MockAndroidAccessibilityManager : AccessibilityManager {
    private var screenReaderEnabled = false
    private var voiceOverEnabled = false
    private var talkBackEnabled = false
    private var systemTextSizeScale = 1.0f
    private var systemHighContrastEnabled = false
    private var reduceMotionEnabled = false
    private var boldTextEnabled = false
    private var buttonShapesEnabled = false
    private val announcementHistory = mutableListOf<String>()
    private val eventHistory = mutableListOf<com.eunio.healthapp.platform.accessibility.AccessibilityEvent>()
    
    override fun isScreenReaderEnabled(): Boolean = screenReaderEnabled
    
    override fun isVoiceOverEnabled(): Boolean = voiceOverEnabled
    
    override fun isTalkBackEnabled(): Boolean = talkBackEnabled
    
    override fun getSystemTextSizeScale(): Float = systemTextSizeScale
    
    override fun isSystemHighContrastEnabled(): Boolean = systemHighContrastEnabled
    
    override fun isReduceMotionEnabled(): Boolean = reduceMotionEnabled
    
    override fun isBoldTextEnabled(): Boolean = boldTextEnabled
    
    override fun isButtonShapesEnabled(): Boolean = buttonShapesEnabled
    
    override fun announceForAccessibility(text: String, priority: com.eunio.healthapp.platform.accessibility.AnnouncementPriority) {
        announcementHistory.add("$text (priority: $priority)")
    }
    
    override fun postAccessibilityEvent(event: com.eunio.healthapp.platform.accessibility.AccessibilityEvent) {
        eventHistory.add(event)
    }
    
    override fun isAccessibilitySupported(): Boolean = true
    
    // Test helper methods
    fun setScreenReaderEnabled(enabled: Boolean) {
        screenReaderEnabled = enabled
    }
    
    fun setVoiceOverEnabled(enabled: Boolean) {
        voiceOverEnabled = enabled
    }
    
    fun setTalkBackEnabled(enabled: Boolean) {
        talkBackEnabled = enabled
    }
    
    fun setSystemTextSizeScale(scale: Float) {
        systemTextSizeScale = scale
    }
    
    fun setSystemHighContrastEnabled(enabled: Boolean) {
        systemHighContrastEnabled = enabled
    }
    
    fun setReduceMotionEnabled(enabled: Boolean) {
        reduceMotionEnabled = enabled
    }
    
    fun setBoldTextEnabled(enabled: Boolean) {
        boldTextEnabled = enabled
    }
    
    fun setButtonShapesEnabled(enabled: Boolean) {
        buttonShapesEnabled = enabled
    }
    
    fun getAnnouncementHistory(): List<String> = announcementHistory.toList()
    
    fun getEventHistory(): List<com.eunio.healthapp.platform.accessibility.AccessibilityEvent> = eventHistory.toList()
    
    fun clearHistory() {
        announcementHistory.clear()
        eventHistory.clear()
    }
    
    fun reset() {
        screenReaderEnabled = false
        voiceOverEnabled = false
        talkBackEnabled = false
        systemTextSizeScale = 1.0f
        systemHighContrastEnabled = false
        reduceMotionEnabled = false
        boldTextEnabled = false
        buttonShapesEnabled = false
        announcementHistory.clear()
        eventHistory.clear()
    }
}

/**
 * Mock Android LifecycleManager implementation for testing
 */
class MockAndroidLifecycleManager : PlatformLifecycleManager {
    private var isStarted = false
    private var isPaused = false
    private val lifecycleEvents = mutableListOf<LifecycleEvent>()
    
    override fun onAppStart() {
        isStarted = true
        isPaused = false
        recordLifecycleEvent("onAppStart")
    }
    
    override fun onAppStop() {
        isStarted = false
        recordLifecycleEvent("onAppStop")
    }
    
    override fun onAppPause() {
        isPaused = true
        recordLifecycleEvent("onAppPause")
    }
    
    override fun onAppResume() {
        isPaused = false
        recordLifecycleEvent("onAppResume")
    }
    
    override fun onLowMemory() {
        recordLifecycleEvent("onLowMemory")
    }
    
    override fun cleanup() {
        recordLifecycleEvent("cleanup")
    }
    
    // Test helper methods
    fun isStarted(): Boolean = isStarted
    
    fun isPaused(): Boolean = isPaused
    
    fun getLifecycleEvents(): List<LifecycleEvent> = lifecycleEvents.toList()
    
    fun clearLifecycleEvents() {
        lifecycleEvents.clear()
    }
    
    fun getLastLifecycleEvent(): LifecycleEvent? = lifecycleEvents.lastOrNull()
    
    fun reset() {
        isStarted = false
        isPaused = false
        lifecycleEvents.clear()
    }
    
    private fun recordLifecycleEvent(event: String, parameter: String? = null) {
        lifecycleEvents.add(LifecycleEvent(event, parameter, Clock.System.now()))
    }
    
    data class LifecycleEvent(
        val event: String,
        val parameter: String?,
        val timestamp: Instant
    )
}

/**
 * Mock Android NavigationManager implementation for testing
 */
class MockAndroidNavigationManager : PlatformNavigationManager {
    private val navigationHistory = mutableListOf<NavigationEvent>()
    private var currentDestination: NavigationDestination? = null
    
    override fun navigate(destination: NavigationDestination) {
        currentDestination = destination
        recordNavigationEvent("navigate", destination.toString())
    }
    
    override fun navigateBack(): Boolean {
        recordNavigationEvent("navigateBack")
        return true // Mock always successful
    }
    
    override fun handleDeepLink(url: String): Boolean {
        recordNavigationEvent("handleDeepLink", url)
        return url.startsWith("eunio://") || url.startsWith("https://eunio.app/")
    }
    
    override fun shareHealthReport(reportPath: String) {
        recordNavigationEvent("shareHealthReport", reportPath)
    }
    
    // Test helper methods
    fun getCurrentDestination(): NavigationDestination? = currentDestination
    
    fun getNavigationHistory(): List<NavigationEvent> = navigationHistory.toList()
    
    fun clearNavigationHistory() {
        navigationHistory.clear()
    }
    
    fun getLastNavigationEvent(): NavigationEvent? = navigationHistory.lastOrNull()
    
    fun getNavigationEventCount(): Int = navigationHistory.size
    
    fun reset() {
        currentDestination = null
        navigationHistory.clear()
    }
    
    private fun recordNavigationEvent(action: String, parameter: String? = null) {
        navigationHistory.add(NavigationEvent(action, parameter, Clock.System.now()))
    }
    
    data class NavigationEvent(
        val action: String,
        val parameter: String?,
        val timestamp: Instant
    )
}