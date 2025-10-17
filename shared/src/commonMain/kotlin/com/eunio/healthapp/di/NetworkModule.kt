package com.eunio.healthapp.di

import com.eunio.healthapp.network.NetworkMonitor
import org.koin.core.module.Module
import org.koin.dsl.module

expect fun platformNetworkModule(): Module

val networkModule = module {
    includes(platformNetworkModule())
}
