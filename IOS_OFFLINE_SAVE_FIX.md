# iOS Offline Save Issue - Analysis and Fix

## Problem
When saving a daily log on iOS while offline, the app gets stuck showing "Saving..." indefinitely. The save button remains disabled and the user cannot interact with the app.

## Root Cause Analysis

### 1. Save Flow
```
iOS UI (DailyLoggingView)
  ↓
ModernDailyLoggingViewModel.saveLog()
  ↓
Shared DailyLoggingViewModel.saveLog()
  ↓
SaveDailyLogUseCase.invoke()
  ↓
LogRepositoryImpl.saveDailyLog()
  ↓
FirestoreService.saveDailyLog() ← HANGS HERE WHEN OFFLINE
```

### 2. The Issue in LogRepositoryImpl

Looking at `LogRepositoryImpl.saveDailyLog()`:

```kotlin
// 1. Save locally first (✅ WORKS)
dailyLogDao.insertOrUpdate(updatedLog)

// 2. Mark as pending sync (✅ WORKS)
dailyLogDao.updateSyncStatus(updatedLog.id, "PENDING")

// 3. Attempt Firebase sync (❌ HANGS WHEN OFFLINE)
val remoteResult = firestoreService.saveDailyLog(updatedLog)
```

The Firebase call doesn't have a timeout and blocks indefinitely when offline.

### 3. iOS-Specific Behavior

The iOS `ModernDailyLoggingViewModel` waits for the save operation:

```swift
func saveLog() async throws {
    // Delegate to shared Kotlin ViewModel
    await withCheckedContinuation { continuation in
        sharedViewModel.saveLog()
        continuation.resume()
    }
    
    // Wait for save to complete
    try? await Task.sleep(nanoseconds: 500_000_000) // 0.5 seconds
    
    // Check if save was successful
    if let error = errorMessage {
        throw DailyLoggingError.saveFailed(error)
    }
}
```

The problem is that `sharedViewModel.saveLog()` is synchronous but launches a coroutine internally. The continuation resumes immediately, but the UI state (`isSaving`) doesn't update because the coroutine is still waiting for Firebase.

## Solution Options

### Option 1: Add Timeout to Firebase Operations (RECOMMENDED)

Wrap Firebase calls with a timeout:

```kotlin
// In LogRepositoryImpl.saveDailyLog()
val remoteResult = withTimeoutOrNull(5000) { // 5 second timeout
    firestoreService.saveDailyLog(updatedLog)
}

if (remoteResult?.isSuccess == true) {
    dailyLogDao.markAsSynced(updatedLog.id)
} else {
    // Keep as pending - will retry later
}
```

### Option 2: Check Network Connectivity First

Check if online before attempting Firebase sync:

```kotlin
// In LogRepositoryImpl.saveDailyLog()
if (networkConnectivity.isConnected()) {
    val remoteResult = firestoreService.saveDailyLog(updatedLog)
    // ... handle result
} else {
    // Skip Firebase sync, keep as pending
    // Return success immediately
}
```

### Option 3: Make Firebase Call Non-Blocking

Launch Firebase sync in background without waiting:

```kotlin
// In LogRepositoryImpl.saveDailyLog()
// Save locally first
dailyLogDao.insertOrUpdate(updatedLog)
dailyLogDao.updateSyncStatus(updatedLog.id, "PENDING")

// Launch background sync (don't wait)
viewModelScope.launch {
    val remoteResult = firestoreService.saveDailyLog(updatedLog)
    if (remoteResult.isSuccess) {
        dailyLogDao.markAsSynced(updatedLog.id)
    }
}

// Return success immediately
return Result.success(Unit)
```

## Recommended Fix

Implement **Option 1 + Option 2** combination:

1. Check network connectivity first
2. If offline, skip Firebase and return immediately
3. If online, add timeout to Firebase call
4. Always return success if local save succeeds

## Implementation

### File: `shared/src/commonMain/kotlin/com/eunio/healthapp/data/repository/LogRepositoryImpl.kt`

```kotlin
override suspend fun saveDailyLog(log: DailyLog): Result<Unit> {
    val startTime = Clock.System.now()
    
    platformLogDebug("LogRepository", "💾 saveDailyLog() called - userId: ${log.userId}, logId: ${log.id}")
    
    return try {
        // Validate log data
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        if (log.date > today) {
            return Result.error(errorHandler.createValidationError("Cannot log data for future dates", "date"))
        }
        
        if (log.bbt != null && (log.bbt < 95.0 || log.bbt > 105.0)) {
            return Result.error(errorHandler.createValidationError("BBT must be between 95°F and 105°F", "bbt"))
        }
        
        // Update timestamp
        val updatedLog = log.copy(updatedAt = Clock.System.now())
        
        // 1. Save locally first (offline-first architecture)
        dailyLogDao.insertOrUpdate(updatedLog)
        
        // 2. Mark as pending sync
        dailyLogDao.updateSyncStatus(updatedLog.id, "PENDING")
        
        // 3. Attempt Firebase sync ONLY if online
        if (networkConnectivity.isConnected()) {
            platformLogDebug("LogRepository", "🌐 Device is online - attempting Firebase sync")
            
            // Add timeout to prevent hanging
            val remoteResult = withTimeoutOrNull(5000) { // 5 second timeout
                firestoreService.saveDailyLog(updatedLog)
            }
            
            val latencyMs = (Clock.System.now() - startTime).inWholeMilliseconds
            
            if (remoteResult?.isSuccess == true) {
                // Mark as synced on success
                dailyLogDao.markAsSynced(updatedLog.id)
                platformLogDebug("LogRepository", "✅ Firebase sync successful - latency: ${latencyMs}ms")
            } else {
                // Keep pending on failure or timeout
                platformLogDebug("LogRepository", "⚠️ Firebase sync failed or timed out - will retry later")
            }
        } else {
            platformLogDebug("LogRepository", "📴 Device is offline - skipping Firebase sync")
        }
        
        // Always return success if local save succeeded
        Result.success(Unit)
        
    } catch (e: Exception) {
        platformLogDebug("LogRepository", "❌ Save failed: ${e.message}")
        Result.error(errorHandler.handleError(e))
    }
}
```

### Additional Fix: Update iOS ViewModel State Observation

The iOS `ModernDailyLoggingViewModel` uses a timer to poll state. This is inefficient and can cause delays. However, for a quick fix, we can reduce the polling interval:

```swift
// In ModernDailyLoggingViewModel.swift
private func setupStateObservation() {
    Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { [weak self] _ in
        // Changed from 0.5 to 0.1 seconds for faster UI updates
        guard let self = self else { return }
        DispatchQueue.main.async {
            let currentState = self.sharedViewModel.uiState.value as! DailyLoggingUiState
            self.updateFromSharedState(currentState)
        }
    }
}
```

## Testing

### Test Scenario 1: Save While Offline
1. Enable Airplane Mode on iOS device
2. Open Daily Logging screen
3. Enter some data
4. Click Save
5. **Expected**: Save completes within 1 second, shows success message
6. **Expected**: Data is saved locally and visible after app restart

### Test Scenario 2: Save While Online
1. Ensure device has network connectivity
2. Open Daily Logging screen
3. Enter some data
4. Click Save
5. **Expected**: Save completes within 5 seconds, shows success message
6. **Expected**: Data is synced to Firebase and visible on other devices

### Test Scenario 3: Save with Slow Network
1. Enable network throttling (slow 3G)
2. Open Daily Logging screen
3. Enter some data
4. Click Save
5. **Expected**: Save completes within 5 seconds (timeout), shows success message
6. **Expected**: Data is saved locally, will sync in background

## Files to Modify

1. `shared/src/commonMain/kotlin/com/eunio/healthapp/data/repository/LogRepositoryImpl.kt`
   - Add network connectivity check
   - Add timeout to Firebase calls
   - Always return success if local save succeeds

2. `iosApp/iosApp/ViewModels/ModernDailyLoggingViewModel.swift` (Optional)
   - Reduce state polling interval from 0.5s to 0.1s

## Priority

**HIGH** - This is a critical bug that prevents users from saving data offline, which is a core feature of the app.

## Estimated Effort

- Implementation: 30 minutes
- Testing: 30 minutes
- Total: 1 hour
