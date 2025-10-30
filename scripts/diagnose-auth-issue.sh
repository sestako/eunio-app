#!/bin/bash

# Firebase Authentication Diagnostic Script
# This script helps diagnose login issues

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Firebase Auth Diagnostic Tool${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Check if device/emulator is connected
echo -e "${BLUE}1. Checking Android device connection${NC}"
echo "-------------------------------------------"

if adb devices | grep -q "device$"; then
    echo -e "${GREEN}✓${NC} Android device/emulator connected"
    DEVICE_CONNECTED=true
else
    echo -e "${RED}✗${NC} No Android device/emulator connected"
    echo "Please connect a device or start an emulator"
    DEVICE_CONNECTED=false
fi

echo ""

if [ "$DEVICE_CONNECTED" = true ]; then
    echo -e "${BLUE}2. Checking if app is installed${NC}"
    echo "-------------------------------------------"
    
    if adb shell pm list packages | grep -q "com.eunio.healthapp.android"; then
        echo -e "${GREEN}✓${NC} App is installed"
        
        # Get app version
        VERSION=$(adb shell dumpsys package com.eunio.healthapp.android | grep versionName | head -1 | awk '{print $1}')
        echo "  Version: $VERSION"
    else
        echo -e "${YELLOW}⚠${NC} App is not installed"
        echo "  Install with: ./gradlew :androidApp:installDebug"
    fi
    
    echo ""
    
    echo -e "${BLUE}3. Checking Firebase configuration${NC}"
    echo "-------------------------------------------"
    
    # Check if google-services.json exists
    if [ -f "androidApp/google-services.json" ]; then
        echo -e "${GREEN}✓${NC} google-services.json found"
        
        # Extract project ID
        PROJECT_ID=$(grep -o '"project_id": "[^"]*"' androidApp/google-services.json | cut -d'"' -f4)
        echo "  Firebase Project ID: $PROJECT_ID"
        
        # Check if Firebase Auth is enabled in the config
        if grep -q "firebase_auth" androidApp/google-services.json; then
            echo -e "${GREEN}✓${NC} Firebase Auth configuration found"
        else
            echo -e "${YELLOW}⚠${NC} Firebase Auth configuration not found in google-services.json"
        fi
    else
        echo -e "${RED}✗${NC} google-services.json not found"
    fi
    
    echo ""
    
    echo -e "${BLUE}4. Checking app logs for auth errors${NC}"
    echo "-------------------------------------------"
    echo "Clearing logcat and monitoring for auth-related logs..."
    echo "Press Ctrl+C to stop monitoring"
    echo ""
    
    # Clear logcat
    adb logcat -c
    
    # Monitor logcat for auth-related logs
    adb logcat -s AuthService:* AuthViewModel:* FirebaseAuth:* AndroidAuthService:* MainActivity:* | while read line; do
        if echo "$line" | grep -qi "error\|exception\|fail"; then
            echo -e "${RED}$line${NC}"
        elif echo "$line" | grep -qi "success\|signed in"; then
            echo -e "${GREEN}$line${NC}"
        elif echo "$line" | grep -qi "warn"; then
            echo -e "${YELLOW}$line${NC}"
        else
            echo "$line"
        fi
    done
fi

echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Diagnostic Instructions${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo "To diagnose the login issue:"
echo ""
echo "1. Make sure the app is running on your device/emulator"
echo "2. Try to log in with test credentials"
echo "3. Watch the logs above for error messages"
echo ""
echo "Common issues:"
echo ""
echo "  • ${YELLOW}Network Error${NC}: Check internet connection"
echo "  • ${YELLOW}Invalid Credentials${NC}: Verify email/password in Firebase Console"
echo "  • ${YELLOW}User Not Found${NC}: Create user in Firebase Console first"
echo "  • ${YELLOW}Firebase Not Initialized${NC}: Check google-services.json"
echo ""
echo "Test credentials:"
echo "  Email: demo@eunio.com"
echo "  Password: demo123"
echo ""
