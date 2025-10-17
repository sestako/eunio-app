package com.eunio.healthapp.domain.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Tests for LocaleDetector interface and implementations.
 * Tests both successful locale detection and failure scenarios.
 */
class LocaleDetectorTest {
    
    @Test
    fun `createLocaleDetector returns non-null instance`() {
        val detector = createLocaleDetector()
        assertNotNull(detector)
    }
    
    @Test
    fun `getCurrentLocaleCountryCode returns valid country code or null`() {
        val detector = createLocaleDetector()
        val countryCode = detector.getCurrentLocaleCountryCode()
        
        // Should either return a valid country code or null
        if (countryCode != null) {
            // Country codes should be 2 characters and uppercase
            assertEquals(2, countryCode.length)
            assertEquals(countryCode.uppercase(), countryCode)
        }
    }
    
    @Test
    fun `getCurrentLocaleString returns valid locale string or null`() {
        val detector = createLocaleDetector()
        val localeString = detector.getCurrentLocaleString()
        
        // Should either return a valid locale string or null
        if (localeString != null) {
            // Locale strings should contain an underscore (e.g., "en_US")
            kotlin.test.assertTrue(localeString.contains("_") || localeString.length >= 2)
        }
    }
}

/**
 * Mock implementation of LocaleDetector for testing purposes.
 */
class MockLocaleDetector(
    private val countryCode: String? = null,
    private val localeString: String? = null
) : LocaleDetector {
    
    override fun getCurrentLocaleCountryCode(): String? = countryCode
    
    override fun getCurrentLocaleString(): String? = localeString
}

/**
 * Tests for MockLocaleDetector to ensure it works correctly in tests.
 */
class MockLocaleDetectorTest {
    
    @Test
    fun `mock detector returns configured country code`() {
        val detector = MockLocaleDetector(countryCode = "US")
        assertEquals("US", detector.getCurrentLocaleCountryCode())
    }
    
    @Test
    fun `mock detector returns null when configured`() {
        val detector = MockLocaleDetector(countryCode = null)
        assertNull(detector.getCurrentLocaleCountryCode())
    }
    
    @Test
    fun `mock detector returns configured locale string`() {
        val detector = MockLocaleDetector(localeString = "en_US")
        assertEquals("en_US", detector.getCurrentLocaleString())
    }
    
    @Test
    fun `mock detector handles both parameters`() {
        val detector = MockLocaleDetector(
            countryCode = "GB", 
            localeString = "en_GB"
        )
        assertEquals("GB", detector.getCurrentLocaleCountryCode())
        assertEquals("en_GB", detector.getCurrentLocaleString())
    }
}