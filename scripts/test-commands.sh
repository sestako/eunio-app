#!/bin/bash

# Quick Test Commands for Authentication Testing

echo "🧪 Authentication Test Commands"
echo "================================"
echo ""

# Android Commands
echo "📱 ANDROID COMMANDS:"
echo ""
echo "1. Launch App:"
echo "   adb shell am start -n com.eunio.healthapp.android/.MainActivity"
echo ""
echo "2. Watch Logs:"
echo "   adb logcat | grep -E '(FirebaseAuth|AuthService|AuthViewModel)'"
echo ""
echo "3. Force Stop App:"
echo "   adb shell am force-stop com.eunio.healthapp.android"
echo ""
echo "4. Check Crashes:"
echo "   adb logcat | grep -E '(FATAL|AndroidRuntime)'"
echo ""
echo "5. Clear App Data (reset):"
echo "   adb shell pm clear com.eunio.healthapp.android"
echo ""

# iOS Commands
echo "📱 iOS COMMANDS:"
echo ""
echo "1. Open Xcode:"
echo "   open iosApp/iosApp.xcodeproj"
echo ""
echo "2. Build and Run:"
echo "   Press Cmd+R in Xcode"
echo ""
echo "3. View Console:"
echo "   Cmd+Shift+Y in Xcode"
echo ""

# Firebase Console
echo "🔥 FIREBASE CONSOLE:"
echo ""
echo "1. Authentication Users:"
echo "   https://console.firebase.google.com/project/eunio-c4dde/authentication/users"
echo ""
echo "2. Firestore Database:"
echo "   https://console.firebase.google.com/project/eunio-c4dde/firestore"
echo ""

# Test Accounts
echo "👤 TEST ACCOUNTS:"
echo ""
echo "Android Test User:"
echo "   Email: test1@example.com"
echo "   Password: password123"
echo ""
echo "iOS Test User:"
echo "   Email: test2@example.com"
echo "   Password: password123"
echo ""

# Quick Actions
echo "⚡ QUICK ACTIONS:"
echo ""
echo "Run this script with an action:"
echo ""
echo "./test-commands.sh launch-android    # Launch Android app"
echo "./test-commands.sh logs-android      # Watch Android logs"
echo "./test-commands.sh stop-android      # Stop Android app"
echo "./test-commands.sh open-ios          # Open iOS in Xcode"
echo "./test-commands.sh firebase          # Open Firebase Console"
echo ""

# Handle arguments
case "$1" in
    launch-android)
        echo "🚀 Launching Android app..."
        adb shell am start -n com.eunio.healthapp.android/.MainActivity
        ;;
    logs-android)
        echo "📋 Watching Android logs..."
        adb logcat | grep -E "(FirebaseAuth|AuthService|AuthViewModel)"
        ;;
    stop-android)
        echo "🛑 Stopping Android app..."
        adb shell am force-stop com.eunio.healthapp.android
        ;;
    open-ios)
        echo "📱 Opening iOS in Xcode..."
        open iosApp/iosApp.xcodeproj
        ;;
    firebase)
        echo "🔥 Opening Firebase Console..."
        open "https://console.firebase.google.com/project/eunio-c4dde/authentication/users"
        ;;
    *)
        if [ -n "$1" ]; then
            echo "❌ Unknown command: $1"
            echo ""
        fi
        ;;
esac
