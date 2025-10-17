# Measurement Display Components

This directory contains Compose UI components for displaying measurements with automatic unit conversion based on user preferences.

## Components

### Basic Display Components

These components require the unit system to be passed explicitly:

#### WeightDisplay
Displays weight measurements with conversion from kg (stored format) to user's preferred unit system.

```kotlin
WeightDisplay(
    weightInKg = 70.0,
    unitSystem = UnitSystem.METRIC // or UnitSystem.IMPERIAL
)
// Output: "70 kg" or "154.32 lbs"
```

#### DistanceDisplay
Displays distance measurements with conversion from km (stored format) to user's preferred unit system.

```kotlin
DistanceDisplay(
    distanceInKm = 5.0,
    unitSystem = UnitSystem.IMPERIAL
)
// Output: "3.11 miles"
```

#### TemperatureDisplay
Displays temperature measurements with conversion from °C (stored format) to user's preferred unit system.

```kotlin
TemperatureDisplay(
    temperatureInCelsius = 36.5,
    unitSystem = UnitSystem.IMPERIAL
)
// Output: "97.7°F"
```

### Reactive Display Components

These components automatically observe unit system changes and update accordingly:

#### ReactiveWeightDisplay
Automatically updates when the user changes their unit system preference.

```kotlin
ReactiveWeightDisplay(
    weightInKg = 70.0
)
// Automatically shows "70 kg" or "154.32 lbs" based on current user preference
```

#### ReactiveDistanceDisplay
Automatically updates when the user changes their unit system preference.

```kotlin
ReactiveDistanceDisplay(
    distanceInKm = 10.0
)
// Automatically shows "10 km" or "6.21 miles" based on current user preference
```

#### ReactiveTemperatureDisplay
Automatically updates when the user changes their unit system preference.

```kotlin
ReactiveTemperatureDisplay(
    temperatureInCelsius = 0.0
)
// Automatically shows "0°C" or "32°F" based on current user preference
```

## Usage Guidelines

### When to Use Basic vs Reactive Components

- **Use Basic Components** when:
  - You already have the unit system available in your composable
  - You want explicit control over the unit system display
  - Performance is critical and you want to avoid additional Flow observations

- **Use Reactive Components** when:
  - You want components that automatically update when preferences change
  - You don't have easy access to the current unit system
  - You want the most user-friendly experience with automatic updates

### Styling

All components accept standard Compose styling parameters:

```kotlin
WeightDisplay(
    weightInKg = 70.0,
    unitSystem = UnitSystem.METRIC,
    modifier = Modifier.padding(16.dp),
    style = MaterialTheme.typography.h6,
    color = MaterialTheme.colors.primary
)
```

### Data Format Requirements

- **Weight**: Always pass values in kilograms (kg)
- **Distance**: Always pass values in kilometers (km)  
- **Temperature**: Always pass values in Celsius (°C)

The components handle all conversion logic internally using the `UnitConverter` service.

## Implementation Details

### Dependency Injection

All components use Koin for dependency injection to access:
- `UnitConverter`: For conversion and formatting logic
- `UnitSystemManager`: For observing unit system changes (reactive components only)

### Conversion Formulas

The components use precise conversion formulas as specified in the requirements:
- Weight: `kg = lbs / 2.20462`
- Distance: `km = miles / 0.621371`  
- Temperature: `°C = (°F - 32) × 5/9`

### Precision and Formatting

- All values are rounded to 2 decimal places using round half up logic
- Trailing zeros are removed for cleaner display
- Units are properly formatted with appropriate symbols (°C, °F, kg, lbs, km, miles)

## Testing

Comprehensive UI tests are available in:
- `MeasurementDisplayTest.kt`: Tests basic display components
- `ReactiveMeasurementDisplayTest.kt`: Tests reactive behavior and unit system changes

Run tests with:
```bash
./gradlew :androidApp:connectedAndroidTest
```