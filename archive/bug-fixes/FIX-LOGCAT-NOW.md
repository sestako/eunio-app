# Fix Logcat - Step by Step

## Do These Steps in Order:

### Step 1: Run This Script

Open a terminal in your project directory and run:

```bash
./check-logs.sh
```

This will:
- Check if your device is connected
- Check if the app is running
- Show you any logs from your app
- Monitor logs in real-time

**Keep this terminal open** - it will show logs as they happen.

### Step 2: In Android Studio

While the script is running:

1. **Stop the app** (red square button)
2. **Clean Project**: Build → Clean Project
3. **Rebuild Project**: Build → Rebuild Project
4. **Run the app** (green play button or Shift+F10)

Watch the terminal - you should see logs appear!

### Step 3: Check Logcat Panel in Android Studio

In the Logcat panel (bottom of Android Studio):

1. **Click the dropdown** at the top left
   - Should show your device name or `com.eunio.healthapp`
   - If it says "No debuggable processes", the app isn't running

2. **Set log level to "Verbose"**
   - Look for a dropdown that might say "Debug", "Info", "Error"
   - Change it to **"Verbose"**

3. **Clear the search box**
   - Remove any text from the filter box
   - Click the trash icon to clear old logs

4. **Type in search box**: `MainActivity`
   - You should see test logs appear

### Step 4: What You Should See

In the terminal (from check-logs.sh):
```
✓ MainActivity logs found:
V/MainActivity: MainActivity onCreate - App Starting
D/MainActivity: DEBUG: Logging system is working!
I/MainActivity: INFO: App initialized successfully
```

In Android Studio Logcat:
- Same logs should appear
- You can filter by typing `MainActivity` or `tag:MainActivity`

## If You Still Don't See Logs:

### Option A: Check Build Variant

1. **View → Tool Windows → Build Variants**
2. Make sure it says **"debug"** (not "release")
3. If it was "release", change to "debug" and rebuild

### Option B: Check from Terminal Only

If the script shows logs but Android Studio doesn't:

```bash
# Watch logs in real-time
adb logcat | grep -E "MainActivity|DailyLogService|UserProfileService"
```

If you see logs in terminal but not in Android Studio:
- **File → Invalidate Caches / Restart**
- Restart Android Studio

### Option C: Check Device Connection

```bash
adb devices
```

Should show:
```
List of devices attached
emulator-5554   device
```

If empty:
- Reconnect device
- Restart emulator
- Run: `adb kill-server && adb start-server`

## Quick Test:

Run these commands to see if logging works at all:

```bash
# Clear logs
adb logcat -c

# Restart app in Android Studio (Stop → Run)

# Check for MainActivity logs
adb logcat -d | grep MainActivity
```

If you see output, logging works! The issue is just with Android Studio's Logcat panel.

## What to Tell Me:

After running `./check-logs.sh`, tell me:

1. **Does the script show MainActivity logs?** (Yes/No)
2. **Does Android Studio Logcat show the same logs?** (Yes/No)
3. **What does the device dropdown in Logcat show?**
4. **What is the log level set to in Logcat?**

This will help me figure out exactly what's wrong!

## Expected Result:

✅ **Success looks like this:**

Terminal shows:
```
✓ MainActivity logs found:
V/MainActivity: MainActivity onCreate - App Starting
D/MainActivity: DEBUG: Logging system is working!
```

Android Studio Logcat shows the same logs when you search for `MainActivity`.

Once we see these test logs, we'll know logging works and can focus on why the service logs aren't appearing (which is likely because the services aren't being used yet).
