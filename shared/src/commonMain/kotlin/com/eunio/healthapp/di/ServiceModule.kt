package com.eunio.healthapp.di

import org.koin.dsl.module

/**
 * Koin dependency injection module for core services.
 * This module is intentionally empty as services are provided by the testModule
 * when running tests, and by platform-specific modules in production.
 */
val serviceModule = module {
    // Services are provided by:
    // - testModule for test environments
    // - platform-specific modules (AndroidModule, IOSModule) for production
    // This module exists to maintain module structure consistency
}