package com.eunio.healthapp.integration

import com.eunio.healthapp.data.local.datasource.PreferencesLocalDataSource
import com.eunio.healthapp.data.remote.datasource.PreferencesRemoteDataSource
import com.eunio.healthapp.data.repository.PreferencesRepositoryImpl
import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.*
import com.eunio.healthapp.domain.repository.UserRepository
import com.eunio.healthapp.domain.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.random.Random
import kotlin.test.*

/**
 * Integration tests for cross-platform data synchronization.
 * Tests data consistency across different devices and platforms, conflict resolution,
 * and offline/online sync scenarios.
 */
class CrossPlatformSyncTest {
    
    /**
     * Simulates a device/platform with its own local storage and network connectivity
     */
    private class MockDevice(
        val deviceId: String,
        val platform: String // "iOS", "Android", "Web"
    ) {
        private val localStorage = mutableMapOf<String, UserPreferences>()
        private var isConnected = true
        private var syncDelay = 0L
        
        val localDataSource = object : PreferencesLocalDataSource {
            override suspend fun getPreferences(userId: String): UserPreferences? = localStorage[userId]
            override suspend fun getPreferences(): UserPreferences? = localStorage.values.firstOrNull()
            
            override suspend fun savePreferences(preferences: UserPreferences): Result<Unit> {
                localStorage[preferences.userId] = preferences
                return Result.success(Unit)
            }
            
            override suspend fun clearPreferences(): Result<Unit> {
                localStorage.clear()
                return Result.success(Unit)
            }
            
            override suspend fun clearPreferences(userId: String): Result<Unit> {
                localStorage.remove(userId)
                return Result.success(Unit)
            }
            
            override suspend fun markAsSynced(userId: String): Result<Unit> {
                localStorage[userId]?.let { prefs ->
                    localStorage[userId] = prefs.copy(syncStatus = SyncStatus.SYNCED)
                }
                return Result.success(Unit)
            }
            
            override suspend fun markAsFailed(userId: String): Result<Unit> {
                localStorage[userId]?.let { prefs ->
                    localStorage[userId] = prefs.copy(syncStatus = SyncStatus.FAILED)
                }
                return Result.success(Unit)
            }
            
            override suspend fun getPendingSyncPreferences(): List<UserPreferences> {
                return localStorage.values.filter { it.syncStatus == SyncStatus.PENDING }
            }
        }
        
        val networkConnectivity = object : NetworkConnectivity {
            override fun isConnected(): Boolean = isConnected
            override fun getNetworkType(): NetworkType = if (isConnected) NetworkType.WIFI else NetworkType.NONE
            override fun observeConnectivity(): kotlinx.coroutines.flow.Flow<Boolean> = 
                kotlinx.coroutines.flow.flowOf(isConnected)
            override suspend fun hasStableConnection(): Boolean = isConnected
        }
        
        fun setConnected(connected: Boolean) {
            isConnected = connected
        }
        
        fun setSyncDelay(delay: Long) {
            syncDelay = delay
        }
        
        suspend fun simulateNetworkDelay() {
            if (syncDelay > 0) delay(syncDelay)
        }
        
        fun getLocalPreferences(userId: String): UserPreferences? = localStorage[userId]
        
        fun getAllLocalPreferences(): Map<String, UserPreferences> = localStorage.toMap()
        
        fun clearLocalStorage() {
            localStorage.clear()
        }
    }
    
    /**
     * Enhanced shared remote storage that all devices sync to with realistic behavior
     */
    private class MockCloudStorage {
        private val storage = mutableMapOf<String, UserPreferences>()
        private val syncHistory = mutableListOf<SyncEvent>()
        private var shouldFailSync = false
        private var failingDevices = mutableSetOf<String>()
        private var conflictResolutionStrategy = ConflictResolutionStrategy.LAST_WRITE_WINS
        private var networkDelay: Duration = 100.milliseconds
        private var errorRate: Double = 0.0
        private var currentDeviceId: String = "unknown"
        
        data class SyncEvent(
            val deviceId: String,
            val userId: String,
            val operation: String, // "GET", "PUT", "DELETE"
            val timestamp: kotlinx.datetime.Instant,
            val preferences: UserPreferences?,
            val success: Boolean = true,
            val errorMessage: String? = null
        )
        
        enum class ConflictResolutionStrategy {
            LAST_WRITE_WINS,
            MANUAL_PREFERENCE_WINS,
            DEVICE_PRIORITY
        }
        
        val remoteDataSource = object : PreferencesRemoteDataSource {
            override suspend fun getPreferences(userId: String): Result<UserPreferences?> {
                delay(networkDelay)
                
                if (shouldFailSync || failingDevices.contains(currentDeviceId) || (errorRate > 0.0 && Random.nextDouble() < errorRate)) {
                    val errorMsg = "Cloud sync failed - GET operation"
                    syncHistory.add(SyncEvent(currentDeviceId, userId, "GET", Clock.System.now(), null, false, errorMsg))
                    return Result.error(AppError.NetworkError(errorMsg))
                }
                
                val preferences = storage[userId]
                syncHistory.add(SyncEvent(currentDeviceId, userId, "GET", Clock.System.now(), preferences, true))
                return Result.success(preferences)
            }
            
            override suspend fun savePreferences(preferences: UserPreferences): Result<Unit> {
                delay(networkDelay)
                
                if (shouldFailSync || failingDevices.contains(currentDeviceId) || (errorRate > 0.0 && Random.nextDouble() < errorRate)) {
                    val errorMsg = "Cloud sync failed - PUT operation"
                    syncHistory.add(SyncEvent(currentDeviceId, preferences.userId, "PUT", Clock.System.now(), preferences, false, errorMsg))
                    return Result.error(AppError.NetworkError(errorMsg))
                }
                
                // Handle conflict resolution if there's existing data
                val existingPrefs = storage[preferences.userId]
                val resolvedPrefs = if (existingPrefs != null) {
                    resolveConflict(preferences, existingPrefs)
                } else {
                    preferences
                }
                
                storage[preferences.userId] = resolvedPrefs
                syncHistory.add(SyncEvent(currentDeviceId, preferences.userId, "PUT", Clock.System.now(), resolvedPrefs, true))
                return Result.success(Unit)
            }
            
            override suspend fun updatePreferences(preferences: UserPreferences): Result<Unit> {
                return savePreferences(preferences)
            }
            
            override suspend fun deletePreferences(userId: String): Result<Unit> {
                delay(networkDelay)
                
                if (shouldFailSync || failingDevices.contains(currentDeviceId) || (errorRate > 0.0 && Random.nextDouble() < errorRate)) {
                    val errorMsg = "Cloud sync failed"
                    syncHistory.add(SyncEvent(currentDeviceId, userId, "DELETE", Clock.System.now(), null, false, errorMsg))
                    return Result.error(AppError.NetworkError(errorMsg))
                }
                
                val removedPrefs = storage.remove(userId)
                syncHistory.add(SyncEvent(currentDeviceId, userId, "DELETE", Clock.System.now(), removedPrefs, true))
                return Result.success(Unit)
            }
            
            override suspend fun clearPreferences(): Result<Unit> {
                delay(networkDelay)
                
                if (shouldFailSync || failingDevices.contains(currentDeviceId) || (errorRate > 0.0 && Random.nextDouble() < errorRate)) {
                    val errorMsg = "Cloud sync failed"
                    syncHistory.add(SyncEvent(currentDeviceId, "all", "CLEAR", Clock.System.now(), null, false, errorMsg))
                    return Result.error(AppError.NetworkError(errorMsg))
                }
                
                storage.clear()
                syncHistory.add(SyncEvent(currentDeviceId, "all", "CLEAR", Clock.System.now(), null, true))
                return Result.success(Unit)
            }
            
            override suspend fun preferencesExist(userId: String): Result<Boolean> {
                delay(networkDelay)
                
                if (shouldFailSync || failingDevices.contains(currentDeviceId) || (errorRate > 0.0 && Random.nextDouble() < errorRate)) {
                    val errorMsg = "Cloud sync failed"
                    syncHistory.add(SyncEvent(currentDeviceId, userId, "EXISTS", Clock.System.now(), null, false, errorMsg))
                    return Result.error(AppError.NetworkError(errorMsg))
                }
                
                val exists = storage.containsKey(userId)
                syncHistory.add(SyncEvent(currentDeviceId, userId, "EXISTS", Clock.System.now(), null, true))
                return Result.success(exists)
            }
            
            override suspend fun getLastModifiedTimestamp(userId: String): Result<Long?> {
                delay(networkDelay)
                
                if (shouldFailSync || failingDevices.contains(currentDeviceId) || (errorRate > 0.0 && Random.nextDouble() < errorRate)) {
                    val errorMsg = "Cloud sync failed"
                    syncHistory.add(SyncEvent(currentDeviceId, userId, "TIMESTAMP", Clock.System.now(), null, false, errorMsg))
                    return Result.error(AppError.NetworkError(errorMsg))
                }
                
                val timestamp = storage[userId]?.lastModified?.epochSeconds
                syncHistory.add(SyncEvent(currentDeviceId, userId, "TIMESTAMP", Clock.System.now(), null, true))
                return Result.success(timestamp)
            }
        }
        
        fun setFailSync(shouldFail: Boolean) {
            shouldFailSync = shouldFail
        }
        
        fun setDeviceFailSync(deviceId: String, shouldFail: Boolean) {
            if (shouldFail) {
                failingDevices.add(deviceId)
            } else {
                failingDevices.remove(deviceId)
            }
        }
        
        fun setConflictResolutionStrategy(strategy: ConflictResolutionStrategy) {
            conflictResolutionStrategy = strategy
        }
        
        fun setNetworkDelay(delay: Duration) {
            networkDelay = delay
        }
        
        fun setErrorRate(rate: Double) {
            errorRate = rate.coerceIn(0.0, 1.0)
        }
        
        fun setCurrentDeviceId(deviceId: String) {
            currentDeviceId = deviceId
        }
        
        fun getStoredPreferences(userId: String): UserPreferences? = storage[userId]
        
        fun getAllStoredPreferences(): Map<String, UserPreferences> = storage.toMap()
        
        fun getSyncHistory(): List<SyncEvent> = syncHistory.toList()
        
        fun getSuccessfulSyncHistory(): List<SyncEvent> = syncHistory.filter { it.success }
        
        fun getFailedSyncHistory(): List<SyncEvent> = syncHistory.filter { !it.success }
        
        fun clearSyncHistory() {
            syncHistory.clear()
        }
        
        fun clear() {
            storage.clear()
            syncHistory.clear()
        }
        
        fun reset() {
            clear()
            shouldFailSync = false
            failingDevices.clear()
            conflictResolutionStrategy = ConflictResolutionStrategy.LAST_WRITE_WINS
            networkDelay = 100.milliseconds
            errorRate = 0.0
            currentDeviceId = "unknown"
        }
        
        /**
         * Simulates conflict resolution when multiple devices sync simultaneously
         */
        fun resolveConflict(
            newPrefs: UserPreferences,
            existingPrefs: UserPreferences
        ): UserPreferences {
            return when (conflictResolutionStrategy) {
                ConflictResolutionStrategy.LAST_WRITE_WINS -> {
                    if (newPrefs.lastModified > existingPrefs.lastModified) newPrefs else existingPrefs
                }
                ConflictResolutionStrategy.MANUAL_PREFERENCE_WINS -> {
                    when {
                        newPrefs.isManuallySet && !existingPrefs.isManuallySet -> newPrefs
                        !newPrefs.isManuallySet && existingPrefs.isManuallySet -> existingPrefs
                        else -> if (newPrefs.lastModified > existingPrefs.lastModified) newPrefs else existingPrefs
                    }
                }
                ConflictResolutionStrategy.DEVICE_PRIORITY -> {
                    // In real implementation, this would consider device priority
                    if (newPrefs.lastModified > existingPrefs.lastModified) newPrefs else existingPrefs
                }
            }
        }
    }
    
    /**
     * Mock user repository for testing
     */
    private class MockUserRepository : UserRepository {
        private var currentUser: User? = null
        
        override suspend fun getCurrentUser(): Result<User?> = Result.success(currentUser)
        override suspend fun updateUser(user: User): Result<Unit> = Result.success(Unit)
        override suspend fun completeOnboarding(userId: String, primaryGoal: HealthGoal): Result<Unit> = Result.success(Unit)
        override suspend fun createUser(email: String, password: String, name: String): Result<User> = 
            Result.success(User("id", email, name, true, HealthGoal.CYCLE_TRACKING, UnitSystem.METRIC, Clock.System.now(), Clock.System.now()))
        override suspend fun signInUser(email: String, password: String): Result<User> = getCurrentUser().map { it!! }
        override suspend fun signOutUser(): Result<Unit> = Result.success(Unit)
        override suspend fun deleteUser(userId: String): Result<Unit> = Result.success(Unit)
        
        fun setCurrentUser(user: User?) {
            currentUser = user
        }
    }
    
    // Test components
    private lateinit var cloudStorage: MockCloudStorage
    private lateinit var userRepository: MockUserRepository
    private lateinit var iosDevice: MockDevice
    private lateinit var androidDevice: MockDevice
    private lateinit var webDevice: MockDevice
    
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
        cloudStorage = MockCloudStorage()
        userRepository = MockUserRepository()
        userRepository.setCurrentUser(testUser)
        
        iosDevice = MockDevice("ios-device-1", "iOS")
        androidDevice = MockDevice("android-device-1", "Android")
        webDevice = MockDevice("web-device-1", "Web")
    }
    
    @AfterTest
    fun tearDown() {
        cloudStorage.reset()
        iosDevice.clearLocalStorage()
        androidDevice.clearLocalStorage()
        webDevice.clearLocalStorage()
    }
    
    private fun createRepository(device: MockDevice): PreferencesRepositoryImpl {
        // Set the current device ID in cloud storage for proper tracking
        cloudStorage.setCurrentDeviceId(device.deviceId)
        
        return PreferencesRepositoryImpl(
            localDataSource = device.localDataSource,
            remoteDataSource = cloudStorage.remoteDataSource,
            networkConnectivity = device.networkConnectivity,
            userRepository = userRepository,
            errorHandler = ErrorHandler()
        )
    }
    
    // Basic Cross-Platform Sync Tests
    
    @Test
    fun `preferences sync across different platforms`() = runTest {
        // Given: User sets preference on iOS device
        val iosRepository = createRepository(iosDevice)
        val initialPrefs = UserPreferences(
            userId = testUserId,
            unitSystem = UnitSystem.IMPERIAL,
            isManuallySet = true,
            lastModified = Clock.System.now(),
            syncStatus = SyncStatus.PENDING
        )
        
        // When: iOS saves and syncs preferences
        iosRepository.saveUserPreferences(initialPrefs)
        iosRepository.syncPreferences()
        
        // Then: Android device can retrieve the same preferences
        val androidRepository = createRepository(androidDevice)
        val retrievedPrefs = androidRepository.getUserPreferences(testUserId)
        
        assertTrue(retrievedPrefs.isSuccess)
        assertEquals(UnitSystem.IMPERIAL, retrievedPrefs.getOrNull()?.unitSystem)
        assertEquals(true, retrievedPrefs.getOrNull()?.isManuallySet)
    }
    
    @Test
    fun `offline changes sync when connectivity restored`() = runTest {
        // Given: User makes changes while offline on Android
        androidDevice.setConnected(false)
        val androidRepository = createRepository(androidDevice)
        
        val offlinePrefs = UserPreferences(
            userId = testUserId,
            unitSystem = UnitSystem.METRIC,
            isManuallySet = false,
            lastModified = Clock.System.now(),
            syncStatus = SyncStatus.PENDING
        )
        
        // When: Saving while offline (should save locally only)
        val saveResult = androidRepository.saveUserPreferences(offlinePrefs)
        assertTrue(saveResult.isSuccess)
        
        // Verify it's stored locally but not synced
        val localPrefs = androidDevice.getLocalPreferences(testUserId)
        assertNotNull(localPrefs)
        assertEquals(SyncStatus.PENDING, localPrefs!!.syncStatus)
        
        // When: Connectivity is restored and sync is attempted
        androidDevice.setConnected(true)
        val syncResult = androidRepository.syncPreferences()
        assertTrue(syncResult.isSuccess)
        
        // Then: iOS device can now retrieve the synced preferences
        val iosRepository = createRepository(iosDevice)
        val retrievedPrefs = iosRepository.getUserPreferences(testUserId)
        
        assertTrue(retrievedPrefs.isSuccess)
        assertEquals(UnitSystem.METRIC, retrievedPrefs.getOrNull()?.unitSystem)
    }
    
    // Conflict Resolution Tests
    
    @Test
    fun `last write wins conflict resolution`() = runTest {
        cloudStorage.setConflictResolutionStrategy(MockCloudStorage.ConflictResolutionStrategy.LAST_WRITE_WINS)
        
        // Given: Both devices have different preferences with different timestamps
        val earlierTime = Clock.System.now()
        delay(1000) // Ensure different timestamps on all platforms
        val laterTime = Clock.System.now()
        
        // Ensure timestamps are actually different
        assertTrue(laterTime > earlierTime, "Later time should be greater than earlier time")
        
        val iosPrefs = UserPreferences(
            userId = testUserId,
            unitSystem = UnitSystem.IMPERIAL,
            isManuallySet = false,
            lastModified = earlierTime,
            syncStatus = SyncStatus.PENDING
        )
        
        val androidPrefs = UserPreferences(
            userId = testUserId,
            unitSystem = UnitSystem.METRIC,
            isManuallySet = false,
            lastModified = laterTime,
            syncStatus = SyncStatus.PENDING
        )
        
        // Test direct cloud storage behavior first
        cloudStorage.setCurrentDeviceId("ios-device-1")
        val iosRemoteResult = cloudStorage.remoteDataSource.savePreferences(iosPrefs)
        assertTrue(iosRemoteResult.isSuccess, "iOS remote save should succeed")
        
        val afterIosCloud = cloudStorage.getStoredPreferences(testUserId)
        assertNotNull(afterIosCloud, "Cloud should have iOS preferences")
        assertEquals(UnitSystem.IMPERIAL, afterIosCloud!!.unitSystem, "Cloud should have iOS IMPERIAL preference")
        
        cloudStorage.setCurrentDeviceId("android-device-1")
        val androidRemoteResult = cloudStorage.remoteDataSource.savePreferences(androidPrefs)
        assertTrue(androidRemoteResult.isSuccess, "Android remote save should succeed")
        
        // Then: Android's newer preference should win due to conflict resolution
        val finalCloudPrefs = cloudStorage.getStoredPreferences(testUserId)
        assertNotNull(finalCloudPrefs)
        
        assertEquals(UnitSystem.METRIC, finalCloudPrefs.unitSystem, "Android's METRIC should win due to later timestamp")
        assertTrue(finalCloudPrefs.lastModified >= laterTime, "Final cloud prefs timestamp should be >= later time")
    }
    
    @Test
    fun `manual preference wins over automatic preference`() = runTest {
        cloudStorage.setConflictResolutionStrategy(MockCloudStorage.ConflictResolutionStrategy.MANUAL_PREFERENCE_WINS)
        
        // Given: iOS has automatic preference, Android has manual preference
        val timestamp = Clock.System.now()
        
        val iosPrefs = UserPreferences(
            userId = testUserId,
            unitSystem = UnitSystem.IMPERIAL,
            isManuallySet = false, // Automatic
            lastModified = timestamp,
            syncStatus = SyncStatus.PENDING
        )
        
        val androidPrefs = UserPreferences(
            userId = testUserId,
            unitSystem = UnitSystem.METRIC,
            isManuallySet = true, // Manual
            lastModified = timestamp,
            syncStatus = SyncStatus.PENDING
        )
        
        // When: iOS syncs first, then Android
        val iosRepository = createRepository(iosDevice)
        iosRepository.saveUserPreferences(iosPrefs)
        iosRepository.syncPreferences()
        
        val androidRepository = createRepository(androidDevice)
        androidRepository.saveUserPreferences(androidPrefs)
        androidRepository.syncPreferences()
        
        // Then: Manual preference (Android) should win
        val finalCloudPrefs = cloudStorage.getStoredPreferences(testUserId)
        assertNotNull(finalCloudPrefs)
        assertEquals(UnitSystem.METRIC, finalCloudPrefs.unitSystem)
        assertTrue(finalCloudPrefs.isManuallySet)
    }
}