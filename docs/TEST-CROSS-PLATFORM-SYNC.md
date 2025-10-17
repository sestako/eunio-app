# Test Cross-Platform Sync - Quick Guide

## Prerequisites
- Android app built and running
- iOS app built and running  
- Both apps connected to same Firebase project
- Internet connection on both devices

## Test 1: Android → iOS Sync

### On Android:
1. Open the app
2. Navigate to "Daily Log"
3. Select today's date
4. Fill in:
   - Temperature: 98.6
   - Mood: Happy
   - Symptoms: Cramps
5. Click "Save"
6. Wait for "Log saved successfully" snackbar

### On iOS:
1. Open the app
2. Navigate to "Daily Log"
3. Select today's date
4. **Expected Result:** All data from Android should appear

## Test 2: iOS → Android Sync

### On iOS:
1. Open the app
2. Navigate to "Daily Log"
3. Select tomorrow's date
4. Fill in:
   - Temperature: 97.8
   - Mood: Calm
   - Symptoms: Fatigue
5. Click "Save"
6. Wait for "Log saved successfully" message

### On Android:
1. Open the app
2. Navigate to "Daily Log"
3. Select tomorrow's date
4. **Expected Result:** All data from iOS should appear

## Test 3: Firebase Console Verification

1. Go to Firebase Console: https://console.firebase.google.com
2. Select your project
3. Click "Firestore Database"
4. Look for `daily_logs` collection
5. Expand `current_user` → `logs`
6. **Expected:** See both log entries

## Troubleshooting

**If data doesn't appear:**
1. Check internet connection
2. Check Firebase Console to see if data was saved
3. Try closing and reopening the app
4. Check for error messages

**If you see errors:**
- Check that both apps use same Firebase project
- Verify `google-services.json` (Android) and `GoogleService-Info.plist` (iOS) are correct
- Check Firebase Console for any security rule issues

## Success!
If you can see data saved on one platform appearing on the other, cross-platform sync is working! 🎉
