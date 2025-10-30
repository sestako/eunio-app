#!/bin/bash
set -e

FRAMEWORK_NAME="EunioBridgeKit"
BUILD_DIR="build"
XCFRAMEWORK_DIR="../shared/src/iosMain/c_interop/libs"

echo "🔨 Building ${FRAMEWORK_NAME} XCFramework..."

# Clean previous builds
rm -rf "${BUILD_DIR}"
rm -rf "${XCFRAMEWORK_DIR}/${FRAMEWORK_NAME}.xcframework"

# Create build directory
mkdir -p "${BUILD_DIR}"
mkdir -p "${XCFRAMEWORK_DIR}"

# Build for iOS device (arm64)
echo "📱 Building for iOS device (arm64)..."
xcodebuild archive \
    -scheme ${FRAMEWORK_NAME} \
    -archivePath "${BUILD_DIR}/${FRAMEWORK_NAME}-iphoneos.xcarchive" \
    -sdk iphoneos \
    -destination "generic/platform=iOS" \
    BUILD_LIBRARY_FOR_DISTRIBUTION=YES \
    SKIP_INSTALL=NO \
    ONLY_ACTIVE_ARCH=NO

# Build for iOS Simulator (arm64 + x86_64)
echo "🖥️  Building for iOS Simulator (arm64 + x86_64)..."
xcodebuild archive \
    -scheme ${FRAMEWORK_NAME} \
    -archivePath "${BUILD_DIR}/${FRAMEWORK_NAME}-iphonesimulator.xcarchive" \
    -sdk iphonesimulator \
    -destination "generic/platform=iOS Simulator" \
    BUILD_LIBRARY_FOR_DISTRIBUTION=YES \
    SKIP_INSTALL=NO \
    ONLY_ACTIVE_ARCH=NO

# Create XCFramework
echo "📦 Creating XCFramework..."
xcodebuild -create-xcframework \
    -framework "${BUILD_DIR}/${FRAMEWORK_NAME}-iphoneos.xcarchive/Products/Library/Frameworks/${FRAMEWORK_NAME}.framework" \
    -framework "${BUILD_DIR}/${FRAMEWORK_NAME}-iphonesimulator.xcarchive/Products/Library/Frameworks/${FRAMEWORK_NAME}.framework" \
    -output "${XCFRAMEWORK_DIR}/${FRAMEWORK_NAME}.xcframework"

echo "✅ XCFramework created at: ${XCFRAMEWORK_DIR}/${FRAMEWORK_NAME}.xcframework"

# List the contents
echo ""
echo "📋 XCFramework contents:"
ls -lh "${XCFRAMEWORK_DIR}/${FRAMEWORK_NAME}.xcframework"
