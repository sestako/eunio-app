package com.eunio.healthapp.testutil

import com.eunio.healthapp.domain.util.NetworkConnectivity
import com.eunio.healthapp.platform.haptic.HapticFeedbackManager
import com.eunio.healthapp.platform.theme.ThemeManager
import com.eunio.healthapp.platform.PlatformManager
import com.eunio.healthapp.domain.model.settings.HapticIntensity
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.BeforeTest
import kotlin.test.AfterTest
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * iOS-specific test compatibility validator to ensure all iOS test infrastructure
 * works correctly with current framework setup.
 */
class IOSTestCompatibilityValidator : KoinTest {
    
    private val networkConnectivity: NetworkConnectivity by inject()
    private val hapticFeedbackManager: HapticFeedbackManager by inject()
    private val themeManager: ThemeManager by inject()
    private val platformManager: PlatformManager by inject()
    
    @BeforeTest
    fun setup() {
        startKoin {
            modules(testModule)
        }
    }
    
    @AfterTest
    fun teardown() {
        stopKoin()
    }
    
    @Test
    fun `iOS test framework setup works correctly`() = runTest {
        // Verify that iOS test environment can be set up
        val iosEnvironment = IOSTestSupport.setupIOSTestEnvironment()
        assertNotNull(iosEnvironment)
        
        // Test that we can get iOS-specific components
        val userDefaults = iosEnvironment.getUserDefaults()
        val platformManager = iosEnvironment.getPlatformManager()
        val hapticManager = iosEnvironment.getHapticManager()
        val themeManager = iosEnvironment.getThemeManager()
        
        assertNotNull(userDefaults)
        assertNotNull(platformManager)
        assertNotNull(hapticManager)
        assertNotNull(themeManager)
        
        // Test that environment can be reset
        iosEnvironment.reset()
    }
    
    @Test
    fun `NSUserDefaults mocking works correctly`() = runTest {
        val mockUserDefaults = IOSTestSupport.createMockUserDefaults()
        
        // Test string operations
        mockUserDefaults.setObject("test_value", "test_key")
        assertEquals("test_value", mockUserDefaults.stringForKey("test_key"))
        
        // Test integer operations
        mockUserDefaults.setObject(42, "int_key")
        assertEquals(42, mockUserDefaults.integerForKey("int_key"))
        
        // Test boolean operations
        mockUserDefaults.setObject(true, "bool_key")
        assertTrue(mockUserDefaults.boolForKey("bool_key"))
        
        // Test float operations
        mockUserDefaults.setObject(3.14f, "float_key")
        assertEquals(3.14f, mockUserDefaults.floatForKey("float_key"))
        
        // Test double operations
        mockUserDefaults.setObject(2.718, "double_key")
        assertEquals(2.718, mockUserDefaults.doubleForKey("double_key"))
        
        // Test array operations
        val testArray = listOf("item1", "item2", "item3")
        mockUserDefaults.setObject(testArray, "array_key")
        assertEquals(testArray, mockUserDefaults.arrayForKey("array_key"))
        
        // Test dictionary operations
        val testDict = mapOf("key1" to "value1", "key2" to "value2")
        mockUserDefaults.setObject(testDict, "dict_key")
        assertEquals(testDict, mockUserDefaults.dictionaryForKey("dict_key"))
        
        // Test removal
        mockUserDefaults.removeObjectForKey("test_key")
        assertEquals(null, mockUserDefaults.stringForKey("test_key"))
        
        // Test synchronization
        assertTrue(mockUserDefaults.synchronize())
        
        // Test helper methods
        assertTrue(mockUserDefaults.hasKey("int_key"))
        assertFalse(mockUserDefaults.hasKey("removed_key"))
        assertTrue(mockUserDefaults.getDataSize() > 0)
        
        // Test reset
        mockUserDefaults.reset()
        assertEquals(0, mockUserDefaults.getDataSize())
    }
    
    @Test
    fun `iOS platform manager integration works correctly`() = runTest {
        val mockPlatformManager = IOSTestSupport.createMockPlatformManager()
        
        // Test platform information
        assertEquals("iOS", mockPlatformManager.getPlatformName())
        assertEquals("17.0", mockPlatformManager.getOSVersion())
        assertEquals("iPhone 15 Pro", mockPlatformManager.getDeviceModel())
        assertEquals("393x852", mockPlatformManager.getScreenSize())
        assertEquals("en_US", mockPlatformManager.getLocale())
        
        // Test performance optimization
        assertFalse(mockPlatformManager.isPerformanceOptimized())
        mockPlatformManager.optimizePerformance()
        assertTrue(mockPlatformManager.isPerformanceOptimized())
        
        // Test performance metrics
        val metrics = mockPlatformManager.getPerformanceMetrics()
        assertNotNull(metrics)
        assertEquals("iPhone 15 Pro", metrics.deviceModel)
        assertEquals("iOS 17.0", metrics.systemVersion)
        assertTrue(metrics.totalMemory > 0)
        assertTrue(metrics.processorCount > 0)
        
        // Test deep link handling
        assertTrue(mockPlatformManager.handleDeepLink("eunio://test"))
        assertTrue(mockPlatformManager.handleDeepLink("https://eunio.app/test"))
        assertFalse(mockPlatformManager.handleDeepLink("invalid://test"))
        
        // Test content sharing (should not throw)
        mockPlatformManager.shareContent("Test content", "Test title")
        
        // Test security configuration (should not throw)
        mockPlatformManager.configureSecurityFeatures()
        
        // Test document picker (should not throw)
        mockPlatformManager.openDocumentPicker()
        
        // Test reset
        mockPlatformManager.reset()
        assertFalse(mockPlatformManager.isPerformanceOptimized())
    }
    
    @Test
    fun `iOS haptic feedback manager integration works correctly`() = runTest {
        val mockHapticManager = IOSTestSupport.createMockHapticFeedbackManager()
        
        // Test availability
        assertTrue(mockHapticManager.isHapticFeedbackAvailable())
        assertTrue(mockHapticManager.isSystemHapticEnabled())
        
        // Test haptic feedback methods
        mockHapticManager.performHapticFeedback(HapticIntensity.LIGHT)
        mockHapticManager.performLightHaptic()
        mockHapticManager.performMediumHaptic()
        mockHapticManager.performStrongHaptic()
        mockHapticManager.performSuccessHaptic()
        mockHapticManager.performErrorHaptic()
        mockHapticManager.performWarningHaptic()
        
        // Verify haptic history
        val history = mockHapticManager.getHapticHistory()
        assertEquals(7, history.size)
        assertEquals("performHapticFeedback", history[0].method)
        assertEquals("LIGHT", history[0].parameter)
        assertEquals("performLightHaptic", history[1].method)
        assertEquals("performMediumHaptic", history[2].method)
        assertEquals("performStrongHaptic", history[3].method)
        assertEquals("performSuccessHaptic", history[4].method)
        assertEquals("performErrorHaptic", history[5].method)
        assertEquals("performWarningHaptic", history[6].method)
        
        // Test last haptic event
        val lastEvent = mockHapticManager.getLastHapticEvent()
        assertNotNull(lastEvent)
        assertEquals("performWarningHaptic", lastEvent.method)
        
        // Test haptic event count
        assertEquals(7, mockHapticManager.getHapticEventCount())
        
        // Test availability control
        mockHapticManager.setHapticFeedbackAvailable(false)
        assertFalse(mockHapticManager.isHapticFeedbackAvailable())
        
        mockHapticManager.setSystemHapticEnabled(false)
        assertFalse(mockHapticManager.isSystemHapticEnabled())
        
        // Test history clearing
        mockHapticManager.clearHapticHistory()
        assertEquals(0, mockHapticManager.getHapticEventCount())
    }
    
    @Test
    fun `iOS theme manager integration works correctly`() = runTest {
        val mockThemeManager = IOSTestSupport.createMockThemeManager()
        
        // Test default values
        assertEquals(1.0f, mockThemeManager.getCurrentTextSizeScale())
        assertFalse(mockThemeManager.isHighContrastModeEnabled())
        assertFalse(mockThemeManager.isThemeApplied())
        
        // Test text size scale updates
        val scaleResult = mockThemeManager.updateTextSizeScale(1.5f)
        assertTrue(scaleResult.isSuccess)
        assertEquals(1.5f, mockThemeManager.getCurrentTextSizeScale())
        
        // Test invalid scale values
        val invalidScaleResult = mockThemeManager.updateTextSizeScale(3.0f)
        assertTrue(invalidScaleResult.isError)
        
        // Test high contrast mode
        val contrastResult = mockThemeManager.updateHighContrastMode(true)
        assertTrue(contrastResult.isSuccess)
        assertTrue(mockThemeManager.isHighContrastModeEnabled())
        
        // Test theme application
        val applyResult = mockThemeManager.applyCurrentTheme()
        assertTrue(applyResult.isSuccess)
        assertTrue(mockThemeManager.isThemeApplied())
        
        // Test reset to system defaults
        val resetResult = mockThemeManager.resetToSystemDefaults()
        assertTrue(resetResult.isSuccess)
        assertEquals(1.0f, mockThemeManager.getCurrentTextSizeScale())
        assertFalse(mockThemeManager.isHighContrastModeEnabled())
        assertFalse(mockThemeManager.isThemeApplied())
        
        // Test manual reset
        mockThemeManager.updateTextSizeScale(2.0f)
        mockThemeManager.updateHighContrastMode(true)
        mockThemeManager.applyCurrentTheme()
        
        mockThemeManager.reset()
        assertEquals(1.0f, mockThemeManager.getCurrentTextSizeScale())
        assertFalse(mockThemeManager.isHighContrastModeEnabled())
        assertFalse(mockThemeManager.isThemeApplied())
    }
    
    @Test
    fun `iOS service mocking and dependency injection works correctly`() = runTest {
        // Verify all services can be injected
        assertNotNull(networkConnectivity)
        assertNotNull(hapticFeedbackManager)
        assertNotNull(themeManager)
        assertNotNull(platformManager)
        
        // Verify they are mock implementations
        assertTrue(networkConnectivity is com.eunio.healthapp.testutil.MockNetworkConnectivity)
        assertTrue(hapticFeedbackManager is MockHapticFeedbackManager)
        assertTrue(themeManager is MockThemeManager)
        assertTrue(platformManager is MockPlatformManager)
        
        // Test that services work correctly through DI
        assertTrue(networkConnectivity.isConnected())
        assertTrue(hapticFeedbackManager.isHapticFeedbackAvailable())
        assertEquals(1.0f, themeManager.getCurrentTextSizeScale())
        assertEquals("Test Platform", platformManager.getPlatformName())
        
        // Test service interactions
        hapticFeedbackManager.performSuccessHaptic()
        val mockHaptic = hapticFeedbackManager as MockHapticFeedbackManager
        assertEquals(1, mockHaptic.getHapticEventCount())
        
        themeManager.updateTextSizeScale(1.2f)
        assertEquals(1.2f, themeManager.getCurrentTextSizeScale())
        
        val mockNetwork = networkConnectivity as com.eunio.healthapp.testutil.MockNetworkConnectivity
        mockNetwork.setConnected(false)
        assertFalse(networkConnectivity.isConnected())
    }
    
    @Test
    fun `iOS app lifecycle simulation works correctly`() = runTest {
        val iosEnvironment = IOSTestSupport.setupIOSTestEnvironment()
        
        // Test lifecycle methods don't throw exceptions
        iosEnvironment.simulateAppDidBecomeActive()
        iosEnvironment.simulateAppWillResignActive()
        iosEnvironment.simulateAppDidEnterBackground()
        iosEnvironment.simulateAppWillEnterForeground()
        iosEnvironment.simulateMemoryWarning()
        
        // All should complete without exceptions
        assertTrue(true)
    }
    
    @Test
    fun `iOS test utilities work correctly`() = runTest {
        // Test orientation simulation
        IOSTestUtils.simulateOrientationChange(DeviceOrientation.LANDSCAPE_LEFT)
        IOSTestUtils.simulateOrientationChange(DeviceOrientation.PORTRAIT)
        
        // Test accessibility settings simulation
        IOSTestUtils.simulateAccessibilitySettingsChange(
            voiceOverEnabled = true,
            reduceMotionEnabled = true,
            increaseContrastEnabled = true
        )
        
        // Test system theme changes
        IOSTestUtils.simulateSystemThemeChange(isDarkMode = true)
        IOSTestUtils.simulateSystemThemeChange(isDarkMode = false)
        
        // Test low power mode simulation
        IOSTestUtils.simulateLowPowerModeChange(isEnabled = true)
        IOSTestUtils.simulateLowPowerModeChange(isEnabled = false)
        
        // Test network connectivity simulation
        IOSTestUtils.simulateNetworkConnectivityChange(
            isConnected = true,
            connectionType = IOSConnectionType.WIFI
        )
        IOSTestUtils.simulateNetworkConnectivityChange(
            isConnected = false,
            connectionType = IOSConnectionType.NONE
        )
        
        // All should complete without exceptions
        assertTrue(true)
    }
    
    @Test
    fun `iOS test compilation and execution works in CI CD environment`() = runTest {
        // This test verifies that iOS tests can run in CI/CD environments
        // using Xcode command line tools
        
        // Test that we can create and use iOS-specific components
        val iosEnvironment = IOSTestSupport.setupIOSTestEnvironment()
        val userDefaults = iosEnvironment.getUserDefaults()
        val platformManager = iosEnvironment.getPlatformManager()
        
        // Perform operations that would typically fail in CI if not properly set up
        userDefaults.setObject("ci_test_value", "ci_test_key")
        assertEquals("ci_test_value", userDefaults.stringForKey("ci_test_key"))
        
        platformManager.optimizePerformance()
        assertTrue((platformManager as MockIOSPlatformManager).isPerformanceOptimized())
        
        // Test that cleanup works properly
        iosEnvironment.reset()
        assertEquals(0, userDefaults.getDataSize())
        assertFalse((platformManager as MockIOSPlatformManager).isPerformanceOptimized())
        
        // Verify no exceptions were thrown
        assertTrue(true)
    }
}