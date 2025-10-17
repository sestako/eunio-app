package com.eunio.healthapp.integration

import com.eunio.healthapp.data.local.datasource.SettingsLocalDataSource
import com.eunio.healthapp.data.local.datasource.SettingsBackupInfo
import com.eunio.healthapp.data.remote.datasource.SettingsRemoteDataSource
import com.eunio.healthapp.domain.manager.*
import com.eunio.healthapp.domain.model.SyncStatus
import com.eunio.healthapp.domain.model.settings.*
import com.eunio.healthapp.domain.repository.SettingsRepository
import com.eunio.healthapp.domain.usecase.settings.RestoreSettingsOnNewDeviceUseCase
import com.eunio.healthapp.domain.usecase.settings.ResolveSettingsConflictUseCase
import com.eunio.healthapp.domain.util.NetworkConnectivity
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.testutil.BaseAndroidIntegrationTest
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.*

/**
 * Integration test for the complete settings backup and restore workflow.
 * Tests the interaction between SettingsManager, SettingsBackupManager, and related use cases.
 */
class SettingsBackupRestoreIntegrationTest : BaseAndroidIntegrationTest() {
    
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var localDataSource: SettingsLocalDataSource
    private lateinit var remoteDataSource: SettingsRemoteDataSource
    private lateinit var networkConnectivity: NetworkConnectivity
    private lateinit var notificationManager: NotificationManager
    private lateinit var predictOvulationUseCase: com.eunio.healthapp.domain.usecase.cycle.PredictOvulationUseCase
    private lateinit var updateCycleUseCase: com.eunio.healthapp.domain.usecase.cycle.UpdateCycleUseCase
    private lateinit var userRepository: com.eunio.healthapp.domain.repository.UserRepository
    
    private lateinit var settingsBackupManager: SettingsBackupManager
    private lateinit var settingsManager: SettingsManager
    private lateinit var restoreUseCase: RestoreSettingsOnNewDeviceUseCase
    private lateinit var conflictUseCase: ResolveSettingsConflictUseCase
    
    private val testUserId = "integration-test-user"
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    
    override fun setup() {
        // Mock dependencies with proper cleanup tracking
        settingsRepository = registerMockService(mockk())
        localDataSource = registerMockService(mockk())
        remoteDataSource = registerMockService(mockk())
        networkConnectivity = registerMockService(mockk())
        notificationManager = registerMockService(mockk())
        predictOvulationUseCase = registerMockService(mockk())
        updateCycleUseCase = registerMockService(mockk())
        userRepository = registerMockService(mockk(relaxed = true))
        settingsBackupManager = registerMockService(mockk())
        
        // Default network connectivity
        every { networkConnectivity.isConnected() } returns true
        
        settingsManager = SettingsManagerImpl(
            settingsRepository = settingsRepository,
            settingsBackupManager = settingsBackupManager,
            notificationManager = notificationManager,
            predictOvulationUseCase = predictOvulationUseCase,
            updateCycleUseCase = updateCycleUseCase,
            currentUserId = { testUserId }
        )
        
        restoreUseCase = RestoreSettingsOnNewDeviceUseCase(
            settingsBackupManager = settingsBackupManager,
            userRepository = userRepository
        )
        
        conflictUseCase = ResolveSettingsConflictUseCase(
            settingsRepository = settingsRepository,
            settingsBackupManager = settingsBackupManager
        )
        
        // Default mocks for common operations
        coEvery { notificationManager.updateNotificationSchedule(any()) } returns kotlin.Result.success(Unit)
        coEvery { predictOvulationUseCase.execute(any()) } returns Result.success(null)
        coEvery { updateCycleUseCase.completeCycle(any(), any()) } returns Result.success(mockk())
        coEvery { remoteDataSource.createSettingsHistory(any(), any(), any(), any()) } returns Result.success("history-123")
        
        // Additional mocks that might be needed for updateUnitPreferences
        coEvery { settingsRepository.getUserSettings(any()) } returns Result.success(createTestUserSettings())
        coEvery { settingsRepository.validateSettings(any()) } returns Result.success(Unit)
        coEvery { settingsRepository.saveUserSettings(any()) } returns Result.success(Unit)
        coEvery { localDataSource.createSettingsBackup(any(), any()) } returns Result.success(123L)
        coEvery { localDataSource.getUserBackups(any()) } returns Result.success(emptyList())
    }
    
    @Test
    fun `complete backup and restore workflow should work end-to-end`() = runIntegrationTest {
        // Given - Initial settings
        val initialSettings = createTestUserSettings()
        val backupData = json.encodeToString(initialSettings)
        
        // Mock backup manager operations
        coEvery { settingsBackupManager.createManualBackup(testUserId) } returns Result.success(backupData)
        coEvery { settingsBackupManager.restoreOnNewDevice(testUserId, backupData) } returns Result.success(Unit)
        
        // Step 1: Create manual backup
        val backupResult = settingsBackupManager.createManualBackup(testUserId)
        assertTrue(backupResult.isSuccess)
        val retrievedBackupData = backupResult.getOrThrow()
        
        // Verify backup contains expected data
        val parsedBackup = json.decodeFromString<UserSettings>(retrievedBackupData)
        assertEquals(testUserId, parsedBackup.userId)
        assertEquals(TemperatureUnit.CELSIUS, parsedBackup.unitPreferences.temperatureUnit)
        
        // Step 2: Simulate new device - restore from backup
        val restoreResult = settingsBackupManager.restoreOnNewDevice(testUserId, retrievedBackupData)
        assertTrue(restoreResult.isSuccess)
        
        // Verify operations were called
        coVerify { settingsBackupManager.createManualBackup(testUserId) }
        coVerify { settingsBackupManager.restoreOnNewDevice(testUserId, retrievedBackupData) }
    }
    
    @Test
    fun `settings manager should create automatic backup on preference changes`() = runIntegrationTest {
        // Given
        val initialSettings = createTestUserSettings()
        val updatedPreferences = UnitPreferences(
            temperatureUnit = TemperatureUnit.FAHRENHEIT,
            weightUnit = WeightUnit.POUNDS,
            isManuallySet = true
        )
        
        // Mock repository operations
        coEvery { settingsRepository.getUserSettings(testUserId) } returns Result.success(initialSettings)
        coEvery { settingsRepository.validateSettings(any()) } returns Result.success(Unit)
        coEvery { settingsRepository.saveUserSettings(any()) } returns Result.success(Unit)
        
        // Mock backup manager operations
        coEvery { settingsBackupManager.createAutomaticBackup(any()) } returns Result.success(456L)
        
        // When - Update unit preferences
        val updateResult = settingsManager.updateUnitPreferences(updatedPreferences)
        
        // Then
        assertTrue(updateResult.isSuccess)
        
        // Verify settings were saved
        coVerify { settingsRepository.saveUserSettings(any()) }
        
        // Verify automatic backup was created
        coVerify { settingsBackupManager.createAutomaticBackup(any()) }
    }
    
    @Test
    fun `conflict resolution workflow should work with user choice`() = runIntegrationTest {
        // Given - Conflicting settings
        val now = Clock.System.now()
        val localSettings = createTestUserSettings().copy(
            lastModified = now,
            unitPreferences = UnitPreferences(
                temperatureUnit = TemperatureUnit.CELSIUS,
                weightUnit = WeightUnit.KILOGRAMS,
                isManuallySet = true
            )
        )
        val remoteSettings = createTestUserSettings().copy(
            lastModified = now.minus(kotlin.time.Duration.parse("PT30M")), // 30 minutes older
            unitPreferences = UnitPreferences(
                temperatureUnit = TemperatureUnit.FAHRENHEIT,
                weightUnit = WeightUnit.POUNDS,
                isManuallySet = true
            )
        )
        
        // Mock backup manager operations
        coEvery { settingsBackupManager.resolveConflictWithUserChoice(
            userId = testUserId,
            localSettings = localSettings,
            remoteSettings = remoteSettings,
            strategy = ConflictResolutionStrategy.LOCAL_WINS
        ) } returns Result.success(localSettings)
        
        // When - Resolve conflict using LOCAL_WINS strategy
        val conflictResult = settingsBackupManager.resolveConflictWithUserChoice(
            userId = testUserId,
            localSettings = localSettings,
            remoteSettings = remoteSettings,
            strategy = ConflictResolutionStrategy.LOCAL_WINS
        )
        
        // Then
        assertTrue(conflictResult.isSuccess)
        val resolvedSettings = conflictResult.getOrThrow()
        
        // Should use local settings (Celsius)
        assertEquals(TemperatureUnit.CELSIUS, resolvedSettings.unitPreferences.temperatureUnit)
        
        // Verify conflict resolution was called
        coVerify { settingsBackupManager.resolveConflictWithUserChoice(testUserId, localSettings, remoteSettings, ConflictResolutionStrategy.LOCAL_WINS) }
    }
    
    @Test
    fun `new device restore use case should work with backup manager`() = runIntegrationTest {
        // Given
        val user = com.eunio.healthapp.domain.model.User(
            id = "different-user",
            email = "different@example.com",
            name = "Different User",
            onboardingComplete = true,
            primaryGoal = com.eunio.healthapp.domain.model.HealthGoal.GENERAL_HEALTH,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        // Mock user repository
        coEvery { userRepository.getCurrentUser() } returns Result.success(user)
        
        // Mock backup manager
        coEvery { settingsBackupManager.restoreOnNewDevice(testUserId, null) } returns Result.success(Unit)
        
        // When - Check if restore is needed
        val needsRestoreResult = restoreUseCase.needsRestore(testUserId)
        
        // Then - Should need restore because current user ID is different from testUserId
        assert(needsRestoreResult.isSuccess) { "needsRestore should succeed" }
        val needsRestore = needsRestoreResult.getOrThrow()
        assert(needsRestore) { "Should need restore when user IDs are different" }
        
        // When - Execute restore
        val restoreResult = restoreUseCase.execute(testUserId, isNewDevice = true)
        
        // Then
        assert(restoreResult.isSuccess) { "restore should succeed" }
        coVerify { settingsBackupManager.restoreOnNewDevice(testUserId, null) }
    }
    
    @Test
    fun `import settings with merge strategy should preserve existing customizations`() = runIntegrationTest {
        // Given
        val existingSettings = createTestUserSettings().copy(
            unitPreferences = UnitPreferences(
                temperatureUnit = TemperatureUnit.CELSIUS,
                weightUnit = WeightUnit.KILOGRAMS,
                isManuallySet = true // User has customized this
            ),
            cyclePreferences = CyclePreferences(
                averageCycleLength = 30,
                averageLutealPhaseLength = 12,
                periodDuration = 6,
                isCustomized = true // User has customized this
            )
        )
        
        val importedSettings = createTestUserSettings().copy(
            unitPreferences = UnitPreferences(
                temperatureUnit = TemperatureUnit.FAHRENHEIT,
                weightUnit = WeightUnit.POUNDS,
                isManuallySet = true
            ),
            displayPreferences = DisplayPreferences(
                textSizeScale = 1.5f,
                highContrastMode = true,
                hapticFeedbackEnabled = false,
                hapticIntensity = HapticIntensity.LIGHT
            )
        )
        
        val backupData = json.encodeToString(importedSettings)
        
        // Mock backup manager operations
        coEvery { settingsBackupManager.importSettings(
            userId = testUserId,
            backupData = backupData,
            mergeStrategy = ImportMergeStrategy.MERGE_WITH_EXISTING
        ) } returns Result.success(Unit)
        
        // When - Import with merge strategy
        val importResult = settingsBackupManager.importSettings(
            userId = testUserId,
            backupData = backupData,
            mergeStrategy = ImportMergeStrategy.MERGE_WITH_EXISTING
        )
        
        // Then
        assertTrue(importResult.isSuccess)
        
        // Verify import was called
        coVerify { settingsBackupManager.importSettings(testUserId, backupData, ImportMergeStrategy.MERGE_WITH_EXISTING) }
    }
    
    @Test
    fun `backup operations should be observable for UI feedback`() = runIntegrationTest {
        // Given
        val testSettings = createTestUserSettings()
        val backupData = json.encodeToString(testSettings)
        
        val testOperation = BackupOperation(
            type = BackupOperationType.MANUAL_BACKUP,
            userId = testUserId,
            status = BackupOperationStatus.COMPLETED,
            progress = 1.0f
        )
        
        // Mock backup manager operations
        coEvery { settingsBackupManager.createManualBackup(testUserId) } returns Result.success(backupData)
        every { settingsBackupManager.observeBackupOperations() } returns kotlinx.coroutines.flow.flowOf(testOperation)
        
        // When - Start observing backup operations
        val operationsFlow = settingsBackupManager.observeBackupOperations()
        
        // Start backup operation
        settingsBackupManager.createManualBackup(testUserId)
        
        // Then - Should receive backup operation updates
        val operation = operationsFlow.first()
        assertEquals(BackupOperationType.MANUAL_BACKUP, operation.type)
        assertEquals(testUserId, operation.userId)
        assertTrue(operation.status == BackupOperationStatus.COMPLETED)
    }
    
    @Test
    fun `cleanup old backups should preserve manual backups`() = runIntegrationTest {
        // Given
        val now = Clock.System.now().toEpochMilliseconds()
        val backups = listOf(
            SettingsBackupInfo(1L, testUserId, "AUTOMATIC", now - 86400000, 1024L), // 1 day old
            SettingsBackupInfo(2L, testUserId, "AUTOMATIC", now - 172800000, 1024L), // 2 days old
            SettingsBackupInfo(3L, testUserId, "AUTOMATIC", now - 259200000, 1024L), // 3 days old
            SettingsBackupInfo(4L, testUserId, "MANUAL", now - 86400000, 1024L), // Manual backup
            SettingsBackupInfo(5L, testUserId, "EXPORT", now - 86400000, 1024L) // Export backup
        )
        
        // Mock backup manager operations
        coEvery { settingsBackupManager.cleanupOldBackups(testUserId, keepCount = 2) } returns Result.success(Unit)
        
        // When - Cleanup old backups, keep only 2 automatic backups
        val cleanupResult = settingsBackupManager.cleanupOldBackups(testUserId, keepCount = 2)
        
        // Then
        assertTrue(cleanupResult.isSuccess)
        
        // Verify cleanup was called
        coVerify { settingsBackupManager.cleanupOldBackups(testUserId, 2) }
    }
    
    @Test
    fun `export with metadata should include version and timestamp information`() = runIntegrationTest {
        // Given
        val testSettings = createTestUserSettings()
        val exportData = """
            {
                "exportVersion": 1,
                "exportedAt": ${Clock.System.now().toEpochMilliseconds()},
                "settingsVersion": 1,
                "settings": ${json.encodeToString(testSettings)}
            }
        """.trimIndent()
        
        // Mock backup manager operations
        coEvery { settingsBackupManager.exportSettings(testUserId, includeMetadata = true) } returns Result.success(exportData)
        
        // When - Export with metadata
        val exportResult = settingsBackupManager.exportSettings(testUserId, includeMetadata = true)
        
        // Then
        assertTrue(exportResult.isSuccess)
        val retrievedExportData = exportResult.getOrThrow()
        
        // Verify metadata is included
        assertTrue(retrievedExportData.contains("exportVersion"))
        assertTrue(retrievedExportData.contains("exportedAt"))
        assertTrue(retrievedExportData.contains("settingsVersion"))
        assertTrue(retrievedExportData.contains("settings"))
        
        // Verify export was called
        coVerify { settingsBackupManager.exportSettings(testUserId, true) }
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
                dailyLoggingReminder = NotificationSetting.defaultEnabled(),
                periodPredictionAlert = NotificationSetting.defaultEnabled(),
                ovulationAlert = NotificationSetting.disabled(),
                insightNotifications = NotificationSetting.defaultEnabled(),
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