package com.eunio.healthapp.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidNetworkMonitor(
    private val context: Context
) : NetworkMonitor {
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val _isConnected = MutableStateFlow(checkInitialConnection())
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d(TAG, "Network available - checking capabilities...")
            // Don't immediately set to true, wait for capabilities check
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            if (capabilities != null) {
                val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                Log.d(TAG, "Network available with internet capability: $hasInternet")
                if (hasInternet) {
                    _isConnected.value = true
                }
            }
        }
        
        override fun onLost(network: Network) {
            Log.d(TAG, "Network lost")
            // Don't immediately mark as offline - check if there's still an active network
            // This handles network switching (e.g., WiFi -> Mobile) gracefully
            val stillConnected = checkInitialConnection()
            Log.d(TAG, "Network lost, but still connected: $stillConnected")
            _isConnected.value = stillConnected
        }
        
        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            val hasInternet = networkCapabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET
            )
            val hasValidated = networkCapabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_VALIDATED
            )
            // Consider connected if has internet capability (don't wait for validation)
            val isConnected = hasInternet
            Log.d(TAG, "Capabilities changed: hasInternet=$hasInternet, hasValidated=$hasValidated, isConnected=$isConnected")
            _isConnected.value = isConnected
        }
    }
    
    override fun startMonitoring() {
        val initialState = checkInitialConnection()
        Log.d(TAG, "Starting network monitoring. Initial state: isConnected=$initialState")
        
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }
    
    override fun stopMonitoring() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            // Already unregistered
        }
    }
    
    private fun checkInitialConnection(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        // Only check for internet capability, not validation
        // This matches the behavior in onCapabilitiesChanged
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    
    companion object {
        private const val TAG = "AndroidNetworkMonitor"
    }
}
