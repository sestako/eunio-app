package com.eunio.healthapp.network

import kotlin.time.Duration.Companion.seconds

/**
 * Example implementations showing how to use retry logic
 * These are reference implementations - integrate into actual services as needed
 */

/**
 * Example 1: Simple retry wrapper for any suspend function
 */
suspend fun <T> retryableOperation(
    operation: suspend () -> T
): T {
    return withRetry(
        policy = RetryPolicy.DEFAULT,
        onRetry = { attempt, error, delay ->
            println("Retrying operation (attempt ${attempt + 1}) in ${delay.inWholeSeconds}s due to: ${error.message}")
        }
    ) {
        operation()
    }
}

/**
 * Example 2: Auth operations with aggressive retry
 */
class RetryableAuthExample {
    suspend fun signInWithRetry(email: String, password: String): Result<String> {
        return try {
            val userId = withRetry(
                policy = RetryPolicy.AGGRESSIVE, // 5 retries, faster
                onRetry = { attempt, error, _ ->
                    println("Sign in retry attempt ${attempt + 1}: ${error.message}")
                }
            ) {
                // Your actual Firebase auth call here
                performSignIn(email, password)
            }
            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun performSignIn(email: String, password: String): String {
        // Placeholder - replace with actual Firebase call
        throw NotImplementedError("Replace with actual Firebase auth call")
    }
}

/**
 * Example 3: Firestore operations with custom retry logic
 */
class RetryableFirestoreExample {
    suspend fun getUserData(userId: String): UserData? {
        return withRetry(
            policy = RetryPolicy(
                maxAttempts = 3,
                initialDelay = 1.seconds,
                maxDelay = 10.seconds,
                multiplier = 2.0
            ),
            shouldRetry = { error ->
                // Custom retry logic - only retry on specific errors
                val message = error.message?.lowercase() ?: ""
                message.contains("network") || 
                message.contains("timeout") ||
                message.contains("unavailable")
            }
        ) {
            // Your actual Firestore call here
            fetchUserData(userId)
        }
    }
    
    suspend fun saveUserData(data: UserData) {
        withRetry(policy = RetryPolicy.AGGRESSIVE) {
            // Your actual Firestore write here
            writeUserData(data)
        }
    }
    
    private suspend fun fetchUserData(userId: String): UserData? {
        // Placeholder
        throw NotImplementedError("Replace with actual Firestore call")
    }
    
    private suspend fun writeUserData(data: UserData) {
        // Placeholder
        throw NotImplementedError("Replace with actual Firestore call")
    }
}

/**
 * Example 4: Network-aware retry
 */
class NetworkAwareRetryExample(
    private val networkMonitor: NetworkMonitor
) {
    suspend fun fetchDataWithNetworkCheck(): String {
        return withRetry(
            shouldRetry = { error ->
                // Only retry if we have network AND error is retryable
                val hasNetwork = networkMonitor.isConnected.value
                val isRetryable = isRetryableError(error)
                hasNetwork && isRetryable
            },
            onRetry = { attempt, error, delay ->
                val networkStatus = if (networkMonitor.isConnected.value) "online" else "offline"
                println("Retry attempt ${attempt + 1} ($networkStatus) in ${delay.inWholeSeconds}s")
            }
        ) {
            performNetworkRequest()
        }
    }
    
    private suspend fun performNetworkRequest(): String {
        // Placeholder
        throw NotImplementedError("Replace with actual network call")
    }
}

/**
 * Example 5: Using RetryResult for non-throwing operations
 */
class SafeRetryExample {
    suspend fun fetchDataSafely(): UserData? {
        return when (val result = withRetryResult { fetchData() }) {
            is RetryResult.Success -> {
                println("✅ Success on attempt ${result.attemptNumber + 1}")
                result.value
            }
            is RetryResult.Failure -> {
                println("❌ Failed after ${result.attemptNumber + 1} attempts")
                println("Error: ${result.error.message}")
                println("Was retryable: ${result.isRetryable}")
                null
            }
        }
    }
    
    private suspend fun fetchData(): UserData {
        // Placeholder
        throw NotImplementedError("Replace with actual data fetch")
    }
}

// Placeholder data classes for examples
data class UserData(val id: String, val name: String)
