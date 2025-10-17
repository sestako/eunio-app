# Android Logcat Filter Quick Reference

## Service Tags

Copy and paste these filters into Android Studio Logcat search box:

### Individual Services
```
tag:DailyLogService
tag:UserProfileService
tag:AuthService
tag:SettingsManager
tag:NavigationManager
```

### Multiple Services
```
tag:DailyLogService|UserProfileService
tag:DailyLogService|UserProfileService|AuthService
```

### By Log Level
```
level:debug
level:warn
level:error
```

### Combined Filters
```
tag:DailyLogService level:error
tag:UserProfileService level:warn
tag:AuthService level:debug
```

## Expected Log Messages

### DailyLogService
- **Success**: `Successfully created log for user [userId] on [date]`
- **Retry**: `Retrying createLog (attempt X) in Xs: [error]`
- **Error**: `Failed to create log after retries`

### UserProfileService
- **Success**: `Successfully created profile for user [userId]`
- **Success**: `Successfully updated profile for user [userId]`
- **Retry**: `Retrying updateProfile (attempt X) in Xs: [error]`
- **Error**: `Failed to update profile after retries`

### AuthService
- **Success**: `Successfully signed in user [userId]`
- **Success**: `Successfully created user account [userId]`
- **Success**: `User signed out`
- **Retry**: `Retrying sign in (attempt X) in Xs: [error]`
- **Error**: `Failed to sign in after retries`

### SettingsManager
- **Warning**: `Failed to save settings to SharedPreferences: [error]`

### NavigationManager
- **Debug**: `Navigating to [destination]`

## Testing Scenarios

### Test Successful Operations
1. Create a daily log → Look for DEBUG logs
2. Update profile → Look for DEBUG logs
3. Navigate screens → Look for DEBUG logs

### Test Retry Logic
1. Enable airplane mode
2. Try to save data
3. Look for WARN logs with retry attempts
4. Disable airplane mode
5. Look for either SUCCESS or ERROR logs

### Test Error Handling
1. Cause operation to fail completely
2. Look for ERROR logs with stack traces
3. Verify exception details are included

## Verification Checklist

- [ ] All 5 service tags work in Logcat
- [ ] DEBUG logs appear for successful operations
- [ ] WARN logs appear for retry attempts
- [ ] ERROR logs appear for failures
- [ ] Stack traces are visible in error logs
- [ ] Log messages include relevant context (userId, date, etc.)
- [ ] No sensitive data exposed in logs

## Troubleshooting

**No logs appearing?**
- Check device/process dropdown is set to your app
- Verify log level is set to "Verbose" or "Debug"
- Ensure app is running in debug mode

**Tags not filtering?**
- Tags are case-sensitive
- Use exact tag names from the list above
- Try clearing and restarting Logcat

**Stack traces missing?**
- Verify app is built in debug mode (not release)
- Check that exceptions are passed to Log.e() as third parameter
