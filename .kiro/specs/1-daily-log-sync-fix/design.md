# Design Document

## Overview

This design document outlines the technical approach to fix the daily log save/load and cross-platform sync issues. The solution standardizes Firebase collection paths, ensures the shared Kotlin code is the single source of truth, implements proper offline-first architecture with conflict resolution, and adds comprehensive structured logging.

### Key Design Principles

1. **Single Source of Truth**: All data operations flow through shared Kotlin code (LogRepository → FirestoreService)
2. **Offline-First**: Local cache is primary, Firebase is secondary with background sync
3. **Conflict Resolution**: Last-write-wins strategy based on `updatedAt` timestamp
4. **Path Consistency**: All platforms use `users/{userId}/dailyLogs/{logId}`
5. **UTC Standardization**: All dates and times stored/queried in UTC
6. **Structured Logging**: Consistent log format across platforms for debugging

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Platform Layer (iOS/Android)             │
│  ┌──────────────────┐              ┌──────────────────┐    │
│  │  iOS ViewModel   │              │ Android ViewModel│    │
│  │  (Swift wrapper) │              │   (Compose)      │    │
│  └────────┬─────────┘              └────────┬─────────┘    │
│           │                                  │               │
└───────────┼──────────────────────────────────┼──────────────┘
            │                                  │
            └──────────────┬───────────────────┘
                           │
┌──────────────────────────▼───────────────────────────────────┐
│              Shared Kotlin Layer (KMP)                        │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              Use Cases / ViewModels                   │   │
│  │  (GetDailyLogUseCase, SaveDailyLogUseCase, etc.)    │   │
│  └────────────────────────┬─────────────────────────────┘   │
│                           │                                   │
│  ┌────────────────────────▼─────────────────────────────┐   │
│  │           LogRepository (Interface)                   │   │
│  │  - saveDailyLog()                                    │   │
│  │  - getDailyLog()                                     │   │
│  │  - getLogsInRange()                                  │   │
│  │  - syncPendingChanges()                              │   │
│  └────────────────────────┬─────────────────────────────┘   │
│                           │                                   │
│  ┌────────────────────────▼─────────────────────────────┐   │
│  │        LogRepositoryImpl (Implementation)             │   │
│  │  ┌──────────────┐  ┌──────────────┐                 │   │
│  │  │ DailyLogDao  │  │  Firestore   │                 │   │
│  │  │  (Local DB)  │  │   Service    │                 │   │
│  │  └──────────────┘  └──────────────┘                 │   │
│  │  Offline-First + Conflict Resolution                 │   │
│  └──────────────────────────────────────────────────────┘   │
└───────────────────────────────────────────────────────────────┘
                           │
┌──────────────────────────▼───────────────────────────────────┐
│                    Firebase Firestore                         │
│         users/{userId}/dailyLogs/{logId}                     │
└───────────────────────────────────────────────────────────────┘
```

### Data Flow

#### Save Operation Flow
```
1. User Input (iOS/Android UI)
   ↓
2. Platform ViewModel calls shared UseCase
   ↓
3. SaveDailyLogUseCase validates and calls LogRepository
   ↓
4. LogRepository.saveDailyLog():
   a. Save to local cache (DailyLogDao) immediately
   b. Mark as pending sync
   c. Attempt Firebase sync in background
   d. If sync succeeds: mark as synced
   e. If sync fails: keep pending, schedule retry
   ↓
5. Return success to UI (even if Firebase fails)
```

#### Load Operation Flow
```
1. User navigates to date (iOS/Android UI)
   ↓
2. Platform ViewModel calls shared UseCase
   ↓
3. GetDailyLogUseCase calls LogRepository
   ↓
4. LogRepository.getDailyLog():
   a. Query Firebase first (if online)
   b. If Firebase returns data:
      - Compare updatedAt with local cache
      - Use newer version (conflict resolution)
      - Update local cache
   c. If Firebase fails/offline:
      - Return local cache data
   d. If no data anywhere: return null
   ↓
5. Return data to UI
```

## Components and Interfaces

### 1. Firebase Path Utility (Shared Kotlin)

**Location**: `shared/src/commonMain/kotlin/com/eunio/healthapp/data/remote/FirestorePaths.kt`

```kotlin
object FirestorePaths {
    private const val USERS_COLLECTION = "users"
    private const val DAILY_LOGS_COLLECTION = "dailyLogs"
    
    /**
     * Generates the Firestore document path for a daily log
     * @param userId The user ID
     * @param logId The log ID (format: yyyy-MM-dd in UTC)
     * @return Full document path: users/{userId}/dailyLogs/{logId}
     */
    fun dailyLogDoc(userId: String, logId: String): String {
        return "$USERS_COLLECTION/$userId/$DAILY_LOGS_COLLECTION/$logId"
    }
    
    /**
     * Generates the Firestore collection path for all daily logs of a user
     * @param userId The user ID
     * @return Collection path: users/{userId}/dailyLogs
     */
    fun dailyLogsCollection(userId: String): String {
        return "$USERS_COLLECTION/$userId/$DAILY_LOGS_COLLECTION"
    }
}
```

### 2. Document Schema and DTOs

**Location**: `shared/src/commonMain/kotlin/com/eunio/healthapp/data/remote/dto/DailyLogDto.kt`

```kotlin
@Serializable
data class DailyLogDto(
    val logId: String,                    // yyyy-MM-dd in UTC
    val dateEpochDays: Long,              // UTC epoch days
    val createdAt: Long,                  // Epoch seconds
    val updatedAt: Long,                  // Epoch seconds
    val periodFlow: String? = null,       // Enum name or null
    val symptoms: List<String>? = null,   // List of enum names or null
    val mood: String? = null,             // Enum name or null
    val bbt: Double? = null,              // Temperature or null
    val cervicalMucus: String? = null,    // Enum name or null
    val opkResult: String? = null,        // Enum name or null
    val notes: String? = null,            // Free text or null
    val v: Int = 1                        // Schema version
) {
    companion object {
        /**
         * Converts domain model to DTO for Firebase storage
         */
        fun fromDomain(log: DailyLog): DailyLogDto {
            return DailyLogDto(
                logId = log.id,
                dateEpochDays = log.date.toEpochDays().toLong(),
                createdAt = log.createdAt.epochSeconds,
                updatedAt = log.updatedAt.epochSeconds,
                periodFlow = log.periodFlow?.name,
                symptoms = log.symptoms.takeIf { it.isNotEmpty() }?.map { it.name },
                mood = log.mood?.name,
                bbt = log.bbt,
                cervicalMucus = log.cervicalMucus?.name,
                opkResult = log.opkResult?.name,
                notes = log.notes,
                v = 1
            )
        }
    }
    
    /**
     * Converts DTO to domain model
     */
    fun toDomain(userId: String): DailyLog {
        return DailyLog(
            id = logId,
            userId = userId,
            date = LocalDate.fromEpochDays(dateEpochDays.toInt()),
            periodFlow = periodFlow?.let { PeriodFlow.valueOf(it) },
            symptoms = symptoms?.map { Symptom.valueOf(it) } ?: emptyList(),
            mood = mood?.let { Mood.valueOf(it) },
            bbt = bbt,
            cervicalMucus = cervicalMucus?.let { CervicalMucus.valueOf(it) },
            opkResult = opkResult?.let { OPKResult.valueOf(it) },
            notes = notes,
            createdAt = Instant.fromEpochSeconds(createdAt),
            updatedAt = Instant.fromEpochSeconds(updatedAt)
        )
    }
}
```

### 3. Enhanced LogRepository Implementation

**Location**: `shared/src/commonMain/kotlin/com/eunio/healthapp/data/repository/LogRepositoryImpl.kt`

Key enhancements:
- Structured logging with operation tracking
- Conflict resolution based on `updatedAt`
- Retry queue for failed syncs
- Detailed timing metrics

```kotlin
class LogRepositoryImpl(
    private val firestoreService: FirestoreService,
    private val dailyLogDao: DailyLogDao,
    private val errorHandler: ErrorHandler
) : LogRepository {
    
    override suspend fun saveDailyLog(log: DailyLog): Result<Unit> {
        val startTime = Clock.System.now()
        
        return try {
            // Log operation start
            logStructured("SAVE_START", mapOf(
                "userId" to log.userId,
                "logId" to log.id,
                "dateEpochDays" to log.date.toEpochDays()
            ))
            
            // Validate
            validateLog(log)
            
            // Update timestamp
            val updatedLog = log.copy(updatedAt = Clock.System.now())
            
            // 1. Save locally first (offline-first)
            dailyLogDao.insertOrUpdate(updatedLog)
            dailyLogDao.markAsPendingSync(updatedLog.id)
            
            // 2. Attempt Firebase sync
            val remoteResult = firestoreService.saveDailyLog(updatedLog)
            val latencyMs = (Clock.System.now() - startTime).inWholeMilliseconds
            
            if (remoteResult.isSuccess) {
                dailyLogDao.markAsSynced(updatedLog.id)
                logStructured("FIRESTORE_WRITE", mapOf(
                    "path" to FirestorePaths.dailyLogDoc(log.userId, log.id),
                    "status" to "SUCCESS",
                    "latencyMs" to latencyMs
                ))
            } else {
                logStructured("FIRESTORE_WRITE", mapOf(
                    "path" to FirestorePaths.dailyLogDoc(log.userId, log.id),
                    "status" to "FAILED",
                    "latencyMs" to latencyMs,
                    "error" to remoteResult.errorOrNull()?.message
                ))
                // Don't fail - will retry later
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            logStructured("SAVE_ERROR", mapOf(
                "userId" to log.userId,
                "logId" to log.id,
                "error" to e.message
            ))
            Result.error(errorHandler.handleError(e))
        }
    }
    
    override suspend fun getDailyLog(userId: String, date: LocalDate): Result<DailyLog?> {
        return try {
            val logId = date.toLogId() // yyyy-MM-dd in UTC
            val path = FirestorePaths.dailyLogDoc(userId, logId)
            
            // Get local cache first
            val localLog = dailyLogDao.getDailyLogByUserIdAndDate(userId, date)
            
            // Try Firebase (if online)
            val remoteResult = firestoreService.getDailyLogByDate(userId, date)
            
            if (remoteResult.isSuccess) {
                val remoteLog = remoteResult.getOrNull()
                
                if (remoteLog != null) {
                    // Conflict resolution: compare updatedAt
                    val winner = if (localLog == null || remoteLog.updatedAt > localLog.updatedAt) {
                        logStructured("SYNC_RESULT", mapOf(
                            "direction" to "REMOTE_TO_LOCAL",
                            "merged" to false,
                            "winner" to "REMOTE",
                            "reason" to "Remote updatedAt is newer"
                        ))
                        remoteLog
                    } else {
                        logStructured("SYNC_RESULT", mapOf(
                            "direction" to "LOCAL_WINS",
                            "merged" to false,
                            "winner" to "LOCAL",
                            "reason" to "Local updatedAt is newer or equal"
                        ))
                        localLog
                    }
                    
                    // Update cache with winner
                    dailyLogDao.insertOrUpdate(winner)
                    dailyLogDao.markAsSynced(winner.id)
                    
                    logStructured("LOAD_RESULT", mapOf(
                        "path" to path,
                        "found" to true,
                        "docUpdatedAt" to remoteLog.updatedAt.epochSeconds,
                        "localUpdatedAt" to (localLog?.updatedAt?.epochSeconds ?: 0)
                    ))
                    
                    return Result.success(winner)
                } else {
                    logStructured("LOAD_RESULT", mapOf(
                        "path" to path,
                        "found" to false,
                        "docUpdatedAt" to null,
                        "localUpdatedAt" to (localLog?.updatedAt?.epochSeconds ?: 0)
                    ))
                }
            }
            
            // Fallback to local cache
            Result.success(localLog)
        } catch (e: Exception) {
            // On error, return local cache
            val localLog = dailyLogDao.getDailyLogByUserIdAndDate(userId, date)
            Result.success(localLog)
        }
    }
    
    /**
     * Syncs pending local changes to Firebase with exponential backoff
     */
    suspend fun syncPendingChanges(): Result<Unit> {
        return try {
            val pendingLogs = dailyLogDao.getPendingSync()
            
            for (log in pendingLogs) {
                val remoteResult = firestoreService.updateDailyLog(log)
                if (remoteResult.isSuccess) {
                    dailyLogDao.markAsSynced(log.id)
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
    }
    
    private fun logStructured(operation: String, data: Map<String, Any?>) {
        val message = "$operation ${data.entries.joinToString(", ") { "${it.key}=${it.value}" }}"
        platformLogDebug("DailyLogSync", message)
    }
    
    private fun LocalDate.toLogId(): String {
        // Format as yyyy-MM-dd in UTC
        return this.toString()
    }
}
```

### 4. Platform-Specific Logging

**Android**: `shared/src/androidMain/kotlin/com/eunio/healthapp/data/repository/LogRepositoryImpl.android.kt`
```kotlin
internal actual fun platformLogDebug(tag: String, message: String) {
    Log.d(tag, message)
}
```

**iOS**: `shared/src/iosMain/kotlin/com/eunio/healthapp/data/repository/LogRepositoryImpl.ios.kt`
```kotlin
import platform.Foundation.NSLog

internal actual fun platformLogDebug(tag: String, message: String) {
    NSLog("[$tag] $message")
}
```

### 5. iOS SwiftDailyLogService Updates

**Location**: `iosApp/iosApp/Services/SwiftDailyLogService.swift`

Changes:
- Update all paths to use `users/{userId}/dailyLogs/`
- Delegate to shared Kotlin code instead of direct Firebase access
- Remove duplicate loading logic

```swift
class SwiftDailyLogService {
    private let db = Firestore.firestore()
    
    // UPDATED: Use correct path
    func createLog(log: DailyLogData) async throws {
        let data = dailyLogToDict(log: log)
        try await db.collection("users")
            .document(log.userId)
            .collection("dailyLogs")  // ← Fixed
            .document(log.id)
            .setData(data)
    }
    
    // UPDATED: Use correct path
    func getLog(userId: String, logId: String) async throws -> DailyLogData? {
        let snapshot = try await db.collection("users")  // ← Fixed
            .document(userId)
            .collection("dailyLogs")  // ← Fixed
            .document(logId)
            .getDocument()
        
        guard let data = snapshot.data() else {
            return nil
        }
        
        return dictToDailyLog(data: data, id: logId, userId: userId)
    }
    
    // Similar updates for updateLog, deleteLog, getLogsByDateRange
}
```

## Data Models

### Firebase Document Structure

```json
{
  "logId": "2025-10-04",
  "dateEpochDays": 20259,
  "createdAt": 1696392000,
  "updatedAt": 1696392305,
  "periodFlow": "MEDIUM",
  "symptoms": ["CRAMPS", "HEADACHE"],
  "mood": "TIRED",
  "bbt": 97.8,
  "cervicalMucus": "CREAMY",
  "opkResult": "NEGATIVE",
  "notes": "Feeling tired today",
  "v": 1
}
```

### Local Cache Schema

The local database (Room/SQLDelight) stores the same data structure with additional sync metadata:

```kotlin
@Entity(tableName = "daily_logs")
data class DailyLogEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val dateEpochDays: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val periodFlow: String?,
    val symptoms: String?,  // JSON array
    val mood: String?,
    val bbt: Double?,
    val cervicalMucus: String?,
    val opkResult: String?,
    val notes: String?,
    val isSynced: Boolean = false,
    val pendingSync: Boolean = false,
    val lastSyncAttempt: Long? = null,
    val syncRetryCount: Int = 0
)
```

## Error Handling

### Offline Handling
- All save operations succeed locally even if Firebase is unavailable
- Failed syncs are queued and retried with exponential backoff
- Load operations fall back to local cache when Firebase is unavailable

### Conflict Resolution
- Use `updatedAt` timestamp to determine which version is newer
- Last-write-wins strategy: newer `updatedAt` always wins
- Log both versions before overwriting for debugging

### Retry Strategy
```kotlin
class SyncRetryManager {
    private val maxRetries = 5
    private val baseDelayMs = 1000L
    
    suspend fun retryWithBackoff(attempt: Int, operation: suspend () -> Result<Unit>): Result<Unit> {
        if (attempt >= maxRetries) {
            return Result.error(AppError.NetworkError("Max retries exceeded"))
        }
        
        val delayMs = baseDelayMs * (2.0.pow(attempt)).toLong()
        delay(delayMs)
        
        return operation()
    }
}
```

## Testing Strategy

### Unit Tests
- Test path generation utility
- Test DTO conversion (domain ↔ DTO)
- Test conflict resolution logic
- Test retry backoff calculation

### Integration Tests
- Test save → load round trip
- Test offline save → online sync
- Test conflict resolution with concurrent updates
- Test cross-platform sync (iOS → Android, Android → iOS)

### Manual Testing Scenarios
1. **iOS → Android Sync**: Save on iOS, verify visible on Android
2. **Android → iOS Sync**: Save on Android, verify visible on iOS
3. **Offline Mode**: Save while offline, verify syncs when online
4. **Conflict Resolution**: Update same log on both platforms, verify last-write-wins
5. **App Restart**: Save data, restart app, verify data persists

## Migration Strategy

### Legacy Data Migration

**Location**: `shared/src/commonMain/kotlin/com/eunio/healthapp/data/migration/DailyLogMigration.kt`

```kotlin
class DailyLogMigration(
    private val firestore: FirebaseFirestore
) {
    suspend fun migrateLegacyLogs(userId: String): MigrationResult {
        val legacyPath = "daily_logs/$userId/logs"
        val newPath = "users/$userId/dailyLogs"
        
        var migratedCount = 0
        var errorCount = 0
        val errors = mutableListOf<String>()
        
        try {
            // Query legacy collection
            val legacyDocs = firestore.collection("daily_logs")
                .document(userId)
                .collection("logs")
                .get()
                .await()
            
            for (doc in legacyDocs.documents) {
                try {
                    val data = doc.data()
                    val logId = doc.id
                    
                    // Check if already exists in new location
                    val existingDoc = firestore.collection("users")
                        .document(userId)
                        .collection("dailyLogs")
                        .document(logId)
                        .get()
                        .await()
                    
                    if (!existingDoc.exists()) {
                        // Copy to new location
                        firestore.collection("users")
                            .document(userId)
                            .collection("dailyLogs")
                            .document(logId)
                            .set(data)
                            .await()
                        
                        migratedCount++
                        Log.d("Migration", "Migrated log: $logId")
                    } else {
                        Log.d("Migration", "Skipped existing log: $logId")
                    }
                } catch (e: Exception) {
                    errorCount++
                    errors.add("Failed to migrate ${doc.id}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            return MigrationResult(
                success = false,
                migratedCount = migratedCount,
                errorCount = errorCount,
                errors = errors + "Migration failed: ${e.message}"
            )
        }
        
        return MigrationResult(
            success = true,
            migratedCount = migratedCount,
            errorCount = errorCount,
            errors = errors
        )
    }
}

data class MigrationResult(
    val success: Boolean,
    val migratedCount: Int,
    val errorCount: Int,
    val errors: List<String>
)
```

## Firebase Security Rules

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // New standardized path
    match /users/{userId}/dailyLogs/{logId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Legacy path (read-only during migration)
    match /daily_logs/{userId}/logs/{logId} {
      allow read: if request.auth != null && request.auth.uid == userId;
      allow write: if false; // Disable writes to legacy path
    }
  }
}
```

## Performance Considerations

### Indexing
Create composite indexes for efficient queries:
- `users/{userId}/dailyLogs` collection: index on `dateEpochDays` (ascending/descending)
- `users/{userId}/dailyLogs` collection: composite index on `dateEpochDays` + `updatedAt`

### Caching Strategy
- Local cache is primary data source for reads
- Firebase is queried in background to check for updates
- Cache invalidation based on `updatedAt` comparison

### Batch Operations
- Sync operations use batch writes when possible
- Limit batch size to 500 documents (Firestore limit)

## Monitoring and Observability

### Structured Logging Format
All logs follow this format:
```
[DailyLogSync] OPERATION_NAME key1=value1, key2=value2, ...
```

Examples:
```
[DailyLogSync] SAVE_START userId=user123, logId=2025-10-04, dateEpochDays=20259
[DailyLogSync] FIRESTORE_WRITE path=users/user123/dailyLogs/2025-10-04, status=SUCCESS, latencyMs=245
[DailyLogSync] LOAD_RESULT path=users/user123/dailyLogs/2025-10-04, found=true, docUpdatedAt=1696392305, localUpdatedAt=1696392000
[DailyLogSync] SYNC_RESULT direction=REMOTE_TO_LOCAL, merged=false, winner=REMOTE, reason=Remote updatedAt is newer
```

### Metrics to Track
- Save operation latency
- Firebase sync success/failure rate
- Conflict resolution frequency
- Retry attempt counts
- Cache hit/miss ratio
