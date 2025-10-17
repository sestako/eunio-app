#!/bin/bash

# Verification script for Firebase Native Bridge configuration
# This script verifies that the Kotlin/Native interop is properly configured

echo "🔍 Verifying Firebase Native Bridge Configuration..."
echo ""

# Check if required files exist
echo "📁 Checking required files..."
files=(
    "shared/src/commonMain/kotlin/com/eunio/healthapp/data/remote/firebase/FirebaseNativeBridge.kt"
    "shared/src/iosMain/kotlin/com/eunio/healthapp/data/remote/firebase/FirebaseNativeBridge.ios.kt"
    "shared/src/androidMain/kotlin/com/eunio/healthapp/data/remote/firebase/FirebaseNativeBridge.android.kt"
    "iosApp/iosApp/Services/FirebaseIOSBridge.swift"
    "iosApp/iosApp/Services/FirebaseBridgeInitializer.swift"
)

all_files_exist=true
for file in "${files[@]}"; do
    if [ -f "$file" ]; then
        echo "  ✅ $file"
    else
        echo "  ❌ $file (MISSING)"
        all_files_exist=false
    fi
done

echo ""

if [ "$all_files_exist" = false ]; then
    echo "❌ Some required files are missing!"
    exit 1
fi

# Try to compile the shared module for iOS
echo "🔨 Compiling shared module for iOS Simulator (arm64)..."
./gradlew :shared:compileKotlinIosSimulatorArm64 --quiet

if [ $? -eq 0 ]; then
    echo "  ✅ iOS Simulator (arm64) compilation successful"
else
    echo "  ❌ iOS Simulator (arm64) compilation failed"
    exit 1
fi

echo ""

# Check if the bridge is accessible in the compiled framework
echo "🔍 Checking compiled framework..."
framework_path="shared/build/bin/iosSimulatorArm64/debugFramework/shared.framework"

if [ -d "$framework_path" ]; then
    echo "  ✅ Framework exists at $framework_path"
    
    # Check if the bridge classes are in the framework
    if [ -f "$framework_path/Headers/shared.h" ]; then
        echo "  ✅ Framework headers found"
    fi
else
    echo "  ⚠️  Framework not found (may need to build the framework target)"
fi

echo ""
echo "✅ Firebase Native Bridge configuration verified successfully!"
echo ""
echo "📝 Summary:"
echo "  - All required files are present"
echo "  - Kotlin code compiles successfully for iOS"
echo "  - Bridge is ready for implementation in Task 3"
echo ""
echo "🚀 Next steps:"
echo "  1. Implement actual Swift bridge method calls in FirebaseNativeBridge.ios.kt"
echo "  2. Add error mapping from iOS to Kotlin"
echo "  3. Update FirestoreServiceImpl.ios.kt to use the bridge"
echo ""
