package com.eunio.healthapp.di

import org.koin.dsl.module

/**
 * Koin module for authentication services
 * Platform-specific implementations are provided via expect/actual
 */
val authModule = module {
    // AuthService is provided by platform-specific modules
    // Android: AndroidAuthService
    // iOS: IOSAuthService
}
