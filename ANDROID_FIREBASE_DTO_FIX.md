# Android Firebase DTO Deserialization Fix

## Problem

The Android app was experiencing a critical Firebase deserialization error when attempting to read daily logs from Firestore:

```
java.lang.RuntimeException: Could not deserialize object. Class com.eunio.healthapp.data.remote.dto.DailyLogDto does not define a no-argument constructor. If you are using ProGuard, make sure these constructors are not stripped
```

This error occurred because Firebase's `toObject()` method on Android requires DTOs to have a no-argument constructor for automatic deserialization.

## Root Cause

The `DailyLogDto` and `SexualActivityDto` classes were defined as Kotlin data classes with required parameters but no default values:

```kotlin
data class DailyLogDto(
    val logId: String,                    // No default value
    val dateEpochDays: Long,              // No default value
    val createdAt: Long,                  // No default value
    val updatedAt: Long,                  // No default value
    // ... other fields
)
```

Without default values, Kotlin doesn't generate a no-argument constructor, which Firebase Android SDK requires.

## Solution

### 1. Added Default Values to DTO Parameters

Modified `DailyLogDto` to include default values for all required parameters:

```kotlin
data class DailyLogDto(
    val logId: String = "",               // Default empty string
    val dateEpochDays: Long = 0L,         // Default 0
    val createdAt: Long = 0L,             // Default 0
    val updatedAt: Long = 0L,             // Default 0
    val periodFlow: String? = null,       // Already nullable
    // ... other fields
)
```

Also updated `SexualActivityDto`:

```kotlin
data class SexualActivityDto(
    val occurred: Boolean = false,        // Default false
    val protection: String? = null
)
```

### 2. Added ProGuard Rules

Created `androidApp/proguard-rules.pro` to ensure Firebase-related classes and their constructors are preserved during code minification:

```proguard
# Firebase Firestore - Keep DTOs for deserialization
-keepclassmembers class com.eunio.healthapp.data.remote.dto.** {
    <init>();
    <fields>;
}

# Keep all DTO classes used with Firebase
-keep class com.eunio.healthapp.data.remote.dto.** { *; }

# Firebase SDK rules
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
# ... additional serialization rules
```

### 3. Updated Build Configuration

Modified `androidApp/build.gradle.kts` to reference the ProGuard rules:

```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
    debug {
        isMinifyEnabled = false
    }
}
```

### 4. Added Compatibility Tests

Created `DailyLogDtoFirebaseCompatibilityTest.kt` to verify:
- DTOs can be instantiated with no arguments
- DTOs can be instantiated with all arguments
- Default values are correctly applied

## Impact

### Before Fix
- ❌ All daily log read operations failed on Android
- ❌ Users couldn't view their saved logs
- ❌ Cross-platform sync was broken

### After Fix
- ✅ Firebase can deserialize daily logs successfully
- ✅ Users can view their saved logs
- ✅ Cross-platform sync works correctly
- ✅ All existing tests pass
- ✅ ProGuard won't strip required constructors in release builds

## Testing

Run the following commands to verify the fix:

```bash
# Run DTO tests
./gradlew :shared:testReleaseUnitTest --tests "com.eunio.healthapp.data.remote.dto.*"

# Build Android app
./gradlew :androidApp:assembleDebug

# Run on device and check logs
adb logcat | grep "FirestoreService"
```

## Files Changed

1. `shared/src/commonMain/kotlin/com/eunio/healthapp/data/remote/dto/DailyLogDto.kt`
   - Added default values to all required parameters
   - Updated documentation

2. `androidApp/proguard-rules.pro` (new file)
   - Added ProGuard rules for Firebase DTOs
   - Added Kotlinx Serialization rules

3. `androidApp/build.gradle.kts`
   - Added ProGuard configuration
   - Configured build types

4. `shared/src/androidUnitTest/kotlin/com/eunio/healthapp/data/remote/dto/DailyLogDtoFirebaseCompatibilityTest.kt` (new file)
   - Added tests for no-arg constructor compatibility

## Notes

- The default values (empty strings, zeros) are only used during Firebase deserialization
- Normal app code continues to provide all required values when creating DTOs
- This fix maintains backward compatibility with existing data
- iOS is not affected as it uses a different Firebase SDK implementation
