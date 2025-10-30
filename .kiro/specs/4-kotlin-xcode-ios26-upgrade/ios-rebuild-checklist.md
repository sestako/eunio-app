# iOS Project Rebuild Checklist

## Status: Ready to Begin

All prerequisites are met:
- ✅ Xcode 26.0.1 installed
- ✅ Kotlin 2.2.20 shared framework builds successfully
- ✅ EunioBridgeKit.xcframework built and ready
- ✅ All 139 Swift source files backed up
- ✅ Configuration files backed up

## Quick Reference

**Bundle ID**: `com.eunio.healthapp`  
**Deployment Target**: iOS 15.0  
**Swift Files**: 139  
**Backup Location**: `.kiro/specs/4-kotlin-xcode-ios26-upgrade/ios-rebuild-backup/`

## Phase 1: Backup Current Project ✅ COMPLETE

- [x] Corrupted project backed up
- [x] Configuration files backed up
- [x] Swift file inventory created
- [x] Shared framework built
- [x] EunioBridgeKit.xcframework built

## Phase 2: Remove Corrupted Project

- [ ] Move corrupted project:
  ```bash
  cd iosApp
  mv iosApp.xcodeproj iosApp.xcodeproj.corrupted
  cd ..
  ```

## Phase 3: Create New Xcode Project (10 min)

- [ ] Open Xcode 26
- [ ] File → New → Project
- [ ] Select: iOS → App
- [ ] Configure:
  - Product Name: `iosApp`
  - Team: (Select your team)
  - Organization Identifier: `com.eunio`
  - Bundle Identifier: `com.eunio.healthapp`
  - Interface: SwiftUI
  - Language: Swift
  - Include Tests: Yes (both Unit and UI)
- [ ] Save in `iosApp` folder (uncheck "Create Git repository")

## Phase 4: Configure Project Settings (15 min)

### General Tab
- [ ] Display Name: `Eunio`
- [ ] Bundle Identifier: `com.eunio.healthapp`
- [ ] Version: `1.0`
- [ ] Build: `1`
- [ ] Minimum Deployments: `iOS 15.0`

### Signing & Capabilities
- [ ] Enable automatic signing
- [ ] Select your team
- [ ] Add Capabilities:
  - [ ] HealthKit
  - [ ] Push Notifications
  - [ ] Background Modes (check: Background fetch, Background processing, Remote notifications)
  - [ ] Keychain Sharing
  - [ ] App Groups

### Configure Capabilities
- [ ] App Groups: Add `group.com.eunio.healthapp`
- [ ] Keychain Sharing: Add `$(AppIdentifierPrefix)com.eunio.healthapp`

### Build Settings
- [ ] Search "Bitcode" → Set to `No`
- [ ] Search "Build Libraries for Distribution" → Set to `No`

## Phase 5: Add Configuration Files (5 min)

- [ ] Replace Info.plist content (from backup)
- [ ] Add GoogleService-Info.plist:
  - Right-click iosApp folder → Add Files
  - Select `iosApp/iosApp/GoogleService-Info.plist`
  - ✅ Copy items, ✅ Add to targets: iosApp
- [ ] Add iosApp.entitlements:
  - File → New → Property List
  - Name: `iosApp.entitlements`
  - Replace content (from backup)
- [ ] Add PrivacyInfo.xcprivacy (if exists)

## Phase 6: Add Swift Source Files (20 min)

### Remove Default Files
- [ ] Delete `ContentView.swift` (Xcode's default)
- [ ] Delete `iosAppApp.swift` (Xcode's default)

### Add Main App File
- [ ] Add `iOSApp.swift` to project

### Create Folder Structure (Groups)
- [ ] Create group: Core
- [ ] Create group: Services
- [ ] Create group: ViewModels
- [ ] Create group: Views
- [ ] Create group: Models
- [ ] Create group: Extensions
- [ ] Create group: Navigation
- [ ] Create group: Utils
- [ ] Create group: UI

### Add Files to Groups
- [ ] Core: Add all files from `iosApp/iosApp/Core/` (with subfolders)
- [ ] Services: Add all files from `iosApp/iosApp/Services/`
- [ ] ViewModels: Add all files from `iosApp/iosApp/ViewModels/`
- [ ] Views: Add all files from `iosApp/iosApp/Views/` (with subfolders)
- [ ] Models: Add all files from `iosApp/iosApp/Models/`
- [ ] Extensions: Add all files from `iosApp/iosApp/Extensions/`
- [ ] Navigation: Add all files from `iosApp/iosApp/Navigation/`
- [ ] Utils: Add all files from `iosApp/iosApp/Utils/`
- [ ] UI: Add all files from `iosApp/iosApp/UI/` (with subfolders)

### Add Root-Level Files
- [ ] Add `ContentView.swift`
- [ ] Add `AuthenticationViews.swift`
- [ ] Add `ProfileTestView.swift`

**Tip**: When adding files, always:
- ✅ Create groups (not folder references)
- ✅ Add to targets: iosApp

## Phase 7: Add Firebase via SPM (10 min)

- [ ] File → Add Package Dependencies
- [ ] URL: `https://github.com/firebase/firebase-ios-sdk`
- [ ] Dependency Rule: Up to Next Major Version → `11.0.0`
- [ ] Select products:
  - [ ] FirebaseAuth
  - [ ] FirebaseCore
  - [ ] FirebaseFirestore
  - [ ] FirebaseCrashlytics
  - [ ] FirebaseAnalytics
  - [ ] FirebasePerformance
- [ ] Wait for package resolution (2-3 min)

## Phase 8: Add EunioBridgeKit XCFramework (10 min)

- [ ] Select iosApp project → iosApp target → General tab
- [ ] Scroll to "Frameworks, Libraries, and Embedded Content"
- [ ] Click + → Add Other → Add Files
- [ ] Navigate to: `shared/src/iosMain/c_interop/libs/EunioBridgeKit.xcframework`
- [ ] Set Embed: **Embed & Sign**

## Phase 9: Add Kotlin Shared Framework (15 min)

### Build Framework
- [ ] Run: `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64`
- [ ] Run: `./gradlew :shared:linkReleaseFrameworkIosSimulatorArm64`

### Add to Xcode
- [ ] General tab → Frameworks, Libraries, and Embedded Content
- [ ] Click + → Add Other → Add Files
- [ ] Navigate to: `shared/build/bin/iosSimulatorArm64/debugFramework/shared.framework`
- [ ] Set Embed: **Embed & Sign**

### Configure Search Paths
- [ ] Build Settings → Search "Framework Search Paths"
- [ ] Add: `$(PROJECT_DIR)/../shared/build/bin/iosSimulatorArm64/debugFramework`
- [ ] Add: `$(PROJECT_DIR)/../shared/build/bin/iosArm64/debugFramework`

### Add Build Phase
- [ ] Build Phases → + → New Run Script Phase
- [ ] Name: "Build Kotlin Framework"
- [ ] Script:
  ```bash
  cd "$SRCROOT/.."
  ./gradlew :shared:embedAndSignAppleFrameworkForXcode
  ```
- [ ] Move this phase **before** "Compile Sources"

## Phase 10: Configure Build Settings (10 min)

### Other Linker Flags
- [ ] Build Settings → Search "Other Linker Flags"
- [ ] Add: `-ObjC`
- [ ] Add: `-l"c++"`
- [ ] Add: `-framework "shared"`
- [ ] Add: `-framework "EunioBridgeKit"`

### Header Search Paths
- [ ] Build Settings → Search "Header Search Paths"
- [ ] Add: `$(PROJECT_DIR)/../shared/build/bin/iosSimulatorArm64/debugFramework/shared.framework/Headers`

### Other Settings
- [ ] Build Settings → "Enable Modules" → Set to `Yes`
- [ ] Build Settings → "Swift Optimization Level":
  - Debug: No Optimization
  - Release: Optimize for Speed

## Phase 11: Build and Test (15 min)

- [ ] Product → Clean Build Folder (Shift+Cmd+K)
- [ ] Product → Build (Cmd+B)
- [ ] Fix any compilation errors
- [ ] Select iPhone 15 simulator (iOS 26)
- [ ] Product → Run (Cmd+R)
- [ ] Verify: App launches
- [ ] Verify: Console shows "Firebase configured successfully"
- [ ] Verify: Can navigate between screens

## Phase 12: Add Test Targets (10 min)

### Unit Tests
- [ ] Right-click iosAppTests folder
- [ ] Add Files to "iosAppTests"
- [ ] Select all files from `iosApp/iosAppTests/`
- [ ] ✅ Add to targets: iosAppTests

### UI Tests
- [ ] Right-click iosAppUITests folder
- [ ] Add Files to "iosAppUITests"
- [ ] Select all files from `iosApp/iosAppUITests/`
- [ ] ✅ Add to targets: iosAppUITests

### Run Tests
- [ ] Product → Test (Cmd+U)
- [ ] Verify tests run

## Phase 13: Final Verification

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

## Phase 14: Commit

- [ ] Verify .pbxproj is clean (no placeholder IDs)
- [ ] Commit:
  ```bash
  git add iosApp/iosApp.xcodeproj
  git add iosApp/iosApp/
  git commit -m "Rebuild iOS project for Xcode 26 (clean .pbxproj)"
  ```

## Troubleshooting

### "No such module 'shared'"
→ Run: `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64`

### "No such module 'EunioBridgeKit'"
→ Verify XCFramework is in Frameworks list with "Embed & Sign"

### "Firebase not configured"
→ Check GoogleService-Info.plist is in Copy Bundle Resources

### "Undefined symbols"
→ Check Other Linker Flags include `-ObjC` and `-l"c++"`

### Crash on Launch
→ Verify FirebaseApp.configure() is called in app initialization

## Estimated Time

- **Total**: 2-3 hours
- Can be done in phases with breaks
- Most time-consuming: Adding Swift files (Phase 6)

## Notes

- Use **only Xcode UI** - no manual .pbxproj editing
- All settings configured through Xcode's interface
- New .pbxproj will be clean and maintainable
- Bitcode disabled (deprecated in iOS 26)
- Uses Swift Package Manager for Firebase

## Help

If you get stuck, refer to:
- Full guide: `.kiro/specs/4-kotlin-xcode-ios26-upgrade/ios-project-rebuild-guide.md`
- Backup config: `.kiro/specs/4-kotlin-xcode-ios26-upgrade/ios-rebuild-backup/project-config.txt`
- Swift file list: `.kiro/specs/4-kotlin-xcode-ios26-upgrade/ios-rebuild-backup/swift-files-inventory.txt`
