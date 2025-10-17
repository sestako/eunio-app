# Firebase Error Mapping for iOS

## Overview

The `FirebaseErrorMapper` provides consistent error handling for Firebase iOS SDK operations by mapping Firebase error codes to the application's `AppError` hierarchy. This ensures that errors are handled uniformly across the iOS platform and provides meaningful error messages to users.

## Error Code Mappings

### Network Errors

| Firebase Error Code | Error Name | Mapped To | User Message |
|---------------------|------------|-----------|--------------|
| 14 | UNAVAILABLE | `AppError.NetworkError` | "No internet connection. Please check your network and try again." |
| 4 | DEADLINE_EXCEEDED | `AppError.NetworkError` | "Request timed out. Please try again." |
| 8 | RESOURCE_EXHAUSTED | `AppError.NetworkError` | "Service temporarily unavailable. Please try again later." |

### Authentication Errors

| Firebase Error Code | Error Name | Mapped To | User Message |
|---------------------|------------|-----------|--------------|
| 16 | UNAUTHENTICATED | `AppError.AuthenticationError` | "Authentication required. Please sign in again." |

### Permission Errors

| Firebase Error Code | Error Name | Mapped To | User Message |
|---------------------|------------|-----------|--------------|
| 7 | PERMISSION_DENIED | `AppError.PermissionError` | "Access denied. You don't have permission to perform this action." |

### Data Sync Errors

| Firebase Error Code | Error Name | Mapped To | User Message |
|---------------------|------------|-----------|--------------|
| 5 | NOT_FOUND | `AppError.DataSyncError` | "Document not found." |
| 6 | ALREADY_EXISTS | `AppError.DataSyncError` | "Document already exists." |
| 10 | ABORTED | `AppError.DataSyncError` | "Operation aborted. Please try again." |
| 1 | CANCELLED | `AppError.DataSyncError` | "Operation cancelled." |

### Validation Errors

| Firebase Error Code | Error Name | Mapped To | User Message |
|---------------------|------------|-----------|--------------|
| 3 | INVALID_ARGUMENT | `AppError.ValidationError` | "Invalid data: [error details]" |
| 9 | FAILED_PRECONDITION | `AppError.ValidationError` | "Operation failed precondition check: [error details]" |

### Database Errors

| Firebase Error Code | Error Name | Mapped To | User Message |
|---------------------|------------|-----------|--------------|
| 13 | INTERNAL | `AppError.DatabaseError` | "Internal server error. Please try again later." |
| 15 | DATA_LOSS | `AppError.DatabaseError` | "Internal server error. Please try again later." |

### Unknown Errors

| Firebase Error Code | Error Name | Mapped To | User Message |
|---------------------|------------|-----------|--------------|
| 12 | UNIMPLEMENTED | `AppError.UnknownError` | "Operation not supported." |
| 2 | UNKNOWN | `AppError.UnknownError` | "Firebase error: [error details]" |
| Other | - | `AppError.UnknownError` | "Firebase error: [error details] (code: [code])" |

## Usage

### Basic Error Mapping

```kotlin
import com.eunio.healthapp.data.remote.firebase.FirebaseErrorMapper
import platform.Foundation.NSError

// In a suspend function with error handling
try {
    // Firebase operation
} catch (e: Exception) {
    val appError = if (e is NSError) {
        FirebaseErrorMapper.mapError(e as NSError, "saveDailyLog")
    } else {
        FirebaseErrorMapper.mapThrowable(e, "saveDailyLog")
    }
    
    // Handle the mapped error
    throw appError
}
```

### With Logging

```kotlin
import com.eunio.healthapp.data.remote.firebase.FirebaseErrorMapper

try {
    // Firebase operation
} catch (e: Exception) {
    val appError = if (e is NSError) {
        FirebaseErrorMapper.mapError(e as NSError, "saveDailyLog")
    } else {
        FirebaseErrorMapper.mapThrowable(e, "saveDailyLog")
    }
    
    // Log the error with context
    FirebaseErrorMapper.logError(
        error = appError,
        operation = "saveDailyLog",
        additionalContext = mapOf(
            "userId" to userId,
            "logId" to logId
        )
    )
    
    throw appError
}
```

### Mapping Generic Throwables

The mapper also handles non-Firebase errors:

```kotlin
// IllegalArgumentException -> ValidationError
val error1 = FirebaseErrorMapper.mapThrowable(
    IllegalArgumentException("Invalid input"),
    "validation"
)

// IllegalStateException -> DataSyncError
val error2 = FirebaseErrorMapper.mapThrowable(
    IllegalStateException("Invalid state"),
    "sync"
)

// AppError -> returned unchanged
val error3 = FirebaseErrorMapper.mapThrowable(
    AppError.NetworkError("Network error")
)

// Other exceptions -> UnknownError
val error4 = FirebaseErrorMapper.mapThrowable(
    RuntimeException("Something went wrong"),
    "operation"
)
```

## Structured Logging

All error mappings are automatically logged using `StructuredLogger` with the following information:

- **Error Code**: The Firebase error code
- **Error Message**: The localized error description
- **Operation**: The operation that failed (if provided)
- **Domain**: The error domain (typically "FIRFirestoreErrorDomain")

Example log output:
```
[FirebaseErrorMapper] ERROR_MAPPING errorCode=14, errorMessage=Network unavailable, operation=saveDailyLog, domain=FIRFirestoreErrorDomain
```

When an error is explicitly logged using `logError()`:
```
[FirebaseErrorMapper] ERROR_LOGGED operation=saveDailyLog, errorType=NetworkError, errorMessage=No internet connection during saveDailyLog, userId=user123, logId=log456
```

## Integration with FirebaseNativeBridge

The `FirebaseNativeBridge` automatically uses the error mapper for all operations:

```kotlin
actual suspend fun saveDailyLog(
    userId: String,
    logId: String,
    data: Map<String, Any>
): Result<Unit> {
    return suspendCancellableCoroutine { continuation ->
        try {
            // Swift bridge call
            bridge.saveDailyLog(userId, logId, data) { error in
                if (error != null) {
                    val appError = FirebaseErrorMapper.mapError(error, "saveDailyLog")
                    FirebaseErrorMapper.logError(
                        error = appError,
                        operation = "saveDailyLog",
                        additionalContext = mapOf(
                            "userId" to userId,
                            "logId" to logId
                        )
                    )
                    continuation.resumeWithException(appError)
                } else {
                    continuation.resume(Result.success(Unit))
                }
            }
        } catch (e: Exception) {
            val appError = FirebaseErrorMapper.mapThrowable(e, "saveDailyLog")
            continuation.resumeWithException(appError)
        }
    }
}
```

## Testing

The error mapper includes comprehensive unit tests covering all error code mappings. See `FirebaseErrorMapperTest.kt` for examples.

## Requirements Satisfied

This implementation satisfies the following requirements from the iOS Firebase Sync Fix spec:

- **Requirement 1.4**: Error handling for Firebase operations
- **Requirement 4.3**: Network error handling with appropriate messages
- **Requirement 4.4**: Authentication error handling
- **Requirement 4.5**: Generic error handling with descriptive messages
- **Requirement 4.6**: Error logging to developer console for debugging

## Future Enhancements

- Add retry logic for transient errors (UNAVAILABLE, DEADLINE_EXCEEDED)
- Implement exponential backoff for network errors
- Add error analytics tracking
- Support for custom error messages based on user locale
