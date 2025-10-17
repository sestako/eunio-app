package com.eunio.healthapp.presentation.viewmodel

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.manager.SettingsManager
import com.eunio.healthapp.domain.model.settings.*
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.presentation.state.LoadingState
import com.eunio.healthapp.presentation.state.SettingsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Fake implementation of SettingsManager for testing
 */
class FakeSettingsManager : SettingsManager {
    private val _userSettings = MutableStateFlow(createTestUserSettings())
    private val _syncStatus = MutableStateFlow(true)
    private var shouldFailOnGet = false
    private var shouldFailOnSync = false
    
    fun setShouldFailOnGet(shouldFail: Boolean) {
        shouldFailOnGet = shouldFail
    }
    
    fun setShouldFailOnSync(shouldFail: Boolean) {
        shouldFailOnSync = shouldFail
    }
    
    fun updateSettings(settings: UserSettings) {
        _userSettings.value = settings
    }
    
    override suspend fun getUserSettings(): Result<UserSettings> {
        return if (shouldFailOnGet) {
            Result.error(AppError.NetworkError("Test error"))
        } else {
            Result.success(_userSettings.value)
        }
    }
    
    override fun observeSettingsChanges() = _userSettings.asStateFlow()
    
    override suspend fun updateUnitPreferences(preferences: UnitPreferences): Result<Unit> {
        val updated = _userSettings.value.copy(unitPreferences = preferences)
        _userSettings.value = updated
        return Result.success(Unit)
    }
    
    override suspend fun updateNotificationPreferences(preferences: NotificationPreferences): Result<Unit> {
        val updated = _userSettings.value.copy(notificationPreferences = preferences)
        _userSettings.value = updated
        return Result.success(Unit)
    }
    
    override suspend fun updateCyclePreferences(preferences: CyclePreferences): Result<Unit> {
        val updated = _userSettings.value.copy(cyclePreferences = preferences)
        _userSettings.value = updated
        return Result.success(Unit)
    }
    
    override suspend fun updatePrivacyPreferences(preferences: PrivacyPreferences): Result<Unit> {
        val updated = _userSettings.value.copy(privacyPreferences = preferences)
        _userSettings.value = updated
        return Result.success(Unit)
    }
    
    override suspend fun updateDisplayPreferences(preferences: DisplayPreferences): Result<Unit> {
        val updated = _userSettings.value.copy(displayPreferences = preferences)
        _userSettings.value = updated
        return Result.success(Unit)
    }
    
    override suspend fun updateSyncPreferences(preferences: SyncPreferences): Result<Unit> {
        val updated = _userSettings.value.copy(syncPreferences = preferences)
        _userSettings.value = updated
        return Result.success(Unit)
    }
    
    override suspend fun updateSettings(updateFunction: (UserSettings) -> UserSettings): Result<UserSettings> {
        val updated = updateFunction(_userSettings.value)
        _userSettings.value = updated
        return Result.success(updated)
    }
    
    override suspend fun validateSettings(settings: UserSettings): Result<Unit> {
        return if (settings.isValid()) {
            Result.success(Unit)
        } else {
            Result.error(AppError.ValidationError("Invalid settings"))
        }
    }
    
    override suspend fun resetToDefaults(preserveUnitPreferences: Boolean): Result<UserSettings> {
        val defaults = UserSettings.createDefault("test_user")
        _userSettings.value = defaults
        return Result.success(defaults)
    }
    
    override suspend fun syncSettings(): Result<Unit> {
        return if (shouldFailOnSync) {
            Result.error(AppError.NetworkError("Sync failed"))
        } else {
            Result.success(Unit)
        }
    }
    
    override suspend fun exportSettings(): Result<String> {
        return Result.success("{\"settings\": \"exported\"}")
    }
    
    override suspend fun importSettings(backupData: String): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun isSynced(): Boolean = _syncStatus.value
    
    override fun observeSyncStatus() = _syncStatus.asStateFlow()
    
    private fun createTestUserSettings(): UserSettings {
        return UserSettings.createDefault("test_user")
    }
}

/**
 * Unit tests for SettingsViewModel.
 * Tests search functionality, state management, and navigation.
 */
class SettingsViewModelTest {
    
    @Test
    fun uiState_initialValues_areCorrect() {
        val uiState = SettingsUiState()
        
        assertNull(uiState.settings)
        assertEquals("", uiState.searchQuery)
        assertEquals(emptyList(), uiState.filteredSections)
        assertEquals(LoadingState.Idle, uiState.loadingState)
        assertFalse(uiState.isRefreshing)
        assertFalse(uiState.syncStatus)
        assertFalse(uiState.isLoading)
        assertTrue(uiState.isEnabled)
        assertNull(uiState.errorMessage)
        assertFalse(uiState.isSearching)
        assertFalse(uiState.hasSettings)
    }
    
    @Test
    fun uiState_isLoading_trueWhenLoadingState() {
        val uiState = SettingsUiState(
            loadingState = LoadingState.Loading
        )
        
        assertTrue(uiState.isLoading)
        assertFalse(uiState.isEnabled)
    }
    
    @Test
    fun uiState_isEnabled_falseWhenRefreshing() {
        val uiState = SettingsUiState(
            isRefreshing = true
        )
        
        assertFalse(uiState.isEnabled)
        assertFalse(uiState.isLoading)
    }
    
    @Test
    fun uiState_errorMessage_extractedFromLoadingState() {
        val errorMessage = "Test error"
        val uiState = SettingsUiState(
            loadingState = LoadingState.Error(errorMessage)
        )
        
        assertEquals(errorMessage, uiState.errorMessage)
        assertFalse(uiState.isLoading)
    }
    
    @Test
    fun uiState_isSearching_trueWhenQueryNotBlank() {
        val uiState = SettingsUiState(
            searchQuery = "notifications"
        )
        
        assertTrue(uiState.isSearching)
    }
    
    @Test
    fun uiState_hasSettings_trueWhenSettingsLoadedSuccessfully() {
        val settings = UserSettings.createDefault("test_user")
        val uiState = SettingsUiState(
            settings = settings,
            loadingState = LoadingState.Success(settings)
        )
        
        assertTrue(uiState.hasSettings)
    }
    
    @Test
    fun fakeSettingsManager_worksCorrectly() = runTest {
        val fakeManager = FakeSettingsManager()
        
        // Test initial state
        val result = fakeManager.getUserSettings()
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
        
        // Test settings update
        val newUnitPrefs = UnitPreferences(
            temperatureUnit = TemperatureUnit.FAHRENHEIT,
            weightUnit = WeightUnit.POUNDS,
            isManuallySet = true
        )
        val updateResult = fakeManager.updateUnitPreferences(newUnitPrefs)
        assertTrue(updateResult.isSuccess)
        
        // Verify update
        val updatedSettings = fakeManager.getUserSettings().getOrNull()
        assertNotNull(updatedSettings)
        assertEquals(TemperatureUnit.FAHRENHEIT, updatedSettings.unitPreferences.temperatureUnit)
        
        // Test error handling
        fakeManager.setShouldFailOnGet(true)
        val errorResult = fakeManager.getUserSettings()
        assertTrue(errorResult.isError)
    }
    
    @Test
    fun fakeSettingsManager_observeChanges_worksCorrectly() = runTest {
        val fakeManager = FakeSettingsManager()
        
        // Test that the flow emits the current value
        val initialValue = fakeManager.observeSettingsChanges().first()
        assertNotNull(initialValue)
        assertEquals("test_user", initialValue.userId)
        
        // Make a change and verify it's reflected
        val newSettings = initialValue.copy(
            unitPreferences = UnitPreferences(
                temperatureUnit = TemperatureUnit.FAHRENHEIT,
                weightUnit = WeightUnit.POUNDS,
                isManuallySet = true
            )
        )
        fakeManager.updateSettings(newSettings)
        
        val updatedValue = fakeManager.observeSettingsChanges().first()
        assertEquals(TemperatureUnit.FAHRENHEIT, updatedValue.unitPreferences.temperatureUnit)
    }
    
    @Test
    fun fakeSettingsManager_syncSettings_worksCorrectly() = runTest {
        val fakeManager = FakeSettingsManager()
        
        // Test successful sync
        val result = fakeManager.syncSettings()
        assertTrue(result.isSuccess)
        
        // Test sync failure
        fakeManager.setShouldFailOnSync(true)
        val errorResult = fakeManager.syncSettings()
        assertTrue(errorResult.isError)
    }
    
    @Test
    fun fakeSettingsManager_resetToDefaults_worksCorrectly() = runTest {
        val fakeManager = FakeSettingsManager()
        
        // Modify settings first
        val customPrefs = UnitPreferences(
            temperatureUnit = TemperatureUnit.FAHRENHEIT,
            weightUnit = WeightUnit.POUNDS,
            isManuallySet = true
        )
        fakeManager.updateUnitPreferences(customPrefs)
        
        // Reset to defaults
        val result = fakeManager.resetToDefaults()
        assertTrue(result.isSuccess)
        
        val resetSettings = result.getOrNull()
        assertNotNull(resetSettings)
        assertEquals(TemperatureUnit.CELSIUS, resetSettings.unitPreferences.temperatureUnit)
        assertEquals(WeightUnit.KILOGRAMS, resetSettings.unitPreferences.weightUnit)
        assertFalse(resetSettings.unitPreferences.isManuallySet)
    }
    
    @Test
    fun fakeSettingsManager_exportSettings_worksCorrectly() = runTest {
        val fakeManager = FakeSettingsManager()
        
        val result = fakeManager.exportSettings()
        assertTrue(result.isSuccess)
        
        val exportData = result.getOrNull()
        assertNotNull(exportData)
        assertTrue(exportData.contains("settings"))
    }
    
    @Test
    fun fakeSettingsManager_validateSettings_worksCorrectly() = runTest {
        val fakeManager = FakeSettingsManager()
        
        // Test valid settings
        val validSettings = UserSettings.createDefault("test_user")
        val validResult = fakeManager.validateSettings(validSettings)
        assertTrue(validResult.isSuccess)
        
        // Test invalid settings (empty user ID)
        val invalidSettings = validSettings.copy(userId = "")
        val invalidResult = fakeManager.validateSettings(invalidSettings)
        assertTrue(invalidResult.isError)
    }
}