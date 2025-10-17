package com.eunio.healthapp.testutil

import kotlinx.datetime.Clock
import kotlin.test.fail

/**
 * Comprehensive test diagnostics utility that provides clear diagnostic information
 * for test failures and integrates with all error handling components.
 */
object TestDiagnostics {
    
    private val diagnosticCollectors = mutableListOf<DiagnosticCollector>()
    private val failureHistory = mutableListOf<TestFailureRecord>()
    
    /**
     * Collects comprehensive diagnostic information for a test failure.
     */
    fun collectDiagnostics(
        testName: String,
        exception: Throwable? = null,
        additionalContext: Map<String, Any> = emptyMap()
    ): DiagnosticReport {
        val timestamp = Clock.System.now()
        val diagnosticData = mutableMapOf<String, Any>()
        
        // Collect data from all registered collectors
        diagnosticCollectors.forEach { collector ->
            try {
                val collectorData = collector.collect()
                diagnosticData[collector.name] = collectorData
            } catch (e: Exception) {
                diagnosticData[collector.name] = "Collection failed: ${e.message}"
            }
        }
        
        // Add additional context
        diagnosticData.putAll(additionalContext)
        
        // Create comprehensive report
        val report = DiagnosticReport(
            testName = testName,
            timestamp = timestamp,
            exception = exception,
            diagnosticData = diagnosticData,
            systemInfo = collectSystemInfo(),
            testEnvironmentInfo = collectTestEnvironmentInfo(),
            recommendations = generateRecommendations(exception, diagnosticData)
        )
        
        // Record the failure for pattern analysis
        recordFailure(report)
        
        return report
    }
    
    /**
     * Fails a test with comprehensive diagnostic information.
     */
    fun failWithDiagnostics(
        testName: String,
        message: String,
        exception: Throwable? = null,
        additionalContext: Map<String, Any> = emptyMap()
    ): Nothing {
        val diagnostics = collectDiagnostics(testName, exception, additionalContext)
        val fullMessage = buildString {
            appendLine("Test Failure: $message")
            appendLine()
            appendLine(formatDiagnosticReport(diagnostics))
        }
        
        fail(fullMessage)
    }
    
    /**
     * Registers a diagnostic collector.
     */
    fun registerDiagnosticCollector(collector: DiagnosticCollector) {
        diagnosticCollectors.add(collector)
    }
    
    /**
     * Registers standard diagnostic collectors.
     */
    fun registerStandardCollectors() {
        // Koin diagnostics collector
        registerDiagnosticCollector(object : DiagnosticCollector {
            override val name = "Koin Context"
            override fun collect(): Any {
                return try {
                    mapOf("status" to "not_available_in_common_code")
                } catch (e: Exception) {
                    mapOf("status" to "error", "error" to e.message)
                }
            }
        })
        
        // Mock service diagnostics collector
        registerDiagnosticCollector(object : DiagnosticCollector {
            override val name = "Mock Services"
            override fun collect(): Any {
                return try {
                    MockServiceManager.getDiagnosticInfo()
                } catch (e: Exception) {
                    "Mock service diagnostics failed: ${e.message}"
                }
            }
        })
        
        // Test health monitor collector
        registerDiagnosticCollector(object : DiagnosticCollector {
            override val name = "Test Health"
            override fun collect(): Any {
                return try {
                    val monitor = TestHealthMonitor()
                    val report = monitor.checkTestHealth()
                    mapOf(
                        "isHealthy" to report.isHealthy,
                        "summary" to report.summary,
                        "failedChecks" to report.checkResults.filter { !it.isHealthy }.map { it.checkName },
                        "recommendations" to report.recommendations
                    )
                } catch (e: Exception) {
                    "Health check failed: ${e.message}"
                }
            }
        })
        
        // Memory diagnostics collector
        registerDiagnosticCollector(object : DiagnosticCollector {
            override val name = "Memory"
            override fun collect(): Any {
                return try {
                    mapOf("status" to "not_available_in_common_code")
                } catch (e: Exception) {
                    "Memory info unavailable: ${e.message}"
                }
            }
        })
        
        // Thread diagnostics collector
        registerDiagnosticCollector(object : DiagnosticCollector {
            override val name = "Threading"
            override fun collect(): Any {
                return try {
                    mapOf("status" to "not_available_in_common_code")
                } catch (e: Exception) {
                    "Thread info unavailable: ${e.message}"
                }
            }
        })
    }
    
    /**
     * Analyzes failure patterns and provides insights.
     */
    fun analyzeFailurePatterns(): FailureAnalysis {
        val recentFailures = failureHistory.takeLast(50)
        
        val failuresByTest = recentFailures.groupBy { it.testName }
        val failuresByException = recentFailures.groupBy { it.exceptionType }
        val commonIssues = identifyCommonIssues(recentFailures)
        
        return FailureAnalysis(
            totalFailures = recentFailures.size,
            uniqueTests = failuresByTest.size,
            mostFailedTests = failuresByTest.entries
                .sortedByDescending { it.value.size }
                .take(5)
                .map { it.key to it.value.size },
            commonExceptions = failuresByException.entries
                .sortedByDescending { it.value.size }
                .take(5)
                .map { it.key to it.value.size },
            commonIssues = commonIssues,
            recommendations = generatePatternRecommendations(commonIssues)
        )
    }
    
    /**
     * Gets recent failure history.
     */
    fun getFailureHistory(limit: Int = 20): List<TestFailureRecord> {
        return failureHistory.takeLast(limit)
    }
    
    /**
     * Clears diagnostic history.
     */
    fun clearHistory() {
        failureHistory.clear()
    }
    
    // Private helper methods
    private fun collectSystemInfo(): Map<String, String> {
        return try {
            mapOf(
                "platform" to "Kotlin Multiplatform",
                "kotlinVersion" to KotlinVersion.CURRENT.toString()
            )
        } catch (e: Exception) {
            mapOf("error" to "System info collection failed: ${e.message}")
        }
    }
    
    private fun collectTestEnvironmentInfo(): Map<String, String> {
        return try {
            mapOf(
                "testFramework" to "Kotlin Test",
                "timestamp" to Clock.System.now().toString()
            )
        } catch (e: Exception) {
            mapOf("error" to "Test environment info collection failed: ${e.message}")
        }
    }
    
    private fun generateRecommendations(
        exception: Throwable?,
        diagnosticData: Map<String, Any>
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        // Exception-based recommendations
        exception?.let { ex ->
            when (ex::class.simpleName) {
                "NullPointerException" -> {
                    recommendations.add("Check for null values in test setup or mock services")
                }
                "ClassCastException" -> {
                    recommendations.add("Verify type compatibility between expected and actual values")
                }
                "IllegalStateException" -> {
                    recommendations.add("Ensure proper test initialization and state management")
                }
                "TimeoutCancellationException" -> {
                    recommendations.add("Consider increasing test timeout or optimizing async operations")
                }
                else -> {
                    recommendations.add("Review exception type: ${ex::class.simpleName}")
                }
            }
        }
        
        // Diagnostic data-based recommendations
        diagnosticData.forEach { (key, value) ->
            when (key) {
                "Koin Context" -> {
                    if (value.toString().contains("not_initialized")) {
                        recommendations.add("Initialize Koin context before running tests")
                    }
                }
                "Mock Services" -> {
                    if (value.toString().contains("failed")) {
                        recommendations.add("Update mock service implementations to match current interfaces")
                    }
                }
                "Memory" -> {
                    if (value.toString().contains("high")) {
                        recommendations.add("Consider reducing test data size or running garbage collection")
                    }
                }
            }
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("Review test setup and ensure all dependencies are properly configured")
        }
        
        return recommendations
    }
    
    private fun recordFailure(report: DiagnosticReport) {
        val record = TestFailureRecord(
            testName = report.testName,
            timestamp = report.timestamp,
            exceptionType = report.exception?.let { it::class.simpleName } ?: "Unknown",
            message = report.exception?.message ?: "No message",
            diagnosticSummary = report.diagnosticData.keys.joinToString(", ")
        )
        
        failureHistory.add(record)
        
        // Keep only recent failures to prevent memory issues
        if (failureHistory.size > 1000) {
            failureHistory.removeAt(0)
        }
    }
    
    private fun identifyCommonIssues(failures: List<TestFailureRecord>): List<String> {
        val issues = mutableListOf<String>()
        
        // Check for recurring exception types
        val exceptionCounts = failures.groupBy { it.exceptionType }.mapValues { it.value.size }
        exceptionCounts.filter { it.value >= 3 }.forEach { (exception, count) ->
            issues.add("Recurring $exception exceptions ($count occurrences)")
        }
        
        // Check for tests that fail frequently
        val testCounts = failures.groupBy { it.testName }.mapValues { it.value.size }
        testCounts.filter { it.value >= 3 }.forEach { (testName, count) ->
            issues.add("Test '$testName' fails frequently ($count failures)")
        }
        
        return issues
    }
    
    private fun generatePatternRecommendations(commonIssues: List<String>): List<String> {
        val recommendations = mutableListOf<String>()
        
        commonIssues.forEach { issue ->
            when {
                issue.contains("NullPointerException") -> {
                    recommendations.add("Review null safety in test setup and mock implementations")
                }
                issue.contains("TimeoutCancellationException") -> {
                    recommendations.add("Optimize async operations or increase test timeouts")
                }
                issue.contains("fails frequently") -> {
                    recommendations.add("Investigate flaky tests and improve test stability")
                }
            }
        }
        
        return recommendations
    }
    
    private fun formatDiagnosticReport(report: DiagnosticReport): String {
        return buildString {
            appendLine("=== DIAGNOSTIC REPORT ===")
            appendLine("Test: ${report.testName}")
            appendLine("Timestamp: ${report.timestamp}")
            
            report.exception?.let { ex ->
                appendLine("Exception: ${ex::class.simpleName}")
                appendLine("Message: ${ex.message}")
                appendLine("Stack trace not available in common code")
            }
            
            appendLine()
            appendLine("System Information:")
            report.systemInfo.forEach { (key, value) ->
                appendLine("  $key: $value")
            }
            
            appendLine()
            appendLine("Test Environment:")
            report.testEnvironmentInfo.forEach { (key, value) ->
                appendLine("  $key: $value")
            }
            
            appendLine()
            appendLine("Diagnostic Data:")
            report.diagnosticData.forEach { (key, value) ->
                appendLine("  $key:")
                appendLine("    ${value.toString().replace("\n", "\n    ")}")
            }
            
            if (report.recommendations.isNotEmpty()) {
                appendLine()
                appendLine("Recommendations:")
                report.recommendations.forEach { recommendation ->
                    appendLine("  • $recommendation")
                }
            }
            
            appendLine("=== END DIAGNOSTIC REPORT ===")
        }
    }
}

/**
 * Interface for diagnostic data collectors.
 */
interface DiagnosticCollector {
    val name: String
    fun collect(): Any
}

/**
 * Comprehensive diagnostic report.
 */
data class DiagnosticReport(
    val testName: String,
    val timestamp: kotlinx.datetime.Instant,
    val exception: Throwable?,
    val diagnosticData: Map<String, Any>,
    val systemInfo: Map<String, String>,
    val testEnvironmentInfo: Map<String, String>,
    val recommendations: List<String>
)

/**
 * Record of a test failure for pattern analysis.
 */
data class TestFailureRecord(
    val testName: String,
    val timestamp: kotlinx.datetime.Instant,
    val exceptionType: String,
    val message: String,
    val diagnosticSummary: String
)

/**
 * Analysis of failure patterns.
 */
data class FailureAnalysis(
    val totalFailures: Int,
    val uniqueTests: Int,
    val mostFailedTests: List<Pair<String, Int>>,
    val commonExceptions: List<Pair<String, Int>>,
    val commonIssues: List<String>,
    val recommendations: List<String>
)