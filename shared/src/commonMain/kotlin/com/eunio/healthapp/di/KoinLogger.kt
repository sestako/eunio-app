package com.eunio.healthapp.di

import kotlinx.datetime.Clock

/**
 * Logging utility for Koin initialization with detailed error reporting
 * and debug console output.
 */
object KoinLogger {
    
    private var isDebugEnabled = true
    
    /**
     * Enable or disable debug logging
     */
    fun setDebugEnabled(enabled: Boolean) {
        isDebugEnabled = enabled
    }
    
    /**
     * Log successful Koin initialization
     */
    fun logSuccess(platform: String, modulesCount: Int) {
        if (isDebugEnabled) {
            println("✅ Koin initialized successfully for $platform")
            println("   📦 Loaded $modulesCount modules")
            println("   🕐 Timestamp: ${getCurrentTimestamp()}")
        }
    }
    
    /**
     * Log Koin initialization failure with detailed error information
     */
    fun logFailure(
        platform: String,
        error: KoinInitializationError,
        fallbackStrategy: FallbackStrategy,
        attempt: Int = 1
    ) {
        println("❌ Koin initialization failed for $platform (Attempt $attempt)")
        println("   🔍 Error Type: ${error::class.simpleName}")
        println("   💬 Message: ${error.message}")
        println("   🔄 Fallback Strategy: $fallbackStrategy")
        println("   🕐 Timestamp: ${getCurrentTimestamp()}")
        
        error.cause?.let { cause ->
            println("   🐛 Root Cause: ${cause::class.simpleName}: ${cause.message}")
            if (isDebugEnabled) {
                println("   📋 Stack Trace:")
                cause.printStackTrace()
            }
        }
    }
    
    /**
     * Log dependency resolution failure
     */
    fun logDependencyResolutionFailure(
        dependencyName: String,
        error: Throwable,
        fallbackUsed: String? = null
    ) {
        println("⚠️ Dependency resolution failed: $dependencyName")
        println("   💬 Error: ${error.message}")
        fallbackUsed?.let {
            println("   🔄 Using fallback: $it")
        }
        if (isDebugEnabled) {
            error.printStackTrace()
        }
    }
    
    /**
     * Log module loading failure
     */
    fun logModuleLoadFailure(moduleName: String, error: Throwable) {
        println("📦❌ Module loading failed: $moduleName")
        println("   💬 Error: ${error.message}")
        if (isDebugEnabled) {
            error.printStackTrace()
        }
    }
    
    /**
     * Log fallback strategy activation
     */
    fun logFallbackActivation(strategy: FallbackStrategy, reason: String) {
        println("🔄 Activating fallback strategy: $strategy")
        println("   📝 Reason: $reason")
        println("   🕐 Timestamp: ${getCurrentTimestamp()}")
    }
    
    /**
     * Log retry attempt
     */
    fun logRetryAttempt(attempt: Int, maxAttempts: Int, delayMs: Long) {
        println("🔄 Retrying Koin initialization (Attempt $attempt/$maxAttempts)")
        println("   ⏱️ Delay: ${delayMs}ms")
    }
    
    /**
     * Log service fallback usage
     */
    fun logServiceFallback(serviceName: String, fallbackType: String) {
        println("🔄 Using fallback service: $serviceName")
        println("   📝 Fallback Type: $fallbackType")
    }
    
    /**
     * Log warning messages
     */
    fun logWarning(message: String) {
        println("⚠️ Warning: $message")
    }
    
    /**
     * Log info messages
     */
    fun logInfo(message: String) {
        if (isDebugEnabled) {
            println("ℹ️ Info: $message")
        }
    }
    
    /**
     * Get current timestamp for logging
     */
    private fun getCurrentTimestamp(): String {
        // Simple timestamp - in a real implementation, you might use kotlinx-datetime
        return kotlinx.datetime.Clock.System.now().toEpochMilliseconds().toString()
    }
    
    /**
     * Log detailed initialization summary
     */
    fun logInitializationSummary(
        platform: String,
        result: KoinInitializationResult,
        totalAttempts: Int,
        totalTimeMs: Long
    ) {
        println("📊 Koin Initialization Summary for $platform")
        println("   🎯 Result: ${if (result is KoinInitializationResult.Success) "SUCCESS" else "FAILURE"}")
        println("   🔄 Total Attempts: $totalAttempts")
        println("   ⏱️ Total Time: ${totalTimeMs}ms")
        
        if (result is KoinInitializationResult.Failure) {
            println("   🔄 Fallback Strategy: ${result.fallbackStrategy}")
            println("   🔄 Can Retry: ${result.canRetry}")
        }
    }
}