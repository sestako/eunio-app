package com.eunio.healthapp.domain.util

/**
 * Interface for detecting device locale to determine appropriate unit system defaults.
 * Platform-specific implementations provide locale detection for Android and iOS.
 */
interface LocaleDetector {
    /**
     * Gets the current device locale country code.
     * 
     * @return The country code (e.g., "US", "GB", "DE") or null if detection fails
     */
    fun getCurrentLocaleCountryCode(): String?
    
    /**
     * Gets the full locale string for debugging purposes.
     * 
     * @return The full locale string (e.g., "en_US", "en_GB") or null if detection fails
     */
    fun getCurrentLocaleString(): String?
}

/**
 * Expected platform-specific implementation of LocaleDetector.
 */
expect fun createLocaleDetector(): LocaleDetector