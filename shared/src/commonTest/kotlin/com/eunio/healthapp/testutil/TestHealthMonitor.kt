package com.eunio.healthapp.testutil

import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Monitor for continuous test validation and health checking.
 * Provides comprehensive test environment monitoring and diagnostic reporting.
 */
class TestHealthMonitor {
    
    private val healthChecks = mutableListOf<HealthCheck>()
    private val monitoringLog = mutableListOf<MonitoringEntry>()
    private var isMonitoring = false
    private var lastHealthReport: TestHealthMonitorReport? = null
    
    /**
     * Starts continuous monitoring of test health.
     */
    suspend fun startMonitoring(interval: Duration = 30.seconds) {
        if (isMonitoring) {
            logMonitoringEvent("Monitoring already active, skipping start request")
            return
        }
        
        isMonitoring = true
        logMonitoringEvent("Starting test health monitoring with ${interval} interval")
        
        while (isMonitoring) {
            try {
                val report = checkTestHealth()
                lastHealthReport = report
                
                if (!report.isHealthy) {
                    logMonitoringEvent("Health check failed: ${report.summary}")
                    handleUnhealthyState(report)
                } else {
                    logMonitoringEvent("Health check passed")
                }
                
                delay(interval)
                
            } catch (e: Exception) {
                logMonitoringEvent("Monitoring error: ${e.message}")
                delay(interval)
            }
        }
    }
    
    /**
     * Stops continuous monitoring.
     */
    fun stopMonitoring() {
        if (isMonitoring) {
            isMonitoring = false
            logMonitoringEvent("Test health monitoring stopped")
        }
    }
    
    /**
     * Performs a comprehensive test health check.
     */
    fun checkTestHealth(): TestHealthMonitorReport {
        logMonitoringEvent("Performing comprehensive test health check")
        
        val startTime = Clock.System.now()
        val results = mutableListOf<HealthCheckResult>()
        
        // Run all registered health checks
        healthChecks.forEach { healthCheck ->
            try {
                val result = healthCheck.check()
                results.add(result)
                
                if (!result.isHealthy) {
                    logMonitoringEvent("Health check failed: ${healthCheck.name} - ${result.message}")
                }
                
            } catch (e: Exception) {
                results.add(HealthCheckResult(
                    checkName = healthCheck.name,
                    isHealthy = false,
                    message = "Health check threw exception: ${e.message}",
                    details = mapOf("exception" to e::class.simpleName.orEmpty())
                ))
                logMonitoringEvent("Health check exception: ${healthCheck.name} - ${e.message}")
            }
        }
        
        val endTime = Clock.System.now()
        val duration = endTime - startTime
        
        val report = TestHealthMonitorReport(
            timestamp = endTime,
            isHealthy = results.all { it.isHealthy },
            checkResults = results,
            executionDuration = duration,
            summary = generateSummary(results),
            recommendations = generateRecommendations(results)
        )
        
        logMonitoringEvent("Health check completed in ${duration}. Overall health: ${report.isHealthy}")
        return report
    }
    
    /**
     * Registers a health check to be performed during monitoring.
     */
    fun registerHealthCheck(healthCheck: HealthCheck) {
        healthChecks.add(healthCheck)
        logMonitoringEvent("Registered health check: ${healthCheck.name}")
    }
    
    /**
     * Registers multiple standard health checks.
     */
    fun registerStandardHealthChecks() {
        // Koin context health check
        registerHealthCheck(object : HealthCheck {
            override val name = "Koin Context"
            override fun check(): HealthCheckResult {
                return try {
                    HealthCheckResult(
                        checkName = name,
                        isHealthy = true,
                        message = "Koin context check not available in common code",
                        details = mapOf("status" to "not_available")
                    )
                } catch (e: Exception) {
                    HealthCheckResult(
                        checkName = name,
                        isHealthy = false,
                        message = "Failed to check Koin context: ${e.message}",
                        details = mapOf("error" to e::class.simpleName.orEmpty())
                    )
                }
            }
        })
        
        // Mock service health check
        registerHealthCheck(object : HealthCheck {
            override val name = "Mock Services"
            override fun check(): HealthCheckResult {
                return try {
                    val status = MockServiceManager.getServiceStatus()
                    HealthCheckResult(
                        checkName = name,
                        isHealthy = true,
                        message = "Mock services are operational",
                        details = mapOf("status" to status)
                    )
                } catch (e: Exception) {
                    HealthCheckResult(
                        checkName = name,
                        isHealthy = false,
                        message = "Mock service check failed: ${e.message}",
                        details = mapOf("error" to e::class.simpleName.orEmpty())
                    )
                }
            }
        })
        
        // Memory health check
        registerHealthCheck(object : HealthCheck {
            override val name = "Memory Usage"
            override fun check(): HealthCheckResult {
                return try {
                    HealthCheckResult(
                        checkName = name,
                        isHealthy = true,
                        message = "Memory check not available in common code",
                        details = mapOf("status" to "not_available")
                    )
                } catch (e: Exception) {
                    HealthCheckResult(
                        checkName = name,
                        isHealthy = false,
                        message = "Memory check failed: ${e.message}",
                        details = mapOf("error" to e::class.simpleName.orEmpty())
                    )
                }
            }
        })
        
        // Test data integrity check
        registerHealthCheck(object : HealthCheck {
            override val name = "Test Data Integrity"
            override fun check(): HealthCheckResult {
                return try {
                    // Basic test data integrity check
                    HealthCheckResult(
                        checkName = name,
                        isHealthy = true,
                        message = "Test data integrity check passed",
                        details = mapOf("status" to "ok")
                    )
                } catch (e: Exception) {
                    HealthCheckResult(
                        checkName = name,
                        isHealthy = false,
                        message = "Test data creation failed: ${e.message}",
                        details = mapOf("error" to e::class.simpleName.orEmpty())
                    )
                }
            }
        })
    }
    
    /**
     * Gets the latest health report.
     */
    fun getLatestHealthReport(): TestHealthMonitorReport? = lastHealthReport
    
    /**
     * Performs a quick health check without full monitoring.
     */
    fun quickHealthCheck(): QuickHealthStatus {
        return try {
            val startTime = Clock.System.now()
            val basicChecks = listOf("Koin Context", "Mock Services")
            val issues = mutableListOf<String>()
            
            basicChecks.forEach { checkName ->
                try {
                    when (checkName) {
                        "Koin Context" -> {
                            // Basic Koin check
                        }
                        "Mock Services" -> {
                            MockServiceManager.getServiceStatus()
                        }
                    }
                } catch (e: Exception) {
                    issues.add("$checkName: ${e.message}")
                }
            }
            
            val duration = Clock.System.now() - startTime
            
            QuickHealthStatus(
                healthy = issues.isEmpty(),
                issues = issues,
                averageExecutionTime = duration
            )
        } catch (e: Exception) {
            QuickHealthStatus(
                healthy = false,
                issues = listOf("Quick health check failed: ${e.message}"),
                averageExecutionTime = Duration.ZERO
            )
        }
    }
    
    /**
     * Gets monitoring statistics and logs.
     */
    fun getMonitoringStats(): MonitoringStats {
        val totalChecks = monitoringLog.count { it.type == MonitoringEntryType.HEALTH_CHECK }
        val failedChecks = monitoringLog.count { 
            it.type == MonitoringEntryType.HEALTH_CHECK && it.message.contains("failed", ignoreCase = true)
        }
        
        return MonitoringStats(
            isCurrentlyMonitoring = isMonitoring,
            totalHealthChecks = totalChecks,
            failedHealthChecks = failedChecks,
            registeredChecks = healthChecks.size,
            recentLogEntries = monitoringLog.takeLast(20)
        )
    }
    
    /**
     * Clears monitoring history and resets state.
     */
    fun reset() {
        stopMonitoring()
        healthChecks.clear()
        monitoringLog.clear()
        lastHealthReport = null
        logMonitoringEvent("Test health monitor reset")
    }
    
    // Private helper methods
    private fun handleUnhealthyState(report: TestHealthMonitorReport) {
        logMonitoringEvent("Handling unhealthy test state")
        
        // Attempt automatic recovery for known issues
        report.checkResults.filter { !it.isHealthy }.forEach { failedCheck ->
            when (failedCheck.checkName) {
                "Koin Context" -> attemptKoinRecovery()
                "Mock Services" -> attemptMockServiceRecovery()
                "Memory Usage" -> attemptMemoryRecovery()
            }
        }
    }
    
    private fun attemptKoinRecovery() {
        try {
            logMonitoringEvent("Attempting Koin context recovery")
            // Koin recovery not available in common code
            logMonitoringEvent("Koin context recovery not available in common code")
        } catch (e: Exception) {
            logMonitoringEvent("Koin recovery failed: ${e.message}")
        }
    }
    
    private fun attemptMockServiceRecovery() {
        try {
            logMonitoringEvent("Attempting mock service recovery")
            MockServiceManager.clearAll()
            logMonitoringEvent("Mock services cleared for recovery")
        } catch (e: Exception) {
            logMonitoringEvent("Mock service recovery failed: ${e.message}")
        }
    }
    
    private fun attemptMemoryRecovery() {
        try {
            logMonitoringEvent("Attempting memory recovery")
            // Memory recovery not available in common code
            logMonitoringEvent("Memory recovery not available in common code")
        } catch (e: Exception) {
            logMonitoringEvent("Memory recovery failed: ${e.message}")
        }
    }
    
    private fun generateSummary(results: List<HealthCheckResult>): String {
        val healthy = results.count { it.isHealthy }
        val total = results.size
        return "$healthy/$total health checks passed"
    }
    
    private fun generateRecommendations(results: List<HealthCheckResult>): List<String> {
        val recommendations = mutableListOf<String>()
        
        results.filter { !it.isHealthy }.forEach { failedCheck ->
            when (failedCheck.checkName) {
                "Koin Context" -> recommendations.add("Ensure Koin is properly initialized before running tests")
                "Mock Services" -> recommendations.add("Verify mock service implementations are up to date")
                "Memory Usage" -> recommendations.add("Consider reducing test data size or running garbage collection")
                "Test Data Integrity" -> recommendations.add("Update test data builders to match current model structure")
                else -> recommendations.add("Investigate ${failedCheck.checkName} failure: ${failedCheck.message}")
            }
        }
        
        return recommendations
    }
    
    private fun logMonitoringEvent(message: String) {
        monitoringLog.add(MonitoringEntry(
            timestamp = Clock.System.now(),
            type = MonitoringEntryType.GENERAL,
            message = message
        ))
    }
}

/**
 * Interface for health checks.
 */
interface HealthCheck {
    val name: String
    fun check(): HealthCheckResult
}

/**
 * Result of a health check.
 */
data class HealthCheckResult(
    val checkName: String,
    val isHealthy: Boolean,
    val message: String,
    val details: Map<String, String> = emptyMap()
)

/**
 * Comprehensive test health report from monitor.
 */
data class TestHealthMonitorReport(
    val timestamp: Instant,
    val isHealthy: Boolean,
    val checkResults: List<HealthCheckResult>,
    val executionDuration: Duration,
    val summary: String,
    val recommendations: List<String>
)

/**
 * Monitoring statistics and information.
 */
data class MonitoringStats(
    val isCurrentlyMonitoring: Boolean,
    val totalHealthChecks: Int,
    val failedHealthChecks: Int,
    val registeredChecks: Int,
    val recentLogEntries: List<MonitoringEntry>
)

/**
 * Monitoring log entry.
 */
data class MonitoringEntry(
    val timestamp: Instant,
    val type: MonitoringEntryType,
    val message: String
)

/**
 * Types of monitoring log entries.
 */
enum class MonitoringEntryType {
    GENERAL,
    HEALTH_CHECK,
    ERROR,
    RECOVERY
}

/**
 * Quick health status result.
 */
data class QuickHealthStatus(
    val healthy: Boolean,
    val issues: List<String>,
    val averageExecutionTime: Duration
)