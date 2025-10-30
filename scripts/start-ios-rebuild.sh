#!/bin/bash

# Quick Start Script for iOS Project Rebuild
# Run this when you're ready to begin the rebuild process

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

clear

echo -e "${CYAN}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║                                                            ║${NC}"
echo -e "${CYAN}║          iOS Project Rebuild - Quick Start                ║${NC}"
echo -e "${CYAN}║                                                            ║${NC}"
echo -e "${CYAN}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Check we're in project root
if [ ! -f "build.gradle.kts" ]; then
    echo -e "${RED}Error: Must run from project root${NC}"
    exit 1
fi

echo -e "${BLUE}📋 Pre-Flight Checklist${NC}"
echo ""

# Check Xcode
echo -n "  Xcode 26: "
XCODE_VERSION=$(xcodebuild -version | grep "Xcode" | awk '{print $2}')
XCODE_MAJOR=$(echo $XCODE_VERSION | cut -d'.' -f1)
if [ "$XCODE_MAJOR" = "26" ]; then
    echo -e "${GREEN}✓${NC} $XCODE_VERSION"
else
    echo -e "${RED}✗${NC} $XCODE_VERSION (need 26.x)"
    exit 1
fi

# Check backups exist
echo -n "  Backups: "
if [ -d ".kiro/specs/4-kotlin-xcode-ios26-upgrade/ios-rebuild-backup" ]; then
    echo -e "${GREEN}✓${NC} Ready"
else
    echo -e "${RED}✗${NC} Not found"
    echo ""
    echo -e "${YELLOW}Run preparation script first:${NC}"
    echo "  ./scripts/prepare-ios-rebuild.sh"
    exit 1
fi

# Check shared framework
echo -n "  Shared Framework: "
if [ -f "shared/build/bin/iosSimulatorArm64/debugFramework/shared.framework/shared" ]; then
    echo -e "${GREEN}✓${NC} Built"
else
    echo -e "${YELLOW}⚠${NC} Not found - will build now"
    ./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 --quiet
    echo -e "    ${GREEN}✓${NC} Built"
fi

# Check EunioBridgeKit
echo -n "  EunioBridgeKit: "
if [ -d "shared/src/iosMain/c_interop/libs/EunioBridgeKit.xcframework" ]; then
    echo -e "${GREEN}✓${NC} Ready"
else
    echo -e "${RED}✗${NC} Not found"
    exit 1
fi

# Check Swift files
echo -n "  Swift Files: "
SWIFT_COUNT=$(find iosApp/iosApp -name "*.swift" -type f 2>/dev/null | wc -l | tr -d ' ')
if [ "$SWIFT_COUNT" = "139" ]; then
    echo -e "${GREEN}✓${NC} $SWIFT_COUNT files"
else
    echo -e "${YELLOW}⚠${NC} $SWIFT_COUNT files (expected 139)"
fi

echo ""
echo -e "${BLUE}📦 Project Configuration${NC}"
echo ""
echo -e "  Bundle ID: ${CYAN}com.eunio.healthapp${NC}"
echo -e "  Deployment Target: ${CYAN}iOS 15.0${NC}"
echo -e "  Build SDK: ${CYAN}iOS 26${NC}"
echo ""

echo -e "${YELLOW}⚠️  Important Notes:${NC}"
echo ""
echo "  1. This process takes 2-3 hours"
echo "  2. You'll need to interact with Xcode UI"
echo "  3. Follow the checklist step-by-step"
echo "  4. Don't skip any steps"
echo ""

echo -e "${BLUE}📚 Available Resources:${NC}"
echo ""
echo -e "  ${CYAN}Interactive Checklist:${NC}"
echo "    .kiro/specs/4-kotlin-xcode-ios26-upgrade/ios-rebuild-checklist.md"
echo ""
echo -e "  ${CYAN}Detailed Guide:${NC}"
echo "    .kiro/specs/4-kotlin-xcode-ios26-upgrade/ios-project-rebuild-guide.md"
echo ""
echo -e "  ${CYAN}Backup Location:${NC}"
echo "    .kiro/specs/4-kotlin-xcode-ios26-upgrade/ios-rebuild-backup/"
echo ""

echo -e "${YELLOW}═══════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${GREEN}Ready to begin!${NC}"
echo ""
echo -e "${YELLOW}Next Steps:${NC}"
echo ""
echo "  1. Move the corrupted project:"
echo -e "     ${CYAN}cd iosApp && mv iosApp.xcodeproj iosApp.xcodeproj.corrupted && cd ..${NC}"
echo ""
echo "  2. Open the checklist:"
echo -e "     ${CYAN}open .kiro/specs/4-kotlin-xcode-ios26-upgrade/ios-rebuild-checklist.md${NC}"
echo ""
echo "  3. Open Xcode 26:"
echo -e "     ${CYAN}open -a Xcode${NC}"
echo ""
echo "  4. In Xcode: File → New → Project"
echo ""
echo -e "${YELLOW}═══════════════════════════════════════════════════════════${NC}"
echo ""

# Ask for confirmation
read -p "$(echo -e ${CYAN}Move corrupted project now? [y/N]: ${NC})" -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo ""
    echo -e "${YELLOW}Moving corrupted project...${NC}"
    cd iosApp
    if [ -d "iosApp.xcodeproj" ]; then
        mv iosApp.xcodeproj iosApp.xcodeproj.corrupted
        echo -e "${GREEN}✓${NC} Moved to iosApp.xcodeproj.corrupted"
    else
        echo -e "${YELLOW}⚠${NC} Project already moved or not found"
    fi
    cd ..
    echo ""
    echo -e "${GREEN}✓ Ready to create new project in Xcode!${NC}"
    echo ""
    echo -e "${CYAN}Opening checklist and Xcode...${NC}"
    sleep 1
    open .kiro/specs/4-kotlin-xcode-ios26-upgrade/ios-rebuild-checklist.md
    sleep 1
    open -a Xcode
    echo ""
    echo -e "${GREEN}Good luck! Follow the checklist step-by-step.${NC}"
    echo ""
else
    echo ""
    echo -e "${YELLOW}Skipped. Run these commands when ready:${NC}"
    echo ""
    echo "  cd iosApp && mv iosApp.xcodeproj iosApp.xcodeproj.corrupted && cd .."
    echo "  open .kiro/specs/4-kotlin-xcode-ios26-upgrade/ios-rebuild-checklist.md"
    echo "  open -a Xcode"
    echo ""
fi
