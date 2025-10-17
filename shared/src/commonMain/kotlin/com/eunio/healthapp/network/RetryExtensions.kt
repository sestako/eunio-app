package com.eunio.healthapp.network

import kotlinx.coroutines.delay
import kotlin.coroutines.cancellation.CancellationException

/**
 * Execute an operation with retry logic and exponential backoff
 *
 * @param policy The retry policy to use
 * @param shouldRetry Custom function to determine if an error should be retried
 * @param onRetry Callback invoked before each retry attempt
 * @param operation The operation to execute
 * @return The result of the operation
 * @throws Exception if all retry attempts fail
 */
suspend fun <T> withRetry(
    policy: RetryPolicy = RetryPolicy.DEFAULT,
    shouldRetry: (Throwable) -> Boolean = ::isRetryableError,
    onRetry: suspend (attempt: Int, error: Throwable, delay: kotlin.time.Duration) -> Unit = { _, _, _ -> },
    operation: suspend () -> T
): T {
    return withRetryAndAnalytics(
        policy = policy,
        shouldRetry = shouldRetry,
        onRetry = onRetry,
        analytics = null,
        operationName = "unknown",
        serviceName = "unknown",
        operation = operation
    )
}

/**
 * Execute an operation with retry logic, exponential backoff, and analytics tracking
 *
 * @param policy The retry policy to use
 * @param shouldRetry Custom function to determine if an error should be retried
 * @param onRetry Callback invoked before each retry attempt
 * @param analytics Analytics tracker for retry events
 * @param operationName Name of the operation (for analytics)
 * @param serviceName Name of the service (for analytics)
 * @param operation The operation to execute
 * @return The result of the operation
 * @throws Exception if all retry attempts fail
 */
suspend fun <T> withRetryAndAnalytics(
    policy: RetryPolicy = RetryPolicy.DEFAULT,
    shouldRetry: (Throwable) -> Boolean = ::isRetryableError,
    onRetry: suspend (attempt: Int, error: Throwable, delay: kotlin.time.Duration) -> Unit = { _, _, _ -> },
    analytics: RetryAnalytics? = null,
    operationName: String = "unknown",
    serviceName: String = "unknown",
    operation: suspend () -> T
): T {
    var lastException: Throwable? = null
    var attempt = 0
    
    while (attempt <= policy.maxAttempts) {
        try {
            // Attempt the operation
            val result = operation()
            
            // Log success if this was a retry
            if (attempt > 0) {
                println("RetryExtensions: Operation succeeded on attempt ${attempt + 1}")
                
                // Log success to analytics
                analytics?.logRetryResult(
                    operationName = operationName,
                    serviceName = serviceName,
                    totalAttempts = attempt + 1,
                    succeeded = true,
                    finalError = null
                )
            }
            
            return result
            
        } catch (e: CancellationException) {
            // Never retry cancellation
            throw e
            
        } catch (e: Exception) {
            lastException = e
            
            // Check if we should retry
            val isRetryable = shouldRetry(e)
            val hasAttemptsLeft = attempt < policy.maxAttempts
            
            if (!isRetryable || !hasAttemptsLeft) {
                // Don't retry - throw the exception
                println("RetryExtensions: Operation failed on attempt ${attempt + 1}, not retrying (retryable=$isRetryable, attemptsLeft=$hasAttemptsLeft)")
                
                // Log failure to analytics
                analytics?.logRetryResult(
                    operationName = operationName,
                    serviceName = serviceName,
                    totalAttempts = attempt + 1,
                    succeeded = false,
                    finalError = e.message
                )
                
                throw e
            }
            
            // Calculate delay and wait
            val delayDuration = policy.delayForAttempt(attempt)
            println("RetryExtensions: Operation failed on attempt ${attempt + 1}, retrying in ${delayDuration.inWholeMilliseconds}ms (error: ${e.message})")
            
            // Log retry attempt to analytics
            analytics?.logRetryAttempt(
                RetryEvent(
                    operationName = operationName,
                    serviceName = serviceName,
                    attemptNumber = attempt + 1,
                    errorType = categorizeError(e),
                    errorMessage = e.message ?: "Unknown error",
                    delayMs = delayDuration.inWholeMilliseconds,
                    succeeded = false
                )
            )
            
            // Invoke callback
            onRetry(attempt, e, delayDuration)
            
            // Wait before retry
            delay(delayDuration)
            
            attempt++
        }
    }
    
    // Should never reach here, but throw last exception if we do
    throw lastException ?: IllegalStateException("Retry failed with no exception")
}

/**
 * Determine if an error should be retried based on its type
 */
fun isRetryableError(error: Throwable): Boolean {
    val message = error.message?.lowercase() ?: ""
    
    return when {
        // Network errors
        message.contains("network") -> true
        message.contains("connection") -> true
        message.contains("socket") -> true
        message.contains("unreachable") -> true
        
        // Timeout errors
        message.contains("timeout") -> true
        message.contains("timed out") -> true
        
        // Server errors (5xx)
        message.contains("internal server error") -> true
        message.contains("service unavailable") -> true
        message.contains("bad gateway") -> true
        message.contains("gateway timeout") -> true
        
        // Rate limiting
        message.contains("rate limit") -> true
        message.contains("too many requests") -> true
        message.contains("quota exceeded") -> true
        
        // Firebase specific
        message.contains("unavailable") -> true
        message.contains("deadline exceeded") -> true
        message.contains("resource exhausted") -> true
        
        // Default: don't retry unknown errors
        else -> false
    }
}

/**
 * Categorize an error into a retry type
 */
fun categorizeError(error: Throwable): RetryableErrorType {
    val message = error.message?.lowercase() ?: ""
    
    return when {
        message.contains("network") || message.contains("connection") -> 
            RetryableErrorType.NETWORK_ERROR
        message.contains("timeout") -> 
            RetryableErrorType.TIMEOUT
        message.contains("internal server error") || message.contains("5") -> 
            RetryableErrorType.SERVER_ERROR
        message.contains("rate limit") || message.contains("too many requests") -> 
            RetryableErrorType.RATE_LIMIT
        message.contains("unavailable") -> 
            RetryableErrorType.UNAVAILABLE
        else -> 
            RetryableErrorType.UNKNOWN
    }
}

/**
 * Execute an operation with retry, returning a RetryResult instead of throwing
 */
suspend fun <T> withRetryResult(
    policy: RetryPolicy = RetryPolicy.DEFAULT,
    shouldRetry: (Throwable) -> Boolean = ::isRetryableError,
    onRetry: suspend (attempt: Int, error: Throwable, delay: kotlin.time.Duration) -> Unit = { _, _, _ -> },
    operation: suspend () -> T
): RetryResult<T> {
    return try {
        val result = withRetry(policy, shouldRetry, onRetry, operation)
        RetryResult.Success(result, 0) // TODO: Track actual attempt number
    } catch (e: Exception) {
        RetryResult.Failure(
            error = e,
            attemptNumber = policy.maxAttempts,
            isRetryable = shouldRetry(e)
        )
    }
}
