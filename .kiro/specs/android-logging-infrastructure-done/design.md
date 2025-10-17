# Design Document

## Overview

This design document outlines the implementation strategy for standardizing logging across the Android application. The current implementation uses inconsistent logging methods (`println()` vs `android.util.Log`), which makes debugging difficult as `println()` output doesn't appear in Android Studio's logcat console with proper filtering and tagging.

The solution involves:
1. Replacing all `println()` calls with appropriate `android.util.Log` methods
2. Implementing consistent tag naming conventions across all Android services
3. Using appropriate log levels (DEBUG, INFO, WARN, ERROR) based on message severity
4. Ensuring all log messages include proper context and exception information

This is a straightforward refactoring task that will significantly improve the developer experience when debugging the Android application.

## Architecture

### Current State

The Android application has two logging patterns:

1. **Proper Logging** (AndroidAnalyticsService, AndroidCrashlyticsService, AndroidNetworkMonitor):
   - Uses `android.util.Log.d()`, `Log.e()`, `Log.w()`
   - Includes proper tags (e.g., "Analytics", "Crashlytics", "NetworkMonitor")
   - Messages appear in logcat with filtering capabilities

2. **Improper Logging** (AndroidDailyLogService, AndroidUserProfileService, AndroidAuthService, AndroidSettingsManager, AndroidNavigationManager):
   - Uses `println()` statements
   - No tags or log levels
   - Messages don't appear in logcat console properly
   - Difficult to filter or search

### Target State

All Android services will use consistent logging:

```kotlin
class AndroidDailyLogService : DailyLogService {
    companion object {
        private const val TAG = "DailyLogService"
    }
    
    override suspend fun createLog(log: DailyLog): Result<Unit> {
        return try {
            withRetry(
                policy = RetryPolicy.AGGRESSIVE,
                onRetry = { attempt, error, delay ->
                    Log.w(TAG, "Retrying createLog (attempt ${attempt + 1}) in ${delay.inWholeSeconds}s: ${error.message}")
                }
            ) {
                // ... operation
            }
            Log.d(TAG, "Successfully created log for user ${log.userId} on ${log.date}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create log after retries", e)
            Result.failure(e)
        }
    }
}
```

## Components and Interfaces

### Log Tag Constants

Each service class will define a companion object with a TAG constant:

```kotlin
companion object {
    private const val TAG = "ServiceName"
}
```

**Tag Naming Convention:**
- `DailyLogService` - for AndroidDailyLogService
- `UserProfileService` - for AndroidUserProfileService
- `AuthService` - for AndroidAuthService
- `SettingsManager` - for AndroidSettingsManager
- `NavigationManager` - for AndroidNavigationManager

This keeps tags concise while remaining identifiable.

### Log Level Mapping

| Scenario | Log Level | Method | Example |
|----------|-----------|--------|---------|
| Routine operations | DEBUG | `Log.d()` | "Successfully created log", "Fetching profile" |
| Important milestones | INFO | `Log.i()` | "Service initialized", "User authenticated" |
| Retry attempts | WARN | `Log.w()` | "Retrying operation (attempt 2)" |
| Recoverable errors | WARN | `Log.w()` | "Failed to save settings to SharedPreferences" |
| Critical failures | ERROR | `Log.e()` | "Failed to create log after retries" |

### Message Structure

Log messages will follow these patterns:

**Success Operations:**
```kotlin
Log.d(TAG, "Successfully created log for user $userId on $date")
Log.d(TAG, "Retrieved ${logs.size} logs for date range")
```

**Retry Operations:**
```kotlin
Log.w(TAG, "Retrying createLog (attempt ${attempt + 1}) in ${delay.inWholeSeconds}s: ${error.message}")
```

**Failures:**
```kotlin
Log.e(TAG, "Failed to create log after retries", exception)
Log.e(TAG, "Failed to update profile for user $userId", exception)
```

## Data Models

No new data models are required. This is purely a logging infrastructure change.

## Error Handling

### Exception Logging

When logging errors, always pass the exception to `Log.e()`:

```kotlin
catch (e: Exception) {
    Log.e(TAG, "Operation failed: ${e.message}", e)
    Result.failure(e)
}
```

This ensures the full stack trace is captured in logcat.

### Sensitive Data Protection

Log messages must not expose sensitive user data:

**Good:**
```kotlin
Log.d(TAG, "Successfully created log for user $userId")
Log.d(TAG, "Updated profile for user $userId")
```

**Bad (avoid):**
```kotlin
Log.d(TAG, "Created log: $log") // May expose sensitive health data
Log.d(TAG, "Profile email: ${profile.email}") // Exposes PII
```

## Testing Strategy

### Manual Verification

1. **Logcat Visibility Test:**
   - Run the app in Android Studio
   - Open Logcat panel
   - Verify all log messages appear with proper tags
   - Test filtering by tag (e.g., filter by "DailyLogService")

2. **Log Level Test:**
   - Trigger various operations (create log, update profile, etc.)
   - Verify appropriate log levels are used
   - Check that errors show full stack traces

3. **Retry Logging Test:**
   - Simulate network issues to trigger retries
   - Verify retry attempts are logged at WARN level
   - Verify final failures are logged at ERROR level

### Service-Specific Testing

For each migrated service:

1. **AndroidDailyLogService:**
   - Create a daily log → verify DEBUG log appears
   - Trigger retry → verify WARN log appears
   - Cause failure → verify ERROR log with exception appears

2. **AndroidUserProfileService:**
   - Create profile → verify DEBUG log
   - Update profile → verify DEBUG log
   - Test retry scenarios → verify WARN logs

3. **AndroidAuthService:**
   - Sign in → verify DEBUG/INFO logs
   - Test retry scenarios → verify WARN logs

4. **AndroidSettingsManager:**
   - Update settings → verify DEBUG log
   - Trigger SharedPreferences error → verify WARN log

5. **AndroidNavigationManager:**
   - Navigate to screen → verify DEBUG log with route

### Logcat Filter Examples

After implementation, developers should be able to use these filters:

```
tag:DailyLogService          # All logs from daily log service
tag:DailyLogService level:error  # Only errors from daily log service
tag:Service                  # All service logs (if using prefix pattern)
level:warn                   # All warnings across the app
```

## Implementation Notes

### Files to Modify

1. `shared/src/androidMain/kotlin/com/eunio/healthapp/services/AndroidDailyLogService.kt`
2. `shared/src/androidMain/kotlin/com/eunio/healthapp/services/AndroidUserProfileService.kt`
3. `shared/src/androidMain/kotlin/com/eunio/healthapp/auth/AndroidAuthService.kt`
4. `shared/src/androidMain/kotlin/com/eunio/healthapp/domain/manager/AndroidSettingsManager.kt`
5. `shared/src/androidMain/kotlin/com/eunio/healthapp/platform/AndroidNavigationManager.kt`

### Import Statement

All files will need:
```kotlin
import android.util.Log
```

### Companion Object Pattern

Each class will add:
```kotlin
companion object {
    private const val TAG = "ServiceName"
}
```

### Migration Pattern

For each `println()` statement:

1. Determine appropriate log level (DEBUG, WARN, ERROR)
2. Replace with corresponding `Log` method
3. Add TAG as first parameter
4. For errors, add exception as third parameter

**Before:**
```kotlin
println("AndroidDailyLogService: createLog failed after retries: ${e.message}")
```

**After:**
```kotlin
Log.e(TAG, "Failed to create log after retries", e)
```

## Build Configuration

### ProGuard/R8 Rules

For release builds, Android's default ProGuard/R8 configuration already removes DEBUG and VERBOSE logs. No additional configuration is needed.

If custom rules are required, add to `proguard-rules.pro`:

```proguard
# Remove debug and verbose logs in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}
```

This ensures production builds don't include verbose logging.

## Success Criteria

The implementation will be considered successful when:

1. ✅ All `println()` statements in Android services are replaced with `Log` methods
2. ✅ All log messages appear in Android Studio's logcat console
3. ✅ Log messages can be filtered by tag
4. ✅ Appropriate log levels are used (DEBUG for operations, WARN for retries, ERROR for failures)
5. ✅ Error logs include full exception stack traces
6. ✅ No sensitive user data is exposed in log messages
7. ✅ Developers can easily debug issues using logcat filtering

## Future Enhancements

Potential future improvements (not in scope for this feature):

1. **Structured Logging Library:** Consider using Timber or similar library for more advanced logging features
2. **Remote Logging:** Integrate with Firebase Crashlytics for remote log collection
3. **Log Aggregation:** Implement log aggregation for analytics and monitoring
4. **Performance Logging:** Add performance metrics logging for slow operations
5. **User Action Logging:** Implement comprehensive user action logging for support purposes
