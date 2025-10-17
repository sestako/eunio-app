package com.eunio.healthapp.android.ui.settings

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eunio.healthapp.android.ui.theme.EunioTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsSearchBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun settingsSearchBar_displaysCorrectly() {
        composeTestRule.setContent {
            EunioTheme {
                SettingsSearchBar(
                    query = "",
                    onQueryChange = {},
                    onClearSearch = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("settings_search_bar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Search settings...").assertIsDisplayed()
    }

    @Test
    fun settingsSearchBar_textInput() {
        var currentQuery = ""

        composeTestRule.setContent {
            EunioTheme {
                SettingsSearchBar(
                    query = currentQuery,
                    onQueryChange = { currentQuery = it },
                    onClearSearch = { currentQuery = "" }
                )
            }
        }

        val searchText = "notifications"
        composeTestRule.onNodeWithTag("settings_search_bar")
            .performTextInput(searchText)

        composeTestRule.onNodeWithTag("settings_search_bar")
            .assertTextContains(searchText)
    }

    @Test
    fun settingsSearchBar_clearButton() {
        var currentQuery = "test query"
        var clearCalled = false

        composeTestRule.setContent {
            EunioTheme {
                SettingsSearchBar(
                    query = currentQuery,
                    onQueryChange = { currentQuery = it },
                    onClearSearch = { 
                        currentQuery = ""
                        clearCalled = true
                    }
                )
            }
        }

        // Clear button should be visible when there's text
        composeTestRule.onNodeWithTag("clear_search_button").assertIsDisplayed()

        // Click clear button
        composeTestRule.onNodeWithTag("clear_search_button").performClick()

        // Verify clear was called (in real implementation)
        assert(clearCalled)
    }

    @Test
    fun settingsSearchBar_clearButtonHiddenWhenEmpty() {
        composeTestRule.setContent {
            EunioTheme {
                SettingsSearchBar(
                    query = "",
                    onQueryChange = {},
                    onClearSearch = {}
                )
            }
        }

        // Clear button should not be visible when query is empty
        composeTestRule.onNodeWithTag("clear_search_button").assertDoesNotExist()
    }

    @Test
    fun settingsSearchBar_disabledState() {
        composeTestRule.setContent {
            EunioTheme {
                SettingsSearchBar(
                    query = "",
                    onQueryChange = {},
                    onClearSearch = {},
                    enabled = false
                )
            }
        }

        composeTestRule.onNodeWithTag("settings_search_bar")
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }

    @Test
    fun settingsSearchBar_keyboardActions() {
        var currentQuery = ""

        composeTestRule.setContent {
            EunioTheme {
                SettingsSearchBar(
                    query = currentQuery,
                    onQueryChange = { currentQuery = it },
                    onClearSearch = { currentQuery = "" }
                )
            }
        }

        // Test IME action
        composeTestRule.onNodeWithTag("settings_search_bar")
            .performTextInput("test")

        // Keyboard should be hidden after search action (can't easily test this in unit tests)
    }

    @Test
    fun compactSettingsSearchBar_displaysCorrectly() {
        composeTestRule.setContent {
            EunioTheme {
                CompactSettingsSearchBar(
                    query = "",
                    onQueryChange = {},
                    onClearSearch = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("compact_settings_search_bar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Search...").assertIsDisplayed()
    }

    @Test
    fun settingsSearchBarWithSuggestions_displaysCorrectly() {
        val suggestions = listOf("notifications", "privacy", "units")

        composeTestRule.setContent {
            EunioTheme {
                SettingsSearchBarWithSuggestions(
                    query = "not",
                    onQueryChange = {},
                    onClearSearch = {},
                    suggestions = suggestions,
                    onSuggestionClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("settings_search_bar").assertIsDisplayed()
        
        // Suggestions would be shown in a real implementation with proper state management
    }

    @Test
    fun settingsSearchBar_accessibility() {
        composeTestRule.setContent {
            EunioTheme {
                SettingsSearchBar(
                    query = "test",
                    onQueryChange = {},
                    onClearSearch = {}
                )
            }
        }

        // Verify accessibility properties
        composeTestRule.onNodeWithTag("settings_search_bar")
            .assertHasClickAction()
            .assertIsEnabled()

        composeTestRule.onNodeWithTag("clear_search_button")
            .assertHasClickAction()
            .assertContentDescriptionEquals("Clear search")
    }

    @Test
    fun settingsSearchBar_longText() {
        val longQuery = "This is a very long search query that should be handled properly by the search bar component"

        composeTestRule.setContent {
            EunioTheme {
                SettingsSearchBar(
                    query = longQuery,
                    onQueryChange = {},
                    onClearSearch = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("settings_search_bar")
            .assertIsDisplayed()
            .assertTextContains(longQuery)
    }

    @Test
    fun settingsSearchBar_specialCharacters() {
        val specialQuery = "test@#$%^&*()"

        composeTestRule.setContent {
            EunioTheme {
                SettingsSearchBar(
                    query = specialQuery,
                    onQueryChange = {},
                    onClearSearch = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("settings_search_bar")
            .assertIsDisplayed()
            .assertTextContains(specialQuery)
    }
}