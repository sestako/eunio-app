# BBT Chart Implementation

## Overview

The BBT (Basal Body Temperature) Chart is a comprehensive temperature tracking and visualization component that provides users with detailed insights into their fertility patterns through temperature trend analysis.

## Features

### 1. Temperature Chart Visualization
- **Custom Canvas Drawing**: Uses Compose Canvas for precise temperature plotting
- **Cycle Phase Backgrounds**: Visual indicators for different menstrual cycle phases
- **Temperature Trend Line**: Smooth line connecting temperature data points
- **Ovulation Detection Line**: Dashed line showing calculated ovulation threshold

### 2. Interactive Controls
- **Zoom Functionality**: 
  - Zoom In/Out buttons with 1.2x increments
  - Reset to 1:1 zoom level
  - Range: 0.5x to 3.0x zoom
- **Pan Support**: Drag gestures to navigate zoomed chart
- **Data Point Selection**: Tap to select specific temperature readings

### 3. Ovulation Detection
- **Temperature Shift Analysis**: Detects sustained temperature rises
- **Visual Markers**: Triangle markers on ovulation days
- **Threshold Calculation**: Automatic calculation of ovulation threshold line
- **Cycle Integration**: Correlates with cycle data for confirmation

### 4. Statistics and Insights
- **Temperature Statistics**:
  - Average temperature
  - Temperature range
  - Number of readings
- **Pattern Recognition**:
  - Elevated temperature detection
  - Consistent rise patterns
  - Temperature variation warnings
- **Cycle Phase Information**: Current phase indicators with color coding

### 5. Unit System Support
- **Reactive Unit Conversion**: Automatically converts between Celsius and Fahrenheit
- **Consistent Formatting**: Uses UnitConverter for proper display formatting
- **Real-time Updates**: Responds to unit system preference changes

## Architecture

### Components

#### BBTChartScreen
Main composable that orchestrates the entire chart interface:
- Manages chart state (zoom, pan, selection)
- Coordinates between different UI sections
- Handles unit system changes

#### BBTChart
Core chart visualization component:
- Custom Canvas drawing with gesture support
- Temperature data plotting and trend lines
- Ovulation markers and phase backgrounds
- Grid system with temperature labels

#### Supporting Components
- **BBTChartHeader**: Title and zoom controls
- **BBTStatistics**: Temperature statistics display
- **CyclePhaseInfo**: Cycle phase indicators
- **BBTPatternInsights**: Pattern analysis results
- **RecentTemperatureReadings**: Latest temperature entries

### Data Models

#### BBTDataPoint
```kotlin
data class BBTDataPoint(
    val date: LocalDate,
    val temperature: Double, // Always stored in Celsius
    val hasOvulationMarker: Boolean = false
)
```

### Key Algorithms

#### Ovulation Detection
```kotlin
private fun calculateOvulationThreshold(temperatures: List<Double>): Double? {
    // Analyzes temperature patterns to detect ovulation
    // Returns threshold temperature for ovulation line
}
```

#### Pattern Analysis
```kotlin
private fun generateTemperatureInsights(
    temperatureData: List<BBTDataPoint>,
    unitSystem: UnitSystem,
    converter: UnitConverter
): List<String> {
    // Generates insights based on temperature patterns
    // Detects elevated temps, consistent rises, variations
}
```

## Usage

### Basic Usage
```kotlin
BBTChartScreen(
    temperatureLogs = dailyLogs.filter { it.bbt != null },
    currentCycle = currentCycle,
    cycleHistory = cycleHistory
)
```

### With Sample Data
```kotlin
BBTChartScreen() // Uses generated sample data for demonstration
```

## Testing

### UI Tests (AndroidTest)
- **BBTChartScreenTest**: Comprehensive UI interaction tests
- Tests zoom controls, data display, empty states
- Verifies statistics, cycle phases, and pattern insights
- Validates gesture handling and scrolling

### Unit Tests
- **BBTChartScreenUnitTest**: Logic and algorithm tests
- Tests ovulation detection algorithms
- Validates pattern recognition logic
- Verifies data generation and formatting

## Design System Integration

### Colors
- **Primary**: `EunioColors.Primary` - Main chart line
- **Secondary**: `EunioColors.Secondary` - Data points
- **Cycle Phases**:
  - Menstrual: `EunioColors.MenstrualPhase`
  - Follicular: `EunioColors.FollicularPhase`
  - Ovulatory: `EunioColors.OvulatoryPhase`
  - Luteal: `EunioColors.LutealPhase`

### Typography
- **Headlines**: `MaterialTheme.typography.headlineSmall`
- **Titles**: `MaterialTheme.typography.titleMedium`
- **Body**: `MaterialTheme.typography.bodyMedium`
- **Labels**: `MaterialTheme.typography.bodySmall`

## Performance Considerations

### Canvas Optimization
- Efficient drawing operations with minimal recomposition
- Zoom and pan state management to prevent unnecessary redraws
- Clipping for off-screen elements

### Data Handling
- Lazy loading for large datasets
- Efficient temperature conversion caching
- Minimal state updates for smooth interactions

## Accessibility

### Screen Reader Support
- Semantic content descriptions for chart elements
- Alternative text for visual markers
- Structured navigation through chart sections

### Interaction Support
- Large touch targets for zoom controls
- Clear visual feedback for selections
- High contrast color schemes

## Future Enhancements

### Planned Features
1. **Export Functionality**: Save chart as image or PDF
2. **Comparison Mode**: Compare multiple cycles
3. **Prediction Overlay**: Show predicted temperatures
4. **Annotation Support**: Add notes to specific data points
5. **Advanced Analytics**: More sophisticated pattern recognition

### Technical Improvements
1. **Performance**: Canvas virtualization for large datasets
2. **Gestures**: Multi-touch zoom and rotation
3. **Animation**: Smooth transitions between data updates
4. **Customization**: User-configurable chart appearance

## Dependencies

### Core Dependencies
- Compose UI and Foundation
- Kotlinx DateTime for date handling
- Koin for dependency injection

### Domain Dependencies
- UnitSystemManager for unit preferences
- UnitConverter for temperature conversion
- Domain models (DailyLog, Cycle)

## Integration Points

### ViewModel Integration
The chart integrates with existing ViewModels:
- **CalendarViewModel**: For cycle and log data
- **DailyLoggingViewModel**: For temperature entries

### Navigation
- Links to daily logging for specific dates
- Integration with calendar navigation

### Data Sources
- Local database via repository pattern
- Real-time updates through reactive streams
- Offline-first architecture support