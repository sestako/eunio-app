package com.eunio.healthapp.data.network

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.annotation.RequiresPermission
import com.eunio.healthapp.domain.util.NetworkConnectivity
import com.eunio.healthapp.domain.util.NetworkType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Android implementation of NetworkConnectivity interface.
 * Uses ConnectivityManager to monitor network state and perform connectivity tests.
 */
class AndroidNetworkConnectivity(
    private val context: Context
) : NetworkConnectivity {
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    override fun isConnected(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            // Check for internet capability only, not validation
            // Validation can be slow or fail on emulators/certain networks
            // We'll rely on the actual Firebase call to determine if we can reach the server
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.isConnected == true
        }
    }
    
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    override fun observeConnectivity(): Flow<Boolean> = callbackFlow {
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }
            
            override fun onLost(network: Network) {
                trySend(false)
            }
            
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                // Check for internet capability only, not validation
                // This ensures the offline banner updates correctly
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                trySend(hasInternet)
            }
        }
        
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        
        // Send initial state
        trySend(isConnected())
        
        awaitClose {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }.distinctUntilChanged()
    
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    override suspend fun hasStableConnection(): Boolean = withContext(Dispatchers.IO) {
        if (!isConnected()) {
            return@withContext false
        }
        
        return@withContext try {
            // Test connection to Google DNS (reliable and fast)
            val socket = Socket()
            socket.connect(InetSocketAddress("8.8.8.8", 53), 3000)
            socket.close()
            true
        } catch (e: Exception) {
            // Fallback test to Cloudflare DNS
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress("1.1.1.1", 53), 3000)
                socket.close()
                true
            } catch (e: Exception) {
                false
            }
        }
    }
    
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    override fun getNetworkType(): NetworkType {
        if (!isConnected()) {
            return NetworkType.NONE
        }
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            
            when {
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> NetworkType.WIFI
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> NetworkType.CELLULAR
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> NetworkType.ETHERNET
                else -> NetworkType.UNKNOWN
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            when (networkInfo?.type) {
                ConnectivityManager.TYPE_WIFI -> NetworkType.WIFI
                ConnectivityManager.TYPE_MOBILE -> NetworkType.CELLULAR
                ConnectivityManager.TYPE_ETHERNET -> NetworkType.ETHERNET
                else -> NetworkType.UNKNOWN
            }
        }
    }
}