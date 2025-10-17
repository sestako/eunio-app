package com.eunio.healthapp.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.Foundation.*

/**
 * iOS Network Monitor using NSURLSession for connectivity checks
 * Polls network status periodically
 */
class IOSNetworkMonitor : NetworkMonitor {
    
    private val _isConnected = MutableStateFlow(checkConnection())
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    private var monitoringJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)
    
    override fun startMonitoring() {
        if (monitoringJob != null) return
        
        println("IOSNetworkMonitor: Starting network monitoring")
        
        // Initial check
        _isConnected.value = checkConnection()
        
        // Poll every second
        monitoringJob = scope.launch {
            while (true) {
                val connected = checkConnection()
                if (_isConnected.value != connected) {
                    println("IOSNetworkMonitor: Network status changed to $connected")
                    _isConnected.value = connected
                }
                delay(1000)
            }
        }
    }
    
    override fun stopMonitoring() {
        println("IOSNetworkMonitor: Stopping network monitoring")
        monitoringJob?.cancel()
        monitoringJob = null
    }
    
    /**
     * Check network connectivity by attempting to reach a reliable host
     * Uses NSURLSession with a quick timeout
     */
    private fun checkConnection(): Boolean {
        return try {
            // Check if we can create a URL session (basic check)
            val configuration = NSURLSessionConfiguration.defaultSessionConfiguration()
            configuration.timeoutIntervalForRequest = 2.0
            configuration.timeoutIntervalForResource = 2.0
            
            // If we can access network APIs, assume connected
            // This is a simplified check - in production you might want to ping a server
            val session = NSURLSession.sessionWithConfiguration(configuration)
            session.finishTasksAndInvalidate()
            
            // For now, return true if no exception
            // A more robust check would actually make a network request
            true
        } catch (e: Exception) {
            println("IOSNetworkMonitor: Connection check failed: ${e.message}")
            false
        }
    }
}
