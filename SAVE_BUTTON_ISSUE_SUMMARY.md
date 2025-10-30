# Save Button Issue - Root Cause Found

## Problem
When clicking "Save Log" button, it disappears and never comes back, preventing further saves.

## Root Cause
Firebase Firestore `.set()` operation is **hanging indefinitely** on Android emulator, causing the save operation to timeout after 10 seconds. The button doesn't actually disappear - it shows a tiny 16dp spinner while `isSaving = true`, but since the save never completes, `isSaving` never gets reset to `false`.

## Evidence from Logs
```
10-23 18:54:23.008 D FirestoreService.Android: 🔵 About to call firestore.set() with Map
10-23 18:54:33.041 E FirestoreService.Android: SAVE_DAILY_LOG_ERROR - Timed out waiting for 10000 ms
```

The `.set()` call hangs for exactly 10 seconds (our timeout), then fails.

## Why It Happens
1. **READ operations work fine** - `GET_DAILY_LOG_BY_DATE_SUCCESS` shows reads are working
2. **WRITE operations hang** - `.set()` never completes
3. **Not a serialization issue** - We tried both Kotlin data class and plain Map, both hang
4. **Not a permissions issue** - Firestore rules allow writes to `users/{userId}/dailyLogs/{logId}`
5. **Not an authentication issue** - User is authenticated (userId: cBTeC3QpeFUKSgFWTOE9qq3HnWh1)

## Most Likely Causes

### 1. Android Emulator + Firebase Issue
Android emulators sometimes have issues with Firebase writes, especially with:
- Network configuration
- Google Play Services
- Firebase SDK initialization timing

### 2. Firestore Offline Persistence
Firestore might be stuck in offline mode or having issues with offline persistence.

### 3. Network/DNS Issues
The emulator might not be able to reach Firebase servers for writes (but can for reads from cache).

## Solutions to Try

### Solution 1: Test on Real Device
The most reliable way to verify if this is an emulator issue:
```bash
# Connect real Android device via USB
adb devices
adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

### Solution 2: Disable Offline Persistence
Add this to Firebase initialization:
```kotlin
firestore.firestoreSettings = firestoreSettings {
    isPersistenceEnabled = false
}
```

### Solution 3: Check Emulator Network
```bash
# Restart emulator with different network settings
# Or use cold boot instead of quick boot
```

### Solution 4: Use Firebase Emulator Suite
Instead of connecting to production Firebase, use local emulators:
```bash
firebase emulators:start
```

## Current Status
- ✅ DTO deserialization fix applied (added default values)
- ✅ Comprehensive logging added
- ✅ Timeout added to prevent infinite hangs
- ✅ Root cause identified (Firebase write hanging)
- ❌ Save operation still fails on Android emulator
- ❓ Unknown if it works on real device

## Next Steps
1. **Test on real Android device** to confirm it's an emulator issue
2. If real device works: Document as known emulator limitation
3. If real device fails: Investigate Firebase configuration further

## Files Modified
1. `shared/src/commonMain/kotlin/com/eunio/healthapp/data/remote/dto/DailyLogDto.kt` - Added default values
2. `shared/src/androidMain/kotlin/com/eunio/healthapp/data/remote/FirestoreServiceImpl.android.kt` - Added timeout and Map conversion
3. `shared/src/commonMain/kotlin/com/eunio/healthapp/presentation/viewmodel/DailyLoggingViewModel.kt` - Added debug logging
4. `androidApp/proguard-rules.pro` - Added Firebase DTO preservation rules
5. `androidApp/build.gradle.kts` - Added ProGuard configuration
