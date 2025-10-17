# Test Writing Guidelines

## Overview

This document provides comprehensive guidelines for writing tests in the Eunio Health App project. Following these guidelines ensures consistent, maintainable, and reliable tests across the codebase.

## Test Structure and Organization

### Test File Naming Convention

```kotlin
// For testing a class named UserRepository
class UserRepositoryTest

// For testing a use case
class UpdateDisplayPreferencesUseCaseTest

// For integration tests
class SettingsIntegrationTest

// For platform-specific tests
class AndroidUserDefaultsTest
class IOSUserDefaultsTest
```

### Test Method Naming

Use descriptive names that clearly indicate what is being tested:

```kotlin
@Test
fun `getUserPreferences returns cached preferences when available`()

@Test
fun `saveUserPreferences updates cache and persists to storage`()

@Test
fun `getUserPreferences throws exception when storage is unavailable`()
```

### Test Class Structure

```kotlin
class UserRepositoryTest : BaseKoinTest() {
    
    // Test subject
    private lateinit var userRepository: UserRepository
    
    // Dependencies (mocks)
    private lateinit var mockLocalDataSource: MockUserLocalDataSource
    private lateinit var mockRemoteDataSource: MockUserRemoteDataSource
    
    @BeforeTest
    fun setup() {
        super.setup() // Initialize Koin
        
        // Initialize mocks
        mockLocalDataSource = MockUserLocalDataSource()
        mockRemoteDataSource = MockUserRemoteDataSource()
        
        // Create test subject
        userRepository = UserRepositoryImpl(
            localDataSource = mockLocalDataSource,
            remoteDataSource = mockRemoteDataSource
        )
    }
    
    @AfterTest
    fun teardown() {
        super.teardown() // Clean up Koin
        // Additional cleanup if needed
    }
    
    @Test
    fun `test method here`() {
        // Test implementation
    }
}
```

## Test Data Creation

### Use TestDataBuilder for Consistent Data

Always use the centralized TestDataBuilder for creating test data:

```kotlin
@Test
fun `saveUserPreferences stores preferences correctly`() {
    // GOOD: Use TestDataBuilder
    val testPreferences = TestDataBuilder.createUserPreferences(
        userId = "test-user-123",
        displaySettings = TestDataBuilder.createDisplaySettings(
            theme = ThemeType.DARK,
            language = LanguageCode.EN
        )
    )
    
    // BAD: Manual construction
    val badPreferences = UserPreferences(
        userId = "test-user",
        displaySettings = DisplaySettings(...), // Might use wrong parameters
        // Missing required fields
    )
}
```

### Test Data Isolation

Each test should use unique test data to avoid interference:

```kotlin
@Test
fun `test with unique user`() {
    val userId = TestDataBuilder.generateUniqueUserId()
    val preferences = TestDataBuilder.createUserPreferences(userId = userId)
    
    // Test implementation
}
```

## Mock Service Usage

### Use Centralized Mock Services

Always use the provided mock services from MockServices:

```kotlin
class MyServiceTest : BaseKoinTest() {
    
    @Test
    fun `test network dependent operation`() {
        // Get mock from Koin test module
        val mockNetworkConnectivity = get<NetworkConnectivity>()
        
        // Configure mock behavior
        (mockNetworkConnectivity as MockNetworkConnectivity).apply {
            isStable = true
            connectionType = ConnectionType.WIFI
        }
        
        // Test implementation
    }
}
```

### Mock Configuration Patterns

```kotlin
// Configure mock to simulate different scenarios
@Test
fun `handles network failure gracefully`() {
    val mockNetwork = get<NetworkConnectivity>() as MockNetworkConnectivity
    mockNetwork.apply {
        isStable = false
        shouldThrowException = true
        exceptionToThrow = NetworkException("Connection failed")
    }
    
    // Test network failure handling
}

@Test
fun `works with stable connection`() {
    val mockNetwork = get<NetworkConnectivity>() as MockNetworkConnectivity
    mockNetwork.apply {
        isStable = true
        connectionType = ConnectionType.WIFI
        latencyMs = 50
    }
    
    // Test normal operation
}
```

## Dependency Injection in Tests

### Using BaseKoinTest

Extend BaseKoinTest for automatic Koin setup:

```kotlin
class MyViewModelTest : BaseKoinTest() {
    
    private lateinit var viewModel: MyViewModel
    
    @BeforeTest
    fun setup() {
        super.setup() // This initializes Koin with test modules
        
        // Get dependencies from Koin
        viewModel = get<MyViewModel>()
    }
    
    @Test
    fun `test viewmodel behavior`() {
        // ViewModel already has mock dependencies injected
        viewModel.performAction()
        
        // Verify behavior
        assertTrue(viewModel.isActionCompleted)
    }
}
```

### Custom Test Modules

For specific test scenarios, create custom modules:

```kotlin
class SpecializedTest : BaseKoinTest() {
    
    override fun getTestModules(): List<Module> {
        return super.getTestModules() + module {
            // Override specific dependencies for this test
            single<SpecialService> { MockSpecialService() }
        }
    }
}
```

## Async Testing Patterns

### Testing Coroutines

```kotlin
@Test
fun `async operation completes successfully`() = runTest {
    val result = async {
        repository.performAsyncOperation()
    }
    
    // Advance time if needed
    advanceTimeBy(1000)
    
    val actualResult = result.await()
    assertEquals(expectedResult, actualResult)
}
```

### Testing StateFlow/Flow

```kotlin
@Test
fun `stateflow emits expected values`() = runTest {
    val emissions = mutableListOf<State>()
    
    val job = launch {
        viewModel.state.collect { state ->
            emissions.add(state)
        }
    }
    
    // Trigger state changes
    viewModel.performAction()
    
    // Verify emissions
    assertEquals(listOf(InitialState, LoadingState, SuccessState), emissions)
    
    job.cancel()
}
```

## Error Testing

### Testing Exception Handling

```kotlin
@Test
fun `handles repository exception gracefully`() {
    // Configure mock to throw exception
    val mockRepository = get<UserRepository>() as MockUserRepository
    mockRepository.shouldThrowException = true
    mockRepository.exceptionToThrow = DataAccessException("Database error")
    
    // Test error handling
    val result = runCatching {
        useCase.execute(request)
    }
    
    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is UseCaseException)
}
```

### Testing Result Types

```kotlin
@Test
fun `returns success result when operation succeeds`() {
    val result = repository.saveData(testData)
    
    assertTrue(result.isSuccess)
    assertEquals(Unit, result.getOrNull())
}

@Test
fun `returns failure result when operation fails`() {
    // Configure failure scenario
    val mockDataSource = get<DataSource>() as MockDataSource
    mockDataSource.shouldFail = true
    
    val result = repository.saveData(testData)
    
    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is DataPersistenceException)
}
```

## Platform-Specific Testing

### Android Tests

```kotlin
class AndroidSpecificTest : BaseKoinTest() {
    
    @BeforeTest
    fun setup() {
        super.setup()
        
        // Use Android test utilities
        val mockContext = AndroidTestContext.createMockContext()
        // Configure Android-specific mocks
    }
    
    @Test
    fun `android specific functionality works`() {
        // Test Android-specific behavior
    }
}
```

### iOS Tests

```kotlin
class IOSSpecificTest : BaseKoinTest() {
    
    @BeforeTest
    fun setup() {
        super.setup()
        
        // Use iOS test utilities
        IOSTestSupport.setupIOSTestEnvironment()
    }
    
    @Test
    fun `ios specific functionality works`() {
        // Test iOS-specific behavior
    }
}
```

## Performance Testing Guidelines

### Resource Management

```kotlin
class PerformanceTest : PerformanceOptimizedBaseTest() {
    
    @Test
    fun `operation completes within time limit`() {
        val startTime = TimeSource.Monotonic.markNow()
        
        // Perform operation
        repository.performExpensiveOperation()
        
        val duration = startTime.elapsedNow()
        assertTrue(duration < 1.seconds, "Operation took too long: $duration")
    }
    
    @Test
    fun `memory usage stays within bounds`() {
        val initialMemory = getMemoryUsage()
        
        // Perform memory-intensive operation
        repeat(1000) {
            repository.createLargeObject()
        }
        
        // Force cleanup
        ResourceCleanupManager.forceCleanup()
        
        val finalMemory = getMemoryUsage()
        val memoryIncrease = finalMemory - initialMemory
        
        assertTrue(memoryIncrease < 10.MB, "Memory leak detected: ${memoryIncrease}")
    }
}
```

## Test Validation and Quality

### Use Test Validation Pipeline

```kotlin
class MyFeatureTest : BaseKoinTest() {
    
    @Test
    fun `comprehensive feature test`() {
        // Use validation pipeline for complex tests
        TestValidationPipeline.Builder()
            .addValidation("setup") { setupIsCorrect() }
            .addValidation("execution") { operationSucceeds() }
            .addValidation("cleanup") { resourcesAreCleanedUp() }
            .build()
            .execute()
    }
}
```

### Deterministic Testing

```kotlin
@Test
fun `test produces consistent results`() {
    // Use deterministic test execution
    DeterministicTestExecution.withFixedSeed(12345) {
        val result1 = performRandomizedOperation()
        val result2 = performRandomizedOperation()
        
        assertEquals(result1, result2, "Results should be deterministic")
    }
}
```

## Common Anti-Patterns to Avoid

### ❌ Don't Do This

```kotlin
// DON'T: Hard-coded test data
val user = User("john@example.com", "password123")

// DON'T: Direct mock creation without Koin
val mockService = MockService()

// DON'T: Tests that depend on external state
@Test
fun `test that depends on current time`() {
    val now = Clock.System.now()
    // This will fail at different times
}

// DON'T: Tests without proper cleanup
@Test
fun `test without cleanup`() {
    createGlobalState()
    // No cleanup - affects other tests
}
```

### ✅ Do This Instead

```kotlin
// DO: Use TestDataBuilder
val user = TestDataBuilder.createUser(email = "test@example.com")

// DO: Use Koin for dependency injection
val mockService = get<Service>()

// DO: Use deterministic time
@Test
fun `test with fixed time`() {
    DeterministicTestExecution.withFixedTime(Instant.parse("2024-01-01T00:00:00Z")) {
        // Test with predictable time
    }
}

// DO: Proper cleanup
@Test
fun `test with cleanup`() {
    try {
        createTestState()
        // Test logic
    } finally {
        cleanupTestState()
    }
}
```

## Best Practices Summary

1. **Always extend BaseKoinTest** for dependency injection support
2. **Use TestDataBuilder** for all test data creation
3. **Configure mocks through Koin** rather than direct instantiation
4. **Write descriptive test names** that explain the scenario
5. **Test both success and failure paths**
6. **Use deterministic execution** for consistent results
7. **Clean up resources** in @AfterTest methods
8. **Isolate test data** to prevent test interference
9. **Use appropriate test utilities** for platform-specific testing
10. **Validate test performance** and resource usage

## Code Review Checklist

When reviewing test code, ensure:

- [ ] Test extends appropriate base class (BaseKoinTest, etc.)
- [ ] Test data created using TestDataBuilder
- [ ] Mocks configured through dependency injection
- [ ] Test name clearly describes the scenario
- [ ] Both positive and negative cases tested
- [ ] Proper setup and cleanup implemented
- [ ] No hard-coded values or external dependencies
- [ ] Async operations properly tested with runTest
- [ ] Platform-specific tests use appropriate utilities
- [ ] Performance considerations addressed for expensive operations