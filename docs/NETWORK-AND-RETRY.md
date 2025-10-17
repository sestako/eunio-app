# Network Monitoring & Retry Logic

## Network Monitoring

The app monitors network connectivity and provides feedback to users.

### Features
- Real-time network status detection
- Automatic retry when connection restored
- User-friendly error messages
- Offline mode indicators

### Implementation
- Android: `ConnectivityManager` with network callbacks
- iOS: `NWPathMonitor` for network status

## Retry Logic

### Automatic Retry
Failed operations automatically retry with exponential backoff:
- 1st retry: 1 second
- 2nd retry: 2 seconds
- 3rd retry: 4 seconds
- Max retries: 3

### Manual Retry
Users can manually retry failed operations via UI buttons.

### Retry Scenarios
- Network errors
- Timeout errors
- Server errors (5xx)
- Firebase connection issues

## Testing

### Simulate Network Issues
1. Enable airplane mode
2. Attempt to save data
3. Observe error message
4. Disable airplane mode
5. Tap retry button
6. Verify operation succeeds

### Monitor Retry Attempts
Check logs for retry attempts:
```bash
# Android
adb logcat | grep "Retry"

# iOS
# Check Xcode console for retry messages
```

## Configuration

Retry settings can be adjusted in:
- Android: `shared/src/commonMain/kotlin/com/eunio/healthapp/util/RetryPolicy.kt`
- iOS: Same shared code

## Best Practices

1. Always show network status to users
2. Provide manual retry options
3. Don't retry indefinitely
4. Cache data locally when offline
5. Sync when connection restored
