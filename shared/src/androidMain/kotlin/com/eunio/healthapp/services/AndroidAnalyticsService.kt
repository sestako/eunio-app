package com.eunio.healthapp.services

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

class AndroidAnalyticsService(context: Context) : AnalyticsService {
    private val analytics: FirebaseAnalytics = Firebase.analytics
    
    override fun logSignUp(method: String) {
        android.util.Log.d("Analytics", "📊 Logging sign_up event with method: $method")
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.METHOD, method)
        analytics.logEvent(FirebaseAnalytics.Event.SIGN_UP, bundle)
        android.util.Log.d("Analytics", "✅ sign_up event logged")
    }
    
    override fun logSignIn(method: String) {
        android.util.Log.d("Analytics", "📊 Logging login event with method: $method")
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.METHOD, method)
        analytics.logEvent(FirebaseAnalytics.Event.LOGIN, bundle)
        android.util.Log.d("Analytics", "✅ login event logged")
    }
    
    override fun logSignOut() {
        android.util.Log.d("Analytics", "📊 Logging sign_out event")
        analytics.logEvent("sign_out", null)
        android.util.Log.d("Analytics", "✅ sign_out event logged")
    }
    
    override fun logProfileCreated() {
        analytics.logEvent("profile_created", null)
    }
    
    override fun logProfileUpdated() {
        analytics.logEvent("profile_updated", null)
    }
    
    override fun logDailyLogCreated() {
        analytics.logEvent("daily_log_created", null)
    }
    
    override fun logDailyLogUpdated() {
        analytics.logEvent("daily_log_updated", null)
    }
    
    override fun logDailyLogDeleted() {
        analytics.logEvent("daily_log_deleted", null)
    }
    
    override fun logCycleStarted() {
        analytics.logEvent("cycle_started", null)
    }
    
    override fun logCycleEnded() {
        analytics.logEvent("cycle_ended", null)
    }
    
    override fun logScreenView(screenName: String) {
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }
    
    override fun logFeatureUsed(featureName: String) {
        val bundle = Bundle()
        bundle.putString("feature_name", featureName)
        analytics.logEvent("feature_used", bundle)
    }
    
    override fun logEvent(eventName: String, params: Map<String, Any>) {
        val bundle = Bundle()
        params.forEach { (key, value) ->
            when (value) {
                is String -> bundle.putString(key, value)
                is Long -> bundle.putLong(key, value)
                is Double -> bundle.putDouble(key, value)
                is Int -> bundle.putLong(key, value.toLong())
                is Boolean -> bundle.putLong(key, if (value) 1L else 0L)
            }
        }
        analytics.logEvent(eventName, bundle)
    }
}
