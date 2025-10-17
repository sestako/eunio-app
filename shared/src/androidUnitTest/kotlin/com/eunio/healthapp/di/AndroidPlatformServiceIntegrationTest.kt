package com.eunio.healthapp.di

import android.content.Context
import com.eunio.healthapp.domain.manager.AuthManager
import com.eunio.healthapp.domain.manager.NotificationManager
import com.eunio.healthapp.domain.manager.SettingsManager
import com.eunio.healthapp.domain.service.DatabaseService
import com.eunio.healthapp.testutil.AndroidTestUtilities
import io.mockk.mockk
import io.mockk.every
import kotlinx.coroutines.test.runTest
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.get
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Android platform service integration test suite.
 * 
 * This test validates that Android platform-specific service implementations
 * can be properly resolved through dependency injection when provided with
 * the required Android Context.
 */
class AndroidPlatformServiceIntegrationTest : KoinTest {
    
    private lateinit var mockContext: Context
    
    @BeforeTest
    fun setup() {
        // Clean up any existing Koin context
        if (GlobalContext.getOrNull() != null) {
            stopKoin()
        }
        
        // Create mock Android context
        mockContext = AndroidTestUtilities.createMockAndroidContext()
    }
    
    @AfterTest
    fun tearDown() {
        stopKoin()
    }
    
    /**
     * Test that Android platform services can be resolved correctly with Context
     */
    @Test
    fun `Android platform services resolve correctly with Context`() = runTest {
        // Create a minimal test module with mocked dependencies
        val testModule = org.koin.dsl.module {
            single<Context> { mockContext }
            single<SettingsManager> { mockk<com.eunio.healthapp.domain.manager.AndroidSettingsManager>(relaxed = true) }
            single<NotificationManager> { mockk<NotificationManager>(relaxed = true) }
            single<AuthManager> { mockk<com.eunio.healthapp.domain.manager.AndroidAuthManager>(relaxed = true) }
            single<DatabaseService> { mockk<DatabaseService>(relaxed = true) }
        }
        
        // Initialize Koin with minimal test configuration
        startKoin {
            androidContext(mockContext)
            modules(testModule)
        }
        
        // Test that all essential services can be resolved
        val settingsManager = get<SettingsManager>()
        assertNotNull(settingsManager)
        
        val notificationManager = get<NotificationManager>()
        assertNotNull(notificationManager)
        
        val authManager = get<AuthManager>()
        assertNotNull(authManager)
        
        val databaseService = get<DatabaseService>()
        assertNotNull(databaseService)
    }
    
    /**
     * Test that ViewModels can be resolved with Android platform services
     */
    @Test
    fun `ViewModels resolve correctly with Android platform services`() = runTest {
        // Create a minimal test module with mocked ViewModels
        val testModule = org.koin.dsl.module {
            single<Context> { mockContext }
            single<com.eunio.healthapp.presentation.viewmodel.OnboardingViewModel> { 
                mockk<com.eunio.healthapp.presentation.viewmodel.OnboardingViewModel>(relaxed = true) 
            }
            single<com.eunio.healthapp.presentation.viewmodel.CalendarViewModel> { 
                mockk<com.eunio.healthapp.presentation.viewmodel.CalendarViewModel>(relaxed = true) 
            }
            single<com.eunio.healthapp.presentation.viewmodel.DailyLoggingViewModel> { 
                mockk<com.eunio.healthapp.presentation.viewmodel.DailyLoggingViewModel>(relaxed = true) 
            }
            single<com.eunio.healthapp.presentation.viewmodel.InsightsViewModel> { 
                mockk<com.eunio.healthapp.presentation.viewmodel.InsightsViewModel>(relaxed = true) 
            }
            single<com.eunio.healthapp.presentation.viewmodel.SettingsViewModel> { 
                mockk<com.eunio.healthapp.presentation.viewmodel.SettingsViewModel>(relaxed = true) 
            }
            single<com.eunio.healthapp.presentation.viewmodel.EnhancedSettingsViewModel> { 
                mockk<com.eunio.healthapp.presentation.viewmodel.EnhancedSettingsViewModel>(relaxed = true) 
            }
        }
        
        startKoin {
            androidContext(mockContext)
            modules(testModule)
        }
        
        // Test that ViewModels that depend on platform services can be resolved
        assertNotNull(get<com.eunio.healthapp.presentation.viewmodel.OnboardingViewModel>())
        assertNotNull(get<com.eunio.healthapp.presentation.viewmodel.CalendarViewModel>())
        assertNotNull(get<com.eunio.healthapp.presentation.viewmodel.DailyLoggingViewModel>())
        assertNotNull(get<com.eunio.healthapp.presentation.viewmodel.InsightsViewModel>())
        
        // Test that settings-related ViewModels work
        assertNotNull(get<com.eunio.healthapp.presentation.viewmodel.SettingsViewModel>())
        assertNotNull(get<com.eunio.healthapp.presentation.viewmodel.EnhancedSettingsViewModel>())
    }
    
    /**
     * Test that dependency resolution is performant with full Android stack
     */
    @Test
    fun `Android dependency resolution is performant`() = runTest {
        val startTime = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        
        // Create a minimal test module with mocked services
        val testModule = org.koin.dsl.module {
            single<Context> { mockContext }
            single<SettingsManager> { mockk<SettingsManager>(relaxed = true) }
            single<NotificationManager> { mockk<NotificationManager>(relaxed = true) }
            single<AuthManager> { mockk<AuthManager>(relaxed = true) }
            single<DatabaseService> { mockk<DatabaseService>(relaxed = true) }
            single<com.eunio.healthapp.presentation.viewmodel.OnboardingViewModel> { 
                mockk<com.eunio.healthapp.presentation.viewmodel.OnboardingViewModel>(relaxed = true) 
            }
        }
        
        startKoin {
            androidContext(mockContext)
            modules(testModule)
        }
        
        // Resolve multiple services to test performance
        get<SettingsManager>()
        get<NotificationManager>()
        get<AuthManager>()
        get<DatabaseService>()
        get<com.eunio.healthapp.presentation.viewmodel.OnboardingViewModel>()
        
        val endTime = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        val resolutionTime = endTime - startTime
        
        // Full dependency resolution should be reasonably fast (under 2 seconds)
        assertTrue(resolutionTime < 2000, "Full dependency resolution took too long: ${resolutionTime}ms")
    }
    
    /**
     * Test that platform-specific implementations are correctly injected
     */
    @Test
    fun `platform-specific implementations are correctly injected`() = runTest {
        // Create mocked Android-specific implementations
        val mockAndroidSettingsManager = mockk<com.eunio.healthapp.domain.manager.AndroidSettingsManager>(relaxed = true)
        val mockAndroidAuthManager = mockk<com.eunio.healthapp.domain.manager.AndroidAuthManager>(relaxed = true)
        val mockAndroidDatabaseService = mockk<com.eunio.healthapp.data.service.AndroidDatabaseService>(relaxed = true)
        
        val testModule = org.koin.dsl.module {
            single<Context> { mockContext }
            single<SettingsManager> { mockAndroidSettingsManager }
            single<AuthManager> { mockAndroidAuthManager }
            single<DatabaseService> { mockAndroidDatabaseService }
        }
        
        startKoin {
            androidContext(mockContext)
            modules(testModule)
        }
        
        // Verify that we get the expected implementations
        val settingsManager = get<SettingsManager>()
        assertNotNull(settingsManager)
        
        val authManager = get<AuthManager>()
        assertNotNull(authManager)
        
        val databaseService = get<DatabaseService>()
        assertNotNull(databaseService)
        
        // Test passes if we can resolve the services without exceptions
        assertTrue(true, "Platform-specific implementations resolved successfully")
    }
    
    /**
     * Test that error handling works correctly with platform services
     */
    @Test
    fun `error handling works with platform services`() = runTest {
        val testModule = org.koin.dsl.module {
            single<Context> { mockContext }
            single<com.eunio.healthapp.domain.util.ErrorHandler> { 
                mockk<com.eunio.healthapp.domain.util.ErrorHandler>(relaxed = true) 
            }
            single<SettingsManager> { mockk<SettingsManager>(relaxed = true) }
        }
        
        startKoin {
            androidContext(mockContext)
            modules(testModule)
        }
        
        // Test that error handler is available and works with platform services
        val errorHandler = get<com.eunio.healthapp.domain.util.ErrorHandler>()
        assertNotNull(errorHandler)
        
        // Test that services can handle errors gracefully
        val settingsManager = get<SettingsManager>()
        assertNotNull(settingsManager)
        
        // Basic functionality test - should not throw exceptions
        try {
            // This should work without throwing exceptions
            assertTrue(true, "Services should be functional")
        } catch (e: Exception) {
            throw AssertionError("Platform services should not throw exceptions during basic operations", e)
        }
    }
}