package com.eunio.healthapp.testutil

import com.eunio.healthapp.domain.model.settings.HapticIntensity
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.presentation.navigation.NavigationDestination
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Tests for Android-specific test utilities to ensure proper mocking and functionality
 */
class AndroidTestUtilitiesTest : BaseAndroidUnitTest() {
    
    @BeforeTest
    fun setup() {
        setupAndroidTest()
    }
    
    @AfterTest
    fun teardown() {
        teardownAndroidTest()
    }
    
    @Test
    fun `Android test environment creation provides all required services`() {
        // Verify all services are available
        assertNotNull(androidTestEnvironment.context)
        assertNotNull(androidTestEnvironment.sharedPreferences)
        assertNotNull(androidTestEnvironment.platformManager)
        assertNotNull(androidTestEnvironment.hapticFeedbackManager)
        assertNotNull(androidTestEnvironment.themeManager)
        assertNotNull(androidTestEnvironment.accessibilityManager)
        assertNotNull(androidTestEnvironment.lifecycleManager)
        assertNotNull(androidTestEnvironment.navigationManager)
    }
    
    @Test
    fun `mock Android Context provides expected functionality`() {
        // Test SharedPreferences access
        val sharedPrefs = mockContext.getSharedPreferences("test", 0)
        assertNotNull(sharedPrefs)
        
        // Test package name
        assertEquals("com.eunio.healthapp.test", mockContext.packageName)
        
        // Test cache and files directories
        assertNotNull(mockContext.cacheDir)
        assertNotNull(mockContext.filesDir)
        assertTrue(mockContext.cacheDir.path.contains("test-cache"))
        assertTrue(mockContext.filesDir.path.contains("test-files"))
    }
    
    @Test
    fun `mock SharedPreferences stores and retrieves data correctly`() {
        val editor = mockSharedPreferences.edit()
        
        // Test string storage
        editor.putString("test_string", "test_value")
        editor.putInt("test_int", 42)
        editor.putBoolean("test_boolean", true)
        editor.putFloat("test_float", 3.14f)
        editor.putLong("test_long", 123456789L)
        editor.apply()
        
        // Verify retrieval
        assertEquals("test_value", mockSharedPreferences.getString("test_string", null))
        assertEquals(42, mockSharedPreferences.getInt("test_int", 0))
        assertTrue(mockSharedPreferences.getBoolean("test_boolean", false))
        assertEquals(3.14f, mockSharedPreferences.getFloat("test_float", 0f), 0.01f)
        assertEquals(123456789L, mockSharedPreferences.getLong("test_long", 0L))
        
        // Test contains
        assertTrue(mockSharedPreferences.contains("test_string"))
        assertFalse(mockSharedPreferences.contains("non_existent_key"))
    }
    
    @Test
    fun `mock HapticFeedbackManager records haptic events correctly`() = runTest {
        val hapticManager = androidTestEnvironment.hapticFeedbackManager as MockAndroidHapticFeedbackManager
        
        // Test haptic feedback methods
        hapticManager.performHapticFeedback(HapticIntensity.LIGHT)
        hapticManager.performMediumHaptic()
        hapticManager.performStrongHaptic()
        hapticManager.performSuccessHaptic()
        
        // Verify events were recorded
        val history = hapticManager.getHapticHistory()
        assertEquals(4, history.size)
        assertEquals("performHapticFeedback", history[0].method)
        assertEquals("LIGHT", history[0].parameter)
        assertEquals("performMediumHaptic", history[1].method)
        assertEquals("performStrongHaptic", history[2].method)
        assertEquals("performSuccessHaptic", history[3].method)
        
        // Test availability checks
        assertTrue(hapticManager.isHapticFeedbackAvailable())
        assertTrue(hapticManager.isSystemHapticEnabled())
        
        // Test configuration changes
        hapticManager.setHapticFeedbackAvailable(false)
        assertFalse(hapticManager.isHapticFeedbackAvailable())
    }
    
    @Test
    fun `mock ThemeManager handles theme operations correctly`() = runTest {
        val themeManager = androidTestEnvironment.themeManager as MockAndroidThemeManager
        
        // Test text size scale updates
        val scaleResult = themeManager.updateTextSizeScale(1.5f)
        assertTrue(scaleResult.isSuccess)
        assertEquals(1.5f, themeManager.getCurrentTextSizeScale())
        
        // Test invalid scale
        val invalidScaleResult = themeManager.updateTextSizeScale(3.0f)
        assertTrue(invalidScaleResult.isError)
        
        // Test high contrast mode
        val contrastResult = themeManager.updateHighContrastMode(true)
        assertTrue(contrastResult.isSuccess)
        assertTrue(themeManager.isHighContrastModeEnabled())
        
        // Test theme application
        val applyResult = themeManager.applyCurrentTheme()
        assertTrue(applyResult.isSuccess)
        assertTrue(themeManager.isThemeApplied())
        
        // Test reset
        val resetResult = themeManager.resetToSystemDefaults()
        assertTrue(resetResult.isSuccess)
        assertEquals(1.0f, themeManager.getCurrentTextSizeScale())
        assertFalse(themeManager.isHighContrastModeEnabled())
    }
    
    @Test
    fun `mock AccessibilityManager provides accessibility information`() {
        val accessibilityManager = androidTestEnvironment.accessibilityManager as MockAndroidAccessibilityManager
        
        // Test initial state
        assertFalse(accessibilityManager.isScreenReaderEnabled())
        assertFalse(accessibilityManager.isVoiceOverEnabled())
        assertFalse(accessibilityManager.isTalkBackEnabled())
        assertEquals(1.0f, accessibilityManager.getSystemTextSizeScale())
        assertFalse(accessibilityManager.isSystemHighContrastEnabled())
        assertFalse(accessibilityManager.isReduceMotionEnabled())
        assertFalse(accessibilityManager.isBoldTextEnabled())
        assertFalse(accessibilityManager.isButtonShapesEnabled())
        assertTrue(accessibilityManager.isAccessibilitySupported())
        
        // Test configuration changes
        accessibilityManager.setScreenReaderEnabled(true)
        assertTrue(accessibilityManager.isScreenReaderEnabled())
        
        accessibilityManager.setVoiceOverEnabled(true)
        assertTrue(accessibilityManager.isVoiceOverEnabled())
        
        accessibilityManager.setTalkBackEnabled(true)
        assertTrue(accessibilityManager.isTalkBackEnabled())
        
        accessibilityManager.setSystemTextSizeScale(1.5f)
        assertEquals(1.5f, accessibilityManager.getSystemTextSizeScale())
        
        accessibilityManager.setSystemHighContrastEnabled(true)
        assertTrue(accessibilityManager.isSystemHighContrastEnabled())
        
        accessibilityManager.setReduceMotionEnabled(true)
        assertTrue(accessibilityManager.isReduceMotionEnabled())
        
        accessibilityManager.setBoldTextEnabled(true)
        assertTrue(accessibilityManager.isBoldTextEnabled())
        
        accessibilityManager.setButtonShapesEnabled(true)
        assertTrue(accessibilityManager.isButtonShapesEnabled())
        
        // Test announcement
        accessibilityManager.announceForAccessibility("Test announcement")
        val announcements = accessibilityManager.getAnnouncementHistory()
        assertEquals(1, announcements.size)
        assertTrue(announcements[0].contains("Test announcement"))
        
        // Test accessibility events
        val event = com.eunio.healthapp.platform.accessibility.AccessibilityEvent.ContentChanged("Content updated")
        accessibilityManager.postAccessibilityEvent(event)
        val events = accessibilityManager.getEventHistory()
        assertEquals(1, events.size)
        assertTrue(events[0] is com.eunio.healthapp.platform.accessibility.AccessibilityEvent.ContentChanged)
    }
    
    @Test
    fun `mock LifecycleManager tracks lifecycle events correctly`() = runTest {
        val lifecycleManager = androidTestEnvironment.lifecycleManager as MockAndroidLifecycleManager
        
        // Test initial state
        assertFalse(lifecycleManager.isStarted())
        assertFalse(lifecycleManager.isPaused())
        
        // Test lifecycle transitions
        lifecycleManager.onAppStart()
        assertTrue(lifecycleManager.isStarted())
        assertFalse(lifecycleManager.isPaused())
        
        lifecycleManager.onAppPause()
        assertTrue(lifecycleManager.isPaused())
        
        lifecycleManager.onAppResume()
        assertFalse(lifecycleManager.isPaused())
        
        lifecycleManager.onAppStop()
        assertFalse(lifecycleManager.isStarted())
        
        lifecycleManager.onLowMemory()
        lifecycleManager.cleanup()
        
        // Verify events were recorded
        val events = lifecycleManager.getLifecycleEvents()
        assertEquals(6, events.size)
        
        val eventTypes = events.map { it.event }
        assertTrue(eventTypes.contains("onAppStart"))
        assertTrue(eventTypes.contains("onAppPause"))
        assertTrue(eventTypes.contains("onAppResume"))
        assertTrue(eventTypes.contains("onAppStop"))
        assertTrue(eventTypes.contains("onLowMemory"))
        assertTrue(eventTypes.contains("cleanup"))
    }
    
    @Test
    fun `mock NavigationManager handles navigation operations correctly`() = runTest {
        val navigationManager = androidTestEnvironment.navigationManager as MockAndroidNavigationManager
        
        // Test initial state
        assertNull(navigationManager.getCurrentDestination())
        
        // Test navigation
        val testDestination = NavigationDestination.Calendar
        navigationManager.navigate(testDestination)
        assertEquals(testDestination, navigationManager.getCurrentDestination())
        
        // Test navigation back
        val backResult = navigationManager.navigateBack()
        assertTrue(backResult)
        
        // Test deep link handling
        val validDeepLink = navigationManager.handleDeepLink("eunio://calendar")
        assertTrue(validDeepLink)
        
        val invalidDeepLink = navigationManager.handleDeepLink("invalid://link")
        assertFalse(invalidDeepLink)
        
        // Test health report sharing
        navigationManager.shareHealthReport("/path/to/report.pdf")
        
        // Verify events were recorded
        val history = navigationManager.getNavigationHistory()
        assertEquals(5, history.size) // navigate, navigateBack, handleDeepLink (2 calls), shareHealthReport
        
        val actions = history.map { it.action }
        assertTrue(actions.contains("navigate"))
        assertTrue(actions.contains("navigateBack"))
        assertTrue(actions.contains("handleDeepLink"))
        assertTrue(actions.contains("shareHealthReport"))
    }
    
    @Test
    fun `mock PlatformManager provides platform information correctly`() {
        val platformManager = androidTestEnvironment.platformManager as MockAndroidPlatformManager
        
        // Test platform information
        assertEquals("Android", platformManager.getPlatformName())
        assertEquals("14.0", platformManager.getOSVersion())
        assertEquals("Test Device", platformManager.getDeviceModel())
        assertEquals("1080x2400", platformManager.getScreenSize())
        assertEquals("en_US", platformManager.getLocale())
        assertEquals("1.0.0", platformManager.getAppVersion())
        assertEquals("100", platformManager.getBuildNumber())
        
        // Test performance optimization
        assertFalse(platformManager.isPerformanceOptimized())
        platformManager.optimizePerformance()
        assertTrue(platformManager.isPerformanceOptimized())
        
        // Test deep link handling
        assertTrue(platformManager.handleDeepLink("eunio://test"))
        assertTrue(platformManager.handleDeepLink("https://eunio.app/test"))
        assertFalse(platformManager.handleDeepLink("invalid://link"))
        
        // Test performance metrics
        val metrics = platformManager.getPerformanceMetrics()
        assertEquals("Test Android Device", metrics.deviceModel)
        assertEquals("Android 14", metrics.systemVersion)
        assertEquals(8, metrics.processorCount)
        assertTrue(metrics.availableMemory > 0)
        assertTrue(metrics.totalMemory > metrics.availableMemory)
        
        // Test dates are reasonable
        assertNotNull(platformManager.getInstallDate())
        assertNotNull(platformManager.getLastUpdateDate())
    }
    
    @Test
    fun `SharedPreferences test data configuration works correctly`() {
        val testSharedPrefs = AndroidTestContext.createMockSharedPreferences()
        AndroidTestUtilities.configureSharedPreferencesWithTestData(testSharedPrefs)
        
        // Verify test data was configured
        assertEquals("test-user-123", testSharedPrefs.getString("user_id", null))
        assertTrue(testSharedPrefs.getBoolean("onboarding_completed", false))
        assertEquals("SYSTEM", testSharedPrefs.getString("theme_preference", null))
        assertEquals(1.0f, testSharedPrefs.getFloat("text_size_scale", 0f))
        assertTrue(testSharedPrefs.getBoolean("haptic_feedback_enabled", false))
        assertEquals("MEDIUM", testSharedPrefs.getString("haptic_intensity", null))
        assertFalse(testSharedPrefs.getBoolean("high_contrast_mode", true))
        assertTrue(testSharedPrefs.getBoolean("notifications_enabled", false))
        assertEquals("CELSIUS", testSharedPrefs.getString("temperature_unit", null))
        assertEquals("KILOGRAMS", testSharedPrefs.getString("weight_unit", null))
    }
    
    @Test
    fun `reset functionality clears all mock states correctly`() = runTest {
        // Modify all mocks
        val hapticManager = androidTestEnvironment.hapticFeedbackManager as MockAndroidHapticFeedbackManager
        hapticManager.performHapticFeedback(HapticIntensity.STRONG)
        
        val themeManager = androidTestEnvironment.themeManager as MockAndroidThemeManager
        themeManager.updateTextSizeScale(1.5f)
        themeManager.updateHighContrastMode(true)
        
        val accessibilityManager = androidTestEnvironment.accessibilityManager as MockAndroidAccessibilityManager
        accessibilityManager.setScreenReaderEnabled(true)
        
        val lifecycleManager = androidTestEnvironment.lifecycleManager as MockAndroidLifecycleManager
        lifecycleManager.onAppStart()
        
        val navigationManager = androidTestEnvironment.navigationManager as MockAndroidNavigationManager
        navigationManager.navigate(NavigationDestination.Calendar)
        
        val platformManager = androidTestEnvironment.platformManager as MockAndroidPlatformManager
        platformManager.optimizePerformance()
        
        // Verify changes were made
        assertTrue(hapticManager.getHapticEventCount() > 0)
        assertEquals(1.5f, themeManager.getCurrentTextSizeScale())
        assertTrue(accessibilityManager.isScreenReaderEnabled())
        assertTrue(lifecycleManager.isStarted())
        assertEquals(NavigationDestination.Calendar, navigationManager.getCurrentDestination())
        assertTrue(platformManager.isPerformanceOptimized())
        
        // Reset all mocks
        AndroidTestUtilities.resetAllAndroidMocks(androidTestEnvironment)
        
        // Verify reset worked
        assertEquals(0, hapticManager.getHapticEventCount())
        assertEquals(1.0f, themeManager.getCurrentTextSizeScale())
        assertFalse(themeManager.isHighContrastModeEnabled())
        assertFalse(accessibilityManager.isScreenReaderEnabled())
        assertFalse(lifecycleManager.isStarted())
        assertNull(navigationManager.getCurrentDestination())
        assertFalse(platformManager.isPerformanceOptimized())
    }
}