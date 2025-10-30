#!/bin/bash

# Android Firebase Sync Test Script
# This script helps test and debug Firebase sync on Android

echo "🔥 Android Firebase Sync Test"
echo "=============================="
echo ""

# Check if device is connected
if ! adb devices | grep -q "device$"; then
    echo "❌ No Android device connected!"
    echo "   Connect a device or start an emulator"
    exit 1
fi

echo "✅ Android device connected"
echo ""

# Install the app
echo "📦 Installing app..."
./gradlew :androidApp:installDebug

if [ $? -ne 0 ]; then
    echo "❌ Failed to install app"
    exit 1
fi

echo "✅ App installed"
echo ""

# Clear app data (optional - uncomment if needed)
# echo "🗑️  Clearing app data..."
# adb shell pm clear com.eunio.healthapp.android

# Start the app
echo "🚀 Starting app..."
adb shell am start -n com.eunio.healthapp.android/.MainActivity

echo "✅ App started"
echo ""

# Wait a moment for app to initialize
sleep 2

echo "📊 Watching logs..."
echo "   Press Ctrl+C to stop"
echo ""
echo "Looking for:"
echo "  - EunioApplication: Firebase initialization"
echo "  - FirebaseDiagnostics: Diagnostic report"
echo "  - LogRepository: Save operations"
echo "  - FirestoreService.Android: Firebase operations"
echo ""
echo "─────────────────────────────────────────────────────────────────"
echo ""

# Watch logs with color highlighting
adb logcat -c  # Clear logcat
adb logcat | grep --line-buffered -E "(EunioApplication|FirebaseDiagnostics|LogRepository|FirestoreService\.Android|SAVE_DAILY_LOG|GET_DAILY_LOG)" | while read line; do
    if echo "$line" | grep -q "ERROR"; then
        echo -e "\033[0;31m$line\033[0m"  # Red for errors
    elif echo "$line" | grep -q "SUCCESS"; then
        echo -e "\033[0;32m$line\033[0m"  # Green for success
    elif echo "$line" | grep -q "START"; then
        echo -e "\033[0;36m$line\033[0m"  # Cyan for start
    else
        echo "$line"
    fi
done
