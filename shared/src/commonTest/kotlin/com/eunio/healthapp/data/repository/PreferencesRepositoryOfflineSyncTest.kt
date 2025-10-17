package com.eunio.healthapp.data.repository

import com.eunio.healthapp.data.local.datasource.PreferencesLocalDataSource
import com.eunio.healthapp.data.remote.datasource.PreferencesRemoteDataSource
import com.eunio.healthapp.domain.error.UnitSystemError
import com.eunio.healthapp.domain.model.SyncStatus
import com.eunio.healthapp.domain.model.UnitSystem
import com.eunio.healthapp.domain.model.User
import com.eunio.healthapp.domain.model.UserPreferences
import com.eunio.healthapp.domain.repository.UserRepository
import com.eunio.healthapp.domain.util.ErrorHandler
import com.eunio.healthapp.domain.util.NetworkConnectivity
import com.eunio.healthapp.domain.util.NetworkType
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.domain.util.UnitSystemErrorHandler
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.*
import kotlin.time.Duration.Companion.hours

class PreferencesRepositoryOfflineSyncTest {
    
    private lateinit var mockLocalDataSource: MockPreferencesLocalDataSourceForOfflineSync
    private lateinit var mockRemoteDataSource: MockPreferencesRemoteDataSourceForOfflineSync
    private lateinit var mockNetworkConnectivity: MockNetworkConnectivityForRepo
    private lateinit var mockUserRepository: MockUserRepositoryForRepo
    private lateinit var errorHandler: ErrorHandler
    private lateinit var unitSystemErrorHandler: UnitSystemErrorHandler
    private lateinit var repository: PreferencesRepositoryImpl
    
    private val testUserId = "test-user-123"
    private val testUser = User(
        id = testUserId,
        email = "test@example.com",
        name = "Test User",
        onboardingComplete = true,
        primaryGoal = com.eunio.healthapp.domain.model.HealthGoal.CYCLE_TRACKING,
        unitSystem = UnitSystem.METRIC,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now()
    )
    
    @BeforeTest
    fun setup() {
        mockLocalDataSource = MockPreferencesLocalDataSourceForOfflineSync()
        mockRemoteDataSource = MockPreferencesRemoteDataSourceForOfflineSync()
        mockNetworkConnectivity = MockNetworkConnectivityForRepo()
        mockUserRepository = MockUserRepositoryForRepo()
        errorHandler = ErrorHandler()
        unitSystemErrorHandler = UnitSystemErrorHandler()
        
        mockUserRepository.currentUser = testUser
        
        repository = PreferencesRepositoryImpl(
            localDataSource = mockLocalDataSource,
            remoteDataSource = mockRemoteDataSource,
            networkConnectivity = mockNetworkConnectivity,
            userRepository = mockUserRepository,
            errorHandler = errorHandler,
            unitSystemErrorHandler = unitSystemErrorHandler
        )
    }
    
    @Test
    fun `syncWithConflictResolution should handle local-only preferences`() = runTest {
        // Given
        val localPrefs = createTestPreferences(
            unitSystem = UnitSystem.IMPERIAL,
            isManuallySet = true,
            lastModified = Clock.System.now()
        )
        mockLocalDataSource.preferences[testUserId] = localPrefs
        mockRemoteDataSource.preferences[testUserId] = null
        mockNetworkConnectivity.setConnected(true)
        
        // When
        val result = repository.syncWithConflictResolution(testUserId)
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(mockRemoteDataSource.savePreferencesCalled)
        assertEquals(SyncStatus.SYNCED, mockLocalDataSource.preferences[testUserId]?.syncStatus)
    }
    
    @Test
    fun `syncWithConflictResolution should handle remote-only preferences`() = runTest {
        // Given
        val remotePrefs = createTestPreferences(
            unitSystem = UnitSystem.IMPERIAL,
            isManuallySet = true,
            lastModified = Clock.System.now()
        )
        mockLocalDataSource.preferences[testUserId] = null
        mockRemoteDataSource.preferences[testUserId] = remotePrefs
        mockNetworkConnectivity.setConnected(true)
        
        // When
        val result = repository.syncWithConflictResolution(testUserId)
        
        // Then
        assertTrue(result.isSuccess)
        assertNotNull(mockLocalDataSource.preferences[testUserId])
        assertEquals(UnitSystem.IMPERIAL, mockLocalDataSource.preferences[testUserId]?.unitSystem)
        assertEquals(SyncStatus.SYNCED, mockLocalDataSource.preferences[testUserId]?.syncStatus)
    }
    
    @Test
    fun `syncWithConflictResolution should resolve conflicts using last-write-wins`() = runTest {
        // Given
        val now = Clock.System.now()
        val olderTime = now - 1.hours
        
        val localPrefs = createTestPreferences(
            unitSystem = UnitSystem.METRIC,
            isManuallySet = true,
            lastModified = olderTime
        )
        val remotePrefs = createTestPreferences(
            unitSystem = UnitSystem.IMPERIAL,
            isManuallySet = true,
            lastModified = now
        )
        
        mockLocalDataSource.preferences[testUserId] = localPrefs
        mockRemoteDataSource.preferences[testUserId] = remotePrefs
        mockNetworkConnectivity.setConnected(true)
        
        // When
        val result = repository.syncWithConflictResolution(testUserId)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(UnitSystem.IMPERIAL, mockLocalDataSource.preferences[testUserId]?.unitSystem)
        assertTrue(mockRemoteDataSource.savePreferencesCalled)
    }
    
    @Test
    fun `syncWithConflictResolution should prefer manual over automatic when timestamps equal`() = runTest {
        // Given
        val now = Clock.System.now()
        
        val localPrefs = createTestPreferences(
            unitSystem = UnitSystem.METRIC,
            isManuallySet = true,
            lastModified = now
        )
        val remotePrefs = createTestPreferences(
            unitSystem = UnitSystem.IMPERIAL,
            isManuallySet = false,
            lastModified = now
        )
        
        mockLocalDataSource.preferences[testUserId] = localPrefs
        mockRemoteDataSource.preferences[testUserId] = remotePrefs
        mockNetworkConnectivity.setConnected(true)
        
        // When
        val result = repository.syncWithConflictResolution(testUserId)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(UnitSystem.METRIC, mockLocalDataSource.preferences[testUserId]?.unitSystem)
        assertTrue(mockLocalDataSource.preferences[testUserId]?.isManuallySet == true)
    }
    
    @Test
    fun `syncWithConflictResolution should fail when offline`() = runTest {
        // Given
        mockNetworkConnectivity.setConnected(false)
        
        // When
        val result = repository.syncWithConflictResolution(testUserId)
        
        // Then
        assertTrue(result.isError)
        val error = result.errorOrNull()
        assertTrue(error is UnitSystemError.UnitSystemNetworkError)
    }
    
    @Test
    fun `handleOfflineMode should mark pending preferences correctly`() = runTest {
        // Given
        val prefs1 = createTestPreferences(syncStatus = SyncStatus.FAILED)
        val prefs2 = createTestPreferences(syncStatus = SyncStatus.SYNCED)
        
        mockLocalDataSource.pendingPreferences = listOf(prefs1, prefs2)
        
        // When
        val result = repository.handleOfflineMode()
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(mockLocalDataSource.savePreferencesCalled)
    }
    
    @Test
    fun `getSyncStatistics should return correct metrics`() = runTest {
        // Given
        val pendingPrefs = listOf(
            createTestPreferences(syncStatus = SyncStatus.PENDING),
            createTestPreferences(syncStatus = SyncStatus.FAILED)
        )
        mockLocalDataSource.pendingPreferences = pendingPrefs
        mockNetworkConnectivity.setConnected(true)
        mockNetworkConnectivity.setNetworkType(NetworkType.WIFI)
        
        // When
        val result = repository.getSyncStatistics()
        
        // Then
        assertTrue(result.isSuccess)
        val stats = result.getOrNull()!!
        assertEquals(2, stats.pendingSyncCount)
        assertTrue(stats.isConnected)
        assertEquals("WIFI", stats.networkType)
    }
    
    @Test
    fun `saveUserPreferences should handle offline mode gracefully`() = runTest {
        // Given
        val preferences = createTestPreferences()
        mockNetworkConnectivity.setConnected(false)
        
        // When
        val result = repository.saveUserPreferences(preferences)
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(mockLocalDataSource.savePreferencesCalled)
        assertFalse(mockRemoteDataSource.savePreferencesCalled)
    }
    
    @Test
    fun `saveUserPreferences should retry on network failure`() = runTest {
        // Given
        val preferences = createTestPreferences()
        mockNetworkConnectivity.setConnected(true)
        mockRemoteDataSource.shouldFailSave = true
        mockRemoteDataSource.saveError = UnitSystemError.UnitSystemNetworkError(
            message = "Network timeout",
            operation = "save"
        )
        
        // When
        val result = repository.saveUserPreferences(preferences)
        
        // Then
        assertTrue(result.isSuccess) // Local save should succeed
        assertTrue(mockLocalDataSource.savePreferencesCalled)
        assertTrue(mockRemoteDataSource.savePreferencesCalled)
        assertEquals(SyncStatus.FAILED, mockLocalDataSource.preferences[testUserId]?.syncStatus)
    }
    
    private fun createTestPreferences(
        unitSystem: UnitSystem = UnitSystem.METRIC,
        isManuallySet: Boolean = false,
        lastModified: Instant = Clock.System.now(),
        syncStatus: SyncStatus = SyncStatus.PENDING
    ): UserPreferences {
        return UserPreferences(
            userId = testUserId,
            unitSystem = unitSystem,
            isManuallySet = isManuallySet,
            lastModified = lastModified,
            syncStatus = syncStatus
        )
    }
}

class MockPreferencesLocalDataSourceForOfflineSync : PreferencesLocalDataSource {
    val preferences = mutableMapOf<String, UserPreferences?>()
    var pendingPreferences = listOf<UserPreferences>()
    var savePreferencesCalled = false
    var markAsSyncedCalled = false
    var markAsFailedCalled = false
    
    override suspend fun getPreferences(): UserPreferences? = null
    
    override suspend fun getPreferences(userId: String): UserPreferences? {
        return preferences[userId]
    }
    
    override suspend fun savePreferences(preferences: UserPreferences): Result<Unit> {
        savePreferencesCalled = true
        this.preferences[preferences.userId] = preferences
        return Result.success(Unit)
    }
    
    override suspend fun getPendingSyncPreferences(): List<UserPreferences> {
        return pendingPreferences
    }
    
    override suspend fun markAsSynced(userId: String): Result<Unit> {
        markAsSyncedCalled = true
        preferences[userId] = preferences[userId]?.copy(syncStatus = SyncStatus.SYNCED)
        return Result.success(Unit)
    }
    
    override suspend fun markAsFailed(userId: String): Result<Unit> {
        markAsFailedCalled = true
        preferences[userId] = preferences[userId]?.copy(syncStatus = SyncStatus.FAILED)
        return Result.success(Unit)
    }
    
    override suspend fun clearPreferences(): Result<Unit> {
        preferences.clear()
        return Result.success(Unit)
    }
    
    override suspend fun clearPreferences(userId: String): Result<Unit> {
        preferences.remove(userId)
        return Result.success(Unit)
    }
}

class MockPreferencesRemoteDataSourceForOfflineSync : PreferencesRemoteDataSource {
    val preferences = mutableMapOf<String, UserPreferences?>()
    var savePreferencesCalled = false
    var shouldFailSave = false
    var saveError: UnitSystemError? = null
    
    override suspend fun getPreferences(userId: String): Result<UserPreferences?> {
        return Result.success(preferences[userId])
    }
    
    override suspend fun savePreferences(preferences: UserPreferences): Result<Unit> {
        savePreferencesCalled = true
        
        if (shouldFailSave) {
            return Result.error(saveError ?: UnitSystemError.PreferencesSyncError(
                message = "Save failed",
                operation = "save"
            ))
        }
        
        this.preferences[preferences.userId] = preferences
        return Result.success(Unit)
    }
    
    override suspend fun updatePreferences(preferences: UserPreferences): Result<Unit> {
        return savePreferences(preferences)
    }
    
    override suspend fun deletePreferences(userId: String): Result<Unit> {
        preferences.remove(userId)
        return Result.success(Unit)
    }
    
    override suspend fun clearPreferences(): Result<Unit> {
        preferences.clear()
        return Result.success(Unit)
    }
    
    override suspend fun preferencesExist(userId: String): Result<Boolean> {
        return Result.success(preferences.containsKey(userId))
    }
    
    override suspend fun getLastModifiedTimestamp(userId: String): Result<Long?> {
        val prefs = preferences[userId]
        return Result.success(prefs?.lastModified?.epochSeconds)
    }
}

class MockNetworkConnectivityForRepo : NetworkConnectivity {
    private var _isConnected = true
    private var _networkType = NetworkType.WIFI
    
    override fun isConnected(): Boolean = _isConnected
    
    override fun observeConnectivity() = kotlinx.coroutines.flow.flowOf(_isConnected)
    
    override suspend fun hasStableConnection(): Boolean = _isConnected
    
    override fun getNetworkType(): NetworkType = _networkType
    
    fun setConnected(connected: Boolean) {
        _isConnected = connected
    }
    
    fun setNetworkType(type: NetworkType) {
        _networkType = type
    }
}

class MockUserRepositoryForRepo : UserRepository {
    var currentUser: User? = null
    
    override suspend fun getCurrentUser(): Result<User?> {
        return Result.success(currentUser)
    }
    
    override suspend fun updateUser(user: User): Result<Unit> {
        currentUser = user
        return Result.success(Unit)
    }
    
    override suspend fun completeOnboarding(userId: String, primaryGoal: com.eunio.healthapp.domain.model.HealthGoal): Result<Unit> {
        currentUser = currentUser?.copy(
            primaryGoal = primaryGoal,
            onboardingComplete = true
        )
        return Result.success(Unit)
    }
    
    override suspend fun createUser(email: String, password: String, name: String): Result<User> {
        val user = User(
            id = "test-user-id",
            email = email,
            name = name,
            onboardingComplete = false,
            primaryGoal = com.eunio.healthapp.domain.model.HealthGoal.CYCLE_TRACKING,
            unitSystem = com.eunio.healthapp.domain.model.UnitSystem.METRIC,
            createdAt = kotlinx.datetime.Clock.System.now(),
            updatedAt = kotlinx.datetime.Clock.System.now()
        )
        currentUser = user
        return Result.success(user)
    }
    
    override suspend fun signInUser(email: String, password: String): Result<User> {
        return currentUser?.let { Result.success(it) } 
            ?: Result.error(com.eunio.healthapp.domain.error.AppError.AuthenticationError("User not found"))
    }
    
    override suspend fun signOutUser(): Result<Unit> {
        currentUser = null
        return Result.success(Unit)
    }
    
    override suspend fun deleteUser(userId: String): Result<Unit> {
        if (currentUser?.id == userId) {
            currentUser = null
        }
        return Result.success(Unit)
    }
}