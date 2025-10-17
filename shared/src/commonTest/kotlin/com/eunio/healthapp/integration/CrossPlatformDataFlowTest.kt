package com.eunio.healthapp.integration

import com.eunio.healthapp.data.local.datasource.PreferencesLocalDataSource
import com.eunio.healthapp.data.remote.datasource.PreferencesRemoteDataSource
import com.eunio.healthapp.data.repository.PreferencesRepositoryImpl
import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.*
import com.eunio.healthapp.domain.repository.UserRepository
import com.eunio.healthapp.domain.util.*
import com.eunio.healthapp.testutil.MockServices
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlin.test.*

/**
 * Integration tests for cross-platform data flow between platforms and backend.
 * Tests data synchronization, consistency, and platform-specific behaviors.
 * 
 * Requirements: 3.2, 3.4
 */
class CrossPlatformDataFlowTest {
    
    /**
     * Simulates a platform device with its own data storage and network connectivity
     */
    private class PlatformDevice(
        val deviceId: String,
        val platform: Platform,
        val mockServices: MockServices
    ) {
        private var isOnline = true
        private var syncDelay = 0L
        private val localStorage = mutableMapOf<String, Any>()
        
        enum class Platform {
            IOS, ANDROID, WEB
        }
        
        val networkConnectivity = object : NetworkConnectivity {
            override fun isConnected(): Boolean = isOnline
            override fun getNetworkType(): NetworkType = if (isOnline) NetworkType.WIFI else NetworkType.NONE
            override fun observeConnectivity(): kotlinx.coroutines.flow.Flow<Boolean> = 
                kotlinx.coroutines.flow.flowOf(isOnline)
            override suspend fun hasStableConnection(): Boolean = isOnline
        }
        
        fun setOnline(online: Boolean) {
            isOnline = online
        }
        
        fun setSyncDelay(delay: Long) {
            syncDelay = delay
        }
        
        suspend fun simulateNetworkDelay() {
            if (syncDelay > 0) delay(syncDelay)
        }
        
        fun storeLocalData(key: String, data: Any) {
            localStorage[key] = data
        }
        
        fun getLocalData(key: String): Any? = localStorage[key]
        
        fun clearLocalData() {
            localStorage.clear()
        }
        
        fun getStoredDataCount(): Int = localStorage.size
    }
    
    /**
     * Simulates backend services that all platforms sync with
     */
    private class BackendServices {
        private val userStorage = mutableMapOf<String, User>()
        private val logStorage = mutableMapOf<String, DailyLog>()
        private val preferencesStorage = mutableMapOf<String, UserPreferences>()
        private val syncHistory = mutableListOf<SyncEvent>()
        private var shouldFailSync = false
        private var failureRate = 0.0
        
        data class SyncEvent(
            val deviceId: String,
            val userId: String,
            val operation: String,
            val dataType: String,
            val timestamp: kotlinx.datetime.Instant,
            val success: Boolean
        )
        
        fun storeUser(user: User): Result<Unit> {
            if (shouldFailSync || kotlin.random.Random.nextDouble() < failureRate) {
                return Result.error(AppError.NetworkError("Backend sync failed"))
            }
            userStorage[user.id] = user
            return Result.success(Unit)
        }
        
        fun getUser(userId: String): Result<User?> {
            if (shouldFailSync || kotlin.random.Random.nextDouble() < failureRate) {
                return Result.error(AppError.NetworkError("Backend sync failed"))
            }
            return Result.success(userStorage[userId])
        }
        
        fun storeDailyLog(log: DailyLog): Result<Unit> {
            if (shouldFailSync || kotlin.random.Random.nextDouble() < failureRate) {
                return Result.error(AppError.NetworkError("Backend sync failed"))
            }
            logStorage[log.id] = log
            return Result.success(Unit)
        }
        
        fun getDailyLog(userId: String, date: LocalDate): Result<DailyLog?> {
            if (shouldFailSync || kotlin.random.Random.nextDouble() < failureRate) {
                return Result.error(AppError.NetworkError("Backend sync failed"))
            }
            val log = logStorage.values.find { it.userId == userId && it.date == date }
            return Result.success(log)
        }
        
        fun storePreferences(preferences: UserPreferences): Result<Unit> {
            if (shouldFailSync || kotlin.random.Random.nextDouble() < failureRate) {
                return Result.error(AppError.NetworkError("Backend sync failed"))
            }
            preferencesStorage[preferences.userId] = preferences
            return Result.success(Unit)
        }
        
        fun getPreferences(userId: String): Result<UserPreferences?> {
            if (shouldFailSync || kotlin.random.Random.nextDouble() < failureRate) {
                return Result.error(AppError.NetworkError("Backend sync failed"))
            }
            return Result.success(preferencesStorage[userId])
        }
        
        fun recordSyncEvent(deviceId: String, userId: String, operation: String, dataType: String, success: Boolean) {
            syncHistory.add(SyncEvent(deviceId, userId, operation, dataType, Clock.System.now(), success))
        }
        
        fun getSyncHistory(): List<SyncEvent> = syncHistory.toList()
        
        fun clearSyncHistory() {
            syncHistory.clear()
        }
        
        fun setFailSync(shouldFail: Boolean) {
            shouldFailSync = shouldFail
        }
        
        fun setFailureRate(rate: Double) {
            failureRate = rate
        }
        
        fun clear() {
            userStorage.clear()
            logStorage.clear()
            preferencesStorage.clear()
            syncHistory.clear()
        }
        
        fun getUserCount(): Int = userStorage.size
        fun getLogCount(): Int = logStorage.size
        fun getPreferencesCount(): Int = preferencesStorage.size
    }
    
    // Test components
    private lateinit var backendServices: BackendServices
    private lateinit var iosDevice: PlatformDevice
    private lateinit var androidDevice: PlatformDevice
    private lateinit var webDevice: PlatformDevice
    
    private val testUserId = "cross-platform-user"
    private val testUser = User(
        id = testUserId,
        email = "crossplatform@example.com",
        name = "Cross Platform User",
        onboardingComplete = true,
        primaryGoal = HealthGoal.CYCLE_TRACKING,
        unitSystem = UnitSystem.METRIC,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now()
    )
    
    @BeforeTest
    fun setup() {
        backendServices = BackendServices()
        iosDevice = PlatformDevice("ios-device-1", PlatformDevice.Platform.IOS, MockServices())
        androidDevice = PlatformDevice("android-device-1", PlatformDevice.Platform.ANDROID, MockServices())
        webDevice = PlatformDevice("web-device-1", PlatformDevice.Platform.WEB, MockServices())
    }
    
    @AfterTest
    fun tearDown() {
        backendServices.clear()
        iosDevice.clearLocalData()
        androidDevice.clearLocalData()
        webDevice.clearLocalData()
    }
    
    // Cross-Platform Data Flow Tests
    
    @Test
    fun `data flows correctly between iOS and Android platforms`() = runTest {
        // Given: User creates account on iOS
        val iosResult = iosDevice.mockServices.userRepository.createUser(
            email = testUser.email,
            password = "securePassword123",
            name = testUser.name
        )
        assertTrue(iosResult.isSuccess)
        val createdUser = iosResult.getOrNull()!!
        
        // Simulate syncing to backend
        val backendStoreResult = backendServices.storeUser(createdUser)
        assertTrue(backendStoreResult.isSuccess)
        backendServices.recordSyncEvent(iosDevice.deviceId, createdUser.id, "CREATE", "USER", true)
        
        // When: User opens app on Android
        val androidGetResult = backendServices.getUser(createdUser.id)
        assertTrue(androidGetResult.isSuccess)
        val syncedUser = androidGetResult.getOrNull()!!
        
        // Set user in Android device
        androidDevice.mockServices.userRepository.setCurrentUser(syncedUser)
        
        // Then: User data is consistent across platforms
        assertEquals(createdUser.id, syncedUser.id)
        assertEquals(createdUser.email, syncedUser.email)
        assertEquals(createdUser.name, syncedUser.name)
        assertEquals(createdUser.primaryGoal, syncedUser.primaryGoal)
        
        // When: User logs data on Android
        val androidLog = DailyLog(
            id = "android-log-1",
            userId = createdUser.id,
            date = LocalDate(2024, 1, 15),
            bbt = 98.2,
            periodFlow = PeriodFlow.MEDIUM,
            symptoms = listOf(Symptom.CRAMPS),
            mood = Mood.HAPPY,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val androidLogResult = androidDevice.mockServices.logRepository.saveDailyLog(androidLog)
        assertTrue(androidLogResult.isSuccess)
        
        // Simulate syncing to backend
        val backendLogResult = backendServices.storeDailyLog(androidLog)
        assertTrue(backendLogResult.isSuccess)
        backendServices.recordSyncEvent(androidDevice.deviceId, createdUser.id, "CREATE", "LOG", true)
        
        // When: User opens app on iOS
        val iosLogResult = backendServices.getDailyLog(createdUser.id, LocalDate(2024, 1, 15))
        assertTrue(iosLogResult.isSuccess)
        val syncedLog = iosLogResult.getOrNull()!!
        
        // Then: Log data is consistent across platforms
        assertEquals(androidLog.id, syncedLog.id)
        assertEquals(androidLog.bbt, syncedLog.bbt)
        assertEquals(androidLog.periodFlow, syncedLog.periodFlow)
        assertEquals(androidLog.symptoms, syncedLog.symptoms)
        assertEquals(androidLog.mood, syncedLog.mood)
        
        // Verify sync history
        val syncHistory = backendServices.getSyncHistory()
        assertEquals(2, syncHistory.size)
        assertTrue(syncHistory.all { it.success })
    }
    
    @Test
    fun `preferences sync correctly across all platforms`() = runTest {
        // Given: User sets preferences on iOS
        val iosPreferences = UserPreferences(
            userId = testUserId,
            unitSystem = UnitSystem.IMPERIAL,
            isManuallySet = true,
            lastModified = Clock.System.now(),
            syncStatus = SyncStatus.PENDING
        )
        
        // Store locally on iOS
        iosDevice.storeLocalData("preferences", iosPreferences)
        
        // Sync to backend
        val iosBackendResult = backendServices.storePreferences(iosPreferences)
        assertTrue(iosBackendResult.isSuccess)
        backendServices.recordSyncEvent(iosDevice.deviceId, testUserId, "UPDATE", "PREFERENCES", true)
        
        // When: User opens Android app
        val androidGetResult = backendServices.getPreferences(testUserId)
        assertTrue(androidGetResult.isSuccess)
        val androidPreferences = androidGetResult.getOrNull()!!
        
        // Store locally on Android
        androidDevice.storeLocalData("preferences", androidPreferences)
        
        // Then: Preferences are consistent
        assertEquals(iosPreferences.unitSystem, androidPreferences.unitSystem)
        assertEquals(iosPreferences.isManuallySet, androidPreferences.isManuallySet)
        
        // When: User changes preferences on Android
        val updatedPreferences = androidPreferences.copy(
            unitSystem = UnitSystem.METRIC,
            lastModified = Clock.System.now()
        )
        
        androidDevice.storeLocalData("preferences", updatedPreferences)
        val androidBackendResult = backendServices.storePreferences(updatedPreferences)
        assertTrue(androidBackendResult.isSuccess)
        backendServices.recordSyncEvent(androidDevice.deviceId, testUserId, "UPDATE", "PREFERENCES", true)
        
        // When: User opens web app
        val webGetResult = backendServices.getPreferences(testUserId)
        assertTrue(webGetResult.isSuccess)
        val webPreferences = webGetResult.getOrNull()!!
        
        // Then: Web app gets the updated preferences
        assertEquals(UnitSystem.METRIC, webPreferences.unitSystem)
        assertEquals(updatedPreferences.lastModified, webPreferences.lastModified)
        
        // When: iOS app syncs again
        val iosUpdatedResult = backendServices.getPreferences(testUserId)
        assertTrue(iosUpdatedResult.isSuccess)
        val iosUpdatedPreferences = iosUpdatedResult.getOrNull()!!
        
        // Then: iOS gets the updated preferences
        assertEquals(UnitSystem.METRIC, iosUpdatedPreferences.unitSystem)
        
        // Verify all platforms have consistent state
        val finalIosPrefs = iosDevice.getLocalData("preferences") as? UserPreferences
        val finalAndroidPrefs = androidDevice.getLocalData("preferences") as? UserPreferences
        val finalWebPrefs = webDevice.getLocalData("preferences") as? UserPreferences
        
        // Update local storage to simulate sync
        iosDevice.storeLocalData("preferences", iosUpdatedPreferences)
        webDevice.storeLocalData("preferences", webPreferences)
        
        val updatedIosPrefs = iosDevice.getLocalData("preferences") as UserPreferences
        val updatedAndroidPrefs = androidDevice.getLocalData("preferences") as UserPreferences
        val updatedWebPrefs = webDevice.getLocalData("preferences") as UserPreferences
        
        assertEquals(updatedAndroidPrefs.unitSystem, updatedIosPrefs.unitSystem)
        assertEquals(updatedAndroidPrefs.unitSystem, updatedWebPrefs.unitSystem)
    }
    
    @Test
    fun `data consistency maintained during platform switching`() = runTest {
        // Simulate user switching between platforms throughout the day
        
        // Morning: User logs on iOS
        val morningLog = DailyLog(
            id = "morning-log",
            userId = testUserId,
            date = LocalDate(2024, 1, 15),
            bbt = 97.8,
            mood = Mood.HAPPY,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        iosDevice.mockServices.logRepository.saveDailyLog(morningLog)
        backendServices.storeDailyLog(morningLog)
        backendServices.recordSyncEvent(iosDevice.deviceId, testUserId, "CREATE", "LOG", true)
        
        // Afternoon: User opens Android and adds symptoms
        val androidGetResult = backendServices.getDailyLog(testUserId, LocalDate(2024, 1, 15))
        assertTrue(androidGetResult.isSuccess)
        val retrievedLog = androidGetResult.getOrNull()!!
        
        val updatedLog = retrievedLog.copy(
            symptoms = listOf(Symptom.CRAMPS, Symptom.BLOATING),
            updatedAt = Clock.System.now()
        )
        
        androidDevice.mockServices.logRepository.saveDailyLog(updatedLog)
        backendServices.storeDailyLog(updatedLog)
        backendServices.recordSyncEvent(androidDevice.deviceId, testUserId, "UPDATE", "LOG", true)
        
        // Evening: User opens web app and adds period flow
        val webGetResult = backendServices.getDailyLog(testUserId, LocalDate(2024, 1, 15))
        assertTrue(webGetResult.isSuccess)
        val webLog = webGetResult.getOrNull()!!
        
        val finalLog = webLog.copy(
            periodFlow = PeriodFlow.HEAVY,
            updatedAt = Clock.System.now()
        )
        
        webDevice.mockServices.logRepository.saveDailyLog(finalLog)
        backendServices.storeDailyLog(finalLog)
        backendServices.recordSyncEvent(webDevice.deviceId, testUserId, "UPDATE", "LOG", true)
        
        // Night: User checks iOS app
        val iosNightResult = backendServices.getDailyLog(testUserId, LocalDate(2024, 1, 15))
        assertTrue(iosNightResult.isSuccess)
        val finalIosLog = iosNightResult.getOrNull()!!
        
        // Verify all data is present and consistent
        assertEquals(morningLog.bbt, finalIosLog.bbt) // Original iOS data
        assertEquals(updatedLog.symptoms, finalIosLog.symptoms) // Android additions
        assertEquals(finalLog.periodFlow, finalIosLog.periodFlow) // Web additions
        
        // Verify sync history shows all operations
        val syncHistory = backendServices.getSyncHistory()
        assertEquals(3, syncHistory.size)
        assertTrue(syncHistory.all { it.success })
        
        val operations = syncHistory.map { it.operation }
        assertTrue(operations.contains("CREATE"))
        assertTrue(operations.count { it == "UPDATE" } == 2)
    }
    
    @Test
    fun `concurrent data modifications across platforms resolve correctly`() = runTest {
        // Given: Same log exists on multiple platforms
        val baseLog = DailyLog(
            id = "concurrent-log",
            userId = testUserId,
            date = LocalDate(2024, 1, 15),
            bbt = 98.0,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        // Store on all platforms
        iosDevice.mockServices.logRepository.saveDailyLog(baseLog)
        androidDevice.mockServices.logRepository.saveDailyLog(baseLog)
        webDevice.mockServices.logRepository.saveDailyLog(baseLog)
        backendServices.storeDailyLog(baseLog)
        
        // When: All platforms make concurrent modifications
        val timestamp1 = Clock.System.now()
        delay(10) // Ensure different timestamps
        val timestamp2 = Clock.System.now()
        delay(10)
        val timestamp3 = Clock.System.now()
        
        val iosModification = baseLog.copy(
            mood = Mood.HAPPY,
            updatedAt = timestamp1
        )
        
        val androidModification = baseLog.copy(
            symptoms = listOf(Symptom.CRAMPS),
            updatedAt = timestamp2
        )
        
        val webModification = baseLog.copy(
            periodFlow = PeriodFlow.MEDIUM,
            updatedAt = timestamp3 // Latest timestamp
        )
        
        // Simulate concurrent syncs (latest timestamp wins)
        backendServices.storeDailyLog(iosModification)
        backendServices.recordSyncEvent(iosDevice.deviceId, testUserId, "UPDATE", "LOG", true)
        
        backendServices.storeDailyLog(androidModification)
        backendServices.recordSyncEvent(androidDevice.deviceId, testUserId, "UPDATE", "LOG", true)
        
        backendServices.storeDailyLog(webModification)
        backendServices.recordSyncEvent(webDevice.deviceId, testUserId, "UPDATE", "LOG", true)
        
        // Then: Latest modification wins (web modification)
        val finalResult = backendServices.getDailyLog(testUserId, LocalDate(2024, 1, 15))
        assertTrue(finalResult.isSuccess)
        val finalLog = finalResult.getOrNull()!!
        
        assertEquals(webModification.periodFlow, finalLog.periodFlow)
        assertEquals(timestamp3, finalLog.updatedAt)
        
        // Verify sync history
        val syncHistory = backendServices.getSyncHistory()
        val updateOperations = syncHistory.filter { it.operation == "UPDATE" }
        assertEquals(3, updateOperations.size)
        assertTrue(updateOperations.all { it.success })
    }
    
    @Test
    fun `platform-specific data formats are handled correctly`() = runTest {
        // Given: Different platforms may format data differently
        
        // iOS uses specific date/time formats
        val iosLog = DailyLog(
            id = "ios-format-log",
            userId = testUserId,
            date = LocalDate(2024, 1, 15),
            bbt = 98.6, // Fahrenheit-style precision
            notes = "iOS formatted notes",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        // Android uses different formats
        val androidLog = DailyLog(
            id = "android-format-log",
            userId = testUserId,
            date = LocalDate(2024, 1, 16),
            bbt = 37.0, // Celsius-style precision
            notes = "Android formatted notes",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        // Web uses another format
        val webLog = DailyLog(
            id = "web-format-log",
            userId = testUserId,
            date = LocalDate(2024, 1, 17),
            bbt = 98.2,
            notes = "Web formatted notes",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        // When: All platforms sync their data
        val iosResult = backendServices.storeDailyLog(iosLog)
        val androidResult = backendServices.storeDailyLog(androidLog)
        val webResult = backendServices.storeDailyLog(webLog)
        
        assertTrue(iosResult.isSuccess)
        assertTrue(androidResult.isSuccess)
        assertTrue(webResult.isSuccess)
        
        // Then: All platforms can retrieve and understand each other's data
        val iosRetrieved = backendServices.getDailyLog(testUserId, LocalDate(2024, 1, 15))
        val androidRetrieved = backendServices.getDailyLog(testUserId, LocalDate(2024, 1, 16))
        val webRetrieved = backendServices.getDailyLog(testUserId, LocalDate(2024, 1, 17))
        
        assertTrue(iosRetrieved.isSuccess)
        assertTrue(androidRetrieved.isSuccess)
        assertTrue(webRetrieved.isSuccess)
        
        // Verify data integrity is maintained
        assertEquals(iosLog.bbt, iosRetrieved.getOrNull()?.bbt)
        assertEquals(androidLog.bbt, androidRetrieved.getOrNull()?.bbt)
        assertEquals(webLog.bbt, webRetrieved.getOrNull()?.bbt)
        
        assertEquals(iosLog.notes, iosRetrieved.getOrNull()?.notes)
        assertEquals(androidLog.notes, androidRetrieved.getOrNull()?.notes)
        assertEquals(webLog.notes, webRetrieved.getOrNull()?.notes)
    }
    
    @Test
    fun `large dataset synchronization works across platforms`() = runTest {
        // Given: Large amount of data to sync
        val largeDailyLogs = (1..100).map { day ->
            DailyLog(
                id = "bulk-log-$day",
                userId = testUserId,
                date = LocalDate(2024, 1, day % 28 + 1),
                bbt = 97.0 + (day % 10) * 0.1,
                periodFlow = if (day % 28 <= 5) PeriodFlow.MEDIUM else null,
                symptoms = if (day % 7 == 0) listOf(Symptom.CRAMPS) else emptyList(),
                mood = if (day % 2 == 0) Mood.HAPPY else Mood.NEUTRAL,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            )
        }
        
        // When: iOS device syncs large dataset
        var successCount = 0
        largeDailyLogs.forEach { log ->
            val result = backendServices.storeDailyLog(log)
            if (result.isSuccess) {
                successCount++
                backendServices.recordSyncEvent(iosDevice.deviceId, testUserId, "CREATE", "LOG", true)
            }
        }
        
        // Then: All data is synced successfully
        assertEquals(100, successCount)
        assertEquals(100, backendServices.getLogCount())
        
        // When: Android device retrieves all data
        var retrievedCount = 0
        (1..28).forEach { day ->
            val result = backendServices.getDailyLog(testUserId, LocalDate(2024, 1, day))
            if (result.isSuccess && result.getOrNull() != null) {
                retrievedCount++
            }
        }
        
        // Then: Android can retrieve all synced data
        assertTrue(retrievedCount > 0, "Android should be able to retrieve synced data")
        
        // Verify sync history
        val syncHistory = backendServices.getSyncHistory()
        assertEquals(100, syncHistory.size)
        assertTrue(syncHistory.all { it.success })
    }
    
    @Test
    fun `network failures during cross-platform sync are handled gracefully`() = runTest {
        // Given: Backend has intermittent failures
        backendServices.setFailureRate(0.3) // 30% failure rate
        
        val testLogs = (1..20).map { day ->
            DailyLog(
                id = "failure-test-log-$day",
                userId = testUserId,
                date = LocalDate(2024, 1, day),
                bbt = 98.0,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            )
        }
        
        // When: iOS attempts to sync all logs
        var successfulSyncs = 0
        var failedSyncs = 0
        
        testLogs.forEach { log ->
            val result = backendServices.storeDailyLog(log)
            if (result.isSuccess) {
                successfulSyncs++
                backendServices.recordSyncEvent(iosDevice.deviceId, testUserId, "CREATE", "LOG", true)
            } else {
                failedSyncs++
                backendServices.recordSyncEvent(iosDevice.deviceId, testUserId, "CREATE", "LOG", false)
            }
        }
        
        // Then: Some syncs succeed, some fail
        assertTrue(successfulSyncs > 0, "Some syncs should succeed")
        assertTrue(failedSyncs > 0, "Some syncs should fail due to network issues")
        assertEquals(20, successfulSyncs + failedSyncs)
        
        // When: Network stabilizes and retries occur
        backendServices.setFailureRate(0.0) // No more failures
        
        var retrySuccesses = 0
        testLogs.forEach { log ->
            val result = backendServices.storeDailyLog(log)
            if (result.isSuccess) {
                retrySuccesses++
            }
        }
        
        // Then: All retries succeed
        assertEquals(20, retrySuccesses)
        
        // Verify sync history includes both failures and successes
        val syncHistory = backendServices.getSyncHistory()
        val successfulEvents = syncHistory.count { it.success }
        val failedEvents = syncHistory.count { !it.success }
        
        assertTrue(successfulEvents > 0)
        assertTrue(failedEvents > 0)
    }
}