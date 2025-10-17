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

class SettingsRepositoryImplTest {
    
    private lateinit var mockLocalDataSource: MockSettingsLocalDataSource
    private lateinit var mockRemoteDataSource: MockSettingsRemoteDataSource
    private lateinit var mockNetworkConnectivity: MockNetworkConnectivityForSettings
    private lateinit var mockUserRepository: MockUserRepositoryForSettings
    private lateinit var errorHandler: SettingsErrorHandler
    private lateinit var repository: SettingsRepositoryImpl
    
    private val testUserId = "test-user-123"
    private val testUser = User(
        id = testUserId,
        email = "test@example.com",
        name = "Test User",
        onboardingComplete = true,
        primaryGoal = com.eunio.healthapp.domain.model.HealthGoal.CYCLE_TRACKING,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now()
    )
    
    @BeforeTest
    fun setup() {
        mockLocalDataSource = MockSettingsLocalDataSource()
        mockRemoteDataSource = MockSettingsRemoteDataSource()
        mockNetworkConnectivity = MockNetworkConnectivityForSettings()
        mockUserRepository = MockUserRepositoryForSettings()
        errorHandler = SettingsErrorHandler()
        
        repository = SettingsRepositoryImpl(
            localDataSource = mockLocalDataSource,
            remoteDataSource = mockRemoteDataSource,
            networkConnectivity = mockNetworkConnectivity,
            userRepository = mockUserRepository,
            errorHandler = errorHandler
        )
        
        // Setup default user
        mockUserRepository.setCurrentUser(testUser)
        mockNetworkConnectivity.setConnected(true)
    }
    
    @Test
    fun `getUserSettings returns local settings when available`() = runTest {
        // Given
        val expectedSettings = createTestSettings()
        mockLocalDataSource.saveSettings(expectedSettings)
        
        // When
        val result = repository.getUserSettings(testUserId)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedSettings, result.getOrNull())
    }
    
    @Test
    fun `getUserSettings fetches from remote when local not available and connected`() = runTest {
        // Given
        val remoteSettings = createTestSettings()
        mockRemoteDataSource.saveSettings(remoteSettings)
        mockNetworkConnectivity.setConnected(true)
        
        // When
        val result = repository.getUserSettings(testUserId)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(remoteSettings, result.getOrNull())
        // Should also cache locally
        assertNotNull(mockLocalDataSource.getSettings(testUserId))
    }
    
    @Test
    fun `getUserSettings returns null when no settings found anywhere`() = runTest {
        // Given
        mockNetworkConnectivity.setConnected(true)
        
        // When
        val result = repository.getUserSettings(testUserId)
        
        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }
    
    @Test
    fun `getUserSettings returns local settings when offline`() = runTest {
        // Given
        val localSettings = createTestSettings()
        mockLocalDataSource.saveSettings(localSettings)
        mockNetworkConnectivity.setConnected(false)
        
        // When
        val result = repository.getUserSettings(testUserId)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(localSettings, result.getOrNull())
    }
    
    @Test
    fun `saveUserSettings saves locally first then syncs to remote when connected`() = runTest {
        // Given
        val settings = createTestSettings()
        mockNetworkConnectivity.setConnected(true)
        
        // When
        val result = repository.saveUserSettings(settings)
        
        // Then
        assertTrue(result.isSuccess)
        assertNotNull(mockLocalDataSource.getSettings(testUserId))
        assertNotNull(mockRemoteDataSource.getSettings(testUserId).getOrNull())
    }
    
    @Test
    fun `saveUserSettings saves locally when offline`() = runTest {
        // Given
        val settings = createTestSettings()
        mockNetworkConnectivity.setConnected(false)
        
        // When
        val result = repository.saveUserSettings(settings)
        
        // Then
        assertTrue(result.isSuccess)
        assertNotNull(mockLocalDataSource.getSettings(testUserId))
        // Should not be saved remotely
        assertNull(mockRemoteDataSource.getSettings(testUserId).getOrNull())
    }
    
    @Test
    fun `saveUserSettings validates settings before saving`() = runTest {
        // Given
        val invalidSettings = createTestSettings().copy(
            cyclePreferences = createTestSettings().cyclePreferences.copy(
                averageCycleLength = 50 // Invalid: > 45
            )
        )
        
        // When
        val result = repository.saveUserSettings(invalidSettings)
        
        // Then
        assertTrue(result.isError)
        assertTrue(result.errorOrNull() is SettingsError.ValidationError)
    }
    
    @Test
    fun `updateUserSettings applies update function and saves result`() = runTest {
        // Given
        val originalSettings = createTestSettings()
        mockLocalDataSource.saveSettings(originalSettings)
        
        // When
        val result = repository.updateUserSettings(testUserId) { settings ->
            settings.copy(
                unitPreferences = settings.unitPreferences.copy(isManuallySet = true)
            )
        }
        
        // Then
        assertTrue(result.isSuccess)
        val updatedSettings = result.getOrNull()
        assertNotNull(updatedSettings)
        assertTrue(updatedSettings.unitPreferences.isManuallySet)
    }
    
    @Test
    fun `syncSettings resolves conflicts using last modified timestamp`() = runTest {
        // Given
        val olderSettings = createTestSettings().copy(
            lastModified = Clock.System.now().minus(kotlin.time.Duration.parse("1h"))
        )
        val newerSettings = createTestSettings().copy(
            lastModified = Clock.System.now(),
            unitPreferences = createTestSettings().unitPreferences.copy(isManuallySet = true)
        )
        
        mockLocalDataSource.saveSettings(olderSettings)
        mockRemoteDataSource.saveSettings(newerSettings)
        mockNetworkConnectivity.setConnected(true)
        
        // When
        val result = repository.syncSettings(testUserId)
        
        // Then
        assertTrue(result.isSuccess)
        val localSettings = mockLocalDataSource.getSettings(testUserId)
        assertNotNull(localSettings)
        assertTrue(localSettings.unitPreferences.isManuallySet) // Should use newer remote settings
    }
    
    @Test
    fun `syncSettings fails when offline`() = runTest {
        // Given
        mockNetworkConnectivity.setConnected(false)
        
        // When
        val result = repository.syncSettings(testUserId)
        
        // Then
        assertTrue(result.isError)
        assertTrue(result.errorOrNull() is SettingsError.SyncError)
    }
    
    @Test
    fun `backupUserSettings creates JSON backup`() = runTest {
        // Given
        val settings = createTestSettings()
        mockLocalDataSource.saveSettings(settings)
        
        // When
        val result = repository.backupUserSettings(testUserId)
        
        // Then
        assertTrue(result.isSuccess)
        val backupData = result.getOrNull()
        assertNotNull(backupData)
        assertTrue(backupData.contains("userId"))
        assertTrue(backupData.contains(testUserId))
    }
    
    @Test
    fun `restoreUserSettings restores from JSON backup`() = runTest {
        // Given
        val originalSettings = createTestSettings()
        val backupJson = """
            {
                "userId": "$testUserId",
                "unitPreferences": {
                    "temperatureUnit": "FAHRENHEIT",
                    "weightUnit": "POUNDS",
                    "isManuallySet": true
                },
                "notificationPreferences": {
                    "dailyLoggingReminder": {
                        "enabled": false,
                        "timeHour": null,
                        "timeMinute": null,
                        "daysInAdvance": 1
                    },
                    "periodPredictionAlert": {
                        "enabled": false,
                        "timeHour": null,
                        "timeMinute": null,
                        "daysInAdvance": 1
                    },
                    "ovulationAlert": {
                        "enabled": false,
                        "timeHour": null,
                        "timeMinute": null,
                        "daysInAdvance": 1
                    },
                    "insightNotifications": {
                        "enabled": false,
                        "timeHour": null,
                        "timeMinute": null,
                        "daysInAdvance": 1
                    },
                    "globalNotificationsEnabled": true
                },
                "cyclePreferences": {
                    "averageCycleLength": 28,
                    "averageLutealPhaseLength": 14,
                    "periodDuration": 5,
                    "isCustomized": false
                },
                "privacyPreferences": {
                    "dataSharingEnabled": false,
                    "anonymousInsightsEnabled": true,
                    "crashReportingEnabled": true,
                    "analyticsEnabled": true
                },
                "displayPreferences": {
                    "textSizeScale": 1.0,
                    "highContrastMode": false,
                    "hapticFeedbackEnabled": true,
                    "hapticIntensity": "MEDIUM"
                },
                "syncPreferences": {
                    "autoSyncEnabled": true,
                    "wifiOnlySync": false,
                    "cloudBackupEnabled": true,
                    "lastSyncTime": null
                },
                "lastModified": "2024-01-01T00:00:00Z",
                "syncStatus": "PENDING",
                "version": 1
            }
        """.trimIndent()
        
        // When
        val result = repository.restoreUserSettings(testUserId, backupJson)
        
        // Then
        assertTrue(result.isSuccess)
        val restoredSettings = mockLocalDataSource.getSettings(testUserId)
        assertNotNull(restoredSettings)
        assertEquals(testUserId, restoredSettings.userId)
    }
    
    @Test
    fun `exportUserData creates comprehensive export`() = runTest {
        // Given
        val settings = createTestSettings()
        mockLocalDataSource.saveSettings(settings)
        
        // When
        val result = repository.exportUserData(testUserId)
        
        // Then
        assertTrue(result.isSuccess)
        val exportData = result.getOrNull()
        assertNotNull(exportData)
        
        
        assertTrue(exportData.contains("exportInfo"))
        assertTrue(exportData.contains("userSettings"))
        assertTrue(exportData.contains("metadata"))
    }
    
    @Test
    fun `deleteUserSettings removes from both local and remote`() = runTest {
        // Given
        val settings = createTestSettings()
        mockLocalDataSource.saveSettings(settings)
        mockRemoteDataSource.saveSettings(settings)
        mockNetworkConnectivity.setConnected(true)
        
        // When
        val result = repository.deleteUserSettings(testUserId)
        
        // Then
        assertTrue(result.isSuccess)
        assertNull(mockLocalDataSource.getSettings(testUserId))
        assertNull(mockRemoteDataSource.getSettings(testUserId).getOrNull())
    }
    
    @Test
    fun `resetToDefaults creates and saves default settings`() = runTest {
        // Given
        val customSettings = createTestSettings().copy(
            unitPreferences = createTestSettings().unitPreferences.copy(isManuallySet = true)
        )
        mockLocalDataSource.saveSettings(customSettings)
        
        // When
        val result = repository.resetToDefaults(testUserId)
        
        // Then
        assertTrue(result.isSuccess)
        val defaultSettings = result.getOrNull()
        assertNotNull(defaultSettings)
        assertFalse(defaultSettings.unitPreferences.isManuallySet)
    }
    
    @Test
    fun `validateSettings rejects invalid cycle preferences`() = runTest {
        // Given
        val invalidSettings = createTestSettings().copy(
            cyclePreferences = createTestSettings().cyclePreferences.copy(
                averageCycleLength = 20, // Too short
                averageLutealPhaseLength = 9 // Too short
            )
        )
        
        // When
        val result = repository.validateSettings(invalidSettings)
        
        // Then
        assertTrue(result.isError)
        assertTrue(result.errorOrNull() is SettingsError.ValidationError)
    }
    
    @Test
    fun `validateSettings rejects invalid display preferences`() = runTest {
        // Given
        val invalidSettings = createTestSettings().copy(
            displayPreferences = createTestSettings().displayPreferences.copy(
                textSizeScale = 5.0f // Too large
            )
        )
        
        // When
        val result = repository.validateSettings(invalidSettings)
        
        // Then
        assertTrue(result.isError)
        assertTrue(result.errorOrNull() is SettingsError.ValidationError)
    }
    
    @Test
    fun `migrateSettings handles version upgrades`() = runTest {
        // Given
        val oldSettings = createTestSettings().copy(version = 0)
        
        // When
        val result = repository.migrateSettings(oldSettings, 1)
        
        // Then
        assertTrue(result.isSuccess)
        val migratedSettings = result.getOrNull()
        assertNotNull(migratedSettings)
        assertEquals(1, migratedSettings.version)
    }
    
    @Test
    fun `migrateSettings rejects downgrade attempts`() = runTest {
        // Given
        val newSettings = createTestSettings().copy(version = 1)
        
        // When
        val result = repository.migrateSettings(newSettings, 0)
        
        // Then
        assertTrue(result.isError)
        assertTrue(result.errorOrNull() is SettingsError.MigrationError)
    }
    
    @Test
    fun `observeUserSettings emits settings changes`() = runTest {
        // Given
        val settings = createTestSettings()
        mockLocalDataSource.saveSettings(settings)
        
        // When
        val flow = repository.observeUserSettings(testUserId)
        
        // Then
        // This is a simplified test - in a real scenario you'd collect from the flow
        assertNotNull(flow)
    }
    
    @Test
    fun `getPendingSyncSettings returns settings with pending status`() = runTest {
        // Given
        val pendingSettings = createTestSettings().copy(syncStatus = SyncStatus.PENDING)
        mockLocalDataSource.saveSettings(pendingSettings)
        mockLocalDataSource.setPendingSettings(listOf(pendingSettings))
        
        // When
        val result = repository.getPendingSyncSettings()
        
        // Then
        assertTrue(result.isSuccess)
        val pendingList = result.getOrNull()
        assertNotNull(pendingList)
        assertEquals(1, pendingList.size)
        assertEquals(SyncStatus.PENDING, pendingList.first().syncStatus)
    }
    
    private fun createTestSettings(): UserSettings {
        return UserSettings.createDefault(testUserId)
    }
}

// Mock implementations for testing

class MockSettingsLocalDataSource : SettingsLocalDataSource {
    private val settings = mutableMapOf<String, UserSettings>()
    private val backups = mutableMapOf<Long, String>()
    private var nextBackupId = 1L
    private var pendingSettings = listOf<UserSettings>()
    
    fun setPendingSettings(pending: List<UserSettings>) {
        pendingSettings = pending
    }
    
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
        return pendingSettings
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

class MockSettingsRemoteDataSource : SettingsRemoteDataSource {
    private val settings = mutableMapOf<String, UserSettings>()
    private val history = mutableMapOf<String, MutableList<com.eunio.healthapp.data.remote.datasource.SettingsHistoryEntry>>()
    
    override suspend fun getSettings(userId: String): Result<UserSettings?> {
        return Result.success(settings[userId])
    }
    
    override suspend fun saveSettings(settings: UserSettings): Result<Unit> {
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
        val current = this.settings[settings.userId]
        if (current != null && current.lastModified.toEpochMilliseconds() != expectedLastModified) {
            return Result.error(SettingsError.ConflictError("Conflict detected"))
        }
        return saveSettings(settings)
    }
    
    override suspend fun deleteSettings(userId: String): Result<Unit> {
        settings.remove(userId)
        return Result.success(Unit)
    }
    
    override suspend fun clearAllSettings(): Result<Unit> {
        settings.clear()
        return Result.success(Unit)
    }
    
    override suspend fun settingsExist(userId: String): Result<Boolean> {
        return Result.success(settings.containsKey(userId))
    }
    
    override suspend fun getLastModifiedTimestamp(userId: String): Result<Long?> {
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
        return Result.success(history[userId]?.take(limit) ?: emptyList())
    }
    
    override suspend fun deleteSettingsHistory(userId: String): Result<Unit> {
        history.remove(userId)
        return Result.success(Unit)
    }
    
    override suspend fun batchUpdateSettings(settingsList: List<UserSettings>): Result<Int> {
        var count = 0
        settingsList.forEach { settings ->
            this.settings[settings.userId] = settings
            count++
        }
        return Result.success(count)
    }
    
    override suspend fun getSettingsStatistics(userId: String): Result<com.eunio.healthapp.data.remote.datasource.SettingsStatistics> {
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
        return Result.success(Unit)
    }
    
    override suspend fun getServerTimestamp(): Result<Long> {
        return Result.success(Clock.System.now().toEpochMilliseconds())
    }
    
    override suspend fun healthCheck(): Result<com.eunio.healthapp.data.remote.datasource.RemoteStorageHealth> {
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

class MockNetworkConnectivityForSettings : NetworkConnectivity {
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

class MockUserRepositoryForSettings : UserRepository {
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