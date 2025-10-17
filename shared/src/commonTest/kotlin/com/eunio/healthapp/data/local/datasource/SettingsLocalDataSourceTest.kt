package com.eunio.healthapp.data.local.datasource

import com.eunio.healthapp.domain.error.SettingsError
import com.eunio.healthapp.domain.model.SyncStatus
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

class SettingsLocalDataSourceTest {
    
    private val testUserId = "test-user-123"
    private val testSettings = UserSettings.createDefault(testUserId)
    
    @Test
    fun `mock local data source should handle basic operations`() = runTest {
        val dataSource = MockSettingsLocalDataSource()
        
        // Test save
        val saveResult = dataSource.saveSettings(testSettings)
        assertTrue(saveResult.isSuccess)
        
        // Test get
        val retrievedSettings = dataSource.getSettings(testUserId)
        assertNotNull(retrievedSettings)
        assertEquals(testUserId, retrievedSettings.userId)
        
        // Test update
        val updatedSettings = testSettings.copy(
            unitPreferences = testSettings.unitPreferences.copy(isManuallySet = true)
        )
        val updateResult = dataSource.updateSettings(updatedSettings)
        assertTrue(updateResult.isSuccess)
        
        // Verify update
        val afterUpdate = dataSource.getSettings(testUserId)
        assertNotNull(afterUpdate)
        assertTrue(afterUpdate.unitPreferences.isManuallySet)
        
        // Test delete
        val deleteResult = dataSource.deleteSettings(testUserId)
        assertTrue(deleteResult.isSuccess)
        
        // Verify deletion
        val afterDelete = dataSource.getSettings(testUserId)
        assertNull(afterDelete)
    }
    
    @Test
    fun `mock local data source should handle sync status operations`() = runTest {
        val dataSource = MockSettingsLocalDataSource()
        
        // Save settings with pending sync
        val pendingSettings = testSettings.copy(syncStatus = SyncStatus.PENDING)
        dataSource.saveSettings(pendingSettings)
        
        // Get pending sync settings
        val pendingList = dataSource.getPendingSyncSettings()
        assertEquals(1, pendingList.size)
        assertEquals(testUserId, pendingList.first().userId)
        
        // Mark as synced
        val syncResult = dataSource.markAsSynced(testUserId)
        assertTrue(syncResult.isSuccess)
        
        // Verify sync status updated
        val syncedSettings = dataSource.getSettings(testUserId)
        assertNotNull(syncedSettings)
        assertEquals(SyncStatus.SYNCED, syncedSettings.syncStatus)
        
        // Mark as sync failed
        val failResult = dataSource.markAsSyncFailed(testUserId)
        assertTrue(failResult.isSuccess)
        
        // Verify sync status updated
        val failedSettings = dataSource.getSettings(testUserId)
        assertNotNull(failedSettings)
        assertEquals(SyncStatus.FAILED, failedSettings.syncStatus)
    }
    
    @Test
    fun `mock local data source should handle backup operations`() = runTest {
        val dataSource = MockSettingsLocalDataSource()
        
        // Save settings
        dataSource.saveSettings(testSettings)
        
        // Create backup
        val backupResult = dataSource.createSettingsBackup(testUserId, "MANUAL")
        assertTrue(backupResult.isSuccess)
        
        val backupId = backupResult.getOrNull()
        assertNotNull(backupId)
        
        // Get backup
        val backupData = dataSource.getSettingsBackup(backupId)
        assertTrue(backupData.isSuccess)
        assertNotNull(backupData.getOrNull())
        
        // Get user backups
        val userBackups = dataSource.getUserBackups(testUserId)
        assertTrue(userBackups.isSuccess)
        assertEquals(1, userBackups.getOrNull()?.size)
        
        // Delete backup
        val deleteResult = dataSource.deleteSettingsBackup(backupId)
        assertTrue(deleteResult.isSuccess)
        
        // Verify deletion
        val afterDelete = dataSource.getSettingsBackup(backupId)
        assertTrue(afterDelete.isSuccess)
        assertNull(afterDelete.getOrNull())
    }
    
    @Test
    fun `mock local data source should handle existence checks`() = runTest {
        val dataSource = MockSettingsLocalDataSource()
        
        // Check non-existent settings
        assertFalse(dataSource.settingsExist(testUserId))
        
        // Save settings
        dataSource.saveSettings(testSettings)
        
        // Check existing settings
        assertTrue(dataSource.settingsExist(testUserId))
        
        // Get timestamp
        val timestamp = dataSource.getLastModifiedTimestamp(testUserId)
        assertNotNull(timestamp)
        assertTrue(timestamp > 0)
    }
    
    @Test
    fun `mock local data source should handle clear operations`() = runTest {
        val dataSource = MockSettingsLocalDataSource()
        
        // Save multiple settings
        dataSource.saveSettings(testSettings)
        dataSource.saveSettings(UserSettings.createDefault("user2"))
        
        // Clear specific user
        val clearUserResult = dataSource.clearSettings(testUserId)
        assertTrue(clearUserResult.isSuccess)
        
        // Verify specific user cleared
        assertNull(dataSource.getSettings(testUserId))
        assertNotNull(dataSource.getSettings("user2"))
        
        // Clear all settings
        val clearAllResult = dataSource.clearAllSettings()
        assertTrue(clearAllResult.isSuccess)
        
        // Verify all cleared
        assertNull(dataSource.getSettings("user2"))
    }
    
    @Test
    fun `mock local data source should handle statistics operations`() = runTest {
        val dataSource = MockSettingsLocalDataSource()
        
        // Save settings
        dataSource.saveSettings(testSettings)
        dataSource.saveSettings(UserSettings.createDefault("user2"))
        
        // Get count
        val count = dataSource.getSettingsCount()
        assertEquals(2L, count)
        
        // Get data size
        val dataSize = dataSource.getSettingsDataSize()
        assertTrue(dataSize > 0)
        
        // Perform maintenance
        val maintenanceResult = dataSource.performMaintenance()
        assertTrue(maintenanceResult.isSuccess)
    }
    
    @Test
    fun `mock local data source should handle observation`() = runTest {
        val dataSource = MockSettingsLocalDataSource()
        
        // Save settings
        dataSource.saveSettings(testSettings)
        
        // Observe settings
        val settingsFlow = dataSource.observeSettings(testUserId)
        assertNotNull(settingsFlow)
        
        // The mock implementation returns a simple flow
        // In a real implementation, this would be a reactive flow
    }
}

/**
 * Mock implementation of SettingsLocalDataSource for testing
 */
private class MockSettingsLocalDataSource : SettingsLocalDataSource {
    private val settingsStorage = mutableMapOf<String, UserSettings>()
    private val backupStorage = mutableMapOf<Long, String>()
    private val backupMetadata = mutableMapOf<Long, SettingsBackupInfo>()
    private var nextBackupId = 1L
    
    override suspend fun getSettings(): UserSettings? {
        return settingsStorage.values.firstOrNull()
    }
    
    override suspend fun getSettings(userId: String): UserSettings? {
        return settingsStorage[userId]
    }
    
    override suspend fun saveSettings(settings: UserSettings): Result<Unit> {
        return try {
            settingsStorage[settings.userId] = settings
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(SettingsError.PersistenceError("Failed to save settings", cause = e))
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
            Result.error(SettingsError.PersistenceError("Failed to update settings", cause = e))
        }
    }
    
    override suspend fun deleteSettings(userId: String): Result<Unit> {
        return try {
            settingsStorage.remove(userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(SettingsError.PersistenceError("Failed to delete settings", cause = e))
        }
    }
    
    override suspend fun getPendingSyncSettings(): List<UserSettings> {
        return settingsStorage.values.filter { it.syncStatus == SyncStatus.PENDING }
    }
    
    override suspend fun markAsSynced(userId: String): Result<Unit> {
        return try {
            val settings = settingsStorage[userId]
            if (settings != null) {
                settingsStorage[userId] = settings.markAsSynced()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(SettingsError.SyncError("Failed to mark as synced", cause = e))
        }
    }
    
    override suspend fun markAsSyncFailed(userId: String): Result<Unit> {
        return try {
            val settings = settingsStorage[userId]
            if (settings != null) {
                settingsStorage[userId] = settings.markAsSyncError()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(SettingsError.SyncError("Failed to mark as sync failed", cause = e))
        }
    }
    
    override suspend fun clearAllSettings(): Result<Unit> {
        return try {
            settingsStorage.clear()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(SettingsError.PersistenceError("Failed to clear all settings", cause = e))
        }
    }
    
    override suspend fun clearSettings(userId: String): Result<Unit> {
        return try {
            settingsStorage.remove(userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(SettingsError.PersistenceError("Failed to clear settings", cause = e))
        }
    }
    
    override suspend fun settingsExist(userId: String): Boolean {
        return settingsStorage.containsKey(userId)
    }
    
    override suspend fun getLastModifiedTimestamp(userId: String): Long? {
        return settingsStorage[userId]?.lastModified?.toEpochMilliseconds()
    }
    
    override fun observeSettings(userId: String): Flow<UserSettings?> {
        return flowOf(settingsStorage[userId])
    }
    
    override suspend fun createSettingsBackup(userId: String, backupType: String): Result<Long> {
        return try {
            val settings = settingsStorage[userId]
                ?: return Result.error(SettingsError.ValidationError("Settings not found for user: $userId"))
            
            val backupId = nextBackupId++
            val backupData = """{"userId":"${settings.userId}","version":${settings.version}}"""
            
            backupStorage[backupId] = backupData
            backupMetadata[backupId] = SettingsBackupInfo(
                backupId = backupId,
                userId = userId,
                backupType = backupType,
                createdAt = Clock.System.now().toEpochMilliseconds(),
                dataSize = backupData.length.toLong()
            )
            
            Result.success(backupId)
        } catch (e: Exception) {
            Result.error(SettingsError.BackupError("Failed to create backup", cause = e))
        }
    }
    
    override suspend fun getSettingsBackup(backupId: Long): Result<String?> {
        return try {
            Result.success(backupStorage[backupId])
        } catch (e: Exception) {
            Result.error(SettingsError.BackupError("Failed to get backup", cause = e))
        }
    }
    
    override suspend fun getUserBackups(userId: String): Result<List<SettingsBackupInfo>> {
        return try {
            val userBackups = backupMetadata.values.filter { it.userId == userId }
            Result.success(userBackups)
        } catch (e: Exception) {
            Result.error(SettingsError.BackupError("Failed to get user backups", cause = e))
        }
    }
    
    override suspend fun deleteSettingsBackup(backupId: Long): Result<Unit> {
        return try {
            backupStorage.remove(backupId)
            backupMetadata.remove(backupId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(SettingsError.BackupError("Failed to delete backup", cause = e))
        }
    }
    
    override suspend fun deleteUserBackups(userId: String): Result<Unit> {
        return try {
            val userBackupIds = backupMetadata.values
                .filter { it.userId == userId }
                .map { it.backupId }
            
            userBackupIds.forEach { backupId ->
                backupStorage.remove(backupId)
                backupMetadata.remove(backupId)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(SettingsError.BackupError("Failed to delete user backups", cause = e))
        }
    }
    
    override suspend fun getSettingsCount(): Long {
        return settingsStorage.size.toLong()
    }
    
    override suspend fun getSettingsDataSize(): Long {
        return settingsStorage.values.sumOf { 1000L } // Mock size calculation
    }
    
    override suspend fun performMaintenance(): Result<Unit> {
        return try {
            // Mock maintenance operation
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(SettingsError.PersistenceError("Failed to perform maintenance", cause = e))
        }
    }
}