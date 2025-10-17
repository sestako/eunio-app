#!/bin/bash

# Complete Build Fix Script
# This script performs all necessary steps to fix the iOS build issues

set -e

echo "🔧 Complete iOS Build Fix"
echo "=========================="
echo ""

# Step 1: Kill Xcode if running
echo "Step 1: Closing Xcode..."
killall Xcode 2>/dev/null || echo "Xcode not running"
sleep 2

# Step 2: Clean derived data
echo ""
echo "Step 2: Cleaning Xcode derived data..."
rm -rf ~/Library/Developer/Xcode/DerivedData/iosApp-*
echo "✅ Derived data cleaned"

# Step 3: Detect architecture
echo ""
echo "Step 3: Detecting Mac architecture..."
ARCH=$(uname -m)
if [ "$ARCH" = "arm64" ]; then
    GRADLE_ARCH="iosSimulatorArm64"
    echo "✅ Detected Apple Silicon (M1/M2) - using $GRADLE_ARCH"
else
    GRADLE_ARCH="iosX64"
    echo "✅ Detected Intel Mac - using $GRADLE_ARCH"
fi

# Step 4: Clean Gradle build
echo ""
echo "Step 4: Cleaning Gradle build..."
cd "$(dirname "$0")/../../.."
./gradlew clean
echo "✅ Gradle build cleaned"

# Step 5: Build shared framework
echo ""
echo "Step 5: Building shared framework for $GRADLE_ARCH..."
./gradlew :shared:linkDebugFramework${GRADLE_ARCH^}
echo "✅ Framework built"

# Step 6: Verify framework
echo ""
echo "Step 6: Verifying framework..."
FRAMEWORK_PATH="shared/build/bin/$GRADLE_ARCH/debugFramework/shared.framework"
if [ -d "$FRAMEWORK_PATH" ]; then
    echo "✅ Framework exists at: $FRAMEWORK_PATH"
    
    # Check framework size
    FRAMEWORK_SIZE=$(du -sh "$FRAMEWORK_PATH" | cut -f1)
    echo "   Framework size: $FRAMEWORK_SIZE"
    
    # Check if Headers directory exists
    if [ -d "$FRAMEWORK_PATH/Headers" ]; then
        echo "✅ Headers directory exists"
    else
        echo "❌ Headers directory missing!"
        exit 1
    fi
    
    # Check if module.modulemap exists
    if [ -f "$FRAMEWORK_PATH/Modules/module.modulemap" ]; then
        echo "✅ Module map exists"
    else
        echo "❌ Module map missing!"
        exit 1
    fi
else
    echo "❌ Framework not found at: $FRAMEWORK_PATH"
    exit 1
fi

# Step 7: Copy framework to Xcode location
echo ""
echo "Step 7: Copying framework to Xcode location..."
XCODE_FRAMEWORK_DIR="shared/build/xcode-frameworks/Debug/iphonesimulator"
mkdir -p "$XCODE_FRAMEWORK_DIR"
cp -R "$FRAMEWORK_PATH" "$XCODE_FRAMEWORK_DIR/"
echo "✅ Framework copied to: $XCODE_FRAMEWORK_DIR"

# Step 8: Open Xcode
echo ""
echo "Step 8: Opening Xcode..."
cd iosApp
open iosApp.xcodeproj
echo "✅ Xcode opened"

echo ""
echo "=========================="
echo "✅ Build fix complete!"
echo ""
echo "Next steps:"
echo "1. Wait for Xcode to fully load"
echo "2. Press Cmd+B to build"
echo "3. Check that build succeeds"
echo ""
echo "If you still see errors:"
echo "- Check the build log (Cmd+9 → Reports)"
echo "- Look for 'Build Shared Framework' phase"
echo "- Verify no Gradle errors"
echo ""
