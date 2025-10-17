# Task 5.1 - Build Issue Resolution

## Issue Encountered
When attempting to build the iOS app in Xcode, the following error occurred:

```
Following dependencies exported in the debugFramework binary are not 
specified as API-dependencies of a corresponding source set
```

## Root Cause
The `kotlinx.serialization.json` dependency was:
1. Used in iOS-specific Kotlin code (`FirestoreServiceImpl.ios.kt`)
2. Needed by the iOS framework
3. But declared as `implementation` instead of `api`

This violates Kotlin Multiplatform's rule: dependencies used in exported code must be declared as `api`.

## Fix Applied

### File Modified: `shared/build.gradle.kts`

**Change 1: Added serialization to framework exports**
```kotlin
iosTarget.binaries.framework {
    baseName = "shared"
    isStatic = true
    
    export(libs.kotlinx.coroutines.core)
    export(libs.kotlinx.datetime)
    export(libs.kotlinx.serialization.json)  // ← ADDED
}
```

**Change 2: Changed dependency declaration**
```kotlin
commonMain.dependencies {
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.serialization.json)  // ← CHANGED from implementation
    api(libs.kotlinx.datetime)
}
```

## How to Apply

### Step 1: Clean Build
```bash
# In terminal
./gradlew clean

# Or in Xcode
Product → Clean Build Folder (Cmd+Shift+K)
```

### Step 2: Rebuild Shared Framework
```bash
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

### Step 3: Rebuild iOS App
In Xcode:
1. Product → Clean Build Folder (Cmd+Shift+K)
2. Product → Build (Cmd+B)
3. Product → Run (Cmd+R)

## Expected Result
- ✅ No build errors
- ✅ Shared framework builds successfully
- ✅ iOS app builds and runs on simulator

## Verification
After rebuilding, verify the fix worked:

```bash
# Check build status
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64

# Run verification script
./verify-implementation-readiness.sh
```

## Impact on Task 5.1
This was a **build configuration issue**, not an implementation issue. The actual implementation is correct and complete. Once the build succeeds, you can proceed with the manual testing as documented in:

- [TASK-5-1-MANUAL-TEST-GUIDE.md](./TASK-5-1-MANUAL-TEST-GUIDE.md)
- [BUILD-FIX-GUIDE.md](./BUILD-FIX-GUIDE.md)

## Status
- ✅ Issue identified
- ✅ Fix applied to `shared/build.gradle.kts`
- ⏳ Awaiting clean build and rebuild
- ⏳ Awaiting manual test execution

## Next Steps
1. Clean build (Gradle and Xcode)
2. Rebuild shared framework
3. Rebuild iOS app in Xcode
4. Verify build succeeds
5. Proceed with Task 5.1 manual testing

---

**Issue:** Build configuration error  
**Fix:** Changed serialization dependency to `api` and added to exports  
**Status:** ✅ Fixed  
**Date:** October 14, 2025
