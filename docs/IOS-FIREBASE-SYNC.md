# iOS Firebase Sync Documentation

## Current Status: ✅ WORKING

iOS now uses **real Firebase** for daily logging, enabling true cross-platform sync between Android and iOS.

## Implementation

### Architecture

```
iOS DailyLoggingView
    ↓
ModernDailyLoggingViewModel
    ↓
SwiftDailyLogService ✅ Real Firebase!
    ↓
Firebase Firestore
    ↑
Android LogRepositoryImpl
    ↑
Android DailyLoggingScreen
```

### Firebase Structure

```
daily_logs/
  └── {userId}/
      └── logs/
          └── {logId}/
              ├── id: String
              ├── userId: String
              ├── date: String (YYYY-MM-DD)
              ├── periodFlow: String?
              ├── symptoms: [String]
              ├── mood: String?
              ├── bbt: Double?
              ├── cervicalMucus: String?
              ├── opkResult: String?
              ├── notes: String?
              ├── createdAt: Int64
              └── updatedAt: Int64
```

## Key Changes Made

### ModernDailyLoggingViewModel.swift
- Uses `SwiftDailyLogService` for real Firebase operations
- Converts Kotlin enums to/from strings for Firebase storage
- Saves to: `daily_logs/{userId}/logs/{logId}`
- Loads from the same collection Android uses

### DateExtensions.swift
- Added ISO8601 date formatting to match Android format (YYYY-MM-DD)

## Testing Cross-Platform Sync

### Android → iOS
1. Save data on Android
2. Open iOS app and navigate to same date
3. Data should load from Firebase

### iOS → Android
1. Save data on iOS
2. Open Android app and navigate to same date
3. Data should load from Firebase

### Firebase Console Verification
1. Save data on either platform
2. Check Firebase Console → Firestore → `daily_logs` collection
3. Verify data appears correctly

## Current Limitations

### 1. Hardcoded User ID
- Both platforms use `"current_user"` as user ID
- All users share the same data (not production-ready)
- **Action needed:** Implement proper authentication

### 2. No Real-Time Sync
- Changes don't appear immediately on other devices
- Need to close/reopen screen to see updates
- **Future improvement:** Add Firestore listeners

### 3. No Offline Support
- Requires internet connection
- No local caching
- **Future improvement:** Enable offline persistence

## Next Steps for Production

### High Priority: Authentication
Replace hardcoded user ID with real authentication:

```swift
// iOS
let userId = AuthService.shared.currentUserId ?? "anonymous"
```

### Medium Priority: Real-Time Sync
Add Firestore listeners for live updates:

```swift
func observeLog(for date: Date) {
    db.collection("daily_logs")
        .document(userId)
        .collection("logs")
        .whereField("date", isEqualTo: dateString)
        .addSnapshotListener { snapshot, error in
            // Update UI when data changes
        }
}
```

### Medium Priority: Offline Support
Enable Firestore offline persistence:

```swift
let settings = FirestoreSettings()
settings.isPersistenceEnabled = true
db.settings = settings
```

## Historical Context

### Original Problem
iOS was using mock in-memory storage in `FirestoreServiceImpl.ios.kt` instead of real Firebase. This meant:
- No cloud backup of iOS data
- No cross-platform sync
- Data lost on app restart

### Solution Implemented
Created `SwiftDailyLogService.swift` that uses Firebase iOS SDK directly, and modified `ModernDailyLoggingViewModel` to use it instead of the shared Kotlin mock implementation.

### Path Mismatch Bug (Fixed)
Initially iOS and Android used different Firebase paths:
- iOS: `users/{userId}/dailyLogs/{logId}` ❌
- Android: `daily_logs/{userId}/logs/{logId}` ✅

This was fixed by updating iOS to use the same path as Android.

## Verification Checklist

- [x] iOS saves to Firebase
- [x] iOS loads from Firebase
- [x] Android saves to Firebase
- [x] Android loads from Firebase
- [x] Same Firebase collection used by both platforms
- [x] Same data structure
- [x] Same date format (ISO8601)
- [x] Enum conversion working
- [ ] Real user authentication
- [ ] Offline behavior tested
- [ ] Conflict resolution tested

## Success Criteria

✅ iOS → Firebase: Data saved on iOS appears in Firebase Console  
✅ Firebase → iOS: Data in Firebase loads correctly on iOS  
✅ Android → iOS: Data saved on Android loads on iOS  
✅ iOS → Android: Data saved on iOS loads on Android  

**Status:** Cross-platform sync is working. Authentication needed for production.
