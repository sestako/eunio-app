# Save Feedback Fix

## Problem
When users clicked the "Save" button on the Daily Logging screen, there was no visual feedback to confirm whether the log was saved successfully or not. Users couldn't tell if their data was actually saved.

## Root Cause
The `DailyLoggingViewModel` was setting `successMessage` and `errorMessage` in the UI state, but the screen was only announcing these messages for accessibility (screen readers) without showing any visual feedback like a Snackbar.

## Solution
Added Snackbar notifications to provide visual feedback:

1. **Created SnackbarHostState** to manage snackbar display
2. **Added LaunchedEffect for success messages** - Shows a short snackbar when save succeeds
3. **Added LaunchedEffect for error messages** - Shows a longer snackbar when save fails
4. **Added SnackbarHost to Scaffold** - Displays the snackbar at the bottom of the screen

### Code Changes
```kotlin
val snackbarHostState = remember { SnackbarHostState() }

// Show snackbar for success messages
LaunchedEffect(uiState.successMessage) {
    uiState.successMessage?.let { message ->
        snackbarHostState.showSnackbar(
            message = message,
            duration = SnackbarDuration.Short
        )
        viewModel.clearMessages()
    }
}

// Show snackbar for error messages
LaunchedEffect(uiState.errorMessage) {
    uiState.errorMessage?.let { error ->
        snackbarHostState.showSnackbar(
            message = error,
            duration = SnackbarDuration.Long
        )
    }
}

Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    // ... rest of scaffold
)
```

## User Experience
Now when users click "Save":
1. The button shows a loading spinner while saving
2. On success: A green snackbar appears at the bottom saying "Log saved successfully"
3. On error: A red snackbar appears with the error message
4. Screen readers also announce the messages for accessibility

## Files Changed
- `androidApp/src/androidMain/kotlin/com/eunio/healthapp/android/ui/logging/DailyLoggingScreen.kt`

## Testing
1. Rebuild the Android app
2. Navigate to Daily Logging screen
3. Fill in some data (e.g., temperature, mood, symptoms)
4. Click "Save"
5. You should see a snackbar at the bottom confirming "Log saved successfully"
