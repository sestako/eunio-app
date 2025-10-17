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
 * UI tests for UnitPreferencesScreen.
 * Tests unit preference selection, immediate updates, and conversion previews.
 */
@RunWith(AndroidJUnit4::class)
class UnitPreferencesScreenUITest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun unitPreferencesScreen_displaysCorrectly() {
        val uiState = UnitPreferencesUiState(
            preferences = UnitPreferences.default(),
            loadingState = LoadingState.Success(UnitPreferences.default())
        )
        
        composeTestRule.setContent {
            EunioTheme {
                UnitPreferencesContent(
                    uiState = uiState,
                    onTemperatureUnitChange = {},
                    onWeightUnitChange = {},
                    onShowConversionPreview = {},
                    onHideConversionPreview = {}
                )
            }
        }
        
        // Verify temperature section is displayed
        composeTestRule
            .onNodeWithTag("preference_section_temperature")
            .assertIsDisplayed()
        
        // Verify weight section is displayed
        composeTestRule
            .onNodeWithTag("preference_section_weight")
            .assertIsDisplayed()
        
        // Verify unit options are displayed
        composeTestRule
            .onNodeWithTag("celsius_option")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithTag("fahrenheit_option")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithTag("kilograms_option")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithTag("pounds_option")
            .assertIsDisplayed()
    }
    
    @Test
    fun unitPreferencesScreen_showsLoadingState() {
        val uiState = UnitPreferencesUiState(
            loadingState = LoadingState.Loading
        )
        
        composeTestRule.setContent {
            EunioTheme {
                UnitPreferencesContent(
                    uiState = uiState,
                    onTemperatureUnitChange = {},
                    onWeightUnitChange = {},
                    onShowConversionPreview = {},
                    onHideConversionPreview = {}
                )
            }
        }
        
        // Verify loading indicator is displayed
        composeTestRule
            .onNodeWithTag("loading_indicator")
            .assertIsDisplayed()
    }
    
    @Test
    fun unitPreferencesScreen_showsErrorState() {
        val errorMessage = "Failed to load preferences"
        val uiState = UnitPreferencesUiState(
            loadingState = LoadingState.Error(errorMessage)
        )
        
        composeTestRule.setContent {
            EunioTheme {
                UnitPreferencesContent(
                    uiState = uiState,
                    onTemperatureUnitChange = {},
                    onWeightUnitChange = {},
                    onShowConversionPreview = {},
                    onHideConversionPreview = {}
                )
            }
        }
        
        // Verify error message is displayed
        composeTestRule
            .onNodeWithTag("error_message")
            .assertIsDisplayed()
            .assertTextContains(errorMessage)
    }
    
    @Test
    fun temperatureUnitSelection_triggersCallback() {
        var selectedUnit: TemperatureUnit? = null
        var showPreviewCalled = false
        
        val uiState = UnitPreferencesUiState(
            preferences = UnitPreferences(temperatureUnit = TemperatureUnit.CELSIUS),
            loadingState = LoadingState.Success(UnitPreferences.default())
        )
        
        composeTestRule.setContent {
            EunioTheme {
                UnitPreferencesContent(
                    uiState = uiState,
                    onTemperatureUnitChange = { selectedUnit = it },
                    onWeightUnitChange = {},
                    onShowConversionPreview = { showPreviewCalled = true },
                    onHideConversionPreview = {}
                )
            }
        }
        
        // Click on Fahrenheit option
        composeTestRule
            .onNodeWithTag("fahrenheit_option")
            .performClick()
        
        // Verify callback was triggered
        assert(selectedUnit == TemperatureUnit.FAHRENHEIT)
        assert(showPreviewCalled)
    }
    
    @Test
    fun weightUnitSelection_triggersCallback() {
        var selectedUnit: WeightUnit? = null
        var showPreviewCalled = false
        
        val uiState = UnitPreferencesUiState(
            preferences = UnitPreferences(weightUnit = WeightUnit.KILOGRAMS),
            loadingState = LoadingState.Success(UnitPreferences.default())
        )
        
        composeTestRule.setContent {
            EunioTheme {
                UnitPreferencesContent(
                    uiState = uiState,
                    onTemperatureUnitChange = {},
                    onWeightUnitChange = { selectedUnit = it },
                    onShowConversionPreview = { showPreviewCalled = true },
                    onHideConversionPreview = {}
                )
            }
        }
        
        // Click on Pounds option
        composeTestRule
            .onNodeWithTag("pounds_option")
            .performClick()
        
        // Verify callback was triggered
        assert(selectedUnit == WeightUnit.POUNDS)
        assert(showPreviewCalled)
    }
    
    @Test
    fun conversionPreview_showsAndHides() {
        val uiState = UnitPreferencesUiState(
            preferences = UnitPreferences.default(),
            loadingState = LoadingState.Success(UnitPreferences.default()),
            showConversionPreview = true
        )
        
        composeTestRule.setContent {
            EunioTheme {
                UnitPreferencesContent(
                    uiState = uiState,
                    onTemperatureUnitChange = {},
                    onWeightUnitChange = {},
                    onShowConversionPreview = {},
                    onHideConversionPreview = {}
                )
            }
        }
        
        // Verify conversion preview is displayed
        composeTestRule
            .onNodeWithTag("conversion_preview_section")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithTag("conversion_preview_card")
            .assertIsDisplayed()
    }
    
    @Test
    fun conversionPreview_dismissButton_works() {
        var hidePreviewCalled = false
        
        val uiState = UnitPreferencesUiState(
            preferences = UnitPreferences.default(),
            loadingState = LoadingState.Success(UnitPreferences.default()),
            showConversionPreview = true
        )
        
        composeTestRule.setContent {
            EunioTheme {
                UnitPreferencesContent(
                    uiState = uiState,
                    onTemperatureUnitChange = {},
                    onWeightUnitChange = {},
                    onShowConversionPreview = {},
                    onHideConversionPreview = { hidePreviewCalled = true }
                )
            }
        }
        
        // Click dismiss button
        composeTestRule
            .onNodeWithTag("dismiss_preview_button")
            .performClick()
        
        // Verify callback was triggered
        assert(hidePreviewCalled)
    }
    
    @Test
    fun sectionHeaders_displayCorrectly() {
        val uiState = UnitPreferencesUiState(
            preferences = UnitPreferences.default(),
            loadingState = LoadingState.Success(UnitPreferences.default())
        )
        
        composeTestRule.setContent {
            EunioTheme {
                UnitPreferencesContent(
                    uiState = uiState,
                    onTemperatureUnitChange = {},
                    onWeightUnitChange = {},
                    onShowConversionPreview = {},
                    onHideConversionPreview = {}
                )
            }
        }
        
        // Verify section titles are displayed
        composeTestRule
            .onNodeWithText("Temperature")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Weight")
            .assertIsDisplayed()
        
        // Verify section subtitles are displayed
        composeTestRule
            .onNodeWithText("Choose your preferred temperature unit for body temperature tracking")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Choose your preferred weight unit for weight tracking")
            .assertIsDisplayed()
    }
}