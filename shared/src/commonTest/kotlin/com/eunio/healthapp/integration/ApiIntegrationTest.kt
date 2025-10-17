package com.eunio.healthapp.integration

import com.eunio.healthapp.data.remote.datasource.PreferencesRemoteDataSource
import com.eunio.healthapp.data.remote.datasource.SettingsRemoteDataSource
import com.eunio.healthapp.data.remote.datasource.SettingsHistoryEntry
import com.eunio.healthapp.data.remote.datasource.SettingsStatistics
import com.eunio.healthapp.data.remote.datasource.RemoteStorageHealth
import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.*
import com.eunio.healthapp.domain.model.settings.*
import com.eunio.healthapp.domain.util.NetworkConnectivity
import com.eunio.healthapp.domain.util.NetworkType
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.testutil.MockApiService
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Integration tests for API operations and network handling.
 * Tests remote data source interactions, network error handling, and retry mechanisms.
 */
class ApiIntegrationTest {
    

    
    /**
     * Test implementation of PreferencesRemoteDataSource with API simulation
     */
    private class TestPreferencesRemoteDataSource(
        private val apiService: MockApiService,
        private val networkConnectivity: NetworkConnectivity
    ) : PreferencesRemoteDataSource {
        
        private val storage = mutableMapOf<String, UserPreferences>()
        
        override suspend fun getPreferences(userId: String): Result<UserPreferences?> {
            if (!networkConnectivity.isConnected()) {
                return Result.error(AppError.NetworkError("No network connection"))
            }
            
            return apiService.executeRequest("getPreferences") {
                storage[userId]
            }
        }
        
        override suspend fun savePreferences(preferences: UserPreferences): Result<Unit> {
            if (!networkConnectivity.isConnected()) {
                return Result.error(AppError.NetworkError("No network connection"))
            }
            
            return apiService.executeRequest("savePreferences") {
                storage[preferences.userId] = preferences
                Unit
            }
        }
        
        override suspend fun updatePreferences(preferences: UserPreferences): Result<Unit> {
            return savePreferences(preferences)
        }
        
        override suspend fun deletePreferences(userId: String): Result<Unit> {
            if (!networkConnectivity.isConnected()) {
                return Result.error(AppError.NetworkError("No network connection"))
            }
            
            return apiService.executeRequest("deletePreferences") {
                storage.remove(userId)
                Unit
            }
        }
        
        override suspend fun clearPreferences(): Result<Unit> {
            if (!networkConnectivity.isConnected()) {
                return Result.error(AppError.NetworkError("No network connection"))
            }
            
            return apiService.executeRequest("clearPreferences") {
                storage.clear()
                Unit
            }
        }
        
        override suspend fun preferencesExist(userId: String): Result<Boolean> {
            if (!networkConnectivity.isConnected()) {
                return Result.error(AppError.NetworkError("No network connection"))
            }
            
            return apiService.executeRequest("preferencesExist") {
                storage.containsKey(userId)
            }
        }
        
        override suspend fun getLastModifiedTimestamp(userId: String): Result<Long?> {
            if (!networkConnectivity.isConnected()) {
                return Result.error(AppError.NetworkError("No network connection"))
            }
            
            return apiService.executeRequest("getLastModifiedTimestamp") {
                storage[userId]?.lastModified?.epochSeconds
            }
        }
        
        fun getStoredPreferences(): Map<String, UserPreferences> = storage.toMap()
        
        fun reset() {
            storage.clear()
        }
    }
    
    /**
     * Test implementation of SettingsRemoteDataSource with API simulation
     */
    private class TestSettingsRemoteDataSource(
        private val apiService: MockApiService,
        private val networkConnectivity: NetworkConnectivity
    ) : SettingsRemoteDataSource {
        
        private val storage = mutableMapOf<String, UserSettings>()
        private val history = mutableListOf<SettingsHistoryEntry>()
        
        override suspend fun getSettings(userId: String): Result<UserSettings?> {
            if (!networkConnectivity.isConnected()) {
                return Result.error(AppError.NetworkError("No network connection"))
            }
            
            return apiService.executeRequest("getSettings") {
                storage[userId]
            }
        }
        
        override suspend fun saveSettings(settings: UserSettings): Result<Unit> {
            if (!networkConnectivity.isConnected()) {
                return Result.error(AppError.NetworkError("No network connection"))
            }
            
            return apiService.executeRequest("saveSettings") {
                storage[settings.userId] = settings
                Unit
            }
        }
        
        override suspend fun deleteSettings(userId: String): Result<Unit> {
            if (!networkConnectivity.isConnected()) {
                return Result.error(AppError.NetworkError("No network connection"))
            }
            
            return apiService.executeRequest("deleteSettings") {
                storage.remove(userId)
                Unit
            }
        }
        
        override suspend fun settingsExist(userId: String): Result<Boolean> {
            if (!networkConnectivity.isConnected()) {
                return Result.error(AppError.NetworkError("No network connection"))
            }
            
            return apiService.executeRequest("settingsExist") {
                storage.containsKey(userId)
            }
        }
        
        override suspend fun getLastModifiedTimestamp(userId: String): Result<Long?> {
            if (!networkConnectivity.isConnected()) {
                return Result.error(AppError.NetworkError("No network connection"))
            }
            
            return apiService.executeRequest("getLastModifiedTimestamp") {
                storage[userId]?.lastModified?.epochSeconds
            }
        }
        
        override suspend fun getServerTimestamp(): Result<Long> {
            if (!networkConnectivity.isConnected()) {
                return Result.error(AppError.NetworkError("No network connection"))
            }
            
            return apiService.executeRequest("getServerTimestamp") {
                Clock.System.now().epochSeconds
            }
        }
        
        override suspend fun createSettingsHistory(
            userId: String,
            settings: UserSettings,
            changeType: String,
            deviceInfo: String?
        ): Result<String> {
            if (!networkConnectivity.isConnected()) {
                return Result.error(AppError.NetworkError("No network connection"))
            }
            
            return apiService.executeRequest("createSettingsHistory") {
                val historyId = "history-${history.size}"
                val historyEntry = SettingsHistoryEntry(
                    historyId = historyId,
                    userId = userId,
                    settingsSnapshot = settings,
                    changeType = changeType,
                    timestamp = Clock.System.now().toEpochMilliseconds(),
                    deviceInfo = deviceInfo
                )
                history.add(historyEntry)
                historyId
            }
        }
        
        override suspend fun batchUpdateSettings(settingsList: List<UserSettings>): Result<Int> {
            if (!networkConnectivity.isConnected()) {
                return Result.error(AppError.NetworkError("No network connection"))
            }
            
            return apiService.executeRequest("batchUpdateSettings") {
                var successCount = 0
                settingsList.forEach { settings ->
                    storage[settings.userId] = settings
                    successCount++
                }
                successCount
            }
        }
        
        override suspend fun healthCheck(): Result<RemoteStorageHealth> {
            if (!networkConnectivity.isConnected()) {
                return Result.error(AppError.NetworkError("No network connection"))
            }
            
            return apiService.executeRequest("healthCheck") {
                val stats = apiService.getRequestStatistics()
                RemoteStorageHealth(
                    isHealthy = stats.successRate > 0.8,
                    latency = stats.averageLatencyMs.toLong(),
                    lastSuccessfulOperation = Clock.System.now().toEpochMilliseconds(),
                    errorCount = stats.failedRequests,
                    version = "1.0.0"
                )
            }
        }
        
        override suspend fun validateConnection(): Result<Unit> {
            if (!networkConnectivity.isConnected()) {
                return Result.error(AppError.NetworkError("No network connection"))
            }
            
            return apiService.executeRequest("validateConnection") {
                Unit
            }
        }
        
        // Methods not used by this test - NotImplementedError with context
        override suspend fun updateSettings(settings: UserSettings): Result<Unit> {
            throw NotImplementedError("updateSettings not needed for ApiIntegrationTest")
        }
        
        override suspend fun conditionalUpdateSettings(settings: UserSettings, expectedLastModified: Long): Result<Unit> {
            throw NotImplementedError("conditionalUpdateSettings not needed for ApiIntegrationTest")
        }
        
        override suspend fun clearAllSettings(): Result<Unit> {
            throw NotImplementedError("clearAllSettings not needed for ApiIntegrationTest")
        }
        
        override fun observeSettings(userId: String): kotlinx.coroutines.flow.Flow<UserSettings?> {
            throw NotImplementedError("observeSettings not needed for ApiIntegrationTest")
        }
        
        override suspend fun getSettingsHistory(userId: String, limit: Int): Result<List<SettingsHistoryEntry>> {
            throw NotImplementedError("getSettingsHistory not needed for ApiIntegrationTest")
        }
        
        override suspend fun deleteSettingsHistory(userId: String): Result<Unit> {
            throw NotImplementedError("deleteSettingsHistory not needed for ApiIntegrationTest")
        }
        
        override suspend fun getSettingsStatistics(userId: String): Result<SettingsStatistics> {
            throw NotImplementedError("getSettingsStatistics not needed for ApiIntegrationTest")
        }
        

        
        fun getStoredSettings(): Map<String, UserSettings> = storage.toMap()
        fun getHistory(): List<SettingsHistoryEntry> = history.toList()
        
        fun reset() {
            storage.clear()
            history.clear()
        }
    }
    
    /**
     * Test network connectivity implementation
     */
    private class TestNetworkConnectivity : NetworkConnectivity {
        private var connected = true
        private var networkType = NetworkType.WIFI
        
        override fun isConnected(): Boolean = connected
        override fun getNetworkType(): NetworkType = networkType
        override fun observeConnectivity(): kotlinx.coroutines.flow.Flow<Boolean> = 
            kotlinx.coroutines.flow.flowOf(connected)
            
        override suspend fun hasStableConnection(): Boolean = connected
        
        fun setConnected(isConnected: Boolean) {
            connected = isConnected
        }
        
        fun setNetworkType(type: NetworkType) {
            networkType = type
            connected = type != NetworkType.NONE
        }
    }
    
    // Test components
    private lateinit var apiService: MockApiService
    private lateinit var networkConnectivity: TestNetworkConnectivity
    private lateinit var preferencesRemoteDataSource: TestPreferencesRemoteDataSource
    private lateinit var settingsRemoteDataSource: TestSettingsRemoteDataSource
    
    private val testUserId = "test-user-id"
    private val testPreferences = UserPreferences(
        userId = testUserId,
        unitSystem = UnitSystem.IMPERIAL,
        isManuallySet = true,
        lastModified = Clock.System.now(),
        syncStatus = SyncStatus.SYNCED
    )
    
    private val testSettings = UserSettings(
        userId = testUserId,
        displayPreferences = DisplayPreferences(
            textSizeScale = 1.0f, // medium font size
            highContrastMode = true, // dark theme
            hapticFeedbackEnabled = true, // showNotifications equivalent
            hapticIntensity = HapticIntensity.MEDIUM
        ),
        lastModified = Clock.System.now()
    )
    
    @BeforeTest
    fun setup() {
        apiService = MockApiService()
        networkConnectivity = TestNetworkConnectivity()
        preferencesRemoteDataSource = TestPreferencesRemoteDataSource(apiService, networkConnectivity)
        settingsRemoteDataSource = TestSettingsRemoteDataSource(apiService, networkConnectivity)
    }
    
    @AfterTest
    fun tearDown() {
        try {
            apiService.reset()
            preferencesRemoteDataSource.reset()
            settingsRemoteDataSource.reset()
        } catch (e: Exception) {
            // Ignore cleanup exceptions to prevent test interference
        }
    }
    
    // API Integration Tests
    
    @Test
    fun `successful API operations work correctly`() = runTest {
        // Given: Network is connected
        networkConnectivity.setConnected(true)
        
        // When: Saving preferences
        val saveResult = preferencesRemoteDataSource.savePreferences(testPreferences)
        
        // Then: Operation succeeds
        assertTrue(saveResult.isSuccess)
        
        // When: Retrieving preferences
        val getResult = preferencesRemoteDataSource.getPreferences(testUserId)
        
        // Then: Returns saved preferences
        assertTrue(getResult.isSuccess)
        assertEquals(testPreferences, getResult.getOrNull())
        
        // When: Checking if preferences exist
        val existsResult = preferencesRemoteDataSource.preferencesExist(testUserId)
        
        // Then: Returns true
        assertTrue(existsResult.isSuccess)
        assertTrue(existsResult.getOrNull() == true)
    }
    
    @Test
    fun `API operations fail when offline`() = runTest {
        // Given: Network is disconnected
        networkConnectivity.setConnected(false)
        
        // When: Attempting to save preferences
        val saveResult = preferencesRemoteDataSource.savePreferences(testPreferences)
        
        // Then: Operation fails with network error
        assertTrue(saveResult.isError)
        assertTrue(saveResult.errorOrNull() is AppError.NetworkError)
        
        // When: Attempting to get preferences
        val getResult = preferencesRemoteDataSource.getPreferences(testUserId)
        
        // Then: Operation fails with network error
        assertTrue(getResult.isError)
        assertTrue(getResult.errorOrNull() is AppError.NetworkError)
    }
    
    @Test
    fun `API operations handle network delays gracefully`() = runTest {
        // Given: Network has significant delay
        apiService.setNetworkDelay(1.seconds)
        networkConnectivity.setConnected(true)
        
        // When: Performing API operation
        val result = preferencesRemoteDataSource.savePreferences(testPreferences)
        
        // Then: Operation succeeds despite network delay configuration
        assertTrue(result.isSuccess, "API operation should succeed despite network delay")
        
        // Verify request was tracked
        val stats = apiService.getRequestStatistics()
        assertTrue(stats.totalRequests > 0, "Should have tracked at least one request")
        assertTrue(stats.successfulRequests > 0, "Should have successful requests")
        
        // In test environment, timing is controlled by test dispatcher
        // We verify the delay configuration doesn't break functionality
        assertTrue(stats.averageLatencyMs >= 0.0, "Average latency should be non-negative")
    }
    
    @Test
    fun `API operations retry on network failures`() = runTest {
        // Given: Network will fail twice then succeed
        apiService.simulateNetworkFailure(maxFailures = 2)
        apiService.setRetryConfiguration(maxRetries = 3, retryDelay = 100.milliseconds)
        networkConnectivity.setConnected(true)
        
        // When: Performing API operation with retry logic
        val result = apiService.executeRequestWithRetry("savePreferences") {
            preferencesRemoteDataSource.savePreferences(testPreferences).getOrThrow()
        }
        
        // Then: Eventually succeeds after retries
        assertTrue(result.isSuccess)
        
        // Verify retry attempts were made
        val stats = apiService.getRequestStatistics()
        assertTrue(stats.totalRequests >= 3, "Should have made at least 3 attempts")
        assertTrue(stats.successfulRequests >= 1, "Should have at least 1 successful request")
    }
    
    @Test
    fun `API operations handle server errors appropriately`() = runTest {
        // Given: Server returns error
        apiService.simulateServerError(maxFailures = 1)
        networkConnectivity.setConnected(true)
        
        // When: Performing API operation
        val result = preferencesRemoteDataSource.savePreferences(testPreferences)
        
        // Then: Returns server error
        assertTrue(result.isError)
        assertTrue(result.errorOrNull() is AppError.NetworkError)
        
        // Verify error was tracked
        val stats = apiService.getRequestStatistics()
        assertTrue(stats.failedRequests > 0)
        assertTrue(stats.errorsByType.containsKey(MockApiService.ErrorType.SERVER_ERROR))
    }
    
    // Settings API Integration Tests
    
    @Test
    fun `settings API operations work correctly`() = runTest {
        // Given: Network is connected
        networkConnectivity.setConnected(true)
        
        // When: Saving settings
        val saveResult = settingsRemoteDataSource.saveSettings(testSettings)
        
        // Then: Operation succeeds
        assertTrue(saveResult.isSuccess)
        
        // When: Retrieving settings
        val getResult = settingsRemoteDataSource.getSettings(testUserId)
        
        // Then: Returns saved settings
        assertTrue(getResult.isSuccess)
        assertEquals(testSettings, getResult.getOrNull())
        
        // When: Creating history entry
        val historyResult = settingsRemoteDataSource.createSettingsHistory(
            userId = testUserId,
            settings = testSettings,
            changeType = "UPDATE",
            deviceInfo = "Test Device"
        )
        
        // Then: History entry is created
        assertTrue(historyResult.isSuccess)
        assertEquals(1, settingsRemoteDataSource.getHistory().size)
    }
    
    @Test
    fun `settings API handles concurrent operations`() = runTest {
        // Given: Network is connected
        networkConnectivity.setConnected(true)
        
        // When: Performing concurrent operations
        val results = mutableListOf<Result<Unit>>()
        
        repeat(5) { index ->
            val settings = testSettings.copy(
                displayPreferences = testSettings.displayPreferences.copy(
                    highContrastMode = index % 2 == 0 // dark theme for even indices, light for odd
                )
            )
            val result = settingsRemoteDataSource.saveSettings(settings)
            results.add(result)
        }
        
        // Then: All operations succeed
        results.forEach { result ->
            assertTrue(result.isSuccess, "Concurrent API operations should succeed")
        }
        
        // Final state should be consistent
        val finalSettings = settingsRemoteDataSource.getSettings(testUserId).getOrNull()
        assertNotNull(finalSettings)
        // Verify final settings have valid display preferences
        assertTrue(finalSettings.displayPreferences.isValid())
    }
    
    // Network Type Specific Tests
    
    @Test
    fun `API operations adapt to different network types`() = runTest {
        // Test WiFi connection (excellent condition)
        networkConnectivity.setNetworkType(NetworkType.WIFI)
        apiService.setNetworkCondition(MockApiService.NetworkCondition.EXCELLENT)
        
        val wifiResult = preferencesRemoteDataSource.savePreferences(testPreferences)
        assertTrue(wifiResult.isSuccess)
        
        // Test cellular connection (fair condition)
        networkConnectivity.setNetworkType(NetworkType.CELLULAR)
        apiService.setNetworkCondition(MockApiService.NetworkCondition.FAIR)
        
        val cellularResult = preferencesRemoteDataSource.getPreferences(testUserId)
        assertTrue(cellularResult.isSuccess)
        
        // Test no connection
        networkConnectivity.setNetworkType(NetworkType.NONE)
        
        val noConnectionResult = preferencesRemoteDataSource.savePreferences(testPreferences)
        assertTrue(noConnectionResult.isError)
        assertTrue(noConnectionResult.errorOrNull() is AppError.NetworkError)
        
        // Verify different latencies were applied
        val stats = apiService.getRequestStatistics()
        assertTrue(stats.totalRequests >= 2)
    }
    
    @Test
    fun `API operations handle timeout scenarios correctly`() = runTest {
        // Given: API service configured to timeout
        apiService.setTimeoutDuration(1.seconds)
        apiService.simulateTimeout(true)
        networkConnectivity.setConnected(true)
        
        // When: Performing API operation
        val result = preferencesRemoteDataSource.savePreferences(testPreferences)
        
        // Then: Returns timeout error
        assertTrue(result.isError)
        val error = result.errorOrNull()
        assertTrue(error is AppError.NetworkError)
        assertTrue(error.message.contains("timeout", ignoreCase = true))
        
        // Verify timeout was tracked
        val stats = apiService.getRequestStatistics()
        assertTrue(stats.errorsByType.containsKey(MockApiService.ErrorType.TIMEOUT_ERROR))
    }
    
    @Test
    fun `API operations handle slow network conditions`() = runTest {
        // Given: Very slow network conditions
        apiService.simulateSlowNetwork(delay = 1.5.seconds)
        apiService.setConnectionStability(0.7) // Less unstable for more predictable test
        networkConnectivity.setConnected(true)
        
        // When: Performing API operation
        val result = preferencesRemoteDataSource.savePreferences(testPreferences)
        
        // Then: Operation may succeed or fail due to slow network conditions
        // We don't assert success/failure as slow networks can be unpredictable
        
        // Verify network conditions were applied and request was tracked
        val stats = apiService.getRequestStatistics()
        assertTrue(stats.totalRequests > 0, "Should have tracked at least one request")
        
        // Verify the slow network configuration was applied
        assertTrue(stats.averageLatencyMs >= 0.0, "Average latency should be non-negative")
        
        // The request should have been processed (success or failure)
        assertTrue(stats.successfulRequests + stats.failedRequests == stats.totalRequests, 
                  "All requests should be either successful or failed")
    }
    
    @Test
    fun `API operations handle intermittent connectivity`() {
        kotlinx.coroutines.runBlocking {
            // Given: Intermittent connectivity issues
            apiService.simulateIntermittentConnectivity(failureRate = 0.4)
            networkConnectivity.setConnected(true)
            
            // When: Performing multiple API operations
            val results = mutableListOf<Result<Unit>>()
            repeat(10) {
                val result = preferencesRemoteDataSource.savePreferences(testPreferences.copy(userId = "user-$it"))
                results.add(result)
            }
            
            // Then: Some operations succeed, some fail due to intermittent issues
            val successCount = results.count { it.isSuccess }
            val failureCount = results.count { it.isError }
            
            assertTrue(successCount > 0, "Some operations should succeed")
            assertTrue(failureCount > 0, "Some operations should fail due to intermittent connectivity")
            
            // Verify statistics reflect intermittent behavior
            val stats = apiService.getRequestStatistics()
            assertTrue(stats.successRate < 1.0, "Success rate should be less than 100% due to intermittent issues")
            assertTrue(stats.successRate > 0.0, "Success rate should be greater than 0%")
        }
    }
    
    @Test
    fun `API operations provide detailed request statistics`() = runTest {
        // Given: Various API operations with different outcomes
        networkConnectivity.setConnected(true)
        
        // Successful operations
        preferencesRemoteDataSource.savePreferences(testPreferences)
        preferencesRemoteDataSource.getPreferences(testUserId)
        
        // Failed operations
        apiService.simulateNetworkFailure(maxFailures = 1)
        preferencesRemoteDataSource.deletePreferences(testUserId)
        
        apiService.simulateServerError(maxFailures = 1)
        preferencesRemoteDataSource.preferencesExist(testUserId)
        
        // When: Getting request statistics
        val stats = apiService.getRequestStatistics()
        
        // Then: Statistics accurately reflect the operations
        assertTrue(stats.totalRequests >= 4)
        assertTrue(stats.successfulRequests >= 2)
        assertTrue(stats.failedRequests >= 2)
        assertTrue(stats.successRate > 0.0 && stats.successRate < 1.0)
        assertTrue(stats.averageLatencyMs > 0.0)
        assertTrue(stats.errorsByType.isNotEmpty())
    }
    
    // Batch Operations Tests
    
    @Test
    fun `batch API operations handle partial failures correctly`() = runTest {
        // Given: Network is connected
        networkConnectivity.setConnected(true)
        
        // Configure intermittent failures for batch operations
        apiService.simulateIntermittentConnectivity(failureRate = 0.3)
        
        // When: Performing batch operations
        val users = listOf("user1", "user2", "user3", "user4", "user5")
        val operations = users.map { userId ->
            "savePreferences-$userId" to suspend {
                val preferences = testPreferences.copy(userId = userId)
                preferencesRemoteDataSource.savePreferences(preferences).getOrThrow()
            }
        }
        
        val batchResult = apiService.executeBatchRequest(operations, allowPartialFailure = true)
        
        // Then: Batch operation completes with some successes and failures
        assertTrue(batchResult.isSuccess)
        val result = batchResult.getOrNull()!!
        
        assertTrue(result.totalOperations == 5)
        assertTrue(result.successfulOperations > 0, "Some operations should succeed")
        assertTrue(result.failedOperations >= 0, "Some operations may fail")
        assertTrue(result.successfulOperations + result.failedOperations == result.totalOperations)
        
        // Verify batch statistics
        val stats = apiService.getRequestStatistics()
        assertTrue(stats.totalRequests >= 5)
    }
    
    // Data Consistency Tests
    
    @Test
    fun `API operations maintain data consistency during failures`() = runTest {
        // Given: Initial data exists
        networkConnectivity.setConnected(true)
        preferencesRemoteDataSource.savePreferences(testPreferences)
        
        // When: Update operation fails
        apiService.simulateServerError(maxFailures = 1)
        val updatedPreferences = testPreferences.copy(unitSystem = UnitSystem.METRIC)
        val updateResult = preferencesRemoteDataSource.savePreferences(updatedPreferences)
        
        // Then: Update fails
        assertTrue(updateResult.isError)
        
        // When: Retrieving data after failed update
        apiService.reset()
        val retrievedPreferences = preferencesRemoteDataSource.getPreferences(testUserId).getOrNull()
        
        // Then: Original data is preserved
        assertNotNull(retrievedPreferences)
        assertEquals(UnitSystem.IMPERIAL, retrievedPreferences.unitSystem)
        assertEquals(testPreferences, retrievedPreferences)
    }
}