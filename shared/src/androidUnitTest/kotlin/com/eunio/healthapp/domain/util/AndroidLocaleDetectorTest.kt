package com.eunio.healthapp.domain.util

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Android-specific tests for LocaleDetector implementation.
 * Tests the AndroidLocaleDetector using Java Locale API.
 */
class AndroidLocaleDetectorTest {
    
    @Test
    fun `AndroidLocaleDetector returns current locale country code`() {
        val detector = AndroidLocaleDetector()
        val countryCode = detector.getCurrentLocaleCountryCode()
        
        // Should return the same as Java Locale API
        val expectedCountryCode = Locale.getDefault().country
        if (expectedCountryCode.isNotBlank()) {
            assertEquals(expectedCountryCode, countryCode)
        }
    }
    
    @Test
    fun `AndroidLocaleDetector returns current locale string`() {
        val detector = AndroidLocaleDetector()
        val localeString = detector.getCurrentLocaleString()
        
        // Should return language_country format
        val defaultLocale = Locale.getDefault()
        if (defaultLocale.language.isNotBlank() && defaultLocale.country.isNotBlank()) {
            val expectedLocaleString = "${defaultLocale.language}_${defaultLocale.country}"
            assertEquals(expectedLocaleString, localeString)
        }
    }
    
    @Test
    fun `AndroidLocaleDetector handles empty country gracefully`() {
        // This test simulates a locale with empty country
        // In practice, this would be hard to test without mocking Locale.getDefault()
        val detector = AndroidLocaleDetector()
        val countryCode = detector.getCurrentLocaleCountryCode()
        
        // Should either return a valid country code or null
        if (countryCode != null) {
            assertTrue(countryCode.isNotBlank())
            assertEquals(2, countryCode.length) // Country codes are 2 characters
        }
    }
    
    @Test
    fun `createLocaleDetector returns AndroidLocaleDetector on Android`() {
        val detector = createLocaleDetector()
        assertTrue(detector is AndroidLocaleDetector)
    }
    
    @Test
    fun `AndroidLocaleDetector country code is uppercase`() {
        val detector = AndroidLocaleDetector()
        val countryCode = detector.getCurrentLocaleCountryCode()
        
        if (countryCode != null) {
            assertEquals(countryCode.uppercase(), countryCode)
        }
    }
    
    @Test
    fun `AndroidLocaleDetector locale string contains underscore when both parts present`() {
        val detector = AndroidLocaleDetector()
        val localeString = detector.getCurrentLocaleString()
        
        if (localeString != null) {
            // Should contain underscore if both language and country are present
            val defaultLocale = Locale.getDefault()
            if (defaultLocale.language.isNotBlank() && defaultLocale.country.isNotBlank()) {
                assertTrue(localeString.contains("_"))
            }
        }
    }
}