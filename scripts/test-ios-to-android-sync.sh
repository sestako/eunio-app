#!/bin/bash

# iOS to Android Sync Test Script
# This script helps coordinate testing of data synchronization from iOS to Android

set -e

echo "=========================================="
echo "iOS to Android Sync Test"
echo "=========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Test configuration
TEST_DATE="October 10, 2025"
TEST_SYMPTOMS="Cramps"
TEST_MOOD="Calm"
TEST_BBT="98.4"
TEST_NOTES="iOS to Android sync test - $(date +%Y-%m-%d\ %H:%M:%S)"

echo -e "${BLUE}Test Configuration:${NC}"
echo "  Date: $TEST_DATE"
echo "  Symptoms: $TEST_SYMPTOMS"
echo "  Mood: $TEST_MOOD"
echo "  BBT: $TEST_BBT"
echo "  Notes: $TEST_NOTES"
echo ""

# Function to wait for user confirmation
wait_for_confirmation() {
    local message=$1
    echo -e "${YELLOW}$message${NC}"
    read -p "Press Enter to continue..."
    echo ""
}

# Function to display checklist item
checklist_item() {
    local item=$1
    echo -e "${GREEN}✓${NC} $item"
}

echo "=========================================="
echo "PART 1: Create Daily Log on iOS"
echo "=========================================="
echo ""

echo "Step 1: Launch iOS App"
echo "  • Open the iOS app on your device/simulator"
echo "  • Ensure you're logged in with the test account"
echo "  • Wait for the app to fully load"
echo ""
wait_for_confirmation "Have you launched the iOS app and logged in?"

echo "Step 2: Navigate to Daily Logging"
echo "  • Tap on the 'Daily Logging' tab"
echo "  • Wait for the screen to load"
echo ""
wait_for_confirmation "Are you on the Daily Logging screen?"

echo "Step 3: Select $TEST_DATE"
echo "  • Tap on the date picker"
echo "  • Navigate to October 2025"
echo "  • Select October 10, 2025"
echo ""
wait_for_confirmation "Have you selected $TEST_DATE?"

echo "Step 4: Enter Test Data"
echo "  Please enter the following data:"
echo ""
echo "  Symptoms: $TEST_SYMPTOMS"
echo "  Mood: $TEST_MOOD"
echo "  BBT: $TEST_BBT"
echo "  Notes: $TEST_NOTES"
echo ""
wait_for_confirmation "Have you entered all the test data?"

echo "Step 5: Save the Log"
echo "  • Tap the 'Save' button"
echo "  • Wait for save confirmation"
echo "  • Look for success message"
echo ""
wait_for_confirmation "Did the log save successfully?"

echo "Step 6: Verify Save on iOS"
echo "  • Navigate to a different date"
echo "  • Navigate back to October 10, 2025"
echo "  • Verify the data is still present"
echo ""
wait_for_confirmation "Is the data still present on iOS?"

echo ""
echo "=========================================="
echo "PART 2: Wait for Firebase Sync"
echo "=========================================="
echo ""

echo -e "${BLUE}Waiting for Firebase sync...${NC}"
echo "This will take approximately 10 seconds"
echo ""

for i in {10..1}; do
    echo -ne "  Syncing... $i seconds remaining\r"
    sleep 1
done
echo -e "\n"

echo -e "${GREEN}✓ Sync wait period complete${NC}"
echo ""

echo "Optional: Check Firebase Console"
echo "  • Open Firebase Console"
echo "  • Navigate to Firestore Database"
echo "  • Look for the daily log document"
echo "  • Verify it contains the correct data"
echo ""
read -p "Press Enter to continue to Android verification..."
echo ""

echo "=========================================="
echo "PART 3: Verify on Android"
echo "=========================================="
echo ""

echo "Step 8: Launch Android App"
echo "  • Open the Android app on your device/emulator"
echo "  • Ensure you're logged in with the SAME test account"
echo "  • Wait for the app to fully load"
echo ""
wait_for_confirmation "Have you launched the Android app with the same account?"

echo "Step 9: Navigate to Daily Logging"
echo "  • Tap on the 'Daily Logging' tab/button"
echo "  • Wait for the screen to load"
echo ""
wait_for_confirmation "Are you on the Daily Logging screen on Android?"

echo "Step 10: Select $TEST_DATE"
echo "  • Use the date navigation to select October 10, 2025"
echo "  • Verify the date displays correctly"
echo "  • Check that quick date selection shows October dates"
echo ""
wait_for_confirmation "Have you selected $TEST_DATE on Android?"

echo ""
echo "=========================================="
echo "VERIFICATION CHECKLIST"
echo "=========================================="
echo ""
echo "Please verify the following on Android:"
echo ""

# Verification checklist
verify_item() {
    local item=$1
    read -p "  [ ] $item (y/n): " response
    if [[ "$response" =~ ^[Yy]$ ]]; then
        echo -e "  ${GREEN}✓${NC} $item"
        return 0
    else
        echo -e "  ${RED}✗${NC} $item"
        return 1
    fi
}

all_passed=true

verify_item "Date displays as 'October 10, 2025' (not shifted)" || all_passed=false
verify_item "Symptoms field shows '$TEST_SYMPTOMS'" || all_passed=false
verify_item "Mood shows '$TEST_MOOD'" || all_passed=false
verify_item "BBT shows '$TEST_BBT'" || all_passed=false
verify_item "Notes contain 'iOS to Android sync test'" || all_passed=false
verify_item "No data corruption or missing fields" || all_passed=false
verify_item "No duplicate entries" || all_passed=false

echo ""
echo "=========================================="
echo "DATE INTEGRITY CHECK"
echo "=========================================="
echo ""

echo "Navigate to October 9, 2025 on Android"
read -p "  Is October 9 empty (unless you created a log there)? (y/n): " oct9
echo ""

echo "Navigate to October 11, 2025 on Android"
read -p "  Is October 11 empty (unless you created a log there)? (y/n): " oct11
echo ""

echo "Navigate back to October 10, 2025 on Android"
read -p "  Is the log still present on October 10? (y/n): " oct10
echo ""

if [[ "$oct9" =~ ^[Yy]$ ]] && [[ "$oct11" =~ ^[Yy]$ ]] && [[ "$oct10" =~ ^[Yy]$ ]]; then
    echo -e "${GREEN}✓ Date integrity verified - no date shifting${NC}"
else
    echo -e "${RED}✗ Date integrity issue detected${NC}"
    all_passed=false
fi

echo ""
echo "=========================================="
echo "TEST RESULTS"
echo "=========================================="
echo ""

if [ "$all_passed" = true ]; then
    echo -e "${GREEN}✓✓✓ ALL TESTS PASSED ✓✓✓${NC}"
    echo ""
    echo "iOS to Android sync is working correctly!"
    echo "  • Daily log created on iOS"
    echo "  • Log synced to Firebase"
    echo "  • Log appeared on Android with correct date"
    echo "  • All data fields intact"
    echo "  • No date shifting or corruption"
    echo ""
    echo "Requirements verified:"
    echo "  ✓ Requirement 4.3: iOS log syncs to Firebase with correct date"
    echo "  ✓ Requirement 4.4: Log displays on Android with correct date"
    echo "  ✓ Requirement 4.5: All log data remains intact"
    echo "  ✓ Requirement 4.6: October 10, 2025 date verified"
    echo ""
    echo "Next steps:"
    echo "  • Update task 5.2 status to complete in tasks.md"
    echo "  • Proceed to task 5.3 (Bidirectional updates)"
    echo ""
    exit 0
else
    echo -e "${RED}✗✗✗ SOME TESTS FAILED ✗✗✗${NC}"
    echo ""
    echo "Issues detected during iOS to Android sync test."
    echo ""
    echo "Troubleshooting steps:"
    echo "  1. Check Firebase Console for the document"
    echo "  2. Verify both apps use the same user account"
    echo "  3. Check network connectivity on both devices"
    echo "  4. Review Firebase security rules"
    echo "  5. Check app logs for sync errors"
    echo ""
    echo "See ios-to-android-sync-test-guide.md for detailed troubleshooting."
    echo ""
    exit 1
fi
