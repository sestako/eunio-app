package com.eunio.healthapp.domain.util

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.error.UnitSystemError
import com.eunio.healthapp.domain.model.UnitSystem

/**
 * Specialized error handler for unit system operations.
 * Provides specific error mapping and recovery mechanisms for unit system related failures.
 */
class UnitSystemErrorHandler {
    
    /**
     * Converts a generic Throwable to a unit system specific error
     */
    fun handleUnitSystemError(error: Throwable, context: String? = null): UnitSystemError {
        return when (error) {
            is NumberFormatException -> UnitSystemError.ConversionError(
                message = "Invalid number format for unit conversion: ${error.message}",
                cause = error
            )
            
            is IllegalArgumentException -> {
                if (error.message?.contains("unit", ignoreCase = true) == true) {
                    UnitSystemError.UnitValidationError(
                        message = "Invalid unit system parameter: ${error.message}",
                        cause = error
                    )
                } else {
                    UnitSystemError.ConversionError(
                        message = "Invalid conversion parameters: ${error.message}",
                        cause = error
                    )
                }
            }
            
            is IllegalStateException -> {
                when {
                    error.message?.contains("locale", ignoreCase = true) == true -> 
                        UnitSystemError.LocaleDetectionError(
                            message = "Failed to detect locale: ${error.message}",
                            cause = error
                        )
                    
                    error.message?.contains("preference", ignoreCase = true) == true -> 
                        UnitSystemError.PreferenceOperationError(
                            message = "Preference operation failed: ${error.message}",
                            operation = context ?: "unknown",
                            cause = error
                        )
                    
                    else -> UnitSystemError.PreferenceOperationError(
                        message = "Unit system operation failed: ${error.message}",
                        operation = context ?: "unknown",
                        cause = error
                    )
                }
            }
            
            else -> {
                // Check error message for specific patterns
                val message = error.message?.lowercase() ?: ""
                when {
                    message.contains("network") || message.contains("connection") || message.contains("timeout") -> 
                        UnitSystemError.UnitSystemNetworkError(
                            message = "Network error during unit system operation: ${error.message}",
                            operation = context,
                            cause = error
                        )
                    
                    message.contains("sync") || message.contains("synchroniz") -> 
                        UnitSystemError.PreferencesSyncError(
                            message = "Preferences sync failed: ${error.message}",
                            operation = context,
                            cause = error
                        )
                    
                    message.contains("conversion") || message.contains("convert") -> 
                        UnitSystemError.ConversionError(
                            message = "Unit conversion failed: ${error.message}",
                            cause = error
                        )
                    
                    message.contains("locale") -> 
                        UnitSystemError.LocaleDetectionError(
                            message = "Locale detection failed: ${error.message}",
                            cause = error
                        )
                    
                    else -> UnitSystemError.PreferenceOperationError(
                        message = error.message ?: "Unknown unit system error occurred",
                        operation = context ?: "unknown",
                        cause = error
                    )
                }
            }
        }
    }
    
    /**
     * Creates a conversion error with detailed context
     */
    fun createConversionError(
        message: String,
        inputValue: Double? = null,
        fromUnit: UnitSystem? = null,
        toUnit: UnitSystem? = null,
        cause: Throwable? = null
    ): UnitSystemError.ConversionError {
        return UnitSystemError.ConversionError(
            message = message,
            inputValue = inputValue,
            fromUnit = fromUnit?.name,
            toUnit = toUnit?.name,
            cause = cause
        )
    }
    
    /**
     * Creates a validation error for unit system inputs
     */
    fun createValidationError(
        message: String,
        field: String? = null,
        inputValue: Any? = null
    ): UnitSystemError.UnitValidationError {
        return UnitSystemError.UnitValidationError(
            message = message,
            field = field,
            inputValue = inputValue
        )
    }
    
    /**
     * Creates a preferences sync error with operation context
     */
    fun createSyncError(
        message: String,
        operation: String? = null,
        userId: String? = null,
        cause: Throwable? = null
    ): UnitSystemError.PreferencesSyncError {
        return UnitSystemError.PreferencesSyncError(
            message = message,
            operation = operation,
            userId = userId,
            cause = cause
        )
    }
    
    /**
     * Creates a locale detection error
     */
    fun createLocaleError(
        message: String,
        locale: String? = null,
        cause: Throwable? = null
    ): UnitSystemError.LocaleDetectionError {
        return UnitSystemError.LocaleDetectionError(
            message = message,
            locale = locale,
            cause = cause
        )
    }
    
    /**
     * Creates a network error specific to unit system operations
     */
    fun createNetworkError(
        message: String,
        operation: String? = null,
        retryAfterSeconds: Int? = null,
        cause: Throwable? = null
    ): UnitSystemError.UnitSystemNetworkError {
        return UnitSystemError.UnitSystemNetworkError(
            message = message,
            operation = operation,
            retryAfterSeconds = retryAfterSeconds,
            cause = cause
        )
    }
    
    /**
     * Determines if an error is retryable based on its type and context
     */
    fun isRetryable(error: UnitSystemError): Boolean {
        return when (error) {
            is UnitSystemError.UnitSystemNetworkError -> true
            is UnitSystemError.PreferencesSyncError -> true
            is UnitSystemError.PreferenceOperationError -> error.retryable
            is UnitSystemError.ConversionError -> false // Conversion errors are usually input-related
            is UnitSystemError.UnitValidationError -> false // Validation errors need input correction
            is UnitSystemError.LocaleDetectionError -> false // Locale errors are usually system-related
        }
    }
    
    /**
     * Gets suggested retry delay in seconds for retryable errors
     */
    fun getRetryDelay(error: UnitSystemError, attemptNumber: Int): Int {
        return when (error) {
            is UnitSystemError.UnitSystemNetworkError -> {
                error.retryAfterSeconds ?: calculateExponentialBackoff(attemptNumber)
            }
            is UnitSystemError.PreferencesSyncError -> {
                calculateExponentialBackoff(attemptNumber)
            }
            is UnitSystemError.PreferenceOperationError -> {
                if (error.retryable) calculateExponentialBackoff(attemptNumber) else 0
            }
            else -> 0
        }
    }
    
    /**
     * Calculates exponential backoff delay with jitter
     */
    private fun calculateExponentialBackoff(attemptNumber: Int): Int {
        val baseDelay = 2 // 2 seconds base delay
        val maxDelay = 60 // Maximum 60 seconds
        val delay = minOf(baseDelay * (1 shl (attemptNumber - 1)), maxDelay)
        
        // Add jitter (±25% of delay)
        val jitter = (delay * 0.25 * (kotlin.random.Random.nextDouble() - 0.5)).toInt()
        return delay + jitter
    }
    
    /**
     * Converts UnitSystemError to generic AppError for compatibility
     */
    fun toAppError(error: UnitSystemError): AppError {
        return when (error) {
            is UnitSystemError.ConversionError -> AppError.ValidationError(
                message = error.message,
                field = error.fromUnit ?: error.toUnit,
                cause = error.cause
            )
            
            is UnitSystemError.UnitValidationError -> AppError.ValidationError(
                message = error.message,
                field = error.field,
                cause = error.cause
            )
            
            is UnitSystemError.PreferencesSyncError -> AppError.DataSyncError(
                message = error.message,
                operation = error.operation,
                cause = error.cause
            )
            
            is UnitSystemError.PreferenceOperationError -> AppError.DataSyncError(
                message = error.message,
                operation = error.operation,
                cause = error.cause
            )
            
            is UnitSystemError.UnitSystemNetworkError -> AppError.NetworkError(
                message = error.message,
                cause = error.cause
            )
            
            is UnitSystemError.LocaleDetectionError -> AppError.UnknownError(
                message = error.message,
                cause = error.cause
            )
        }
    }
}