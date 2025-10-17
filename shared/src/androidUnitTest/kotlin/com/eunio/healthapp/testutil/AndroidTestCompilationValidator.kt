package com.eunio.healthapp.testutil

import android.content.Context
import android.content.SharedPreferences
import com.eunio.healthapp.platform.haptic.HapticFeedbackManager
import com.eunio.healthapp.platform.theme.ThemeManager
import com.eunio.healthapp.platform.accessibility.AccessibilityManager
import com.eunio.healthapp.platform.PlatformLifecycleManager
import com.eunio.healthapp.platform.PlatformNavigationManager
import com.eunio.healthapp.platform.PlatformManager
import com.eunio.healthapp.domain.model.settings.HapticIntensity
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Validation test to ensure all Android-specific test compilation issues are resolved.
 * This test verifies that all Android platform services can be properly mocked and used in tests.
 */
class AndroidTestCompilationValidator {
    
    @Test
    fun `all Android platform services can be instantiated and used`() = runTest {
        // Test that all Android mock services can be created without compilation errors
        val context = AndroidTestUtilities.createMockAndroidContext()
        val sharedPreferences = AndroidTestContext.createMockSharedPreferences()
        val platformManager = AndroidTestContext.createMockPlatformManager()
        val hapticFeedbackManager = AndroidTestContext.createMockHapticFeedbackManager()
        val themeManager = AndroidTestContext.createMockThemeManager()
        val accessibilityManager = AndroidTestContext.createMockAccessibilityManager()
        val lifecycleManager = AndroidTestContext.createMockLifecycleManager()
        val navigationManager = AndroidTestContext.createMockNavigationManager()
        
        // Verify all services are properly typed and functional
        assertNotNull(context)
        assertNotNull(sharedPreferences)
        assertNotNull(platformManager)
        assertNotNull(hapticFeedbackManager)
        assertNotNull(themeManager)
        assertNotNull(accessibilityManager)
        assertNotNull(lifecycleManager)
        assertNotNull(navigationManager)
        
        // Test that services implement expected interfaces
        assertTrue(context is Context)
        assertTrue(sharedPreferences is SharedPreferences)
        assertTrue(platformManager is PlatformManager)
        assertTrue(hapticFeedbackManager is HapticFeedbackManager)
        assertTrue(themeManager is ThemeManager)
        assertTrue(accessibilityManager is AccessibilityManager)
        assertTrue(lifecycleManager is PlatformLifecycleManager)
        assertTrue(navigationManager is PlatformNavigationManager)
    }
    
    @Test
    fun `Android Context mocking works correctly`() {
        val mockContext = AndroidTestUtilities.createMockAndroidContext()
        
        // Test SharedPreferences access
        val sharedPrefs = mockContext.getSharedPreferences("test", Context.MODE_PRIVATE)
        assertNotNull(sharedPrefs)
        
        // Test basic Context methods
        assertEquals("com.eunio.healthapp.test", mockContext.packageName)
        assertNotNull(mockContext.cacheDir)
        assertNotNull(mockContext.filesDir)
        assertNotNull(mockContext.applicationContext)
    }
    
    @Test
    fun `SharedPreferences mocking supports all required operations`() {
        val sharedPreferences = AndroidTestContext.createMockSharedPreferences()
        val editor = sharedPreferences.edit()
        
        // Test all data types
        editor.putString("string_key", "test_value")
        editor.putInt("int_key", 42)
        editor.putBoolean("boolean_key", true)
        editor.putFloat("float_key", 3.14f)
        editor.putLong("long_key", 123456789L)
        editor.putStringSet("string_set_key", setOf("value1", "value2"))
        editor.apply()
        
        // Verify retrieval
        assertEquals("test_value", sharedPreferences.getString("string_key", null))
        assertEquals(42, sharedPreferences.getInt("int_key", 0))
        assertTrue(sharedPreferences.getBoolean("boolean_key", false))
        assertEquals(3.14f, sharedPreferences.getFloat("float_key", 0f), 0.01f)
        assertEquals(123456789L, sharedPreferences.getLong("long_key", 0L))
        assertEquals(setOf("value1", "value2"), sharedPreferences.getStringSet("string_set_key", null))
        
        // Test contains and removal
        assertTrue(sharedPreferences.contains("string_key"))
        val removeEditor = sharedPreferences.edit()
        removeEditor.remove("string_key")
        removeEditor.apply()
        assertFalse(sharedPreferences.contains("string_key"))
        
        // Verify other keys still exist before clear
        assertTrue(sharedPreferences.contains("int_key"))
        assertTrue(sharedPreferences.contains("boolean_key"))
        
        // Test clear
        val clearEditor = sharedPreferences.edit()
        clearEditor.clear()
        clearEditor.apply()
        assertFalse(sharedPreferences.contains("int_key"))
        assertFalse(sharedPreferences.contains("boolean_key"))
        assertFalse(sharedPreferences.contains("float_key"))
        assertFalse(sharedPreferences.contains("long_key"))
        assertFalse(sharedPreferences.contains("string_set_key"))
    }
    
    @Test
    fun `HapticFeedbackManager mock supports all haptic operations`() = runTest {
        val hapticManager = AndroidTestContext.createMockHapticFeedbackManager()
        
        // Test all haptic feedback methods
        hapticManager.performHapticFeedback(HapticIntensity.LIGHT)
        hapticManager.performHapticFeedback(HapticIntensity.MEDIUM)
        hapticManager.performHapticFeedback(HapticIntensity.STRONG)
        hapticManager.performLightHaptic()
        hapticManager.performMediumHaptic()
        hapticManager.performStrongHaptic()
        hapticManager.performSuccessHaptic()
        hapticManager.performErrorHaptic()
        hapticManager.performWarningHaptic()
        
        // Test availability checks
        assertTrue(hapticManager.isHapticFeedbackAvailable())
        assertTrue(hapticManager.isSystemHapticEnabled())
        
        // Verify mock-specific functionality
        val mockHapticManager = hapticManager as MockAndroidHapticFeedbackManager
        assertEquals(9, mockHapticManager.getHapticEventCount())
        assertNotNull(mockHapticManager.getLastHapticEvent())
    }
    
    @Test
    fun `ThemeManager mock supports all theme operations`() = runTest {
        val themeManager = AndroidTestContext.createMockThemeManager()
        
        // Test text size scale operations
        val scaleResult = themeManager.updateTextSizeScale(1.5f)
        assertTrue(scaleResult.isSuccess)
        assertEquals(1.5f, themeManager.getCurrentTextSizeScale())
        
        // Test high contrast mode
        val contrastResult = themeManager.updateHighContrastMode(true)
        assertTrue(contrastResult.isSuccess)
        assertTrue(themeManager.isHighContrastModeEnabled())
        
        // Test theme application
        val applyResult = themeManager.applyCurrentTheme()
        assertTrue(applyResult.isSuccess)
        
        // Test reset
        val resetResult = themeManager.resetToSystemDefaults()
        assertTrue(resetResult.isSuccess)
        assertEquals(1.0f, themeManager.getCurrentTextSizeScale())
        assertFalse(themeManager.isHighContrastModeEnabled())
        
        // Test validation
        val invalidScaleResult = themeManager.updateTextSizeScale(5.0f)
        assertTrue(invalidScaleResult.isError)
    }
    
    @Test
    fun `AccessibilityManager mock provides accessibility functionality`() {
        val accessibilityManager = AndroidTestContext.createMockAccessibilityManager()
        
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
        
        // Test configuration methods
        val mockAccessibilityManager = accessibilityManager as MockAndroidAccessibilityManager
        mockAccessibilityManager.setScreenReaderEnabled(true)
        mockAccessibilityManager.setVoiceOverEnabled(true)
        mockAccessibilityManager.setTalkBackEnabled(true)
        mockAccessibilityManager.setSystemTextSizeScale(1.5f)
        mockAccessibilityManager.setSystemHighContrastEnabled(true)
        mockAccessibilityManager.setReduceMotionEnabled(true)
        mockAccessibilityManager.setBoldTextEnabled(true)
        mockAccessibilityManager.setButtonShapesEnabled(true)
        
        assertTrue(accessibilityManager.isScreenReaderEnabled())
        assertTrue(accessibilityManager.isVoiceOverEnabled())
        assertTrue(accessibilityManager.isTalkBackEnabled())
        assertEquals(1.5f, accessibilityManager.getSystemTextSizeScale())
        assertTrue(accessibilityManager.isSystemHighContrastEnabled())
        assertTrue(accessibilityManager.isReduceMotionEnabled())
        assertTrue(accessibilityManager.isBoldTextEnabled())
        assertTrue(accessibilityManager.isButtonShapesEnabled())
        
        // Test announcement operations (should not throw)
        accessibilityManager.announceForAccessibility("Test message")
        
        // Test accessibility events
        val event = com.eunio.healthapp.platform.accessibility.AccessibilityEvent.ContentChanged("Test content")
        accessibilityManager.postAccessibilityEvent(event)
        
        // Verify history tracking
        val announcements = mockAccessibilityManager.getAnnouncementHistory()
        assertEquals(1, announcements.size)
        
        val events = mockAccessibilityManager.getEventHistory()
        assertEquals(1, events.size)
    }
    
    @Test
    fun `LifecycleManager mock handles lifecycle events correctly`() = runTest {
        val lifecycleManager = AndroidTestContext.createMockLifecycleManager()
        
        // Test lifecycle transitions
        lifecycleManager.onAppStart()
        lifecycleManager.onAppPause()
        lifecycleManager.onAppResume()
        lifecycleManager.onAppStop()
        lifecycleManager.onLowMemory()
        lifecycleManager.cleanup()
        
        // Verify mock-specific functionality
        val mockLifecycleManager = lifecycleManager as MockAndroidLifecycleManager
        val events = mockLifecycleManager.getLifecycleEvents()
        assertEquals(6, events.size)
        assertNotNull(mockLifecycleManager.getLastLifecycleEvent())
        
        // Test state tracking
        assertFalse(mockLifecycleManager.isStarted()) // Should be false after onAppStop
        assertFalse(mockLifecycleManager.isPaused()) // Should be false after onAppStop
    }
    
    @Test
    fun `NavigationManager mock handles navigation operations correctly`() = runTest {
        val navigationManager = AndroidTestContext.createMockNavigationManager()
        
        // Test navigation operations
        val mockNavigationManager = navigationManager as MockAndroidNavigationManager
        assertNull(mockNavigationManager.getCurrentDestination())
        
        // Test navigation methods
        navigationManager.navigate(com.eunio.healthapp.presentation.navigation.NavigationDestination.Calendar)
        assertEquals(com.eunio.healthapp.presentation.navigation.NavigationDestination.Calendar, mockNavigationManager.getCurrentDestination())
        
        val backResult = navigationManager.navigateBack()
        assertTrue(backResult)
        
        // Test deep link handling
        assertTrue(navigationManager.handleDeepLink("eunio://test"))
        assertTrue(navigationManager.handleDeepLink("https://eunio.app/test"))
        assertFalse(navigationManager.handleDeepLink("invalid://link"))
        
        // Test health report sharing
        navigationManager.shareHealthReport("/path/to/report.pdf")
        
        // Verify mock-specific functionality
        val history = mockNavigationManager.getNavigationHistory()
        assertTrue(history.size >= 4) // At least navigate, navigateBack, handleDeepLink, shareHealthReport
        assertNotNull(mockNavigationManager.getLastNavigationEvent())
        
        val actions = history.map { it.action }
        assertTrue(actions.contains("navigate"))
        assertTrue(actions.contains("navigateBack"))
        assertTrue(actions.contains("handleDeepLink"))
        assertTrue(actions.contains("shareHealthReport"))
    }
    
    @Test
    fun `PlatformManager mock provides platform information correctly`() {
        val platformManager = AndroidTestContext.createMockPlatformManager()
        
        // Test platform information methods
        assertEquals("Android", platformManager.getPlatformName())
        assertEquals("14.0", platformManager.getOSVersion())
        assertEquals("Test Device", platformManager.getDeviceModel())
        assertEquals("1080x2400", platformManager.getScreenSize())
        assertEquals("en_US", platformManager.getLocale())
        assertEquals("1.0.0", platformManager.getAppVersion())
        assertEquals("100", platformManager.getBuildNumber())
        
        // Test performance operations
        val mockPlatformManager = platformManager as MockAndroidPlatformManager
        assertFalse(mockPlatformManager.isPerformanceOptimized())
        platformManager.optimizePerformance()
        assertTrue(mockPlatformManager.isPerformanceOptimized())
        
        // Test other platform operations
        assertTrue(platformManager.handleDeepLink("eunio://test"))
        assertFalse(platformManager.handleDeepLink("invalid://link"))
        
        // Test performance metrics
        val metrics = platformManager.getPerformanceMetrics()
        assertNotNull(metrics)
        assertEquals("Test Android Device", metrics.deviceModel)
        assertEquals("Android 14", metrics.systemVersion)
        assertTrue(metrics.processorCount > 0)
        assertTrue(metrics.totalMemory > 0)
        assertTrue(metrics.availableMemory > 0)
        
        // Test date methods
        assertNotNull(platformManager.getInstallDate())
        assertNotNull(platformManager.getLastUpdateDate())
        
        // Test other methods (should not throw)
        platformManager.configureSecurityFeatures()
        platformManager.shareContent("test content", "test title")
        platformManager.openDocumentPicker()
    }
    
    @Test
    fun `Android test environment integration works correctly`() {
        val environment = AndroidTestUtilities.createAndroidTestEnvironment()
        
        // Verify all components are available
        assertNotNull(environment.context)
        assertNotNull(environment.sharedPreferences)
        assertNotNull(environment.platformManager)
        assertNotNull(environment.hapticFeedbackManager)
        assertNotNull(environment.themeManager)
        assertNotNull(environment.accessibilityManager)
        assertNotNull(environment.lifecycleManager)
        assertNotNull(environment.navigationManager)
        
        // Test that components work together
        val sharedPrefs = environment.context.getSharedPreferences("test", Context.MODE_PRIVATE)
        assertNotNull(sharedPrefs)
        
        // Test reset functionality
        AndroidTestUtilities.resetAllAndroidMocks(environment)
        
        // Verify reset worked (check a few key indicators)
        val mockHaptic = environment.hapticFeedbackManager as MockAndroidHapticFeedbackManager
        assertEquals(0, mockHaptic.getHapticEventCount())
        
        val mockTheme = environment.themeManager as MockAndroidThemeManager
        assertEquals(1.0f, mockTheme.getCurrentTextSizeScale())
        
        val mockAccessibility = environment.accessibilityManager as MockAndroidAccessibilityManager
        assertFalse(mockAccessibility.isScreenReaderEnabled())
    }
    
    @Test
    fun `BaseAndroidUnitTest provides proper test infrastructure`() {
        val testInstance = object : BaseAndroidUnitTest() {
            fun testSetup() {
                setupAndroidTest()
                
                // Verify setup worked
                assertNotNull(androidTestEnvironment)
                assertNotNull(mockContext)
                assertNotNull(mockSharedPreferences)
                
                // Test that configured data is available
                assertEquals("test-user-123", mockSharedPreferences.getString("user_id", null))
                assertTrue(mockSharedPreferences.getBoolean("onboarding_completed", false))
                
                teardownAndroidTest()
            }
        }
        
        testInstance.testSetup()
    }
}