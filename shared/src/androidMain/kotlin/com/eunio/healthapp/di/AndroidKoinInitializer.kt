package com.eunio.healthapp.di

import android.content.Context
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlinx.coroutines.runBlocking

/**
 * Koin initialization for Android platform with comprehensive error handling
 */
object AndroidKoinInitializer {
    
    private var lastInitializationResult: KoinInitializationResult? = null
    
    /**
     * Initialize Koin with comprehensive error handling and fallback mechanisms
     */
    fun initKoin(context: Context): KoinInitializationResult {
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
                platform = "Android",
                modules = listOf(
                    sharedModule,
                    androidModule,
                    androidAuthModule,
                    repositoryModule,
                    useCaseModule,
                    viewModelModule,
                    unitSystemModule,
                    settingsIntegrationModule,
                    networkModule  // Network monitoring
                ),
                platformSpecificSetup = {
                    androidContext(context)
                }
            )
            
            lastInitializationResult = result
            result
        }
    }
    
    /**
     * Legacy initialization method for backward compatibility
     * @deprecated Use initKoin(context) which returns KoinInitializationResult
     */
    @Deprecated("Use initKoin(context) which returns KoinInitializationResult")
    fun initKoinLegacy(context: Context) {
        try {
            startKoin {
                androidContext(context)
                modules(
                    sharedModule,
                    androidModule,
                    androidAuthModule,
                    repositoryModule,
                    useCaseModule,
                    viewModelModule,
                    unitSystemModule,
                    settingsIntegrationModule
                )
            }
            KoinLogger.logSuccess("Android (Legacy)", 7)
        } catch (e: Exception) {
            KoinLogger.logFailure(
                "Android (Legacy)",
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
            stopKoin()
            KoinLogger.logInfo("Koin stopped successfully for Android")
        } catch (e: Exception) {
            KoinLogger.logWarning("Error stopping Koin for Android: ${e.message}")
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
     * Safe dependency resolution with Android-specific fallbacks
     */
    inline fun <reified T : Any> safeGet(context: Context): T? {
        return SafeKoinInitializer.safeGet<T>() ?: run {
            // Android-specific fallback logic could go here
            createAndroidFallback<T>(context)
        }
    }
    
    /**
     * Create Android-specific fallback implementations
     */
    inline fun <reified T : Any> createAndroidFallback(context: Context): T? {
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
                KoinLogger.logWarning("No Android fallback available for ${T::class.simpleName}")
                null
            }
        }
    }
}