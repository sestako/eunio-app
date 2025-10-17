package com.eunio.healthapp.presentation.viewmodel

import com.eunio.healthapp.domain.manager.SettingsManager
import com.eunio.healthapp.domain.model.settings.SyncPreferences
import com.eunio.healthapp.domain.model.settings.UserSettings
import com.eunio.healthapp.domain.repository.SettingsRepository
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.presentation.state.LoadingState
import com.eunio.healthapp.presentation.state.SyncPreferencesUiState
// SyncStatus will be referenced with full path
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlinx.datetime.Clock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.test.*

/**
 * Unit tests for SyncPreferencesViewModel.
 * Tests sync settings management, manual sync operations, and conflict resolution.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SyncPreferencesViewModelTest {
    
    private lateinit var mockSettingsManager: MockSettingsManager
    private lateinit var mockSettingsRepository: MockSettingsRepository
    private lateinit var viewModel: SyncPreferencesViewModel
    private val testDispatcher = StandardTestDispatcher()
    
    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockSettingsManager = MockSettingsManager()
        mockSettingsRepository = MockSettingsRepository()
        viewModel = SyncPreferencesViewModel(mockSettingsManager, mockSettingsRepository)
    }
    
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun uiState_initialValues_areCorrect() {
        val uiState = SyncPreferencesUiState()
        
        assertEquals(SyncPreferences.default(), uiState.preferences)
        assertEquals(LoadingState.Idle, uiState.loadingState)
        assertFalse(uiState.isUpdating)
        assertFalse(uiState.isSyncing)
        assertFalse(uiState.isResolvingConflict)
        assertEquals(0f, uiState.syncProgress)
        assertEquals(com.eunio.healthapp.presentation.state.SyncStatus.IDLE, uiState.syncStatus)
        assertNull(uiState.conflictData)
        assertNull(uiState.storageUsage)
    }
    
    @Test
    fun uiState_isLoading_trueWhenLoadingState() {
        val uiState = SyncPreferencesUiState(
            loadingState = LoadingState.Loading
        )
        
        assertTrue(uiState.isLoading)
    }
    
    @Test
    fun uiState_isEnabled_falseWhenUpdating() {
        val uiState = SyncPreferencesUiState(
            isUpdating = true
        )
        
        assertFalse(uiState.isEnabled)
    }
    
    @Test
    fun uiState_isEnabled_falseWhenSyncing() {
        val uiState = SyncPreferencesUiState(
            isSyncing = true
        )
        
        assertFalse(uiState.isEnabled)
    }
    
    @Test
    fun uiState_isEnabled_falseWhenResolvingConflict() {
        val uiState = SyncPreferencesUiState(
            isResolvingConflict = true
        )
        
        assertFalse(uiState.isEnabled)
    }
    
    @Test
    fun uiState_isSyncInProgress_trueWhenSyncingWithProgress() {
        val uiState = SyncPreferencesUiState(
            isSyncing = true,
            syncProgress = 0.5f
        )
        
        assertTrue(uiState.isSyncInProgress)
    }
    
    @Test
    fun uiState_isSyncComplete_trueWhenSyncingWithFullProgress() {
        val uiState = SyncPreferencesUiState(
            isSyncing = true,
            syncProgress = 1f
        )
        
        assertTrue(uiState.isSyncComplete)
    }
    
    @Test
    fun uiState_getSyncStatusDescription_returnsCorrectDescriptions() {
        val idleState = SyncPreferencesUiState(syncStatus = com.eunio.healthapp.presentation.state.SyncStatus.IDLE)
        assertEquals("Ready to sync", idleState.getSyncStatusDescription())
        
        val syncingState = SyncPreferencesUiState(syncStatus = com.eunio.healthapp.presentation.state.SyncStatus.SYNCING)
        assertEquals("Syncing data...", syncingState.getSyncStatusDescription())
        
        val successState = SyncPreferencesUiState(syncStatus = com.eunio.healthapp.presentation.state.SyncStatus.SUCCESS)
        assertEquals("Sync completed successfully", successState.getSyncStatusDescription())
        
        val failedState = SyncPreferencesUiState(syncStatus = com.eunio.healthapp.presentation.state.SyncStatus.FAILED)
        assertEquals("Sync failed", failedState.getSyncStatusDescription())
        
        val conflictState = SyncPreferencesUiState(syncStatus = com.eunio.healthapp.presentation.state.SyncStatus.CONFLICT)
        assertEquals("Sync conflict detected", conflictState.getSyncStatusDescription())
        
        val offlineState = SyncPreferencesUiState(syncStatus = com.eunio.healthapp.presentation.state.SyncStatus.OFFLINE)
        assertEquals("Offline - will sync when connected", offlineState.getSyncStatusDescription())
    }
    
    @Test
    fun uiState_getSyncProgressPercentage_worksCorrectly() {
        val uiState = SyncPreferencesUiState(
            syncProgress = 0.75f
        )
        
        assertEquals(75, uiState.getSyncProgressPercentage())
        
        val completeState = SyncPreferencesUiState(
            syncProgress = 1f
        )
        
        assertEquals(100, completeState.getSyncProgressPercentage())
    }
    
    @Test
    fun loadSyncPreferences_success_updatesUiState() = runTest {
        // Given
        val testPreferences = SyncPreferences(
            autoSyncEnabled = false,
            wifiOnlySync = true,
            cloudBackupEnabled = true,
            lastSyncTime = Clock.System.now()
        )
        val testSettings = createTestUserSettings().copy(syncPreferences = testPreferences)
        mockSettingsManager.userSettingsResult = Result.success(testSettings)
        
        // When
        viewModel = SyncPreferencesViewModel(mockSettingsManager, mockSettingsRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val uiState = viewModel.uiState.value
        assertEquals(testPreferences, uiState.preferences)
        assertEquals(testPreferences.lastSyncTime, uiState.lastSyncTime)
        assertTrue(uiState.loadingState is LoadingState.Success<*>)
    }
    
    @Test
    fun loadSyncPreferences_failure_updatesUiStateWithError() = runTest {
        // Given
        mockSettingsManager.userSettingsResult = com.eunio.healthapp.domain.util.Result.Error(
            com.eunio.healthapp.domain.error.AppError.UnknownError("Load failed")
        )
        mockSettingsManager.shouldEmitFromFlow = false // Prevent flow from overriding error state
        
        // When
        viewModel = SyncPreferencesViewModel(mockSettingsManager, mockSettingsRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val uiState = viewModel.uiState.value
        assertTrue(uiState.loadingState is LoadingState.Error)
        assertEquals("Failed to load sync preferences: Load failed", (uiState.loadingState as LoadingState.Error).message)
    }
    
    @Test
    fun toggleAutoSync_success_updatesPreferences() = runTest {
        // Given
        val initialPreferences = SyncPreferences(autoSyncEnabled = false)
        mockSettingsManager.userSettingsResult = Result.success(
            createTestUserSettings().copy(syncPreferences = initialPreferences)
        )
        mockSettingsManager.updateSyncPreferencesResult = Result.success(Unit)
        
        viewModel = SyncPreferencesViewModel(mockSettingsManager, mockSettingsRepository)
        
        // When
        viewModel.toggleAutoSync(true)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        assertTrue(mockSettingsManager.updateSyncPreferencesCalled)
        val updatedPreferences = mockSettingsManager.lastUpdatedSyncPreferences
        assertTrue(updatedPreferences?.autoSyncEnabled == true)
    }
    
    @Test
    fun toggleWifiOnlySync_success_updatesPreferences() = runTest {
        // Given
        val initialPreferences = SyncPreferences(wifiOnlySync = false)
        mockSettingsManager.userSettingsResult = Result.success(
            createTestUserSettings().copy(syncPreferences = initialPreferences)
        )
        mockSettingsManager.updateSyncPreferencesResult = Result.success(Unit)
        
        viewModel = SyncPreferencesViewModel(mockSettingsManager, mockSettingsRepository)
        
        // When
        viewModel.toggleWifiOnlySync(true)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        assertTrue(mockSettingsManager.updateSyncPreferencesCalled)
        val updatedPreferences = mockSettingsManager.lastUpdatedSyncPreferences
        assertTrue(updatedPreferences?.wifiOnlySync == true)
    }
    
    @Test
    fun toggleCloudBackup_success_updatesPreferences() = runTest {
        // Given
        val initialPreferences = SyncPreferences(cloudBackupEnabled = false)
        mockSettingsManager.userSettingsResult = Result.success(
            createTestUserSettings().copy(syncPreferences = initialPreferences)
        )
        mockSettingsManager.updateSyncPreferencesResult = Result.success(Unit)
        
        viewModel = SyncPreferencesViewModel(mockSettingsManager, mockSettingsRepository)
        
        // When
        viewModel.toggleCloudBackup(true)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        assertTrue(mockSettingsManager.updateSyncPreferencesCalled)
        val updatedPreferences = mockSettingsManager.lastUpdatedSyncPreferences
        assertTrue(updatedPreferences?.cloudBackupEnabled == true)
    }
    
    @Test
    fun setDataConservativeMode_updatesPreferences() = runTest {
        // Given
        mockSettingsManager.userSettingsResult = Result.success(createTestUserSettings())
        mockSettingsManager.updateSyncPreferencesResult = Result.success(Unit)
        
        viewModel = SyncPreferencesViewModel(mockSettingsManager, mockSettingsRepository)
        
        // When
        viewModel.setDataConservativeMode()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        assertTrue(mockSettingsManager.updateSyncPreferencesCalled)
        val updatedPreferences = mockSettingsManager.lastUpdatedSyncPreferences
        assertEquals(SyncPreferences.dataConservative(), updatedPreferences)
    }
    
    @Test
    fun setOfflineFirstMode_updatesPreferences() = runTest {
        // Given
        mockSettingsManager.userSettingsResult = Result.success(createTestUserSettings())
        mockSettingsManager.updateSyncPreferencesResult = Result.success(Unit)
        
        viewModel = SyncPreferencesViewModel(mockSettingsManager, mockSettingsRepository)
        
        // When
        viewModel.setOfflineFirstMode()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        assertTrue(mockSettingsManager.updateSyncPreferencesCalled)
        val updatedPreferences = mockSettingsManager.lastUpdatedSyncPreferences
        assertEquals(SyncPreferences.offlineFirst(), updatedPreferences)
    }
    
    @Test
    fun setMaxAvailabilityMode_updatesPreferences() = runTest {
        // Given
        mockSettingsManager.userSettingsResult = Result.success(createTestUserSettings())
        mockSettingsManager.updateSyncPreferencesResult = Result.success(Unit)
        
        viewModel = SyncPreferencesViewModel(mockSettingsManager, mockSettingsRepository)
        
        // When
        viewModel.setMaxAvailabilityMode()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        assertTrue(mockSettingsManager.updateSyncPreferencesCalled)
        val updatedPreferences = mockSettingsManager.lastUpdatedSyncPreferences
        assertEquals(SyncPreferences.maxAvailability(), updatedPreferences)
    }
    
    @Test
    fun triggerManualSync_success_updatesUiStateAndCompletes() = runTest {
        // Given
        mockSettingsManager.userSettingsResult = Result.success(createTestUserSettings())
        mockSettingsManager.syncSettingsResult = Result.success(Unit)
        
        viewModel = SyncPreferencesViewModel(mockSettingsManager, mockSettingsRepository)
        
        // When
        viewModel.triggerManualSync()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        assertTrue(mockSettingsManager.syncSettingsCalled)
        
        // Verify sync completed
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isSyncing)
        assertEquals(com.eunio.healthapp.presentation.state.SyncStatus.IDLE, finalState.syncStatus) // Should reset after completion
    }
    
    @Test
    fun triggerManualSync_failure_updatesUiStateWithError() = runTest {
        // Given
        mockSettingsManager.userSettingsResult = Result.success(createTestUserSettings())
        mockSettingsManager.syncSettingsResult = com.eunio.healthapp.domain.util.Result.Error(
            com.eunio.healthapp.domain.error.AppError.UnknownError("Sync failed")
        )
        
        viewModel = SyncPreferencesViewModel(mockSettingsManager, mockSettingsRepository)
        
        // When
        viewModel.triggerManualSync()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        assertTrue(mockSettingsManager.syncSettingsCalled)
        
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isSyncing)
        assertEquals(com.eunio.healthapp.presentation.state.SyncStatus.FAILED, finalState.syncStatus)
    }
    
    @Test
    fun triggerManualSync_conflictError_setsConflictState() = runTest {
        // Given
        mockSettingsManager.userSettingsResult = Result.success(createTestUserSettings())
        mockSettingsManager.syncSettingsResult = com.eunio.healthapp.domain.util.Result.Error(
            com.eunio.healthapp.domain.error.AppError.UnknownError("Sync conflict detected")
        )
        
        viewModel = SyncPreferencesViewModel(mockSettingsManager, mockSettingsRepository)
        
        // When
        viewModel.triggerManualSync()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val finalState = viewModel.uiState.value
        assertEquals(com.eunio.healthapp.presentation.state.SyncStatus.CONFLICT, finalState.syncStatus)
        assertNotNull(finalState.conflictData)
    }
    
    @Test
    fun resolveConflictWithLocal_success_clearsConflict() = runTest {
        // Given
        mockSettingsManager.userSettingsResult = Result.success(createTestUserSettings())
        
        viewModel = SyncPreferencesViewModel(mockSettingsManager, mockSettingsRepository)
        
        // When
        viewModel.resolveConflictWithLocal()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isResolvingConflict)
        assertNull(finalState.conflictData)
        assertEquals(com.eunio.healthapp.presentation.state.SyncStatus.SUCCESS, finalState.syncStatus)
    }
    
    @Test
    fun resolveConflictWithRemote_success_clearsConflict() = runTest {
        // Given
        mockSettingsManager.userSettingsResult = Result.success(createTestUserSettings())
        
        viewModel = SyncPreferencesViewModel(mockSettingsManager, mockSettingsRepository)
        
        // When
        viewModel.resolveConflictWithRemote()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isResolvingConflict)
        assertNull(finalState.conflictData)
        assertEquals(com.eunio.healthapp.presentation.state.SyncStatus.SUCCESS, finalState.syncStatus)
    }
    
    @Test
    fun dismissConflict_clearsConflictData() = runTest {
        // Given
        mockSettingsManager.userSettingsResult = Result.success(createTestUserSettings())
        
        viewModel = SyncPreferencesViewModel(mockSettingsManager, mockSettingsRepository)
        
        // When
        viewModel.dismissConflict()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val finalState = viewModel.uiState.value
        assertNull(finalState.conflictData)
        assertEquals(com.eunio.healthapp.presentation.state.SyncStatus.IDLE, finalState.syncStatus)
    }
    
    @Test
    fun getSyncModeDescription_returnsCorrectDescriptions() = runTest {
        // Given
        mockSettingsManager.userSettingsResult = Result.success(createTestUserSettings())
        
        viewModel = SyncPreferencesViewModel(mockSettingsManager, mockSettingsRepository)
        
        // Test different sync modes
        val offlineOnly = SyncPreferences(autoSyncEnabled = false, cloudBackupEnabled = false)
        assertEquals("Offline Only", getSyncModeForPreferences(offlineOnly))
        
        val wifiOnly = SyncPreferences(autoSyncEnabled = true, wifiOnlySync = true, cloudBackupEnabled = true)
        assertEquals("WiFi Sync Only", getSyncModeForPreferences(wifiOnly))
        
        val fullSync = SyncPreferences(autoSyncEnabled = true, cloudBackupEnabled = true, wifiOnlySync = false)
        assertEquals("Full Sync Enabled", getSyncModeForPreferences(fullSync))
        
        val backupOnly = SyncPreferences(autoSyncEnabled = false, cloudBackupEnabled = true)
        assertEquals("Backup Only", getSyncModeForPreferences(backupOnly))
    }
    
    @Test
    fun getSyncRecommendations_returnsAppropriateRecommendations() = runTest {
        // Given
        val preferences = SyncPreferences(
            autoSyncEnabled = false,
            cloudBackupEnabled = false,
            wifiOnlySync = false,
            lastSyncTime = null
        )
        mockSettingsManager.userSettingsResult = Result.success(
            createTestUserSettings().copy(syncPreferences = preferences)
        )
        
        viewModel = SyncPreferencesViewModel(mockSettingsManager, mockSettingsRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When
        val recommendations = viewModel.getSyncRecommendations()
        
        // Then
        assertTrue(recommendations.contains("Enable auto-sync for seamless data synchronization"))
        assertTrue(recommendations.contains("Enable cloud backup to protect your data"))
        assertTrue(recommendations.contains("Perform your first sync to backup your data"))
    }
    
    @Test
    fun needsAttention_returnsTrueWhenSyncDisabled() = runTest {
        // Given
        val preferences = SyncPreferences(autoSyncEnabled = false, cloudBackupEnabled = false)
        mockSettingsManager.userSettingsResult = Result.success(
            createTestUserSettings().copy(syncPreferences = preferences)
        )
        
        viewModel = SyncPreferencesViewModel(mockSettingsManager, mockSettingsRepository)
        
        // When
        val needsAttention = viewModel.needsAttention()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        assertTrue(needsAttention)
    }
    
    @Test
    fun needsAttention_returnsTrueWhenSyncFailed() = runTest {
        // Given
        mockSettingsManager.userSettingsResult = Result.success(createTestUserSettings())
        
        viewModel = SyncPreferencesViewModel(mockSettingsManager, mockSettingsRepository)
        
        // Simulate sync failure
        mockSettingsManager.syncSettingsResult = com.eunio.healthapp.domain.util.Result.Error(
            com.eunio.healthapp.domain.error.AppError.UnknownError("Sync failed")
        )
        viewModel.triggerManualSync()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When
        val needsAttention = viewModel.needsAttention()
        
        // Then
        assertTrue(needsAttention)
    }
    
    @Test
    fun needsAttention_returnsFalseWhenSyncHealthy() = runTest {
        // Given
        val preferences = SyncPreferences(
            autoSyncEnabled = true,
            cloudBackupEnabled = true,
            lastSyncTime = Clock.System.now()
        )
        mockSettingsManager.userSettingsResult = Result.success(
            createTestUserSettings().copy(syncPreferences = preferences)
        )
        
        viewModel = SyncPreferencesViewModel(mockSettingsManager, mockSettingsRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When
        val needsAttention = viewModel.needsAttention()
        
        // Then
        assertFalse(needsAttention)
    }
    
    // Helper methods
    
    private fun createTestUserSettings(): UserSettings {
        return UserSettings(
            userId = "test_user",
            unitPreferences = com.eunio.healthapp.domain.model.settings.UnitPreferences.default(),
            notificationPreferences = com.eunio.healthapp.domain.model.settings.NotificationPreferences.default(),
            cyclePreferences = com.eunio.healthapp.domain.model.settings.CyclePreferences.default(),
            privacyPreferences = com.eunio.healthapp.domain.model.settings.PrivacyPreferences.default(),
            displayPreferences = com.eunio.healthapp.domain.model.settings.DisplayPreferences.default(),
            syncPreferences = SyncPreferences.default(),
            lastModified = Clock.System.now(),
            syncStatus = com.eunio.healthapp.domain.model.SyncStatus.PENDING
        )
    }
    
    private fun getSyncModeForPreferences(preferences: SyncPreferences): String {
        return when {
            !preferences.autoSyncEnabled && !preferences.cloudBackupEnabled -> "Offline Only"
            preferences.wifiOnlySync && preferences.autoSyncEnabled -> "WiFi Sync Only"
            preferences.autoSyncEnabled && preferences.cloudBackupEnabled -> "Full Sync Enabled"
            preferences.cloudBackupEnabled && !preferences.autoSyncEnabled -> "Backup Only"
            else -> "Custom Configuration"
        }
    }
}

// Mock implementations

class MockSettingsManager : SettingsManager {
    var userSettingsResult: Result<UserSettings> = Result.success(createDefaultUserSettings())
        set(value) {
            field = value
            // Update the flow when the result changes (only for success)
            if (value is Result.Success) {
                settingsFlow.value = value.data
            }
            // For error cases, we don't update the flow since the ViewModel
            // will handle the error through getUserSettings() directly
        }
    var updateSyncPreferencesResult: Result<Unit> = Result.success(Unit)
    var syncSettingsResult: Result<Unit> = Result.success(Unit)
    
    var updateSyncPreferencesCalled = false
    var syncSettingsCalled = false
    var lastUpdatedSyncPreferences: SyncPreferences? = null
    var shouldEmitFromFlow = true
    
    private val settingsFlow = MutableStateFlow(createDefaultUserSettings())
    
    override suspend fun getUserSettings(): Result<UserSettings> = userSettingsResult
    
    override fun observeSettingsChanges() = if (shouldEmitFromFlow) settingsFlow else flowOf()
    
    override suspend fun updateUnitPreferences(preferences: com.eunio.healthapp.domain.model.settings.UnitPreferences): Result<Unit> = Result.success(Unit)
    
    override suspend fun updateNotificationPreferences(preferences: com.eunio.healthapp.domain.model.settings.NotificationPreferences): Result<Unit> = Result.success(Unit)
    
    override suspend fun updateCyclePreferences(preferences: com.eunio.healthapp.domain.model.settings.CyclePreferences): Result<Unit> = Result.success(Unit)
    
    override suspend fun updatePrivacyPreferences(preferences: com.eunio.healthapp.domain.model.settings.PrivacyPreferences): Result<Unit> = Result.success(Unit)
    
    override suspend fun updateDisplayPreferences(preferences: com.eunio.healthapp.domain.model.settings.DisplayPreferences): Result<Unit> = Result.success(Unit)
    
    override suspend fun updateSyncPreferences(preferences: SyncPreferences): Result<Unit> {
        updateSyncPreferencesCalled = true
        lastUpdatedSyncPreferences = preferences
        return updateSyncPreferencesResult
    }
    
    override suspend fun updateSettings(updateFunction: (UserSettings) -> UserSettings): Result<UserSettings> = Result.success(createDefaultUserSettings())
    
    override suspend fun validateSettings(settings: UserSettings): Result<Unit> = Result.success(Unit)
    
    override suspend fun resetToDefaults(preserveUnitPreferences: Boolean): Result<UserSettings> = Result.success(createDefaultUserSettings())
    
    override suspend fun syncSettings(): Result<Unit> {
        syncSettingsCalled = true
        return syncSettingsResult
    }
    
    override suspend fun exportSettings(): Result<String> = Result.success("{}")
    
    override suspend fun importSettings(backupData: String): Result<Unit> = Result.success(Unit)
    
    override suspend fun isSynced(): Boolean = true
    
    override fun observeSyncStatus() = flowOf(true)
    
    private fun createDefaultUserSettings(): UserSettings {
        return UserSettings(
            userId = "test_user",
            unitPreferences = com.eunio.healthapp.domain.model.settings.UnitPreferences.default(),
            notificationPreferences = com.eunio.healthapp.domain.model.settings.NotificationPreferences.default(),
            cyclePreferences = com.eunio.healthapp.domain.model.settings.CyclePreferences.default(),
            privacyPreferences = com.eunio.healthapp.domain.model.settings.PrivacyPreferences.default(),
            displayPreferences = com.eunio.healthapp.domain.model.settings.DisplayPreferences.default(),
            syncPreferences = SyncPreferences.default(),
            lastModified = Clock.System.now(),
            syncStatus = com.eunio.healthapp.domain.model.SyncStatus.PENDING
        )
    }
}

class MockSettingsRepository : SettingsRepository {
    // Core settings methods - grouped together for clarity
    override suspend fun getUserSettings(): Result<UserSettings?> = Result.success(null)
    override suspend fun getUserSettings(userId: String): Result<UserSettings?> = Result.success(null)
    override suspend fun saveUserSettings(settings: UserSettings): Result<Unit> = Result.success(Unit)
    override suspend fun updateUserSettings(userId: String, updateFunction: (UserSettings) -> UserSettings): Result<UserSettings> = Result.success(createDefaultUserSettings())
    
    // Sync methods - grouped together
    override suspend fun syncSettings(): Result<Unit> = Result.success(Unit)
    override suspend fun syncSettings(userId: String): Result<Unit> = Result.success(Unit)
    override suspend fun markAsSynced(userId: String): Result<Unit> = Result.success(Unit)
    override suspend fun markAsSyncFailed(userId: String): Result<Unit> = Result.success(Unit)
    override suspend fun getPendingSyncSettings(): Result<List<UserSettings>> = Result.success(emptyList())
    override fun observeSyncStatus() = flowOf(true)
    
    // Conflict resolution and backup methods
    override suspend fun resolveSettingsConflict(userId: String, localSettings: UserSettings, remoteSettings: UserSettings): Result<UserSettings> = Result.success(localSettings)
    override suspend fun backupUserSettings(userId: String): Result<String> = Result.success("{}")
    override suspend fun restoreUserSettings(userId: String, backupData: String): Result<Unit> = Result.success(Unit)
    override suspend fun exportUserData(userId: String): Result<String> = Result.success("{}")
    
    // Utility methods
    override suspend fun deleteUserSettings(userId: String): Result<Unit> = Result.success(Unit)
    override suspend fun clearLocalSettings(): Result<Unit> = Result.success(Unit)
    override suspend fun resetToDefaults(userId: String, locale: String?): Result<UserSettings> = Result.success(createDefaultUserSettings())
    override suspend fun settingsExist(userId: String): Result<Boolean> = Result.success(true)
    override suspend fun getLastModifiedTimestamp(userId: String): Result<Long?> = Result.success(null)
    override suspend fun validateSettings(settings: UserSettings): Result<Unit> = Result.success(Unit)
    override suspend fun migrateSettings(settings: UserSettings, targetVersion: Int): Result<UserSettings> = Result.success(settings)
    
    // Observation methods
    override fun observeUserSettings(userId: String) = flowOf<UserSettings?>(null)
    
    private fun createDefaultUserSettings(): UserSettings {
        return UserSettings(
            userId = "test_user",
            unitPreferences = com.eunio.healthapp.domain.model.settings.UnitPreferences.default(),
            notificationPreferences = com.eunio.healthapp.domain.model.settings.NotificationPreferences.default(),
            cyclePreferences = com.eunio.healthapp.domain.model.settings.CyclePreferences.default(),
            privacyPreferences = com.eunio.healthapp.domain.model.settings.PrivacyPreferences.default(),
            displayPreferences = com.eunio.healthapp.domain.model.settings.DisplayPreferences.default(),
            syncPreferences = SyncPreferences.default(),
            lastModified = Clock.System.now(),
            syncStatus = com.eunio.healthapp.domain.model.SyncStatus.PENDING
        )
    }
}