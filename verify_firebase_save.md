# Verify Firebase Save - Testing Guide

## Steps to Verify Daily Log is Saved to Firebase

### 1. Check Xcode Console Logs

When you save a daily log in the iOS app, watch the Xcode console for these messages:

```
📱 ViewModel.loadLogForSelectedDate() called for: 2025-10-22
💾 [SAVE] Starting save operation via shared Kotlin code...
[FirestoreService.iOS] SAVE_DAILY_LOG_START - userId: xxx, logId: 2025-10-22, path: users/xxx/dailyLogs/2025-10-22
[FirestoreService.iOS] SAVE_DAILY_LOG_SUCCESS - userId: xxx, logId: 2025-10-22
✅ [SAVE] Save operation delegated to shared Kotlin code
```

### 2. Check Firebase Console

1. Open Firebase Console: https://console.firebase.google.com/
2. Select your project (Eunio Health App)
3. Click on "Firestore Database" in the left menu
4. Navigate to the data structure:
   ```
   users/
     └── {your-user-id}/
           └── dailyLogs/
                 └── 2025-10-22  (or whatever date you saved)
   ```

### 3. Verify Data Structure

The saved document should have this structure:
```json
{
  "v": 1,
  "d": 19664,  // date as epoch days
  "pf": "MEDIUM",  // period flow (if set)
  "s": ["CRAMPS", "FATIGUE"],  // symptoms array
  "m": "TIRED",  // mood
  "bbt": 98.6,  // basal body temperature
  "cm": "CREAMY",  // cervical mucus
  "opk": "NEGATIVE",  // OPK result
  "sa": {  // sexual activity
    "o": true,
    "p": "CONDOM"
  },
  "n": "Feeling tired today",  // notes
  "ca": 1729641600,  // created at (epoch seconds)
  "ua": 1729641600   // updated at (epoch seconds)
}
```

### 4. Common Issues

**If logs don't appear in Firebase:**

1. **Check Authentication**: Make sure you're logged in
   - Look for: `Current user ID: xxx` in console
   - If you see `❌ No user ID - not logged in`, you need to log in first

2. **Check Firebase Rules**: Verify Firestore security rules allow writes
   ```javascript
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /users/{userId}/dailyLogs/{logId} {
         allow read, write: if request.auth != null && request.auth.uid == userId;
       }
     }
   }
   ```

3. **Check Network Connection**: Ensure device/simulator has internet
   - Firestore works offline but needs connection to sync

4. **Check for Errors**: Look for error messages in console:
   ```
   [FirestoreService.iOS] SAVE_DAILY_LOG_ERROR - error: ...
   ```

### 5. Test Scenario

**Complete test flow:**
1. Open the app
2. Log in (if not already logged in)
3. Navigate to Daily Logging
4. Select today's date
5. Add some data:
   - Period Flow: Medium
   - Symptoms: Cramps, Fatigue
   - Mood: Tired
6. Click "Save"
7. Watch console for success message
8. Check Firebase Console for the document

**Then test date switching:**
1. Change to yesterday
2. Form should be empty (bug fix verification)
3. Add different data
4. Save
5. Switch back to today
6. Should see original data (not yesterday's)
7. Check Firebase Console - should have 2 documents

### 6. Expected Console Output

```
📱 ViewModel.loadLogForSelectedDate() called for: 2025-10-22
📱 Current user ID: abc123xyz
📱 Setting isLoading = true
📱 Calling getDailyLogUseCase...
📱 ✅ UseCase SUCCESS - log: NULL
💾 [SAVE] Starting save operation via shared Kotlin code...
[FirestoreService.iOS] SAVE_DAILY_LOG_START
  userId: abc123xyz
  logId: 2025-10-22
  path: users/abc123xyz/dailyLogs/2025-10-22
  dateEpochDays: 19664
[FirestoreService.iOS] SAVE_DAILY_LOG_SUCCESS
  userId: abc123xyz
  logId: 2025-10-22
  updatedAt: 1729641600
✅ [SAVE] Save operation delegated to shared Kotlin code
✅ Save successful for user: abc123xyz, date: 2025-10-22
```

### 7. Debugging Tips

If you don't see the expected logs:

1. **Enable verbose logging** in Xcode:
   - Product > Scheme > Edit Scheme
   - Run > Arguments
   - Add environment variable: `OS_ACTIVITY_MODE` = `disable`

2. **Filter console output**:
   - In Xcode console, use the filter box
   - Search for: `FirestoreService` or `SAVE` or `DailyLog`

3. **Check Firebase SDK initialization**:
   - Look for Firebase initialization logs at app startup
   - Should see: `Firebase configured successfully`

4. **Test with a simple log first**:
   - Just set Period Flow to "Medium"
   - Don't add other fields
   - This isolates any data serialization issues
