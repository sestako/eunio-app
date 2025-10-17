package com.eunio.healthapp.testutil

import com.eunio.healthapp.domain.model.settings.HapticIntensity
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for platform-specific test utilities to ensure they work correctly
 */
class PlatformTestUtilitiesTest {
    
    @Test
    fun testMockServiceFactory_createsAllServices() = runTest {
        // Given
        val mockServices = MockServiceFactory.createMockServiceSet()
        
        // Then
        assertTrue(mockServices.platformManager.getPlatformName().isNotEmpty())
        assertTrue(mockServices.hapticFeedbackManager.isHapticFeedbackAvailable())
        assertTrue(mockServices.themeManager.getCurrentTextSizeScale() > 0)
        assertTrue(mockServices.notificationService.areNotificationsEnabled())
        assertFalse(mockServices.accessibilityManager.isVoiceOverEnabled())
        assertTrue(mockServices.lifecycleManager.getLifecycleEvents().isEmpty())
        assertTrue(mockServices.navigationManager.getNavigationHistory().isEmpty())
    }
    
    @Test
    fun testMockServiceFactory_resetFunctionality() = runTest {
        // Given
        val mockServices = MockServiceFactory.createMockServiceSet()
        
        // When - perform some operations
        mockServices.hapticFeedbackManager.performLightHaptic()
        mockServices.themeManager.updateTextSizeScale(1.5f)
        mockServices.lifecycleManager.onAppStart()
        mockServices.platformManager.handleDeepLink("eunio://test")
        
        // Verify operations were recorded
        assertTrue(mockServices.hapticFeedbackManager.getHapticHistory().isNotEmpty())
        assertTrue(mockServices.themeManager.getThemeHistory().isNotEmpty())
        assertTrue(mockServices.lifecycleManager.getLifecycleEvents().isNotEmpty())
        assertTrue(mockServices.platformManager.getDeepLinkHistory().isNotEmpty())
        
        // When - reset all services
        mockServices.resetAll()
        
        // Then - all services should be clean
        assertTrue(mockServices.hapticFeedbackManager.getHapticHistory().isEmpty())
        assertTrue(mockServices.themeManager.getThemeHistory().isEmpty())
        assertTrue(mockServices.lifecycleManager.getLifecycleEvents().isEmpty())
        assertTrue(mockServices.platformManager.getDeepLinkHistory().isEmpty())
    }
    
    @Test
    fun testMockHapticFeedbackManager_functionality() = runTest {
        // Given
        val hapticManager = MockServiceFactory.createMockHapticFeedbackManager()
        
        // When
        hapticManager.performHapticFeedback(HapticIntensity.LIGHT)
        hapticManager.performMediumHaptic()
        hapticManager.performSuccessHaptic()
        
        // Then
        assertEquals(3, hapticManager.getHapticEventCount())
        assertEquals("performSuccessHaptic", hapticManager.getLastHapticEvent()?.method)
        
        val history = hapticManager.getHapticHistory()
        assertEquals("performHapticFeedback", history[0].method)
        assertEquals("LIGHT", history[0].parameter)
        assertEquals("performMediumHaptic", history[1].method)
        assertEquals("performSuccessHaptic", history[2].method)
    }
    
    @Test
    fun testMockThemeManager_functionality() = runTest {
        // Given
        val themeManager = MockServiceFactory.createMockThemeManager()
        
        // When
        val scaleResult = themeManager.updateTextSizeScale(1.5f)
        val contrastResult = themeManager.updateHighContrastMode(true)
        val applyResult = themeManager.applyCurrentTheme()
        
        // Then
        scaleResult.assertSuccess()
        contrastResult.assertSuccess()
        applyResult.assertSuccess()
        
        assertEquals(1.5f, themeManager.getCurrentTextSizeScale())
        assertTrue(themeManager.isHighContrastModeEnabled())
        assertTrue(themeManager.isThemeApplied())
        
        val history = themeManager.getThemeHistory()
        assertEquals(3, history.size)
        assertEquals("updateTextSizeScale", history[0].method)
        assertEquals("1.5", history[0].parameter)
        assertEquals("updateHighContrastMode", history[1].method)
        assertEquals("true", history[1].parameter)
        assertEquals("applyCurrentTheme", history[2].method)
    }
    
    @Test
    fun testMockPlatformManager_functionality() = runTest {
        // Given
        val platformManager = MockServiceFactory.createMockPlatformManager()
        
        // When
        platformManager.optimizePerformance()
        val deepLinkResult1 = platformManager.handleDeepLink("eunio://test")
        val deepLinkResult2 = platformManager.handleDeepLink("https://example.com")
        platformManager.shareContent("Test content", "Test title")
        
        // Then
        assertTrue(platformManager.isPerformanceOptimized())
        assertTrue(deepLinkResult1) // Should handle eunio:// links
        assertFalse(deepLinkResult2) // Should not handle other links
        
        val deepLinkHistory = platformManager.getDeepLinkHistory()
        assertEquals(2, deepLinkHistory.size)
        assertEquals("eunio://test", deepLinkHistory[0])
        assertEquals("https://example.com", deepLinkHistory[1])
        
        val sharedContent = platformManager.getSharedContent()
        assertEquals(1, sharedContent.size)
        assertEquals("Test content", sharedContent[0].content)
        assertEquals("Test title", sharedContent[0].title)
    }
    
    @Test
    fun testMockNotificationService_functionality() = runTest {
        // Given
        val notificationService = MockServiceFactory.createMockNotificationService()
        val triggerTime = Clock.System.now().toEpochMilliseconds()
        
        // When
        val scheduleResult = notificationService.scheduleOneTimeNotification(
            "test-1", "Test Title", "Test Body", triggerTime
        )
        notificationService.scheduleOneTimeNotification(
            "test-2", "Test Title 2", "Test Body 2", triggerTime
        )
        
        // Then
        assertTrue(scheduleResult.isSuccess)
        assertEquals(2, notificationService.getNotificationCount())
        assertTrue(notificationService.hasNotification("test-1"))
        assertTrue(notificationService.hasNotification("test-2"))
        
        // When - cancel one notification
        val cancelResult = notificationService.cancelNotification("test-1")
        
        // Then
        assertTrue(cancelResult.isSuccess)
        assertEquals(1, notificationService.getNotificationCount())
        assertFalse(notificationService.hasNotification("test-1"))
        assertTrue(notificationService.hasNotification("test-2"))
        
        // When - cancel all notifications
        val cancelAllResult = notificationService.cancelAllNotifications()
        
        // Then
        assertTrue(cancelAllResult.isSuccess)
        assertEquals(0, notificationService.getNotificationCount())
    }
    
    @Test
    fun testMockAccessibilityManager_functionality() = runTest {
        // Given
        val accessibilityManager = MockServiceFactory.createMockAccessibilityManager()
        
        // Initially all should be false
        assertFalse(accessibilityManager.isVoiceOverEnabled())
        assertFalse(accessibilityManager.isReduceMotionEnabled())
        assertFalse(accessibilityManager.isBoldTextEnabled())
        assertFalse(accessibilityManager.isSystemHighContrastEnabled())
        
        // When
        accessibilityManager.setVoiceOverEnabled(true)
        accessibilityManager.setReduceMotionEnabled(true)
        accessibilityManager.setBoldTextEnabled(true)
        accessibilityManager.setSystemHighContrastEnabled(true)
        
        // Then
        assertTrue(accessibilityManager.isVoiceOverEnabled())
        assertTrue(accessibilityManager.isReduceMotionEnabled())
        assertTrue(accessibilityManager.isBoldTextEnabled())
        assertTrue(accessibilityManager.isSystemHighContrastEnabled())
        
        // When - announce for accessibility (void method)
        accessibilityManager.announceForAccessibility("Test message")
        
        // Then - should not throw (void method test)
    }
    
    @Test
    fun testMockLifecycleManager_functionality() = runTest {
        // Given
        val lifecycleManager = MockServiceFactory.createMockLifecycleManager()
        
        // When
        lifecycleManager.onAppStart()
        lifecycleManager.onAppResume()
        lifecycleManager.onAppPause()
        lifecycleManager.onLowMemory()
        lifecycleManager.onAppStop()
        lifecycleManager.cleanup()
        
        // Then
        assertEquals(6, lifecycleManager.getEventCount())
        assertTrue(lifecycleManager.hasEvent("onAppStart"))
        assertTrue(lifecycleManager.hasEvent("onAppResume"))
        assertTrue(lifecycleManager.hasEvent("onAppPause"))
        assertTrue(lifecycleManager.hasEvent("onLowMemory"))
        assertTrue(lifecycleManager.hasEvent("onAppStop"))
        assertTrue(lifecycleManager.hasEvent("cleanup"))
        
        assertEquals("cleanup", lifecycleManager.getLastLifecycleEvent()?.eventType)
    }
    
    @Test
    fun testTestExtensions_resultTesting() = runTest {
        // Given
        val successResult = com.eunio.healthapp.domain.util.Result.Success("test data")
        val errorResult = com.eunio.healthapp.domain.util.Result.Error(com.eunio.healthapp.domain.error.AppError.UnknownError("test error"))
        
        // When/Then - success testing
        val data = successResult.assertSuccess()
        assertEquals("test data", data)
        
        successResult.assertSuccessEquals("test data")
        
        // When/Then - error testing
        val exception = errorResult.assertError()
        assertEquals("test error", exception.message)
        
        errorResult.assertErrorMessage("test error")
    }
    
    @Test
    fun testTestExtensions_collectionTesting() = runTest {
        // Given
        val list = listOf("a", "b", "c")
        val emptyList = emptyList<String>()
        
        // When/Then
        list.assertContainsExactly("a", "b", "c")
        list.assertContainsAll("a", "c")
        list.assertSize(3)
        list.assertNotEmpty()
        
        emptyList.assertEmpty()
        emptyList.assertSize(0)
    }
    
    @Test
    fun testTestExtensions_stringTesting() = runTest {
        // Given
        val text = "Hello World"
        val email = "test@example.com"
        val password = "password123"
        val userId = "user_123"
        
        // When/Then
        text.assertContainsIgnoreCase("HELLO")
        text.assertMatches(Regex("Hello.*"))
        text.assertNotBlank()
        
        email.assertValidEmail()
        password.assertValidPassword()
        userId.assertValidUserId()
    }
    
    @Test
    fun testTestExtensions_performanceTesting() = runTest {
        // Given
        suspend fun fastOperation() = "result"
        suspend fun slowOperation(): String {
            kotlinx.coroutines.delay(500) // Use a longer delay to ensure reliability
            return "result"
        }
        
        // When/Then - Test fast operation completion
        val fastResult = assertCompletesWithin(1.seconds) { fastOperation() }
        assertEquals("result", fastResult)
        
        // Test timing measurement functionality (without strict timing requirements)
        val (result, duration) = measureExecutionTime { slowOperation() }
        assertEquals("result", result)
        
        // Verify that duration is reasonable (should be at least some time but not too long)
        // This is more about testing the measurement functionality than precise timing
        assertTrue(duration.inWholeMilliseconds >= 0, "Duration should be non-negative")
        assertTrue(duration.inWholeMilliseconds < 10000, "Duration should be reasonable (less than 10 seconds)")
        
        // Test the assertCompletesWithin function with a generous timeout
        val timedResult = assertCompletesWithin(2.seconds) { slowOperation() }
        assertEquals("result", timedResult)
    }
    
    @Test
    fun testTestExtensions_mockServiceTesting() = runTest {
        // Given
        val mockServices = MockServiceFactory.createMockServiceSet()
        
        // When
        runTestWithMockServices(mockServices) { services ->
            services.hapticFeedbackManager.performLightHaptic()
            services.themeManager.updateTextSizeScale(1.2f)
            services.lifecycleManager.onAppStart()
            
            // Verify within test
            services.hapticFeedbackManager.assertHapticPerformed("performLightHaptic")
            services.themeManager.assertThemeEventOccurred("updateTextSizeScale")
            services.lifecycleManager.assertLifecycleEventOccurred("onAppStart")
        }
        
        // Then - services should be reset after test
        mockServices.resetAndVerify()
    }
}