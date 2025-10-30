# Task 22: Offline Mode and Local Persistence Test Plan

## Overview
This document provides a comprehensive test plan for validating offline mode functionality and local persistence on both Android and iOS platforms.

## Test Environment Setup

### Android Setup
1. Android device or emulator with API 26+
2. Eunio Health App installed
3. Ability to toggle airplane mode or disable network
4. ADB access for log monitoring

### iOS Setup
1. iOS device or simulator with iOS 15.0+
2. Eunio Health App installed
3. Ability to toggle airplane mode or disable network
4. Xcode for log monitoring

## Test Scenarios

### Scenario 1: Save Data Offline on Android

**Objective**: Verify that daily log data can be saved locally when device is offline

**Prerequisites**:
- App is installed and user is logged in
- Device has network connectivity initially

**Steps**:
1. Open the Eunio Health App on Android
2. Navigate to Daily Logging screen
3. Enable Airplane Mode or disable WiFi/Cellular data
4. Enter daily log data:
   - BBT: 98.2°F
   - Period Flow: Medium
   - Symptoms: Cramps
   - Mood: Happy
   - Notes: "Testing offline save"
5. Tap Save button
6. Observe success message
7. Navigate away and return to Daily Logging
8. Verify data is still present

**Expected Results**:
- ✅ Save operation succeeds with success message
- ✅ Data is visible immediately after save
- ✅ Data persists after navigating away and returning
- ✅ Offline indicator is shown in UI
- ✅ Pending sync indicator is visible

**Verification Commands** (Android):
```bash
# Monitor logs for offline save
adb logcat | grep -i "offline\|sync\|dailylog"

# Check local database
adb shell run-as com.eunio.healthapp ls -la /data/data/com.eunio.healthapp/databases/
```

---

### Scenario 2: Save Data Offline on iOS

**Objective**: Verify that daily log data can be saved locally when device is offline

**Prerequisites**:
- App is installed and user is logged in
- Device has network connectivity initially

**Steps**:
1. Open the Eunio Health App on iOS
2. Navigate to Daily Logging screen
3. Enable Airplane Mode or disable WiFi/Cellular data
4. Enter daily log data:
   - BBT: 98.2°F
   - Period Flow: Medium
   - Symptoms: Cramps
   - Mood: Happy
   - Notes: "Testing offline save on iOS"
5. Tap Save button
6. Observe success message
7. Navigate away and return to Daily Logging
8. Verify data is still present

**Expected Results**:
- ✅ Save operation succeeds with success message
- ✅ Data is visible immediately after save
- ✅ Data persists after navigating away and returning
- ✅ Offline indicator is shown in UI
- ✅ Pending sync indicator is visible

**Verification** (iOS):
- Check Xcode console for offline save logs
- Verify no network errors are shown to user

---

### Scenario 3: Data Persists After App Restart (Android)

**Objective**: Verify that offline data persists after app is closed and reopened

**Prerequisites**:
- Scenario 1 completed successfully
- Device still offline

**Steps**:
1. With device still in Airplane Mode
2. Force close the Eunio Health App
3. Wait 5 seconds
4. Reopen the app
5. Navigate to Daily Logging screen
6. Verify previously saved data is still present

**Expected Results**:
- ✅ App opens successfully while offline
- ✅ Previously saved data is visible
- ✅ All data fields match what was saved
- ✅ Offline indicator still shown
- ✅ Pending sync indicator still visible

---

### Scenario 4: Data Persists After App Restart (iOS)

**Objective**: Verify that offline data persists after app is closed and reopened

**Prerequisites**:
- Scenario 2 completed successfully
- Device still offline

**Steps**:
1. With device still in Airplane Mode
2. Force close the Eunio Health App (swipe up from app switcher)
3. Wait 5 seconds
4. Reopen the app
5. Navigate to Daily Logging screen
6. Verify previously saved data is still present

**Expected Results**:
- ✅ App opens successfully while offline
- ✅ Previously saved data is visible
- ✅ All data fields match what was saved
- ✅ Offline indicator still shown
- ✅ Pending sync indicator still visible

---

### Scenario 5: Sync After Coming Back Online (Android)

**Objective**: Verify that offline data syncs to Firebase when connectivity is restored

**Prerequisites**:
- Scenarios 1 and 3 completed successfully
- Device still offline with pending data

**Steps**:
1. Note the current pending sync count (if visible in UI)
2. Disable Airplane Mode or re-enable WiFi/Cellular data
3. Wait for network connection to establish
4. Observe sync indicator/progress
5. Wait for sync to complete (should be automatic)
6. Verify offline indicator disappears
7. Verify pending sync indicator disappears
8. Open Firebase Console and verify data is present

**Expected Results**:
- ✅ Network connection is detected automatically
- ✅ Sync starts automatically within 5 seconds
- ✅ Sync completes successfully
- ✅ Offline indicator disappears
- ✅ Pending sync indicator disappears
- ✅ Data is visible in Firebase Console
- ✅ No error messages shown

**Verification Commands** (Android):
```bash
# Monitor sync process
adb logcat | grep -i "sync\|firebase\|firestore"

# Check for sync errors
adb logcat | grep -E "ERROR|Exception" | grep -i sync
```

---

### Scenario 6: Sync After Coming Back Online (iOS)

**Objective**: Verify that offline data syncs to Firebase when connectivity is restored

**Prerequisites**:
- Scenarios 2 and 4 completed successfully
- Device still offline with pending data

**Steps**:
1. Note the current pending sync count (if visible in UI)
2. Disable Airplane Mode or re-enable WiFi/Cellular data
3. Wait for network connection to establish
4. Observe sync indicator/progress
5. Wait for sync to complete (should be automatic)
6. Verify offline indicator disappears
7. Verify pending sync indicator disappears
8. Open Firebase Console and verify data is present

**Expected Results**:
- ✅ Network connection is detected automatically
- ✅ Sync starts automatically within 5 seconds
- ✅ Sync completes successfully
- ✅ Offline indicator disappears
- ✅ Pending sync indicator disappears
- ✅ Data is visible in Firebase Console
- ✅ No error messages shown

**Verification** (iOS):
- Check Xcode console for sync completion logs
- Verify Firebase Analytics events for sync success

---

### Scenario 7: Multiple Offline Entries (Android)

**Objective**: Verify that multiple daily logs can be saved offline and synced correctly

**Prerequisites**:
- Fresh app state or cleared previous test data
- Device has network connectivity initially

**Steps**:
1. Open the Eunio Health App on Android
2. Enable Airplane Mode
3. Create 5 daily logs for different dates:
   - Day 1: BBT 97.8°F, Period Flow: Heavy
   - Day 2: BBT 98.0°F, Period Flow: Medium
   - Day 3: BBT 98.2°F, Period Flow: Light
   - Day 4: BBT 98.4°F, Symptoms: Headache
   - Day 5: BBT 98.6°F, Mood: Happy
4. Verify all 5 logs are saved locally
5. Disable Airplane Mode
6. Wait for automatic sync
7. Verify all 5 logs appear in Firebase Console

**Expected Results**:
- ✅ All 5 logs save successfully while offline
- ✅ All 5 logs are visible in local storage
- ✅ Pending sync count shows 5 operations
- ✅ All 5 logs sync successfully when online
- ✅ All 5 logs appear in Firebase with correct data
- ✅ Sync completes within 30 seconds

---

### Scenario 8: Multiple Offline Entries (iOS)

**Objective**: Verify that multiple daily logs can be saved offline and synced correctly

**Prerequisites**:
- Fresh app state or cleared previous test data
- Device has network connectivity initially

**Steps**:
1. Open the Eunio Health App on iOS
2. Enable Airplane Mode
3. Create 5 daily logs for different dates:
   - Day 1: BBT 97.8°F, Period Flow: Heavy
   - Day 2: BBT 98.0°F, Period Flow: Medium
   - Day 3: BBT 98.2°F, Period Flow: Light
   - Day 4: BBT 98.4°F, Symptoms: Headache
   - Day 5: BBT 98.6°F, Mood: Happy
4. Verify all 5 logs are saved locally
5. Disable Airplane Mode
6. Wait for automatic sync
7. Verify all 5 logs appear in Firebase Console

**Expected Results**:
- ✅ All 5 logs save successfully while offline
- ✅ All 5 logs are visible in local storage
- ✅ Pending sync count shows 5 operations
- ✅ All 5 logs sync successfully when online
- ✅ All 5 logs appear in Firebase with correct data
- ✅ Sync completes within 30 seconds

---

### Scenario 9: Offline Mode Works Correctly (Android)

**Objective**: Verify that offline mode indicator and feature availability are correct

**Prerequisites**:
- App is installed and user is logged in

**Steps**:
1. Open the Eunio Health App on Android
2. Verify app is online (no offline indicator)
3. Enable Airplane Mode
4. Observe offline indicator appears
5. Test available features:
   - Daily Logging: Should work ✅
   - Calendar View: Should work ✅
   - Settings: Should work ✅
   - Insights: May show cached data ⚠️
   - Reports: May be limited ⚠️
6. Disable Airplane Mode
7. Verify offline indicator disappears

**Expected Results**:
- ✅ Offline indicator appears within 2 seconds of going offline
- ✅ Core features (logging, calendar, settings) remain functional
- ✅ Limited features show appropriate warnings
- ✅ Offline indicator disappears within 2 seconds of coming online
- ✅ No crashes or errors during offline mode

---

### Scenario 10: Offline Mode Works Correctly (iOS)

**Objective**: Verify that offline mode indicator and feature availability are correct

**Prerequisites**:
- App is installed and user is logged in

**Steps**:
1. Open the Eunio Health App on iOS
2. Verify app is online (no offline indicator)
3. Enable Airplane Mode
4. Observe offline indicator appears
5. Test available features:
   - Daily Logging: Should work ✅
   - Calendar View: Should work ✅
   - Settings: Should work ✅
   - Insights: May show cached data ⚠️
   - Reports: May be limited ⚠️
6. Disable Airplane Mode
7. Verify offline indicator disappears

**Expected Results**:
- ✅ Offline indicator appears within 2 seconds of going offline
- ✅ Core features (logging, calendar, settings) remain functional
- ✅ Limited features show appropriate warnings
- ✅ Offline indicator disappears within 2 seconds of coming online
- ✅ No crashes or errors during offline mode

---

## Cross-Platform Verification

### Scenario 11: Cross-Platform Data Consistency

**Objective**: Verify that offline data from one platform syncs and appears on the other platform

**Prerequisites**:
- Both Android and iOS devices with app installed
- Same user account logged in on both devices
- Both devices online initially

**Steps**:
1. On Android device:
   - Enable Airplane Mode
   - Create daily log for today with BBT 98.2°F
   - Verify saved locally
2. On iOS device:
   - Verify today's log is NOT yet visible (Android is offline)
3. On Android device:
   - Disable Airplane Mode
   - Wait for sync to complete
4. On iOS device:
   - Pull to refresh or wait for automatic sync
   - Verify today's log now appears with correct data

**Expected Results**:
- ✅ Android saves data offline successfully
- ✅ iOS doesn't show data until Android syncs
- ✅ After Android syncs, data appears on iOS
- ✅ Data on iOS matches what was entered on Android
- ✅ No data loss or corruption

---

## Test Execution Checklist

### Pre-Test Setup
- [ ] Android device/emulator ready with app installed
- [ ] iOS device/simulator ready with app installed
- [ ] Test user account created and logged in on both platforms
- [ ] Firebase Console access available
- [ ] ADB and Xcode console access configured
- [ ] Network connectivity can be toggled on both devices

### Android Tests
- [ ] Scenario 1: Save Data Offline on Android
- [ ] Scenario 3: Data Persists After App Restart (Android)
- [ ] Scenario 5: Sync After Coming Back Online (Android)
- [ ] Scenario 7: Multiple Offline Entries (Android)
- [ ] Scenario 9: Offline Mode Works Correctly (Android)

### iOS Tests
- [ ] Scenario 2: Save Data Offline on iOS
- [ ] Scenario 4: Data Persists After App Restart (iOS)
- [ ] Scenario 6: Sync After Coming Back Online (iOS)
- [ ] Scenario 8: Multiple Offline Entries (iOS)
- [ ] Scenario 10: Offline Mode Works Correctly (iOS)

### Cross-Platform Tests
- [ ] Scenario 11: Cross-Platform Data Consistency

### Post-Test Verification
- [ ] All test scenarios passed
- [ ] No crashes or errors observed
- [ ] Firebase data matches local data
- [ ] Performance is acceptable (sync < 30 seconds)
- [ ] User experience is smooth and intuitive

---

## Known Issues and Limitations

### Current Implementation Status
- ✅ Local persistence using SQLDelight
- ✅ Offline-first architecture in LogRepository
- ✅ Automatic sync when connectivity restored
- ✅ Conflict resolution (last-write-wins)
- ✅ Retry logic with exponential backoff
- ✅ Network connectivity monitoring

### Potential Issues to Watch For
- ⚠️ Large amounts of offline data may take longer to sync
- ⚠️ Intermittent network may cause partial sync failures
- ⚠️ Storage limits on device may affect offline capacity
- ⚠️ Background sync may be limited by OS restrictions

---

## Success Criteria

Task 22 is considered complete when:

1. ✅ All Android test scenarios pass (Scenarios 1, 3, 5, 7, 9)
2. ✅ All iOS test scenarios pass (Scenarios 2, 4, 6, 8, 10)
3. ✅ Cross-platform consistency verified (Scenario 11)
4. ✅ No data loss occurs during offline/online transitions
5. ✅ Sync completes successfully within acceptable timeframe
6. ✅ User experience is smooth with appropriate feedback
7. ✅ No crashes or critical errors observed

---

## Requirements Mapping

This test plan validates the following requirements:

- **Requirement 6.6**: Offline mode and local persistence
  - Verified by: Scenarios 1-4, 9-10
  
- **Requirement 7.6**: Testing offline mode
  - Verified by: All scenarios
  
- **Requirement 7.4**: Cross-platform sync
  - Verified by: Scenario 11

---

## Test Execution Notes

### Date: _____________
### Tester: _____________
### Android Device: _____________
### iOS Device: _____________

### Results Summary:
- Total Scenarios: 11
- Passed: _____
- Failed: _____
- Blocked: _____

### Issues Found:
1. _____________________________________________
2. _____________________________________________
3. _____________________________________________

### Additional Notes:
_____________________________________________
_____________________________________________
_____________________________________________
