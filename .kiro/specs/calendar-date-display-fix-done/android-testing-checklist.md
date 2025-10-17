# Android Calendar Testing Checklist
## Quick Reference for Task 3 Verification

### Test Date Display on October 10, 2025

#### Visual Verification
- [ ] Open Daily Logging screen
- [ ] Verify main date shows: **2025-10-10**
- [ ] Verify quick selection shows: **7, 8, 9, 10, 11, 12, 13**
- [ ] Verify month abbreviation: **Oct** (not Jan)
- [ ] Verify date 10 is visually highlighted
- [ ] Verify NO January dates are displayed

**Status:** _______________  
**Notes:** _______________

---

### Test Previous/Next Day Navigation

#### Previous Day Navigation
- [ ] Click previous day button (←)
- [ ] Verify date changes to: **2025-10-09**
- [ ] Verify quick selection updates: **6, 7, 8, 9, 10, 11, 12**
- [ ] Verify month still shows: **Oct**

#### Next Day Navigation
- [ ] Click next day button (→) twice
- [ ] Verify date changes to: **2025-10-11**
- [ ] Verify quick selection updates: **8, 9, 10, 11, 12, 13, 14**
- [ ] Verify month still shows: **Oct**

#### Multiple Navigation
- [ ] Click previous 5 times from Oct 10
- [ ] Verify date is: **2025-10-05**
- [ ] Click next 10 times
- [ ] Verify date is: **2025-10-15**
- [ ] Verify October dates still showing (not January)

**Status:** _______________  
**Notes:** _______________

---

### Test Month Boundary Scenarios

#### October 1st Boundary
- [ ] Navigate to October 1, 2025
- [ ] Verify date shows: **2025-10-01**
- [ ] Verify quick selection spans: **Sep 28, 29, 30, Oct 1, 2, 3, 4**
- [ ] Verify both **Sep** and **Oct** are visible
- [ ] Verify dates are correctly labeled

#### October 31st Boundary
- [ ] Navigate to October 31, 2025
- [ ] Verify date shows: **2025-10-31**
- [ ] Verify quick selection spans: **Oct 28, 29, 30, 31, Nov 1, 2, 3**
- [ ] Verify both **Oct** and **Nov** are visible
- [ ] Verify dates are correctly labeled

**Status:** _______________  
**Notes:** _______________

---

### Test Year Boundary Scenarios

#### January 1st Boundary
- [ ] Navigate to January 1, 2025
- [ ] Verify date shows: **2025-01-01**
- [ ] Verify quick selection spans December 2024 and January 2025
- [ ] Verify both **Dec** and **Jan** are visible
- [ ] Verify year 2025 is displayed

#### December 31st Boundary
- [ ] Navigate to December 31, 2025
- [ ] Verify date shows: **2025-12-31**
- [ ] Verify quick selection spans December 2025 and January 2026
- [ ] Verify both **Dec** and **Jan** are visible
- [ ] Verify year boundary handled correctly

**Status:** _______________  
**Notes:** _______________

---

### Verify Accessibility with TalkBack

#### Enable TalkBack
- [ ] Open Settings → Accessibility → TalkBack
- [ ] Toggle TalkBack ON
- [ ] Return to Daily Logging screen

#### Date Navigation Accessibility
- [ ] Swipe to date navigation section
- [ ] Verify announces: "Date navigation section"
- [ ] Swipe to previous day button
- [ ] Verify announces: "Go to previous day, button"
- [ ] Swipe to selected date
- [ ] Verify announces: "Selected date: 2025-10-10"
- [ ] Swipe to next day button
- [ ] Verify announces: "Go to next day, button"

#### Quick Date Selection Accessibility
- [ ] Swipe through quick date selection
- [ ] Verify announces: "Quick date selection, swipe to browse recent dates"
- [ ] For each date, verify format: "Select Oct [day], button"
- [ ] For selected date, verify: "Oct 10, currently selected, button"

#### Date Change Announcements
- [ ] Double-tap previous day button
- [ ] Verify TalkBack announces new date automatically
- [ ] Swipe to selected date
- [ ] Verify announces: "Selected date: 2025-10-09"
- [ ] Double-tap next day button
- [ ] Verify TalkBack announces new date automatically

#### Touch Target Sizes
- [ ] Enable Developer Options → Show layout bounds
- [ ] Verify previous day button ≥ 48dp × 48dp
- [ ] Verify next day button ≥ 48dp × 48dp
- [ ] Verify date cards ≥ 48dp height
- [ ] Verify all buttons are easily tappable

**Status:** _______________  
**Notes:** _______________

---

## Automated Test Execution

### Run Automated Tests
```bash
# Run all calendar tests
./gradlew :androidApp:connectedDebugAndroidTest

# View results
open androidApp/build/reports/androidTests/connected/index.html
```

- [ ] All tests pass
- [ ] No test failures
- [ ] Test report generated

**Test Results:** _______________  
**Pass Rate:** _____ / _____

---

## Requirements Coverage

### Requirement 1: Display Correct Calendar Dates
- [ ] 1.1 - Display dates relative to selected date
- [ ] 1.2 - Show October dates on Oct 10, 2025
- [ ] 1.3 - Update on navigation
- [ ] 1.4 - Show correct day and month

### Requirement 2: Dynamic Date Calculation
- [ ] 2.1 - Calculate dates dynamically
- [ ] 2.2 - Use actual selected date
- [ ] 2.3 - Format day and month correctly
- [ ] 2.4 - Update immediately on change

### Requirement 3: Maintain Accessibility
- [ ] 3.1 - Accurate accessibility descriptions
- [ ] 3.2 - Indicate selected date
- [ ] 3.3 - Announce changes
- [ ] 3.4 - Minimum 48dp touch targets

**Requirements Met:** _____ / 12

---

## Bug Fix Verification

### Original Bug
- [x] Calendar showed January dates instead of October dates
- [x] Dates were hardcoded (30, 29, 28, etc.)
- [x] Month was always "January"

### Fix Verification
- [ ] NO January dates shown when viewing October
- [ ] Dates are calculated dynamically
- [ ] Month abbreviation matches actual month
- [ ] Date range updates when navigating
- [ ] All months display correctly

**Bug Fixed:** [ ] Yes [ ] No

---

## Final Sign-Off

**Tester Name:** _______________  
**Date:** _______________  
**Device/Emulator:** _______________  
**Android Version:** _______________  
**App Version:** _______________

**Overall Result:** [ ] PASS [ ] FAIL

**Summary:**
_____________________________________________________________________________
_____________________________________________________________________________
_____________________________________________________________________________

**Issues Found:**
_____________________________________________________________________________
_____________________________________________________________________________
_____________________________________________________________________________

**Recommendations:**
_____________________________________________________________________________
_____________________________________________________________________________
_____________________________________________________________________________
