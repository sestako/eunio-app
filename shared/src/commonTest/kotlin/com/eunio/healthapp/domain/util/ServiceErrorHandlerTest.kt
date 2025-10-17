package com.eunio.healthapp.domain.util

import com.eunio.healthapp.domain.error.ServiceError
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Tests for ServiceErrorHandler to verify comprehensive error handling,
 * logging, and fallback mechanisms.
 */
class ServiceErrorHandlerTest {
    
    @Test
    fun `safeExecute should return success for successful operations`() = runTest {
        val result = ServiceErrorHandler.safeExecute(
            serviceName = "TestService",
            operation = "testOperation"
        ) {
            "success"
        }
        
        assertIs<Result.Success<String>>(result)
        assertEquals("success", result.data)
    }
    
    @Test
    fun `safeExecute should handle timeout errors`() = runTest {
        val result = ServiceErrorHandler.safeExecute(
            serviceName = "TestService",
            operation = "testOperation",
            timeoutMs = 100L
        ) {
            kotlinx.coroutines.delay(200L)
            "should not reach here"
        }
        
        assertIs<Result.Error>(result)
        assertIs<ServiceError.TimeoutError>(result.error)
        
        val timeoutError = result.error as ServiceError.TimeoutError
        assertEquals("TestService", timeoutError.serviceName)
        assertEquals("testOperation", timeoutError.operation)
        assertEquals(100L, timeoutError.timeoutMs)
    }
    
    @Test
    fun `safeExecute should use fallback on timeout`() = runTest {
        val result = ServiceErrorHandler.safeExecute(
            serviceName = "TestService",
            operation = "testOperation",
            timeoutMs = 100L,
            fallback = { "fallback_result" }
        ) {
            kotlinx.coroutines.delay(200L)
            "should not reach here"
        }
        
        assertIs<Result.Success<String>>(result)
        assertEquals("fallback_result", result.data)
    }
    
    @Test
    fun `safeExecute should handle IllegalArgumentException as ConfigurationError`() = runTest {
        val result = ServiceErrorHandler.safeExecute(
            serviceName = "TestService",
            operation = "testOperation"
        ) {
            throw IllegalArgumentException("Invalid configuration")
        }
        
        assertIs<Result.Error>(result)
        assertIs<ServiceError.ConfigurationError>(result.error)
        
        val configError = result.error as ServiceError.ConfigurationError
        assertEquals("TestService", configError.serviceName)
        assertEquals("Invalid configuration", configError.configurationIssue)
    }
    
    @Test
    fun `safeExecute should handle IllegalStateException as InitializationError`() = runTest {
        val result = ServiceErrorHandler.safeExecute(
            serviceName = "TestService",
            operation = "testOperation"
        ) {
            throw IllegalStateException("Service not initialized")
        }
        
        assertIs<Result.Error>(result)
        assertIs<ServiceError.InitializationError>(result.error)
        
        val initError = result.error as ServiceError.InitializationError
        assertEquals("TestService", initError.serviceName)
    }
    
    @Test
    fun `safeExecute should handle generic exceptions as OperationError`() = runTest {
        val result = ServiceErrorHandler.safeExecute(
            serviceName = "TestService",
            operation = "testOperation"
        ) {
            throw RuntimeException("Something went wrong")
        }
        
        assertIs<Result.Error>(result)
        assertIs<ServiceError.OperationError>(result.error)
        
        val opError = result.error as ServiceError.OperationError
        assertEquals("TestService", opError.serviceName)
        assertEquals("testOperation", opError.operation)
    }
    
    @Test
    fun `safeExecute should use fallback on generic exceptions`() = runTest {
        val result = ServiceErrorHandler.safeExecute(
            serviceName = "TestService",
            operation = "testOperation",
            fallback = { "fallback_result" }
        ) {
            throw RuntimeException("Something went wrong")
        }
        
        assertIs<Result.Success<String>>(result)
        assertEquals("fallback_result", result.data)
    }
    
    @Test
    fun `safeExecute should handle fallback failures`() = runTest {
        val result = ServiceErrorHandler.safeExecute(
            serviceName = "TestService",
            operation = "testOperation",
            fallback = { throw RuntimeException("Fallback also failed") }
        ) {
            throw RuntimeException("Primary operation failed")
        }
        
        assertIs<Result.Error>(result)
        assertIs<ServiceError.OperationError>(result.error)
        assertTrue(result.error.message?.contains("Primary operation failed") == true)
    }
    
    @Test
    fun `safeExecuteWithRetry should retry on failures`() = runTest {
        var attemptCount = 0
        
        val result = ServiceErrorHandler.safeExecuteWithRetry(
            serviceName = "TestService",
            operation = "testOperation",
            maxRetries = 2,
            retryDelayMs = 10L
        ) {
            attemptCount++
            if (attemptCount < 3) {
                throw RuntimeException("Attempt $attemptCount failed")
            }
            "success on attempt $attemptCount"
        }
        
        assertIs<Result.Success<String>>(result)
        assertEquals("success on attempt 3", result.data)
        assertEquals(3, attemptCount)
    }
    
    @Test
    fun `safeExecuteWithRetry should use fallback after all retries fail`() = runTest {
        var attemptCount = 0
        
        val result = ServiceErrorHandler.safeExecuteWithRetry(
            serviceName = "TestService",
            operation = "testOperation",
            maxRetries = 2,
            retryDelayMs = 10L,
            fallback = { "fallback_result" }
        ) {
            attemptCount++
            throw RuntimeException("Attempt $attemptCount failed")
        }
        
        assertIs<Result.Success<String>>(result)
        assertEquals("fallback_result", result.data)
        assertEquals(3, attemptCount) // Initial attempt + 2 retries
    }
    
    @Test
    fun `safeExecuteWithRetry should return error when all retries and fallback fail`() = runTest {
        var attemptCount = 0
        
        val result = ServiceErrorHandler.safeExecuteWithRetry(
            serviceName = "TestService",
            operation = "testOperation",
            maxRetries = 2,
            retryDelayMs = 10L,
            fallback = { throw RuntimeException("Fallback failed") }
        ) {
            attemptCount++
            throw RuntimeException("Attempt $attemptCount failed")
        }
        
        assertIs<Result.Error>(result)
        assertEquals(3, attemptCount) // Initial attempt + 2 retries
    }
    
    @Test
    fun `createDegradedModeError should create proper error with operation lists`() {
        val error = ServiceErrorHandler.createDegradedModeError(
            serviceName = "TestService",
            availableOperations = listOf("read", "list"),
            unavailableOperations = listOf("write", "delete")
        )
        
        assertEquals("TestService", error.serviceName)
        assertEquals(listOf("read", "list"), error.availableOperations)
        assertEquals(listOf("write", "delete"), error.unavailableOperations)
        assertTrue(error.message.contains("degraded mode"))
        assertTrue(error.message.contains("read, list"))
        assertTrue(error.message.contains("write, delete"))
    }
}