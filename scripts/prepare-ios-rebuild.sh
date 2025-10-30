#!/bin/bash

# Prepare for iOS Project Rebuild
# This script backs up critical files and verifies prerequisites

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}iOS Project Rebuild Preparation${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Check we're in project root
if [ ! -f "build.gradle.kts" ]; then
    echo -e "${RED}Error: Must run from project root${NC}"
    exit 1
fi

echo -e "${YELLOW}Step 1: Verifying Prerequisites${NC}"
echo ""

# Check Xcode 26
echo -n "Checking Xcode version... "
XCODE_VERSION=$(xcodebuild -version | grep "Xcode" | awk '{print $2}')
XCODE_MAJOR=$(echo $XCODE_VERSION | cut -d'.' -f1)
if [ "$XCODE_MAJOR" = "26" ]; then
    echo -e "${GREEN}✓${NC} Xcode $XCODE_VERSION"
else
    echo -e "${RED}✗${NC} Xcode $XCODE_VERSION (need 26.x)"
    echo -e "${RED}Please install Xcode 26 before proceeding${NC}"
    exit 1
fi

# Check shared framework can build
echo -n "Checking shared module... "
if [ -f "shared/build.gradle.kts" ]; then
    echo -e "${GREEN}✓${NC} Found"
else
    echo -e "${RED}✗${NC} Not found"
    exit 1
fi

# Check EunioBridgeKit
echo -n "Checking EunioBridgeKit... "
if [ -d "EunioBridgeKit" ]; then
    echo -e "${GREEN}✓${NC} Found"
else
    echo -e "${RED}✗${NC} Not found"
    exit 1
fi

# Count Swift files
echo -n "Counting Swift source files... "
SWIFT_COUNT=$(find iosApp/iosApp -name "*.swift" -type f | wc -l | tr -d ' ')
echo -e "${GREEN}✓${NC} $SWIFT_COUNT files"

echo ""
echo -e "${YELLOW}Step 2: Creating Backups${NC}"
echo ""

# Create backup directory
BACKUP_DIR=".kiro/specs/4-kotlin-xcode-ios26-upgrade/ios-rebuild-backup"
mkdir -p "$BACKUP_DIR"

# Backup corrupted project
echo -n "Backing up corrupted .xcodeproj... "
if [ -d "iosApp/iosApp.xcodeproj" ]; then
    cp -r "iosApp/iosApp.xcodeproj" "$BACKUP_DIR/iosApp.xcodeproj.corrupted"
    echo -e "${GREEN}✓${NC}"
else
    echo -e "${YELLOW}⚠${NC} Not found (may already be removed)"
fi

# Backup configuration files
echo -n "Backing up Info.plist... "
cp "iosApp/iosApp/Info.plist" "$BACKUP_DIR/Info.plist.backup"
echo -e "${GREEN}✓${NC}"

echo -n "Backing up GoogleService-Info.plist... "
cp "iosApp/iosApp/GoogleService-Info.plist" "$BACKUP_DIR/GoogleService-Info.plist.backup"
echo -e "${GREEN}✓${NC}"

echo -n "Backing up iosApp.entitlements... "
cp "iosApp/iosApp/iosApp.entitlements" "$BACKUP_DIR/iosApp.entitlements.backup"
echo -e "${GREEN}✓${NC}"

if [ -f "iosApp/iosApp/PrivacyInfo.xcprivacy" ]; then
    echo -n "Backing up PrivacyInfo.xcprivacy... "
    cp "iosApp/iosApp/PrivacyInfo.xcprivacy" "$BACKUP_DIR/PrivacyInfo.xcprivacy.backup"
    echo -e "${GREEN}✓${NC}"
fi

# Create file list
echo -n "Creating Swift file inventory... "
find iosApp/iosApp -name "*.swift" -type f | sort > "$BACKUP_DIR/swift-files-inventory.txt"
echo -e "${GREEN}✓${NC}"

echo ""
echo -e "${YELLOW}Step 3: Extracting Project Configuration${NC}"
echo ""

# Extract bundle ID from GoogleService-Info.plist
BUNDLE_ID=$(grep -A 1 "BUNDLE_ID" iosApp/iosApp/GoogleService-Info.plist | grep "<string>" | sed 's/.*<string>\(.*\)<\/string>.*/\1/')
echo -e "Bundle Identifier: ${GREEN}$BUNDLE_ID${NC}"

# Extract reversed client ID
REVERSED_CLIENT_ID=$(grep -A 1 "REVERSED_CLIENT_ID" iosApp/iosApp/GoogleService-Info.plist | grep "<string>" | sed 's/.*<string>\(.*\)<\/string>.*/\1/')
echo -e "Reversed Client ID: ${GREEN}$REVERSED_CLIENT_ID${NC}"

# Save configuration
cat > "$BACKUP_DIR/project-config.txt" << EOF
iOS Project Configuration
==========================

Bundle Identifier: $BUNDLE_ID
Reversed Client ID: $REVERSED_CLIENT_ID
Deployment Target: iOS 15.0
Swift Files: $SWIFT_COUNT

Capabilities Required:
- HealthKit
- HealthKit Background Delivery
- Keychain Sharing
- App Groups: group.com.eunio.healthapp
- Push Notifications
- Background Modes:
  - Background fetch
  - Background processing
  - HealthKit
  - Remote notifications
  - Processing

Firebase Products (SPM):
- FirebaseAuth
- FirebaseCore
- FirebaseFirestore
- FirebaseCrashlytics
- FirebaseAnalytics
- FirebasePerformance

Frameworks to Link:
- shared.framework (from Kotlin)
- EunioBridgeKit.xcframework

Build Settings:
- Enable Bitcode: No
- Other Linker Flags: -ObjC -l"c++"
- Enable Modules: Yes
EOF

echo -e "${GREEN}✓${NC} Configuration saved to $BACKUP_DIR/project-config.txt"

echo ""
echo -e "${YELLOW}Step 4: Building Prerequisites${NC}"
echo ""

# Build shared framework
echo "Building Kotlin shared framework..."
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 --quiet
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓${NC} Shared framework built successfully"
else
    echo -e "${RED}✗${NC} Failed to build shared framework"
    exit 1
fi

# Check if EunioBridgeKit is built
echo -n "Checking EunioBridgeKit.xcframework... "
if [ -d "EunioBridgeKit/build/EunioBridgeKit.xcframework" ]; then
    echo -e "${GREEN}✓${NC} Found"
else
    echo -e "${YELLOW}⚠${NC} Not found - you may need to build it"
    echo "  Run: cd EunioBridgeKit && ./build-xcframework.sh"
fi

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Preparation Complete!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "Backups saved to: ${BLUE}$BACKUP_DIR${NC}"
echo ""
echo -e "${YELLOW}Next Steps:${NC}"
echo "1. Review the rebuild guide:"
echo -e "   ${BLUE}.kiro/specs/4-kotlin-xcode-ios26-upgrade/ios-project-rebuild-guide.md${NC}"
echo ""
echo "2. When ready, move the corrupted project:"
echo -e "   ${BLUE}cd iosApp && mv iosApp.xcodeproj iosApp.xcodeproj.corrupted${NC}"
echo ""
echo "3. Open Xcode 26 and follow the guide to create a new project"
echo ""
echo -e "${YELLOW}Estimated time: 2-3 hours${NC}"
echo ""
