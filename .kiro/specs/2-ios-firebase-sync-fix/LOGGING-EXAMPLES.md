# iOS Firebase Sync Logging Examples

## Overview
This document shows example log output from the iOS FirestoreService implementation with structured logging enabled.

## Example 1: Successful Save Operation

```
[FirestoreService.iOS] SAVE_DAILY_LOG_START userId=abc123, logId=2025-10-14, path=users/abc123/dailyLogs/2025-10-14, dateEpochDays=20371, createdAt=1729000000, updatedAt=1729000000

[FirestoreService.iOS] SAVE_DAILY_LOG_SUCCESS userId=abc123, logId=2025-10-14, path=users/abc123/dailyLogs/2025-10-14, dateEpochDays=20371, updatedAt=1729000000
```

## Example 2: Successful Read Operation

```
[FirestoreService.iOS] GET_DAILY_LOG_BY_DATE_START userId=abc123, date=2025-10-14, dateEpochDays=20371, path=users/abc123/dailyLogs

[FirestoreService.iOS] GET_DAILY_LOG_BY_DATE_SUCCESS userId=abc123, date=2025-10-14, dateEpochDays=20371, logId=2025-10-14, path=users/abc123/dailyLogs, updatedAt=1729000000
```

## Example 3: Read Operation - Not Found

```
[FirestoreService.iOS] GET_DAILY_LOG_START userId=abc123, logId=2025-10-15, path=users/abc123/dailyLogs/2025-10-15

[FirestoreService.iOS] GET_DAILY_LOG_NOT_FOUND userId=abc123, logId=2025-10-15, path=users/abc123/dailyLogs/2025-10-15
```

## Example 4: Network Error

```
[FirestoreService.iOS] SAVE_DAILY_LOG_START userId=abc123, logId=2025-10-14, path=users/abc123/dailyLogs/2025-10-14, dateEpochDays=20371, createdAt=1729000000, updatedAt=1729000000

[FirestoreService.iOS] SAVE_DAILY_LOG_ERROR userId=abc123, logId=2025-10-14, path=users/abc123/dailyLogs/2025-10-14, dateEpochDays=20371, error=Network unavailable, errorType=NetworkError
```

## Example 5: Range Query

```
[FirestoreService.iOS] GET_LOGS_IN_RANGE_START userId=abc123, startDate=2025-10-01, endDate=2025-10-31, startEpochDays=20358, endEpochDays=20388, path=users/abc123/dailyLogs

[FirestoreService.iOS] GET_LOGS_IN_RANGE_SUCCESS userId=abc123, startDate=2025-10-01, endDate=2025-10-31, startEpochDays=20358, endEpochDays=20388, path=users/abc123/dailyLogs, logsCount=15
```

## Example 6: Recent Logs Query

```
[FirestoreService.iOS] GET_RECENT_LOGS_START userId=abc123, limit=10, path=users/abc123/dailyLogs

[FirestoreService.iOS] GET_RECENT_LOGS_SUCCESS userId=abc123, limit=10, path=users/abc123/dailyLogs, logsCount=10, startDate=2025-07-16, endDate=2025-10-14
```

## Example 7: Delete Operation

```
[FirestoreService.iOS] DELETE_DAILY_LOG_START userId=abc123, logId=2025-10-14, path=users/abc123/dailyLogs/2025-10-14

[FirestoreService.iOS] DELETE_DAILY_LOG_SUCCESS userId=abc123, logId=2025-10-14, path=users/abc123/dailyLogs/2025-10-14
```

## Example 8: Update Operation

```
[FirestoreService.iOS] UPDATE_DAILY_LOG_START userId=abc123, logId=2025-10-14, path=users/abc123/dailyLogs/2025-10-14, dateEpochDays=20371, updatedAt=1729000100

[FirestoreService.iOS] SAVE_DAILY_LOG_START userId=abc123, logId=2025-10-14, path=users/abc123/dailyLogs/2025-10-14, dateEpochDays=20371, createdAt=1729000000, updatedAt=1729000100

[FirestoreService.iOS] SAVE_DAILY_LOG_SUCCESS userId=abc123, logId=2025-10-14, path=users/abc123/dailyLogs/2025-10-14, dateEpochDays=20371, updatedAt=1729000100

[FirestoreService.iOS] UPDATE_DAILY_LOG_SUCCESS userId=abc123, logId=2025-10-14, path=users/abc123/dailyLogs/2025-10-14, dateEpochDays=20371, updatedAt=1729000100
```

## Example 9: Authentication Error

```
[FirestoreService.iOS] GET_DAILY_LOG_START userId=abc123, logId=2025-10-14, path=users/abc123/dailyLogs/2025-10-14

[FirestoreService.iOS] GET_DAILY_LOG_ERROR userId=abc123, logId=2025-10-14, path=users/abc123/dailyLogs/2025-10-14, error=User not authenticated, errorType=AuthenticationError
```

## Example 10: Complete Save-Read Cycle

```
# User saves a log
[FirestoreService.iOS] SAVE_DAILY_LOG_START userId=abc123, logId=2025-10-14, path=users/abc123/dailyLogs/2025-10-14, dateEpochDays=20371, createdAt=1729000000, updatedAt=1729000000

[FirestoreService.iOS] SAVE_DAILY_LOG_SUCCESS userId=abc123, logId=2025-10-14, path=users/abc123/dailyLogs/2025-10-14, dateEpochDays=20371, updatedAt=1729000000

# User navigates away and back, triggering a read
[FirestoreService.iOS] GET_DAILY_LOG_BY_DATE_START userId=abc123, date=2025-10-14, dateEpochDays=20371, path=users/abc123/dailyLogs

[FirestoreService.iOS] GET_DAILY_LOG_BY_DATE_SUCCESS userId=abc123, date=2025-10-14, dateEpochDays=20371, logId=2025-10-14, path=users/abc123/dailyLogs, updatedAt=1729000000
```

## Debugging Tips

### 1. Trace a Specific Log
Filter by logId to see all operations on a specific daily log:
```bash
# In Xcode console, filter for:
2025-10-14
```

### 2. Monitor a Specific User
Filter by userId to see all operations for a user:
```bash
# In Xcode console, filter for:
userId=abc123
```

### 3. Find Errors Only
Filter for error operations:
```bash
# In Xcode console, filter for:
_ERROR
```

### 4. Track Sync Issues
Compare paths between iOS and Android logs to ensure consistency:
```
iOS:     path=users/abc123/dailyLogs/2025-10-14
Android: path=users/abc123/dailyLogs/2025-10-14  ✅ Match!
```

### 5. Monitor Performance
Calculate operation duration by comparing timestamps:
```
START: 14:30:00.123
SUCCESS: 14:30:00.456
Duration: 333ms
```

## Log Levels

All logs use the `platformLogDebug` function, which on iOS maps to `NSLog`:
- **Development**: Visible in Xcode console
- **TestFlight**: Visible in device logs (Console.app)
- **Production**: Can be captured by crash reporting tools

## Filtering in Xcode

To filter logs in Xcode console:
1. Open the console pane (⇧⌘C)
2. Use the search box at the bottom
3. Filter by:
   - Tag: `FirestoreService.iOS`
   - Operation: `SAVE_DAILY_LOG_START`
   - User: `userId=abc123`
   - Error: `_ERROR`

## Integration with Firebase Console

When debugging sync issues:
1. Check iOS logs for the operation
2. Note the `path` value
3. Open Firebase Console
4. Navigate to Firestore
5. Follow the path to verify the document exists
6. Compare timestamps: `updatedAt` in logs vs. Firebase Console
