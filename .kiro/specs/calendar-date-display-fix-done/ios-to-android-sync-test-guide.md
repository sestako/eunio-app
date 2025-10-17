# iOS to Android Sync Test Guide

## Overview
This guide provides step-by-step instructions for testing data synchronization from iOS to Android through Firebase, verifying that daily logs created on iOS appear correctly on Android with accurate dates and complete data.

## Test Objective
Verify that daily logs created on iOS for October 10, 2025 sync to Android with:
- Correct date (October 10, 2025)
- All data fields intact (symptoms, mood, BBT, notes)
- No date shifting or timezone issues
- Proper data integrity

## Prerequisites

### iOS Device/Simulator
- iOS app installed and running
- Logged in with test account
- Internet connection active
- Firebase authentication working

### Android Device/Emulator
- Android app installed and running
- Logged in with **same test account** as iOS
- Internet connection active
- Firebase authentication working

### Firebase Console Access
- Access to Firebase Console
- Ability to view Firestore database
- Ability to monitor real-time sync

## Test Procedure

### Part 1: Create Daily Log on iOS

#### Step 1: Launch iOS App
1. Open the iOS app on your device/simulator
2. Ensure you're logged in with the test account
3. Wait for the app to fully load

#### Step 2: Navigate to Daily Logging
1. Tap on the "Daily Logging" tab (usually second tab in bottom navigation)
2. Wait for the Daily Logging screen to appear
3. Verify the screen loads without errors

#### Step 3: Select October 10, 2025
1. Locate the date picker at the top of the screen
2. Tap on the date picker to open date selection
3. Navigate to October 2025
4. Select October 10, 2025
5. Verify the selected date is displayed correctly

#### Step 4: Enter Test Data
Enter the following test data:

**Symptoms:**
- Tap on symptoms field
- Enter: "Cramps"

**Mood:**
- Tap on mood selector
- Select: "Calm"

**BBT (Basal Body Temperature):**
- Tap on BBT field
- Enter: "98.4"

**Notes:**
- Tap on notes field
- Enter: "iOS to Android sync test - [current timestamp]"
- Example: "iOS to Android sync test - 2025-10-10 14:30"

#### Step 5: Save the Log
1. Tap the "Save" button
2. Wait for save confirmation
3. Look for success message (e.g., "Log saved successfully")
4. Verify no error messages appear

#### Step 6: Verify Save Success on iOS
1. Navigate away from the date (select a different date)
2. Navigate back to October 10, 2025
3. Verify the log data is still present
4. Confirm all fields show the correct data

### Part 2: Wait for Firebase Sync

#### Step 7: Monitor Firebase Sync
1. Wait **at least 10 seconds** for Firebase sync to complete
2. Optionally, check Firebase Console:
   - Open Firebase Console
   - Navigate to Firestore Database
   - Look for the daily log document
   - Verify it contains the correct data and date

**Expected Firebase Document Structure:**
```json
{
  "userId": "test-user-id",
  "date": "2025-10-10",
  "symptoms": ["Cramps"],
  "mood": "Calm",
  "bbt": 98.4,
  "notes": "iOS to Android sync test - 2025-10-10 14:30",
  "createdAt": "2025-10-10T14:30:00Z",
  "updatedAt": "2025-10-10T14:30:00Z"
}
```

### Part 3: Verify on Android

#### Step 8: Launch Android App
1. Open the Android app on your device/emulator
2. Ensure you're logged in with the **same test account**
3. Wait for the app to fully load
4. Allow time for Firebase sync to download data

#### Step 9: Navigate to Daily Logging
1. Tap on the "Daily Logging" tab/button
2. Wait for the Daily Logging screen to appear
3. Verify the screen loads without errors

#### Step 10: Select October 10, 2025
1. Locate the date navigation section
2. Use the calendar or date navigation to select October 10, 2025
3. Verify the date is displayed correctly in the UI
4. Check that the quick date selection shows October dates (not January)

#### Step 11: Verify Synced Data
Check that all data fields match the iOS entry:

**✅ Verification Checklist:**
- [ ] Date displays as "October 10, 2025" (not shifted)
- [ ] Symptoms field shows "Cramps"
- [ ] Mood shows "Calm"
- [ ] BBT shows "98.4"
- [ ] Notes show "iOS to Android sync test - [timestamp]"
- [ ] No data corruption or missing fields
- [ ] No duplicate entries

#### Step 12: Verify Date Integrity
1. Navigate to October 9, 2025 - should be empty (unless you created a log there)
2. Navigate to October 11, 2025 - should be empty (unless you created a log there)
3. Navigate back to October 10, 2025 - log should still be present
4. Verify no date shifting occurred (log is on correct date)

## Automated Test Execution

### Running the iOS UI Test

```bash
# Navigate to iOS app directory
cd iosApp

# Run the iOS to Android sync test
xcodebuild test \
  -project iosApp.xcodeproj \
  -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 15,OS=17.0' \
  -only-testing:iosAppUITests/IOSToAndroidSyncVerificationTests/testIOSToAndroidSync
```

### Test Output
The automated test will:
1. Create a daily log on iOS for October 10, 2025
2. Save the log with test data
3. Wait for Firebase sync
4. Print verification instructions for manual Android check

**Note:** The automated test creates the iOS log but requires manual verification on Android since we cannot automate cross-platform testing in a single test suite.

## Expected Results

### ✅ Success Criteria
- Daily log created on iOS saves successfully
- Log syncs to Firebase within 10 seconds
- Log appears on Android with correct date (October 10, 2025)
- All data fields are intact and match iOS entry:
  - Symptoms: "Cramps"
  - Mood: "Calm"
  - BBT: "98.4"
  - Notes: "iOS to Android sync test - [timestamp]"
- No date shifting or timezone issues
- No data corruption or missing fields

### ❌ Failure Scenarios

**Scenario 1: Log doesn't appear on Android**
- Possible causes:
  - Firebase sync not working
  - Different user accounts on iOS and Android
  - Network connectivity issues
  - Firebase rules blocking sync
- Troubleshooting:
  - Check Firebase Console for the document
  - Verify both apps use same user account
  - Check network connectivity
  - Review Firebase security rules

**Scenario 2: Date is incorrect on Android**
- Possible causes:
  - Timezone conversion issues
  - Date parsing errors
  - Calendar display bug
- Troubleshooting:
  - Check Firebase document date field
  - Verify timezone handling in Android code
  - Review date parsing logic

**Scenario 3: Data fields are missing or corrupted**
- Possible causes:
  - Serialization/deserialization issues
  - Firebase data model mismatch
  - Field mapping errors
- Troubleshooting:
  - Check Firebase document structure
  - Verify data model consistency
  - Review serialization code

**Scenario 4: Duplicate entries appear**
- Possible causes:
  - Multiple save operations
  - Sync conflict resolution issues
  - Document ID generation problems
- Troubleshooting:
  - Check Firebase for duplicate documents
  - Review document ID generation logic
  - Verify sync conflict resolution

## Multiple Date Testing

### Test Multiple Dates (Optional Extended Test)
To verify comprehensive sync integrity:

1. Create logs on iOS for multiple dates:
   - October 8, 2025
   - October 9, 2025
   - October 10, 2025
   - October 11, 2025
   - October 12, 2025

2. Wait for Firebase sync (15-20 seconds)

3. Verify on Android that all logs appear with correct dates

4. Confirm no date shifting across the date range

### Running Multiple Date Test

```bash
xcodebuild test \
  -project iosApp.xcodeproj \
  -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 15,OS=17.0' \
  -only-testing:iosAppUITests/IOSToAndroidSyncVerificationTests/testMultipleDateIOSToAndroidSync
```

## Troubleshooting

### Firebase Console Verification
1. Open Firebase Console: https://console.firebase.google.com
2. Select your project
3. Navigate to Firestore Database
4. Look for collection: `dailyLogs` or `users/{userId}/dailyLogs`
5. Find document for October 10, 2025
6. Verify document contains correct data

### Network Monitoring
- Use Firebase Console to monitor real-time sync
- Check app logs for sync errors
- Verify network connectivity on both devices

### Authentication Issues
- Ensure both apps are logged in with same account
- Check Firebase Authentication console
- Verify user ID matches on both platforms

## Requirements Coverage

This test verifies the following requirements:

- **Requirement 4.3:** Daily log created on iOS syncs to Firebase with correct date
- **Requirement 4.4:** Log viewed on Android displays with correct date matching iOS entry
- **Requirement 4.5:** All log data remains intact and accurate during sync
- **Requirement 4.6:** Test verifies logs created on October 10, 2025 appear with correct date on both platforms

## Test Completion Checklist

- [ ] iOS app launched successfully
- [ ] Navigated to Daily Logging screen on iOS
- [ ] Selected October 10, 2025 on iOS
- [ ] Entered all test data (symptoms, mood, BBT, notes)
- [ ] Saved log successfully on iOS
- [ ] Verified save success on iOS
- [ ] Waited for Firebase sync (10+ seconds)
- [ ] Launched Android app with same account
- [ ] Navigated to Daily Logging screen on Android
- [ ] Selected October 10, 2025 on Android
- [ ] Verified date displays correctly (October 10, 2025)
- [ ] Verified all data fields match iOS entry
- [ ] Verified no date shifting occurred
- [ ] Verified no data corruption
- [ ] Documented test results

## Next Steps

After completing this test:
1. Document results in test report
2. If successful, proceed to Task 5.3 (Bidirectional updates)
3. If failures occur, investigate and fix issues before proceeding
4. Update task status in tasks.md

## Related Documents
- `requirements.md` - Requirements 4.3, 4.4, 4.5, 4.6
- `design.md` - Cross-Platform Sync Testing section
- `tasks.md` - Task 5.2
- `android-to-ios-sync-test-guide.md` - Reverse direction test
