# iOS Save Persistence Issue - Diagnosis and Fix

## Problem Description

**Issue:** Data saved on iOS for October 10, 2025 disappears when navigating to October 11 and back to October 10.

**User Report:**
> "i saved log, switch to 11-10-2025 and back to 10.10.2025. Data are not saved on 10-10-2025"

## Root Cause Analysis

### Architecture Mismatch

The iOS app has a **dual-path architecture** that causes data inconsistency:

1. **Save Path:** Uses `SwiftDailyLogService` (direct Firebase)
   ```swift
   // In ModernDailyLoggingViewModel.saveLog()
   try await firebaseService.createLog(log: logData)
   ```

2. **Load Path:** Uses shared Kotlin `DailyLoggingViewModel` → `LogRepository` → local cache + Firebase
   ```swift
   // In ModernDailyLoggingViewModel.selectDate()
   sharedViewModel.selectDate(date: kotlinDate)
   // This calls loadLogForSelectedDate() in Kotlin
   ```

### The Problem

1. **When you save:**
   - iOS saves directly to Firebase using `SwiftDailyLogService`
   - Data goes to Firebase Firestore
   - Local Kotlin cache is **NOT updated**

2. **When you navigate away and back:**
   - iOS calls `sharedViewModel.selectDate()`
   - Kotlin ViewModel calls `loadLogForSelectedDate()`
   - Kotlin repository checks **local cache first**
   - Local cache is empty (because save bypassed it)
   - Repository tries to fetch from Firebase
   - **But there's a timing/sync issue** - data might not be retrieved correctly

### Key Code Locations

**iOS Save (bypasses Kotlin):**
```swift
// File: iosApp/iosApp/ViewModels/ModernDailyLoggingViewModel.swift
// Line: ~250
func saveLog() async throws {
    // ...
    try await firebaseService.createLog(log: logData)  // Direct Firebase
    // ❌ Does NOT update Kotlin cache
}
```

**Kotlin Load (uses cache):**
```kotlin
// File: shared/src/commonMain/kotlin/com/eunio/healthapp/presentation/viewmodel/DailyLoggingViewModel.kt
// Line: ~40
private fun loadLogForSelectedDate() {
    getDailyLogUseCase(userId, selectedDate)  // Checks cache first
}
```

**Repository Logic:**
```kotlin
// File: shared/src/commonMain/kotlin/com/eunio/healthapp/data/repository/LogRepositoryImpl.kt
// Line: ~50
override suspend fun getDailyLog(userId: String, date: LocalDate): Result<DailyLog?> {
    // Try local cache first
    val localLog = dailyLogDao.getDailyLogByUserIdAndDate(userId, date)
    if (localLog != null) {
        return Result.success(localLog)  // Returns cached data
    }
    // If not in cache, fetch from remote...
}
```

## Why This Happens

The iOS app was designed with two separate data paths:

1. **Original Design:** Use shared Kotlin ViewModel for everything
2. **iOS-Specific Addition:** Added `SwiftDailyLogService` for direct Firebase access
3. **Result:** Save and load use different code paths, causing cache inconsistency

## Solutions

### Solution 1: Use Shared ViewModel for Save (Recommended)

Make iOS use the shared Kotlin ViewModel's `saveLog()` method instead of `SwiftDailyLogService`.

**Advantages:**
- Maintains single source of truth
- Cache stays synchronized
- Consistent behavior across platforms
- Less code duplication

**Implementation:**
```swift
// In ModernDailyLoggingViewModel.swift
func saveLog() async throws {
    isSaving = true
    errorMessage = nil
    
    defer {
        isSaving = false
    }
    
    // Use shared ViewModel instead of SwiftDailyLogService
    await withCheckedContinuation { continuation in
        sharedViewModel.saveLog()
        continuation.resume()
    }
    
    // Wait for save to complete by observing state
    // The shared ViewModel will update isSaving and successMessage
}
```

### Solution 2: Update Cache After iOS Save

After saving with `SwiftDailyLogService`, manually update the Kotlin cache.

**Advantages:**
- Keeps direct Firebase access
- Fixes immediate issue

**Disadvantages:**
- More complex
- Potential for cache inconsistencies
- Requires accessing Kotlin repository from Swift

**Implementation:**
```swift
// After successful save
try await firebaseService.createLog(log: logData)

// Update Kotlin cache
let kotlinLog = logData.toKotlinDailyLog()
// Need to call repository.saveDailyLog() somehow
// This is complex because we need to access Kotlin repository from Swift
```

### Solution 3: Force Reload from Firebase

After navigating back, force a reload from Firebase instead of using cache.

**Advantages:**
- Simple fix
- Always gets latest data

**Disadvantages:**
- Slower (network call every time)
- Defeats purpose of offline-first architecture
- More network usage

**Implementation:**
```kotlin
// In DailyLoggingViewModel.kt
fun selectDate(date: LocalDate) {
    if (date == uiState.value.selectedDate) return
    
    updateState { 
        it.copy(
            selectedDate = date,
            hasUnsavedChanges = false,
            errorMessage = null,
            successMessage = null
        )
    }
    loadLogForSelectedDate(forceRemote = true)  // Force remote fetch
}
```

## Recommended Fix: Solution 1

Use the shared Kotlin ViewModel for saving. This is the cleanest solution that maintains architectural consistency.

### Implementation Steps

1. **Modify `ModernDailyLoggingViewModel.saveLog()`** to use shared ViewModel
2. **Remove direct `SwiftDailyLogService` usage** for saves
3. **Keep `SwiftDailyLogService`** only for iOS-specific features if needed
4. **Test** that data persists after navigation

### Code Changes Required

**File:** `iosApp/iosApp/ViewModels/ModernDailyLoggingViewModel.swift`

**Current Code (Lines ~230-280):**
```swift
func saveLog() async throws {
    // ... validation ...
    
    try await firebaseService.createLog(log: logData)  // ❌ Direct Firebase
    
    successMessage = "Log saved successfully"
    hasUnsavedChanges = false
}
```

**New Code:**
```swift
func saveLog() async throws {
    // Validate before saving
    if !bbt.isEmpty && !isBbtValid {
        throw DailyLoggingError.invalidBBT
    }
    
    isSaving = true
    errorMessage = nil
    
    defer {
        isSaving = false
    }
    
    // Use shared ViewModel - this updates both cache and Firebase
    await withCheckedContinuation { continuation in
        sharedViewModel.saveLog()
        continuation.resume()
    }
    
    // The shared ViewModel will update the state with success/error
    // We observe these changes through updateFromSharedState()
}
```

## Testing the Fix

After implementing the fix:

1. **Save a log on October 10, 2025**
   - Enter test data
   - Click Save
   - Verify success message

2. **Navigate to October 11, 2025**
   - Click next day or select from calendar
   - Verify empty form (no data for Oct 11)

3. **Navigate back to October 10, 2025**
   - Click previous day or select from calendar
   - **Verify data is still present** ✅

4. **Close and reopen app**
   - Navigate to October 10, 2025
   - **Verify data persists** ✅

## Additional Considerations

### Firebase Sync Timing

Even with the fix, there might be a slight delay for Firebase sync. The offline-first architecture handles this:

1. Save to local cache immediately (fast)
2. Sync to Firebase in background (slower)
3. Load from local cache (fast)
4. Background sync from Firebase updates cache

### Error Handling

The shared ViewModel has comprehensive error handling:
- Validation errors
- Network errors
- Firebase errors
- Cache errors

All errors are propagated to the UI through the state.

### Offline Support

Using the shared ViewModel maintains offline support:
- Saves work offline (to local cache)
- Syncs to Firebase when online
- Loads from cache when offline

## Related Files

- `iosApp/iosApp/ViewModels/ModernDailyLoggingViewModel.swift` - iOS ViewModel (needs fix)
- `shared/src/commonMain/kotlin/com/eunio/healthapp/presentation/viewmodel/DailyLoggingViewModel.kt` - Shared ViewModel
- `shared/src/commonMain/kotlin/com/eunio/healthapp/data/repository/LogRepositoryImpl.kt` - Repository with cache logic
- `iosApp/iosApp/Services/SwiftDailyLogService.swift` - Direct Firebase service (currently used for save)

## Conclusion

The issue is caused by iOS using a separate save path that bypasses the Kotlin cache. The recommended fix is to use the shared Kotlin ViewModel for saving, which maintains cache consistency and architectural integrity.

**Status:** Issue diagnosed, solution identified  
**Priority:** High (data loss issue)  
**Complexity:** Low (simple code change)  
**Testing:** Required after fix  
