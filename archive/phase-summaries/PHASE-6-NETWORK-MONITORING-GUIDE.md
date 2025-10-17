# Phase 6.1: Network Status Monitoring

**Time:** 1-2 hours  
**Priority:** HIGH (Foundation for other features)  
**Difficulty:** Medium

---

## 🎯 What We're Building

A cross-platform network monitoring system that:
- ✅ Detects online/offline status
- ✅ Provides reactive state updates
- ✅ Shows UI indicator when offline
- ✅ Triggers sync when connection restored

---

## 📋 Implementation Steps

### Step 1: Create Shared Interface (5 min)

**File:** `shared/src/commonMain/kotlin/com/eunio/healthapp/network/NetworkMonitor.kt`

```kotlin
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
```

---

### Step 2: Android Implementation (30 min)

**File:** `shared/src/androidMain/kotlin/com/eunio/healthapp/network/AndroidNetworkMonitor.kt`

```kotlin
package com.eunio.healthapp.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
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
            _isConnected.value = true
        }
        
        override fun onLost(network: Network) {
            _isConnected.value = false
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
            _isConnected.value = hasInternet && hasValidated
        }
    }
    
    override fun startMonitoring() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }
    
    override fun stopMonitoring() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
    
    private fun checkInitialConnection(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
```

---

### Step 3: iOS Implementation (30 min)

**File:** `shared/src/iosMain/kotlin/com/eunio/healthapp/network/IOSNetworkMonitor.kt`

```kotlin
package com.eunio.healthapp.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Network.*
import platform.darwin.dispatch_queue_create
import platform.darwin.DISPATCH_QUEUE_SERIAL

class IOSNetworkMonitor : NetworkMonitor {
    
    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    private var pathMonitor: NWPathMonitor? = null
    private val monitorQueue = dispatch_queue_create("NetworkMonitor", DISPATCH_QUEUE_SERIAL)
    
    override fun startMonitoring() {
        pathMonitor = NWPathMonitor().apply {
            setPathUpdateHandler { path ->
                _isConnected.value = path.status == NWPathStatusSatisfied
            }
            start(monitorQueue)
        }
    }
    
    override fun stopMonitoring() {
        pathMonitor?.cancel()
        pathMonitor = null
    }
}
```

---

### Step 4: Add to Koin DI (10 min)

**File:** `shared/src/commonMain/kotlin/com/eunio/healthapp/di/NetworkModule.kt`

```kotlin
package com.eunio.healthapp.di

import com.eunio.healthapp.network.NetworkMonitor
import org.koin.core.module.Module
import org.koin.dsl.module

expect fun platformNetworkModule(): Module

val networkModule = module {
    includes(platformNetworkModule())
}
```

**File:** `shared/src/androidMain/kotlin/com/eunio/healthapp/di/NetworkModule.android.kt`

```kotlin
package com.eunio.healthapp.di

import com.eunio.healthapp.network.AndroidNetworkMonitor
import com.eunio.healthapp.network.NetworkMonitor
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformNetworkModule(): Module = module {
    single<NetworkMonitor> { AndroidNetworkMonitor(androidContext()) }
}
```

**File:** `shared/src/iosMain/kotlin/com/eunio/healthapp/di/NetworkModule.ios.kt`

```kotlin
package com.eunio.healthapp.di

import com.eunio.healthapp.network.IOSNetworkMonitor
import com.eunio.healthapp.network.NetworkMonitor
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformNetworkModule(): Module = module {
    single<NetworkMonitor> { IOSNetworkMonitor() }
}
```

---

### Step 5: Add UI Indicator - Android (15 min)

**File:** `androidApp/src/androidMain/kotlin/com/eunio/healthapp/android/ui/components/OfflineBanner.kt`

```kotlin
package com.eunio.healthapp.android.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eunio.healthapp.network.NetworkMonitor
import org.koin.compose.koinInject

@Composable
fun OfflineBanner(
    modifier: Modifier = Modifier,
    networkMonitor: NetworkMonitor = koinInject()
) {
    val isConnected by networkMonitor.isConnected.collectAsState()
    
    AnimatedVisibility(
        visible = !isConnected,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut()
    ) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.errorContainer,
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = "Offline",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "You're offline. Changes will sync when connected.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}
```

---

### Step 6: Add UI Indicator - iOS (15 min)

**File:** `iosApp/iosApp/Views/Components/OfflineBanner.swift`

```swift
import SwiftUI

struct OfflineBanner: View {
    let isConnected: Bool
    
    var body: some View {
        if !isConnected {
            HStack {
                Image(systemName: "wifi.slash")
                    .foregroundColor(.white)
                
                Text("You're offline. Changes will sync when connected.")
                    .font(.subheadline)
                    .foregroundColor(.white)
            }
            .padding()
            .frame(maxWidth: .infinity)
            .background(Color.orange)
            .transition(.move(edge: .top))
        }
    }
}
```

---

### Step 7: Integrate into Main Screens (10 min)

**Android - Add to MainScreen:**
```kotlin
@Composable
fun MainScreen() {
    Column {
        OfflineBanner() // Add this
        
        // Rest of your UI
    }
}
```

**iOS - Add to ContentView:**
```swift
struct ContentView: View {
    @StateObject private var networkMonitor = NetworkMonitorWrapper()
    
    var body: some View {
        VStack(spacing: 0) {
            OfflineBanner(isConnected: networkMonitor.isConnected)
            
            // Rest of your UI
        }
        .onAppear {
            networkMonitor.start()
        }
        .onDisappear {
            networkMonitor.stop()
        }
    }
}
```

---

## 🧪 Testing

### Test Cases

1. **Airplane Mode Test**
   - Turn on airplane mode
   - ✅ Banner appears within 1 second
   - Turn off airplane mode
   - ✅ Banner disappears within 1 second

2. **WiFi Toggle Test**
   - Turn off WiFi
   - ✅ Banner appears
   - Turn on WiFi
   - ✅ Banner disappears

3. **App Lifecycle Test**
   - Go offline
   - Kill app
   - Reopen app
   - ✅ Banner shows immediately

4. **Weak Connection Test**
   - Simulate weak connection
   - ✅ Proper detection (may show offline if no internet)

---

## ✅ Success Criteria

- [ ] Network monitor detects online/offline correctly
- [ ] UI banner appears/disappears smoothly
- [ ] No memory leaks (monitor stopped properly)
- [ ] Works on both Android and iOS
- [ ] Banner text is clear and helpful
- [ ] Animation is smooth

---

## 🎯 Next Steps

After completing network monitoring:

1. **Add Retry Logic** - Use network status to trigger retries
2. **Offline Queue** - Queue operations when offline
3. **Sync Indicator** - Show "Syncing..." when coming online

---

**Ready to implement?** Start with Step 1 and work through each step!

**Estimated Time:** 1-2 hours  
**Difficulty:** Medium  
**Value:** High (foundation for offline support)
