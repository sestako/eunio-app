# Android Logging Guide

## Overview

Android app uses structured logging with Timber for debugging and monitoring.

## Log Filtering

### Watch Specific App Logs
```bash
./scripts/watch-my-app-logs.sh
```

### Filter by Tag
```bash
adb logcat -s "YourTag:*"
```

### Filter by Priority
```bash
adb logcat *:E  # Errors only
adb logcat *:W  # Warnings and above
adb logcat *:I  # Info and above
```

### Common Tags
- `DailyLoggingViewModel` - Daily logging screen
- `LogRepository` - Data repository operations
- `FirestoreService` - Firebase operations
- `AuthViewModel` - Authentication
- `NetworkMonitor` - Network connectivity

## Troubleshooting

### No Logs Appearing
1. Check device is connected: `adb devices`
2. Restart adb: `adb kill-server && adb start-server`
3. Clear logcat buffer: `adb logcat -c`

### Too Many Logs
Use the watch script with grep:
```bash
./scripts/watch-my-app-logs.sh | grep "Firebase"
```

### Save Logs to File
```bash
adb logcat > logs.txt
```

## Best Practices

1. Use appropriate log levels:
   - `Timber.e()` - Errors
   - `Timber.w()` - Warnings
   - `Timber.i()` - Info
   - `Timber.d()` - Debug
   - `Timber.v()` - Verbose

2. Include context in log messages
3. Don't log sensitive data (passwords, tokens, etc.)
4. Use structured tags for filtering
