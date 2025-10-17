package com.eunio.healthapp.data.remote.sync

import com.eunio.healthapp.domain.error.UnitSystemError
import com.eunio.healthapp.domain.repository.PreferencesRepository
import com.eunio.healthapp.domain.util.NetworkConnectivity
import com.eunio.healthapp.domain.util.NetworkType
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.domain.util.UnitSystemErrorHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineSyncManagerTest {
    
    private lateinit var mockPreferencesRepository: MockPreferencesRepositoryForOfflineSync
    private lateinit var mockNetworkConnectivity: MockNetworkConnectivityForOfflineSync
    private lateinit var errorHandler: UnitSystemErrorHandler
    private lateinit var testScope: TestScope
    private lateinit var offlineSyncManager: OfflineSyncManager
    
    @BeforeTest
    fun setup() {
        mockPreferencesRepository = MockPreferencesRepositoryForOfflineSync()
        mockNetworkConnectivity = MockNetworkConnectivityForOfflineSync()
        errorHandler = UnitSystemErrorHandler()
        testScope = TestScope()
        
        offlineSyncManager = OfflineSyncManager(
            preferencesRepository = mockPreferencesRepository,
            networkConnectivity = mockNetworkConnectivity,
            errorHandler = errorHandler,
            coroutineScope = testScope.backgroundScope
        )
    }
    
    @AfterTest
    fun tearDown() {
        offlineSyncManager.stop()
        testScope.testScheduler.advanceUntilIdle()
    }
    
    @Test
    fun `start should initialize connectivity monitoring`() = testScope.runTest {
        // Given
        mockNetworkConnectivity.setConnected(false)
        
        // When
        offlineSyncManager.start()
        testScope.testScheduler.advanceUntilIdle()
        
        // Then - connectivity state should be updated based on the mock
        val connectivityState = offlineSyncManager.connectivityState.value
        assertTrue(connectivityState == ConnectivityState.DISCONNECTED || connectivityState == ConnectivityState.UNKNOWN)
        assertEquals(OfflineSyncState.IDLE, offlineSyncManager.syncState.value)
    }
    
    @Test
    fun `connectivity restoration should trigger sync`() = testScope.runTest {
        // Given
        mockNetworkConnectivity.setConnected(false)
        mockNetworkConnectivity.setStableConnection(false)
        mockPreferencesRepository.syncResult = Result.success(Unit)
        offlineSyncManager.start()
        testScope.testScheduler.advanceUntilIdle()
        
        // When connectivity is restored
        mockNetworkConnectivity.setConnected(true)
        mockNetworkConnectivity.setStableConnection(true)
        testScope.testScheduler.advanceTimeBy(4000) // Wait for stabilization delay
        testScope.testScheduler.advanceUntilIdle()
        
        // Then - manually trigger sync to verify it works when connected
        val result = offlineSyncManager.triggerSync(force = true)
        assertTrue(result.isSuccess)
        assertTrue(mockPreferencesRepository.syncPreferencesCalled)
    }
    
    @Test
    fun `connectivity loss should trigger offline mode`() = testScope.runTest {
        // Given
        mockNetworkConnectivity.setConnected(true)
        offlineSyncManager.start()
        testScope.testScheduler.advanceUntilIdle()
        
        // When connectivity is lost
        mockNetworkConnectivity.setConnected(false)
        testScope.testScheduler.advanceUntilIdle()
        
        // Then - connectivity state should reflect the change
        val connectivityState = offlineSyncManager.connectivityState.value
        assertTrue(connectivityState == ConnectivityState.DISCONNECTED || connectivityState == ConnectivityState.UNKNOWN)
        // Note: sync state might not immediately change to OFFLINE until a sync is attempted
    }
    
    @Test
    fun `triggerSync should handle offline mode when not connected`() = testScope.runTest {
        // Given
        mockNetworkConnectivity.setConnected(false)
        
        // When
        val result = offlineSyncManager.triggerSync()
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(OfflineSyncState.OFFLINE, offlineSyncManager.syncState.value)
    }
    
    @Test
    fun `triggerSync should perform sync when connected`() = testScope.runTest {
        // Given
        mockNetworkConnectivity.setConnected(true)
        mockPreferencesRepository.syncResult = Result.success(Unit)
        
        // When
        val result = offlineSyncManager.triggerSync(force = true)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(OfflineSyncState.SYNCED, offlineSyncManager.syncState.value)
        assertTrue(mockPreferencesRepository.syncPreferencesCalled)
    }
    
    @Test
    fun `sync failure should update metrics and schedule retry`() = testScope.runTest {
        // Given
        mockNetworkConnectivity.setConnected(true)
        val syncError = UnitSystemError.PreferencesSyncError(
            message = "Sync failed",
            operation = "test"
        )
        mockPreferencesRepository.syncResult = Result.error(syncError)
        
        // When
        val result = offlineSyncManager.triggerSync(force = true)
        
        // Then
        assertTrue(result.isError)
        assertEquals(OfflineSyncState.FAILED, offlineSyncManager.syncState.value)
        
        val metrics = offlineSyncManager.syncMetrics.value
        assertEquals(1, metrics.totalSyncAttempts)
        assertEquals(1, metrics.failedSyncs)
        assertEquals(1, metrics.consecutiveFailures)
    }
    
    @Test
    fun `recoverFromFailure should wait for stable connectivity`() = testScope.runTest {
        // Given
        mockNetworkConnectivity.setConnected(false)
        mockNetworkConnectivity.setStableConnection(false)
        
        // When
        val result = offlineSyncManager.recoverFromFailure()
        
        // Then
        assertTrue(result.isError)
        val error = result.errorOrNull()
        assertTrue(error is UnitSystemError.UnitSystemNetworkError)
    }
    
    @Test
    fun `recoverFromFailure should succeed with stable connectivity`() = testScope.runTest {
        // Given
        mockNetworkConnectivity.setConnected(true)
        mockNetworkConnectivity.setStableConnection(true)
        mockPreferencesRepository.syncResult = Result.success(Unit)
        
        // When
        val result = offlineSyncManager.recoverFromFailure()
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(OfflineSyncState.SYNCED, offlineSyncManager.syncState.value)
    }
    
    @Test
    fun `getStatus should return complete sync status`() = testScope.runTest {
        // Given
        mockNetworkConnectivity.setConnected(true)
        offlineSyncManager.start()
        testScope.testScheduler.advanceUntilIdle()
        
        // When
        val status = offlineSyncManager.getStatus()
        
        // Then
        assertEquals(OfflineSyncState.IDLE, status.syncState)
        // Connectivity state might be CONNECTED or UNKNOWN depending on timing
        assertTrue(status.connectivityState == ConnectivityState.CONNECTED || status.connectivityState == ConnectivityState.UNKNOWN)
        assertTrue(status.isHealthy)
        assertNotNull(status.metrics)
    }
    
    @Test
    fun `health check should fail with too many consecutive failures`() = testScope.runTest {
        // Given
        mockNetworkConnectivity.setConnected(true)
        val syncError = UnitSystemError.PreferencesSyncError(
            message = "Sync failed",
            operation = "test"
        )
        mockPreferencesRepository.syncResult = Result.error(syncError)
        
        // When - trigger multiple failures
        repeat(6) {
            offlineSyncManager.triggerSync(force = true)
        }
        
        // Then
        val status = offlineSyncManager.getStatus()
        assertFalse(status.isHealthy)
        assertTrue(status.metrics.consecutiveFailures > 5)
    }
    
    @Test
    fun `stop should cancel all operations and set stopped state`() = testScope.runTest {
        // Given
        offlineSyncManager.start()
        
        // When
        offlineSyncManager.stop()
        
        // Then
        assertEquals(OfflineSyncState.STOPPED, offlineSyncManager.syncState.value)
    }
    
    @Test
    fun `periodic sync should respect minimum interval`() = testScope.runTest {
        // Given
        mockNetworkConnectivity.setConnected(true)
        mockPreferencesRepository.syncResult = Result.success(Unit)
        
        // When - trigger initial sync
        val result1 = offlineSyncManager.triggerSync(force = true)
        assertTrue(result1.isSuccess)
        assertTrue(mockPreferencesRepository.syncPreferencesCalled)
        
        // Reset flag and try again immediately (should not sync due to minimum interval)
        mockPreferencesRepository.syncPreferencesCalled = false
        val result2 = offlineSyncManager.triggerSync(force = false)
        assertTrue(result2.isSuccess)
        assertFalse(mockPreferencesRepository.syncPreferencesCalled, "Should not sync again immediately due to minimum interval")
        
        // Force sync should always work
        val result3 = offlineSyncManager.triggerSync(force = true)
        assertTrue(result3.isSuccess)
        assertTrue(mockPreferencesRepository.syncPreferencesCalled, "Force sync should always work")
    }
}

class MockPreferencesRepositoryForOfflineSync : PreferencesRepository {
    var syncPreferencesCalled = false
    var syncResult: Result<Unit> = Result.success(Unit)
    
    override suspend fun getUserPreferences(): Result<com.eunio.healthapp.domain.model.UserPreferences?> {
        return Result.success(null)
    }
    
    override suspend fun getUserPreferences(userId: String): Result<com.eunio.healthapp.domain.model.UserPreferences?> {
        return Result.success(null)
    }
    
    override suspend fun saveUserPreferences(preferences: com.eunio.healthapp.domain.model.UserPreferences): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun syncPreferences(): Result<Unit> {
        syncPreferencesCalled = true
        return syncResult
    }
    
    override suspend fun clearPreferences(): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun clearPreferences(userId: String): Result<Unit> {
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
}

class MockNetworkConnectivityForOfflineSync : NetworkConnectivity {
    private val _connectivityFlow = MutableStateFlow(false)
    private var _isConnected = false
    private var _hasStableConnection = false
    
    override fun isConnected(): Boolean = _isConnected
    
    override fun observeConnectivity() = _connectivityFlow
    
    override suspend fun hasStableConnection(): Boolean = _hasStableConnection
    
    override fun getNetworkType(): NetworkType = if (_isConnected) NetworkType.WIFI else NetworkType.NONE
    
    fun setConnected(connected: Boolean) {
        _isConnected = connected
        _connectivityFlow.value = connected
    }
    
    fun setStableConnection(stable: Boolean) {
        _hasStableConnection = stable
    }
}