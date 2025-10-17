package com.eunio.healthapp.di

import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlinx.coroutines.runBlocking

/**
 * Koin initialization for iOS platform with comprehensive error handling
 */
object IOSKoinInitializer {
    
    // Shared instance property for Swift interop
    val shared = this
    
    private var lastInitializationResult: KoinInitializationResult? = null
    
    /**
     * Initialize Koin with comprehensive error handling and fallback mechanisms
     * This method matches the Swift call signature
     */
    fun doInitKoin(): KoinInitializationResult {
        return runBlocking {
            // Configure safe initialization
            SafeKoinInitializer.configure(
                KoinInitializationConfig(
                    enableDetailedLogging = true,
                    fallbackStrategy = FallbackStrategy.GRACEFUL_DEGRADATION,
                    maxRetryAttempts = 3,
                    retryDelayMs = 1000,
                    enableFallbackServices = true
                )
            )
            
            // Attempt safe initialization
            val result = SafeKoinInitializer.initKoinSafely(
                platform = "iOS",
                modules = listOf(
                    sharedModule,
                    iosModule,
                    iosAuthModule,
                    repositoryModule,
                    useCaseModule,
                    viewModelModule,
                    unitSystemModule,
                    settingsIntegrationModule,
                    networkModule  // Network monitoring
                ),
                platformSpecificSetup = null // iOS doesn't need context setup
            )
            
            lastInitializationResult = result
            result
        }
    }
    
    /**
     * Legacy initialization method for backward compatibility
     * @deprecated Use doInitKoin() which returns KoinInitializationResult
     */
    @Deprecated("Use doInitKoin() which returns KoinInitializationResult")
    fun initKoin() {
        try {
            startKoin {
                modules(
                    sharedModule,
                    iosModule,
                    iosAuthModule,
                    repositoryModule,
                    useCaseModule,
                    viewModelModule,
                    unitSystemModule,
                    settingsIntegrationModule
                )
            }
            KoinLogger.logSuccess("iOS (Legacy)", 7)
        } catch (e: Exception) {
            KoinLogger.logFailure(
                "iOS (Legacy)",
                KoinInitializationError.UnknownInitializationError(
                    "Legacy initialization failed: ${e.message}",
                    e
                ),
                FallbackStrategy.FAIL_FAST
            )
        }
    }
    
    /**
     * Stop Koin safely without throwing exceptions
     */
    fun stopKoin() {
        try {
            org.koin.core.context.stopKoin()
            KoinLogger.logInfo("Koin stopped successfully for iOS")
        } catch (e: Exception) {
            KoinLogger.logWarning("Error stopping Koin for iOS: ${e.message}")
        }
    }
    
    /**
     * Check if Koin is properly initialized
     */
    fun isInitialized(): Boolean {
        return SafeKoinInitializer.isKoinInitialized()
    }
    
    /**
     * Get the last initialization result
     */
    fun getLastInitializationResult(): KoinInitializationResult? {
        return lastInitializationResult
    }
    
    /**
     * Safe dependency resolution with iOS-specific fallbacks
     */
    inline fun <reified T : Any> safeGet(): T? {
        return SafeKoinInitializer.safeGet<T>() ?: run {
            // iOS-specific fallback logic could go here
            createIOSFallback<T>()
        }
    }
    
    /**
     * Create iOS-specific fallback implementations
     */
    inline fun <reified T : Any> createIOSFallback(): T? {
        return when (T::class) {
            com.eunio.healthapp.domain.manager.SettingsManager::class -> {
                @Suppress("UNCHECKED_CAST")
                FallbackServiceFactory.createFallbackSettingsManager() as? T
            }
            com.eunio.healthapp.domain.manager.NotificationManager::class -> {
                @Suppress("UNCHECKED_CAST")
                FallbackServiceFactory.createFallbackNotificationManager() as? T
            }
            com.eunio.healthapp.domain.manager.AuthManager::class -> {
                @Suppress("UNCHECKED_CAST")
                FallbackServiceFactory.createFallbackAuthManager() as? T
            }
            else -> {
                KoinLogger.logWarning("No iOS fallback available for ${T::class.simpleName}")
                null
            }
        }
    }
}