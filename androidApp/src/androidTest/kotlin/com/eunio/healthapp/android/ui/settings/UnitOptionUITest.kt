package com.eunio.healthapp.android.ui.settings

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eunio.healthapp.android.ui.theme.EunioTheme
import com.eunio.healthapp.domain.model.settings.TemperatureUnit
import com.eunio.healthapp.domain.model.settings.WeightUnit
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for UnitOption component.
 * Tests selection states, animations, and user interactions.
 */
@RunWith(AndroidJUnit4::class)
class UnitOptionUITest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun temperatureUnitOption_displaysCorrectly() {
        composeTestRule.setContent {
            EunioTheme {
                UnitOption(
                    unit = TemperatureUnit.CELSIUS,
                    isSelected = false,
                    onSelected = {}
                )
            }
        }
        
        // Verify unit name is displayed
        composeTestRule
            .onNodeWithTag("unit_name")
            .assertIsDisplayed()
            .assertTextEquals("Celsius")
        
        // Verify unit symbol is displayed
        composeTestRule
            .onNodeWithTag("unit_symbol")
            .assertIsDisplayed()
            .assertTextEquals("°C")
        
        // Verify unit description is displayed
        composeTestRule
            .onNodeWithTag("unit_description")
            .assertIsDisplayed()
            .assertTextEquals("Used worldwide")
    }
    
    @Test
    fun weightUnitOption_displaysCorrectly() {
        composeTestRule.setContent {
            EunioTheme {
                UnitOption(
                    unit = WeightUnit.POUNDS,
                    isSelected = false,
                    onSelected = {}
                )
            }
        }
        
        // Verify unit name is displayed
        composeTestRule
            .onNodeWithTag("unit_name")
            .assertIsDisplayed()
            .assertTextEquals("Pounds")
        
        // Verify unit symbol is displayed
        composeTestRule
            .onNodeWithTag("unit_symbol")
            .assertIsDisplayed()
            .assertTextEquals("lbs")
        
        // Verify unit description is displayed
        composeTestRule
            .onNodeWithTag("unit_description")
            .assertIsDisplayed()
            .assertTextEquals("Imperial system")
    }
    
    @Test
    fun unitOption_selectedState_showsCheckmark() {
        composeTestRule.setContent {
            EunioTheme {
                UnitOption(
                    unit = TemperatureUnit.CELSIUS,
                    isSelected = true,
                    onSelected = {}
                )
            }
        }
        
        // Verify the option is marked as selected
        composeTestRule
            .onNodeWithTag("unit_option_celsius")
            .assertIsSelected()
    }
    
    @Test
    fun unitOption_unselectedState_noCheckmark() {
        composeTestRule.setContent {
            EunioTheme {
                UnitOption(
                    unit = TemperatureUnit.CELSIUS,
                    isSelected = false,
                    onSelected = {}
                )
            }
        }
        
        // Verify the option is not marked as selected
        composeTestRule
            .onNodeWithTag("unit_option_celsius")
            .assertIsNotSelected()
    }
    
    @Test
    fun unitOption_click_triggersCallback() {
        var callbackTriggered = false
        
        composeTestRule.setContent {
            EunioTheme {
                UnitOption(
                    unit = TemperatureUnit.FAHRENHEIT,
                    isSelected = false,
                    onSelected = { callbackTriggered = true }
                )
            }
        }
        
        // Click the option
        composeTestRule
            .onNodeWithTag("unit_option_fahrenheit")
            .performClick()
        
        // Verify callback was triggered
        assert(callbackTriggered)
    }
    
    @Test
    fun unitOption_disabled_preventsInteraction() {
        var callbackTriggered = false
        
        composeTestRule.setContent {
            EunioTheme {
                UnitOption(
                    unit = TemperatureUnit.CELSIUS,
                    isSelected = false,
                    onSelected = { callbackTriggered = true },
                    enabled = false
                )
            }
        }
        
        // Try to click the disabled option
        composeTestRule
            .onNodeWithTag("unit_option_celsius")
            .performClick()
        
        // Verify callback was not triggered
        assert(!callbackTriggered)
    }
    
    @Test
    fun unitOption_hasCorrectSemantics() {
        composeTestRule.setContent {
            EunioTheme {
                UnitOption(
                    unit = TemperatureUnit.CELSIUS,
                    isSelected = true,
                    onSelected = {}
                )
            }
        }
        
        // Verify semantic properties
        composeTestRule
            .onNodeWithTag("unit_option_celsius")
            .assert(hasContentDescription("Celsius unit option, selected"))
    }
    
    @Test
    fun compactUnitOption_displaysCorrectly() {
        composeTestRule.setContent {
            EunioTheme {
                CompactUnitOption(
                    unit = WeightUnit.KILOGRAMS,
                    isSelected = true,
                    onSelected = {}
                )
            }
        }
        
        // Verify compact option is displayed
        composeTestRule
            .onNodeWithTag("compact_unit_option_kilograms")
            .assertIsDisplayed()
            .assertIsSelected()
    }
    
    @Test
    fun compactUnitOption_click_triggersCallback() {
        var callbackTriggered = false
        
        composeTestRule.setContent {
            EunioTheme {
                CompactUnitOption(
                    unit = WeightUnit.POUNDS,
                    isSelected = false,
                    onSelected = { callbackTriggered = true }
                )
            }
        }
        
        // Click the compact option
        composeTestRule
            .onNodeWithTag("compact_unit_option_pounds")
            .performClick()
        
        // Verify callback was triggered
        assert(callbackTriggered)
    }
    
    @Test
    fun unitOption_accessibilitySupport() {
        composeTestRule.setContent {
            EunioTheme {
                UnitOption(
                    unit = TemperatureUnit.FAHRENHEIT,
                    isSelected = false,
                    onSelected = {}
                )
            }
        }
        
        // Verify accessibility properties
        composeTestRule
            .onNodeWithTag("unit_option_fahrenheit")
            .assert(hasContentDescription("Fahrenheit unit option, not selected"))
            .assertHasClickAction()
    }
    
    @Test
    fun unitOption_visualStates_different() {
        composeTestRule.setContent {
            EunioTheme {
                // Test both selected and unselected states side by side
                androidx.compose.foundation.layout.Row {
                    UnitOption(
                        unit = TemperatureUnit.CELSIUS,
                        isSelected = true,
                        onSelected = {},
                        modifier = Modifier.testTag("selected_option")
                    )
                    UnitOption(
                        unit = TemperatureUnit.FAHRENHEIT,
                        isSelected = false,
                        onSelected = {},
                        modifier = Modifier.testTag("unselected_option")
                    )
                }
            }
        }
        
        // Both options should be displayed
        composeTestRule
            .onNodeWithTag("selected_option")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithTag("unselected_option")
            .assertIsDisplayed()
        
        // Selected option should be marked as selected
        composeTestRule
            .onNodeWithTag("selected_option")
            .assertIsSelected()
        
        // Unselected option should not be marked as selected
        composeTestRule
            .onNodeWithTag("unselected_option")
            .assertIsNotSelected()
    }
}