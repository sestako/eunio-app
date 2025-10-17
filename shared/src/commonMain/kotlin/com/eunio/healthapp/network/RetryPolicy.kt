package com.eunio.healthapp.network

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Configuration for retry behavior with exponential backoff
 */
data class RetryPolicy(
    /**
     * Maximum number of retry attempts (not including the initial attempt)
     * Default: 3 retries (4 total attempts)
     */
    val maxAttempts: Int = 3,
    
    /**
     * Initial delay before first retry
     * Default: 1 second
     */
    val initialDelay: Duration = 1.seconds,
    
    /**
     * Maximum delay between retries
     * Default: 30 seconds
     */
    val maxDelay: Duration = 30.seconds,
    
    /**
     * Multiplier for exponential backoff
     * Each retry delay = previous delay * multiplier
     * Default: 2.0 (doubles each time)
     */
    val multiplier: Double = 2.0,
    
    /**
     * Whether to add random jitter to prevent thundering herd
     * Default: true
     */
    val jitter: Boolean = true
) {
    init {
        require(maxAttempts >= 0) { "maxAttempts must be >= 0" }
        require(initialDelay.isPositive()) { "initialDelay must be positive" }
        require(maxDelay >= initialDelay) { "maxDelay must be >= initialDelay" }
        require(multiplier > 1.0) { "multiplier must be > 1.0" }
    }
    
    /**
     * Calculate delay for a specific attempt number
     * @param attempt The attempt number (0-based, where 0 is first retry)
     * @return The delay duration before this retry
     */
    fun delayForAttempt(attempt: Int): Duration {
        if (attempt < 0) return Duration.ZERO
        
        // Calculate exponential delay: initialDelay * (multiplier ^ attempt)
        var exponentialDelay = initialDelay.inWholeMilliseconds.toDouble()
        repeat(attempt) {
            exponentialDelay *= multiplier
        }
        
        // Cap at maxDelay
        val cappedDelay = minOf(exponentialDelay, maxDelay.inWholeMilliseconds.toDouble())
        
        // Add jitter if enabled (random 0-25% of delay)
        val finalDelay = if (jitter) {
            val jitterAmount = cappedDelay * (kotlin.random.Random.nextDouble() * 0.25)
            cappedDelay + jitterAmount
        } else {
            cappedDelay
        }
        
        return finalDelay.toLong().milliseconds
    }
    
    companion object {
        /**
         * Default retry policy for most operations
         */
        val DEFAULT = RetryPolicy()
        
        /**
         * Aggressive retry policy for critical operations
         * More attempts, faster initial retry
         */
        val AGGRESSIVE = RetryPolicy(
            maxAttempts = 5,
            initialDelay = 500.milliseconds,
            multiplier = 1.5
        )
        
        /**
         * Conservative retry policy for non-critical operations
         * Fewer attempts, longer delays
         */
        val CONSERVATIVE = RetryPolicy(
            maxAttempts = 2,
            initialDelay = 2.seconds,
            multiplier = 3.0
        )
        
        /**
         * No retry policy - fail immediately
         */
        val NONE = RetryPolicy(maxAttempts = 0)
    }
}

/**
 * Exception types that should be retried
 */
enum class RetryableErrorType {
    NETWORK_ERROR,      // Network connectivity issues
    TIMEOUT,            // Request timeout
    SERVER_ERROR,       // 5xx server errors
    RATE_LIMIT,         // 429 rate limiting
    UNAVAILABLE,        // Service temporarily unavailable
    UNKNOWN             // Unknown transient error
}

/**
 * Result of a retry attempt
 */
sealed class RetryResult<out T> {
    data class Success<T>(val value: T, val attemptNumber: Int) : RetryResult<T>()
    data class Failure(
        val error: Throwable,
        val attemptNumber: Int,
        val isRetryable: Boolean
    ) : RetryResult<Nothing>()
}
