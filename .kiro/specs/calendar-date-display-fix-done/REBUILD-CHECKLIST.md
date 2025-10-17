# Rebuild Checklist for Sync Fix

## What Needs to be Rebuilt

Both iOS and Android need to be rebuilt because we changed:
1. **Shared module** (`LogRepositoryImpl.kt`) - Used by both platforms
2. **iOS** (`SwiftDailyLogService.swift`) - iOS-specific
3. **iOS ViewModel** (`ModernDailyLoggingViewModel.swift`) - iOS-specific

## Rebuild Steps

### 1. Clean and Rebuild Shared Module
```bash
cd shared
./gradlew clean build
```

### 2. Rebuild iOS
```bash
# In Xcode:
Product → Clean Build Folder (Cmd+Shift+K)
Product → Build (Cmd+B)
```

### 3. Rebuild Android
```bash
cd androidApp
./gradlew clean assembleDebug
```

## Verification Steps

### After Rebuilding Both Apps:

1. **Clear old data** (optional but recommended):
   - Delete both apps from devices
   - Reinstall fresh

2. **Test iOS → Android:**
   - iOS: Save "Test 1" on Oct 10, 2025
   - Android: Open Oct 10, 2025
   - Expected: See "Test 1"

3. **Test Android → iOS:**
   - Android: Save "Test 2" on Oct 11, 2025
   - iOS: Open Oct 11, 2025
   - Expected: See "Test 2"

## If Still Not Working

Check Firebase Console:
1. Go to Firestore Database
2. Navigate to: `users/{userId}/dailyLogs`
3. Check if documents exist
4. Verify document format matches:
   ```json
   {
     "date": 19636,  // Long (epoch days)
     "createdAt": 1760201686,  // Long (seconds)
     "updatedAt": 1760201686,  // Long (seconds)
     "notes": "Test"
   }
   ```

## Common Issues

### Issue: Android still shows old data
**Solution:** Android might be using cached data
- Clear app data on Android
- Or wait a few seconds and refresh

### Issue: Different user IDs
**Solution:** Make sure both apps are logged in with same account
- Check user ID in Firebase Console
- Verify Auth.currentUser.uid matches on both

### Issue: Firebase Rules blocking
**Solution:** Check Firebase Console for errors
- Look for permission denied errors
- Verify rules allow authenticated users

---

**Status:** Rebuild checklist  
**Action:** Rebuild both iOS and Android  
**Test:** After rebuild, test sync both directions  
