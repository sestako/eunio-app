# Calendar UI Implementation

This directory contains the smart calendar UI implementation for the Eunio health app, providing cycle phase visualization and fertility tracking.

## Components

### CalendarScreen.kt
The main calendar screen that displays:
- **Monthly Calendar Grid**: Shows dates with cycle phase indicators
- **Navigation Controls**: Previous/next month and "go to today" functionality
- **Cycle Information Card**: Current cycle details and predictions
- **Legend**: Visual guide for understanding calendar indicators
- **Nature-inspired Color Coding**: Uses Eunio design system colors for different cycle phases

#### Key Features:
- **Period Tracking**: Red indicators for actual and predicted periods
- **Ovulation Indicators**: Orange markers for confirmed and predicted ovulation
- **Fertility Window**: Green highlighting for fertile days
- **Interactive Date Selection**: Tap dates to navigate to daily logging
- **Real-time Updates**: Syncs with logged data and cycle predictions

### CycleTrackingScreen.kt
Detailed cycle information screen featuring:
- **Current Cycle Overview**: Cycle day, progress, and statistics
- **Cycle Predictions**: Next period, ovulation, and fertility windows
- **Phase Information**: Current cycle phase with descriptions
- **Cycle History**: Recent cycle data and patterns

## Design System Integration

### Colors
- **Menstrual Phase**: `EunioColors.MenstrualPhase` (red tones)
- **Follicular Phase**: `EunioColors.FollicularPhase` (green tones)
- **Ovulatory Phase**: `EunioColors.OvulatoryPhase` (orange tones)
- **Luteal Phase**: `EunioColors.LutealPhase` (purple tones)

### Typography
- Uses Material 3 typography scale
- Consistent with Eunio design system
- Clear hierarchy for information display

### Layout
- Card-based design with generous white space
- Consistent 16dp padding and spacing
- Responsive grid layout for calendar days

## State Management

### CalendarUiState
```kotlin
data class CalendarUiState(
    val currentMonth: LocalDate?,
    val selectedDate: LocalDate?,
    val currentCycle: Cycle?,
    val logsInMonth: Map<LocalDate, DailyLog>,
    val predictions: CyclePredictions?,
    val isLoading: Boolean,
    val errorMessage: String?
)
```

### CalendarDay
```kotlin
data class CalendarDay(
    val date: LocalDate,
    val isToday: Boolean,
    val isInCurrentMonth: Boolean,
    val dayType: CalendarDayType,
    val hasLog: Boolean,
    val periodFlow: PeriodFlow?
)
```

## Navigation Integration

The calendar integrates with the app's navigation system:
- Selecting dates navigates to daily logging screen
- Supports deep linking with specific dates
- Maintains navigation state across screen transitions

## Testing

### UI Tests (CalendarScreenTest.kt)
- Calendar display and navigation
- Cycle information rendering
- Error state handling
- Loading state verification
- Legend display

### Unit Tests (CalendarScreenUnitTest.kt)
- Calendar day type logic
- Date calculations
- State management
- Fertility window calculations

### Integration Tests (CalendarIntegrationTest.kt)
- ViewModel integration with use cases
- Data loading and error handling
- Navigation state management
- Calendar day generation

## Usage

```kotlin
@Composable
fun MyScreen() {
    CalendarScreen(
        viewModel = koinViewModel(),
        onNavigateToLogging = { date ->
            // Handle navigation to daily logging
        }
    )
}
```

## Requirements Fulfilled

This implementation addresses the following requirements:

### Requirement 3.1: Smart Calendar and Cycle Tracking
- ✅ Automatic cycle record creation from period start dates
- ✅ Visual calendar with cycle phase indicators
- ✅ Date selection and navigation

### Requirement 3.2: Cycle Predictions
- ✅ Future ovulation and period date predictions
- ✅ Cycle phase visualization
- ✅ Fertility window display

### Requirement 3.3: Cycle Management
- ✅ Automatic cycle closure and new cycle creation
- ✅ Cycle length tracking and display
- ✅ Historical cycle information

### Requirement 8.1: Cross-Platform Experience
- ✅ Consistent Eunio design system implementation
- ✅ Nature-inspired color palette
- ✅ Responsive layout design

### Requirement 8.4: User Interface Design
- ✅ Card-based layouts with generous white space
- ✅ Clear typography and visual hierarchy
- ✅ Intuitive navigation and interaction patterns

## Performance Considerations

- **Lazy Loading**: Calendar grid uses LazyVerticalGrid for efficient rendering
- **State Optimization**: Minimal recomposition with proper state management
- **Memory Efficiency**: Only loads data for current month view
- **Caching**: Leverages ViewModel state for data persistence across configuration changes