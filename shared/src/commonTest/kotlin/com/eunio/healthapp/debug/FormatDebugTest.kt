package com.eunio.healthapp.debug

import com.eunio.healthapp.domain.model.UnitSystem
import com.eunio.healthapp.domain.util.UnitConverter
import com.eunio.healthapp.domain.util.UnitConverterImpl
import kotlin.test.*

/**
 * Debug test to check actual formatting output
 */
class FormatDebugTest {

    private val unitConverter: UnitConverter = UnitConverterImpl()

    @Test
    fun `check actual formatting output`() {
        // Test weight formatting
        val weight = 70.0
        val weightFormatted = unitConverter.formatWeight(weight, UnitSystem.METRIC)
        println("Weight formatted: '$weightFormatted'")
        
        // Test distance formatting
        val distance = 5.0
        val distanceFormatted = unitConverter.formatDistance(distance, UnitSystem.METRIC)
        println("Distance formatted: '$distanceFormatted'")
        
        // Test temperature formatting
        val temperature = 36.5
        val temperatureFormatted = unitConverter.formatTemperature(temperature, UnitSystem.METRIC)
        println("Temperature formatted: '$temperatureFormatted'")
        
        // Test imperial conversions
        val imperialWeight = unitConverter.convertWeight(weight, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        val imperialWeightFormatted = unitConverter.formatWeight(imperialWeight, UnitSystem.IMPERIAL)
        println("Imperial weight: $imperialWeight -> '$imperialWeightFormatted'")
        
        val imperialDistance = unitConverter.convertDistance(distance, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        val imperialDistanceFormatted = unitConverter.formatDistance(imperialDistance, UnitSystem.IMPERIAL)
        println("Imperial distance: $imperialDistance -> '$imperialDistanceFormatted'")
        
        val imperialTemp = unitConverter.convertTemperature(temperature, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        val imperialTempFormatted = unitConverter.formatTemperature(imperialTemp, UnitSystem.IMPERIAL)
        println("Imperial temperature: $imperialTemp -> '$imperialTempFormatted'")
        
        // Always pass - this is just for debugging
        assertTrue(true)
    }
}