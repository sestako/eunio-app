# Android TalkBack Testing Guide
## Calendar Date Display Accessibility Verification

### Purpose
This guide provides step-by-step instructions for manually testing the calendar date display functionality with TalkBack to verify Requirements 3.1, 3.2, 3.3, and 3.4.

---

## Prerequisites

### Device Setup
1. Android device or emulator running API 24+
2. Eunio Health App installed
3. TalkBack accessibility service available

### Enable TalkBack
1. Open **Settings** → **Accessibility**
2. Find **TalkBack** (or **Screen Reader**)
3. Toggle **ON**
4. Confirm the activation dialog
5. **TalkBack Gestures:**
   - **Swipe right:** Move to next item
   - **Swipe left:** Move to previous item
   - **Double-tap:** Activate/click item
   - **Two-finger swipe down:** Back button
   - **Two-finger swipe up:** Home button

---

## Test Scenario 1: Date Navigation Section Accessibility

### Requirement: 3.1 - Accurate accessibility descriptions

**Steps:**
1. Open the Eunio Health App
2. Navigate to **Daily Logging** screen
3. Enable TalkBack if not already enabled
4. Swipe right until you reach the date navigation area

**Expected TalkBack Announcements:**
- ✅ "Date navigation section"
- ✅ "Date navigation controls, current date: 2025-10-10"

**Pass Criteria:**
- [ ] TalkBack announces the date navigation section
- [ ] Current date is clearly announced
- [ ] Announcement is clear and understandable

---

## Test Scenario 2: Navigation Button Accessibility

### Requirement: 3.1, 3.4 - Descriptions and touch targets

**Steps:**
1. Continue from Test Scenario 1
2. Swipe right to the previous day button
3. Note the TalkBack announcement
4. Swipe right to the selected date
5. Note the TalkBack announcement
6. Swipe right to the next day button
7. Note the TalkBack announcement

**Expected TalkBack Announcements:**
- ✅ "Go to previous day, button"
- ✅ "Selected date: 2025-10-10, heading"
- ✅ "Go to next day, button"

**Pass Criteria:**
- [ ] Previous day button is announced correctly
- [ ] Selected date is announced with full date
- [ ] Next day button is announced correctly
- [ ] All buttons are identified as "button" role
- [ ] Selected date is identified as "heading"

---

## Test Scenario 3: Quick Date Selection Accessibility

### Requirement: 3.1, 3.2 - Date descriptions and selected indication

**Steps:**
1. Continue swiping right through the quick date selection
2. Listen for each date card announcement
3. Pay special attention to the currently selected date (Oct 10)

**Expected TalkBack Announcements:**
- ✅ "Quick date selection, swipe to browse recent dates"
- ✅ "Select Oct 7, button"
- ✅ "Select Oct 8, button"
- ✅ "Select Oct 9, button"
- ✅ "Oct 10, currently selected, button"
- ✅ "Select Oct 11, button"
- ✅ "Select Oct 12, button"
- ✅ "Select Oct 13, button"

**Pass Criteria:**
- [ ] Quick date selection area is announced
- [ ] Each date is announced with month abbreviation and day
- [ ] Selected date (Oct 10) includes "currently selected"
- [ ] Non-selected dates include "Select" prefix
- [ ] All dates are identified as "button" role

---

## Test Scenario 4: Date Change Announcements

### Requirement: 3.3 - Announce date changes

**Steps:**
1. Navigate back to the previous day button
2. Double-tap to activate (go to previous day)
3. Wait for TalkBack announcement
4. Swipe to the selected date
5. Note the new date announcement

**Expected TalkBack Announcements:**
- ✅ After clicking previous: "Selected date: 2025-10-09" (or similar announcement)
- ✅ When swiping to date: "Selected date: 2025-10-09, heading"

**Pass Criteria:**
- [ ] Date change is announced automatically
- [ ] New date is clearly stated
- [ ] Announcement happens immediately after navigation
- [ ] Selected date reflects the new date

---

## Test Scenario 5: Multiple Date Changes

### Requirement: 3.3 - Consistent announcements

**Steps:**
1. Navigate to the next day button
2. Double-tap to activate (go to next day)
3. Wait for announcement
4. Double-tap again (go to next day again)
5. Wait for announcement
6. Navigate to previous day button
7. Double-tap to activate
8. Wait for announcement

**Expected TalkBack Announcements:**
- ✅ First next: "Selected date: 2025-10-10"
- ✅ Second next: "Selected date: 2025-10-11"
- ✅ Previous: "Selected date: 2025-10-10"

**Pass Criteria:**
- [ ] Each date change is announced
- [ ] Announcements are consistent
- [ ] Dates are accurate after each navigation
- [ ] No duplicate or missing announcements

---

## Test Scenario 6: Touch Target Size Verification

### Requirement: 3.4 - Minimum 48dp touch targets

**Steps:**
1. Disable TalkBack temporarily
2. Enable **Developer Options** → **Show layout bounds**
3. Navigate to Daily Logging screen
4. Visually inspect the touch target sizes

**Visual Verification:**
- ✅ Previous day button: At least 48dp × 48dp
- ✅ Next day button: At least 48dp × 48dp
- ✅ Each date card: At least 48dp in height
- ✅ Date cards: Adequate width for easy tapping

**Pass Criteria:**
- [ ] Previous day button meets minimum size
- [ ] Next day button meets minimum size
- [ ] Date cards meet minimum height
- [ ] All interactive elements are easily tappable
- [ ] No overlapping touch targets

---

## Test Scenario 7: Month Boundary Accessibility

### Requirement: 3.1, 3.3 - Correct announcements at boundaries

**Steps:**
1. Re-enable TalkBack
2. Navigate to October 1, 2025 (click previous day 9 times)
3. Swipe through the quick date selection
4. Note the month abbreviations announced

**Expected TalkBack Announcements:**
- ✅ "Select Sep 28, button"
- ✅ "Select Sep 29, button"
- ✅ "Select Sep 30, button"
- ✅ "Oct 1, currently selected, button"
- ✅ "Select Oct 2, button"
- ✅ "Select Oct 3, button"
- ✅ "Select Oct 4, button"

**Pass Criteria:**
- [ ] September dates are announced with "Sep"
- [ ] October dates are announced with "Oct"
- [ ] Month transition is clear
- [ ] Selected date (Oct 1) is indicated correctly

---

## Test Scenario 8: Year Boundary Accessibility

### Requirement: 3.1, 3.3 - Correct announcements at year boundaries

**Steps:**
1. Navigate to December 31, 2025
2. Swipe through the quick date selection
3. Note the month abbreviations and year handling

**Expected TalkBack Announcements:**
- ✅ Dates include "Dec" for December dates
- ✅ Dates include "Jan" for January dates (next year)
- ✅ Selected date includes full year: "2025-12-31"

**Pass Criteria:**
- [ ] December dates are announced with "Dec"
- [ ] January dates are announced with "Jan"
- [ ] Year boundary is handled correctly
- [ ] No confusion between years

---

## Test Scenario 9: Form Section Accessibility

### Requirement: 3.1 - All sections have descriptions

**Steps:**
1. Continue swiping right through the form
2. Note the section headings

**Expected TalkBack Announcements:**
- ✅ "Period Flow section, heading"
- ✅ "Symptoms section, heading"
- ✅ "Mood section, heading"
- ✅ "Basal Body Temperature section, heading"
- ✅ "Cervical Mucus section, heading"
- ✅ "Ovulation Test (OPK) section, heading"
- ✅ "Sexual Activity section, heading"
- ✅ "Notes section, heading"

**Pass Criteria:**
- [ ] All sections are announced as headings
- [ ] Section names are clear and descriptive
- [ ] Sections are in logical order
- [ ] Easy to navigate between sections

---

## Test Scenario 10: Error and Success Message Accessibility

### Requirement: 3.3 - Announce important messages

**Steps:**
1. Fill out some form data
2. Navigate to the Save button
3. Double-tap to save
4. Wait for success message announcement

**Expected TalkBack Announcements:**
- ✅ "Log saved successfully" (or similar success message)
- ✅ Message is announced automatically (live region)

**Pass Criteria:**
- [ ] Success message is announced automatically
- [ ] Message is clear and understandable
- [ ] Announcement happens immediately after save
- [ ] User doesn't need to search for confirmation

---

## Test Results Summary

### Date Navigation Accessibility
- [ ] Date navigation section announced correctly
- [ ] Navigation buttons have clear descriptions
- [ ] Selected date is clearly identified

### Quick Date Selection Accessibility
- [ ] Quick selection area is announced
- [ ] Each date has accurate description
- [ ] Selected date includes "currently selected"
- [ ] Month abbreviations are correct

### Date Change Announcements
- [ ] Date changes are announced automatically
- [ ] Announcements are immediate and clear
- [ ] Multiple changes are handled correctly

### Touch Target Sizes
- [ ] All buttons meet 48dp minimum
- [ ] Date cards meet minimum height
- [ ] All targets are easily tappable

### Boundary Handling
- [ ] Month boundaries announced correctly
- [ ] Year boundaries announced correctly
- [ ] No confusion at boundaries

### Overall Accessibility
- [ ] All interactive elements are accessible
- [ ] Navigation is logical and intuitive
- [ ] Announcements are clear and helpful
- [ ] User can complete all tasks with TalkBack

---

## Common Issues and Troubleshooting

### Issue: TalkBack not announcing date changes
**Solution:** Ensure live regions are properly implemented in the code

### Issue: Touch targets too small
**Solution:** Verify `minimumTouchTarget()` modifier is applied

### Issue: Confusing announcements
**Solution:** Review content descriptions for clarity

### Issue: Missing announcements
**Solution:** Check that all interactive elements have content descriptions

---

## Conclusion

This manual testing guide ensures that the calendar date display functionality is fully accessible to users relying on TalkBack. All requirements (3.1, 3.2, 3.3, 3.4) should be verified through these test scenarios.

**Testing Completed By:** _______________  
**Date:** _______________  
**Device:** _______________  
**TalkBack Version:** _______________  
**Overall Result:** [ ] Pass [ ] Fail  

**Notes:**
_____________________________________________________________________________
_____________________________________________________________________________
_____________________________________________________________________________
