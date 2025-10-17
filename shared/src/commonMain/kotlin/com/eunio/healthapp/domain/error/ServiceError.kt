package com.eunio.healthapp.domain.error

/**
 * Sealed class hierarchy for service-specific errors.
 * Extends AppError to integrate with the existing error handling system.
 */
sealed class ServiceError : AppError() {
    
    /**
     * Service initialization errors
     */
    data class InitializationError(
        override val message: String,
        val serviceName: String,
        override val cause: Throwable? = null
    ) : ServiceError()
    
    /**
     * Service operation timeout errors
     */
    data class TimeoutError(
        override val message: String,
        val serviceName: String,
        val operation: String,
        val timeoutMs: Long,
        override val cause: Throwable? = null
    ) : ServiceError()
    
    /**
     * Service dependency unavailable errors
     */
    data class DependencyUnavailableError(
        override val message: String,
        val serviceName: String,
        val dependencyName: String,
        override val cause: Throwable? = null
    ) : ServiceError()
    
    /**
     * Service configuration errors
     */
    data class ConfigurationError(
        override val message: String,
        val serviceName: String,
        val configurationIssue: String,
        override val cause: Throwable? = null
    ) : ServiceError()
    
    /**
     * Service operation errors
     */
    data class OperationError(
        override val message: String,
        val serviceName: String,
        val operation: String,
        override val cause: Throwable? = null
    ) : ServiceError()
    
    /**
     * Service degraded mode errors - when service is partially functional
     */
    data class DegradedModeError(
        override val message: String,
        val serviceName: String,
        val availableOperations: List<String>,
        val unavailableOperations: List<String>,
        override val cause: Throwable? = null
    ) : ServiceError()
}