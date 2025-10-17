#!/bin/bash

# iOS Build Validation Script for Eunio Health App
# This script validates the iOS build configuration and ensures App Store readiness

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
IOS_DIR="$PROJECT_DIR/iosApp"
XCODE_PROJECT="$IOS_DIR/iosApp.xcodeproj"
INFO_PLIST="$IOS_DIR/iosApp/Info.plist"
ENTITLEMENTS="$IOS_DIR/iosApp/iosApp.entitlements"
PRIVACY_MANIFEST="$IOS_DIR/iosApp/PrivacyInfo.xcprivacy"

echo -e "${BLUE}🔍 Starting iOS Build Validation for Eunio Health App${NC}"
echo "=================================================="

# Function to print status
print_status() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✅ $2${NC}"
    else
        echo -e "${RED}❌ $2${NC}"
        exit 1
    fi
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

# 1. Check if Xcode project exists
echo -e "\n${BLUE}1. Checking Xcode Project Structure${NC}"
if [ -f "$XCODE_PROJECT/project.pbxproj" ]; then
    print_status 0 "Xcode project file exists"
else
    print_status 1 "Xcode project file not found"
fi

# 2. Validate Info.plist
echo -e "\n${BLUE}2. Validating Info.plist${NC}"
if [ -f "$INFO_PLIST" ]; then
    print_status 0 "Info.plist exists"
    
    # Check required keys
    required_keys=(
        "CFBundleIdentifier"
        "CFBundleShortVersionString"
        "CFBundleVersion"
        "NSHealthShareUsageDescription"
        "NSHealthUpdateUsageDescription"
        "LSApplicationCategoryType"
    )
    
    for key in "${required_keys[@]}"; do
        if /usr/libexec/PlistBuddy -c "Print :$key" "$INFO_PLIST" >/dev/null 2>&1; then
            print_status 0 "$key is present"
        else
            print_status 1 "$key is missing from Info.plist"
        fi
    done
    
    # Check if using build variables
    version_string=$(/usr/libexec/PlistBuddy -c "Print :CFBundleShortVersionString" "$INFO_PLIST" 2>/dev/null || echo "")
    if [[ "$version_string" == *"MARKETING_VERSION"* ]]; then
        print_status 0 "Using MARKETING_VERSION variable"
    else
        print_warning "Consider using \$(MARKETING_VERSION) for version string"
    fi
    
else
    print_status 1 "Info.plist not found"
fi

# 3. Validate Entitlements
echo -e "\n${BLUE}3. Validating Entitlements${NC}"
if [ -f "$ENTITLEMENTS" ]; then
    print_status 0 "Entitlements file exists"
    
    # Check HealthKit entitlement
    if /usr/libexec/PlistBuddy -c "Print :com.apple.developer.healthkit" "$ENTITLEMENTS" >/dev/null 2>&1; then
        print_status 0 "HealthKit entitlement is present"
    else
        print_status 1 "HealthKit entitlement is missing"
    fi
    
    # Check background delivery
    if /usr/libexec/PlistBuddy -c "Print :com.apple.developer.healthkit.background-delivery" "$ENTITLEMENTS" >/dev/null 2>&1; then
        print_status 0 "HealthKit background delivery entitlement is present"
    else
        print_warning "HealthKit background delivery entitlement is missing"
    fi
    
else
    print_status 1 "Entitlements file not found"
fi

# 4. Validate Privacy Manifest
echo -e "\n${BLUE}4. Validating Privacy Manifest${NC}"
if [ -f "$PRIVACY_MANIFEST" ]; then
    print_status 0 "Privacy manifest exists"
    
    # Check if health data collection is declared
    if grep -q "NSPrivacyCollectedDataTypeHealthAndFitness" "$PRIVACY_MANIFEST"; then
        print_status 0 "Health data collection is declared"
    else
        print_status 1 "Health data collection not declared in privacy manifest"
    fi
    
else
    print_status 1 "Privacy manifest not found"
fi

# 5. Check Build Configuration
echo -e "\n${BLUE}5. Checking Build Configuration${NC}"
if [ -f "$XCODE_PROJECT/project.pbxproj" ]; then
    # Check deployment target
    deployment_target=$(grep -o "IPHONEOS_DEPLOYMENT_TARGET = [0-9.]*" "$XCODE_PROJECT/project.pbxproj" | head -1 | cut -d' ' -f3)
    if [ ! -z "$deployment_target" ]; then
        print_status 0 "iOS deployment target: $deployment_target"
        
        # Check if deployment target is recent enough
        if [[ $(echo "$deployment_target >= 15.0" | bc -l) -eq 1 ]]; then
            print_status 0 "Deployment target is iOS 15.0 or later"
        else
            print_warning "Consider updating deployment target to iOS 15.0 or later"
        fi
    else
        print_warning "Could not determine deployment target"
    fi
    
    # Check Swift version
    swift_version=$(grep -o "SWIFT_VERSION = [0-9.]*" "$XCODE_PROJECT/project.pbxproj" | head -1 | cut -d' ' -f3)
    if [ ! -z "$swift_version" ]; then
        print_status 0 "Swift version: $swift_version"
    else
        print_warning "Could not determine Swift version"
    fi
    
    # Check if manual code signing is configured for release
    if grep -q "CODE_SIGN_STYLE = Manual" "$XCODE_PROJECT/project.pbxproj"; then
        print_status 0 "Manual code signing is configured"
    else
        print_warning "Consider using manual code signing for App Store builds"
    fi
    
    # Check if provisioning profile is specified
    if grep -q "PROVISIONING_PROFILE_SPECIFIER" "$XCODE_PROJECT/project.pbxproj"; then
        print_status 0 "Provisioning profile specifier is configured"
    else
        print_warning "Provisioning profile specifier should be configured for distribution"
    fi
fi

# 6. Check Shared Framework Integration
echo -e "\n${BLUE}6. Checking Shared Framework Integration${NC}"
shared_framework_path="$PROJECT_DIR/shared/build/bin"
if [ -d "$shared_framework_path" ]; then
    print_status 0 "Shared framework build directory exists"
else
    print_warning "Shared framework not built yet - run './gradlew :shared:embedAndSignAppleFrameworkForXcode' first"
fi

# Check build script
if grep -q "embedAndSignAppleFrameworkForXcode" "$XCODE_PROJECT/project.pbxproj"; then
    print_status 0 "Shared framework build script is configured"
else
    print_status 1 "Shared framework build script is missing"
fi

# 7. Validate App Store Requirements
echo -e "\n${BLUE}7. App Store Requirements Check${NC}"

# Check bundle identifier format
bundle_id=$(grep -o "PRODUCT_BUNDLE_IDENTIFIER = [^;]*" "$XCODE_PROJECT/project.pbxproj" | head -1 | cut -d' ' -f3)
if [[ "$bundle_id" =~ ^[a-zA-Z0-9.-]+$ ]]; then
    print_status 0 "Bundle identifier format is valid: $bundle_id"
else
    print_warning "Bundle identifier format may be invalid: $bundle_id"
fi

# Check if app uses encryption
if grep -q "ITSAppUsesNonExemptEncryption" "$INFO_PLIST"; then
    encryption_value=$(/usr/libexec/PlistBuddy -c "Print :ITSAppUsesNonExemptEncryption" "$INFO_PLIST" 2>/dev/null || echo "")
    if [ "$encryption_value" = "false" ]; then
        print_status 0 "App declares no encryption usage"
    else
        print_warning "App declares encryption usage - ensure compliance documentation is ready"
    fi
else
    print_warning "ITSAppUsesNonExemptEncryption not declared - add to Info.plist"
fi

# 8. Check for common issues
echo -e "\n${BLUE}8. Common Issues Check${NC}"

# Check for hardcoded team ID
if grep -q "DEVELOPMENT_TEAM = [A-Z0-9]" "$XCODE_PROJECT/project.pbxproj"; then
    team_id=$(grep -o "DEVELOPMENT_TEAM = [A-Z0-9]*" "$XCODE_PROJECT/project.pbxproj" | head -1 | cut -d' ' -f3)
    print_status 0 "Development team is configured: $team_id"
else
    print_warning "Development team should be configured"
fi

# Check for bitcode (should be disabled for KMP)
if grep -q "ENABLE_BITCODE = NO" "$XCODE_PROJECT/project.pbxproj"; then
    print_status 0 "Bitcode is disabled (correct for KMP)"
else
    print_warning "Bitcode should be disabled for Kotlin Multiplatform projects"
fi

# Final summary
echo -e "\n${BLUE}=================================================="
echo -e "🏁 iOS Build Validation Complete${NC}"
echo -e "\n${GREEN}✅ Build configuration appears ready for App Store submission${NC}"
echo -e "\n${YELLOW}📋 Pre-submission checklist:${NC}"
echo "   • Test on physical devices"
echo "   • Verify all health data flows work correctly"
echo "   • Test accessibility features"
echo "   • Generate and test release build"
echo "   • Validate with App Store Connect"
echo ""
echo -e "${BLUE}💡 To build for App Store:${NC}"
echo "   cd $IOS_DIR && fastlane build_release"
echo ""
echo -e "${BLUE}💡 To submit to TestFlight:${NC}"
echo "   cd $IOS_DIR && fastlane beta"
echo ""

exit 0