package com.eunio.healthapp.data.remote.datasource

import com.eunio.healthapp.domain.error.SettingsError
import com.eunio.healthapp.domain.model.settings.UserSettings
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsRemoteDataSourceTest {
    
    private val testUserId = "test-user-123"
    private val testSettings = UserSettings.createDefault(testUserId)
    
    @Test
    fun `mock remote data source should handle basic operations`() = runTest {
        val dataSource = MockSettingsRemoteDataSource()
        
        // Test save
        val saveResult = dataSource.saveSettings(testSettings)
        assertTrue(saveResult.isSuccess)
        
        // Test get
        val getResult = dataSource.getSettings(testUserId)
        assertTrue(getResult.isSuccess)
        assertNotNull(getResult.getOrNull())
        assertEquals(testUserId, getResult.getOrNull()?.userId)
        
        // Test update
        val updatedSettings = testSettings.copy(
            unitPreferences = testSettings.unitPreferences.copy(isManuallySet = true)
        )
        val updateResult = dataSource.updateSettings(updatedSettings)
        assertTrue(updateResult.isSuccess)
        
        // Verify update
        val afterUpdate = dataSource.getSettings(testUserId)
        assertTrue(afterUpdate.isSuccess)
        assertTrue(afterUpdate.getOrNull()?.unitPreferences?.isManuallySet == true)
        
        // Test delete
        val deleteResult = dataSource.deleteSettings(testUserId)
        assertTrue(deleteResult.isSuccess)
        
        // Verify deletion
        val afterDelete = dataSource.getSettings(testUserId)
        assertTrue(afterDelete.isSuccess)
        assertNull(afterDelete.getOrNull())
    }
    
    @Test
    fun `mock remote data source should handle conditional updates`() = runTest {
        val dataSource = MockSettingsRemoteDataSource()
        
        // Save initial settings
        dataSource.saveSettings(testSettings)
        
        val currentTimestamp = testSettings.lastModified.toEpochMilliseconds()
        val updatedSettings = testSettings.copy(
            unitPreferences = testSettings.unitPreferences.copy(isManuallySet = true),
            lastModified = Clock.System.now()
        )
        
        // Test conditional update with correct timestamp
        val conditionalResult = dataSource.conditionalUpdateSettings(
            updatedSettings,
            currentTimestamp
        )
        assertTrue(conditionalResult.isSuccess)
        
        // Test conditional update with incorrect timestamp (should fail)
        val conflictResult = dataSource.conditionalUpdateSettings(
            updatedSettings,
            currentTimestamp - 1000
        )
        assertTrue(conflictResult.isError)
        assertTrue(conflictResult.errorOrNull() is SettingsError.ConflictResolutionError)
    }
    
    @Test
    fun `mock remote data source should handle existence checks`() = runTest {
        val dataSource = MockSettingsRemoteDataSource()
        
        // Check non-existent settings
        val existsBeforeSave = dataSource.settingsExist(testUserId)
        assertTrue(existsBeforeSave.isSuccess)
        assertFalse(existsBeforeSave.getOrNull() ?: true)
        
        // Save settings
        dataSource.saveSettings(testSettings)
        
        // Check existing settings
        val existsAfterSave = dataSource.settingsExist(testUserId)
        assertTrue(existsAfterSave.isSuccess)
        assertTrue(existsAfterSave.getOrNull() ?: false)
        
        // Get timestamp
        val timestampResult = dataSource.getLastModifiedTimestamp(testUserId)
        assertTrue(timestampResult.isSuccess)
        assertNotNull(timestampResult.getOrNull())
    }
    
    @Test
    fun `mock remote data source should handle settings history`() = runTest {
        val dataSource = MockSettingsRemoteDataSource()
        
        // Save settings
        dataSource.saveSettings(testSettings)
        
        // Create history entry
        val historyResult = dataSource.createSettingsHistory(
            testUserId,
            testSettings,
            "UPDATE",
            "Test Device"
        )
        assertTrue(historyResult.isSuccess)
        assertNotNull(historyResult.getOrNull())
        
        // Get history
        val getHistoryResult = dataSource.getSettingsHistory(testUserId, 10)
        assertTrue(getHistoryResult.isSuccess)
        assertEquals(1, getHistoryResult.getOrNull()?.size)
        
        val historyEntry = getHistoryResult.getOrNull()?.first()
        assertNotNull(historyEntry)
        assertEquals(testUserId, historyEntry.userId)
        assertEquals("UPDATE", historyEntry.changeType)
        assertEquals("Test Device", historyEntry.deviceInfo)
        
        // Delete history
        val deleteHistoryResult = dataSource.deleteSettingsHistory(testUserId)
        assertTrue(deleteHistoryResult.isSuccess)
        
        // Verify history deleted
        val afterDeleteHistory = dataSource.getSettingsHistory(testUserId, 10)
        assertTrue(afterDeleteHistory.isSuccess)
        assertEquals(0, afterDeleteHistory.getOrNull()?.size)
    }
    
    @Test
    fun `mock remote data source should handle batch operations`() = runTest {
        val dataSource = MockSettingsRemoteDataSource()
        
        val settingsList = listOf(
            UserSettings.createDefault("user1"),
            UserSettings.createDefault("user2"),
            UserSettings.createDefault("user3")
        )
        
        // Test batch update
        val batchResult = dataSource.batchUpdateSettings(settingsList)
        assertTrue(batchResult.isSuccess)
        assertEquals(3, batchResult.getOrNull())
        
        // Verify all settings were saved
        settingsList.forEach { settings ->
            val getResult = dataSource.getSettings(settings.userId)
            assertTrue(getResult.isSuccess)
            assertNotNull(getResult.getOrNull())
        }
    }
    
    @Test
    fun `mock remote data source should handle statistics`() = runTest {
        val dataSource = MockSettingsRemoteDataSource()
        
        // Save settings
        dataSource.saveSettings(testSettings)
        
        // Get statistics
        val statsResult = dataSource.getSettingsStatistics(testUserId)
        assertTrue(statsResult.isSuccess)
        
        val stats = statsResult.getOrNull()
        assertNotNull(stats)
        assertEquals(testUserId, stats.userId)
        assertTrue(stats.totalUpdates >= 0)
        assertTrue(stats.lastUpdateTime > 0)
    }
    
    @Test
    fun `mock remote data source should handle connection validation`() = runTest {
        val dataSource = MockSettingsRemoteDataSource()
        
        // Test connection validation
        val validateResult = dataSource.validateConnection()
        assertTrue(validateResult.isSuccess)
        
        // Test server timestamp
        val timestampResult = dataSource.getServerTimestamp()
        assertTrue(timestampResult.isSuccess)
        assertTrue(timestampResult.getOrNull()!! > 0)
        
        // Test health check
        val healthResult = dataSource.healthCheck()
        assertTrue(healthResult.isSuccess)
        
        val health = healthResult.getOrNull()
        assertNotNull(health)
        assertTrue(health.isHealthy)
        assertTrue(health.latency >= 0)
    }
    
    @Test
    fun `mock remote data source should handle observation`() = runTest {
        val dataSource = MockSettingsRemoteDataSource()
        
        // Save settings
        dataSource.saveSettings(testSettings)
        
        // Observe settings
        val settingsFlow = dataSource.observeSettings(testUserId)
        assertNotNull(settingsFlow)
        
        // The mock implementation returns a simple flow
        // In a real implementation, this would be a reactive Firestore listener
    }
    
    @Test
    fun `mock remote data source should handle clear operations`() = runTest {
        val dataSource = MockSettingsRemoteDataSource()
        
        // Save multiple settings
        dataSource.saveSettings(testSettings)
        dataSource.saveSettings(UserSettings.createDefault("user2"))
        
        // Clear all settings
        val clearResult = dataSource.clearAllSettings()
        assertTrue(clearResult.isSuccess)
        
        // Verify all cleared
        val getResult1 = dataSource.getSettings(testUserId)
        assertTrue(getResult1.isSuccess)
        assertNull(getResult1.getOrNull())
        
        val getResult2 = dataSource.getSettings("user2")
        assertTrue(getResult2.isSuccess)
        assertNull(getResult2.getOrNull())
    }
}

/**
 * Mock implementation of SettingsRemoteDataSource for testing
 */
private class MockSettingsRemoteDataSource : SettingsRemoteDataSource {
    private val settingsStorage = mutableMapOf<String, UserSettings>()
    private val historyStorage = mutableMapOf<String, MutableList<SettingsHistoryEntry>>()
    private var nextHistoryId = 1L
    
    override suspend fun getSettings(userId: String): Result<UserSettings?> {
        return try {
            Result.success(settingsStorage[userId])
        } catch (e: Exception) {
            Result.error(SettingsError.SyncError("Failed to get settings", cause = e))
        }
    }
    
    override suspend fun saveSettings(settings: UserSettings): Result<Unit> {
        return try {
            settingsStorage[settings.userId] = settings
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(SettingsError.SyncError("Failed to save settings", cause = e))
        }
    }
    
    override suspend fun updateSettings(settings: UserSettings): Result<Unit> {
        return try {
            if (settingsStorage.containsKey(settings.userId)) {
                settingsStorage[settings.userId] = settings
                Result.success(Unit)
            } else {
                Result.error(SettingsError.ValidationError("Settings not found for user: ${settings.userId}"))
            }
        } catch (e: Exception) {
            Result.error(SettingsError.SyncError("Failed to update settings", cause = e))
        }
    }
    
    override suspend fun conditionalUpdateSettings(
        settings: UserSettings,
        expectedLastModified: Long
    ): Result<Unit> {
        return try {
            val currentSettings = settingsStorage[settings.userId]
            if (currentSettings != null) {
                val currentTimestamp = currentSettings.lastModified.toEpochMilliseconds()
                if (currentTimestamp == expectedLastModified) {
                    settingsStorage[settings.userId] = settings
                    Result.success(Unit)
                } else {
                    Result.error(
                        SettingsError.ConflictResolutionError(
                            "Timestamp mismatch",
                            expectedLastModified,
                            currentTimestamp
                        )
                    )
                }
            } else {
                Result.error(SettingsError.ValidationError("Settings not found for user: ${settings.userId}"))
            }
        } catch (e: Exception) {
            Result.error(SettingsError.SyncError("Failed to conditionally update settings", cause = e))
        }
    }
    
    override suspend fun deleteSettings(userId: String): Result<Unit> {
        return try {
            settingsStorage.remove(userId)
            historyStorage.remove(userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(SettingsError.SyncError("Failed to delete settings", cause = e))
        }
    }
    
    override suspend fun clearAllSettings(): Result<Unit> {
        return try {
            settingsStorage.clear()
            historyStorage.clear()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(SettingsError.SyncError("Failed to clear all settings", cause = e))
        }
    }
    
    override suspend fun settingsExist(userId: String): Result<Boolean> {
        return try {
            Result.success(settingsStorage.containsKey(userId))
        } catch (e: Exception) {
            Result.error(SettingsError.SyncError("Failed to check settings existence", cause = e))
        }
    }
    
    override suspend fun getLastModifiedTimestamp(userId: String): Result<Long?> {
        return try {
            val timestamp = settingsStorage[userId]?.lastModified?.toEpochMilliseconds()
            Result.success(timestamp)
        } catch (e: Exception) {
            Result.error(SettingsError.SyncError("Failed to get timestamp", cause = e))
        }
    }
    
    override fun observeSettings(userId: String): Flow<UserSettings?> {
        return flowOf(settingsStorage[userId])
    }
    
    override suspend fun createSettingsHistory(
        userId: String,
        settings: UserSettings,
        changeType: String,
        deviceInfo: String?
    ): Result<String> {
        return try {
            val historyId = "history_${nextHistoryId++}"
            val historyEntry = SettingsHistoryEntry(
                historyId = historyId,
                userId = userId,
                settingsSnapshot = settings,
                changeType = changeType,
                timestamp = Clock.System.now().toEpochMilliseconds(),
                deviceInfo = deviceInfo
            )
            
            historyStorage.getOrPut(userId) { mutableListOf() }.add(historyEntry)
            Result.success(historyId)
        } catch (e: Exception) {
            Result.error(SettingsError.SyncError("Failed to create settings history", cause = e))
        }
    }
    
    override suspend fun getSettingsHistory(userId: String, limit: Int): Result<List<SettingsHistoryEntry>> {
        return try {
            val history = historyStorage[userId] ?: emptyList()
            Result.success(history.take(limit))
        } catch (e: Exception) {
            Result.error(SettingsError.SyncError("Failed to get settings history", cause = e))
        }
    }
    
    override suspend fun deleteSettingsHistory(userId: String): Result<Unit> {
        return try {
            historyStorage.remove(userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(SettingsError.SyncError("Failed to delete settings history", cause = e))
        }
    }
    
    override suspend fun batchUpdateSettings(settingsList: List<UserSettings>): Result<Int> {
        return try {
            var successCount = 0
            settingsList.forEach { settings ->
                settingsStorage[settings.userId] = settings
                successCount++
            }
            Result.success(successCount)
        } catch (e: Exception) {
            Result.error(SettingsError.SyncError("Failed to batch update settings", cause = e))
        }
    }
    
    override suspend fun getSettingsStatistics(userId: String): Result<SettingsStatistics> {
        return try {
            val settings = settingsStorage[userId]
            val stats = SettingsStatistics(
                userId = userId,
                totalUpdates = 1L,
                lastUpdateTime = settings?.lastModified?.toEpochMilliseconds() ?: 0L,
                mostChangedSection = "unitPreferences",
                syncFrequency = 1.0,
                averageUpdateSize = 1000L
            )
            Result.success(stats)
        } catch (e: Exception) {
            Result.error(SettingsError.SyncError("Failed to get settings statistics", cause = e))
        }
    }
    
    override suspend fun validateConnection(): Result<Unit> {
        return try {
            // Mock connection validation
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(SettingsError.SyncError("Connection validation failed", cause = e))
        }
    }
    
    override suspend fun getServerTimestamp(): Result<Long> {
        return try {
            Result.success(Clock.System.now().toEpochMilliseconds())
        } catch (e: Exception) {
            Result.error(SettingsError.SyncError("Failed to get server timestamp", cause = e))
        }
    }
    
    override suspend fun healthCheck(): Result<RemoteStorageHealth> {
        return try {
            val health = RemoteStorageHealth(
                isHealthy = true,
                latency = 50L,
                lastSuccessfulOperation = Clock.System.now().toEpochMilliseconds(),
                errorCount = 0,
                version = "1.0.0"
            )
            Result.success(health)
        } catch (e: Exception) {
            Result.error(SettingsError.SyncError("Health check failed", cause = e))
        }
    }
}