# Design Document: Kotlin 2.2.20 + Xcode 26 + iOS 26 SDK Upgrade

## Overview

This design document outlines the approach for upgrading the Eunio Health App from Kotlin 1.9.21 to Kotlin 2.2.20, and from iOS 17 SDK to iOS 26 SDK with Xcode 26. The upgrade will modernize the development toolchain while maintaining backward compatibility with iOS 15.0+ devices.

**Note:** Starting with version 26, Apple unified the versioning for Xcode and iOS SDK, making them both version 26. This simplifies toolchain management and ensures consistent compatibility between development tools and target platforms.

**macOS Versioning Update:** Starting with version 26, Apple unified OS versioning across iOS, macOS, iPadOS, and Xcode. macOS 26 (Tahoe) replaces Sequoia 15, aligning macOS version numbers with iOS and Xcode for the first time.

### Current State
- Kotlin version: 1.9.21
- Xcode: 15.4 (or older)
- iOS SDK: iOS 17
- iOS deployment target: iOS 15.0
- Android Gradle Plugin: 8.2.2
- Gradle: 8.2.2

### Target State
- Kotlin version: 2.2.20
- Xcode: 26.x (latest stable)
- iOS SDK: iOS 26
- iOS deployment target: iOS 15.0 (maintained)
- Android Gradle Plugin: 8.7+ (compatible with Kotlin 2.2.20)
- Gradle: 8.10+ (compatible with Kotlin 2.2.20)

## Architecture

### Upgrade Strategy

```
┌─────────────────────────────────────────────────────────────┐
│                    Upgrade Phases                            │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  Phase 1: Preparation                                        │
│  ┌────────────────────────────────────────────┐             │
│  │ • Create backup branch                      │             │
│  │ • Document current versions                 │             │
│  │ • Review Kotlin 2.2 migration guide         │             │
│  │ • Check dependency compatibility            │             │
│  └────────────────────────────────────────────┘             │
│                         │                                     │
│  Phase 2: Kotlin Upgrade                                     │
│  ┌────────────────────▼───────────────────────┐             │
│  │ • Update Kotlin to 2.2.20                   │             │
│  │ • Update Kotlin compiler plugins (KSP, etc) │             │
│  │ • Update kotlinx libraries                  │             │
│  │ • Update Compose Multiplatform              │             │
│  │ • Enable strict dependency verification     │             │
│  │ • Update other Kotlin dependencies          │             │
│  │ • Fix compilation errors                    │             │
│  │ • Run Android tests                         │             │
│  └────────────────────────────────────────────┘             │
│                         │                                     │
│  Phase 3: Xcode & iOS SDK Upgrade                           │
│  ┌────────────────────▼───────────────────────┐             │
│  │ • Install Xcode 26                          │             │
│  │ • Update iOS project settings               │             │
│  │ • Disable Bitcode (deprecated in iOS 26)    │             │
│  │ • Configure arm64 + x86_64 architectures    │             │
│  │ • Migrate to Swift Package Manager (SPM)    │             │
│  │ • Update CocoaPods dependencies             │             │
│  │ • Fix iOS compilation errors                │             │
│  │ • Run iOS tests                             │             │
│  └────────────────────────────────────────────┘             │
│                         │                                     │
│  Phase 4: Integration Testing                                │
│  ┌────────────────────▼───────────────────────┐             │
│  │ • Test Firebase on both platforms           │             │
│  │ • Test cross-platform sync                  │             │
│  │ • Test all major features                   │             │
│  │ • Performance validation                    │             │
│  └────────────────────────────────────────────┘             │
│                         │                                     │
│  Phase 5: CI/CD & Documentation                              │
│  ┌────────────────────▼───────────────────────┐             │
│  │ • Create toolchain verification script      │             │
│  │ • Update CI/CD to macOS 26 (Tahoe)         │             │
│  │ • Configure CI caching (Gradle/CocoaPods)   │             │
│  │ • Update documentation                      │             │
│  │ • Create rollback plan                      │             │
│  │ • Final validation                          │             │
│  └────────────────────────────────────────────┘             │
└─────────────────────────────────────────────────────────────┘
```

## Components and Interfaces

### 1. Kotlin Version Configuration

**File:** `gradle/libs.versions.toml`

**Current:**
```toml
[versions]
kotlin = "1.9.21"
kotlinx-coroutines = "1.7.3"
kotlinx-serialization = "1.6.2"
kotlinx-datetime = "0.5.0"
compose-plugin = "1.5.11"
ktor = "2.3.7"
koin = "3.5.3"
sqlDelight = "2.0.1"
```

**Target:**
```toml
[versions]
kotlin = "2.2.20"
kotlinx-coroutines = "1.9.0"  # Compatible with Kotlin 2.2
kotlinx-serialization = "1.7.3"  # Compatible with Kotlin 2.2
kotlinx-datetime = "0.6.1"  # Compatible with Kotlin 2.2
compose-plugin = "1.7.1"  # Compatible with Kotlin 2.2
ktor = "3.0.1"  # Compatible with Kotlin 2.2
koin = "4.0.0"  # Compatible with Kotlin 2.2
sqlDelight = "2.0.2"  # Compatible with Kotlin 2.2
agp = "8.7.3"  # Android Gradle Plugin compatible with Kotlin 2.2
```

### 2. Gradle Configuration

**File:** `build.gradle.kts` (root)

**Updates needed:**
```kotlin
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.sqlDelight) apply false
    alias(libs.plugins.googleServices) apply false
    kotlin("android") version "2.2.20" apply false
}
```

**File:** `gradle.properties`

**Add/Update:**
```properties
# Kotlin compiler options for 2.2.20
kotlin.mpp.stability.nowarn=true
kotlin.mpp.androidSourceSetLayoutVersion=2
kotlin.native.cacheKind=none  # Disable cache during upgrade for clean build

# Dependency verification
dependencyVerificationMode=strict
```

### 3. iOS Project Configuration

**File:** `iosApp/iosApp.xcodeproj/project.pbxproj`

**Settings to update:**
- `IPHONEOS_DEPLOYMENT_TARGET = 15.0` (maintain)
- `TARGETED_DEVICE_FAMILY = "1,2"` (iPhone and iPad)
- Xcode will automatically use iOS 26 SDK when opened in Xcode 26

**File:** `iosApp/Podfile` (if using CocoaPods)

**Update:**
```ruby
platform :ios, '15.0'  # Minimum deployment target

target 'iosApp' do
  use_frameworks!
  
  # Firebase pods - ensure latest versions compatible with iOS 26
  pod 'Firebase/Auth'
  pod 'Firebase/Firestore'
  pod 'Firebase/Functions'
  
  # Other dependencies
end
```

**Note:** Prefer Swift Package Manager (SPM) for Firebase integration unless dependencies explicitly require CocoaPods.

### 4. Shared Module Configuration

**File:** `shared/build.gradle.kts`

**iOS target configuration:**
```kotlin
kotlin {
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = true
            
            // Disable Bitcode (deprecated in iOS 26)
            bitcode = Framework.BitcodeEmbeddingMode.DISABLE
            
            // Support both arm64 and x86_64 simulator architectures
            // Ensure compatibility with iOS 26
            freeCompilerArgs += listOf(
                "-Xbinary=bundleId=com.eunio.healthapp.shared"
            )
        }
    }
    
    sourceSets {
        commonMain.dependencies {
            // Updated dependencies
        }
        
        iosMain.dependencies {
            // iOS-specific dependencies
        }
    }
}
```

### 5. Toolchain Verification Script

**Purpose**: Automated validation of toolchain versions before build to catch configuration issues early.

**File:** `scripts/verify-toolchain.sh`

**Implementation:**
```bash
#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "🔍 Verifying Toolchain Versions..."
echo ""

ERRORS=0

# Check Kotlin version
echo "Checking Kotlin version..."
KOTLIN_VERSION=$(grep 'kotlin = ' gradle/libs.versions.toml | cut -d'"' -f2)
if [ "$KOTLIN_VERSION" = "2.2.20" ]; then
    echo -e "${GREEN}✓${NC} Kotlin: $KOTLIN_VERSION"
else
    echo -e "${RED}✗${NC} Kotlin: $KOTLIN_VERSION (expected 2.2.20)"
    ERRORS=$((ERRORS + 1))
fi

# Check Gradle version
echo "Checking Gradle version..."
GRADLE_VERSION=$(./gradlew --version | grep "Gradle" | awk '{print $2}')
GRADLE_MAJOR=$(echo $GRADLE_VERSION | cut -d'.' -f1)
GRADLE_MINOR=$(echo $GRADLE_VERSION | cut -d'.' -f2)
if [ "$GRADLE_MAJOR" -ge 8 ] && [ "$GRADLE_MINOR" -ge 10 ]; then
    echo -e "${GREEN}✓${NC} Gradle: $GRADLE_VERSION"
else
    echo -e "${RED}✗${NC} Gradle: $GRADLE_VERSION (expected 8.10+)"
    ERRORS=$((ERRORS + 1))
fi

# Check Xcode version (macOS only)
if [[ "$OSTYPE" == "darwin"* ]]; then
    echo "Checking Xcode version..."
    XCODE_VERSION=$(xcodebuild -version | grep "Xcode" | awk '{print $2}')
    XCODE_MAJOR=$(echo $XCODE_VERSION | cut -d'.' -f1)
    if [ "$XCODE_MAJOR" = "26" ]; then
        echo -e "${GREEN}✓${NC} Xcode: $XCODE_VERSION"
    else
        echo -e "${RED}✗${NC} Xcode: $XCODE_VERSION (expected 26.x)"
        ERRORS=$((ERRORS + 1))
    fi
fi

echo ""
if [ $ERRORS -eq 0 ]; then
    echo -e "${GREEN}✓ All toolchain versions are correct!${NC}"
    exit 0
else
    echo -e "${RED}✗ Found $ERRORS toolchain version mismatch(es)${NC}"
    echo ""
    echo "Please update your toolchain to the required versions:"
    echo "  - Kotlin: 2.2.20"
    echo "  - Gradle: 8.10+"
    echo "  - Xcode: 26.x (macOS only)"
    exit 1
fi
```

**Usage:**
```bash
# Make script executable
chmod +x scripts/verify-toolchain.sh

# Run manually
./scripts/verify-toolchain.sh

# In CI (GitHub Actions)
- name: Verify Toolchain
  run: ./scripts/verify-toolchain.sh
```

### 6. CI/CD Configuration

**File:** `.github/workflows/build.yml`

**Updates for macOS 26 (Tahoe) and caching:**
```yaml
name: Build and Test

on: [push, pull_request]

jobs:
  android:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Verify Toolchain
        run: ./scripts/verify-toolchain.sh
      
      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Setup Gradle Cache
        uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
          restore-keys: |
            ${{ runner.os }}-gradle-
      
      - name: Build Android
        run: ./gradlew :androidApp:assembleDebug
      
      - name: Run Android Tests
        run: ./gradlew :androidApp:testDebugUnitTest

  ios:
    runs-on: macos-26  # macOS 26 (Tahoe) with Xcode 26
    steps:
      - uses: actions/checkout@v4
      
      - name: Verify Toolchain
        run: ./scripts/verify-toolchain.sh
      
      - name: Select Xcode 26
        run: sudo xcode-select -s /Applications/Xcode_26.app
      
      - name: Setup Gradle Cache
        uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
          restore-keys: |
            ${{ runner.os }}-gradle-
      
      - name: Setup CocoaPods Cache
        uses: actions/cache@v4
        with:
          path: Pods
          key: ${{ runner.os }}-pods-${{ hashFiles('**/Podfile.lock') }}
          restore-keys: |
            ${{ runner.os }}-pods-
      
      - name: Build iOS Framework
        run: ./gradlew :shared:linkDebugFrameworkIosArm64
      
      - name: Install CocoaPods
        run: |
          cd iosApp
          pod install
      
      - name: Build iOS App
        run: |
          xcodebuild -workspace iosApp/iosApp.xcworkspace \
            -scheme iosApp \
            -destination 'platform=iOS Simulator,name=iPhone 15,OS=26.0' \
            build
      
      - name: Run iOS Tests
        run: |
          xcodebuild test -workspace iosApp/iosApp.xcworkspace \
            -scheme iosApp \
            -destination 'platform=iOS Simulator,name=iPhone 15,OS=26.0'
```

## Data Models

### Version Tracking

Create a new file to track versions:

**File:** `versions.md`

```markdown
# Version History

## Current Versions (Post-Upgrade)
- Kotlin: 2.2.20
- Xcode: 26.x
- iOS SDK: 26
- iOS Deployment Target: 15.0
- Android Gradle Plugin: 8.7.3
- Gradle: 8.10

## Previous Versions
- Kotlin: 1.9.21
- Xcode: 15.4
- iOS SDK: 17
- iOS Deployment Target: 15.0
- Android Gradle Plugin: 8.2.2
- Gradle: 8.2.2

## Upgrade Date
[Date of upgrade]

## Breaking Changes
[Document any breaking changes encountered]
```

## Error Handling

### Kotlin 2.2 Migration Issues

**Common issues and solutions:**

1. **Kotlin/Native cinterop changes**
   - Error: "cinterop definition file format changed"
   - Solution: Update .def files if using custom cinterop

2. **Coroutines API changes**
   - Error: "Deprecated API usage"
   - Solution: Update to new coroutines APIs

3. **Serialization changes**
   - Error: "Serializer not found"
   - Solution: Regenerate serializers with new plugin

4. **Compose Multiplatform compatibility**
   - Error: "Compose version incompatible"
   - Solution: Update to Compose 1.7.1+

### iOS 26 SDK Issues

**Common issues and solutions:**

1. **Deprecated iOS APIs**
   - Warning: "API deprecated in iOS 26"
   - Solution: Update to new iOS 26 APIs or add availability checks

2. **SwiftUI changes**
   - Error: "SwiftUI API changed"
   - Solution: Update SwiftUI code to iOS 26 patterns

3. **Privacy manifest requirements**
   - Error: "Privacy manifest missing"
   - Solution: Add PrivacyInfo.xcprivacy file if needed

## Testing Strategy

### 1. Compilation Testing

**Phase 1: Kotlin Compilation**
```bash
# Clean build
./gradlew clean

# Build shared module
./gradlew :shared:build

# Build Android app
./gradlew :androidApp:assembleDebug

# Expected: All tasks succeed
```

**Phase 2: iOS Compilation**
```bash
# Build iOS framework
./gradlew :shared:linkDebugFrameworkIosArm64

# Open Xcode and build
open iosApp/iosApp.xcodeproj

# Expected: Build succeeds in Xcode 26
```

### 2. Unit Testing

```bash
# Run all unit tests
./gradlew test

# Run Android tests
./gradlew :androidApp:testDebugUnitTest

# Run iOS tests (in Xcode)
# Product > Test (Cmd+U)

# Expected: All tests pass
```

### 3. Integration Testing

**Test scenarios:**
1. User authentication (sign-up, sign-in, sign-out)
2. Daily log creation and retrieval
3. Cross-platform sync (Android → iOS, iOS → Android)
4. Offline mode and sync recovery
5. Settings persistence
6. Firebase operations

### 4. Performance Testing

**Metrics to measure:**
- App startup time
- Build time (Gradle + Xcode)
- Firebase sync latency
- Memory usage
- UI responsiveness

**Acceptance criteria:**
- No metric should degrade by more than 10%
- Ideally, some metrics improve with newer Kotlin version

## Rollback Procedure

### Git Branch Strategy

```bash
# Before starting upgrade
git checkout -b upgrade/kotlin-2.2-ios26
git push -u origin upgrade/kotlin-2.2-ios26

# Main branch remains on old versions
# Can quickly revert if needed
```

### Rollback Steps

If critical issues are found:

1. **Revert Kotlin version**
   ```bash
   # Checkout main branch
   git checkout main
   
   # Or revert specific commits
   git revert <commit-hash>
   ```

2. **Revert Xcode**
   ```bash
   # Switch back to Xcode 15.4 (if still installed)
   sudo xcode-select -s /Applications/Xcode_15.4.app
   ```

3. **Clean and rebuild**
   ```bash
   ./gradlew clean
   rm -rf build
   rm -rf .gradle
   ./gradlew build
   ```

4. **Verify rollback**
   - Run tests
   - Test on devices
   - Verify Firebase works

## Migration Guide

### Step-by-Step Process

#### Step 1: Preparation (30 minutes)
1. Create backup branch
2. Document current state
3. Review Kotlin 2.2 release notes
4. Check dependency compatibility matrix

#### Step 2: Update Gradle Files (1 hour)
1. Update `gradle/libs.versions.toml`
2. Update `build.gradle.kts` files
3. Update `gradle.properties`
4. Sync Gradle files

#### Step 3: Fix Kotlin Compilation (2-4 hours)
1. Run `./gradlew clean build`
2. Fix compilation errors one by one
3. Update deprecated API usage
4. Run Android tests

#### Step 4: Install Xcode 26 (1 hour)
1. Download Xcode 26 from App Store
2. Install to /Applications/Xcode.app
3. Run `sudo xcode-select -s /Applications/Xcode.app`
4. Verify: `xcodebuild -version`

#### Step 5: Update iOS Project (2-3 hours)
1. Open project in Xcode 26
2. Update project settings if prompted
3. Run `pod install` or `pod update`
4. Fix iOS compilation errors
5. Run iOS tests

#### Step 6: Integration Testing (3-4 hours)
1. Test Firebase authentication
2. Test data sync
3. Test all major features
4. Test on physical devices
5. Performance validation

#### Step 7: CI/CD Updates (1-2 hours)
1. Update GitHub Actions workflows
2. Test CI builds
3. Update documentation

**Total estimated time: 10-16 hours**

## Dependency Compatibility Matrix

| Dependency | Current | Target | Status | Notes |
|------------|---------|--------|--------|-------|
| Kotlin | 1.9.21 | 2.2.20 | ✅ Stable | Update all compiler plugins |
| Kotlin Serialization Plugin | 1.9.21 | 2.2.20 | ✅ Compatible | Must match Kotlin version |
| KSP | 1.9.21-1.0.16 | 2.2.20-1.0.28 | ✅ Compatible | Update for Kotlin 2.2.20 |
| kotlinx-coroutines | 1.7.3 | 1.9.0 | ✅ Compatible | |
| kotlinx-serialization | 1.6.2 | 1.7.3 | ✅ Compatible | |
| kotlinx-datetime | 0.5.0 | 0.6.1 | ✅ Compatible | |
| Compose Multiplatform | 1.5.11 | 1.7.1 | ✅ Compatible | Compiler plugin must match |
| Ktor | 2.3.7 | 3.0.1 | ⚠️ Major version change | Review migration guide |
| Koin | 3.5.3 | 4.0.0 | ⚠️ Major version change | Review migration guide |
| SQLDelight | 2.0.1 | 2.0.2 | ✅ Compatible | |
| Gradle | 8.2.2 | 8.10+ | ✅ Required | Minimum for Kotlin 2.2.20 |
| Android Gradle Plugin | 8.2.2 | 8.7.3 | ✅ Compatible | |
| Firebase Android | 32.7.0 | Latest | ✅ Compatible | |
| Firebase iOS (SPM) | Latest | Latest | ✅ Compatible with iOS 26 | Prefer SPM over CocoaPods |
| Xcode | 15.4 | 26.x | ✅ Required | macOS 26 (Tahoe) |
| macOS | Any | 26 (Tahoe)+ | ✅ Required | For CI/CD |

**Legend:**
- ✅ Compatible - No breaking changes expected
- ⚠️ Major version change - Review migration guide

**Key Requirements:**
- All Kotlin compiler plugins must be updated to 2.2.20 compatible versions
- Enable `dependencyVerificationMode = "strict"` in gradle.properties
- Disable Bitcode in iOS framework configuration (deprecated in iOS 26)
- Support both arm64 and x86_64 simulator architectures
- Prefer Swift Package Manager over CocoaPods for Firebase
- CI/CD must use macOS 26 (Tahoe) with Xcode 26

## Security Considerations

1. **Dependency vulnerabilities**: Check for security updates in new versions
2. **iOS privacy**: Ensure compliance with iOS 26 privacy requirements
3. **Firebase security rules**: Verify they still work correctly
4. **Data encryption**: Verify local encryption still works

## Performance Considerations

### Expected Improvements

1. **Kotlin 2.2.20 improvements:**
   - Faster compilation times
   - Better Kotlin/Native performance
   - Improved memory usage

2. **iOS 26 SDK improvements:**
   - Better SwiftUI performance
   - Improved system integration

### Potential Regressions

1. **First build after upgrade**: May be slower due to cache invalidation
2. **Ktor 3.0**: May have different performance characteristics
3. **Koin 4.0**: May have different initialization time

## Monitoring and Maintenance

### Post-Upgrade Monitoring

1. **Crash reporting**: Monitor Firebase Crashlytics for new crashes
2. **Performance metrics**: Track app performance in Firebase Performance
3. **User feedback**: Monitor for reports of issues
4. **Build times**: Track CI/CD build duration

### Ongoing Maintenance

1. Keep dependencies up to date
2. Monitor Kotlin release notes for future updates
3. Monitor iOS SDK changes
4. Update documentation as needed
