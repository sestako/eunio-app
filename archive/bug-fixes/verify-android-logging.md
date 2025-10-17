# Android Logging Verification Guide

This guide will help you verify that all logging has been properly migrated from `println()` to Android's `Log` class.

## Prerequisites

1. Open the project in Android Studio
2. Connect an Android device or start an emulator
3. Open the Logcat panel (View → Tool Windows → Logcat)

## Verification Steps

### Step 1: Initial Setup

1. **Clear Logcat**: Click the "Clear logcat" button (trash icon) in the Logcat panel
2. **Configure Filters**: Set the filter dropdown to "Show only selected application"
3. **Run the App**: Click the Run button or press Shift+F10

### Step 2: Verify Service Tags

After the app starts, verify that you can filter by each service tag:

#### Test DailyLogService Logging

1. **Filter Setup**: In Logcat, type `tag:DailyLogService` in the search box
2. **Trigger Operations**:
   - Navigate to the Daily Logging screen
   - Create a new daily log entry
   - Add symptoms, mood, or other data
   - Save the entry
3. **Expected Logs**:
   ```
   D/DailyLogService: Successfully created log for user [userId] on [date]
   D/DailyLogService: Retrieved [N] logs for date range
   ```
4. **Test Retry Scenario** (if possible, enable airplane mode temporarily):
   - Try to save while offline
   - Expected: `W/DailyLogService: Retrying createLog (attempt 2) in Xs: [error message]`
   - Re-enable network
   - Expected: `D/DailyLogService: Successfully created log...` or `E/DailyLogService: Failed to create log after retries`

#### Test UserProfileService Logging

1. **Filter Setup**: Type `tag:UserProfileService` in Logcat search
2. **Trigger Operations**:
   - Navigate to Profile/Settings
   - Update profile information (name, age, etc.)
   - Save changes
3. **Expected Logs**:
   ```
   D/UserProfileService: Successfully created profile for user [userId]
   D/UserProfileService: Successfully updated profile for user [userId]
   D/UserProfileService: Retrieved profile for user [userId]
   ```

#### Test AuthService Logging

1. **Filter Setup**: Type `tag:AuthService` in Logcat search
2. **Trigger Operations**:
   - Sign out (if signed in)
   - Sign in with credentials
   - Or create a new account
3. **Expected Logs**:
   ```
   D/AuthService: Successfully signed in user [userId]
   D/AuthService: Successfully created user account [userId]
   D/AuthService: User signed out
   ```

#### Test SettingsManager Logging

1. **Filter Setup**: Type `tag:SettingsManager` in Logcat search
2. **Trigger Operations**:
   - Navigate to Settings
   - Toggle various settings (notifications, theme, etc.)
   - Change preferences
3. **Expected Logs**:
   ```
   D/SettingsManager: [Setting operation logs]
   ```
   - If SharedPreferences issues occur: `W/SettingsManager: [Warning message]`

#### Test NavigationManager Logging

1. **Filter Setup**: Type `tag:NavigationManager` in Logcat search
2. **Trigger Operations**:
   - Navigate between different screens
   - Use bottom navigation tabs
   - Navigate to settings and back
3. **Expected Logs**:
   ```
   D/NavigationManager: Navigating to [route/screen]
   ```

### Step 3: Verify Log Levels

Test that different log levels are used appropriately:

1. **DEBUG Logs** (routine operations):
   - Filter: `level:debug`
   - Should show successful operations, data retrieval, navigation

2. **WARN Logs** (retry attempts, recoverable issues):
   - Filter: `level:warn`
   - Trigger by temporarily disabling network
   - Should show retry attempts with attempt numbers and delays

3. **ERROR Logs** (critical failures):
   - Filter: `level:error`
   - Trigger by causing operations to fail completely
   - Should show error messages with full stack traces

### Step 4: Verify Stack Traces

1. **Cause an Error**: Try to trigger a failure (e.g., network timeout)
2. **Check Logcat**: Error logs should include full exception stack traces
3. **Expected Format**:
   ```
   E/DailyLogService: Failed to create log after retries
       java.io.IOException: Network error
           at com.eunio.healthapp.services.AndroidDailyLogService.createLog(AndroidDailyLogService.kt:45)
           at ...
   ```

### Step 5: Test Combined Filters

Test advanced filtering capabilities:

1. **All Service Logs**: `tag:Service` (if using prefix pattern)
2. **Errors Only**: `level:error`
3. **Specific Service Errors**: `tag:DailyLogService level:error`
4. **Multiple Tags**: `tag:DailyLogService|UserProfileService`

## Verification Checklist

Use this checklist to confirm all requirements are met:

- [ ] All log messages appear in Android Studio Logcat panel
- [ ] Can filter logs by tag (DailyLogService, UserProfileService, AuthService, SettingsManager, NavigationManager)
- [ ] DEBUG level used for successful operations
- [ ] WARN level used for retry attempts
- [ ] ERROR level used for failures
- [ ] Error logs include full stack traces with file names and line numbers
- [ ] Log messages are clear and include relevant context (userId, date, etc.)
- [ ] No sensitive data (passwords, health details) exposed in logs
- [ ] No `println()` statements remain (all migrated to Log)

## Common Issues and Solutions

### Issue: No logs appearing in Logcat

**Solution**:
- Verify the app is selected in the device/process dropdown
- Check that log level filter is not set too high (should be "Verbose" or "Debug")
- Ensure the app is actually running on the device/emulator

### Issue: Tags not filtering correctly

**Solution**:
- Use exact tag names: `tag:DailyLogService` (case-sensitive)
- Check for typos in tag constants in the code
- Verify companion objects with TAG constants are present

### Issue: Stack traces not showing

**Solution**:
- Verify exceptions are passed as third parameter to Log.e()
- Check that ProGuard/R8 is not stripping debug info in debug builds
- Ensure the app is built in debug mode, not release mode

## Success Criteria

The verification is complete when:

1. ✅ All five services (DailyLog, UserProfile, Auth, Settings, Navigation) log properly
2. ✅ All logs are visible and filterable in Logcat
3. ✅ Appropriate log levels are used throughout
4. ✅ Error logs include complete stack traces
5. ✅ No `println()` statements remain in the codebase
6. ✅ Developers can efficiently debug using Logcat filters

## Next Steps

After completing verification:

1. Document any issues found
2. Fix any remaining `println()` statements
3. Update tag names if needed for consistency
4. Consider adding this verification process to the development workflow
5. Share Logcat filtering tips with the team

