# Android to iOS Sync Test Guide

## Overview

This guide provides step-by-step instructions for testing cross-platform Firebase sync between Android and iOS for the calendar date display fix. The test verifies that daily logs created on Android sync correctly to iOS with the correct dates and data integrity.

**Test Requirements:** 4.1, 4.2, 4.5, 4.6

## Prerequisites

### Required Setup

1. **Android Device/Emulator**
   - Android Studio installed
   - Android device or emulator running
   - App installed and authenticated

2. **iOS Device/Simulator**
   - Xcode installed
   - iOS device or simulator running
   - App installed and authenticated

3. **Firebase Configuration**
   - Both apps connected to the same Firebase project
   - Firestore database accessible
   - Authentication enabled
   - Internet connection on both devices

4. **User Account**
   - Same user account logged in on both Android and iOS
   - User must be authenticated before running tests

## Test Execution Steps

### Phase 1: Android Test Execution

#### Step 1: Run Android Sync Tests

```bash
# Navigate to project root
cd /path/to/Eunio-app

# Run Android instrumentation tests
./gradlew :androidApp:connectedAndroidTest \
  --tests "com.eunio.healthapp.android.sync.AndroidToIOSSyncTest"
```

#### Step 2: Verify Android Test Results

Expected console output:
```
✅ Android to iOS Sync Test: Daily log created successfully
📝 Test Data Summary:
   Date: October 10, 2025
   Period Flow: Light
   Symptoms: Headache, Cramps
   Mood: Happy
   BBT: 98.2°F
   Notes: Android to iOS sync test - October 10, 2025

⏳ Waiting for Firebase sync to complete...
```

#### Step 3: Wait for Firebase Sync

**IMPORTANT:** Wait 10-15 seconds after Android tests complete to allow Firebase sync to finish.

You can verify sync in Firebase Console:
1. Open Firebase Console
2. Navigate to Firestore Database
3. Look for collection: `dailyLogs` or `users/{userId}/dailyLogs`
4. Verify document exists with date: `2025-10-10`

### Phase 2: iOS Verification

#### Step 4: Run iOS Verification Tests

```bash
# Open Xcode project
cd iosApp
open iosApp.xcodeproj

# Or run from command line:
xcodebuild test \
  -project iosApp.xcodeproj \
  -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 15' \
  -only-testing:iosAppUITests/AndroidToIOSSyncVerificationTests
```

#### Step 5: Verify iOS Test Results

Expected console output:
```
📱 iOS Sync Verification Test: Checking for Android-created log
   Expected Date: October 10, 2025
   Expected Data: Period Flow=Light, Mood=Happy, BBT=98.2

✅ Period Flow verified: Light
✅ Symptoms verified: Headache, Cramps
✅ Mood verified: Happy
✅ BBT verified: 98.2°F
✅ Notes verified: Contains 'Android to iOS sync test'

🎉 SUCCESS: All data from Android log synced correctly to iOS!
   ✓ Date: October 10, 2025
   ✓ Period Flow: Light
   ✓ Symptoms: Headache, Cramps
   ✓ Mood: Happy
   ✓ BBT: 98.2°F
   ✓ Notes: Android to iOS sync test
```

## Test Cases

### Test Case 1: Single Date Sync (October 10, 2025)

**Android Test:** `testCreateDailyLogForOctober10_2025()`

**Test Data:**
- Date: October 10, 2025
- Period Flow: Light
- Symptoms: Headache, Cramps
- Mood: Happy
- BBT: 98.2°F
- Notes: "Android to iOS sync test - October 10, 2025"

**iOS Verification:** `testVerifyAndroidLogAppearsOnIOS_October10()`

**Success Criteria:**
- ✅ Log appears on iOS with correct date (October 10, 2025)
- ✅ Period Flow is "Light"
- ✅ Symptoms include "Headache" and "Cramps"
- ✅ Mood is "Happy"
- ✅ BBT is 98.2°F
- ✅ Notes contain "Android to iOS sync test"
- ✅ No date shifting or timezone issues

### Test Case 2: Multiple Date Sync Integrity

**Android Test:** `testMultipleDateSyncIntegrity()`

**Test Data:**
- October 8, 2025: Note "Oct 8 test"
- October 9, 2025: Note "Oct 9 test"
- October 10, 2025: Note "Oct 10 test"
- October 11, 2025: Note "Oct 11 test"
- October 12, 2025: Note "Oct 12 test"

**iOS Verification:** `testVerifyMultipleDateSyncIntegrity()`

**Success Criteria:**
- ✅ All 5 logs appear on iOS
- ✅ Each log has the correct date (no shifting)
- ✅ Each log has the correct note text
- ✅ Dates are in correct chronological order

### Test Case 3: Date Format Integrity

**Android Test:** `testDateFormatIntegrity()`

**Test Data:**
- Date: October 10, 2025
- Notes: "Date format integrity test"

**iOS Verification:** `testVerifyDateFormatIntegrity()`

**Success Criteria:**
- ✅ Date is displayed as October 10, 2025 on iOS
- ✅ No timezone conversion issues
- ✅ Date format matches Android (ISO 8601)

## Manual Verification (Alternative)

If automated tests fail or are unavailable, follow these manual steps:

### Manual Android Steps

1. Open Android app
2. Navigate to Daily Logging screen
3. Verify date is October 10, 2025
4. Fill in test data:
   - Period Flow: Light
   - Symptoms: Headache, Cramps
   - Mood: Happy
   - BBT: 98.2
   - Notes: "Manual Android to iOS sync test"
5. Tap "Save"
6. Verify success message
7. Wait 15 seconds for sync

### Manual iOS Steps

1. Open iOS app
2. Navigate to Daily Logging screen
3. Tap date picker
4. Select October 10, 2025
5. Verify the following data appears:
   - Period Flow: Light
   - Symptoms: Headache, Cramps
   - Mood: Happy
   - BBT: 98.2°F
   - Notes: "Manual Android to iOS sync test"
6. Verify date is displayed as October 10, 2025 (not shifted)

## Troubleshooting

### Issue: iOS tests fail to find synced data

**Possible Causes:**
1. Firebase sync hasn't completed yet
2. Different user accounts on Android and iOS
3. Network connectivity issues
4. Firebase configuration mismatch

**Solutions:**
1. Wait longer (30-60 seconds) and re-run iOS tests
2. Verify same user is logged in on both platforms
3. Check internet connection on both devices
4. Verify Firebase project ID matches in both apps

### Issue: Date appears shifted by one day

**Possible Causes:**
1. Timezone conversion issue
2. Date stored as timestamp instead of LocalDate
3. Different timezone settings on devices

**Solutions:**
1. Check that date is stored as LocalDate (not Instant)
2. Verify timezone handling in Firebase serialization
3. Check device timezone settings

### Issue: Some data fields are missing

**Possible Causes:**
1. Incomplete save on Android
2. Serialization issue
3. Firebase security rules blocking fields

**Solutions:**
1. Re-run Android test and verify save completes
2. Check Firebase Console to see what data was written
3. Review Firestore security rules

### Issue: Authentication errors

**Possible Causes:**
1. User not logged in
2. Token expired
3. Firebase Auth not configured

**Solutions:**
1. Log in on both devices before running tests
2. Restart apps to refresh auth tokens
3. Verify Firebase Auth is enabled in console

## Verification Checklist

Use this checklist to verify test completion:

### Android Phase
- [ ] Android tests run successfully
- [ ] Console shows "✅ Android to iOS Sync Test: Daily log created successfully"
- [ ] Waited 10-15 seconds for Firebase sync
- [ ] Verified data in Firebase Console (optional)

### iOS Phase
- [ ] iOS tests run successfully
- [ ] Console shows "🎉 SUCCESS: All data from Android log synced correctly to iOS!"
- [ ] All data fields verified:
  - [ ] Date: October 10, 2025
  - [ ] Period Flow: Light
  - [ ] Symptoms: Headache, Cramps
  - [ ] Mood: Happy
  - [ ] BBT: 98.2°F
  - [ ] Notes: Contains test text
- [ ] No date shifting observed
- [ ] Multiple date test passed (if run)

### Final Verification
- [ ] All test cases passed
- [ ] No errors in console
- [ ] Data integrity maintained
- [ ] Dates are correct on both platforms
- [ ] Requirements 4.1, 4.2, 4.5, 4.6 satisfied

## Test Results Documentation

### Test Execution Date
Date: _______________

### Test Environment
- Android Device/Emulator: _______________
- Android OS Version: _______________
- iOS Device/Simulator: _______________
- iOS Version: _______________
- Firebase Project: _______________

### Test Results

#### Test Case 1: Single Date Sync
- Status: [ ] PASS [ ] FAIL
- Notes: _______________________________________________

#### Test Case 2: Multiple Date Sync
- Status: [ ] PASS [ ] FAIL
- Notes: _______________________________________________

#### Test Case 3: Date Format Integrity
- Status: [ ] PASS [ ] FAIL
- Notes: _______________________________________________

### Issues Encountered
_______________________________________________
_______________________________________________
_______________________________________________

### Overall Result
- [ ] All tests passed - Task 5.1 COMPLETE
- [ ] Some tests failed - See issues above
- [ ] Tests could not be run - See notes

### Tester Signature
Name: _______________
Date: _______________

## Next Steps

After completing Task 5.1 (Android to iOS sync), proceed to:

**Task 5.2:** Test iOS to Android sync
- Create logs on iOS
- Verify they appear on Android
- Follow similar process in reverse

**Task 5.3:** Test bidirectional updates
- Update logs on Android
- Verify updates sync to iOS
- Update logs on iOS
- Verify updates sync to Android

**Task 5.4:** Test multiple date sync integrity
- Create logs on multiple dates
- Verify all sync correctly
- Check for date shifting issues

## References

- Requirements Document: `.kiro/specs/calendar-date-display-fix/requirements.md`
- Design Document: `.kiro/specs/calendar-date-display-fix/design.md`
- Android Test File: `androidApp/src/androidTest/kotlin/com/eunio/healthapp/android/sync/AndroidToIOSSyncTest.kt`
- iOS Test File: `iosApp/iosAppUITests/AndroidToIOSSyncVerificationTests.swift`
