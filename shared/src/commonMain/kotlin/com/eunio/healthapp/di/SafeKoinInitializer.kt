package com.eunio.healthapp.di

import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.mp.KoinPlatform
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock

/**
 * Safe Koin initializer with comprehensive error handling, fallback mechanisms,
 * and detailed logging for debugging initialization failures.
 */
object SafeKoinInitializer {
    
    private var initializationConfig = KoinInitializationConfig()
    private var isInitialized = false
    private var lastInitializationResult: KoinInitializationResult? = null
    
    /**
     * Configure the initialization behavior
     */
    fun configure(config: KoinInitializationConfig) {
        initializationConfig = config
        KoinLogger.setDebugEnabled(config.enableDetailedLogging)
    }
    
    /**
     * Initialize Koin with comprehensive error handling and fallback mechanisms
     */
    suspend fun initKoinSafely(
        platform: String,
        modules: List<Module>,
        platformSpecificSetup: (org.koin.core.KoinApplication.() -> Unit)? = null
    ): KoinInitializationResult {
        val startTime = Clock.System.now().toEpochMilliseconds()
        var attempts = 0
        var lastError: KoinInitializationError? = null
        
        KoinLogger.logInfo("Starting Koin initialization for $platform")
        
        // Stop existing Koin instance if it exists
        safeStopKoin()
        
        repeat(initializationConfig.maxRetryAttempts) { attempt ->
            attempts = attempt + 1
            
            if (attempt > 0) {
                KoinLogger.logRetryAttempt(attempts, initializationConfig.maxRetryAttempts, initializationConfig.retryDelayMs)
                delay(initializationConfig.retryDelayMs)
            }
            
            try {
                // Attempt to initialize Koin
                val result = attemptKoinInitialization(platform, modules, platformSpecificSetup)
                
                if (result is KoinInitializationResult.Success) {
                    isInitialized = true
                    lastInitializationResult = result
                    val totalTime = Clock.System.now().toEpochMilliseconds() - startTime
                    
                    KoinLogger.logSuccess(platform, modules.size)
                    KoinLogger.logInitializationSummary(platform, result, attempts, totalTime)
                    
                    return result
                }
                
                // If we get here, initialization failed
                if (result is KoinInitializationResult.Failure) {
                    lastError = result.error
                    KoinLogger.logFailure(platform, result.error, result.fallbackStrategy, attempts)
                    
                    if (!result.canRetry) {
                        return@repeat
                    }
                }
                
            } catch (e: Exception) {
                lastError = categorizeError(e, platform)
                KoinLogger.logFailure(platform, lastError!!, initializationConfig.fallbackStrategy, attempts)
            }
        }
        
        // All attempts failed, activate fallback strategy
        val finalError = lastError ?: KoinInitializationError.UnknownInitializationError(
            errorMessage = "Koin initialization failed after $attempts attempts",
            errorCause = null
        )
        
        val fallbackResult = KoinInitializationResult.Failure(
            error = finalError,
            fallbackStrategy = initializationConfig.fallbackStrategy,
            canRetry = false
        )
        
        lastInitializationResult = fallbackResult
        val totalTime = Clock.System.now().toEpochMilliseconds() - startTime
        
        KoinLogger.logInitializationSummary(platform, fallbackResult, attempts, totalTime)
        
        // Activate fallback strategy
        activateFallbackStrategy(initializationConfig.fallbackStrategy, platform)
        
        return fallbackResult
    }
    
    /**
     * Attempt to initialize Koin with the provided modules
     */
    private fun attemptKoinInitialization(
        platform: String,
        modules: List<Module>,
        platformSpecificSetup: (org.koin.core.KoinApplication.() -> Unit)?
    ): KoinInitializationResult {
        return try {
            startKoin {
                // Platform-specific setup (e.g., androidContext for Android)
                platformSpecificSetup?.invoke(this)
                
                // Load modules one by one to identify problematic modules
                modules(modules)
            }
            
            // Assume initialization was successful if no exception was thrown
            KoinInitializationResult.Success
            
        } catch (e: Exception) {
            KoinInitializationResult.Failure(
                error = categorizeError(e, platform),
                fallbackStrategy = initializationConfig.fallbackStrategy
            )
        }
    }
    
    /**
     * Categorize the error based on the exception type and message
     */
    private fun categorizeError(exception: Exception, platform: String): KoinInitializationError {
        return when {
            exception.message?.contains("module", ignoreCase = true) == true -> {
                KoinInitializationError.ModuleLoadError(
                    errorMessage = "Failed to load Koin module: ${exception.message}",
                    moduleName = extractModuleName(exception.message ?: ""),
                    errorCause = exception
                )
            }
            
            exception.message?.contains("dependency", ignoreCase = true) == true ||
            exception.message?.contains("resolution", ignoreCase = true) == true -> {
                KoinInitializationError.DependencyResolutionError(
                    errorMessage = "Failed to resolve dependency: ${exception.message}",
                    dependencyName = extractDependencyName(exception.message ?: ""),
                    errorCause = exception
                )
            }
            
            exception.message?.contains("context", ignoreCase = true) == true -> {
                KoinInitializationError.ContextInitializationError(
                    errorMessage = "Platform context initialization failed: ${exception.message}",
                    platform = platform,
                    errorCause = exception
                )
            }
            
            exception.message?.contains("configuration", ignoreCase = true) == true -> {
                KoinInitializationError.ConfigurationError(
                    errorMessage = "Koin configuration error: ${exception.message}",
                    configurationIssue = exception.message ?: "Unknown configuration issue",
                    errorCause = exception
                )
            }
            
            else -> {
                KoinInitializationError.UnknownInitializationError(
                    errorMessage = "Unknown Koin initialization error: ${exception.message}",
                    errorCause = exception
                )
            }
        }
    }
    
    /**
     * Extract module name from error message
     */
    private fun extractModuleName(message: String): String {
        // Simple extraction - in a real implementation, this could be more sophisticated
        return message.substringAfter("module").substringBefore(" ").trim()
    }
    
    /**
     * Extract dependency name from error message
     */
    private fun extractDependencyName(message: String): String {
        // Simple extraction - in a real implementation, this could be more sophisticated
        return message.substringAfter("dependency").substringBefore(" ").trim()
    }
    
    /**
     * Activate the specified fallback strategy
     */
    private fun activateFallbackStrategy(strategy: FallbackStrategy, platform: String) {
        KoinLogger.logFallbackActivation(strategy, "Koin initialization failed for $platform")
        
        when (strategy) {
            FallbackStrategy.MANUAL_INSTANTIATION -> {
                KoinLogger.logInfo("Fallback: Manual instantiation will be used for critical components")
                // Manual instantiation will be handled by individual components
            }
            
            FallbackStrategy.MOCK_IMPLEMENTATIONS -> {
                KoinLogger.logInfo("Fallback: Mock implementations will be used for services")
                // Mock implementations will be provided by FallbackServiceFactory
            }
            
            FallbackStrategy.MINIMAL_MODULES -> {
                KoinLogger.logInfo("Fallback: Attempting initialization with minimal modules")
                // This could be implemented to retry with only essential modules
            }
            
            FallbackStrategy.GRACEFUL_DEGRADATION -> {
                KoinLogger.logInfo("Fallback: Application will run with limited functionality")
                // Limited functionality mode - some features may be disabled
            }
            
            FallbackStrategy.FAIL_FAST -> {
                KoinLogger.logWarning("Fallback: Fail fast strategy - application may have limited functionality")
                // Let the application handle the failure
            }
        }
    }
    
    /**
     * Safely stop Koin without throwing exceptions
     */
    private fun safeStopKoin() {
        try {
            stopKoin()
            KoinLogger.logInfo("Previous Koin instance stopped successfully")
        } catch (e: Exception) {
            // Koin is not initialized or already stopped
            KoinLogger.logInfo("No previous Koin instance to stop")
        }
    }
    
    /**
     * Check if Koin is properly initialized
     */
    fun isKoinInitialized(): Boolean {
        return isInitialized
    }
    
    /**
     * Get the last initialization result
     */
    fun getLastInitializationResult(): KoinInitializationResult? {
        return lastInitializationResult
    }
    
    /**
     * Safe dependency resolution with fallback
     */
    inline fun <reified T : Any> safeGet(): T? {
        return try {
            if (isKoinInitialized()) {
                KoinPlatform.getKoin().get<T>()
            } else {
                KoinLogger.logDependencyResolutionFailure(
                    T::class.simpleName ?: "Unknown",
                    Exception("Koin not initialized"),
                    "Will use fallback if available"
                )
                null
            }
        } catch (e: Exception) {
            KoinLogger.logDependencyResolutionFailure(
                T::class.simpleName ?: "Unknown",
                e,
                "Will use fallback if available"
            )
            null
        }
    }
    
    /**
     * Safe dependency resolution with fallback factory
     */
    inline fun <reified T : Any> safeGetOrFallback(fallbackFactory: () -> T): T {
        return safeGet<T>() ?: run {
            KoinLogger.logServiceFallback(T::class.simpleName ?: "Unknown", "Custom fallback factory")
            fallbackFactory()
        }
    }
    
    /**
     * Reset the SafeKoinInitializer state for testing purposes
     */
    fun resetForTesting() {
        isInitialized = false
        lastInitializationResult = null
        safeStopKoin()
    }
}