#!/bin/bash
set -e

# Change to script directory
cd "$(dirname "$0")"

FRAMEWORK_NAME="EunioBridgeKit"
BUILD_DIR="build"
XCFRAMEWORK_DIR="../shared/src/iosMain/c_interop/libs"

echo "🔨 Building ${FRAMEWORK_NAME} framework directly..."

# Clean previous builds
rm -rf "${BUILD_DIR}"
rm -rf "${XCFRAMEWORK_DIR}/${FRAMEWORK_NAME}.xcframework"

# Create build directories
mkdir -p "${BUILD_DIR}/iphoneos"
mkdir -p "${BUILD_DIR}/iphonesimulator"
mkdir -p "${XCFRAMEWORK_DIR}"

# Function to build framework for a specific SDK and architecture
build_framework() {
    local SDK=$1
    local ARCH=$2
    local OUTPUT_DIR="${BUILD_DIR}/${SDK}"
    local FRAMEWORK_DIR="${OUTPUT_DIR}/${FRAMEWORK_NAME}.framework"
    
    echo "📱 Building for ${SDK} (${ARCH})..."
    
    # Create framework structure
    mkdir -p "${FRAMEWORK_DIR}/Headers"
    mkdir -p "${FRAMEWORK_DIR}/Modules"
    
    # Copy headers
    cp EunioBridgeKit/*.h "${FRAMEWORK_DIR}/Headers/"
    
    # Create module map
    cat > "${FRAMEWORK_DIR}/Modules/module.modulemap" <<EOF
framework module ${FRAMEWORK_NAME} {
    umbrella header "${FRAMEWORK_NAME}.h"
    export *
    module * { export * }
}
EOF
    
    # Copy Info.plist
    cp EunioBridgeKit/Info.plist "${FRAMEWORK_DIR}/Info.plist"
    
    # Since this is a header-only framework (protocol definition), create a minimal binary
    # We'll create an empty .m file and compile it
    echo "// Empty implementation for header-only framework" > "${BUILD_DIR}/empty.m"
    
    # Compile the empty file to create the framework binary
    local SDK_PATH=$(xcrun --sdk ${SDK} --show-sdk-path)
    xcrun clang \
        -arch ${ARCH} \
        -dynamiclib \
        -isysroot "${SDK_PATH}" \
        -install_name "@rpath/${FRAMEWORK_NAME}.framework/${FRAMEWORK_NAME}" \
        -framework Foundation \
        -o "${FRAMEWORK_DIR}/${FRAMEWORK_NAME}" \
        "${BUILD_DIR}/empty.m"
    
    # Sign the framework
    codesign --force --sign - "${FRAMEWORK_DIR}"
    
    echo "✅ Built ${SDK} (${ARCH})"
}

# Build for different platforms
build_framework "iphoneos" "arm64"

# For simulator, we need to build a fat binary with both architectures
echo "📱 Building for iphonesimulator (arm64 + x86_64)..."
OUTPUT_DIR="${BUILD_DIR}/iphonesimulator"
FRAMEWORK_DIR="${OUTPUT_DIR}/${FRAMEWORK_NAME}.framework"

mkdir -p "${FRAMEWORK_DIR}/Headers"
mkdir -p "${FRAMEWORK_DIR}/Modules"

cp EunioBridgeKit/*.h "${FRAMEWORK_DIR}/Headers/"

cat > "${FRAMEWORK_DIR}/Modules/module.modulemap" <<EOF
framework module ${FRAMEWORK_NAME} {
    umbrella header "${FRAMEWORK_NAME}.h"
    export *
    module * { export * }
}
EOF

cp EunioBridgeKit/Info.plist "${FRAMEWORK_DIR}/Info.plist"

SDK_PATH=$(xcrun --sdk iphonesimulator --show-sdk-path)

# Build for arm64
xcrun clang \
    -arch arm64 \
    -dynamiclib \
    -isysroot "${SDK_PATH}" \
    -install_name "@rpath/${FRAMEWORK_NAME}.framework/${FRAMEWORK_NAME}" \
    -framework Foundation \
    -o "${BUILD_DIR}/empty_arm64.dylib" \
    "${BUILD_DIR}/empty.m"

# Build for x86_64
xcrun clang \
    -arch x86_64 \
    -dynamiclib \
    -isysroot "${SDK_PATH}" \
    -install_name "@rpath/${FRAMEWORK_NAME}.framework/${FRAMEWORK_NAME}" \
    -framework Foundation \
    -o "${BUILD_DIR}/empty_x86_64.dylib" \
    "${BUILD_DIR}/empty.m"

# Create fat binary
lipo -create \
    "${BUILD_DIR}/empty_arm64.dylib" \
    "${BUILD_DIR}/empty_x86_64.dylib" \
    -output "${FRAMEWORK_DIR}/${FRAMEWORK_NAME}"

codesign --force --sign - "${FRAMEWORK_DIR}"

echo "✅ Built iphonesimulator (arm64 + x86_64)"

# Create XCFramework
echo "📦 Creating XCFramework..."
xcodebuild -create-xcframework \
    -framework "${BUILD_DIR}/iphoneos/${FRAMEWORK_NAME}.framework" \
    -framework "${BUILD_DIR}/iphonesimulator/${FRAMEWORK_NAME}.framework" \
    -output "${XCFRAMEWORK_DIR}/${FRAMEWORK_NAME}.xcframework"

echo "✅ XCFramework created at: ${XCFRAMEWORK_DIR}/${FRAMEWORK_NAME}.xcframework"

# List the contents
echo ""
echo "📋 XCFramework structure:"
find "${XCFRAMEWORK_DIR}/${FRAMEWORK_NAME}.xcframework" -type f | head -20
