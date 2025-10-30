#!/bin/bash

# Offline Mode and Local Persistence Test Script
# This script helps automate testing of offline functionality on Android and iOS

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test configuration
TEST_USER_ID="offline-test-user"
TEST_DATE=$(date +%Y-%m-%d)

echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║   Offline Mode and Local Persistence Test Suite          ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Function to print test header
print_test_header() {
    echo -e "\n${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
}

# Function to print test result
print_result() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓ PASS${NC}: $2"
    else
        echo -e "${RED}✗ FAIL${NC}: $2"
    fi
}

# Function to check if Android device is connected
check_android_device() {
    if ! command -v adb &> /dev/null; then
        echo -e "${RED}✗ ADB not found${NC}. Please install Android SDK Platform Tools."
        return 1
    fi
    
    DEVICE_COUNT=$(adb devices | grep -v "List" | grep "device$" | wc -l)
    if [ "$DEVICE_COUNT" -eq 0 ]; then
        echo -e "${RED}✗ No Android device connected${NC}"
        return 1
    fi
    
    echo -e "${GREEN}✓ Android device connected${NC}"
    return 0
}

# Function to check if iOS simulator is available
check_ios_simulator() {
    if ! command -v xcrun &> /dev/null; then
        echo -e "${RED}✗ Xcode not found${NC}. iOS tests will be skipped."
        return 1
    fi
    
    # Check if any simulator is booted
    BOOTED_SIM=$(xcrun simctl list devices | grep "Booted" | wc -l)
    if [ "$BOOTED_SIM" -eq 0 ]; then
        echo -e "${YELLOW}⚠ No iOS simulator booted${NC}. Attempting to boot iPhone 15..."
        xcrun simctl boot "iPhone 15" 2>/dev/null || {
            echo -e "${RED}✗ Failed to boot iOS simulator${NC}"
            return 1
        }
        sleep 5
    fi
    
    echo -e "${GREEN}✓ iOS simulator available${NC}"
    return 0
}

# Function to run Kotlin tests
run_kotlin_tests() {
    print_test_header "Running Kotlin Multiplatform Offline Tests"
    
    echo "Running offline mode service tests..."
    ./gradlew :shared:cleanAllTests :shared:allTests --tests "*OfflineModeServiceTest" || {
        print_result 1 "Offline mode service tests"
        return 1
    }
    print_result 0 "Offline mode service tests"
    
    echo ""
    echo "Running offline functionality integration tests..."
    ./gradlew :shared:cleanAllTests :shared:allTests --tests "*OfflineFunctionalityTest" || {
        print_result 1 "Offline functionality tests"
        return 1
    }
    print_result 0 "Offline functionality tests"
    
    echo ""
    echo "Running log repository sync tests..."
    ./gradlew :shared:cleanAllTests :shared:allTests --tests "*LogRepositorySyncRetryTest" || {
        print_result 1 "Log repository sync tests"
        return 1
    }
    print_result 0 "Log repository sync tests"
    
    return 0
}

# Function to test Android offline functionality
test_android_offline() {
    print_test_header "Testing Android Offline Functionality"
    
    if ! check_android_device; then
        echo -e "${YELLOW}⚠ Skipping Android tests - no device available${NC}"
        return 1
    fi
    
    PACKAGE_NAME="com.eunio.healthapp"
    
    # Check if app is installed
    if ! adb shell pm list packages | grep -q "$PACKAGE_NAME"; then
        echo -e "${YELLOW}⚠ App not installed. Installing...${NC}"
        ./gradlew :androidApp:installDebug || {
            echo -e "${RED}✗ Failed to install app${NC}"
            return 1
        }
    fi
    
    echo ""
    echo "Testing offline data storage..."
    echo "1. Monitoring logs for offline operations..."
    
    # Start log monitoring in background
    adb logcat -c
    adb logcat | grep -i "offline\|sync\|dailylog" > /tmp/android_offline_test.log &
    LOGCAT_PID=$!
    
    echo "2. Please perform the following manual steps:"
    echo "   a. Open the Eunio Health App"
    echo "   b. Enable Airplane Mode"
    echo "   c. Create a daily log entry"
    echo "   d. Save the entry"
    echo "   e. Close and reopen the app"
    echo "   f. Verify the entry is still there"
    echo "   g. Disable Airplane Mode"
    echo "   h. Wait for sync to complete"
    echo ""
    read -p "Press Enter when manual testing is complete..."
    
    # Stop log monitoring
    kill $LOGCAT_PID 2>/dev/null || true
    
    # Analyze logs
    echo ""
    echo "Analyzing logs..."
    
    if grep -q "saveDailyLog" /tmp/android_offline_test.log; then
        print_result 0 "Daily log save operation detected"
    else
        print_result 1 "Daily log save operation not detected"
    fi
    
    if grep -q "PENDING" /tmp/android_offline_test.log; then
        print_result 0 "Pending sync operation detected"
    else
        print_result 1 "Pending sync operation not detected"
    fi
    
    if grep -q "syncPendingOperations\|SYNC_SUCCESS" /tmp/android_offline_test.log; then
        print_result 0 "Sync operation detected"
    else
        print_result 1 "Sync operation not detected"
    fi
    
    # Check local database
    echo ""
    echo "Checking local database..."
    DB_PATH="/data/data/$PACKAGE_NAME/databases"
    if adb shell "run-as $PACKAGE_NAME ls $DB_PATH" 2>/dev/null | grep -q "eunio_health.db"; then
        print_result 0 "Local database exists"
        
        # Try to get database size
        DB_SIZE=$(adb shell "run-as $PACKAGE_NAME stat -c%s $DB_PATH/eunio_health.db" 2>/dev/null || echo "0")
        if [ "$DB_SIZE" -gt 0 ]; then
            print_result 0 "Local database has data (size: $DB_SIZE bytes)"
        else
            print_result 1 "Local database is empty"
        fi
    else
        print_result 1 "Local database not found"
    fi
    
    return 0
}

# Function to test iOS offline functionality
test_ios_offline() {
    print_test_header "Testing iOS Offline Functionality"
    
    if ! check_ios_simulator; then
        echo -e "${YELLOW}⚠ Skipping iOS tests - no simulator available${NC}"
        return 1
    fi
    
    BUNDLE_ID="com.eunio.healthapp"
    
    # Check if app is installed
    if ! xcrun simctl listapps booted | grep -q "$BUNDLE_ID"; then
        echo -e "${YELLOW}⚠ App not installed on simulator${NC}"
        echo "Please build and install the app first:"
        echo "  xcodebuild -workspace iosApp/iosApp.xcworkspace -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 15' build"
        return 1
    fi
    
    echo ""
    echo "Testing offline data storage..."
    echo "1. Please perform the following manual steps:"
    echo "   a. Open the Eunio Health App on simulator"
    echo "   b. Enable Airplane Mode (Cmd+Shift+H, then Settings)"
    echo "   c. Create a daily log entry"
    echo "   d. Save the entry"
    echo "   e. Close and reopen the app"
    echo "   f. Verify the entry is still there"
    echo "   g. Disable Airplane Mode"
    echo "   h. Wait for sync to complete"
    echo ""
    read -p "Press Enter when manual testing is complete..."
    
    # Check app container for database
    echo ""
    echo "Checking local storage..."
    
    # Get app container path
    CONTAINER_PATH=$(xcrun simctl get_app_container booted "$BUNDLE_ID" data 2>/dev/null || echo "")
    
    if [ -n "$CONTAINER_PATH" ]; then
        print_result 0 "App container found: $CONTAINER_PATH"
        
        # Check for database files
        if find "$CONTAINER_PATH" -name "*.db" -o -name "*.sqlite" | grep -q .; then
            print_result 0 "Local database files found"
            
            # List database files
            echo "Database files:"
            find "$CONTAINER_PATH" -name "*.db" -o -name "*.sqlite" | while read -r db_file; do
                DB_SIZE=$(stat -f%z "$db_file" 2>/dev/null || echo "0")
                echo "  - $(basename "$db_file"): $DB_SIZE bytes"
            done
        else
            print_result 1 "No database files found"
        fi
    else
        print_result 1 "App container not found"
    fi
    
    return 0
}

# Function to run unit tests
test_unit_tests() {
    print_test_header "Running Unit Tests"
    
    echo "Building shared module..."
    ./gradlew :shared:build || {
        echo -e "${RED}✗ Failed to build shared module${NC}"
        return 1
    }
    
    echo ""
    run_kotlin_tests || {
        echo -e "${RED}✗ Some unit tests failed${NC}"
        return 1
    }
    
    return 0
}

# Main test execution
main() {
    echo "Test Date: $TEST_DATE"
    echo "Test User: $TEST_USER_ID"
    echo ""
    
    # Parse command line arguments
    RUN_UNIT_TESTS=true
    RUN_ANDROID_TESTS=false
    RUN_IOS_TESTS=false
    
    if [ $# -eq 0 ]; then
        # No arguments - run all tests
        RUN_ANDROID_TESTS=true
        RUN_IOS_TESTS=true
    else
        # Parse arguments
        for arg in "$@"; do
            case $arg in
                --unit-only)
                    RUN_ANDROID_TESTS=false
                    RUN_IOS_TESTS=false
                    ;;
                --android)
                    RUN_ANDROID_TESTS=true
                    ;;
                --ios)
                    RUN_IOS_TESTS=true
                    ;;
                --no-unit)
                    RUN_UNIT_TESTS=false
                    ;;
                --help)
                    echo "Usage: $0 [OPTIONS]"
                    echo ""
                    echo "Options:"
                    echo "  --unit-only    Run only unit tests (no platform tests)"
                    echo "  --android      Run Android platform tests"
                    echo "  --ios          Run iOS platform tests"
                    echo "  --no-unit      Skip unit tests"
                    echo "  --help         Show this help message"
                    echo ""
                    echo "Examples:"
                    echo "  $0                    # Run all tests"
                    echo "  $0 --unit-only        # Run only unit tests"
                    echo "  $0 --android          # Run unit tests + Android tests"
                    echo "  $0 --ios --no-unit    # Run only iOS tests"
                    exit 0
                    ;;
                *)
                    echo -e "${RED}Unknown option: $arg${NC}"
                    echo "Use --help for usage information"
                    exit 1
                    ;;
            esac
        done
    fi
    
    # Track test results
    TESTS_PASSED=0
    TESTS_FAILED=0
    
    # Run unit tests
    if [ "$RUN_UNIT_TESTS" = true ]; then
        if test_unit_tests; then
            ((TESTS_PASSED++))
        else
            ((TESTS_FAILED++))
        fi
    fi
    
    # Run Android tests
    if [ "$RUN_ANDROID_TESTS" = true ]; then
        if test_android_offline; then
            ((TESTS_PASSED++))
        else
            ((TESTS_FAILED++))
        fi
    fi
    
    # Run iOS tests
    if [ "$RUN_IOS_TESTS" = true ]; then
        if test_ios_offline; then
            ((TESTS_PASSED++))
        else
            ((TESTS_FAILED++))
        fi
    fi
    
    # Print summary
    echo ""
    echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║                     Test Summary                          ║${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "Tests Passed: ${GREEN}$TESTS_PASSED${NC}"
    echo -e "Tests Failed: ${RED}$TESTS_FAILED${NC}"
    echo ""
    
    if [ $TESTS_FAILED -eq 0 ]; then
        echo -e "${GREEN}✓ All tests passed!${NC}"
        echo ""
        echo "Next steps:"
        echo "1. Review the test plan: .kiro/specs/4-kotlin-xcode-ios26-upgrade/task-22-offline-mode-test-plan.md"
        echo "2. Complete manual testing scenarios on physical devices"
        echo "3. Verify cross-platform data consistency"
        echo "4. Document any issues found"
        return 0
    else
        echo -e "${RED}✗ Some tests failed${NC}"
        echo ""
        echo "Please review the test output above and:"
        echo "1. Fix any failing unit tests"
        echo "2. Verify platform-specific implementations"
        echo "3. Check logs for detailed error messages"
        return 1
    fi
}

# Run main function
main "$@"
