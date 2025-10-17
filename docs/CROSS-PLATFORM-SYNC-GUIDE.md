# Cross-Platform Data Sync Guide

## How Data Syncs Between Android and iOS

Both Android and iOS use the **same shared Kotlin code** for data management, which means they share the same logic for saving and loading data.

### Architecture

```
Android App                    iOS App
    ↓                             ↓
DailyLoggingViewModel (Shared Kotlin Code)
    ↓                             ↓
LogRepositoryImpl (Shared Kotlin Code)
    ↓                             ↓
    ├─→ Local SQLite Database (Device-specific)
    └─→ Firebase Firestore (Shared Cloud)
```

### Data Flow

1. **When you save on Android:**
   - Saves to local SQLite database on Android device
   - Syncs to Firebase Firestore (cloud)
   - Returns success message

2. **When you open iOS:**
   - Checks local SQLite database on iOS device (empty if first time)
   - Fetches from Firebase Firestore (cloud)
   - Caches locally on iOS device
   - Displays the data

### What You Need to Verify

For cross-platform sync to work, you need to ensure:

#### 1. Same Firebase Project
Both apps must be configured to use the **same Firebase project**:
- Android: `androidApp/google-services.json`
- iOS: `iosApp/GoogleService-Info.plist`

Check that both files have the same `project_id`.

#### 2. Same User ID
Both apps must be logged in as the **same user**. Currently, the code uses:
```kotlin
userId = "current_user" // Hardcoded for testing
```

For real sync, you need to:
- Implement proper authentication
- Use the same user ID on both platforms

#### 3. Network Connection
Both devices need internet connection to sync via Firebase.

### Testing Cross-Platform Sync

1. **Save data on Android:**
   - Open Android app
   - Fill in daily log (temperature, mood, symptoms)
   - Click "Save"
   - Wait for "Log saved successfully" message

2. **Check Firebase Console:**
   - Go to Firebase Console → Firestore Database
   - Look for the `dailyLogs` collection
   - Verify your log entry is there

3. **Load data on iOS:**
   - Open iOS app
   - Navigate to Daily Logging screen
   - Select the same date
   - The data should load from Firebase

### Current Limitations

1. **User ID is hardcoded** - Both apps use `"current_user"` for testing
2. **No real-time sync** - You need to refresh/reopen the screen to see updates
3. **Offline changes** - If you save offline, it syncs when you go back online

### Troubleshooting

**If data doesn't appear on iOS:**

1. Check Firebase Console to confirm data was saved
2. Verify both apps use the same Firebase project
3. Check iOS logs for any sync errors
4. Try pulling down to refresh (if implemented)
5. Restart the iOS app to force a fresh data load

**If you see "current_user" in Firebase:**
This is expected during testing. Once authentication is implemented, you'll see real user IDs.

### Next Steps for Production

To make sync work properly in production:

1. **Implement Authentication:**
   - Use Firebase Auth or your auth system
   - Get real user IDs
   - Update `currentUserId` in `DailyLoggingViewModel`

2. **Add Real-time Sync:**
   - Use Firebase Firestore listeners
   - Update UI when data changes remotely

3. **Add Pull-to-Refresh:**
   - Let users manually trigger sync
   - Show sync status

4. **Handle Conflicts:**
   - What if user edits same log on both devices?
   - Implement conflict resolution strategy
