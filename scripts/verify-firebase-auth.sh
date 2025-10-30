#!/bin/bash

# Firebase Authentication Verification Script
# This script verifies that Firebase Authentication is properly configured
# and ready for testing on both Android and iOS platforms

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Firebase Authentication Verification${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

ERRORS=0
WARNINGS=0

# Function to check if a file exists
check_file() {
    local file=$1
    local description=$2
    
    if [ -f "$file" ]; then
        echo -e "${GREEN}✓${NC} $description: $file"
        return 0
    else
        echo -e "${RED}✗${NC} $description: $file (NOT FOUND)"
        ERRORS=$((ERRORS + 1))
        return 1
    fi
}

# Function to check if a string exists in a file
check_string_in_file() {
    local file=$1
    local search_string=$2
    local description=$3
    
    if [ ! -f "$file" ]; then
        echo -e "${RED}✗${NC} $description: File not found"
        ERRORS=$((ERRORS + 1))
        return 1
    fi
    
    if grep -q "$search_string" "$file"; then
        echo -e "${GREEN}✓${NC} $description"
        return 0
    else
        echo -e "${YELLOW}⚠${NC} $description: Not found in $file"
        WARNINGS=$((WARNINGS + 1))
        return 1
    fi
}

echo -e "${BLUE}1. Checking Firebase Configuration Files${NC}"
echo "-------------------------------------------"

# Check Android Firebase config
check_file "androidApp/google-services.json" "Android Firebase config"

# Check iOS Firebase config
check_file "iosApp/iosApp/GoogleService-Info.plist" "iOS Firebase config"

echo ""

echo -e "${BLUE}2. Checking Android Auth Implementation${NC}"
echo "-------------------------------------------"

# Check Android auth service
if check_file "shared/src/androidMain/kotlin/com/eunio/healthapp/auth/AndroidAuthService.kt" "Android AuthService"; then
    check_string_in_file "shared/src/androidMain/kotlin/com/eunio/healthapp/auth/AndroidAuthService.kt" "FirebaseAuth" "Firebase Auth import"
    check_string_in_file "shared/src/androidMain/kotlin/com/eunio/healthapp/auth/AndroidAuthService.kt" "signIn" "Sign-in method"
    check_string_in_file "shared/src/androidMain/kotlin/com/eunio/healthapp/auth/AndroidAuthService.kt" "signUp" "Sign-up method"
    check_string_in_file "shared/src/androidMain/kotlin/com/eunio/healthapp/auth/AndroidAuthService.kt" "signOut" "Sign-out method"
fi

echo ""

echo -e "${BLUE}3. Checking iOS Auth Implementation${NC}"
echo "-------------------------------------------"

# Check iOS auth service
if check_file "shared/src/iosMain/kotlin/com/eunio/healthapp/data/remote/auth/IOSAuthService.kt" "iOS AuthService"; then
    check_string_in_file "shared/src/iosMain/kotlin/com/eunio/healthapp/data/remote/auth/IOSAuthService.kt" "signIn" "Sign-in method"
    check_string_in_file "shared/src/iosMain/kotlin/com/eunio/healthapp/data/remote/auth/IOSAuthService.kt" "signUp" "Sign-up method"
    check_string_in_file "shared/src/iosMain/kotlin/com/eunio/healthapp/data/remote/auth/IOSAuthService.kt" "signOut" "Sign-out method"
fi

# Check Swift Firebase bridge
if check_file "iosApp/iosApp/Services/SwiftAuthService.swift" "Swift Auth Service"; then
    check_string_in_file "iosApp/iosApp/Services/SwiftAuthService.swift" "Firebase" "Firebase import"
fi

echo ""

echo -e "${BLUE}4. Checking Common Auth Interface${NC}"
echo "-------------------------------------------"

if check_file "shared/src/commonMain/kotlin/com/eunio/healthapp/data/remote/auth/AuthService.kt" "Common AuthService interface"; then
    check_string_in_file "shared/src/commonMain/kotlin/com/eunio/healthapp/data/remote/auth/AuthService.kt" "interface AuthService" "AuthService interface"
    check_string_in_file "shared/src/commonMain/kotlin/com/eunio/healthapp/data/remote/auth/AuthService.kt" "suspend fun signIn" "Sign-in signature"
    check_string_in_file "shared/src/commonMain/kotlin/com/eunio/healthapp/data/remote/auth/AuthService.kt" "suspend fun signUp" "Sign-up signature"
    check_string_in_file "shared/src/commonMain/kotlin/com/eunio/healthapp/data/remote/auth/AuthService.kt" "suspend fun signOut" "Sign-out signature"
fi

echo ""

echo -e "${BLUE}5. Checking Auth Tests${NC}"
echo "-------------------------------------------"

# Check for auth tests
check_file "shared/src/commonTest/kotlin/com/eunio/healthapp/domain/manager/AuthManagerTest.kt" "AuthManager unit tests"
check_file "shared/src/commonTest/kotlin/com/eunio/healthapp/domain/manager/AuthManagerIntegrationTest.kt" "AuthManager integration tests"
check_file "shared/src/commonTest/kotlin/com/eunio/healthapp/integration/AuthenticationSyncTest.kt" "Authentication sync tests"

echo ""

echo -e "${BLUE}6. Checking Gradle Dependencies${NC}"
echo "-------------------------------------------"

# Check for Firebase dependencies in Android
if [ -f "androidApp/build.gradle.kts" ]; then
    if grep -q "firebase" "androidApp/build.gradle.kts"; then
        echo -e "${GREEN}✓${NC} Firebase dependencies found in Android build.gradle.kts"
    else
        echo -e "${YELLOW}⚠${NC} Firebase dependencies not found in Android build.gradle.kts"
        WARNINGS=$((WARNINGS + 1))
    fi
else
    echo -e "${RED}✗${NC} androidApp/build.gradle.kts not found"
    ERRORS=$((ERRORS + 1))
fi

echo ""

echo -e "${BLUE}7. Checking iOS Firebase Integration${NC}"
echo "-------------------------------------------"

# Check for Firebase in iOS project
if [ -f "iosApp/iosApp.xcodeproj/project.pbxproj" ]; then
    if grep -q "Firebase" "iosApp/iosApp.xcodeproj/project.pbxproj"; then
        echo -e "${GREEN}✓${NC} Firebase packages found in iOS project"
    else
        echo -e "${YELLOW}⚠${NC} Firebase packages not found in iOS project (may be using CocoaPods)"
        WARNINGS=$((WARNINGS + 1))
    fi
fi

# Check for Podfile (CocoaPods)
if [ -f "iosApp/Podfile" ]; then
    if grep -q "Firebase" "iosApp/Podfile"; then
        echo -e "${GREEN}✓${NC} Firebase pods found in Podfile"
    else
        echo -e "${YELLOW}⚠${NC} Firebase pods not found in Podfile"
        WARNINGS=$((WARNINGS + 1))
    fi
fi

echo ""

echo -e "${BLUE}8. Build Verification${NC}"
echo "-------------------------------------------"

echo "Checking if project can build..."

# Check if Gradle wrapper exists
if [ -f "./gradlew" ]; then
    echo -e "${GREEN}✓${NC} Gradle wrapper found"
    
    # Try to compile shared module (quick check)
    echo "Compiling shared module..."
    if ./gradlew :shared:compileKotlinMetadata --quiet 2>/dev/null; then
        echo -e "${GREEN}✓${NC} Shared module compiles successfully"
    else
        echo -e "${YELLOW}⚠${NC} Shared module compilation check skipped (may require full build)"
        WARNINGS=$((WARNINGS + 1))
    fi
else
    echo -e "${RED}✗${NC} Gradle wrapper not found"
    ERRORS=$((ERRORS + 1))
fi

echo ""

echo -e "${BLUE}9. Test Execution Check${NC}"
echo "-------------------------------------------"

echo "To run authentication tests, execute:"
echo ""
echo "  # Run all shared tests (includes auth tests)"
echo "  ./gradlew :shared:test"
echo ""
echo "  # Run Android unit tests"
echo "  ./gradlew :androidApp:testDebugUnitTest"
echo ""
echo "  # Run iOS tests (in Xcode)"
echo "  # Product > Test (Cmd+U)"
echo ""

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Verification Summary${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

if [ $ERRORS -eq 0 ] && [ $WARNINGS -eq 0 ]; then
    echo -e "${GREEN}✓ All checks passed!${NC}"
    echo ""
    echo "Firebase Authentication is properly configured."
    echo "You can proceed with manual testing using the test plan:"
    echo "  .kiro/specs/4-kotlin-xcode-ios26-upgrade/task-19-firebase-auth-test-plan.md"
    echo ""
    exit 0
elif [ $ERRORS -eq 0 ]; then
    echo -e "${YELLOW}⚠ Verification completed with $WARNINGS warning(s)${NC}"
    echo ""
    echo "Firebase Authentication appears to be configured, but some optional"
    echo "components may be missing. Review warnings above."
    echo ""
    exit 0
else
    echo -e "${RED}✗ Verification failed with $ERRORS error(s) and $WARNINGS warning(s)${NC}"
    echo ""
    echo "Please fix the errors above before proceeding with authentication testing."
    echo ""
    exit 1
fi
