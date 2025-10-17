package com.eunio.healthapp.domain.util

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.error.ServiceError
import com.eunio.healthapp.di.KoinLogger
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.Clock

/**
 * Comprehensive error handling utility for service operations.
 * Provides consistent error logging, graceful degradation, and fallback mechanisms.
 */
object ServiceErrorHandler {
    
    /**
     * Execute a service operation with comprehensive error handling.
     * 
     * @param serviceName Name of the service for logging
     * @param operation Name of the operation for logging
     * @param timeoutMs Timeout for the operation in milliseconds
     * @param fallback Fallback function to execute if the main operation fails
     * @param block The main operation to execute
     * @return Result containing the operation result or error
     */
    suspend fun <T> safeExecute(
        serviceName: String,
        operation: String,
        timeoutMs: Long = 30000L,
        fallback: (suspend () -> T)? = null,
        block: suspend () -> T
    ): Result<T> {
        return try {
            logOperationStart(serviceName, operation)
            
            val result = withTimeout(timeoutMs) {
                block()
            }
            
            logOperationSuccess(serviceName, operation)
            Result.success(result)
            
        } catch (e: TimeoutCancellationException) {
            val error = ServiceError.TimeoutError(
                message = "Operation '$operation' timed out after ${timeoutMs}ms",
                serviceName = serviceName,
                operation = operation,
                timeoutMs = timeoutMs,
                cause = e
            )
            logOperationError(serviceName, operation, error)
            
            // Try fallback if available
            fallback?.let { fallbackFn ->
                try {
                    logFallbackAttempt(serviceName, operation)
                    val fallbackResult = fallbackFn()
                    logFallbackSuccess(serviceName, operation)
                    return Result.success(fallbackResult)
                } catch (fallbackError: Exception) {
                    logFallbackFailure(serviceName, operation, fallbackError)
                }
            }
            
            Result.error(error)
            
        } catch (e: Exception) {
            val error = when (e) {
                is AppError -> e
                is IllegalArgumentException -> ServiceError.ConfigurationError(
                    message = "Invalid configuration: ${e.message}",
                    serviceName = serviceName,
                    configurationIssue = e.message ?: "Unknown configuration issue",
                    cause = e
                )
                is IllegalStateException -> ServiceError.InitializationError(
                    message = "Service not properly initialized: ${e.message}",
                    serviceName = serviceName,
                    cause = e
                )
                else -> ServiceError.OperationError(
                    message = "Operation '$operation' failed: ${e.message}",
                    serviceName = serviceName,
                    operation = operation,
                    cause = e
                )
            }
            
            logOperationError(serviceName, operation, error)
            
            // Try fallback if available
            fallback?.let { fallbackFn ->
                try {
                    logFallbackAttempt(serviceName, operation)
                    val fallbackResult = fallbackFn()
                    logFallbackSuccess(serviceName, operation)
                    return Result.success(fallbackResult)
                } catch (fallbackError: Exception) {
                    logFallbackFailure(serviceName, operation, fallbackError)
                }
            }
            
            Result.error(error)
        }
    }
    
    /**
     * Execute a service operation with retry logic and error handling.
     * 
     * @param serviceName Name of the service for logging
     * @param operation Name of the operation for logging
     * @param maxRetries Maximum number of retry attempts
     * @param retryDelayMs Delay between retry attempts in milliseconds
     * @param fallback Fallback function to execute if all retries fail
     * @param block The main operation to execute
     * @return Result containing the operation result or error
     */
    suspend fun <T> safeExecuteWithRetry(
        serviceName: String,
        operation: String,
        maxRetries: Int = 3,
        retryDelayMs: Long = 1000L,
        fallback: (suspend () -> T)? = null,
        block: suspend () -> T
    ): Result<T> {
        var lastError: AppError? = null
        
        repeat(maxRetries + 1) { attempt ->
            if (attempt > 0) {
                logRetryAttempt(serviceName, operation, attempt, maxRetries)
                kotlinx.coroutines.delay(retryDelayMs * attempt) // Exponential backoff
            }
            
            val result = safeExecute(
                serviceName = serviceName,
                operation = operation,
                fallback = null, // Don't use fallback on individual attempts
                block = block
            )
            
            when (result) {
                is Result.Success -> return result
                is Result.Error -> {
                    lastError = result.error
                    // Continue to next retry attempt
                }
            }
        }
        
        // All retries failed, try fallback
        fallback?.let { fallbackFn ->
            try {
                logFallbackAttempt(serviceName, operation)
                val fallbackResult = fallbackFn()
                logFallbackSuccess(serviceName, operation)
                return Result.success(fallbackResult)
            } catch (fallbackError: Exception) {
                logFallbackFailure(serviceName, operation, fallbackError)
            }
        }
        
        logAllRetriesFailed(serviceName, operation, maxRetries)
        return Result.error(lastError ?: ServiceError.OperationError(
            message = "Operation '$operation' failed after $maxRetries retries",
            serviceName = serviceName,
            operation = operation
        ))
    }
    
    /**
     * Create a degraded mode error when service is partially functional.
     */
    fun createDegradedModeError(
        serviceName: String,
        availableOperations: List<String>,
        unavailableOperations: List<String>,
        cause: Throwable? = null
    ): ServiceError.DegradedModeError {
        return ServiceError.DegradedModeError(
            message = "Service '$serviceName' is running in degraded mode. Available: ${availableOperations.joinToString()}, Unavailable: ${unavailableOperations.joinToString()}",
            serviceName = serviceName,
            availableOperations = availableOperations,
            unavailableOperations = unavailableOperations,
            cause = cause
        )
    }
    
    /**
     * Log the start of a service operation.
     */
    private fun logOperationStart(serviceName: String, operation: String) {
        KoinLogger.logInfo("🔄 Starting $serviceName.$operation")
    }
    
    /**
     * Log successful completion of a service operation.
     */
    private fun logOperationSuccess(serviceName: String, operation: String) {
        KoinLogger.logInfo("✅ Completed $serviceName.$operation successfully")
    }
    
    /**
     * Log service operation error with detailed information.
     */
    private fun logOperationError(serviceName: String, operation: String, error: AppError) {
        println("❌ Service operation failed: $serviceName.$operation")
        println("   🔍 Error Type: ${error::class.simpleName}")
        println("   💬 Message: ${error.message}")
        println("   🕐 Timestamp: ${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}")
        
        error.cause?.let { cause ->
            println("   🐛 Root Cause: ${cause::class.simpleName}: ${cause.message}")
            cause.printStackTrace()
        }
    }
    
    /**
     * Log fallback attempt.
     */
    private fun logFallbackAttempt(serviceName: String, operation: String) {
        KoinLogger.logInfo("🔄 Attempting fallback for $serviceName.$operation")
    }
    
    /**
     * Log successful fallback execution.
     */
    private fun logFallbackSuccess(serviceName: String, operation: String) {
        KoinLogger.logInfo("✅ Fallback succeeded for $serviceName.$operation")
    }
    
    /**
     * Log fallback failure.
     */
    private fun logFallbackFailure(serviceName: String, operation: String, error: Throwable) {
        println("❌ Fallback failed for $serviceName.$operation: ${error.message}")
        error.printStackTrace()
    }
    
    /**
     * Log retry attempt.
     */
    private fun logRetryAttempt(serviceName: String, operation: String, attempt: Int, maxRetries: Int) {
        KoinLogger.logInfo("🔄 Retrying $serviceName.$operation (Attempt $attempt/$maxRetries)")
    }
    
    /**
     * Log when all retries have failed.
     */
    private fun logAllRetriesFailed(serviceName: String, operation: String, maxRetries: Int) {
        println("❌ All retries failed for $serviceName.$operation after $maxRetries attempts")
    }
}