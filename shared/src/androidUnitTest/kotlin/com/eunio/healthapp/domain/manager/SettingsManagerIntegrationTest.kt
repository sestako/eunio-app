package com.eunio.healthapp.domain.manager

import com.eunio.healthapp.domain.error.SettingsError
import com.eunio.healthapp.domain.model.settings.*
import com.eunio.healthapp.domain.repository.SettingsRepository
import com.eunio.healthapp.domain.usecase.cycle.PredictOvulationUseCase
import com.eunio.healthapp.domain.usecase.cycle.UpdateCycleUseCase
import com.eunio.healthapp.domain.util.Result
import io.mockk.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.*

class SettingsManagerIntegrationTest {
    
    private val mockSettingsRepository = mockk<SettingsRepository>()
    private val mockSettingsBackupManager = mockk<SettingsBackupManager>()
    private val mockNotificationManager = mockk<NotificationManager>()
    private val mockPredictOvulationUseCase = mockk<PredictOvulationUseCase>()
    private val mockUpdateCycleUseCase = mockk<UpdateCycleUseCase>()
    private val testUserId = "integration-test-user"
    private val currentUserId: () -> String = { testUserId }
    
    private lateinit var settingsManager: SettingsManagerImpl
    
    @BeforeTest
    fun setup() {
        clearAllMocks()
        settingsManager = SettingsManagerImpl(
            settingsRepository = mockSettingsRepository,
            settingsBackupManager = mockSettingsBackupManager,
            notificationManager = mockNotificationManager,
            predictOvulationUseCase = mockPredictOvulationUseCase,
            updateCycleUseCase = mockUpdateCycleUseCase,
            currentUserId = currentUserId
        )
    }
    
    @AfterTest
    fun tearDown() {
        clearAllMocks()
    }
    
    @Test
    fun `reactive settings flow emits updates when settings change`() = runTest {
        // Given
        val initialSettings = createTestUserSettings()
        val updatedUnitPrefs = UnitPreferences(
            temperatureUnit = TemperatureUnit.FAHRENHEIT,
            weightUnit = WeightUnit.POUNDS,
            isManuallySet = true
        )
        
        coEvery { mockSettingsRepository.getUserSettings(testUserId) } returns Result.success(initialSettings)
        coEvery { mockSettingsRepository.saveUserSettings(any()) } returns Result.success(Unit)
        
        // When
        settingsManager.getUserSettings() // Initialize
        
        // Get initial settings from flow
        val initialFromFlow = settingsManager.observeSettingsChanges().first()
        assertEquals(TemperatureUnit.CELSIUS, initialFromFlow.unitPreferences.temperatureUnit)
        
        // Update settings
        settingsManager.updateUnitPreferences(updatedUnitPrefs)
        
        // Get updated settings from flow
        val updatedFromFlow = settingsManager.observeSettingsChanges().first()
        assertEquals(TemperatureUnit.FAHRENHEIT, updatedFromFlow.unitPreferences.temperatureUnit)
    }
    
    @Test
    fun `notification preferences update triggers notification system reconfiguration`() = runTest {
        // Given
        val initialSettings = createTestUserSettings()
        val newNotificationPrefs = NotificationPreferences(
            dailyLoggingReminder = NotificationSetting.defaultEnabled(),
            periodPredictionAlert = NotificationSetting.defaultEnabled(),
            globalNotificationsEnabled = true
        )
        
        coEvery { mockSettingsRepository.getUserSettings(testUserId) } returns Result.success(initialSettings)
        coEvery { mockSettingsRepository.saveUserSettings(any()) } returns Result.success(Unit)
        coEvery { mockNotificationManager.updateNotificationSchedule(any()) } returns kotlin.Result.success(Unit)
        
        // When
        val result = settingsManager.updateNotificationPreferences(newNotificationPrefs)
        
        // Then
        assertTrue(result.isSuccess)
        
        // Verify the notification manager was called with the correct preferences
        coVerify { 
            mockNotificationManager.updateNotificationSchedule(
                match { prefs ->
                    prefs.dailyLoggingReminder.enabled &&
                    prefs.periodPredictionAlert.enabled &&
                    prefs.globalNotificationsEnabled
                }
            )
        }
        
        // Verify settings were saved
        coVerify { 
            mockSettingsRepository.saveUserSettings(
                match { settings ->
                    settings.notificationPreferences == newNotificationPrefs
                }
            )
        }
    }
    
    @Test
    fun `cycle preferences update triggers prediction recalculation workflow`() = runTest {
        // Given
        val initialSettings = createTestUserSettings()
        val newCyclePrefs = CyclePreferences(
            averageCycleLength = 30,
            averageLutealPhaseLength = 12,
            periodDuration = 4,
            isCustomized = true
        )
        
        coEvery { mockSettingsRepository.getUserSettings(testUserId) } returns Result.success(initialSettings)
        coEvery { mockSettingsRepository.saveUserSettings(any()) } returns Result.success(Unit)
        coEvery { mockUpdateCycleUseCase.recalculateCycleMetrics(testUserId) } returns Result.success(mockk())
        coEvery { mockPredictOvulationUseCase.updatePredictionWithCurrentData(testUserId) } returns Result.success(mockk())
        
        // When
        val result = settingsManager.updateCyclePreferences(newCyclePrefs)
        
        // Then
        assertTrue(result.isSuccess)
        
        // Verify the prediction workflow was triggered in correct order
        coVerifyOrder {
            mockSettingsRepository.saveUserSettings(any())
            mockUpdateCycleUseCase.recalculateCycleMetrics(testUserId)
            mockPredictOvulationUseCase.updatePredictionWithCurrentData(testUserId)
        }
    }
    
    @Test
    fun `settings validation prevents invalid data from being saved`() = runTest {
        // Given
        val initialSettings = createTestUserSettings()
        coEvery { mockSettingsRepository.getUserSettings(testUserId) } returns Result.success(initialSettings)
        
        // When - Try to update with invalid cycle preferences
        val invalidCyclePrefs = CyclePreferences(
            averageCycleLength = 50, // Too long
            averageLutealPhaseLength = 20, // Too long
            periodDuration = 10, // Too long
            isCustomized = true
        )
        
        val result = settingsManager.updateCyclePreferences(invalidCyclePrefs)
        
        // Then
        assertTrue(result.isError)
        assertTrue(result.errorOrNull() is SettingsError.ValidationError)
        
        // Verify no save operation was attempted
        coVerify(exactly = 0) { mockSettingsRepository.saveUserSettings(any()) }
        coVerify(exactly = 0) { mockUpdateCycleUseCase.recalculateCycleMetrics(any()) }
    }
    
    @Test
    fun `atomic settings update ensures all-or-nothing behavior`() = runTest {
        // Given
        val initialSettings = createTestUserSettings()
        coEvery { mockSettingsRepository.getUserSettings(testUserId) } returns Result.success(initialSettings)
        coEvery { mockSettingsRepository.saveUserSettings(any()) } returns Result.error(
            SettingsError.PersistenceError("Database error")
        )
        
        // When - Try to update multiple settings sections
        val result = settingsManager.updateSettings { settings ->
            settings.copy(
                unitPreferences = settings.unitPreferences.copy(temperatureUnit = TemperatureUnit.FAHRENHEIT),
                displayPreferences = settings.displayPreferences.copy(textSizeScale = 1.5f)
            )
        }
        
        // Then
        assertTrue(result.isError)
        assertTrue(result.errorOrNull() is SettingsError.PersistenceError)
        
        // Verify the settings flow was not updated due to save failure
        val currentSettings = settingsManager.observeSettingsChanges().first()
        assertEquals(TemperatureUnit.CELSIUS, currentSettings.unitPreferences.temperatureUnit)
        assertEquals(1.0f, currentSettings.displayPreferences.textSizeScale)
    }
    
    @Test
    fun `sync status flow reflects settings sync state changes`() = runTest {
        // Given
        val unsyncedSettings = createTestUserSettings() // Default sync status is PENDING
        
        coEvery { mockSettingsRepository.getUserSettings(testUserId) } returns Result.success(unsyncedSettings)
        coEvery { mockSettingsRepository.saveUserSettings(any()) } returns Result.success(Unit)
        
        // When
        settingsManager.getUserSettings() // Initialize with unsynced settings
        
        // Check initial sync status
        val initialSyncStatus = settingsManager.observeSyncStatus().first()
        assertFalse(initialSyncStatus) // Should be false for PENDING status
        
        // Update settings (this should keep sync status as false since it creates PENDING status)
        settingsManager.updateUnitPreferences(
            UnitPreferences(temperatureUnit = TemperatureUnit.FAHRENHEIT, isManuallySet = true)
        )
        
        // Check sync status after update
        val updatedSyncStatus = settingsManager.observeSyncStatus().first()
        assertFalse(updatedSyncStatus) // Should still be false after update (PENDING)
    }
    
    @Test
    fun `settings import preserves current user ID and triggers dependent updates`() = runTest {
        // Given
        val importedSettings = UserSettings.createDefault("different-user-id").copy(
            unitPreferences = UnitPreferences(
                temperatureUnit = TemperatureUnit.FAHRENHEIT,
                weightUnit = WeightUnit.POUNDS,
                isManuallySet = true
            ),
            notificationPreferences = NotificationPreferences.withDefaults(),
            cyclePreferences = CyclePreferences.create(30, 12, 4)
        )
        
        val exportData = """{"userId":"different-user-id","unitPreferences":{"temperatureUnit":"FAHRENHEIT","weightUnit":"POUNDS","isManuallySet":true},"notificationPreferences":{"dailyLoggingReminder":{"enabled":true,"time":"20:00","daysInAdvance":1},"periodPredictionAlert":{"enabled":true,"time":"20:00","daysInAdvance":1},"ovulationAlert":{"enabled":true,"time":"20:00","daysInAdvance":1},"insightNotifications":{"enabled":false,"time":null,"daysInAdvance":1},"globalNotificationsEnabled":true},"cyclePreferences":{"averageCycleLength":30,"averageLutealPhaseLength":12,"periodDuration":4,"isCustomized":true},"privacyPreferences":{"dataSharingEnabled":false,"anonymousInsightsEnabled":true,"crashReportingEnabled":true,"analyticsEnabled":true},"displayPreferences":{"textSizeScale":1.0,"highContrastMode":false,"hapticFeedbackEnabled":true,"hapticIntensity":"MEDIUM"},"syncPreferences":{"autoSyncEnabled":true,"wifiOnlySync":false,"cloudBackupEnabled":true,"lastSyncTime":null},"lastModified":"2024-01-01T00:00:00Z","syncStatus":"PENDING","version":1}"""
        
        coEvery { mockSettingsRepository.saveUserSettings(any()) } returns Result.success(Unit)
        coEvery { mockSettingsRepository.getUserSettings(testUserId) } returns Result.success(importedSettings.copy(userId = testUserId))
        coEvery { mockNotificationManager.updateNotificationSchedule(any()) } returns kotlin.Result.success(Unit)
        coEvery { mockUpdateCycleUseCase.recalculateCycleMetrics(testUserId) } returns Result.success(mockk())
        coEvery { mockPredictOvulationUseCase.updatePredictionWithCurrentData(testUserId) } returns Result.success(mockk())
        
        // Mock the backup manager to return success for import
        coEvery { mockSettingsBackupManager.importSettings(testUserId, exportData, any()) } returns Result.success(Unit)
        
        // When
        val result = settingsManager.importSettings(exportData)
        
        // Then
        assertTrue(result.isSuccess)
        
        // Verify settings were saved with current user ID
        coVerify { 
            mockSettingsRepository.saveUserSettings(
                match { settings ->
                    settings.userId == testUserId && // Should use current user, not imported user
                    settings.unitPreferences.temperatureUnit == TemperatureUnit.FAHRENHEIT &&
                    settings.cyclePreferences.averageCycleLength == 30
                }
            )
        }
        
        // Verify dependent systems were updated
        coVerify { mockNotificationManager.updateNotificationSchedule(any()) }
        coVerify { mockUpdateCycleUseCase.recalculateCycleMetrics(testUserId) }
        coVerify { mockPredictOvulationUseCase.updatePredictionWithCurrentData(testUserId) }
    }
    
    @Test
    fun `reset to defaults preserves unit preferences when requested and updates notifications`() = runTest {
        // Given
        val currentSettings = createTestUserSettings().copy(
            unitPreferences = UnitPreferences(
                temperatureUnit = TemperatureUnit.FAHRENHEIT,
                weightUnit = WeightUnit.POUNDS,
                isManuallySet = true
            ),
            notificationPreferences = NotificationPreferences.withDefaults(),
            displayPreferences = DisplayPreferences(textSizeScale = 1.5f, highContrastMode = true)
        )
        
        coEvery { mockSettingsRepository.getUserSettings(testUserId) } returns Result.success(currentSettings)
        coEvery { mockSettingsRepository.saveUserSettings(any()) } returns Result.success(Unit)
        coEvery { mockNotificationManager.updateNotificationSchedule(any()) } returns kotlin.Result.success(Unit)
        
        // When
        val result = settingsManager.resetToDefaults(preserveUnitPreferences = true)
        
        // Then
        assertTrue(result.isSuccess)
        val resetSettings = result.getOrNull()
        assertNotNull(resetSettings)
        
        // Unit preferences should be preserved
        assertEquals(TemperatureUnit.FAHRENHEIT, resetSettings.unitPreferences.temperatureUnit)
        assertEquals(WeightUnit.POUNDS, resetSettings.unitPreferences.weightUnit)
        assertTrue(resetSettings.unitPreferences.isManuallySet)
        
        // Other preferences should be reset to defaults
        assertEquals(NotificationPreferences.default(), resetSettings.notificationPreferences)
        assertEquals(DisplayPreferences.default(), resetSettings.displayPreferences)
        
        // Verify notification system was updated with default preferences
        coVerify { 
            mockNotificationManager.updateNotificationSchedule(
                NotificationPreferences.default()
            )
        }
    }
    
    @Test
    fun `business logic validation prevents inconsistent settings combinations`() = runTest {
        // Given
        val initialSettings = createTestUserSettings()
        coEvery { mockSettingsRepository.getUserSettings(testUserId) } returns Result.success(initialSettings)
        
        // When - Try to create settings with inconsistent haptic feedback configuration
        val result = settingsManager.updateSettings { settings ->
            settings.copy(
                displayPreferences = DisplayPreferences(
                    textSizeScale = 1.0f,
                    highContrastMode = false,
                    hapticFeedbackEnabled = false, // Disabled
                    hapticIntensity = HapticIntensity.STRONG // But intensity is not disabled
                )
            )
        }
        
        // Then
        assertTrue(result.isError)
        assertTrue(result.errorOrNull() is SettingsError.ValidationError)
        
        // Verify no save operation was attempted
        coVerify(exactly = 0) { mockSettingsRepository.saveUserSettings(any()) }
    }
    
    private fun createTestUserSettings(): UserSettings {
        return UserSettings.createDefault(testUserId).copy(
            lastModified = Clock.System.now()
        )
    }
}