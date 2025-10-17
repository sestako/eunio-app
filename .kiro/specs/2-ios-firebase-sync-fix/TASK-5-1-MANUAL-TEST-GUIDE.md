# Task 5.1: Manual Test Guide - iOS Firebase Save Operation

## Overview
This guide provides step-by-step instructions for manually testing the iOS Firebase save operation to verify that daily logs are correctly saved to Firestore.

## Test Environment Setup

### Prerequisites
- ✅ Xcode installed with iOS Simulator
- ✅ Firebase project configured
- ✅ Test user account available
- ✅ Internet connection active

### Test Account
Use one of these test accounts:
- Email: `test@example.com`
- Password: `password123`

Or create a new account during testing.

## Test Procedure

### Phase 1: Launch Application

1. **Open Xcode Project**
   ```bash
   cd iosApp
   open iosApp.xcodeproj
   ```

2. **Select Simulator**
   - Choose: iPhone 15 (or any iOS 17+ simulator)
   - Ensure simulator is booted

3. **Build and Run**
   - Press `Cmd + R` or click the Run button
   - Wait for app to launch in simulator

4. **Verify Launch**
   - App should open to sign-in screen
   - No crash or error dialogs

### Phase 2: Authentication

1. **Sign In**
   - Enter email: `test@example.com`
   - Enter password: `password123`
   - Tap "Sign In" button

2. **Verify Authentication**
   - Should navigate to main app (tab bar visible)
   - Check Xcode console for:
     ```
     ✅ Sign in successful: {userId}
     ✅ Stored user info in UserDefaults: {userId}
     ```

### Phase 3: Navigate to Daily Logging

1. **Open Daily Log Tab**
   - Tap "Daily Log" tab in bottom navigation
   - Should see daily logging interface

2. **Verify Date Selection**
   - Current date should be selected by default
   - Date picker should be visible at top

### Phase 4: Fill in Test Data

Fill in the following test data to create a comprehensive log:

1. **Period Flow**
   - Tap "Medium" button
   - Button should highlight in pink

2. **Symptoms**
   - Tap "Cramps" button
   - Tap "Headache" button
   - Both buttons should highlight in pink

3. **Mood**
   - Tap "Happy" emoji (😊)
   - Button should highlight

4. **Basal Body Temperature**
   - Tap BBT text field
   - Enter: `98.6`
   - Verify no validation error appears

5. **Cervical Mucus**
   - Tap "Creamy" button
   - Button should highlight

6. **Ovulation Test**
   - Tap "Positive" button
   - Button should highlight

7. **Sexual Activity**
   - Tap "Yes" button
   - Protection options should appear
   - Tap "Condom" button

8. **Notes**
   - Tap notes text area
   - Enter: `Test log from iOS - Task 5.1 verification`

### Phase 5: Save Operation

1. **Initiate Save**
   - Scroll to bottom
   - Tap "Save Daily Log" button
   - Button should show loading spinner

2. **Monitor Console Output**
   Open Xcode console (Cmd + Shift + Y) and watch for these logs:

   ```
   💾 [SAVE] Starting save operation via shared Kotlin code...
   📤 [FIRESTORE] Saving daily log to Firebase
   📤 [FIRESTORE] Path: users/{userId}/dailyLogs/{date}
   📤 [FIRESTORE] Data: {date=19371, periodFlow=MEDIUM, ...}
   ✅ [FIRESTORE] Successfully saved daily log to Firebase
   ✅ [FIRESTORE] Document ID: {date}
   ✅ [SAVE] Save operation delegated to shared Kotlin code
   ```

3. **Verify Success Message**
   - Green success card should appear at bottom
   - Message: "Log saved successfully"
   - Card should auto-dismiss after 3 seconds

### Phase 6: Verify in Firebase Console

1. **Open Firebase Console**
   - Navigate to: https://console.firebase.google.com
   - Select your project
   - Click "Firestore Database" in left menu

2. **Navigate to Document**
   - Expand: `users` collection
   - Find and expand: `{userId}` document (check console for actual ID)
   - Expand: `dailyLogs` subcollection
   - Find: `{date}` document (format: YYYY-MM-DD, e.g., 2025-10-14)

3. **Verify Document Fields**
   The document should contain these fields with correct values:

   | Field | Expected Value | Type | Notes |
   |-------|---------------|------|-------|
   | `date` | 19371 (example) | number | Epoch days since 1970-01-01 |
   | `periodFlow` | "MEDIUM" | string | Uppercase enum value |
   | `symptoms` | ["CRAMPS", "HEADACHE"] | array | Uppercase enum values |
   | `mood` | "HAPPY" | string | Uppercase enum value |
   | `bbt` | 98.6 | number | Decimal value |
   | `cervicalMucus` | "CREAMY" | string | Uppercase enum value |
   | `opkResult` | "POSITIVE" | string | Uppercase enum value |
   | `sexualActivity` | {occurred: true, protection: "CONDOM"} | map | Nested object |
   | `notes` | "Test log from iOS - Task 5.1 verification" | string | User text |
   | `createdAt` | 1729000000 (example) | number | Seconds since epoch |
   | `updatedAt` | 1729000000 (example) | number | Seconds since epoch |
   | `userId` | {userId} | string | Matches authenticated user |

4. **Verify Path Structure**
   - Path should be: `users/{userId}/dailyLogs/{date}`
   - NOT: `users/{userId}/logs/{date}` (wrong)
   - NOT: `dailyLogs/{userId}/{date}` (wrong)

### Phase 7: Cross-Platform Verification

1. **Check Data Format Consistency**
   - Compare with Android-saved document (if available)
   - Field names should be identical
   - Data types should match exactly
   - Enum values should be uppercase strings

2. **Verify Date Format**
   - Date should be epoch days (number)
   - NOT ISO string (e.g., "2025-10-14")
   - NOT milliseconds timestamp

3. **Verify Timestamp Format**
   - Timestamps should be in seconds
   - NOT milliseconds (would be 13 digits instead of 10)

## Expected Results Checklist

### ✅ UI Behavior
- [ ] Save button shows loading spinner during save
- [ ] Success message appears after save
- [ ] Success message is green with checkmark icon
- [ ] Success message auto-dismisses after 3 seconds
- [ ] No error messages appear
- [ ] Form remains filled after save (data not cleared)

### ✅ Console Logs
- [ ] Save operation start log appears
- [ ] Firestore save log with path appears
- [ ] Firestore save log with data appears
- [ ] Success log with document ID appears
- [ ] No error logs appear
- [ ] Logs include userId and date

### ✅ Firebase Console
- [ ] Document exists at correct path
- [ ] All fields are present
- [ ] Field names match Android format
- [ ] Data types are correct (numbers, strings, arrays)
- [ ] Enum values are uppercase
- [ ] Date is epoch days (number)
- [ ] Timestamps are in seconds (10 digits)
- [ ] userId matches authenticated user

### ✅ Data Integrity
- [ ] Period flow value matches selection
- [ ] All selected symptoms are present
- [ ] Mood value matches selection
- [ ] BBT value is correct decimal
- [ ] Cervical mucus value matches selection
- [ ] OPK result value matches selection
- [ ] Sexual activity data is complete
- [ ] Notes text is preserved exactly
- [ ] Timestamps are reasonable (recent)

## Troubleshooting

### Issue: Save Button Does Nothing

**Symptoms:**
- Tapping save button has no effect
- No loading spinner appears
- No console logs appear

**Solutions:**
1. Check that form has unsaved changes
2. Verify BBT value is valid (95-105°F)
3. Check that user is authenticated
4. Restart the app

### Issue: Error Message Appears

**Symptoms:**
- Red error card appears after save
- Error message displayed

**Common Errors:**

1. **"No internet connection"**
   - Check simulator has network access
   - Try opening Safari in simulator
   - Restart simulator

2. **"Please sign in again"**
   - User authentication expired
   - Sign out and sign in again
   - Check Firebase Auth console

3. **"Permission denied"**
   - Firebase security rules issue
   - Check rules allow writes for authenticated users
   - Verify user ID matches rule

### Issue: No Console Logs

**Symptoms:**
- Save appears to work but no logs in console
- Console is empty or shows unrelated logs

**Solutions:**
1. Ensure console is visible (Cmd + Shift + Y)
2. Check console filter (should show "All Output")
3. Look for NSLog output (not just print statements)
4. Verify StructuredLogger is enabled
5. Check that you're viewing the correct target

### Issue: Document Not in Firebase

**Symptoms:**
- Save succeeds but document not in Firestore
- Console shows success but Firebase Console is empty

**Solutions:**
1. Wait 5-10 seconds and refresh Firebase Console
2. Verify you're looking at correct project
3. Check correct user ID (from console logs)
4. Verify date format (YYYY-MM-DD)
5. Check Firebase security rules
6. Look for document in different collection (wrong path)

### Issue: Wrong Data Format

**Symptoms:**
- Document exists but fields are wrong type
- Date is string instead of number
- Timestamps are in milliseconds

**Solutions:**
1. Check DailyLogDto serialization
2. Verify FirebaseIOSBridge data conversion
3. Compare with Android document format
4. Check Kotlin/Swift bridge data mapping

## Success Criteria

All of the following must be true for task 5.1 to be complete:

1. ✅ **Requirement 1.1**: iOS user can save daily log to Firebase
2. ✅ **Requirement 1.2**: Log uses correct Firestore path structure
3. ✅ **Requirement 1.5**: Success message displays after save
4. ✅ **Requirement 4.2**: "Log saved successfully" message appears
5. ✅ **Requirement 6.1**: Firebase logs show successful writes
6. ✅ **Requirement 6.2**: Document appears in Firebase Console with correct fields

## Next Steps

After completing this test:

1. Document results in completion summary
2. Take screenshots of:
   - Success message in app
   - Console logs
   - Firebase Console document
3. Proceed to Task 5.2: Test read operation
4. If any issues found, create bug reports with details

## Test Data Cleanup

After testing, you may want to clean up test data:

1. **Delete Test Document**
   - In Firebase Console
   - Navigate to test document
   - Click three dots menu
   - Select "Delete document"

2. **Or Keep for Next Test**
   - Leave document for Task 5.2 (read operation test)
   - Use same document to verify read functionality

## Notes

- This is a manual test that requires human verification
- Automated tests exist but don't replace manual verification
- Take your time and verify each step carefully
- Document any unexpected behavior
- Screenshots are valuable for documentation

## Related Documents

- [TASK-5-1-TEST-GUIDE.md](./TASK-5-1-TEST-GUIDE.md) - Quick reference guide
- [verify-test-readiness.sh](./verify-test-readiness.sh) - Automated verification script
- [LOGGING-EXAMPLES.md](./LOGGING-EXAMPLES.md) - Expected log output examples
- [requirements.md](./requirements.md) - Full requirements document
- [design.md](./design.md) - Architecture and design details
