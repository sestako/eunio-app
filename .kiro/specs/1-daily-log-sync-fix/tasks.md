# Implementation Plan

- [x] 1. Create shared Firebase path utility and standardize collection paths
  - Create `FirestorePaths.kt` with `dailyLogDoc()` and `dailyLogsCollection()` functions
  - Update all Firebase path references to use the utility functions
  - Ensure all paths use `users/{userId}/dailyLogs/{logId}` format
  - _Requirements: 1.1, 1.2, 1.3_

- [x] 2. Implement standardized document schema and DTO layer
  - [x] 2.1 Create `DailyLogDto` with all required fields (logId, dateEpochDays, createdAt, updatedAt, v)
    - Implement `fromDomain()` to convert DailyLog to DTO
    - Implement `toDomain()` to convert DTO to DailyLog
    - Store dates as UTC epoch days (Long)
    - Store timestamps as epoch seconds (Long)
    - Include schema version field `v = 1`
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8_

  - [x] 2.2 Update FirestoreServiceImpl to use DailyLogDto
    - Modify `saveDailyLog()` to convert domain model to DTO before saving
    - Modify `getDailyLogByDate()` to convert DTO to domain model after loading
    - Modify `getLogsInRange()` to use DTO conversion
    - Update all Firebase queries to use `dateEpochDays` field
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

- [x] 3. Enhance LogRepositoryImpl with offline-first architecture and structured logging
  - [x] 3.1 Add structured logging utility function
    - Create `logStructured()` function that formats logs consistently
    - Implement platform-specific logging (Log.d for Android, NSLog for iOS)
    - Add log formats: SAVE_START, FIRESTORE_WRITE, LOAD_RESULT, SYNC_RESULT
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7_

  - [x] 3.2 Implement offline-first save operation
    - Update `saveDailyLog()` to save locally first
    - Mark saved logs as pending sync
    - Attempt Firebase sync in background
    - Mark as synced on success, keep pending on failure
    - Add timing metrics and structured logging
    - _Requirements: 4.1, 4.2, 4.3, 6.1, 6.2_

  - [x] 3.3 Implement conflict resolution in load operation
    - Update `getDailyLog()` to query Firebase first
    - Compare `updatedAt` timestamps between remote and local
    - Implement last-write-wins strategy
    - Log both versions before overwriting
    - Update local cache with winner
    - Fall back to local cache if Firebase unavailable
    - _Requirements: 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 4.10, 5.6_

  - [x] 3.4 Implement sync retry mechanism with exponential backoff
    - Create `syncPendingChanges()` function
    - Query all logs marked as pending sync
    - Retry failed syncs with exponential backoff
    - Mark as synced on success
    - _Requirements: 4.3, 4.4_

- [x] 4. Update iOS SwiftDailyLogService to use correct Firebase paths
  - [x] 4.1 Fix all Firebase collection paths in SwiftDailyLogService
    - Update `createLog()` to use `users/{userId}/dailyLogs/`
    - Update `getLog()` to use `users/{userId}/dailyLogs/`
    - Update `updateLog()` to use `users/{userId}/dailyLogs/`
    - Update `deleteLog()` to use `users/{userId}/dailyLogs/`
    - Update `getLogsByDateRange()` to use `users/{userId}/dailyLogs/`
    - _Requirements: 1.4, 1.5_

  - [x] 4.2 Ensure iOS uses shared Kotlin code for data operations
    - Verify iOS ViewModel delegates to shared Kotlin UseCase
    - Remove duplicate loading logic from iOS ViewModel if present
    - Ensure SwiftDailyLogService is only used for platform-specific UI needs
    - _Requirements: 3.1, 3.2, 3.3, 3.5, 3.7_

- [x] 5. Update Android AndroidDailyLogService to use correct Firebase paths
  - [x] 5.1 Fix all Firebase collection paths in AndroidDailyLogService
    - Update all operations to use `users/{userId}/dailyLogs/`
    - Use `FirestorePaths` utility for path generation
    - _Requirements: 1.5_

  - [x] 5.2 Ensure Android uses shared Kotlin code for data operations
    - Verify Android ViewModel delegates to shared Kotlin UseCase
    - Ensure AndroidDailyLogService is only used for platform-specific UI needs
    - _Requirements: 3.1, 3.2, 3.3, 3.6, 3.7_

- [x] 6. Add local database sync metadata fields
  - [x] 6.1 Update DailyLogEntity schema
    - Add `isSynced` boolean field
    - Add `pendingSync` boolean field
    - Add `lastSyncAttempt` timestamp field
    - Add `syncRetryCount` integer field
    - _Requirements: 4.3, 4.4_

  - [x] 6.2 Update DailyLogDao with sync operations
    - Add `markAsSynced(logId: String)` function
    - Add `markAsPendingSync(logId: String)` function
    - Add `getPendingSync()` function to query logs needing sync
    - Add `incrementSyncRetryCount(logId: String)` function
    - _Requirements: 4.3, 4.4_

- [x] 7. Implement legacy data migration
  - [x] 7.1 Create DailyLogMigration utility
    - Create `migrateLegacyLogs(userId: String)` function
    - Query legacy path `daily_logs/{userId}/logs/`
    - Copy documents to new path `users/{userId}/dailyLogs/`
    - Make migration idempotent (skip existing documents)
    - Log migration progress for each document
    - Return MigrationResult with counts and errors
    - _Requirements: 1.6, 1.7, 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7_

  - [ ]* 7.2 Create migration script or admin function
    - Create command-line script or admin UI to trigger migration
    - Add progress reporting
    - Add error handling and rollback capability
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [x] 8. Update Firebase security rules
  - [x] 8.1 Add security rules for new collection path
    - Allow read/write to `users/{userId}/dailyLogs/{logId}` where `request.auth.uid == userId`
    - Deny access to other users' data
    - _Requirements: 8.1, 8.2, 8.3_

  - [x] 8.2 Add legacy path rules for migration period
    - Allow read-only access to `daily_logs/{userId}/logs/{logId}`
    - Disable writes to legacy path
    - _Requirements: 8.4_

  - [x] 8.3 Create Firestore indexes
    - Create index on `dateEpochDays` field (ascending and descending)
    - Create composite index on `dateEpochDays` + `updatedAt`
    - _Requirements: 8.6, 8.7_

- [ ] 9. Implement cross-platform sync validation
  - [x] 9.1 Create iOS → Android sync test
    - Save a log on iOS
    - Query the same log from Android
    - Verify data matches exactly
    - _Requirements: 5.1, 5.4, 5.5_

  - [ ] 9.2 Create Android → iOS sync test
    - Save a log on Android
    - Query the same log from iOS
    - Verify data matches exactly
    - _Requirements: 5.2, 5.4, 5.5_

  - [ ] 9.3 Create conflict resolution test
    - Update same log on both platforms with different data
    - Verify last-write-wins based on `updatedAt`
    - Verify no data loss
    - _Requirements: 5.3, 5.6, 5.7_

  - [ ] 9.4 Create offline mode test
    - Save log while offline
    - Verify local save succeeds
    - Go online and verify sync occurs
    - _Requirements: 4.1, 4.2, 4.3, 9.3_

  - [ ] 9.5 Create app restart persistence test
    - Save log
    - Restart app
    - Verify log is still available from local cache
    - _Requirements: 4.10, 9.6_

- [ ] 10. Add comprehensive logging and monitoring
  - [ ] 10.1 Verify structured logging is working
    - Check logs show SAVE_START with userId, logId, dateEpochDays
    - Check logs show FIRESTORE_WRITE with path, status, latencyMs
    - Check logs show LOAD_RESULT with path, found, timestamps
    - Check logs show SYNC_RESULT with direction, winner, reason
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.6_

  - [ ] 10.2 Add timing metrics
    - Measure and log save operation latency
    - Measure and log Firebase sync latency
    - Measure and log load operation latency
    - _Requirements: 6.2, 6.6_

- [ ] 11. Documentation and cleanup
  - [ ] 11.1 Update code documentation
    - Document FirestorePaths utility usage
    - Document DTO conversion process
    - Document conflict resolution strategy
    - Document retry mechanism
    - _Requirements: All_

  - [ ] 11.2 Create migration guide
    - Document how to run migration for existing users
    - Document rollback procedure if needed
    - Document verification steps
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

  - [ ] 11.3 Remove legacy code
    - Remove old path references after migration complete
    - Remove legacy Firebase rules after migration complete
    - Clean up unused platform-specific services if fully delegated
    - _Requirements: 8.5_
