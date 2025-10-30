package com.eunio.healthapp.di

import com.eunio.healthapp.data.repository.LogRepositoryImpl
import com.eunio.healthapp.data.sync.SyncManager
import com.eunio.healthapp.network.AndroidNetworkMonitor
import com.eunio.healthapp.network.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Network module for Android platform
 * Provides NetworkMonitor and SyncManager for automatic background sync
 */
actual fun platformNetworkModule(): Module = module {
    // Network Monitor - singleton that monitors connectivity changes
    single<NetworkMonitor> { 
        AndroidNetworkMonitor(androidContext()) 
    }
    
    // Application-level coroutine scope for background operations
    single<CoroutineScope> {
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
    
    // Sync Manager - handles automatic sync when connectivity is restored
    single<SyncManager> {
        SyncManager(
            networkMonitor = get(),
            logRepository = get<com.eunio.healthapp.domain.repository.LogRepository>() as LogRepositoryImpl,
            scope = get()
        )
    }
}
