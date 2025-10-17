package com.eunio.healthapp.platform

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.datetime.Instant

/**
 * Android-specific platform optimizations for performance and user experience
 */
class AndroidPlatformOptimizations(private val context: Context) : PlatformManager {
    
    /**
     * Configure edge-to-edge display for modern Android versions
     */
    fun configureEdgeToEdge(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowCompat.setDecorFitsSystemWindows(activity.window, false)
            
            val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
            controller.hide(WindowInsetsCompat.Type.statusBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
    
    /**
     * Optimize memory usage for large datasets
     */
    fun optimizeMemoryUsage() {
        // Enable hardware acceleration for better performance
        System.setProperty("android.graphics.hardware_acceleration", "true")
        
        // Optimize garbage collection
        System.gc()
    }
    
    /**
     * Configure secure screen settings for health data
     */
    fun configureSecureScreen(activity: Activity) {
        // Prevent screenshots in secure mode
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }
    
    /**
     * Get device-specific performance metrics
     */
    fun getAndroidPerformanceMetrics(): AndroidPerformanceMetrics {
        val runtime = Runtime.getRuntime()
        return AndroidPerformanceMetrics(
            totalMemory = runtime.totalMemory(),
            freeMemory = runtime.freeMemory(),
            maxMemory = runtime.maxMemory(),
            availableProcessors = runtime.availableProcessors(),
            sdkVersion = Build.VERSION.SDK_INT,
            deviceModel = Build.MODEL,
            manufacturer = Build.MANUFACTURER
        )
    }
    
    // PlatformManager implementation
    override fun optimizePerformance() {
        optimizeMemoryUsage()
    }
    
    override fun getPerformanceMetrics(): PlatformPerformanceMetrics {
        val androidMetrics = getAndroidPerformanceMetrics()
        return AndroidPlatformPerformanceMetrics(androidMetrics)
    }
    
    override fun configureSecurityFeatures() {
        // Android-specific security configuration
    }
    
    override fun handleDeepLink(url: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override fun shareContent(content: String, title: String) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, content)
            putExtra(Intent.EXTRA_SUBJECT, title)
        }
        
        val chooserIntent = Intent.createChooser(shareIntent, title)
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooserIntent)
    }
    
    override fun openDocumentPicker() {
        // This would need to be handled by the Activity
        // Implementation depends on activity context
    }
    
    // Device and app information methods for support
    override fun getPlatformName(): String = "Android"
    
    override fun getOSVersion(): String = Build.VERSION.RELEASE
    
    override fun getDeviceModel(): String = "${Build.MANUFACTURER} ${Build.MODEL}"
    
    override fun getScreenSize(): String {
        val displayMetrics = context.resources.displayMetrics
        return "${displayMetrics.widthPixels}x${displayMetrics.heightPixels}"
    }
    
    override fun getLocale(): String = context.resources.configuration.locales[0].toString()
    
    override fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    override fun getBuildNumber(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toString()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toString()
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    override fun getInstallDate(): kotlinx.datetime.Instant? {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            kotlinx.datetime.Instant.fromEpochMilliseconds(packageInfo.firstInstallTime)
        } catch (e: Exception) {
            null
        }
    }
    
    override fun getLastUpdateDate(): kotlinx.datetime.Instant? {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            kotlinx.datetime.Instant.fromEpochMilliseconds(packageInfo.lastUpdateTime)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Configure Android-specific performance optimizations
     */
    fun configureAndroidOptimizations() {
        // Enable strict mode for development builds
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            configureStrictMode()
        }
        
        // Configure memory management
        configureMemoryManagement()
        
        // Configure network optimizations
        configureNetworkOptimizations()
    }
    
    private fun configureStrictMode() {
        // Configure StrictMode for detecting performance issues
        // This would be enabled in debug builds only
    }
    
    private fun configureMemoryManagement() {
        // Configure Android-specific memory management
        // Set up memory pressure callbacks
    }
    
    private fun configureNetworkOptimizations() {
        // Configure network request batching and caching
        // Set up connection pooling
    }
    
    /**
     * Handle Android-specific lifecycle events
     */
    fun handleAndroidLifecycleEvent(event: AndroidLifecycleEvent) {
        when (event) {
            AndroidLifecycleEvent.ON_CREATE -> handleOnCreate()
            AndroidLifecycleEvent.ON_START -> handleOnStart()
            AndroidLifecycleEvent.ON_RESUME -> handleOnResume()
            AndroidLifecycleEvent.ON_PAUSE -> handleOnPause()
            AndroidLifecycleEvent.ON_STOP -> handleOnStop()
            AndroidLifecycleEvent.ON_DESTROY -> handleOnDestroy()
            AndroidLifecycleEvent.ON_LOW_MEMORY -> handleOnLowMemory()
        }
    }
    
    private fun handleOnCreate() {
        // Initialize Android-specific components
    }
    
    private fun handleOnStart() {
        // Resume sync operations
    }
    
    private fun handleOnResume() {
        // Resume all operations
    }
    
    private fun handleOnPause() {
        // Pause non-critical operations
    }
    
    private fun handleOnStop() {
        // Save state and pause operations
    }
    
    private fun handleOnDestroy() {
        // Cleanup resources
    }
    
    private fun handleOnLowMemory() {
        // Handle low memory situations
        optimizeMemoryUsage()
    }
}

enum class AndroidLifecycleEvent {
    ON_CREATE,
    ON_START,
    ON_RESUME,
    ON_PAUSE,
    ON_STOP,
    ON_DESTROY,
    ON_LOW_MEMORY
}

data class AndroidPerformanceMetrics(
    val totalMemory: Long,
    val freeMemory: Long,
    val maxMemory: Long,
    val availableProcessors: Int,
    val sdkVersion: Int,
    val deviceModel: String,
    val manufacturer: String
)

class AndroidPlatformPerformanceMetrics(
    private val androidMetrics: AndroidPerformanceMetrics
) : PlatformPerformanceMetrics {
    override val deviceModel: String = androidMetrics.deviceModel
    override val systemVersion: String = androidMetrics.sdkVersion.toString()
    override val availableMemory: Long = androidMetrics.freeMemory
    override val totalMemory: Long = androidMetrics.totalMemory
    override val processorCount: Int = androidMetrics.availableProcessors
}