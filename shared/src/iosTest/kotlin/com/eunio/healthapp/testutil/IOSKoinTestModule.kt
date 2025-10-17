package com.eunio.healthapp.testutil

import com.eunio.healthapp.domain.util.NetworkConnectivity
import com.eunio.healthapp.platform.PlatformManager
import com.eunio.healthapp.platform.haptic.HapticFeedbackManager
import com.eunio.healthapp.platform.theme.ThemeManager
import com.eunio.healthapp.platform.notification.PlatformNotificationService
import com.eunio.healthapp.platform.accessibility.AccessibilityManager
import com.eunio.healthapp.platform.PlatformLifecycleManager
import com.eunio.healthapp.platform.PlatformNavigationManager
import org.koin.dsl.module

/**
 * iOS-specific Koin test module that provides iOS-optimized mock implementations
 * for all platform services.
 */
val iosTestModule = module {
    
    // Network connectivity with iOS-specific behavior
    single<NetworkConnectivity> { 
        com.eunio.healthapp.testutil.MockNetworkConnectivity().apply {
            // Configure for iOS-specific network behavior
            setNetworkType(com.eunio.healthapp.domain.util.NetworkType.WIFI)
            setConnected(true)
        }
    }
    
    // iOS Platform Manager
    single<PlatformManager> { 
        MockIOSPlatformManager()
    }
    
    // iOS Haptic Feedback Manager
    single<HapticFeedbackManager> { 
        MockIOSHapticFeedbackManager().apply {
            // Configure for iOS haptic capabilities
            setHapticFeedbackAvailable(true)
            setSystemHapticEnabled(true)
        }
    }
    
    // iOS Theme Manager
    single<ThemeManager> { 
        MockIOSThemeManager()
    }
    
    // iOS Notification Service
    single<PlatformNotificationService> { 
        MockNotificationService()
    }
    
    // iOS Accessibility Manager
    single<AccessibilityManager> { 
        MockAccessibilityManager().apply {
            // Configure for iOS accessibility features
            setScreenReaderEnabled(false)
            setSystemTextSizeScale(1.0f)
        }
    }
    
    // iOS Lifecycle Manager
    single<PlatformLifecycleManager> { 
        MockPlatformLifecycleManager()
    }
    
    // iOS Navigation Manager
    single<PlatformNavigationManager> { 
        MockPlatformNavigationManager()
    }
    
    // iOS Test Environment
    single<IOSTestEnvironment> { 
        IOSTestSupport.setupIOSTestEnvironment()
    }
    
    // Mock NSUserDefaults
    single<MockNSUserDefaults> { 
        IOSTestSupport.createMockUserDefaults()
    }
    
    // iOS Mock Service Set for coordinated testing
    single<IOSMockServiceSet> { 
        IOSMockServiceSet(
            networkConnectivity = get(),
            platformManager = get(),
            hapticFeedbackManager = get(),
            themeManager = get(),
            notificationService = get(),
            accessibilityManager = get(),
            lifecycleManager = get(),
            navigationManager = get(),
            testEnvironment = get(),
            userDefaults = get()
        )
    }
}

/**
 * iOS-specific mock service set for coordinated testing and state management
 */
class IOSMockServiceSet(
    private val networkConnectivity: NetworkConnectivity,
    private val platformManager: PlatformManager,
    private val hapticFeedbackManager: HapticFeedbackManager,
    private val themeManager: ThemeManager,
    private val notificationService: PlatformNotificationService,
    private val accessibilityManager: AccessibilityManager,
    private val lifecycleManager: PlatformLifecycleManager,
    private val navigationManager: PlatformNavigationManager,
    private val testEnvironment: IOSTestEnvironment,
    private val userDefaults: MockNSUserDefaults
) {
    
    /**
     * Resets all iOS mock services to their default state
     */
    fun resetAll() {
        // Reset network connectivity
        (networkConnectivity as com.eunio.healthapp.testutil.MockNetworkConnectivity).apply {
            setConnected(true)
            setNetworkType(com.eunio.healthapp.domain.util.NetworkType.WIFI)
            reset()
        }
        
        // Reset platform manager
        (platformManager as MockIOSPlatformManager).reset()
        
        // Reset haptic feedback manager
        (hapticFeedbackManager as MockIOSHapticFeedbackManager).apply {
            clearHapticHistory()
            setHapticFeedbackAvailable(true)
            setSystemHapticEnabled(true)
        }
        
        // Reset theme manager
        (themeManager as MockIOSThemeManager).reset()
        
        // Reset notification service
        (notificationService as MockNotificationService).apply {
            reset()
        }
        
        // Reset accessibility manager
        (accessibilityManager as MockAccessibilityManager).apply {
            setScreenReaderEnabled(false)
            setSystemTextSizeScale(1.0f)
        }
        
        // Reset lifecycle manager
        (lifecycleManager as MockPlatformLifecycleManager).reset()
        
        // Reset navigation manager
        (navigationManager as MockPlatformNavigationManager).reset()
        
        // Reset test environment
        testEnvironment.reset()
        
        // Reset user defaults
        userDefaults.reset()
    }
    
    /**
     * Configures all services for iOS-specific testing scenarios
     */
    suspend fun configureForIOSScenario(scenario: IOSTestScenario) {
        when (scenario) {
            IOSTestScenario.LOW_POWER_MODE -> {
                (platformManager as MockIOSPlatformManager).apply {
                    // Simulate low power mode constraints
                }
                (hapticFeedbackManager as MockIOSHapticFeedbackManager).apply {
                    setHapticFeedbackAvailable(false) // Haptics disabled in low power mode
                }
            }
            
            IOSTestScenario.ACCESSIBILITY_ENABLED -> {
                (accessibilityManager as MockAccessibilityManager).apply {
                    setScreenReaderEnabled(true)
                    setSystemTextSizeScale(1.5f)
                }
                (themeManager as MockIOSThemeManager).apply {
                    updateHighContrastMode(true)
                    updateTextSizeScale(1.5f)
                }
            }
            
            IOSTestScenario.OFFLINE_MODE -> {
                (networkConnectivity as com.eunio.healthapp.testutil.MockNetworkConnectivity).apply {
                    setConnected(false)
                    setNetworkType(com.eunio.healthapp.domain.util.NetworkType.NONE)
                }
            }
            
            IOSTestScenario.BACKGROUND_MODE -> {
                testEnvironment.simulateAppDidEnterBackground()
                // Configure for background notification behavior
            }
            
            IOSTestScenario.MEMORY_WARNING -> {
                testEnvironment.simulateMemoryWarning()
                // Services should handle memory pressure gracefully
            }
        }
    }
    
    /**
     * Gets the current state of all iOS services for validation
     */
    fun getCurrentState(): IOSServiceState {
        return IOSServiceState(
            isNetworkConnected = networkConnectivity.isConnected(),
            networkType = (networkConnectivity as com.eunio.healthapp.testutil.MockNetworkConnectivity).getNetworkType(),
            isHapticAvailable = hapticFeedbackManager.isHapticFeedbackAvailable(),
            hapticEventCount = (hapticFeedbackManager as MockIOSHapticFeedbackManager).getHapticEventCount(),
            textSizeScale = themeManager.getCurrentTextSizeScale(),
            isHighContrastEnabled = themeManager.isHighContrastModeEnabled(),
            isPerformanceOptimized = (platformManager as MockIOSPlatformManager).isPerformanceOptimized(),
            notificationCount = (notificationService as MockNotificationService).getNotificationCount(),
            isScreenReaderEnabled = accessibilityManager.isScreenReaderEnabled(),
            lifecycleEventCount = (lifecycleManager as MockPlatformLifecycleManager).getEventCount(),
            navigationHistorySize = (navigationManager as MockPlatformNavigationManager).getNavigationHistory().size,
            userDefaultsSize = userDefaults.getDataSize()
        )
    }
}

/**
 * iOS test scenarios for configuring mock services
 */
enum class IOSTestScenario {
    LOW_POWER_MODE,
    ACCESSIBILITY_ENABLED,
    OFFLINE_MODE,
    BACKGROUND_MODE,
    MEMORY_WARNING
}

/**
 * Current state of all iOS services for validation
 */
data class IOSServiceState(
    val isNetworkConnected: Boolean,
    val networkType: com.eunio.healthapp.domain.util.NetworkType,
    val isHapticAvailable: Boolean,
    val hapticEventCount: Int,
    val textSizeScale: Float,
    val isHighContrastEnabled: Boolean,
    val isPerformanceOptimized: Boolean,
    val notificationCount: Int,
    val isScreenReaderEnabled: Boolean,
    val lifecycleEventCount: Int,
    val navigationHistorySize: Int,
    val userDefaultsSize: Int
)