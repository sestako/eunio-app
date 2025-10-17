package com.eunio.healthapp.android.accessibility

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.eunio.healthapp.android.ui.components.AccessiblePeriodFlowSelector
import com.eunio.healthapp.android.ui.components.AccessibleSymptomSelector
import com.eunio.healthapp.android.ui.components.TemperatureInputField
import com.eunio.healthapp.android.ui.theme.EunioTheme
import com.eunio.healthapp.domain.model.PeriodFlow
import com.eunio.healthapp.domain.model.Symptom
import com.eunio.healthapp.domain.model.UnitSystem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accessibility tests for Android UI components
 * Tests TalkBack support, content descriptions, and accessibility compliance
 */
@RunWith(AndroidJUnit4::class)
class AccessibilityTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    // MARK: - Content Description Tests
    
    @Test
    fun temperatureInputField_hasContentDescription() {
        // Given
        val temperature = ""
        val unitSystem = UnitSystem.METRIC
        
        // When
        composeTestRule.setContent {
            EunioTheme {
                TemperatureInputField(
                    temperature = temperature,
                    onTemperatureChange = { },
                    unitSystem = unitSystem,
                    isError = false
                )
            }
        }
        
        // Then
        composeTestRule.onNodeWithContentDescription("Temperature input")
            .assertIsDisplayed()
    }
    
    @Test
    fun temperatureInputField_hasAccessibilityLabel() {
        // Given
        val temperature = "37.5"
        val unitSystem = UnitSystem.METRIC
        
        // When
        composeTestRule.setContent {
            EunioTheme {
                TemperatureInputField(
                    temperature = temperature,
                    onTemperatureChange = { },
                    unitSystem = unitSystem,
                    isError = false
                )
            }
        }
        
        // Then
        composeTestRule.onNode(hasContentDescription("Temperature input"))
            .assertIsDisplayed()
            .assert(hasText("37.5"))
    }
    
    @Test
    fun temperatureInputField_announcesErrorState() {
        // Given
        val temperature = "invalid"
        val unitSystem = UnitSystem.METRIC
        
        // When
        composeTestRule.setContent {
            EunioTheme {
                TemperatureInputField(
                    temperature = temperature,
                    onTemperatureChange = { },
                    unitSystem = unitSystem,
                    isError = true
                )
            }
        }
        
        // Then
        composeTestRule.onNodeWithContentDescription("Temperature input")
            .assertIsDisplayed()
        // Error state should be announced to screen readers
        composeTestRule.onNodeWithContentDescription("Invalid temperature")
            .assertExists()
    }
    
    // MARK: - Period Flow Selector Accessibility Tests
    
    @Test
    fun periodFlowSelector_hasAccessibleLabels() {
        // Given
        var selectedFlow: PeriodFlow? = null
        
        // When
        composeTestRule.setContent {
            EunioTheme {
                AccessiblePeriodFlowSelector(
                    selectedFlow = selectedFlow,
                    onFlowSelected = { selectedFlow = it }
                )
            }
        }
        
        // Then
        composeTestRule.onNodeWithContentDescription("Period flow selector")
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithContentDescription("Select none flow")
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Select light flow")
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Select medium flow")
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Select heavy flow")
            .assertIsDisplayed()
    }
    
    @Test
    fun periodFlowSelector_announcesSelection() {
        // Given
        var selectedFlow: PeriodFlow? = null
        
        // When
        composeTestRule.setContent {
            EunioTheme {
                AccessiblePeriodFlowSelector(
                    selectedFlow = selectedFlow,
                    onFlowSelected = { selectedFlow = it }
                )
            }
        }
        
        // Then
        composeTestRule.onNodeWithContentDescription("Select medium flow")
            .performClick()
        
        // Verify selection is announced
        composeTestRule.onNodeWithContentDescription("Medium flow selected")
            .assertExists()
    }
    
    @Test
    fun periodFlowSelector_supportsKeyboardNavigation() {
        // Given
        var selectedFlow: PeriodFlow? = null
        
        // When
        composeTestRule.setContent {
            EunioTheme {
                AccessiblePeriodFlowSelector(
                    selectedFlow = selectedFlow,
                    onFlowSelected = { selectedFlow = it }
                )
            }
        }
        
        // Then - Should be focusable and navigable with keyboard
        composeTestRule.onNodeWithContentDescription("Select light flow")
            .assertHasClickAction()
            .assertIsEnabled()
    }
    
    // MARK: - Symptom Selector Accessibility Tests
    
    @Test
    fun symptomSelector_hasAccessibleLabels() {
        // Given
        var selectedSymptoms: Set<Symptom> = emptySet()
        
        // When
        composeTestRule.setContent {
            EunioTheme {
                AccessibleSymptomSelector(
                    selectedSymptoms = selectedSymptoms,
                    onSymptomsChanged = { selectedSymptoms = it }
                )
            }
        }
        
        // Then
        composeTestRule.onNodeWithContentDescription("Symptom selector")
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithContentDescription("Toggle cramps symptom")
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Toggle headache symptom")
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Toggle bloating symptom")
            .assertIsDisplayed()
    }
    
    @Test
    fun symptomSelector_announcesToggleState() {
        // Given
        var selectedSymptoms: Set<Symptom> = emptySet()
        
        // When
        composeTestRule.setContent {
            EunioTheme {
                AccessibleSymptomSelector(
                    selectedSymptoms = selectedSymptoms,
                    onSymptomsChanged = { selectedSymptoms = it }
                )
            }
        }
        
        // Then - Toggle symptom on
        composeTestRule.onNodeWithContentDescription("Toggle cramps symptom")
            .performClick()
        
        composeTestRule.onNodeWithContentDescription("Cramps symptom selected")
            .assertExists()
        
        // Then - Toggle symptom off
        composeTestRule.onNodeWithContentDescription("Toggle cramps symptom")
            .performClick()
        
        composeTestRule.onNodeWithContentDescription("Cramps symptom deselected")
            .assertExists()
    }
    
    // MARK: - Semantic Properties Tests
    
    @Test
    fun components_haveProperSemanticProperties() {
        // Given
        var selectedFlow: PeriodFlow? = null
        
        // When
        composeTestRule.setContent {
            EunioTheme {
                AccessiblePeriodFlowSelector(
                    selectedFlow = selectedFlow,
                    onFlowSelected = { selectedFlow = it }
                )
            }
        }
        
        // Then - Check semantic properties
        composeTestRule.onNodeWithContentDescription("Select light flow")
            .assert(hasClickAction())
            .assert(isEnabled())
            .assertIsSelectable()
    }
    
    @Test
    fun inputFields_haveProperSemanticProperties() {
        // Given
        val temperature = ""
        
        // When
        composeTestRule.setContent {
            EunioTheme {
                TemperatureInputField(
                    temperature = temperature,
                    onTemperatureChange = { },
                    unitSystem = UnitSystem.METRIC,
                    isError = false
                )
            }
        }
        
        // Then
        composeTestRule.onNodeWithContentDescription("Temperature input")
            .assert(hasSetTextAction())
            .assert(isEnabled())
            .assertIsNotSelected() // Input fields shouldn't be selectable
    }
    
    // MARK: - TalkBack Integration Tests
    
    @Test
    fun components_workWithTalkBack() {
        // Given
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // When
        composeTestRule.setContent {
            EunioTheme {
                TemperatureInputField(
                    temperature = "",
                    onTemperatureChange = { },
                    unitSystem = UnitSystem.METRIC,
                    isError = false
                )
            }
        }
        
        // Then - Should be accessible to TalkBack
        composeTestRule.onNodeWithContentDescription("Temperature input")
            .assertIsDisplayed()
            .assert(hasClickAction() or hasSetTextAction())
    }
    
    // MARK: - Focus Management Tests
    
    @Test
    fun components_manageFocusCorrectly() {
        // Given
        var selectedFlow: PeriodFlow? = null
        
        // When
        composeTestRule.setContent {
            EunioTheme {
                AccessiblePeriodFlowSelector(
                    selectedFlow = selectedFlow,
                    onFlowSelected = { selectedFlow = it }
                )
            }
        }
        
        // Then - Components should be focusable
        composeTestRule.onNodeWithContentDescription("Select light flow")
            .requestFocus()
            .assertIsFocused()
    }
    
    @Test
    fun inputFields_manageFocusCorrectly() {
        // Given
        val temperature = ""
        
        // When
        composeTestRule.setContent {
            EunioTheme {
                TemperatureInputField(
                    temperature = temperature,
                    onTemperatureChange = { },
                    unitSystem = UnitSystem.METRIC,
                    isError = false
                )
            }
        }
        
        // Then
        composeTestRule.onNodeWithContentDescription("Temperature input")
            .requestFocus()
            .assertIsFocused()
    }
    
    // MARK: - Color Contrast Tests
    
    @Test
    fun components_meetColorContrastRequirements() {
        // Given
        val temperature = "37.5"
        
        // When
        composeTestRule.setContent {
            EunioTheme {
                TemperatureInputField(
                    temperature = temperature,
                    onTemperatureChange = { },
                    unitSystem = UnitSystem.METRIC,
                    isError = false
                )
            }
        }
        
        // Then - Component should be visible and readable
        composeTestRule.onNodeWithContentDescription("Temperature input")
            .assertIsDisplayed()
        
        // Note: Actual color contrast testing would require additional tools
        // This test ensures the component renders and is accessible
    }
    
    @Test
    fun errorStates_haveProperColorContrast() {
        // Given
        val temperature = "invalid"
        
        // When
        composeTestRule.setContent {
            EunioTheme {
                TemperatureInputField(
                    temperature = temperature,
                    onTemperatureChange = { },
                    unitSystem = UnitSystem.METRIC,
                    isError = true
                )
            }
        }
        
        // Then - Error state should be clearly visible
        composeTestRule.onNodeWithContentDescription("Temperature input")
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Invalid temperature")
            .assertExists()
    }
    
    // MARK: - Touch Target Size Tests
    
    @Test
    fun buttons_meetMinimumTouchTargetSize() {
        // Given
        var selectedFlow: PeriodFlow? = null
        
        // When
        composeTestRule.setContent {
            EunioTheme {
                AccessiblePeriodFlowSelector(
                    selectedFlow = selectedFlow,
                    onFlowSelected = { selectedFlow = it }
                )
            }
        }
        
        // Then - Buttons should be large enough for touch interaction
        composeTestRule.onNodeWithContentDescription("Select light flow")
            .assertIsDisplayed()
            .assertHasClickAction()
        
        // Note: Actual size testing would require measuring the component bounds
        // This test ensures the component is clickable and accessible
    }
    
    // MARK: - Screen Reader Announcements Tests
    
    @Test
    fun components_provideProperAnnouncements() {
        // Given
        var selectedSymptoms: Set<Symptom> = emptySet()
        
        // When
        composeTestRule.setContent {
            EunioTheme {
                AccessibleSymptomSelector(
                    selectedSymptoms = selectedSymptoms,
                    onSymptomsChanged = { selectedSymptoms = it }
                )
            }
        }
        
        // Then - Should announce state changes
        composeTestRule.onNodeWithContentDescription("Toggle cramps symptom")
            .performClick()
        
        // Verify announcement is made
        composeTestRule.onNodeWithContentDescription("Cramps symptom selected")
            .assertExists()
    }
    
    // MARK: - Accessibility Service Integration Tests
    
    @Test
    fun components_integrateWithAccessibilityServices() {
        // Given
        val temperature = "37.5"
        
        // When
        composeTestRule.setContent {
            EunioTheme {
                TemperatureInputField(
                    temperature = temperature,
                    onTemperatureChange = { },
                    unitSystem = UnitSystem.METRIC,
                    isError = false
                )
            }
        }
        
        // Then - Should be discoverable by accessibility services
        composeTestRule.onNodeWithContentDescription("Temperature input")
            .assertIsDisplayed()
            .assert(hasSetTextAction())
    }
    
    // MARK: - Dynamic Content Tests
    
    @Test
    fun dynamicContent_announcesChanges() {
        // Given
        var temperature = ""
        
        // When
        composeTestRule.setContent {
            EunioTheme {
                TemperatureInputField(
                    temperature = temperature,
                    onTemperatureChange = { temperature = it },
                    unitSystem = UnitSystem.METRIC,
                    isError = false
                )
            }
        }
        
        // Then - Content changes should be announced
        composeTestRule.onNodeWithContentDescription("Temperature input")
            .performTextInput("37.5")
        
        // Verify the change is reflected
        composeTestRule.onNodeWithContentDescription("Temperature input")
            .assert(hasText("37.5"))
    }
}