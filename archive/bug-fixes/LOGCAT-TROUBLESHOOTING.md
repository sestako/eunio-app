# Logcat Troubleshooting Guide

## Issue: No Logs Appearing in Logcat

If you can't see any logs in Android Studio Logcat, follow these steps:

## Step 1: Check Logcat Configuration

### 1.1 Verify Device/Process Selection
- In the Logcat panel, check the dropdown at the top
- Make sure your app process is selected: `com.eunio.healthapp` (or similar)
- If you see "No debuggable processes", the app might not be running

### 1.2 Check Log Level Filter
- Look for the log level dropdown (usually shows "Verbose", "Debug", "Info", etc.)
- **Set it to "Verbose"** to see all logs
- If it's set to "Error" or "Warn", you won't see DEBUG logs

### 1.3 Clear Filters
- Remove any text from the search/filter box
- Click the "Clear logcat" button (trash icon) to start fresh
- Run the app again

### 1.4 Check "Show only selected application"
- There's usually a dropdown that says "Show only selected application"
- Make sure this is enabled and your app is selected

## Step 2: Verify App is Running in Debug Mode

### 2.1 Check Build Variant
1. In Android Studio, go to: **View → Tool Windows → Build Variants**
2. Make sure the build variant is set to **"debug"** (not "release")
3. If it was set to "release", change it to "debug" and rebuild

### 2.2 Rebuild and Reinstall
```bash
# Clean and rebuild
./gradlew clean
./gradlew :androidApp:assembleDebug

# Or in Android Studio:
# Build → Clean Project
# Build → Rebuild Project
```

## Step 3: Verify Services Are Being Called

The logs only appear when the service methods are actually called. If you're not seeing logs, it might be because:

1. **You haven't triggered any operations yet**
   - Try creating a daily log entry
   - Try updating your profile
   - Try signing in/out
   - Try navigating between screens

2. **The services aren't being used yet**
   - Check if the app is actually calling these services
   - The services might not be wired up to the UI yet

## Step 4: Add Test Logs to Verify Logging Works

Let me create a simple test to verify logging is working at all.

### 4.1 Add a Test Log in MainActivity

Find your MainActivity (usually in `androidApp/src/main/kotlin/.../MainActivity.kt`) and add this at the top of `onCreate()`:

```kotlin
import android.util.Log

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // TEST LOG - Remove after verification
        Log.d("TEST", "MainActivity onCreate called - Logging is working!")
        Log.i("TEST", "This is an INFO log")
        Log.w("TEST", "This is a WARN log")
        Log.e("TEST", "This is an ERROR log")
        
        // ... rest of your code
    }
}
```

### 4.2 Run the App and Check Logcat

1. Run the app
2. In Logcat, search for: `tag:TEST`
3. You should see 4 log messages

**If you see the TEST logs:**
- ✅ Logging is working!
- The issue is that your services aren't being called yet
- You need to trigger operations that use the services

**If you DON'T see the TEST logs:**
- ❌ There's a Logcat configuration issue
- Continue to Step 5

## Step 5: Advanced Troubleshooting

### 5.1 Check ADB Connection

```bash
# Check if device is connected
adb devices

# Should show something like:
# List of devices attached
# emulator-5554   device
```

If no devices are listed:
- Reconnect your device
- Restart the emulator
- Restart ADB: `adb kill-server && adb start-server`

### 5.2 Check Logcat from Command Line

Try viewing logs directly from the terminal:

```bash
# Clear logs
adb logcat -c

# View all logs
adb logcat

# Filter by tag
adb logcat -s DailyLogService:D UserProfileService:D AuthService:D

# Filter by your app package
adb logcat | grep "com.eunio.healthapp"
```

If you see logs in the terminal but not in Android Studio:
- Restart Android Studio
- Invalidate Caches: **File → Invalidate Caches / Restart**

### 5.3 Check ProGuard/R8 Configuration

If you're building in release mode, ProGuard/R8 might be stripping logs.

Check `androidApp/build.gradle.kts`:

```kotlin
buildTypes {
    getByName("debug") {
        isMinifyEnabled = false  // Should be false for debug
        isDebuggable = true      // Should be true for debug
    }
}
```

## Step 6: Verify Services Are Instantiated

The services need to be created and used for logs to appear. Let me check where they're instantiated.

### 6.1 Check Dependency Injection / Service Creation

Look for where these services are created:
- `AndroidDailyLogService`
- `AndroidUserProfileService`
- `AndroidAuthService`
- `AndroidSettingsManager`
- `AndroidNavigationManager`

### 6.2 Add Initialization Logs

Add logs to the service constructors or init blocks:

```kotlin
class AndroidDailyLogService : DailyLogService {
    
    companion object {
        private const val TAG = "DailyLogService"
    }
    
    init {
        Log.d(TAG, "AndroidDailyLogService initialized")
    }
    
    // ... rest of code
}
```

## Common Solutions

### Solution 1: App Not Running
**Problem**: The app crashed or isn't running
**Fix**: Check the Run panel for errors, rebuild and run again

### Solution 2: Wrong Log Level
**Problem**: Logcat filter set too high
**Fix**: Set log level to "Verbose" in Logcat dropdown

### Solution 3: Wrong Process Selected
**Problem**: Logcat showing logs from different app
**Fix**: Select your app process in the device/process dropdown

### Solution 4: Services Not Called Yet
**Problem**: Services exist but aren't being used
**Fix**: Trigger operations in the app that use these services

### Solution 5: Logcat Panel Issue
**Problem**: Android Studio Logcat panel not working
**Fix**: 
- Close and reopen Logcat panel
- Restart Android Studio
- Use command line: `adb logcat`

## Quick Diagnostic Checklist

Run through this checklist:

- [ ] App is running on device/emulator
- [ ] Correct app process selected in Logcat dropdown
- [ ] Log level set to "Verbose" or "Debug"
- [ ] No filters in the search box (or correct filter applied)
- [ ] Build variant is "debug" (not "release")
- [ ] ADB connection working (`adb devices` shows device)
- [ ] Tried adding TEST log in MainActivity
- [ ] Tried viewing logs from command line (`adb logcat`)
- [ ] Actually triggered operations that use the services

## Next Steps

1. **First**: Add the TEST log to MainActivity and verify basic logging works
2. **Then**: If TEST logs work, the issue is that services aren't being called
3. **Finally**: Check where services are instantiated and add init logs

Let me know which step reveals the issue and I can help further!
