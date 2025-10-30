# Requirements Document

## Introduction

The Firebase synchronization between iOS and Android platforms requires comprehensive debugging infrastructure and systematic verification to ensure data consistency, proper authentication, correct configuration, and reliable network operations. Currently, when sync issues occur, there is insufficient logging and diagnostic tooling to quickly identify root causes such as misconfigured Firebase projects, permission errors, missing indexes, or network failures.

This spec establishes a systematic debugging and verification framework that includes detailed Firebase logging, configuration validation, authentication verification, Firestore rules checking, minimal integrity tests, and network diagnostics. The goal is to provide developers with actionable diagnostic information to quickly identify and resolve sync issues across both platforms.

## Glossary

- **Firebase SDK**: The Firebase client library for iOS (Swift/Objective-C) or Android (Kotlin/Java)
- **Firestore**: Firebase's NoSQL cloud database service
- **Auth Token**: Authentication token provided by Firebase Authentication
- **Security Rules**: Server-side rules that control access to Firestore data
- **Composite Index**: Firestore index required for complex queries involving multiple fields
- **Debug Logger**: Firebase's built-in logging system that outputs detailed operation information
- **GoogleService-Info.plist**: iOS Firebase configuration file
- **google-services.json**: Android Firebase configuration file
- **Project ID**: Unique identifier for a Firebase project
- **Application ID**: Unique identifier for an iOS/Android app (bundle ID / package name)

## Requirements

### Requirement 1: Enable Detailed Firebase Logging

**User Story:** As a developer, I want detailed Firebase debug logs on both platforms, so that I can see exactly what operations are being performed and identify failures.

#### Acceptance Criteria

1. WHEN the iOS app launches THEN the system SHALL call `FirebaseConfiguration.shared.setLoggerLevel(.debug)` immediately after `FirebaseApp.configure()`
2. WHEN the Android app launches with flag `-PFirebaseDebugEnabled` THEN the system SHALL enable Firebase debug logging
3. WHEN Firestore operations execute on iOS THEN the system SHALL enable Firestore logging via `setLoggingEnabled(true)`
4. WHEN Firestore operations execute on Android THEN the system SHALL enable Firestore logging via `FirebaseFirestore.setLoggingEnabled(true)`
5. WHEN debug logging is enabled THEN all Firebase operations SHALL output detailed logs including operation type, collection paths, document IDs, and timestamps
6. WHEN a Firebase operation fails THEN the debug log SHALL include error codes, error messages, and stack traces
7. WHEN collecting diagnostic logs THEN the system SHALL capture logs from app launch through first sync attempt

### Requirement 2: Validate Firebase Initialization and Configuration

**User Story:** As a developer, I want to verify that Firebase is correctly initialized with valid configuration files, so that I can rule out configuration issues as the cause of sync failures.

#### Acceptance Criteria

1. WHEN the iOS app initializes THEN the system SHALL call `FirebaseApp.configure()` exactly once during app launch
2. WHEN the Android app initializes THEN the system SHALL call `FirebaseApp.initializeApp()` exactly once during app launch
3. WHEN the iOS app builds THEN the system SHALL verify `GoogleService-Info.plist` exists in the correct target (Debug/Release)
4. WHEN the iOS app builds THEN the system SHALL verify `GoogleService-Info.plist` is included in the app bundle
5. WHEN the Android app builds THEN the system SHALL verify `google-services.json` exists in the correct `app/` directory
6. WHEN the Android app builds THEN the system SHALL verify `google-services.json` matches the current `applicationId`
7. WHEN comparing iOS and Android configurations THEN `projectId`, `apiKey`, and `gcmSenderId` SHALL match between platforms for the same environment
8. IF configuration files point to different Firebase projects THEN the system SHALL log a warning during initialization

### Requirement 3: Verify Authentication State

**User Story:** As a developer, I want to verify that users are properly authenticated before attempting sync operations, so that I can identify authentication issues as the root cause of permission errors.

#### Acceptance Criteria

1. WHEN the app starts THEN the system SHALL verify the current authentication mode (Anonymous/Email/OAuth)
2. WHEN the app starts THEN the system SHALL verify `currentUser` is not null before attempting any Firestore operations
3. WHEN the app starts THEN the system SHALL verify the current user has a valid authentication token
4. WHEN a Firestore operation fails with `PERMISSION_DENIED` THEN the system SHALL log the current user ID and authentication state
5. WHEN a Firestore operation fails with `Missing or insufficient permissions` THEN the system SHALL log the attempted operation path and user ID
6. WHEN authentication state changes THEN the system SHALL log the new authentication state and user ID
7. WHEN the user is not authenticated THEN the system SHALL prevent Firestore operations and display an authentication prompt

### Requirement 4: Verify Firestore Security Rules and Indexes

**User Story:** As a developer, I want to verify that Firestore security rules allow the required operations and that necessary indexes exist, so that I can identify permission and query issues.

#### Acceptance Criteria

1. WHEN reviewing Firestore configuration THEN the system SHALL verify security rules allow write access for the authenticated user to `users/{userId}/dailyLogs`
2. WHEN reviewing Firestore configuration THEN the system SHALL verify security rules allow read access for the authenticated user to `users/{userId}/dailyLogs`
3. WHEN a query requires a composite index THEN the system SHALL check if the index exists in Firestore
4. IF a required index is missing THEN the Firestore error SHALL include a URL to create the index
5. WHEN a permission error occurs THEN the system SHALL log the attempted operation, collection path, and current user ID
6. WHEN security rules are updated THEN the system SHALL verify the changes allow the expected operations
7. WHEN testing security rules THEN the system SHALL use the Firebase Console Rules Playground to simulate operations

### Requirement 5: Implement Minimal Integrity Test for Write/Read Operations

**User Story:** As a developer, I want a minimal test that writes and reads a document to verify basic Firestore connectivity, so that I can quickly confirm the sync pipeline is functional.

#### Acceptance Criteria

1. WHEN the debug flag `DEBUG_SYNC_TEST` is enabled THEN the system SHALL execute a minimal integrity test on app launch
2. WHEN the integrity test runs THEN the system SHALL write a document to collection `sync_test` with ID `<timestamp>-<device>`
3. WHEN writing the test document THEN the system SHALL include fields `source` (ios|android), `ts` (server timestamp), and `deviceId`
4. WHEN the write operation completes THEN the system SHALL immediately read the document back from Firestore
5. WHEN the read operation completes THEN the system SHALL verify the document contains the expected data
6. WHEN the integrity test completes THEN the system SHALL log the result (success/failure) and operation latency in milliseconds
7. WHEN the integrity test fails THEN the system SHALL log the error message and relevant context for debugging

### Requirement 6: Verify KMP Shared Layer Execution

**User Story:** As a developer, I want to verify that Kotlin Multiplatform shared code executes correctly on both platforms without blocking the main thread, so that I can identify platform-specific issues.

#### Acceptance Criteria

1. WHEN KMP shared code calls Firestore THEN the system SHALL execute the operation on a background dispatcher (not Main)
2. WHEN KMP shared code calls Firestore THEN the system SHALL not block the main thread
3. WHEN an exception occurs in KMP shared code THEN the system SHALL catch and log the exception with full stack trace
4. WHEN an exception occurs in KMP shared code THEN the system SHALL not silently swallow the exception
5. WHEN reviewing recent changes THEN the system SHALL examine git diffs for the `shared` module related to sync use-cases
6. WHEN KMP code executes on iOS THEN the system SHALL log platform-specific context (iOS version, device model)
7. WHEN KMP code executes on Android THEN the system SHALL log platform-specific context (Android version, device model)

### Requirement 7: Verify Network and Persistence Configuration

**User Story:** As a developer, I want to verify that network connectivity is working and that Firestore persistence settings are not blocking sync operations, so that I can identify network-related issues.

#### Acceptance Criteria

1. WHEN the app starts THEN the system SHALL verify Firestore is not in forced offline mode
2. WHEN the app starts THEN the system SHALL verify Firestore persistence settings are not blocking write operations
3. WHEN testing on iOS THEN the system SHALL verify App Transport Security (ATS) settings allow connections to Firebase endpoints
4. WHEN a network error occurs THEN the system SHALL log the error type (timeout, connection refused, DNS failure)
5. WHEN retry logic executes THEN the system SHALL log each retry attempt with backoff duration
6. WHEN retry logic executes THEN the system SHALL verify exponential backoff is implemented correctly
7. WHEN network connectivity changes THEN the system SHALL log the new connectivity state and attempt to flush pending operations

### Requirement 8: Capture and Export Diagnostic Logs

**User Story:** As a developer, I want to capture all diagnostic logs from app launch through sync attempts and export them to a file, so that I can analyze issues offline or share logs with the team.

#### Acceptance Criteria

1. WHEN diagnostic mode is enabled THEN the system SHALL capture all logs from app launch through first sync attempt
2. WHEN capturing logs on iOS THEN the system SHALL use `os_log` or `NSLog` to ensure logs are captured by system logging
3. WHEN capturing logs on Android THEN the system SHALL use `adb logcat` with Firebase tag filter
4. WHEN diagnostic capture completes THEN the system SHALL export logs to a text file with timestamp in filename
5. WHEN exporting logs THEN the system SHALL include device information (platform, OS version, app version)
6. WHEN exporting logs THEN the system SHALL include Firebase configuration summary (project ID, app ID)
7. WHEN exporting logs THEN the system SHALL sanitize any sensitive information (auth tokens, user IDs) before export

### Requirement 9: Create Diagnostic Report

**User Story:** As a developer, I want a structured diagnostic report that summarizes all verification checks, so that I can quickly identify which components are working and which are failing.

#### Acceptance Criteria

1. WHEN diagnostic mode runs THEN the system SHALL generate a structured report with sections for each verification area
2. WHEN the report is generated THEN it SHALL include Firebase initialization status (success/failure)
3. WHEN the report is generated THEN it SHALL include authentication status (authenticated/not authenticated, user ID)
4. WHEN the report is generated THEN it SHALL include configuration validation results (matching project IDs, valid config files)
5. WHEN the report is generated THEN it SHALL include integrity test results (write success, read success, latency)
6. WHEN the report is generated THEN it SHALL include network status (online/offline, connectivity type)
7. WHEN the report is generated THEN it SHALL include a summary of any errors encountered with recommended next steps
8. WHEN the report is complete THEN the system SHALL save it to a file and display the file path to the developer
