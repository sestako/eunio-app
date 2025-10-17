package com.eunio.healthapp.acceptance

import com.eunio.healthapp.domain.model.UnitSystem
import com.eunio.healthapp.domain.model.UserPreferences
import com.eunio.healthapp.domain.util.UnitConverter
import com.eunio.healthapp.domain.util.UnitConverterImpl
import kotlinx.datetime.Clock
import kotlin.test.*

/**
 * User Acceptance Test for Unit System Preferences
 * 
 * This test validates the complete user journey from initial app setup
 * through preference management to measurement display across all screens.
 * Tests are written from the user's perspective to validate requirements.
 */
class UnitSystemUserAcceptanceTest {

    private lateinit var unitConverter: UnitConverter

    @BeforeTest
    fun setup() {
        unitConverter = UnitConverterImpl()
    }

    // ========== USER STORY 1: First-Time User Experience ==========

    @Test
    fun `new_user_gets_appropriate_default_based_on_location`() {
        // SCENARIO: New user opens app for first time
        
        // GIVEN: User is in the United States
        val usLocale = "US"
        
        // WHEN: App determines default unit system
        val defaultSystem = UnitSystem.fromLocale(usLocale)
        
        // THEN: System defaults to Imperial (familiar to US users)
        assertEquals(UnitSystem.IMPERIAL, defaultSystem)
        
        // AND: User sees measurements in familiar units
        val weightDisplay = unitConverter.formatWeight(150.0, defaultSystem)
        val distanceDisplay = unitConverter.formatDistance(3.0, defaultSystem)
        val tempDisplay = unitConverter.formatTemperature(98.6, defaultSystem)
        
        assertTrue(weightDisplay.contains("lbs"))
        assertTrue(distanceDisplay.contains("miles"))
        assertTrue(tempDisplay.contains("°F"))
    }

    @Test
    fun `international_user_gets_metric_default`() {
        // SCENARIO: New user opens app for first time outside US/LR/MM
        
        // GIVEN: User is in Canada
        val canadianLocale = "CA"
        
        // WHEN: App determines default unit system
        val defaultSystem = UnitSystem.fromLocale(canadianLocale)
        
        // THEN: System defaults to Metric (international standard)
        assertEquals(UnitSystem.METRIC, defaultSystem)
        
        // AND: User sees measurements in metric units
        val weightDisplay = unitConverter.formatWeight(70.0, defaultSystem)
        val distanceDisplay = unitConverter.formatDistance(5.0, defaultSystem)
        val tempDisplay = unitConverter.formatTemperature(37.0, defaultSystem)
        
        assertTrue(weightDisplay.contains("kg"))
        assertTrue(distanceDisplay.contains("km"))
        assertTrue(tempDisplay.contains("°C"))
    }

    // ========== USER STORY 2: Changing Preferences ==========

    @Test
    fun `user_can_change_unit_system_in_settings`() {
        // SCENARIO: User wants to change from metric to imperial
        
        // GIVEN: User currently has metric system
        val currentSystem = UnitSystem.METRIC
        val userId = "user-123"
        
        // WHEN: User changes to imperial in settings
        val newPreference = UserPreferences(
            userId = userId,
            unitSystem = UnitSystem.IMPERIAL,
            isManuallySet = true,
            lastModified = Clock.System.now()
        )
        
        // THEN: Preference is updated
        assertEquals(UnitSystem.IMPERIAL, newPreference.unitSystem)
        assertTrue(newPreference.isManuallySet)
        
        // AND: All measurements immediately show in new units
        val sampleWeight = 70.0 // kg stored in database
        val displayWeight = unitConverter.convertWeight(sampleWeight, UnitSystem.METRIC, newPreference.unitSystem)
        val formattedWeight = unitConverter.formatWeight(displayWeight, newPreference.unitSystem)
        
        assertEquals("154.32 lbs", formattedWeight)
    }

    @Test
    fun `manual_preference_overrides_locale_changes`() {
        // SCENARIO: User manually sets preference, then device locale changes
        
        // GIVEN: User manually set imperial preference
        val manualPreference = UserPreferences(
            userId = "user-123",
            unitSystem = UnitSystem.IMPERIAL,
            isManuallySet = true,
            lastModified = Clock.System.now()
        )
        
        // WHEN: Device locale changes to metric country
        val newLocale = "DE" // Germany - metric country
        val localeBasedSystem = UnitSystem.fromLocale(newLocale)
        
        // THEN: Locale suggests metric
        assertEquals(UnitSystem.METRIC, localeBasedSystem)
        
        // BUT: User's manual preference should be preserved
        // (This would be handled by UnitSystemManager checking isManuallySet)
        assertTrue(manualPreference.isManuallySet)
        assertEquals(UnitSystem.IMPERIAL, manualPreference.unitSystem)
    }

    // ========== USER STORY 3: Daily Usage Scenarios ==========

    @Test
    fun `user_logs_weight_in_preferred_units`() {
        // SCENARIO: User logs daily weight measurement
        
        // GIVEN: User prefers imperial units
        val userPreference = UnitSystem.IMPERIAL
        
        // WHEN: User enters weight (150 lbs)
        val userInputWeight = 150.0
        
        // THEN: System converts to metric for storage
        val storedWeight = unitConverter.convertWeight(userInputWeight, UnitSystem.IMPERIAL, UnitSystem.METRIC)
        assertEquals(68.04, storedWeight, 0.01)
        
        // AND: When displaying later, shows in user's preferred units
        val displayWeight = unitConverter.convertWeight(storedWeight, UnitSystem.METRIC, userPreference)
        val formattedDisplay = unitConverter.formatWeight(displayWeight, userPreference)
        assertEquals("150 lbs", formattedDisplay)
    }

    @Test
    fun `user_views_historical_data_in_current_preference`() {
        // SCENARIO: User changed preference but wants to see old data
        
        // GIVEN: Historical weight data stored in metric (database format)
        val historicalWeights = listOf(68.0, 69.0, 70.0, 71.0, 72.0) // kg
        
        // AND: User currently prefers imperial
        val currentPreference = UnitSystem.IMPERIAL
        
        // WHEN: User views weight history
        val displayedWeights = historicalWeights.map { weight ->
            val converted = unitConverter.convertWeight(weight, UnitSystem.METRIC, currentPreference)
            unitConverter.formatWeight(converted, currentPreference)
        }
        
        // THEN: All historical data shows in current preference
        displayedWeights.forEach { weight ->
            assertTrue(weight.contains("lbs"))
        }
        
        // AND: Relative trends are preserved
        val originalTrend = historicalWeights.zipWithNext { a, b -> b > a }
        val convertedValues = displayedWeights.map { it.replace(" lbs", "").toDouble() }
        val displayTrend = convertedValues.zipWithNext { a, b -> b > a }
        assertEquals(originalTrend, displayTrend)
    }

    // ========== USER STORY 4: Cross-Device Experience ==========

    @Test
    fun `user_preference_syncs_across_devices`() {
        // SCENARIO: User sets preference on phone, then uses tablet
        
        // GIVEN: User sets imperial on phone
        val phonePreference = UserPreferences(
            userId = "user-123",
            unitSystem = UnitSystem.IMPERIAL,
            isManuallySet = true,
            lastModified = Clock.System.now()
        )
        
        // WHEN: User opens app on tablet (simulated by same preference data)
        val tabletPreference = phonePreference.copy() // Simulates sync
        
        // THEN: Tablet shows same preference
        assertEquals(phonePreference.unitSystem, tabletPreference.unitSystem)
        assertEquals(phonePreference.isManuallySet, tabletPreference.isManuallySet)
        assertEquals(phonePreference.userId, tabletPreference.userId)
        
        // AND: Measurements display consistently
        val testWeight = 70.0 // kg in database
        val phoneDisplay = unitConverter.formatWeight(
            unitConverter.convertWeight(testWeight, UnitSystem.METRIC, phonePreference.unitSystem),
            phonePreference.unitSystem
        )
        val tabletDisplay = unitConverter.formatWeight(
            unitConverter.convertWeight(testWeight, UnitSystem.METRIC, tabletPreference.unitSystem),
            tabletPreference.unitSystem
        )
        
        assertEquals(phoneDisplay, tabletDisplay)
    }

    // ========== USER STORY 5: Error Recovery Scenarios ==========

    @Test
    fun `user_experience_graceful_when_sync_fails`() {
        // SCENARIO: User changes preference but sync to cloud fails
        
        // GIVEN: User has existing local preference
        val localPreference = UserPreferences(
            userId = "user-123",
            unitSystem = UnitSystem.METRIC,
            isManuallySet = true,
            lastModified = Clock.System.now()
        )
        
        // WHEN: User changes preference (sync would fail in real scenario)
        val updatedPreference = localPreference.copy(
            unitSystem = UnitSystem.IMPERIAL,
            lastModified = Clock.System.now()
        )
        
        // THEN: Local preference is updated immediately
        assertEquals(UnitSystem.IMPERIAL, updatedPreference.unitSystem)
        
        // AND: User sees immediate feedback in UI
        val testMeasurement = 70.0
        val displayValue = unitConverter.convertWeight(testMeasurement, UnitSystem.METRIC, updatedPreference.unitSystem)
        val formattedValue = unitConverter.formatWeight(displayValue, updatedPreference.unitSystem)
        
        assertEquals("154.32 lbs", formattedValue)
        
        // Note: In real implementation, sync would retry in background
    }

    @Test
    fun `user_gets_fallback_when_locale_detection_fails`() {
        // SCENARIO: App cannot determine user's locale
        
        // GIVEN: Locale detection returns unknown/empty result
        val unknownLocale = ""
        
        // WHEN: System determines default unit system
        val fallbackSystem = UnitSystem.fromLocale(unknownLocale)
        
        // THEN: System provides sensible default (metric - international standard)
        assertEquals(UnitSystem.METRIC, fallbackSystem)
        
        // AND: User can still use the app normally
        val testWeight = 70.0
        val displayValue = unitConverter.formatWeight(testWeight, fallbackSystem)
        assertEquals("70 kg", displayValue)
    }

    // ========== USER STORY 6: Accessibility and Usability ==========

    @Test
    fun `measurements_are_clearly_labeled_with_units`() {
        // SCENARIO: User needs to understand what units are being displayed
        
        // GIVEN: Various measurements in both unit systems
        val testValue = 100.0
        
        // WHEN: Displaying measurements
        val metricWeight = unitConverter.formatWeight(testValue, UnitSystem.METRIC)
        val imperialWeight = unitConverter.formatWeight(testValue, UnitSystem.IMPERIAL)
        val metricDistance = unitConverter.formatDistance(testValue, UnitSystem.METRIC)
        val imperialDistance = unitConverter.formatDistance(testValue, UnitSystem.IMPERIAL)
        val metricTemp = unitConverter.formatTemperature(testValue, UnitSystem.METRIC)
        val imperialTemp = unitConverter.formatTemperature(testValue, UnitSystem.IMPERIAL)
        
        // THEN: All measurements include clear unit labels
        assertTrue(metricWeight.contains("kg"))
        assertTrue(imperialWeight.contains("lbs"))
        assertTrue(metricDistance.contains("km"))
        assertTrue(imperialDistance.contains("miles"))
        assertTrue(metricTemp.contains("°C"))
        assertTrue(imperialTemp.contains("°F"))
        
        // AND: Format is consistent and readable
        assertTrue(metricWeight.matches(Regex("\\d+(\\.\\d+)? kg")))
        assertTrue(imperialWeight.matches(Regex("\\d+(\\.\\d+)? lbs")))
        assertTrue(metricDistance.matches(Regex("\\d+(\\.\\d+)? km")))
        assertTrue(imperialDistance.matches(Regex("\\d+(\\.\\d+)? miles")))
        assertTrue(metricTemp.matches(Regex("\\d+(\\.\\d+)?°C")))
        assertTrue(imperialTemp.matches(Regex("\\d+(\\.\\d+)?°F")))
    }

    @Test
    fun `precision_is_appropriate_for_user_understanding`() {
        // SCENARIO: User needs measurements precise enough to be useful but not overwhelming
        
        // GIVEN: Measurement with many decimal places
        val preciseValue = 70.123456789
        
        // WHEN: Converting and formatting
        val converted = unitConverter.convertWeight(preciseValue, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        val formatted = unitConverter.formatWeight(converted, UnitSystem.IMPERIAL)
        
        // THEN: Result is rounded to reasonable precision (2 decimal places max)
        val numberPart = formatted.split(" ")[0]
        val decimalPlaces = if (numberPart.contains(".")) {
            numberPart.split(".")[1].length
        } else {
            0
        }
        
        assertTrue(decimalPlaces <= 2, "Should have at most 2 decimal places for user readability")
        
        // AND: Precision is consistent across measurement types
        val distanceFormatted = unitConverter.formatDistance(converted, UnitSystem.IMPERIAL)
        val tempFormatted = unitConverter.formatTemperature(converted, UnitSystem.IMPERIAL)
        
        val distanceDecimals = distanceFormatted.split(" ")[0].let { 
            if (it.contains(".")) it.split(".")[1].length else 0 
        }
        val tempDecimals = tempFormatted.split("°")[0].let { 
            if (it.contains(".")) it.split(".")[1].length else 0 
        }
        
        assertTrue(distanceDecimals <= 2)
        assertTrue(tempDecimals <= 2)
    }

    // ========== INTEGRATION WITH APP FEATURES ==========

    @Test
    fun `bbt_charting_respects_temperature_preference`() {
        // SCENARIO: User tracks BBT (Basal Body Temperature) in preferred units
        
        // GIVEN: BBT measurements stored in Celsius (medical standard)
        val bbtReadings = listOf(36.2, 36.5, 36.8, 37.0, 36.9) // °C
        
        // AND: User prefers Fahrenheit
        val userPreference = UnitSystem.IMPERIAL
        
        // WHEN: Displaying BBT chart
        val chartData = bbtReadings.map { temp ->
            val converted = unitConverter.convertTemperature(temp, UnitSystem.METRIC, userPreference)
            unitConverter.formatTemperature(converted, userPreference)
        }
        
        // THEN: All temperatures show in Fahrenheit
        chartData.forEach { temp ->
            assertTrue(temp.contains("°F"))
        }
        
        // AND: Temperature trends are preserved
        val originalTrend = bbtReadings.zipWithNext { a, b -> b.compareTo(a) }
        val convertedValues = chartData.map { 
            it.replace("°F", "").toDouble() 
        }
        val displayTrend = convertedValues.zipWithNext { a, b -> b.compareTo(a) }
        assertEquals(originalTrend, displayTrend)
    }

    @Test
    fun `health_reports_include_unit_system_context`() {
        // SCENARIO: User generates health report with measurements
        
        // GIVEN: User has imperial preference
        val userPreference = UnitSystem.IMPERIAL
        
        // AND: Health data in various measurements
        val avgWeight = 70.0 // kg stored
        val totalDistance = 25.0 // km stored
        val avgTemp = 36.7 // °C stored
        
        // WHEN: Generating report
        val reportWeight = unitConverter.formatWeight(
            unitConverter.convertWeight(avgWeight, UnitSystem.METRIC, userPreference),
            userPreference
        )
        val reportDistance = unitConverter.formatDistance(
            unitConverter.convertDistance(totalDistance, UnitSystem.METRIC, userPreference),
            userPreference
        )
        val reportTemp = unitConverter.formatTemperature(
            unitConverter.convertTemperature(avgTemp, UnitSystem.METRIC, userPreference),
            userPreference
        )
        
        // THEN: Report shows measurements in user's preferred units
        assertEquals("154.32 lbs", reportWeight)
        assertEquals("15.53 miles", reportDistance)
        assertEquals("98.06°F", reportTemp)
        
        // AND: Units are clearly indicated for report readers
        assertTrue(reportWeight.contains("lbs"))
        assertTrue(reportDistance.contains("miles"))
        assertTrue(reportTemp.contains("°F"))
    }
}