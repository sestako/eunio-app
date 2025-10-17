# Manual Test Checklist - Day 4

**Date:** January 9, 2025  
**Tester:** ___________  
**Device/Simulator:** ___________

---

## Pre-Test Setup

- [ ] Open `iosApp.xcodeproj` in Xcode
- [ ] Select a simulator (iPhone 15 Pro recommended)
- [ ] Build and run the app (⌘R)
- [ ] Open Console in Xcode (⌘⇧C) to see logs

---

## Test 1: App Launch ✅

**Goal:** Verify app launches without crashes

### Steps:
1. App should launch
2. Check for Koin initialization logs in console

### Expected Console Output:
```
🔥 AppDelegate: Starting Firebase initialization...
🔥 AppDelegate: Firebase.configure() called
✅ Koin initialized successfully
```

### Result:
- [ ] ✅ App launched successfully
- [ ] ✅ No crashes
- [ ] ✅ Koin initialization logs visible
- [ ] ❌ Failed - describe issue: ___________

---

## Test 2: Authentication 🔐

**Goal:** Sign in or create account

### Steps:
1. If you see sign-in screen, either:
   - Sign in with existing account
   - OR create new account
2. Wait for authentication to complete

### Result:
- [ ] ✅ Successfully signed in
- [ ] ✅ Navigated to main app
- [ ] ❌ Failed - describe issue: ___________

---

## Test 3: Navigate to Daily Logging 📝

**Goal:** Access the daily logging screen

### Steps:
1. Look for "Daily Logging" or "Log" tab/button
2. Tap to navigate
3. Should see today's date

### Result:
- [ ] ✅ Found daily logging screen
- [ ] ✅ Today's date is selected
- [ ] ✅ Form is visible
- [ ] ❌ Failed - describe issue: ___________

---

## Test 4: Save Daily Log 💾

**Goal:** Save health data and verify it works

### Steps:
1. Fill in the form:
   - **Period Flow:** Select "Medium"
   - **Symptoms:** Select "Cramps"
   - **Mood:** Select "Happy"
   - **Notes:** Type "Test from Day 4"
2. Tap "Save" button
3. Watch console for logs

### Expected Console Output:
```
📱 [ViewModel] Saving log...
🔧 [UseCase] Processing log...
💾 [Repository] Saving to database...
🗄️  [DAO] Inserting log...
☁️  [Firestore] Syncing to cloud...
✅ Save successful
```

### Result:
- [ ] ✅ Save button worked
- [ ] ✅ Success message shown
- [ ] ✅ Console shows data flow
- [ ] ✅ No error messages
- [ ] ❌ Failed - describe issue: ___________

**Console logs (copy relevant lines):**
```
[Paste console output here]
```

---

## Test 5: Verify Data Persistence 🔄

**Goal:** Ensure data survives app restart

### Steps:
1. After saving (Test 4), **force quit the app**
   - Swipe up from bottom (or double-click home)
   - Swipe app away
2. **Relaunch the app** from Xcode (⌘R)
3. Sign in again if needed
4. Navigate to Daily Logging
5. Select today's date
6. Check if data is still there

### Expected Result:
- Period Flow: Medium
- Symptoms: Cramps
- Mood: Happy
- Notes: "Test from Day 4"

### Result:
- [ ] ✅ All data persisted
- [ ] ✅ Period Flow: Medium
- [ ] ✅ Symptoms: Cramps
- [ ] ✅ Mood: Happy
- [ ] ✅ Notes: "Test from Day 4"
- [ ] ❌ Data lost - describe what's missing: ___________

---

## Test 6: Calendar Integration 📅

**Goal:** Verify calendar shows logged dates

### Steps:
1. Navigate to Calendar screen
2. Look at today's date
3. Should have some indicator (dot, color, etc.)
4. Tap on today's date
5. Should show the log data

### Result:
- [ ] ✅ Calendar screen accessible
- [ ] ✅ Today's date has indicator
- [ ] ✅ Tapping shows log data
- [ ] ✅ Data matches what was saved
- [ ] ❌ Failed - describe issue: ___________

---

## Test 7: Update Existing Log ✏️

**Goal:** Verify updates work

### Steps:
1. Go back to Daily Logging
2. Select today's date (should load existing log)
3. Change something (e.g., add "Bloating" symptom)
4. Save again
5. Force quit and relaunch
6. Verify change persisted

### Result:
- [ ] ✅ Update saved successfully
- [ ] ✅ Change persisted after restart
- [ ] ❌ Failed - describe issue: ___________

---

## Test 8: Multiple Days 📆

**Goal:** Test with multiple log entries

### Steps:
1. Navigate to Daily Logging
2. Select yesterday's date
3. Add different data
4. Save
5. Select day before yesterday
6. Add different data
7. Save
8. Go to Calendar
9. Verify all 3 days show indicators

### Result:
- [ ] ✅ Can log multiple days
- [ ] ✅ Each day saves independently
- [ ] ✅ Calendar shows all logged days
- [ ] ❌ Failed - describe issue: ___________

---

## Test 9: Error Handling 🚨

**Goal:** Verify error handling works

### Steps:
1. Try to save invalid data (if possible)
   - Future date?
   - Invalid BBT value?
2. Check if error message appears

### Result:
- [ ] ✅ Validation works
- [ ] ✅ Error messages shown
- [ ] ⚠️  Couldn't test - no validation found
- [ ] ❌ Failed - describe issue: ___________

---

## Test 10: Console Log Analysis 🔍

**Goal:** Verify complete data flow in logs

### Check console for these patterns:
- [ ] ✅ Koin initialization logs
- [ ] ✅ ViewModel logs
- [ ] ✅ UseCase logs
- [ ] ✅ Repository logs
- [ ] ✅ DAO/Database logs
- [ ] ✅ Firestore logs (if online)
- [ ] ❌ Missing logs - which ones: ___________

---

## Overall Test Results

### Summary:
- **Tests Passed:** ___/10
- **Tests Failed:** ___/10
- **Tests Skipped:** ___/10

### Critical Issues Found:
1. ___________
2. ___________
3. ___________

### Minor Issues Found:
1. ___________
2. ___________
3. ___________

### Observations:
___________
___________
___________

---

## Conclusion

### Day 4 Status:
- [ ] ✅ **PASS** - All critical tests passed, data flow works!
- [ ] ⚠️  **PARTIAL** - Most tests passed, minor issues found
- [ ] ❌ **FAIL** - Critical issues blocking functionality

### Next Steps:
- [ ] If PASS: Move to Day 5 (validation) and Phase 1 complete!
- [ ] If PARTIAL: Document issues, fix minor problems
- [ ] If FAIL: Investigate critical blockers, create fix plan

### Actual Functionality Estimate:
Based on testing, the app is approximately **____%** functional.

---

## Notes for Developer

**What worked well:**
___________
___________

**What needs improvement:**
___________
___________

**Surprises (good or bad):**
___________
___________

**Time spent testing:** _____ minutes

---

**Tester Signature:** ___________  
**Date Completed:** ___________
