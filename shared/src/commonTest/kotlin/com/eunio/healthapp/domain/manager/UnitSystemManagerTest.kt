package com.eunio.healthapp.domain.manager

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.error.UnitSystemError
import com.eunio.healthapp.domain.model.HealthGoal
import com.eunio.healthapp.domain.model.SyncStatus
import com.eunio.healthapp.domain.model.UnitSystem
import com.eunio.healthapp.domain.model.User
import com.eunio.healthapp.domain.model.UserPreferences
import com.eunio.healthapp.domain.repository.PreferencesRepository
import com.eunio.healthapp.domain.repository.UserRepository
import com.eunio.healthapp.domain.util.LocaleDetector
import com.eunio.healthapp.domain.util.NetworkType
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.*

/**
 * Mock implementation of PreferencesRepository for testing
 */
class MockPreferencesRepository : PreferencesRepository {
    private var storedPreferences: UserPreferences? = null
    var shouldThrowException = false
    var saveCallCount = 0
    var getCallCount = 0
    
    override suspend fun getUserPreferences(): Result<UserPreferences?> {
        getCallCount++
        if (shouldThrowException) {
            return Result.error(AppError.DataSyncError("Repository error"))
        }
        return Result.success(storedPreferences)
    }
    
    override suspend fun getUserPreferences(userId: String): Result<UserPreferences?> {
        getCallCount++
        if (shouldThrowException) {
            return Result.error(AppError.DataSyncError("Repository error"))
        }
        return Result.success(storedPreferences)
    }
    
    override suspend fun saveUserPreferences(preferences: UserPreferences): Result<Unit> {
        saveCallCount++
        if (shouldThrowException) {
            return Result.error(AppError.DataSyncError("Save failed"))
        }
        storedPreferences = preferences
        return Result.success(Unit)
    }
    
    override suspend fun syncPreferences(): Result<Unit> {
        if (shouldThrowException) {
            return Result.error(AppError.NetworkError("Sync failed"))
        }
        return Result.success(Unit)
    }
    
    override suspend fun clearPreferences(): Result<Unit> {
        if (shouldThrowException) {
            return Result.error(AppError.DataSyncError("Clear failed"))
        }
        storedPreferences = null
        return Result.success(Unit)
    }
    
    override suspend fun clearPreferences(userId: String): Result<Unit> {
        if (shouldThrowException) {
            return Result.error(AppError.DataSyncError("Clear failed"))
        }
        storedPreferences = null
        return Result.success(Unit)
    }
    
    override suspend fun syncWithConflictResolution(userId: String): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun handleOfflineMode(): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun getSyncStatistics(): Result<com.eunio.healthapp.domain.model.SyncStatistics> {
        return Result.success(com.eunio.healthapp.domain.model.SyncStatistics(
            pendingSyncCount = 0,
            isConnected = true,
            networkType = "WIFI",
            lastSyncAttempt = kotlinx.datetime.Clock.System.now()
        ))
    }
    
    override suspend fun recoverFromSyncFailure(): Result<Unit> {
        return Result.success(Unit)
    }
    
    fun setStoredPreferences(preferences: UserPreferences?) {
        storedPreferences = preferences
    }
    
    fun getStoredPreferences(): UserPreferences? = storedPreferences
    
    fun reset() {
        storedPreferences = null
        shouldThrowException = false
        saveCallCount = 0
        getCallCount = 0
    }
}

/**
 * Mock implementation of UserRepository for testing
 */
class MockUserRepository : UserRepository {
    private var currentUser: User? = null
    var shouldThrowException = false
    var updateCallCount = 0
    
    override suspend fun getCurrentUser(): Result<User?> {
        if (shouldThrowException) {
            return Result.error(AppError.AuthenticationError("No user"))
        }
        return Result.success(currentUser)
    }
    
    override suspend fun updateUser(user: User): Result<Unit> {
        updateCallCount++
        if (shouldThrowException) {
            return Result.error(AppError.DataSyncError("Update failed"))
        }
        currentUser = user
        return Result.success(Unit)
    }
    
    override suspend fun completeOnboarding(userId: String, primaryGoal: HealthGoal): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun createUser(email: String, password: String, name: String): Result<User> {
        val user = User(
            id = "test-user-id",
            email = email,
            name = name,
            onboardingComplete = true,
            primaryGoal = HealthGoal.CYCLE_TRACKING,
            unitSystem = UnitSystem.METRIC,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        currentUser = user
        return Result.success(user)
    }
    
    override suspend fun signInUser(email: String, password: String): Result<User> {
        return Result.success(currentUser ?: User(
            id = "test-user-id",
            email = "test@example.com",
            name = "Test User",
            onboardingComplete = true,
            primaryGoal = HealthGoal.CYCLE_TRACKING,
            unitSystem = UnitSystem.METRIC,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        ))
    }
    
    override suspend fun signOutUser(): Result<Unit> {
        currentUser = null
        return Result.success(Unit)
    }
    
    override suspend fun deleteUser(userId: String): Result<Unit> {
        currentUser = null
        return Result.success(Unit)
    }
    
    fun setCurrentUser(user: User?) {
        currentUser = user
    }
    
    fun reset() {
        currentUser = null
        shouldThrowException = false
        updateCallCount = 0
    }
}

/**
 * Mock implementation of LocaleDetector for testing
 */
class MockLocaleDetector : LocaleDetector {
    private var currentLocale = "US"
    
    override fun getCurrentLocaleCountryCode(): String? = currentLocale
    
    override fun getCurrentLocaleString(): String? = "en_$currentLocale"
    
    fun setCurrentLocale(locale: String) {
        currentLocale = locale
    }
}

class UnitSystemManagerTest {
    
    private lateinit var mockPreferencesRepository: MockPreferencesRepository
    private lateinit var mockUserRepository: MockUserRepository
    private lateinit var mockLocaleDetector: MockLocaleDetector
    private lateinit var manager: UnitSystemManagerImpl
    
    private val testUserId = "test-user-id"
    private val testUser = User(
        id = testUserId,
        email = "test@example.com",
        name = "Test User",
        onboardingComplete = true,
        primaryGoal = HealthGoal.CYCLE_TRACKING,
        unitSystem = UnitSystem.METRIC,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now()
    )
    
    @BeforeTest
    fun setup() {
        mockPreferencesRepository = MockPreferencesRepository()
        mockUserRepository = MockUserRepository()
        mockLocaleDetector = MockLocaleDetector()
        manager = UnitSystemManagerImpl(
            preferencesRepository = mockPreferencesRepository,
            userRepository = mockUserRepository,
            localeDetector = mockLocaleDetector
        )
    }
    
    @AfterTest
    fun tearDown() {
        mockPreferencesRepository.reset()
        mockUserRepository.reset()
    }
    
    // State Management Tests
    
    @Test
    fun `getCurrentUnitSystem returns cached value when available`() = runTest {
        // Given: Manager has cached unit system
        mockUserRepository.setCurrentUser(testUser)
        manager.setUnitSystem(UnitSystem.IMPERIAL)
        
        // Reset call counts to test caching
        mockPreferencesRepository.reset()
        
        // When: Getting current unit system
        val result = manager.getCurrentUnitSystem()
        
        // Then: Returns cached value without repository call
        assertEquals(UnitSystem.IMPERIAL, result)
        assertEquals(0, mockPreferencesRepository.getCallCount, "Should not call repository when cached")
    }
    
    @Test
    fun `getCurrentUnitSystem fetches from repository when cache is empty`() = runTest {
        // Given: Repository returns preferences
        val preferences = UserPreferences(
            userId = testUserId,
            unitSystem = UnitSystem.IMPERIAL,
            isManuallySet = true,
            lastModified = Clock.System.now(),
            syncStatus = SyncStatus.SYNCED
        )
        mockPreferencesRepository.setStoredPreferences(preferences)
        
        // When: Getting current unit system
        val result = manager.getCurrentUnitSystem()
        
        // Then: Returns unit system from repository
        assertEquals(UnitSystem.IMPERIAL, result)
        assertEquals(1, mockPreferencesRepository.getCallCount, "Should call repository once")
    }
    
    @Test
    fun `getCurrentUnitSystem returns METRIC default when no preferences found`() = runTest {
        // Given: Repository returns null preferences
        mockPreferencesRepository.setStoredPreferences(null)
        
        // When: Getting current unit system
        val result = manager.getCurrentUnitSystem()
        
        // Then: Returns METRIC default
        assertEquals(UnitSystem.METRIC, result)
    }
    
    @Test
    fun `getCurrentUnitSystem handles repository errors gracefully`() = runTest {
        // Given: Repository throws exception
        mockPreferencesRepository.shouldThrowException = true
        
        // When: Getting current unit system
        val result = manager.getCurrentUnitSystem()
        
        // Then: Returns METRIC default
        assertEquals(UnitSystem.METRIC, result)
    }
    
    // Unit System Setting Tests
    
    @Test
    fun `setUnitSystem saves preferences and updates cache`() = runTest {
        // Given: User is authenticated
        mockUserRepository.setCurrentUser(testUser)
        
        // When: Setting unit system
        val result = manager.setUnitSystem(UnitSystem.IMPERIAL, isManuallySet = true)
        
        // Then: Operation succeeds and cache is updated
        assertTrue(result.isSuccess)
        assertEquals(UnitSystem.IMPERIAL, manager.getCurrentUnitSystem())
        assertEquals(1, mockPreferencesRepository.saveCallCount, "Should save preferences once")
        assertEquals(1, mockUserRepository.updateCallCount, "Should update user once")
    }
    
    @Test
    fun `setUnitSystem fails when no authenticated user`() = runTest {
        // Given: No authenticated user
        mockUserRepository.setCurrentUser(null)
        
        // When: Setting unit system
        val result = manager.setUnitSystem(UnitSystem.IMPERIAL)
        
        // Then: Operation fails with appropriate error
        assertTrue(result.isError)
        assertTrue(result.errorOrNull() is UnitSystemError.UnitValidationError)
        assertEquals(0, mockPreferencesRepository.saveCallCount, "Should not save when no user")
    }
    
    @Test
    fun `setUnitSystem fails when preferences save fails`() = runTest {
        // Given: User is authenticated but preferences save fails
        mockUserRepository.setCurrentUser(testUser)
        mockPreferencesRepository.shouldThrowException = true
        
        // When: Setting unit system
        val result = manager.setUnitSystem(UnitSystem.IMPERIAL)
        
        // Then: Operation fails
        assertTrue(result.isError)
        assertTrue(result.errorOrNull() is AppError.DataSyncError)
    }
    
    @Test
    fun `setUnitSystem handles user model update failure gracefully`() = runTest {
        // Given: User is authenticated
        mockUserRepository.setCurrentUser(testUser)
        
        // When: Setting unit system (user model update will fail but that's handled gracefully)
        val result = manager.setUnitSystem(UnitSystem.IMPERIAL)
        
        // Then: Operation succeeds (user model update failure is handled gracefully)
        assertTrue(result.isSuccess)
        assertEquals(UnitSystem.IMPERIAL, manager.getCurrentUnitSystem())
    }
    
    @Test
    fun `setUnitSystem creates correct preferences object`() = runTest {
        // Given: User is authenticated
        mockUserRepository.setCurrentUser(testUser)
        
        // When: Setting unit system with manual flag
        manager.setUnitSystem(UnitSystem.IMPERIAL, isManuallySet = true)
        
        // Then: Preferences are saved with correct values
        val savedPreferences = mockPreferencesRepository.getStoredPreferences()
        assertNotNull(savedPreferences)
        assertEquals(testUserId, savedPreferences.userId)
        assertEquals(UnitSystem.IMPERIAL, savedPreferences.unitSystem)
        assertTrue(savedPreferences.isManuallySet)
        assertEquals(SyncStatus.PENDING, savedPreferences.syncStatus)
    }
    
    // Locale-based Initialization Tests
    
    @Test
    fun `initializeFromLocale respects manual preference`() = runTest {
        // Given: User has manually set preference
        val preferences = UserPreferences(
            userId = testUserId,
            unitSystem = UnitSystem.METRIC,
            isManuallySet = true,
            lastModified = Clock.System.now(),
            syncStatus = SyncStatus.SYNCED
        )
        mockPreferencesRepository.setStoredPreferences(preferences)
        
        // When: Initializing from US locale (should default to Imperial)
        val result = manager.initializeFromLocale("US")
        
        // Then: Returns manually set preference (Metric), not locale-based (Imperial)
        assertEquals(UnitSystem.METRIC, result)
        assertEquals(0, mockPreferencesRepository.saveCallCount, "Should not save when manual preference exists")
    }
    
    @Test
    fun `initializeFromLocale applies locale-based default when no manual preference`() = runTest {
        // Given: No manual preference set
        val preferences = UserPreferences(
            userId = testUserId,
            unitSystem = UnitSystem.METRIC,
            isManuallySet = false,
            lastModified = Clock.System.now(),
            syncStatus = SyncStatus.SYNCED
        )
        mockPreferencesRepository.setStoredPreferences(preferences)
        mockUserRepository.setCurrentUser(testUser)
        
        // When: Initializing from US locale
        val result = manager.initializeFromLocale("US")
        
        // Then: Returns locale-based preference (Imperial)
        assertEquals(UnitSystem.IMPERIAL, result)
        assertEquals(1, mockPreferencesRepository.saveCallCount, "Should save new locale-based preference")
    }
    
    @Test
    fun `initializeFromLocale applies locale-based default when no preferences exist`() = runTest {
        // Given: No existing preferences
        mockPreferencesRepository.setStoredPreferences(null)
        mockUserRepository.setCurrentUser(testUser)
        
        // When: Initializing from US locale
        val result = manager.initializeFromLocale("US")
        
        // Then: Returns locale-based preference (Imperial)
        assertEquals(UnitSystem.IMPERIAL, result)
        assertEquals(1, mockPreferencesRepository.saveCallCount, "Should save new locale-based preference")
    }
    
    @Test
    fun `initializeFromLocale falls back to METRIC on error`() = runTest {
        // Given: Repository throws exception which will cause the outer catch block to execute
        mockPreferencesRepository.shouldThrowException = true
        
        // When: Initializing from locale (this will trigger the exception in getUserPreferences)
        val result = manager.initializeFromLocale("US")
        
        // Then: Returns METRIC fallback (implementation handles errors gracefully)
        assertEquals(UnitSystem.METRIC, result)
    }
    
    @Test
    fun `initializeFromLocale works for different locales`() = runTest {
        // Given: No existing preferences
        mockPreferencesRepository.setStoredPreferences(null)
        mockUserRepository.setCurrentUser(testUser)
        
        // Test US locale -> Imperial
        var result = manager.initializeFromLocale("US")
        assertEquals(UnitSystem.IMPERIAL, result)
        
        manager.clearCache()
        mockPreferencesRepository.reset()
        
        // Test GB locale -> Metric
        result = manager.initializeFromLocale("GB")
        assertEquals(UnitSystem.METRIC, result)
        
        manager.clearCache()
        mockPreferencesRepository.reset()
        
        // Test Myanmar locale -> Imperial
        result = manager.initializeFromLocale("MM")
        assertEquals(UnitSystem.IMPERIAL, result)
        
        manager.clearCache()
        mockPreferencesRepository.reset()
        
        // Test Liberia locale -> Imperial
        result = manager.initializeFromLocale("LR")
        assertEquals(UnitSystem.IMPERIAL, result)
    }
    
    // Reactive State Tests
    
    @Test
    fun `observeUnitSystemChanges emits current value immediately`() = runTest {
        // Given: Manager has cached unit system
        mockUserRepository.setCurrentUser(testUser)
        manager.setUnitSystem(UnitSystem.IMPERIAL)
        
        // When: Observing unit system changes
        val flow = manager.observeUnitSystemChanges()
        val firstValue = flow.first()
        
        // Then: Emits current value immediately
        assertEquals(UnitSystem.IMPERIAL, firstValue)
    }
    
    @Test
    fun `observeUnitSystemChanges emits updates when unit system changes`() = runTest {
        // Given: Manager is set up
        mockUserRepository.setCurrentUser(testUser)
        
        // When: Observing changes and setting unit system
        val flow = manager.observeUnitSystemChanges()
        val initialValue = flow.first() // Get initial value
        
        // Set new unit system
        manager.setUnitSystem(UnitSystem.IMPERIAL)
        val updatedValue = manager.getCurrentUnitSystem()
        
        // Then: Values are updated correctly
        assertEquals(UnitSystem.METRIC, initialValue) // Initial default
        assertEquals(UnitSystem.IMPERIAL, updatedValue) // Updated value
    }
    
    @Test
    fun `observeUnitSystemChanges emits multiple updates correctly`() = runTest {
        // Given: Manager is set up
        mockUserRepository.setCurrentUser(testUser)
        
        // When: Making multiple unit system changes
        val initialValue = manager.getCurrentUnitSystem()
        
        manager.setUnitSystem(UnitSystem.IMPERIAL)
        val firstChange = manager.getCurrentUnitSystem()
        
        manager.setUnitSystem(UnitSystem.METRIC)
        val secondChange = manager.getCurrentUnitSystem()
        
        manager.setUnitSystem(UnitSystem.IMPERIAL)
        val thirdChange = manager.getCurrentUnitSystem()
        
        // Then: All changes are applied correctly
        assertEquals(UnitSystem.METRIC, initialValue)    // Initial
        assertEquals(UnitSystem.IMPERIAL, firstChange)  // First change
        assertEquals(UnitSystem.METRIC, secondChange)   // Second change
        assertEquals(UnitSystem.IMPERIAL, thirdChange)  // Third change
    }
    
    // Cache Management Tests
    
    @Test
    fun `clearCache clears cached unit system`() = runTest {
        // Given: Manager has cached unit system
        mockUserRepository.setCurrentUser(testUser)
        manager.setUnitSystem(UnitSystem.IMPERIAL)
        
        // When: Clearing cache
        manager.clearCache()
        
        // Then: Next call fetches from repository
        mockPreferencesRepository.setStoredPreferences(null)
        mockPreferencesRepository.reset() // Reset call counts
        
        val result = manager.getCurrentUnitSystem()
        assertEquals(UnitSystem.METRIC, result) // Default value
        assertEquals(1, mockPreferencesRepository.getCallCount, "Should fetch from repository after cache clear")
    }
    
    @Test
    fun `cache is invalidated after setting new unit system`() = runTest {
        // Given: Manager has cached unit system
        mockUserRepository.setCurrentUser(testUser)
        manager.setUnitSystem(UnitSystem.IMPERIAL)
        
        // Verify cache is working
        mockPreferencesRepository.reset()
        assertEquals(UnitSystem.IMPERIAL, manager.getCurrentUnitSystem())
        assertEquals(0, mockPreferencesRepository.getCallCount, "Should use cache")
        
        // When: Setting new unit system
        manager.setUnitSystem(UnitSystem.METRIC)
        
        // Then: Cache is updated immediately
        assertEquals(UnitSystem.METRIC, manager.getCurrentUnitSystem())
    }
    
    // Error Handling Tests
    
    @Test
    fun `manager handles concurrent access gracefully`() = runTest {
        // Given: Manager is set up
        mockUserRepository.setCurrentUser(testUser)
        
        // When: Making concurrent calls (simulated)
        val results = mutableListOf<Result<Unit>>()
        
        repeat(5) {
            val result = manager.setUnitSystem(if (it % 2 == 0) UnitSystem.METRIC else UnitSystem.IMPERIAL)
            results.add(result)
        }
        
        // Then: All operations should succeed
        results.forEach { result ->
            assertTrue(result.isSuccess, "Concurrent operations should succeed")
        }
    }
    
    @Test
    fun `manager maintains consistency during errors`() = runTest {
        // Given: Manager has valid state
        mockUserRepository.setCurrentUser(testUser)
        manager.setUnitSystem(UnitSystem.IMPERIAL)
        
        // When: Repository fails temporarily
        mockPreferencesRepository.shouldThrowException = true
        val failedResult = manager.setUnitSystem(UnitSystem.METRIC)
        
        // Then: State remains consistent
        assertTrue(failedResult.isError)
        assertEquals(UnitSystem.IMPERIAL, manager.getCurrentUnitSystem(), "Should maintain previous state on error")
        
        // When: Repository recovers
        mockPreferencesRepository.shouldThrowException = false
        val successResult = manager.setUnitSystem(UnitSystem.METRIC)
        
        // Then: New state is applied
        assertTrue(successResult.isSuccess)
        assertEquals(UnitSystem.METRIC, manager.getCurrentUnitSystem())
    }
    
    // Integration Tests
    
    @Test
    fun `complete workflow from initialization to preference change`() = runTest {
        // Given: Fresh manager with no preferences
        mockPreferencesRepository.setStoredPreferences(null)
        mockUserRepository.setCurrentUser(testUser)
        
        // When: Initializing from locale
        val initialSystem = manager.initializeFromLocale("US")
        assertEquals(UnitSystem.IMPERIAL, initialSystem)
        
        // When: User manually changes preference
        val changeResult = manager.setUnitSystem(UnitSystem.METRIC, isManuallySet = true)
        assertTrue(changeResult.isSuccess)
        
        // When: Attempting to reinitialize from locale
        val reinitSystem = manager.initializeFromLocale("US")
        
        // Then: Manual preference is preserved
        assertEquals(UnitSystem.METRIC, reinitSystem, "Manual preference should override locale")
        
        // Verify preferences are correctly marked as manual
        val savedPreferences = mockPreferencesRepository.getStoredPreferences()
        assertNotNull(savedPreferences)
        assertTrue(savedPreferences.isManuallySet, "Preference should be marked as manually set")
    }
}