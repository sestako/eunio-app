package com.eunio.healthapp.services

/**
 * Analytics service for tracking user events and app usage
 */
interface AnalyticsService {
    // Authentication Events
    fun logSignUp(method: String)
    fun logSignIn(method: String)
    fun logSignOut()
    
    // User Profile Events
    fun logProfileCreated()
    fun logProfileUpdated()
    
    // Daily Log Events
    fun logDailyLogCreated()
    fun logDailyLogUpdated()
    fun logDailyLogDeleted()
    
    // Cycle Events
    fun logCycleStarted()
    fun logCycleEnded()
    
    // Screen View Events
    fun logScreenView(screenName: String)
    
    // Feature Usage Events
    fun logFeatureUsed(featureName: String)
    
    // Custom Events
    fun logEvent(eventName: String, params: Map<String, Any> = emptyMap())
}
