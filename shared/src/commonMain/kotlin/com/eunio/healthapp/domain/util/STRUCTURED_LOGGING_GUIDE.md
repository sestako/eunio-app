# Structured Logging Guide

## Overview

The `StructuredLogger` utility provides consistent, platform-agnostic logging for the Eunio Health App. It ensures that all log messages follow a standardized format, making it easier to debug issues and analyze application behavior across iOS and Android platforms.

## Log Format

All structured logs follow this format:

```
[tag] OPERATION_NAME key1=value1, key2=value2, ...
```

### Examples

```
[DailyLogSync] SAVE_START userId=user123, logId=2025-10-04, dateEpochDays=20259
[DailyLogSync] FIRESTORE_WRITE path=users/user123/dailyLogs/2025-10-04, status=SUCCESS, latencyMs=245
[DailyLogSync] LOAD_RESULT path=users/user123/dailyLogs/2025-10-04, found=true, docUpdatedAt=1696392305, localUpdatedAt=1696392000
[DailyLogSync] SYNC_RESULT direction=REMOTE_TO_LOCAL, merged=false, winner=REMOTE, reason=Remote updatedAt is newer
```

## Platform-Specific Implementation

### Android
Uses `android.util.Log.d()` for debug logging.

```kotlin
// Output in Logcat:
D/DailyLogSync: SAVE_START userId=user123, logId=2025-10-04, dateEpochDays=20259
```

### iOS
Uses `NSLog()` for logging with tag prefix.

```swift
// Output in Console:
[DailyLogSync] SAVE_START userId=user123, logId=2025-10-04, dateEpochDays=20259
```

## Standard Log Operations

The `StructuredLogger.LogOperation` enum defines standard operations for daily log sync:

### SAVE_START
Logged when a save operation begins.

**Required fields:**
- `userId`: The user ID
- `logId`: The log ID (format: yyyy-MM-dd)
- `dateEpochDays`: The date as UTC epoch days

**Example:**
```kotlin
StructuredLogger.logStructured(
    tag = "DailyLogSync",
    operation = StructuredLogger.LogOperation.SAVE_START,
    data = mapOf(
        "userId" to "user123",
        "logId" to "2025-10-04",
        "dateEpochDays" to 20259
    )
)
```

### FIRESTORE_WRITE
Logged when writing to Firestore.

**Required fields:**
- `path`: The Firestore document path
- `status`: SUCCESS or FAILED
- `latencyMs`: Operation latency in milliseconds

**Optional fields:**
- `error`: Error message if status is FAILED

**Example:**
```kotlin
StructuredLogger.logStructured(
    tag = "DailyLogSync",
    operation = StructuredLogger.LogOperation.FIRESTORE_WRITE,
    data = mapOf(
        "path" to "users/user123/dailyLogs/2025-10-04",
        "status" to "SUCCESS",
        "latencyMs" to 245
    )
)
```

### LOAD_RESULT
Logged when loading data from Firestore or local cache.

**Required fields:**
- `path`: The Firestore document path
- `found`: Boolean indicating if data was found

**Optional fields:**
- `docUpdatedAt`: Remote document's updatedAt timestamp (epoch seconds)
- `localUpdatedAt`: Local cache's updatedAt timestamp (epoch seconds)

**Example:**
```kotlin
StructuredLogger.logStructured(
    tag = "DailyLogSync",
    operation = StructuredLogger.LogOperation.LOAD_RESULT,
    data = mapOf(
        "path" to "users/user123/dailyLogs/2025-10-04",
        "found" to true,
        "docUpdatedAt" to 1696392305L,
        "localUpdatedAt" to 1696392000L
    )
)
```

### SYNC_RESULT
Logged when syncing data between remote and local storage.

**Required fields:**
- `direction`: REMOTE_TO_LOCAL, LOCAL_TO_REMOTE, or LOCAL_WINS
- `merged`: Boolean indicating if data was merged
- `winner`: REMOTE or LOCAL
- `reason`: Human-readable explanation

**Example:**
```kotlin
StructuredLogger.logStructured(
    tag = "DailyLogSync",
    operation = StructuredLogger.LogOperation.SYNC_RESULT,
    data = mapOf(
        "direction" to "REMOTE_TO_LOCAL",
        "merged" to false,
        "winner" to "REMOTE",
        "reason" to "Remote updatedAt is newer"
    )
)
```

### Error Operations
- `SAVE_ERROR`: Logged when a save operation fails
- `LOAD_ERROR`: Logged when a load operation fails
- `SYNC_ERROR`: Logged when a sync operation fails

**Required fields:**
- `userId`: The user ID
- `logId`: The log ID
- `error`: Error message

**Example:**
```kotlin
StructuredLogger.logStructured(
    tag = "DailyLogSync",
    operation = StructuredLogger.LogOperation.SAVE_ERROR,
    data = mapOf(
        "userId" to "user123",
        "logId" to "2025-10-04",
        "error" to "Database connection failed"
    )
)
```

## Usage in Repository

Here's how to integrate structured logging into `LogRepositoryImpl`:

```kotlin
class LogRepositoryImpl(
    private val firestoreService: FirestoreService,
    private val dailyLogDao: DailyLogDao
) : LogRepository {
    
    override suspend fun saveDailyLog(log: DailyLog): Result<Unit> {
        val startTime = Clock.System.now()
        
        // Log operation start
        StructuredLogger.logStructured(
            tag = "DailyLogSync",
            operation = StructuredLogger.LogOperation.SAVE_START,
            data = mapOf(
                "userId" to log.userId,
                "logId" to log.id,
                "dateEpochDays" to log.date.toEpochDays()
            )
        )
        
        try {
            // Save locally
            dailyLogDao.insertOrUpdate(log)
            
            // Sync to Firebase
            val remoteResult = firestoreService.saveDailyLog(log)
            val latencyMs = (Clock.System.now() - startTime).inWholeMilliseconds
            
            if (remoteResult.isSuccess) {
                StructuredLogger.logStructured(
                    tag = "DailyLogSync",
                    operation = StructuredLogger.LogOperation.FIRESTORE_WRITE,
                    data = mapOf(
                        "path" to FirestorePaths.dailyLogDoc(log.userId, log.id),
                        "status" to "SUCCESS",
                        "latencyMs" to latencyMs
                    )
                )
            } else {
                StructuredLogger.logStructured(
                    tag = "DailyLogSync",
                    operation = StructuredLogger.LogOperation.FIRESTORE_WRITE,
                    data = mapOf(
                        "path" to FirestorePaths.dailyLogDoc(log.userId, log.id),
                        "status" to "FAILED",
                        "latencyMs" to latencyMs,
                        "error" to remoteResult.errorOrNull()?.message
                    )
                )
            }
            
            return Result.success(Unit)
        } catch (e: Exception) {
            StructuredLogger.logStructured(
                tag = "DailyLogSync",
                operation = StructuredLogger.LogOperation.SAVE_ERROR,
                data = mapOf(
                    "userId" to log.userId,
                    "logId" to log.id,
                    "error" to e.message
                )
            )
            return Result.error(e)
        }
    }
}
```

## Custom Operations

You can also log custom operations using string operation names:

```kotlin
StructuredLogger.logStructured(
    tag = "DailyLogSync",
    operation = "MIGRATION_START",
    data = mapOf(
        "userId" to "user123",
        "legacyCount" to 150
    )
)
```

## Best Practices

1. **Always include context**: Include userId, logId, and other identifying information
2. **Log timing**: Include latencyMs for operations that involve network or database calls
3. **Log both success and failure**: Always log the outcome of operations
4. **Use consistent tags**: Use "DailyLogSync" for all daily log sync operations
5. **Include error details**: When logging errors, include the error message
6. **Avoid sensitive data**: Never log passwords, tokens, or other sensitive information
7. **Use null for optional fields**: If a field is not applicable, use null rather than omitting it

## Filtering Logs

### Android (Logcat)
```bash
# Filter by tag
adb logcat -s DailyLogSync

# Filter by operation
adb logcat | grep "SAVE_START"

# Filter by user
adb logcat | grep "userId=user123"
```

### iOS (Console)
```bash
# Filter by tag
log stream --predicate 'eventMessage contains "DailyLogSync"'

# Filter by operation
log stream --predicate 'eventMessage contains "SAVE_START"'
```

## Requirements Satisfied

This structured logging implementation satisfies the following requirements:

- **6.1**: SAVE_START logs include userId, logId, dateEpochDays
- **6.2**: FIRESTORE_WRITE logs include path, status, latencyMs, error
- **6.3**: LOAD_RESULT logs include path, found, docUpdatedAt, localUpdatedAt
- **6.4**: SYNC_RESULT logs include direction, merged, winner, reason
- **6.5**: Platform-specific logging (Log.d for Android, NSLog for iOS)
- **6.6**: Logs clearly show operation type, Firebase path, success/failure, and timing
- **6.7**: Error logs include error message and relevant context
