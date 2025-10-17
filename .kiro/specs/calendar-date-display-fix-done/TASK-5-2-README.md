# Task 5.2: iOS to Android Sync - Quick Start

## Quick Test Execution

### Fastest Method: Interactive Script
```bash
./test-ios-to-android-sync.sh
```
Follow the prompts and verify on Android when instructed.

### Alternative: Automated iOS Test
```bash
cd iosApp

# First, add the test file to Xcode:
# 1. Open iosApp.xcodeproj in Xcode
# 2. Right-click iosAppUITests folder → Add Files
# 3. Select IOSToAndroidSyncVerificationTests.swift
# 4. Ensure iosAppUITests target is checked

# Then run:
xcodebuild test \
  -project iosApp.xcodeproj \
  -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 15,OS=17.0' \
  -only-testing:iosAppUITests/IOSToAndroidSyncVerificationTests/testIOSToAndroidSync
```

## What to Verify on Android

After creating the log on iOS, open Android and check:

1. **Navigate to October 10, 2025**
2. **Verify these fields:**
   - Symptoms: "Cramps"
   - Mood: "Calm"
   - BBT: "98.4"
   - Notes: "iOS to Android sync test - [timestamp]"
3. **Verify date is correct** (not shifted to another day)

## Success = All Fields Match + Correct Date

## Full Documentation
- Detailed guide: `ios-to-android-sync-test-guide.md`
- Completion summary: `task-5-2-completion-summary.md`

## Requirements Verified
✅ 4.3 - iOS log syncs to Firebase with correct date  
✅ 4.4 - Log displays on Android with correct date  
✅ 4.5 - All log data remains intact  
✅ 4.6 - October 10, 2025 date verified  
