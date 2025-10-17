package com.eunio.healthapp.platform

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.eunio.healthapp.data.sync.BackgroundSyncService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Android-specific lifecycle management for background sync and data persistence
 */
class AndroidLifecycleManager(
    private val backgroundSyncService: BackgroundSyncService
) : DefaultLifecycleObserver, PlatformLifecycleManager {
    
    private val lifecycleScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    fun initialize() {
        // Initialize lifecycle observer
        // In a real implementation, this would be called from the Application class
    }
    
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        // App moved to foreground - resume sync operations
        lifecycleScope.launch {
            backgroundSyncService.performFullSync()
        }
    }
    
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        // App moved to background - trigger background sync
        lifecycleScope.launch {
            backgroundSyncService.performFullSync()
        }
    }
    
    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        lifecycleScope.cancel()
    }
    
    /**
     * Handle low memory situations
     */
    fun handleLowMemory() {
        lifecycleScope.launch {
            // Clear non-essential caches
            // This would be implemented with actual cache clearing logic
        }
    }
    
    /**
     * Handle configuration changes (rotation, etc.)
     */
    fun onConfigurationChanged() {
        // Preserve critical state during configuration changes
        lifecycleScope.launch {
            // This would be implemented with actual state preservation logic
        }
    }
    
    // PlatformLifecycleManager implementation
    override fun onAppStart() {
        lifecycleScope.launch {
            backgroundSyncService.performFullSync()
        }
    }
    
    override fun onAppStop() {
        lifecycleScope.launch {
            backgroundSyncService.performFullSync()
        }
    }
    
    override fun onAppPause() {
        lifecycleScope.launch {
            backgroundSyncService.stop()
        }
    }
    
    override fun onAppResume() {
        lifecycleScope.launch {
            backgroundSyncService.performFullSync()
        }
    }
    
    override fun onLowMemory() {
        handleLowMemory()
    }
    
    override fun cleanup() {
        lifecycleScope.cancel()
    }
}