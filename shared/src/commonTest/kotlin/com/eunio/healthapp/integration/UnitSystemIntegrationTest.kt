package com.eunio.healthapp.integration

import com.eunio.healthapp.data.local.datasource.PreferencesLocalDataSource
import com.eunio.healthapp.data.remote.datasource.PreferencesRemoteDataSource
import com.eunio.healthapp.data.repository.PreferencesRepositoryImpl
import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.manager.UnitSystemManagerImpl
import com.eunio.healthapp.domain.model.HealthGoal
import com.eunio.healthapp.domain.model.SyncStatus
import com.eunio.healthapp.domain.model.UnitSystem
import com.eunio.healthapp.domain.model.User
import com.eunio.healthapp.domain.model.UserPreferences
import com.eunio.healthapp.domain.repository.UserRepository
import com.eunio.healthapp.domain.util.LocaleDetector
import com.eunio.healthapp.domain.util.NetworkConnectivity
import com.eunio.healthapp.domain.util.NetworkType
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.domain.util.UnitConverter
import com.eunio.healthapp.domain.util.UnitConverterImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.*

/**
 * Integration test for the complete unit system preference flow
 * Tests the interaction between UnitSystemManager, PreferencesRepository, and data sources
 */
class UnitSystemIntegrationTest {
    
    // Test implementations
    private class TestPreferencesLocalDataSource : PreferencesLocalDataSource {
        private val storage = mutableMapOf<String, UserPreferences>()
        var shouldFail = false
        
        override suspend fun getPreferences(): UserPreferences? = storage.values.firstOrNull()
        override suspend fun getPreferences(userId: String): UserPreferences? = storage[userId]
        
        override suspend fun savePreferences(preferences: UserPreferences): Result<Unit> {
            if (shouldFail) return Result.error(AppError.DatabaseError("Local save failed"))
            storage[preferences.userId] = preferences
            return Result.success(Unit)
        }
        
        override suspend fun getPendingSyncPreferences(): List<UserPreferences> =
            storage.values.filter { it.syncStatus == SyncStatus.PENDING }
        
        override suspend fun markAsSynced(userId: String): Result<Unit> {
            if (shouldFail) return Result.error(AppError.DatabaseError("Mark synced failed"))
            storage[userId]?.let { prefs ->
                storage[userId] = prefs.copy(syncStatus = SyncStatus.SYNCED)
            }
            return Result.success(Unit)
        }
        
        override suspend fun markAsFailed(userId: String): Result<Unit> {
            if (shouldFail) return Result.error(AppError.DatabaseError("Mark failed failed"))
            storage[userId]?.let { prefs ->
                storage[userId] = prefs.copy(syncStatus = SyncStatus.FAILED)
            }
            return Result.success(Unit)
        }
        
        override suspend fun clearPreferences(): Result<Unit> {
            if (shouldFail) return Result.error(AppError.DatabaseError("Clear failed"))
            storage.clear()
            return Result.success(Unit)
        }
        
        override suspend fun clearPreferences(userId: String): Result<Unit> {
            if (shouldFail) return Result.error(AppError.DatabaseError("Clear user failed"))
            storage.remove(userId)
            return Result.success(Unit)
        }
        
        fun reset() {
            storage.clear()
            shouldFail = false
        }
    }
    
    private class TestPreferencesRemoteDataSource : PreferencesRemoteDataSource {
        private val storage = mutableMapOf<String, UserPreferences>()
        var shouldFail = false
        
        override suspend fun getPreferences(userId: String): Result<UserPreferences?> {
            if (shouldFail) return Result.error(AppError.NetworkError("Remote get failed"))
            return Result.success(storage[userId])
        }
        
        override suspend fun savePreferences(preferences: UserPreferences): Result<Unit> {
            if (shouldFail) return Result.error(AppError.NetworkError("Remote save failed"))
            storage[preferences.userId] = preferences
            return Result.success(Unit)
        }
        
        override suspend fun updatePreferences(preferences: UserPreferences): Result<Unit> {
            if (shouldFail) return Result.error(AppError.NetworkError("Remote update failed"))
            storage[preferences.userId] = preferences
            return Result.success(Unit)
        }
        
        override suspend fun deletePreferences(userId: String): Result<Unit> {
            if (shouldFail) return Result.error(AppError.NetworkError("Remote delete failed"))
            storage.remove(userId)
            return Result.success(Unit)
        }
        
        override suspend fun clearPreferences(): Result<Unit> {
            if (shouldFail) return Result.error(AppError.NetworkError("Remote clear failed"))
            storage.clear()
            return Result.success(Unit)
        }
        
        override suspend fun preferencesExist(userId: String): Result<Boolean> {
            if (shouldFail) return Result.error(AppError.NetworkError("Remote exist check failed"))
            return Result.success(storage.containsKey(userId))
        }
        
        override suspend fun getLastModifiedTimestamp(userId: String): Result<Long?> {
            if (shouldFail) return Result.error(AppError.NetworkError("Remote timestamp failed"))
            return Result.success(storage[userId]?.lastModified?.epochSeconds)
        }
        
        fun reset() {
            storage.clear()
            shouldFail = false
        }
    }
    
    private class TestUserRepository : UserRepository {
        private var currentUser: User? = null
        var shouldFail = false
        
        override suspend fun getCurrentUser(): Result<User?> {
            if (shouldFail) return Result.error(AppError.AuthenticationError("Get user failed"))
            return Result.success(currentUser)
        }
        
        override suspend fun updateUser(user: User): Result<Unit> {
            if (shouldFail) return Result.error(AppError.DataSyncError("Update user failed"))
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
            return Result.success(currentUser ?: createUser(email, password, "Test User").getOrThrow())
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
            shouldFail = false
        }
    }
    
    private class TestNetworkConnectivity : NetworkConnectivity {
        private var connected = true
        
        override fun isConnected(): Boolean = connected
        override fun observeConnectivity(): kotlinx.coroutines.flow.Flow<Boolean> = kotlinx.coroutines.flow.flowOf(connected)
        override suspend fun hasStableConnection(): Boolean = connected
        override fun getNetworkType(): NetworkType = if (connected) NetworkType.WIFI else NetworkType.NONE
        
        fun setConnected(isConnected: Boolean) {
            connected = isConnected
        }
    }
    
    private class TestLocaleDetector : LocaleDetector {
        private var locale = "US"
        
        override fun getCurrentLocaleCountryCode(): String? = locale
        override fun getCurrentLocaleString(): String? = "en_$locale"
        
        fun setLocale(newLocale: String) {
            locale = newLocale
        }
    }
    
    // Test components
    private lateinit var localDataSource: TestPreferencesLocalDataSource
    private lateinit var remoteDataSource: TestPreferencesRemoteDataSource
    private lateinit var userRepository: TestUserRepository
    private lateinit var networkConnectivity: TestNetworkConnectivity
    private lateinit var localeDetector: TestLocaleDetector
    private lateinit var preferencesRepository: PreferencesRepositoryImpl
    private lateinit var unitSystemManager: UnitSystemManagerImpl
    private lateinit var unitConverter: UnitConverter
    
    private val testUser = User(
        id = "test-user-id",
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
        localDataSource = TestPreferencesLocalDataSource()
        remoteDataSource = TestPreferencesRemoteDataSource()
        userRepository = TestUserRepository()
        networkConnectivity = TestNetworkConnectivity()
        localeDetector = TestLocaleDetector()
        
        preferencesRepository = PreferencesRepositoryImpl(
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource,
            networkConnectivity = networkConnectivity,
            userRepository = userRepository,
            errorHandler = com.eunio.healthapp.domain.util.ErrorHandler()
        )
        
        unitSystemManager = UnitSystemManagerImpl(
            preferencesRepository = preferencesRepository,
            userRepository = userRepository,
            localeDetector = localeDetector
        )
        
        unitConverter = UnitConverterImpl()
        
        userRepository.setCurrentUser(testUser)
    }
    
    @AfterTest
    fun tearDown() {
        localDataSource.reset()
        remoteDataSource.reset()
        userRepository.reset()
    }
    
    // Complete Workflow Tests
    
    @Test
    fun `complete onboarding workflow with locale detection`() = runTest {
        // Given: New user in US locale
        localeDetector.setLocale("US")
        networkConnectivity.setConnected(true)
        
        // When: Initializing unit system from locale
        val initialSystem = unitSystemManager.initializeFromLocale("US")
        
        // Then: Sets Imperial based on locale
        assertEquals(UnitSystem.IMPERIAL, initialSystem)
        
        // Verify preferences are saved
        val savedPrefs = preferencesRepository.getUserPreferences().getOrNull()
        assertNotNull(savedPrefs)
        assertEquals(UnitSystem.IMPERIAL, savedPrefs.unitSystem)
        assertFalse(savedPrefs.isManuallySet, "Should not be marked as manually set")
        
        // Verify user model is updated
        val updatedUser = userRepository.getCurrentUser().getOrNull()
        assertNotNull(updatedUser)
        assertEquals(UnitSystem.IMPERIAL, updatedUser.unitSystem)
    }
    
    @Test
    fun `user changes preference and it overrides locale`() = runTest {
        // Given: User initialized with US locale (Imperial)
        localeDetector.setLocale("US")
        unitSystemManager.initializeFromLocale("US")
        
        // When: User manually changes to Metric
        val changeResult = unitSystemManager.setUnitSystem(UnitSystem.METRIC, isManuallySet = true)
        
        // Then: Change succeeds
        assertTrue(changeResult.isSuccess)
        assertEquals(UnitSystem.METRIC, unitSystemManager.getCurrentUnitSystem())
        
        // When: Attempting to reinitialize from locale
        val reinitResult = unitSystemManager.initializeFromLocale("US")
        
        // Then: Manual preference is preserved
        assertEquals(UnitSystem.METRIC, reinitResult, "Manual preference should override locale")
        
        // Verify preferences are marked as manual
        val savedPrefs = preferencesRepository.getUserPreferences().getOrNull()
        assertNotNull(savedPrefs)
        assertTrue(savedPrefs.isManuallySet, "Should be marked as manually set")
    }
    
    @Test
    fun `offline preference changes sync when connectivity restored`() = runTest {
        // Given: User is offline
        networkConnectivity.setConnected(false)
        
        // When: User changes preference offline
        val offlineResult = unitSystemManager.setUnitSystem(UnitSystem.IMPERIAL)
        
        // Then: Change succeeds locally
        assertTrue(offlineResult.isSuccess)
        assertEquals(UnitSystem.IMPERIAL, unitSystemManager.getCurrentUnitSystem())
        
        // Verify local storage has pending sync
        val localPrefs = localDataSource.getPreferences()
        assertNotNull(localPrefs)
        assertEquals(SyncStatus.PENDING, localPrefs.syncStatus)
        
        // When: Connectivity is restored and sync is triggered
        networkConnectivity.setConnected(true)
        val syncResult = preferencesRepository.syncPreferences()
        
        // Then: Sync succeeds
        assertTrue(syncResult.isSuccess)
        
        // Verify remote storage is updated
        val remotePrefs = remoteDataSource.getPreferences(testUser.id).getOrNull()
        assertNotNull(remotePrefs)
        assertEquals(UnitSystem.IMPERIAL, remotePrefs.unitSystem)
    }
    
    @Test
    fun `measurement display updates reactively with preference changes`() = runTest {
        // Given: Initial metric system
        unitSystemManager.setUnitSystem(UnitSystem.METRIC)
        
        // When: Getting current unit system
        val initialSystem = unitSystemManager.getCurrentUnitSystem()
        assertEquals(UnitSystem.METRIC, initialSystem)
        
        // When: Changing unit system
        val changeResult = unitSystemManager.setUnitSystem(UnitSystem.IMPERIAL)
        assertTrue(changeResult.isSuccess)
        
        // Then: Current system is updated
        val updatedSystem = unitSystemManager.getCurrentUnitSystem()
        assertEquals(UnitSystem.IMPERIAL, updatedSystem)
        
        // Test measurement conversion with new unit system
        val weightInKg = 70.0
        val displayWeight = unitConverter.convertWeight(weightInKg, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        val formattedWeight = unitConverter.formatWeight(displayWeight, UnitSystem.IMPERIAL)
        
        assertEquals("154.32 lbs", formattedWeight)
    }
    
    @Test
    fun `cross-platform data consistency`() = runTest {
        // Given: User sets preference on one platform
        networkConnectivity.setConnected(true)
        unitSystemManager.setUnitSystem(UnitSystem.IMPERIAL)
        
        // Simulate data sync to cloud
        preferencesRepository.syncPreferences()
        
        // When: Simulating login on another platform (clear local cache)
        localDataSource.reset()
        unitSystemManager.clearCache()
        
        // When: Getting preferences (should fetch from remote)
        val retrievedSystem = unitSystemManager.getCurrentUnitSystem()
        
        // Then: Preference is consistent across platforms
        assertEquals(UnitSystem.IMPERIAL, retrievedSystem)
        
        // Verify local cache is populated from remote
        val localPrefs = localDataSource.getPreferences()
        assertNotNull(localPrefs)
        assertEquals(UnitSystem.IMPERIAL, localPrefs.unitSystem)
    }
    
    @Test
    fun `error recovery and graceful degradation`() = runTest {
        // Given: System is working normally
        unitSystemManager.setUnitSystem(UnitSystem.IMPERIAL)
        
        // When: Remote service fails
        remoteDataSource.shouldFail = true
        
        // When: User changes preference (should still work locally)
        val result = unitSystemManager.setUnitSystem(UnitSystem.METRIC)
        
        // Then: Operation succeeds locally
        assertTrue(result.isSuccess)
        assertEquals(UnitSystem.METRIC, unitSystemManager.getCurrentUnitSystem())
        
        // When: Remote service recovers
        remoteDataSource.shouldFail = false
        val syncResult = preferencesRepository.syncPreferences()
        
        // Then: Sync succeeds
        assertTrue(syncResult.isSuccess)
    }
    
    @Test
    fun `preference persistence across app restarts`() = runTest {
        // Given: User sets preference
        unitSystemManager.setUnitSystem(UnitSystem.IMPERIAL)
        
        // Simulate app restart by creating new manager instance
        val newManager = UnitSystemManagerImpl(
            preferencesRepository = preferencesRepository,
            userRepository = userRepository,
            localeDetector = localeDetector
        )
        
        // When: Getting preference after restart
        val restoredSystem = newManager.getCurrentUnitSystem()
        
        // Then: Preference is restored
        assertEquals(UnitSystem.IMPERIAL, restoredSystem)
    }
    
    @Test
    fun `concurrent preference changes are handled correctly`() = runTest {
        // Given: Multiple concurrent operations
        val results = mutableListOf<Result<Unit>>()
        
        // When: Performing concurrent preference changes
        repeat(5) { index ->
            val unitSystem = if (index % 2 == 0) UnitSystem.METRIC else UnitSystem.IMPERIAL
            val result = unitSystemManager.setUnitSystem(unitSystem)
            results.add(result)
        }
        
        // Then: All operations succeed
        results.forEach { result ->
            assertTrue(result.isSuccess, "Concurrent operations should succeed")
        }
        
        // Final state should be consistent
        val finalSystem = unitSystemManager.getCurrentUnitSystem()
        assertTrue(finalSystem == UnitSystem.METRIC || finalSystem == UnitSystem.IMPERIAL)
    }
    
    @Test
    fun `locale change after manual preference does not override`() = runTest {
        // Given: User manually sets preference
        unitSystemManager.setUnitSystem(UnitSystem.METRIC, isManuallySet = true)
        
        // When: Device locale changes to Imperial-preferring region
        localeDetector.setLocale("US")
        val result = unitSystemManager.initializeFromLocale("US")
        
        // Then: Manual preference is preserved
        assertEquals(UnitSystem.METRIC, result)
        assertEquals(UnitSystem.METRIC, unitSystemManager.getCurrentUnitSystem())
    }
    
    @Test
    fun `measurement conversion accuracy throughout workflow`() = runTest {
        // Given: User starts with metric system
        unitSystemManager.setUnitSystem(UnitSystem.METRIC)
        
        // Test weight conversion
        val weightKg = 75.5
        var displayWeight = unitConverter.convertWeight(weightKg, UnitSystem.METRIC, UnitSystem.METRIC)
        assertEquals(75.5, displayWeight, 0.01)
        
        // When: User switches to imperial
        unitSystemManager.setUnitSystem(UnitSystem.IMPERIAL)
        val currentSystem = unitSystemManager.getCurrentUnitSystem()
        
        // Test conversion with new system
        displayWeight = unitConverter.convertWeight(weightKg, UnitSystem.METRIC, currentSystem)
        assertEquals(166.45, displayWeight, 0.01) // 75.5 kg = 166.45 lbs
        
        // Test formatting
        val formatted = unitConverter.formatWeight(displayWeight, currentSystem)
        assertEquals("166.45 lbs", formatted)
        
        // Test round-trip conversion accuracy
        val backToKg = unitConverter.convertWeight(displayWeight, UnitSystem.IMPERIAL, UnitSystem.METRIC)
        assertEquals(weightKg, backToKg, 0.1) // Allow small rounding difference
    }
    
    @Test
    fun `complete user journey from onboarding to daily use`() = runTest {
        // Step 1: New user onboarding with locale detection
        localeDetector.setLocale("GB") // UK uses metric
        val initialSystem = unitSystemManager.initializeFromLocale("GB")
        assertEquals(UnitSystem.METRIC, initialSystem)
        
        // Step 2: User logs some measurements (simulated)
        val bodyWeight = 68.5 // kg
        val walkDistance = 5.2 // km
        val bodyTemp = 36.8 // celsius
        
        // Verify measurements display correctly in metric
        val weightFormatted = unitConverter.formatWeight(bodyWeight, UnitSystem.METRIC)
        val distanceFormatted = unitConverter.formatDistance(walkDistance, UnitSystem.METRIC)
        val tempFormatted = unitConverter.formatTemperature(bodyTemp, UnitSystem.METRIC)
        
        // Check that formatting includes correct units and values
        assertTrue(weightFormatted.contains("kg"))
        assertTrue(weightFormatted.contains("68.5"))
        assertTrue(distanceFormatted.contains("km"))
        assertTrue(distanceFormatted.contains("5.2"))
        assertTrue(tempFormatted.contains("°C"))
        assertTrue(tempFormatted.contains("36.8"))
        
        // Step 3: User travels to US and wants to switch to Imperial
        val changeResult = unitSystemManager.setUnitSystem(UnitSystem.IMPERIAL, isManuallySet = true)
        assertTrue(changeResult.isSuccess)
        
        // Step 4: Verify all measurements now display in Imperial
        val currentSystem = unitSystemManager.getCurrentUnitSystem()
        assertEquals(UnitSystem.IMPERIAL, currentSystem)
        
        val weightLbs = unitConverter.convertWeight(bodyWeight, UnitSystem.METRIC, currentSystem)
        val distanceMiles = unitConverter.convertDistance(walkDistance, UnitSystem.METRIC, currentSystem)
        val tempF = unitConverter.convertTemperature(bodyTemp, UnitSystem.METRIC, currentSystem)
        
        assertEquals("151.02 lbs", unitConverter.formatWeight(weightLbs, currentSystem))
        assertEquals("3.23 miles", unitConverter.formatDistance(distanceMiles, currentSystem))
        assertEquals("98.24°F", unitConverter.formatTemperature(tempF, currentSystem))
        
        // Step 5: User returns home, locale changes back but preference is preserved
        localeDetector.setLocale("GB")
        val afterTravelSystem = unitSystemManager.initializeFromLocale("GB")
        assertEquals(UnitSystem.IMPERIAL, afterTravelSystem, "Manual preference should be preserved")
        
        // Step 6: Verify data consistency and sync
        val syncResult = preferencesRepository.syncPreferences()
        assertTrue(syncResult.isSuccess)
        
        val finalPrefs = preferencesRepository.getUserPreferences().getOrNull()
        assertNotNull(finalPrefs)
        assertEquals(UnitSystem.IMPERIAL, finalPrefs.unitSystem)
        assertTrue(finalPrefs.isManuallySet)
    }
}