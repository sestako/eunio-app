#!/bin/bash

# Toolchain Verification Script
# Validates that the development environment has the correct versions
# of Kotlin, Gradle, and Xcode (macOS only) for the project.

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Expected versions
EXPECTED_KOTLIN="2.2.20"
EXPECTED_GRADLE_MAJOR=8
EXPECTED_GRADLE_MINOR=10
EXPECTED_XCODE_MAJOR=26

echo ""
echo -e "${BLUE}🔍 Verifying Toolchain Versions...${NC}"
echo ""

ERRORS=0

# Check Kotlin version
echo "Checking Kotlin version..."
if [ -f "gradle/libs.versions.toml" ]; then
    KOTLIN_VERSION=$(grep '^kotlin = ' gradle/libs.versions.toml | sed 's/.*"\(.*\)".*/\1/')
    if [ "$KOTLIN_VERSION" = "$EXPECTED_KOTLIN" ]; then
        echo -e "${GREEN}✓${NC} Kotlin: $KOTLIN_VERSION"
    else
        echo -e "${RED}✗${NC} Kotlin: $KOTLIN_VERSION (expected $EXPECTED_KOTLIN)"
        ERRORS=$((ERRORS + 1))
    fi
else
    echo -e "${RED}✗${NC} Could not find gradle/libs.versions.toml"
    ERRORS=$((ERRORS + 1))
fi

# Check Gradle version
echo "Checking Gradle version..."
if [ -f "./gradlew" ]; then
    GRADLE_VERSION=$(./gradlew --version 2>/dev/null | grep "^Gradle" | awk '{print $2}')
    if [ -n "$GRADLE_VERSION" ]; then
        GRADLE_MAJOR=$(echo "$GRADLE_VERSION" | cut -d'.' -f1)
        GRADLE_MINOR=$(echo "$GRADLE_VERSION" | cut -d'.' -f2)
        
        if [ "$GRADLE_MAJOR" -gt "$EXPECTED_GRADLE_MAJOR" ] || \
           ([ "$GRADLE_MAJOR" -eq "$EXPECTED_GRADLE_MAJOR" ] && [ "$GRADLE_MINOR" -ge "$EXPECTED_GRADLE_MINOR" ]); then
            echo -e "${GREEN}✓${NC} Gradle: $GRADLE_VERSION"
        else
            echo -e "${RED}✗${NC} Gradle: $GRADLE_VERSION (expected $EXPECTED_GRADLE_MAJOR.$EXPECTED_GRADLE_MINOR+)"
            ERRORS=$((ERRORS + 1))
        fi
    else
        echo -e "${RED}✗${NC} Could not determine Gradle version"
        ERRORS=$((ERRORS + 1))
    fi
else
    echo -e "${RED}✗${NC} Could not find gradlew script"
    ERRORS=$((ERRORS + 1))
fi

# Check Xcode version (macOS only)
if [[ "$OSTYPE" == "darwin"* ]]; then
    echo "Checking Xcode version..."
    if command -v xcodebuild &> /dev/null; then
        XCODE_VERSION=$(xcodebuild -version 2>/dev/null | grep "^Xcode" | awk '{print $2}')
        if [ -n "$XCODE_VERSION" ]; then
            XCODE_MAJOR=$(echo "$XCODE_VERSION" | cut -d'.' -f1)
            
            if [ "$XCODE_MAJOR" -eq "$EXPECTED_XCODE_MAJOR" ]; then
                echo -e "${GREEN}✓${NC} Xcode: $XCODE_VERSION"
            else
                echo -e "${RED}✗${NC} Xcode: $XCODE_VERSION (expected $EXPECTED_XCODE_MAJOR.x)"
                ERRORS=$((ERRORS + 1))
            fi
        else
            echo -e "${RED}✗${NC} Could not determine Xcode version"
            ERRORS=$((ERRORS + 1))
        fi
    else
        echo -e "${RED}✗${NC} Xcode is not installed or xcodebuild is not in PATH"
        ERRORS=$((ERRORS + 1))
    fi
else
    echo -e "${YELLOW}⊘${NC} Xcode check skipped (not running on macOS)"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

if [ $ERRORS -eq 0 ]; then
    echo -e "${GREEN}✓ All toolchain versions are correct!${NC}"
    echo ""
    exit 0
else
    echo -e "${RED}✗ Found $ERRORS toolchain version mismatch(es)${NC}"
    echo ""
    echo "Please update your toolchain to the required versions:"
    echo "  • Kotlin: $EXPECTED_KOTLIN"
    echo "  • Gradle: $EXPECTED_GRADLE_MAJOR.$EXPECTED_GRADLE_MINOR+"
    if [[ "$OSTYPE" == "darwin"* ]]; then
        echo "  • Xcode: $EXPECTED_XCODE_MAJOR.x (macOS only)"
    fi
    echo ""
    echo "For more information, see the project documentation."
    echo ""
    exit 1
fi
