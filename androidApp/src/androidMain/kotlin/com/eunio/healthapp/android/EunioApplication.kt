package com.eunio.healthapp.android

import android.app.Application
import android.content.pm.ApplicationInfo
import android.util.Log
import com.eunio.healthapp.di.AndroidKoinInitializer
import com.eunio.healthapp.network.NetworkMonitor
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import org.koin.android.ext.android.inject

/**
 * Application class for initializing Firebase and Koin dependency injection
 * 
 * Note: Firebase Performance Monitoring is automatically enabled via the
 * firebase-perf Gradle plugin. No explicit initialization needed!
 */
class EunioApplication : Application() {
    
    private val networkMonitor: NetworkMonitor by inject()
    private val syncManager: com.eunio.healthapp.data.sync.SyncManager by inject()
    
    companion object {
        private const val TAG = "EunioApplication"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        Log.d(TAG, "🔥 Starting Firebase initialization...")
        
        // Check if this is a debug build
        val isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        
        // Enable Firebase debug logging in debug builds
        if (isDebuggable) {
            // Firebase debug logging is controlled via adb:
            // adb shell setprop log.tag.FirebaseFirestore DEBUG
            // adb shell setprop log.tag.FirebaseAuth DEBUG
            Log.d(TAG, "🔥 Debug build - Firebase debug logging available via adb")
            Log.d(TAG, "   Run: adb shell setprop log.tag.FirebaseFirestore DEBUG")
            Log.d(TAG, "   Run: adb shell setprop log.tag.FirebaseAuth DEBUG")
        }
        
        // Initialize Firebase first
        val firebaseApp = FirebaseApp.initializeApp(this)
        
        if (firebaseApp != null) {
            Log.d(TAG, "🔥 Firebase initialized successfully")
            Log.d(TAG, "   App name: ${firebaseApp.name}")
            Log.d(TAG, "   Project ID: ${firebaseApp.options.projectId}")
            Log.d(TAG, "   Application ID: ${firebaseApp.options.applicationId}")
            Log.d(TAG, "   API Key: ${firebaseApp.options.apiKey}")
            
            // Configure Firestore settings
            val firestore = FirebaseFirestore.getInstance()
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            firestore.firestoreSettings = settings
            Log.d(TAG, "🔥 Firestore persistence enabled")
            
            // Enable Firestore logging in debug builds
            if (isDebuggable) {
                FirebaseFirestore.setLoggingEnabled(true)
                Log.d(TAG, "🔥 Firestore debug logging enabled")
            }
        } else {
            Log.e(TAG, "❌ Firebase initialization failed!")
        }
        
        // Initialize Koin with Android context
        AndroidKoinInitializer.initKoin(this)
        
        // Start network monitoring
        networkMonitor.startMonitoring()
        
        // Start sync manager for automatic background sync
        syncManager.startMonitoring()
        Log.d(TAG, "🔄 Sync manager started")
        
        // Generate diagnostic report in debug builds
        if (isDebuggable) {
            Log.d(TAG, "\n")
            FirebaseDiagnostics.printReport(this)
            
            // Run integrity test if flag is set
            val prefs = getSharedPreferences("debug_settings", MODE_PRIVATE)
            if (prefs.getBoolean("DEBUG_SYNC_TEST", false)) {
                FirebaseDiagnostics.runIntegrityTest(this)
            }
        }
        
        Log.d(TAG, "🔥 Application initialization complete")
    }
}