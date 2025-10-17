#!/bin/bash

echo "=========================================="
echo "Android Logcat Diagnostic Script"
echo "=========================================="
echo ""

# Check if ADB is available
if ! command -v adb &> /dev/null; then
    echo "❌ ADB not found. Please install Android SDK Platform Tools."
    exit 1
fi

echo "✓ ADB found"
echo ""

# Check connected devices
echo "Checking connected devices..."
DEVICES=$(adb devices | grep -v "List of devices" | grep "device$" | wc -l)

if [ "$DEVICES" -eq 0 ]; then
    echo "❌ No devices connected"
    echo ""
    echo "Please:"
    echo "1. Connect your Android device via USB, or"
    echo "2. Start an Android emulator"
    echo ""
    exit 1
fi

echo "✓ Found $DEVICES device(s) connected"
adb devices
echo ""

# Check if app is running
echo "Checking if app is running..."
APP_RUNNING=$(adb shell ps | grep "com.eunio.healthapp" | wc -l)

if [ "$APP_RUNNING" -eq 0 ]; then
    echo "⚠️  App is not currently running"
    echo ""
    echo "Please run the app from Android Studio first, then run this script again."
    echo ""
    read -p "Press Enter after you've started the app..."
fi

echo "✓ App appears to be running"
echo ""

# Clear logcat
echo "Clearing old logs..."
adb logcat -c
echo "✓ Logs cleared"
echo ""

# Wait a moment
echo "Waiting 2 seconds for new logs..."
sleep 2

# Check for MainActivity logs
echo ""
echo "=========================================="
echo "Looking for MainActivity test logs..."
echo "=========================================="
MAIN_LOGS=$(adb logcat -d | grep "MainActivity")

if [ -z "$MAIN_LOGS" ]; then
    echo "❌ No MainActivity logs found"
    echo ""
    echo "This means either:"
    echo "1. The app hasn't started yet (restart the app)"
    echo "2. The test logs weren't added properly"
    echo "3. Logging is being filtered/blocked"
else
    echo "✓ MainActivity logs found:"
    echo "$MAIN_LOGS"
fi

echo ""
echo "=========================================="
echo "Looking for Service initialization logs..."
echo "=========================================="

# Check for service logs
SERVICE_LOGS=$(adb logcat -d | grep -E "DailyLogService|UserProfileService|AuthService|SettingsManager|NavigationManager")

if [ -z "$SERVICE_LOGS" ]; then
    echo "⚠️  No service logs found"
    echo ""
    echo "This is expected if the services haven't been instantiated yet."
    echo "Services only log when they're created and used."
else
    echo "✓ Service logs found:"
    echo "$SERVICE_LOGS"
fi

echo ""
echo "=========================================="
echo "All logs from your app (last 100 lines):"
echo "=========================================="
adb logcat -d | grep "com.eunio.healthapp" | tail -100

echo ""
echo "=========================================="
echo "Live log monitoring (Ctrl+C to stop)"
echo "=========================================="
echo "Watching for: MainActivity, DailyLogService, UserProfileService, AuthService"
echo ""

# Monitor logs in real-time
adb logcat | grep -E "MainActivity|DailyLogService|UserProfileService|AuthService|SettingsManager|NavigationManager"
