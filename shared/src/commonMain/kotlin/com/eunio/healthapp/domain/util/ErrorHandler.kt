package com.eunio.healthapp.domain.util

import com.eunio.healthapp.domain.error.AppError

/**
 * Utility class for converting system exceptions to domain-specific AppError instances.
 * Provides centralized error handling and mapping logic.
 */
class ErrorHandler {
    
    /**
     * Converts a generic Throwable to an appropriate AppError instance
     * based on the exception type and message.
     */
    fun handleError(error: Throwable): AppError {
        return when {
            // Network-related errors
            error.message?.contains("network", ignoreCase = true) == true ||
            error.message?.contains("connection", ignoreCase = true) == true ||
            error.message?.contains("timeout", ignoreCase = true) == true -> {
                AppError.NetworkError(
                    message = error.message ?: "Network connection failed",
                    cause = error
                )
            }
            
            // Authentication errors
            error.message?.contains("auth", ignoreCase = true) == true ||
            error.message?.contains("unauthorized", ignoreCase = true) == true ||
            error.message?.contains("permission denied", ignoreCase = true) == true -> {
                AppError.AuthenticationError(
                    message = error.message ?: "Authentication failed",
                    cause = error
                )
            }
            
            // Validation errors
            error is IllegalArgumentException -> {
                AppError.ValidationError(
                    message = error.message ?: "Invalid input provided",
                    cause = error
                )
            }
            
            // Database errors
            error.message?.contains("database", ignoreCase = true) == true ||
            error.message?.contains("sql", ignoreCase = true) == true -> {
                AppError.DatabaseError(
                    message = error.message ?: "Database operation failed",
                    cause = error
                )
            }
            
            // Sync errors
            error.message?.contains("sync", ignoreCase = true) == true ||
            error.message?.contains("conflict", ignoreCase = true) == true -> {
                AppError.DataSyncError(
                    message = error.message ?: "Data synchronization failed",
                    cause = error
                )
            }
            
            // Permission errors
            error.message?.contains("permission", ignoreCase = true) == true ||
            error.message?.contains("access denied", ignoreCase = true) == true -> {
                AppError.PermissionError(
                    message = error.message ?: "Permission denied",
                    cause = error
                )
            }
            
            // Default to unknown error
            else -> {
                AppError.UnknownError(
                    message = error.message ?: "An unexpected error occurred",
                    cause = error
                )
            }
        }
    }
    
    /**
     * Creates a validation error for specific field validation failures
     */
    fun createValidationError(message: String, field: String): AppError.ValidationError {
        return AppError.ValidationError(
            message = message,
            field = field
        )
    }
    
    /**
     * Creates a network error with optional operation context
     */
    fun createNetworkError(message: String, operation: String? = null): AppError.NetworkError {
        return AppError.NetworkError(
            message = if (operation != null) "$operation failed: $message" else message
        )
    }
    
    /**
     * Creates a database error with operation context
     */
    fun createDatabaseError(message: String, operation: String): AppError.DatabaseError {
        return AppError.DatabaseError(
            message = message,
            operation = operation
        )
    }
}