package com.eunio.healthapp.android.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eunio.healthapp.android.ui.components.*
import com.eunio.healthapp.domain.model.settings.*
import com.eunio.healthapp.domain.util.EnhancedUnitConverterImpl
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI integration tests for settings impact on Android components.
 * Tests that UI components properly respond to settings changes.
 */
@RunWith(AndroidJUnit4::class)
class SettingsIntegrationUITest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    private val enhancedConverter = EnhancedUnitConverterImpl()
    
    @Test
    fun temperatureDisplay_updatesWhenUnitPreferencesChange() {
        val celsiusTemp = 36.5
        var currentUnit = TemperatureUnit.CELSIUS
        
        composeTestRule.setContent {
            TemperatureDisplay(
                temperatureInCelsius = celsiusTemp,
                temperatureUnit = currentUnit
            )
        }
        
        // Initially shows Celsius
        composeTestRule.onNodeWithText("36.5°C").assertExists()
        
        // Change to Fahrenheit
        currentUnit = TemperatureUnit.FAHRENHEIT
        composeTestRule.setContent {
            TemperatureDisplay(
                temperatureInCelsius = celsiusTemp,
                temperatureUnit = currentUnit
            )
        }
        
        // Should now show Fahrenheit
        composeTestRule.onNodeWithText("97.7°F").assertExists()
    }
    
    @Test
    fun weightDisplay_updatesWhenUnitPreferencesChange() {
        val kgWeight = 70.0
        var currentUnit = WeightUnit.KILOGRAMS
        
        composeTestRule.setContent {
            WeightDisplay(
                weightInKg = kgWeight,
                weightUnit = currentUnit
            )
        }
        
        // Initially shows Kilograms
        composeTestRule.onNodeWithText("70 kg").assertExists()
        
        // Change to Pounds
        currentUnit = WeightUnit.POUNDS
        composeTestRule.setContent {
            WeightDisplay(
                weightInKg = kgWeight,
                weightUnit = currentUnit
            )
        }
        
        // Should now show Pounds
        composeTestRule.onNodeWithText("154.32 lbs").assertExists()
    }
    
    @Test
    fun temperatureInputField_convertsInputCorrectly() {
        var storedValue: Double? = null
        var currentUnit = TemperatureUnit.CELSIUS
        
        composeTestRule.setContent {
            TemperatureInputField(
                value = storedValue,
                onValueChange = { storedValue = it },
                label = "Temperature"
            )
        }
        
        // Input Celsius value
        composeTestRule.onNodeWithText("Temperature").performTextInput("37.0")
        
        // Should store as Celsius (same value)
        assert(storedValue == 37.0)
        
        // Change to Fahrenheit unit
        currentUnit = TemperatureUnit.FAHRENHEIT
        composeTestRule.setContent {
            TemperatureInputField(
                value = storedValue,
                onValueChange = { storedValue = it },
                label = "Temperature"
            )
        }
        
        // Clear and input Fahrenheit value
        composeTestRule.onNodeWithText("Temperature").performTextClearance()
        composeTestRule.onNodeWithText("Temperature").performTextInput("98.6")
        
        // Should convert and store as Celsius
        assert(storedValue != null && kotlin.math.abs(storedValue!! - 37.0) < 0.1)
    }
    
    @Test
    fun weightInputField_convertsInputCorrectly() {
        var storedValue: Double? = null
        
        composeTestRule.setContent {
            WeightInputField(
                value = storedValue,
                onValueChange = { storedValue = it },
                label = "Weight"
            )
        }
        
        // Input weight value
        composeTestRule.onNodeWithText("Weight").performTextInput("70.0")
        
        // Should store as Kilograms (same value for kg input)
        assert(storedValue == 70.0)
    }
    
    @Test
    fun bbtInputField_showsProperRangeGuidance() {
        composeTestRule.setContent {
            BBTInputField(
                value = null,
                onValueChange = { }
            )
        }
        
        // Should show BBT-specific guidance
        composeTestRule.onNodeWithText("Basal Body Temperature").assertExists()
        composeTestRule.onNodeWithText("Typical BBT range: 35.0 - 38.0°C").assertExists()
    }
    
    @Test
    fun bodyWeightInputField_showsProperRangeGuidance() {
        composeTestRule.setContent {
            BodyWeightInputField(
                value = null,
                onValueChange = { }
            )
        }
        
        // Should show weight-specific guidance
        composeTestRule.onNodeWithText("Body Weight").assertExists()
        composeTestRule.onNodeWithText("Typical range: 30.0 - 200.0 kg").assertExists()
    }
    
    @Test
    fun temperatureInputField_showsCorrectUnitSymbol() {
        composeTestRule.setContent {
            TemperatureInputField(
                value = null,
                onValueChange = { },
                label = "Temperature"
            )
        }
        
        // Should show Celsius symbol by default
        composeTestRule.onNodeWithText("°C").assertExists()
        composeTestRule.onNodeWithText("Enter temperature in Celsius").assertExists()
    }
    
    @Test
    fun weightInputField_showsCorrectUnitSymbol() {
        composeTestRule.setContent {
            WeightInputField(
                value = null,
                onValueChange = { },
                label = "Weight"
            )
        }
        
        // Should show kg symbol by default
        composeTestRule.onNodeWithText("kg").assertExists()
        composeTestRule.onNodeWithText("Enter weight in Kilograms").assertExists()
    }
    
    @Test
    fun temperatureInputField_handlesErrorStates() {
        composeTestRule.setContent {
            TemperatureInputField(
                value = null,
                onValueChange = { },
                label = "Temperature",
                isError = true,
                errorMessage = "Invalid temperature"
            )
        }
        
        // Should show error message instead of helper text
        composeTestRule.onNodeWithText("Invalid temperature").assertExists()
        composeTestRule.onNodeWithText("Enter temperature in Celsius").assertDoesNotExist()
    }
    
    @Test
    fun weightInputField_handlesErrorStates() {
        composeTestRule.setContent {
            WeightInputField(
                value = null,
                onValueChange = { },
                label = "Weight",
                isError = true,
                errorMessage = "Invalid weight"
            )
        }
        
        // Should show error message instead of helper text
        composeTestRule.onNodeWithText("Invalid weight").assertExists()
        composeTestRule.onNodeWithText("Enter weight in Kilograms").assertDoesNotExist()
    }
}