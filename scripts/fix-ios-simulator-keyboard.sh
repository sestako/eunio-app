#!/bin/bash

# Fix iOS Simulator Keyboard Issue
# This script helps reconnect the hardware keyboard to the simulator

echo "🔧 Fixing iOS Simulator Keyboard..."
echo ""
echo "Option 1: Using xcrun (automatic)"
echo "=================================="
xcrun simctl spawn booted defaults write com.apple.iphonesimulator ConnectHardwareKeyboard 1
echo "✅ Hardware keyboard enabled"
echo ""
echo "Option 2: Manual fix in Simulator"
echo "=================================="
echo "1. In Simulator menu: I/O → Keyboard → Connect Hardware Keyboard"
echo "2. Or press: Cmd+Shift+K"
echo ""
echo "Option 3: Restart Simulator"
echo "==========================="
echo "1. Close the Simulator app completely"
echo "2. Reopen and run your app again"
echo ""
echo "If none of these work, try:"
echo "- Restart Xcode"
echo "- Restart your Mac"
