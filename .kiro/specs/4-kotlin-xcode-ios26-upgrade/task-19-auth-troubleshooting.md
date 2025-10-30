# Firebase Authentication Troubleshooting Guide

## Issue: Cannot Log In with Firebase Account

### Quick Diagnostic Steps

1. **Run the diagnostic script**:
   ```bash
   ./scripts/diagnose-auth-issue.sh
   ```
   This will check your setup and monitor logs in real-time.

2. **Check which platform** you're testing:
   - Android
   - iOS

3. **Note the exact error** you're seeing:
   - Error message displayed in the app
   - Behavior (e.g., button does nothing, loading forever, etc.)

---

## Common Issues and Solutions

### Issue 1: "User Not Found" or "Invalid Credentials"

**Cause**: The test user doesn't exist in Firebase, or credentials are incorrect.

**Solution**:

1. **Check Firebase Console**:
   - Go to [Firebase Console](https://console.firebase.google.com)
   - Select your project
   - Navigate to Authentication → Users
   - Verify the user exists

2. **Create test user manually**:
   - In Firebase Console → Authentication → Users
   - Click "Add User"
   - Email: `demo@eunio.com`
   - Password: `demo123`

3. **Or use the sign-up flow first**:
   - Use the app's sign-up screen to create a new account
   - Then try signing in with those credentials

### Issue 2: Network Error / Connection Failed

**Cause**: Firebase can't connect to the backend.

**Solution**:

1. **Check internet connection**:
   ```bash
   # On Android emulator
   adb shell ping -c 3 8.8.8.8
   ```

2. **Verify Firebase project is active**:
   - Check Firebase Console
   - Ensure project is not disabled or deleted

3. **Check google-services.json**:
   ```bash
   # Verify file exists
   ls -la androidApp/google-services.json
   
   # Check project ID
   grep "project_id" androidApp/google-services.json
   ```

4. **Rebuild the app**:
   ```bash
   ./gradlew clean
   ./gradlew :androidApp:assembleDebug
   ./gradlew :androidApp:installDebug
   ```

### Issue 3: Button Does Nothing / No Response

**Cause**: Possible UI state issue or exception being swallowed.

**Solution**:

1. **Check logcat for errors**:
   ```bash
   adb logcat -s AuthService:* AuthViewModel:* FirebaseAuth:*
   ```

2. **Verify email/password fields are not empty**:
   - The sign-in button is disabled if fields are blank
   - Make sure you've entered both email and password

3. **Check for Koin DI issues**:
   ```bash
   adb logcat | grep -i "koin\|dependency"
   ```

### Issue 4: "Firebase Not Initialized" Error

**Cause**: Firebase SDK not properly initialized.

**Solution**:

1. **Verify EunioApplication is registered in AndroidManifest.xml**:
   ```xml
   <application
       android:name=".EunioApplication"
       ...>
   ```

2. **Check Firebase initialization in EunioApplication.kt**:
   ```kotlin
   FirebaseApp.initializeApp(this)
   ```

3. **Rebuild and reinstall**:
   ```bash
   ./gradlew clean
   ./gradlew :androidApp:installDebug
   ```

### Issue 5: iOS Login Not Working

**Cause**: iOS uses a mock auth service by default.

**Current iOS Implementation**:
The iOS app currently uses `IOSAuthService` which is a mock implementation for development. It stores credentials locally using NSUserDefaults.

**Solution for iOS**:

1. **Use the pre-configured demo user**:
   - Email: `demo@eunio.com`
   - Password: `demo123`
   - This user is pre-populated in the mock service

2. **Or sign up first**:
   - Use the sign-up flow to create a new account
   - The mock service will store it locally
   - Then sign in with those credentials

3. **For production Firebase integration**:
   - The iOS app needs to be updated to use the actual Firebase iOS SDK
   - Currently it's using a mock implementation for development

### Issue 6: "Email Already in Use" During Sign-Up

**Cause**: User already exists in Firebase.

**Solution**:

1. **Use sign-in instead of sign-up**

2. **Or use a different email address**

3. **Or delete the existing user from Firebase Console**:
   - Firebase Console → Authentication → Users
   - Find the user and click the menu → Delete account

### Issue 7: Loading Forever / Hangs

**Cause**: Network timeout or Firebase not responding.

**Solution**:

1. **Check network connectivity**

2. **Check Firebase status**:
   - Visit [Firebase Status Dashboard](https://status.firebase.google.com/)

3. **Increase timeout** (if needed):
   - Edit `shared/src/androidMain/kotlin/com/eunio/healthapp/network/RetryPolicy.kt`
   - Increase timeout values

4. **Check for deadlocks in logs**:
   ```bash
   adb logcat | grep -i "timeout\|deadlock\|anr"
   ```

---

## Platform-Specific Debugging

### Android Debugging

1. **Enable verbose logging**:
   ```bash
   adb shell setprop log.tag.FirebaseAuth VERBOSE
   adb shell setprop log.tag.AuthService VERBOSE
   ```

2. **Monitor all auth-related logs**:
   ```bash
   adb logcat -s AuthService:* AuthViewModel:* FirebaseAuth:* AndroidAuthService:*
   ```

3. **Check Firebase Auth state**:
   ```bash
   adb logcat | grep "FirebaseAuth"
   ```

4. **Verify app permissions**:
   ```bash
   adb shell dumpsys package com.eunio.healthapp.android | grep permission
   ```

### iOS Debugging

1. **Check Xcode console** for error messages

2. **Enable Firebase debug logging**:
   - Edit scheme in Xcode
   - Add argument: `-FIRDebugEnabled`

3. **Check NSUserDefaults** (for mock auth):
   ```swift
   // In Xcode debugger
   po UserDefaults.standard.string(forKey: "currentUserId")
   po UserDefaults.standard.string(forKey: "currentUserEmail")
   ```

4. **Verify Firebase is initialized**:
   - Check `iOSApp.swift` for `FirebaseApp.configure()`

---

## Testing Checklist

Before reporting an issue, verify:

- [ ] Internet connection is working
- [ ] Firebase project is active and accessible
- [ ] google-services.json (Android) or GoogleService-Info.plist (iOS) is present
- [ ] App is built and installed with latest code
- [ ] Test user exists in Firebase Console (for Android)
- [ ] Using correct credentials (email and password)
- [ ] No typos in email or password
- [ ] Checked logcat/Xcode console for error messages

---

## Getting More Help

If the issue persists:

1. **Collect diagnostic information**:
   ```bash
   # Android
   adb logcat -d > auth-logs.txt
   
   # Include:
   # - Platform (Android/iOS)
   # - Error message
   # - Steps to reproduce
   # - Logs from diagnostic script
   ```

2. **Check Firebase Console**:
   - Authentication → Users (verify user exists)
   - Authentication → Sign-in method (verify Email/Password is enabled)
   - Authentication → Settings (check authorized domains)

3. **Verify Firebase configuration**:
   ```bash
   # Check project ID matches
   grep "project_id" androidApp/google-services.json
   grep "project_id" iosApp/iosApp/GoogleService-Info.plist
   ```

---

## Quick Test Commands

```bash
# 1. Clean and rebuild
./gradlew clean
./gradlew :androidApp:assembleDebug

# 2. Install on device
./gradlew :androidApp:installDebug

# 3. Monitor logs
adb logcat -c && adb logcat -s AuthService:* AuthViewModel:* FirebaseAuth:*

# 4. Test sign-in
# (Use the app to sign in with demo@eunio.com / demo123)

# 5. Check if user is authenticated
adb logcat | grep "signed in\|authentication"
```

---

## Expected Behavior

### Successful Sign-In Flow:

1. User enters email and password
2. Taps "Sign In" button
3. Button shows loading indicator
4. After 1-2 seconds:
   - Success: User is redirected to main app
   - Error: Error message is displayed below the form

### Logs for Successful Sign-In:

```
AuthService: Successfully signed in user: demo@eunio.com
FirebaseAuth: User authenticated: [user_id]
AuthViewModel: Sign in successful
```

### Logs for Failed Sign-In:

```
AuthService: Sign in failed with Firebase auth error: ERROR_WRONG_PASSWORD
AuthViewModel: Sign in error: Incorrect password
```

---

## Next Steps

Once login is working:

1. Test sign-out functionality
2. Test sign-up with a new user
3. Test password reset
4. Test cross-platform (create user on Android, sign in on iOS)
5. Proceed to Task 20: Test Firestore data operations
