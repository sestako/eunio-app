# Task 5.1 - User ID Issue Discovered

## Problem
The iOS save operation is failing because the Kotlin code cannot access the Firebase user ID.

### Root Cause
1. **Swift layer**: Firebase authentication happens in Swift, user ID is stored in UserDefaults
2. **Kotlin layer**: The iOS AuthService uses a mock implementation that doesn't read from UserDefaults
3. **Result**: When trying to save a daily log, the Kotlin code doesn't have the correct user ID

### Evidence
- User creates log for October 15, 2025 with note "ios test"
- Log does NOT appear in Firebase
- Firebase only shows old logs from October 25, 2025 (date: 20373)
- Swift code stores user ID in UserDefaults with key "currentUserId"
- Kotlin iOS code doesn't read from UserDefaults

## Solution Required
The Kotlin iOS code needs to read the Firebase user ID from UserDefaults (or get it via a bridge).

### Option 1: Read from UserDefaults (Recommended)
Update `IOSAuthService.kt` to read the user ID from NSUserDefaults using Kotlin/Native platform APIs.

### Option 2: Add to Firebase Bridge
Add a method to `FirebaseIOSBridge.swift` to return the current user ID, and call it from Kotlin.

## Next Steps
1. Implement user ID retrieval in Kotlin iOS code
2. Test that the correct user ID is being used when saving logs
3. Verify logs appear in Firebase with the correct user ID path
