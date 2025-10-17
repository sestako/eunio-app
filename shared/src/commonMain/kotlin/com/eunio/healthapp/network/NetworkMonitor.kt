package com.eunio.healthapp.network

import kotlinx.coroutines.flow.StateFlow

/**
 * Monitors network connectivity status across platforms
 */
interface NetworkMonitor {
    /**
     * Current network connectivity state
     * true = connected, false = offline
     */
    val isConnected: StateFlow<Boolean>
    
    /**
     * Start monitoring network changes
     */
    fun startMonitoring()
    
    /**
     * Stop monitoring network changes
     */
    fun stopMonitoring()
}

/**
 * Network connectivity state
 */
enum class ConnectionType {
    WIFI,
    CELLULAR,
    NONE
}
