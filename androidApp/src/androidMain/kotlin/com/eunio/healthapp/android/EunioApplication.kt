package com.eunio.healthapp.android

import android.app.Application
import com.eunio.healthapp.di.AndroidKoinInitializer
import com.eunio.healthapp.network.NetworkMonitor
import com.google.firebase.FirebaseApp
import org.koin.android.ext.android.inject

/**
 * Application class for initializing Firebase and Koin dependency injection
 * 
 * Note: Firebase Performance Monitoring is automatically enabled via the
 * firebase-perf Gradle plugin. No explicit initialization needed!
 */
class EunioApplication : Application() {
    
    private val networkMonitor: NetworkMonitor by inject()
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase first
        FirebaseApp.initializeApp(this)
        
        // Initialize Koin with Android context
        AndroidKoinInitializer.initKoin(this)
        
        // Start network monitoring
        networkMonitor.startMonitoring()
    }
}