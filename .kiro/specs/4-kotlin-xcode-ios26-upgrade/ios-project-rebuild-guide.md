# iOS Project Rebuild Guide - Option 2

## Context
The iosApp.xcodeproj/project.pbxproj file is corrupted (contains placeholder IDs). This guide provides step-by-step instructions to rebuild the iOS project from scratch using Xcode 26 UI only.

## Prerequisites
- ✅ Xcode 26 installed and selected
- ✅ Kotlin 2.2.20 shared framework builds successfully
- ✅ EunioBridgeKit.xcframework is built and ready
- ✅ All Swift source files are intact in iosApp/iosApp/

## Project Configuration to Preserve

### Bundle Identifier
```
com.eunio.healthapp
```

### Deployment Target
```
iOS 15.0
```

### Capabilities Required
- HealthKit
- HealthKit Background Delivery
- Keychain Sharing
- App Groups: `group.com.eunio.healthapp`
- Push Notifications
- Background Modes:
  - Background fetch
  - Background processing
  - HealthKit
  - Remote notifications
  - Processing

### Firebase Configuration
- GoogleService-Info.plist (preserved)
- REVERSED_CLIENT_ID: `com.googleusercontent.apps.458394672643-jhfu0g7fm77ok3epbhc5e1e48loqivoo`

## Step-by-Step Rebuild Process

### Phase 1: Backup and Cleanup (5 minutes)

1. **Backup current project**
   ```bash
   cd iosApp
   mv iosApp.xcodeproj iosApp.xcodeproj.corrupted.backup
   ```

2. **Verify Swift files are intact**
   ```bash
   find iosApp -name "*.swift" -type f | wc -l
   # Should show: 139 files
   ```

### Phase 2: Create New Xcode Project (10 minutes)

1. **Open Xcode 26**
   - Launch Xcode from Applications

2. **Create New Project**
   - File → New → Project
   - Choose: **iOS** → **App**
   - Click **Next**

3. **Configure Project**
   - Product Name: `iosApp`
   - Team: Select your Apple Developer Team
   - Organization Identifier: `com.eunio`
   - Bundle Identifier: `com.eunio.healthapp` (should auto-fill)
   - Interface: **SwiftUI**
   - Language: **Swift**
   - Storage: **None** (we'll use our own)
   - Include Tests: **Yes** (check both Unit and UI Tests)
   - Click **Next**

4. **Save Location**
   - Navigate to your project root
   - Select the `iosApp` folder
   - **IMPORTANT**: Uncheck "Create Git repository"
   - Click **Create**
   - If prompted about replacing, confirm

### Phase 3: Configure Project Settings (15 minutes)

1. **Select iosApp target** in project navigator

2. **General Tab**
   - Display Name: `Eunio`
   - Bundle Identifier: `com.eunio.healthapp` (verify)
   - Version: `1.0`
   - Build: `1`
   - Minimum Deployments: `iOS 15.0`
   - Supported Destinations: iPhone, iPad
   - Status Bar Style: Default

3. **Signing & Capabilities Tab**
   - **Automatically manage signing**: ✅ Enabled
   - Team: Select your team
   
   - **Add Capabilities** (click + button):
     - HealthKit
     - Push Notifications
     - Background Modes
     - Keychain Sharing
     - App Groups

4. **Configure Background Modes**
   - ✅ Background fetch
   - ✅ Background processing
   - ✅ Remote notifications

5. **Configure App Groups**
   - Add: `group.com.eunio.healthapp`

6. **Configure Keychain Sharing**
   - Add: `$(AppIdentifierPrefix)com.eunio.healthapp`

7. **Build Settings Tab**
   - Search for "Bitcode"
   - Set **Enable Bitcode**: `No`
   - Search for "Build Libraries for Distribution"
   - Set to: `No` (for app target)

### Phase 4: Add Configuration Files (5 minutes)

1. **Add Info.plist entries**
   - Select `Info.plist` in project navigator
   - Right-click → Open As → Source Code
   - Replace with the preserved Info.plist content (see backup)

2. **Add GoogleService-Info.plist**
   - Right-click iosApp folder in Xcode
   - Add Files to "iosApp"...
   - Select `iosApp/iosApp/GoogleService-Info.plist`
   - ✅ Copy items if needed
   - ✅ Add to targets: iosApp
   - Click Add

3. **Add iosApp.entitlements**
   - File → New → File
   - Choose: Property List
   - Name: `iosApp.entitlements`
   - Save in: iosApp/iosApp/
   - Add to target: iosApp
   - Replace content with preserved entitlements (see backup)

4. **Add PrivacyInfo.xcprivacy** (if exists)
   - Right-click iosApp folder
   - Add Files to "iosApp"...
   - Select `iosApp/iosApp/PrivacyInfo.xcprivacy`
   - Add to target: iosApp

### Phase 5: Add Swift Source Files (20 minutes)

**IMPORTANT**: Add files through Xcode UI only, maintaining folder structure.

1. **Remove default files**
   - Delete `ContentView.swift` (Xcode created)
   - Delete `iosAppApp.swift` (Xcode created)
   - Keep: Assets.xcassets, Preview Content

2. **Add main app file**
   - Right-click iosApp folder
   - Add Files to "iosApp"...
   - Select `iosApp/iosApp/iOSApp.swift`
   - ✅ Add to targets: iosApp
   - Click Add

3. **Add folder structure** (create groups in Xcode):
   - Right-click iosApp → New Group → "Core"
   - Right-click iosApp → New Group → "Services"
   - Right-click iosApp → New Group → "ViewModels"
   - Right-click iosApp → New Group → "Views"
   - Right-click iosApp → New Group → "Models"
   - Right-click iosApp → New Group → "Extensions"
   - Right-click iosApp → New Group → "Navigation"
   - Right-click iosApp → New Group → "Utils"
   - Right-click iosApp → New Group → "UI"

4. **Add files to each group**:

   **Core/** (add all files from iosApp/iosApp/Core/):
   - Right-click Core group → Add Files to "iosApp"...
   - Navigate to `iosApp/iosApp/Core/`
   - Select all Swift files
   - ✅ Create groups (not folder references)
   - ✅ Add to targets: iosApp
   - Click Add

   **Services/** (add all files from iosApp/iosApp/Services/):
   - Right-click Services group → Add Files to "iosApp"...
   - Navigate to `iosApp/iosApp/Services/`
   - Select all Swift files
   - ✅ Add to targets: iosApp
   - Click Add

   **ViewModels/** (add all files from iosApp/iosApp/ViewModels/):
   - Right-click ViewModels group → Add Files to "iosApp"...
   - Navigate to `iosApp/iosApp/ViewModels/`
   - Select all Swift files
   - ✅ Add to targets: iosApp
   - Click Add

   **Views/** (add all files from iosApp/iosApp/Views/):
   - Right-click Views group → Add Files to "iosApp"...
   - Navigate to `iosApp/iosApp/Views/`
   - Select all Swift files and subfolders
   - ✅ Create groups
   - ✅ Add to targets: iosApp
   - Click Add

   **Models/** (add all files from iosApp/iosApp/Models/):
   - Right-click Models group → Add Files to "iosApp"...
   - Navigate to `iosApp/iosApp/Models/`
   - Select all Swift files
   - ✅ Add to targets: iosApp
   - Click Add

   **Extensions/** (add all files from iosApp/iosApp/Extensions/):
   - Right-click Extensions group → Add Files to "iosApp"...
   - Navigate to `iosApp/iosApp/Extensions/`
   - Select all Swift files
   - ✅ Add to targets: iosApp
   - Click Add

   **Navigation/** (add all files from iosApp/iosApp/Navigation/):
   - Right-click Navigation group → Add Files to "iosApp"...
   - Navigate to `iosApp/iosApp/Navigation/`
   - Select all Swift files
   - ✅ Add to targets: iosApp
   - Click Add

   **Utils/** (add all files from iosApp/iosApp/Utils/):
   - Right-click Utils group → Add Files to "iosApp"...
   - Navigate to `iosApp/iosApp/Utils/`
   - Select all Swift files
   - ✅ Add to targets: iosApp
   - Click Add

   **UI/** (add all files from iosApp/iosApp/UI/):
   - Right-click UI group → Add Files to "iosApp"...
   - Navigate to `iosApp/iosApp/UI/`
   - Select all Swift files and subfolders
   - ✅ Create groups
   - ✅ Add to targets: iosApp
   - Click Add

5. **Add remaining root-level Swift files**:
   - `ContentView.swift`
   - `AuthenticationViews.swift`
   - `ProfileTestView.swift`
   - Any other .swift files in iosApp/iosApp/ root

### Phase 6: Add Firebase via Swift Package Manager (10 minutes)

1. **Add Firebase Package**
   - File → Add Package Dependencies...
   - Enter URL: `https://github.com/firebase/firebase-ios-sdk`
   - Dependency Rule: **Up to Next Major Version** → `11.0.0`
   - Click **Add Package**

2. **Select Firebase Products** (check these):
   - ✅ FirebaseAuth
   - ✅ FirebaseCore
   - ✅ FirebaseFirestore
   - ✅ FirebaseCrashlytics
   - ✅ FirebaseAnalytics
   - ✅ FirebasePerformance
   - Click **Add Package**

3. **Wait for package resolution** (may take 2-3 minutes)

### Phase 7: Add EunioBridgeKit XCFramework (10 minutes)

1. **Verify XCFramework exists**
   ```bash
   ls -la EunioBridgeKit/build/EunioBridgeKit.xcframework
   ```

2. **Add to Xcode**
   - Select iosApp project in navigator
   - Select iosApp target
   - Go to **General** tab
   - Scroll to **Frameworks, Libraries, and Embedded Content**
   - Click **+** button
   - Click **Add Other...** → **Add Files...**
   - Navigate to `EunioBridgeKit/build/EunioBridgeKit.xcframework`
   - Click **Open**
   - Set **Embed**: **Embed & Sign**

### Phase 8: Add Kotlin Shared Framework (15 minutes)

1. **Build shared framework first**
   ```bash
   cd /path/to/project
   ./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
   ./gradlew :shared:linkReleaseFrameworkIosSimulatorArm64
   ```

2. **Add framework to Xcode**
   - Select iosApp project
   - Select iosApp target
   - **General** tab → **Frameworks, Libraries, and Embedded Content**
   - Click **+**
   - Click **Add Other...** → **Add Files...**
   - Navigate to `shared/build/bin/iosSimulatorArm64/debugFramework/shared.framework`
   - Click **Open**
   - Set **Embed**: **Embed & Sign**

3. **Configure Framework Search Paths**
   - Select iosApp target
   - **Build Settings** tab
   - Search for "Framework Search Paths"
   - Add: `$(PROJECT_DIR)/../shared/build/bin/iosSimulatorArm64/debugFramework`
   - Add: `$(PROJECT_DIR)/../shared/build/bin/iosArm64/debugFramework`

4. **Add Build Phase for Framework**
   - Select iosApp target
   - **Build Phases** tab
   - Click **+** → **New Run Script Phase**
   - Name it: "Build Kotlin Framework"
   - Add script:
     ```bash
     cd "$SRCROOT/.."
     ./gradlew :shared:embedAndSignAppleFrameworkForXcode
     ```
   - Move this phase **before** "Compile Sources"

### Phase 9: Configure Build Settings (10 minutes)

1. **Other Linker Flags**
   - Build Settings → Search "Other Linker Flags"
   - Add: `-ObjC`
   - Add: `-l"c++"`
   - Add: `-framework "shared"`
   - Add: `-framework "EunioBridgeKit"`

2. **Header Search Paths**
   - Build Settings → Search "Header Search Paths"
   - Add: `$(PROJECT_DIR)/../shared/build/bin/iosSimulatorArm64/debugFramework/shared.framework/Headers`

3. **Enable Modules**
   - Build Settings → Search "Enable Modules"
   - Set to: **Yes**

4. **Swift Optimization Level**
   - Debug: **No Optimization**
   - Release: **Optimize for Speed**

### Phase 10: Build and Test (15 minutes)

1. **Clean Build Folder**
   - Product → Clean Build Folder (Shift+Cmd+K)

2. **Build Project**
   - Product → Build (Cmd+B)
   - Fix any compilation errors

3. **Run on Simulator**
   - Select iPhone 15 simulator (iOS 26)
   - Product → Run (Cmd+R)

4. **Verify Firebase Initialization**
   - Check console for: "Firebase configured successfully"

5. **Test Basic Functionality**
   - App launches
   - Firebase Auth works
   - Can navigate between screens

### Phase 11: Add Test Targets (10 minutes)

1. **Add Unit Test Files**
   - Right-click iosAppTests folder
   - Add Files to "iosAppTests"...
   - Select all files from `iosApp/iosAppTests/`
   - ✅ Add to targets: iosAppTests

2. **Add UI Test Files**
   - Right-click iosAppUITests folder
   - Add Files to "iosAppUITests"...
   - Select all files from `iosApp/iosAppUITests/`
   - ✅ Add to targets: iosAppUITests

3. **Run Tests**
   - Product → Test (Cmd+U)

## Troubleshooting

### Build Errors

**Error: "No such module 'shared'"**
- Solution: Ensure shared framework is built and linked
- Run: `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64`

**Error: "No such module 'EunioBridgeKit'"**
- Solution: Verify XCFramework is added and embedded
- Check: General → Frameworks → EunioBridgeKit.xcframework

**Error: "Firebase not configured"**
- Solution: Verify GoogleService-Info.plist is in Copy Bundle Resources
- Check: Build Phases → Copy Bundle Resources

**Error: "Undefined symbols"**
- Solution: Check Other Linker Flags
- Ensure `-ObjC` and `-l"c++"` are present

### Runtime Errors

**Crash on Launch**
- Check: FirebaseApp.configure() is called in app initialization
- Check: All required capabilities are enabled

**Firebase Auth Not Working**
- Verify: URL Types includes REVERSED_CLIENT_ID
- Check: GoogleService-Info.plist is correct

## Verification Checklist

- [ ] Project builds without errors
- [ ] App launches on simulator
- [ ] Firebase initializes successfully
- [ ] Can sign in with Firebase Auth
- [ ] Can save/load data from Firestore
- [ ] Shared Kotlin module is accessible
- [ ] EunioBridgeKit bridge works
- [ ] All 139 Swift files are included
- [ ] All capabilities are configured
- [ ] Tests run successfully

## Post-Rebuild

1. **Commit new project**
   ```bash
   git add iosApp/iosApp.xcodeproj
   git commit -m "Rebuild iOS project for Xcode 26 (clean .pbxproj)"
   ```

2. **Update task status**
   - Mark task 16 as complete
   - Proceed to task 17

## Estimated Time
- Total: 2-3 hours (depending on familiarity with Xcode)
- Can be done in phases with breaks

## Notes
- This rebuild uses **only Xcode UI** - no manual .pbxproj editing
- All settings are configured through Xcode's interface
- The new .pbxproj will be clean and maintainable
- Bitcode is disabled (deprecated in iOS 26)
- Uses Swift Package Manager for Firebase (preferred over CocoaPods)
