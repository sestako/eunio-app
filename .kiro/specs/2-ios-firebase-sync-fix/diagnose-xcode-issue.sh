#!/bin/bash

# Diagnose Xcode Framework Issue

echo "=========================================="
echo "Xcode Framework Diagnostic"
echo "=========================================="
echo ""

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "1. Checking if shared framework exists..."
if [ -d "shared/build/bin/iosSimulatorArm64/debugFramework/shared.framework" ]; then
    echo -e "${GREEN}✓ Framework exists${NC}"
    ls -lh shared/build/bin/iosSimulatorArm64/debugFramework/shared.framework/shared
else
    echo -e "${RED}✗ Framework not found${NC}"
    echo "Run: ./gradlew :shared:linkDebugFrameworkIosSimulatorArm64"
fi
echo ""

echo "2. Checking framework headers..."
if [ -d "shared/build/bin/iosSimulatorArm64/debugFramework/shared.framework/Headers" ]; then
    echo -e "${GREEN}✓ Headers directory exists${NC}"
    ls shared/build/bin/iosSimulatorArm64/debugFramework/shared.framework/Headers/
else
    echo -e "${RED}✗ Headers not found${NC}"
fi
echo ""

echo "3. Checking if Xcode project exists..."
if [ -f "iosApp/iosApp.xcodeproj/project.pbxproj" ]; then
    echo -e "${GREEN}✓ Xcode project found${NC}"
else
    echo -e "${RED}✗ Xcode project not found${NC}"
fi
echo ""

echo "4. Checking for build script in Xcode project..."
if grep -q "embedAndSignAppleFrameworkForXcode" iosApp/iosApp.xcodeproj/project.pbxproj; then
    echo -e "${GREEN}✓ Build script found${NC}"
else
    echo -e "${YELLOW}⚠ Build script not found${NC}"
fi
echo ""

echo "5. Checking Xcode derived data..."
if [ -d ~/Library/Developer/Xcode/DerivedData ]; then
    echo -e "${GREEN}✓ Derived data directory exists${NC}"
    echo "Size: $(du -sh ~/Library/Developer/Xcode/DerivedData | cut -f1)"
else
    echo -e "${YELLOW}⚠ No derived data${NC}"
fi
echo ""

echo "=========================================="
echo "Recommendations:"
echo "=========================================="
echo ""
echo "The error 'Unable to find module dependency: shared' means:"
echo "Xcode cannot find the shared framework module."
echo ""
echo "This usually happens because:"
echo "1. The framework hasn't been built by Xcode's build script"
echo "2. Xcode's build cache is stale"
echo "3. The framework path is incorrect"
echo ""
echo "To fix:"
echo "1. In Xcode: Product → Clean Build Folder (hold Option, Cmd+Shift+K)"
echo "2. Delete derived data: rm -rf ~/Library/Developer/Xcode/DerivedData/iosApp-*"
echo "3. In Xcode: Product → Build (Cmd+B)"
echo ""
echo "The build script will automatically run and build the framework."
echo ""
echo "If that doesn't work:"
echo "1. Check Xcode build log for errors in the 'Build Shared Framework' phase"
echo "2. Look for Gradle errors in the build output"
echo "3. Verify the framework path in Xcode project settings"
echo ""
