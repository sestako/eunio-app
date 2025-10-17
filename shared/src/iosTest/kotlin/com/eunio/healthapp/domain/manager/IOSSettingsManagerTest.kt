package com.eunio.healthapp.domain.manager

import com.eunio.healthapp.domain.model.SyncStatus
import com.eunio.healthapp.domain.model.settings.*
import com.eunio.healthapp.domain.repository.SettingsRepository
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlin.test.*

class IOSSettingsManagerTest {
    
    private val mockSettingsRepository = MockSettingsRepository()
    private lateinit var iosSettingsManager: IOSSettingsManager
    
    @BeforeTest
    fun setup() {
        iosSettingsManager = IOSSettingsManager(mockSettingsRepository)
    }
    
    @Test
    fun `getUserSettings returns settings from repository when available`() = runTest {
        // Given
        val expectedSettings = createTestUserSettings()
        mockSettingsRepository.getUserSettingsResult = Result.success(expectedSettings)
        
        // When
        val result = iosSettingsManager.getUserSettings()
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedSettings, result.getOrNull())
        assertTrue(mockSettingsRepository.getUserSettingsCalled)
    }
    
    @Test
    fun `getUserSettings creates default settings when repository returns null`() = runTest {
        // Given
        mockSettingsRepository.getUserSettingsResult = Result.success(null)
        mockSettingsRepository.saveUserSettingsResult = Result.success(Unit)
        
        // When
        val result = iosSettingsManager.getUserSettings()
        
        // Then
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
        assertEquals("default_user", result.getOrNull()?.userId)
    }
    
    @Test
    fun `updateUnitPreferences saves to repository`() = runTest {
        // Given
        val currentSettings = createTestUserSettings()
        val newPreferences = UnitPreferences(
            temperatureUnit = TemperatureUnit.FAHRENHEIT,
            weightUnit = WeightUnit.KILOGRAMS,
            isManuallySet = true
        )
        
        mockSettingsRepository.getUserSettingsResult = Result.success(currentSettings)
        mockSettingsRepository.saveUserSettingsResult = Result.success(Unit)
        
        // When
        val result = iosSettingsManager.updateUnitPreferences(newPreferences)
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(mockSettingsRepository.saveUserSettingsCalled)
    }
    
    @Test
    fun `updateNotificationPreferences saves to repository`() = runTest {
        // Given
        val currentSettings = createTestUserSettings()
        val newPreferences = NotificationPreferences(
            dailyLoggingReminder = NotificationSetting.disabled(),
            periodPredictionAlert = NotificationSetting.defaultEnabled(),
            ovulationAlert = NotificationSetting.defaultEnabled(),
            insightNotifications = NotificationSetting.disabled(),
            globalNotificationsEnabled = true
        )
        
        mockSettingsRepository.getUserSettingsResult = Result.success(currentSettings)
        mockSettingsRepository.saveUserSettingsResult = Result.success(Unit)
        
        // When
        val result = iosSettingsManager.updateNotificationPreferences(newPreferences)
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(mockSettingsRepository.saveUserSettingsCalled)
    }
    
    @Test
    fun `updateCyclePreferences saves to repository`() = runTest {
        // Given
        val currentSettings = createTestUserSettings()
        val newPreferences = CyclePreferences(
            averageCycleLength = 30,
            averageLutealPhaseLength = 14,
            periodDuration = 5,
            isCustomized = true
        )
        
        mockSettingsRepository.getUserSettingsResult = Result.success(currentSettings)
        mockSettingsRepository.saveUserSettingsResult = Result.success(Unit)
        
        // When
        val result = iosSettingsManager.updateCyclePreferences(newPreferences)
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(mockSettingsRepository.saveUserSettingsCalled)
    }
    
    @Test
    fun `updatePrivacyPreferences saves to repository`() = runTest {
        // Given
        val currentSettings = createTestUserSettings()
        val newPreferences = PrivacyPreferences(
            dataSharingEnabled = false,
            anonymousInsightsEnabled = true,
            crashReportingEnabled = true,
            analyticsEnabled = false
        )
        
        mockSettingsRepository.getUserSettingsResult = Result.success(currentSettings)
        mockSettingsRepository.saveUserSettingsResult = Result.success(Unit)
        
        // When
        val result = iosSettingsManager.updatePrivacyPreferences(newPreferences)
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(mockSettingsRepository.saveUserSettingsCalled)
    }
    
    @Test
    fun `updateDisplayPreferences saves to repository`() = runTest {
        // Given
        val currentSettings = createTestUserSettings()
        val newPreferences = DisplayPreferences(
            textSizeScale = 1.2f,
            highContrastMode = false,
            hapticFeedbackEnabled = true,
            hapticIntensity = HapticIntensity.STRONG
        )
        
        mockSettingsRepository.getUserSettingsResult = Result.success(currentSettings)
        mockSettingsRepository.saveUserSettingsResult = Result.success(Unit)
        
        // When
        val result = iosSettingsManager.updateDisplayPreferences(newPreferences)
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(mockSettingsRepository.saveUserSettingsCalled)
    }
    
    @Test
    fun `updateSyncPreferences saves to repository`() = runTest {
        // Given
        val currentSettings = createTestUserSettings()
        val newPreferences = SyncPreferences(
            autoSyncEnabled = false,
            wifiOnlySync = true,
            cloudBackupEnabled = true,
            lastSyncTime = null
        )
        
        mockSettingsRepository.getUserSettingsResult = Result.success(currentSettings)
        mockSettingsRepository.saveUserSettingsResult = Result.success(Unit)
        
        // When
        val result = iosSettingsManager.updateSyncPreferences(newPreferences)
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(mockSettingsRepository.saveUserSettingsCalled)
    }
    
    @Test
    fun `updateSettings works when repository fails`() = runTest {
        // Given
        val currentSettings = createTestUserSettings()
        
        mockSettingsRepository.getUserSettingsResult = Result.success(currentSettings)
        mockSettingsRepository.saveUserSettingsResult = Result.error(
            com.eunio.healthapp.domain.error.SettingsError.PersistenceError(
                message = "Repository failed",
                operation = "saveUserSettings"
            )
        )
        
        // When
        val result = iosSettingsManager.updateSettings { settings ->
            settings.copy(unitPreferences = settings.unitPreferences.copy(temperatureUnit = TemperatureUnit.FAHRENHEIT))
        }
        
        // Then
        assertTrue(result.isSuccess) // Should succeed even if repository fails
    }
    
    @Test
    fun `validateSettings returns success for valid settings`() = runTest {
        // Given
        val validSettings = createTestUserSettings()
        
        // When
        val result = iosSettingsManager.validateSettings(validSettings)
        
        // Then
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `validateSettings returns error for invalid settings`() = runTest {
        // Given
        val invalidSettings = createTestUserSettings().copy(userId = "")
        
        // When
        val result = iosSettingsManager.validateSettings(invalidSettings)
        
        // Then
        assertTrue(result.isError)
        assertTrue(result.errorOrNull()?.message?.contains("User ID cannot be blank") == true)
    }
    
    @Test
    fun `resetToDefaults creates default settings`() = runTest {
        // Given
        mockSettingsRepository.saveUserSettingsResult = Result.success(Unit)
        
        // When
        val result = iosSettingsManager.resetToDefaults(preserveUnitPreferences = false)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals("default_user", result.getOrNull()?.userId)
        assertTrue(mockSettingsRepository.saveUserSettingsCalled)
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
        
        mockSettingsRepository.getUserSettingsResult = Result.success(currentSettings)
        mockSettingsRepository.saveUserSettingsResult = Result.success(Unit)
        
        // When
        val result = iosSettingsManager.resetToDefaults(preserveUnitPreferences = true)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(TemperatureUnit.FAHRENHEIT, result.getOrNull()?.unitPreferences?.temperatureUnit)
    }
    
    @Test
    fun `exportSettings returns JSON string of current settings`() = runTest {
        // Given
        val currentSettings = createTestUserSettings()
        mockSettingsRepository.getUserSettingsResult = Result.success(currentSettings)
        
        // When
        val result = iosSettingsManager.exportSettings()
        
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
        
        mockSettingsRepository.saveUserSettingsResult = Result.success(Unit)
        
        // When
        val result = iosSettingsManager.importSettings(backupData)
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(mockSettingsRepository.saveUserSettingsCalled)
    }
    
    @Test
    fun `observeSettingsChanges emits settings updates`() = runTest {
        // Given
        val initialSettings = createTestUserSettings()
        mockSettingsRepository.getUserSettingsResult = Result.success(initialSettings)
        mockSettingsRepository.saveUserSettingsResult = Result.success(Unit)
        
        // When
        iosSettingsManager.getUserSettings() // Initialize the flow
        val settingsFlow = iosSettingsManager.observeSettingsChanges()
        val firstEmission = settingsFlow.first()
        
        // Then
        assertEquals(initialSettings, firstEmission)
    }
    
    @Test
    fun `isSynced returns correct sync status`() = runTest {
        // Given
        val syncedSettings = createTestUserSettings().copy(syncStatus = SyncStatus.SYNCED)
        mockSettingsRepository.getUserSettingsResult = Result.success(syncedSettings)
        
        // When
        val result = iosSettingsManager.isSynced()
        
        // Then
        assertTrue(result)
    }
    
    @Test
    fun `syncSettings delegates to repository`() = runTest {
        // Given
        mockSettingsRepository.syncSettingsResult = Result.success(Unit)
        mockSettingsRepository.getUserSettingsResult = Result.success(createTestUserSettings())
        
        // When
        val result = iosSettingsManager.syncSettings()
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(mockSettingsRepository.syncSettingsCalled)
    }
    
    private fun createTestUserSettings(): UserSettings {
        return UserSettings.createDefault("test_user_123")
    }
}

// Mock implementation for testing
private class MockSettingsRepository : SettingsRepository {
    var getUserSettingsResult: Result<UserSettings?> = Result.success(null)
    var saveUserSettingsResult: Result<Unit> = Result.success(Unit)
    var syncSettingsResult: Result<Unit> = Result.success(Unit)
    
    var getUserSettingsCalled = false
    var saveUserSettingsCalled = false
    var syncSettingsCalled = false
    
    override suspend fun getUserSettings(): Result<UserSettings?> {
        getUserSettingsCalled = true
        return getUserSettingsResult
    }
    
    override suspend fun getUserSettings(userId: String): Result<UserSettings?> {
        getUserSettingsCalled = true
        return getUserSettingsResult
    }
    
    override suspend fun saveUserSettings(settings: UserSettings): Result<Unit> {
        saveUserSettingsCalled = true
        return saveUserSettingsResult
    }
    
    override suspend fun syncSettings(): Result<Unit> {
        syncSettingsCalled = true
        return syncSettingsResult
    }
    
    // Other methods not used in tests - return default implementations
    override suspend fun updateUserSettings(userId: String, updateFunction: (UserSettings) -> UserSettings): Result<UserSettings> = Result.success(UserSettings.createDefault(userId))
    override suspend fun syncSettings(userId: String): Result<Unit> = Result.success(Unit)
    override suspend fun resolveSettingsConflict(userId: String, localSettings: UserSettings, remoteSettings: UserSettings): Result<UserSettings> = Result.success(localSettings)
    override suspend fun backupUserSettings(userId: String): Result<String> = Result.success("{}")
    override suspend fun restoreUserSettings(userId: String, backupData: String): Result<Unit> = Result.success(Unit)
    override suspend fun exportUserData(userId: String): Result<String> = Result.success("{}")
    override suspend fun deleteUserSettings(userId: String): Result<Unit> = Result.success(Unit)
    override suspend fun clearLocalSettings(): Result<Unit> = Result.success(Unit)
    override suspend fun resetToDefaults(userId: String, locale: String?): Result<UserSettings> = Result.success(UserSettings.createDefault(userId))
    override fun observeUserSettings(userId: String): kotlinx.coroutines.flow.Flow<UserSettings?> = kotlinx.coroutines.flow.flowOf(null)
    override fun observeSyncStatus(): kotlinx.coroutines.flow.Flow<Boolean> = kotlinx.coroutines.flow.flowOf(false)
    override suspend fun getPendingSyncSettings(): Result<List<UserSettings>> = Result.success(emptyList())
    override suspend fun markAsSynced(userId: String): Result<Unit> = Result.success(Unit)
    override suspend fun markAsSyncFailed(userId: String): Result<Unit> = Result.success(Unit)
    override suspend fun settingsExist(userId: String): Result<Boolean> = Result.success(false)
    override suspend fun getLastModifiedTimestamp(userId: String): Result<Long?> = Result.success(null)
    override suspend fun validateSettings(settings: UserSettings): Result<Unit> = Result.success(Unit)
    override suspend fun migrateSettings(settings: UserSettings, targetVersion: Int): Result<UserSettings> = Result.success(settings)
}