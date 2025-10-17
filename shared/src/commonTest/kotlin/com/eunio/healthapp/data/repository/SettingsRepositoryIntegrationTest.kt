package com.eunio.healthapp.data.repository

import com.eunio.healthapp.data.local.datasource.SettingsLocalDataSource
import com.eunio.healthapp.data.remote.datasource.SettingsRemoteDataSource
import com.eunio.healthapp.domain.error.SettingsError
import com.eunio.healthapp.domain.error.SettingsErrorHandler
import com.eunio.healthapp.domain.model.SyncStatus
import com.eunio.healthapp.domain.model.User
import com.eunio.healthapp.domain.model.settings.UserSettings
import com.eunio.healthapp.domain.repository.UserRepository
import com.eunio.healthapp.domain.util.NetworkConnectivity
import com.eunio.healthapp.domain.util.NetworkType
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for SettingsRepositoryImpl focusing on sync scenarios,
 * offline behavior, and conflict resolution.
 */
class SettingsRepositoryIntegrationTest {
    
    private lateinit var mockLocalDataSource: TestSettingsLocalDataSource
    private lateinit var mockRemoteDataSource: TestSettingsRemoteDataSource
    private lateinit var mockNetworkConnectivity: TestNetworkConnectivity
    private lateinit var mockUserRepository: TestUserRepository
    private lateinit var errorHandler: SettingsErrorHandler
    private lateinit var repository: SettingsRepositoryImpl
    
    private val testUserId = "integration-test-user"
    private val testUser = User(
        id = testUserId,
        email = "integration@test.com",
        name = "Integration Test User",
        onboardingComplete = true,
        primaryGoal = com.eunio.healthapp.domain.model.HealthGoal.CYCLE_TRACKING,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now()
    )
    
    @BeforeTest
    fun setup() {
        mockLocalDataSource = TestSettingsLocalDataSource()
        mockRemoteDataSource = TestSettingsRemoteDataSource()
        mockNetworkConnectivity = TestNetworkConnectivity()
        mockUserRepository = TestUserRepository()
        errorHandler = SettingsErrorHandler()
        
        repository = SettingsRepositoryImpl(
            localDataSource = mockLocalDataSource,
            remoteDataSource = mockRemoteDataSource,
            networkConnectivity = mockNetworkConnectivity,
            userRepository = mockUserRepository,
            errorHandler = errorHandler
        )
        
        mockUserRepository.setCurrentUser(testUser)
        mockNetworkConnectivity.setConnected(true)
    }
    
    @Test
    fun `offline settings changes sync when connectivity restored`() = runTest {
        // Given: User is offline
        mockNetworkConnectivity.setConnected(false)
        val offlineSettings = createTestSettings().copy(
            unitPreferences = createTestSettings().unitPreferences.copy(isManuallySet = true)
        )
        
        // When: User changes settings offline
        val saveResult = repository.saveUserSettings(offlineSettings)
        assertTrue(saveResult.isSuccess)
        
        // Verify settings are saved locally but not remotely
        assertNotNull(mockLocalDataSource.getSettings(testUserId))
        assertNull(mockRemoteDataSource.getSettings(testUserId).getOrNull())
        
        // When: Connectivity is restored and sync is triggered
        mockNetworkConnectivity.setConnected(true)
        val syncResult = repository.syncSettings(testUserId)
        
        // Then: Settings should be synced to remote
        assertTrue(syncResult.isSuccess)
        assertNotNull(mockRemoteDataSource.getSettings(testUserId).getOrNull())
        assertEquals(
            offlineSettings.unitPreferences.isManuallySet,
            mockRemoteDataSource.getSettings(testUserId).getOrNull()?.unitPreferences?.isManuallySet
        )
    }
    
    @Test
    fun `concurrent modifications create conflict that gets resolved`() = runTest {
        // Given: Settings exist on both local and remote
        val baseTime = Clock.System.now()
        val localSettings = createTestSettings().copy(
            lastModified = baseTime.plus(kotlin.time.Duration.parse("5m")),
            unitPreferences = createTestSettings().unitPreferences.copy(isManuallySet = true)
        )
        val remoteSettings = createTestSettings().copy(
            lastModified = baseTime.plus(kotlin.time.Duration.parse("10m")),
            cyclePreferences = createTestSettings().cyclePreferences.copy(isCustomized = true)
        )
        
        mockLocalDataSource.saveSettings(localSettings)
        mockRemoteDataSource.saveSettings(remoteSettings)
        
        // When: Sync is triggered
        val syncResult = repository.syncSettings(testUserId)
        
        // Then: Remote settings should win (newer timestamp)
        assertTrue(syncResult.isSuccess)
        val finalLocalSettings = mockLocalDataSource.getSettings(testUserId)
        assertNotNull(finalLocalSettings)
        assertTrue(finalLocalSettings.cyclePreferences.isCustomized) // From remote
        assertEquals(SyncStatus.SYNCED, finalLocalSettings.syncStatus)
    }
    
    @Test
    fun `backup and restore preserves all settings data`() = runTest {
        // Given: User has customized settings
        val originalSettings = createTestSettings().copy(
            unitPreferences = createTestSettings().unitPreferences.copy(isManuallySet = true),
            cyclePreferences = createTestSettings().cyclePreferences.copy(
                averageCycleLength = 30,
                isCustomized = true
            ),
            displayPreferences = createTestSettings().displayPreferences.copy(
                textSizeScale = 1.5f,
                highContrastMode = true
            )
        )
        
        repository.saveUserSettings(originalSettings)
        
        // When: User creates backup
        val backupResult = repository.backupUserSettings(testUserId)
        assertTrue(backupResult.isSuccess)
        val backupData = backupResult.getOrNull()
        assertNotNull(backupData)
        
        // Clear settings to simulate fresh install
        repository.deleteUserSettings(testUserId)
        assertNull(mockLocalDataSource.getSettings(testUserId))
        
        // When: User restores from backup
        val restoreResult = repository.restoreUserSettings(testUserId, backupData)
        assertTrue(restoreResult.isSuccess)
        
        // Then: All settings should be restored
        val restoredSettings = mockLocalDataSource.getSettings(testUserId)
        assertNotNull(restoredSettings)
        assertEquals(originalSettings.unitPreferences.isManuallySet, restoredSettings.unitPreferences.isManuallySet)
        assertEquals(originalSettings.cyclePreferences.averageCycleLength, restoredSettings.cyclePreferences.averageCycleLength)
        assertEquals(originalSettings.displayPreferences.textSizeScale, restoredSettings.displayPreferences.textSizeScale)
        assertEquals(originalSettings.displayPreferences.highContrastMode, restoredSettings.displayPreferences.highContrastMode)
    }
    
    @Test
    fun `data export includes comprehensive user information`() = runTest {
        // Given: User has settings with various customizations
        val settings = createTestSettings().copy(
            unitPreferences = createTestSettings().unitPreferences.copy(isManuallySet = true),
            notificationPreferences = createTestSettings().notificationPreferences.copy(
                globalNotificationsEnabled = false
            ),
            privacyPreferences = createTestSettings().privacyPreferences.copy(
                dataSharingEnabled = false,
                analyticsEnabled = false
            )
        )
        
        repository.saveUserSettings(settings)
        
        // When: User exports data
        val exportResult = repository.exportUserData(testUserId)
        
        // Then: Export should contain all relevant information
        assertTrue(exportResult.isSuccess)
        val exportData = exportResult.getOrNull()
        assertNotNull(exportData)
        
        // Verify export structure
        assertTrue(exportData.contains("exportInfo"))
        assertTrue(exportData.contains("userSettings"))
        assertTrue(exportData.contains("metadata"))
        assertTrue(exportData.contains("exportDate"))
        assertTrue(exportData.contains("appVersion"))
        assertTrue(exportData.contains("hasCustomizations"))
    }
    
    @Test
    fun `settings validation prevents invalid data persistence`() = runTest {
        // Given: Various invalid settings scenarios
        val invalidCycleSettings = createTestSettings().copy(
            cyclePreferences = createTestSettings().cyclePreferences.copy(
                averageCycleLength = 50, // Too long
                averageLutealPhaseLength = 5 // Too short
            )
        )
        
        val invalidDisplaySettings = createTestSettings().copy(
            displayPreferences = createTestSettings().displayPreferences.copy(
                textSizeScale = 10.0f // Too large
            )
        )
        
        // When: Attempting to save invalid settings
        val cycleResult = repository.saveUserSettings(invalidCycleSettings)
        val displayResult = repository.saveUserSettings(invalidDisplaySettings)
        
        // Then: Both should fail validation
        assertTrue(cycleResult.isError)
        assertTrue(cycleResult.errorOrNull() is SettingsError.ValidationError)
        
        assertTrue(displayResult.isError)
        assertTrue(displayResult.errorOrNull() is SettingsError.ValidationError)
        
        // Verify no invalid data was persisted
        assertNull(mockLocalDataSource.getSettings(testUserId))
    }
    
    @Test
    fun `network failure during sync maintains local data integrity`() = runTest {
        // Given: User has local settings saved while offline
        mockNetworkConnectivity.setConnected(false)
        val localSettings = createTestSettings()
        repository.saveUserSettings(localSettings)
        
        // Simulate network failure during sync
        mockRemoteDataSource.setShouldFailOperations(true)
        mockNetworkConnectivity.setConnected(true)
        
        // When: Sync is attempted
        val syncResult = repository.syncSettings(testUserId)
        
        // Then: Sync should fail but local data should remain intact
        assertTrue(syncResult.isError)
        
        val localData = mockLocalDataSource.getSettings(testUserId)
        assertNotNull(localData)
        assertEquals(localSettings.userId, localData.userId)
        // The sync status might be FAILED after a failed sync attempt
        assertTrue(localData.syncStatus == SyncStatus.PENDING || localData.syncStatus == SyncStatus.FAILED)
    }
    
    @Test
    fun `multiple rapid updates maintain consistency`() = runTest {
        // Given: Initial settings
        val baseSettings = createTestSettings()
        repository.saveUserSettings(baseSettings)
        
        // When: Multiple rapid updates
        val updates = listOf(
            { settings: UserSettings -> settings.copy(unitPreferences = settings.unitPreferences.copy(isManuallySet = true)) },
            { settings: UserSettings -> settings.copy(cyclePreferences = settings.cyclePreferences.copy(averageCycleLength = 30)) },
            { settings: UserSettings -> settings.copy(displayPreferences = settings.displayPreferences.copy(textSizeScale = 1.2f)) }
        )
        
        for (update in updates) {
            val result = repository.updateUserSettings(testUserId, update)
            assertTrue(result.isSuccess)
        }
        
        // Then: Final state should reflect all updates
        val finalSettings = repository.getUserSettings(testUserId).getOrNull()
        assertNotNull(finalSettings)
        assertTrue(finalSettings.unitPreferences.isManuallySet)
        assertEquals(30, finalSettings.cyclePreferences.averageCycleLength)
        assertEquals(1.2f, finalSettings.displayPreferences.textSizeScale)
    }
    
    @Test
    fun `settings migration handles version upgrades correctly`() = runTest {
        // Given: Settings with older version
        val oldVersionSettings = createTestSettings().copy(version = 0)
        
        // When: Migration is performed
        val migrationResult = repository.migrateSettings(oldVersionSettings, 1)
        
        // Then: Settings should be upgraded
        assertTrue(migrationResult.isSuccess)
        val migratedSettings = migrationResult.getOrNull()
        assertNotNull(migratedSettings)
        assertEquals(1, migratedSettings.version)
        
        // All other data should be preserved
        assertEquals(oldVersionSettings.userId, migratedSettings.userId)
        assertEquals(oldVersionSettings.unitPreferences, migratedSettings.unitPreferences)
        assertEquals(oldVersionSettings.cyclePreferences, migratedSettings.cyclePreferences)
    }
    
    private fun createTestSettings(): UserSettings {
        return UserSettings.createDefault(testUserId)
    }
}

// Test helper classes for integration testing

class TestSettingsLocalDataSource : SettingsLocalDataSource {
    private val settings = mutableMapOf<String, UserSettings>()
    private val backups = mutableMapOf<Long, String>()
    private var nextBackupId = 1L
    private var pendingSettings = listOf<UserSettings>()
    
    override suspend fun getSettings(): UserSettings? {
        return settings.values.firstOrNull()
    }
    
    override suspend fun getSettings(userId: String): UserSettings? {
        return settings[userId]
    }
    
    override suspend fun saveSettings(settings: UserSettings): Result<Unit> {
        this.settings[settings.userId] = settings
        return Result.success(Unit)
    }
    
    override suspend fun updateSettings(settings: UserSettings): Result<Unit> {
        return saveSettings(settings)
    }
    
    override suspend fun deleteSettings(userId: String): Result<Unit> {
        settings.remove(userId)
        return Result.success(Unit)
    }
    
    override suspend fun getPendingSyncSettings(): List<UserSettings> {
        return settings.values.filter { it.syncStatus == SyncStatus.PENDING }
    }
    
    override suspend fun markAsSynced(userId: String): Result<Unit> {
        settings[userId]?.let { currentSettings ->
            settings[userId] = currentSettings.markAsSynced()
        }
        return Result.success(Unit)
    }
    
    override suspend fun markAsSyncFailed(userId: String): Result<Unit> {
        settings[userId]?.let { currentSettings ->
            settings[userId] = currentSettings.markAsSyncError()
        }
        return Result.success(Unit)
    }
    
    override suspend fun clearAllSettings(): Result<Unit> {
        settings.clear()
        return Result.success(Unit)
    }
    
    override suspend fun clearSettings(userId: String): Result<Unit> {
        settings.remove(userId)
        return Result.success(Unit)
    }
    
    override suspend fun settingsExist(userId: String): Boolean {
        return settings.containsKey(userId)
    }
    
    override suspend fun getLastModifiedTimestamp(userId: String): Long? {
        return settings[userId]?.lastModified?.toEpochMilliseconds()
    }
    
    override fun observeSettings(userId: String): Flow<UserSettings?> {
        return flowOf(settings[userId])
    }
    
    override suspend fun createSettingsBackup(userId: String, backupType: String): Result<Long> {
        val backupId = nextBackupId++
        settings[userId]?.let { userSettings ->
            backups[backupId] = kotlinx.serialization.json.Json.encodeToString(UserSettings.serializer(), userSettings)
        }
        return Result.success(backupId)
    }
    
    override suspend fun getSettingsBackup(backupId: Long): Result<String?> {
        return Result.success(backups[backupId])
    }
    
    override suspend fun getUserBackups(userId: String): Result<List<com.eunio.healthapp.data.local.datasource.SettingsBackupInfo>> {
        return Result.success(emptyList())
    }
    
    override suspend fun deleteSettingsBackup(backupId: Long): Result<Unit> {
        backups.remove(backupId)
        return Result.success(Unit)
    }
    
    override suspend fun deleteUserBackups(userId: String): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun getSettingsCount(): Long {
        return settings.size.toLong()
    }
    
    override suspend fun getSettingsDataSize(): Long {
        return settings.size.toLong()
    }
    
    override suspend fun performMaintenance(): Result<Unit> {
        return Result.success(Unit)
    }
}

class TestSettingsRemoteDataSource : SettingsRemoteDataSource {
    private val settings = mutableMapOf<String, UserSettings>()
    private val history = mutableMapOf<String, MutableList<com.eunio.healthapp.data.remote.datasource.SettingsHistoryEntry>>()
    private var shouldFailOperations = false
    
    fun setShouldFailOperations(shouldFail: Boolean) {
        shouldFailOperations = shouldFail
    }
    
    override suspend fun getSettings(userId: String): Result<UserSettings?> {
        if (shouldFailOperations) {
            return Result.error(SettingsError.SyncError("Simulated network failure"))
        }
        return Result.success(settings[userId])
    }
    
    override suspend fun saveSettings(settings: UserSettings): Result<Unit> {
        if (shouldFailOperations) {
            return Result.error(SettingsError.SyncError("Simulated network failure"))
        }
        this.settings[settings.userId] = settings
        return Result.success(Unit)
    }
    
    override suspend fun updateSettings(settings: UserSettings): Result<Unit> {
        return saveSettings(settings)
    }
    
    override suspend fun conditionalUpdateSettings(
        settings: UserSettings,
        expectedLastModified: Long
    ): Result<Unit> {
        if (shouldFailOperations) {
            return Result.error(SettingsError.SyncError("Simulated network failure"))
        }
        
        val current = this.settings[settings.userId]
        if (current != null && current.lastModified.toEpochMilliseconds() != expectedLastModified) {
            return Result.error(SettingsError.ConflictError("Conflict detected"))
        }
        return saveSettings(settings)
    }
    
    override suspend fun deleteSettings(userId: String): Result<Unit> {
        if (shouldFailOperations) {
            return Result.error(SettingsError.SyncError("Simulated network failure"))
        }
        settings.remove(userId)
        return Result.success(Unit)
    }
    
    override suspend fun clearAllSettings(): Result<Unit> {
        if (shouldFailOperations) {
            return Result.error(SettingsError.SyncError("Simulated network failure"))
        }
        settings.clear()
        return Result.success(Unit)
    }
    
    override suspend fun settingsExist(userId: String): Result<Boolean> {
        if (shouldFailOperations) {
            return Result.error(SettingsError.SyncError("Simulated network failure"))
        }
        return Result.success(settings.containsKey(userId))
    }
    
    override suspend fun getLastModifiedTimestamp(userId: String): Result<Long?> {
        if (shouldFailOperations) {
            return Result.error(SettingsError.SyncError("Simulated network failure"))
        }
        return Result.success(settings[userId]?.lastModified?.toEpochMilliseconds())
    }
    
    override fun observeSettings(userId: String): Flow<UserSettings?> {
        return flowOf(settings[userId])
    }
    
    override suspend fun createSettingsHistory(
        userId: String,
        settings: UserSettings,
        changeType: String,
        deviceInfo: String?
    ): Result<String> {
        if (shouldFailOperations) {
            return Result.error(SettingsError.SyncError("Simulated network failure"))
        }
        
        val historyId = "${userId}_${changeType}_${Clock.System.now().toEpochMilliseconds()}"
        val entry = com.eunio.healthapp.data.remote.datasource.SettingsHistoryEntry(
            historyId = historyId,
            userId = userId,
            settingsSnapshot = settings,
            changeType = changeType,
            timestamp = Clock.System.now().toEpochMilliseconds(),
            deviceInfo = deviceInfo
        )
        
        history.getOrPut(userId) { mutableListOf() }.add(entry)
        return Result.success(historyId)
    }
    
    override suspend fun getSettingsHistory(
        userId: String,
        limit: Int
    ): Result<List<com.eunio.healthapp.data.remote.datasource.SettingsHistoryEntry>> {
        if (shouldFailOperations) {
            return Result.error(SettingsError.SyncError("Simulated network failure"))
        }
        return Result.success(history[userId]?.take(limit) ?: emptyList())
    }
    
    override suspend fun deleteSettingsHistory(userId: String): Result<Unit> {
        if (shouldFailOperations) {
            return Result.error(SettingsError.SyncError("Simulated network failure"))
        }
        history.remove(userId)
        return Result.success(Unit)
    }
    
    override suspend fun batchUpdateSettings(settingsList: List<UserSettings>): Result<Int> {
        if (shouldFailOperations) {
            return Result.error(SettingsError.SyncError("Simulated network failure"))
        }
        
        var count = 0
        settingsList.forEach { settings ->
            this.settings[settings.userId] = settings
            count++
        }
        return Result.success(count)
    }
    
    override suspend fun getSettingsStatistics(userId: String): Result<com.eunio.healthapp.data.remote.datasource.SettingsStatistics> {
        if (shouldFailOperations) {
            return Result.error(SettingsError.SyncError("Simulated network failure"))
        }
        
        val stats = com.eunio.healthapp.data.remote.datasource.SettingsStatistics(
            userId = userId,
            totalUpdates = history[userId]?.size?.toLong() ?: 0L,
            lastUpdateTime = settings[userId]?.lastModified?.toEpochMilliseconds() ?: 0L,
            mostChangedSection = "general",
            syncFrequency = 1.0,
            averageUpdateSize = 1024L
        )
        return Result.success(stats)
    }
    
    override suspend fun validateConnection(): Result<Unit> {
        if (shouldFailOperations) {
            return Result.error(SettingsError.SyncError("Simulated network failure"))
        }
        return Result.success(Unit)
    }
    
    override suspend fun getServerTimestamp(): Result<Long> {
        if (shouldFailOperations) {
            return Result.error(SettingsError.SyncError("Simulated network failure"))
        }
        return Result.success(Clock.System.now().toEpochMilliseconds())
    }
    
    override suspend fun healthCheck(): Result<com.eunio.healthapp.data.remote.datasource.RemoteStorageHealth> {
        if (shouldFailOperations) {
            return Result.error(SettingsError.SyncError("Simulated network failure"))
        }
        
        val health = com.eunio.healthapp.data.remote.datasource.RemoteStorageHealth(
            isHealthy = true,
            latency = 100L,
            lastSuccessfulOperation = Clock.System.now().toEpochMilliseconds(),
            errorCount = 0,
            version = "1.0.0"
        )
        return Result.success(health)
    }
}

class TestNetworkConnectivity : NetworkConnectivity {
    private var connected = true
    private var networkType = NetworkType.WIFI
    
    fun setConnected(connected: Boolean) {
        this.connected = connected
    }
    
    fun setNetworkType(type: NetworkType) {
        this.networkType = type
    }
    
    override fun isConnected(): Boolean = connected
    
    override fun observeConnectivity(): Flow<Boolean> = flowOf(connected)
    
    override suspend fun hasStableConnection(): Boolean = connected
    
    override fun getNetworkType(): NetworkType = networkType
}

class TestUserRepository : UserRepository {
    private var currentUser: User? = null
    
    fun setCurrentUser(user: User?) {
        currentUser = user
    }
    
    override suspend fun getCurrentUser(): Result<User?> {
        return Result.success(currentUser)
    }
    
    override suspend fun updateUser(user: User): Result<Unit> {
        currentUser = user
        return Result.success(Unit)
    }
    
    override suspend fun completeOnboarding(userId: String, primaryGoal: com.eunio.healthapp.domain.model.HealthGoal): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun createUser(email: String, password: String, name: String): Result<User> {
        val user = User(
            id = "new-user",
            email = email,
            name = name,
            onboardingComplete = false,
            primaryGoal = com.eunio.healthapp.domain.model.HealthGoal.CYCLE_TRACKING,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        return Result.success(user)
    }
    
    override suspend fun signInUser(email: String, password: String): Result<User> {
        return currentUser?.let { Result.success(it) } ?: Result.error(com.eunio.healthapp.domain.error.AppError.AuthenticationError("User not found"))
    }
    
    override suspend fun signOutUser(): Result<Unit> {
        currentUser = null
        return Result.success(Unit)
    }
    
    override suspend fun deleteUser(userId: String): Result<Unit> {
        if (currentUser?.id == userId) {
            currentUser = null
        }
        return Result.success(Unit)
    }
}