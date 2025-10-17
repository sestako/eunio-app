#!/bin/bash

# Force Framework Build - Bypass Xcode
# This script builds the framework and copies it to where Xcode expects it

echo "=========================================="
echo "Force Building Shared Framework"
echo "=========================================="
echo ""

# Detect architecture
ARCH=$(uname -m)
if [ "$ARCH" = "arm64" ]; then
    GRADLE_ARCH="iosSimulatorArm64"
    XCODE_ARCH="arm64"
else
    GRADLE_ARCH="iosX64"
    XCODE_ARCH="x86_64"
fi

echo "Detected architecture: $ARCH"
echo "Gradle target: $GRADLE_ARCH"
echo ""

# Clean first
echo "Cleaning..."
./gradlew clean
echo ""

# Build the framework
echo "Building framework for $GRADLE_ARCH..."

# Capitalize first letter for task name
if [ "$GRADLE_ARCH" = "iosSimulatorArm64" ]; then
    TASK_NAME="IosSimulatorArm64"
elif [ "$GRADLE_ARCH" = "iosX64" ]; then
    TASK_NAME="IosX64"
else
    TASK_NAME="IosArm64"
fi

./gradlew :shared:linkDebugFramework${TASK_NAME}
if [ $? -ne 0 ]; then
    echo "❌ Framework build failed"
    exit 1
fi
echo "✅ Framework built"
echo ""

# Create Xcode frameworks directory
XCODE_FRAMEWORKS_DIR="shared/build/xcode-frameworks/Debug/iphonesimulator"
mkdir -p "$XCODE_FRAMEWORKS_DIR"

# Copy framework to Xcode location
FRAMEWORK_SOURCE="shared/build/bin/$GRADLE_ARCH/debugFramework/shared.framework"
FRAMEWORK_DEST="$XCODE_FRAMEWORKS_DIR/shared.framework"

echo "Copying framework..."
echo "From: $FRAMEWORK_SOURCE"
echo "To: $FRAMEWORK_DEST"

if [ -d "$FRAMEWORK_SOURCE" ]; then
    rm -rf "$FRAMEWORK_DEST"
    cp -R "$FRAMEWORK_SOURCE" "$FRAMEWORK_DEST"
    echo "✅ Framework copied"
else
    echo "❌ Framework not found at $FRAMEWORK_SOURCE"
    exit 1
fi

echo ""
echo "=========================================="
echo "Framework Ready"
echo "=========================================="
echo ""
echo "The framework has been built and placed where Xcode expects it."
echo ""
echo "Next steps:"
echo "1. In Xcode: Product → Clean Build Folder (hold Option)"
echo "2. In Xcode: Product → Build"
echo ""
echo "The Swift errors should now be resolved because the framework is available."
echo ""
