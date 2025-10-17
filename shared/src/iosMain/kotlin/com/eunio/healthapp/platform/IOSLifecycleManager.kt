package com.eunio.healthapp.platform

import com.eunio.healthapp.data.sync.BackgroundSyncService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNotificationName
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationWillResignActiveNotification
import platform.UIKit.UIApplicationWillTerminateNotification

/**
 * iOS-specific lifecycle management for background sync and data persistence
 */
class IOSLifecycleManager(
    private val backgroundSyncService: BackgroundSyncService
) : PlatformLifecycleManager {
    
    private val lifecycleScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val notificationCenter = NSNotificationCenter.defaultCenter
    
    init {
        setupNotificationObservers()
    }
    
    private fun setupNotificationObservers() {
        // App became active
        notificationCenter.addObserverForName(
            name = UIApplicationDidBecomeActiveNotification,
            `object` = null,
            queue = null
        ) { _ ->
            onAppBecameActive()
        }
        
        // App will resign active
        notificationCenter.addObserverForName(
            name = UIApplicationWillResignActiveNotification,
            `object` = null,
            queue = null
        ) { _ ->
            onAppWillResignActive()
        }
        
        // App entered background
        notificationCenter.addObserverForName(
            name = UIApplicationDidEnterBackgroundNotification,
            `object` = null,
            queue = null
        ) { _ ->
            onAppEnteredBackground()
        }
        
        // App will terminate
        notificationCenter.addObserverForName(
            name = UIApplicationWillTerminateNotification,
            `object` = null,
            queue = null
        ) { _ ->
            onAppWillTerminate()
        }
    }
    
    private fun onAppBecameActive() {
        lifecycleScope.launch {
            backgroundSyncService.performFullSync()
        }
    }
    
    private fun onAppWillResignActive() {
        lifecycleScope.launch {
            backgroundSyncService.stop()
        }
    }
    
    private fun onAppEnteredBackground() {
        lifecycleScope.launch {
            backgroundSyncService.performFullSync()
        }
    }
    
    private fun onAppWillTerminate() {
        lifecycleScope.launch {
            backgroundSyncService.stop()
        }
        lifecycleScope.cancel()
    }
    
    /**
     * Handle iOS memory warnings
     */
    fun onMemoryWarning() {
        lifecycleScope.launch {
            // Clear non-essential caches
            // This would be implemented with actual cache clearing logic
        }
    }
    
    /**
     * Configure background app refresh
     */
    fun configureBackgroundAppRefresh() {
        // Configure iOS background app refresh capabilities
        // This would integrate with iOS background processing
    }
    
    // PlatformLifecycleManager implementation
    override fun onAppStart() {
        onAppBecameActive()
    }
    
    override fun onAppStop() {
        onAppEnteredBackground()
    }
    
    override fun onAppPause() {
        onAppWillResignActive()
    }
    
    override fun onAppResume() {
        onAppBecameActive()
    }
    
    override fun onLowMemory() {
        onMemoryWarning()
    }
    
    override fun cleanup() {
        notificationCenter.removeObserver(this)
        lifecycleScope.cancel()
    }
}