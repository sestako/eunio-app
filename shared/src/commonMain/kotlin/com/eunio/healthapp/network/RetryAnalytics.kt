package com.eunio.healthapp.network

/**
 * Analytics tracking for retry operations
 * Integrates with Firebase Analytics to monitor retry behavior
 */

/**
 * Retry event data for analytics
 */
data class RetryEvent(
    val operationName: String,
    val serviceName: String,
    val attemptNumber: Int,
    val errorType: RetryableErrorType,
    val errorMessage: String,
    val delayMs: Long,
    val succeeded: Boolean
)

/**
 * Retry metrics for monitoring
 */
data class RetryMetrics(
    val totalRetries: Int = 0,
    val successfulRetries: Int = 0,
    val failedRetries: Int = 0,
    val averageAttempts: Double = 0.0,
    val averageDelayMs: Long = 0,
    val errorTypeDistribution: Map<RetryableErrorType, Int> = emptyMap()
) {
    val successRate: Double
        get() = if (totalRetries > 0) successfulRetries.toDouble() / totalRetries else 0.0
    
    val failureRate: Double
        get() = if (totalRetries > 0) failedRetries.toDouble() / totalRetries else 0.0
}

/**
 * Interface for retry analytics tracking
 */
interface RetryAnalytics {
    /**
     * Log a retry attempt
     */
    fun logRetryAttempt(event: RetryEvent)
    
    /**
     * Log final retry result (success or failure after all attempts)
     */
    fun logRetryResult(
        operationName: String,
        serviceName: String,
        totalAttempts: Int,
        succeeded: Boolean,
        finalError: String?
    )
    
    /**
     * Get current retry metrics (optional, for monitoring dashboards)
     */
    fun getMetrics(): RetryMetrics
}

/**
 * No-op implementation for when analytics is disabled
 */
class NoOpRetryAnalytics : RetryAnalytics {
    override fun logRetryAttempt(event: RetryEvent) {
        // No-op
    }
    
    override fun logRetryResult(
        operationName: String,
        serviceName: String,
        totalAttempts: Int,
        succeeded: Boolean,
        finalError: String?
    ) {
        // No-op
    }
    
    override fun getMetrics(): RetryMetrics = RetryMetrics()
}

/**
 * In-memory retry analytics for testing and monitoring
 */
class InMemoryRetryAnalytics : RetryAnalytics {
    private val events = mutableListOf<RetryEvent>()
    private val results = mutableListOf<RetryResult>()
    
    data class RetryResult(
        val operationName: String,
        val serviceName: String,
        val totalAttempts: Int,
        val succeeded: Boolean,
        val finalError: String?
    )
    
    override fun logRetryAttempt(event: RetryEvent) {
        events.add(event)
    }
    
    override fun logRetryResult(
        operationName: String,
        serviceName: String,
        totalAttempts: Int,
        succeeded: Boolean,
        finalError: String?
    ) {
        results.add(RetryResult(operationName, serviceName, totalAttempts, succeeded, finalError))
    }
    
    override fun getMetrics(): RetryMetrics {
        // Note: Not thread-safe, but acceptable for testing/monitoring
        val totalRetries = results.size
        val successfulRetries = results.count { it.succeeded }
        val failedRetries = results.count { !it.succeeded }
        
        val averageAttempts = if (results.isNotEmpty()) {
            results.map { it.totalAttempts }.average()
        } else 0.0
        
        val averageDelay = if (events.isNotEmpty()) {
            events.map { it.delayMs }.average().toLong()
        } else 0L
        
        val errorDistribution = events
            .groupBy { it.errorType }
            .mapValues { it.value.size }
        
        return RetryMetrics(
            totalRetries = totalRetries,
            successfulRetries = successfulRetries,
            failedRetries = failedRetries,
            averageAttempts = averageAttempts,
            averageDelayMs = averageDelay,
            errorTypeDistribution = errorDistribution
        )
    }
    
    fun clear() {
        events.clear()
        results.clear()
    }
}