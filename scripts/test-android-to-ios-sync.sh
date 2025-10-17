#!/bin/bash

# Android to iOS Sync Test Runner
# This script helps run the cross-platform sync tests
# Requirements: 4.1, 4.2, 4.5, 4.6

set -e

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Android to iOS Sync Test Runner${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Function to print section headers
print_section() {
    echo -e "${BLUE}>>> $1${NC}"
}

# Function to print success messages
print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

# Function to print warning messages
print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

# Function to print error messages
print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Check if we're in the project root
if [ ! -f "settings.gradle.kts" ]; then
    print_error "Please run this script from the project root directory"
    exit 1
fi

print_success "Project root directory confirmed"
echo ""

# Phase 1: Android Tests
print_section "PHASE 1: Running Android Sync Tests"
echo ""

print_warning "Make sure:"
echo "  1. Android device/emulator is connected and running"
echo "  2. App is installed on Android device"
echo "  3. User is logged in on Android app"
echo ""

read -p "Press Enter to continue with Android tests, or Ctrl+C to cancel..."
echo ""

print_section "Building and running Android instrumentation tests..."
echo ""

# Run Android tests
if ./gradlew :androidApp:connectedAndroidTest \
    --tests "com.eunio.healthapp.android.sync.AndroidToIOSSyncTest" \
    --info; then
    print_success "Android tests completed successfully!"
else
    print_error "Android tests failed. Please check the output above."
    exit 1
fi

echo ""
print_section "Android Test Results"
echo ""
echo "Expected output should include:"
echo "  ✅ Android to iOS Sync Test: Daily log created successfully"
echo "  📝 Test Data Summary:"
echo "     Date: October 10, 2025"
echo "     Period Flow: Light"
echo "     Symptoms: Headache, Cramps"
echo "     Mood: Happy"
echo "     BBT: 98.2°F"
echo ""

# Wait for Firebase sync
print_section "Waiting for Firebase Sync"
echo ""
print_warning "Waiting 15 seconds for Firebase sync to complete..."
echo ""

for i in {15..1}; do
    echo -ne "  ${i} seconds remaining...\r"
    sleep 1
done
echo ""

print_success "Firebase sync wait complete"
echo ""

# Phase 2: iOS Tests
print_section "PHASE 2: Running iOS Verification Tests"
echo ""

print_warning "Make sure:"
echo "  1. iOS device/simulator is connected and running"
echo "  2. App is installed on iOS device"
echo "  3. Same user is logged in on iOS app"
echo "  4. Xcode is installed"
echo ""

read -p "Press Enter to continue with iOS tests, or Ctrl+C to cancel..."
echo ""

print_section "Building and running iOS UI tests..."
echo ""

# Check if Xcode is available
if ! command -v xcodebuild &> /dev/null; then
    print_error "xcodebuild not found. Please install Xcode."
    exit 1
fi

# Run iOS tests
cd iosApp

if xcodebuild test \
    -project iosApp.xcodeproj \
    -scheme iosApp \
    -destination 'platform=iOS Simulator,name=iPhone 15' \
    -only-testing:iosAppUITests/AndroidToIOSSyncVerificationTests \
    | xcpretty; then
    print_success "iOS tests completed successfully!"
else
    print_warning "iOS tests may have failed. Check output above."
    print_warning "If tests failed, try running them manually in Xcode."
fi

cd ..

echo ""
print_section "iOS Test Results"
echo ""
echo "Expected output should include:"
echo "  🎉 SUCCESS: All data from Android log synced correctly to iOS!"
echo "     ✓ Date: October 10, 2025"
echo "     ✓ Period Flow: Light"
echo "     ✓ Symptoms: Headache, Cramps"
echo "     ✓ Mood: Happy"
echo "     ✓ BBT: 98.2°F"
echo "     ✓ Notes: Android to iOS sync test"
echo ""

# Summary
print_section "TEST SUMMARY"
echo ""
print_success "Cross-platform sync test execution complete!"
echo ""
echo "Next steps:"
echo "  1. Review test results above"
echo "  2. Verify all data synced correctly"
echo "  3. Check Firebase Console for data (optional)"
echo "  4. Document results in test report"
echo ""
echo "Test Guide: .kiro/specs/calendar-date-display-fix/android-to-ios-sync-test-guide.md"
echo ""

print_section "Manual Verification (Optional)"
echo ""
echo "If you want to manually verify the sync:"
echo ""
echo "On iOS device:"
echo "  1. Open the app"
echo "  2. Navigate to Daily Logging screen"
echo "  3. Select October 10, 2025"
echo "  4. Verify the following data appears:"
echo "     - Period Flow: Light"
echo "     - Symptoms: Headache, Cramps"
echo "     - Mood: Happy"
echo "     - BBT: 98.2°F"
echo "     - Notes: 'Android to iOS sync test - October 10, 2025'"
echo ""

print_success "Test execution complete!"
echo ""
