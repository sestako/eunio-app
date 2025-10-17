package com.eunio.healthapp.domain.service

import com.eunio.healthapp.domain.manager.SettingsManager
import com.eunio.healthapp.domain.model.settings.*
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.domain.util.UserNotificationService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Tests for EnhancedSettingsManager to verify error handling and fallback mechanisms.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EnhancedSettingsManagerTest {
    
    private class MockSettingsManager(
        private val shouldFail: Boolean = false,
        private val failureMessage: String = "Mock failure"
    ) : SettingsManager {
        
        private val defaultSettings = UserSettings.createDefault("test_user")
        
        override suspend fun getUserSettings(): Result<UserSettings> {
            return if (shouldFail) {
                Result.Error(com.eunio.healthapp.domain.error.AppError.UnknownError(failureMessage))
            } else {
                Result.Success(defaultSettings)
            }
        }
        
        override fun observeSettingsChanges() = flowOf(defaultSettings)
        
        override suspend fun updateUnitPreferences(preferences: UnitPreferences): Result<Unit> {
            return if (shouldFail) Result.Error(com.eunio.healthapp.domain.error.AppError.UnknownError(failureMessage)) else Result.Success(Unit)
        }
        
        override suspend fun updateNotificationPreferences(preferences: NotificationPreferences): Result<Unit> {
            return if (shouldFail) Result.Error(com.eunio.healthapp.domain.error.AppError.UnknownError(failureMessage)) else Result.Success(Unit)
        }
        
        override suspend fun updateCyclePreferences(preferences: CyclePreferences): Result<Unit> {
            return if (shouldFail) Result.Error(com.eunio.healthapp.domain.error.AppError.UnknownError(failureMessage)) else Result.Success(Unit)
        }
        
        override suspend fun updatePrivacyPreferences(preferences: PrivacyPreferences): Result<Unit> {
            return if (shouldFail) Result.Error(com.eunio.healthapp.domain.error.AppError.UnknownError(failureMessage)) else Result.Success(Unit)
        }
        
        override suspend fun updateDisplayPreferences(preferences: DisplayPreferences): Result<Unit> {
            return if (shouldFail) Result.Error(com.eunio.healthapp.domain.error.AppError.UnknownError(failureMessage)) else Result.Success(Unit)
        }
        
        override suspend fun updateSyncPreferences(preferences: SyncPreferences): Result<Unit> {
            return if (shouldFail) Result.Error(com.eunio.healthapp.domain.error.AppError.UnknownError(failureMessage)) else Result.Success(Unit)
        }
        
        override suspend fun updateSettings(updateFunction: (UserSettings) -> UserSettings): Result<UserSettings> {
            return if (shouldFail) {
                Result.Error(com.eunio.healthapp.domain.error.AppError.UnknownError(failureMessage))
            } else {
                Result.Success(updateFunction(defaultSettings))
            }
        }
        
        override suspend fun validateSettings(settings: UserSettings): Result<Unit> {
            return if (shouldFail) Result.Error(com.eunio.healthapp.domain.error.AppError.UnknownError(failureMessage)) else Result.Success(Unit)
        }
        
        override suspend fun resetToDefaults(preserveUnitPreferences: Boolean): Result<UserSettings> {
            return if (shouldFail) Result.Error(com.eunio.healthapp.domain.error.AppError.UnknownError(failureMessage)) else Result.Success(defaultSettings)
        }
        
        override suspend fun syncSettings(): Result<Unit> {
            return if (shouldFail) Result.Error(com.eunio.healthapp.domain.error.AppError.UnknownError(failureMessage)) else Result.Success(Unit)
        }
        
        override suspend fun exportSettings(): Result<String> {
            return if (shouldFail) Result.Error(com.eunio.healthapp.domain.error.AppError.UnknownError(failureMessage)) else Result.Success("{}")
        }
        
        override suspend fun importSettings(backupData: String): Result<Unit> {
            return if (shouldFail) Result.Error(com.eunio.healthapp.domain.error.AppError.UnknownError(failureMessage)) else Result.Success(Unit)
        }
        
        override suspend fun isSynced(): Boolean = !shouldFail
        
        override fun observeSyncStatus() = flowOf(!shouldFail)
    }
    
    @Test
    fun `getUserSettings should return success when primary manager works`() = runTest {
        val primaryManager = MockSettingsManager(shouldFail = false)
        val enhancedManager = EnhancedSettingsManager(primaryManager)
        
        val result = enhancedManager.getUserSettings()
        
        assertIs<Result.Success<UserSettings>>(result)
        assertEquals("test_user", result.data.userId)
    }
    
    @Test
    fun `getUserSettings should use fallback when primary manager fails`() = runTest {
        val primaryManager = MockSettingsManager(shouldFail = true)
        val fallbackManager = MockSettingsManager(shouldFail = false)
        val enhancedManager = EnhancedSettingsManager(primaryManager, fallbackManager)
        
        val result = enhancedManager.getUserSettings()
        
        assertIs<Result.Success<UserSettings>>(result)
        assertEquals("test_user", result.data.userId)
    }
    
    @Test
    fun `getUserSettings should create default settings when both managers fail`() = runTest {
        val primaryManager = MockSettingsManager(shouldFail = true)
        val fallbackManager = MockSettingsManager(shouldFail = true)
        val enhancedManager = EnhancedSettingsManager(primaryManager, fallbackManager)
        
        val result = enhancedManager.getUserSettings()
        
        assertIs<Result.Success<UserSettings>>(result)
        assertEquals("fallback_user_id", result.data.userId)
    }
    
    @Test
    fun `updateUnitPreferences should show warning when using fallback`() = runTest {
        val primaryManager = MockSettingsManager(shouldFail = true)
        val fallbackManager = MockSettingsManager(shouldFail = false)
        val enhancedManager = EnhancedSettingsManager(primaryManager, fallbackManager)
        
        val result = enhancedManager.updateUnitPreferences(UnitPreferences())
        
        assertIs<Result.Success<Unit>>(result)
        
        testScheduler.advanceUntilIdle()
        
        val message = UserNotificationService.messages.first()
        assertTrue(message.text.contains("updated locally only"))
    }
    
    @Test
    fun `updateNotificationPreferences should show appropriate warning`() = runTest {
        val primaryManager = MockSettingsManager(shouldFail = true)
        val enhancedManager = EnhancedSettingsManager(primaryManager)
        
        val result = enhancedManager.updateNotificationPreferences(NotificationPreferences())
        
        assertIs<Result.Success<Unit>>(result)
        
        val message = UserNotificationService.messages.first()
        assertTrue(message.text.contains("Some notifications may not work"))
    }
    
    @Test
    fun `syncSettings should show warning when fallback is used`() = runTest {
        val primaryManager = MockSettingsManager(shouldFail = true)
        val enhancedManager = EnhancedSettingsManager(primaryManager)
        
        val result = enhancedManager.syncSettings()
        
        assertIs<Result.Success<Unit>>(result)
        
        val message = UserNotificationService.messages.first()
        assertTrue(message.text.contains("saved locally"))
        assertTrue(message.text.contains("will sync when connection is restored"))
    }
    
    @Test
    fun `isHealthy should return false when primary manager fails`() = runTest {
        val primaryManager = MockSettingsManager(shouldFail = true)
        val enhancedManager = EnhancedSettingsManager(primaryManager)
        
        val isHealthy = enhancedManager.isHealthy()
        
        assertEquals(false, isHealthy)
    }
    
    @Test
    fun `isHealthy should return true when primary manager works`() = runTest {
        val primaryManager = MockSettingsManager(shouldFail = false)
        val enhancedManager = EnhancedSettingsManager(primaryManager)
        
        val isHealthy = enhancedManager.isHealthy()
        
        assertEquals(true, isHealthy)
    }
    
    @Test
    fun `getServiceStatus should include detailed information`() = runTest {
        val primaryManager = MockSettingsManager(shouldFail = false)
        val fallbackManager = MockSettingsManager(shouldFail = false)
        val enhancedManager = EnhancedSettingsManager(primaryManager, fallbackManager)
        
        val status = enhancedManager.getServiceStatus()
        
        assertEquals("SettingsManager", status.serviceName)
        assertEquals(true, status.isHealthy)
        assertTrue(status.details.containsKey("primaryManagerAvailable"))
        assertTrue(status.details.containsKey("fallbackManagerAvailable"))
        assertTrue(status.details.containsKey("isDegraded"))
        assertTrue(status.details.containsKey("degradedOperations"))
    }
    
    @Test
    fun `isSynced should return false when primary manager fails`() = runTest {
        val primaryManager = MockSettingsManager(shouldFail = true)
        val enhancedManager = EnhancedSettingsManager(primaryManager)
        
        val isSynced = enhancedManager.isSynced()
        
        assertEquals(false, isSynced)
    }
}