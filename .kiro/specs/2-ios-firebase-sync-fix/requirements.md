# Requirements Document

## Introduction

The iOS app currently saves daily logs locally but does not sync them to Firebase, while the Android app successfully syncs to Firebase. This creates a critical data inconsistency where iOS users' data is not backed up or accessible across devices. The root cause is that the iOS implementation of `FirestoreServiceImpl` uses mock in-memory storage instead of actual Firebase SDK calls, while Android uses the real Firebase SDK.

## Requirements

### Requirement 1: iOS Firebase Integration

**User Story:** As an iOS user, I want my daily logs to be saved to Firebase, so that my data is backed up and accessible across devices.

#### Acceptance Criteria

1. WHEN an iOS user saves a daily log THEN the system SHALL persist the log to Firebase Firestore using the Firebase iOS SDK
2. WHEN an iOS user saves a daily log THEN the system SHALL use the same Firestore path structure as Android (`users/{userId}/dailyLogs/{logId}`)
3. WHEN an iOS user saves a daily log THEN the system SHALL store data in the same format as Android (epoch days for dates, seconds for timestamps)
4. IF the Firebase save operation fails THEN the system SHALL display an error message to the user
5. IF the Firebase save operation succeeds THEN the system SHALL display a success message to the user
6. WHEN an iOS user loads a daily log THEN the system SHALL read the log from Firebase using the same path and field mapping as Android

### Requirement 2: Cross-Platform Data Consistency

**User Story:** As a user with both iOS and Android devices, I want my data to sync seamlessly between platforms, so that I can access my health logs from any device.

#### Acceptance Criteria

1. WHEN a daily log is saved on iOS THEN it SHALL be immediately visible in the Firebase console
2. WHEN a daily log is saved on iOS THEN it SHALL be readable by the Android app
3. WHEN a daily log is saved on Android THEN it SHALL be readable by the iOS app
4. WHEN comparing iOS and Android saved logs THEN they SHALL use identical field names and data types
5. WHEN comparing iOS and Android saved logs THEN they SHALL use identical Firestore collection paths
6. WHEN cross-platform sync occurs THEN the system SHALL log document IDs, timestamps, and sync status for debugging purposes

### Requirement 3: Maintain Existing iOS Architecture

**User Story:** As a developer, I want the Firebase integration to work within the existing Kotlin Multiplatform architecture, so that we maintain code consistency and don't introduce technical debt.

#### Acceptance Criteria

1. WHEN implementing iOS Firebase sync THEN the system SHALL use the existing `FirestoreService` interface from shared Kotlin code
2. WHEN implementing iOS Firebase sync THEN the system SHALL replace the mock implementation in `FirestoreServiceImpl.ios.kt` with actual Firebase calls
3. WHEN implementing iOS Firebase sync THEN the system SHALL use Kotlin/Native interop to call the Firebase iOS SDK
4. WHEN implementing iOS Firebase sync THEN the system MAY use a thin Swift bridging layer to expose Firebase functions to shared Kotlin code via Kotlin/Native interop
5. WHEN implementing iOS Firebase sync THEN the system SHALL maintain the existing error handling patterns using `Result` and `ErrorHandler`
6. WHEN implementing iOS Firebase sync THEN the system SHALL NOT break existing iOS functionality

### Requirement 4: Error Handling and User Feedback

**User Story:** As an iOS user, I want clear feedback when my data saves or fails to save, so that I know whether my health data is safely stored.

#### Acceptance Criteria

1. WHEN a Firebase save operation is in progress THEN the system SHALL display a loading indicator
2. WHEN a Firebase save operation succeeds THEN the system SHALL display "Log saved successfully" for 3 seconds
3. WHEN a Firebase save operation fails due to network issues THEN the system SHALL display "Failed to save: Check your internet connection"
4. WHEN a Firebase save operation fails due to authentication issues THEN the system SHALL display "Failed to save: Please sign in again"
5. WHEN a Firebase save operation fails for any other reason THEN the system SHALL display a descriptive error message
6. WHEN any Firebase error occurs THEN the system SHALL log the error code and message to the developer console for debugging

### Requirement 5: Backward Compatibility

**User Story:** As an iOS user who has been using the app, I want my existing local data to continue working, so that I don't lose any health information.

#### Acceptance Criteria

1. WHEN the Firebase integration is deployed THEN existing local SQLite data SHALL remain accessible
2. WHEN the Firebase integration is deployed THEN the system SHALL continue to save data to local SQLite for offline access
3. WHEN the Firebase integration is deployed THEN the system SHALL sync local data to Firebase when online
4. IF a user has local-only data from before the fix THEN the system SHALL upload it to Firebase on next save
5. WHEN offline THEN the system SHALL save to local SQLite and sync to Firebase when connection is restored
6. WHEN conflicts exist between local and remote data THEN the system SHALL use updatedAt timestamps to resolve conflicts using a last-write-wins strategy


### Requirement 6: Validation and Testing

**User Story:** As a developer, I want to verify Firebase synchronization on iOS and across platforms, so that I can confirm the fix works as intended.

#### Acceptance Criteria

1. WHEN running the app on iOS THEN Firebase logs SHALL show successful writes to `users/{userId}/dailyLogs/{logId}`
2. WHEN opening Firebase Console THEN newly saved logs SHALL appear under the correct path
3. WHEN Android loads logs THEN data SHALL match iOS entries exactly
4. WHEN iOS loads logs after reinstall THEN previously synced data SHALL be restored
5. WHEN testing offline THEN local saves SHALL sync automatically once connection restores
