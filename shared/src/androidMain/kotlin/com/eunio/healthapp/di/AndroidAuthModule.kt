package com.eunio.healthapp.di

import com.eunio.healthapp.auth.AndroidAuthService
import com.eunio.healthapp.auth.AuthService
import com.eunio.healthapp.services.AnalyticsService
import com.eunio.healthapp.services.AndroidAnalyticsService
import com.eunio.healthapp.services.AndroidCrashlyticsService
import com.eunio.healthapp.services.CrashlyticsService
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Android-specific auth module
 */
val androidAuthModule = module {
    single<AnalyticsService> { AndroidAnalyticsService(androidContext()) }
    single<CrashlyticsService> { AndroidCrashlyticsService() }
    single<AuthService> { AndroidAuthService(get(), get()) }
}
