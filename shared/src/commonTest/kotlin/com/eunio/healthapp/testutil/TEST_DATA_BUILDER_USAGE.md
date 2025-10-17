# TestDataBuilder Usage Guide

The `TestDataBuilder` object provides centralized test data creation utilities for consistent model instantiation across all tests.

## New Methods Added

### createUserPreferences()

Creates `UserPreferences` with current constructor parameters:

```kotlin
// Default creation
val defaultPrefs = TestDataBuilder.createUserPreferences()
// userId = "test-user", unitSystem = METRIC, isManuallySet = false, syncStatus = PENDING

// Custom creation
val customPrefs = TestDataBuilder.createUserPreferences(
    userId = "custom-user",
    unitSystem = UnitSystem.IMPERIAL,
    isManuallySet = true,
    syncStatus = SyncStatus.SYNCED
)
```

### createNotificationSettings()

Creates `NotificationPreferences` with enum-based configuration:

```kotlin
// Default creation (daily logging and period prediction enabled)
val defaultNotifications = TestDataBuilder.createNotificationSettings()

// Custom configuration
val customNotifications = TestDataBuilder.createNotificationSettings(
    dailyLoggingEnabled = false,
    periodPredictionEnabled = true,
    ovulationEnabled = true,
    ovulationTime = LocalTime(8, 30),
    insightNotificationsEnabled = false,
    globalNotificationsEnabled = true
)
```

### Convenience Methods

#### Notification Presets
- `createAllNotificationsEnabled()` - All notifications turned on
- `createAllNotificationsDisabled()` - All notifications turned off
- `createEssentialNotificationsOnly()` - Only daily logging and period prediction

#### UserPreferences Presets
- `createImperialUserPreferences(userId)` - Imperial unit system, manually set
- `createMetricUserPreferences(userId)` - Metric unit system, manually set
- `createSyncedUserPreferences(userId)` - Synced status
- `createFailedSyncUserPreferences(userId)` - Failed sync status

## Usage Examples

### Testing Notification Configurations

```kotlin
@Test
fun `test notification preferences validation`() {
    val allEnabled = TestDataBuilder.createAllNotificationsEnabled()
    assertTrue(allEnabled.hasEnabledNotifications())
    assertTrue(allEnabled.isValid())
    
    val allDisabled = TestDataBuilder.createAllNotificationsDisabled()
    assertFalse(allDisabled.hasEnabledNotifications())
    assertTrue(allDisabled.isValid())
}
```

### Testing UserPreferences with Different Unit Systems

```kotlin
@Test
fun `test unit system preferences`() {
    val imperialUser = TestDataBuilder.createImperialUserPreferences("imperial-user")
    assertEquals(UnitSystem.IMPERIAL, imperialUser.unitSystem)
    assertTrue(imperialUser.isManuallySet)
    
    val metricUser = TestDataBuilder.createMetricUserPreferences("metric-user")
    assertEquals(UnitSystem.METRIC, metricUser.unitSystem)
    assertTrue(metricUser.isManuallySet)
}
```

### Testing Sync Status Scenarios

```kotlin
@Test
fun `test sync status handling`() {
    val syncedPrefs = TestDataBuilder.createSyncedUserPreferences("synced-user")
    assertEquals(SyncStatus.SYNCED, syncedPrefs.syncStatus)
    
    val failedPrefs = TestDataBuilder.createFailedSyncUserPreferences("failed-user")
    assertEquals(SyncStatus.FAILED, failedPrefs.syncStatus)
}
```

## Benefits

1. **Consistency** - All tests use the same data creation patterns
2. **Maintainability** - Changes to model constructors only need updates in one place
3. **Readability** - Clear, descriptive method names make test intent obvious
4. **Flexibility** - Both default and custom configurations supported
5. **Type Safety** - Enum-based configurations prevent invalid combinations

## Requirements Satisfied

- ✅ `createUserPreferences()` with current constructor parameters
- ✅ `createDisplayPreferences()` with proper field mapping (already existed, enhanced)
- ✅ `createNotificationSettings()` with enum-based configuration
- ✅ Factory methods for all models
- ✅ Centralized test data creation utilities