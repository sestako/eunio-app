package com.eunio.healthapp.services

import com.google.firebase.crashlytics.FirebaseCrashlytics

class AndroidCrashlyticsService : CrashlyticsService {
    
    private val crashlytics = FirebaseCrashlytics.getInstance()
    
    override fun recordException(exception: Throwable) {
        android.util.Log.d("Crashlytics", "📊 Recording exception: ${exception.message}")
        crashlytics.recordException(exception)
    }
    
    override fun log(message: String) {
        android.util.Log.d("Crashlytics", "📝 Logging: $message")
        crashlytics.log(message)
    }
    
    override fun setUserId(userId: String) {
        android.util.Log.d("Crashlytics", "👤 Setting user ID: $userId")
        crashlytics.setUserId(userId)
    }
    
    override fun setCustomKey(key: String, value: String) {
        crashlytics.setCustomKey(key, value)
    }
    
    override fun setCustomKey(key: String, value: Boolean) {
        crashlytics.setCustomKey(key, value)
    }
    
    override fun setCustomKey(key: String, value: Int) {
        crashlytics.setCustomKey(key, value)
    }
    
    override fun testCrash() {
        android.util.Log.w("Crashlytics", "⚠️ TEST CRASH - This will crash the app!")
        throw RuntimeException("Test crash from Crashlytics")
    }
}
