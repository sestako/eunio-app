package com.eunio.healthapp.data.remote.firebase

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.util.StructuredLogger
import platform.Foundation.NSError

/**
 * Maps iOS Firebase errors to domain AppError types.
 * 
 * This mapper translates Firebase iOS SDK error codes into the application's
 * error hierarchy, providing consistent error handling across platforms.
 * 
 * Firebase Error Codes Reference:
 * - 0: OK (no error)
 * - 1: CANCELLED
 * - 2: UNKNOWN
 * - 3: INVALID_ARGUMENT
 * - 4: DEADLINE_EXCEEDED
 * - 5: NOT_FOUND
 * - 6: ALREADY_EXISTS
 * - 7: PERMISSION_DENIED
 * - 8: RESOURCE_EXHAUSTED
 * - 9: FAILED_PRECONDITION
 * - 10: ABORTED
 * - 11: OUT_OF_RANGE
 * - 12: UNIMPLEMENTED
 * - 13: INTERNAL
 * - 14: UNAVAILABLE
 * - 15: DATA_LOSS
 * - 16: UNAUTHENTICATED
 */
object FirebaseErrorMapper {
    
    private const val LOG_TAG = "FirebaseErrorMapper"
    
    // Firebase error codes (from FirestoreErrorCode)
    private const val ERROR_CODE_OK = 0L
    private const val ERROR_CODE_CANCELLED = 1L
    private const val ERROR_CODE_UNKNOWN = 2L
    private const val ERROR_CODE_INVALID_ARGUMENT = 3L
    private const val ERROR_CODE_DEADLINE_EXCEEDED = 4L
    private const val ERROR_CODE_NOT_FOUND = 5L
    private const val ERROR_CODE_ALREADY_EXISTS = 6L
    private const val ERROR_CODE_PERMISSION_DENIED = 7L
    private const val ERROR_CODE_RESOURCE_EXHAUSTED = 8L
    private const val ERROR_CODE_FAILED_PRECONDITION = 9L
    private const val ERROR_CODE_ABORTED = 10L
    private const val ERROR_CODE_OUT_OF_RANGE = 11L
    private const val ERROR_CODE_UNIMPLEMENTED = 12L
    private const val ERROR_CODE_INTERNAL = 13L
    private const val ERROR_CODE_UNAVAILABLE = 14L
    private const val ERROR_CODE_DATA_LOSS = 15L
    private const val ERROR_CODE_UNAUTHENTICATED = 16L
    
    /**
     * Maps a Firebase NSError to an AppError.
     * 
     * @param error The NSError from Firebase iOS SDK
     * @param operation Optional operation context for better error messages
     * @return Mapped AppError with appropriate type and message
     */
    fun mapError(error: NSError, operation: String? = null): AppError {
        val errorCode = error.code
        val errorMessage = error.localizedDescription
        val operationContext = operation?.let { " during $it" } ?: ""
        
        // Log the error mapping for debugging
        StructuredLogger.logStructured(
            tag = LOG_TAG,
            operation = "ERROR_MAPPING",
            data = mapOf(
                "errorCode" to errorCode,
                "errorMessage" to errorMessage,
                "operation" to (operation ?: "unknown"),
                "domain" to error.domain
            )
        )
        
        return when (errorCode) {
            ERROR_CODE_UNAVAILABLE -> {
                AppError.NetworkError(
                    message = "No internet connection$operationContext. Please check your network and try again.",
                    cause = createThrowableFromNSError(error)
                )
            }
            
            ERROR_CODE_UNAUTHENTICATED -> {
                AppError.AuthenticationError(
                    message = "Authentication required$operationContext. Please sign in again.",
                    cause = createThrowableFromNSError(error)
                )
            }
            
            ERROR_CODE_PERMISSION_DENIED -> {
                AppError.PermissionError(
                    message = "Access denied$operationContext. You don't have permission to perform this action.",
                    requiredPermission = "firestore.write",
                    cause = createThrowableFromNSError(error)
                )
            }
            
            ERROR_CODE_NOT_FOUND -> {
                AppError.DataSyncError(
                    message = "Document not found$operationContext.",
                    operation = operation,
                    cause = createThrowableFromNSError(error)
                )
            }
            
            ERROR_CODE_ALREADY_EXISTS -> {
                AppError.DataSyncError(
                    message = "Document already exists$operationContext.",
                    operation = operation,
                    cause = createThrowableFromNSError(error)
                )
            }
            
            ERROR_CODE_DEADLINE_EXCEEDED -> {
                AppError.NetworkError(
                    message = "Request timed out$operationContext. Please try again.",
                    cause = createThrowableFromNSError(error)
                )
            }
            
            ERROR_CODE_RESOURCE_EXHAUSTED -> {
                AppError.NetworkError(
                    message = "Service temporarily unavailable$operationContext. Please try again later.",
                    cause = createThrowableFromNSError(error)
                )
            }
            
            ERROR_CODE_INVALID_ARGUMENT -> {
                AppError.ValidationError(
                    message = "Invalid data$operationContext: $errorMessage",
                    cause = createThrowableFromNSError(error)
                )
            }
            
            ERROR_CODE_FAILED_PRECONDITION -> {
                AppError.ValidationError(
                    message = "Operation failed precondition check$operationContext: $errorMessage",
                    cause = createThrowableFromNSError(error)
                )
            }
            
            ERROR_CODE_ABORTED -> {
                AppError.DataSyncError(
                    message = "Operation aborted$operationContext. Please try again.",
                    operation = operation,
                    cause = createThrowableFromNSError(error)
                )
            }
            
            ERROR_CODE_INTERNAL, ERROR_CODE_DATA_LOSS -> {
                AppError.DatabaseError(
                    message = "Internal server error$operationContext. Please try again later.",
                    operation = operation,
                    cause = createThrowableFromNSError(error)
                )
            }
            
            ERROR_CODE_CANCELLED -> {
                AppError.DataSyncError(
                    message = "Operation cancelled$operationContext.",
                    operation = operation,
                    cause = createThrowableFromNSError(error)
                )
            }
            
            ERROR_CODE_UNIMPLEMENTED -> {
                AppError.UnknownError(
                    message = "Operation not supported$operationContext.",
                    cause = createThrowableFromNSError(error)
                )
            }
            
            else -> {
                // Unknown error code
                StructuredLogger.logStructured(
                    tag = LOG_TAG,
                    operation = "UNKNOWN_ERROR_CODE",
                    data = mapOf(
                        "errorCode" to errorCode,
                        "errorMessage" to errorMessage,
                        "operation" to (operation ?: "unknown")
                    )
                )
                
                AppError.UnknownError(
                    message = "Firebase error$operationContext: $errorMessage (code: $errorCode)",
                    cause = createThrowableFromNSError(error)
                )
            }
        }
    }
    
    /**
     * Maps a generic Throwable to an AppError.
     * This is used for non-Firebase errors that might occur during operations.
     * 
     * @param throwable The throwable to map
     * @param operation Optional operation context
     * @return Mapped AppError
     */
    fun mapThrowable(throwable: Throwable, operation: String? = null): AppError {
        val operationContext = operation?.let { " during $it" } ?: ""
        
        StructuredLogger.logStructured(
            tag = LOG_TAG,
            operation = "THROWABLE_MAPPING",
            data = mapOf(
                "throwableType" to throwable::class.simpleName,
                "message" to (throwable.message ?: "unknown"),
                "operation" to (operation ?: "unknown")
            )
        )
        
        return when (throwable) {
            is AppError -> throwable
            is IllegalArgumentException -> AppError.ValidationError(
                message = "Invalid argument$operationContext: ${throwable.message}",
                cause = throwable
            )
            is IllegalStateException -> AppError.DataSyncError(
                message = "Invalid state$operationContext: ${throwable.message}",
                operation = operation,
                cause = throwable
            )
            else -> AppError.UnknownError(
                message = "Unexpected error$operationContext: ${throwable.message ?: "Unknown error"}",
                cause = throwable
            )
        }
    }
    
    /**
     * Creates a Throwable from an NSError for use as a cause in AppError.
     * This preserves the original error information for debugging.
     */
    private fun createThrowableFromNSError(error: NSError): Throwable {
        return Exception(
            "Firebase error (code: ${error.code}, domain: ${error.domain}): ${error.localizedDescription}"
        )
    }
    
    /**
     * Logs an error with structured logging.
     * 
     * @param error The AppError to log
     * @param operation The operation that failed
     * @param additionalContext Additional context to include in the log
     */
    fun logError(
        error: AppError,
        operation: String,
        additionalContext: Map<String, Any?> = emptyMap()
    ) {
        val logData = mutableMapOf<String, Any?>(
            "operation" to operation,
            "errorType" to error::class.simpleName,
            "errorMessage" to error.message
        )
        logData.putAll(additionalContext)
        
        StructuredLogger.logStructured(
            tag = LOG_TAG,
            operation = "ERROR_LOGGED",
            data = logData
        )
    }
}
