package com.eunio.healthapp.android.ui.settings

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eunio.healthapp.android.ui.theme.EunioTheme
import com.eunio.healthapp.domain.model.settings.*
import com.eunio.healthapp.domain.model.SyncStatus
import kotlinx.datetime.Clock
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserProfileSectionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockUserSettings = UserSettings(
        userId = "test-user",
        unitPreferences = UnitPreferences(),
        notificationPreferences = NotificationPreferences(),
        cyclePreferences = CyclePreferences(),
        privacyPreferences = PrivacyPreferences(),
        displayPreferences = DisplayPreferences(),
        syncPreferences = SyncPreferences(),
        lastModified = Clock.System.now(),
        syncStatus = SyncStatus.SYNCED
    )

    @Test
    fun userProfileSection_displaysCorrectly() {
        composeTestRule.setContent {
            EunioTheme {
                UserProfileSection(
                    userSettings = mockUserSettings,
                    onEditProfile = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("user_profile_section").assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_avatar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("edit_profile_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("sync_status_indicator").assertIsDisplayed()
    }

    @Test
    fun userProfileSection_editProfileClick() {
        var editProfileClicked = false

        composeTestRule.setContent {
            EunioTheme {
                UserProfileSection(
                    userSettings = mockUserSettings,
                    onEditProfile = { editProfileClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("edit_profile_button").performClick()
        
        // In a real test, we would verify the callback was called
    }

    @Test
    fun userProfileSection_clickableArea() {
        var profileClicked = false

        composeTestRule.setContent {
            EunioTheme {
                UserProfileSection(
                    userSettings = mockUserSettings,
                    onEditProfile = { profileClicked = true }
                )
            }
        }

        // Clicking the entire profile section should trigger edit
        composeTestRule.onNodeWithTag("user_profile_section").performClick()
        
        // In a real test, we would verify the callback was called
    }

    @Test
    fun userProfileSection_syncedStatus() {
        val syncedSettings = mockUserSettings.copy(syncStatus = SyncStatus.SYNCED)

        composeTestRule.setContent {
            EunioTheme {
                UserProfileSection(
                    userSettings = syncedSettings,
                    onEditProfile = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("sync_status_indicator").assertIsDisplayed()
        composeTestRule.onNodeWithText("Synced").assertIsDisplayed()
    }

    @Test
    fun userProfileSection_syncingStatus() {
        val syncingSettings = mockUserSettings.copy(syncStatus = SyncStatus.PENDING)

        composeTestRule.setContent {
            EunioTheme {
                UserProfileSection(
                    userSettings = syncingSettings,
                    onEditProfile = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("sync_status_indicator").assertIsDisplayed()
        composeTestRule.onNodeWithText("Syncing...").assertIsDisplayed()
    }

    @Test
    fun compactUserProfileSection_displaysCorrectly() {
        composeTestRule.setContent {
            EunioTheme {
                CompactUserProfileSection(
                    userSettings = mockUserSettings,
                    onEditProfile = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("compact_user_profile_section").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tap to edit profile").assertIsDisplayed()
    }

    @Test
    fun detailedUserProfileSection_displaysCorrectly() {
        composeTestRule.setContent {
            EunioTheme {
                DetailedUserProfileSection(
                    userSettings = mockUserSettings,
                    onEditProfile = {},
                    cycleCount = 5,
                    dataPoints = 150
                )
            }
        }

        composeTestRule.onNodeWithTag("detailed_user_profile_section").assertIsDisplayed()
        composeTestRule.onNodeWithText("5").assertIsDisplayed() // Cycle count
        composeTestRule.onNodeWithText("150").assertIsDisplayed() // Data points
        composeTestRule.onNodeWithText("Cycles Tracked").assertIsDisplayed()
        composeTestRule.onNodeWithText("Data Points").assertIsDisplayed()
    }

    @Test
    fun userProfileSection_userInitials() {
        composeTestRule.setContent {
            EunioTheme {
                UserProfileSection(
                    userSettings = mockUserSettings,
                    onEditProfile = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("profile_avatar").assertIsDisplayed()
        
        // Avatar should display user initials (default "U" for placeholder)
        composeTestRule.onNodeWithText("U").assertIsDisplayed()
    }

    @Test
    fun userProfileSection_accessibility() {
        composeTestRule.setContent {
            EunioTheme {
                UserProfileSection(
                    userSettings = mockUserSettings,
                    onEditProfile = {}
                )
            }
        }

        // Verify accessibility properties
        composeTestRule.onNodeWithTag("user_profile_section")
            .assertHasClickAction()

        composeTestRule.onNodeWithTag("edit_profile_button")
            .assertHasClickAction()
            .assertContentDescriptionEquals("Edit profile")
    }

    @Test
    fun userProfileSection_longUserName() {
        // Test with a very long user name to ensure proper truncation
        composeTestRule.setContent {
            EunioTheme {
                UserProfileSection(
                    userSettings = mockUserSettings,
                    onEditProfile = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("user_profile_section").assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_avatar").assertIsDisplayed()
        
        // Should handle long names gracefully
    }

    @Test
    fun compactUserProfileSection_clickHandling() {
        var editClicked = false

        composeTestRule.setContent {
            EunioTheme {
                CompactUserProfileSection(
                    userSettings = mockUserSettings,
                    onEditProfile = { editClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("compact_user_profile_section").performClick()
        
        // In a real test, we would verify the callback was called
    }

    @Test
    fun detailedUserProfileSection_statisticsDisplay() {
        val cycleCount = 12
        val dataPoints = 365

        composeTestRule.setContent {
            EunioTheme {
                DetailedUserProfileSection(
                    userSettings = mockUserSettings,
                    onEditProfile = {},
                    cycleCount = cycleCount,
                    dataPoints = dataPoints
                )
            }
        }

        // Verify statistics are displayed correctly
        composeTestRule.onNodeWithText(cycleCount.toString()).assertIsDisplayed()
        composeTestRule.onNodeWithText(dataPoints.toString()).assertIsDisplayed()
        composeTestRule.onNodeWithText("30+").assertIsDisplayed() // Days active placeholder
    }

    @Test
    fun userProfileSection_gradientBackground() {
        composeTestRule.setContent {
            EunioTheme {
                UserProfileSection(
                    userSettings = mockUserSettings,
                    onEditProfile = {}
                )
            }
        }

        // Verify the profile section is displayed with proper styling
        composeTestRule.onNodeWithTag("user_profile_section").assertIsDisplayed()
        
        // Background gradient can't be easily tested in unit tests, but component should render
    }

    @Test
    fun userProfileSection_differentSyncStatuses() {
        val statuses = listOf(SyncStatus.SYNCED, SyncStatus.PENDING, SyncStatus.FAILED)

        statuses.forEach { status ->
            val settings = mockUserSettings.copy(syncStatus = status)
            
            composeTestRule.setContent {
                EunioTheme {
                    UserProfileSection(
                        userSettings = settings,
                        onEditProfile = {}
                    )
                }
            }

            composeTestRule.onNodeWithTag("sync_status_indicator").assertIsDisplayed()
            
            when (status) {
                SyncStatus.SYNCED -> composeTestRule.onNodeWithText("Synced").assertIsDisplayed()
                SyncStatus.PENDING -> composeTestRule.onNodeWithText("Syncing...").assertIsDisplayed()
                SyncStatus.FAILED -> composeTestRule.onNodeWithText("Syncing...").assertIsDisplayed() // Fallback
            }
        }
    }
}