# iOS Project Rebuild - Complete Guide

## Problem

The `iosApp.xcodeproj/project.pbxproj` file is corrupted with placeholder IDs (all A's and B's), making the project unbuildable. The EunioBridgeKit Xcode project is also corrupted (missing project.pbxproj entirely).

## Solution

Complete rebuild of the iOS project using Xcode 26 UI only, with all prerequisites prepared and ready.

## Status: ✅ Ready to Begin

All prerequisites verified:
- Xcode 26.0.1 installed
- Kotlin 2.2.20 shared framework builds
- EunioBridgeKit.xcframework built
- All 139 Swift files backed up
- Configuration files preserved

## Quick Start

### Option 1: Automated Start (Recommended)
```bash
./scripts/start-ios-rebuild.sh
```

This script will:
- Verify all prerequisites
- Show project configuration
- Optionally move corrupted project
- Open checklist and Xcode for you

### Option 2: Manual Start
```bash
# 1. Move corrupted project
cd iosApp
mv iosApp.xcodeproj iosApp.xcodeproj.corrupted
cd ..

# 2. Open checklist
open .kiro/specs/4-kotlin-xcode-ios26-upgrade/ios-rebuild-checklist.md

# 3. Open Xcode
open -a Xcode

# 4. Follow checklist: File → New → Project
```

## Documentation

### 📋 Interactive Checklist (Start Here)
**File**: `ios-rebuild-checklist.md`

Phase-by-phase checklist with checkboxes. Track your progress as you rebuild.

**Use this for**: Step-by-step execution

### 📖 Detailed Guide (Reference)
**File**: `ios-project-rebuild-guide.md`

Comprehensive guide with detailed explanations, troubleshooting, and examples.

**Use this for**: Understanding details, troubleshooting

### 📦 Backup Files
**Directory**: `ios-rebuild-backup/`

Contains:
- Corrupted project backup
- All configuration files
- Swift file inventory
- Project configuration reference

### 🔧 Preparation Script
**File**: `../../scripts/prepare-ios-rebuild.sh`

Already run. Created all backups and built dependencies.

### 🚀 Start Script
**File**: `../../scripts/start-ios-rebuild.sh`

Quick start script to begin the rebuild process.

## Project Configuration

```yaml
Bundle Identifier: com.eunio.healthapp
Organization: com.eunio
Deployment Target: iOS 15.0
Build SDK: iOS 26.0
Interface: SwiftUI
Language: Swift
Swift Files: 139

Capabilities:
  - HealthKit
  - HealthKit Background Delivery
  - Keychain Sharing
  - App Groups (group.com.eunio.healthapp)
  - Push Notifications
  - Background Modes

Firebase (SPM):
  - FirebaseAuth
  - FirebaseCore
  - FirebaseFirestore
  - FirebaseCrashlytics
  - FirebaseAnalytics
  - FirebasePerformance

Frameworks:
  - shared.framework (Kotlin Multiplatform)
  - EunioBridgeKit.xcframework (Firebase Bridge)
```

## Rebuild Process Overview

### Phase 1: Preparation ✅ COMPLETE
- Backups created
- Dependencies built
- Configuration extracted

### Phase 2: Project Creation (10 min)
- Create new iOS App project in Xcode 26
- Configure bundle ID and settings

### Phase 3: Configuration (15 min)
- Set deployment target
- Enable capabilities
- Configure signing

### Phase 4: Add Files (20 min)
- Add configuration files
- Add all 139 Swift source files
- Maintain folder structure

### Phase 5: Dependencies (35 min)
- Add Firebase via SPM
- Add EunioBridgeKit.xcframework
- Add Kotlin shared.framework
- Configure build phases

### Phase 6: Build Settings (10 min)
- Configure linker flags
- Set search paths
- Enable modules

### Phase 7: Build & Test (15 min)
- Build project
- Run on simulator
- Verify functionality

### Phase 8: Tests (10 min)
- Add test files
- Run test suite

### Phase 9: Commit (5 min)
- Verify clean .pbxproj
- Commit new project

**Total Time**: 2-3 hours

## Key Principles

### 1. Xcode UI Only
- No manual .pbxproj editing
- All configuration through Xcode interface
- Results in clean, maintainable project

### 2. Proper Framework Embedding
- Both frameworks must be "Embed & Sign"
- Correct search paths configured
- Build phase order matters

### 3. iOS 26 Compatibility
- Bitcode disabled (deprecated)
- Swift Package Manager for Firebase
- Modern build settings

### 4. Preserve Everything
- All 139 Swift files
- All capabilities
- All configuration
- All test files

## Verification

### Before Starting
```bash
./scripts/prepare-ios-rebuild.sh
```

Should show all ✓ checks.

### After Completion
- [ ] Project builds without errors
- [ ] App launches on simulator
- [ ] Firebase initializes
- [ ] Can navigate screens
- [ ] Kotlin module accessible
- [ ] Bridge functional
- [ ] Tests run

## Troubleshooting

### Build Errors

**"No such module 'shared'"**
```bash
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

**"No such module 'EunioBridgeKit'"**
- Check: General → Frameworks → EunioBridgeKit.xcframework
- Verify: Embed & Sign

**"Firebase not configured"**
- Check: Build Phases → Copy Bundle Resources
- Verify: GoogleService-Info.plist is listed

**"Undefined symbols"**
- Check: Build Settings → Other Linker Flags
- Must include: `-ObjC -l"c++"`

### Runtime Errors

**Crash on Launch**
- Verify: FirebaseApp.configure() in app init
- Check: All capabilities enabled

**Firebase Auth Not Working**
- Check: Info → URL Types
- Verify: REVERSED_CLIENT_ID present

## Files Created

```
.kiro/specs/4-kotlin-xcode-ios26-upgrade/
├── ios-project-rebuild-guide.md          # Detailed guide
├── ios-rebuild-checklist.md              # Interactive checklist
├── IOS-REBUILD-README.md                 # This file
├── task-16-ios-rebuild-prep-summary.md   # Preparation summary
└── ios-rebuild-backup/                   # Backup directory
    ├── iosApp.xcodeproj.corrupted/       # Corrupted project
    ├── Info.plist.backup
    ├── GoogleService-Info.plist.backup
    ├── iosApp.entitlements.backup
    ├── PrivacyInfo.xcprivacy.backup
    ├── swift-files-inventory.txt
    └── project-config.txt

scripts/
├── prepare-ios-rebuild.sh                # Preparation (already run)
└── start-ios-rebuild.sh                  # Quick start
```

## Success Criteria

✅ Project builds in Xcode 26  
✅ App runs on iOS 26 simulator  
✅ Firebase services work  
✅ Kotlin shared module accessible  
✅ EunioBridgeKit bridge functional  
✅ All 139 Swift files included  
✅ All tests pass  
✅ Clean .pbxproj file  

## Next Steps

1. **Run the start script**:
   ```bash
   ./scripts/start-ios-rebuild.sh
   ```

2. **Follow the checklist**:
   - Open `ios-rebuild-checklist.md`
   - Work through each phase
   - Check off items as you complete them

3. **Reference the guide**:
   - Open `ios-project-rebuild-guide.md`
   - Use for detailed explanations
   - Refer to troubleshooting section

4. **Commit when done**:
   ```bash
   git add iosApp/iosApp.xcodeproj
   git commit -m "Rebuild iOS project for Xcode 26 (clean .pbxproj)"
   ```

## Support

If you encounter issues:

1. Check the troubleshooting section in the guide
2. Verify all prerequisites are met
3. Review the backup configuration files
4. Ensure you're following steps in order

## Notes

- This is a **manual process** requiring Xcode UI
- Cannot be fully automated
- Take breaks between phases
- Don't skip steps
- Verify each phase before moving on

## Timeline

- **Preparation**: ✅ Complete
- **Rebuild**: 2-3 hours (your time)
- **Testing**: Included in rebuild
- **Commit**: 5 minutes

## References

- Xcode 26 Documentation
- Firebase iOS SDK 11.x
- Kotlin Multiplatform iOS Guide
- Swift Package Manager Docs

---

**Status**: Ready to Begin  
**Date**: October 19, 2025  
**Xcode**: 26.0.1  
**Kotlin**: 2.2.20  
**iOS Target**: 15.0 - 26.0  

**Start Command**: `./scripts/start-ios-rebuild.sh`
