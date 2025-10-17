package com.eunio.healthapp.domain.error

/**
 * Sealed class hierarchy for unit system specific errors.
 * Extends AppError to integrate with the existing error handling system.
 */
sealed class UnitSystemError : AppError() {
    
    /**
     * Errors that occur during unit conversion operations
     */
    data class ConversionError(
        override val message: String,
        val inputValue: Double? = null,
        val fromUnit: String? = null,
        val toUnit: String? = null,
        override val cause: Throwable? = null
    ) : UnitSystemError()
    
    /**
     * Errors that occur during preference synchronization
     */
    data class PreferencesSyncError(
        override val message: String,
        val operation: String? = null,
        val userId: String? = null,
        override val cause: Throwable? = null
    ) : UnitSystemError()
    
    /**
     * Errors that occur during locale detection for unit system initialization
     */
    data class LocaleDetectionError(
        override val message: String,
        val locale: String? = null,
        override val cause: Throwable? = null
    ) : UnitSystemError()
    
    /**
     * Validation errors specific to unit system inputs
     */
    data class UnitValidationError(
        override val message: String,
        val field: String? = null,
        val inputValue: Any? = null,
        override val cause: Throwable? = null
    ) : UnitSystemError()
    
    /**
     * Errors that occur during unit system preference operations
     */
    data class PreferenceOperationError(
        override val message: String,
        val operation: String,
        val retryable: Boolean = true,
        override val cause: Throwable? = null
    ) : UnitSystemError()
    
    /**
     * Network-related errors specific to unit system operations
     */
    data class UnitSystemNetworkError(
        override val message: String,
        val operation: String? = null,
        val retryAfterSeconds: Int? = null,
        override val cause: Throwable? = null
    ) : UnitSystemError()
}