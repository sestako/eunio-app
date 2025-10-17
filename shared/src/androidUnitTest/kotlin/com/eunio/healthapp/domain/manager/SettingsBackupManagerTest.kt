package com.eunio.healthapp.domain.manager

import com.eunio.healthapp.data.local.datasource.SettingsLocalDataSource
import com.eunio.healthapp.data.local.datasource.SettingsBackupInfo
import com.eunio.healthapp.data.remote.datasource.SettingsRemoteDataSource
import com.eunio.healthapp.domain.error.SettingsError
import com.eunio.healthapp.domain.model.SyncStatus
import com.eunio.healthapp.domain.model.settings.*
import com.eunio.healthapp.domain.repository.SettingsRepository
import com.eunio.healthapp.domain.util.NetworkConnectivity
import com.eunio.healthapp.domain.util.Result
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.*

class SettingsBackupManagerTest {
    
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var localDataSource: SettingsLocalDataSource
    private lateinit var remoteDataSource: SettingsRemoteDataSource
    private lateinit var networkConnectivity: NetworkConnectivity
    private lateinit var backupManager: SettingsBackupManagerImpl
    
    private val testUserId = "test-user-123"
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    
    @BeforeTest
    fun setup() {
        settingsRepository = mockk()
        localDataSource = mockk()
        remoteDataSource = mockk()
        networkConnectivity = mockk()
        
        backupManager = SettingsBackupManagerImpl(
            settingsRepository = settingsRepository,
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource,
            networkConnectivity = networkConnectivity
        )
        
        // Default network connectivity
        every { networkConnectivity.isConnected() } returns true
    }
    
    @Test
    fun `createAutomaticBackup should create backup and cleanup old ones`() = runTest {
        // Given
        val testSettings = createTestUserSettings()
        val backupId = 123L
        
        coEvery { localDataSource.createSettingsBackup(testUserId, "AUTOMATIC") } returns Result.success(backupId)
        coEvery { localDataSource.getUserBackups(testUserId) } returns Result.success(emptyList())
        
        // When
        val result = backupManager.createAutomaticBackup(testSettings)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(backupId, result.getOrThrow())
        
        coVerify { localDataSource.createSettingsBackup(testUserId, "AUTOMATIC") }
        coVerify { localDataSource.getUserBackups(testUserId) }
    }
    
    @Test
    fun `createAutomaticBackup should handle backup failure gracefully`() = runTest {
        // Given
        val testSettings = createTestUserSettings()
        val error = SettingsError.BackupError("Backup failed", "AUTOMATIC")
        
        coEvery { localDataSource.createSettingsBackup(testUserId, "AUTOMATIC") } returns Result.error(error)
        
        // When
        val result = backupManager.createAutomaticBackup(testSettings)
        
        // Then
        assertTrue(result.isError)
        assertTrue(result.errorOrNull() is SettingsError.BackupError)
    }
    
    @Test
    fun `createManualBackup should create backup and return JSON data`() = runTest {
        // Given
        val testSettings = createTestUserSettings()
        val backupId = 456L
        val expectedJson = json.encodeToString(testSettings)
        
        coEvery { settingsRepository.getUserSettings(testUserId) } returns Result.success(testSettings)
        coEvery { localDataSource.createSettingsBackup(testUserId, "MANUAL") } returns Result.success(backupId)
        
        // When
        val result = backupManager.createManualBackup(testUserId)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedJson, result.getOrThrow())
        
        coVerify { settingsRepository.getUserSettings(testUserId) }
        coVerify { localDataSource.createSettingsBackup(testUserId, "MANUAL") }
    }
    
    @Test
    fun `createManualBackup should fail when no settings found`() = runTest {
        // Given
        coEvery { settingsRepository.getUserSettings(testUserId) } returns Result.success(null)
        
        // When
        val result = backupManager.createManualBackup(testUserId)
        
        // Then
        assertTrue(result.isError)
        assertTrue(result.errorOrNull() is SettingsError.BackupError)
        assertEquals("No settings found to backup", result.errorOrNull()?.message)
    }
    
    @Test
    fun `restoreOnNewDevice should restore from remote when no backup data provided`() = runTest {
        // Given
        val testSettings = createTestUserSettings()
        
        coEvery { remoteDataSource.getSettings(testUserId) } returns Result.success(testSettings)
        coEvery { settingsRepository.validateSettings(any()) } returns Result.success(Unit)
        coEvery { settingsRepository.saveUserSettings(any()) } returns Result.success(Unit)
        coEvery { remoteDataSource.createSettingsHistory(any(), any(), any(), any()) } returns Result.success("history-123")
        
        // When
        val result = backupManager.restoreOnNewDevice(testUserId)
        
        // Then
        assertTrue(result.isSuccess)
        
        coVerify { remoteDataSource.getSettings(testUserId) }
        coVerify { settingsRepository.saveUserSettings(any()) }
        coVerify { remoteDataSource.createSettingsHistory(testUserId, any(), "RESTORE", any()) }
    }
    
    @Test
    fun `restoreOnNewDevice should create defaults when no remote settings found`() = runTest {
        // Given
        coEvery { remoteDataSource.getSettings(testUserId) } returns Result.success(null)
        coEvery { settingsRepository.saveUserSettings(any()) } returns Result.success(Unit)
        
        // When
        val result = backupManager.restoreOnNewDevice(testUserId)
        
        // Then
        assertTrue(result.isSuccess)
        
        coVerify { remoteDataSource.getSettings(testUserId) }
        coVerify { settingsRepository.saveUserSettings(any()) }
    }
    
    @Test
    fun `restoreOnNewDevice should restore from provided backup data`() = runTest {
        // Given
        val testSettings = createTestUserSettings()
        val backupData = json.encodeToString(testSettings)
        
        coEvery { settingsRepository.validateSettings(any()) } returns Result.success(Unit)
        coEvery { settingsRepository.saveUserSettings(any()) } returns Result.success(Unit)
        coEvery { remoteDataSource.createSettingsHistory(any(), any(), any(), any()) } returns Result.success("history-123")
        
        // When
        val result = backupManager.restoreOnNewDevice(testUserId, backupData)
        
        // Then
        assertTrue(result.isSuccess)
        
        coVerify { settingsRepository.validateSettings(any()) }
        coVerify { settingsRepository.saveUserSettings(any()) }
        coVerify(exactly = 0) { remoteDataSource.getSettings(any()) } // Should not fetch remote
    }
    
    @Test
    fun `restoreOnNewDevice should fail with invalid backup data`() = runTest {
        // Given
        val invalidBackupData = "invalid json"
        
        // When
        val result = backupManager.restoreOnNewDevice(testUserId, invalidBackupData)
        
        // Then
        assertTrue(result.isError)
        assertTrue(result.errorOrNull() is SettingsError.BackupError)
    }
    
    @Test
    fun `resolveConflictWithUserChoice should use LAST_WRITE_WINS strategy correctly`() = runTest {
        // Given
        val now = Clock.System.now()
        val localSettings = createTestUserSettings().copy(lastModified = now)
        val remoteSettings = createTestUserSettings().copy(
            lastModified = now.minus(kotlin.time.Duration.parse("PT1H")),
            unitPreferences = UnitPreferences(
                temperatureUnit = TemperatureUnit.FAHRENHEIT,
                weightUnit = WeightUnit.POUNDS,
                isManuallySet = true
            )
        )
        
        coEvery { settingsRepository.saveUserSettings(any()) } returns Result.success(Unit)
        coEvery { remoteDataSource.createSettingsHistory(any(), any(), any(), any()) } returns Result.success("history-123")
        
        // When
        val result = backupManager.resolveConflictWithUserChoice(
            userId = testUserId,
            localSettings = localSettings,
            remoteSettings = remoteSettings,
            strategy = ConflictResolutionStrategy.LAST_WRITE_WINS
        )
        
        // Then
        assertTrue(result.isSuccess)
        val resolvedSettings = result.getOrThrow()
        assertEquals(TemperatureUnit.CELSIUS, resolvedSettings.unitPreferences.temperatureUnit) // Local wins
        
        coVerify { settingsRepository.saveUserSettings(any()) }
        coVerify { remoteDataSource.createSettingsHistory(testUserId, any(), "CONFLICT_RESOLVED", any()) }
    }
    
    @Test
    fun `resolveConflictWithUserChoice should use LOCAL_WINS strategy correctly`() = runTest {
        // Given
        val localSettings = createTestUserSettings()
        val remoteSettings = createTestUserSettings().copy(
            unitPreferences = UnitPreferences(
                temperatureUnit = TemperatureUnit.FAHRENHEIT,
                weightUnit = WeightUnit.POUNDS,
                isManuallySet = true
            )
        )
        
        coEvery { settingsRepository.saveUserSettings(any()) } returns Result.success(Unit)
        coEvery { remoteDataSource.createSettingsHistory(any(), any(), any(), any()) } returns Result.success("history-123")
        
        // When
        val result = backupManager.resolveConflictWithUserChoice(
            userId = testUserId,
            localSettings = localSettings,
            remoteSettings = remoteSettings,
            strategy = ConflictResolutionStrategy.LOCAL_WINS
        )
        
        // Then
        assertTrue(result.isSuccess)
        val resolvedSettings = result.getOrThrow()
        assertEquals(TemperatureUnit.CELSIUS, resolvedSettings.unitPreferences.temperatureUnit) // Local wins
    }
    
    @Test
    fun `resolveConflictWithUserChoice should use REMOTE_WINS strategy correctly`() = runTest {
        // Given
        val localSettings = createTestUserSettings()
        val remoteSettings = createTestUserSettings().copy(
            unitPreferences = UnitPreferences(
                temperatureUnit = TemperatureUnit.FAHRENHEIT,
                weightUnit = WeightUnit.POUNDS,
                isManuallySet = true
            )
        )
        
        coEvery { settingsRepository.saveUserSettings(any()) } returns Result.success(Unit)
        coEvery { remoteDataSource.createSettingsHistory(any(), any(), any(), any()) } returns Result.success("history-123")
        
        // When
        val result = backupManager.resolveConflictWithUserChoice(
            userId = testUserId,
            localSettings = localSettings,
            remoteSettings = remoteSettings,
            strategy = ConflictResolutionStrategy.REMOTE_WINS
        )
        
        // Then
        assertTrue(result.isSuccess)
        val resolvedSettings = result.getOrThrow()
        assertEquals(TemperatureUnit.FAHRENHEIT, resolvedSettings.unitPreferences.temperatureUnit) // Remote wins
    }
    
    @Test
    fun `resolveConflictWithUserChoice should fail for MANUAL_RESOLUTION strategy`() = runTest {
        // Given
        val localSettings = createTestUserSettings()
        val remoteSettings = createTestUserSettings()
        
        // When
        val result = backupManager.resolveConflictWithUserChoice(
            userId = testUserId,
            localSettings = localSettings,
            remoteSettings = remoteSettings,
            strategy = ConflictResolutionStrategy.MANUAL_RESOLUTION
        )
        
        // Then
        assertTrue(result.isError)
        assertTrue(result.errorOrNull() is SettingsError.ConflictResolutionError)
        assertEquals("Manual resolution requires UI interaction", result.errorOrNull()?.message)
    }
    
    @Test
    fun `importSettings should replace all settings with REPLACE_ALL strategy`() = runTest {
        // Given
        val importedSettings = createTestUserSettings().copy(
            unitPreferences = UnitPreferences(
                temperatureUnit = TemperatureUnit.FAHRENHEIT,
                weightUnit = WeightUnit.POUNDS,
                isManuallySet = true
            )
        )
        val backupData = json.encodeToString(importedSettings)
        
        coEvery { settingsRepository.getUserSettings(testUserId) } returns Result.success(createTestUserSettings())
        coEvery { settingsRepository.validateSettings(any()) } returns Result.success(Unit)
        coEvery { settingsRepository.saveUserSettings(any()) } returns Result.success(Unit)
        coEvery { remoteDataSource.createSettingsHistory(any(), any(), any(), any()) } returns Result.success("history-123")
        
        // When
        val result = backupManager.importSettings(
            userId = testUserId,
            backupData = backupData,
            mergeStrategy = ImportMergeStrategy.REPLACE_ALL
        )
        
        // Then
        assertTrue(result.isSuccess)
        
        coVerify { settingsRepository.validateSettings(any()) }
        coVerify { settingsRepository.saveUserSettings(any()) }
        coVerify { remoteDataSource.createSettingsHistory(testUserId, any(), "IMPORT", any()) }
    }
    
    @Test
    fun `importSettings should merge with existing settings using MERGE_WITH_EXISTING strategy`() = runTest {
        // Given
        val existingSettings = createTestUserSettings()
        val importedSettings = createTestUserSettings().copy(
            unitPreferences = UnitPreferences(
                temperatureUnit = TemperatureUnit.FAHRENHEIT,
                weightUnit = WeightUnit.POUNDS,
                isManuallySet = true
            )
        )
        val backupData = json.encodeToString(importedSettings)
        
        coEvery { settingsRepository.validateSettings(any()) } returns Result.success(Unit)
        coEvery { settingsRepository.getUserSettings(testUserId) } returns Result.success(existingSettings)
        coEvery { settingsRepository.saveUserSettings(any()) } returns Result.success(Unit)
        coEvery { remoteDataSource.createSettingsHistory(any(), any(), any(), any()) } returns Result.success("history-123")
        
        // When
        val result = backupManager.importSettings(
            userId = testUserId,
            backupData = backupData,
            mergeStrategy = ImportMergeStrategy.MERGE_WITH_EXISTING
        )
        
        // Then
        assertTrue(result.isSuccess)
        
        coVerify { settingsRepository.getUserSettings(testUserId) }
        coVerify { settingsRepository.saveUserSettings(any()) }
    }
    
    @Test
    fun `importSettings should fail with invalid backup data`() = runTest {
        // Given
        val invalidBackupData = "invalid json"
        
        // When
        val result = backupManager.importSettings(
            userId = testUserId,
            backupData = invalidBackupData,
            mergeStrategy = ImportMergeStrategy.REPLACE_ALL
        )
        
        // Then
        assertTrue(result.isError)
        assertTrue(result.errorOrNull() is SettingsError.BackupError)
    }
    
    @Test
    fun `exportSettings should create export with metadata`() = runTest {
        // Given
        val testSettings = createTestUserSettings()
        
        coEvery { settingsRepository.getUserSettings(testUserId) } returns Result.success(testSettings)
        coEvery { localDataSource.createSettingsBackup(testUserId, "EXPORT") } returns Result.success(789L)
        coEvery { remoteDataSource.createSettingsHistory(any(), any(), any(), any()) } returns Result.success("history-123")
        
        // When
        val result = backupManager.exportSettings(testUserId, includeMetadata = true)
        
        // Then
        assertTrue(result.isSuccess)
        val exportData = result.getOrThrow()
        assertTrue(exportData.contains("exportVersion"))
        assertTrue(exportData.contains("exportedAt"))
        assertTrue(exportData.contains("settings"))
        
        coVerify { settingsRepository.getUserSettings(testUserId) }
        coVerify { localDataSource.createSettingsBackup(testUserId, "EXPORT") }
        coVerify { remoteDataSource.createSettingsHistory(testUserId, any(), "EXPORT", any()) }
    }
    
    @Test
    fun `getBackupHistory should return backup metadata`() = runTest {
        // Given
        val backupInfo = listOf(
            SettingsBackupInfo(
                backupId = 1L,
                userId = testUserId,
                backupType = "AUTOMATIC",
                createdAt = Clock.System.now().toEpochMilliseconds(),
                dataSize = 1024L
            ),
            SettingsBackupInfo(
                backupId = 2L,
                userId = testUserId,
                backupType = "MANUAL",
                createdAt = Clock.System.now().toEpochMilliseconds(),
                dataSize = 2048L
            )
        )
        
        coEvery { localDataSource.getUserBackups(testUserId) } returns Result.success(backupInfo)
        
        // When
        val result = backupManager.getBackupHistory(testUserId)
        
        // Then
        assertTrue(result.isSuccess)
        val metadata = result.getOrThrow()
        assertEquals(2, metadata.size)
        assertEquals("AUTOMATIC", metadata[0].backupType)
        assertEquals("MANUAL", metadata[1].backupType)
        
        coVerify { localDataSource.getUserBackups(testUserId) }
    }
    
    @Test
    fun `cleanupOldBackups should delete old automatic backups`() = runTest {
        // Given
        val now = Clock.System.now().toEpochMilliseconds()
        val oldBackups = listOf(
            SettingsBackupInfo(1L, testUserId, "AUTOMATIC", now - 86400000, 1024L), // 1 day old
            SettingsBackupInfo(2L, testUserId, "AUTOMATIC", now - 172800000, 1024L), // 2 days old
            SettingsBackupInfo(3L, testUserId, "AUTOMATIC", now - 259200000, 1024L), // 3 days old
            SettingsBackupInfo(4L, testUserId, "MANUAL", now - 86400000, 1024L) // Manual backup
        )
        
        coEvery { localDataSource.getUserBackups(testUserId) } returns Result.success(oldBackups)
        coEvery { localDataSource.deleteSettingsBackup(any()) } returns Result.success(Unit)
        
        // When
        val result = backupManager.cleanupOldBackups(testUserId, keepCount = 2)
        
        // Then
        assertTrue(result.isSuccess)
        
        // Should delete the oldest automatic backup (ID 3), but keep manual backup
        coVerify { localDataSource.deleteSettingsBackup(3L) }
        coVerify(exactly = 0) { localDataSource.deleteSettingsBackup(4L) } // Manual backup should not be deleted
        
        coVerify { localDataSource.getUserBackups(testUserId) }
    }
    
    @Test
    fun `observeBackupOperations should emit backup operation updates`() = runTest {
        // Given
        val testSettings = createTestUserSettings()
        coEvery { localDataSource.createSettingsBackup(testUserId, "AUTOMATIC") } returns Result.success(123L)
        coEvery { localDataSource.getUserBackups(testUserId) } returns Result.success(emptyList())
        
        // When
        val operationsFlow = backupManager.observeBackupOperations()
        
        // Start backup operation
        backupManager.createAutomaticBackup(testSettings)
        
        // Then
        val operation = operationsFlow.first()
        assertEquals(BackupOperationType.AUTOMATIC_BACKUP, operation.type)
        assertEquals(testUserId, operation.userId)
        assertTrue(operation.status == BackupOperationStatus.COMPLETED)
    }
    
    private fun createTestUserSettings(): UserSettings {
        return UserSettings(
            userId = testUserId,
            unitPreferences = UnitPreferences(
                temperatureUnit = TemperatureUnit.CELSIUS,
                weightUnit = WeightUnit.KILOGRAMS,
                isManuallySet = false
            ),
            notificationPreferences = NotificationPreferences(
                dailyLoggingReminder = NotificationSetting(enabled = true),
                periodPredictionAlert = NotificationSetting(enabled = true),
                ovulationAlert = NotificationSetting(enabled = false),
                insightNotifications = NotificationSetting(enabled = true),
                globalNotificationsEnabled = true
            ),
            cyclePreferences = CyclePreferences(
                averageCycleLength = 28,
                averageLutealPhaseLength = 14,
                periodDuration = 5,
                isCustomized = false
            ),
            privacyPreferences = PrivacyPreferences(
                dataSharingEnabled = false,
                anonymousInsightsEnabled = true,
                crashReportingEnabled = true,
                analyticsEnabled = true
            ),
            displayPreferences = DisplayPreferences(
                textSizeScale = 1.0f,
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
            syncStatus = SyncStatus.SYNCED,
            version = 1
        )
    }
}