package com.eunio.healthapp.testutil

import com.eunio.healthapp.domain.util.NetworkConnectivity
import com.eunio.healthapp.platform.haptic.HapticFeedbackManager
import com.eunio.healthapp.platform.theme.ThemeManager
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

/**
 * Test result enum for complex operation example
 */
enum class OperationResult {
    Success,
    SuccessHighContrast,
    NetworkError
}

/**
 * Example demonstrating how to use mock services with Koin in tests.
 * This shows practical patterns for testing with dependency injection.
 */
class MockServiceKoinUsageExample : KoinTest {
    
    // Get services from Koin context using inject
    private val networkConnectivity: NetworkConnectivity by inject()
    private val hapticFeedbackManager: HapticFeedbackManager by inject()
    private val themeManager: ThemeManager by inject()
    
    @BeforeTest
    fun setup() {
        startKoin {
            modules(testModule) // Use the full test module
        }
    }
    
    @AfterTest
    fun teardown() {
        stopKoin()
    }
    
    @Test
    fun `example - testing network-dependent functionality`() = runTest {
        // Simulate a component that depends on network connectivity
        class NetworkDependentService(private val connectivity: NetworkConnectivity) {
            suspend fun performNetworkOperation(): String {
                return if (connectivity.hasStableConnection()) {
                    "Operation successful"
                } else {
                    "Operation failed - no stable connection"
                }
            }
        }
        
        val service = NetworkDependentService(networkConnectivity)
        
        // Test with stable connection (default state)
        assertEquals("Operation successful", service.performNetworkOperation())
        
        // Simulate connection issues
        val mockConnectivity = networkConnectivity as com.eunio.healthapp.testutil.MockNetworkConnectivity
        mockConnectivity.setConnected(false)
        
        // Test with no connection
        assertEquals("Operation failed - no stable connection", service.performNetworkOperation())
        
        // Restore connection
        mockConnectivity.setConnected(true)
        assertEquals("Operation successful", service.performNetworkOperation())
    }
    
    @Test
    fun `example - testing haptic feedback integration`() = runTest {
        // Simulate a UI component that provides haptic feedback
        class InteractiveButton(private val hapticManager: HapticFeedbackManager) {
            suspend fun onTap() {
                if (hapticManager.isHapticFeedbackAvailable()) {
                    hapticManager.performHapticFeedback(HapticIntensity.LIGHT)
                }
            }
            
            suspend fun onSuccess() {
                hapticManager.performSuccessHaptic()
            }
            
            suspend fun onError() {
                hapticManager.performErrorHaptic()
            }
        }
        
        val button = InteractiveButton(hapticFeedbackManager)
        val mockHaptic = hapticFeedbackManager as MockHapticFeedbackManager
        
        // Test normal interaction
        button.onTap()
        assertEquals(1, mockHaptic.getHapticEventCount())
        assertEquals("performHapticFeedback", mockHaptic.getLastHapticEvent()?.method)
        
        // Test success feedback
        button.onSuccess()
        assertEquals(2, mockHaptic.getHapticEventCount())
        assertEquals("performSuccessHaptic", mockHaptic.getLastHapticEvent()?.method)
        
        // Test error feedback
        button.onError()
        assertEquals(3, mockHaptic.getHapticEventCount())
        assertEquals("performErrorHaptic", mockHaptic.getLastHapticEvent()?.method)
        
        // Test with haptic feedback disabled
        mockHaptic.setHapticFeedbackAvailable(false)
        mockHaptic.clearHapticHistory()
        
        button.onTap() // Should not trigger haptic feedback
        assertEquals(0, mockHaptic.getHapticEventCount())
    }
    
    @Test
    fun `example - testing theme-dependent functionality`() = runTest {
        // Simulate a component that adapts to theme settings
        class AdaptiveComponent(private val themeManager: ThemeManager) {
            suspend fun getRecommendedFontSize(): Float {
                val baseSize = 16.0f
                return baseSize * themeManager.getCurrentTextSizeScale()
            }
            
            suspend fun shouldUseHighContrastColors(): Boolean {
                return themeManager.isHighContrastModeEnabled()
            }
            
            suspend fun applyAccessibilitySettings() {
                // Simulate applying accessibility-friendly settings
                if (themeManager.getCurrentTextSizeScale() > 1.2f) {
                    themeManager.updateHighContrastMode(true)
                }
            }
        }
        
        val component = AdaptiveComponent(themeManager)
        val mockTheme = themeManager as MockThemeManager
        
        // Test default settings
        assertEquals(16.0f, component.getRecommendedFontSize())
        assertFalse(component.shouldUseHighContrastColors())
        
        // Test with larger text scale
        themeManager.updateTextSizeScale(1.5f)
        assertEquals(24.0f, component.getRecommendedFontSize())
        
        // Test accessibility settings application
        component.applyAccessibilitySettings()
        assertTrue(component.shouldUseHighContrastColors())
        
        // Verify theme history tracking
        val themeHistory = mockTheme.getThemeHistory()
        assertTrue(themeHistory.any { it.method == "updateTextSizeScale" })
        assertTrue(themeHistory.any { it.method == "updateHighContrastMode" })
    }
    
    @Test
    fun `example - testing with multiple mock services`() = runTest {
        // Simulate a complex component that uses multiple services
        class ComplexFeature(
            private val connectivity: NetworkConnectivity,
            private val haptic: HapticFeedbackManager,
            private val theme: ThemeManager
        ) {
            suspend fun performComplexOperation(): OperationResult {
                // Check network first
                if (!connectivity.hasStableConnection()) {
                    haptic.performErrorHaptic()
                    return OperationResult.NetworkError
                }
                
                // Perform operation with appropriate feedback
                haptic.performHapticFeedback(HapticIntensity.MEDIUM)
                
                // Apply theme-based adjustments
                if (theme.isHighContrastModeEnabled()) {
                    // Use high contrast mode
                    return OperationResult.SuccessHighContrast
                } else {
                    return OperationResult.Success
                }
            }
        }
        

        
        val feature = ComplexFeature(networkConnectivity, hapticFeedbackManager, themeManager)
        
        // Test successful operation
        val result1 = feature.performComplexOperation()
        assertEquals(OperationResult.Success, result1)
        
        // Test with high contrast mode
        themeManager.updateHighContrastMode(true)
        val result2 = feature.performComplexOperation()
        assertEquals(OperationResult.SuccessHighContrast, result2)
        
        // Test with network error
        (networkConnectivity as com.eunio.healthapp.testutil.MockNetworkConnectivity).setConnected(false)
        val result3 = feature.performComplexOperation()
        assertEquals(OperationResult.NetworkError, result3)
        
        // Verify haptic feedback was called appropriately
        val hapticHistory = (hapticFeedbackManager as MockHapticFeedbackManager).getHapticHistory()
        assertTrue(hapticHistory.any { it.method == "performHapticFeedback" })
        assertTrue(hapticHistory.any { it.method == "performErrorHaptic" })
    }
    
    @Test
    fun `example - testing service state isolation between tests`() = runTest {
        // This test demonstrates that mock services maintain state within a test
        // but are properly isolated between tests
        
        val mockConnectivity = networkConnectivity as com.eunio.healthapp.testutil.MockNetworkConnectivity
        val mockHaptic = hapticFeedbackManager as MockHapticFeedbackManager
        
        // Modify service states
        mockConnectivity.setConnected(false)
        mockHaptic.performSuccessHaptic()
        
        // Verify states are modified
        assertFalse(networkConnectivity.isConnected())
        assertEquals(1, mockHaptic.getHapticEventCount())
        
        // Note: In a real test suite, these states would be reset between tests
        // by the BaseKoinTest setup/teardown or by explicit reset calls
    }
    
    @Test
    fun `example - manual service reset during test`() = runTest {
        // Sometimes you need to reset services during a test
        val mockConnectivity = networkConnectivity as com.eunio.healthapp.testutil.MockNetworkConnectivity
        val mockHaptic = hapticFeedbackManager as MockHapticFeedbackManager
        
        // Perform some operations
        mockConnectivity.setConnected(false)
        mockHaptic.performErrorHaptic()
        
        // Verify initial state
        assertFalse(networkConnectivity.isConnected())
        assertEquals(1, mockHaptic.getHapticEventCount())
        
        // Reset services manually
        mockConnectivity.setConnected(true)
        mockHaptic.reset()
        
        // Verify reset state
        assertTrue(networkConnectivity.isConnected())
        assertEquals(0, mockHaptic.getHapticEventCount())
    }
}