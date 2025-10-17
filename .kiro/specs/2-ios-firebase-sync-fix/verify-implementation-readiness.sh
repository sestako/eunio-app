#!/bin/bash

# Verify Implementation Readiness for Task 5.1
# This script checks that all required files and implementations are in place

echo "=========================================="
echo "Task 5.1 Implementation Readiness Check"
echo "=========================================="
echo ""

ERRORS=0
WARNINGS=0

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

check_file() {
    if [ -f "$1" ]; then
        echo -e "${GREEN}✓${NC} Found: $1"
        return 0
    else
        echo -e "${RED}✗${NC} Missing: $1"
        ERRORS=$((ERRORS + 1))
        return 1
    fi
}

check_content() {
    if grep -q "$2" "$1" 2>/dev/null; then
        echo -e "${GREEN}✓${NC} $3"
        return 0
    else
        echo -e "${RED}✗${NC} $3"
        ERRORS=$((ERRORS + 1))
        return 1
    fi
}

check_warning() {
    if grep -q "$2" "$1" 2>/dev/null; then
        echo -e "${GREEN}✓${NC} $3"
        return 0
    else
        echo -e "${YELLOW}⚠${NC} $3"
        WARNINGS=$((WARNINGS + 1))
        return 1
    fi
}

echo "Checking Swift Firebase Bridge..."
check_file "iosApp/iosApp/Services/FirebaseIOSBridge.swift"
if [ $? -eq 0 ]; then
    check_content "iosApp/iosApp/Services/FirebaseIOSBridge.swift" "saveDailyLog" "  - saveDailyLog method exists"
    check_content "iosApp/iosApp/Services/FirebaseIOSBridge.swift" "users.*dailyLogs" "  - Uses correct path structure"
    check_content "iosApp/iosApp/Services/FirebaseIOSBridge.swift" "completion" "  - Has completion handler"
fi
echo ""

echo "Checking Kotlin/Native Interop..."
check_file "shared/src/iosMain/kotlin/com/eunio/healthapp/data/remote/FirestoreServiceImpl.ios.kt"
if [ $? -eq 0 ]; then
    check_content "shared/src/iosMain/kotlin/com/eunio/healthapp/data/remote/FirestoreServiceImpl.ios.kt" "FirebaseNativeBridge" "  - Uses FirebaseNativeBridge"
    check_content "shared/src/iosMain/kotlin/com/eunio/healthapp/data/remote/FirestoreServiceImpl.ios.kt" "saveDailyLog" "  - saveDailyLog implemented"
    check_content "shared/src/iosMain/kotlin/com/eunio/healthapp/data/remote/FirestoreServiceImpl.ios.kt" "dtoToMap" "  - Has data conversion method"
fi
echo ""

echo "Checking Error Mapping..."
check_file "shared/src/iosMain/kotlin/com/eunio/healthapp/data/remote/firebase/FirebaseErrorMapper.kt"
if [ $? -eq 0 ]; then
    check_content "shared/src/iosMain/kotlin/com/eunio/healthapp/data/remote/firebase/FirebaseErrorMapper.kt" "mapError" "  - mapError function exists"
    check_content "shared/src/iosMain/kotlin/com/eunio/healthapp/data/remote/firebase/FirebaseErrorMapper.kt" "NetworkError" "  - Maps network errors"
    check_content "shared/src/iosMain/kotlin/com/eunio/healthapp/data/remote/firebase/FirebaseErrorMapper.kt" "AuthenticationError" "  - Maps auth errors"
fi
echo ""

echo "Checking iOS ViewModel..."
check_file "iosApp/iosApp/ViewModels/ModernDailyLoggingViewModel.swift"
if [ $? -eq 0 ]; then
    check_content "iosApp/iosApp/ViewModels/ModernDailyLoggingViewModel.swift" "saveLog" "  - saveLog method exists"
    check_content "iosApp/iosApp/ViewModels/ModernDailyLoggingViewModel.swift" "successMessage" "  - Has successMessage property"
    check_content "iosApp/iosApp/ViewModels/ModernDailyLoggingViewModel.swift" "errorMessage" "  - Has errorMessage property"
    check_content "iosApp/iosApp/ViewModels/ModernDailyLoggingViewModel.swift" "isSaving" "  - Has isSaving property"
fi
echo ""

echo "Checking iOS UI..."
check_file "iosApp/iosApp/Views/Logging/DailyLoggingView.swift"
if [ $? -eq 0 ]; then
    check_content "iosApp/iosApp/Views/Logging/DailyLoggingView.swift" "Save Daily Log" "  - Has save button"
    check_content "iosApp/iosApp/Views/Logging/DailyLoggingView.swift" "successCard" "  - Has success message card"
    check_content "iosApp/iosApp/Views/Logging/DailyLoggingView.swift" "errorCard" "  - Has error message card"
    check_content "iosApp/iosApp/Views/Logging/DailyLoggingView.swift" "ProgressView" "  - Has loading indicator"
fi
echo ""

echo "Checking Data Models..."
check_file "shared/src/commonMain/kotlin/com/eunio/healthapp/data/remote/dto/DailyLogDto.kt"
if [ $? -eq 0 ]; then
    check_content "shared/src/commonMain/kotlin/com/eunio/healthapp/data/remote/dto/DailyLogDto.kt" "fromDomain" "  - Has fromDomain method"
    check_content "shared/src/commonMain/kotlin/com/eunio/healthapp/data/remote/dto/DailyLogDto.kt" "dateEpochDays" "  - Uses epoch days for dates"
fi
echo ""

echo "Checking Firebase Configuration..."
check_file "iosApp/iosApp/GoogleService-Info.plist"
check_file "iosApp/iosApp/iOSApp.swift"
if [ -f "iosApp/iosApp/iOSApp.swift" ]; then
    check_content "iosApp/iosApp/iOSApp.swift" "FirebaseApp.configure" "  - Firebase initialized"
fi
echo ""

echo "Checking Test Documentation..."
check_file ".kiro/specs/ios-firebase-sync-fix/TASK-5-1-TEST-GUIDE.md"
check_file ".kiro/specs/ios-firebase-sync-fix/TASK-5-1-MANUAL-TEST-GUIDE.md"
check_file ".kiro/specs/ios-firebase-sync-fix/test-ios-save-operation.sh"
echo ""

echo "Checking Unit Tests..."
check_file "shared/src/iosTest/kotlin/com/eunio/healthapp/data/remote/FirestoreServiceImplTest.kt"
check_file "shared/src/iosTest/kotlin/com/eunio/healthapp/data/remote/firebase/FirebaseErrorMapperTest.kt"
echo ""

echo "=========================================="
echo "Summary"
echo "=========================================="

if [ $ERRORS -eq 0 ] && [ $WARNINGS -eq 0 ]; then
    echo -e "${GREEN}✓ All checks passed!${NC}"
    echo ""
    echo "Implementation is ready for testing."
    echo "Next steps:"
    echo "  1. Open Xcode: cd iosApp && open iosApp.xcodeproj"
    echo "  2. Build and run on simulator"
    echo "  3. Follow test guide: cat .kiro/specs/ios-firebase-sync-fix/TASK-5-1-MANUAL-TEST-GUIDE.md"
    exit 0
elif [ $ERRORS -eq 0 ]; then
    echo -e "${YELLOW}⚠ $WARNINGS warnings found${NC}"
    echo ""
    echo "Implementation is mostly ready, but some optional components may be missing."
    echo "You can proceed with testing, but review the warnings above."
    exit 0
else
    echo -e "${RED}✗ $ERRORS errors found${NC}"
    if [ $WARNINGS -gt 0 ]; then
        echo -e "${YELLOW}⚠ $WARNINGS warnings found${NC}"
    fi
    echo ""
    echo "Implementation is NOT ready for testing."
    echo "Please fix the errors above before proceeding."
    exit 1
fi
