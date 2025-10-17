package com.eunio.healthapp.di

import com.eunio.healthapp.network.IOSNetworkMonitor
import com.eunio.healthapp.network.NetworkMonitor
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformNetworkModule(): Module = module {
    single<NetworkMonitor> { IOSNetworkMonitor() }
}
