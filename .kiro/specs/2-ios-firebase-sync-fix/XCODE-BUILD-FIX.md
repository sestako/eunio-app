# Xcode Build Fix - Complete Guide

## Current Status
✅ Kotlin shared framework rebuilt successfully  
✅ Framework exports kotlinx.serialization, kotlinx.datetime, kotlinx.coroutines  
⏳ Xcode needs to clean and rebuild to pick up changes

## Errors You're Seeing
```
DateExtensions:
- Cannot find type 'Kotlinx_datetimeLocalDate' in scope
- Cannot find type 'Kotlinx_datetimeInstant' in scope

StateFlowExtensions:
- Cannot find type 'Kotlinx_coroutines_coreStateFlow' in scope
```

## Why This Happens
1. The shared Kotlin framework was rebuilt with new exports
2. Xcode is still using the old cached framework
3. Xcode needs to clean its build cache and rebuild

## Solution: Clean and Rebuild in Xcode

### Step 1: Clean Build Folder
In Xcode:
1. Click **Product** menu
2. Hold **Option (⌥)** key
3. Click **Clean Build Folder** (Cmd+Shift+Option+K)
4. Wait for cleaning to complete

### Step 2: Delete Derived Data (Recommended)
This ensures all caches are cleared:

**Option A: From Xcode**
1. Go to **Xcode → Settings** (Cmd+,)
2. Click **Locations** tab
3. Click the arrow next to **Derived Data** path
4. In Finder, delete the folder for your project (starts with "iosApp-")

**Option B: From Terminal**
```bash
rm -rf ~/Library/Developer/Xcode/DerivedData/iosApp-*
```

### Step 3: Rebuild the Project
In Xcode:
1. Click **Product → Build** (Cmd+B)
2. Wait for build to complete
3. Check for errors

### Step 4: Run on Simulator
If build succeeds:
1. Select iPhone 15 (or any iOS 17+ simulator)
2. Click **Product → Run** (Cmd+R)

## Alternative: Complete Clean Rebuild

If the above doesn't work, try this complete clean:

```bash
# 1. Clean Gradle
./gradlew clean

# 2. Rebuild shared framework
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64

# 3. Delete Xcode derived data
rm -rf ~/Library/Developer/Xcode/DerivedData

# 4. Open Xcode
cd iosApp
open iosApp.xcodeproj

# 5. In Xcode: Product → Clean Build Folder (Cmd+Shift+Option+K)
# 6. In Xcode: Product → Build (Cmd+B)
# 7. In Xcode: Product → Run (Cmd+R)
```

## Verification

After rebuilding, verify the types are available:

### Check 1: Build Succeeds
- No errors in Xcode
- Build completes successfully

### Check 2: Types Are Found
The following types should be available in Swift:
- `Kotlinx_datetimeLocalDate`
- `Kotlinx_datetimeInstant`
- `Kotlinx_coroutines_coreStateFlow`
- `Kotlinx_serialization_jsonJson`

### Check 3: App Runs
- App launches on simulator
- No runtime crashes
- Can navigate to Daily Logging screen

## Troubleshooting

### Still Getting "Cannot find type" Errors

**Problem:** Xcode still can't find the Kotlin types

**Solutions:**

1. **Verify framework path in Xcode**
   - Open project settings
   - Select "iosApp" target
   - Go to "Build Phases" → "Link Binary With Libraries"
   - Check that `shared.framework` is listed
   - Path should be: `../shared/build/bin/iosSimulatorArm64/debugFramework/shared.framework`

2. **Check framework search paths**
   - Open project settings
   - Select "iosApp" target
   - Go to "Build Settings"
   - Search for "Framework Search Paths"
   - Should include: `$(SRCROOT)/../shared/build/bin/iosSimulatorArm64/debugFramework`

3. **Rebuild framework for correct architecture**
   ```bash
   # For M1/M2 Mac simulator
   ./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
   
   # For Intel Mac simulator
   ./gradlew :shared:linkDebugFrameworkIosX64
   
   # For physical device
   ./gradlew :shared:linkDebugFrameworkIosArm64
   ```

4. **Check import statement**
   In Swift files, ensure you have:
   ```swift
   import shared
   ```

### Build Succeeds But App Crashes

**Problem:** App builds but crashes at runtime

**Solutions:**

1. **Check console for error messages**
   - Open Console in Xcode (Cmd+Shift+Y)
   - Look for crash logs or error messages

2. **Verify Firebase initialization**
   - Check that `FirebaseApp.configure()` is called
   - Check that `GoogleService-Info.plist` exists

3. **Check Koin initialization**
   - Verify `IOSKoinInitializer.shared.doInitKoin()` is called
   - Check for Koin-related errors in console

### Framework Not Found

**Problem:** Xcode says it can't find shared.framework

**Solutions:**

1. **Rebuild framework**
   ```bash
   ./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
   ```

2. **Check framework exists**
   ```bash
   ls -la shared/build/bin/iosSimulatorArm64/debugFramework/shared.framework
   ```

3. **Update framework path in Xcode**
   - Remove old framework reference
   - Add new framework reference
   - Point to correct path

### Wrong Architecture

**Problem:** Framework built for wrong architecture

**Symptoms:**
- "Building for iOS Simulator, but linking in dylib built for iOS"
- Architecture mismatch errors

**Solutions:**

1. **Check your Mac architecture**
   ```bash
   uname -m
   # arm64 = M1/M2 Mac
   # x86_64 = Intel Mac
   ```

2. **Build for correct architecture**
   ```bash
   # M1/M2 Mac
   ./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
   
   # Intel Mac
   ./gradlew :shared:linkDebugFrameworkIosX64
   ```

## Quick Reference Commands

```bash
# Clean everything
./gradlew clean
rm -rf ~/Library/Developer/Xcode/DerivedData

# Rebuild framework (M1/M2 Mac)
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64

# Rebuild framework (Intel Mac)
./gradlew :shared:linkDebugFrameworkIosX64

# Check framework exists
ls -la shared/build/bin/iosSimulatorArm64/debugFramework/shared.framework

# Open Xcode
cd iosApp && open iosApp.xcodeproj
```

## Expected Result

After following these steps:
- ✅ No build errors in Xcode
- ✅ All Kotlin types are found
- ✅ App builds successfully
- ✅ App runs on simulator
- ✅ Can proceed with Task 5.1 manual testing

## Next Steps

Once the build succeeds:
1. ✅ Verify app launches
2. ✅ Sign in with test account
3. ✅ Navigate to Daily Logging
4. ✅ Follow [TASK-5-1-MANUAL-TEST-GUIDE.md](./TASK-5-1-MANUAL-TEST-GUIDE.md)

## Summary

**Problem:** Xcode using old cached framework  
**Solution:** Clean build folder and derived data, then rebuild  
**Status:** Framework rebuilt, awaiting Xcode clean and rebuild  
**Next Action:** Clean Build Folder in Xcode (Cmd+Shift+Option+K)

---

**Updated:** October 14, 2025  
**Framework Status:** ✅ Rebuilt successfully  
**Xcode Status:** ⏳ Needs clean and rebuild
