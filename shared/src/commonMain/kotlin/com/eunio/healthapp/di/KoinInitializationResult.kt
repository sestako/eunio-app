package com.eunio.healthapp.di

import com.eunio.healthapp.domain.error.AppError

/**
 * Represents the result of Koin initialization with detailed error information
 * and fallback strategies for graceful degradation.
 */
sealed class KoinInitializationResult {
    
    /**
     * Successful Koin initialization
     */
    object Success : KoinInitializationResult()
    
    /**
     * Failed Koin initialization with error details and fallback strategy
     */
    data class Failure(
        val error: KoinInitializationError,
        val fallbackStrategy: FallbackStrategy,
        val canRetry: Boolean = true
    ) : KoinInitializationResult()
}

/**
 * Specific errors that can occur during Koin initialization
 */
sealed class KoinInitializationError(
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause) {
    
    /**
     * Module loading errors - when specific modules fail to load
     */
    data class ModuleLoadError(
        val errorMessage: String,
        val moduleName: String,
        val errorCause: Throwable? = null
    ) : KoinInitializationError(errorMessage, errorCause)
    
    /**
     * Dependency resolution errors - when dependencies cannot be resolved
     */
    data class DependencyResolutionError(
        val errorMessage: String,
        val dependencyName: String,
        val errorCause: Throwable? = null
    ) : KoinInitializationError(errorMessage, errorCause)
    
    /**
     * Context initialization errors - platform-specific context issues
     */
    data class ContextInitializationError(
        val errorMessage: String,
        val platform: String,
        val errorCause: Throwable? = null
    ) : KoinInitializationError(errorMessage, errorCause)
    
    /**
     * Configuration errors - invalid Koin configuration
     */
    data class ConfigurationError(
        val errorMessage: String,
        val configurationIssue: String,
        val errorCause: Throwable? = null
    ) : KoinInitializationError(errorMessage, errorCause)
    
    /**
     * Unknown initialization errors
     */
    data class UnknownInitializationError(
        val errorMessage: String,
        val errorCause: Throwable? = null
    ) : KoinInitializationError(errorMessage, errorCause)
}

/**
 * Fallback strategies for when Koin initialization fails
 */
enum class FallbackStrategy {
    /**
     * Use manual instantiation for critical components
     */
    MANUAL_INSTANTIATION,
    
    /**
     * Use mock implementations for non-critical components
     */
    MOCK_IMPLEMENTATIONS,
    
    /**
     * Retry initialization with minimal modules
     */
    MINIMAL_MODULES,
    
    /**
     * Graceful degradation with limited functionality
     */
    GRACEFUL_DEGRADATION,
    
    /**
     * No fallback - fail fast
     */
    FAIL_FAST
}

/**
 * Configuration for Koin initialization with error handling options
 */
data class KoinInitializationConfig(
    val enableDetailedLogging: Boolean = true,
    val fallbackStrategy: FallbackStrategy = FallbackStrategy.GRACEFUL_DEGRADATION,
    val maxRetryAttempts: Int = 3,
    val retryDelayMs: Long = 1000,
    val enableFallbackServices: Boolean = true
)