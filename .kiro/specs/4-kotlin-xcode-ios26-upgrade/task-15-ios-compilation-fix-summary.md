# Task 15: Fix iOS Compilation Errors - Summary

## Problem Resolved
Fixed the "No such module 'Shared'" error that was preventing iOS app compilation.

## Root Causes Identified

### 1. Missing Framework Search Paths
The Xcode project was missing `FRAMEWORK_SEARCH_PATHS` build setting, so the Swift compiler couldn't find the Kotlin framework during compilation.

### 2. Framework Name Mismatch
- The Kotlin framework was named "shared" (lowercase)
- Swift code was importing both "Shared" (capital S) and "shared" (lowercase)
- Module names are case-sensitive in Swift

## Changes Made

### 1. Added Framework Search Paths to Xcode Project
Updated `iosApp/iosApp.xcodeproj/project.pbxproj` to add:
```
FRAMEWORK_SEARCH_PATHS = (
    "$(inherited)",
    "$(BUILT_PRODUCTS_DIR)",
    "$(SRCROOT)/../shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)",
    "$(SRCROOT)/../shared/build/bin/iosSimulatorArm64/debugFramework",
    "$(SRCROOT)/../shared/build/bin/iosArm64/debugFramework",
    "$(SRCROOT)/../shared/build/bin/iosX64/debugFramework",
);
```

### 2. Renamed Framework to "Shared" (Capital S)
Updated `shared/build.gradle.kts`:
```kotlin
iosTarget.binaries.framework {
    baseName = "Shared"  // Changed from "shared"
    isStatic = true
    // ...
}
```

### 3. Updated Xcode Build Script
Modified the build script in `project.pbxproj` to use "Shared.framework" instead of "shared.framework".

### 4. Standardized Swift Imports
Changed all Swift files to use `import Shared` (capital S):
- Found 64 files with imports
- Changed all lowercase "import shared" to "import Shared"
- Used: `find iosApp/iosApp -name "*.swift" -type f -exec sed -i '' 's/import shared$/import Shared/g' {} \;`

## Verification

### Framework Built Successfully
```bash
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
# BUILD SUCCESSFUL

ls -la shared/build/bin/iosSimulatorArm64/debugFramework/
# Shared.framework exists

cat shared/build/bin/iosSimulatorArm64/debugFramework/Shared.framework/Modules/module.modulemap
# framework module "Shared" { ... }
```

### Xcode Build Progress
The "No such module 'Shared'" error is now **RESOLVED**. The build progresses much further and can now find the Shared module.

## Remaining Issues

The build now fails with different errors (not related to finding the Shared module):

1. **Duplicate Declaration**: `CustomTextFieldStyle` is declared in multiple places
2. **Ambiguous Type**: `MainTab` type is ambiguous
3. **API Changes**: Some SwiftUI API usage issues

These are code-level issues that need to be addressed separately. They are NOT related to the Kotlin/Xcode upgrade framework linking issue.

## Next Steps

1. Fix the duplicate `CustomTextFieldStyle` declaration
2. Resolve the `MainTab` ambiguity
3. Fix SwiftUI API usage issues
4. Continue with remaining compilation errors

## Files Modified

1. `iosApp/iosApp.xcodeproj/project.pbxproj` - Added FRAMEWORK_SEARCH_PATHS, updated build script
2. `shared/build.gradle.kts` - Changed baseName from "shared" to "Shared"
3. 64 Swift files - Changed `import shared` to `import Shared`

## Status

✅ **RESOLVED**: "No such module 'Shared'" error
✅ Framework builds successfully with correct name
✅ Xcode can find and import the Shared module
⚠️ **IN PROGRESS**: Fixing remaining Swift compilation errors (unrelated to framework linking)

## Documentation Created

- `.kiro/specs/4-kotlin-xcode-ios26-upgrade/fix-framework-search-paths.md` - Detailed fix guide
- `.kiro/specs/4-kotlin-xcode-ios26-upgrade/task-15-ios-compilation-fix-summary.md` - This summary
