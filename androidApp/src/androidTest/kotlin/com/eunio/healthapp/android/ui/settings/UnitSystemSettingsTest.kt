package com.eunio.healthapp.android.ui.settings

import com.eunio.healthapp.domain.model.UnitSystem
import org.junit.Assert.*
import org.junit.Test

/**
 * Basic tests for unit system settings components.
 * Tests basic functionality without UI dependencies.
 */
class UnitSystemSettingsTest {
    
    @Test
    fun unitSystem_displayNames_areCorrect() {
        assertEquals("Metric", UnitSystem.METRIC.displayName)
        assertEquals("Imperial", UnitSystem.IMPERIAL.displayName)
    }
    
    @Test
    fun unitSystem_fromLocale_worksCorrectly() {
        assertEquals(UnitSystem.IMPERIAL, UnitSystem.fromLocale("US"))
        assertEquals(UnitSystem.IMPERIAL, UnitSystem.fromLocale("LR"))
        assertEquals(UnitSystem.IMPERIAL, UnitSystem.fromLocale("MM"))
        assertEquals(UnitSystem.METRIC, UnitSystem.fromLocale("GB"))
        assertEquals(UnitSystem.METRIC, UnitSystem.fromLocale("DE"))
        assertEquals(UnitSystem.METRIC, UnitSystem.fromLocale("FR"))
    }
}