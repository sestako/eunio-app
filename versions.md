# Version History

## Current Versions (Pre-Upgrade)

### Build Tools
- **Kotlin**: 1.9.21
- **Gradle**: 8.5
- **Android Gradle Plugin (AGP)**: 8.2.2
- **Xcode**: Not installed (Command Line Tools only)
- **iOS SDK**: N/A (Xcode not installed)
- **iOS Deployment Target**: 15.0

### Kotlin Ecosystem
- **kotlinx-coroutines**: 1.7.3
- **kotlinx-serialization**: 1.6.2
- **kotlinx-datetime**: 0.5.0
- **Compose Multiplatform**: 1.5.11
- **Compose UI**: 1.5.4

### Dependencies
- **Ktor**: 2.3.7
- **Koin**: 3.5.3
- **SQLDelight**: 2.0.1

### Firebase
- **Firebase BOM**: 32.7.0
- **Firebase Auth**: 22.3.1
- **Firebase Firestore**: 24.10.0
- **Firebase Functions**: 20.4.0
- **Google Services Plugin**: 4.4.3

### Android
- **Compile SDK**: 34
- **Min SDK**: 24
- **Target SDK**: 34
- **AndroidX Core KTX**: 1.12.0
- **AndroidX AppCompat**: 1.6.1
- **Material Components**: 1.11.0

### Testing
- **JUnit**: 4.13.2
- **MockK**: 1.13.8
- **AndroidX Test JUnit**: 1.1.5
- **Espresso Core**: 3.5.1

## Target Versions (Post-Upgrade)

### Build Tools
- **Kotlin**: 2.2.20
- **Gradle**: 8.10+
- **Android Gradle Plugin (AGP)**: 8.7.3
- **Xcode**: 26.x
- **iOS SDK**: 26
- **iOS Deployment Target**: 15.0 (maintained)

### Kotlin Ecosystem
- **kotlinx-coroutines**: 1.9.0
- **kotlinx-serialization**: 1.7.3
- **kotlinx-datetime**: 0.6.1
- **Compose Multiplatform**: 1.7.1
- **KSP**: 2.2.20-1.0.28

### Dependencies
- **Ktor**: 3.0.1 (major version upgrade)
- **Koin**: 4.0.0 (major version upgrade)
- **SQLDelight**: 2.0.2

### Firebase
- **Firebase**: Latest versions compatible with iOS 26 SDK
- **Dependency Manager**: Swift Package Manager (SPM) preferred over CocoaPods

## Upgrade Date
[To be filled upon completion]

## Breaking Changes Encountered
[To be documented during upgrade process]

## Notes
- Xcode is not currently installed on this system
- iOS development will require Xcode 26 installation
- Bitcode will be disabled in iOS framework (deprecated in iOS 26)
- Strict dependency verification will be enabled
- macOS 26 (Tahoe) required for CI/CD with Xcode 26
