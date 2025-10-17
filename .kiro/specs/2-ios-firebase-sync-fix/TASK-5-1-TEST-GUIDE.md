# Task 5.1: iOS Simulator Save Operation Test Guide

## Overview
This guide walks through testing the iOS Firebase save operation on the simulator to verify that daily logs are successfully saved to Firebase and that all requirements are met.

## Prerequisites
- iOS simulator running
- Firebase project configured
- User authenticated in the app
- Xcode console visible for log monitoring

## Test Procedure

### Step 1: Launch iOS App on Simulator

```bash
# Open the iOS project in Xcode
cd iosApp
open iosApp.xcodeproj
```

**Actions:**
1. Select a simulator (e.g., iPhone 15 Pro)
2. Build and run the app (Cmd+R)
3. Wait for app to launch

### Step 2: Authenticate User

**Actions:**
1. If not logged in, sign in with test credentials
2. Verify you reach the main app screen
3. Note the user ID from console logs (look for authentication logs)

**Expected Console Output:**
```
[Auth] User authenticated: userId=<USER_ID>
```

### Step 3: Navigate to Daily Logging Screen

**Actions:**
1. Tap on the "Log" or "Daily Logging" tab
2. Verify the daily logging interface loads
3. Select today's date or a specific date

### Step 4: Fill Out Daily Log Form

**Actions:**
1. Enter test data:
   - Period Flow: Select "Light" or "Medium"
   - Symptoms: Select 1-2 symptoms (e.g., "Cramps", "Headache")
   - Mood: Select a mood (e.g., "Happy")
   - BBT: Enter a temperature (e.g., 36.5)
   - Cervical Mucus: Select an option
   - Notes: Enter "Test log from iOS simulator"

### Step 5: Save the Daily Log

**Actions:**
1. Tap the "Save" button
2. Observe the UI for feedback

**Expected UI Behavior:**
- ✅ Loading indicator appears briefly
- ✅ Success message displays: "Log saved successfully"
- ✅ Message disappears after ~3 seconds
- ✅ Form remains populated with saved data

**Requirement Verification:**
- ✅ Requirement 1.1: Log persisted to Firebase
- ✅ Requirement 1.5: Success message displayed
- ✅ Requirement 4.2: Success feedback shown

### Step 6: Check Xcode Console for Structured Logs

**Expected Console Output Pattern:**

```
[FirestoreService] Saving daily log to Firebase
  userId: <USER_ID>
  logId: <LOG_ID>
  date: 2025-10-14
  path: users/<USER_ID>/dailyLogs/<LOG_ID>

[FirebaseIOSBridge] saveDailyLog called
  path: users/<USER_ID>/dailyLogs/<LOG_ID>
  fields: [date, periodFlow, symptoms, mood, bbt, cervicalMucus, notes, createdAt, updatedAt]

[FirebaseIOSBridge] Document saved successfully
  documentId: <LOG_ID>
  timestamp: <TIMESTAMP>

[FirestoreService] Successfully saved daily log to Firebase
  userId: <USER_ID>
  logId: <LOG_ID>
  duration: <DURATION>ms
```

**Verification Checklist:**
- ✅ "Saving daily log to Firebase" message appears
- ✅ userId and logId are logged
- ✅ Correct path format: `users/{userId}/dailyLogs/{logId}`
- ✅ "Document saved successfully" message appears
- ✅ No error messages in console

**Requirement Verification:**
- ✅ Requirement 1.2: Correct Firestore path structure used
- ✅ Requirement 6.1: Firebase logs show successful writes

### Step 7: Verify Document in Firebase Console

**Actions:**
1. Open Firebase Console: https://console.firebase.google.com
2. Select your project
3. Navigate to Firestore Database
4. Browse to: `users` → `<USER_ID>` → `dailyLogs` → `<LOG_ID>`

**Expected Document Structure:**

```json
{
  "date": 20371,                    // Epoch days (e.g., for 2025-10-14)
  "periodFlow": "Light",
  "symptoms": ["Cramps", "Headache"],
  "mood": "Happy",
  "bbt": 36.5,
  "cervicalMucus": "Sticky",
  "notes": "Test log from iOS simulator",
  "createdAt": 1729000000,          // Seconds since epoch
  "updatedAt": 1729000000           // Seconds since epoch
}
```

**Verification Checklist:**
- ✅ Document exists at correct path
- ✅ `date` field is a number (epoch days)
- ✅ `createdAt` is a number (seconds since epoch)
- ✅ `updatedAt` is a number (seconds since epoch)
- ✅ All entered fields are present
- ✅ Field names match exactly (camelCase)
- ✅ Data types are correct (numbers, strings, arrays)

**Requirement Verification:**
- ✅ Requirement 1.1: Log persisted to Firebase
- ✅ Requirement 1.2: Correct path structure
- ✅ Requirement 1.3: Correct data format (epoch days, seconds)
- ✅ Requirement 6.2: Document visible in Firebase Console

### Step 8: Verify Field Values Match Input

**Actions:**
1. Compare each field in Firebase Console with what you entered
2. Verify data type conversions are correct

**Field Validation:**

| Field | Input Type | Firebase Type | Example Input | Expected Firebase Value |
|-------|-----------|---------------|---------------|------------------------|
| date | Date | Number (epoch days) | 2025-10-14 | 20371 |
| periodFlow | String | String | "Light" | "Light" |
| symptoms | Array | Array | ["Cramps"] | ["Cramps"] |
| mood | String | String | "Happy" | "Happy" |
| bbt | Number | Number | 36.5 | 36.5 |
| cervicalMucus | String | String | "Sticky" | "Sticky" |
| notes | String | String | "Test log..." | "Test log..." |
| createdAt | Timestamp | Number (seconds) | Now | 1729000000 |
| updatedAt | Timestamp | Number (seconds) | Now | 1729000000 |

**Requirement Verification:**
- ✅ Requirement 1.3: Data stored in same format as Android

## Error Scenario Testing (Optional)

### Test Network Error Handling

**Actions:**
1. Enable Airplane Mode on simulator
2. Try to save a daily log
3. Observe error message

**Expected Behavior:**
- ✅ Error message: "Failed to save: Check your internet connection"
- ✅ Error logged to console
- ✅ Requirement 4.3: Network error handling works

### Test Authentication Error Handling

**Actions:**
1. Sign out of the app
2. Try to save a daily log (if possible)

**Expected Behavior:**
- ✅ Error message: "Failed to save: Please sign in again"
- ✅ Requirement 4.4: Auth error handling works

## Success Criteria Summary

All of the following must be true for this task to pass:

- [ ] iOS app successfully saves daily log
- [ ] "Log saved successfully" message appears in UI
- [ ] Xcode console shows structured logs with correct path
- [ ] Firebase Console shows document at `users/{userId}/dailyLogs/{logId}`
- [ ] Document contains all expected fields with correct data types
- [ ] `date` field uses epoch days format
- [ ] `createdAt` and `updatedAt` use seconds since epoch
- [ ] Field names match Android implementation exactly
- [ ] No errors in Xcode console during save operation

## Requirements Coverage

This test verifies the following requirements:

- ✅ **Requirement 1.1**: iOS user saves daily log to Firebase using Firebase iOS SDK
- ✅ **Requirement 1.2**: Same Firestore path structure as Android
- ✅ **Requirement 1.3**: Same data format as Android (epoch days, seconds)
- ✅ **Requirement 1.5**: Success message displayed
- ✅ **Requirement 4.2**: Success feedback shown for 3 seconds
- ✅ **Requirement 6.1**: Firebase logs show successful writes
- ✅ **Requirement 6.2**: Document appears in Firebase Console

## Troubleshooting

### Issue: No logs appear in Xcode console

**Solution:**
1. Check that StructuredLogger is enabled
2. Verify log level is set to INFO or DEBUG
3. Check console filter settings in Xcode

### Issue: "Log saved successfully" doesn't appear

**Solution:**
1. Check that success message UI component is implemented
2. Verify Result.isSuccess is being checked
3. Check for UI update on main thread

### Issue: Document not in Firebase Console

**Solution:**
1. Verify Firebase is initialized: Check for `FirebaseApp.configure()` in app startup
2. Check authentication: User must be signed in
3. Verify network connection
4. Check Firestore security rules allow writes
5. Look for errors in Xcode console

### Issue: Wrong data format in Firebase

**Solution:**
1. Verify DailyLogDto.toMap() is being used
2. Check date conversion to epoch days
3. Check timestamp conversion to seconds
4. Compare with Android implementation

## Next Steps

After completing this test:

1. Document results in a completion summary
2. Take screenshots of:
   - Success message in app
   - Xcode console logs
   - Firebase Console document
3. Move to Task 5.2: Test read operation
4. If any issues found, create bug reports with details

## Test Execution Log Template

```markdown
# Task 5.1 Test Execution Results

**Date:** 2025-10-14
**Tester:** [Your Name]
**Simulator:** iPhone 15 Pro (iOS 17.0)
**User ID:** [USER_ID]
**Log ID:** [LOG_ID]

## Test Results

### Save Operation
- [ ] Success message appeared: YES/NO
- [ ] Message text: "[actual text]"
- [ ] Message duration: ~3 seconds

### Console Logs
- [ ] Structured logs present: YES/NO
- [ ] Correct path format: YES/NO
- [ ] No errors: YES/NO

### Firebase Console
- [ ] Document exists: YES/NO
- [ ] Correct path: YES/NO
- [ ] All fields present: YES/NO
- [ ] Correct data types: YES/NO

### Field Verification
- [ ] date: [value] (epoch days)
- [ ] periodFlow: [value]
- [ ] symptoms: [value]
- [ ] mood: [value]
- [ ] bbt: [value]
- [ ] cervicalMucus: [value]
- [ ] notes: [value]
- [ ] createdAt: [value] (seconds)
- [ ] updatedAt: [value] (seconds)

## Issues Found
[List any issues or discrepancies]

## Screenshots
[Attach screenshots]

## Conclusion
PASS / FAIL

## Notes
[Any additional observations]
```
