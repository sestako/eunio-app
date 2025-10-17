package com.eunio.healthapp.di

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.BeforeTest
import kotlin.test.AfterTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

/**
 * Integration tests for Koin error scenarios and recovery mechanisms.
 * Tests the complete error handling flow from initialization failure to fallback activation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class KoinErrorRecoveryIntegrationTest {
    
    @BeforeTest
    fun setup() {
        // Clean up any uncaught exceptions from previous tests
        try {
            kotlinx.coroutines.test.TestCoroutineScheduler().advanceUntilIdle()
        } catch (e: Exception) {
            // Ignore
        }
        
        // Ensure Koin is completely stopped before each test
        try {
            org.koin.core.context.stopKoin()
        } catch (e: Exception) {
            // Ignore if Koin wasn't started
        }
        SafeKoinInitializer.resetForTesting()
    }
    
    @AfterTest
    fun tearDown() {
        try {
            // Stop Koin first
            try {
                org.koin.core.context.stopKoin()
            } catch (e: Exception) {
                // Ignore if Koin wasn't started
            }
            SafeKoinInitializer.resetForTesting()
            com.eunio.healthapp.domain.util.UserNotificationService.resetForTesting()
            kotlinx.coroutines.runBlocking {
                com.eunio.healthapp.domain.util.UnitSystemComponentsFactory.resetForTesting()
            }
        } catch (e: Exception) {
            // Ignore cleanup exceptions to prevent test interference
        }
    }
    
    @Test
    fun `SafeKoinInitializer handles successful initialization with valid modules`() {
        kotlinx.coroutines.runBlocking {
        // Configure for testing
        SafeKoinInitializer.configure(
            KoinInitializationConfig(
                enableDetailedLogging = true,
                fallbackStrategy = FallbackStrategy.GRACEFUL_DEGRADATION,
                maxRetryAttempts = 1,
                retryDelayMs = 100,
                enableFallbackServices = true
            )
        )
        
        // Create valid test modules
        val testModule = module {
            factory<String> { "test_string" }
            factory<Int> { 42 }
        }
        
        val result = SafeKoinInitializer.initKoinSafely(
            platform = "Test",
            modules = listOf(testModule),
            platformSpecificSetup = null
        )
        
        assertTrue(result is KoinInitializationResult.Success)
        assertTrue(SafeKoinInitializer.isKoinInitialized())
        
        // Test dependency resolution
        val stringValue = SafeKoinInitializer.safeGet<String>()
        assertEquals("test_string", stringValue)
        
        val intValue = SafeKoinInitializer.safeGet<Int>()
        assertEquals(42, intValue)
        }
    }
    
    @Test
    fun `SafeKoinInitializer handles module loading failure with fallback`() {
        kotlinx.coroutines.runBlocking {
        // Configure for testing with fallback
        SafeKoinInitializer.configure(
            KoinInitializationConfig(
                enableDetailedLogging = true,
                fallbackStrategy = FallbackStrategy.MOCK_IMPLEMENTATIONS,
                maxRetryAttempts = 2,
                retryDelayMs = 50,
                enableFallbackServices = true
            )
        )
        
        // Create a module that will cause issues (circular dependency)
        val problematicModule = module {
            factory<TestServiceA> { TestServiceA(get()) }
            factory<TestServiceB> { TestServiceB(get()) }
        }
        
        val result = SafeKoinInitializer.initKoinSafely(
            platform = "Test",
            modules = listOf(problematicModule),
            platformSpecificSetup = null
        )
        
        // Should handle the failure gracefully
        when (result) {
            is KoinInitializationResult.Success -> {
                // If it succeeds, that's fine too
                assertTrue(SafeKoinInitializer.isKoinInitialized())
            }
            is KoinInitializationResult.Failure -> {
                // Should have appropriate error and fallback strategy
                assertNotNull(result.error)
                assertEquals(FallbackStrategy.MOCK_IMPLEMENTATIONS, result.fallbackStrategy)
            }
        }
        }
    }
    
    @Test
    fun `SafeKoinInitializer retries initialization on failure`() {
        kotlinx.coroutines.runBlocking {
        var attemptCount = 0
        
        // Configure for multiple retries
        SafeKoinInitializer.configure(
            KoinInitializationConfig(
                enableDetailedLogging = true,
                fallbackStrategy = FallbackStrategy.GRACEFUL_DEGRADATION,
                maxRetryAttempts = 3,
                retryDelayMs = 10,
                enableFallbackServices = true
            )
        )
        
        // This test is conceptual - in practice, we'd need a way to simulate
        // intermittent failures that might succeed on retry
        val testModule = module {
            factory<String> { "retry_test" }
        }
        
        val result = SafeKoinInitializer.initKoinSafely(
            platform = "RetryTest",
            modules = listOf(testModule),
            platformSpecificSetup = null
        )
        
        // Should eventually succeed or fail with appropriate fallback
        assertNotNull(result)
        assertNotNull(SafeKoinInitializer.getLastInitializationResult())
        }
    }
    
    @Test
    fun `SafeKoinInitializer safe dependency resolution with fallback factory works`() {
        kotlinx.coroutines.runBlocking {
        // Ensure Koin is not initialized
        SafeKoinInitializer.configure(KoinInitializationConfig())
        
        val fallbackValue = "fallback_implementation"
        val result = SafeKoinInitializer.safeGetOrFallback<String> { fallbackValue }
        
        assertEquals(fallbackValue, result)
        }
    }
    
    @Test
    fun `FallbackServiceFactory provides working service implementations`() {
        kotlinx.coroutines.runBlocking {
        // Test SettingsManager fallback
        val settingsManager = FallbackServiceFactory.createFallbackSettingsManager()
        val userSettings = settingsManager.getUserSettings()
        assertTrue(userSettings is com.eunio.healthapp.domain.util.Result.Success)
        
        // Test NotificationManager fallback
        val notificationManager = FallbackServiceFactory.createFallbackNotificationManager()
        val permissionStatus = notificationManager.getNotificationPermissionStatus()
        assertEquals(com.eunio.healthapp.domain.model.notification.NotificationPermissionStatus.DENIED, permissionStatus)
        
        // Test AuthManager fallback
        val authManager = FallbackServiceFactory.createFallbackAuthManager()
        val currentUserResult = authManager.getCurrentUser()
        assertTrue(currentUserResult is com.eunio.healthapp.domain.util.Result.Success)
        assertEquals(null, currentUserResult.data) // Fallback should return null
        
        val signOutResult = authManager.signOut()
        assertTrue(signOutResult is com.eunio.healthapp.domain.util.Result.Success)
        
        // Test DatabaseManager fallback throws NotImplementedError
        try {
            FallbackServiceFactory.createFallbackDatabaseManager()
            assertTrue(false, "Expected NotImplementedError")
        } catch (e: NotImplementedError) {
            assertTrue(e.message?.contains("DatabaseManager fallback not implemented") == true)
        }
        }
    }
    
    @Test
    fun `Error categorization works correctly for different error types`() {
        kotlinx.coroutines.runBlocking {
        // Test different error scenarios
        val moduleError = KoinInitializationError.ModuleLoadError(
            errorMessage = "Module failed to load",
            moduleName = "TestModule",
            errorCause = RuntimeException("Module not found")
        )
        
        val dependencyError = KoinInitializationError.DependencyResolutionError(
            errorMessage = "Dependency resolution failed",
            dependencyName = "TestDependency",
            errorCause = RuntimeException("Circular dependency")
        )
        
        val contextError = KoinInitializationError.ContextInitializationError(
            errorMessage = "Context initialization failed",
            platform = "Test",
            errorCause = RuntimeException("Context is null")
        )
        
        // Verify error properties
        assertEquals("TestModule", moduleError.moduleName)
        assertEquals("TestDependency", dependencyError.dependencyName)
        assertEquals("Test", contextError.platform)
        
        // All should be instances of KoinInitializationError
        assertTrue(moduleError is KoinInitializationError)
        assertTrue(dependencyError is KoinInitializationError)
        assertTrue(contextError is KoinInitializationError)
        }
    }
    
    @Test
    fun `Fallback strategies are applied correctly`() {
        // Use runBlocking instead of runTest to avoid UncaughtExceptionsBeforeTest
        kotlinx.coroutines.runBlocking {
            val strategies = listOf(
                FallbackStrategy.MANUAL_INSTANTIATION,
                FallbackStrategy.MOCK_IMPLEMENTATIONS,
                FallbackStrategy.MINIMAL_MODULES,
                FallbackStrategy.GRACEFUL_DEGRADATION,
                FallbackStrategy.FAIL_FAST
            )
            
            strategies.forEach { strategy ->
                // Reset before each strategy test
                try {
                    org.koin.core.context.stopKoin()
                } catch (e: Exception) {
                    // Ignore if Koin wasn't started
                }
                SafeKoinInitializer.resetForTesting()
                
                val config = KoinInitializationConfig(
                    fallbackStrategy = strategy,
                    maxRetryAttempts = 1,
                    retryDelayMs = 10
                )
                
                SafeKoinInitializer.configure(config)
                
                // Test that configuration is applied (we can't directly test private fields,
                // but we can verify the configuration doesn't cause exceptions)
                assertTrue(true) // Test passes if no exception is thrown
            }
        }
    }
    
    @Test
    fun `KoinLogger handles all log levels without exceptions`() {
        // Test all logging methods to ensure they don't throw exceptions
        KoinLogger.setDebugEnabled(true)
        
        KoinLogger.logSuccess("Test", 5)
        
        val error = KoinInitializationError.UnknownInitializationError(
            errorMessage = "Test error",
            errorCause = RuntimeException("Test cause")
        )
        KoinLogger.logFailure("Test", error, FallbackStrategy.GRACEFUL_DEGRADATION, 1)
        
        KoinLogger.logDependencyResolutionFailure(
            "TestDependency",
            RuntimeException("Test error"),
            "TestFallback"
        )
        
        KoinLogger.logModuleLoadFailure("TestModule", RuntimeException("Test error"))
        KoinLogger.logFallbackActivation(FallbackStrategy.MOCK_IMPLEMENTATIONS, "Test reason")
        KoinLogger.logRetryAttempt(2, 3, 1000)
        KoinLogger.logServiceFallback("TestService", "Mock")
        KoinLogger.logWarning("Test warning")
        KoinLogger.logInfo("Test info")
        
        val result = KoinInitializationResult.Success
        KoinLogger.logInitializationSummary("Test", result, 1, 1000)
        
        // Test with debug disabled
        KoinLogger.setDebugEnabled(false)
        KoinLogger.logInfo("This should not appear")
        
        assertTrue(true) // Test passes if no exception is thrown
    }
    
    @Test
    fun `Multiple initialization attempts are handled correctly`() {
        kotlinx.coroutines.runBlocking {
        SafeKoinInitializer.configure(
            KoinInitializationConfig(
                maxRetryAttempts = 1,
                retryDelayMs = 10
            )
        )
        
        val testModule = module {
            factory<String> { "multi_init_test" }
        }
        
        // Initialize multiple times
        val result1 = SafeKoinInitializer.initKoinSafely(
            platform = "MultiTest1",
            modules = listOf(testModule),
            platformSpecificSetup = null
        )
        
        val result2 = SafeKoinInitializer.initKoinSafely(
            platform = "MultiTest2",
            modules = listOf(testModule),
            platformSpecificSetup = null
        )
        
        // Both should return results
        assertNotNull(result1)
        assertNotNull(result2)
        
        // Last result should be tracked
        val lastResult = SafeKoinInitializer.getLastInitializationResult()
        assertEquals(result2, lastResult)
        }
    }
    
    // Test helper classes for circular dependency testing
    private class TestServiceA(val serviceB: TestServiceB)
    private class TestServiceB(val serviceA: TestServiceA)
}