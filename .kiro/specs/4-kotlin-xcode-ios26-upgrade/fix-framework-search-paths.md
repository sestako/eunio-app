# Fix: "No such module 'Shared'" Error

## Problem
The iOS app can't find the Kotlin `shared.framework` during compilation, resulting in "No such module 'Shared'" errors.

## Root Cause
The Xcode project is missing the `FRAMEWORK_SEARCH_PATHS` build setting that tells the Swift compiler where to find the framework.

## Solution

### Option 1: Fix in Xcode (Recommended)

1. Open `iosApp/iosApp.xcodeproj` in Xcode
2. Select the **iosApp** project in the navigator (blue icon at the top)
3. Select the **iosApp** target (not the project)
4. Go to **Build Settings** tab
5. Search for "Framework Search Paths"
6. Double-click on **Framework Search Paths** to edit
7. Add the following paths (click the + button for each):
   ```
   $(BUILT_PRODUCTS_DIR)
   $(SRCROOT)/../shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)
   $(SRCROOT)/../shared/build/bin/iosSimulatorArm64/debugFramework
   ```
8. Make sure these are set for **both Debug and Release** configurations
9. Clean the build folder: **Product → Clean Build Folder** (Cmd+Shift+K)
10. Build again: **Product → Build** (Cmd+B)

### Option 2: Automated Fix (Using Script)

Run this command from the project root:

```bash
# This will add the framework search paths to your Xcode project
python3 << 'EOF'
import re
import sys

pbxproj_path = "iosApp/iosApp.xcodeproj/project.pbxproj"

with open(pbxproj_path, 'r') as f:
    content = f.read()

# Find the iosApp target Debug configuration
debug_pattern = r'(47BB6E3B2EA51C8A00D94C2C /\* Debug \*/ = \{[^}]+buildSettings = \{[^}]+?)(IPHONEOS_DEPLOYMENT_TARGET)'
release_pattern = r'(47BB6E3C2EA51C8A00D94C2C /\* Release \*/ = \{[^}]+buildSettings = \{[^}]+?)(IPHONEOS_DEPLOYMENT_TARGET)'

framework_search_paths = '''FRAMEWORK_SEARCH_PATHS = (
					"$(inherited)",
					"$(BUILT_PRODUCTS_DIR)",
					"$(SRCROOT)/../shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)",
					"$(SRCROOT)/../shared/build/bin/iosSimulatorArm64/debugFramework",
				);
				'''

# Add to Debug configuration
if 'FRAMEWORK_SEARCH_PATHS' not in content:
    content = re.sub(debug_pattern, r'\1' + framework_search_paths + r'\2', content)
    content = re.sub(release_pattern, r'\1' + framework_search_paths + r'\2', content)
    
    with open(pbxproj_path, 'w') as f:
        f.write(content)
    
    print("✅ Framework search paths added successfully!")
    print("Now run: ./gradlew :shared:linkDebugFrameworkIosSimulatorArm64")
    print("Then build in Xcode")
else:
    print("⚠️  Framework search paths already exist")
    sys.exit(1)
EOF
```

### Option 3: Quick Test

Before making changes, verify the framework builds correctly:

```bash
# Clean and rebuild the framework
./gradlew clean
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64

# Check if framework exists
ls -la shared/build/bin/iosSimulatorArm64/debugFramework/shared.framework

# Check if framework is valid
file shared/build/bin/iosSimulatorArm64/debugFramework/shared.framework/shared
```

## Verification

After applying the fix:

1. Clean build folder in Xcode (Cmd+Shift+K)
2. Build the project (Cmd+B)
3. You should see no "No such module 'Shared'" errors
4. The app should compile successfully

## Additional Notes

- The build script in Xcode already builds the Kotlin framework before compilation
- The script copies the framework to `$BUILT_PRODUCTS_DIR`
- But Swift needs `FRAMEWORK_SEARCH_PATHS` to find it during compilation
- This is a common issue after Kotlin/Xcode upgrades

## Related Task

This fix is part of **Task 15: Fix iOS compilation errors** in the upgrade spec.
