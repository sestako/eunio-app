package com.eunio.healthapp.domain.util

import kotlinx.coroutines.flow.Flow

/**
 * Interface for checking network connectivity status and monitoring connectivity changes.
 * Platform-specific implementations should be provided for Android and iOS.
 */
interface NetworkConnectivity {
    
    /**
     * Checks if the device is currently connected to the internet.
     * @return true if connected, false otherwise
     */
    fun isConnected(): Boolean
    
    /**
     * Observes network connectivity changes.
     * Emits true when connected, false when disconnected.
     * @return Flow of connectivity status changes
     */
    fun observeConnectivity(): Flow<Boolean>
    
    /**
     * Checks if the device has a stable internet connection.
     * This may perform a network test beyond just checking connectivity status.
     * @return true if stable connection is available, false otherwise
     */
    suspend fun hasStableConnection(): Boolean
    
    /**
     * Gets the current network type (WiFi, Cellular, etc.)
     * @return NetworkType enum value
     */
    fun getNetworkType(): NetworkType
}

/**
 * Enum representing different types of network connections
 */
enum class NetworkType {
    WIFI,
    CELLULAR,
    ETHERNET,
    UNKNOWN,
    NONE
}