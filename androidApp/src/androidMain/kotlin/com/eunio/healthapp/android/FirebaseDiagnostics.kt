package com.eunio.healthapp.android

import android.content.Context
import android.content.pm.ApplicationInfo
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Firebase Diagnostics utility for debugging sync issues on Android.
 * Provides comprehensive diagnostic information about Firebase configuration,
 * authentication, network connectivity, and Firestore operations.
 */
object FirebaseDiagnostics {
    
    private const val TAG = "FirebaseDiagnostics"
    
    /**
     * Generate a comprehensive diagnostic report.
     * Returns a formatted string with all diagnostic information.
     */
    fun generateReport(context: Context): String {
        val report = StringBuilder()
        
        report.appendLine("================================================================================")
        report.appendLine("FIREBASE DIAGNOSTICS REPORT (Android)")
        report.appendLine("Generated: ${Date()}")
        report.appendLine("================================================================================")
        
        // 1. Firebase Initialization
        report.appendLine("\n📱 FIREBASE INITIALIZATION")
        report.appendLine("─────────────────────────────────────────────────────────────────────────────")
        val firebaseApp = FirebaseApp.getInstance()
        if (firebaseApp != null) {
            report.appendLine("✅ Firebase initialized: YES")
            report.appendLine("   App name: ${firebaseApp.name}")
            report.appendLine("   Project ID: ${firebaseApp.options.projectId}")
            report.appendLine("   Application ID: ${firebaseApp.options.applicationId}")
            report.appendLine("   API Key: ${firebaseApp.options.apiKey}")
            report.appendLine("   GCM Sender ID: ${firebaseApp.options.gcmSenderId}")
        } else {
            report.appendLine("❌ Firebase initialized: NO")
        }
        
        // 2. Authentication Status
        report.appendLine("\n🔐 AUTHENTICATION STATUS")
        report.appendLine("─────────────────────────────────────────────────────────────────────────────")
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            report.appendLine("✅ User authenticated: YES")
            report.appendLine("   User ID: ${currentUser.uid}")
            report.appendLine("   Email: ${currentUser.email ?: "nil"}")
            report.appendLine("   Display Name: ${currentUser.displayName ?: "nil"}")
            report.appendLine("   Email Verified: ${currentUser.isEmailVerified}")
            report.appendLine("   Provider: ${currentUser.providerData.firstOrNull()?.providerId ?: "unknown"}")
        } else {
            report.appendLine("❌ User authenticated: NO")
            
            // Check SharedPreferences for mock auth
            val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            val mockUserId = prefs.getString("currentUserId", null)
            if (mockUserId != null) {
                report.appendLine("⚠️  Mock user found in SharedPreferences: $mockUserId")
            }
        }
        
        // 3. Network Connectivity
        report.appendLine("\n🌐 NETWORK CONNECTIVITY")
        report.appendLine("─────────────────────────────────────────────────────────────────────────────")
        val networkStatus = checkNetworkConnectivity(context)
        report.appendLine("   Status: $networkStatus")
        
        // 4. Firestore Configuration
        report.appendLine("\n🔥 FIRESTORE CONFIGURATION")
        report.appendLine("─────────────────────────────────────────────────────────────────────────────")
        val firestore = FirebaseFirestore.getInstance()
        val settings = firestore.firestoreSettings
        report.appendLine("   Persistence enabled: ${settings.isPersistenceEnabled}")
        report.appendLine("   Cache size: ${settings.cacheSizeBytes} bytes")
        report.appendLine("   Host: ${settings.host}")
        report.appendLine("   SSL enabled: ${settings.isSslEnabled}")
        
        // 5. Device Information
        report.appendLine("\n📱 DEVICE INFORMATION")
        report.appendLine("─────────────────────────────────────────────────────────────────────────────")
        report.appendLine("   Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        report.appendLine("   Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        report.appendLine("   App Version: ${getAppVersion(context)}")
        val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        report.appendLine("   Build Type: ${if (isDebuggable) "debug" else "release"}")
        
        report.appendLine("\n================================================================================")
        
        return report.toString()
    }
    
    /**
     * Check network connectivity status.
     */
    private fun checkNetworkConnectivity(context: Context): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return "Unknown"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return "Offline"
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "Offline"
            
            return when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Connected (WiFi)"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Connected (Cellular)"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Connected (Ethernet)"
                else -> "Connected (Unknown)"
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            return if (networkInfo?.isConnected == true) {
                when (networkInfo.type) {
                    ConnectivityManager.TYPE_WIFI -> "Connected (WiFi)"
                    ConnectivityManager.TYPE_MOBILE -> "Connected (Cellular)"
                    else -> "Connected (Unknown)"
                }
            } else {
                "Offline"
            }
        }
    }
    
    /**
     * Get app version from package info.
     */
    private fun getAppVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${packageInfo.versionName} (${packageInfo.versionCode})"
        } catch (e: Exception) {
            "unknown"
        }
    }
    
    /**
     * Save diagnostic report to a file.
     * Returns the file path if successful.
     */
    fun saveReportToFile(context: Context): String? {
        val report = generateReport(context)
        
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "firebase_diagnostics_$timestamp.txt"
        val file = File(context.getExternalFilesDir(null), fileName)
        
        return try {
            file.writeText(report)
            Log.d(TAG, "📄 Diagnostic report saved to: ${file.absolutePath}")
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to save diagnostic report: ${e.message}", e)
            null
        }
    }
    
    /**
     * Print diagnostic report to logcat.
     */
    fun printReport(context: Context) {
        val report = generateReport(context)
        // Split by lines and log each line separately to avoid logcat truncation
        report.lines().forEach { line ->
            Log.d(TAG, line)
        }
    }
    
    /**
     * Run minimal integrity test: write and read a test document to verify Firebase connectivity.
     */
    fun runIntegrityTest(context: Context) {
        Log.d(TAG, "🧪 Running Firebase integrity test...")
        
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        
        if (currentUser == null) {
            Log.w(TAG, "⚠️ Integrity test skipped - no authenticated user")
            return
        }
        
        val userId = currentUser.uid
        val timestamp = System.currentTimeMillis()
        val deviceId = Build.MODEL.replace(" ", "_")
        val testDocId = "${timestamp}_$deviceId"
        
        val testData = hashMapOf(
            "source" to "android",
            "ts" to timestamp,
            "deviceId" to deviceId,
            "testId" to testDocId
        )
        
        val firestore = FirebaseFirestore.getInstance()
        val startTime = System.currentTimeMillis()
        
        // Use coroutine for async operations
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Write test document
                firestore.collection("sync_test")
                    .document(testDocId)
                    .set(testData)
                    .await()
                
                val writeLatency = System.currentTimeMillis() - startTime
                Log.d(TAG, "✅ Integrity test - write successful (${writeLatency}ms)")
                
                // Read test document back
                val readStartTime = System.currentTimeMillis()
                val snapshot = firestore.collection("sync_test")
                    .document(testDocId)
                    .get()
                    .await()
                
                val readLatency = System.currentTimeMillis() - readStartTime
                
                if (snapshot.exists()) {
                    val totalLatency = System.currentTimeMillis() - startTime
                    Log.d(TAG, "✅ Integrity test PASSED")
                    Log.d(TAG, "🧪 Write latency: ${writeLatency}ms")
                    Log.d(TAG, "🧪 Read latency: ${readLatency}ms")
                    Log.d(TAG, "🧪 Total latency: ${totalLatency}ms")
                    Log.d(TAG, "🧪 Test data: ${snapshot.data}")
                } else {
                    Log.e(TAG, "❌ Integrity test FAILED - document not found")
                    Log.d(TAG, "🧪 Read latency: ${readLatency}ms")
                }
            } catch (e: Exception) {
                val latency = System.currentTimeMillis() - startTime
                Log.e(TAG, "❌ Integrity test FAILED - error: ${e.message}")
                Log.d(TAG, "🧪 Latency: ${latency}ms")
            }
        }
    }
}
