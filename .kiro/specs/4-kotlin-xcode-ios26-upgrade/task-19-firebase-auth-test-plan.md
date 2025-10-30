# Task 19: Firebase Authentication Testing Plan

## Overview
This document provides a comprehensive manual testing plan for Firebase Authentication on both Android and iOS platforms after the Kotlin 2.2.20 and iOS 26 SDK upgrade.

## Test Environment Setup

### Android
- Device/Emulator: Android device or emulator running Android API 26+
- Build: Debug build from `./gradlew :androidApp:assembleDebug`
- Firebase: Connected to Firebase project with Authentication enabled

### iOS
- Device/Simulator: iOS 15+ device or simulator
- Build: Debug build from Xcode 26
- Firebase: Connected to Firebase project with Authentication enabled

## Test Credentials

### Test User 1 (Existing)
- Email: `demo@eunio.com`
- Password: `demo123`
- Purpose: Test sign-in flow with existing account

### Test User 2 (New)
- Email: `test.android@eunio.com`
- Password: `TestPass123!`
- Purpose: Test sign-up flow on Android

### Test User 3 (New)
- Email: `test.ios@eunio.com`
- Password: `TestPass123!`
- Purpose: Test sign-up flow on iOS

## Test Cases

### TC-1: Android Sign-Up Flow

**Objective**: Verify new user registration works on Android

**Steps**:
1. Launch Android app
2. Navigate to sign-up screen
3. Enter email: `test.android@eunio.com`
4. Enter password: `TestPass123!`
5. Enter name: `Android Test User`
6. Tap "Sign Up" button

**Expected Results**:
- ✅ Sign-up completes successfully
- ✅ User is redirected to onboarding or home screen
- ✅ User ID is generated and stored
- ✅ Firebase Console shows new user in Authentication section
- ✅ No errors in Logcat

**Actual Results**:
- [ ] Pass
- [ ] Fail (describe issue): _______________

---

### TC-2: iOS Sign-Up Flow

**Objective**: Verify new user registration works on iOS

**Steps**:
1. Launch iOS app
2. Navigate to sign-up screen
3. Enter email: `test.ios@eunio.com`
4. Enter password: `TestPass123!`
5. Enter name: `iOS Test User`
6. Tap "Sign Up" button

**Expected Results**:
- ✅ Sign-up completes successfully
- ✅ User is redirected to onboarding or home screen
- ✅ User ID is generated and stored
- ✅ Firebase Console shows new user in Authentication section
- ✅ No errors in Xcode console

**Actual Results**:
- [ ] Pass
- [ ] Fail (describe issue): _______________

---

### TC-3: Android Sign-In Flow (Existing User)

**Objective**: Verify existing user can sign in on Android

**Steps**:
1. Launch Android app
2. Navigate to sign-in screen
3. Enter email: `demo@eunio.com`
4. Enter password: `demo123`
5. Tap "Sign In" button

**Expected Results**:
- ✅ Sign-in completes successfully
- ✅ User is redirected to home screen
- ✅ User data is loaded correctly
- ✅ No errors in Logcat

**Actual Results**:
- [ ] Pass
- [ ] Fail (describe issue): _______________

---

### TC-4: iOS Sign-In Flow (Existing User)

**Objective**: Verify existing user can sign in on iOS

**Steps**:
1. Launch iOS app
2. Navigate to sign-in screen
3. Enter email: `demo@eunio.com`
4. Enter password: `demo123`
5. Tap "Sign In" button

**Expected Results**:
- ✅ Sign-in completes successfully
- ✅ User is redirected to home screen
- ✅ User data is loaded correctly
- ✅ No errors in Xcode console

**Actual Results**:
- [ ] Pass
- [ ] Fail (describe issue): _______________

---

### TC-5: Android Sign-In Flow (New User from iOS)

**Objective**: Verify user created on iOS can sign in on Android

**Prerequisites**: Complete TC-2 first

**Steps**:
1. Launch Android app
2. Navigate to sign-in screen
3. Enter email: `test.ios@eunio.com`
4. Enter password: `TestPass123!`
5. Tap "Sign In" button

**Expected Results**:
- ✅ Sign-in completes successfully
- ✅ User is redirected to home screen
- ✅ User data syncs correctly across platforms
- ✅ No errors in Logcat

**Actual Results**:
- [ ] Pass
- [ ] Fail (describe issue): _______________

---

### TC-6: iOS Sign-In Flow (New User from Android)

**Objective**: Verify user created on Android can sign in on iOS

**Prerequisites**: Complete TC-1 first

**Steps**:
1. Launch iOS app
2. Navigate to sign-in screen
3. Enter email: `test.android@eunio.com`
4. Enter password: `TestPass123!`
5. Tap "Sign In" button

**Expected Results**:
- ✅ Sign-in completes successfully
- ✅ User is redirected to home screen
- ✅ User data syncs correctly across platforms
- ✅ No errors in Xcode console

**Actual Results**:
- [ ] Pass
- [ ] Fail (describe issue): _______________

---

### TC-7: Android Sign-Out Flow

**Objective**: Verify user can sign out on Android

**Prerequisites**: User must be signed in

**Steps**:
1. Launch Android app (user already signed in)
2. Navigate to Settings or Profile screen
3. Tap "Sign Out" button
4. Confirm sign out if prompted

**Expected Results**:
- ✅ User is signed out successfully
- ✅ User is redirected to sign-in screen
- ✅ Session is cleared
- ✅ Attempting to access protected screens redirects to auth
- ✅ No errors in Logcat

**Actual Results**:
- [ ] Pass
- [ ] Fail (describe issue): _______________

---

### TC-8: iOS Sign-Out Flow

**Objective**: Verify user can sign out on iOS

**Prerequisites**: User must be signed in

**Steps**:
1. Launch iOS app (user already signed in)
2. Navigate to Settings or Profile screen
3. Tap "Sign Out" button
4. Confirm sign out if prompted

**Expected Results**:
- ✅ User is signed out successfully
- ✅ User is redirected to sign-in screen
- ✅ Session is cleared
- ✅ Attempting to access protected screens redirects to auth
- ✅ No errors in Xcode console

**Actual Results**:
- [ ] Pass
- [ ] Fail (describe issue): _______________

---

### TC-9: Android Invalid Credentials

**Objective**: Verify error handling for invalid credentials on Android

**Steps**:
1. Launch Android app
2. Navigate to sign-in screen
3. Enter email: `test@eunio.com`
4. Enter password: `wrongpassword`
5. Tap "Sign In" button

**Expected Results**:
- ✅ Sign-in fails with appropriate error message
- ✅ Error message is user-friendly (e.g., "Invalid email or password")
- ✅ User remains on sign-in screen
- ✅ No app crash
- ✅ Error is logged in Logcat

**Actual Results**:
- [ ] Pass
- [ ] Fail (describe issue): _______________

---

### TC-10: iOS Invalid Credentials

**Objective**: Verify error handling for invalid credentials on iOS

**Steps**:
1. Launch iOS app
2. Navigate to sign-in screen
3. Enter email: `test@eunio.com`
4. Enter password: `wrongpassword`
5. Tap "Sign In" button

**Expected Results**:
- ✅ Sign-in fails with appropriate error message
- ✅ Error message is user-friendly (e.g., "Invalid email or password")
- ✅ User remains on sign-in screen
- ✅ No app crash
- ✅ Error is logged in Xcode console

**Actual Results**:
- [ ] Pass
- [ ] Fail (describe issue): _______________

---

### TC-11: Android Validation Errors

**Objective**: Verify input validation on Android

**Steps**:
1. Launch Android app
2. Navigate to sign-up screen
3. Test the following scenarios:
   - Empty email field
   - Invalid email format (e.g., "notanemail")
   - Empty password field
   - Short password (less than 6 characters)
   - Empty name field

**Expected Results**:
- ✅ Each validation error shows appropriate message
- ✅ Form submission is blocked until valid
- ✅ Error messages are clear and helpful
- ✅ No app crash

**Actual Results**:
- [ ] Pass
- [ ] Fail (describe issue): _______________

---

### TC-12: iOS Validation Errors

**Objective**: Verify input validation on iOS

**Steps**:
1. Launch iOS app
2. Navigate to sign-up screen
3. Test the following scenarios:
   - Empty email field
   - Invalid email format (e.g., "notanemail")
   - Empty password field
   - Short password (less than 6 characters)
   - Empty name field

**Expected Results**:
- ✅ Each validation error shows appropriate message
- ✅ Form submission is blocked until valid
- ✅ Error messages are clear and helpful
- ✅ No app crash

**Actual Results**:
- [ ] Pass
- [ ] Fail (describe issue): _______________

---

## Firebase Console Verification

After completing all test cases, verify in Firebase Console:

### Authentication Section
- [ ] All test users are visible in the Users list
- [ ] User IDs match what's shown in the app
- [ ] Sign-in timestamps are recent
- [ ] No duplicate users

### Firestore Database
- [ ] User profile documents exist for each test user
- [ ] User data includes correct email and name
- [ ] Timestamps are correct

## Performance Checks

### Android
- [ ] Sign-up completes in < 3 seconds
- [ ] Sign-in completes in < 2 seconds
- [ ] Sign-out is instant
- [ ] No memory leaks (check Android Profiler)

### iOS
- [ ] Sign-up completes in < 3 seconds
- [ ] Sign-in completes in < 2 seconds
- [ ] Sign-out is instant
- [ ] No memory leaks (check Instruments)

## Compatibility Verification

### Android Versions
- [ ] Tested on Android 8.0 (API 26)
- [ ] Tested on Android 12 (API 31)
- [ ] Tested on Android 14 (API 34)

### iOS Versions
- [ ] Tested on iOS 15.0
- [ ] Tested on iOS 17.0
- [ ] Tested on iOS 26.0 (simulator)

## Network Conditions

Test authentication under various network conditions:

### Android
- [ ] WiFi connection
- [ ] Mobile data connection
- [ ] Slow network (use Network Throttling in Android Studio)
- [ ] Offline mode (should show appropriate error)

### iOS
- [ ] WiFi connection
- [ ] Mobile data connection
- [ ] Slow network (use Network Link Conditioner)
- [ ] Offline mode (should show appropriate error)

## Security Checks

- [ ] Passwords are not visible in logs
- [ ] Auth tokens are not exposed in logs
- [ ] User data is encrypted in transit (HTTPS)
- [ ] Session persists across app restarts
- [ ] Session expires appropriately

## Known Issues

Document any issues found during testing:

1. Issue: _______________
   - Platform: Android / iOS
   - Severity: Critical / High / Medium / Low
   - Steps to reproduce: _______________
   - Workaround: _______________

## Test Summary

**Date**: _______________
**Tester**: _______________
**Build Version**: _______________

### Results
- Total Test Cases: 12
- Passed: _____ / 12
- Failed: _____ / 12
- Blocked: _____ / 12

### Overall Status
- [ ] ✅ All tests passed - Firebase Auth is working correctly on both platforms
- [ ] ⚠️ Some tests failed - Issues need to be addressed
- [ ] ❌ Critical failures - Firebase Auth is not working

### Recommendations
_______________

### Sign-off
- [ ] Android authentication verified and approved
- [ ] iOS authentication verified and approved
- [ ] Ready to proceed to next task (Task 20: Test Firestore data operations)

---

## Automated Test Verification

Run the existing automated tests to complement manual testing:

```bash
# Run shared module tests (includes auth tests)
./gradlew :shared:test

# Run Android unit tests
./gradlew :androidApp:testDebugUnitTest

# Run iOS tests (in Xcode)
# Product > Test (Cmd+U)
```

Expected automated test results:
- [ ] AuthManagerTest passes
- [ ] AuthManagerIntegrationTest passes
- [ ] AuthenticationSyncTest passes
- [ ] All other auth-related tests pass
