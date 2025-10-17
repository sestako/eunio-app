package com.eunio.healthapp.services

/**
 * Crashlytics service for crash and error reporting
 */
interface CrashlyticsService {
    /**
     * Log a non-fatal exception
     */
    fun recordException(exception: Throwable)
    
    /**
     * Log a message
     */
    fun log(message: String)
    
    /**
     * Set user identifier for crash reports
     */
    fun setUserId(userId: String)
    
    /**
     * Set custom key-value pair for crash reports
     */
    fun setCustomKey(key: String, value: String)
    
    /**
     * Set custom key-value pair for crash reports (boolean)
     */
    fun setCustomKey(key: String, value: Boolean)
    
    /**
     * Set custom key-value pair for crash reports (int)
     */
    fun setCustomKey(key: String, value: Int)
    
    /**
     * Force a crash (for testing only)
     */
    fun testCrash()
}
