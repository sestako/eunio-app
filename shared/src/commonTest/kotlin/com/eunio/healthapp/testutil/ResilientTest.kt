package com.eunio.healthapp.testutil

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlin.test.fail
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Base class for resilient test execution with error recovery and diagnostic information.
 * Provides utilities for handling test failures gracefully and collecting diagnostic data.
 */
abstract class ResilientTest {
    
    private val testExecutionLog = mutableListOf<TestExecutionEntry>()
    private var currentTestName: String? = null
    
    /**
     * Executes a test with error recovery and diagnostic collection.
     * 
     * @param testName Name of the test for logging and diagnostics
     * @param timeout Maximum time to wait for test completion
     * @param retryCount Number of retry attempts on failure
     * @param test The test block to execute
     * @return Test result or null if all attempts failed
     */
    protected suspend fun <T> safeTestExecution(
        testName: String,
        timeout: Duration = 30.seconds,
        retryCount: Int = 0,
        test: suspend () -> T
    ): T? {
        currentTestName = testName
        var lastException: Exception? = null
        
        repeat(retryCount + 1) { attempt ->
            try {
                logTestStart(testName, attempt)
                
                val result = withTimeout(timeout) {
                    test()
                }
                
                logTestSuccess(testName, attempt)
                return result
                
            } catch (e: TimeoutCancellationException) {
                lastException = e
                logTestTimeout(testName, attempt, timeout)
                
            } catch (e: Exception) {
                lastException = e
                logTestFailure(testName, attempt, e)
            }
            
            if (attempt < retryCount) {
                logRetryAttempt(testName, attempt + 1)
            }
        }
        
        logTestExhausted(testName, retryCount, lastException)
        return null
    }
    
    /**
     * Executes a test that must succeed, failing the test if it doesn't.
     */
    protected suspend fun <T> requireTestSuccess(
        testName: String,
        timeout: Duration = 30.seconds,
        retryCount: Int = 0,
        test: suspend () -> T
    ): T {
        return safeTestExecution(testName, timeout, retryCount, test)
            ?: fail("Test '$testName' failed after ${retryCount + 1} attempts. ${getDiagnosticInfo()}")
    }
    
    /**
     * Executes a test with custom error handling.
     */
    protected suspend fun <T> testWithErrorHandler(
        testName: String,
        errorHandler: (Exception) -> T?,
        test: suspend () -> T
    ): T? {
        currentTestName = testName
        
        return try {
            logTestStart(testName, 0)
            val result = test()
            logTestSuccess(testName, 0)
            result
            
        } catch (e: Exception) {
            logTestFailure(testName, 0, e)
            errorHandler(e)
        }
    }
    
    /**
     * Validates test preconditions and provides diagnostic info on failure.
     */
    protected fun validatePreconditions(
        testName: String,
        vararg conditions: Pair<String, Boolean>
    ) {
        val failedConditions = conditions.filter { !it.second }
        
        if (failedConditions.isNotEmpty()) {
            val diagnostics = buildString {
                appendLine("Test '$testName' precondition validation failed:")
                failedConditions.forEach { (description, _) ->
                    appendLine("  - $description")
                }
                appendLine()
                appendLine("Diagnostic Information:")
                appendLine(getDiagnosticInfo())
            }
            
            fail(diagnostics)
        }
    }
    
    /**
     * Collects and formats diagnostic information for test failures.
     */
    protected fun getDiagnosticInfo(): String {
        return buildString {
            appendLine("=== Test Diagnostic Information ===")
            appendLine("Current Test: ${currentTestName ?: "Unknown"}")
            appendLine("Execution Log:")
            
            testExecutionLog.takeLast(10).forEach { entry ->
                appendLine("  ${entry.timestamp}: ${entry.message}")
            }
            
            appendLine()
            appendLine("System Information:")
            appendLine("  Platform: ${getPlatformInfo()}")
            appendLine("  Memory: ${getMemoryInfo()}")
            appendLine("  Thread: Current thread info not available")
            
            appendLine()
            appendLine("Test Environment:")
            appendLine("  Koin Status: ${getKoinStatus()}")
            appendLine("  Mock Services: ${getMockServiceStatus()}")
        }
    }
    
    /**
     * Clears diagnostic information (call in test cleanup).
     */
    protected fun clearDiagnostics() {
        testExecutionLog.clear()
        currentTestName = null
    }
    
    // Private logging methods
    private fun logTestStart(testName: String, attempt: Int) {
        val message = if (attempt == 0) {
            "Starting test: $testName"
        } else {
            "Retrying test: $testName (attempt ${attempt + 1})"
        }
        testExecutionLog.add(TestExecutionEntry(getCurrentTimestamp(), message))
    }
    
    private fun logTestSuccess(testName: String, attempt: Int) {
        val message = if (attempt == 0) {
            "Test completed successfully: $testName"
        } else {
            "Test completed successfully on retry: $testName (attempt ${attempt + 1})"
        }
        testExecutionLog.add(TestExecutionEntry(getCurrentTimestamp(), message))
    }
    
    private fun logTestFailure(testName: String, attempt: Int, exception: Exception) {
        val message = "Test failed: $testName (attempt ${attempt + 1}) - ${exception::class.simpleName}: ${exception.message}"
        testExecutionLog.add(TestExecutionEntry(getCurrentTimestamp(), message))
    }
    
    private fun logTestTimeout(testName: String, attempt: Int, timeout: Duration) {
        val message = "Test timed out: $testName (attempt ${attempt + 1}) after $timeout"
        testExecutionLog.add(TestExecutionEntry(getCurrentTimestamp(), message))
    }
    
    private fun logRetryAttempt(testName: String, nextAttempt: Int) {
        val message = "Preparing retry for test: $testName (next attempt: $nextAttempt)"
        testExecutionLog.add(TestExecutionEntry(getCurrentTimestamp(), message))
    }
    
    private fun logTestExhausted(testName: String, retryCount: Int, lastException: Exception?) {
        val message = "Test exhausted all attempts: $testName (${retryCount + 1} attempts) - Last error: ${lastException?.message}"
        testExecutionLog.add(TestExecutionEntry(getCurrentTimestamp(), message))
    }
    
    // Platform-specific information gathering
    private fun getPlatformInfo(): String {
        return try {
            "Kotlin Multiplatform"
        } catch (e: Exception) {
            "Unknown platform"
        }
    }
    
    private fun getMemoryInfo(): String {
        return try {
            "Memory info not available in common code"
        } catch (e: Exception) {
            "Memory info unavailable"
        }
    }
    
    private fun getKoinStatus(): String {
        return try {
            // Check if Koin is started
            "Koin status check not available in common code"
        } catch (e: Exception) {
            "Unknown (${e.message})"
        }
    }
    
    private fun getMockServiceStatus(): String {
        return try {
            MockServiceManager.getServiceStatus()
        } catch (e: Exception) {
            "Status unavailable (${e.message})"
        }
    }
    
    private fun getCurrentTimestamp(): String {
        return kotlinx.datetime.Clock.System.now().toString()
    }
}

/**
 * Data class for test execution log entries.
 */
data class TestExecutionEntry(
    val timestamp: String,
    val message: String
)