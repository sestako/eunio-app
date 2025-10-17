package com.eunio.healthapp.domain.manager

import android.content.Context
import android.content.SharedPreferences
import com.eunio.healthapp.domain.model.SyncStatus
import com.eunio.healthapp.domain.model.settings.*
import com.eunio.healthapp.domain.repository.SettingsRepository
import com.eunio.healthapp.domain.util.Result
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlin.test.*

class AndroidSettingsManagerTest {
    
    private val mockContext = mockk<Context>()
    private val mockSettingsRepository = mockk<SettingsRepository>()
    private val mockSharedPreferences = mockk<SharedPreferences>()
    private val mockEditor = mockk<SharedPreferences.Editor>()
    
    private lateinit var androidSettingsManager: AndroidSettingsManager
    
    @BeforeTest
    fun setup() {
        // Setup SharedPreferences mocks
        every { mockContext.getSharedPreferences("eunio_settings", Context.MODE_PRIVATE) } returns mockSharedPreferences
        every { mockSharedPreferences.edit() } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { mockEditor.putBoolean(any(), any()) } returns mockEditor
        every { mockEditor.apply() } just Runs
        
        androidSettingsManager = AndroidSettingsManager(mockContext, mockSettingsRepository)
    }
    
    @AfterTest
    fun tearDown() {
        clearAllMocks()
    }
    
    @Test
    fun `getUserSettings returns settings from repository when available`() = runTest {
        // Given
        val expectedSettings = createTestUserSettings()
        coEvery { mockSettingsRepository.getUserSettings() } returns Result.success(expectedSettings)
        
        // When
        val result = androidSettingsManager.getUserSettings()
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedSettings, result.getOrNull())
        coVerify { mockSettingsRepository.getUserSettings() }
    }
    
    @Test
    fun `getUserSettings falls back to SharedPreferences when repository fails`() = runTest {
        // Given
        val expectedSettings = createTestUserSettings()
        val settingsJson = Json.encodeToString(expectedSettings)
        
        coEvery { mockSettingsRepository.getUserSettings() } returns Result.success(null)
        every { mockSharedPreferences.getString("user_settings", null) } returns settingsJson
        every { mockSharedPreferences.getBoolean("sync_status", false) } returns true
        
        // When
        val result = androidSettingsManager.getUserSettings()
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedSettings.userId, result.getOrNull()?.userId)
    }
    
    @Test
    fun `getUserSettings creates default settings when nothing found`() = runTest {
        // Given
        coEvery { mockSettingsRepository.getUserSettings() } returns Result.success(null)
        every { mockSharedPreferences.getString("user_settings", null) } returns null
        coEvery { mockSettingsRepository.saveUserSettings(any()) } returns Result.success(Unit)
        
        // When
        val result = androidSettingsManager.getUserSettings()
        
        // Then
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
        assertEquals("default_user", result.getOrNull()?.userId)
    }
    
    @Test
    fun `updateUnitPreferences saves to repository and SharedPreferences`() = runTest {
        // Given
        val currentSettings = createTestUserSettings()
        val newPreferences = UnitPreferences(
            temperatureUnit = TemperatureUnit.FAHRENHEIT,
            weightUnit = WeightUnit.KILOGRAMS,
            isManuallySet = true
        )
        
        coEvery { mockSettingsRepository.getUserSettings() } returns Result.success(currentSettings)
        coEvery { mockSettingsRepository.saveUserSettings(any()) } returns Result.success(Unit)
        
        // When
        val result = androidSettingsManager.updateUnitPreferences(newPreferences)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { mockSettingsRepository.saveUserSettings(any()) }
        verify { mockEditor.putString("user_settings", any()) }
        verify { mockEditor.apply() }
    }
    
    @Test
    fun `updateNotificationPreferences saves to repository and SharedPreferences`() = runTest {
        // Given
        val currentSettings = createTestUserSettings()
        val newPreferences = NotificationPreferences(
            dailyLoggingReminder = NotificationSetting.disabled(),
            periodPredictionAlert = NotificationSetting.defaultEnabled(),
            ovulationAlert = NotificationSetting.defaultEnabled(),
            insightNotifications = NotificationSetting.disabled(),
            globalNotificationsEnabled = true
        )
        
        coEvery { mockSettingsRepository.getUserSettings() } returns Result.success(currentSettings)
        coEvery { mockSettingsRepository.saveUserSettings(any()) } returns Result.success(Unit)
        
        // When
        val result = androidSettingsManager.updateNotificationPreferences(newPreferences)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { mockSettingsRepository.saveUserSettings(any()) }
        verify { mockEditor.putString("user_settings", any()) }
        verify { mockEditor.apply() }
    }
    
    @Test
    fun `updateCyclePreferences saves to repository and SharedPreferences`() = runTest {
        // Given
        val currentSettings = createTestUserSettings()
        val newPreferences = CyclePreferences(
            averageCycleLength = 30,
            averageLutealPhaseLength = 14,
            periodDuration = 5,
            isCustomized = true
        )
        
        coEvery { mockSettingsRepository.getUserSettings() } returns Result.success(currentSettings)
        coEvery { mockSettingsRepository.saveUserSettings(any()) } returns Result.success(Unit)
        
        // When
        val result = androidSettingsManager.updateCyclePreferences(newPreferences)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { mockSettingsRepository.saveUserSettings(any()) }
        verify { mockEditor.putString("user_settings", any()) }
        verify { mockEditor.apply() }
    }
    
    @Test
    fun `updatePrivacyPreferences saves to repository and SharedPreferences`() = runTest {
        // Given
        val currentSettings = createTestUserSettings()
        val newPreferences = PrivacyPreferences(
            dataSharingEnabled = false,
            anonymousInsightsEnabled = true,
            crashReportingEnabled = true,
            analyticsEnabled = false
        )
        
        coEvery { mockSettingsRepository.getUserSettings() } returns Result.success(currentSettings)
        coEvery { mockSettingsRepository.saveUserSettings(any()) } returns Result.success(Unit)
        
        // When
        val result = androidSettingsManager.updatePrivacyPreferences(newPreferences)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { mockSettingsRepository.saveUserSettings(any()) }
        verify { mockEditor.putString("user_settings", any()) }
        verify { mockEditor.apply() }
    }
    
    @Test
    fun `updateDisplayPreferences saves to repository and SharedPreferences`() = runTest {
        // Given
        val currentSettings = createTestUserSettings()
        val newPreferences = DisplayPreferences(
            textSizeScale = 1.2f,
            highContrastMode = false,
            hapticFeedbackEnabled = true,
            hapticIntensity = HapticIntensity.STRONG
        )
        
        coEvery { mockSettingsRepository.getUserSettings() } returns Result.success(currentSettings)
        coEvery { mockSettingsRepository.saveUserSettings(any()) } returns Result.success(Unit)
        
        // When
        val result = androidSettingsManager.updateDisplayPreferences(newPreferences)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { mockSettingsRepository.saveUserSettings(any()) }
        verify { mockEditor.putString("user_settings", any()) }
        verify { mockEditor.apply() }
    }
    
    @Test
    fun `updateSyncPreferences saves to repository and SharedPreferences`() = runTest {
        // Given
        val currentSettings = createTestUserSettings()
        val newPreferences = SyncPreferences(
            autoSyncEnabled = false,
            wifiOnlySync = true,
            cloudBackupEnabled = true,
            lastSyncTime = null
        )
        
        coEvery { mockSettingsRepository.getUserSettings() } returns Result.success(currentSettings)
        coEvery { mockSettingsRepository.saveUserSettings(any()) } returns Result.success(Unit)
        
        // When
        val result = androidSettingsManager.updateSyncPreferences(newPreferences)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { mockSettingsRepository.saveUserSettings(any()) }
        verify { mockEditor.putString("user_settings", any()) }
        verify { mockEditor.apply() }
    }
    
    @Test
    fun `updateSettings works when repository fails but SharedPreferences succeeds`() = runTest {
        // Given
        val currentSettings = createTestUserSettings()
        
        coEvery { mockSettingsRepository.getUserSettings() } returns Result.success(currentSettings)
        coEvery { mockSettingsRepository.saveUserSettings(any()) } returns Result.error(
            com.eunio.healthapp.domain.error.SettingsError.PersistenceError(
                message = "Repository failed",
                operation = "saveUserSettings"
            )
        )
        
        // When
        val result = androidSettingsManager.updateSettings { settings ->
            settings.copy(unitPreferences = settings.unitPreferences.copy(temperatureUnit = TemperatureUnit.FAHRENHEIT))
        }
        
        // Then
        assertTrue(result.isSuccess)
        verify { mockEditor.putString("user_settings", any()) }
        verify { mockEditor.apply() }
    }
    
    @Test
    fun `validateSettings returns success for valid settings`() = runTest {
        // Given
        val validSettings = createTestUserSettings()
        
        // When
        val result = androidSettingsManager.validateSettings(validSettings)
        
        // Then
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `validateSettings returns error for invalid settings`() = runTest {
        // Given
        val invalidSettings = createTestUserSettings().copy(userId = "")
        
        // When
        val result = androidSettingsManager.validateSettings(invalidSettings)
        
        // Then
        assertTrue(result.isError)
        assertTrue(result.errorOrNull()?.message?.contains("User ID cannot be blank") == true)
    }
    
    @Test
    fun `resetToDefaults creates default settings`() = runTest {
        // Given
        coEvery { mockSettingsRepository.saveUserSettings(any()) } returns Result.success(Unit)
        
        // When
        val result = androidSettingsManager.resetToDefaults(preserveUnitPreferences = false)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals("default_user", result.getOrNull()?.userId)
        coVerify { mockSettingsRepository.saveUserSettings(any()) }
        verify { mockEditor.putString("user_settings", any()) }
        verify { mockEditor.apply() }
    }
    
    @Test
    fun `resetToDefaults preserves unit preferences when requested`() = runTest {
        // Given
        val currentSettings = createTestUserSettings().copy(
            unitPreferences = UnitPreferences(
                temperatureUnit = TemperatureUnit.FAHRENHEIT,
                weightUnit = WeightUnit.KILOGRAMS,
                isManuallySet = true
            )
        )
        
        coEvery { mockSettingsRepository.getUserSettings() } returns Result.success(currentSettings)
        coEvery { mockSettingsRepository.saveUserSettings(any()) } returns Result.success(Unit)
        
        // When
        val result = androidSettingsManager.resetToDefaults(preserveUnitPreferences = true)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(TemperatureUnit.FAHRENHEIT, result.getOrNull()?.unitPreferences?.temperatureUnit)
    }
    
    @Test
    fun `exportSettings returns JSON string of current settings`() = runTest {
        // Given
        val currentSettings = createTestUserSettings()
        coEvery { mockSettingsRepository.getUserSettings() } returns Result.success(currentSettings)
        
        // When
        val result = androidSettingsManager.exportSettings()
        
        // Then
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
        assertTrue(result.getOrNull()!!.contains(currentSettings.userId))
    }
    
    @Test
    fun `importSettings parses JSON and saves settings`() = runTest {
        // Given
        val settingsToImport = createTestUserSettings()
        val backupData = Json.encodeToString(settingsToImport)
        
        coEvery { mockSettingsRepository.saveUserSettings(any()) } returns Result.success(Unit)
        
        // When
        val result = androidSettingsManager.importSettings(backupData)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { mockSettingsRepository.saveUserSettings(any()) }
        verify { mockEditor.putString("user_settings", any()) }
        verify { mockEditor.apply() }
    }
    
    @Test
    fun `observeSettingsChanges emits settings updates`() = runTest {
        // Given
        val initialSettings = createTestUserSettings()
        coEvery { mockSettingsRepository.getUserSettings() } returns Result.success(initialSettings)
        coEvery { mockSettingsRepository.saveUserSettings(any()) } returns Result.success(Unit)
        
        // When
        androidSettingsManager.getUserSettings() // Initialize the flow
        val settingsFlow = androidSettingsManager.observeSettingsChanges()
        val firstEmission = settingsFlow.first()
        
        // Then
        assertEquals(initialSettings, firstEmission)
    }
    
    @Test
    fun `isSynced returns correct sync status`() = runTest {
        // Given
        val syncedSettings = createTestUserSettings().copy(syncStatus = SyncStatus.SYNCED)
        coEvery { mockSettingsRepository.getUserSettings() } returns Result.success(syncedSettings)
        
        // When
        val result = androidSettingsManager.isSynced()
        
        // Then
        assertTrue(result)
    }
    
    @Test
    fun `syncSettings delegates to repository`() = runTest {
        // Given
        coEvery { mockSettingsRepository.syncSettings() } returns Result.success(Unit)
        coEvery { mockSettingsRepository.getUserSettings() } returns Result.success(createTestUserSettings())
        
        // When
        val result = androidSettingsManager.syncSettings()
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { mockSettingsRepository.syncSettings() }
    }
    
    private fun createTestUserSettings(): UserSettings {
        return UserSettings.createDefault("test_user_123")
    }
}