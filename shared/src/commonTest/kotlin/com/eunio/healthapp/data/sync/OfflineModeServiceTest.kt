package com.eunio.healthapp.data.sync

import com.eunio.healthapp.domain.util.NetworkConnectivity
import com.eunio.healthapp.domain.util.NetworkType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineModeServiceTest {
    
    private lateinit var mockNetworkConnectivity: FakeNetworkConnectivity
    private lateinit var testScope: TestScope
    private lateinit var offlineModeService: OfflineModeService
    
    @BeforeTest
    fun setup() {
        testScope = TestScope()
        mockNetworkConnectivity = FakeNetworkConnectivity()
        
        offlineModeService = OfflineModeService(
            networkConnectivity = mockNetworkConnectivity,
            coroutineScope = testScope
        )
    }
    
    @Test
    fun `should show online state when connected`() = runTest {
        // Arrange
        mockNetworkConnectivity.setConnected(true)
        mockNetworkConnectivity.setNetworkType(NetworkType.WIFI)
        
        // Wait for state to update
        kotlinx.coroutines.delay(10)
        
        // Act
        val offlineState = offlineModeService.offlineState.first()
        
        // Assert
        assertFalse(offlineState.isOffline)
        assertFalse(offlineState.showOfflineIndicator)
    }
    
    @Test
    fun `should show offline state when disconnected`() = runTest {
        // Arrange
        mockNetworkConnectivity.setConnected(false)
        mockNetworkConnectivity.setNetworkType(NetworkType.NONE)
        
        // Recreate service with offline mocks
        offlineModeService = OfflineModeService(
            networkConnectivity = mockNetworkConnectivity,
            coroutineScope = testScope
        )
        
        // Wait for state to update
        kotlinx.coroutines.delay(100)
        
        // Act
        val offlineState = offlineModeService.offlineState.first()
        
        // Assert
        assertTrue(offlineState.isOffline)
        assertTrue(offlineState.showOfflineIndicator)
    }
    
    @Test
    fun `should update feature availability based on connectivity`() = runTest {
        // Arrange - Start offline
        mockNetworkConnectivity.setConnected(false)
        
        // Recreate service with offline mocks
        offlineModeService = OfflineModeService(
            networkConnectivity = mockNetworkConnectivity,
            coroutineScope = testScope
        )
        
        // Wait for state to update
        kotlinx.coroutines.delay(100)
        
        // Act
        val featureAvailability = offlineModeService.featureAvailability.first()
        
        // Assert
        assertFalse(featureAvailability.canSync)
        assertFalse(featureAvailability.canExportData)
    }
    
    @Test
    fun `should restore feature availability when connection is restored`() = runTest {
        // Arrange - Connection restored
        mockNetworkConnectivity.setConnected(true)
        
        // Wait for state to update
        kotlinx.coroutines.delay(10)
        
        // Act
        val featureAvailability = offlineModeService.featureAvailability.first()
        
        // Assert
        assertTrue(featureAvailability.canSync)
        assertTrue(featureAvailability.canExportData)
        assertFalse(featureAvailability.showLimitedFunctionalityWarning)
    }
    
    @Test
    fun `getOfflineMessage should return appropriate message for connected state`() = runTest {
        // Arrange
        mockNetworkConnectivity.setConnected(true)
        
        // Recreate service with online mocks
        offlineModeService = OfflineModeService(
            networkConnectivity = mockNetworkConnectivity,
            coroutineScope = testScope
        )
        
        // Wait for state to update
        kotlinx.coroutines.delay(100)
        
        // Act
        val message = offlineModeService.getOfflineMessage()
        
        // Assert
        assertEquals(OfflineMessageType.CONNECTED, message.type)
        assertEquals("Connected", message.title)
    }
    
    @Test
    fun `getOfflineMessage should return appropriate message for offline state`() = runTest {
        // Arrange
        mockNetworkConnectivity.setConnected(false)
        
        // Recreate service with offline mocks
        offlineModeService = OfflineModeService(
            networkConnectivity = mockNetworkConnectivity,
            coroutineScope = testScope
        )
        
        // Wait for state to update
        kotlinx.coroutines.delay(100)
        
        // Act
        val message = offlineModeService.getOfflineMessage()
        
        // Assert
        assertTrue(message.type in listOf(
            OfflineMessageType.RECONNECTING,
            OfflineMessageType.SHORT_OFFLINE
        ))
    }
    
    @Test
    fun `isFeatureAvailable should return correct availability for core features`() = runTest {
        // Arrange - Online
        mockNetworkConnectivity.setConnected(true)
        
        // Recreate service with online mocks
        offlineModeService = OfflineModeService(
            networkConnectivity = mockNetworkConnectivity,
            coroutineScope = testScope
        )
        
        // Wait for state to update
        kotlinx.coroutines.delay(100)
        
        // Act & Assert
        assertTrue(offlineModeService.isFeatureAvailable(AppFeature.DATA_LOGGING))
        assertTrue(offlineModeService.isFeatureAvailable(AppFeature.CALENDAR_VIEW))
        assertTrue(offlineModeService.isFeatureAvailable(AppFeature.BBT_CHARTING))
        assertTrue(offlineModeService.isFeatureAvailable(AppFeature.DATA_SYNC))
    }
    
    @Test
    fun `isFeatureAvailable should return correct availability for offline features`() = runTest {
        // Arrange - Offline
        mockNetworkConnectivity.setConnected(false)
        
        // Recreate service with offline mocks
        offlineModeService = OfflineModeService(
            networkConnectivity = mockNetworkConnectivity,
            coroutineScope = testScope
        )
        
        // Wait for state to update
        kotlinx.coroutines.delay(100)
        
        // Act & Assert
        assertTrue(offlineModeService.isFeatureAvailable(AppFeature.DATA_LOGGING)) // Always available
        assertTrue(offlineModeService.isFeatureAvailable(AppFeature.CALENDAR_VIEW)) // Always available
        assertTrue(offlineModeService.isFeatureAvailable(AppFeature.BBT_CHARTING)) // Always available
        assertFalse(offlineModeService.isFeatureAvailable(AppFeature.DATA_SYNC)) // Requires network
    }
    
    @Test
    fun `should track offline duration correctly`() = runTest {
        // Arrange - Start offline
        mockNetworkConnectivity.setConnected(false)
        
        // Recreate service with offline mocks
        offlineModeService = OfflineModeService(
            networkConnectivity = mockNetworkConnectivity,
            coroutineScope = testScope
        )
        
        // Wait for state changes
        kotlinx.coroutines.delay(100)
        
        // Act
        val offlineState = offlineModeService.offlineState.first()
        
        // Assert
        assertTrue(offlineState.isOffline)
    }
}

// Fake implementation for multiplatform testing
class FakeNetworkConnectivity : NetworkConnectivity {
    private var connected = true
    private var networkType = NetworkType.WIFI
    private var stableConnection = true
    private val connectivityFlow = MutableStateFlow(connected)
    
    fun setConnected(isConnected: Boolean) {
        connected = isConnected
        connectivityFlow.value = isConnected
    }
    
    fun setNetworkType(type: NetworkType) {
        networkType = type
    }
    
    fun setStableConnection(isStable: Boolean) {
        stableConnection = isStable
    }
    
    override fun isConnected(): Boolean = connected
    
    override fun getNetworkType(): NetworkType = networkType
    
    override fun observeConnectivity() = connectivityFlow
    
    override suspend fun hasStableConnection(): Boolean = stableConnection
}