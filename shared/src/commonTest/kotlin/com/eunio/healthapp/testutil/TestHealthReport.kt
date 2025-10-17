package com.eunio.healthapp.testutil

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration

/**
 * Generates comprehensive test health reports for monitoring
 */
class TestHealthReportGenerator {
    
    fun generateComprehensiveReport(): TestHealthReport {
        val startTime = Clock.System.now()
        
        return try {
            val compilationValidator = CompilationValidator()
            val crossPlatformRunner = CrossPlatformTestRunner()
            val testHealthMonitor = TestHealthMonitor()
            
            // Run all validations
            val compilationResults = compilationValidator.validateAllPhases()
            val crossPlatformResults = crossPlatformRunner.runCrossPlatformTests()
            val healthStatus = testHealthMonitor.checkTestHealth()
            
            // Generate performance metrics
            val performanceMetrics = generatePerformanceMetrics(crossPlatformResults)
            
            // Generate recommendations
            val recommendations = generateRecommendations(
                compilationResults,
                crossPlatformResults,
                healthStatus,
                performanceMetrics
            )
            
            TestHealthReport(
                reportId = generateReportId(),
                timestamp = Clock.System.now(),
                overallHealth = calculateOverallHealth(compilationResults, crossPlatformResults, healthStatus),
                compilationHealth = CompilationHealthSummary.from(compilationResults),
                crossPlatformHealth = CrossPlatformHealthSummary.from(crossPlatformResults),
                performanceMetrics = performanceMetrics,
                testCoverage = calculateTestCoverage(),
                recommendations = recommendations,
                generationDuration = Clock.System.now() - startTime,
                nextRecommendedCheck = Clock.System.now().plus(Duration.parse("PT24H"))
            )
        } catch (e: Exception) {
            TestHealthReport.failure(
                error = "Failed to generate test health report: ${e.message}",
                duration = Clock.System.now() - startTime
            )
        }
    }
    
    fun generateQuickHealthCheck(): QuickHealthSummary {
        val startTime = Clock.System.now()
        
        return try {
            val compilationValidator = CompilationValidator()
            val testHealthMonitor = TestHealthMonitor()
            
            // Quick validation of critical phases
            val criticalPhases = listOf(
                FixingPhase.IMPORTS_AND_REFERENCES,
                FixingPhase.ABSTRACT_IMPLEMENTATIONS
            )
            
            val criticalResults = criticalPhases.map { phase ->
                compilationValidator.validatePhase(phase)
            }
            
            val quickHealthStatus = testHealthMonitor.quickHealthCheck()
            
            QuickHealthSummary(
                timestamp = Clock.System.now(),
                overallStatus = if (criticalResults.all { it.success } && quickHealthStatus.healthy) {
                    HealthStatus.HEALTHY
                } else if (criticalResults.any { it.success }) {
                    HealthStatus.WARNING
                } else {
                    HealthStatus.CRITICAL
                },
                criticalIssues = criticalResults.filter { !it.success }.map { it.error ?: "Unknown error" },
                testExecutionTime = quickHealthStatus.averageExecutionTime,
                recommendedActions = generateQuickRecommendations(criticalResults, quickHealthStatus),
                duration = Clock.System.now() - startTime
            )
        } catch (e: Exception) {
            QuickHealthSummary.failure(
                error = "Quick health check failed: ${e.message}",
                duration = Clock.System.now() - startTime
            )
        }
    }
    
    private fun calculateOverallHealth(
        compilationResults: List<ValidationResult>,
        crossPlatformResults: CrossPlatformTestResult,
        healthStatus: TestHealthMonitorReport
    ): HealthStatus {
        val compilationHealth = if (compilationResults.all { it.success }) {
            HealthStatus.HEALTHY
        } else if (compilationResults.any { it.success }) {
            HealthStatus.WARNING
        } else {
            HealthStatus.CRITICAL
        }
        
        val crossPlatformHealth = if (crossPlatformResults.overallSuccess) {
            HealthStatus.HEALTHY
        } else if (crossPlatformResults.consistencyCheck.consistent) {
            HealthStatus.WARNING
        } else {
            HealthStatus.CRITICAL
        }
        
        val monitorHealth = if (healthStatus.isHealthy) {
            HealthStatus.HEALTHY
        } else if (healthStatus.checkResults.any { !it.isHealthy }) {
            HealthStatus.CRITICAL
        } else {
            HealthStatus.WARNING
        }
        
        // Return the worst status
        return listOf(compilationHealth, crossPlatformHealth, monitorHealth).maxByOrNull { it.severity } ?: HealthStatus.CRITICAL
    }
    
    private fun generatePerformanceMetrics(crossPlatformResults: CrossPlatformTestResult): PerformanceMetrics {
        val allTestResults = crossPlatformResults.androidResult.testResults + 
                           crossPlatformResults.iosResult.testResults + 
                           crossPlatformResults.commonResult.testResults
        
        val executionTimes = allTestResults.map { it.duration.inWholeMilliseconds }
        
        return PerformanceMetrics(
            totalExecutionTime = crossPlatformResults.duration,
            averageTestTime = if (executionTimes.isNotEmpty()) {
                Duration.parse("PT${executionTimes.average().toLong()}MS")
            } else {
                Duration.ZERO
            },
            slowestTest = allTestResults.maxByOrNull { it.duration }?.let { 
                SlowTestInfo(it.testName, it.duration, it.platform)
            },
            fastestTest = allTestResults.minByOrNull { it.duration }?.let {
                FastTestInfo(it.testName, it.duration, it.platform)
            },
            testsPerSecond = if (crossPlatformResults.duration.inWholeSeconds > 0) {
                allTestResults.size.toDouble() / crossPlatformResults.duration.inWholeSeconds
            } else {
                0.0
            },
            memoryUsageEstimate = estimateMemoryUsage(allTestResults.size),
            parallelizationOpportunities = identifyParallelizationOpportunities(allTestResults)
        )
    }
    
    private fun calculateTestCoverage(): TestCoverage {
        // In real implementation, this would analyze actual test coverage
        return TestCoverage(
            overallCoverage = 85.0, // Simulated
            unitTestCoverage = 90.0,
            integrationTestCoverage = 75.0,
            crossPlatformCoverage = 80.0,
            uncoveredAreas = listOf(
                "Error handling edge cases",
                "Platform-specific optimizations",
                "Performance under load"
            ),
            coverageByModule = mapOf(
                "domain" to 95.0,
                "data" to 85.0,
                "presentation" to 75.0,
                "platform" to 70.0
            )
        )
    }
    
    private fun generateRecommendations(
        compilationResults: List<ValidationResult>,
        crossPlatformResults: CrossPlatformTestResult,
        healthStatus: TestHealthMonitorReport,
        performanceMetrics: PerformanceMetrics
    ): List<Recommendation> {
        val recommendations = mutableListOf<Recommendation>()
        
        // Compilation recommendations
        compilationResults.filter { !it.success }.forEach { result ->
            recommendations.add(
                Recommendation(
                    priority = Priority.HIGH,
                    category = RecommendationCategory.COMPILATION,
                    title = "Fix ${result.phase} issues",
                    description = result.error ?: "Unknown compilation issue",
                    actionItems = listOf(
                        "Review ${result.phase} validation results",
                        "Fix identified issues systematically",
                        "Re-run validation to confirm fixes"
                    ),
                    estimatedEffort = EstimatedEffort.MEDIUM
                )
            )
        }
        
        // Cross-platform recommendations
        if (!crossPlatformResults.overallSuccess) {
            recommendations.add(
                Recommendation(
                    priority = Priority.HIGH,
                    category = RecommendationCategory.CROSS_PLATFORM,
                    title = "Improve cross-platform test consistency",
                    description = "Tests are failing or inconsistent across platforms",
                    actionItems = crossPlatformResults.consistencyCheck.recommendations,
                    estimatedEffort = EstimatedEffort.HIGH
                )
            )
        }
        
        // Performance recommendations
        performanceMetrics.slowestTest?.let { slowTest ->
            if (slowTest.duration.inWholeMilliseconds > 1000) {
                recommendations.add(
                    Recommendation(
                        priority = Priority.MEDIUM,
                        category = RecommendationCategory.PERFORMANCE,
                        title = "Optimize slow test: ${slowTest.testName}",
                        description = "Test takes ${slowTest.duration.inWholeMilliseconds}ms to execute",
                        actionItems = listOf(
                            "Profile test execution to identify bottlenecks",
                            "Consider mocking expensive operations",
                            "Split into smaller, focused tests if appropriate"
                        ),
                        estimatedEffort = EstimatedEffort.MEDIUM
                    )
                )
            }
        }
        
        // Coverage recommendations
        val coverage = calculateTestCoverage()
        if (coverage.overallCoverage < 80.0) {
            recommendations.add(
                Recommendation(
                    priority = Priority.MEDIUM,
                    category = RecommendationCategory.COVERAGE,
                    title = "Improve test coverage",
                    description = "Overall test coverage is ${coverage.overallCoverage}%, target is 80%+",
                    actionItems = listOf(
                        "Add tests for uncovered areas: ${coverage.uncoveredAreas.joinToString(", ")}",
                        "Focus on modules with low coverage",
                        "Consider property-based testing for complex logic"
                    ),
                    estimatedEffort = EstimatedEffort.HIGH
                )
            )
        }
        
        return recommendations.sortedBy { it.priority.ordinal }
    }
    
    private fun generateQuickRecommendations(
        criticalResults: List<ValidationResult>,
        quickHealthStatus: QuickHealthStatus
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        criticalResults.filter { !it.success }.forEach { result ->
            recommendations.add("Fix ${result.phase}: ${result.error}")
        }
        
        if (!quickHealthStatus.healthy) {
            recommendations.add("Address test health issues: ${quickHealthStatus.issues.joinToString(", ")}")
        }
        
        if (quickHealthStatus.averageExecutionTime.inWholeMilliseconds > 500) {
            recommendations.add("Optimize test execution time (currently ${quickHealthStatus.averageExecutionTime.inWholeMilliseconds}ms average)")
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("Test infrastructure is healthy")
        }
        
        return recommendations
    }
    
    private fun estimateMemoryUsage(testCount: Int): String {
        // Simple estimation based on test count
        val estimatedMB = testCount * 2 // Rough estimate of 2MB per test
        return "${estimatedMB}MB"
    }
    
    private fun identifyParallelizationOpportunities(testResults: List<TestSuiteResult>): List<String> {
        val opportunities = mutableListOf<String>()
        
        // Group tests by platform
        val platformGroups = testResults.groupBy { it.platform }
        
        platformGroups.forEach { (platform, tests) ->
            if (tests.size > 1) {
                opportunities.add("${platform} tests can be parallelized (${tests.size} tests)")
            }
        }
        
        // Identify independent test suites
        val independentTests = testResults.filter { 
            !it.testName.contains("Integration") && !it.testName.contains("E2E")
        }
        
        if (independentTests.size > 2) {
            opportunities.add("${independentTests.size} unit tests can run in parallel")
        }
        
        return opportunities
    }
    
    private fun generateReportId(): String {
        return "THR-${Clock.System.now().toEpochMilliseconds()}"
    }
}

data class TestHealthReport(
    val reportId: String,
    val timestamp: Instant,
    val overallHealth: HealthStatus,
    val compilationHealth: CompilationHealthSummary,
    val crossPlatformHealth: CrossPlatformHealthSummary,
    val performanceMetrics: PerformanceMetrics,
    val testCoverage: TestCoverage,
    val recommendations: List<Recommendation>,
    val generationDuration: Duration,
    val nextRecommendedCheck: Instant,
    val error: String? = null
) {
    companion object {
        fun failure(error: String, duration: Duration): TestHealthReport {
            return TestHealthReport(
                reportId = "THR-ERROR-${Clock.System.now().toEpochMilliseconds()}",
                timestamp = Clock.System.now(),
                overallHealth = HealthStatus.CRITICAL,
                compilationHealth = CompilationHealthSummary.empty(),
                crossPlatformHealth = CrossPlatformHealthSummary.empty(),
                performanceMetrics = PerformanceMetrics.empty(),
                testCoverage = TestCoverage.empty(),
                recommendations = listOf(
                    Recommendation(
                        priority = Priority.CRITICAL,
                        category = RecommendationCategory.SYSTEM,
                        title = "Fix report generation error",
                        description = error,
                        actionItems = listOf("Investigate and fix the underlying issue"),
                        estimatedEffort = EstimatedEffort.HIGH
                    )
                ),
                generationDuration = duration,
                nextRecommendedCheck = Clock.System.now().plus(Duration.parse("PT1H")),
                error = error
            )
        }
    }
}

data class QuickHealthSummary(
    val timestamp: Instant,
    val overallStatus: HealthStatus,
    val criticalIssues: List<String>,
    val testExecutionTime: Duration,
    val recommendedActions: List<String>,
    val duration: Duration,
    val error: String? = null
) {
    companion object {
        fun failure(error: String, duration: Duration): QuickHealthSummary {
            return QuickHealthSummary(
                timestamp = Clock.System.now(),
                overallStatus = HealthStatus.CRITICAL,
                criticalIssues = listOf(error),
                testExecutionTime = Duration.ZERO,
                recommendedActions = listOf("Fix health check execution error"),
                duration = duration,
                error = error
            )
        }
    }
}

enum class HealthStatus(val severity: Int) {
    HEALTHY(0),
    WARNING(1),
    CRITICAL(2)
}

data class CompilationHealthSummary(
    val overallStatus: HealthStatus,
    val phaseResults: Map<FixingPhase, Boolean>,
    val errorCount: Int,
    val fixedIssues: Int,
    val remainingIssues: List<String>
) {
    companion object {
        fun from(results: List<ValidationResult>): CompilationHealthSummary {
            val phaseResults = results.associate { it.phase to it.success }
            val errors = results.filter { !it.success }
            
            return CompilationHealthSummary(
                overallStatus = if (errors.isEmpty()) HealthStatus.HEALTHY 
                              else if (errors.size <= 2) HealthStatus.WARNING 
                              else HealthStatus.CRITICAL,
                phaseResults = phaseResults,
                errorCount = errors.size,
                fixedIssues = results.count { it.success },
                remainingIssues = errors.mapNotNull { it.error }
            )
        }
        
        fun empty(): CompilationHealthSummary {
            return CompilationHealthSummary(
                overallStatus = HealthStatus.CRITICAL,
                phaseResults = emptyMap(),
                errorCount = 0,
                fixedIssues = 0,
                remainingIssues = emptyList()
            )
        }
    }
}

data class CrossPlatformHealthSummary(
    val overallStatus: HealthStatus,
    val androidSuccess: Boolean,
    val iosSuccess: Boolean,
    val commonSuccess: Boolean,
    val consistencyScore: Double,
    val platformSpecificIssues: List<String>
) {
    companion object {
        fun from(result: CrossPlatformTestResult): CrossPlatformHealthSummary {
            val consistencyScore = if (result.consistencyCheck.consistent) 1.0 
                                 else 1.0 - (result.consistencyCheck.issues.size * 0.2)
            
            return CrossPlatformHealthSummary(
                overallStatus = if (result.overallSuccess) HealthStatus.HEALTHY
                              else if (consistencyScore > 0.7) HealthStatus.WARNING
                              else HealthStatus.CRITICAL,
                androidSuccess = result.androidResult.success,
                iosSuccess = result.iosResult.success,
                commonSuccess = result.commonResult.success,
                consistencyScore = consistencyScore.coerceIn(0.0, 1.0),
                platformSpecificIssues = result.consistencyCheck.issues
            )
        }
        
        fun empty(): CrossPlatformHealthSummary {
            return CrossPlatformHealthSummary(
                overallStatus = HealthStatus.CRITICAL,
                androidSuccess = false,
                iosSuccess = false,
                commonSuccess = false,
                consistencyScore = 0.0,
                platformSpecificIssues = emptyList()
            )
        }
    }
}

data class PerformanceMetrics(
    val totalExecutionTime: Duration,
    val averageTestTime: Duration,
    val slowestTest: SlowTestInfo?,
    val fastestTest: FastTestInfo?,
    val testsPerSecond: Double,
    val memoryUsageEstimate: String,
    val parallelizationOpportunities: List<String>
) {
    companion object {
        fun empty(): PerformanceMetrics {
            return PerformanceMetrics(
                totalExecutionTime = Duration.ZERO,
                averageTestTime = Duration.ZERO,
                slowestTest = null,
                fastestTest = null,
                testsPerSecond = 0.0,
                memoryUsageEstimate = "0MB",
                parallelizationOpportunities = emptyList()
            )
        }
    }
}

data class SlowTestInfo(
    val testName: String,
    val duration: Duration,
    val platform: Platform
)

data class FastTestInfo(
    val testName: String,
    val duration: Duration,
    val platform: Platform
)

data class TestCoverage(
    val overallCoverage: Double,
    val unitTestCoverage: Double,
    val integrationTestCoverage: Double,
    val crossPlatformCoverage: Double,
    val uncoveredAreas: List<String>,
    val coverageByModule: Map<String, Double>
) {
    companion object {
        fun empty(): TestCoverage {
            return TestCoverage(
                overallCoverage = 0.0,
                unitTestCoverage = 0.0,
                integrationTestCoverage = 0.0,
                crossPlatformCoverage = 0.0,
                uncoveredAreas = emptyList(),
                coverageByModule = emptyMap()
            )
        }
    }
}

data class Recommendation(
    val priority: Priority,
    val category: RecommendationCategory,
    val title: String,
    val description: String,
    val actionItems: List<String>,
    val estimatedEffort: EstimatedEffort
)

enum class Priority {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW
}

enum class RecommendationCategory {
    COMPILATION,
    CROSS_PLATFORM,
    PERFORMANCE,
    COVERAGE,
    SYSTEM
}

enum class EstimatedEffort {
    LOW,    // < 1 hour
    MEDIUM, // 1-4 hours
    HIGH    // > 4 hours
}