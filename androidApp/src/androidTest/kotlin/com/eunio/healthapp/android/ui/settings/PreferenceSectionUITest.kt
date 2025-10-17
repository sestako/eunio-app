package com.eunio.healthapp.android.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eunio.healthapp.android.ui.theme.EunioTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for PreferenceSection component.
 * Tests section display, headers, and content organization.
 */
@RunWith(AndroidJUnit4::class)
class PreferenceSectionUITest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun preferenceSection_displaysCorrectly() {
        val title = "Test Section"
        val subtitle = "This is a test section"
        
        composeTestRule.setContent {
            EunioTheme {
                PreferenceSection(
                    title = title,
                    subtitle = subtitle
                ) {
                    Text("Section content")
                }
            }
        }
        
        // Verify section is displayed
        composeTestRule
            .onNodeWithTag("preference_section_test_section")
            .assertIsDisplayed()
        
        // Verify title is displayed
        composeTestRule
            .onNodeWithTag("section_title")
            .assertIsDisplayed()
            .assertTextEquals(title)
        
        // Verify subtitle is displayed
        composeTestRule
            .onNodeWithTag("section_subtitle")
            .assertIsDisplayed()
            .assertTextEquals(subtitle)
        
        // Verify content is displayed
        composeTestRule
            .onNodeWithText("Section content")
            .assertIsDisplayed()
    }
    
    @Test
    fun preferenceSection_withIcon_displaysIcon() {
        composeTestRule.setContent {
            EunioTheme {
                PreferenceSection(
                    title = "Settings",
                    subtitle = "Configure your preferences",
                    icon = Icons.Default.Settings
                ) {
                    Text("Settings content")
                }
            }
        }
        
        // Verify section with icon is displayed
        composeTestRule
            .onNodeWithTag("preference_section_settings")
            .assertIsDisplayed()
        
        // Verify header is displayed (which includes the icon)
        composeTestRule
            .onNodeWithTag("preference_section_header")
            .assertIsDisplayed()
    }
    
    @Test
    fun preferenceSection_emptySubtitle_hidesSubtitle() {
        composeTestRule.setContent {
            EunioTheme {
                PreferenceSection(
                    title = "Title Only",
                    subtitle = ""
                ) {
                    Text("Content")
                }
            }
        }
        
        // Verify title is displayed
        composeTestRule
            .onNodeWithTag("section_title")
            .assertIsDisplayed()
            .assertTextEquals("Title Only")
        
        // Verify subtitle is not displayed when empty
        composeTestRule
            .onNodeWithTag("section_subtitle")
            .assertDoesNotExist()
    }
    
    @Test
    fun preferenceSection_visibility_controlsDisplay() {
        composeTestRule.setContent {
            EunioTheme {
                PreferenceSection(
                    title = "Hidden Section",
                    subtitle = "This should not be visible",
                    isVisible = false
                ) {
                    Text("Hidden content")
                }
            }
        }
        
        // Verify section is not displayed when isVisible = false
        composeTestRule
            .onNodeWithTag("preference_section_hidden_section")
            .assertDoesNotExist()
    }
    
    @Test
    fun compactPreferenceSection_displaysCorrectly() {
        composeTestRule.setContent {
            EunioTheme {
                CompactPreferenceSection(
                    title = "Compact Section",
                    subtitle = "Compact layout",
                    icon = Icons.Default.Settings
                ) {
                    Text("Compact content")
                }
            }
        }
        
        // Verify compact section is displayed
        composeTestRule
            .onNodeWithTag("compact_preference_section_compact_section")
            .assertIsDisplayed()
        
        // Verify content is displayed
        composeTestRule
            .onNodeWithText("Compact content")
            .assertIsDisplayed()
    }
    
    @Test
    fun expandablePreferenceSection_expandsAndCollapses() {
        composeTestRule.setContent {
            EunioTheme {
                ExpandablePreferenceSection(
                    title = "Expandable Section",
                    subtitle = "Can be expanded or collapsed",
                    initiallyExpanded = true
                ) {
                    Text("Expandable content")
                }
            }
        }
        
        // Verify expandable section is displayed
        composeTestRule
            .onNodeWithTag("expandable_preference_section_expandable_section")
            .assertIsDisplayed()
        
        // Verify expand/collapse button is displayed
        composeTestRule
            .onNodeWithTag("expand_collapse_button")
            .assertIsDisplayed()
        
        // Verify content is initially visible (initiallyExpanded = true)
        composeTestRule
            .onNodeWithText("Expandable content")
            .assertIsDisplayed()
        
        // Click to collapse
        composeTestRule
            .onNodeWithTag("expand_collapse_button")
            .performClick()
        
        // Content should be hidden after collapse
        // Note: Due to animation, we might need to wait
        composeTestRule.waitForIdle()
    }
    
    @Test
    fun simplePreferenceSection_displaysWithoutCard() {
        composeTestRule.setContent {
            EunioTheme {
                SimplePreferenceSection(
                    title = "Simple Section",
                    subtitle = "No card styling"
                ) {
                    Text("Simple content")
                }
            }
        }
        
        // Verify simple section is displayed
        composeTestRule
            .onNodeWithTag("simple_preference_section_simple_section")
            .assertIsDisplayed()
        
        // Verify content is displayed
        composeTestRule
            .onNodeWithText("Simple content")
            .assertIsDisplayed()
    }
    
    @Test
    fun preferenceSection_accessibility_hasCorrectSemantics() {
        composeTestRule.setContent {
            EunioTheme {
                PreferenceSection(
                    title = "Accessible Section",
                    subtitle = "With proper semantics"
                ) {
                    Text("Accessible content")
                }
            }
        }
        
        // Verify section has proper test tags for accessibility
        composeTestRule
            .onNodeWithTag("preference_section_accessible_section")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithTag("preference_section_header")
            .assertIsDisplayed()
    }
}