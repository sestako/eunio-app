# Design Document: iOS Firebase Sync Fix

## Overview

This design document outlines the implementation strategy for enabling Firebase Firestore synchronization on iOS. Currently, the iOS app uses a mock in-memory implementation of `FirestoreService`, while Android uses the actual Firebase SDK. This creates a critical data inconsistency where iOS users' data is not backed up or synced to the cloud.

The solution involves replacing the mock iOS implementation with actual Firebase iOS SDK calls using Kotlin/Native interop, while maintaining the existing architecture and ensuring cross-platform data consistency.

## Architecture

### Current Architecture

```
iOS App (Swift)
    ↓
ModernDailyLoggingViewModel (Swift)
    ↓
DailyLoggingViewModel (Kotlin Shared)
    ↓
LogRepository (Kotlin Shared)
    ↓
FirestoreServiceImpl.ios.kt (MOCK - In-Memory Only) ❌
```

### Target Architecture

```
iOS App (Swift)
    ↓
ModernDailyLoggingViewModel (Swift)
    ↓
DailyLoggingViewModel (Kotlin Shared)
    ↓
LogRepository (Kotlin Shared)
    ↓
FirestoreServiceImpl.ios.kt (Kotlin/Native)
    ↓
FirebaseIOSBridge (Swift) - NEW
    ↓
Firebase iOS SDK (Firestore) ✅
```

### Architecture Decisions

1. **Kotlin/Native Interop Approach**: Use Kotlin/Native's `expect`/`actual` pattern with Swift bridging
   - **Rationale**: Maintains consistency with existing KMP architecture while leveraging native Firebase SDK
   - **Alternative Considered**: Pure Swift implementation - rejected because it would bypass shared business logic

2. **Swift Bridge Layer**: Create a thin Swift wrapper around Firebase iOS SDK
   - **Rationale**: Firebase iOS SDK is Objective-C/Swift, easier to wrap than use directly from Kotlin/Native
   - **Alternative Considered**: Direct cinterop - rejected due to complexity and maintenance burden

3. **Maintain Existing Interface**: Keep `FirestoreService` interface unchanged
   - **Rationale**: Minimizes changes to shared code and ensures Android compatibility
   - **Alternative Considered**: Create iOS-specific interface - rejected to maintain cross-platform consistency

## Components and Interfaces

### 1. FirebaseIOSBridge (Swift)

A new Swift class that wraps Firebase iOS SDK operations and exposes them to Kotlin/Native.

```swift
// Location: iosApp/iosApp/Services/FirebaseIOSBridge.swift

import Foundation
import FirebaseFirestore

@objc public class FirebaseIOSBridge: NSObject {
    private let db = Firestore.firestore()
    
    // Daily Log Operations
    @objc public func saveDailyLog(
        userId: String,
        logId: String,
        data: [String: Any],
        completion: @escaping (Error?) -> Void
    ) {
        db.collection("users")
            .document(userId)
            .collection("dailyLogs")
            .document(logId)
            .setData(data) { error in
                completion(error)
            }
    }
    
    @objc public func getDailyLog(
        userId: String,
        logId: String,
        completion: @escaping ([String: Any]?, Error?) -> Void
    ) {
        db.collection("users")
            .document(userId)
            .collection("dailyLogs")
            .document(logId)
            .getDocument { snapshot, error in
                completion(snapshot?.data(), error)
            }
    }
    
    // Additional operations for cycles, insights, etc.
    // ...
}
```

### 2. FirestoreServiceImpl.ios.kt (Updated)

Replace the mock implementation with actual Firebase calls via the Swift bridge.

```kotlin
// Location: shared/src/iosMain/kotlin/com/eunio/healthapp/data/remote/FirestoreServiceImpl.ios.kt

package com.eunio.healthapp.data.remote

import com.eunio.healthapp.domain.model.*
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.domain.util.ErrorHandler
import kotlinx.datetime.LocalDate
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual class FirestoreServiceImpl actual constructor(
    private val errorHandler: ErrorHandler
) : FirestoreService {
    
    private val bridge = FirebaseIOSBridge()
    
    override suspend fun saveDailyLog(dailyLog: DailyLog): Result<Unit> {
        return Result.catching(errorHandler) {
            val dto = DailyLogDto.fromDomain(dailyLog)
            val data = dto.toMap()
            
            suspendCancellableCoroutine { continuation ->
                bridge.saveDailyLog(
                    userId = dailyLog.userId,
                    logId = dailyLog.id,
                    data = data
                ) { error ->
                    if (error != null) {
                        continuation.resumeWithException(error)
                    } else {
                        continuation.resume(Unit)
                    }
                }
            }
        }
    }
    
    // Additional implementations...
}
```

### 3. Kotlin/Native Interop Configuration

Update the iOS build configuration to expose the Swift bridge to Kotlin.

```kotlin
// Location: shared/build.gradle.kts

kotlin {
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = true
            
            // Export Firebase bridge
            export("com.eunio.healthapp:firebase-ios-bridge:1.0.0")
        }
        
        iosTarget.compilations.getByName("main") {
            cinterops {
                val FirebaseIOSBridge by creating {
                    defFile(project.file("src/iosMain/def/FirebaseIOSBridge.def"))
                    packageName("com.eunio.healthapp.firebase")
                }
            }
        }
    }
}
```

## Data Models

### DailyLogDto Mapping

Ensure consistent data format between iOS and Android:

```kotlin
// Shared DTO structure (already exists)
data class DailyLogDto(
    val date: Long,              // Epoch days
    val periodFlow: String?,
    val symptoms: List<String>,
    val mood: String?,
    val bbt: Double?,
    val cervicalMucus: String?,
    val opkResult: String?,
    val notes: String?,
    val createdAt: Long,         // Seconds since epoch
    val updatedAt: Long          // Seconds since epoch
) {
    fun toMap(): Map<String, Any> {
        return buildMap {
            put("date", date)
            periodFlow?.let { put("periodFlow", it) }
            if (symptoms.isNotEmpty()) put("symptoms", symptoms)
            mood?.let { put("mood", it) }
            bbt?.let { put("bbt", it) }
            cervicalMucus?.let { put("cervicalMucus", it) }
            opkResult?.let { put("opkResult", it) }
            notes?.let { put("notes", it) }
            put("createdAt", createdAt)
            put("updatedAt", updatedAt)
        }
    }
}
```

### Path Consistency

Use `FirestorePaths` utility for all path generation:

```kotlin
// Example usage in FirestoreServiceImpl.ios.kt
val path = FirestorePaths.dailyLogDoc(userId, logId)
// Returns: "users/{userId}/dailyLogs/{logId}"
```

## Error Handling

### Error Mapping Strategy

Map Firebase iOS errors to domain errors:

```kotlin
// Location: shared/src/iosMain/kotlin/com/eunio/healthapp/data/remote/FirebaseErrorMapper.kt

object FirebaseErrorMapper {
    fun mapError(error: NSError): AppError {
        return when (error.code) {
            FirestoreErrorCode.UNAVAILABLE -> AppError.NetworkError("No internet connection")
            FirestoreErrorCode.UNAUTHENTICATED -> AppError.AuthenticationError("Please sign in again")
            FirestoreErrorCode.PERMISSION_DENIED -> AppError.AuthorizationError("Access denied")
            FirestoreErrorCode.NOT_FOUND -> AppError.NotFoundError("Document not found")
            else -> AppError.UnknownError("Firebase error: ${error.localizedDescription}")
        }
    }
}
```

### Logging Strategy

Implement structured logging for debugging:

```kotlin
// Use existing StructuredLogger
override suspend fun saveDailyLog(dailyLog: DailyLog): Result<Unit> {
    logger.info(
        message = "Saving daily log to Firebase",
        context = mapOf(
            "userId" to dailyLog.userId,
            "logId" to dailyLog.id,
            "date" to dailyLog.date.toString(),
            "path" to FirestorePaths.dailyLogDoc(dailyLog.userId, dailyLog.id)
        )
    )
    
    return Result.catching(errorHandler) {
        // ... save operation ...
        
        logger.info(
            message = "Successfully saved daily log to Firebase",
            context = mapOf(
                "userId" to dailyLog.userId,
                "logId" to dailyLog.id
            )
        )
    }.onFailure { error ->
        logger.error(
            message = "Failed to save daily log to Firebase",
            error = error,
            context = mapOf(
                "userId" to dailyLog.userId,
                "logId" to dailyLog.id
            )
        )
    }
}
```

## Testing Strategy

### Unit Tests

Test the Kotlin/Native implementation with mocked Swift bridge:

```kotlin
// Location: shared/src/iosTest/kotlin/com/eunio/healthapp/data/remote/FirestoreServiceImplTest.kt

class FirestoreServiceImplTest {
    @Test
    fun `saveDailyLog should call bridge with correct parameters`() = runTest {
        // Given
        val mockBridge = MockFirebaseIOSBridge()
        val service = FirestoreServiceImpl(mockBridge, mockErrorHandler)
        val dailyLog = createTestDailyLog()
        
        // When
        val result = service.saveDailyLog(dailyLog)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(dailyLog.userId, mockBridge.lastUserId)
        assertEquals(dailyLog.id, mockBridge.lastLogId)
    }
}
```

### Integration Tests

Test actual Firebase operations on iOS simulator:

```swift
// Location: iosApp/iosAppTests/FirebaseSyncTests.swift

class FirebaseSyncTests: XCTestCase {
    func testSaveDailyLogToFirebase() async throws {
        // Given
        let userId = "test_user_\(UUID().uuidString)"
        let logId = "2025-10-14"
        let viewModel = createTestViewModel()
        
        // When
        try await viewModel.saveLog()
        
        // Then
        let db = Firestore.firestore()
        let snapshot = try await db.collection("users")
            .document(userId)
            .collection("dailyLogs")
            .document(logId)
            .getDocument()
        
        XCTAssertTrue(snapshot.exists)
        XCTAssertEqual(snapshot.data()?["date"] as? Int64, 20371) // Epoch days
    }
}
```

### Cross-Platform Sync Tests

Verify iOS-Android data compatibility:

```kotlin
// Location: shared/src/commonTest/kotlin/com/eunio/healthapp/data/sync/CrossPlatformSyncTest.kt

class CrossPlatformSyncTest {
    @Test
    fun `iOS saved log should be readable by Android`() = runTest {
        // Given: Log saved from iOS
        val iosService = createIOSFirestoreService()
        val dailyLog = createTestDailyLog()
        iosService.saveDailyLog(dailyLog).getOrThrow()
        
        // When: Android reads the log
        val androidService = createAndroidFirestoreService()
        val result = androidService.getDailyLog(dailyLog.userId, dailyLog.id)
        
        // Then: Data should match exactly
        assertTrue(result.isSuccess)
        val retrievedLog = result.getOrNull()
        assertNotNull(retrievedLog)
        assertEquals(dailyLog.date, retrievedLog.date)
        assertEquals(dailyLog.periodFlow, retrievedLog.periodFlow)
        assertEquals(dailyLog.symptoms, retrievedLog.symptoms)
    }
}
```

## Implementation Phases

### Phase 1: Swift Bridge Setup
1. Create `FirebaseIOSBridge.swift` with daily log operations
2. Configure Kotlin/Native cinterop
3. Verify bridge is accessible from Kotlin

### Phase 2: Kotlin Implementation
1. Update `FirestoreServiceImpl.ios.kt` to use bridge
2. Implement daily log save/read operations
3. Add error mapping and logging

### Phase 3: Testing & Validation
1. Write unit tests for Kotlin implementation
2. Write integration tests for Firebase operations
3. Test cross-platform sync (iOS ↔ Android)

### Phase 4: Remaining Operations
1. Implement cycle operations
2. Implement insight operations
3. Implement user operations
4. Implement batch operations

### Phase 5: Production Readiness
1. Add offline support (already exists in repository layer)
2. Add conflict resolution (use existing strategy)
3. Performance optimization
4. Documentation updates

## Security Considerations

1. **Firebase Rules**: Ensure Firestore security rules allow iOS writes
   ```javascript
   // firestore.rules
   match /users/{userId}/dailyLogs/{logId} {
     allow read, write: if request.auth != null && request.auth.uid == userId;
   }
   ```

2. **Authentication**: Verify Firebase Auth is properly initialized on iOS
   ```swift
   // Ensure FirebaseApp.configure() is called in app initialization
   ```

3. **Data Validation**: Validate data before sending to Firebase
   ```kotlin
   // Use existing validation in DailyLogDto
   require(dailyLog.userId.isNotBlank()) { "User ID cannot be blank" }
   require(dailyLog.id.isNotBlank()) { "Log ID cannot be blank" }
   ```

## Performance Considerations

1. **Batch Operations**: Use Firebase batch writes for multiple documents
2. **Caching**: Leverage existing SQLite cache to minimize Firebase reads
3. **Offline Support**: Use existing repository layer's offline-first strategy
4. **Connection Monitoring**: Use existing network monitor to defer syncs when offline

## Migration Strategy

1. **No Data Migration Needed**: Mock implementation had no persistent data
2. **Gradual Rollout**: Deploy to TestFlight first
3. **Monitoring**: Add Firebase Analytics events for sync operations
4. **Rollback Plan**: Keep mock implementation as fallback (feature flag)

## Success Metrics

1. **Functional**: iOS logs appear in Firebase Console
2. **Cross-Platform**: Android can read iOS logs and vice versa
3. **Performance**: Save operations complete within 2 seconds
4. **Reliability**: 99% success rate for Firebase operations when online
5. **User Experience**: Clear feedback for save success/failure
