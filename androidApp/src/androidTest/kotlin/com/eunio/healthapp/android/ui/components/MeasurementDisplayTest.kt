package com.eunio.healthapp.android.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eunio.healthapp.android.ui.components.*
import com.eunio.healthapp.domain.model.UnitSystem
import com.eunio.healthapp.domain.util.UnitConverter
import com.eunio.healthapp.domain.util.UnitConverterImpl
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.compose.KoinApplication
import org.koin.dsl.module

/**
 * UI tests for measurement display components.
 * Tests the display, formatting, and unit conversion functionality.
 */
@RunWith(AndroidJUnit4::class)
class MeasurementDisplayTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    private val testModule = module {
        single<UnitConverter> { UnitConverterImpl() }
    }
    
    @Test
    fun weightDisplay_showsCorrectMetricValue() {
        composeTestRule.setContent {
            KoinApplication(application = { modules(testModule) }) {
                WeightDisplay(
                    weightInKg = 70.0,
                    unitSystem = UnitSystem.METRIC
                )
            }
        }
        
        composeTestRule.onNodeWithText("70 kg").assertIsDisplayed()
    }
    
    @Test
    fun weightDisplay_showsCorrectImperialValue() {
        composeTestRule.setContent {
            KoinApplication(application = { modules(testModule) }) {
                WeightDisplay(
                    weightInKg = 70.0,
                    unitSystem = UnitSystem.IMPERIAL
                )
            }
        }
        
        composeTestRule.onNodeWithText("154.32 lbs").assertIsDisplayed()
    }
    
    @Test
    fun distanceDisplay_showsCorrectMetricValue() {
        composeTestRule.setContent {
            KoinApplication(application = { modules(testModule) }) {
                DistanceDisplay(
                    distanceInKm = 5.0,
                    unitSystem = UnitSystem.METRIC
                )
            }
        }
        
        composeTestRule.onNodeWithText("5 km").assertIsDisplayed()
    }
    
    @Test
    fun distanceDisplay_showsCorrectImperialValue() {
        composeTestRule.setContent {
            KoinApplication(application = { modules(testModule) }) {
                DistanceDisplay(
                    distanceInKm = 5.0,
                    unitSystem = UnitSystem.IMPERIAL
                )
            }
        }
        
        composeTestRule.onNodeWithText("3.11 miles").assertIsDisplayed()
    }
    
    @Test
    fun temperatureDisplay_showsCorrectMetricValue() {
        composeTestRule.setContent {
            KoinApplication(application = { modules(testModule) }) {
                TemperatureDisplay(
                    temperatureInCelsius = 36.5,
                    unitSystem = UnitSystem.METRIC
                )
            }
        }
        
        composeTestRule.onNodeWithText("36.5°C").assertIsDisplayed()
    }
    
    @Test
    fun temperatureDisplay_showsCorrectImperialValue() {
        composeTestRule.setContent {
            KoinApplication(application = { modules(testModule) }) {
                TemperatureDisplay(
                    temperatureInCelsius = 36.5,
                    unitSystem = UnitSystem.IMPERIAL
                )
            }
        }
        
        composeTestRule.onNodeWithText("97.7°F").assertIsDisplayed()
    }
    
    @Test
    fun weightDisplay_handlesZeroValue() {
        composeTestRule.setContent {
            KoinApplication(application = { modules(testModule) }) {
                WeightDisplay(
                    weightInKg = 0.0,
                    unitSystem = UnitSystem.METRIC
                )
            }
        }
        
        composeTestRule.onNodeWithText("0 kg").assertIsDisplayed()
    }
    
    @Test
    fun distanceDisplay_handlesDecimalValues() {
        composeTestRule.setContent {
            KoinApplication(application = { modules(testModule) }) {
                DistanceDisplay(
                    distanceInKm = 1.5,
                    unitSystem = UnitSystem.METRIC
                )
            }
        }
        
        composeTestRule.onNodeWithText("1.5 km").assertIsDisplayed()
    }
    
    @Test
    fun temperatureDisplay_handlesNegativeValues() {
        composeTestRule.setContent {
            KoinApplication(application = { modules(testModule) }) {
                TemperatureDisplay(
                    temperatureInCelsius = -10.0,
                    unitSystem = UnitSystem.METRIC
                )
            }
        }
        
        composeTestRule.onNodeWithText("-10°C").assertIsDisplayed()
    }
    
    @Test
    fun temperatureDisplay_handlesNegativeImperialValues() {
        composeTestRule.setContent {
            KoinApplication(application = { modules(testModule) }) {
                TemperatureDisplay(
                    temperatureInCelsius = -10.0,
                    unitSystem = UnitSystem.IMPERIAL
                )
            }
        }
        
        composeTestRule.onNodeWithText("14°F").assertIsDisplayed()
    }
}