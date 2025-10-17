package com.eunio.healthapp.domain.error

/**
 * Sealed class hierarchy representing all possible application errors.
 * Used throughout the app for consistent error handling and user feedback.
 */
sealed class AppError : Exception() {
    
    /**
     * Network-related errors including connectivity issues and API failures
     */
    data class NetworkError(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError()
    
    /**
     * Authentication and authorization errors
     */
    data class AuthenticationError(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError()
    
    /**
     * Data validation errors for user input
     */
    data class ValidationError(
        override val message: String,
        val field: String? = null,
        override val cause: Throwable? = null
    ) : AppError()
    
    /**
     * Data synchronization errors between local and remote storage
     */
    data class DataSyncError(
        override val message: String,
        val operation: String? = null,
        override val cause: Throwable? = null
    ) : AppError()
    
    /**
     * Database operation errors
     */
    data class DatabaseError(
        override val message: String,
        val operation: String? = null,
        override val cause: Throwable? = null
    ) : AppError()
    
    /**
     * Permission and access control errors
     */
    data class PermissionError(
        override val message: String,
        val requiredPermission: String? = null,
        override val cause: Throwable? = null
    ) : AppError()
    
    /**
     * Security-related errors including encryption and access violations
     */
    data class SecurityError(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError()
    
    /**
     * Unknown or unexpected errors
     */
    data class UnknownError(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError()
}