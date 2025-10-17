#!/bin/bash

# Rebuild iOS Framework Script
# This script rebuilds the shared Kotlin framework for iOS

echo "=========================================="
echo "Rebuilding iOS Shared Framework"
echo "=========================================="
echo ""

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "Step 1: Clean build..."
./gradlew clean
if [ $? -ne 0 ]; then
    echo -e "${RED}✗ Clean failed${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Clean successful${NC}"
echo ""

echo "Step 2: Build shared framework for iOS Simulator (ARM64)..."
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
if [ $? -ne 0 ]; then
    echo -e "${RED}✗ Framework build failed${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Framework built successfully${NC}"
echo ""

echo "Step 3: Verify framework exists..."
FRAMEWORK_PATH="shared/build/bin/iosSimulatorArm64/debugFramework/shared.framework"
if [ -d "$FRAMEWORK_PATH" ]; then
    echo -e "${GREEN}✓ Framework found at: $FRAMEWORK_PATH${NC}"
    echo ""
    echo "Framework contents:"
    ls -la "$FRAMEWORK_PATH"
else
    echo -e "${RED}✗ Framework not found at: $FRAMEWORK_PATH${NC}"
    exit 1
fi
echo ""

echo "=========================================="
echo "Next Steps:"
echo "=========================================="
echo "1. Open Xcode: cd iosApp && open iosApp.xcodeproj"
echo "2. Clean Build Folder: Product → Clean Build Folder (Cmd+Shift+K)"
echo "3. Build: Product → Build (Cmd+B)"
echo "4. Run: Product → Run (Cmd+R)"
echo ""
echo -e "${GREEN}Framework rebuild complete!${NC}"
