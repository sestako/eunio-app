#!/bin/bash

echo "🔍 Simple Save Test"
echo "==================="
echo ""

# Clear logs
adb logcat -c

echo "👉 Click 'Save Log' button NOW"
echo ""
echo "Watching for logs (15 seconds)..."
echo ""

# Watch for the specific log tags we know exist
timeout 15 adb logcat -s LogRepository:D FirestoreService.Android:D DailyLoggingViewModel:D System:W

echo ""
echo "==================="
echo "Done!"
