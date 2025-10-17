package com.eunio.healthapp.testutil

import com.eunio.healthapp.domain.util.NetworkConnectivity
import com.eunio.healthapp.domain.util.NetworkType
import com.eunio.healthapp.platform.PlatformManager
import com.eunio.healthapp.platform.haptic.HapticFeedbackManager
import com.eunio.healthapp.platform.theme.ThemeManager
import com.eunio.healthapp.platform.notification.PlatformNotificationService
import com.eunio.healthapp.platform.accessibility.AccessibilityManager
import com.eunio.healthapp.platform.PlatformLifecycleManager
import com.eunio.healthapp.platform.PlatformNavigationManager
import com.eunio.healthapp.domain.model.settings.HapticIntensity
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.BeforeTest
import kotlin.test.AfterTest
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Test class to validate that all mock services are properly integrated with Koin
 * and can be injected and used in tests.
 */
class MockServiceKoinIntegrationTest : KoinTest {
    
    // Get services from Koin context using inject
    private val networkConnectivity: NetworkConnectivity by inject()
    private val platformManager: PlatformManager by inject()
    private val hapticFeedbackManager: HapticFeedbackManager by inject()
    private val themeManager: ThemeManager by inject()
    private val notificationService: PlatformNotificationService by inject()
    private val accessibilityManager: AccessibilityManager by inject()
    private val lifecycleManager: PlatformLifecycleManager by inject()
    private val navigationManager: PlatformNavigationManager by inject()
    
    @BeforeTest
    fun setup() {
        startKoin {
            modules(platformServicesTestModule)
        }
    }
    
    @AfterTest
    fun teardown() {
        stopKoin()
    }
    
    @Test
    fun `test NetworkConnectivity injection and functionality`() = runTest {
        // Verify injection works
        assertNotNull(networkConnectivity)
        
        // Test basic functionality
        assertTrue(networkConnectivity.isConnected())
        assertEquals(NetworkType.WIFI, networkConnectivity.getNetworkType())
        assertTrue(networkConnectivity.hasStableConnection())
        
        // Test mock-specific functionality
        val mockNetworkConnectivity = networkConnectivity as com.eunio.healthapp.testutil.MockNetworkConnectivity
        mockNetworkConnectivity.setConnected(false)
        mockNetworkConnectivity.setNetworkType(NetworkType.NONE)
        assertFalse(networkConnectivity.isConnected())
        assertEquals(NetworkType.NONE, networkConnectivity.getNetworkType())
        assertFalse(networkConnectivity.hasStableConnection())
    }
    
    @Test
    fun `test HapticFeedbackManager injection and functionality`() = runTest {
        // Verify injection works
        assertNotNull(hapticFeedbackManager)
        
        // Test basic functionality
        assertTrue(hapticFeedbackManager.isHapticFeedbackAvailable())
        assertTrue(hapticFeedbackManager.isSystemHapticEnabled())
        
        // Test haptic feedback methods
        hapticFeedbackManager.performHapticFeedback(HapticIntensity.LIGHT)
        hapticFeedbackManager.performSuccessHaptic()
        hapticFeedbackManager.performErrorHaptic()
        
        // Test mock-specific functionality
        val mockHapticManager = hapticFeedbackManager as MockHapticFeedbackManager
        assertEquals(3, mockHapticManager.getHapticEventCount())
        assertNotNull(mockHapticManager.getLastHapticEvent())
    }
    
    @Test
    fun `test ThemeManager injection and functionality`() = runTest {
        // Verify injection works
        assertNotNull(themeManager)
        
        // Test basic functionality
        assertEquals(1.0f, themeManager.getCurrentTextSizeScale())
        assertFalse(themeManager.isHighContrastModeEnabled())
        
        // Test theme operations
        val updateResult = themeManager.updateTextSizeScale(1.5f)
        assertTrue(updateResult.isSuccess)
        assertEquals(1.5f, themeManager.getCurrentTextSizeScale())
        
        val contrastResult = themeManager.updateHighContrastMode(true)
        assertTrue(contrastResult.isSuccess)
        assertTrue(themeManager.isHighContrastModeEnabled())
        
        // Test mock-specific functionality
        val mockThemeManager = themeManager as MockThemeManager
        assertTrue(mockThemeManager.getThemeHistory().isNotEmpty())
    }
    
    @Test
    fun `test PlatformManager injection and functionality`() = runTest {
        // Verify injection works
        assertNotNull(platformManager)
        
        // Test basic functionality
        assertEquals("Test Platform", platformManager.getPlatformName())
        assertEquals("Test OS 1.0", platformManager.getOSVersion())
        assertEquals("Test Device", platformManager.getDeviceModel())
        
        // Test platform operations
        platformManager.optimizePerformance()
        assertTrue(platformManager.handleDeepLink("eunio://test"))
        assertFalse(platformManager.handleDeepLink("invalid://test"))
        
        // Test mock-specific functionality
        val mockPlatformManager = platformManager as MockPlatformManager
        assertTrue(mockPlatformManager.isPerformanceOptimized())
        assertTrue(mockPlatformManager.getDeepLinkHistory().isNotEmpty())
    }
    
    @Test
    fun `test NotificationService injection and functionality`() = runTest {
        // Verify injection works
        assertNotNull(notificationService)
        
        // Test basic functionality
        assertTrue(notificationService.areNotificationsEnabled())
        
        // Test notification operations
        val scheduleResult = notificationService.scheduleOneTimeNotification(
            "test-id",
            "Test Title",
            "Test Body",
            Clock.System.now().toEpochMilliseconds() + 60000
        )
        assertTrue(scheduleResult.isSuccess)
        
        val cancelResult = notificationService.cancelNotification("test-id")
        assertTrue(cancelResult.isSuccess)
        
        // Test mock-specific functionality
        val mockNotificationService = notificationService as MockNotificationService
        assertEquals(0, mockNotificationService.getNotificationCount()) // Should be 0 after cancellation
    }
    
    @Test
    fun `test AccessibilityManager injection and functionality`() = runTest {
        // Verify injection works
        assertNotNull(accessibilityManager)
        
        // Test basic functionality
        assertTrue(accessibilityManager.isAccessibilitySupported())
        assertFalse(accessibilityManager.isScreenReaderEnabled())
        assertEquals(1.0f, accessibilityManager.getSystemTextSizeScale())
        
        // Test mock-specific functionality
        val mockAccessibilityManager = accessibilityManager as MockAccessibilityManager
        mockAccessibilityManager.setScreenReaderEnabled(true)
        assertTrue(accessibilityManager.isScreenReaderEnabled())
    }
    
    @Test
    fun `test LifecycleManager injection and functionality`() = runTest {
        // Verify injection works
        assertNotNull(lifecycleManager)
        
        // Test lifecycle operations
        lifecycleManager.onAppStart()
        lifecycleManager.onAppResume()
        lifecycleManager.onAppPause()
        lifecycleManager.onAppStop()
        
        // Test mock-specific functionality
        val mockLifecycleManager = lifecycleManager as MockPlatformLifecycleManager
        assertEquals(4, mockLifecycleManager.getEventCount())
        assertTrue(mockLifecycleManager.hasEvent("onAppStart"))
        assertTrue(mockLifecycleManager.hasEvent("onAppStop"))
    }
    
    @Test
    fun `test NavigationManager injection and functionality`() = runTest {
        // Verify injection works
        assertNotNull(navigationManager)
        
        // Test navigation operations
        assertTrue(navigationManager.navigateBack())
        assertTrue(navigationManager.handleDeepLink("eunio://calendar"))
        assertFalse(navigationManager.handleDeepLink("invalid://link"))
        
        navigationManager.shareHealthReport("/path/to/report.pdf")
        
        // Test mock-specific functionality
        val mockNavigationManager = navigationManager as MockPlatformNavigationManager
        assertTrue(mockNavigationManager.getNavigationHistory().isNotEmpty())
        assertTrue(mockNavigationManager.getDeepLinkHistory().contains("eunio://calendar"))
        assertTrue(mockNavigationManager.getSharedReports().contains("/path/to/report.pdf"))
    }
    
    @Test
    fun `test MockServiceSet reset functionality`() = runTest {
        // Get the MockServiceSet instance from Koin
        val mockServiceSet: MockServiceSet by inject()
        
        // Perform some operations to create state
        (networkConnectivity as com.eunio.healthapp.testutil.MockNetworkConnectivity).setConnected(false)
        (hapticFeedbackManager as MockHapticFeedbackManager).performSuccessHaptic()
        (themeManager as MockThemeManager).updateTextSizeScale(1.5f)
        
        // Verify state exists
        assertFalse(networkConnectivity.isConnected())
        assertTrue((hapticFeedbackManager as MockHapticFeedbackManager).getHapticHistory().isNotEmpty())
        assertEquals(1.5f, themeManager.getCurrentTextSizeScale())
        
        // Reset all services
        mockServiceSet.resetAll()
        
        // Verify state is reset
        assertTrue(networkConnectivity.isConnected())
        assertTrue((hapticFeedbackManager as MockHapticFeedbackManager).getHapticHistory().isEmpty())
        assertEquals(1.0f, themeManager.getCurrentTextSizeScale())
    }
    
    @Test
    fun `test all services are properly registered in Koin`() {
        // Verify all services can be resolved from Koin without errors
        assertNotNull(networkConnectivity)
        assertNotNull(platformManager)
        assertNotNull(hapticFeedbackManager)
        assertNotNull(themeManager)
        assertNotNull(notificationService)
        assertNotNull(accessibilityManager)
        assertNotNull(lifecycleManager)
        assertNotNull(navigationManager)
        
        // Verify they are the correct mock implementations
        assertTrue(networkConnectivity is com.eunio.healthapp.testutil.MockNetworkConnectivity)
        assertTrue(platformManager is MockPlatformManager)
        assertTrue(hapticFeedbackManager is MockHapticFeedbackManager)
        assertTrue(themeManager is MockThemeManager)
        assertTrue(notificationService is MockNotificationService)
        assertTrue(accessibilityManager is MockAccessibilityManager)
        assertTrue(lifecycleManager is MockPlatformLifecycleManager)
        assertTrue(navigationManager is MockPlatformNavigationManager)
    }
}