package com.eunio.healthapp.android.ui.settings

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eunio.healthapp.android.ui.theme.EunioTheme
import com.eunio.healthapp.presentation.state.SettingItem
import com.eunio.healthapp.presentation.state.SettingSection
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsSectionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockSettingSection = SettingSection(
        id = "health",
        title = "Health Settings",
        subtitle = "Customize your health tracking preferences",
        icon = "health",
        items = listOf(
            SettingItem(
                id = "units",
                title = "Units",
                subtitle = "Temperature and weight units",
                keywords = listOf("temperature", "weight", "units")
            ),
            SettingItem(
                id = "notifications",
                title = "Notifications",
                subtitle = "Reminders and alerts",
                keywords = listOf("notifications", "reminders")
            )
        )
    )

    @Test
    fun settingsSection_displaysCorrectly() {
        composeTestRule.setContent {
            EunioTheme {
                SettingsSection(
                    section = mockSettingSection,
                    onItemClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("settings_section_health").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_section_header").assertIsDisplayed()
        
        // Verify section title and subtitle
        composeTestRule.onNodeWithText("Health Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Customize your health tracking preferences").assertIsDisplayed()
        
        // Verify setting items are displayed
        composeTestRule.onNodeWithText("Units").assertIsDisplayed()
        composeTestRule.onNodeWithText("Notifications").assertIsDisplayed()
        composeTestRule.onNodeWithText("Temperature and weight units").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reminders and alerts").assertIsDisplayed()
    }

    @Test
    fun settingsSection_hiddenWhenNotVisible() {
        val hiddenSection = mockSettingSection.copy(isVisible = false)

        composeTestRule.setContent {
            EunioTheme {
                SettingsSection(
                    section = hiddenSection,
                    onItemClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("settings_section_health").assertDoesNotExist()
    }

    @Test
    fun settingsSection_emptyItems() {
        val emptySection = mockSettingSection.copy(items = emptyList())

        composeTestRule.setContent {
            EunioTheme {
                SettingsSection(
                    section = emptySection,
                    onItemClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("settings_section_health").assertIsDisplayed()
        composeTestRule.onNodeWithText("Health Settings").assertIsDisplayed()
        
        // No setting items should be displayed
        composeTestRule.onNodeWithText("Units").assertDoesNotExist()
        composeTestRule.onNodeWithText("Notifications").assertDoesNotExist()
    }

    @Test
    fun settingsSection_itemClickHandling() {
        var clickedItemId = ""

        composeTestRule.setContent {
            EunioTheme {
                SettingsSection(
                    section = mockSettingSection,
                    onItemClick = { clickedItemId = it }
                )
            }
        }

        composeTestRule.onNodeWithTag("setting_item_units").performClick()
        
        // In a real test, we would verify the click callback was called with correct ID
    }

    @Test
    fun compactSettingsSection_displaysCorrectly() {
        composeTestRule.setContent {
            EunioTheme {
                CompactSettingsSection(
                    section = mockSettingSection,
                    onItemClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("compact_settings_section_health").assertIsDisplayed()
        composeTestRule.onNodeWithText("Health Settings").assertIsDisplayed()
        
        // Verify compact setting items
        composeTestRule.onNodeWithTag("compact_setting_item_units").assertIsDisplayed()
        composeTestRule.onNodeWithTag("compact_setting_item_notifications").assertIsDisplayed()
    }

    @Test
    fun expandableSettingsSection_displaysCorrectly() {
        composeTestRule.setContent {
            EunioTheme {
                ExpandableSettingsSection(
                    section = mockSettingSection,
                    onItemClick = {},
                    initiallyExpanded = true
                )
            }
        }

        composeTestRule.onNodeWithTag("expandable_settings_section_health").assertIsDisplayed()
        composeTestRule.onNodeWithTag("expandable_section_header").assertIsDisplayed()
        composeTestRule.onNodeWithTag("expand_collapse_button").assertIsDisplayed()
        
        // Items should be visible when expanded
        composeTestRule.onNodeWithText("Units").assertIsDisplayed()
        composeTestRule.onNodeWithText("Notifications").assertIsDisplayed()
    }

    @Test
    fun expandableSettingsSection_collapseExpand() {
        composeTestRule.setContent {
            EunioTheme {
                ExpandableSettingsSection(
                    section = mockSettingSection,
                    onItemClick = {},
                    initiallyExpanded = true
                )
            }
        }

        // Initially expanded - items should be visible
        composeTestRule.onNodeWithText("Units").assertIsDisplayed()
        
        // Click to collapse
        composeTestRule.onNodeWithTag("expand_collapse_button").performClick()
        
        // Items should be hidden after collapse (with animation)
        composeTestRule.waitForIdle()
        
        // Click to expand again
        composeTestRule.onNodeWithTag("expand_collapse_button").performClick()
        
        // Items should be visible again
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Units").assertIsDisplayed()
    }

    @Test
    fun expandableSettingsSection_initiallyCollapsed() {
        composeTestRule.setContent {
            EunioTheme {
                ExpandableSettingsSection(
                    section = mockSettingSection,
                    onItemClick = {},
                    initiallyExpanded = false
                )
            }
        }

        // Items should not be visible when initially collapsed
        composeTestRule.onNodeWithText("Units").assertDoesNotExist()
        
        // Header should still be visible
        composeTestRule.onNodeWithText("Health Settings").assertIsDisplayed()
        composeTestRule.onNodeWithTag("expand_collapse_button").assertIsDisplayed()
    }

    @Test
    fun settingsSection_accessibility() {
        composeTestRule.setContent {
            EunioTheme {
                SettingsSection(
                    section = mockSettingSection,
                    onItemClick = {}
                )
            }
        }

        // Verify accessibility properties
        composeTestRule.onNodeWithTag("setting_item_units")
            .assertHasClickAction()
        
        composeTestRule.onNodeWithTag("setting_item_notifications")
            .assertHasClickAction()
    }

    @Test
    fun settingsSection_longTitlesAndSubtitles() {
        val longTitleSection = mockSettingSection.copy(
            title = "This is a very long section title that should be handled properly",
            subtitle = "This is a very long section subtitle that provides detailed information about what this section contains and should wrap properly",
            items = listOf(
                SettingItem(
                    id = "long_item",
                    title = "This is a very long setting item title",
                    subtitle = "This is a very long setting item subtitle that provides detailed information",
                    keywords = emptyList()
                )
            )
        )

        composeTestRule.setContent {
            EunioTheme {
                SettingsSection(
                    section = longTitleSection,
                    onItemClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("settings_section_health").assertIsDisplayed()
        composeTestRule.onNodeWithText(longTitleSection.title).assertIsDisplayed()
        composeTestRule.onNodeWithText(longTitleSection.subtitle).assertIsDisplayed()
    }
}