#!/bin/bash

echo "🔍 Monitoring Android save operations..."
echo "Click the 'Save Log' button in the app now..."
echo ""

# Monitor all relevant logs for save operations
adb logcat -c  # Clear logs
adb logcat | grep -E "(saveLog|isSaving|SAVE_DAILY_LOG|SaveDailyLogUseCase|LogRepository.*save)" --line-buffered
