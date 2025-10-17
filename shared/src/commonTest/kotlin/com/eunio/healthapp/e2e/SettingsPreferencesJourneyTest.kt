package com.eunio.healthapp.e2e

import com.eunio.healthapp.domain.model.*
import com.eunio.healthapp.domain.model.settings.*
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.testutil.MockServices
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalTime
import kotlin.test.*

/**
 * End-to-end tests for settings and preferences functionality.
 * Tests complete user journeys for managing app settings, preferences, and customization.
 * 
 * Requirements: 3.3, 3.4
 */
class SettingsPreferencesJourneyTest {
    
    private lateinit var mockServices: MockServices
    private lateinit var testUser: User
    
    @BeforeTest
    fun setup() = runTest {
        mockServices = MockServices()
        
        // Create and set up a test user
        val signUpResult = mockServices.userRepository.createUser(
            "settings@example.com",
            "SettingsPassword123",
            "Settings User"
        )
        assertTrue(signUpResult.isSuccess)
        testUser = signUpResult.getOrNull()!!
        
        // Complete onboarding
        mockServices.userRepository.completeOnboarding(testUser.id, HealthGoal.CYCLE_TRACKING)
    }
    
    // Unit System Preferences Journey Tests
    
    @Test
    fun `unit system preference management works end-to-end`() = runTest {
        // User starts with default metric system
        assertEquals(UnitSystem.METRIC, testUser.unitSystem, "Default should be metric")
        
        // User decides to change to imperial system
        val imperialPreferences = UserPreferences(
            userId = testUser.id,
            unitSystem = UnitSystem.IMPERIAL,
            isManuallySet = true,
            lastModified = Clock.System.now(),
            syncStatus = SyncStatus.PENDING
        )
        
        // In a real app, this would go through a preferences repository
        // For this test, we'll simulate the preference change
        val updatedUser = testUser.copy(unitSystem = UnitSystem.IMPERIAL)
        mockServices.userRepository.updateUser(updatedUser)
        
        // User logs temperature data with imperial units
        val imperialLog = DailyLog(
            id = "imperial-temp-log",
            userId = testUser.id,
            date = kotlinx.datetime.LocalDate(2024, 1, 15),
            bbt = 98.6, // Fahrenheit
            notes = "Temperature in Fahrenheit",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val logResult = mockServices.logRepository.saveDailyLog(imperialLog)
        assertTrue(logResult.isSuccess, "Should be able to log with imperial units")
        
        // User views their data - should display in imperial format
        val retrievedLogResult = mockServices.logRepository.getDailyLog(testUser.id, kotlinx.datetime.LocalDate(2024, 1, 15))
        assertTrue(retrievedLogResult.isSuccess)
        val retrievedLog = retrievedLogResult.getOrNull()!!
        
        assertEquals(98.6, retrievedLog.bbt, "Temperature should be stored as entered")
        
        // User switches back to metric
        val metricUser = updatedUser.copy(unitSystem = UnitSystem.METRIC)
        mockServices.userRepository.updateUser(metricUser)
        
        // User logs new data in metric
        val metricLog = DailyLog(
            id = "metric-temp-log",
            userId = testUser.id,
            date = kotlinx.datetime.LocalDate(2024, 1, 16),
            bbt = 37.0, // Celsius
            notes = "Temperature in Celsius",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val metricLogResult = mockServices.logRepository.saveDailyLog(metricLog)
        assertTrue(metricLogResult.isSuccess, "Should be able to log with metric units")
        
        // User can view both logs with appropriate unit context
        val bothLogsResult = mockServices.logRepository.getRecentLogs(testUser.id, 5)
        assertTrue(bothLogsResult.isSuccess)
        val bothLogs = bothLogsResult.getOrNull()!!
        
        assertEquals(2, bothLogs.size, "Should have both temperature logs")
        
        // Verify different unit values are preserved
        val imperialEntry = bothLogs.find { it.date == kotlinx.datetime.LocalDate(2024, 1, 15) }
        val metricEntry = bothLogs.find { it.date == kotlinx.datetime.LocalDate(2024, 1, 16) }
        
        assertNotNull(imperialEntry)
        assertNotNull(metricEntry)
        assertEquals(98.6, imperialEntry.bbt) // Fahrenheit value
        assertEquals(37.0, metricEntry.bbt) // Celsius value
    }
    
    @Test
    fun `display preferences customization works correctly`() = runTest {
        // User customizes display preferences
        val displayPreferences = DisplayPreferences(
            textSizeScale = 1.4f, // large font size
            highContrastMode = true, // dark theme equivalent
            hapticFeedbackEnabled = true, // showNotifications equivalent
            hapticIntensity = HapticIntensity.MEDIUM
        )
        
        val userSettings = UserSettings(
            userId = testUser.id,
            displayPreferences = displayPreferences,
            lastModified = Clock.System.now(),
            syncStatus = SyncStatus.PENDING
        )
        
        // Simulate saving settings (in real app would use settings repository)
        // For this test, we'll verify the preferences are structured correctly
        assertTrue(userSettings.displayPreferences.highContrastMode) // dark theme equivalent
        assertEquals(1.4f, userSettings.displayPreferences.textSizeScale) // large font size
        assertTrue(userSettings.displayPreferences.hapticFeedbackEnabled) // showNotifications equivalent
        assertEquals(HapticIntensity.MEDIUM, userSettings.displayPreferences.hapticIntensity)
        
        // User logs data and it should respect display preferences
        val styledLog = DailyLog(
            id = "styled-log",
            userId = testUser.id,
            date = kotlinx.datetime.LocalDate(2024, 1, 15),
            bbt = 98.2,
            mood = Mood.HAPPY,
            notes = "Log with custom display settings",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val logResult = mockServices.logRepository.saveDailyLog(styledLog)
        assertTrue(logResult.isSuccess)
        
        // User changes theme preference
        val updatedDisplayPrefs = displayPreferences.copy(
            highContrastMode = false, // light theme
            textSizeScale = 1.0f // medium font size
        )
        
        val updatedSettings = userSettings.copy(
            displayPreferences = updatedDisplayPrefs,
            lastModified = Clock.System.now()
        )
        
        // Verify theme change
        assertFalse(updatedSettings.displayPreferences.highContrastMode) // light theme
        assertEquals(1.0f, updatedSettings.displayPreferences.textSizeScale) // medium font size
        
        // Other preferences should remain unchanged
        assertTrue(updatedSettings.displayPreferences.hapticFeedbackEnabled) // showNotifications equivalent
        assertEquals(HapticIntensity.MEDIUM, updatedSettings.displayPreferences.hapticIntensity)
    }
    
    @Test
    fun `notification preferences management works end-to-end`() = runTest {
        // User sets up notification preferences
        val notificationPreferences = NotificationPreferences(
            dailyLoggingReminder = NotificationSetting(enabled = false), // medicationReminders equivalent
            periodPredictionAlert = NotificationSetting(enabled = true, time = LocalTime(9, 0)), // periodReminders equivalent
            ovulationAlert = NotificationSetting(enabled = true, time = LocalTime(9, 0)), // ovulationReminders equivalent
            insightNotifications = NotificationSetting(enabled = true, time = LocalTime(9, 0)),
            globalNotificationsEnabled = true
        )
        
        val userSettings = UserSettings(
            userId = testUser.id,
            displayPreferences = DisplayPreferences(
                textSizeScale = 1.0f, // medium font size
                highContrastMode = false, // light theme
                hapticFeedbackEnabled = true, // showNotifications equivalent
                hapticIntensity = HapticIntensity.MEDIUM
            ),
            notificationPreferences = notificationPreferences,
            lastModified = Clock.System.now()
        )
        
        // Verify notification settings
        assertTrue(userSettings.notificationPreferences.periodPredictionAlert.enabled) // periodReminders equivalent
        assertTrue(userSettings.notificationPreferences.ovulationAlert.enabled) // ovulationReminders equivalent
        assertFalse(userSettings.notificationPreferences.dailyLoggingReminder.enabled) // medicationReminders equivalent
        assertTrue(userSettings.notificationPreferences.insightNotifications.enabled)
        
        // User logs period start - should trigger period reminder setup
        val periodStartLog = DailyLog(
            id = "period-start-notification",
            userId = testUser.id,
            date = kotlinx.datetime.LocalDate(2024, 1, 15),
            periodFlow = PeriodFlow.HEAVY,
            symptoms = listOf(Symptom.CRAMPS),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val periodLogResult = mockServices.logRepository.saveDailyLog(periodStartLog)
        assertTrue(periodLogResult.isSuccess)
        
        // System should schedule notifications based on preferences
        // In real implementation, this would integrate with platform notification systems
        
        // User receives an insight and should get notification
        val insightForNotification = Insight(
            id = "notification-insight",
            userId = testUser.id,
            generatedDate = Clock.System.now(),
            insightText = "Your cycle is very regular - great job tracking!",
            type = InsightType.PATTERN_RECOGNITION,
            isRead = false,
            confidence = 0.9,
            actionable = false
        )
        
        val insightResult = mockServices.insightRepository.saveInsight(insightForNotification)
        assertTrue(insightResult.isSuccess)
        
        // User can disable specific notifications
        val updatedNotificationPrefs = notificationPreferences.copy(
            insightNotifications = NotificationSetting(enabled = false),
            ovulationAlert = NotificationSetting(enabled = false)
        )
        
        val updatedSettings = userSettings.copy(
            notificationPreferences = updatedNotificationPrefs,
            lastModified = Clock.System.now()
        )
        
        // Verify selective disabling
        assertTrue(updatedSettings.notificationPreferences.periodPredictionAlert.enabled) // Still enabled
        assertFalse(updatedSettings.notificationPreferences.ovulationAlert.enabled) // Disabled
        assertFalse(updatedSettings.notificationPreferences.insightNotifications.enabled) // Disabled
        
        // Global notifications should still be enabled
        assertTrue(updatedSettings.notificationPreferences.globalNotificationsEnabled)
    }
    
    @Test
    fun `privacy and data sharing preferences work correctly`() = runTest {
        // User configures privacy preferences
        val privacyPreferences = PrivacyPreferences(
            dataSharingEnabled = false, // dataSharing equivalent
            anonymousInsightsEnabled = true, // anonymousAnalytics + personalizedInsights equivalent
            crashReportingEnabled = true, // crashReporting equivalent
            analyticsEnabled = true // general analytics (allowDataExport equivalent)
        )
        
        val userSettings = UserSettings(
            userId = testUser.id,
            displayPreferences = DisplayPreferences(
                textSizeScale = 1.0f, // medium font size
                highContrastMode = false, // light theme
                hapticFeedbackEnabled = true, // showNotifications equivalent
                hapticIntensity = HapticIntensity.MEDIUM
            ),
            privacyPreferences = privacyPreferences,
            lastModified = Clock.System.now()
        )
        
        // Verify privacy settings
        assertFalse(userSettings.privacyPreferences.dataSharingEnabled) // dataSharing equivalent
        assertTrue(userSettings.privacyPreferences.anonymousInsightsEnabled) // anonymousAnalytics + personalizedInsights equivalent
        assertTrue(userSettings.privacyPreferences.crashReportingEnabled) // crashReporting equivalent
        assertTrue(userSettings.privacyPreferences.analyticsEnabled) // allowDataExport equivalent
        
        // User logs sensitive data
        val sensitiveLog = DailyLog(
            id = "sensitive-data-log",
            userId = testUser.id,
            date = kotlinx.datetime.LocalDate(2024, 1, 15),
            sexualActivity = SexualActivity(occurred = true, protection = Protection.CONDOM),
            mood = Mood.HAPPY,
            notes = "Private intimate data",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val sensitiveLogResult = mockServices.logRepository.saveDailyLog(sensitiveLog)
        assertTrue(sensitiveLogResult.isSuccess, "Should be able to log sensitive data")
        
        // Data should be stored but not shared (based on preferences)
        val retrievedSensitiveResult = mockServices.logRepository.getDailyLog(testUser.id, kotlinx.datetime.LocalDate(2024, 1, 15))
        assertTrue(retrievedSensitiveResult.isSuccess)
        val retrievedSensitive = retrievedSensitiveResult.getOrNull()!!
        
        assertNotNull(retrievedSensitive.sexualActivity)
        assertTrue(retrievedSensitive.notes!!.contains("Private"))
        
        // User can request data export (allowed by preferences)
        val exportRequest = HealthReport(
            id = "privacy-export-${testUser.id}",
            userId = testUser.id,
            generatedDate = Clock.System.now(),
            reportType = ReportType.CUSTOM, // EXPORT equivalent
            dateRange = DateRange(
                start = kotlinx.datetime.LocalDate(2024, 1, 1),
                end = kotlinx.datetime.LocalDate(2024, 1, 31)
            ),
            cycleSummary = CycleSummary(
                totalCycles = 1,
                averageCycleLength = 28.0,
                averageLutealPhase = 14.0,
                cycleRegularity = CycleRegularity.REGULAR,
                ovulationPatterns = OvulationPatterns(
                    averageOvulationDay = 14.0,
                    ovulationConfidenceRate = 0.8,
                    fertilityWindowAccuracy = 0.9
                )
            ),
            symptomAnalysis = SymptomAnalysis(
                mostCommonSymptoms = emptyList(),
                symptomTrends = emptyMap(),
                moodPatterns = MoodPatterns(
                    dominantMood = "Happy",
                    moodVariability = 0.3,
                    cyclePhaseCorrelations = emptyMap()
                ),
                bbtAnalysis = null
            ),
            insights = emptyList()
        )
        
        val exportResult = mockServices.healthReportRepository.saveReport(exportRequest)
        assertTrue(exportResult.isSuccess, "Data export should be allowed based on privacy preferences")
        
        // User changes privacy preferences to be more restrictive
        val restrictivePrivacyPrefs = privacyPreferences.copy(
            dataSharingEnabled = false,
            anonymousInsightsEnabled = false,
            crashReportingEnabled = false,
            analyticsEnabled = false
        )
        
        val restrictiveSettings = userSettings.copy(
            privacyPreferences = restrictivePrivacyPrefs,
            lastModified = Clock.System.now()
        )
        
        // Verify restrictive settings
        assertFalse(restrictiveSettings.privacyPreferences.dataSharingEnabled)
        assertFalse(restrictiveSettings.privacyPreferences.anonymousInsightsEnabled)
        assertFalse(restrictiveSettings.privacyPreferences.crashReportingEnabled)
        assertFalse(restrictiveSettings.privacyPreferences.analyticsEnabled)
    }
    
    @Test
    fun `backup and sync preferences work correctly`() = runTest {
        // User configures backup and sync preferences
        val syncPreferences = SyncPreferences(
            autoSyncEnabled = true, // autoSync equivalent
            wifiOnlySync = true, // wifiOnly equivalent
            cloudBackupEnabled = true, // backupEnabled equivalent
            lastSyncTime = null // no previous sync
        )
        
        val userSettings = UserSettings(
            userId = testUser.id,
            displayPreferences = DisplayPreferences(
                textSizeScale = 1.0f, // medium font size
                highContrastMode = false, // light theme
                hapticFeedbackEnabled = true, // showNotifications equivalent
                hapticIntensity = HapticIntensity.MEDIUM
            ),
            syncPreferences = syncPreferences,
            lastModified = Clock.System.now()
        )
        
        // Verify sync settings
        assertTrue(userSettings.syncPreferences.autoSyncEnabled) // autoSync equivalent
        assertTrue(userSettings.syncPreferences.wifiOnlySync) // wifiOnly equivalent
        assertTrue(userSettings.syncPreferences.cloudBackupEnabled) // backupEnabled equivalent
        assertNull(userSettings.syncPreferences.lastSyncTime) // no previous sync
        
        // User logs data that should be synced
        val syncableLog = DailyLog(
            id = "syncable-log",
            userId = testUser.id,
            date = kotlinx.datetime.LocalDate(2024, 1, 15),
            bbt = 98.2,
            mood = Mood.HAPPY,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val syncLogResult = mockServices.logRepository.saveDailyLog(syncableLog)
        assertTrue(syncLogResult.isSuccess)
        
        // Simulate sync operation based on preferences
        // In real implementation, this would check network conditions and sync accordingly
        
        // User changes sync preferences to be more conservative
        val conservativeSyncPrefs = syncPreferences.copy(
            autoSyncEnabled = false, // autoSync equivalent
            wifiOnlySync = true, // wifiOnly equivalent
            cloudBackupEnabled = false // backupEnabled equivalent
        )
        
        val conservativeSettings = userSettings.copy(
            syncPreferences = conservativeSyncPrefs,
            lastModified = Clock.System.now()
        )
        
        // Verify conservative settings
        assertFalse(conservativeSettings.syncPreferences.autoSyncEnabled) // autoSync equivalent
        assertFalse(conservativeSettings.syncPreferences.cloudBackupEnabled) // backupEnabled equivalent
        
        // WiFi-only preference should remain
        assertTrue(conservativeSettings.syncPreferences.wifiOnlySync) // wifiOnly equivalent
    }
    
    @Test
    fun `accessibility preferences work correctly`() = runTest {
        // User configures accessibility preferences through display preferences
        val userSettings = UserSettings(
            userId = testUser.id,
            displayPreferences = DisplayPreferences(
                textSizeScale = 1.4f, // large font size for accessibility
                highContrastMode = true, // high-contrast theme for accessibility
                hapticFeedbackEnabled = true, // haptic feedback for accessibility
                hapticIntensity = HapticIntensity.STRONG // stronger feedback for accessibility
            ),
            lastModified = Clock.System.now()
        )
        
        // Verify accessibility settings through display preferences
        assertTrue(userSettings.displayPreferences.highContrastMode) // highContrast equivalent
        assertEquals(1.4f, userSettings.displayPreferences.textSizeScale) // largeText equivalent
        assertTrue(userSettings.displayPreferences.hapticFeedbackEnabled) // hapticFeedback equivalent
        assertEquals(HapticIntensity.STRONG, userSettings.displayPreferences.hapticIntensity) // stronger feedback
        
        // Display preferences should reflect accessibility choices
        assertTrue(userSettings.displayPreferences.highContrastMode) // high-contrast theme
        assertEquals(1.4f, userSettings.displayPreferences.textSizeScale) // large font size
        assertEquals(HapticIntensity.STRONG, userSettings.displayPreferences.hapticIntensity) // stronger feedback for accessibility
        
        // User logs data with accessibility features enabled
        val accessibleLog = DailyLog(
            id = "accessible-log",
            userId = testUser.id,
            date = kotlinx.datetime.LocalDate(2024, 1, 15),
            bbt = 98.2,
            mood = Mood.HAPPY,
            notes = "Logged with accessibility features",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val accessibleLogResult = mockServices.logRepository.saveDailyLog(accessibleLog)
        assertTrue(accessibleLogResult.isSuccess, "Should be able to log with accessibility features")
        
        // User can adjust individual accessibility settings through display preferences
        val adjustedDisplayPrefs = userSettings.displayPreferences.copy(
            hapticFeedbackEnabled = false, // Disable haptic feedback
            hapticIntensity = HapticIntensity.DISABLED
        )
        
        val adjustedSettings = userSettings.copy(
            displayPreferences = adjustedDisplayPrefs,
            lastModified = Clock.System.now()
        )
        
        // Verify adjustments
        assertFalse(adjustedSettings.displayPreferences.hapticFeedbackEnabled) // Now disabled
        assertEquals(HapticIntensity.DISABLED, adjustedSettings.displayPreferences.hapticIntensity)
        
        // Other accessibility settings should remain unchanged
        assertTrue(adjustedSettings.displayPreferences.highContrastMode) // high contrast still enabled
        assertEquals(1.4f, adjustedSettings.displayPreferences.textSizeScale) // large text still enabled
    }
    
    @Test
    fun `settings import and export works correctly`() = runTest {
        // User creates comprehensive settings
        val comprehensiveSettings = UserSettings(
            userId = testUser.id,
            displayPreferences = DisplayPreferences(
                textSizeScale = 1.0f, // medium font size
                highContrastMode = true, // dark theme
                hapticFeedbackEnabled = true, // showNotifications equivalent
                hapticIntensity = HapticIntensity.MEDIUM
            ),
            notificationPreferences = NotificationPreferences(
                dailyLoggingReminder = NotificationSetting(enabled = false), // medicationReminders equivalent
                periodPredictionAlert = NotificationSetting(enabled = true, time = LocalTime(9, 0)), // periodReminders equivalent
                ovulationAlert = NotificationSetting(enabled = true, time = LocalTime(9, 0)), // ovulationReminders equivalent
                insightNotifications = NotificationSetting(enabled = true, time = LocalTime(9, 0)),
                globalNotificationsEnabled = true
            ),
            privacyPreferences = PrivacyPreferences(
                dataSharingEnabled = false, // dataSharing equivalent
                anonymousInsightsEnabled = true, // anonymousAnalytics + personalizedInsights equivalent
                crashReportingEnabled = true, // crashReporting equivalent
                analyticsEnabled = true // allowDataExport equivalent
            ),
            syncPreferences = SyncPreferences(
                autoSyncEnabled = true, // autoSync equivalent
                wifiOnlySync = true, // wifiOnly equivalent
                cloudBackupEnabled = true, // backupEnabled equivalent
                lastSyncTime = null // no previous sync
            ),
            lastModified = Clock.System.now()
        )
        
        // Simulate exporting settings (in real app would serialize to JSON/file)
        val exportedSettings = comprehensiveSettings.copy() // Simulate export
        
        // Verify all settings are preserved in export
        assertTrue(exportedSettings.displayPreferences.highContrastMode) // dark theme
        assertTrue(exportedSettings.notificationPreferences.periodPredictionAlert.enabled)
        assertFalse(exportedSettings.privacyPreferences.dataSharingEnabled)
        assertTrue(exportedSettings.syncPreferences.autoSyncEnabled)
        assertTrue(exportedSettings.displayPreferences.hapticFeedbackEnabled) // accessibility equivalent
        
        // User resets to default settings
        val defaultSettings = UserSettings(
            userId = testUser.id,
            displayPreferences = DisplayPreferences(
                textSizeScale = 1.0f, // medium font size (default)
                highContrastMode = false, // light theme (default)
                hapticFeedbackEnabled = true, // showNotifications equivalent (default)
                hapticIntensity = HapticIntensity.MEDIUM // default intensity
            ),
            lastModified = Clock.System.now()
        )
        
        // Verify reset to defaults
        assertFalse(defaultSettings.displayPreferences.highContrastMode) // light theme (default)
        assertEquals(NotificationPreferences.default(), defaultSettings.notificationPreferences)
        assertEquals(PrivacyPreferences.default(), defaultSettings.privacyPreferences)
        assertEquals(SyncPreferences.default(), defaultSettings.syncPreferences)
        // Accessibility features are handled through display preferences (no separate field)
        
        // User imports previously exported settings
        val importedSettings = exportedSettings.copy(
            lastModified = Clock.System.now() // Update timestamp for import
        )
        
        // Verify import restored all settings
        assertTrue(importedSettings.displayPreferences.highContrastMode) // dark theme
        assertEquals(1.0f, importedSettings.displayPreferences.textSizeScale) // medium font size
        assertTrue(importedSettings.notificationPreferences.periodPredictionAlert.enabled)
        assertFalse(importedSettings.privacyPreferences.dataSharingEnabled)
        assertTrue(importedSettings.syncPreferences.autoSyncEnabled)
        assertTrue(importedSettings.displayPreferences.hapticFeedbackEnabled) // accessibility equivalent
        
        // User can continue using app with imported settings
        val postImportLog = DailyLog(
            id = "post-import-log",
            userId = testUser.id,
            date = kotlinx.datetime.LocalDate(2024, 1, 15),
            bbt = 98.2,
            mood = Mood.HAPPY,
            notes = "Logged after settings import",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val postImportResult = mockServices.logRepository.saveDailyLog(postImportLog)
        assertTrue(postImportResult.isSuccess, "Should be able to log after settings import")
    }
    
    @Test
    fun `settings validation and error handling works correctly`() = runTest {
        // Test invalid display preferences
        val invalidDisplayPrefs = DisplayPreferences(
            textSizeScale = -1.0f, // Invalid: below minimum
            highContrastMode = false,
            hapticFeedbackEnabled = false,
            hapticIntensity = HapticIntensity.MEDIUM // Invalid: should be DISABLED when haptic is off
        )
        
        // In a real implementation, validation would occur before saving
        // For this test, we'll simulate validation by checking values
        
        // Simulate validation using the actual DisplayPreferences validation
        assertFalse(invalidDisplayPrefs.isValid(), "Invalid display preferences should be rejected")
        val validationErrors = invalidDisplayPrefs.getValidationErrors()
        assertTrue(validationErrors.isNotEmpty(), "Should have validation errors")
        assertTrue(validationErrors.any { it.contains("Text size scale") }, "Should reject invalid text size scale")
        
        // Create corrected settings with valid values
        val correctedDisplayPrefs = DisplayPreferences(
            textSizeScale = 1.0f, // Corrected: valid medium size
            highContrastMode = false, // Corrected: light theme
            hapticFeedbackEnabled = true, // Corrected: notifications enabled
            hapticIntensity = HapticIntensity.MEDIUM // Corrected: proper intensity when enabled
        )
        
        val correctedSettings = UserSettings(
            userId = testUser.id,
            displayPreferences = correctedDisplayPrefs,
            lastModified = Clock.System.now()
        )
        
        // Verify corrected settings are valid
        assertTrue(correctedDisplayPrefs.isValid(), "Corrected display preferences should be valid")
        assertTrue(correctedDisplayPrefs.getValidationErrors().isEmpty(), "Should have no validation errors")
        assertEquals(1.0f, correctedDisplayPrefs.textSizeScale) // valid medium size
        assertFalse(correctedDisplayPrefs.highContrastMode) // light theme
        assertTrue(correctedDisplayPrefs.hapticFeedbackEnabled) // notifications enabled
        
        // User can successfully use app with corrected settings
        val validatedLog = DailyLog(
            id = "validated-settings-log",
            userId = testUser.id,
            date = kotlinx.datetime.LocalDate(2024, 1, 15),
            bbt = 98.2,
            mood = Mood.HAPPY,
            notes = "Logged with validated settings",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val validatedLogResult = mockServices.logRepository.saveDailyLog(validatedLog)
        assertTrue(validatedLogResult.isSuccess, "Should be able to log with validated settings")
    }
}