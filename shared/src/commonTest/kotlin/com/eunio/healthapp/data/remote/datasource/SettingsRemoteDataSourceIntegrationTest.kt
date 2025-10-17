package com.eunio.healthapp.data.remote.datasource

import com.eunio.healthapp.data.remote.FirestoreService
import com.eunio.healthapp.domain.error.SettingsError
import com.eunio.healthapp.domain.model.SyncStatus
import com.eunio.healthapp.domain.model.settings.*
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.*

/**
 * Integration tests for SettingsRemoteDataSourceImpl.
 * Tests Firebase/Firestore synchronization operations.
 */
class SettingsRemoteDataSourceIntegrationTest {
    
    private lateinit var mockFirestoreService: MockFirestoreService
    private lateinit var settingsRemoteDataSource: SettingsRemoteDataSourceImpl
    
    @BeforeTest
    fun setup() {
        mockFirestoreService = MockFirestoreService()
        settingsRemoteDataSource = SettingsRemoteDataSourceImpl(mockFirestoreService)
    }
    
    @Test
    fun `saveSettings should save settings to Firestore and create history entry`() = runTest {
        // Given
        val userId = "test-user-123"
        val settings = createTestUserSettings(userId)
        
        // When
        val result = settingsRemoteDataSource.saveSettings(settings)
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(mockFirestoreService.updateUserSettingsCalled)
        assertTrue(mockFirestoreService.saveSettingsHistoryCalled)
    }
    
    @Test
    fun `getSettings should retrieve settings from Firestore`() = runTest {
        // Given
        val userId = "test-user-123"
        val expectedSettings = createTestUserSettings(userId)
        mockFirestoreService.mockUserSettings = expectedSettings
        
        // When
        val result = settingsRemoteDataSource.getSettings(userId)
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(mockFirestoreService.getUserCalled)
        // Note: In a real test, we would verify the actual settings content
    }
    
    @Test
    fun `getSettings should return null when settings do not exist`() = runTest {
        // Given
        val userId = "nonexistent-user"
        mockFirestoreService.mockUserSettings = null
        
        // When
        val result = settingsRemoteDataSource.getSettings(userId)
        
        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }
    
    @Test
    fun `conditionalUpdateSettings should succeed when no conflict exists`() = runTest {
        // Given
        val userId = "test-user-123"
        val settings = createTestUserSettings(userId)
        val expectedTimestamp = settings.lastModified.toEpochMilliseconds()
        mockFirestoreService.mockUserSettings = settings
        
        // When
        val result = settingsRemoteDataSource.conditionalUpdateSettings(settings, expectedTimestamp)
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(mockFirestoreService.updateUserSettingsCalled)
    }
    
    @Test
    fun `conditionalUpdateSettings should fail when conflict exists`() = runTest {
        // Given
        val userId = "test-user-123"
        val settings = createTestUserSettings(userId)
        val outdatedTimestamp = settings.lastModified.toEpochMilliseconds() - 1000
        mockFirestoreService.mockUserSettings = settings
        
        // When
        val result = settingsRemoteDataSource.conditionalUpdateSettings(settings, outdatedTimestamp)
        
        // Then
        assertTrue(result.isError)
        assertTrue(result.errorOrNull() is SettingsError.ConflictError)
    }
    
    @Test
    fun `deleteSettings should remove settings and create history entry`() = runTest {
        // Given
        val userId = "test-user-123"
        val settings = createTestUserSettings(userId)
        mockFirestoreService.mockUserSettings = settings
        
        // When
        val result = settingsRemoteDataSource.deleteSettings(userId)
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(mockFirestoreService.deleteUserSettingsCalled)
        assertTrue(mockFirestoreService.saveSettingsHistoryCalled)
    }
    
    @Test
    fun `settingsExist should return true when settings exist`() = runTest {
        // Given
        val userId = "test-user-123"
        val settings = createTestUserSettings(userId)
        mockFirestoreService.mockUserSettings = settings
        
        // When
        val result = settingsRemoteDataSource.settingsExist(userId)
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
    }
    
    @Test
    fun `settingsExist should return false when settings do not exist`() = runTest {
        // Given
        val userId = "nonexistent-user"
        mockFirestoreService.mockUserSettings = null
        
        // When
        val result = settingsRemoteDataSource.settingsExist(userId)
        
        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() == true)
    }
    
    @Test
    fun `getLastModifiedTimestamp should return correct timestamp`() = runTest {
        // Given
        val userId = "test-user-123"
        val settings = createTestUserSettings(userId)
        val expectedTimestamp = settings.lastModified.toEpochMilliseconds()
        mockFirestoreService.mockUserSettings = settings
        
        // When
        val result = settingsRemoteDataSource.getLastModifiedTimestamp(userId)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedTimestamp, result.getOrNull())
    }
    
    @Test
    fun `createSettingsHistory should save history entry`() = runTest {
        // Given
        val userId = "test-user-123"
        val settings = createTestUserSettings(userId)
        val changeType = "UPDATE"
        val deviceInfo = "Test Device"
        
        // When
        val result = settingsRemoteDataSource.createSettingsHistory(userId, settings, changeType, deviceInfo)
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(mockFirestoreService.saveSettingsHistoryCalled)
        assertNotNull(result.getOrNull())
    }
    
    @Test
    fun `getSettingsHistory should retrieve history entries`() = runTest {
        // Given
        val userId = "test-user-123"
        val limit = 10
        mockFirestoreService.mockHistoryEntries = listOf(
            "history1" to mapOf("changeType" to "UPDATE", "timestamp" to Clock.System.now().toEpochMilliseconds()),
            "history2" to mapOf("changeType" to "DELETE", "timestamp" to Clock.System.now().toEpochMilliseconds())
        )
        
        // When
        val result = settingsRemoteDataSource.getSettingsHistory(userId, limit)
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(mockFirestoreService.getSettingsHistoryCalled)
    }
    
    @Test
    fun `batchUpdateSettings should update multiple settings`() = runTest {
        // Given
        val settingsList = listOf(
            createTestUserSettings("user1"),
            createTestUserSettings("user2"),
            createTestUserSettings("user3")
        )
        
        // When
        val result = settingsRemoteDataSource.batchUpdateSettings(settingsList)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrNull())
    }
    
    @Test
    fun `validateConnection should succeed when Firestore is accessible`() = runTest {
        // Given
        mockFirestoreService.isHealthy = true
        
        // When
        val result = settingsRemoteDataSource.validateConnection()
        
        // Then
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `validateConnection should fail when Firestore is not accessible`() = runTest {
        // Given
        mockFirestoreService.isHealthy = false
        
        // When
        val result = settingsRemoteDataSource.validateConnection()
        
        // Then
        assertTrue(result.isError)
    }
    
    @Test
    fun `healthCheck should return health status`() = runTest {
        // Given
        mockFirestoreService.isHealthy = true
        
        // When
        val result = settingsRemoteDataSource.healthCheck()
        
        // Then
        assertTrue(result.isSuccess)
        val health = result.getOrNull()
        assertNotNull(health)
        assertTrue(health.isHealthy)
    }
    
    @Test
    fun `getServerTimestamp should return current timestamp`() = runTest {
        // When
        val result = settingsRemoteDataSource.getServerTimestamp()
        
        // Then
        assertTrue(result.isSuccess)
        val timestamp = result.getOrNull()
        assertNotNull(timestamp)
        assertTrue(timestamp > 0)
    }
    
    @Test
    fun `error handling should wrap Firestore exceptions properly`() = runTest {
        // Given
        val userId = "test-user-123"
        mockFirestoreService.shouldThrowException = true
        
        // When
        val result = settingsRemoteDataSource.getSettings(userId)
        
        // Then
        assertTrue(result.isError)
        assertTrue(result.errorOrNull() is SettingsError.SyncError)
    }
    
    private fun createTestUserSettings(userId: String): UserSettings {
        return UserSettings(
            userId = userId,
            unitPreferences = UnitPreferences(
                temperatureUnit = TemperatureUnit.CELSIUS,
                weightUnit = WeightUnit.KILOGRAMS,
                isManuallySet = true
            ),
            notificationPreferences = NotificationPreferences(
                dailyLoggingReminder = NotificationSetting(
                    enabled = true,
                    time = kotlinx.datetime.LocalTime(20, 0),
                    daysInAdvance = 1
                ),
                globalNotificationsEnabled = true
            ),
            cyclePreferences = CyclePreferences(
                averageCycleLength = 28,
                averageLutealPhaseLength = 14,
                periodDuration = 5,
                isCustomized = true
            ),
            privacyPreferences = PrivacyPreferences(
                dataSharingEnabled = false,
                anonymousInsightsEnabled = true,
                crashReportingEnabled = true,
                analyticsEnabled = true
            ),
            displayPreferences = DisplayPreferences(
                textSizeScale = 1.2f,
                highContrastMode = false,
                hapticFeedbackEnabled = true,
                hapticIntensity = HapticIntensity.MEDIUM
            ),
            syncPreferences = SyncPreferences(
                autoSyncEnabled = true,
                wifiOnlySync = false,
                cloudBackupEnabled = true,
                lastSyncTime = Clock.System.now()
            ),
            lastModified = Clock.System.now(),
            syncStatus = SyncStatus.PENDING,
            version = 1
        )
    }
}