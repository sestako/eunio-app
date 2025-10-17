package com.eunio.healthapp.testutil

import com.eunio.healthapp.platform.PlatformManager
import com.eunio.healthapp.platform.PlatformLifecycleManager
import com.eunio.healthapp.platform.PlatformNavigationManager
import com.eunio.healthapp.platform.PlatformPerformanceMetrics
import com.eunio.healthapp.platform.haptic.HapticFeedbackManager
import com.eunio.healthapp.platform.theme.ThemeManager
import com.eunio.healthapp.platform.notification.PlatformNotificationService
import com.eunio.healthapp.platform.accessibility.AccessibilityManager
import com.eunio.healthapp.platform.accessibility.AnnouncementPriority
import com.eunio.healthapp.domain.model.settings.HapticIntensity
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.domain.util.NetworkConnectivity
import com.eunio.healthapp.domain.util.NetworkType
import com.eunio.healthapp.presentation.navigation.NavigationDestination
import kotlinx.datetime.Instant
import kotlinx.datetime.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Factory for creating consistent mock services across all test scenarios
 */
class MockServiceFactory {
    
    companion object {
        /**
         * Creates a complete set of mock services for testing
         */
        fun createMockServiceSet(): MockServiceSet {
            return MockServiceSet(
                networkConnectivity = createMockNetworkConnectivity(),
                platformManager = createMockPlatformManager(),
                hapticFeedbackManager = createMockHapticFeedbackManager(),
                themeManager = createMockThemeManager(),
                notificationService = createMockNotificationService(),
                accessibilityManager = createMockAccessibilityManager(),
                lifecycleManager = createMockLifecycleManager(),
                navigationManager = createMockNavigationManager()
            )
        }
        
        /**
         * Creates a mock NetworkConnectivity
         */
        fun createMockNetworkConnectivity(): com.eunio.healthapp.testutil.MockNetworkConnectivity {
            return com.eunio.healthapp.testutil.MockNetworkConnectivity()
        }
        
        /**
         * Creates a mock PlatformManager
         */
        fun createMockPlatformManager(): MockPlatformManager {
            return MockPlatformManager()
        }
        
        /**
         * Creates a mock HapticFeedbackManager
         */
        fun createMockHapticFeedbackManager(): MockHapticFeedbackManager {
            return MockHapticFeedbackManager()
        }
        
        /**
         * Creates a mock ThemeManager
         */
        fun createMockThemeManager(): MockThemeManager {
            return MockThemeManager()
        }
        
        /**
         * Creates a mock PlatformNotificationService
         */
        fun createMockNotificationService(): MockNotificationService {
            return MockNotificationService()
        }
        
        /**
         * Creates a mock AccessibilityManager
         */
        fun createMockAccessibilityManager(): MockAccessibilityManager {
            return MockAccessibilityManager()
        }
        
        /**
         * Creates a mock PlatformLifecycleManager
         */
        fun createMockLifecycleManager(): MockPlatformLifecycleManager {
            return MockPlatformLifecycleManager()
        }
        
        /**
         * Creates a mock PlatformNavigationManager
         */
        fun createMockNavigationManager(): MockPlatformNavigationManager {
            return MockPlatformNavigationManager()
        }
    }
}

/**
 * Container for all mock services
 */
data class MockServiceSet(
    val networkConnectivity: com.eunio.healthapp.testutil.MockNetworkConnectivity,
    val platformManager: MockPlatformManager,
    val hapticFeedbackManager: MockHapticFeedbackManager,
    val themeManager: MockThemeManager,
    val notificationService: MockNotificationService,
    val accessibilityManager: MockAccessibilityManager,
    val lifecycleManager: MockPlatformLifecycleManager,
    val navigationManager: MockPlatformNavigationManager
) {
    /**
     * Resets all mock services to clean state
     */
    fun resetAll() {
        networkConnectivity.reset()
        platformManager.reset()
        hapticFeedbackManager.reset()
        themeManager.reset()
        notificationService.reset()
        accessibilityManager.reset()
        lifecycleManager.reset()
        navigationManager.reset()
    }
}

/**
 * Mock PlatformManager implementation for testing
 */
class MockPlatformManager : PlatformManager {
    private var performanceOptimized = false
    private val performanceMetrics = MockPerformanceMetrics()
    private val deepLinkHistory = mutableListOf<String>()
    private val sharedContent = mutableListOf<SharedContent>()
    
    override fun optimizePerformance() {
        performanceOptimized = true
    }
    
    override fun getPerformanceMetrics(): PlatformPerformanceMetrics = performanceMetrics
    
    override fun configureSecurityFeatures() {
        // Mock implementation - no-op for testing
    }
    
    override fun handleDeepLink(url: String): Boolean {
        deepLinkHistory.add(url)
        return url.startsWith("eunio://") || url.startsWith("https://eunio.app/")
    }
    
    override fun shareContent(content: String, title: String) {
        sharedContent.add(SharedContent(content, title, Clock.System.now()))
    }
    
    override fun openDocumentPicker() {
        // Mock implementation - no-op for testing
    }
    
    override fun getPlatformName(): String = "Test Platform"
    
    override fun getOSVersion(): String = "Test OS 1.0"
    
    override fun getDeviceModel(): String = "Test Device"
    
    override fun getScreenSize(): String = "1080x1920"
    
    override fun getLocale(): String = "en_US"
    
    override fun getAppVersion(): String = "1.0.0"
    
    override fun getBuildNumber(): String = "100"
    
    override fun getInstallDate(): Instant = Clock.System.now()
    
    override fun getLastUpdateDate(): Instant = Clock.System.now()
    
    // Test helper methods
    fun isPerformanceOptimized(): Boolean = performanceOptimized
    
    fun getDeepLinkHistory(): List<String> = deepLinkHistory.toList()
    
    fun getSharedContent(): List<SharedContent> = sharedContent.toList()
    
    fun getLastSharedContent(): SharedContent? = sharedContent.lastOrNull()
    
    fun reset() {
        performanceOptimized = false
        deepLinkHistory.clear()
        sharedContent.clear()
    }
    
    data class SharedContent(
        val content: String,
        val title: String,
        val timestamp: Instant
    )
}

/**
 * Mock PerformanceMetrics implementation for testing
 */
class MockPerformanceMetrics : PlatformPerformanceMetrics {
    override val deviceModel: String = "Test Device"
    override val systemVersion: String = "Test OS 1.0"
    override val availableMemory: Long = 2048L * 1024 * 1024 // 2GB
    override val totalMemory: Long = 4096L * 1024 * 1024 // 4GB
    override val processorCount: Int = 4
}

/**
 * Mock HapticFeedbackManager implementation for testing
 */
class MockHapticFeedbackManager : HapticFeedbackManager {
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
    
    fun reset() {
        hapticFeedbackAvailable = true
        systemHapticEnabled = true
        hapticHistory.clear()
    }
    
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
 * Mock ThemeManager implementation for testing
 */
class MockThemeManager : ThemeManager {
    private var textSizeScale: Float = 1.0f
    private var highContrastModeEnabled: Boolean = false
    private var themeApplied: Boolean = false
    private val themeHistory = mutableListOf<ThemeEvent>()
    
    override suspend fun updateTextSizeScale(scale: Float): Result<Unit> {
        return if (scale in 0.5f..2.0f) {
            textSizeScale = scale
            recordThemeEvent("updateTextSizeScale", scale.toString())
            Result.Success(Unit)
        } else {
            Result.Error(com.eunio.healthapp.domain.error.AppError.ValidationError("Text size scale must be between 0.5 and 2.0"))
        }
    }
    
    override suspend fun updateHighContrastMode(enabled: Boolean): Result<Unit> {
        highContrastModeEnabled = enabled
        recordThemeEvent("updateHighContrastMode", enabled.toString())
        return Result.Success(Unit)
    }
    
    override fun getCurrentTextSizeScale(): Float = textSizeScale
    
    override fun isHighContrastModeEnabled(): Boolean = highContrastModeEnabled
    
    override suspend fun applyCurrentTheme(): Result<Unit> {
        themeApplied = true
        recordThemeEvent("applyCurrentTheme")
        return Result.Success(Unit)
    }
    
    override suspend fun resetToSystemDefaults(): Result<Unit> {
        textSizeScale = 1.0f
        highContrastModeEnabled = false
        themeApplied = false
        recordThemeEvent("resetToSystemDefaults")
        return Result.Success(Unit)
    }
    
    // Test helper methods
    fun isThemeApplied(): Boolean = themeApplied
    
    fun getThemeHistory(): List<ThemeEvent> = themeHistory.toList()
    
    fun getLastThemeEvent(): ThemeEvent? = themeHistory.lastOrNull()
    
    fun reset() {
        textSizeScale = 1.0f
        highContrastModeEnabled = false
        themeApplied = false
        themeHistory.clear()
    }
    
    private fun recordThemeEvent(method: String, parameter: String? = null) {
        themeHistory.add(ThemeEvent(method, parameter, Clock.System.now()))
    }
    
    data class ThemeEvent(
        val method: String,
        val parameter: String?,
        val timestamp: Instant
    )
}

/**
 * Mock PlatformNotificationService implementation for testing
 */
class MockNotificationService : PlatformNotificationService {
    private val scheduledNotifications = mutableListOf<ScheduledNotification>()
    private var notificationsEnabled = true
    private var permissionStatus = com.eunio.healthapp.domain.model.notification.NotificationPermissionStatus.GRANTED
    
    override suspend fun scheduleRepeatingNotification(
        id: String,
        title: String,
        body: String,
        time: kotlinx.datetime.LocalTime,
        repeatInterval: com.eunio.healthapp.domain.model.notification.RepeatInterval
    ): kotlin.Result<Unit> {
        scheduledNotifications.add(
            ScheduledNotification(id, title, body, Clock.System.now(), Clock.System.now(), true)
        )
        return kotlin.Result.success(Unit)
    }
    
    override suspend fun scheduleOneTimeNotification(
        id: String,
        title: String,
        body: String,
        triggerTimeMillis: Long
    ): kotlin.Result<Unit> {
        scheduledNotifications.add(
            ScheduledNotification(id, title, body, Clock.System.now(), Clock.System.now(), false)
        )
        return kotlin.Result.success(Unit)
    }
    
    override suspend fun cancelNotification(id: String): kotlin.Result<Unit> {
        scheduledNotifications.removeAll { it.id == id }
        return kotlin.Result.success(Unit)
    }
    
    override suspend fun cancelAllNotifications(): kotlin.Result<Unit> {
        scheduledNotifications.clear()
        return kotlin.Result.success(Unit)
    }
    
    override suspend fun requestPermission(): kotlin.Result<Boolean> {
        return kotlin.Result.success(notificationsEnabled)
    }
    
    override suspend fun getPermissionStatus(): com.eunio.healthapp.domain.model.notification.NotificationPermissionStatus {
        return permissionStatus
    }
    
    override suspend fun areNotificationsEnabled(): Boolean = notificationsEnabled
    
    override suspend fun openNotificationSettings(): kotlin.Result<Unit> {
        return kotlin.Result.success(Unit)
    }
    
    override suspend fun showTestNotification(id: String, title: String, body: String): kotlin.Result<Unit> {
        return kotlin.Result.success(Unit)
    }
    
    override suspend fun getScheduledNotificationIds(): List<String> {
        return scheduledNotifications.map { it.id }
    }
    
    override suspend fun initialize(): kotlin.Result<Unit> {
        return kotlin.Result.success(Unit)
    }
    
    // Test helper methods
    fun setNotificationsEnabled(enabled: Boolean) {
        notificationsEnabled = enabled
    }
    
    fun setPermissionStatus(status: com.eunio.healthapp.domain.model.notification.NotificationPermissionStatus) {
        permissionStatus = status
    }
    
    fun getScheduledNotifications(): List<ScheduledNotification> = scheduledNotifications.toList()
    
    fun getNotificationCount(): Int = scheduledNotifications.size
    
    fun hasNotification(id: String): Boolean = scheduledNotifications.any { it.id == id }
    
    fun reset() {
        scheduledNotifications.clear()
        notificationsEnabled = true
        permissionStatus = com.eunio.healthapp.domain.model.notification.NotificationPermissionStatus.GRANTED
    }
    
    data class ScheduledNotification(
        val id: String,
        val title: String,
        val body: String,
        val scheduledTime: Instant,
        val createdTime: Instant,
        val isRepeating: Boolean
    )
}

/**
 * Mock AccessibilityManager implementation for testing
 */
class MockAccessibilityManager : AccessibilityManager {
    private var screenReaderEnabled = false
    private var voiceOverEnabled = false
    private var talkBackEnabled = false
    private var systemTextSizeScale = 1.0f
    private var systemHighContrastEnabled = false
    private var reduceMotionEnabled = false
    private var boldTextEnabled = false
    private var buttonShapesEnabled = false
    
    override fun isScreenReaderEnabled(): Boolean = screenReaderEnabled
    
    override fun isVoiceOverEnabled(): Boolean = voiceOverEnabled
    
    override fun isTalkBackEnabled(): Boolean = talkBackEnabled
    
    override fun getSystemTextSizeScale(): Float = systemTextSizeScale
    
    override fun isSystemHighContrastEnabled(): Boolean = systemHighContrastEnabled
    
    override fun isReduceMotionEnabled(): Boolean = reduceMotionEnabled
    
    override fun isBoldTextEnabled(): Boolean = boldTextEnabled
    
    override fun isButtonShapesEnabled(): Boolean = buttonShapesEnabled
    
    override fun announceForAccessibility(text: String, priority: AnnouncementPriority) {
        // Mock implementation - no-op for testing
    }
    
    override fun postAccessibilityEvent(event: com.eunio.healthapp.platform.accessibility.AccessibilityEvent) {
        // Mock implementation - no-op for testing
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
    
    fun reset() {
        screenReaderEnabled = false
        voiceOverEnabled = false
        talkBackEnabled = false
        systemTextSizeScale = 1.0f
        systemHighContrastEnabled = false
        reduceMotionEnabled = false
        boldTextEnabled = false
        buttonShapesEnabled = false
    }
}

/**
 * Mock PlatformLifecycleManager implementation for testing
 */
class MockPlatformLifecycleManager : PlatformLifecycleManager {
    private val lifecycleEvents = mutableListOf<LifecycleEvent>()
    
    override fun onAppStart() {
        recordLifecycleEvent("onAppStart")
    }
    
    override fun onAppStop() {
        recordLifecycleEvent("onAppStop")
    }
    
    override fun onAppPause() {
        recordLifecycleEvent("onAppPause")
    }
    
    override fun onAppResume() {
        recordLifecycleEvent("onAppResume")
    }
    
    override fun onLowMemory() {
        recordLifecycleEvent("onLowMemory")
    }
    
    override fun cleanup() {
        recordLifecycleEvent("cleanup")
    }
    
    // Test helper methods
    fun getLifecycleEvents(): List<LifecycleEvent> = lifecycleEvents.toList()
    
    fun getLastLifecycleEvent(): LifecycleEvent? = lifecycleEvents.lastOrNull()
    
    fun getEventCount(): Int = lifecycleEvents.size
    
    fun hasEvent(eventType: String): Boolean = lifecycleEvents.any { it.eventType == eventType }
    
    fun reset() {
        lifecycleEvents.clear()
    }
    
    private fun recordLifecycleEvent(eventType: String) {
        lifecycleEvents.add(LifecycleEvent(eventType, Clock.System.now()))
    }
    
    data class LifecycleEvent(
        val eventType: String,
        val timestamp: Instant
    )
}

/**
 * Mock PlatformNavigationManager implementation for testing
 */
class MockPlatformNavigationManager : PlatformNavigationManager {
    private val navigationHistory = mutableListOf<NavigationEvent>()
    private val deepLinkHistory = mutableListOf<String>()
    private val sharedReports = mutableListOf<String>()
    
    override fun navigate(destination: NavigationDestination) {
        recordNavigationEvent("navigate", destination.toString())
    }
    
    override fun navigateBack(): Boolean {
        recordNavigationEvent("navigateBack")
        return true // Always successful in mock
    }
    
    override fun handleDeepLink(url: String): Boolean {
        deepLinkHistory.add(url)
        recordNavigationEvent("handleDeepLink", url)
        return url.startsWith("eunio://") || url.startsWith("https://eunio.app/")
    }
    
    override fun shareHealthReport(reportPath: String) {
        sharedReports.add(reportPath)
        recordNavigationEvent("shareHealthReport", reportPath)
    }
    
    // Test helper methods
    fun getNavigationHistory(): List<NavigationEvent> = navigationHistory.toList()
    
    fun getDeepLinkHistory(): List<String> = deepLinkHistory.toList()
    
    fun getSharedReports(): List<String> = sharedReports.toList()
    
    fun getLastNavigationEvent(): NavigationEvent? = navigationHistory.lastOrNull()
    
    fun reset() {
        navigationHistory.clear()
        deepLinkHistory.clear()
        sharedReports.clear()
    }
    
    private fun recordNavigationEvent(eventType: String, parameter: String? = null) {
        navigationHistory.add(NavigationEvent(eventType, parameter, Clock.System.now()))
    }
    
    data class NavigationEvent(
        val eventType: String,
        val parameter: String?,
        val timestamp: Instant
    )
}



