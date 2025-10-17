package com.eunio.healthapp.network

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RetryLogicTest {
    
    @Test
    fun testRetrySucceedsOnSecondAttempt() = runTest {
        var attempts = 0
        
        val result = withRetry(policy = RetryPolicy(maxAttempts = 3)) {
            attempts++
            if (attempts < 2) {
                throw Exception("Network error")
            }
            "Success"
        }
        
        assertEquals("Success", result)
        assertEquals(2, attempts)
    }
    
    @Test
    fun testRetryExhaustsAllAttempts() = runTest {
        var attempts = 0
        
        assertFailsWith<Exception> {
            withRetry(policy = RetryPolicy(maxAttempts = 2)) {
                attempts++
                throw Exception("Persistent network error")
            }
        }
        
        assertEquals(3, attempts) // Initial + 2 retries
    }
    
    @Test
    fun testRetryWithNonRetryableError() = runTest {
        var attempts = 0
        
        assertFailsWith<Exception> {
            withRetry(
                policy = RetryPolicy(maxAttempts = 3),
                shouldRetry = { error ->
                    // Only retry network errors
                    error.message?.contains("network") == true
                }
            ) {
                attempts++
                throw Exception("Invalid credentials") // Not retryable
            }
        }
        
        assertEquals(1, attempts) // Should not retry
    }
    
    @Test
    fun testRetryPolicyDelayCalculation() {
        val policy = RetryPolicy(
            maxAttempts = 3,
            initialDelay = kotlin.time.Duration.parse("1s"),
            multiplier = 2.0,
            jitter = false // Disable jitter for predictable testing
        )
        
        // First retry: 1s
        val delay0 = policy.delayForAttempt(0)
        assertEquals(1000, delay0.inWholeMilliseconds)
        
        // Second retry: 2s
        val delay1 = policy.delayForAttempt(1)
        assertEquals(2000, delay1.inWholeMilliseconds)
        
        // Third retry: 4s
        val delay2 = policy.delayForAttempt(2)
        assertEquals(4000, delay2.inWholeMilliseconds)
    }
    
    @Test
    fun testRetryPolicyMaxDelay() {
        val policy = RetryPolicy(
            maxAttempts = 10,
            initialDelay = kotlin.time.Duration.parse("1s"),
            maxDelay = kotlin.time.Duration.parse("5s"),
            multiplier = 2.0,
            jitter = false
        )
        
        // Should cap at maxDelay (5s)
        val delay5 = policy.delayForAttempt(5) // Would be 32s without cap
        assertTrue(delay5.inWholeMilliseconds <= 5000)
    }
    
    @Test
    fun testErrorCategorization() {
        // Network errors
        assertTrue(isRetryableError(Exception("Network connection lost")))
        assertTrue(isRetryableError(Exception("Connection timeout")))
        assertTrue(isRetryableError(Exception("Socket error")))
        
        // Server errors
        assertTrue(isRetryableError(Exception("Service unavailable")))
        assertTrue(isRetryableError(Exception("Internal server error")))
        
        // Non-retryable errors
        assertTrue(!isRetryableError(Exception("Invalid credentials")))
        assertTrue(!isRetryableError(Exception("Permission denied")))
    }
    
    @Test
    fun testRetryAnalytics() = runTest {
        val analytics = InMemoryRetryAnalytics()
        var attempts = 0
        
        try {
            withRetryAndAnalytics(
                policy = RetryPolicy(maxAttempts = 2),
                analytics = analytics,
                operationName = "testOperation",
                serviceName = "TestService"
            ) {
                attempts++
                throw Exception("Network error") // Always fail
            }
        } catch (e: Exception) {
            // Expected to fail after all attempts
        }
        
        val metrics = analytics.getMetrics()
        // Should have logged one final result (failure)
        assertTrue(metrics.totalRetries >= 1)
        assertEquals(0, metrics.successfulRetries)
        assertTrue(metrics.failedRetries >= 1)
    }
    
    @Test
    fun testRetryAnalyticsSuccess() = runTest {
        val analytics = InMemoryRetryAnalytics()
        var attempts = 0
        
        withRetryAndAnalytics(
            policy = RetryPolicy(maxAttempts = 3),
            analytics = analytics,
            operationName = "testOperation",
            serviceName = "TestService"
        ) {
            attempts++
            if (attempts < 2) {
                throw Exception("Network error")
            }
            "Success"
        }
        
        val metrics = analytics.getMetrics()
        assertEquals(1, metrics.totalRetries)
        assertEquals(1, metrics.successfulRetries)
        assertEquals(0, metrics.failedRetries)
    }
    
    @Test
    fun testAggressivePolicy() {
        val policy = RetryPolicy.AGGRESSIVE
        
        assertEquals(5, policy.maxAttempts)
        assertTrue(policy.initialDelay.inWholeMilliseconds == 500L)
        assertEquals(1.5, policy.multiplier)
    }
    
    @Test
    fun testConservativePolicy() {
        val policy = RetryPolicy.CONSERVATIVE
        
        assertEquals(2, policy.maxAttempts)
        assertTrue(policy.initialDelay.inWholeMilliseconds == 2000L)
        assertEquals(3.0, policy.multiplier)
    }
}
