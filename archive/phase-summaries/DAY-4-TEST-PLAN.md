# Day 4: End-to-End Data Flow Testing

**Date:** January 9, 2025  
**Status:** Ready to test

---

## Test Objectives

Verify that data flows correctly from UI → ViewModel → UseCase → Repository → Database/Firestore

---

## Test 1: iOS App - Save Daily Log

### Steps:
1. Build and run iOS app in Xcode
2. Sign in (or create account if needed)
3. Navigate to Daily Logging screen
4. Fill in data:
   - Date: Today
   - Period Flow: MEDIUM
   - Symptoms: Select "Cramps"
   - Mood: Select "Happy"
   - Notes: "Test entry from remediation"
5. Tap "Save"
6. Check console logs

### Expected Console Output:
```
📱 [ViewModel] Saving log for date: 2025-01-09
🔧 [UseCase] Processing log: log_2025-01-09_xxxxx
💾 [Repository] Saving to local database
🗄️  [DAO] Storing log: log_2025-01-09_xxxxx
☁️  [Firestore] Syncing to cloud
✅ Save successful
```

### Success Criteria:
- [ ] No crashes
- [ ] Console shows complete data flow
- [ ] Success message displayed
- [ ] No error messages

---

## Test 2: Verify Data Persistence

### Steps:
1. After saving (Test 1), force quit the app
2. Relaunch the app
3. Sign in again
4. Navigate to Daily Logging
5. Select today's date
6. Verify data is still there

### Expected Result:
- Period Flow: MEDIUM
- Symptoms: Cramps
- Mood: Happy
- Notes: "Test entry from remediation"

### Success Criteria:
- [ ] All data persists after restart
- [ ] No data loss
- [ ] UI displays correct values

---

## Test 3: Calendar Integration

### Steps:
1. After saving (Test 1), navigate to Calendar screen
2. Look at today's date
3. Verify it shows a log indicator
4. Tap on today's date
5. Verify log details appear

### Expected Result:
- Today's date has a visual indicator (dot, color, etc.)
- Tapping shows the log data
- Data matches what was saved

### Success Criteria:
- [ ] Calendar shows log indicator
- [ ] Tapping date shows correct data
- [ ] Navigation works smoothly

---

## Test 4: Android App - Same Tests

### Steps:
Repeat Tests 1-3 on Android:
1. Build and run Android app
2. Perform same save operation
3. Verify persistence
4. Check calendar integration

### Success Criteria:
- [ ] Android app works same as iOS
- [ ] Data saves correctly
- [ ] Data persists
- [ ] Calendar integration works

---

## Test 5: Cross-Platform Sync (If Firebase is configured)

### Steps:
1. Save data on iOS device
2. Wait 10 seconds for sync
3. Open app on Android device (or vice versa)
4. Check if data appears

### Expected Result:
- Data syncs between devices
- Both devices show same data

### Success Criteria:
- [ ] Data appears on second device
- [ ] Sync happens automatically
- [ ] No conflicts

---

## Troubleshooting

### If app crashes on launch:
1. Check console for error messages
2. Verify Firebase is configured (GoogleService files)
3. Check Koin initialization logs

### If save fails:
1. Check console for error messages
2. Verify database is initialized
3. Check repository logs

### If data doesn't persist:
1. Verify database file is created
2. Check DAO implementation
3. Review error logs

### If sync doesn't work:
1. Verify Firebase configuration
2. Check network connectivity
3. Review Firestore rules

---

## Quick Test Commands

### Build iOS:
```bash
cd iosApp
xcodebuild -workspace iosApp.xcworkspace -scheme iosApp -configuration Debug
```

### Build Android:
```bash
cd androidApp
./gradlew assembleDebug
```

### Check logs:
```bash
# iOS
xcrun simctl spawn booted log stream --predicate 'subsystem contains "com.eunio"'

# Android
adb logcat | grep "Eunio"
```

---

## Results Template

```markdown
## Test Results - Day 4

**Date:** [Date]
**Tester:** [Name]

### Test 1: iOS Save
- Status: [ ] Pass / [ ] Fail
- Notes: 

### Test 2: Data Persistence
- Status: [ ] Pass / [ ] Fail
- Notes:

### Test 3: Calendar Integration
- Status: [ ] Pass / [ ] Fail
- Notes:

### Test 4: Android Tests
- Status: [ ] Pass / [ ] Fail
- Notes:

### Test 5: Cross-Platform Sync
- Status: [ ] Pass / [ ] Fail / [ ] Skipped
- Notes:

### Overall Result:
- [ ] All tests passed - Day 4 complete!
- [ ] Some tests failed - needs investigation
- [ ] Blocked - cannot proceed

### Issues Found:
1. 
2. 
3. 

### Next Steps:
1. 
2. 
```

---

## Success Criteria for Day 4

To consider Day 4 complete, we need:
- [ ] iOS app saves data successfully
- [ ] Data persists after app restart
- [ ] Calendar shows logged dates
- [ ] Android app works similarly
- [ ] No critical errors or crashes
- [ ] Console logs show complete data flow

If all criteria met: **Phase 1 is 80% complete!**

