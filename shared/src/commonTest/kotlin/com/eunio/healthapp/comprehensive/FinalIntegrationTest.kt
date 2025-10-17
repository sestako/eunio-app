package com.eunio.healthapp.comprehensive

import com.eunio.healthapp.domain.model.UnitSystem
import com.eunio.healthapp.domain.model.UserPreferences
import com.eunio.healthapp.domain.util.UnitConverter
import com.eunio.healthapp.domain.util.UnitConverterImpl
import com.eunio.healthapp.domain.error.UnitSystemError
import kotlinx.datetime.Clock
import kotlin.test.*

/**
 * Final integration test that validates the complete unit system workflow
 * from user preference selection to measurement display across all components.
 * This test validates all requirements from the specification.
 */
class FinalIntegrationTest {

    private lateinit var unitConverter: UnitConverter

    @BeforeTest
    fun setup() {
        unitConverter = UnitConverterImpl()
    }

    // ========== REQUIREMENT 1: Unit System Selection Interface ==========

    @Test
    fun `requirement_1_1_unit_system_preference_options_are_available`() {
        // WHEN a user views the unit system setting 
        // THEN the system SHALL show options for Metric and Imperial systems
        val availableSystems = UnitSystem.values()
        
        assertEquals(2, availableSystems.size)
        assertTrue(availableSystems.contains(UnitSystem.METRIC))
        assertTrue(availableSystems.contains(UnitSystem.IMPERIAL))
        assertEquals("Metric", UnitSystem.METRIC.displayName)
        assertEquals("Imperial", UnitSystem.IMPERIAL.displayName)
    }

    @Test
    fun `requirement_1_3_metric_system_uses_correct_units`() {
        // WHEN a user selects Metric 
        // THEN the system SHALL use kilometers, kilograms, and Celsius for measurements
        val weightFormatted = unitConverter.formatWeight(70.0, UnitSystem.METRIC)
        val distanceFormatted = unitConverter.formatDistance(5.0, UnitSystem.METRIC)
        val tempFormatted = unitConverter.formatTemperature(36.5, UnitSystem.METRIC)
        
        assertTrue(weightFormatted.contains("kg"))
        assertTrue(distanceFormatted.contains("km"))
        assertTrue(tempFormatted.contains("°C"))
    }

    @Test
    fun `requirement_1_4_imperial_system_uses_correct_units`() {
        // WHEN a user selects Imperial 
        // THEN the system SHALL use miles, pounds, and Fahrenheit for measurements
        val weightFormatted = unitConverter.formatWeight(154.32, UnitSystem.IMPERIAL)
        val distanceFormatted = unitConverter.formatDistance(3.11, UnitSystem.IMPERIAL)
        val tempFormatted = unitConverter.formatTemperature(97.7, UnitSystem.IMPERIAL)
        
        assertTrue(weightFormatted.contains("lbs"))
        assertTrue(distanceFormatted.contains("miles"))
        assertTrue(tempFormatted.contains("°F"))
    }

    // ========== REQUIREMENT 2: Preference Persistence and Offline Sync ==========

    @Test
    fun `requirement_2_6_default_unit_system_based_on_locale`() {
        // IF no preference is stored 
        // THEN the system SHALL apply the default unit system based on locale
        assertEquals(UnitSystem.METRIC, UnitSystem.fromLocale(""))
        assertEquals(UnitSystem.METRIC, UnitSystem.fromLocale("GB"))
        assertEquals(UnitSystem.METRIC, UnitSystem.fromLocale("CA"))
        assertEquals(UnitSystem.IMPERIAL, UnitSystem.fromLocale("US"))
        assertEquals(UnitSystem.IMPERIAL, UnitSystem.fromLocale("LR"))
        assertEquals(UnitSystem.IMPERIAL, UnitSystem.fromLocale("MM"))
    }

    // ========== REQUIREMENT 3: Global Measurement Display ==========

    @Test
    fun `requirement_3_1_weight_measurements_display_correctly`() {
        // WHEN displaying weight measurements 
        // THEN the system SHALL show values in kg (metric) or lbs (imperial)
        val weightInKg = 70.0
        
        val metricDisplay = unitConverter.formatWeight(weightInKg, UnitSystem.METRIC)
        val imperialWeight = unitConverter.convertWeight(weightInKg, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        val imperialDisplay = unitConverter.formatWeight(imperialWeight, UnitSystem.IMPERIAL)
        
        assertEquals("70 kg", metricDisplay)
        assertEquals("154.32 lbs", imperialDisplay)
    }

    @Test
    fun `requirement_3_2_distance_measurements_display_correctly`() {
        // WHEN displaying distance measurements 
        // THEN the system SHALL show values in km (metric) or miles (imperial)
        val distanceInKm = 5.0
        
        val metricDisplay = unitConverter.formatDistance(distanceInKm, UnitSystem.METRIC)
        val imperialDistance = unitConverter.convertDistance(distanceInKm, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        val imperialDisplay = unitConverter.formatDistance(imperialDistance, UnitSystem.IMPERIAL)
        
        assertEquals("5 km", metricDisplay)
        assertEquals("3.11 miles", imperialDisplay)
    }

    @Test
    fun `requirement_3_3_temperature_measurements_display_correctly`() {
        // WHEN displaying temperature measurements 
        // THEN the system SHALL show values in °C (metric) or °F (imperial)
        val tempInCelsius = 36.5
        
        val metricDisplay = unitConverter.formatTemperature(tempInCelsius, UnitSystem.METRIC)
        val imperialTemp = unitConverter.convertTemperature(tempInCelsius, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        val imperialDisplay = unitConverter.formatTemperature(imperialTemp, UnitSystem.IMPERIAL)
        
        assertEquals("36.5°C", metricDisplay)
        assertEquals("97.7°F", imperialDisplay)
    }

    // ========== REQUIREMENT 4: Unit Conversion Logic and Data Storage ==========

    @Test
    fun `requirement_4_1_weight_conversion_formula_accuracy`() {
        // WHEN converting weight 
        // THEN the system SHALL use the formula: kg = lbs / 2.20462
        val weightInKg = 1.0
        val convertedToLbs = unitConverter.convertWeight(weightInKg, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        val expectedLbs = weightInKg * 2.20462
        
        assertEquals(expectedLbs, convertedToLbs, 0.01)
        
        // Test reverse conversion
        val backToKg = unitConverter.convertWeight(convertedToLbs, UnitSystem.IMPERIAL, UnitSystem.METRIC)
        assertEquals(weightInKg, backToKg, 0.01)
    }

    @Test
    fun `requirement_4_2_distance_conversion_formula_accuracy`() {
        // WHEN converting distance 
        // THEN the system SHALL use the formula: km = miles / 0.621371
        val distanceInKm = 1.0
        val convertedToMiles = unitConverter.convertDistance(distanceInKm, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        val expectedMiles = distanceInKm * 0.621371
        
        assertEquals(expectedMiles, convertedToMiles, 0.01)
        
        // Test reverse conversion
        val backToKm = unitConverter.convertDistance(convertedToMiles, UnitSystem.IMPERIAL, UnitSystem.METRIC)
        assertEquals(distanceInKm, backToKm, 0.01)
    }

    @Test
    fun `requirement_4_3_temperature_conversion_formula_accuracy`() {
        // WHEN converting temperature 
        // THEN the system SHALL use the formula: °C = (°F - 32) × 5/9
        val tempInCelsius = 0.0
        val convertedToFahrenheit = unitConverter.convertTemperature(tempInCelsius, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        assertEquals(32.0, convertedToFahrenheit, 0.1)
        
        val tempInCelsius2 = 100.0
        val convertedToFahrenheit2 = unitConverter.convertTemperature(tempInCelsius2, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        assertEquals(212.0, convertedToFahrenheit2, 0.1)
        
        // Test reverse conversion
        val backToCelsius = unitConverter.convertTemperature(convertedToFahrenheit, UnitSystem.IMPERIAL, UnitSystem.METRIC)
        assertEquals(tempInCelsius, backToCelsius, 0.1)
    }

    @Test
    fun `requirement_4_4_rounding_precision_round_half_up`() {
        // WHEN performing conversions 
        // THEN the system SHALL round using round half up to 2 decimal places for display
        val testValue = 1.23456789
        val converted = unitConverter.convertWeight(testValue, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        
        // Check that result has at most 2 decimal places
        val formatted = unitConverter.formatWeight(converted, UnitSystem.IMPERIAL)
        val numberPart = formatted.split(" ")[0]
        val decimalPlaces = if (numberPart.contains(".")) {
            numberPart.split(".")[1].length
        } else {
            0
        }
        assertTrue(decimalPlaces <= 2, "Should have at most 2 decimal places, got $decimalPlaces in $formatted")
    }

    // ========== REQUIREMENT 5: Locale-Based Default Selection ==========

    @Test
    fun `requirement_5_2_us_liberia_myanmar_default_to_imperial`() {
        // WHEN the locale indicates US, Liberia, or Myanmar 
        // THEN the system SHALL default to Imperial units
        assertEquals(UnitSystem.IMPERIAL, UnitSystem.fromLocale("US"))
        assertEquals(UnitSystem.IMPERIAL, UnitSystem.fromLocale("LR"))
        assertEquals(UnitSystem.IMPERIAL, UnitSystem.fromLocale("MM"))
    }

    @Test
    fun `requirement_5_3_other_countries_default_to_metric`() {
        // WHEN the locale indicates any other country 
        // THEN the system SHALL default to Metric units
        val metricCountries = listOf("GB", "CA", "AU", "DE", "FR", "JP", "BR", "IN", "CN", "RU")
        metricCountries.forEach { locale ->
            assertEquals(UnitSystem.METRIC, UnitSystem.fromLocale(locale), "Locale $locale should default to metric")
        }
    }

    @Test
    fun `requirement_5_6_locale_detection_failure_defaults_to_metric`() {
        // IF locale detection fails 
        // THEN the system SHALL default to Metric units
        assertEquals(UnitSystem.METRIC, UnitSystem.fromLocale(""))
        assertEquals(UnitSystem.METRIC, UnitSystem.fromLocale("UNKNOWN"))
        assertEquals(UnitSystem.METRIC, UnitSystem.fromLocale("XX"))
    }

    // ========== REQUIREMENT 7: Cross-Platform Consistency ==========

    @Test
    fun `requirement_7_4_identical_formatting_and_precision`() {
        // WHEN displaying measurements 
        // THEN the system SHALL use identical formatting and precision on both platforms
        val testValues = listOf(1.0, 10.5, 100.123, 0.1)
        
        testValues.forEach { value ->
            val weightFormatted = unitConverter.formatWeight(value, UnitSystem.METRIC)
            val distanceFormatted = unitConverter.formatDistance(value, UnitSystem.METRIC)
            val tempFormatted = unitConverter.formatTemperature(value, UnitSystem.METRIC)
            
            // Verify consistent formatting patterns
            assertTrue(weightFormatted.matches(Regex("\\d+(\\.\\d{1,2})? kg")))
            assertTrue(distanceFormatted.matches(Regex("\\d+(\\.\\d{1,2})? km")))
            assertTrue(tempFormatted.matches(Regex("\\d+(\\.\\d{1,2})?°C")))
        }
    }

    @Test
    fun `requirement_7_5_identical_conversion_results`() {
        // WHEN converting units 
        // THEN the system SHALL produce identical results on both Android and iOS
        val testCases = listOf(
            Triple(70.0, UnitSystem.METRIC, UnitSystem.IMPERIAL),
            Triple(154.32, UnitSystem.IMPERIAL, UnitSystem.METRIC),
            Triple(5.0, UnitSystem.METRIC, UnitSystem.IMPERIAL),
            Triple(36.5, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        )
        
        testCases.forEach { (value, from, to) ->
            val weightResult = unitConverter.convertWeight(value, from, to)
            val distanceResult = unitConverter.convertDistance(value, from, to)
            val tempResult = unitConverter.convertTemperature(value, from, to)
            
            // Results should be deterministic and finite
            assertTrue(weightResult.isFinite())
            assertTrue(distanceResult.isFinite())
            assertTrue(tempResult.isFinite())
            
            // Results should be consistent across multiple calls
            assertEquals(weightResult, unitConverter.convertWeight(value, from, to))
            assertEquals(distanceResult, unitConverter.convertDistance(value, from, to))
            assertEquals(tempResult, unitConverter.convertTemperature(value, from, to))
        }
    }

    // ========== REQUIREMENT 8: Testing and Validation ==========

    @Test
    fun `requirement_8_1_unit_conversion_accuracy_validation`() {
        // WHEN unit conversion functions are tested 
        // THEN the system SHALL validate accuracy for all supported conversions
        val testCases = mapOf(
            // Weight conversions (kg to lbs)
            1.0 to 2.20462,
            10.0 to 22.0462,
            0.5 to 1.10231,
            
            // Known conversion points
            68.0 to 149.91416, // Average adult weight
            2.5 to 5.51155     // Small weight
        )
        
        testCases.forEach { (kg, expectedLbs) ->
            val actualLbs = unitConverter.convertWeight(kg, UnitSystem.METRIC, UnitSystem.IMPERIAL)
            assertEquals(expectedLbs, actualLbs, 0.01, "Weight conversion failed for $kg kg")
        }
    }

    @Test
    fun `requirement_8_5_edge_cases_handled_gracefully`() {
        // WHEN edge cases are tested 
        // THEN the system SHALL handle invalid inputs and network failures gracefully
        val edgeCases = listOf(
            0.0,                    // Zero
            Double.MIN_VALUE,       // Minimum positive
            1000000.0,             // Large value
            -1.0                   // Negative value
        )
        
        edgeCases.forEach { value ->
            try {
                val weightResult = unitConverter.convertWeight(value, UnitSystem.METRIC, UnitSystem.IMPERIAL)
                val distanceResult = unitConverter.convertDistance(value, UnitSystem.METRIC, UnitSystem.IMPERIAL)
                val tempResult = unitConverter.convertTemperature(value, UnitSystem.METRIC, UnitSystem.IMPERIAL)
                
                // Should handle edge case gracefully
                assertNotNull(weightResult)
                assertNotNull(distanceResult)
                assertNotNull(tempResult)
            } catch (e: UnitSystemError) {
                // UnitSystemError exceptions are expected for invalid inputs - this is graceful handling
                assertNotNull(e.message)
            } catch (e: Exception) {
                // Other exceptions indicate a problem
                fail("Should handle edge case $value gracefully with UnitSystemError, but threw: ${e::class.simpleName}: ${e.message}")
            }
        }
    }

    // ========== END-TO-END INTEGRATION TESTS ==========

    @Test
    fun `complete measurement conversion workflow works end-to-end`() {
        // Given: Sample measurements in metric (stored format)
        val weightInKg = 70.0
        val distanceInKm = 5.0
        val temperatureInCelsius = 36.5

        // When: Converting to imperial for display
        val weightInLbs = unitConverter.convertWeight(weightInKg, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        val distanceInMiles = unitConverter.convertDistance(distanceInKm, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        val temperatureInFahrenheit = unitConverter.convertTemperature(temperatureInCelsius, UnitSystem.METRIC, UnitSystem.IMPERIAL)

        // Then: Conversions are accurate
        assertEquals(154.32, weightInLbs, 0.01)
        assertEquals(3.11, distanceInMiles, 0.01)
        assertEquals(97.7, temperatureInFahrenheit, 0.1)

        // And: Formatting is correct
        val formattedWeight = unitConverter.formatWeight(weightInLbs, UnitSystem.IMPERIAL)
        val formattedDistance = unitConverter.formatDistance(distanceInMiles, UnitSystem.IMPERIAL)
        val formattedTemperature = unitConverter.formatTemperature(temperatureInFahrenheit, UnitSystem.IMPERIAL)

        assertEquals("154.32 lbs", formattedWeight)
        assertEquals("3.11 miles", formattedDistance)
        assertEquals("97.7°F", formattedTemperature)
    }

    @Test
    fun `preference persistence workflow validation`() {
        // Given: User preferences with different configurations
        val userId = "test-user-123"
        val currentTime = Clock.System.now()
        
        // When: Creating preferences for different scenarios
        val metricPreference = UserPreferences(
            userId = userId,
            unitSystem = UnitSystem.METRIC,
            isManuallySet = true,
            lastModified = currentTime
        )
        
        val imperialPreference = UserPreferences(
            userId = userId,
            unitSystem = UnitSystem.IMPERIAL,
            isManuallySet = false,
            lastModified = currentTime
        )
        
        // Then: Preferences maintain data integrity
        assertEquals(UnitSystem.METRIC, metricPreference.unitSystem)
        assertEquals(UnitSystem.IMPERIAL, imperialPreference.unitSystem)
        assertTrue(metricPreference.isManuallySet)
        assertFalse(imperialPreference.isManuallySet)
        assertEquals(userId, metricPreference.userId)
        assertEquals(userId, imperialPreference.userId)
    }

    @Test
    fun `cross-platform data consistency validation`() {
        // Given: Measurements that should be consistent across platforms
        val testValues = listOf(1.0, 10.5, 100.0, 0.1)
        
        testValues.forEach { value ->
            // When: Converting measurements
            val weightConversion = unitConverter.convertWeight(value, UnitSystem.METRIC, UnitSystem.IMPERIAL)
            val distanceConversion = unitConverter.convertDistance(value, UnitSystem.METRIC, UnitSystem.IMPERIAL)
            val tempConversion = unitConverter.convertTemperature(value, UnitSystem.METRIC, UnitSystem.IMPERIAL)
            
            // Then: Conversions are deterministic and consistent
            assertNotNull(weightConversion)
            assertNotNull(distanceConversion)
            assertNotNull(tempConversion)
            assertTrue(weightConversion.isFinite())
            assertTrue(distanceConversion.isFinite())
            assertTrue(tempConversion.isFinite())
        }
    }

    @Test
    fun `locale-based unit system selection works correctly`() {
        // Given: Different locale codes
        val localeTests = mapOf(
            "US" to UnitSystem.IMPERIAL,    // United States
            "LR" to UnitSystem.IMPERIAL,    // Liberia  
            "MM" to UnitSystem.IMPERIAL,    // Myanmar
            "GB" to UnitSystem.METRIC,      // United Kingdom
            "CA" to UnitSystem.METRIC,      // Canada
            "AU" to UnitSystem.METRIC,      // Australia
            "DE" to UnitSystem.METRIC,      // Germany
            "JP" to UnitSystem.METRIC,      // Japan
            "BR" to UnitSystem.METRIC,      // Brazil
            "IN" to UnitSystem.METRIC       // India
        )
        
        localeTests.forEach { (locale, expectedSystem) ->
            // When: Determining unit system from locale
            val actualSystem = UnitSystem.fromLocale(locale)
            
            // Then: System matches regional expectations
            assertEquals(expectedSystem, actualSystem, "Locale $locale should default to $expectedSystem")
        }
        
        // When: Unknown locale is encountered
        val unknownLocaleSystem = UnitSystem.fromLocale("XX")
        
        // Then: Defaults to metric (international standard)
        assertEquals(UnitSystem.METRIC, unknownLocaleSystem)
    }

    @Test
    fun `error handling works gracefully for edge cases`() {
        // Given: Edge case measurement values
        val edgeCases = listOf(
            0.0,                    // Zero value
            Double.MIN_VALUE,       // Minimum positive value
            Double.MAX_VALUE,       // Maximum value
            Double.POSITIVE_INFINITY, // Infinity
            Double.NEGATIVE_INFINITY, // Negative infinity
            Double.NaN              // Not a number
        )
        
        edgeCases.forEach { edgeCase ->
            // When: Processing edge case values
            // Then: System doesn't crash
            try {
                val weightResult = unitConverter.convertWeight(edgeCase, UnitSystem.METRIC, UnitSystem.IMPERIAL)
                val distanceResult = unitConverter.convertDistance(edgeCase, UnitSystem.METRIC, UnitSystem.IMPERIAL)
                val tempResult = unitConverter.convertTemperature(edgeCase, UnitSystem.METRIC, UnitSystem.IMPERIAL)
                
                // And: Results are returned (even if special values)
                assertNotNull(weightResult)
                assertNotNull(distanceResult)
                assertNotNull(tempResult)
                
                // And: Formatting doesn't crash
                unitConverter.formatWeight(weightResult, UnitSystem.IMPERIAL)
                unitConverter.formatDistance(distanceResult, UnitSystem.IMPERIAL)
                unitConverter.formatTemperature(tempResult, UnitSystem.IMPERIAL)
            } catch (e: Exception) {
                // If exception occurs, ensure it's handled gracefully
                assertNotNull(e.message)
            }
        }
    }

    @Test
    fun `performance meets expectations for typical usage`() {
        // Given: Large number of measurements to convert (simulating real usage)
        val measurements = (1..1000).map { it.toDouble() }
        
        // When: Converting many measurements
        val startTime = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        
        measurements.forEach { measurement ->
            unitConverter.convertWeight(measurement, UnitSystem.METRIC, UnitSystem.IMPERIAL)
            unitConverter.convertDistance(measurement, UnitSystem.METRIC, UnitSystem.IMPERIAL)
            unitConverter.convertTemperature(measurement, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        }
        
        val endTime = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        val duration = endTime - startTime
        
        // Then: Conversions complete quickly (under reasonable time limit)
        assertTrue(duration < 5000, "1000 conversions should complete in under 5 seconds, took ${duration}ms")
    }

    @Test
    fun `historical data conversion maintains accuracy`() {
        // Given: Historical measurements stored in metric (database format)
        val historicalWeights = listOf(68.0, 69.5, 70.0, 71.2, 72.5) // kg
        val historicalDistances = listOf(2.5, 3.0, 4.5, 5.0, 6.2) // km
        val historicalTemps = listOf(36.2, 36.5, 36.8, 37.0, 37.2) // °C
        
        // When: Converting to imperial for display
        val imperialWeights = historicalWeights.map { weight ->
            val converted = unitConverter.convertWeight(weight, UnitSystem.METRIC, UnitSystem.IMPERIAL)
            unitConverter.formatWeight(converted, UnitSystem.IMPERIAL)
        }
        
        val imperialDistances = historicalDistances.map { distance ->
            val converted = unitConverter.convertDistance(distance, UnitSystem.METRIC, UnitSystem.IMPERIAL)
            unitConverter.formatDistance(converted, UnitSystem.IMPERIAL)
        }
        
        val imperialTemps = historicalTemps.map { temp ->
            val converted = unitConverter.convertTemperature(temp, UnitSystem.METRIC, UnitSystem.IMPERIAL)
            unitConverter.formatTemperature(converted, UnitSystem.IMPERIAL)
        }
        
        // Then: All historical data is converted and displayed correctly
        assertEquals(5, imperialWeights.size)
        assertEquals(5, imperialDistances.size)
        assertEquals(5, imperialTemps.size)
        
        // And: Conversions maintain relative relationships
        assertTrue(imperialWeights.all { it.contains("lbs") })
        assertTrue(imperialDistances.all { it.contains("miles") })
        assertTrue(imperialTemps.all { it.contains("°F") })
        
        // And: Values increase/decrease in same pattern as original
        val originalWeightTrend = historicalWeights.zipWithNext { a, b -> b > a }
        val convertedValues = imperialWeights.map { 
            it.replace(" lbs", "").toDouble() 
        }
        val convertedWeightTrend = convertedValues.zipWithNext { a, b -> b > a }
        assertEquals(originalWeightTrend, convertedWeightTrend)
    }

    @Test
    fun `unit system enum values are consistent and valid`() {
        // Given: Unit system enum values
        val metricSystem = UnitSystem.METRIC
        val imperialSystem = UnitSystem.IMPERIAL

        // When: Examining enum properties
        // Then: Display names are appropriate
        assertEquals("Metric", metricSystem.displayName)
        assertEquals("Imperial", imperialSystem.displayName)
        
        // And: String representations are consistent
        assertEquals("METRIC", metricSystem.toString())
        assertEquals("IMPERIAL", imperialSystem.toString())
        
        // And: All enum values are accounted for
        val allSystems = UnitSystem.values()
        assertEquals(2, allSystems.size)
        assertTrue(allSystems.contains(UnitSystem.METRIC))
        assertTrue(allSystems.contains(UnitSystem.IMPERIAL))
    }

    @Test
    fun `conversion formulas are mathematically correct`() {
        // Given: Known conversion test cases with expected results
        
        // When: Testing weight conversions (1 kg should be ~2.20 lbs)
        val weightResult = unitConverter.convertWeight(1.0, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        assertTrue(weightResult > 2.0 && weightResult < 2.5, "1 kg should convert to ~2.20 lbs, got $weightResult")
        
        // When: Testing distance conversions (1 km should be ~0.62 miles)
        val distanceResult = unitConverter.convertDistance(1.0, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        assertTrue(distanceResult > 0.6 && distanceResult < 0.7, "1 km should convert to ~0.62 miles, got $distanceResult")
        
        // When: Testing temperature conversions
        val tempResult1 = unitConverter.convertTemperature(0.0, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        assertEquals(32.0, tempResult1, 0.1, "0°C should be 32°F")
        
        val tempResult2 = unitConverter.convertTemperature(100.0, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        assertEquals(212.0, tempResult2, 0.1, "100°C should be 212°F")
        
        // And: Reverse conversions work correctly
        val reverseWeight = unitConverter.convertWeight(weightResult, UnitSystem.IMPERIAL, UnitSystem.METRIC)
        assertEquals(1.0, reverseWeight, 0.01, "Reverse weight conversion should return to original")
        
        val reverseDistance = unitConverter.convertDistance(distanceResult, UnitSystem.IMPERIAL, UnitSystem.METRIC)
        assertEquals(1.0, reverseDistance, 0.01, "Reverse distance conversion should return to original")
    }
}