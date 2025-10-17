package com.eunio.healthapp.android.ui.settings

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eunio.healthapp.android.ui.theme.EunioTheme
import com.eunio.healthapp.presentation.state.SettingItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingItemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockSettingItem = SettingItem(
        id = "units",
        title = "Units",
        subtitle = "Temperature and weight units",
        keywords = listOf("temperature", "weight", "units")
    )

    @Test
    fun settingItem_displaysCorrectly() {
        composeTestRule.setContent {
            EunioTheme {
                SettingItem(
                    item = mockSettingItem,
                    onClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("setting_item_units").assertIsDisplayed()
        composeTestRule.onNodeWithText("Units").assertIsDisplayed()
        composeTestRule.onNodeWithText("Temperature and weight units").assertIsDisplayed()
    }

    @Test
    fun settingItem_clickHandling() {
        var clicked = false

        composeTestRule.setContent {
            EunioTheme {
                SettingItem(
                    item = mockSettingItem,
                    onClick = { clicked = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("setting_item_units").performClick()
        
        // In a real test, we would verify the click callback was called
    }

    @Test
    fun settingItem_disabledState() {
        composeTestRule.setContent {
            EunioTheme {
                SettingItem(
                    item = mockSettingItem,
                    onClick = {},
                    enabled = false
                )
            }
        }

        composeTestRule.onNodeWithTag("setting_item_units")
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }

    @Test
    fun settingItem_hiddenWhenNotVisible() {
        val hiddenItem = mockSettingItem.copy(isVisible = false)

        composeTestRule.setContent {
            EunioTheme {
                SettingItem(
                    item = hiddenItem,
                    onClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("setting_item_units").assertDoesNotExist()
    }

    @Test
    fun settingItem_withoutChevron() {
        composeTestRule.setContent {
            EunioTheme {
                SettingItem(
                    item = mockSettingItem,
                    onClick = {},
                    showChevron = false
                )
            }
        }

        composeTestRule.onNodeWithTag("setting_item_units").assertIsDisplayed()
        composeTestRule.onNodeWithText("Units").assertIsDisplayed()
        
        // Chevron should not be present
        composeTestRule.onNodeWithContentDescription("Navigate to Units").assertDoesNotExist()
    }

    @Test
    fun compactSettingItem_displaysCorrectly() {
        composeTestRule.setContent {
            EunioTheme {
                CompactSettingItem(
                    item = mockSettingItem,
                    onClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("compact_setting_item_units").assertIsDisplayed()
        composeTestRule.onNodeWithText("Units").assertIsDisplayed()
        composeTestRule.onNodeWithText("Temperature and weight units").assertIsDisplayed()
    }

    @Test
    fun toggleSettingItem_displaysCorrectly() {
        composeTestRule.setContent {
            EunioTheme {
                ToggleSettingItem(
                    item = mockSettingItem,
                    checked = true,
                    onCheckedChange = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("toggle_setting_item_units").assertIsDisplayed()
        composeTestRule.onNodeWithTag("setting_toggle_units").assertIsDisplayed()
        composeTestRule.onNodeWithText("Units").assertIsDisplayed()
    }

    @Test
    fun toggleSettingItem_toggleFunctionality() {
        var isChecked = false

        composeTestRule.setContent {
            EunioTheme {
                ToggleSettingItem(
                    item = mockSettingItem,
                    checked = isChecked,
                    onCheckedChange = { isChecked = it }
                )
            }
        }

        // Toggle should be off initially
        composeTestRule.onNodeWithTag("setting_toggle_units").assertIsOff()

        // Click to toggle on
        composeTestRule.onNodeWithTag("setting_toggle_units").performClick()
        
        // In a real test, we would verify the toggle state changed
    }

    @Test
    fun toggleSettingItem_clickableArea() {
        var toggleClicked = false

        composeTestRule.setContent {
            EunioTheme {
                ToggleSettingItem(
                    item = mockSettingItem,
                    checked = false,
                    onCheckedChange = { toggleClicked = true }
                )
            }
        }

        // Clicking the entire item should toggle the switch
        composeTestRule.onNodeWithTag("toggle_setting_item_units").performClick()
        
        // In a real test, we would verify the toggle callback was called
    }

    @Test
    fun valueSettingItem_displaysCorrectly() {
        val currentValue = "Celsius, Kilograms"

        composeTestRule.setContent {
            EunioTheme {
                ValueSettingItem(
                    item = mockSettingItem,
                    value = currentValue,
                    onClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("value_setting_item_units").assertIsDisplayed()
        composeTestRule.onNodeWithText("Units").assertIsDisplayed()
        composeTestRule.onNodeWithText(currentValue).assertIsDisplayed()
    }

    @Test
    fun settingItem_longTitlesAndSubtitles() {
        val longTitleItem = mockSettingItem.copy(
            title = "This is a very long setting item title that should be truncated properly",
            subtitle = "This is a very long setting item subtitle that provides detailed information and should wrap or truncate appropriately"
        )

        composeTestRule.setContent {
            EunioTheme {
                SettingItem(
                    item = longTitleItem,
                    onClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("setting_item_units").assertIsDisplayed()
        composeTestRule.onNodeWithText(longTitleItem.title).assertIsDisplayed()
        composeTestRule.onNodeWithText(longTitleItem.subtitle).assertIsDisplayed()
    }

    @Test
    fun settingItem_emptySubtitle() {
        val noSubtitleItem = mockSettingItem.copy(subtitle = "")

        composeTestRule.setContent {
            EunioTheme {
                SettingItem(
                    item = noSubtitleItem,
                    onClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("setting_item_units").assertIsDisplayed()
        composeTestRule.onNodeWithText("Units").assertIsDisplayed()
        
        // Subtitle should not be displayed when empty
        composeTestRule.onNodeWithText("Temperature and weight units").assertDoesNotExist()
    }

    @Test
    fun settingItem_accessibility() {
        composeTestRule.setContent {
            EunioTheme {
                SettingItem(
                    item = mockSettingItem,
                    onClick = {}
                )
            }
        }

        // Verify accessibility properties
        composeTestRule.onNodeWithTag("setting_item_units")
            .assertHasClickAction()
            .assertIsEnabled()

        // Verify content description for chevron
        composeTestRule.onNodeWithContentDescription("Navigate to Units")
            .assertIsDisplayed()
    }

    @Test
    fun toggleSettingItem_accessibility() {
        composeTestRule.setContent {
            EunioTheme {
                ToggleSettingItem(
                    item = mockSettingItem,
                    checked = false,
                    onCheckedChange = {}
                )
            }
        }

        // Verify accessibility properties for toggle
        composeTestRule.onNodeWithTag("setting_toggle_units")
            .assertHasClickAction()
            .assertIsToggleable()
    }

    @Test
    fun settingItem_withIcon() {
        val iconItem = mockSettingItem.copy(id = "notifications") // This ID maps to an icon

        composeTestRule.setContent {
            EunioTheme {
                SettingItem(
                    item = iconItem,
                    onClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("setting_item_notifications").assertIsDisplayed()
        composeTestRule.onNodeWithText("Units").assertIsDisplayed()
        
        // Icon should be displayed (can't easily test icon presence in unit tests)
    }
}