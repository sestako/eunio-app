package com.eunio.healthapp.domain.manager

import com.eunio.healthapp.domain.model.UnitSystem
import com.eunio.healthapp.domain.util.LocaleDetector
import com.eunio.healthapp.domain.util.MockLocaleDetector
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Mock UnitSystemManager for testing
 */
class MockUnitSystemManager : UnitSystemManager {
    var currentUnitSystem = UnitSystem.METRIC
    var setUnitSystemResult: Result<Unit> = Result.success(Unit)
    var setUnitSystemCalls = mutableListOf<Pair<UnitSystem, Boolean>>()
    
    override suspend fun getCurrentUnitSystem(): UnitSystem = currentUnitSystem
    
    override suspend fun setUnitSystem(unitSystem: UnitSystem, isManuallySet: Boolean): Result<Unit> {
        setUnitSystemCalls.add(unitSystem to isManuallySet)
        if (setUnitSystemResult.isSuccess) {
            currentUnitSystem = unitSystem
        }
        return setUnitSystemResult
    }
    
    override suspend fun initializeFromLocale(locale: String): UnitSystem {
        val system = UnitSystem.fromLocale(locale)
        currentUnitSystem = system
        return system
    }
    
    override suspend fun initializeFromCurrentLocale(): UnitSystem = currentUnitSystem
    
    override fun observeUnitSystemChanges() = kotlinx.coroutines.flow.flowOf(currentUnitSystem)
    
    override suspend fun clearCache() {}
}

/**
 * Tests for UnitSystemInitializer implementation.
 * Covers locale-based initialization, fallback scenarios, and manual preference handling.
 */
class UnitSystemInitializerTest {
    
    private val mockUnitSystemManager = MockUnitSystemManager()
    
    @Test
    fun `initializeForNewUser sets metric for non-US locale`() = runTest {
        val localeDetector = MockLocaleDetector(countryCode = "GB")
        val initializer = UnitSystemInitializerImpl(localeDetector, mockUnitSystemManager)
        
        val result = initializer.initializeForNewUser()
        
        assertTrue(result.isSuccess)
        assertEquals(UnitSystem.METRIC, result.getOrNull())
        assertTrue(mockUnitSystemManager.setUnitSystemCalls.any { it.first == UnitSystem.METRIC && !it.second })
    }
    
    @Test
    fun `initializeForNewUser sets imperial for US locale`() = runTest {
        val localeDetector = MockLocaleDetector(countryCode = "US")
        val initializer = UnitSystemInitializerImpl(localeDetector, mockUnitSystemManager)
        
        val result = initializer.initializeForNewUser()
        
        assertTrue(result.isSuccess)
        assertEquals(UnitSystem.IMPERIAL, result.getOrNull())
        assertTrue(mockUnitSystemManager.setUnitSystemCalls.any { it.first == UnitSystem.IMPERIAL && !it.second })
    }
    
    @Test
    fun `initializeForNewUser falls back to metric when locale detection fails`() = runTest {
        val localeDetector = MockLocaleDetector(countryCode = null)
        val initializer = UnitSystemInitializerImpl(localeDetector, mockUnitSystemManager)
        
        val result = initializer.initializeForNewUser()
        
        assertTrue(result.isSuccess)
        assertEquals(UnitSystem.METRIC, result.getOrNull())
    }
    
    @Test
    fun `getUnitSystemForCurrentLocale returns correct system for various locales`() {
        val testCases = mapOf(
            "US" to UnitSystem.IMPERIAL,
            "LR" to UnitSystem.IMPERIAL,
            "MM" to UnitSystem.IMPERIAL,
            "GB" to UnitSystem.METRIC,
            "DE" to UnitSystem.METRIC,
            "FR" to UnitSystem.METRIC,
            "CA" to UnitSystem.METRIC,
            "AU" to UnitSystem.METRIC,
            null to UnitSystem.METRIC
        )
        
        testCases.forEach { (countryCode, expectedSystem) ->
            val localeDetector = MockLocaleDetector(countryCode = countryCode)
            val initializer = UnitSystemInitializerImpl(localeDetector, mockUnitSystemManager)
            
            val result = initializer.getUnitSystemForCurrentLocale()
            assertEquals(expectedSystem, result, "Failed for country code: $countryCode")
        }
    }
}