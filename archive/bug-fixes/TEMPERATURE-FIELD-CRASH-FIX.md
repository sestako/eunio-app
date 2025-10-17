# Temperature Field Crash Fix

## Problem
The app was crashing when trying to write a number into the temperature field with this error:
```
Fatal Exception: org.koin.core.error.InstanceCreationException: 
Could not create instance for '[Singleton:'com.eunio.healthapp.domain.manager.SettingsAwareUnitSystemManager']'
```

## Root Cause
The crash was caused by **missing data source registrations** in the Koin dependency injection configuration.

The dependency chain was:
1. `ReactiveTemperatureDisplay` needs `SettingsAwareUnitSystemManager`
2. `SettingsAwareUnitSystemManager` needs `SettingsManager`
3. `SettingsManager` needs `SettingsBackupManager`
4. `SettingsBackupManager` needs `SettingsLocalDataSource` and `SettingsRemoteDataSource`
5. **These data sources were NEVER registered in Koin** ❌

When Koin tried to create the entire chain, it failed at step 4 because the data sources didn't exist.

## Solution
Added the missing data source registrations to `RepositoryModule.kt`:

```kotlin
// Data Sources - Settings
single<SettingsLocalDataSource> { 
    SettingsLocalDataSourceImpl(databaseManager = get())
}

single<SettingsRemoteDataSource> { 
    SettingsRemoteDataSourceImpl(firestoreService = get())
}

// Data Sources - Preferences
single<PreferencesLocalDataSource> { 
    PreferencesLocalDataSourceImpl(databaseManager = get())
}

single<PreferencesRemoteDataSource> { 
    PreferencesRemoteDataSourceImpl(firestoreService = get())
}
```

Also reordered dependencies in `SettingsIntegrationModule.kt` to ensure proper initialization order.

## Files Changed
- `shared/src/commonMain/kotlin/com/eunio/healthapp/di/RepositoryModule.kt` - Added data source registrations
- `shared/src/commonMain/kotlin/com/eunio/healthapp/di/SettingsIntegrationModule.kt` - Reordered dependencies
- `shared/src/commonMain/kotlin/com/eunio/healthapp/domain/manager/SettingsAwareUnitSystemManager.kt` - Fixed flow emission
- `androidApp/src/androidMain/kotlin/com/eunio/healthapp/android/ui/components/ReactiveTemperatureDisplay.kt` - Simplified to avoid recomposition issues

## Additional Fix - Second Digit Crash

After the initial fix, the app could accept one digit but crashed on the second digit. This was caused by:

**Problem:** The `ReactiveTemperatureDisplay` component was calling `koinInject` and `collectAsState` on every recomposition. Since it's used in the `trailingIcon` of a TextField, it recomposes on every keystroke, causing:
1. Multiple Koin dependency injections per second
2. Multiple flow collections being created and abandoned
3. Race conditions and crashes

**Solution 1:** Modified `observeUnitPreferences()` to emit an initial value immediately:
```kotlin
override fun observeUnitPreferences(): Flow<UnitPreferences> {
    return flow {
        emit(UnitPreferences.default())
        settingsManager.observeSettingsChanges()
            .map { it.unitPreferences }
            .collect { emit(it) }
    }.catch { emit(UnitPreferences.default()) }
}
```

**Solution 2:** Simplified `ReactiveTemperatureDisplay` to use a simple state instead of flow observation to avoid recomposition issues in text field trailing icons:
```kotlin
var unitPreferences by remember { 
    mutableStateOf(UnitPreferences.default()) 
}
```

## Testing
To verify the fix:
1. Rebuild the Android app
2. Navigate to the daily logging screen
3. Try to enter a temperature value in the BBT field (e.g., "98.6")
4. The app should no longer crash and should display the temperature with proper unit conversion as you type

## Technical Details
The crash occurred in the composition chain:
```
DailyLoggingScreen 
  → AccessibleBBTInput 
    → ReactiveTemperatureDisplay 
      → koinInject<SettingsAwareUnitSystemManager>() ❌ CRASH
```

The `ReactiveTemperatureDisplay` component needs `SettingsAwareUnitSystemManager` to observe temperature unit changes and display values in the user's preferred units (Celsius/Fahrenheit).
