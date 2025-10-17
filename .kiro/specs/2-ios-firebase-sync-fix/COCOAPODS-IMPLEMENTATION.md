# CocoaPods Firebase Implementation

## What Was Done

### 1. Added CocoaPods Configuration to build.gradle.kts
```kotlin
cocoapods {
    summary = "Shared module for Eunio Health App"
    homepage = "https://github.com/eunio/healthapp"
    version = "1.0"
    ios.deploymentTarget = "14.0"
    
    // Firebase pods - these expose Firebase classes to Kotlin/Native
    pod("FirebaseCore") { version = "~> 10.0" }
    pod("FirebaseFirestore") { version = "~> 10.0" }
    pod("FirebaseAuth") { version = "~> 10.0" }
    
    framework {
        baseName = "shared"
        isStatic = true
    }
}
```

This automatically exposes Firebase iOS SDK classes to Kotlin/Native:
- `FIRFirestore` - Firestore database instance
- `FIRDocumentReference` - Document references
- `FIRQuery` - Query builder
- `FIRWriteBatch` - Batch operations
- All other Firebase classes

### 2. Rewrote FirestoreServiceImpl.ios.kt
**Removed:**
- ❌ Swift bridge pattern (unstable, not type-safe)
- ❌ Dynamic method invocation
- ❌ Manual interop with Swift classes
- ❌ FirebaseNativeBridge wrapper

**Added:**
- ✅ Direct Firebase SDK access via CocoaPods
- ✅ Type-safe Kotlin/Native interop
- ✅ Proper suspend functions with `suspendCancellableCoroutine`
- ✅ Consistent data format with Android (epoch days, seconds, v=1)
- ✅ NSLog debugging for all operations

### 3. Implementation Details

#### Firestore Access
```kotlin
private val db: FIRFirestore
    get() = FIRFirestore.firestore()
```

#### Save Operation
```kotlin
override suspend fun saveDailyLog(dailyLog: DailyLog): Result<Unit> {
    val docRef = db.collectionWithPath(USERS_COLLECTION)
        .documentWithPath(dailyLog.userId)
        .collectionWithPath(DAILY_LOGS_COLLECTION)
        .documentWithPath(dailyLog.id)
    
    val data = dtoToFirestoreMap(dto)  // Converts to primitive types
    setDocument(docRef, data)  // Uses suspendCancellableCoroutine
}
```

#### Data Format
All data uses primitive Objective-C compatible types:
- `NSNumber(long = ...)` for Long values (dates, timestamps)
- `NSNumber(double = ...)` for Double values (temperature)
- `NSNumber(int = ...)` for Int values (schema version)
- `String` for text
- `Boolean` for flags
- `List<String>` for arrays
- `Map<String, Any>` for nested objects

#### Path Structure
Same as Android:
```
users/{userId}/dailyLogs/{logId}
```

#### Schema
```kotlin
{
    "logId": "2025-10-15",
    "dateEpochDays": 20376,
    "createdAt": 1760532296,
    "updatedAt": 1760553649,
    "v": 1,
    "periodFlow": "HEAVY",
    "symptoms": ["CRAMPS", "HEADACHE"],
    "mood": "SAD",
    "notes": "ios test fix"
}
```

### 4. What Needs to Be Done Next

#### Step 1: Install CocoaPods Dependencies
```bash
cd iosApp
pod install
```

This will:
- Download Firebase iOS SDK pods
- Generate `.xcworkspace` file
- Configure Xcode project

#### Step 2: Open Workspace (Not Project!)
```bash
open iosApp.xcworkspace  # NOT iosApp.xcodeproj
```

#### Step 3: Build Kotlin Framework
```bash
./gradlew shared:linkDebugFrameworkIosSimulatorArm64
```

This will:
- Compile Kotlin code with Firebase interop
- Generate cinterop bindings for Firebase pods
- Create the shared.framework

#### Step 4: Build and Run iOS App
In Xcode:
1. Clean build folder (Cmd+Shift+K)
2. Build (Cmd+B)
3. Run on simulator (Cmd+R)

#### Step 5: Test Save Operation
1. Sign in to the app
2. Create a daily log for October 15, 2025
3. Add note "cocoapods test"
4. Save
5. Check Xcode console for NSLog messages
6. Check Firebase Console for the data

### 5. Expected Console Output

```
[FirestoreService.iOS] SAVE_DAILY_LOG_START userId=8FzGtzfcIkUjAwZW9qqA6OkbtNL2, logId=2025-10-15, path=users/8FzGtzfcIkUjAwZW9qqA6OkbtNL2/dailyLogs/2025-10-15, dateEpochDays=20376
[FirestoreService.iOS] SAVE_DAILY_LOG_SUCCESS userId=8FzGtzfcIkUjAwZW9qqA6OkbtNL2, logId=2025-10-15, path=users/8FzGtzfcIkUjAwZW9qqA6OkbtNL2/dailyLogs/2025-10-15
```

### 6. Benefits of This Approach

✅ **Type-Safe**: Kotlin/Native knows about all Firebase types at compile time
✅ **Stable**: Uses official Kotlin Multiplatform patterns
✅ **No Swift Bridge**: Direct SDK access, no intermediate layer
✅ **Consistent**: Same data format and paths as Android
✅ **Maintainable**: Standard KMM approach, easy to understand
✅ **Debuggable**: NSLog shows all operations and errors

### 7. Troubleshooting

#### If CocoaPods not installed:
```bash
sudo gem install cocoapods
```

#### If pod install fails:
```bash
cd iosApp
pod repo update
pod install
```

#### If Kotlin build fails:
- Make sure you're using Xcode 14.0 or later
- Check that iOS deployment target is 14.0+
- Verify Firebase pod versions are compatible

#### If Firebase not initialized:
- Ensure `FirebaseApp.configure()` is called in AppDelegate
- Check that GoogleService-Info.plist is in the iOS project
- Verify Firebase project is set up correctly

### 8. Next Steps After Testing

Once this works:
1. ✅ Mark task 5.1 as complete
2. Move to task 5.2 (test load operation)
3. Implement remaining Firestore operations (users, cycles, insights)
4. Add offline persistence support
5. Implement sync conflict resolution

## Summary

This implementation replaces the unstable Swift bridge pattern with the proper CocoaPods interop approach. It's type-safe, stable, and follows Kotlin Multiplatform best practices. All Firebase operations are now direct SDK calls with proper error handling and logging.
