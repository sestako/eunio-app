# Build and Test Guide - Eunio Health App

**Last Updated:** January 9, 2025

---

## Prerequisites

### Required Tools
- **Xcode** 15.0+ (for iOS)
- **Android Studio** Hedgehog or later (for Android)
- **JDK** 17 or later
- **Kotlin** 1.9.0+
- **CocoaPods** (for iOS dependencies)

### Firebase Setup
- `google-services.json` in `androidApp/`
- `GoogleService-Info.plist` in `iosApp/iosApp/`

---

## Building iOS App

### Method 1: Xcode (Recommended)
```bash
# Open the iOS project
open iosApp/iosApp.xcodeproj

# Or if using workspace
open iosApp/iosApp.xcworkspace
```

Then in Xcode:
1. Select a simulator or device
2. Press `Cmd + R` to build and run

### Method 2: Command Line
```bash
# Build the iOS app
cd iosApp
xcodebuild -project iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Debug \
  -destination 'platform=iOS Simulator,name=iPhone 15' \
  build

# Or use our verified command
xcodebuild -project iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Debug \
  -sdk iphonesimulator \
  -arch arm64 \
  build
```

### Testing iOS App
1. Launch the app in simulator
2. App will start with simplified auth (bypassed for testing)
3. Navigate to "Daily Log" tab
4. Test data entry:
   - Select a date
   - Enter period flow
   - Add symptoms
   - Enter BBT
   - Add notes
   - Tap "Save"
5. Verify data persists by:
   - Closing the app
   - Reopening
   - Checking the same date

---

## Building Android App

### Method 1: Android Studio (Recommended)
```bash
# Open the project
open -a "Android Studio" .
```

Then in Android Studio:
1. Wait for Gradle sync to complete
2. Select a device/emulator
3. Click the "Run" button or press `Ctrl + R`

### Method 2: Command Line
```bash
# Build debug APK
./gradlew :androidApp:assembleDebug

# Install on connected device/emulator
./gradlew :androidApp:installDebug

# Build and install in one command
./gradlew :androidApp:installDebug
```

### Testing Android App
1. Launch the app
2. Complete onboarding flow (or sign in)
3. From main screen, tap "🧪 Test Daily Log"
4. Test data entry (same as iOS)
5. Verify data persistence

---

## Testing Data Flow

### Complete Data Flow Test

#### iOS
```
1. Open app → Daily Log tab
2. Select today's date
3. Enter data:
   - Period Flow: Medium
   - Symptoms: Cramps, Headache
   - Mood: Happy
   - BBT: 98.2
   - Notes: "Feeling good"
4. Tap "Save"
5. See success message
6. Force quit app (swipe up from app switcher)
7. Reopen app
8. Navigate to same date
9. Verify all data is still there ✅
```

#### Android
```
1. Open app → Tap "🧪 Test Daily Log"
2. Select today's date
3. Enter same data as iOS test
4. Tap "Save"
5. See success message
6. Press back → Tap "🧪 Test Daily Log" again
7. Verify data persists ✅
```

---

## Database Verification

### iOS - Check SQLite Database
```bash
# Find the app's data directory
xcrun simctl get_app_container booted com.eunio.healthapp data

# Navigate to database
cd "$(xcrun simctl get_app_container booted com.eunio.healthapp data)/Documents"

# Open database
sqlite3 eunio_health.db

# Query daily logs
SELECT * FROM DailyLog;

# Exit
.quit
```

### Android - Check SQLite Database
```bash
# Using adb
adb shell

# Navigate to app data
cd /data/data/com.eunio.healthapp.android/databases/

# List databases
ls -la

# Pull database to computer
exit
adb pull /data/data/com.eunio.healthapp.android/databases/eunio_health.db

# Open with sqlite3
sqlite3 eunio_health.db
SELECT * FROM DailyLog;
```

---

## Common Issues and Solutions

### iOS Issues

#### Issue: "No such module 'shared'"
**Solution:**
```bash
cd iosApp
pod install
# Then rebuild in Xcode
```

#### Issue: Database migration error
**Solution:** Already fixed in `DatabaseMigrations.kt` with existence checks

#### Issue: Black screen on launch
**Solution:** Already fixed - simplified auth flow in `iOSApp.swift`

#### Issue: Threading error on save
**Solution:** Already fixed - added `@MainActor` to ViewModel

### Android Issues

#### Issue: Gradle sync fails
**Solution:**
```bash
./gradlew clean
./gradlew build --refresh-dependencies
```

#### Issue: "Unresolved reference: koin"
**Solution:**
```bash
# Rebuild shared module
./gradlew :shared:build
```

#### Issue: Firebase not initialized
**Solution:**
- Verify `google-services.json` exists in `androidApp/`
- Check `EunioApplication.kt` initializes Firebase

---

## Testing Checklist

### ✅ iOS Testing
- [ ] App launches without crashes
- [ ] Daily Log screen loads
- [ ] Can select dates
- [ ] Can enter all health metrics
- [ ] Save button works
- [ ] Data persists after app restart
- [ ] No threading errors
- [ ] Database migrations work

### ✅ Android Testing
- [ ] App launches without crashes
- [ ] Onboarding flow works
- [ ] Can access test screens
- [ ] Daily Log screen loads
- [ ] Can enter all health metrics
- [ ] Save button works
- [ ] Data persists
- [ ] Accessibility works (TalkBack)

### ✅ Cross-Platform Testing
- [ ] Same data structure on both platforms
- [ ] Database schema matches
- [ ] ViewModels work on both platforms
- [ ] Koin DI works on both platforms

---

## Performance Testing

### iOS Performance
```bash
# Run with Instruments
# In Xcode: Product → Profile (Cmd + I)
# Select "Time Profiler" or "Leaks"
```

### Android Performance
```bash
# Enable profiling in Android Studio
# Run → Profile 'androidApp'
# Check CPU, Memory, Network usage
```

---

## Debugging Tips

### iOS Debugging
```swift
// Add breakpoints in Swift code
// Use lldb commands:
po viewModel.uiState.value
po dailyLog

// Check console for logs
print("Debug: \(value)")
```

### Android Debugging
```kotlin
// Add breakpoints in Kotlin code
// Use logcat:
Log.d("DailyLog", "Saving: $dailyLog")

// Or use println for shared code
println("Debug: $value")
```

### Shared Code Debugging
```kotlin
// In shared module, use println
println("ViewModel: Saving daily log for date: $date")

// Check logs in:
// iOS: Xcode console
// Android: Logcat
```

---

## Next Steps After Successful Build

1. **Verify Core Functionality**
   - Test daily logging on both platforms
   - Verify data persistence
   - Check database integrity

2. **Test Edge Cases**
   - Invalid BBT values
   - Empty form submission
   - Date navigation boundaries
   - Offline mode

3. **Accessibility Testing**
   - iOS: Enable VoiceOver
   - Android: Enable TalkBack
   - Navigate through app using only screen reader

4. **Performance Testing**
   - Large datasets (100+ daily logs)
   - Rapid date navigation
   - Memory usage
   - Battery impact

5. **Integration Testing**
   - Firebase authentication
   - Analytics events
   - Crashlytics reporting
   - Network sync

---

## Success Criteria

### ✅ iOS App is Working When:
- Builds without errors
- Launches without crashes
- Daily Log screen functional
- Data saves and loads correctly
- No threading errors
- Database migrations work

### ✅ Android App is Working When:
- Builds without errors
- Launches without crashes
- Onboarding flow completes
- Daily Log screen functional
- Data saves and loads correctly
- Accessibility works properly

### ✅ Both Platforms Working When:
- Same data appears on both platforms
- Shared ViewModels work correctly
- Database schema is consistent
- Koin DI works on both platforms
- No platform-specific crashes

---

## Support

If you encounter issues:

1. Check this guide first
2. Review error messages carefully
3. Check the relevant platform's console/logcat
4. Verify Firebase configuration
5. Ensure all dependencies are installed
6. Try cleaning and rebuilding

**Current Status:** Both platforms are functional and ready for testing! 🎉
