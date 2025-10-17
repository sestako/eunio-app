package com.eunio.healthapp.android.ui.settings

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eunio.healthapp.android.ui.theme.EunioTheme
import com.eunio.healthapp.domain.model.settings.TemperatureUnit
import com.eunio.healthapp.domain.model.settings.UnitPreferences
import com.eunio.healthapp.domain.model.settings.WeightUnit
import com.eunio.healthapp.presentation.state.LoadingState
import com.eunio.healthapp.presentation.state.UnitPreferencesUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for ConversionPreviewCard component.
 * Tests conversion display, dismiss functionality, and preview accuracy.
 */
@RunWith(AndroidJUnit4::class)
class ConversionPreviewCardUITest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun conversionPreviewCard_displaysCorrectly() {
        val uiState = UnitPreferencesUiState(
            preferences = UnitPreferences(
                temperatureUnit = TemperatureUnit.CELSIUS,
                weightUnit = WeightUnit.KILOGRAMS
            ),
            loadingState = LoadingState.Success(UnitPreferences.default()),
            previewTemperature = 37.0,
            previewWeight = 70.0
        )
        
        composeTestRule.setContent {
            EunioTheme {
                ConversionPreviewCard(
                    uiState = uiState,
                    onDismiss = {}
                )
            }
        }
        
        // Verify card is displayed
        composeTestRule
            .onNodeWithTag("conversion_preview_card")
            .assertIsDisplayed()
        
        // Verify header is displayed
        composeTestRule
            .onNodeWithTag("preview_header")
            .assertIsDisplayed()
        
        // Verify title is displayed
        composeTestRule
            .onNodeWithText("Conversion Preview")
            .assertIsDisplayed()
        
        // Verify dismiss button is displayed
        composeTestRule
            .onNodeWithTag("dismiss_preview_button")
            .assertIsDisplayed()
    }
    
    @Test
    fun conversionPreviewCard_showsTemperatureConversion() {
        val uiState = UnitPreferencesUiState(
            preferences = UnitPreferences(
                temperatureUnit = TemperatureUnit.CELSIUS,
                weightUnit = WeightUnit.KILOGRAMS
            ),
            loadingState = LoadingState.Success(UnitPreferences.default()),
            previewTemperature = 37.0,
            previewWeight = 70.0
        )
        
        composeTestRule.setContent {
            EunioTheme {
                ConversionPreviewCard(
                    uiState = uiState,
                    onDismiss = {}
                )
            }
        }
        
        // Verify temperature conversion is displayed
        composeTestRule
            .onNodeWithTag("temperature_conversion")
            .assertIsDisplayed()
        
        // Verify temperature title is displayed
        composeTestRule
            .onNodeWithText("Temperature")
            .assertIsDisplayed()
    }
    
    @Test
    fun conversionPreviewCard_showsWeightConversion() {
        val uiState = UnitPreferencesUiState(
            preferences = UnitPreferences(
                temperatureUnit = TemperatureUnit.CELSIUS,
                weightUnit = WeightUnit.KILOGRAMS
            ),
            loadingState = LoadingState.Success(UnitPreferences.default()),
            previewTemperature = 37.0,
            previewWeight = 70.0
        )
        
        composeTestRule.setContent {
            EunioTheme {
                ConversionPreviewCard(
                    uiState = uiState,
                    onDismiss = {}
                )
            }
        }
        
        // Verify weight conversion is displayed
        composeTestRule
            .onNodeWithTag("weight_conversion")
            .assertIsDisplayed()
        
        // Verify weight title is displayed
        composeTestRule
            .onNodeWithText("Weight")
            .assertIsDisplayed()
    }
    
    @Test
    fun conversionPreviewCard_dismissButton_triggersCallback() {
        var dismissCalled = false
        
        val uiState = UnitPreferencesUiState(
            preferences = UnitPreferences.default(),
            loadingState = LoadingState.Success(UnitPreferences.default())
        )
        
        composeTestRule.setContent {
            EunioTheme {
                ConversionPreviewCard(
                    uiState = uiState,
                    onDismiss = { dismissCalled = true }
                )
            }
        }
        
        // Click dismiss button
        composeTestRule
            .onNodeWithTag("dismiss_preview_button")
            .performClick()
        
        // Verify callback was triggered
        assert(dismissCalled)
    }
    
    @Test
    fun conversionPreviewCard_showsInfoText() {
        val uiState = UnitPreferencesUiState(
            preferences = UnitPreferences.default(),
            loadingState = LoadingState.Success(UnitPreferences.default())
        )
        
        composeTestRule.setContent {
            EunioTheme {
                ConversionPreviewCard(
                    uiState = uiState,
                    onDismiss = {}
                )
            }
        }
        
        // Verify info text is displayed
        composeTestRule
            .onNodeWithTag("conversion_info_text")
            .assertIsDisplayed()
        
        // Verify info text content
        composeTestRule
            .onNodeWithText("These are sample conversions. All your existing data will be automatically converted to your preferred units.")
            .assertIsDisplayed()
    }
    
    @Test
    fun conversionPreviewCard_celsiusToFahrenheit_showsCorrectConversion() {
        val uiState = UnitPreferencesUiState(
            preferences = UnitPreferences(
                temperatureUnit = TemperatureUnit.CELSIUS,
                weightUnit = WeightUnit.KILOGRAMS
            ),
            loadingState = LoadingState.Success(UnitPreferences.default()),
            previewTemperature = 37.0, // Body temperature
            previewWeight = 70.0
        )
        
        composeTestRule.setContent {
            EunioTheme {
                ConversionPreviewCard(
                    uiState = uiState,
                    onDismiss = {}
                )
            }
        }
        
        // Verify current temperature is displayed
        composeTestRule
            .onNodeWithText("37.0°C")
            .assertIsDisplayed()
        
        // Verify converted temperature is displayed (37°C = 98.6°F)
        composeTestRule
            .onNodeWithText("98.6°F")
            .assertIsDisplayed()
    }
    
    @Test
    fun conversionPreviewCard_kilogramsToPounds_showsCorrectConversion() {
        val uiState = UnitPreferencesUiState(
            preferences = UnitPreferences(
                temperatureUnit = TemperatureUnit.CELSIUS,
                weightUnit = WeightUnit.KILOGRAMS
            ),
            loadingState = LoadingState.Success(UnitPreferences.default()),
            previewTemperature = 37.0,
            previewWeight = 70.0 // 70kg
        )
        
        composeTestRule.setContent {
            EunioTheme {
                ConversionPreviewCard(
                    uiState = uiState,
                    onDismiss = {}
                )
            }
        }
        
        // Verify current weight is displayed
        composeTestRule
            .onNodeWithText("70.0 kg")
            .assertIsDisplayed()
        
        // Verify converted weight is displayed (70kg ≈ 154.3 lbs)
        composeTestRule
            .onNodeWithText("154.3 lbs")
            .assertIsDisplayed()
    }
    
    @Test
    fun conversionPreviewCard_accessibility_hasCorrectSemantics() {
        val uiState = UnitPreferencesUiState(
            preferences = UnitPreferences.default(),
            loadingState = LoadingState.Success(UnitPreferences.default())
        )
        
        composeTestRule.setContent {
            EunioTheme {
                ConversionPreviewCard(
                    uiState = uiState,
                    onDismiss = {}
                )
            }
        }
        
        // Verify dismiss button has correct content description
        composeTestRule
            .onNodeWithTag("dismiss_preview_button")
            .assert(hasContentDescription("Dismiss preview"))
            .assertHasClickAction()
    }
}