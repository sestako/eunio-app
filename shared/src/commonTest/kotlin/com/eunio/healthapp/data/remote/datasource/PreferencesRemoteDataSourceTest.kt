package com.eunio.healthapp.data.remote.datasource

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.HealthGoal
import com.eunio.healthapp.domain.model.SyncStatus
import com.eunio.healthapp.domain.model.UnitSystem
import com.eunio.healthapp.domain.model.User
import com.eunio.healthapp.domain.model.UserPreferences
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Fake implementation of PreferencesRemoteDataSource for testing
 */
class FakePreferencesRemoteDataSource : PreferencesRemoteDataSource {
    private val storedPreferences = mutableMapOf<String, UserPreferences>()
    var shouldThrowException = false
    
    override suspend fun getPreferences(userId: String): Result<UserPreferences?> {
        if (shouldThrowException) return Result.error(AppError.NetworkError("Remote error"))
        return Result.success(storedPreferences[userId])
    }
    
    override suspend fun savePreferences(preferences: UserPreferences): Result<Unit> {
        if (shouldThrowException) return Result.error(AppError.NetworkError("Save failed"))
        storedPreferences[preferences.userId] = preferences
        return Result.success(Unit)
    }
    
    override suspend fun updatePreferences(preferences: UserPreferences): Result<Unit> {
        if (shouldThrowException) return Result.error(AppError.NetworkError("Update failed"))
        storedPreferences[preferences.userId] = preferences
        return Result.success(Unit)
    }
    
    override suspend fun deletePreferences(userId: String): Result<Unit> {
        if (shouldThrowException) return Result.error(AppError.NetworkError("Delete failed"))
        storedPreferences.remove(userId)
        return Result.success(Unit)
    }
    
    override suspend fun clearPreferences(): Result<Unit> {
        if (shouldThrowException) return Result.error(AppError.NetworkError("Clear failed"))
        storedPreferences.clear()
        return Result.success(Unit)
    }
    
    override suspend fun preferencesExist(userId: String): Result<Boolean> {
        if (shouldThrowException) return Result.error(AppError.NetworkError("Exist check failed"))
        return Result.success(storedPreferences.containsKey(userId))
    }
    
    override suspend fun getLastModifiedTimestamp(userId: String): Result<Long?> {
        if (shouldThrowException) return Result.error(AppError.NetworkError("Timestamp failed"))
        return Result.success(storedPreferences[userId]?.lastModified?.epochSeconds)
    }
    
    fun setStoredPreferences(preferences: UserPreferences) {
        storedPreferences[preferences.userId] = preferences
    }
    
    fun clear() {
        storedPreferences.clear()
    }
}

class PreferencesRemoteDataSourceTest {
    
    private val fakeDataSource = FakePreferencesRemoteDataSource()
    
    private val testUserId = "test-user-123"
    private val testTimestamp = Instant.fromEpochSeconds(Clock.System.now().epochSeconds)
    
    private val testUser = User(
        id = testUserId,
        email = "test@example.com",
        name = "Test User",
        onboardingComplete = true,
        primaryGoal = HealthGoal.CYCLE_TRACKING,
        unitSystem = UnitSystem.IMPERIAL,
        createdAt = testTimestamp,
        updatedAt = testTimestamp
    )
    
    private val testUserPreferences = UserPreferences(
        userId = testUserId,
        unitSystem = UnitSystem.IMPERIAL,
        isManuallySet = true,
        lastModified = testTimestamp,
        syncStatus = SyncStatus.PENDING
    )
    
    @Test
    fun `getPreferences returns preferences when user exists`() = runTest {
        // Given
        fakeDataSource.setStoredPreferences(testUserPreferences)
        
        // When
        val result = fakeDataSource.getPreferences(testUserId)
        
        // Then
        assertTrue(result is Result.Success)
        val preferences = result.getOrNull()
        assertEquals(testUserId, preferences?.userId)
        assertEquals(UnitSystem.IMPERIAL, preferences?.unitSystem)
        assertEquals(testTimestamp, preferences?.lastModified)
        assertEquals(SyncStatus.PENDING, preferences?.syncStatus)
    }
    
    @Test
    fun `getPreferences returns null when user does not exist`() = runTest {
        // Given - no preferences stored
        
        // When
        val result = fakeDataSource.getPreferences(testUserId)
        
        // Then
        assertTrue(result is Result.Success)
        assertNull(result.getOrNull())
    }
    
    @Test
    fun `getPreferences returns failure when service fails`() = runTest {
        // Given
        fakeDataSource.shouldThrowException = true
        
        // When
        val result = fakeDataSource.getPreferences(testUserId)
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue(result.errorOrNull()?.message?.contains("Remote error") == true)
    }
    
    @Test
    fun `savePreferences saves preferences successfully`() = runTest {
        // Given
        val newPreferences = testUserPreferences.copy(unitSystem = UnitSystem.METRIC)
        
        // When
        val result = fakeDataSource.savePreferences(newPreferences)
        
        // Then
        assertTrue(result is Result.Success)
        assertEquals(newPreferences, fakeDataSource.getPreferences(testUserId).getOrNull())
    }
    
    @Test
    fun `savePreferences fails when service fails`() = runTest {
        // Given
        fakeDataSource.shouldThrowException = true
        
        // When
        val result = fakeDataSource.savePreferences(testUserPreferences)
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue(result.errorOrNull()?.message?.contains("Save failed") == true)
    }
    
    @Test
    fun `updatePreferences works correctly`() = runTest {
        // Given
        val updatedPreferences = testUserPreferences.copy(unitSystem = UnitSystem.METRIC)
        
        // When
        val result = fakeDataSource.updatePreferences(updatedPreferences)
        
        // Then
        assertTrue(result is Result.Success)
        assertEquals(updatedPreferences, fakeDataSource.getPreferences(testUserId).getOrNull())
    }
    
    @Test
    fun `deletePreferences removes preferences`() = runTest {
        // Given
        fakeDataSource.setStoredPreferences(testUserPreferences)
        
        // When
        val result = fakeDataSource.deletePreferences(testUserId)
        
        // Then
        assertTrue(result is Result.Success)
        assertNull(fakeDataSource.getPreferences(testUserId).getOrNull())
    }
    
    @Test
    fun `clearPreferences removes all preferences`() = runTest {
        // Given
        fakeDataSource.setStoredPreferences(testUserPreferences)
        
        // When
        val result = fakeDataSource.clearPreferences()
        
        // Then
        assertTrue(result is Result.Success)
        assertNull(fakeDataSource.getPreferences(testUserId).getOrNull())
    }
    
    @Test
    fun `preferencesExist returns true when preferences exist`() = runTest {
        // Given
        fakeDataSource.setStoredPreferences(testUserPreferences)
        
        // When
        val result = fakeDataSource.preferencesExist(testUserId)
        
        // Then
        assertTrue(result is Result.Success)
        assertEquals(true, result.getOrNull())
    }
    
    @Test
    fun `preferencesExist returns false when preferences do not exist`() = runTest {
        // Given - no preferences stored
        
        // When
        val result = fakeDataSource.preferencesExist(testUserId)
        
        // Then
        assertTrue(result is Result.Success)
        assertEquals(false, result.getOrNull())
    }
    
    @Test
    fun `getLastModifiedTimestamp returns correct timestamp`() = runTest {
        // Given
        fakeDataSource.setStoredPreferences(testUserPreferences)
        
        // When
        val result = fakeDataSource.getLastModifiedTimestamp(testUserId)
        
        // Then
        assertTrue(result is Result.Success)
        assertEquals(testTimestamp.epochSeconds, result.getOrNull())
    }
    
    @Test
    fun `getLastModifiedTimestamp returns null when user does not exist`() = runTest {
        // Given - no preferences stored
        
        // When
        val result = fakeDataSource.getLastModifiedTimestamp(testUserId)
        
        // Then
        assertTrue(result is Result.Success)
        assertNull(result.getOrNull())
    }
}