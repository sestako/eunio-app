#!/bin/bash

echo "🧪 Testing Save Operation"
echo "=========================="
echo ""
echo "1. Clearing logs..."
adb logcat -c
sleep 1

echo "2. Starting comprehensive log monitor..."
echo ""
echo "   👉 Now click 'Save Log' button in the app"
echo "   (Monitoring for 15 seconds...)"
echo ""

# Monitor multiple patterns simultaneously
timeout 15 adb logcat | grep -E "(saveLog|SAVE_DAILY_LOG|isSaving|SaveDailyLogUseCase|LogRepository.*save|✅|❌|Exception|Error|userId.*null|Please log in)" --line-buffered

echo ""
echo "=========================="
echo "✅ Test complete!"
echo ""
echo "Analysis:"
echo "- If you see 'saveLog() called': ViewModel received the click"
echo "- If you see 'SAVE_DAILY_LOG_START': Firebase operation started"
echo "- If you see 'SAVE_DAILY_LOG_SUCCESS': Save completed successfully"
echo "- If you see 'Please log in': Authentication issue"
echo "- If you see 'Exception' or 'Error': Check the error message"
echo "- If you see nothing: The button click isn't reaching the ViewModel"
