package com.eunio.healthapp.presentation.viewmodel

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.settings.PrivacyPreferences
import com.eunio.healthapp.domain.repository.SettingsRepository
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.presentation.state.LoadingState
import com.eunio.healthapp.presentation.state.PrivacyPreferencesUiState
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Fake implementation of SettingsRepository for testing privacy features
 */
class FakeSettingsRepositoryForPrivacy : SettingsRepository {
    private var shouldFailOnExport = false
    private var shouldFailOnDelete = false
    
    fun setShouldFailOnExport(shouldFail: Boolean) {
        shouldFailOnExport = shouldFail
    }
    
    fun setShouldFailOnDelete(shouldFail: Boolean) {
        shouldFailOnDelete = shouldFail
    }
    
    override suspend fun exportUserData(userId: String): com.eunio.healthapp.domain.util.Result<String> {
        return if (shouldFailOnExport) {
            com.eunio.healthapp.domain.util.Result.error(AppError.ValidationError("Export failed"))
        } else {
            val exportData = """
                {
                    "userId": "$userId",
                    "settings": {
                        "unitPreferences": {"temperatureUnit": "CELSIUS", "weightUnit": "KILOGRAMS"},
                        "notificationPreferences": {"globalNotificationsEnabled": true},
                        "cyclePreferences": {"averageCycleLength": 28},
                        "privacyPreferences": {"dataSharingEnabled": false},
                        "displayPreferences": {"textSizeScale": 1.0},
                        "syncPreferences": {"autoSyncEnabled": true}
                    },
                    "exportDate": "2024-01-15T10:30:00Z",
                    "version": 1
                }
            """.trimIndent()
            com.eunio.healthapp.domain.util.Result.success(exportData)
        }
    }
    
    override suspend fun deleteUserSettings(userId: String): com.eunio.healthapp.domain.util.Result<Unit> {
        return if (shouldFailOnDelete) {
            com.eunio.healthapp.domain.util.Result.error(AppError.ValidationError("Delete failed"))
        } else {
            com.eunio.healthapp.domain.util.Result.success(Unit)
        }
    }
    
    // Stub implementations for other methods (not used in privacy tests)
    override suspend fun getUserSettings() = com.eunio.healthapp.domain.util.Result.success(null)
    override suspend fun getUserSettings(userId: String) = com.eunio.healthapp.domain.util.Result.success(null)
    override suspend fun saveUserSettings(settings: com.eunio.healthapp.domain.model.settings.UserSettings) = com.eunio.healthapp.domain.util.Result.success(Unit)
    override suspend fun updateUserSettings(userId: String, updateFunction: (com.eunio.healthapp.domain.model.settings.UserSettings) -> com.eunio.healthapp.domain.model.settings.UserSettings) = com.eunio.healthapp.domain.util.Result.success(com.eunio.healthapp.domain.model.settings.UserSettings.createDefault(userId))
    override suspend fun syncSettings() = com.eunio.healthapp.domain.util.Result.success(Unit)
    override suspend fun syncSettings(userId: String) = com.eunio.healthapp.domain.util.Result.success(Unit)
    override suspend fun resolveSettingsConflict(userId: String, localSettings: com.eunio.healthapp.domain.model.settings.UserSettings, remoteSettings: com.eunio.healthapp.domain.model.settings.UserSettings) = com.eunio.healthapp.domain.util.Result.success(localSettings)
    override suspend fun backupUserSettings(userId: String) = com.eunio.healthapp.domain.util.Result.success("{}")
    override suspend fun restoreUserSettings(userId: String, backupData: String) = com.eunio.healthapp.domain.util.Result.success(Unit)
    override suspend fun clearLocalSettings() = com.eunio.healthapp.domain.util.Result.success(Unit)
    override suspend fun resetToDefaults(userId: String, locale: String?) = com.eunio.healthapp.domain.util.Result.success(com.eunio.healthapp.domain.model.settings.UserSettings.createDefault(userId))
    override fun observeUserSettings(userId: String) = kotlinx.coroutines.flow.flowOf(null)
    override fun observeSyncStatus() = kotlinx.coroutines.flow.flowOf(true)
    override suspend fun getPendingSyncSettings() = com.eunio.healthapp.domain.util.Result.success(emptyList<com.eunio.healthapp.domain.model.settings.UserSettings>())
    override suspend fun markAsSynced(userId: String) = com.eunio.healthapp.domain.util.Result.success(Unit)
    override suspend fun markAsSyncFailed(userId: String) = com.eunio.healthapp.domain.util.Result.success(Unit)
    override suspend fun settingsExist(userId: String) = com.eunio.healthapp.domain.util.Result.success(true)
    override suspend fun getLastModifiedTimestamp(userId: String) = com.eunio.healthapp.domain.util.Result.success(System.currentTimeMillis())
    override suspend fun validateSettings(settings: com.eunio.healthapp.domain.model.settings.UserSettings) = com.eunio.healthapp.domain.util.Result.success(Unit)
    override suspend fun migrateSettings(settings: com.eunio.healthapp.domain.model.settings.UserSettings, targetVersion: Int) = com.eunio.healthapp.domain.util.Result.success(settings)
}

/**
 * Unit tests for PrivacyPreferencesViewModel.
 * Tests data export, account deletion, and privacy settings management.
 */
class PrivacyPreferencesViewModelTest {
    
    @Test
    fun uiState_initialValues_areCorrect() {
        val uiState = PrivacyPreferencesUiState()
        
        assertEquals(PrivacyPreferences.default(), uiState.preferences)
        assertEquals(LoadingState.Idle, uiState.loadingState)
        assertFalse(uiState.isUpdating)
        assertFalse(uiState.isExportingData)
        assertFalse(uiState.isDeletingAccount)
        assertEquals(0f, uiState.exportProgress)
        assertFalse(uiState.showDeleteConfirmation)
        assertEquals(0, uiState.deleteConfirmationStep)
        assertNull(uiState.exportedDataSize)
        assertNull(uiState.lastExportDate)
        assertFalse(uiState.isLoading)
        assertTrue(uiState.isEnabled)
        assertNull(uiState.errorMessage)
        assertFalse(uiState.hasPreferences)
        assertFalse(uiState.isDataExportInProgress)
        assertFalse(uiState.isDataExportComplete)
        assertFalse(uiState.isAccountDeletionInProgress)
        assertFalse(uiState.isDeleteConfirmationFinalStep)
    }
    
    @Test
    fun uiState_isLoading_trueWhenLoadingState() {
        val uiState = PrivacyPreferencesUiState(
            loadingState = LoadingState.Loading
        )
        
        assertTrue(uiState.isLoading)
        assertFalse(uiState.isEnabled)
    }
    
    @Test
    fun uiState_isEnabled_falseWhenUpdating() {
        val uiState = PrivacyPreferencesUiState(
            isUpdating = true
        )
        
        assertFalse(uiState.isEnabled)
        assertFalse(uiState.isLoading)
    }
    
    @Test
    fun uiState_isEnabled_falseWhenExportingData() {
        val uiState = PrivacyPreferencesUiState(
            isExportingData = true
        )
        
        assertFalse(uiState.isEnabled)
        assertFalse(uiState.isLoading)
    }
    
    @Test
    fun uiState_isEnabled_falseWhenDeletingAccount() {
        val uiState = PrivacyPreferencesUiState(
            isDeletingAccount = true
        )
        
        assertFalse(uiState.isEnabled)
        assertFalse(uiState.isLoading)
    }
    
    @Test
    fun uiState_isDataExportInProgress_trueWhenExportingWithProgress() {
        val uiState = PrivacyPreferencesUiState(
            isExportingData = true,
            exportProgress = 0.5f
        )
        
        assertTrue(uiState.isDataExportInProgress)
        assertFalse(uiState.isDataExportComplete)
    }
    
    @Test
    fun uiState_isDataExportComplete_trueWhenExportingWithFullProgress() {
        val uiState = PrivacyPreferencesUiState(
            isExportingData = true,
            exportProgress = 1f
        )
        
        assertTrue(uiState.isDataExportComplete)
        assertFalse(uiState.isDataExportInProgress)
    }
    
    @Test
    fun uiState_isAccountDeletionInProgress_trueWhenDeletingOrShowingConfirmation() {
        val deletingState = PrivacyPreferencesUiState(
            isDeletingAccount = true
        )
        assertTrue(deletingState.isAccountDeletionInProgress)
        
        val confirmationState = PrivacyPreferencesUiState(
            showDeleteConfirmation = true
        )
        assertTrue(confirmationState.isAccountDeletionInProgress)
    }
    
    @Test
    fun uiState_isDeleteConfirmationFinalStep_trueWhenStepTwoOrMore() {
        val step1State = PrivacyPreferencesUiState(
            deleteConfirmationStep = 1
        )
        assertFalse(step1State.isDeleteConfirmationFinalStep)
        
        val step2State = PrivacyPreferencesUiState(
            deleteConfirmationStep = 2
        )
        assertTrue(step2State.isDeleteConfirmationFinalStep)
        
        val step3State = PrivacyPreferencesUiState(
            deleteConfirmationStep = 3
        )
        assertTrue(step3State.isDeleteConfirmationFinalStep)
    }
    
    @Test
    fun uiState_isDataCollectionDisabled_trueWhenAllDisabled() {
        val allDisabledPrefs = PrivacyPreferences(
            dataSharingEnabled = false,
            anonymousInsightsEnabled = false,
            crashReportingEnabled = false,
            analyticsEnabled = false
        )
        
        val uiState = PrivacyPreferencesUiState(
            preferences = allDisabledPrefs
        )
        
        assertTrue(uiState.isDataCollectionDisabled)
        assertFalse(uiState.isDataSharingEnabled)
    }
    
    @Test
    fun uiState_isDataSharingEnabled_trueWhenEnabled() {
        val dataSharingPrefs = PrivacyPreferences(
            dataSharingEnabled = true
        )
        
        val uiState = PrivacyPreferencesUiState(
            preferences = dataSharingPrefs
        )
        
        assertTrue(uiState.isDataSharingEnabled)
        assertFalse(uiState.isDataCollectionDisabled)
    }
    
    @Test
    fun uiState_getPrivacyLevelDescription_worksCorrectly() {
        // Maximum Privacy
        val maxPrivacyPrefs = PrivacyPreferences(
            dataSharingEnabled = false,
            anonymousInsightsEnabled = false,
            crashReportingEnabled = false,
            analyticsEnabled = false
        )
        val maxPrivacyState = PrivacyPreferencesUiState(preferences = maxPrivacyPrefs)
        assertEquals("Maximum Privacy", maxPrivacyState.getPrivacyLevelDescription())
        
        // High Privacy
        val highPrivacyPrefs = PrivacyPreferences(
            dataSharingEnabled = false,
            anonymousInsightsEnabled = false,
            crashReportingEnabled = true,
            analyticsEnabled = false
        )
        val highPrivacyState = PrivacyPreferencesUiState(preferences = highPrivacyPrefs)
        assertEquals("High Privacy", highPrivacyState.getPrivacyLevelDescription())
        
        // Balanced Privacy
        val balancedPrivacyPrefs = PrivacyPreferences(
            dataSharingEnabled = false,
            anonymousInsightsEnabled = true,
            crashReportingEnabled = true,
            analyticsEnabled = true
        )
        val balancedPrivacyState = PrivacyPreferencesUiState(preferences = balancedPrivacyPrefs)
        assertEquals("Balanced Privacy", balancedPrivacyState.getPrivacyLevelDescription())
        
        // Standard Privacy
        val standardPrivacyPrefs = PrivacyPreferences(
            dataSharingEnabled = true,
            anonymousInsightsEnabled = true,
            crashReportingEnabled = true,
            analyticsEnabled = true
        )
        val standardPrivacyState = PrivacyPreferencesUiState(preferences = standardPrivacyPrefs)
        assertEquals("Standard Privacy", standardPrivacyState.getPrivacyLevelDescription())
    }
    
    @Test
    fun uiState_getEnabledDataCollectionCount_worksCorrectly() {
        val someEnabledPrefs = PrivacyPreferences(
            dataSharingEnabled = true,
            anonymousInsightsEnabled = false,
            crashReportingEnabled = true,
            analyticsEnabled = true
        )
        
        val uiState = PrivacyPreferencesUiState(
            preferences = someEnabledPrefs
        )
        
        assertEquals(3, uiState.getEnabledDataCollectionCount())
        
        val noneEnabledPrefs = PrivacyPreferences(
            dataSharingEnabled = false,
            anonymousInsightsEnabled = false,
            crashReportingEnabled = false,
            analyticsEnabled = false
        )
        
        val noneEnabledState = PrivacyPreferencesUiState(
            preferences = noneEnabledPrefs
        )
        
        assertEquals(0, noneEnabledState.getEnabledDataCollectionCount())
    }
    
    @Test
    fun uiState_getExportProgressPercentage_worksCorrectly() {
        val uiState = PrivacyPreferencesUiState(
            exportProgress = 0.75f
        )
        
        assertEquals(75, uiState.getExportProgressPercentage())
        
        val completeState = PrivacyPreferencesUiState(
            exportProgress = 1f
        )
        
        assertEquals(100, completeState.getExportProgressPercentage())
    }
    
    @Test
    fun fakeSettingsRepository_exportUserData_worksCorrectly() = runTest {
        val fakeRepository = FakeSettingsRepositoryForPrivacy()
        
        // Test successful export
        val result = fakeRepository.exportUserData("test_user")
        assertTrue(result.isSuccess)
        
        val exportData = result.getOrNull()
        assertNotNull(exportData)
        assertTrue(exportData.contains("test_user"))
        assertTrue(exportData.contains("settings"))
        assertTrue(exportData.contains("exportDate"))
        
        // Test export failure
        fakeRepository.setShouldFailOnExport(true)
        val errorResult = fakeRepository.exportUserData("test_user")
        assertTrue(errorResult.isError)
    }
    
    @Test
    fun fakeSettingsRepository_deleteUserSettings_worksCorrectly() = runTest {
        val fakeRepository = FakeSettingsRepositoryForPrivacy()
        
        // Test successful deletion
        val result = fakeRepository.deleteUserSettings("test_user")
        assertTrue(result.isSuccess)
        
        // Test deletion failure
        fakeRepository.setShouldFailOnDelete(true)
        val errorResult = fakeRepository.deleteUserSettings("test_user")
        assertTrue(errorResult.isError)
    }
    
    @Test
    fun privacyPreferences_default_valuesAreCorrect() {
        val defaultPrefs = PrivacyPreferences.default()
        
        assertFalse(defaultPrefs.dataSharingEnabled)
        assertTrue(defaultPrefs.anonymousInsightsEnabled)
        assertTrue(defaultPrefs.crashReportingEnabled)
        assertTrue(defaultPrefs.analyticsEnabled)
    }
    
    @Test
    fun privacyPreferences_maxPrivacy_valuesAreCorrect() {
        val maxPrivacyPrefs = PrivacyPreferences.maxPrivacy()
        
        assertFalse(maxPrivacyPrefs.dataSharingEnabled)
        assertFalse(maxPrivacyPrefs.anonymousInsightsEnabled)
        assertFalse(maxPrivacyPrefs.crashReportingEnabled)
        assertFalse(maxPrivacyPrefs.analyticsEnabled)
    }
    
    @Test
    fun privacyPreferences_balanced_valuesAreCorrect() {
        val balancedPrefs = PrivacyPreferences.balanced()
        
        assertFalse(balancedPrefs.dataSharingEnabled)
        assertTrue(balancedPrefs.anonymousInsightsEnabled)
        assertTrue(balancedPrefs.crashReportingEnabled)
        assertFalse(balancedPrefs.analyticsEnabled)
    }
    
    @Test
    fun privacyPreferences_isValid_worksCorrectly() {
        val validPrefs = PrivacyPreferences.default()
        assertTrue(validPrefs.isValid())
        
        val maxPrivacyPrefs = PrivacyPreferences.maxPrivacy()
        assertTrue(maxPrivacyPrefs.isValid())
        
        val balancedPrefs = PrivacyPreferences.balanced()
        assertTrue(balancedPrefs.isValid())
    }
    
    @Test
    fun privacyPreferences_hasDataCollectionEnabled_worksCorrectly() {
        val defaultPrefs = PrivacyPreferences.default()
        assertTrue(defaultPrefs.hasDataCollectionEnabled())
        
        val maxPrivacyPrefs = PrivacyPreferences.maxPrivacy()
        assertFalse(maxPrivacyPrefs.hasDataCollectionEnabled())
        
        val partialPrefs = PrivacyPreferences(
            dataSharingEnabled = false,
            anonymousInsightsEnabled = true,
            crashReportingEnabled = false,
            analyticsEnabled = false
        )
        assertTrue(partialPrefs.hasDataCollectionEnabled())
    }
}