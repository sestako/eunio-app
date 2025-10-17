package com.eunio.healthapp.testutil

import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

/**
 * Integration test demonstrating the error handling infrastructure.
 * Shows how ResilientTest, MockServiceManager, TestHealthMonitor, and TestDiagnostics work together.
 */
class ErrorHandlingIntegrationTest : ResilientTest() {
    
    private lateinit var healthMonitor: TestHealthMonitor
    
    @BeforeTest
    fun setup() {
        // Initialize diagnostic collectors
        TestDiagnostics.registerStandardCollectors()
        
        // Initialize health monitor
        healthMonitor = TestHealthMonitor()
        healthMonitor.registerStandardHealthChecks()
        
        // Setup Koin with test module
        startKoin {
            modules(MockServiceManager.createTestModule())
        }
    }
    
    @AfterTest
    fun teardown() {
        healthMonitor.stopMonitoring()
        healthMonitor.reset()
        MockServiceManager.clearAll()
        TestDiagnostics.clearHistory()
        clearDiagnostics()
        stopKoin()
    }
    
    @Test
    fun testSuccessfulExecution() = runTest {
        // Test successful execution with error handling infrastructure
        val result = safeTestExecution("successful_test") {
            "Test completed successfully"
        }
        
        assertNotNull(result)
        assertEquals("Test completed successfully", result)
    }
    
    @Test
    fun testErrorRecoveryWithRetries() = runTest {
        var attemptCount = 0
        
        val result = safeTestExecution(
            testName = "retry_test",
            retryCount = 2
        ) {
            attemptCount++
            if (attemptCount < 3) {
                throw RuntimeException("Simulated failure on attempt $attemptCount")
            }
            "Success on attempt $attemptCount"
        }
        
        assertNotNull(result)
        assertEquals("Success on attempt 3", result)
        assertEquals(3, attemptCount)
    }
    
    @Test
    fun testTimeoutHandling() = runTest {
        val result = safeTestExecution(
            testName = "timeout_test",
            timeout = 1.seconds
        ) {
            kotlinx.coroutines.delay(2.seconds)
            "This should timeout"
        }
        
        assertNull(result) // Should return null due to timeout
    }
    
    @Test
    fun testMockServiceErrorHandling() = runTest {
        // Register a failure handler for a mock service
        MockServiceManager.registerFailureHandler(TestService::class) { exception ->
            println("Mock service creation failed: ${exception.message}")
            FallbackTestService()
        }
        
        val result = safeTestExecution("mock_service_test") {
            val service = MockServiceManager.createMockService(TestService::class) {
                FallbackTestService()
            }
            
            assertNotNull(service)
            service.performOperation()
        }
        
        assertNotNull(result)
        assertEquals("Fallback operation", result)
    }
    
    @Test
    fun testHealthMonitoringIntegration() = runTest {
        val result = safeTestExecution("health_monitoring_test") {
            // Perform health check
            val healthReport = healthMonitor.checkTestHealth()
            
            assertTrue(healthReport.isHealthy, "Test environment should be healthy")
            assertNotNull(healthReport.summary)
            assertTrue(healthReport.checkResults.isNotEmpty())
            
            "Health monitoring working correctly"
        }
        
        assertNotNull(result)
    }
    
    @Test
    fun testDiagnosticCollection() = runTest {
        // Test that demonstrates diagnostic collection on failure
        try {
            requireTestSuccess("diagnostic_test") {
                throw IllegalStateException("Simulated test failure for diagnostics")
            }
            fail("Test should have failed")
        } catch (e: AssertionError) {
            // Verify that diagnostic information is included in the failure message
            assertTrue(e.message?.contains("Test Diagnostic Information") == true)
            assertTrue(e.message?.contains("System Information") == true)
        }
    }
    
    @Test
    fun testPreconditionValidation() = runTest {
        val result = safeTestExecution("precondition_test") {
            // Validate preconditions before test execution
            validatePreconditions(
                "precondition_test",
                "Mock services available" to true,
                "Test data valid" to true
            )
            
            "Preconditions validated successfully"
        }
        
        assertNotNull(result)
    }
    
    @Test
    fun testCustomErrorHandler() = runTest {
        val result = testWithErrorHandler(
            testName = "custom_error_handler_test",
            errorHandler = { exception ->
                when (exception) {
                    is IllegalArgumentException -> "Handled IllegalArgumentException"
                    is RuntimeException -> "Handled RuntimeException"
                    else -> null
                }
            }
        ) {
            throw IllegalArgumentException("Test exception")
        }
        
        assertEquals("Handled IllegalArgumentException", result)
    }
    
    @Test
    fun testFailurePatternAnalysis() = runTest {
        // Generate some test failures for pattern analysis by directly recording them
        repeat(3) { attempt ->
            val exception = NullPointerException("Simulated NPE for pattern analysis")
            TestDiagnostics.collectDiagnostics(
                testName = "pattern_test_$attempt",
                exception = exception
            )
        }
        
        val analysis = TestDiagnostics.analyzeFailurePatterns()
        assertTrue(analysis.totalFailures >= 3)
        assertTrue(analysis.commonExceptions.any { it.first == "NullPointerException" })
    }
    
    @Test
    fun testMockServiceValidation() = runTest {
        val result = safeTestExecution("mock_validation_test") {
            // Create some mock services
            MockServiceManager.createMockService(TestService::class) { FallbackTestService() }
            
            // Validate mock services
            val validation = MockServiceManager.validateMockServices(listOf(TestService::class))
            
            assertTrue(validation.isValid, "Mock service validation should pass")
            assertTrue(validation.missingServices.isEmpty())
            assertTrue(validation.failedServices.isEmpty())
            
            "Mock service validation passed"
        }
        
        assertNotNull(result)
    }
    
    @Test
    fun testComprehensiveDiagnostics() = runTest {
        val result = safeTestExecution("comprehensive_diagnostics_test") {
            // Collect comprehensive diagnostics
            val diagnostics = TestDiagnostics.collectDiagnostics(
                testName = "comprehensive_diagnostics_test",
                additionalContext = mapOf(
                    "testPhase" to "execution",
                    "customData" to "test value"
                )
            )
            
            assertNotNull(diagnostics.systemInfo)
            assertNotNull(diagnostics.testEnvironmentInfo)
            assertTrue(diagnostics.diagnosticData.isNotEmpty())
            assertTrue(diagnostics.diagnosticData.containsKey("testPhase"))
            
            "Comprehensive diagnostics collected successfully"
        }
        
        assertNotNull(result)
    }
}

// Test interfaces and implementations are defined in MockServiceManager