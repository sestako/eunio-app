# Design Document

## Overview

This design addresses the calendar date display bug in the Daily Logging screen where hardcoded January dates are shown instead of dynamically calculated dates based on the selected date. The fix involves modifying the `AccessibleDateNavigationSection` composable to calculate dates relative to the currently selected date and properly format them with the correct month and day.

## Architecture

### Current Implementation Issue

The current implementation in `DailyLoggingScreen.kt` has a hardcoded loop that always displays January dates:

```kotlin
items(7) { index ->
    val dayNumber = 30 - index  // Hardcoded: always 30, 29, 28...
    val dateDescription = "January $dayNumber"  // Hardcoded: always January
    // ...
}
```

This ignores the `selectedDate` parameter passed to the component and always shows the same dates regardless of the actual selected date.

### Proposed Solution

The solution involves:
1. Using the `selectedDate` parameter to calculate a range of dates around the selected date
2. Parsing the `selectedDate` string to a `LocalDate` object
3. Calculating dates dynamically (e.g., selected date, selected date - 1 day, selected date - 2 days, etc.)
4. Formatting each date to extract the day number and month abbreviation
5. Updating accessibility descriptions to reflect the correct dates

## Components and Interfaces

### Modified Component: AccessibleDateNavigationSection

**Input Parameters:**
- `selectedDate: String` - The currently selected date in ISO format (e.g., "2025-10-10")
- `onPreviousDay: () -> Unit` - Callback for navigating to previous day
- `onNextDay: () -> Unit` - Callback for navigating to next day
- `modifier: Modifier` - Compose modifier

**Internal Logic:**
1. Parse `selectedDate` string to `LocalDate` using `kotlinx.datetime.LocalDate.parse()`
2. Generate a list of dates centered around the selected date (e.g., 3 days before to 3 days after)
3. For each date in the list:
   - Extract day number using `date.dayOfMonth`
   - Extract month abbreviation using `date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }`
   - Determine if the date matches the selected date for highlighting
4. Render each date card with correct formatting and accessibility labels

### Date Calculation Logic

```kotlin
// Parse the selected date
val currentDate = LocalDate.parse(selectedDate)

// Generate date range (e.g., 3 days before to 3 days after)
val dateRange = (-3..3).map { offset ->
    currentDate.plus(offset, DateTimeUnit.DAY)
}
```

### Date Formatting Logic

```kotlin
data class DateDisplay(
    val date: LocalDate,
    val dayNumber: Int,
    val monthAbbreviation: String,
    val isSelected: Boolean
)

fun formatDateForDisplay(date: LocalDate, selectedDate: LocalDate): DateDisplay {
    return DateDisplay(
        date = date,
        dayNumber = date.dayOfMonth,
        monthAbbreviation = date.month.name.take(3).lowercase()
            .replaceFirstChar { it.uppercase() },
        isSelected = date == selectedDate
    )
}
```

## Data Models

### DateDisplay (Internal Data Class)

```kotlin
private data class DateDisplay(
    val date: LocalDate,
    val dayNumber: Int,
    val monthAbbreviation: String,
    val isSelected: Boolean,
    val accessibilityDescription: String
)
```

This internal data class encapsulates all the information needed to render a single date card in the quick selection row.

## Error Handling

### Date Parsing Errors

**Scenario:** The `selectedDate` string cannot be parsed as a valid `LocalDate`

**Handling:**
- Wrap `LocalDate.parse()` in a try-catch block
- Fall back to current system date if parsing fails
- Log the error for debugging purposes
- Display a user-friendly message if the date is invalid

```kotlin
val currentDate = try {
    LocalDate.parse(selectedDate)
} catch (e: Exception) {
    // Fallback to today's date
    Clock.System.todayIn(TimeZone.currentSystemDefault())
}
```

### Edge Cases

1. **Month Boundaries:** When the selected date is near the beginning or end of a month, the date range may span multiple months
   - Solution: The `LocalDate.plus()` function handles month boundaries automatically

2. **Year Boundaries:** When the selected date is near the beginning or end of a year
   - Solution: The `LocalDate.plus()` function handles year boundaries automatically

3. **Empty or Null Selected Date:** If `selectedDate` is empty or null
   - Solution: Use current system date as fallback

## Testing Strategy

### Unit Tests

1. **Date Calculation Tests**
   - Test that dates are calculated correctly relative to the selected date
   - Test month boundary scenarios (e.g., October 1st should show September dates)
   - Test year boundary scenarios (e.g., January 1st should show December dates from previous year)

2. **Date Formatting Tests**
   - Test that day numbers are extracted correctly
   - Test that month abbreviations are formatted correctly (e.g., "Oct", "Jan", "Dec")
   - Test that selected date is identified correctly

3. **Error Handling Tests**
   - Test behavior when `selectedDate` is an invalid string
   - Test behavior when `selectedDate` is null or empty

### Integration Tests

1. **UI State Tests**
   - Test that changing the selected date updates the quick selection row
   - Test that clicking previous/next day buttons updates the displayed dates
   - Test that clicking a date in the quick selection updates the selected date

2. **Accessibility Tests**
   - Test that accessibility descriptions include correct dates
   - Test that selected date is announced correctly
   - Test that date changes are announced to screen readers

3. **Cross-Platform Sync Tests**
   - Test that logs created on Android with corrected dates sync to Firebase
   - Test that logs created on iOS sync to Firebase with correct dates
   - Test that synced logs display with correct dates on both platforms
   - Test that date fields are not corrupted during serialization/deserialization
   - Test timezone handling to ensure dates remain consistent across platforms

### Manual Testing Checklist

#### Android Calendar Display Testing
1. Open Daily Logging screen on October 10, 2025
2. Verify quick selection shows dates around October 10 (e.g., Oct 7-13)
3. Click previous day button
4. Verify quick selection updates to show dates around October 9
5. Click next day button twice
6. Verify quick selection updates to show dates around October 11
7. Test with dates near month boundaries (e.g., October 1, October 31)
8. Test with dates near year boundaries (e.g., January 1, December 31)
9. Enable TalkBack and verify date announcements are correct

#### iOS Calendar Display Testing
1. Open Daily Logging screen on iOS on October 10, 2025
2. Verify the date picker shows the correct date
3. Navigate to previous/next days and verify dates update correctly
4. Test with dates near month and year boundaries
5. Enable VoiceOver and verify date announcements are correct

#### Cross-Platform Firebase Sync Testing
1. **Android to iOS Sync:**
   - On Android, select October 10, 2025 in the calendar
   - Create a daily log with test data (e.g., period flow: Light, mood: Happy, BBT: 98.2)
   - Save the log and verify success message
   - Wait for Firebase sync (or trigger manual sync if available)
   - Open iOS app and navigate to October 10, 2025
   - Verify the log appears with correct date and all data fields match

2. **iOS to Android Sync:**
   - On iOS, select October 10, 2025 in the date picker
   - Create a daily log with different test data (e.g., symptoms: Headache, notes: "Test from iOS")
   - Save the log and verify success
   - Wait for Firebase sync
   - Open Android app and navigate to October 10, 2025
   - Verify the log appears with correct date and all data fields match

3. **Bidirectional Update Sync:**
   - Update an existing log on Android (e.g., add a symptom)
   - Verify the update syncs to iOS with correct date maintained
   - Update the same log on iOS (e.g., change mood)
   - Verify the update syncs back to Android with correct date maintained

4. **Date Integrity Verification:**
   - Create logs on multiple dates (Oct 8, 9, 10, 11, 12)
   - Verify all logs sync with correct dates to the other platform
   - Verify no date shifting or timezone issues occur during sync

## Implementation Notes

### Dependencies

- `kotlinx.datetime.LocalDate` - For date parsing and manipulation
- `kotlinx.datetime.DateTimeUnit` - For date arithmetic
- `kotlinx.datetime.Clock` - For fallback to current date
- `kotlinx.datetime.TimeZone` - For timezone-aware date operations

### Performance Considerations

- Date calculations are lightweight and can be performed on every recomposition
- Consider memoizing the date list if performance issues arise
- The date range is small (7 dates), so performance impact is minimal

### Accessibility Considerations

- Each date card must have a unique and descriptive content description
- Selected date must be clearly indicated in accessibility announcements
- Date format should be consistent with user's locale preferences (future enhancement)
