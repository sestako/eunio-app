# Retry Logic with Exponential Backoff - Implementation Guide

## ✅ Completed

### Core Components
- ✅ `RetryPolicy.kt` - Configuration for retry behavior
- ✅ `RetryExtensions.kt` - `withRetry()` function and error categorization
- ✅ Both Android and iOS builds successful

## 📚 How to Use

### Basic Usage

```kotlin
import com.eunio.healthapp.network.withRetry
import com.eunio.healthapp.network.RetryPolicy

// Simple retry with default policy (3 retries, exponential backoff)
suspend fun fetchData(): Data {
    return withRetry {
        // Your operation that might fail
        firestore.collection("data").document("id").get()
    }
}
```

### Custom Retry Policy

```kotlin
// Aggressive retry for critical operations
suspend fun criticalOperation(): Result {
    return withRetry(policy = RetryPolicy.AGGRESSIVE) {
        // 5 retries, faster initial retry
        performCriticalOperation()
    }
}

// Conservative retry for non-critical operations
suspend fun nonCriticalOperation(): Result {
    return withRetry(policy = RetryPolicy.CONSERVATIVE) {
        // 2 retries, longer delays
        performNonCriticalOperation()
    }
}

// Custom policy
suspend fun customOperation(): Result {
    return withRetry(
        policy = RetryPolicy(
            maxAttempts = 4,
            initialDelay = 2.seconds,
            maxDelay = 60.seconds,
            multiplier = 2.5,
            jitter = true
        )
    ) {
        performOperation()
    }
}
```

### Custom Retry Logic

```kotlin
// Custom shouldRetry function
suspend fun fetchWithCustomRetry(): Data {
    return withRetry(
        shouldRetry = { error ->
            // Only retry on specific errors
            error is NetworkException || error is TimeoutException
        }
    ) {
        fetchData()
    }
}

// With retry callback
suspend fun fetchWithCallback(): Data {
    return withRetry(
        onRetry = { attempt, error, delay ->
            println("Retry attempt $attempt after ${delay.inWholeSeconds}s due to: ${error.message}")
            // Could log to analytics, show UI feedback, etc.
        }
    ) {
        fetchData()
    }
}
```

### Using RetryResult (Non-Throwing)

```kotlin
import com.eunio.healthapp.network.withRetryResult
import com.eunio.healthapp.network.RetryResult

suspend fun fetchDataSafely(): Data? {
    return when (val result = withRetryResult { fetchData() }) {
        is RetryResult.Success -> {
            println("Success on attempt ${result.attemptNumber}")
            result.value
        }
        is RetryResult.Failure -> {
            println("Failed after ${result.attemptNumber} attempts: ${result.error.message}")
            null
        }
    }
}
```

## 🔧 Integration Examples

### Example 1: Auth Service

```kotlin
class FirebaseAuthService : AuthService {
    override suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            val user = withRetry(policy = RetryPolicy.AGGRESSIVE) {
                // Firebase sign in with retry
                firebaseAuth.signInWithEmailAndPassword(email, password).await()
            }
            Result.Success(user.toUser())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
```

### Example 2: Firestore Operations

```kotlin
class UserProfileService {
    suspend fun getUserProfile(userId: String): UserProfile? {
        return withRetry(
            policy = RetryPolicy.DEFAULT,
            onRetry = { attempt, error, delay ->
                println("Retrying getUserProfile (attempt $attempt) in ${delay.inWholeSeconds}s")
            }
        ) {
            firestore.collection("users")
                .document(userId)
                .get()
                .await()
                .toObject(UserProfile::class.java)
        }
    }
    
    suspend fun updateUserProfile(profile: UserProfile) {
        withRetry(policy = RetryPolicy.AGGRESSIVE) {
            firestore.collection("users")
                .document(profile.userId)
                .set(profile)
                .await()
        }
    }
}
```

### Example 3: Network-Aware Retry

```kotlin
class SmartRetryService(
    private val networkMonitor: NetworkMonitor
) {
    suspend fun fetchData(): Data {
        return withRetry(
            shouldRetry = { error ->
                // Only retry if we have network connection
                val isRetryable = isRetryableError(error)
                val hasNetwork = networkMonitor.isConnected.value
                isRetryable && hasNetwork
            }
        ) {
            performNetworkRequest()
        }
    }
}
```

## 📊 Retry Behavior

### Default Policy
- **Max Attempts**: 3 retries (4 total attempts)
- **Initial Delay**: 1 second
- **Max Delay**: 30 seconds
- **Multiplier**: 2.0 (doubles each time)
- **Jitter**: Enabled (adds 0-25% random variation)

**Delay Sequence:**
1. First retry: ~1s (1s + jitter)
2. Second retry: ~2s (2s + jitter)
3. Third retry: ~4s (4s + jitter)

### Aggressive Policy
- **Max Attempts**: 5 retries
- **Initial Delay**: 500ms
- **Multiplier**: 1.5

**Delay Sequence:**
1. ~500ms
2. ~750ms
3. ~1.1s
4. ~1.7s
5. ~2.5s

### Conservative Policy
- **Max Attempts**: 2 retries
- **Initial Delay**: 2 seconds
- **Multiplier**: 3.0

**Delay Sequence:**
1. ~2s
2. ~6s

## 🎯 Retryable Errors

The following errors are automatically retried:

### Network Errors
- Connection errors
- Socket errors
- Unreachable errors

### Timeout Errors
- Request timeout
- Operation timeout

### Server Errors (5xx)
- Internal server error (500)
- Service unavailable (503)
- Bad gateway (502)
- Gateway timeout (504)

### Rate Limiting
- Too many requests (429)
- Quota exceeded
- Rate limit errors

### Firebase Specific
- UNAVAILABLE
- DEADLINE_EXCEEDED
- RESOURCE_EXHAUSTED

## ⚠️ Non-Retryable Errors

These errors will NOT be retried:

- Authentication errors (401, 403)
- Not found errors (404)
- Bad request errors (400)
- Validation errors
- Cancellation exceptions
- Permission denied errors

## 🧪 Testing

### Test Retry Logic

```kotlin
@Test
fun testRetryWithSuccess() = runTest {
    var attempts = 0
    val result = withRetry(policy = RetryPolicy(maxAttempts = 3)) {
        attempts++
        if (attempts < 3) {
            throw IOException("Network error")
        }
        "Success"
    }
    assertEquals("Success", result)
    assertEquals(3, attempts)
}

@Test
fun testRetryExhaustion() = runTest {
    var attempts = 0
    assertFailsWith<IOException> {
        withRetry(policy = RetryPolicy(maxAttempts = 2)) {
            attempts++
            throw IOException("Persistent error")
        }
    }
    assertEquals(3, attempts) // Initial + 2 retries
}
```

## 📈 Monitoring

Add logging to track retry behavior:

```kotlin
suspend fun monitoredOperation(): Data {
    return withRetry(
        onRetry = { attempt, error, delay ->
            // Log to analytics
            analytics.logEvent("operation_retry", mapOf(
                "attempt" to attempt,
                "error_type" to categorizeError(error).name,
                "delay_ms" to delay.inWholeMilliseconds
            ))
        }
    ) {
        performOperation()
    }
}
```

## 🚀 Next Steps

1. **Integrate with existing services**:
   - Wrap Firebase Auth operations
   - Wrap Firestore read/write operations
   - Wrap Cloud Functions calls

2. **Add monitoring**:
   - Log retry attempts to Firebase Analytics
   - Track retry success/failure rates
   - Monitor average retry delays

3. **Combine with network monitoring**:
   - Only retry when network is available
   - Show UI feedback during retries
   - Queue operations when offline

4. **Test edge cases**:
   - Rapid network on/off transitions
   - Server rate limiting
   - Concurrent retry operations

## 📝 Best Practices

1. **Choose appropriate policy**:
   - Use AGGRESSIVE for critical user-facing operations
   - Use DEFAULT for most operations
   - Use CONSERVATIVE for background operations
   - Use NONE for operations that should fail fast

2. **Provide user feedback**:
   - Show loading indicators during retries
   - Display retry count for long operations
   - Allow users to cancel retry operations

3. **Combine with offline queue**:
   - Retry logic handles transient failures
   - Offline queue handles extended offline periods
   - Together they provide robust reliability

4. **Monitor and adjust**:
   - Track retry success rates
   - Adjust policies based on real-world data
   - Consider network conditions in retry logic

5. **Don't retry everything**:
   - Authentication errors should not be retried
   - Validation errors should not be retried
   - User cancellations should not be retried
