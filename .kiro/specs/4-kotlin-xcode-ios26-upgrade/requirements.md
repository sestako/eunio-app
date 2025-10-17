# Requirements Document

## Introduction

This document outlines the requirements for upgrading the Eunio Health App to use Kotlin 2.2.20, Xcode 26, and iOS 26 SDK. The current project uses Kotlin 1.9.21 with iOS 17 SDK, which is outdated and incompatible with the latest Xcode versions that macOS requires. This upgrade will modernize the development toolchain, enable access to the latest iOS features, and ensure compatibility with current Apple development requirements.

**Note:** Starting with version 26, Apple unified the versioning for Xcode and iOS SDK, making them both version 26. This simplifies toolchain management and ensures consistent compatibility between development tools and target platforms.

**macOS Versioning Update:** Starting with version 26, Apple unified OS versioning across iOS, macOS, iPadOS, and Xcode. macOS 26 (Tahoe) replaces Sequoia 15, aligning macOS version numbers with iOS and Xcode for the first time.

## Glossary

- **Kotlin Multiplatform (KMP)**: Framework for sharing code between Android and iOS
- **Xcode**: Apple's integrated development environment for iOS/macOS development
- **iOS SDK**: Software Development Kit for building iOS applications
- **Gradle**: Build automation tool used for Kotlin projects
- **CocoaPods**: Dependency manager for iOS projects
- **Swift Package Manager (SPM)**: Apple's dependency manager for Swift and Objective-C projects
- **Firebase**: Backend services platform used for authentication and data storage
- **SQLDelight**: Database library for Kotlin Multiplatform
- **Deployment Target**: Minimum iOS version required to run the app
- **Build Target**: iOS SDK version used to compile the app
- **Bitcode**: Apple's intermediate representation of compiled code (deprecated in iOS 26)
- **KSP**: Kotlin Symbol Processing - compiler plugin for code generation
- **Version Catalog**: Gradle feature for centralized dependency version management
- **cinterop**: Kotlin/Native interoperability with C libraries

## Requirements

### Requirement 1: Kotlin Version Upgrade

**User Story:** As a developer, I want to upgrade Kotlin to version 2.2.20, so that I can use the latest language features and ensure compatibility with iOS 26 SDK.

#### Acceptance Criteria

1. WHEN updating gradle/libs.versions.toml THEN the Kotlin version SHALL be set to "2.2.20"
2. WHEN building the project THEN all Kotlin compilation SHALL succeed without errors
3. WHEN running the Android app THEN it SHALL function identically to the previous version
4. WHEN running the iOS app THEN it SHALL function identically to the previous version
5. WHEN using Kotlin/Native features THEN they SHALL be compatible with iOS 26 SDK
6. WHEN building for iOS THEN Kotlin/Native cinterop SHALL work with Xcode 26
7. WHEN updating Kotlin compiler plugins THEN all plugins (Compose, Serialization, KSP) SHALL be updated to versions compatible with Kotlin 2.2.20
8. IF compilation fails THEN error messages SHALL clearly indicate the source of incompatibility

### Requirement 2: Kotlin Dependencies Update

**User Story:** As a developer, I want to update Kotlin-related dependencies to compatible versions, so that the project builds successfully with Kotlin 2.2.20.

#### Acceptance Criteria

1. WHEN updating kotlinx-coroutines THEN the version SHALL be compatible with Kotlin 2.2.20
2. WHEN updating kotlinx-serialization THEN the version SHALL be compatible with Kotlin 2.2.20
3. WHEN updating kotlinx-datetime THEN the version SHALL be compatible with Kotlin 2.2.20
4. WHEN updating Compose Multiplatform THEN the version SHALL be compatible with Kotlin 2.2.20
5. WHEN updating Ktor client THEN the version SHALL be compatible with Kotlin 2.2.20
6. WHEN updating Koin THEN the version SHALL be compatible with Kotlin 2.2.20
7. WHEN updating SQLDelight THEN the version SHALL be compatible with Kotlin 2.2.20
8. WHEN building the project THEN all dependencies SHALL resolve without conflicts
9. WHEN using Gradle version catalogs THEN all dependency versions SHALL be aligned and consistent
10. WHEN enabling dependency verification THEN dependencyVerificationMode SHALL be set to "strict" to prevent version mismatches

### Requirement 3: Xcode and iOS SDK Upgrade

**User Story:** As a developer, I want to upgrade to Xcode 26 with iOS 26 SDK, so that I can build the app with the latest Apple tools and access iOS 26 features.

#### Acceptance Criteria

1. WHEN installing Xcode 26 THEN it SHALL be installed to /Applications/Xcode.app
2. WHEN running xcode-select THEN it SHALL point to Xcode 26
3. WHEN running xcodebuild -version THEN it SHALL display "Xcode 26.x"
4. WHEN opening the iOS project THEN Xcode 26 SHALL open without errors
5. WHEN building the iOS app THEN it SHALL compile successfully with iOS 26 SDK
6. WHEN running on iOS simulator THEN the app SHALL launch and function correctly
7. WHEN running on physical iOS device THEN the app SHALL launch and function correctly
8. WHEN building iOS framework THEN it SHALL support both arm64 and x86_64 simulator architectures
9. WHEN configuring iOS framework THEN Bitcode SHALL be disabled (binaries.framework.bitcode = Framework.BitcodeEmbeddingMode.DISABLE) as iOS 26 deprecates Bitcode

### Requirement 4: iOS Deployment Target Configuration

**User Story:** As a developer, I want to maintain iOS 15.0 as the minimum deployment target, so that users on older iOS versions can still use the app.

#### Acceptance Criteria

1. WHEN reviewing Xcode project settings THEN iOS Deployment Target SHALL be set to 15.0
2. WHEN reviewing build.gradle.kts THEN iosDeploymentTarget SHALL be set to "15.0"
3. WHEN building the app THEN it SHALL compile against iOS 26 SDK
4. WHEN running on iOS 15 device THEN the app SHALL function correctly
5. WHEN using iOS 26 features THEN they SHALL have availability checks for iOS 15 compatibility
6. WHEN the app runs on iOS 15-26 THEN all core features SHALL work correctly

### Requirement 5: Build Configuration Updates

**User Story:** As a developer, I want to update build configurations for Kotlin 2.2.20 and iOS 26, so that the build process works correctly with the new toolchain.

#### Acceptance Criteria

1. WHEN updating build.gradle.kts THEN Kotlin plugin version SHALL be 2.2.20
2. WHEN updating build.gradle.kts THEN Android Gradle Plugin SHALL be compatible with Kotlin 2.2.20
3. WHEN updating gradle.properties THEN Kotlin compiler options SHALL be configured correctly
4. WHEN building shared module THEN Kotlin/Native compilation SHALL succeed
5. WHEN building iOS framework THEN it SHALL be compatible with Xcode 26
6. WHEN running Gradle tasks THEN they SHALL complete without warnings about version incompatibilities
7. WHEN checking Gradle version THEN it SHALL be 8.10 or higher as required for Kotlin 2.2.20
8. WHEN updating Compose Multiplatform THEN the compiler plugin SHALL be upgraded to the version compatible with Kotlin 2.2.20

### Requirement 6: Firebase Compatibility Verification

**User Story:** As a developer, I want to verify Firebase works with the upgraded toolchain, so that authentication and data sync continue to function.

#### Acceptance Criteria

1. WHEN building with Kotlin 2.2.20 THEN Firebase Android SDK SHALL work without issues
2. WHEN building with iOS 26 SDK THEN Firebase iOS SDK SHALL work without issues
3. WHEN authenticating users THEN Firebase Auth SHALL function correctly on both platforms
4. WHEN saving data THEN Firestore SHALL sync correctly on both platforms
5. WHEN loading data THEN Firestore SHALL retrieve data correctly on both platforms
6. WHEN running offline THEN local persistence SHALL work correctly on both platforms
7. WHEN integrating Firebase on iOS THEN it SHALL use the latest FirebaseCore and FirebaseFirestore APIs compatible with iOS 26 SDK
8. WHEN choosing iOS dependency manager THEN Swift Package Manager (SPM) SHALL be preferred over CocoaPods for Firebase integration unless a dependency explicitly requires Pods

### Requirement 7: Testing and Validation

**User Story:** As a developer, I want comprehensive testing after the upgrade, so that I can verify no functionality has been broken.

#### Acceptance Criteria

1. WHEN running unit tests THEN all tests SHALL pass on both Android and iOS
2. WHEN running integration tests THEN all tests SHALL pass on both platforms
3. WHEN testing daily logging THEN save and load operations SHALL work correctly
4. WHEN testing cross-platform sync THEN data SHALL sync between Android and iOS
5. WHEN testing authentication THEN sign-in and sign-up SHALL work correctly
6. WHEN testing offline mode THEN local storage and sync SHALL work correctly
7. WHEN testing UI THEN all screens SHALL render and function correctly

### Requirement 8: CocoaPods and iOS Dependencies

**User Story:** As a developer, I want to update iOS dependencies to be compatible with Xcode 26 and iOS 26, so that the iOS app builds successfully.

#### Acceptance Criteria

1. WHEN running pod install THEN all CocoaPods dependencies SHALL resolve successfully
2. WHEN building iOS app THEN Firebase iOS SDK SHALL be compatible with iOS 26
3. WHEN building iOS app THEN all Swift Package Manager dependencies SHALL resolve
4. WHEN updating Podfile THEN minimum iOS version SHALL be set to 15.0
5. IF any pods are incompatible THEN they SHALL be updated to compatible versions

### Requirement 9: Documentation and Migration Guide

**User Story:** As a developer, I want clear documentation of the upgrade process, so that future upgrades are easier and team members understand the changes.

#### Acceptance Criteria

1. WHEN reviewing documentation THEN it SHALL list all version changes made
2. WHEN reviewing documentation THEN it SHALL explain why each change was necessary
3. WHEN reviewing documentation THEN it SHALL include troubleshooting steps for common issues
4. WHEN reviewing documentation THEN it SHALL document any API changes in Kotlin 2.2.20
5. WHEN reviewing documentation THEN it SHALL document any breaking changes encountered
6. WHEN reviewing documentation THEN it SHALL include verification steps for the upgrade

### Requirement 10: Rollback Plan

**User Story:** As a developer, I want a clear rollback plan, so that I can revert the upgrade if critical issues are discovered.

#### Acceptance Criteria

1. WHEN creating rollback plan THEN it SHALL document how to revert Kotlin version
2. WHEN creating rollback plan THEN it SHALL document how to revert Xcode version
3. WHEN creating rollback plan THEN it SHALL document how to revert dependency versions
4. WHEN creating rollback plan THEN it SHALL include git branch strategy for safe upgrade
5. WHEN critical issues are found THEN the rollback process SHALL restore working state within 1 hour

### Requirement 11: CI/CD Pipeline Updates

**User Story:** As a developer, I want to update CI/CD pipelines for the new toolchain, so that automated builds and tests work correctly.

#### Acceptance Criteria

1. WHEN updating GitHub Actions THEN it SHALL use Xcode 26 for iOS builds
2. WHEN updating GitHub Actions THEN it SHALL use Kotlin 2.2.20 for compilation
3. WHEN running CI builds THEN they SHALL succeed on both Android and iOS
4. WHEN running CI tests THEN they SHALL pass on both platforms
5. WHEN CI fails THEN error messages SHALL clearly indicate the cause
6. WHEN configuring CI environment THEN it SHALL use macOS 26 (Tahoe) or newer with Xcode 26 preinstalled
7. WHEN optimizing CI performance THEN caching SHALL be configured for both Gradle and CocoaPods to reduce build times

### Requirement 12: Performance Validation

**User Story:** As a developer, I want to verify app performance after the upgrade, so that users don't experience degradation.

#### Acceptance Criteria

1. WHEN measuring app startup time THEN it SHALL be equal to or better than before upgrade
2. WHEN measuring build time THEN it SHALL be equal to or better than before upgrade
3. WHEN measuring memory usage THEN it SHALL be equal to or better than before upgrade
4. WHEN measuring Firebase sync time THEN it SHALL be equal to or better than before upgrade
5. IF performance degrades THEN the cause SHALL be identified and addressed

### Requirement 13: Toolchain Verification Script

**User Story:** As a developer, I want an automated verification script that validates toolchain versions before build, so that I can quickly identify environment configuration issues.

#### Acceptance Criteria

1. WHEN running the verification script THEN it SHALL confirm Kotlin version is 2.2.20
2. WHEN running the verification script THEN it SHALL confirm Gradle version is 8.10 or higher
3. WHEN running the verification script THEN it SHALL confirm Xcode version is 26.x
4. WHEN all versions are correct THEN the script SHALL display a clear success message
5. WHEN any version is incorrect THEN the script SHALL display a clear failure message with expected vs actual versions
6. WHEN running in CI environment THEN the verification script SHALL execute automatically as a pre-check step before build
7. WHEN the script fails THEN the build process SHALL halt with exit code 1 to prevent builds with incorrect toolchain
