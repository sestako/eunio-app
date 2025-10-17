# Daily Log Migration Guide

This guide explains how to migrate daily logs from the legacy Firebase path to the new standardized path.

## Overview

**Legacy Path:** `daily_logs/{userId}/logs/{logId}`  
**New Path:** `users/{userId}/dailyLogs/{logId}`

The migration utility safely copies all daily log documents from the legacy path to the new path without data loss.

## Features

- ✅ **Idempotent**: Safe to run multiple times - skips documents that already exist at the new path
- ✅ **Resilient**: Continues processing even if individual documents fail
- ✅ **Logged**: Comprehensive structured logging for debugging and monitoring
- ✅ **Progress Tracking**: Returns detailed results with counts and errors
- ✅ **Platform-Specific**: Optimized implementations for Android and iOS

## Usage

### Basic Usage

```kotlin
import com.eunio.healthapp.data.sync.DailyLogMigrationFactory

// Create migration instance
val migration = DailyLogMigrationFactory.create()

// Run migration for a user
val result = migration.migrateLegacyLogs(userId = "user123")

// Check results
if (result.success) {
    println("Migration completed successfully!")
    println("Migrated: ${result.migratedCount} documents")
    println("Skipped: ${result.skippedCount} documents (already exist)")
} else {
    println("Migration failed with ${result.errorCount} errors")
    result.errors.forEach { error ->
        println("Error: $error")
    }
}

// Print full summary
println(result.summary())
```

### Android-Specific Usage

```kotlin
import com.eunio.healthapp.data.sync.AndroidDailyLogMigration
import com.google.firebase.firestore.FirebaseFirestore

// Create Android-specific migration
val firestore = FirebaseFirestore.getInstance()
val migration = AndroidDailyLogMigration(firestore)

// Run migration
val result = migration.migrateLegacyLogs(userId)
```

### iOS Usage

For iOS, the current implementation uses a mock. For production, you should either:

1. Use the SwiftDailyLogService from the iOS app layer to perform migration
2. Implement proper Kotlin/Native bindings to Firebase iOS SDK

```swift
// iOS Swift example using SwiftDailyLogService
let service = SwiftDailyLogService()
await service.migrateLegacyLogs(userId: "user123")
```

## Migration Result

The `MigrationResult` object contains:

```kotlin
data class MigrationResult(
    val success: Boolean,        // Overall success status
    val migratedCount: Int,      // Number of documents migrated
    val skippedCount: Int,       // Number of documents skipped (already exist)
    val errorCount: Int,         // Number of documents that failed
    val errors: List<String>     // List of error messages
)
```

## Logging

The migration utility uses structured logging. All logs follow this format:

```
[DailyLogMigration] OPERATION_NAME key1=value1, key2=value2, ...
```

### Log Operations

- `MIGRATION_START`: Migration begins for a user
- `MIGRATION_PATHS`: Shows legacy and new paths
- `MIGRATION_FOUND`: Number of legacy documents found
- `MIGRATION_NO_DATA`: No legacy data found for user
- `MIGRATION_SKIP`: Document skipped (already exists)
- `MIGRATION_SUCCESS`: Document successfully migrated
- `MIGRATION_ERROR`: Error migrating specific document
- `MIGRATION_COMPLETE`: Migration finished with summary
- `MIGRATION_FATAL`: Critical error that stopped migration

### Example Logs

```
[DailyLogMigration] MIGRATION_START userId=user123
[DailyLogMigration] MIGRATION_PATHS legacyPath=daily_logs/user123/logs, newPath=users/user123/dailyLogs
[DailyLogMigration] MIGRATION_FOUND userId=user123, documentCount=15
[DailyLogMigration] MIGRATION_SUCCESS userId=user123, logId=2025-10-01, progress=1/15
[DailyLogMigration] MIGRATION_SKIP userId=user123, logId=2025-10-02, reason=Document already exists at new path
[DailyLogMigration] MIGRATION_COMPLETE userId=user123, success=true, migratedCount=14, skippedCount=1, errorCount=0
```

## Best Practices

### 1. Test First

Always test migration on a test user before running on production data:

```kotlin
val testUserId = "test_user_123"
val result = migration.migrateLegacyLogs(testUserId)
println(result.summary())
```

### 2. Run During Low Traffic

Schedule migration during low-traffic periods to minimize impact.

### 3. Monitor Logs

Watch the structured logs to track progress and catch issues early.

### 4. Handle Errors

Check the result and handle errors appropriately:

```kotlin
val result = migration.migrateLegacyLogs(userId)

if (result.errorCount > 0) {
    // Log errors for investigation
    logger.error("Migration had ${result.errorCount} errors")
    result.errors.forEach { error ->
        logger.error("Migration error: $error")
    }
    
    // Optionally retry failed documents
    if (result.migratedCount > 0) {
        // Partial success - some documents migrated
        // Consider retrying later
    }
}
```

### 5. Verify Migration

After migration, verify data integrity:

```kotlin
// Query both paths to verify
val legacyLog = queryLegacyPath(userId, logId)
val newLog = queryNewPath(userId, logId)

// Compare data
if (legacyLog != null && newLog != null) {
    assert(legacyLog.dateEpochDays == newLog.dateEpochDays)
    assert(legacyLog.notes == newLog.notes)
    // ... verify other fields
}
```

## Batch Migration

To migrate multiple users:

```kotlin
suspend fun migrateAllUsers(userIds: List<String>) {
    val migration = DailyLogMigrationFactory.create()
    val results = mutableMapOf<String, MigrationResult>()
    
    userIds.forEach { userId ->
        try {
            val result = migration.migrateLegacyLogs(userId)
            results[userId] = result
            
            println("User $userId: ${result.migratedCount} migrated, ${result.errorCount} errors")
        } catch (e: Exception) {
            println("Failed to migrate user $userId: ${e.message}")
        }
    }
    
    // Summary
    val totalMigrated = results.values.sumOf { it.migratedCount }
    val totalErrors = results.values.sumOf { it.errorCount }
    println("Total migrated: $totalMigrated documents")
    println("Total errors: $totalErrors documents")
}
```

## Rollback

If you need to rollback (copy data back to legacy path), you can create a reverse migration:

```kotlin
// Note: This is not implemented by default
// You would need to implement a reverse migration utility
// that copies from new path back to legacy path
```

## Cleanup

After successful migration and verification, you can:

1. Update Firebase security rules to make legacy path read-only
2. Eventually delete legacy data (after sufficient verification period)
3. Remove legacy path support from code

```javascript
// Firebase security rules - make legacy path read-only
match /daily_logs/{userId}/logs/{logId} {
  allow read: if request.auth != null && request.auth.uid == userId;
  allow write: if false; // Disable writes
}
```

## Troubleshooting

### Migration Returns Zero Documents

**Cause**: No legacy data exists for the user  
**Solution**: This is normal if the user has no legacy data or already migrated

### Migration Fails with Permission Error

**Cause**: Firebase security rules may be blocking access  
**Solution**: Ensure security rules allow read from legacy path and write to new path

### Some Documents Fail to Migrate

**Cause**: Individual document errors (network, permissions, invalid data)  
**Solution**: Check error messages in result.errors, fix issues, and re-run migration (idempotent)

### Migration is Slow

**Cause**: Large number of documents or network latency  
**Solution**: Consider batching or running during off-peak hours

## Support

For issues or questions about migration:

1. Check structured logs for detailed error information
2. Verify Firebase security rules allow necessary operations
3. Test with a single user first before batch migration
4. Review the MigrationResult errors list for specific failures
