package com.eunio.healthapp.network

import com.eunio.healthapp.services.AnalyticsService

/**
 * Firebase Analytics implementation for retry tracking
 */
class FirebaseRetryAnalytics(
    private val analyticsService: AnalyticsService
) : RetryAnalytics {
    
    override fun logRetryAttempt(event: RetryEvent) {
        // Log individual retry attempt
        analyticsService.logEvent(
            eventName = "retry_attempt",
            params = mapOf(
                "operation" to event.operationName,
                "service" to event.serviceName,
                "attempt" to event.attemptNumber.toString(),
                "error_type" to event.errorType.name,
                "delay_ms" to event.delayMs.toString(),
                "succeeded" to event.succeeded.toString()
            )
        )
    }
    
    override fun logRetryResult(
        operationName: String,
        serviceName: String,
        totalAttempts: Int,
        succeeded: Boolean,
        finalError: String?
    ) {
        // Log final result after all retry attempts
        val params = mutableMapOf(
            "operation" to operationName,
            "service" to serviceName,
            "total_attempts" to totalAttempts.toString(),
            "succeeded" to succeeded.toString()
        )
        
        if (finalError != null) {
            params["error"] = finalError.take(100) // Limit error message length
        }
        
        analyticsService.logEvent(
            eventName = if (succeeded) "retry_success" else "retry_failure",
            params = params
        )
    }
    
    override fun getMetrics(): RetryMetrics {
        // Firebase Analytics doesn't provide real-time metrics
        // Return empty metrics (use Firebase Console for actual metrics)
        return RetryMetrics()
    }
}
