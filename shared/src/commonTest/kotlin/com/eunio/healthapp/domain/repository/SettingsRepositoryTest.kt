package com.eunio.healthapp.domain.repository

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

class SettingsRepositoryTest {
    
    private val testUserId = "test-user-123"
    private val testSettings = UserSettings.createDefault(testUserId)
    
    @Test
    fun `mock repository should handle basic CRUD operations`() = runTest {
        val repository = MockSettingsRepository()
        
        // Test save
        val saveResult = repository.saveUserSettings(testSettings)
        assertTrue(saveResult.isSuccess)
        
        // Test get
        val getResult = repository.getUserSettings(testUserId)
        assertTrue(getResult.isSuccess)
        assertNotNull(getResult.getOrNull())
        assertEquals(testUserId, getResult.getOrNull()?.userId)
        
        // Test delete
        val deleteResult = repository.deleteUserSettings(testUserId)
        assertTrue(deleteResult.isSuccess)
        
        // Verify deletion
        val getAfterDeleteResult = repository.getUserSettings(testUserId)
        assertTrue(getAfterDeleteResult.isSuccess)
        assertNull(getAfterDeleteResult.getOrNull())
    }
    
    @Test
    fun `mock repository should handle settings updates`() = runTest {
        val repository = MockSettingsRepository()
        
        // Save initial settings
        repository.saveUserSettings(testSettings)
        
        // Update settings
        val updateResult = repository.updateUserSettings(testUserId) { currentSettings ->
            currentSettings.copy(
                unitPreferences = currentSettings.unitPreferences.copy(isManuallySet = true),
                lastModified = Clock.System.now()
            )
        }
        
        assertTrue(updateResult.isSuccess)
        val updatedSettings = updateResult.getOrNull()
        assertNotNull(updatedSettings)
        assertTrue(updatedSettings.unitPreferences.isManuallySet)
    }
    
    @Test
    fun `mock repository should handle sync operations`() = runTest {
        val repository = MockSettingsRepository()
        
        // Save settings with pending sync
        val settingsWithPendingSync = testSettings.copy(syncStatus = SyncStatus.PENDING)
        repository.saveUserSettings(settingsWithPendingSync)
        
        // Test sync
        val syncResult = repository.syncSettings(testUserId)
        assertTrue(syncResult.isSuccess)
        
        // Verify sync status updated
        val settingsAfterSync = repository.getUserSettings(testUserId).getOrNull()
        assertNotNull(settingsAfterSync)
        assertEquals(SyncStatus.SYNCED, settingsAfterSync.syncStatus)
    }
    
    @Test
    fun `mock repository should handle backup operations`() = runTest {
        val repository = MockSettingsRepository()
        
        // Save settings
        repository.saveUserSettings(testSettings)
        
        // Test backup
        val backupResult = repository.backupUserSettings(testUserId)
        assertTrue(backupResult.isSuccess)
        
        val backupData = backupResult.getOrNull()
        assertNotNull(backupData)
        assertTrue(backupData.contains(testUserId))
    }
    
    @Test
    fun `mock repository should handle restore operations`() = runTest {
        val repository = MockSettingsRepository()
        
        // Create backup data
        val backupData = """{"userId":"$testUserId","version":1}"""
        
        // Test restore
        val restoreResult = repository.restoreUserSettings(testUserId, backupData)
        assertTrue(restoreResult.isSuccess)
        
        // Verify settings were restored
        val restoredSettings = repository.getUserSettings(testUserId).getOrNull()
        assertNotNull(restoredSettings)
        assertEquals(testUserId, restoredSettings.userId)
    }
    
    @Test
    fun `mock repository should handle validation errors`() = runTest {
        val repository = MockSettingsRepository()
        
        // Create invalid settings
        val invalidSettings = testSettings.copy(userId = "")
        
        // Test validation
        val validationResult = repository.validateSettings(invalidSettings)
        assertTrue(validationResult.isError)
        
        val error = validationResult.errorOrNull()
        assertTrue(error is SettingsError.ValidationError)
    }
    
    @Test
    fun `mock repository should handle conflict resolution`() = runTest {
        val repository = MockSettingsRepository()
        
        val now = Clock.System.now()
        val localSettings = testSettings.copy(lastModified = now)
        val remoteSettings = testSettings.copy(
            lastModified = kotlinx.datetime.Instant.fromEpochMilliseconds(now.toEpochMilliseconds() + 1000),
            unitPreferences = testSettings.unitPreferences.copy(isManuallySet = true)
        )
        
        val conflictResult = repository.resolveSettingsConflict(
            testUserId,
            localSettings,
            remoteSettings
        )
        
        assertTrue(conflictResult.isSuccess)
        val resolvedSettings = conflictResult.getOrNull()
        assertNotNull(resolvedSettings)
        // Should use the more recent version
        assertTrue(resolvedSettings.unitPreferences.isManuallySet)
    }
    
    @Test
    fun `mock repository should handle settings existence checks`() = runTest {
        val repository = MockSettingsRepository()
        
        // Check non-existent settings
        val existsBeforeSave = repository.settingsExist(testUserId)
        assertTrue(existsBeforeSave.isSuccess)
        assertFalse(existsBeforeSave.getOrNull() ?: true)
        
        // Save settings
        repository.saveUserSettings(testSettings)
        
        // Check existing settings
        val existsAfterSave = repository.settingsExist(testUserId)
        assertTrue(existsAfterSave.isSuccess)
        assertTrue(existsAfterSave.getOrNull() ?: false)
    }
    
    @Test
    fun `mock repository should handle reset to defaults`() = runTest {
        val repository = MockSettingsRepository()
        
        // Save customized settings
        val customizedSettings = testSettings.copy(
            unitPreferences = testSettings.unitPreferences.copy(isManuallySet = true)
        )
        repository.saveUserSettings(customizedSettings)
        
        // Reset to defaults
        val resetResult = repository.resetToDefaults(testUserId, "en_US")
        assertTrue(resetResult.isSuccess)
        
        val resetSettings = resetResult.getOrNull()
        assertNotNull(resetSettings)
        assertFalse(resetSettings.unitPreferences.isManuallySet)
    }
    
    @Test
    fun `mock repository should handle migration`() = runTest {
        val repository = MockSettingsRepository()
        
        // Create settings with old version
        val oldVersionSettings = testSettings.copy(version = 0)
        
        // Test migration
        val migrationResult = repository.migrateSettings(oldVersionSettings, 1)
        assertTrue(migrationResult.isSuccess)
        
        val migratedSettings = migrationResult.getOrNull()
        assertNotNull(migratedSettings)
        assertEquals(1, migratedSettings.version)
    }
}

/**
 * Mock implementation of SettingsRepository for testing
 */
private class MockSettingsRepository : SettingsRepository {
    private val settingsStorage = mutableMapOf<String, UserSettings>()
    
    // Core getUserSettings methods - consistent implementation
    override suspend fun getUserSettings(): Result<UserSettings?> {
        return Result.success(settingsStorage.values.firstOrNull())
    }
    
    override suspend fun getUserSettings(userId: String): Result<UserSettings?> {
        return Result.success(settingsStorage[userId])
    }
    
    override suspend fun saveUserSettings(settings: UserSettings): Result<Unit> {
        return try {
            settingsStorage[settings.userId] = settings
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(SettingsError.PersistenceError("Failed to save settings", cause = e))
        }
    }
    
    override suspend fun updateUserSettings(
        userId: String,
        updateFunction: (UserSettings) -> UserSettings
    ): Result<UserSettings> {
        return try {
            val currentSettings = settingsStorage[userId] 
                ?: return Result.error(SettingsError.ValidationError("Settings not found for user: $userId"))
            
            val updatedSettings = updateFunction(currentSettings)
            settingsStorage[userId] = updatedSettings
            Result.success(updatedSettings)
        } catch (e: Exception) {
            Result.error(SettingsError.PersistenceError("Failed to update settings", cause = e))
        }
    }
    
    override suspend fun syncSettings(): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun syncSettings(userId: String): Result<Unit> {
        return try {
            val settings = settingsStorage[userId]
            if (settings != null) {
                settingsStorage[userId] = settings.markAsSynced()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(SettingsError.SyncError("Failed to sync settings", cause = e))
        }
    }
    
    override suspend fun resolveSettingsConflict(
        userId: String,
        localSettings: UserSettings,
        remoteSettings: UserSettings
    ): Result<UserSettings> {
        return try {
            // Use the more recent version
            val resolvedSettings = if (localSettings.lastModified >= remoteSettings.lastModified) {
                localSettings
            } else {
                remoteSettings
            }
            settingsStorage[userId] = resolvedSettings
            Result.success(resolvedSettings)
        } catch (e: Exception) {
            Result.error(SettingsError.ConflictResolutionError("Failed to resolve conflict", cause = e))
        }
    }
    
    override suspend fun backupUserSettings(userId: String): Result<String> {
        return try {
            val settings = settingsStorage[userId]
                ?: return Result.error(SettingsError.ValidationError("Settings not found for user: $userId"))
            
            val backupData = """{"userId":"${settings.userId}","version":${settings.version}}"""
            Result.success(backupData)
        } catch (e: Exception) {
            Result.error(SettingsError.BackupError("Failed to backup settings", cause = e))
        }
    }
    
    override suspend fun restoreUserSettings(userId: String, backupData: String): Result<Unit> {
        return try {
            // Simple mock restore - just create default settings
            val restoredSettings = UserSettings.createDefault(userId)
            settingsStorage[userId] = restoredSettings
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(SettingsError.BackupError("Failed to restore settings", cause = e))
        }
    }
    
    override suspend fun exportUserData(userId: String): Result<String> {
        return try {
            val settings = settingsStorage[userId]
                ?: return Result.error(SettingsError.ValidationError("Settings not found for user: $userId"))
            
            Result.success("Exported data for user: ${settings.userId}")
        } catch (e: Exception) {
            Result.error(SettingsError.ExportError("Failed to export data", cause = e))
        }
    }
    
    override suspend fun deleteUserSettings(userId: String): Result<Unit> {
        return try {
            settingsStorage.remove(userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(SettingsError.PersistenceError("Failed to delete settings", cause = e))
        }
    }
    
    override suspend fun clearLocalSettings(): Result<Unit> {
        return try {
            settingsStorage.clear()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(SettingsError.PersistenceError("Failed to clear settings", cause = e))
        }
    }
    
    override suspend fun resetToDefaults(userId: String, locale: String?): Result<UserSettings> {
        return try {
            val defaultSettings = UserSettings.createDefault(userId, locale)
            settingsStorage[userId] = defaultSettings
            Result.success(defaultSettings)
        } catch (e: Exception) {
            Result.error(SettingsError.ValidationError("Failed to reset settings", cause = e))
        }
    }
    
    override fun observeUserSettings(userId: String): Flow<UserSettings?> {
        return flowOf(settingsStorage[userId])
    }
    
    override fun observeSyncStatus(): Flow<Boolean> {
        return flowOf(true)
    }
    
    override suspend fun getPendingSyncSettings(): Result<List<UserSettings>> {
        return try {
            val pendingSettings = settingsStorage.values.filter { it.needsSync() }
            Result.success(pendingSettings)
        } catch (e: Exception) {
            Result.error(SettingsError.SyncError("Failed to get pending sync settings", cause = e))
        }
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
    
    override suspend fun settingsExist(userId: String): Result<Boolean> {
        return Result.success(settingsStorage.containsKey(userId))
    }
    
    override suspend fun getLastModifiedTimestamp(userId: String): Result<Long?> {
        return try {
            val settings = settingsStorage[userId]
            Result.success(settings?.lastModified?.toEpochMilliseconds())
        } catch (e: Exception) {
            Result.error(SettingsError.ValidationError("Failed to get timestamp", cause = e))
        }
    }
    
    override suspend fun validateSettings(settings: UserSettings): Result<Unit> {
        return if (settings.isValid()) {
            Result.success(Unit)
        } else {
            Result.error(SettingsError.ValidationError("Settings validation failed"))
        }
    }
    
    override suspend fun migrateSettings(settings: UserSettings, targetVersion: Int): Result<UserSettings> {
        return try {
            val migratedSettings = settings.copy(version = targetVersion)
            Result.success(migratedSettings)
        } catch (e: Exception) {
            Result.error(SettingsError.MigrationError("Failed to migrate settings", cause = e))
        }
    }
}