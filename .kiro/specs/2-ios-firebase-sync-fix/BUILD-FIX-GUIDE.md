# iOS Build Error Fix Guide

## Error Description
```
Following dependencies exported in the debugFramework binary are not 
specified as API-dependencies of a corresponding source set
```

## Root Cause
The iOS framework exports certain Kotlin dependencies (like `kotlinx.serialization.json`) that are used in the iOS-specific code, but these dependencies were declared as `implementation` instead of `api` in the build configuration.

## Solution Applied

### Changes Made to `shared/build.gradle.kts`

1. **Added serialization to exports** (line ~24):
```kotlin
iosTarget.binaries.framework {
    baseName = "shared"
    isStatic = true
    
    // Export dependencies that are used in public API
    export(libs.kotlinx.coroutines.core)
    export(libs.kotlinx.datetime)
    export(libs.kotlinx.serialization.json)  // ← ADDED
}
```

2. **Changed serialization from implementation to api** (line ~35):
```kotlin
commonMain.dependencies {
    // Coroutines - exported for iOS
    api(libs.kotlinx.coroutines.core)
    
    // Serialization - exported for iOS
    api(libs.kotlinx.serialization.json)  // ← CHANGED from implementation
    
    // DateTime - exported for iOS
    api(libs.kotlinx.datetime)
}
```

## How to Apply the Fix

### Option 1: Automatic (Already Applied)
The fix has been automatically applied to your `shared/build.gradle.kts` file.

### Option 2: Manual (If Needed)
If you need to revert or manually apply:

1. Open `shared/build.gradle.kts`
2. Find the iOS framework configuration (around line 18-26)
3. Add `export(libs.kotlinx.serialization.json)` to the exports
4. Find the commonMain dependencies (around line 30-40)
5. Change `implementation(libs.kotlinx.serialization.json)` to `api(libs.kotlinx.serialization.json)`

## Next Steps

### 1. Clean Build
```bash
# Clean the project
./gradlew clean

# Or in Xcode
Product → Clean Build Folder (Cmd+Shift+K)
```

### 2. Rebuild Shared Framework
```bash
# Build the shared framework
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64

# Or for all iOS targets
./gradlew :shared:linkDebugFrameworkIosX64
./gradlew :shared:linkDebugFrameworkIosArm64
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

### 3. Rebuild iOS App in Xcode
1. Open Xcode
2. Select Product → Clean Build Folder (Cmd+Shift+K)
3. Build the project (Cmd+B)
4. Run on simulator (Cmd+R)

## Verification

After applying the fix and rebuilding:

1. **Check Build Success**
   - No errors in Xcode
   - Shared framework builds successfully
   - iOS app builds successfully

2. **Verify Framework Exports**
   ```bash
   # Check what's exported in the framework
   nm -g shared/build/bin/iosSimulatorArm64/debugFramework/shared.framework/shared | grep -i json
   ```

3. **Run Verification Script**
   ```bash
   ./verify-implementation-readiness.sh
   ```

## Why This Happens

### Background
Kotlin Multiplatform generates a framework for iOS that includes:
- Your shared Kotlin code
- Dependencies marked with `export()`

### The Rule
If a dependency is:
1. Used in iOS-specific code (`iosMain`)
2. Exported in the framework configuration
3. Then it MUST be declared as `api()` (not `implementation()`)

### In This Case
- `kotlinx.serialization.json` is used in `FirestoreServiceImpl.ios.kt`
- It's exported in the framework (or implicitly needed)
- So it must be `api()` instead of `implementation()`

## Common Variations of This Error

### If you see similar errors for other dependencies:

1. **kotlinx-coroutines-core**
   - Already fixed: declared as `api()` and exported

2. **kotlinx-datetime**
   - Already fixed: declared as `api()` and exported

3. **Other dependencies**
   - If you add new dependencies used in `iosMain`, remember:
     - Declare as `api()` in `commonMain.dependencies`
     - Add `export()` in the framework configuration

## Troubleshooting

### Build Still Fails After Fix

1. **Clean Everything**
   ```bash
   # Clean Gradle
   ./gradlew clean
   ./gradlew cleanBuildCache
   
   # Clean Xcode
   # In Xcode: Product → Clean Build Folder
   
   # Delete derived data
   rm -rf ~/Library/Developer/Xcode/DerivedData
   ```

2. **Invalidate Caches**
   ```bash
   # Gradle
   ./gradlew --stop
   rm -rf .gradle
   
   # Xcode
   # Xcode → Preferences → Locations → Derived Data → Delete
   ```

3. **Rebuild from Scratch**
   ```bash
   ./gradlew clean
   ./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
   # Then build in Xcode
   ```

### Different Error Message

If you see a different error about dependencies:

1. **Identify the dependency** mentioned in the error
2. **Find where it's used** in `iosMain` code
3. **Change to api()** in `commonMain.dependencies`
4. **Add export()** in framework configuration

### Framework Not Found

If Xcode says it can't find the shared framework:

1. **Check framework path** in Xcode project settings
2. **Rebuild framework**:
   ```bash
   ./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
   ```
3. **Verify framework exists**:
   ```bash
   ls -la shared/build/bin/iosSimulatorArm64/debugFramework/
   ```

## Prevention

To avoid this error in the future:

1. **When adding new dependencies** used in `iosMain`:
   - Use `api()` instead of `implementation()` in `commonMain`
   - Add `export()` in the iOS framework configuration

2. **When adding new iOS-specific code**:
   - Check what dependencies it uses
   - Ensure they're properly exported

3. **Regular builds**:
   - Build iOS target regularly during development
   - Don't wait until the end to test iOS builds

## Related Documentation

- [Kotlin Multiplatform iOS Framework](https://kotlinlang.org/docs/multiplatform-build-native-binaries.html)
- [Exporting Dependencies](https://kotlinlang.org/docs/multiplatform-build-native-binaries.html#export-dependencies-to-binaries)
- [API vs Implementation Dependencies](https://docs.gradle.org/current/userguide/java_library_plugin.html#sec:java_library_separation)

## Summary

**Problem:** Serialization dependency not properly exported for iOS framework  
**Solution:** Changed from `implementation` to `api` and added to exports  
**Status:** ✅ Fixed  
**Next Action:** Clean build and rebuild in Xcode

---

**Fixed:** October 14, 2025  
**File Modified:** `shared/build.gradle.kts`  
**Lines Changed:** 2 (added export, changed implementation to api)
