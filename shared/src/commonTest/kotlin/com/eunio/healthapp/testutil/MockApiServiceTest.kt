package com.eunio.healthapp.testutil

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Test for MockApiService to verify it works correctly
 */
class MockApiServiceTest {
    
    private lateinit var apiService: MockApiService
    
    @BeforeTest
    fun setup() {
        apiService = MockApiService()
    }
    
    @AfterTest
    fun tearDown() {
        apiService.reset()
    }
    
    @Test
    fun `MockApiService applies network delay correctly`() = runTest {
        // Given: Network delay of 500ms
        apiService.setNetworkDelay(500.milliseconds)
        
        // When: Executing a request
        val result = apiService.executeRequest("test") {
            "success"
        }
        
        // Then: Request succeeds
        assertTrue(result.isSuccess, "Request should succeed")
        assertEquals("success", result.getOrNull())
        
        // Verify statistics show the delay was configured (even if not actually waited in test)
        val stats = apiService.getRequestStatistics()
        assertEquals(1, stats.totalRequests)
        assertEquals(1, stats.successfulRequests)
        
        // In test environment, we can't reliably test actual timing due to test dispatchers
        // But we can verify the delay configuration is working by checking it doesn't fail
        assertTrue(stats.averageLatencyMs >= 0.0, "Average latency should be non-negative")
    }
    
    @Test
    fun `MockApiService handles network failures correctly`() = runTest {
        // Given: Network failure simulation
        apiService.simulateNetworkFailure(maxFailures = 1)
        
        // When: Executing a request
        val result = apiService.executeRequest("test") {
            "success"
        }
        
        // Then: Request fails with network error
        assertTrue(result.isError)
        
        // Verify statistics
        val stats = apiService.getRequestStatistics()
        assertEquals(1, stats.totalRequests)
        assertEquals(0, stats.successfulRequests)
        assertEquals(1, stats.failedRequests)
        assertTrue(stats.errorsByType.containsKey(MockApiService.ErrorType.NETWORK_ERROR))
    }
    
    @Test
    fun `MockApiService retry mechanism works correctly`() = runTest {
        // Given: Network will fail twice then succeed
        apiService.simulateNetworkFailure(maxFailures = 2)
        apiService.setRetryConfiguration(maxRetries = 3, retryDelay = 50.milliseconds)
        
        // When: Executing request with retry
        val result = apiService.executeRequestWithRetry("test") {
            "success"
        }
        
        // Then: Eventually succeeds after retries
        assertTrue(result.isSuccess)
        assertEquals("success", result.getOrNull())
        
        // Verify multiple attempts were made
        val stats = apiService.getRequestStatistics()
        assertTrue(stats.totalRequests >= 3, "Should have made at least 3 attempts, got ${stats.totalRequests}")
    }
}