# Task 19: Quick Test Guide - Firebase Authentication

## Quick Start

### 1. Verify Setup (30 seconds)
```bash
./scripts/verify-firebase-auth.sh
```
Expected: ✅ All checks passed

### 2. Run Automated Tests (2-3 minutes)
```bash
# Shared module tests
./gradlew :shared:test

# Android tests
./gradlew :androidApp:testDebugUnitTest
```

### 3. Manual Testing (15-20 minutes)

#### Android Testing
```bash
# Build and install
./gradlew :androidApp:installDebug

# Launch app on device/emulator
adb shell am start -n com.eunio.healthapp.android/.MainActivity
```

**Test Sequence**:
1. Sign up: `test.android@eunio.com` / `TestPass123!`
2. Sign out
3. Sign in: `demo@eunio.com` / `demo123`
4. Sign out

#### iOS Testing
```bash
# Open in Xcode
open iosApp/iosApp.xcodeproj

# Build and run (Cmd+R)
# Or from command line:
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 15' build
```

**Test Sequence**:
1. Sign up: `test.ios@eunio.com` / `TestPass123!`
2. Sign out
3. Sign in: `demo@eunio.com` / `demo123`
4. Sign out

### 4. Cross-Platform Verification (5 minutes)

**Test 1**: Android → iOS
1. Create user on Android: `cross.test1@eunio.com` / `TestPass123!`
2. Sign in on iOS with same credentials
3. Verify user data syncs

**Test 2**: iOS → Android
1. Create user on iOS: `cross.test2@eunio.com` / `TestPass123!`
2. Sign in on Android with same credentials
3. Verify user data syncs

### 5. Firebase Console Check (2 minutes)

1. Open [Firebase Console](https://console.firebase.google.com)
2. Navigate to Authentication → Users
3. Verify all test users appear:
   - `demo@eunio.com`
   - `test.android@eunio.com`
   - `test.ios@eunio.com`
   - `cross.test1@eunio.com`
   - `cross.test2@eunio.com`

## Expected Results

### All Tests Should Show:
- ✅ Sign-up completes in < 3 seconds
- ✅ Sign-in completes in < 2 seconds
- ✅ Sign-out is instant
- ✅ No crashes or errors
- ✅ Users appear in Firebase Console
- ✅ Cross-platform sign-in works

### Common Issues

**Issue**: "User already exists"
- **Solution**: Use a different email or delete user from Firebase Console

**Issue**: "Network error"
- **Solution**: Check internet connection and Firebase project configuration

**Issue**: "Invalid credentials"
- **Solution**: Verify email/password are correct, check for typos

## Quick Validation Checklist

- [ ] Verification script passes
- [ ] Automated tests pass
- [ ] Android sign-up works
- [ ] Android sign-in works
- [ ] Android sign-out works
- [ ] iOS sign-up works
- [ ] iOS sign-in works
- [ ] iOS sign-out works
- [ ] Cross-platform sign-in works (Android → iOS)
- [ ] Cross-platform sign-in works (iOS → Android)
- [ ] Users visible in Firebase Console
- [ ] No errors in logs

## Time Estimate
- **Total**: 20-30 minutes
- **Automated**: 3-5 minutes
- **Manual**: 15-20 minutes
- **Verification**: 2-5 minutes

## Documentation
- **Full Test Plan**: `task-19-firebase-auth-test-plan.md`
- **Completion Summary**: `task-19-completion-summary.md`
- **Verification Script**: `scripts/verify-firebase-auth.sh`

## Next Task
Once all tests pass, proceed to **Task 20**: Test Firestore data operations on both platforms
