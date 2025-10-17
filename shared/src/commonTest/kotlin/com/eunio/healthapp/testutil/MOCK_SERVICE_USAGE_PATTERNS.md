# Mock Service Usage Patterns

## Overview

This document provides comprehensive patterns and best practices for using mock services in the Eunio Health App test infrastructure. Mock services are essential for creating predictable, isolated test environments.

## Available Mock Services

### Core Mock Services

The following mock services are available through the test infrastructure:

```kotlin
// Network and Connectivity
MockNetworkConnectivity          // Network connection simulation
MockSyncService                  // Data synchronization mocking

// Platform Services  
MockHapticFeedbackManager       // Haptic feedback simulation
MockThemeManager                // Theme management mocking
MockNotificationManager         // Notification system mocking

// Data Services
MockUserLocalDataSource         // Local user data mocking
MockUserRemoteDataSource        // Remote user data mocking
MockPreferencesLocalDataSource  // Local preferences mocking
MockPreferencesRemoteDataSource // Remote preferences mocking
MockHealthDataRepository        // Health data repository mocking
MockSettingsRepository          // Settings repository mocking

// Security Services
MockEncryptionService           // Data encryption mocking
MockAuthenticationService       // Authentication mocking
```

## Basic Usage Patterns

### 1. Getting Mock Services from Koin

```kotlin
class MyServiceTest : BaseKoinTest() {
    
    @Test
    fun `test with network connectivity mock`() {
        // Get mock service from Koin test module
        val mockNetwork = get<NetworkConnectivity>()
        
        // Cast to mock implementation for configuration
        val networkMock = mockNetwork as MockNetworkConnectivity
        
        // Configure mock behavior
        networkMock.isStable = true
        networkMock.connectionType = ConnectionType.WIFI
        
        // Use in test
        val service = get<MyService>()
        val result = service.performNetworkOperation()
        
        assertTrue(result.isSuccess)
    }
}
```

### 2. Direct Mock Creation (When Needed)

```kotlin
@Test
fun `test with directly created mock`() {
    // Create mock directly (use sparingly)
    val mockDataSource = MockUserLocalDataSource().apply {
        shouldReturnError = false
        userData = TestDataBuilder.createUser()
    }
    
    // Use in service creation
    val service = UserService(localDataSource = mockDataSource)
    
    // Test service behavior
    val result = service.getUser("test-id")
    assertNotNull(result)
}
```

## Mock Configuration Patterns

### Network Connectivity Mock

```kotlin
class NetworkDependentTest : BaseKoinTest() {
    
    private lateinit var mockNetwork: MockNetworkConnectivity
    
    @BeforeTest
    fun setup() {
        super.setup()
        mockNetwork = get<NetworkConnectivity>() as MockNetworkConnectivity
    }
    
    @Test
    fun `handles stable wifi connection`() {
        mockNetwork.apply {
            isStable = true
            connectionType = ConnectionType.WIFI
            latencyMs = 50
            bandwidthMbps = 100.0
        }
        
        val service = get<SyncService>()
        val result = service.syncData()
        
        assertTrue(result.isSuccess)
        assertEquals(SyncStatus.COMPLETED, result.getOrNull()?.status)
    }
    
    @Test
    fun `handles unstable mobile connection`() {
        mockNetwork.apply {
            isStable = false
            connectionType = ConnectionType.MOBILE
            latencyMs = 200
            bandwidthMbps = 5.0
            intermittentFailures = true
        }
        
        val service = get<SyncService>()
        val result = service.syncData()
        
        // Should handle unstable connection gracefully
        assertTrue(result.isSuccess)
        assertEquals(SyncStatus.PARTIAL, result.getOrNull()?.status)
    }
    
    @Test
    fun `handles no connection`() {
        mockNetwork.apply {
            isConnected = false
            shouldThrowException = true
            exceptionToThrow = NetworkUnavailableException("No connection")
        }
        
        val service = get<SyncService>()
        val result = service.syncData()
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkUnavailableException)
    }
}
```

### Data Source Mock Patterns

```kotlin
class DataRepositoryTest : BaseKoinTest() {
    
    @Test
    fun `repository handles local data source success`() {
        val mockLocal = get<UserLocalDataSource>() as MockUserLocalDataSource
        mockLocal.apply {
            shouldReturnError = false
            userData = TestDataBuilder.createUser(id = "test-user")
            operationDelay = 100 // Simulate realistic delay
        }
        
        val repository = get<UserRepository>()
        val result = repository.getUser("test-user")
        
        assertTrue(result.isSuccess)
        assertEquals("test-user", result.getOrNull()?.id)
    }
    
    @Test
    fun `repository handles local data source failure`() {
        val mockLocal = get<UserLocalDataSource>() as MockUserLocalDataSource
        mockLocal.apply {
            shouldReturnError = true
            errorToReturn = DataAccessException("Database corrupted")
        }
        
        val repository = get<UserRepository>()
        val result = repository.getUser("test-user")
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DataAccessException)
    }
    
    @Test
    fun `repository falls back to remote when local fails`() {
        // Configure local to fail
        val mockLocal = get<UserLocalDataSource>() as MockUserLocalDataSource
        mockLocal.apply {
            shouldReturnError = true
            errorToReturn = DataAccessException("Local storage unavailable")
        }
        
        // Configure remote to succeed
        val mockRemote = get<UserRemoteDataSource>() as MockUserRemoteDataSource
        mockRemote.apply {
            shouldReturnError = false
            userData = TestDataBuilder.createUser(id = "test-user")
        }
        
        val repository = get<UserRepository>()
        val result = repository.getUser("test-user")
        
        assertTrue(result.isSuccess)
        assertEquals("test-user", result.getOrNull()?.id)
        
        // Verify fallback behavior
        assertTrue(mockLocal.getLastError() is DataAccessException)
        assertTrue(mockRemote.wasAccessed)
    }
}
```

### Platform Service Mock Patterns

```kotlin
class PlatformServiceTest : BaseKoinTest() {
    
    @Test
    fun `haptic feedback works correctly`() {
        val mockHaptic = get<HapticFeedbackManager>() as MockHapticFeedbackManager
        mockHaptic.apply {
            isEnabled = true
            supportedTypes = setOf(
                HapticType.LIGHT_IMPACT,
                HapticType.MEDIUM_IMPACT,
                HapticType.HEAVY_IMPACT
            )
        }
        
        val service = get<UserInteractionService>()
        service.performActionWithFeedback(UserAction.BUTTON_PRESS)
        
        // Verify haptic feedback was triggered
        assertEquals(1, mockHaptic.feedbackCount)
        assertEquals(HapticType.LIGHT_IMPACT, mockHaptic.lastFeedbackType)
    }
    
    @Test
    fun `theme manager handles theme changes`() {
        val mockTheme = get<ThemeManager>() as MockThemeManager
        mockTheme.apply {
            currentTheme = ThemeType.LIGHT
            availableThemes = setOf(ThemeType.LIGHT, ThemeType.DARK, ThemeType.AUTO)
        }
        
        val service = get<DisplayPreferencesService>()
        val result = service.updateTheme(ThemeType.DARK)
        
        assertTrue(result.isSuccess)
        assertEquals(ThemeType.DARK, mockTheme.currentTheme)
        assertTrue(mockTheme.themeChangeEvents.contains(ThemeType.DARK))
    }
}
```

## Advanced Mock Patterns

### Stateful Mock Behavior

```kotlin
class StatefulMockTest : BaseKoinTest() {
    
    @Test
    fun `mock maintains state across operations`() {
        val mockRepository = get<SettingsRepository>() as MockSettingsRepository
        
        // Initial state
        mockRepository.apply {
            settings = TestDataBuilder.createUserSettings()
            syncStatus = SyncStatus.SYNCED
        }
        
        val service = get<SettingsService>()
        
        // First operation - update settings
        val updateResult = service.updateDisplayPreferences(
            TestDataBuilder.createDisplayPreferences(theme = ThemeType.DARK)
        )
        assertTrue(updateResult.isSuccess)
        
        // Verify state changed
        assertEquals(SyncStatus.PENDING, mockRepository.syncStatus)
        assertEquals(ThemeType.DARK, mockRepository.settings?.displayPreferences?.theme)
        
        // Second operation - sync
        val syncResult = service.syncSettings()
        assertTrue(syncResult.isSuccess)
        
        // Verify final state
        assertEquals(SyncStatus.SYNCED, mockRepository.syncStatus)
    }
}
```

### Mock Behavior Chains

```kotlin
class MockBehaviorChainTest : BaseKoinTest() {
    
    @Test
    fun `mock simulates complex interaction sequence`() {
        val mockNetwork = get<NetworkConnectivity>() as MockNetworkConnectivity
        
        // Configure behavior chain: connected -> disconnected -> reconnected
        mockNetwork.behaviorChain = listOf(
            NetworkBehavior(isConnected = true, duration = 1000),
            NetworkBehavior(isConnected = false, duration = 2000),
            NetworkBehavior(isConnected = true, duration = 1000)
        )
        
        val service = get<SyncService>()
        
        // Start sync operation
        val syncJob = service.startContinuousSync()
        
        // Advance through behavior chain
        advanceTimeBy(1500) // Network disconnects
        assertEquals(SyncStatus.PAUSED, service.currentStatus)
        
        advanceTimeBy(2500) // Network reconnects
        assertEquals(SyncStatus.RESUMING, service.currentStatus)
        
        advanceTimeBy(1000) // Sync completes
        assertEquals(SyncStatus.COMPLETED, service.currentStatus)
        
        syncJob.cancel()
    }
}
```

### Conditional Mock Responses

```kotlin
class ConditionalMockTest : BaseKoinTest() {
    
    @Test
    fun `mock responds differently based on input`() {
        val mockDataSource = get<UserRemoteDataSource>() as MockUserRemoteDataSource
        
        // Configure conditional responses
        mockDataSource.conditionalResponses = mapOf(
            "valid-user" to MockResponse.success(TestDataBuilder.createUser(id = "valid-user")),
            "invalid-user" to MockResponse.error(UserNotFoundException("User not found")),
            "admin-user" to MockResponse.success(TestDataBuilder.createAdminUser())
        )
        
        val repository = get<UserRepository>()
        
        // Test different scenarios
        val validResult = repository.getUser("valid-user")
        assertTrue(validResult.isSuccess)
        
        val invalidResult = repository.getUser("invalid-user")
        assertTrue(invalidResult.isFailure)
        assertTrue(invalidResult.exceptionOrNull() is UserNotFoundException)
        
        val adminResult = repository.getUser("admin-user")
        assertTrue(adminResult.isSuccess)
        assertTrue(adminResult.getOrNull()?.isAdmin == true)
    }
}
```

## Mock Verification Patterns

### Interaction Verification

```kotlin
class MockVerificationTest : BaseKoinTest() {
    
    @Test
    fun `verifies mock interactions occurred`() {
        val mockDataSource = get<UserLocalDataSource>() as MockUserLocalDataSource
        val mockNetwork = get<NetworkConnectivity>() as MockNetworkConnectivity
        
        val service = get<UserService>()
        
        // Perform operations
        service.getUser("user1")
        service.getUser("user2")
        service.updateUser(TestDataBuilder.createUser(id = "user1"))
        
        // Verify interactions
        assertEquals(2, mockDataSource.getCallCount)
        assertEquals(1, mockDataSource.updateCallCount)
        assertEquals(listOf("user1", "user2"), mockDataSource.accessedUserIds)
        
        assertTrue(mockNetwork.connectionCheckCount > 0)
    }
    
    @Test
    fun `verifies interaction order`() {
        val mockRepository = get<SettingsRepository>() as MockSettingsRepository
        
        val service = get<SettingsService>()
        
        // Perform operations in specific order
        service.loadSettings()
        service.updateSettings(TestDataBuilder.createUserSettings())
        service.saveSettings()
        
        // Verify order of operations
        val expectedOrder = listOf("load", "update", "save")
        assertEquals(expectedOrder, mockRepository.operationOrder)
    }
}
```

### State Verification

```kotlin
@Test
fun `verifies mock state changes`() {
    val mockSync = get<SyncService>() as MockSyncService
    
    // Initial state
    assertEquals(SyncStatus.IDLE, mockSync.status)
    assertEquals(0, mockSync.syncedItemCount)
    
    val service = get<DataService>()
    service.performDataSync()
    
    // Verify state changes
    assertEquals(SyncStatus.COMPLETED, mockSync.status)
    assertTrue(mockSync.syncedItemCount > 0)
    assertNotNull(mockSync.lastSyncTimestamp)
}
```

## Mock Performance Patterns

### Simulating Realistic Performance

```kotlin
class MockPerformanceTest : BaseKoinTest() {
    
    @Test
    fun `mock simulates realistic operation timing`() {
        val mockDataSource = get<UserRemoteDataSource>() as MockUserRemoteDataSource
        mockDataSource.apply {
            // Simulate realistic network delays
            operationDelay = 500 // 500ms for network operations
            variableDelay = true // Add some randomness
            delayVariance = 200 // ±200ms variance
        }
        
        val startTime = TimeSource.Monotonic.markNow()
        
        val repository = get<UserRepository>()
        val result = repository.fetchUserFromRemote("test-user")
        
        val duration = startTime.elapsedNow()
        
        assertTrue(result.isSuccess)
        assertTrue(duration >= 300.milliseconds) // At least 300ms
        assertTrue(duration <= 800.milliseconds) // At most 800ms
    }
    
    @Test
    fun `mock simulates performance degradation`() {
        val mockNetwork = get<NetworkConnectivity>() as MockNetworkConnectivity
        mockNetwork.apply {
            // Simulate degrading performance
            performanceDegradation = PerformanceDegradation(
                initialLatency = 50,
                degradationRate = 10, // +10ms per operation
                maxLatency = 500
            )
        }
        
        val service = get<SyncService>()
        
        // Perform multiple operations
        val durations = mutableListOf<Duration>()
        repeat(10) {
            val start = TimeSource.Monotonic.markNow()
            service.syncSingleItem(TestDataBuilder.createHealthData())
            durations.add(start.elapsedNow())
        }
        
        // Verify performance degradation
        assertTrue(durations.first() < durations.last())
        assertTrue(durations.last() <= 500.milliseconds)
    }
}
```

## Mock Error Simulation

### Transient Error Patterns

```kotlin
class MockErrorSimulationTest : BaseKoinTest() {
    
    @Test
    fun `mock simulates transient network errors`() {
        val mockNetwork = get<NetworkConnectivity>() as MockNetworkConnectivity
        mockNetwork.apply {
            // Fail first 2 attempts, then succeed
            transientFailures = TransientFailurePattern(
                failureCount = 2,
                failureType = NetworkException("Temporary network error"),
                recoveryDelay = 1000
            )
        }
        
        val service = get<SyncService>()
        
        // First attempt should fail
        val result1 = service.syncData()
        assertTrue(result1.isFailure)
        
        // Second attempt should fail
        val result2 = service.syncData()
        assertTrue(result1.isFailure)
        
        // Third attempt should succeed
        advanceTimeBy(1100) // Wait for recovery
        val result3 = service.syncData()
        assertTrue(result3.isSuccess)
    }
    
    @Test
    fun `mock simulates intermittent failures`() {
        val mockDataSource = get<UserLocalDataSource>() as MockUserLocalDataSource
        mockDataSource.apply {
            // Fail every 3rd operation
            intermittentFailures = IntermittentFailurePattern(
                failureInterval = 3,
                failureType = DataAccessException("Intermittent database error")
            )
        }
        
        val repository = get<UserRepository>()
        
        // Operations 1 and 2 should succeed
        assertTrue(repository.getUser("user1").isSuccess)
        assertTrue(repository.getUser("user2").isSuccess)
        
        // Operation 3 should fail
        assertTrue(repository.getUser("user3").isFailure)
        
        // Operation 4 should succeed again
        assertTrue(repository.getUser("user4").isSuccess)
    }
}
```

## Best Practices

### 1. Mock Configuration

```kotlin
// ✅ Good: Clear, explicit configuration
val mockNetwork = get<NetworkConnectivity>() as MockNetworkConnectivity
mockNetwork.apply {
    isStable = true
    connectionType = ConnectionType.WIFI
    latencyMs = 50
}

// ❌ Bad: Unclear configuration
val mockNetwork = get<NetworkConnectivity>() as MockNetworkConnectivity
mockNetwork.isStable = true // What about other properties?
```

### 2. Mock Verification

```kotlin
// ✅ Good: Specific verification
assertEquals(3, mockDataSource.getCallCount)
assertEquals(listOf("user1", "user2", "user3"), mockDataSource.accessedUserIds)

// ❌ Bad: Vague verification
assertTrue(mockDataSource.getCallCount > 0) // How many exactly?
```

### 3. Mock State Management

```kotlin
// ✅ Good: Clean state management
@BeforeTest
fun setup() {
    super.setup()
    // Mocks are fresh from Koin test module
}

@AfterTest
fun teardown() {
    super.teardown()
    // Koin cleanup handles mock cleanup
}

// ❌ Bad: Manual mock management
val mockService = MockService() // Shared across tests - bad!
```

### 4. Mock Behavior Definition

```kotlin
// ✅ Good: Behavior defined in test
@Test
fun `handles network failure`() {
    val mockNetwork = get<NetworkConnectivity>() as MockNetworkConnectivity
    mockNetwork.shouldThrowException = true
    mockNetwork.exceptionToThrow = NetworkException("Connection failed")
    
    // Test behavior
}

// ❌ Bad: Behavior defined globally
class MyTest : BaseKoinTest() {
    private val mockNetwork = MockNetworkConnectivity().apply {
        shouldThrowException = true // Affects all tests!
    }
}
```

## Common Pitfalls to Avoid

1. **Shared Mock State**: Don't reuse mock instances across tests
2. **Over-Mocking**: Don't mock everything - test real interactions when possible
3. **Under-Verification**: Always verify that mocks were called as expected
4. **Unrealistic Behavior**: Make mocks behave like real services would
5. **Configuration Leakage**: Ensure mock configuration doesn't affect other tests
6. **Missing Edge Cases**: Test both success and failure scenarios
7. **Performance Ignorance**: Consider realistic timing in mock responses

## Mock Service Reference

For detailed information about specific mock services and their capabilities, see:

- `MockServices.kt` - Complete list of available mock services
- `MockServiceFactory.kt` - Factory methods for creating configured mocks
- `TestModule.kt` - Koin module configuration for test mocks
- Individual mock service files for specific configuration options