package com.eunio.healthapp.domain.service

import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.domain.util.ServiceErrorHandler
import com.eunio.healthapp.domain.util.UserNotificationService
import kotlinx.datetime.Clock

/**
 * Base class for all service implementations providing consistent error handling,
 * logging, and graceful degradation capabilities.
 */
abstract class BaseService {
    
    /**
     * The name of this service for logging and error reporting.
     */
    abstract val serviceName: String
    
    /**
     * Execute a service operation with comprehensive error handling.
     * 
     * @param operation Name of the operation for logging
     * @param showUserMessage Whether to show user-facing error messages
     * @param timeoutMs Timeout for the operation in milliseconds
     * @param fallback Fallback function to execute if the main operation fails
     * @param block The main operation to execute
     * @return Result containing the operation result or error
     */
    protected suspend fun <T> executeOperation(
        operation: String,
        showUserMessage: Boolean = true,
        timeoutMs: Long = 30000L,
        fallback: (suspend () -> T)? = null,
        block: suspend () -> T
    ): Result<T> {
        val result = ServiceErrorHandler.safeExecute(
            serviceName = serviceName,
            operation = operation,
            timeoutMs = timeoutMs,
            fallback = fallback,
            block = block
        )
        
        // Show user message for errors if requested
        if (showUserMessage && result is Result.Error) {
            handleUserNotification(operation, result.error)
        }
        
        return result
    }
    
    /**
     * Execute a service operation with retry logic and error handling.
     * 
     * @param operation Name of the operation for logging
     * @param showUserMessage Whether to show user-facing error messages
     * @param maxRetries Maximum number of retry attempts
     * @param retryDelayMs Delay between retry attempts in milliseconds
     * @param fallback Fallback function to execute if all retries fail
     * @param block The main operation to execute
     * @return Result containing the operation result or error
     */
    protected suspend fun <T> executeOperationWithRetry(
        operation: String,
        showUserMessage: Boolean = true,
        maxRetries: Int = 3,
        retryDelayMs: Long = 1000L,
        fallback: (suspend () -> T)? = null,
        block: suspend () -> T
    ): Result<T> {
        val result = ServiceErrorHandler.safeExecuteWithRetry(
            serviceName = serviceName,
            operation = operation,
            maxRetries = maxRetries,
            retryDelayMs = retryDelayMs,
            fallback = fallback,
            block = block
        )
        
        // Show user message for errors if requested
        if (showUserMessage && result is Result.Error) {
            handleUserNotification(operation, result.error)
        }
        
        return result
    }
    
    /**
     * Handle user notification for service errors.
     * Can be overridden by subclasses for custom user messaging.
     */
    protected open suspend fun handleUserNotification(operation: String, error: Throwable) {
        val userMessage = getUserFriendlyMessage(operation, error)
        UserNotificationService.showError(userMessage)
    }
    
    /**
     * Convert technical errors to user-friendly messages.
     * Can be overridden by subclasses for service-specific messaging.
     */
    protected open fun getUserFriendlyMessage(operation: String, error: Throwable): String {
        return when {
            error.message?.contains("timeout", ignoreCase = true) == true -> 
                "The operation is taking longer than expected. Please try again."
            
            error.message?.contains("network", ignoreCase = true) == true -> 
                "Please check your internet connection and try again."
            
            error.message?.contains("permission", ignoreCase = true) == true -> 
                "Permission required. Please check your settings."
            
            error.message?.contains("authentication", ignoreCase = true) == true -> 
                "Please sign in again to continue."
            
            else -> "Something went wrong. Please try again later."
        }
    }
    
    /**
     * Check if the service is in a healthy state.
     * Should be overridden by subclasses to provide service-specific health checks.
     */
    open suspend fun isHealthy(): Boolean {
        return true
    }
    
    /**
     * Get the current status of the service.
     * Should be overridden by subclasses to provide detailed status information.
     */
    open suspend fun getServiceStatus(): ServiceStatus {
        return ServiceStatus(
            serviceName = serviceName,
            isHealthy = isHealthy(),
            lastChecked = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
            details = emptyMap()
        )
    }
}

/**
 * Represents the status of a service.
 */
data class ServiceStatus(
    val serviceName: String,
    val isHealthy: Boolean,
    val lastChecked: Long,
    val errorMessage: String? = null,
    val details: Map<String, Any> = emptyMap()
)