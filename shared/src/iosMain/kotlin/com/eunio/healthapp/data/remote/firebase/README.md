# Firebase Native Bridge for iOS

## Overview

This package provides a bridge between Kotlin Multiplatform shared code and the Firebase iOS SDK. The bridge allows the shared Kotlin code to make Firebase Firestore calls through the native iOS SDK.

## Architecture

```
Kotlin Shared Code (FirestoreServiceImpl.ios.kt)
    ↓
FirebaseNativeBridge (Kotlin expect/actual)
    ↓
FirebaseIOSBridge (Swift)
    ↓
Firebase iOS SDK (Firestore)
```

## Configuration

### 1. Kotlin/Native Interop Setup

The bridge uses Kotlin's `expect`/`actual` mechanism to provide platform-specific implementations:

- **Common**: `FirebaseNativeBridge` interface defined in `commonMain`
- **iOS**: `FirebaseNativeBridge.ios.kt` implementation in `iosMain`
- **Android**: `FirebaseNativeBridge.android.kt` stub in `androidMain`

### 2. Swift Bridge

The Swift bridge (`FirebaseIOSBridge.swift`) wraps the Firebase iOS SDK and exposes it to Kotlin through the shared framework.

**Location**: `iosApp/iosApp/Services/FirebaseIOSBridge.swift`

**Key Features**:
- Uses `@objc` annotations for Objective-C compatibility
- Provides completion handler-based APIs (compatible with Kotlin/Native)
- Implements structured logging for debugging
- Uses standardized Firestore paths

### 3. Initialization

The bridge must be initialized from the iOS app before use:

```swift
import shared

// In your app initialization (e.g., AppDelegate or App struct)
FirebaseBridgeInitializer.initialize()
```

This sets the Swift bridge instance in the Kotlin shared code, making it accessible to `FirebaseNativeBridge`.

### 4. Testing Bridge Connectivity

To verify the bridge is properly configured:

```swift
let isConnected = FirebaseBridgeInitializer.testConnection()
if isConnected {
    print("Bridge is ready to use")
} else {
    print("Bridge initialization failed")
}
```

From Kotlin:

```kotlin
val bridge = FirebaseNativeBridge()
val isConnected = bridge.testConnection()
```

## Usage

### From Kotlin Shared Code

```kotlin
val bridge = FirebaseNativeBridge()

// Save a daily log
val result = bridge.saveDailyLog(
    userId = "user123",
    logId = "2025-10-14",
    data = mapOf(
        "date" to 20371L,  // Epoch days
        "periodFlow" to "medium",
        "mood" to "happy",
        "createdAt" to 1728950400L,  // Seconds since epoch
        "updatedAt" to 1728950400L
    )
)

result.onSuccess {
    println("Log saved successfully")
}.onFailure { error ->
    println("Failed to save log: ${error.message}")
}

// Get a daily log
val getResult = bridge.getDailyLog(
    userId = "user123",
    logId = "2025-10-14"
)

getResult.onSuccess { data ->
    if (data != null) {
        println("Log found: $data")
    } else {
        println("Log not found")
    }
}.onFailure { error ->
    println("Failed to get log: ${error.message}")
}
```

## Implementation Status

### ✅ Completed
- [x] Kotlin expect/actual interface definition
- [x] iOS actual implementation structure
- [x] Swift bridge with all daily log operations
- [x] Bridge initialization mechanism
- [x] Connection testing
- [x] Build configuration (no cinterop needed)

### 🚧 In Progress
- [ ] Actual Swift bridge method calls from Kotlin
- [ ] Error mapping from iOS to Kotlin
- [ ] Coroutine integration with Swift completion handlers

### 📋 Planned
- [ ] Cycle operations
- [ ] Insight operations
- [ ] User operations
- [ ] Batch operations
- [ ] Comprehensive error handling

## Technical Notes

### Why Not Use cinterop?

Initially, we considered using Kotlin/Native's cinterop to directly bind the Swift code. However, we opted for a simpler approach:

1. **Simplicity**: The Swift bridge is already marked with `@objc`, making it accessible through the shared framework
2. **Maintainability**: No need to maintain `.def` files and header configurations
3. **Flexibility**: Easier to modify the bridge interface without rebuilding cinterop bindings

### Data Type Conversions

The bridge handles conversions between Kotlin and Swift/Objective-C types:

- `String` ↔ `NSString`
- `Long` ↔ `Int64`
- `Map<String, Any>` ↔ `NSDictionary`
- `List<Map<String, Any>>` ↔ `NSArray<NSDictionary>`

### Error Handling

Errors from the Firebase iOS SDK are converted to Kotlin `Result` types:

- Success: `Result.success(value)`
- Failure: `Result.failure(exception)`

## Troubleshooting

### Bridge Not Initialized Error

**Error**: `IllegalStateException: Swift bridge not initialized`

**Solution**: Ensure `FirebaseBridgeInitializer.initialize()` is called during app startup, after Firebase configuration.

### Build Errors

If you encounter build errors related to the bridge:

1. Clean the build: `./gradlew clean`
2. Rebuild the shared framework: `./gradlew :shared:build`
3. Clean Xcode build folder: Product → Clean Build Folder

### Runtime Errors

If Firebase operations fail at runtime:

1. Check Firebase configuration in `GoogleService-Info.plist`
2. Verify Firebase is initialized: `FirebaseApp.configure()`
3. Check Firestore security rules
4. Enable Firebase debug logging in Xcode

## References

- [Kotlin Multiplatform Documentation](https://kotlinlang.org/docs/multiplatform.html)
- [Firebase iOS SDK Documentation](https://firebase.google.com/docs/ios/setup)
- [Kotlin/Native Interop](https://kotlinlang.org/docs/native-c-interop.html)
