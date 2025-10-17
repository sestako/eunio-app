# TestTimingManager Usage Guide

The `TestTimingManager` is designed to fix timing-dependent test failures by providing deterministic timing control, configurable delays, and proper clock synchronization for multi-device tests.

## Key Features

### 1. Deterministic Timing
- Virtual time control that advances predictably
- No dependency on system performance or timing variations
- Consistent test execution across different environments

### 2. Configurable Delay Scenarios
- Pre-configured scenarios for different operations (network, database, auth, sync)
- Customizable delay patterns with variability and failure rates
- Retry mechanisms with exponential backoff

### 3. Multi-Device Clock Synchronization
- Simulate clock skew between different devices
- Synchronize device clocks for consistent multi-device tests
- Proper timestamp assertions with tolerance

### 4. Async Operation Handling
- Proper synchronization for async operations
- Condition waiting with timeout
- Race condition prevention

## Basic Usage

### Extending BaseIntegrationTest

```kotlin
class MyTimingTest : BaseIntegrationTest() {
    
    @Test
    fun `my test with proper timing`() = runIntegrationTest {
        // Test implementation using timing manager
    }
}
```

### Using Timing Scenarios

```kotlin
// Execute with network delay
executeWithTiming("network_fast") {
    // Your operation here
}

// Execute with retry on failure
executeWithRetryTiming("network_unreliable") {
    // Operation that might fail and need retry
}
```

### Waiting for Async Operations

```kotlin
waitForAsyncOperation(
    operationName = "save_data",
    operation = {
        // Async operation
        mockServices.repository.saveData(data)
    },
    verification = {
        // Check if operation completed
        mockServices.repository.getData() != null
    }
)
```

### Timestamp Management

```kotlin
// Create deterministic timestamp gaps
createTimestampSeparation(100.milliseconds)

// Assert timestamps with tolerance
assertTimestampWithinRange(
    actual = actualTimestamp,
    expected = expectedTimestamp,
    tolerance = 1.seconds
)
```

### Multi-Device Testing

```kotlin
// Register devices with clock skew
registerTestDevice("device1", 0.milliseconds)
registerTestDevice("device2", 2.seconds)

// Get device-specific times
val device1Time = getDeviceTime("device1")
val device2Time = getDeviceTime("device2")

// Synchronize clocks
synchronizeDeviceClocks("device1") // Use device1 as master
```

## Available Timing Scenarios

### Network Operations
- `network_fast`: 50ms base delay, 20ms variability
- `network_slow`: 2s base delay, 500ms variability, 10% failure rate
- `network_unreliable`: 500ms base delay, 1s variability, 30% failure rate

### Database Operations
- `database_fast`: 10ms base delay, 5ms variability
- `database_slow`: 500ms base delay, 200ms variability, 5% failure rate

### Authentication Operations
- `auth_normal`: 150ms base delay, 50ms variability
- `auth_slow`: 3s base delay, 1s variability, 10% failure rate

### Sync Operations
- `sync_normal`: 200ms base delay, 100ms variability
- `sync_conflict`: 1s base delay, 500ms variability, 20% failure rate

## Extension Functions

```kotlin
// Convenient methods for common operations
timingManager.networkOperation(slow = false) { /* operation */ }
timingManager.databaseOperation(slow = true) { /* operation */ }
timingManager.authenticationOperation(slow = false) { /* operation */ }
timingManager.syncOperation(withConflicts = true) { /* operation */ }
```

## Best Practices

### 1. Use Deterministic Timing
Instead of:
```kotlin
delay(100) // Arbitrary delay
```

Use:
```kotlin
createTimestampSeparation(100.milliseconds) // Controlled timing
```

### 2. Wait for Conditions, Don't Assume Timing
Instead of:
```kotlin
operation()
delay(1000) // Hope it's done
assert(condition)
```

Use:
```kotlin
waitForAsyncOperation(
    operationName = "my_operation",
    operation = { operation() },
    verification = { condition }
)
```

### 3. Use Appropriate Scenarios
Instead of:
```kotlin
// Generic delay
delay(500)
```

Use:
```kotlin
// Scenario-specific timing
executeWithTiming("network_fast") { /* network operation */ }
executeWithTiming("database_slow") { /* database operation */ }
```

### 4. Handle Failures Gracefully
Instead of:
```kotlin
// Might fail randomly
operation()
```

Use:
```kotlin
// Retry with backoff
executeWithRetryTiming("network_unreliable") {
    operation()
}
```

### 5. Account for Clock Skew in Multi-Device Tests
Instead of:
```kotlin
// Exact timestamp comparison
assertEquals(timestamp1, timestamp2)
```

Use:
```kotlin
// Tolerance-based comparison
assertTimestampWithinRange(
    actual = timestamp1,
    expected = timestamp2,
    tolerance = 1.seconds
)
```

## Common Patterns

### Sequential Operations with Timing
```kotlin
@Test
fun `sequential operations work correctly`() = runIntegrationTest {
    // Step 1: Authentication
    executeWithTiming("auth_normal") {
        mockServices.userRepository.signIn(email, password)
    }
    
    // Step 2: Data loading (ensure timestamp separation)
    createTimestampSeparation(50.milliseconds)
    executeWithTiming("database_fast") {
        mockServices.dataRepository.loadUserData(userId)
    }
    
    // Step 3: Sync with retry
    executeWithRetryTiming("sync_normal") {
        mockServices.syncService.syncData()
    }
}
```

### Multi-Device Sync Testing
```kotlin
@Test
fun `multi-device sync handles clock skew`() = runIntegrationTest {
    // Setup devices with different clock skews
    registerTestDevice("mobile", 0.milliseconds)
    registerTestDevice("web", 1.seconds)
    
    // Create data on mobile
    val mobileTime = getDeviceTime("mobile")
    val mobileData = createDataWithTimestamp(mobileTime)
    
    // Create conflicting data on web
    createTimestampSeparation(100.milliseconds)
    val webTime = getDeviceTime("web")
    val webData = createDataWithTimestamp(webTime)
    
    // Sync and verify conflict resolution
    executeWithTiming("sync_conflict") {
        syncService.resolveConflicts(mobileData, webData)
    }
    
    // Verify timestamps are handled correctly
    assertTimestampWithinRange(
        actual = resolvedData.timestamp,
        expected = maxOf(mobileTime, webTime),
        tolerance = 500.milliseconds
    )
}
```

This timing manager addresses the requirements:
- **7.3**: Tests are not dependent on system performance or timing variations
- **7.6**: Proper synchronization is implemented for async operations