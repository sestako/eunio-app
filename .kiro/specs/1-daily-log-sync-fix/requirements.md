# Requirements Document

## Introduction

The daily log save/load functionality has critical cross-platform sync issues due to inconsistent Firebase collection paths and desynchronized logic between iOS, Android, and shared Kotlin layers. Currently, there are multiple services accessing different Firebase paths, causing data to be saved in one location but queried from another. This results in logs appearing to save successfully but not loading back, breaking the core functionality of the health tracking app.

The system has three different implementations:
1. **Shared FirestoreService** (used by LogRepository) - queries `users/{userId}/dailyLogs/{logId}`
2. **iOS SwiftDailyLogService** - saves to `users/{userId}/dailyLogs/` but reads from `daily_logs/{userId}/logs/`
3. **Android AndroidDailyLogService** - uses `daily_logs/{userId}/logs/{logId}` (not currently used by main flow)

This spec will standardize all implementations to use a single, consistent Firebase collection path (`users/{userId}/dailyLogs/{logId}`), ensure the shared Kotlin code is the single source of truth, implement proper offline-first architecture with conflict resolution, add comprehensive structured logging, and validate full cross-platform synchronization. The fix must address root causes without removing or simplifying functionality.

## Requirements

### Requirement 1: Standardize Firebase Collection Paths and Path Generation

**User Story:** As a developer, I want all platform implementations to use the same Firebase collection path with consistent path generation, so that data saved on one platform can be loaded on any platform.

#### Acceptance Criteria

1. WHEN any service saves a daily log THEN it SHALL use the path `users/{userId}/dailyLogs/{logId}`
2. WHEN any service queries a daily log THEN it SHALL use the path `users/{userId}/dailyLogs/{logId}`
3. WHEN generating a Firebase path THEN the system SHALL use a shared utility function `dailyLogDoc(userId: String, logId: String)`
4. WHEN iOS SwiftDailyLogService performs any operation THEN it SHALL use `users/{userId}/dailyLogs/` collection path
5. WHEN Android AndroidDailyLogService performs any operation THEN it SHALL use `users/{userId}/dailyLogs/` collection path
6. IF there are existing logs in `daily_logs/{userId}/logs/` THEN the system SHALL provide migration logic or script
7. WHEN migration runs THEN it SHALL be idempotent and log its progress

### Requirement 2: Standardize Document Schema and Data Format

**User Story:** As a developer, I want all daily log documents to follow a consistent schema with standardized data types, so that data can be reliably parsed across platforms.

#### Acceptance Criteria

1. WHEN saving a daily log document THEN it SHALL include fields: `logId`, `dateEpochDays`, `createdAt`, `updatedAt`, `v`
2. WHEN storing the date THEN it SHALL be stored as `dateEpochDays` (Long) representing UTC epoch days
3. WHEN storing timestamps THEN `createdAt` and `updatedAt` SHALL be stored as epoch seconds (Long)
4. WHEN saving optional fields THEN they SHALL only be included if non-null (periodFlow, mood, bbt, cervicalMucus, opkResult, notes, symptoms)
5. WHEN creating a new document THEN it SHALL include schema version `v = 1`
6. WHEN saving a log THEN the system SHALL use server timestamp for `createdAt` and `updatedAt`
7. WHEN generating logId THEN it SHALL use format `yyyy-MM-dd` in UTC timezone
8. WHEN storing or querying dates THEN all operations SHALL use UTC timezone for consistency

### Requirement 3: Ensure Shared Kotlin Code is Single Source of Truth

**User Story:** As a developer, I want the shared Kotlin code to be the single source of truth for all data operations, so that business logic is consistent across platforms and platform-specific code only handles UI concerns.

#### Acceptance Criteria

1. WHEN the app needs to save a daily log THEN it SHALL use the shared LogRepository
2. WHEN the app needs to load a daily log THEN it SHALL use the shared LogRepository
3. WHEN the shared LogRepository queries Firebase THEN it SHALL use FirestoreService
4. WHEN platform-specific services exist THEN they SHALL delegate all persistence operations to shared Kotlin code
5. WHEN iOS ViewModel loads data THEN it SHALL rely on the shared Kotlin ViewModel/UseCase, not implement separate loading logic
6. WHEN Android ViewModel loads data THEN it SHALL rely on the shared Kotlin ViewModel/UseCase
7. IF platform-specific services are needed THEN they SHALL only handle platform-specific UI operations, not data persistence

### Requirement 4: Implement Offline-First Architecture with Conflict Resolution

**User Story:** As a user, I want my logs to save locally first and sync to the cloud with proper conflict resolution, so that I can use the app offline and my data stays consistent across devices.

#### Acceptance Criteria

1. WHEN a user saves a log THEN it SHALL save to local cache immediately before attempting remote sync
2. WHEN local save succeeds THEN the system SHALL attempt to sync to Firebase in the background
3. IF Firebase sync fails THEN the local save SHALL still succeed and the operation SHALL be queued for retry
4. WHEN retry is needed THEN the system SHALL use exponential backoff strategy
5. WHEN loading data THEN the system SHALL show local cache immediately
6. WHEN loading data THEN the system SHALL fetch remote data and update local cache if remote data is newer
7. WHEN comparing versions THEN the system SHALL use `updatedAt` timestamp to determine which version is newer
8. WHEN a conflict is detected THEN the system SHALL implement last-write-wins strategy based on `updatedAt`
9. WHEN resolving a conflict THEN the system SHALL log both versions before overwriting
10. WHEN app restarts THEN previously saved logs SHALL be available from local cache

### Requirement 5: Validate Cross-Platform Synchronization

**User Story:** As a user with multiple devices, I want my logs to sync correctly between iOS and Android in real-time, so that I see the same data everywhere regardless of which device I use.

#### Acceptance Criteria

1. WHEN a log is saved on iOS THEN it SHALL be visible when querying from Android within sync interval
2. WHEN a log is saved on Android THEN it SHALL be visible when querying from iOS within sync interval
3. WHEN a log is updated on one platform THEN the update SHALL be reflected on the other platform
4. WHEN querying by date THEN both platforms SHALL return the same log for the same date
5. WHEN querying by date range THEN both platforms SHALL return identical data sets for the same userId
6. WHEN both platforms modify the same log THEN the version with the latest `updatedAt` SHALL win
7. WHEN cross-platform sync occurs THEN no data SHALL be lost or corrupted

### Requirement 6: Add Comprehensive Structured Logging

**User Story:** As a developer, I want detailed structured logs of all save/load operations with timing and path information, so that I can quickly diagnose sync issues and performance problems.

#### Acceptance Criteria

1. WHEN a save operation starts THEN it SHALL log `SAVE_START {userId, logId, dateEpochDays}`
2. WHEN writing to Firestore THEN it SHALL log `FIRESTORE_WRITE {path, status, latencyMs, error?}`
3. WHEN loading data THEN it SHALL log `LOAD_RESULT {path, found, docUpdatedAt, localUpdatedAt}`
4. WHEN syncing data THEN it SHALL log `SYNC_RESULT {direction, merged, winner, reason}`
5. WHEN using platform-specific logging THEN Android SHALL use `Log.d()` and iOS SHALL use `NSLog()`
6. WHEN logging operations THEN logs SHALL clearly show operation type, Firebase path, success/failure, and timing
7. WHEN an error occurs THEN it SHALL log the error message and relevant context

### Requirement 7: Implement Legacy Data Migration

**User Story:** As a system administrator, I want to migrate existing logs from the old Firebase path to the new standardized path, so that users don't lose their historical data.

#### Acceptance Criteria

1. IF legacy data exists in `daily_logs/{userId}/logs/{logId}` THEN the system SHALL provide migration logic
2. WHEN migration runs THEN it SHALL copy documents from old path to new path `users/{userId}/dailyLogs/{logId}`
3. WHEN migration runs THEN it SHALL be idempotent (safe to run multiple times)
4. WHEN migration runs THEN it SHALL log progress for each document migrated
5. WHEN migration completes THEN it SHALL report total documents migrated and any errors
6. WHEN migration encounters errors THEN it SHALL continue processing remaining documents
7. IF a document already exists at the new path THEN migration SHALL skip it or use conflict resolution

### Requirement 8: Update Firebase Security Rules and Indexes

**User Story:** As a system administrator, I want Firebase security rules and indexes configured correctly for the new collection path, so that users can access their data securely and queries perform efficiently.

#### Acceptance Criteria

1. WHEN Firebase rules are configured THEN they SHALL allow read/write to `users/{userId}/dailyLogs/{logId}` where `request.auth.uid == userId`
2. WHEN a user accesses their own data THEN they SHALL have read and write permissions
3. WHEN a user tries to access another user's data THEN they SHALL be denied
4. IF legacy paths exist THEN rules SHALL support both old and new paths during migration period (read-only for legacy)
5. WHEN migration is complete THEN legacy path rules SHALL be removed
6. WHEN querying by dateEpochDays THEN Firestore SHALL have a composite index on `dateEpochDays`
7. WHEN querying date ranges THEN Firestore SHALL have indexes for range queries on `dateEpochDays`

### Requirement 9: Add Automated and Manual Tests

**User Story:** As a developer, I want comprehensive tests that verify cross-platform sync, offline behavior, and conflict resolution, so that I can be confident the system works correctly.

#### Acceptance Criteria

1. WHEN tests run THEN they SHALL verify iOS → Android sync
2. WHEN tests run THEN they SHALL verify Android → iOS sync
3. WHEN tests run THEN they SHALL verify offline save and later sync
4. WHEN tests run THEN they SHALL verify conflict resolution by `updatedAt`
5. WHEN tests run THEN they SHALL verify local load fallback when offline
6. WHEN tests run THEN they SHALL verify data persistence after app restart
7. WHEN tests run THEN they SHALL verify date range queries return consistent results across platforms
