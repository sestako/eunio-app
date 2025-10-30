#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "🔧 Fixing Xcode Build Issues..."
echo ""

# Step 1: Clean Gradle build
echo "1️⃣  Cleaning Gradle build..."
./gradlew clean
echo -e "${GREEN}✓${NC} Gradle clean complete"
echo ""

# Step 2: Build shared framework
echo "2️⃣  Building shared framework for simulator..."
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓${NC} Shared framework built successfully"
else
    echo -e "${RED}✗${NC} Failed to build shared framework"
    exit 1
fi
echo ""

# Step 3: Clean Xcode derived data
echo "3️⃣  Cleaning Xcode derived data..."
rm -rf ~/Library/Developer/Xcode/DerivedData/iosApp-*
echo -e "${GREEN}✓${NC} Derived data cleaned"
echo ""

# Step 4: Reset Swift Package Manager cache
echo "4️⃣  Resetting Swift Package Manager cache..."
rm -rf ~/Library/Caches/org.swift.swiftpm
rm -rf iosApp/iosApp.xcodeproj/project.xcworkspace/xcshareddata/swiftpm/Package.resolved
echo -e "${GREEN}✓${NC} SPM cache cleared"
echo ""

echo -e "${YELLOW}📝 Next steps:${NC}"
echo "1. Open iosApp/iosApp.xcodeproj in Xcode"
echo "2. Wait for Xcode to resolve Swift packages (watch the status bar)"
echo "3. File → Packages → Resolve Package Versions"
echo "4. Product → Clean Build Folder (Cmd+Shift+K)"
echo "5. Product → Build (Cmd+B)"
echo ""
echo -e "${GREEN}✓ Cleanup complete!${NC}"
