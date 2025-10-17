package com.eunio.healthapp.domain.util

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * iOS-specific tests for LocaleDetector implementation.
 * Tests the IOSLocaleDetector using Foundation NSLocale API.
 */
class IOSLocaleDetectorTest {
    
    @Test
    fun `IOSLocaleDetector returns country code or null`() {
        val detector = IOSLocaleDetector()
        val countryCode = detector.getCurrentLocaleCountryCode()
        
        // Should either return a valid country code or null
        if (countryCode != null) {
            assertTrue(countryCode.isNotBlank())
            // Country codes should be 2 characters
            assertTrue(countryCode.length == 2)
        }
    }
    
    @Test
    fun `IOSLocaleDetector returns locale string or null`() {
        val detector = IOSLocaleDetector()
        val localeString = detector.getCurrentLocaleString()
        
        // Should either return a valid locale string or null
        if (localeString != null) {
            assertTrue(localeString.isNotBlank())
        }
    }
    
    @Test
    fun `createLocaleDetector returns IOSLocaleDetector on iOS`() {
        val detector = createLocaleDetector()
        assertTrue(detector is IOSLocaleDetector)
    }
    
    @Test
    fun `IOSLocaleDetector handles NSLocale API gracefully`() {
        // Test that the detector doesn't crash when calling NSLocale APIs
        val detector = IOSLocaleDetector()
        
        // These calls should not throw exceptions
        val countryCode = detector.getCurrentLocaleCountryCode()
        val localeString = detector.getCurrentLocaleString()
        
        // Both can be null, but calls should succeed
        assertTrue(true) // If we get here, no exceptions were thrown
    }
}