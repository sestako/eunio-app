# Save Issue Diagnosis

## Problem
User reports "Failed to save..." error when clicking the Save button in the Daily Logging screen.

## Investigation

### Code Flow Analysis

1. **User clicks Save** → `DailyLoggingViewModel.saveLog()`
2. **ViewModel validates** → Checks BBT range, selected date
3. **ViewModel calls UseCase** → `SaveDailyLogUseCase(dailyLog)`
4. **UseCase validates** → Checks date not in future, BBT range, notes length, etc.
5. **UseCase calls Repository** → `LogRepositoryImpl.saveDailyLog(log)`
6. **Repository saves locally** → `dailyLogDao.insertOrUpdate(updatedLog)`
7. **Repository syncs remotely** → `firestoreService.saveDailyLog(updatedLog)`

### Potential Failure Points

#### 1. User ID Issue
```kotlin
// In DailyLoggingViewModel.saveLog()
userId = "current_user", // This should come from auth state
```

**Problem:** The userId is hardcoded as "current_user" instead of getting the actual authenticated user ID.

**Impact:** 
- If Firebase requires authentication, this will fail
- If the user is not logged in, this will fail
- The hardcoded ID doesn't match any real user

#### 2. Firebase Authentication
The Firebase warnings in the console show:
```
12.3.0 - [FirebaseAuth][I-AUT000018] Error getting App Check token
```

**Problem:** Firebase AppCheck is failing (expected in simulator), but this might also indicate auth issues.

#### 3. Database Initialization
The DAOs are provided via:
```kotlin
single { 
    get<DatabaseManager>().getDailyLogDao()
}
```

**Potential Issue:** If DatabaseManager fails to initialize, the DAO will be null or throw an exception.

### Most Likely Cause

**The userId is hardcoded as "current_user"** which is not a valid authenticated user. When the repository tries to save to Firebase, it fails because:

1. No user is authenticated
2. The hardcoded "current_user" doesn't exist in Firebase
3. Firebase security rules likely require authentication

## Solution

### Option 1: Get Real User ID from Auth (Recommended)

Modify `DailyLoggingViewModel` to get the actual user ID:

```kotlin
class DailyLoggingViewModel(
    private val getDailyLogUseCase: GetDailyLogUseCase,
    private val saveDailyLogUseCase: SaveDailyLogUseCase,
    private val authManager: AuthManager, // Add this
    dispatcher: kotlinx.coroutines.CoroutineDispatcher = kotlinx.coroutines.Dispatchers.Main
) : BaseViewModel<DailyLoggingUiState>(dispatcher) {
    
    fun saveLog() {
        val state = uiState.value
        val selectedDate = state.selectedDate
        
        if (selectedDate == null) {
            updateState { it.copy(errorMessage = "No date selected") }
            return
        }
        
        viewModelScope.launch {
            // Get current user ID
            val userId = authManager.getCurrentUserId()
            if (userId == null) {
                updateState { it.copy(errorMessage = "Please log in to save data") }
                return@launch
            }
            
            updateState { it.copy(isSaving = true, errorMessage = null) }
            
            val dailyLog = DailyLog(
                id = state.currentLog?.id ?: "log_${selectedDate}_${Clock.System.now().epochSeconds}",
                userId = userId, // Use real user ID
                date = selectedDate,
                // ... rest of the fields
            )
            
            saveDailyLogUseCase(dailyLog)
                .onSuccess { /* ... */ }
                .onError { /* ... */ }
        }
    }
}
```

### Option 2: Allow Offline-Only Mode (Temporary)

Modify `LogRepositoryImpl` to not fail if remote sync fails:

```kotlin
override suspend fun saveDailyLog(log: DailyLog): Result<Unit> {
    return try {
        // Save locally first (offline-first)
        dailyLogDao.insertOrUpdate(updatedLog)
        
        // Attempt to sync to remote (don't fail if this fails)
        try {
            val remoteResult = firestoreService.saveDailyLog(updatedLog)
            if (remoteResult.isSuccess) {
                dailyLogDao.markAsSynced(updatedLog.id)
            }
        } catch (e: Exception) {
            // Log but don't fail - will sync later
            println("Remote sync failed, will retry later: ${e.message}")
        }
        
        Result.success(Unit) // Always succeed if local save works
    } catch (e: Exception) {
        Result.error(errorHandler.handleError(e))
    }
}
```

## Recommended Action

1. **Check if user is logged in** - The app might not have authentication set up properly
2. **Fix the userId** - Get it from AuthManager instead of hardcoding
3. **Add better error messages** - Show specific error (auth vs database vs network)
4. **Test offline mode** - Ensure local save works even if Firebase is unavailable

## Testing Steps

1. Check if Firebase Auth is initialized
2. Check if user is logged in
3. Try saving with airplane mode on (test offline-first)
4. Check Logcat/Console for specific error messages
5. Verify database file is created and writable

