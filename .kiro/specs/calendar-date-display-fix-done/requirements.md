# Requirements Document

## Introduction

The Daily Logging screen's calendar date picker is displaying incorrect dates. When the current date is October 10, 2025, the quick date selection row shows January dates (30 Jan, 29 Jan, 28 Jan, etc.) instead of the correct October dates. This bug affects user experience by showing misleading date information and potentially causing users to log data for the wrong dates.

## Requirements

### Requirement 1: Display Correct Calendar Dates

**User Story:** As a user logging my daily health data, I want to see the correct dates in the calendar picker, so that I can accurately select and log data for the right day.

#### Acceptance Criteria

1. WHEN the user opens the Daily Logging screen THEN the quick date selection row SHALL display dates relative to the currently selected date
2. WHEN the selected date is October 10, 2025 THEN the calendar SHALL show dates around October 10 (e.g., Oct 10, Oct 9, Oct 8, etc.)
3. WHEN the user navigates to a different date using the arrow buttons THEN the quick date selection row SHALL update to show dates relative to the new selected date
4. WHEN the quick date selection displays dates THEN each date SHALL show the correct day number and month abbreviation

### Requirement 2: Dynamic Date Calculation

**User Story:** As a user, I want the calendar to dynamically calculate and display dates based on my current selection, so that the date picker always shows relevant and accurate information.

#### Acceptance Criteria

1. WHEN the calendar renders the quick date selection THEN it SHALL calculate dates dynamically based on the selected date
2. WHEN calculating dates for the quick selection row THEN the system SHALL use the actual selected date from the UI state
3. WHEN displaying dates THEN the system SHALL correctly format both the day number and month abbreviation
4. WHEN the selected date changes THEN the quick date selection SHALL immediately reflect the new date range

### Requirement 3: Maintain Accessibility Features

**User Story:** As a user relying on accessibility features, I want the corrected calendar to maintain all accessibility announcements and descriptions, so that I can continue to use the app effectively.

#### Acceptance Criteria

1. WHEN dates are displayed in the quick selection THEN each date SHALL have an accurate accessibility description including the correct month and day
2. WHEN the selected date is highlighted THEN the accessibility description SHALL indicate "currently selected" with the correct date
3. WHEN dates are updated THEN screen readers SHALL announce the changes appropriately
4. WHEN users navigate dates THEN all touch targets SHALL remain at minimum 48dp size for accessibility

### Requirement 4: Cross-Platform Data Synchronization Verification

**User Story:** As a user who uses both Android and iOS devices, I want to verify that my daily logs sync correctly between platforms after the calendar fix, so that I can trust my data is consistent across all my devices.

#### Acceptance Criteria

1. WHEN a user creates a daily log on Android with the corrected calendar date THEN the log SHALL sync to Firebase with the correct date
2. WHEN a user views the same log on iOS THEN the log SHALL display with the correct date matching the Android entry
3. WHEN a user creates a daily log on iOS with the corrected calendar date THEN the log SHALL sync to Firebase with the correct date
4. WHEN a user views the same log on Android THEN the log SHALL display with the correct date matching the iOS entry
5. WHEN logs are synced between platforms THEN all log data (period flow, symptoms, mood, BBT, etc.) SHALL remain intact and accurate
6. WHEN testing cross-platform sync THEN the test SHALL verify logs created on October 10, 2025 appear with the correct date on both platforms
