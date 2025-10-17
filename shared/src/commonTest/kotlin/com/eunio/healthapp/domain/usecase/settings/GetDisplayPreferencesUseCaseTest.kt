package com.eunio.healthapp.domain.usecase.settings

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.error.SettingsError
import com.eunio.healthapp.domain.model.settings.DisplayPreferences
import com.eunio.healthapp.domain.model.settings.HapticIntensity
import com.eunio.healthapp.domain.model.settings.UserSettings
import com.eunio.healthapp.domain.repository.SettingsRepository
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.*

/**
 * Mock implementation of SettingsRepository for testing GetDisplayPreferencesUseCase
 */
class MockSettingsRepositoryForGet : SettingsRepository {
    private var userSettings: UserSettings? = null
    var shouldThrowException = false
    var getCallCount = 0
    
    // Core getUserSettings methods - consistent implementation
    override suspend fun getUserSettings(): Result<UserSettings?> {
        getCallCount++
        if (shouldThrowException) {
            return Result.error(AppError.DatabaseError("Repository error"))
        }
        return Result.success(userSettings)
    }
    
    override suspend fun getUserSettings(userId: String): Result<UserSettings?> {
        getCallCount++
        if (shouldThrowException) {
            return Result.error(AppError.DatabaseError("Repository error"))
        }
        return Result.success(userSettings)
    }
    
    override suspend fun saveUserSettings(settings: UserSettings): Result<Unit> {
        userSettings = settings
        return Result.success(Unit)
    }
    
    override suspend fun updateUserSettings(userId: String, updateFunction: (UserSettings) -> UserSettings): Result<UserSettings> {
        val current = userSettings ?: return Result.error(AppError.DatabaseError("No settings found"))
        val updated = updateFunction(current)
        userSettings = updated
        return Result.success(updated)
    }
    
    override suspend fun syncSettings(): Result<Unit> = Result.success(Unit)
    
    override suspend fun syncSettings(userId: String): Result<Unit> = Result.success(Unit)
    
    // Methods not used by this test - clear NotImplementedError with context
    override suspend fun resolveSettingsConflict(userId: String, localSettings: UserSettings, remoteSettings: UserSettings): Result<UserSettings> {
        throw NotImplementedError("resolveSettingsConflict not needed for GetDisplayPreferencesUseCaseTest")
    }
    
    override suspend fun backupUserSettings(userId: String): Result<String> {
        throw NotImplementedError("backupUserSettings not needed for GetDisplayPreferencesUseCaseTest")
    }
    
    override suspend fun restoreUserSettings(userId: String, backupData: String): Result<Unit> {
        throw NotImplementedError("restoreUserSettings not needed for GetDisplayPreferencesUseCaseTest")
    }
    
    override suspend fun exportUserData(userId: String): Result<String> {
        throw NotImplementedError("exportUserData not needed for GetDisplayPreferencesUseCaseTest")
    }
    
    override suspend fun deleteUserSettings(userId: String): Result<Unit> {
        throw NotImplementedError("deleteUserSettings not needed for GetDisplayPreferencesUseCaseTest")
    }
    
    override suspend fun clearLocalSettings(): Result<Unit> {
        throw NotImplementedError("clearLocalSettings not needed for GetDisplayPreferencesUseCaseTest")
    }
    
    override suspend fun resetToDefaults(userId: String, locale: String?): Result<UserSettings> {
        throw NotImplementedError("resetToDefaults not needed for GetDisplayPreferencesUseCaseTest")
    }
    
    override fun observeUserSettings(userId: String): kotlinx.coroutines.flow.Flow<UserSettings?> {
        throw NotImplementedError("observeUserSettings not needed for GetDisplayPreferencesUseCaseTest")
    }
    
    override fun observeSyncStatus(): kotlinx.coroutines.flow.Flow<Boolean> {
        throw NotImplementedError("observeSyncStatus not needed for GetDisplayPreferencesUseCaseTest")
    }
    
    override suspend fun getPendingSyncSettings(): Result<List<UserSettings>> {
        throw NotImplementedError("getPendingSyncSettings not needed for GetDisplayPreferencesUseCaseTest")
    }
    
    override suspend fun markAsSynced(userId: String): Result<Unit> {
        throw NotImplementedError("markAsSynced not needed for GetDisplayPreferencesUseCaseTest")
    }
    
    override suspend fun markAsSyncFailed(userId: String): Result<Unit> {
        throw NotImplementedError("markAsSyncFailed not needed for GetDisplayPreferencesUseCaseTest")
    }
    
    override suspend fun settingsExist(userId: String): Result<Boolean> {
        throw NotImplementedError("settingsExist not needed for GetDisplayPreferencesUseCaseTest")
    }
    
    override suspend fun getLastModifiedTimestamp(userId: String): Result<Long?> {
        throw NotImplementedError("getLastModifiedTimestamp not needed for GetDisplayPreferencesUseCaseTest")
    }
    
    override suspend fun validateSettings(settings: UserSettings): Result<Unit> {
        throw NotImplementedError("validateSettings not needed for GetDisplayPreferencesUseCaseTest")
    }
    
    override suspend fun migrateSettings(settings: UserSettings, targetVersion: Int): Result<UserSettings> {
        throw NotImplementedError("migrateSettings not needed for GetDisplayPreferencesUseCaseTest")
    }
    
    fun setUserSettings(settings: UserSettings?) {
        userSettings = settings
    }
    
    fun reset() {
        userSettings = null
        shouldThrowException = false
        getCallCount = 0
    }
}

class GetDisplayPreferencesUseCaseTest {
    
    private lateinit var mockRepository: MockSettingsRepositoryForGet
    private lateinit var useCase: GetDisplayPreferencesUseCase
    
    private val testDisplayPreferences = DisplayPreferences(
        textSizeScale = 1.2f,
        highContrastMode = true,
        hapticFeedbackEnabled = true,
        hapticIntensity = HapticIntensity.MEDIUM
    )
    
    private val testUserSettings = UserSettings(
        userId = "test-user-id",
        displayPreferences = testDisplayPreferences,
        lastModified = Clock.System.now()
    )
    
    @BeforeTest
    fun setup() {
        mockRepository = MockSettingsRepositoryForGet()
        useCase = GetDisplayPreferencesUseCase(mockRepository)
    }
    
    @AfterTest
    fun tearDown() {
        mockRepository.reset()
    }
    
    @Test
    fun `invoke returns display preferences when user settings exist`() = runTest {
        // Given: Repository returns user settings with display preferences
        mockRepository.setUserSettings(testUserSettings)
        
        // When: Invoking use case
        val result = useCase()
        
        // Then: Returns display preferences successfully
        assertTrue(result.isSuccess)
        assertEquals(testDisplayPreferences, result.getOrNull())
        assertEquals(1, mockRepository.getCallCount)
    }
    
    @Test
    fun `invoke throws IllegalStateException when user settings are null`() = runTest {
        // Given: Repository returns null user settings
        mockRepository.setUserSettings(null)
        
        // When: Invoking use case
        val result = useCase()
        
        // Then: Returns error with SettingsError
        assertTrue(result.isError)
        val error = result.errorOrNull()
        assertTrue(error is SettingsError.ValidationError)
        assertEquals("User settings not found", error?.message)
    }
    
    @Test
    fun `invoke propagates repository errors`() = runTest {
        // Given: Repository throws exception
        mockRepository.shouldThrowException = true
        
        // When: Invoking use case
        val result = useCase()
        
        // Then: Returns error from repository
        assertTrue(result.isError)
        val error = result.errorOrNull()
        assertTrue(error is AppError.DatabaseError)
        assertEquals("Repository error", error?.message)
    }
    
    @Test
    fun `invoke calls repository exactly once`() = runTest {
        // Given: Repository returns user settings
        mockRepository.setUserSettings(testUserSettings)
        
        // When: Invoking use case multiple times
        useCase()
        useCase()
        useCase()
        
        // Then: Repository is called for each invocation
        assertEquals(3, mockRepository.getCallCount)
    }
    

}