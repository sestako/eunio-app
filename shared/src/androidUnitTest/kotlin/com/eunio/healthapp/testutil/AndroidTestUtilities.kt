package com.eunio.healthapp.testutil

import android.content.Context
import android.content.SharedPreferences
import com.eunio.healthapp.platform.PlatformManager
import com.eunio.healthapp.platform.PlatformLifecycleManager
import com.eunio.healthapp.platform.PlatformNavigationManager
import com.eunio.healthapp.platform.haptic.HapticFeedbackManager
import com.eunio.healthapp.platform.theme.ThemeManager
import com.eunio.healthapp.platform.accessibility.AccessibilityManager
import io.mockk.mockk
import io.mockk.every
import io.mockk.just
import io.mockk.Runs

/**
 * Comprehensive Android test utilities for creating properly configured mock services
 * and Android-specific test environments.
 */
object AndroidTestUtilities {
    
    /**
     * Creates a complete Android test environment with all necessary mocks
     */
    fun createAndroidTestEnvironment(): AndroidTestEnvironment {
        return AndroidTestEnvironment(
            context = createMockAndroidContext(),
            sharedPreferences = AndroidTestContext.createMockSharedPreferences(),
            platformManager = AndroidTestContext.createMockPlatformManager(),
            hapticFeedbackManager = AndroidTestContext.createMockHapticFeedbackManager(),
            themeManager = AndroidTestContext.createMockThemeManager(),
            accessibilityManager = AndroidTestContext.createMockAccessibilityManager(),
            lifecycleManager = AndroidTestContext.createMockLifecycleManager(),
            navigationManager = AndroidTestContext.createMockNavigationManager()
        )
    }
    
    /**
     * Creates a mock Android Context using mockk for tests that need Context behavior
     */
    fun createMockAndroidContext(): Context {
        val mockContext = mockk<Context>(relaxed = true)
        val mockSharedPreferences = AndroidTestContext.createMockSharedPreferences()
        
        every { mockContext.getSharedPreferences(any(), any()) } returns mockSharedPreferences
        every { mockContext.cacheDir } returns java.io.File("/tmp/test-cache")
        every { mockContext.filesDir } returns java.io.File("/tmp/test-files")
        every { mockContext.packageName } returns "com.eunio.healthapp.test"
        every { mockContext.applicationContext } returns mockContext
        
        return mockContext
    }
    
    /**
     * Creates a complete set of Android platform service mocks
     */
    fun createAndroidPlatformServices(): AndroidPlatformServices {
        return AndroidPlatformServices(
            hapticFeedbackManager = AndroidTestContext.createMockHapticFeedbackManager(),
            themeManager = AndroidTestContext.createMockThemeManager(),
            accessibilityManager = AndroidTestContext.createMockAccessibilityManager(),
            lifecycleManager = AndroidTestContext.createMockLifecycleManager(),
            navigationManager = AndroidTestContext.createMockNavigationManager(),
            platformManager = AndroidTestContext.createMockPlatformManager()
        )
    }
    
    /**
     * Configures SharedPreferences mocks with common test data
     */
    fun configureSharedPreferencesWithTestData(sharedPreferences: MockSharedPreferences) {
        val editor = sharedPreferences.edit()
        
        // Add common test preferences
        editor.putString("user_id", "test-user-123")
        editor.putBoolean("onboarding_completed", true)
        editor.putString("theme_preference", "SYSTEM")
        editor.putFloat("text_size_scale", 1.0f)
        editor.putBoolean("haptic_feedback_enabled", true)
        editor.putString("haptic_intensity", "MEDIUM")
        editor.putBoolean("high_contrast_mode", false)
        editor.putBoolean("notifications_enabled", true)
        editor.putString("temperature_unit", "CELSIUS")
        editor.putString("weight_unit", "KILOGRAMS")
        
        editor.apply()
    }
    
    /**
     * Resets all Android test utilities to clean state
     */
    fun resetAllAndroidMocks(environment: AndroidTestEnvironment) {
        (environment.hapticFeedbackManager as MockAndroidHapticFeedbackManager).clearHapticHistory()
        (environment.themeManager as MockAndroidThemeManager).reset()
        (environment.accessibilityManager as MockAndroidAccessibilityManager).reset()
        (environment.lifecycleManager as MockAndroidLifecycleManager).reset()
        (environment.navigationManager as MockAndroidNavigationManager).reset()
        (environment.platformManager as MockAndroidPlatformManager).reset()
    }
}

/**
 * Container for complete Android test environment
 */
data class AndroidTestEnvironment(
    val context: Context,
    val sharedPreferences: SharedPreferences,
    val platformManager: PlatformManager,
    val hapticFeedbackManager: HapticFeedbackManager,
    val themeManager: ThemeManager,
    val accessibilityManager: AccessibilityManager,
    val lifecycleManager: PlatformLifecycleManager,
    val navigationManager: PlatformNavigationManager
)

/**
 * Container for Android platform services
 */
data class AndroidPlatformServices(
    val hapticFeedbackManager: HapticFeedbackManager,
    val themeManager: ThemeManager,
    val accessibilityManager: AccessibilityManager,
    val lifecycleManager: PlatformLifecycleManager,
    val navigationManager: PlatformNavigationManager,
    val platformManager: PlatformManager
)

/**
 * Base class for Android unit tests with proper setup and teardown
 */
abstract class BaseAndroidUnitTest {
    protected lateinit var androidTestEnvironment: AndroidTestEnvironment
    protected lateinit var mockContext: Context
    protected lateinit var mockSharedPreferences: SharedPreferences
    
    open fun setupAndroidTest() {
        androidTestEnvironment = AndroidTestUtilities.createAndroidTestEnvironment()
        mockContext = androidTestEnvironment.context
        mockSharedPreferences = androidTestEnvironment.sharedPreferences
        
        // Configure with test data
        AndroidTestUtilities.configureSharedPreferencesWithTestData(
            mockSharedPreferences as MockSharedPreferences
        )
    }
    
    open fun teardownAndroidTest() {
        AndroidTestUtilities.resetAllAndroidMocks(androidTestEnvironment)
    }
}