#!/bin/bash

# Task 5.1 Test Readiness Verification Script
# This script checks if all components are in place for iOS Firebase save testing

echo "🔍 Verifying iOS Firebase Save Test Readiness..."
echo ""

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check counter
CHECKS_PASSED=0
CHECKS_FAILED=0

# Function to check file exists
check_file() {
    if [ -f "$1" ]; then
        echo -e "${GREEN}✓${NC} $2"
        ((CHECKS_PASSED++))
        return 0
    else
        echo -e "${RED}✗${NC} $2 - File not found: $1"
        ((CHECKS_FAILED++))
        return 1
    fi
}

# Function to check directory exists
check_dir() {
    if [ -d "$1" ]; then
        echo -e "${GREEN}✓${NC} $2"
        ((CHECKS_PASSED++))
        return 0
    else
        echo -e "${RED}✗${NC} $2 - Directory not found: $1"
        ((CHECKS_FAILED++))
        return 1
    fi
}

# Function to check file contains text
check_content() {
    if grep -q "$2" "$1" 2>/dev/null; then
        echo -e "${GREEN}✓${NC} $3"
        ((CHECKS_PASSED++))
        return 0
    else
        echo -e "${RED}✗${NC} $3 - Not found in $1"
        ((CHECKS_FAILED++))
        return 1
    fi
}

echo "📦 Checking Core Components..."
echo "================================"

# Check Swift Bridge
check_file "iosApp/iosApp/Services/FirebaseIOSBridge.swift" "Swift Firebase Bridge exists"
check_content "iosApp/iosApp/Services/FirebaseIOSBridge.swift" "saveDailyLog" "Swift Bridge has saveDailyLog method"
check_content "iosApp/iosApp/Services/FirebaseIOSBridge.swift" "getDailyLog" "Swift Bridge has getDailyLog method"

# Check Kotlin Implementation
check_file "shared/src/iosMain/kotlin/com/eunio/healthapp/data/remote/FirestoreServiceImpl.ios.kt" "iOS FirestoreService implementation exists"
check_content "shared/src/iosMain/kotlin/com/eunio/healthapp/data/remote/FirestoreServiceImpl.ios.kt" "FirebaseNativeBridge" "FirestoreService uses Firebase bridge"

# Check Error Mapper
check_file "shared/src/iosMain/kotlin/com/eunio/healthapp/data/remote/firebase/FirebaseErrorMapper.kt" "Firebase Error Mapper exists"

# Check iOS App Files
check_file "iosApp/iosApp/iOSApp.swift" "iOS App entry point exists"
check_file "iosApp/iosApp/Views/Logging/DailyLoggingView.swift" "Daily Logging View exists"
check_file "iosApp/iosApp/ViewModels/ModernDailyLoggingViewModel.swift" "Daily Logging ViewModel exists"

echo ""
echo "🔧 Checking Configuration..."
echo "================================"

# Check Firebase configuration
check_file "iosApp/iosApp/GoogleService-Info.plist" "Firebase configuration file exists"
check_content "iosApp/iosApp/iOSApp.swift" "FirebaseApp.configure()" "Firebase is initialized in app"
check_content "iosApp/iosApp/iOSApp.swift" "FirebaseBridgeInitializer.initialize()" "Firebase Bridge is initialized"

echo ""
echo "📱 Checking iOS Project Structure..."
echo "================================"

# Check Xcode project
check_file "iosApp/iosApp.xcodeproj/project.pbxproj" "Xcode project file exists"
check_dir "iosApp/iosApp/Services" "Services directory exists"
check_dir "iosApp/iosApp/Views/Logging" "Logging views directory exists"

echo ""
echo "🧪 Checking Test Infrastructure..."
echo "================================"

# Check test files
check_file "shared/src/iosTest/kotlin/com/eunio/healthapp/data/remote/FirestoreServiceImplTest.kt" "iOS FirestoreService tests exist"
check_file "shared/src/iosTest/kotlin/com/eunio/healthapp/data/remote/firebase/FirebaseErrorMapperTest.kt" "Error Mapper tests exist"

echo ""
echo "📝 Checking Documentation..."
echo "================================"

# Check documentation
check_file ".kiro/specs/ios-firebase-sync-fix/requirements.md" "Requirements document exists"
check_file ".kiro/specs/ios-firebase-sync-fix/design.md" "Design document exists"
check_file ".kiro/specs/ios-firebase-sync-fix/tasks.md" "Tasks document exists"
check_file ".kiro/specs/ios-firebase-sync-fix/TASK-5-1-TEST-GUIDE.md" "Test guide exists"

echo ""
echo "🔐 Checking Authentication Setup..."
echo "================================"

check_content "iosApp/iosApp/iOSApp.swift" "AuthenticationManager" "Authentication manager exists"
check_content "iosApp/iosApp/iOSApp.swift" "currentUserId" "User ID is stored in UserDefaults"

echo ""
echo "💾 Checking Data Models..."
echo "================================"

# Check if DailyLogDto exists in shared code
if find shared/src/commonMain -name "*DailyLog*.kt" | grep -q .; then
    echo -e "${GREEN}✓${NC} DailyLog data models exist in shared code"
    ((CHECKS_PASSED++))
else
    echo -e "${RED}✗${NC} DailyLog data models not found in shared code"
    ((CHECKS_FAILED++))
fi

echo ""
echo "================================"
echo "📊 Test Readiness Summary"
echo "================================"
echo -e "Checks Passed: ${GREEN}${CHECKS_PASSED}${NC}"
echo -e "Checks Failed: ${RED}${CHECKS_FAILED}${NC}"
echo ""

if [ $CHECKS_FAILED -eq 0 ]; then
    echo -e "${GREEN}✅ All checks passed! Ready to test iOS Firebase save operation.${NC}"
    echo ""
    echo "Next steps:"
    echo "1. Open iosApp/iosApp.xcodeproj in Xcode"
    echo "2. Select a simulator (e.g., iPhone 15 Pro)"
    echo "3. Build and run the app (Cmd+R)"
    echo "4. Follow the test guide: .kiro/specs/ios-firebase-sync-fix/TASK-5-1-TEST-GUIDE.md"
    echo ""
    exit 0
else
    echo -e "${RED}❌ Some checks failed. Please fix the issues above before testing.${NC}"
    echo ""
    exit 1
fi
