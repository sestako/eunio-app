# Firestore Service Tests

This directory contains multiplatform tests for the FirestoreService implementation.

## Test Structure

- `FirestoreServiceTest.kt` - Comprehensive multiplatform tests that run on all platforms
- `MockFirestoreService.kt` - Mock implementation for testing (embedded in the test file)

## Running Tests

### All Platforms
```bash
./gradlew shared:allTests
```

### Android Only
```bash
./gradlew shared:testDebugUnitTest
```

### iOS Simulator (ARM64)
```bash
./gradlew shared:iosSimulatorArm64Test
```

### iOS Simulator (x64)
```bash
./gradlew shared:iosX64Test
```

## Test Coverage

The tests cover:

### CRUD Operations
- ✅ User management (create, read, update, delete)
- ✅ Cycle tracking (current cycle, history)
- ✅ Daily log operations (date-based queries, range queries)
- ✅ Insight management (unread filtering, read status)

### Batch Operations
- ✅ Batch saving of multiple entities
- ✅ Atomic operations for sync scenarios

### Sync Operations
- ✅ Timestamp management for sync
- ✅ Changed document retrieval
- ✅ Conflict resolution scenarios

### Error Handling
- ✅ Validation errors (blank IDs, emails)
- ✅ Proper AppError type mapping
- ✅ Exception handling and recovery

## Mock Implementation

The `MockFirestoreService` provides:
- In-memory storage that works across all platforms
- Proper validation and error handling
- Realistic behavior for testing sync scenarios
- No external dependencies (Firebase, network, etc.)

## Integration with Real Firebase

For integration testing with real Firebase:
1. Use Firebase Test Lab for Android
2. Use Firebase iOS SDK with test configuration
3. Consider using Firebase Emulator Suite for local testing

## Notes

- Tests use `kotlin.test` framework for multiplatform compatibility
- All tests are designed to be deterministic and isolated
- Mock implementation mirrors the behavior of real Firebase operations
- Error scenarios are tested to ensure proper error handling