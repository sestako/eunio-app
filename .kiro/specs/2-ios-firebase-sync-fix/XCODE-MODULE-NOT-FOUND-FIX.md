# Fix: "Unable to find module dependency: 'shared'"

## Error
```
Unable to find module dependency: 'shared'
import shared
```

## Root Cause
Xcode cannot find the `shared` Kotlin framework module. This happens when:
1. Xcode's build script hasn't run to embed the framework
2. Xcode's build cache is stale
3. The framework wasn't built by Xcode's automated build process

## Solution

### Step 1: Clean Everything

**In Terminal:**
```bash
# Clean Gradle
./gradlew clean

# Delete Xcode derived data
rm -rf ~/Library/Developer/Xcode/DerivedData/iosApp-*

# Verify framework exists
ls -la shared/build/bin/iosSimulatorArm64/debugFramework/shared.framework/
```

**In Xcode:**
1. Hold **Option (⌥)** key
2. Click **Product → Clean Build Folder** (Cmd+Shift+Option+K)
3. Wait for cleaning to complete

### Step 2: Build in Xcode

**Important:** The framework MUST be built by Xcode's build script, not manually.

1. In Xcode: **Product → Build** (Cmd+B)
2. Watch the build output for "Build Shared Framework" phase
3. This phase runs the Gradle task to build and embed the framework
4. Wait for build to complete

### Step 3: Verify Build Script Ran

In Xcode's build log (View → Navigators → Reports):
1. Select the latest build
2. Look for "Build Shared Framework" phase
3. Should see Gradle output building the framework
4. Should see "BUILD SUCCESSFUL"

### Step 4: Check for Errors

If build fails, check the build log for:
- Gradle errors
- Framework linking errors
- Path issues

## Common Issues

### Issue 1: Build Script Doesn't Run

**Symptoms:**
- No "Build Shared Framework" phase in build log
- Framework not embedded

**Solution:**
1. Open Xcode project settings
2. Select "iosApp" target
3. Go to "Build Phases"
4. Verify "Build Shared Framework" script exists
5. Should be before "Compile Sources"

### Issue 2: Gradle Task Fails

**Symptoms:**
- Build script runs but fails
- Gradle errors in build log

**Solution:**
1. Check Gradle error message
2. Try running manually: `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64`
3. Fix any Gradle issues
4. Clean and rebuild in Xcode

### Issue 3: Wrong Architecture

**Symptoms:**
- "Building for iOS Simulator, but linking in dylib built for iOS"
- Architecture mismatch errors

**Solution:**
The build script automatically detects architecture. If it fails:
1. Check your Mac architecture: `uname -m`
2. For M1/M2 (arm64): Framework should be iosSimulatorArm64
3. For Intel (x86_64): Framework should be iosX64

### Issue 4: Framework Path Wrong

**Symptoms:**
- Framework exists but Xcode can't find it
- "Framework not found" errors

**Solution:**
1. Open Xcode project settings
2. Select "iosApp" target
3. Go to "Build Settings"
4. Search for "Framework Search Paths"
5. Should include: `$(SRCROOT)/../shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)`
6. Also check: `$(SRCROOT)/../shared/build/bin/$(NATIVE_ARCH)/$(CONFIGURATION:lower)Framework`

## Detailed Steps

### Complete Clean and Rebuild Process

```bash
# 1. Clean Gradle
./gradlew clean

# 2. Delete derived data
rm -rf ~/Library/Developer/Xcode/DerivedData

# 3. Verify Gradle works
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64

# 4. Open Xcode
cd iosApp
open iosApp.xcodeproj
```

**In Xcode:**
1. Product → Clean Build Folder (hold Option, Cmd+Shift+Option+K)
2. Product → Build (Cmd+B)
3. Watch build log for "Build Shared Framework" phase
4. Wait for "BUILD SUCCESSFUL"
5. Product → Run (Cmd+R)

## Verification

After building, verify:

### 1. Framework Embedded
```bash
# Check if framework was embedded
ls -la iosApp/build/Debug-iphonesimulator/iosApp.app/Frameworks/shared.framework/
```

### 2. Module Available
In Xcode, the `import shared` statement should:
- Not show any errors
- Autocomplete should work
- Types like `DailyLog`, `FirestoreService` should be available

### 3. Build Succeeds
- No errors in Xcode
- Build completes successfully
- App can be run on simulator

## Understanding the Build Process

### How It Works

1. **Xcode Build Starts**
   - Xcode runs build phases in order

2. **Build Shared Framework Phase**
   - Runs before compiling Swift code
   - Executes Gradle task: `embedAndSignAppleFrameworkForXcode`
   - Gradle builds the framework for correct architecture
   - Framework is copied to Xcode's build directory

3. **Compile Swift Code**
   - Swift compiler can now find `shared` module
   - Imports work correctly

4. **Link and Embed**
   - Framework is linked into the app
   - Framework is embedded in app bundle

### Why Manual Build Doesn't Work

Running `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64` manually:
- ✅ Builds the framework
- ❌ Doesn't embed it in Xcode's build directory
- ❌ Xcode still can't find it

The `embedAndSignAppleFrameworkForXcode` task:
- ✅ Builds the framework
- ✅ Copies it to Xcode's expected location
- ✅ Signs it if needed
- ✅ Xcode can find it

## Troubleshooting Checklist

- [ ] Gradle clean completed
- [ ] Xcode derived data deleted
- [ ] Xcode build folder cleaned
- [ ] Build in Xcode (not manual Gradle)
- [ ] "Build Shared Framework" phase ran
- [ ] No Gradle errors in build log
- [ ] Framework exists in build directory
- [ ] `import shared` has no errors
- [ ] Build succeeds
- [ ] App runs on simulator

## Still Not Working?

### Check Build Log

1. In Xcode: View → Navigators → Reports (Cmd+9)
2. Select latest build
3. Expand "Build Shared Framework"
4. Look for errors

### Common Error Messages

**"Please run the embedAndSignAppleFrameworkForXcode task from Xcode"**
- You tried to run the task manually
- Must be run by Xcode's build script

**"SDK_NAME not provided"**
- Build script missing environment variables
- Check build phase script is correct

**"Gradle daemon disappeared unexpectedly"**
- Gradle crashed
- Try: `./gradlew --stop` then rebuild

**"Framework not found"**
- Framework path is wrong
- Check framework search paths in build settings

## Quick Fix Script

```bash
#!/bin/bash
# Quick fix for module not found

echo "Cleaning everything..."
./gradlew clean
rm -rf ~/Library/Developer/Xcode/DerivedData/iosApp-*

echo "Building framework..."
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64

echo "Done! Now:"
echo "1. Open Xcode"
echo "2. Product → Clean Build Folder (hold Option)"
echo "3. Product → Build"
echo "4. Check build log for 'Build Shared Framework' phase"
```

## Summary

**Problem:** Xcode can't find `shared` module  
**Cause:** Framework not embedded by Xcode's build process  
**Solution:** Clean everything, then build in Xcode (not manually)  
**Key Point:** Must let Xcode's build script run to embed framework

---

**Status:** Framework exists, needs Xcode build  
**Next Action:** Clean Build Folder in Xcode, then Build  
**Expected:** "Build Shared Framework" phase runs successfully
