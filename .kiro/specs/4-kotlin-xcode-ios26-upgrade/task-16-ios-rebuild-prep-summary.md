# Task 16: iOS Project Rebuild Preparation - Complete

## Summary

The iOS Xcode project (.pbxproj file) is corrupted with placeholder IDs. All prerequisites for a clean rebuild have been prepared and verified.

## What Was Done

### 1. Verified Prerequisites ✅
- Xcode 26.0.1 installed and configured
- Kotlin 2.2.20 shared framework builds successfully
- All 139 Swift source files are intact

### 2. Built EunioBridgeKit XCFramework ✅
- Used direct build script (Xcode project was also corrupted)
- Successfully built for:
  - iOS device (arm64)
  - iOS Simulator (arm64 + x86_64)
- XCFramework location: `shared/src/iosMain/c_interop/libs/EunioBridgeKit.xcframework`

### 3. Created Comprehensive Backup ✅
- Backed up corrupted .xcodeproj
- Backed up all configuration files:
  - Info.plist
  - GoogleService-Info.plist
  - iosApp.entitlements
  - PrivacyInfo.xcprivacy
- Created Swift file inventory (139 files)
- Extracted project configuration

### 4. Created Rebuild Documentation ✅

**Three documents created:**

1. **ios-project-rebuild-guide.md** (Detailed guide)
   - Complete step-by-step instructions
   - All configuration details
   - Troubleshooting section
   - Estimated 2-3 hours

2. **ios-rebuild-checklist.md** (Interactive checklist)
   - Phase-by-phase checklist
   - Quick reference information
   - Checkbox format for tracking progress
   - Troubleshooting quick tips

3. **prepare-ios-rebuild.sh** (Automation script)
   - Verifies prerequisites
   - Creates backups
   - Builds dependencies
   - Extracts configuration

## Project Configuration Preserved

```
Bundle Identifier: com.eunio.healthapp
Reversed Client ID: com.googleusercontent.apps.458394672643-jhfu0g7fm77ok3epbhc5e1e48loqivoo
Deployment Target: iOS 15.0
Swift Files: 139

Capabilities:
- HealthKit
- HealthKit Background Delivery
- Keychain Sharing ($(AppIdentifierPrefix)com.eunio.healthapp)
- App Groups (group.com.eunio.healthapp)
- Push Notifications
- Background Modes (fetch, processing, remote-notification)

Firebase Products (SPM):
- FirebaseAuth
- FirebaseCore
- FirebaseFirestore
- FirebaseCrashlytics
- FirebaseAnalytics
- FirebasePerformance

Frameworks:
- shared.framework (Kotlin)
- EunioBridgeKit.xcframework
```

## Backup Location

All backups saved to:
```
.kiro/specs/4-kotlin-xcode-ios26-upgrade/ios-rebuild-backup/
```

Contents:
- `iosApp.xcodeproj.corrupted/` - Corrupted project backup
- `Info.plist.backup` - App configuration
- `GoogleService-Info.plist.backup` - Firebase configuration
- `iosApp.entitlements.backup` - Capabilities configuration
- `PrivacyInfo.xcprivacy.backup` - Privacy manifest
- `swift-files-inventory.txt` - List of all 139 Swift files
- `project-config.txt` - Complete configuration reference

## Next Steps

### Option A: Manual Rebuild (Recommended for Learning)
Follow the interactive checklist:
```bash
open .kiro/specs/4-kotlin-xcode-ios26-upgrade/ios-rebuild-checklist.md
```

### Option B: Detailed Guide
Read the comprehensive guide:
```bash
open .kiro/specs/4-kotlin-xcode-ios26-upgrade/ios-project-rebuild-guide.md
```

### Step 1: Remove Corrupted Project
```bash
cd iosApp
mv iosApp.xcodeproj iosApp.xcodeproj.corrupted
cd ..
```

### Step 2: Open Xcode 26
- Launch Xcode
- File → New → Project
- Follow checklist or guide

## Estimated Time

- **Total**: 2-3 hours
- **Phase 3-5** (Project creation & config): 30 minutes
- **Phase 6** (Adding Swift files): 20 minutes
- **Phase 7-9** (Dependencies): 35 minutes
- **Phase 10-11** (Build settings & testing): 25 minutes
- **Phase 12-14** (Tests & commit): 20 minutes

## Key Points

1. **Use Xcode UI Only**
   - No manual .pbxproj editing
   - All configuration through Xcode interface
   - Results in clean, maintainable project file

2. **Bitcode Disabled**
   - Deprecated in iOS 26
   - Must be set to "No" in build settings

3. **Swift Package Manager**
   - Using SPM for Firebase (not CocoaPods)
   - Cleaner dependency management
   - Better Xcode 26 integration

4. **Framework Embedding**
   - Both shared.framework and EunioBridgeKit.xcframework
   - Must be set to "Embed & Sign"
   - Proper search paths configured

5. **Build Phase Order**
   - "Build Kotlin Framework" must run **before** "Compile Sources"
   - Ensures framework is available during Swift compilation

## Verification Commands

Check prerequisites:
```bash
./scripts/prepare-ios-rebuild.sh
```

Verify shared framework:
```bash
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

Check EunioBridgeKit:
```bash
ls -la shared/src/iosMain/c_interop/libs/EunioBridgeKit.xcframework
```

Count Swift files:
```bash
find iosApp/iosApp -name "*.swift" -type f | wc -l
# Should show: 139
```

## Troubleshooting

### If Build Fails After Rebuild

1. **Clean Build Folder**
   ```
   Product → Clean Build Folder (Shift+Cmd+K)
   ```

2. **Verify Framework Paths**
   - Check Framework Search Paths in Build Settings
   - Ensure frameworks are in "Embed & Sign" mode

3. **Check Linker Flags**
   - Must include: `-ObjC -l"c++"`
   - Must include framework references

4. **Rebuild Kotlin Framework**
   ```bash
   ./gradlew clean
   ./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
   ```

### If Firebase Doesn't Initialize

1. **Check GoogleService-Info.plist**
   - Must be in Copy Bundle Resources
   - Verify in Build Phases

2. **Check URL Types**
   - Should include REVERSED_CLIENT_ID
   - Check in Info tab of target settings

3. **Verify Firebase Packages**
   - All 6 Firebase products added via SPM
   - Check in Package Dependencies

## Success Criteria

- [ ] Project builds without errors in Xcode 26
- [ ] App launches on iOS 26 simulator
- [ ] Firebase initializes (check console logs)
- [ ] Can navigate between screens
- [ ] Shared Kotlin module accessible
- [ ] EunioBridgeKit bridge functional
- [ ] All 139 Swift files included
- [ ] Tests run successfully
- [ ] .pbxproj file is clean (no placeholder IDs)

## Files Created

1. `.kiro/specs/4-kotlin-xcode-ios26-upgrade/ios-project-rebuild-guide.md`
2. `.kiro/specs/4-kotlin-xcode-ios26-upgrade/ios-rebuild-checklist.md`
3. `.kiro/specs/4-kotlin-xcode-ios26-upgrade/ios-rebuild-backup/` (directory)
4. `scripts/prepare-ios-rebuild.sh`

## Task Status

**Task 16 Preparation**: ✅ Complete

**Next**: Follow the rebuild checklist to create the new iOS project

## Notes

- This is a **manual process** requiring Xcode UI interaction
- Cannot be fully automated due to Xcode's project creation workflow
- The checklist provides step-by-step guidance
- Estimated 2-3 hours for complete rebuild
- Can be done in phases with breaks

## References

- Xcode 26 Release Notes
- Firebase iOS SDK 11.x Documentation
- Kotlin Multiplatform iOS Integration Guide
- Swift Package Manager Best Practices

---

**Prepared**: October 19, 2025  
**Xcode Version**: 26.0.1  
**Kotlin Version**: 2.2.20  
**iOS Deployment Target**: 15.0  
**iOS SDK**: 26.0
