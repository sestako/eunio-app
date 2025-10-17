package com.eunio.healthapp.di

import com.eunio.healthapp.auth.AuthService
import com.eunio.healthapp.auth.IOSAuthService
import org.koin.dsl.module

/**
 * iOS-specific auth module
 */
val iosAuthModule = module {
    single<AuthService> { IOSAuthService() }
}
