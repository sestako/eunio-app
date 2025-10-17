#!/bin/bash

# Android Logging Migration Verification Script
# This script verifies that all println() statements have been replaced with proper Log calls

echo "=========================================="
echo "Android Logging Migration Verification"
echo "=========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Counter for issues
ISSUES=0

# Check 1: Verify no println() statements remain
echo "Check 1: Verifying no println() statements in Android services..."
PRINTLN_COUNT=$(find shared/src/androidMain/kotlin/com/eunio/healthapp -name "*.kt" -type f -exec grep -l "println" {} \; 2>/dev/null | wc -l)

if [ "$PRINTLN_COUNT" -eq 0 ]; then
    echo -e "${GREEN}✓ PASS${NC}: No println() statements found"
else
    echo -e "${RED}✗ FAIL${NC}: Found $PRINTLN_COUNT files with println() statements"
    find shared/src/androidMain/kotlin/com/eunio/healthapp -name "*.kt" -type f -exec grep -l "println" {} \; 2>/dev/null
    ISSUES=$((ISSUES + 1))
fi
echo ""

# Check 2: Verify Log imports
echo "Check 2: Verifying android.util.Log imports..."
EXPECTED_FILES=("AndroidDailyLogService.kt" "AndroidUserProfileService.kt" "AndroidAuthService.kt" "AndroidSettingsManager.kt" "AndroidNavigationManager.kt")
MISSING_IMPORTS=0

for file in "${EXPECTED_FILES[@]}"; do
    FILE_PATH=$(find shared/src/androidMain/kotlin/com/eunio/healthapp -name "$file" -type f 2>/dev/null)
    if [ -n "$FILE_PATH" ]; then
        if grep -q "import android.util.Log" "$FILE_PATH"; then
            echo -e "${GREEN}✓${NC} $file has Log import"
        else
            echo -e "${RED}✗${NC} $file missing Log import"
            MISSING_IMPORTS=$((MISSING_IMPORTS + 1))
        fi
    else
        echo -e "${YELLOW}⚠${NC} $file not found"
    fi
done

if [ "$MISSING_IMPORTS" -eq 0 ]; then
    echo -e "${GREEN}✓ PASS${NC}: All files have proper Log imports"
else
    echo -e "${RED}✗ FAIL${NC}: $MISSING_IMPORTS files missing Log imports"
    ISSUES=$((ISSUES + 1))
fi
echo ""

# Check 3: Verify TAG constants
echo "Check 3: Verifying TAG constants..."
MISSING_TAGS=0

for file in "${EXPECTED_FILES[@]}"; do
    FILE_PATH=$(find shared/src/androidMain/kotlin/com/eunio/healthapp -name "$file" -type f 2>/dev/null)
    if [ -n "$FILE_PATH" ]; then
        if grep -q "private const val TAG" "$FILE_PATH"; then
            TAG_VALUE=$(grep "private const val TAG" "$FILE_PATH" | sed 's/.*= "\(.*\)".*/\1/')
            echo -e "${GREEN}✓${NC} $file has TAG: \"$TAG_VALUE\""
        else
            echo -e "${RED}✗${NC} $file missing TAG constant"
            MISSING_TAGS=$((MISSING_TAGS + 1))
        fi
    fi
done

if [ "$MISSING_TAGS" -eq 0 ]; then
    echo -e "${GREEN}✓ PASS${NC}: All files have TAG constants"
else
    echo -e "${RED}✗ FAIL${NC}: $MISSING_TAGS files missing TAG constants"
    ISSUES=$((ISSUES + 1))
fi
echo ""

# Check 4: Verify Log method usage
echo "Check 4: Verifying Log method usage (Log.d, Log.w, Log.e)..."
LOG_USAGE=0

for file in "${EXPECTED_FILES[@]}"; do
    FILE_PATH=$(find shared/src/androidMain/kotlin/com/eunio/healthapp -name "$file" -type f 2>/dev/null)
    if [ -n "$FILE_PATH" ]; then
        LOG_CALLS=$(grep -c "Log\.[dwie](" "$FILE_PATH" 2>/dev/null || echo "0")
        if [ "$LOG_CALLS" -gt 0 ]; then
            echo -e "${GREEN}✓${NC} $file uses Log methods ($LOG_CALLS calls)"
            LOG_USAGE=$((LOG_USAGE + 1))
        else
            echo -e "${YELLOW}⚠${NC} $file has no Log method calls"
        fi
    fi
done

if [ "$LOG_USAGE" -eq ${#EXPECTED_FILES[@]} ]; then
    echo -e "${GREEN}✓ PASS${NC}: All files use Log methods"
else
    echo -e "${YELLOW}⚠ WARNING${NC}: Some files may not have Log calls"
fi
echo ""

# Check 5: List all TAG values for reference
echo "Check 5: Summary of TAG values for Logcat filtering..."
echo "Use these tags in Android Studio Logcat:"
echo ""
for file in "${EXPECTED_FILES[@]}"; do
    FILE_PATH=$(find shared/src/androidMain/kotlin/com/eunio/healthapp -name "$file" -type f 2>/dev/null)
    if [ -n "$FILE_PATH" ]; then
        TAG_VALUE=$(grep "private const val TAG" "$FILE_PATH" | sed 's/.*= "\(.*\)".*/\1/' 2>/dev/null)
        if [ -n "$TAG_VALUE" ]; then
            echo "  - tag:$TAG_VALUE"
        fi
    fi
done
echo ""

# Final summary
echo "=========================================="
echo "Verification Summary"
echo "=========================================="
if [ "$ISSUES" -eq 0 ]; then
    echo -e "${GREEN}✓ ALL CHECKS PASSED${NC}"
    echo ""
    echo "Migration complete! All println() statements have been replaced with proper Log calls."
    echo ""
    echo "Next steps:"
    echo "1. Run the Android app in Android Studio"
    echo "2. Open Logcat panel (View → Tool Windows → Logcat)"
    echo "3. Use the tags listed above to filter logs"
    echo "4. Verify logs appear correctly during app usage"
    echo ""
    echo "See verify-android-logging.md for detailed verification steps."
else
    echo -e "${RED}✗ FOUND $ISSUES ISSUE(S)${NC}"
    echo ""
    echo "Please fix the issues above before proceeding with manual verification."
fi
echo ""

exit $ISSUES
