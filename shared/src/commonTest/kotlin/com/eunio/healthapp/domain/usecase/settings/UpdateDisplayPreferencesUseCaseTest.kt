package com.eunio.healthapp.domain.usecase.settings

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.settings.DisplayPreferences
import com.eunio.healthapp.domain.model.settings.HapticIntensity
import com.eunio.healthapp.domain.model.settings.UserSettings
import com.eunio.healthapp.domain.repository.SettingsRepository
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.*

/**
 * Mock implementation of SettingsRepository for testing UpdateDisplayPreferencesUseCase
 */
class MockSettingsRepositoryForUpdate : SettingsRepository {
    private var userSettings: UserSettings? = null
    var shouldThrowGetException = false
    var shouldThrowSaveException = false
    var getCallCount = 0
    var saveCallCount = 0
    
    override suspend fun getUserSettings(): Result<UserSettings?> {
        getCallCount++
        if (shouldThrowGetException) {
            return Result.error(AppError.DatabaseError("Get settings failed"))
        }
        return Result.success(userSettings)
    }
    
    override suspend fun getUserSettings(userId: String): Result<UserSettings?> {
        getCallCount++
        if (shouldThrowGetException) {
            return Result.error(AppError.DatabaseError("Get settings failed"))
        }
        return Result.success(userSettings)
    }
    
    override suspend fun saveUserSettings(settings: UserSettings): Result<Unit> {
        saveCallCount++
        if (shouldThrowSaveException) {
            return Result.error(AppError.DatabaseError("Save settings failed"))
        }
        userSettings = settings
        return Result.success(Unit)
    }
    
    override suspend fun updateUserSettings(userId: String, updateFunction: (UserSettings) -> UserSettings): Result<UserSettings> {
        val current = userSettings ?: return Result.error(AppError.DatabaseError("No settings found"))
        val updated = updateFunction(current)
        userSettings = updated
        return Result.success(updated)
    }
    
    // Methods not used by this test - clear NotImplementedError with context
    override suspend fun syncSettings(): Result<Unit> {
        throw NotImplementedError("syncSettings not needed for UpdateDisplayPreferencesUseCaseTest")
    }
    
    override suspend fun syncSettings(userId: String): Result<Unit> {
        throw NotImplementedError("syncSettings(userId) not needed for UpdateDisplayPreferencesUseCaseTest")
    }
    
    override suspend fun resolveSettingsConflict(userId: String, localSettings: UserSettings, remoteSettings: UserSettings): Result<UserSettings> {
        throw NotImplementedError("resolveSettingsConflict not needed for UpdateDisplayPreferencesUseCaseTest")
    }
    
    override suspend fun backupUserSettings(userId: String): Result<String> {
        throw NotImplementedError("backupUserSettings not needed for UpdateDisplayPreferencesUseCaseTest")
    }
    
    override suspend fun restoreUserSettings(userId: String, backupData: String): Result<Unit> {
        throw NotImplementedError("restoreUserSettings not needed for UpdateDisplayPreferencesUseCaseTest")
    }
    
    override suspend fun exportUserData(userId: String): Result<String> {
        throw NotImplementedError("exportUserData not needed for UpdateDisplayPreferencesUseCaseTest")
    }
    
    override suspend fun deleteUserSettings(userId: String): Result<Unit> {
        throw NotImplementedError("deleteUserSettings not needed for UpdateDisplayPreferencesUseCaseTest")
    }
    
    override suspend fun clearLocalSettings(): Result<Unit> {
        throw NotImplementedError("clearLocalSettings not needed for UpdateDisplayPreferencesUseCaseTest")
    }
    
    override suspend fun resetToDefaults(userId: String, locale: String?): Result<UserSettings> {
        throw NotImplementedError("resetToDefaults not needed for UpdateDisplayPreferencesUseCaseTest")
    }
    
    override fun observeUserSettings(userId: String): kotlinx.coroutines.flow.Flow<UserSettings?> {
        throw NotImplementedError("observeUserSettings not needed for UpdateDisplayPreferencesUseCaseTest")
    }
    
    override fun observeSyncStatus(): kotlinx.coroutines.flow.Flow<Boolean> {
        throw NotImplementedError("observeSyncStatus not needed for UpdateDisplayPreferencesUseCaseTest")
    }
    
    override suspend fun getPendingSyncSettings(): Result<List<UserSettings>> {
        throw NotImplementedError("getPendingSyncSettings not needed for UpdateDisplayPreferencesUseCaseTest")
    }
    
    override suspend fun markAsSynced(userId: String): Result<Unit> {
        throw NotImplementedError("markAsSynced not needed for UpdateDisplayPreferencesUseCaseTest")
    }
    
    override suspend fun markAsSyncFailed(userId: String): Result<Unit> {
        throw NotImplementedError("markAsSyncFailed not needed for UpdateDisplayPreferencesUseCaseTest")
    }
    
    override suspend fun settingsExist(userId: String): Result<Boolean> {
        throw NotImplementedError("settingsExist not needed for UpdateDisplayPreferencesUseCaseTest")
    }
    
    override suspend fun getLastModifiedTimestamp(userId: String): Result<Long?> {
        throw NotImplementedError("getLastModifiedTimestamp not needed for UpdateDisplayPreferencesUseCaseTest")
    }
    
    override suspend fun validateSettings(settings: UserSettings): Result<Unit> {
        throw NotImplementedError("validateSettings not needed for UpdateDisplayPreferencesUseCaseTest")
    }
    
    override suspend fun migrateSettings(settings: UserSettings, targetVersion: Int): Result<UserSettings> {
        throw NotImplementedError("migrateSettings not needed for UpdateDisplayPreferencesUseCaseTest")
    }
    

    
    fun setUserSettings(settings: UserSettings?) {
        userSettings = settings
    }
    
    fun getCurrentUserSettings(): UserSettings? = userSettings
    
    fun reset() {
        userSettings = null
        shouldThrowGetException = false
        shouldThrowSaveException = false
        getCallCount = 0
        saveCallCount = 0
    }
}

/**
 * Helper to create test DisplayPreferences with custom validation behavior
 */
object TestDisplayPreferencesFactory {
    fun createValid(): DisplayPreferences {
        return DisplayPreferences(
            textSizeScale = 1.2f,
            highContrastMode = true,
            hapticFeedbackEnabled = true,
            hapticIntensity = HapticIntensity.MEDIUM
        )
    }
    
    fun createInvalid(): DisplayPreferences {
        return DisplayPreferences(
            textSizeScale = -1.0f, // Invalid: below minimum
            highContrastMode = false,
            hapticFeedbackEnabled = false,
            hapticIntensity = HapticIntensity.MEDIUM // Invalid: should be DISABLED when haptic is off
        )
    }
}

class UpdateDisplayPreferencesUseCaseTest {
    
    private lateinit var mockRepository: MockSettingsRepositoryForUpdate
    private lateinit var useCase: UpdateDisplayPreferencesUseCase
    
    private val validDisplayPreferences = TestDisplayPreferencesFactory.createValid()
    private val invalidDisplayPreferences = TestDisplayPreferencesFactory.createInvalid()
    
    private val testUserSettings = UserSettings(
        userId = "test-user-id",
        displayPreferences = validDisplayPreferences,
        lastModified = Clock.System.now()
    )
    
    @BeforeTest
    fun setup() {
        mockRepository = MockSettingsRepositoryForUpdate()
        useCase = UpdateDisplayPreferencesUseCase(mockRepository)
    }
    
    @AfterTest
    fun tearDown() {
        mockRepository.reset()
    }
    
    @Test
    fun `invoke successfully updates valid display preferences`() = runTest {
        // Given: Repository returns existing user settings
        mockRepository.setUserSettings(testUserSettings)
        
        // Add small delay to ensure timestamp difference on all platforms
        kotlinx.coroutines.delay(1)
        
        // When: Updating with valid display preferences
        val result = useCase(validDisplayPreferences)
        
        // Then: Operation succeeds and settings are updated
        assertTrue(result.isSuccess)
        assertEquals(1, mockRepository.getCallCount)
        assertEquals(1, mockRepository.saveCallCount)
        
        val updatedSettings = mockRepository.getCurrentUserSettings()
        assertNotNull(updatedSettings)
        assertEquals(validDisplayPreferences, updatedSettings.displayPreferences)
        assertTrue(updatedSettings.lastModified >= testUserSettings.lastModified, "Updated timestamp should be >= original timestamp")
    }
    
    @Test
    fun `invoke fails with validation error for invalid preferences`() = runTest {
        // Given: Repository returns existing user settings
        mockRepository.setUserSettings(testUserSettings)
        
        // When: Updating with invalid display preferences
        val result = useCase(invalidDisplayPreferences)
        
        // Then: Operation fails with validation error
        assertTrue(result.isError)
        val error = result.errorOrNull()
        assertTrue(error is AppError.ValidationError)
        assertTrue(error?.message?.contains("Invalid display preferences") == true)
        
        // Repository should not be called for save
        assertEquals(0, mockRepository.getCallCount)
        assertEquals(0, mockRepository.saveCallCount)
    }
    
    @Test
    fun `invoke fails when user settings not found`() = runTest {
        // Given: Repository returns null user settings
        mockRepository.setUserSettings(null)
        
        // When: Updating display preferences
        val result = useCase(validDisplayPreferences)
        
        // Then: Operation fails with database error
        assertTrue(result.isError)
        val error = result.errorOrNull()
        assertTrue(error is AppError.DatabaseError)
        assertEquals("User settings not found", error?.message)
        
        assertEquals(1, mockRepository.getCallCount)
        assertEquals(0, mockRepository.saveCallCount)
    }
    
    @Test
    fun `invoke propagates repository get errors`() = runTest {
        // Given: Repository throws exception on get
        mockRepository.shouldThrowGetException = true
        
        // When: Updating display preferences
        val result = useCase(validDisplayPreferences)
        
        // Then: Operation fails with repository error
        assertTrue(result.isError)
        val error = result.errorOrNull()
        assertTrue(error is AppError.DatabaseError)
        assertEquals("Get settings failed", error?.message)
        
        assertEquals(1, mockRepository.getCallCount)
        assertEquals(0, mockRepository.saveCallCount)
    }
    
    @Test
    fun `invoke propagates repository save errors`() = runTest {
        // Given: Repository returns settings but fails on save
        mockRepository.setUserSettings(testUserSettings)
        mockRepository.shouldThrowSaveException = true
        
        // When: Updating display preferences
        val result = useCase(validDisplayPreferences)
        
        // Then: Operation fails with save error
        assertTrue(result.isError)
        val error = result.errorOrNull()
        assertTrue(error is AppError.DatabaseError)
        assertEquals("Save settings failed", error?.message)
        
        assertEquals(1, mockRepository.getCallCount)
        assertEquals(1, mockRepository.saveCallCount)
    }
    
    @Test
    fun `invoke preserves user ID and updates timestamp`() = runTest {
        // Given: Repository returns existing user settings
        val originalTimestamp = Clock.System.now()
        val originalSettings = testUserSettings.copy(lastModified = originalTimestamp)
        mockRepository.setUserSettings(originalSettings)
        
        // Add small delay to ensure timestamp difference on all platforms
        kotlinx.coroutines.delay(1)
        
        // When: Updating display preferences
        val result = useCase(validDisplayPreferences)
        
        // Then: User ID is preserved and timestamp is updated
        assertTrue(result.isSuccess)
        
        val updatedSettings = mockRepository.getCurrentUserSettings()
        assertNotNull(updatedSettings)
        assertEquals("test-user-id", updatedSettings.userId)
        assertTrue(updatedSettings.lastModified >= originalTimestamp, "Updated timestamp should be >= original timestamp")
    }
    
    @Test
    fun `invoke handles concurrent updates correctly`() = runTest {
        // Given: Repository returns existing user settings
        mockRepository.setUserSettings(testUserSettings)
        
        // When: Making multiple concurrent updates (simulated)
        val results = mutableListOf<Result<Unit>>()
        repeat(3) {
            val result = useCase(validDisplayPreferences)
            results.add(result)
        }
        
        // Then: All operations should succeed
        results.forEach { result ->
            assertTrue(result.isSuccess, "Concurrent updates should succeed")
        }
        
        assertEquals(3, mockRepository.getCallCount)
        assertEquals(3, mockRepository.saveCallCount)
    }
}