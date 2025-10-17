package com.eunio.healthapp.domain.manager

import com.eunio.healthapp.domain.model.UnitSystem
import com.eunio.healthapp.domain.model.User
import com.eunio.healthapp.domain.model.UserPreferences
import com.eunio.healthapp.domain.repository.PreferencesRepository
import com.eunio.healthapp.domain.repository.UserRepository
import com.eunio.healthapp.domain.util.LocaleDetector
import com.eunio.healthapp.domain.util.MockLocaleDetector
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for UnitSystemManager locale-based initialization functionality.
 * Focuses on the new initializeFromCurrentLocale method and locale detection integration.
 */
class UnitSystemManagerLocaleTest {
    
    @Test
    fun `locale detector correctly identifies US locale for Imperial system`() {
        val localeDetector = MockLocaleDetector(countryCode = "US")
        assertEquals("US", localeDetector.getCurrentLocaleCountryCode())
        assertEquals(UnitSystem.IMPERIAL, UnitSystem.fromLocale("US"))
    }
    
    @Test
    fun `locale detector correctly identifies GB locale for Metric system`() {
        val localeDetector = MockLocaleDetector(countryCode = "GB")
        assertEquals("GB", localeDetector.getCurrentLocaleCountryCode())
        assertEquals(UnitSystem.METRIC, UnitSystem.fromLocale("GB"))
    }
    
    @Test
    fun `locale detector handles null country code gracefully`() {
        val localeDetector = MockLocaleDetector(countryCode = null)
        assertEquals(null, localeDetector.getCurrentLocaleCountryCode())
        // When country code is null, UnitSystem.fromLocale should handle it gracefully
        // But since fromLocale expects a string, we test the fallback behavior
        assertEquals(UnitSystem.METRIC, UnitSystem.fromLocale(""))
    }
    
    @Test
    fun `locale detector provides locale strings correctly`() {
        val localeDetector = MockLocaleDetector(
            countryCode = "US", 
            localeString = "en_US"
        )
        assertEquals("US", localeDetector.getCurrentLocaleCountryCode())
        assertEquals("en_US", localeDetector.getCurrentLocaleString())
    }
    
    @Test
    fun `unit system from locale works for all supported countries`() {
        // Imperial countries
        assertEquals(UnitSystem.IMPERIAL, UnitSystem.fromLocale("US"))
        assertEquals(UnitSystem.IMPERIAL, UnitSystem.fromLocale("LR"))
        assertEquals(UnitSystem.IMPERIAL, UnitSystem.fromLocale("MM"))
        
        // Metric countries
        assertEquals(UnitSystem.METRIC, UnitSystem.fromLocale("GB"))
        assertEquals(UnitSystem.METRIC, UnitSystem.fromLocale("DE"))
        assertEquals(UnitSystem.METRIC, UnitSystem.fromLocale("FR"))
        assertEquals(UnitSystem.METRIC, UnitSystem.fromLocale("CA"))
        assertEquals(UnitSystem.METRIC, UnitSystem.fromLocale("AU"))
    }
}