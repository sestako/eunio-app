#!/bin/bash

echo "📱 Capturing ALL app logs..."
echo "================================"
echo ""
echo "👉 Now click 'Save Log' button in the app"
echo ""
echo "Monitoring..."
echo ""

# Clear logs first
adb logcat -c

# Capture ALL logs from your app (not filtered)
adb logcat | grep "com.eunio.healthapp.android"
