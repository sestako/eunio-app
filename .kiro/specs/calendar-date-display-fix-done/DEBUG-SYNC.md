# Debug Sync Issues

## Quick Debug Steps

### 1. Check Firebase Console

1. Go to https://console.firebase.google.com
2. Select your project
3. Click "Firestore Database"
4. Navigate to: `users/{userId}/dailyLogs`

**What to look for:**
- ✅ Document exists for the date you saved
- ✅ `date` field is a **number** (e.g., `19636`), not a string
- ✅ `createdAt` and `updatedAt` are numbers (seconds)
- ✅ Document ID matches the log ID

**If document doesn't exist:**
- iOS save failed
- Check iOS logs for errors

**If document format is wrong:**
- iOS is still using old format
- Rebuild iOS again

### 2. Check User IDs Match

**iOS:**
```swift
// In iOS logs, look for:
print("User ID: \(Auth.auth().currentUser?.uid)")
```

**Android:**
```kotlin
// In Android logs, look for:
Log.d("Auth", "User ID: ${FirebaseAuth.getInstance().currentUser?.uid}")
```

**They must be identical!**

### 3. Check Android Logs (Logcat)

Look for errors when loading data:

```
// Search for:
- "FirebaseFirestore"
- "getDailyLog"
- "Failed to"
- "Error"
- "Exception"
```

**Common errors:**
- Permission denied → Check Firebase Rules
- Parsing error → Data format mismatch
- Not found → Document doesn't exist

### 4. Test Firebase Query Directly

**In Firebase Console:**
1. Go to Firestore Database
2. Click on `dailyLogs` collection
3. Try to find the document manually
4. Check if it's under the correct user ID

### 5. Check Date Conversion

**iOS saves date as:** Epoch days (e.g., `19636` for Oct 10, 2025)

**Calculate expected epoch days:**
```
October 10, 2025 = Days since Jan 1, 1970
= (2025 - 1970) * 365 + leap years + days in 2025
≈ 19636 days
```

**In Firebase Console:**
- Check if `date` field = `19636` (or close to it)

### 6. Force Refresh on Android

Sometimes Android caches data. Try:
1. Close Android app completely
2. Clear app data (Settings → Apps → Your App → Clear Data)
3. Reopen and login again
4. Navigate to the date

### 7. Check Network Connectivity

**Both devices must be online!**
- iOS needs internet to save to Firebase
- Android needs internet to read from Firebase

### 8. Check Firebase Rules

In Firebase Console → Firestore Database → Rules:

```javascript
// Should allow authenticated users:
match /users/{userId}/dailyLogs/{logId} {
  allow read, write: if request.auth != null && request.auth.uid == userId;
}
```

## Debug Output to Add

### iOS (in saveLog):
```swift
print("🔵 iOS Saving:")
print("  User ID: \(userId)")
print("  Date: \(selectedDate)")
print("  Epoch Days: \(dateStringToEpochDays(selectedDate.toISO8601String()))")
print("  Document Path: users/\(userId)/dailyLogs/\(logData.id)")
```

### Android (in getDailyLog):
```kotlin
Log.d("Sync", "🟢 Android Loading:")
Log.d("Sync", "  User ID: $userId")
Log.d("Sync", "  Date: $date")
Log.d("Sync", "  Epoch Days: ${date.toEpochDays()}")
Log.d("Sync", "  Fetching from Firebase...")
```

## Quick Test

### Test 1: Verify iOS Save
1. iOS: Save log with unique text: "iOS Test [timestamp]"
2. Check Firebase Console immediately
3. Verify document appears with correct format

### Test 2: Verify Android Read
1. Android: Navigate to same date
2. Check Logcat for "Fetching from Firebase..."
3. Check if document is retrieved
4. Check if parsing succeeds

## Common Issues & Solutions

### Issue: Document exists but Android doesn't see it
**Possible causes:**
- Different user IDs
- Android using cached empty data
- Date format mismatch in query

**Solution:**
- Verify user IDs match
- Clear Android app data
- Check date field format in Firebase

### Issue: iOS saves but document has wrong format
**Possible causes:**
- iOS not rebuilt after format fix
- Old code still running

**Solution:**
- Clean build folder in Xcode
- Rebuild completely
- Verify new code is running

### Issue: Permission denied
**Possible causes:**
- Firebase Rules too restrictive
- User not authenticated
- User ID mismatch

**Solution:**
- Check Firebase Rules
- Verify authentication on both platforms
- Check user IDs match

---

**Next Step:** Check Firebase Console first to see if data is there!
