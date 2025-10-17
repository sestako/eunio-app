#!/bin/bash

echo "=========================================="
echo "Rebuilding App with Test Logs"
echo "=========================================="
echo ""

echo "Step 1: Cleaning project..."
./gradlew clean

echo ""
echo "Step 2: Building debug APK..."
./gradlew :androidApp:assembleDebug

echo ""
echo "Step 3: Installing on device..."
./gradlew :androidApp:installDebug

echo ""
echo "=========================================="
echo "Build complete! Now:"
echo "1. Launch the app from Android Studio (or from your device)"
echo "2. Watch the logs below"
echo "=========================================="
echo ""

# Clear logcat
adb logcat -c

echo "Waiting for app to start..."
sleep 3

echo ""
echo "Looking for MainActivity logs..."
adb logcat -d | grep "MainActivity"

echo ""
echo "=========================================="
echo "Monitoring logs in real-time..."
echo "Press Ctrl+C to stop"
echo "=========================================="
echo ""

# Monitor logs
adb logcat | grep -E "MainActivity|DailyLogService|UserProfileService|AuthService|SyncDebug"
