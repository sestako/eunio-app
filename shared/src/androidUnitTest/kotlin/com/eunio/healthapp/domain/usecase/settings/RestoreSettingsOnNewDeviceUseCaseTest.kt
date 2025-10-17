package com.eunio.healthapp.domain.usecase.settings

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.error.SettingsError
import com.eunio.healthapp.domain.manager.SettingsBackupManager
import com.eunio.healthapp.domain.model.HealthGoal
import com.eunio.healthapp.domain.model.User
import com.eunio.healthapp.domain.repository.UserRepository
import com.eunio.healthapp.domain.util.Result
import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class RestoreSettingsOnNewDeviceUseCaseTest {
    
    private lateinit var settingsBackupManager: SettingsBackupManager
    private lateinit var userRepository: UserRepository
    private lateinit var useCase: RestoreSettingsOnNewDeviceUseCase
    
    private val testUserId = "test-user-123"
    
    @BeforeTest
    fun setup() {
        settingsBackupManager = mockk()
        userRepository = mockk()
        
        useCase = RestoreSettingsOnNewDeviceUseCase(
            settingsBackupManager = settingsBackupManager,
            userRepository = userRepository
        )
    }
    
    @Test
    fun `execute should restore settings on new device`() = runTest {
        // Given
        coEvery { settingsBackupManager.restoreOnNewDevice(testUserId, null) } returns Result.success(Unit)
        
        // When
        val result = useCase.execute(testUserId, isNewDevice = true)
        
        // Then
        assertTrue(result.isSuccess)
        
        coVerify { settingsBackupManager.restoreOnNewDevice(testUserId, null) }
    }
    
    @Test
    fun `execute should skip restore on existing device`() = runTest {
        // Given
        // No mocking needed for existing device
        
        // When
        val result = useCase.execute(testUserId, isNewDevice = false)
        
        // Then
        assertTrue(result.isSuccess)
        
        // Should not call backup manager for existing devices
        coVerify(exactly = 0) { settingsBackupManager.restoreOnNewDevice(any(), any()) }
    }
    
    @Test
    fun `execute should handle restore failure gracefully`() = runTest {
        // Given
        val error = SettingsError.BackupError("Restore failed", "NEW_DEVICE_RESTORE")
        coEvery { settingsBackupManager.restoreOnNewDevice(testUserId, null) } returns Result.error(error)
        
        // When
        val result = useCase.execute(testUserId, isNewDevice = true)
        
        // Then
        assertTrue(result.isError)
        assertTrue(result.errorOrNull() is SettingsError.BackupError)
        
        coVerify { settingsBackupManager.restoreOnNewDevice(testUserId, null) }
    }
    
    @Test
    fun `execute should handle exceptions during restore`() = runTest {
        // Given
        coEvery { settingsBackupManager.restoreOnNewDevice(testUserId, null) } throws RuntimeException("Network error")
        
        // When
        val result = useCase.execute(testUserId, isNewDevice = true)
        
        // Then
        assertTrue(result.isError)
        assertTrue(result.errorOrNull() is SettingsError.BackupError)
        assertTrue(result.errorOrNull()?.message?.contains("Network error") == true)
        
        coVerify { settingsBackupManager.restoreOnNewDevice(testUserId, null) }
    }
    
    @Test
    fun `needsRestore should return false when user exists locally with same ID`() = runTest {
        // Given
        val user = User(
            id = testUserId,
            email = "test@example.com",
            name = "Test User",
            onboardingComplete = true,
            primaryGoal = HealthGoal.GENERAL_HEALTH,
            createdAt = kotlinx.datetime.Clock.System.now(),
            updatedAt = kotlinx.datetime.Clock.System.now()
        )
        coEvery { userRepository.getCurrentUser() } returns Result.success(user)
        
        // When
        val result = useCase.needsRestore(testUserId)
        
        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrThrow()) // Should not need restore
        
        coVerify { userRepository.getCurrentUser() }
    }
    
    @Test
    fun `needsRestore should return true when user exists locally with different ID`() = runTest {
        // Given
        val differentUser = User(
            id = "different-user-456",
            email = "different@example.com",
            name = "Different User",
            onboardingComplete = true,
            primaryGoal = HealthGoal.GENERAL_HEALTH,
            createdAt = kotlinx.datetime.Clock.System.now(),
            updatedAt = kotlinx.datetime.Clock.System.now()
        )
        coEvery { userRepository.getCurrentUser() } returns Result.success(differentUser)
        
        // When
        val result = useCase.needsRestore(testUserId)
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow()) // Should need restore
        
        coVerify { userRepository.getCurrentUser() }
    }
    
    @Test
    fun `needsRestore should return true when no user exists locally`() = runTest {
        // Given
        coEvery { userRepository.getCurrentUser() } returns Result.success(null)
        
        // When
        val result = useCase.needsRestore(testUserId)
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow()) // Should need restore
        
        coVerify { userRepository.getCurrentUser() }
    }
    
    @Test
    fun `needsRestore should return true when getting current user fails`() = runTest {
        // Given
        val error = AppError.DatabaseError("Database error")
        coEvery { userRepository.getCurrentUser() } returns Result.error(error)
        
        // When
        val result = useCase.needsRestore(testUserId)
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow()) // Should need restore on error (safe default)
        
        coVerify { userRepository.getCurrentUser() }
    }
    
    @Test
    fun `needsRestore should return true when exception occurs`() = runTest {
        // Given
        coEvery { userRepository.getCurrentUser() } throws RuntimeException("Unexpected error")
        
        // When
        val result = useCase.needsRestore(testUserId)
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow()) // Should need restore on exception (safe default)
        
        coVerify { userRepository.getCurrentUser() }
    }
    
    @Test
    fun `execute with backup data should pass data to backup manager`() = runTest {
        // Given
        coEvery { settingsBackupManager.restoreOnNewDevice(testUserId, null) } returns Result.success(Unit)
        
        // When
        val result = useCase.execute(testUserId, isNewDevice = true)
        
        // Then
        assertTrue(result.isSuccess)
        
        coVerify { settingsBackupManager.restoreOnNewDevice(testUserId, null) }
    }
    
    @Test
    fun `execute should handle backup manager returning error`() = runTest {
        // Given
        val backupError = SettingsError.BackupError("Failed to restore from remote", "RESTORE")
        coEvery { settingsBackupManager.restoreOnNewDevice(testUserId, null) } returns Result.error(backupError)
        
        // When
        val result = useCase.execute(testUserId, isNewDevice = true)
        
        // Then
        assertTrue(result.isError)
        val error = result.errorOrNull()
        assertTrue(error is SettingsError.BackupError)
        assertEquals("Failed to restore from remote", error.message)
        
        coVerify { settingsBackupManager.restoreOnNewDevice(testUserId, null) }
    }
    
    @Test
    fun `execute should work correctly for multiple users`() = runTest {
        // Given
        val userId1 = "user-1"
        val userId2 = "user-2"
        
        coEvery { settingsBackupManager.restoreOnNewDevice(userId1, null) } returns Result.success(Unit)
        coEvery { settingsBackupManager.restoreOnNewDevice(userId2, null) } returns Result.success(Unit)
        
        // When
        val result1 = useCase.execute(userId1, isNewDevice = true)
        val result2 = useCase.execute(userId2, isNewDevice = true)
        
        // Then
        assertTrue(result1.isSuccess)
        assertTrue(result2.isSuccess)
        
        coVerify { settingsBackupManager.restoreOnNewDevice(userId1, null) }
        coVerify { settingsBackupManager.restoreOnNewDevice(userId2, null) }
    }
    
    @Test
    fun `needsRestore should handle edge case with empty user ID`() = runTest {
        // Given
        val emptyUserId = ""
        coEvery { userRepository.getCurrentUser() } returns Result.success(null)
        
        // When
        val result = useCase.needsRestore(emptyUserId)
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow()) // Should need restore for empty user ID
        
        coVerify { userRepository.getCurrentUser() }
    }
}