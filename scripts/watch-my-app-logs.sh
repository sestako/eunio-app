#!/bin/bash

echo "=========================================="
echo "Watching ONLY Your App Logs"
echo "=========================================="
echo ""
echo "Package: com.eunio.healthapp"
echo "Press Ctrl+C to stop"
echo ""

# Clear old logs
adb logcat -c

echo "Waiting for app to start..."
echo ""

# Watch only your app's logs
adb logcat | grep "com.eunio.healthapp"
