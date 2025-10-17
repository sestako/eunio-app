#!/bin/bash

# Comprehensive Test Suite for Eunio Health App
# This script tests both Android and iOS platforms

set -e  # Exit on any error

echo "🚀 Starting Comprehensive Test Suite for Eunio Health App"
echo "=========================================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Test counters
TESTS_PASSED=0
TESTS_FAILED=0

# Function to run a test
run_test() {
    local test_name="$1"
    local test_command="$2"
    
    print_status "Running: $test_name"
    
    if eval "$test_command" > /dev/null 2>&1; then
        print_success "$test_name"
        ((TESTS_PASSED++))
        return 0
    else
        print_error "$test_name"
        ((TESTS_FAILED++))
        return 1
    fi
}

# 1. Project Structure Verification
echo -e "\n${BLUE}Phase 1: Project Structure Verification${NC}"
echo "----------------------------------------"

run_test "Gradle wrapper exists" "test -f ./gradlew"
run_test "Android app module exists" "test -d ./androidApp"
run_test "iOS app module exists" "test -d ./iosApp"
run_test "Shared module exists" "test -d ./shared"
run_test "Firebase configuration exists" "test -f ./firebase.json"
run_test "Android manifest exists" "test -f ./androidApp/src/androidMain/AndroidManifest.xml"
run_test "iOS Info.plist exists" "test -f ./iosApp/iosApp/Info.plist"

# 2. Build System Tests
echo -e "\n${BLUE}Phase 2: Build System Tests${NC}"
echo "----------------------------"

run_test "Gradle clean" "./gradlew clean"
run_test "Shared module compilation" "./gradlew :shared:compileKotlinMetadata"
run_test "Android module compilation" "./gradlew :androidApp:compileDebugKotlinAndroid"

# 3. Code Quality Tests
echo -e "\n${BLUE}Phase 3: Code Quality Tests${NC}"
echo "---------------------------"

run_test "Lint checks" "./gradlew :shared:lintDebug"
run_test "Kotlin compilation (Android)" "./gradlew :shared:compileDebugKotlinAndroid"
run_test "Kotlin compilation (iOS)" "./gradlew :shared:compileDebugKotlinIosSimulatorArm64"

# 4. Unit Tests
echo -e "\n${BLUE}Phase 4: Unit Tests${NC}"
echo "-------------------"

run_test "Shared module unit tests" "./gradlew :shared:testDebugUnitTest"
run_test "Android unit tests" "./gradlew :androidApp:testDebugUnitTest"

# 5. Integration Tests
echo -e "\n${BLUE}Phase 5: Integration Tests${NC}"
echo "--------------------------"

run_test "Database schema validation" "./gradlew :shared:testDebugUnitTest --tests '*DatabaseSchemaIntegrationTest*'"
run_test "Repository integration tests" "./gradlew :shared:testDebugUnitTest --tests '*RepositoryTest*'"
run_test "Use case integration tests" "./gradlew :shared:testDebugUnitTest --tests '*UseCaseTest*'"

# 6. Performance Tests
echo -e "\n${BLUE}Phase 6: Performance Tests${NC}"
echo "--------------------------"

run_test "Unit converter performance" "./gradlew :shared:testDebugUnitTest --tests '*UnitConverterPerformanceTest*'"
run_test "Unit system performance" "./gradlew :shared:testDebugUnitTest --tests '*UnitSystemPerformanceTest*'"

# 7. Security Tests
echo -e "\n${BLUE}Phase 7: Security Tests${NC}"
echo "-----------------------"

run_test "Data encryption tests" "./gradlew :shared:testDebugUnitTest --tests '*DataEncryptionTest*'"
run_test "Security validation tests" "./gradlew :shared:testDebugUnitTest --tests '*SecurityTest*'"

# 8. Platform-Specific Tests
echo -e "\n${BLUE}Phase 8: Platform-Specific Tests${NC}"
echo "--------------------------------"

# Android specific tests
run_test "Android build" "./gradlew :androidApp:assembleDebug"
run_test "Android lint" "./gradlew :androidApp:lintDebug"

# iOS specific tests
if command -v xcodebuild &> /dev/null; then
    run_test "iOS project validation" "xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -sdk iphonesimulator -destination 'platform=iOS Simulator,name=iPhone 15,OS=latest' clean build CODE_SIGNING_ALLOWED=NO"
else
    print_warning "Xcode not available, skipping iOS build test"
fi

# 9. Firebase Integration Tests
echo -e "\n${BLUE}Phase 9: Firebase Integration Tests${NC}"
echo "-----------------------------------"

run_test "Firebase functions compilation" "cd functions && npm install && npm run build"
run_test "Firebase functions tests" "cd functions && npm test"
run_test "Firestore rules validation" "test -f ./firestore.rules"

# 10. Final Build Test
echo -e "\n${BLUE}Phase 10: Final Build Test${NC}"
echo "---------------------------"

run_test "Complete project build" "./gradlew build"

# Summary
echo -e "\n${BLUE}Test Summary${NC}"
echo "============"
echo -e "Tests Passed: ${GREEN}$TESTS_PASSED${NC}"
echo -e "Tests Failed: ${RED}$TESTS_FAILED${NC}"
echo -e "Total Tests: $((TESTS_PASSED + TESTS_FAILED))"

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "\n${GREEN}🎉 All tests passed! The project is ready for development.${NC}"
    exit 0
else
    echo -e "\n${RED}❌ Some tests failed. Please review the issues above.${NC}"
    exit 1
fi