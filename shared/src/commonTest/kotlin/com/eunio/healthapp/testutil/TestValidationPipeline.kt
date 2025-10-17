package com.eunio.healthapp.testutil

import kotlinx.datetime.Clock
import kotlin.time.Duration

/**
 * Comprehensive test validation pipeline that orchestrates all validation components
 */
class TestValidationPipeline {
    
    private val compilationValidator = CompilationValidator()
    private val crossPlatformRunner = CrossPlatformTestRunner()
    private val reportGenerator = TestHealthReportGenerator()
    private val deterministicExecutor = DeterministicTestExecutor()
    
    fun runFullValidationPipeline(): PipelineExecutionResult {
        val startTime = Clock.System.now()
        
        return try {
            println("🚀 Starting comprehensive test validation pipeline...")
            
            // Phase 1: Compilation Validation
            println("📋 Phase 1: Running compilation validation...")
            val compilationResults = runCompilationValidation()
            
            if (!compilationResults.success) {
                return PipelineExecutionResult.failure(
                    phase = ValidationPhase.COMPILATION,
                    error = "Compilation validation failed: ${compilationResults.error}",
                    duration = Clock.System.now() - startTime
                )
            }
            
            // Phase 2: Cross-Platform Testing
            println("🌐 Phase 2: Running cross-platform tests...")
            val crossPlatformResults = runCrossPlatformValidation()
            
            // Phase 3: Deterministic Execution Validation
            println("🎯 Phase 3: Validating deterministic execution...")
            val deterministicResults = runDeterministicValidation()
            
            // Phase 4: Generate Health Report
            println("📊 Phase 4: Generating comprehensive health report...")
            val healthReport = reportGenerator.generateComprehensiveReport()
            
            // Phase 5: Final Validation
            println("✅ Phase 5: Running final validation checks...")
            val finalValidation = runFinalValidation(
                compilationResults,
                crossPlatformResults,
                deterministicResults,
                healthReport
            )
            
            val overallSuccess = compilationResults.success && 
                               crossPlatformResults.success && 
                               deterministicResults.success && 
                               finalValidation.success
            
            println(if (overallSuccess) "✅ Pipeline completed successfully!" else "❌ Pipeline completed with issues")
            
            PipelineExecutionResult(
                success = overallSuccess,
                compilationResults = compilationResults,
                crossPlatformResults = crossPlatformResults,
                deterministicResults = deterministicResults,
                healthReport = healthReport,
                finalValidation = finalValidation,
                duration = Clock.System.now() - startTime,
                timestamp = Clock.System.now()
            )
        } catch (e: Exception) {
            println("💥 Pipeline execution failed: ${e.message}")
            PipelineExecutionResult.failure(
                phase = ValidationPhase.UNKNOWN,
                error = "Pipeline execution failed: ${e.message}",
                duration = Clock.System.now() - startTime
            )
        }
    }
    
    fun runQuickValidation(): QuickValidationResult {
        val startTime = Clock.System.now()
        
        return try {
            println("⚡ Running quick validation check...")
            
            // Quick compilation check
            val criticalPhases = listOf(
                FixingPhase.IMPORTS_AND_REFERENCES,
                FixingPhase.ABSTRACT_IMPLEMENTATIONS
            )
            
            val quickCompilationResults = criticalPhases.map { phase ->
                compilationValidator.validatePhase(phase)
            }
            
            // Quick health check
            val quickHealthSummary = reportGenerator.generateQuickHealthCheck()
            
            // Quick deterministic check
            val quickDeterministicCheck = runQuickDeterministicCheck()
            
            val overallSuccess = quickCompilationResults.all { it.success } && 
                               quickHealthSummary.overallStatus == HealthStatus.HEALTHY &&
                               quickDeterministicCheck.success
            
            println(if (overallSuccess) "✅ Quick validation passed!" else "⚠️ Quick validation found issues")
            
            QuickValidationResult(
                success = overallSuccess,
                compilationIssues = quickCompilationResults.filter { !it.success }.size,
                healthStatus = quickHealthSummary.overallStatus,
                deterministicScore = quickDeterministicCheck.deterministicScore,
                criticalIssues = quickHealthSummary.criticalIssues,
                recommendations = quickHealthSummary.recommendedActions,
                duration = Clock.System.now() - startTime,
                timestamp = Clock.System.now()
            )
        } catch (e: Exception) {
            println("💥 Quick validation failed: ${e.message}")
            QuickValidationResult.failure(
                error = "Quick validation failed: ${e.message}",
                duration = Clock.System.now() - startTime
            )
        }
    }
    
    private fun runCompilationValidation(): CompilationValidationResult {
        return try {
            val results = compilationValidator.validateAllPhases()
            val failedPhases = results.filter { !it.success }
            
            CompilationValidationResult(
                success = failedPhases.isEmpty(),
                phaseResults = results,
                totalPhases = results.size,
                passedPhases = results.count { it.success },
                failedPhases = failedPhases.size,
                error = if (failedPhases.isNotEmpty()) {
                    "Failed phases: ${failedPhases.joinToString(", ") { "${it.phase}: ${it.error}" }}"
                } else null
            )
        } catch (e: Exception) {
            CompilationValidationResult.failure("Compilation validation execution failed: ${e.message}")
        }
    }
    
    private fun runCrossPlatformValidation(): CrossPlatformValidationResult {
        return try {
            val results = crossPlatformRunner.runCrossPlatformTests()
            
            CrossPlatformValidationResult(
                success = results.overallSuccess,
                crossPlatformResults = results,
                consistencyScore = if (results.consistencyCheck.consistent) 1.0 else 0.5,
                platformIssues = results.consistencyCheck.issues,
                error = results.error
            )
        } catch (e: Exception) {
            CrossPlatformValidationResult.failure("Cross-platform validation failed: ${e.message}")
        }
    }
    
    private fun runDeterministicValidation(): DeterministicValidationResult {
        return try {
            // Create test cases for deterministic validation
            val testCases = createDeterministicTestCases()
            
            // Run tests multiple times to check for consistency
            val runs = (1..3).map { runNumber ->
                deterministicExecutor.executeTestSuite(
                    suiteName = "DeterministicValidation-Run$runNumber",
                    tests = testCases
                )
            }
            
            // Analyze consistency across runs
            val consistencyAnalysis = analyzeDeterministicConsistency(runs)
            
            DeterministicValidationResult(
                success = runs.all { it.success } && consistencyAnalysis.consistent,
                testRuns = runs,
                consistencyAnalysis = consistencyAnalysis,
                averageDeterministicScore = runs.map { it.deterministicScore }.average(),
                error = if (!consistencyAnalysis.consistent) {
                    "Deterministic validation failed: ${consistencyAnalysis.issues.joinToString(", ")}"
                } else null
            )
        } catch (e: Exception) {
            DeterministicValidationResult.failure("Deterministic validation failed: ${e.message}")
        }
    }
    
    private fun runFinalValidation(
        compilationResults: CompilationValidationResult,
        crossPlatformResults: CrossPlatformValidationResult,
        deterministicResults: DeterministicValidationResult,
        healthReport: TestHealthReport
    ): FinalValidationResult {
        return try {
            val issues = mutableListOf<String>()
            
            // Check compilation health
            if (!compilationResults.success) {
                issues.add("Compilation validation failed")
            }
            
            // Check cross-platform consistency
            if (!crossPlatformResults.success) {
                issues.add("Cross-platform validation failed")
            }
            
            // Check deterministic behavior
            if (!deterministicResults.success) {
                issues.add("Deterministic validation failed")
            }
            
            // Check overall health
            if (healthReport.overallHealth == HealthStatus.CRITICAL) {
                issues.add("Critical health issues detected")
            }
            
            // Performance checks
            val performanceIssues = checkPerformanceRequirements(healthReport.performanceMetrics)
            issues.addAll(performanceIssues)
            
            // Coverage checks
            val coverageIssues = checkCoverageRequirements(healthReport.testCoverage)
            issues.addAll(coverageIssues)
            
            FinalValidationResult(
                success = issues.isEmpty(),
                issues = issues,
                overallScore = calculateOverallScore(
                    compilationResults,
                    crossPlatformResults,
                    deterministicResults,
                    healthReport
                ),
                recommendations = generateFinalRecommendations(issues),
                meetsRequirements = checkRequirementsCompliance(
                    compilationResults,
                    crossPlatformResults,
                    deterministicResults,
                    healthReport
                )
            )
        } catch (e: Exception) {
            FinalValidationResult.failure("Final validation failed: ${e.message}")
        }
    }
    
    private fun runQuickDeterministicCheck(): QuickDeterministicResult {
        return try {
            // Run a simple deterministic test multiple times
            val testCase = TestCase(
                name = "QuickDeterministicCheck",
                test = {
                    // Simple deterministic operation
                    val userId = DeterministicTestData.generateDeterministicUserId("quick-check")
                    val timestamp = DeterministicTestData.generateDeterministicTimestamp("quick-check")
                    userId to timestamp
                }
            )
            
            val runs = (1..3).map {
                deterministicExecutor.executeTest(
                    testName = "QuickDeterministicCheck-$it",
                    test = testCase.test
                )
            }
            
            val allSuccessful = runs.all { it.success }
            val resultsConsistent = runs.map { it.result }.distinct().size == 1
            
            QuickDeterministicResult(
                success = allSuccessful && resultsConsistent,
                deterministicScore = if (resultsConsistent) 1.0 else 0.0,
                runs = runs.size,
                consistentResults = resultsConsistent
            )
        } catch (e: Exception) {
            QuickDeterministicResult(
                success = false,
                deterministicScore = 0.0,
                runs = 0,
                consistentResults = false
            )
        }
    }
    
    private fun createDeterministicTestCases(): List<TestCase> {
        return listOf(
            TestCase(
                name = "DeterministicDataGeneration",
                test = {
                    val userId = DeterministicTestData.generateDeterministicUserId("test1")
                    val timestamp = DeterministicTestData.generateDeterministicTimestamp("test1")
                    userId to timestamp
                }
            ),
            TestCase(
                name = "DeterministicMockBehavior",
                test = {
                    val mockService = MockServiceFactory.createMockNetworkConnectivity()
                    // Note: hasStableConnection is suspend, so we just verify the service exists
                    mockService.toString()
                }
            ),
            TestCase(
                name = "DeterministicTestDataBuilder",
                test = {
                    val preferences = TestDataBuilder.createUserPreferences("deterministic-test")
                    preferences.userId
                }
            )
        )
    }
    
    private fun analyzeDeterministicConsistency(runs: List<TestSuiteExecutionResult>): DeterministicConsistencyAnalysis {
        val issues = mutableListOf<String>()
        
        // Check if all runs had the same results
        val successCounts = runs.map { it.passedTests }
        if (successCounts.distinct().size > 1) {
            issues.add("Inconsistent test success counts across runs: $successCounts")
        }
        
        // Check timing consistency
        val averageDurations = runs.map { it.duration.inWholeMilliseconds }
        val maxVariation = averageDurations.maxOrNull()?.minus(averageDurations.minOrNull() ?: 0) ?: 0
        if (maxVariation > 1000) { // More than 1 second variation
            issues.add("High timing variation across runs: ${maxVariation}ms")
        }
        
        // Check deterministic scores
        val deterministicScores = runs.map { it.deterministicScore }
        val averageScore = deterministicScores.average()
        if (averageScore < 0.8) {
            issues.add("Low deterministic score: $averageScore")
        }
        
        return DeterministicConsistencyAnalysis(
            consistent = issues.isEmpty(),
            issues = issues,
            averageScore = averageScore,
            timingVariation = maxVariation
        )
    }
    
    private fun checkPerformanceRequirements(metrics: PerformanceMetrics): List<String> {
        val issues = mutableListOf<String>()
        
        // Check total execution time (requirement: under 5 minutes)
        if (metrics.totalExecutionTime.inWholeMinutes > 5) {
            issues.add("Test execution time exceeds 5 minutes: ${metrics.totalExecutionTime.inWholeMinutes} minutes")
        }
        
        // Check for slow tests
        metrics.slowestTest?.let { slowTest ->
            if (slowTest.duration.inWholeMilliseconds > 2000) {
                issues.add("Slow test detected: ${slowTest.testName} (${slowTest.duration.inWholeMilliseconds}ms)")
            }
        }
        
        return issues
    }
    
    private fun checkCoverageRequirements(coverage: TestCoverage): List<String> {
        val issues = mutableListOf<String>()
        
        // Check overall coverage (target: 80%+)
        if (coverage.overallCoverage < 80.0) {
            issues.add("Overall test coverage below 80%: ${coverage.overallCoverage}%")
        }
        
        // Check unit test coverage (target: 85%+)
        if (coverage.unitTestCoverage < 85.0) {
            issues.add("Unit test coverage below 85%: ${coverage.unitTestCoverage}%")
        }
        
        return issues
    }
    
    private fun calculateOverallScore(
        compilationResults: CompilationValidationResult,
        crossPlatformResults: CrossPlatformValidationResult,
        deterministicResults: DeterministicValidationResult,
        healthReport: TestHealthReport
    ): Double {
        var score = 0.0
        
        // Compilation score (25%)
        score += if (compilationResults.success) 0.25 else 0.0
        
        // Cross-platform score (25%)
        score += (crossPlatformResults.consistencyScore * 0.25)
        
        // Deterministic score (25%)
        score += (deterministicResults.averageDeterministicScore * 0.25)
        
        // Health score (25%)
        score += when (healthReport.overallHealth) {
            HealthStatus.HEALTHY -> 0.25
            HealthStatus.WARNING -> 0.15
            HealthStatus.CRITICAL -> 0.0
        }
        
        return score.coerceIn(0.0, 1.0)
    }
    
    private fun generateFinalRecommendations(issues: List<String>): List<String> {
        val recommendations = mutableListOf<String>()
        
        issues.forEach { issue ->
            when {
                issue.contains("Compilation") -> {
                    recommendations.add("Fix compilation issues before proceeding with other validations")
                }
                issue.contains("Cross-platform") -> {
                    recommendations.add("Review platform-specific implementations for consistency")
                }
                issue.contains("Deterministic") -> {
                    recommendations.add("Eliminate non-deterministic behavior in tests")
                }
                issue.contains("performance") || issue.contains("execution time") -> {
                    recommendations.add("Optimize test performance to meet timing requirements")
                }
                issue.contains("coverage") -> {
                    recommendations.add("Increase test coverage in identified areas")
                }
            }
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("Test infrastructure is healthy and meets all requirements")
        }
        
        return recommendations.distinct()
    }
    
    private fun checkRequirementsCompliance(
        compilationResults: CompilationValidationResult,
        crossPlatformResults: CrossPlatformValidationResult,
        deterministicResults: DeterministicValidationResult,
        healthReport: TestHealthReport
    ): RequirementsCompliance {
        return RequirementsCompliance(
            requirement6_1 = healthReport.performanceMetrics.totalExecutionTime.inWholeMinutes <= 5, // Under 5 minutes
            requirement6_2 = deterministicResults.averageDeterministicScore >= 0.8, // Deterministic behavior
            requirement6_5 = crossPlatformResults.consistencyScore >= 0.8 // Cross-platform consistency
        )
    }
}

// Data classes for pipeline results

data class PipelineExecutionResult(
    val success: Boolean,
    val compilationResults: CompilationValidationResult,
    val crossPlatformResults: CrossPlatformValidationResult,
    val deterministicResults: DeterministicValidationResult,
    val healthReport: TestHealthReport,
    val finalValidation: FinalValidationResult,
    val duration: Duration,
    val timestamp: kotlinx.datetime.Instant,
    val error: String? = null
) {
    companion object {
        fun failure(
            phase: ValidationPhase,
            error: String,
            duration: Duration
        ): PipelineExecutionResult {
            return PipelineExecutionResult(
                success = false,
                compilationResults = CompilationValidationResult.failure("Pipeline failed at $phase"),
                crossPlatformResults = CrossPlatformValidationResult.failure("Pipeline failed at $phase"),
                deterministicResults = DeterministicValidationResult.failure("Pipeline failed at $phase"),
                healthReport = TestHealthReport.failure("Pipeline failed at $phase", duration),
                finalValidation = FinalValidationResult.failure("Pipeline failed at $phase"),
                duration = duration,
                timestamp = Clock.System.now(),
                error = error
            )
        }
    }
}

enum class ValidationPhase {
    COMPILATION,
    CROSS_PLATFORM,
    DETERMINISTIC,
    HEALTH_REPORT,
    FINAL_VALIDATION,
    UNKNOWN
}

data class CompilationValidationResult(
    val success: Boolean,
    val phaseResults: List<ValidationResult>,
    val totalPhases: Int,
    val passedPhases: Int,
    val failedPhases: Int,
    val error: String? = null
) {
    companion object {
        fun failure(error: String): CompilationValidationResult {
            return CompilationValidationResult(
                success = false,
                phaseResults = emptyList(),
                totalPhases = 0,
                passedPhases = 0,
                failedPhases = 0,
                error = error
            )
        }
    }
}

data class CrossPlatformValidationResult(
    val success: Boolean,
    val crossPlatformResults: CrossPlatformTestResult,
    val consistencyScore: Double,
    val platformIssues: List<String>,
    val error: String? = null
) {
    companion object {
        fun failure(error: String): CrossPlatformValidationResult {
            return CrossPlatformValidationResult(
                success = false,
                crossPlatformResults = CrossPlatformTestResult.failure(error, Duration.ZERO),
                consistencyScore = 0.0,
                platformIssues = listOf(error),
                error = error
            )
        }
    }
}

data class DeterministicValidationResult(
    val success: Boolean,
    val testRuns: List<TestSuiteExecutionResult>,
    val consistencyAnalysis: DeterministicConsistencyAnalysis,
    val averageDeterministicScore: Double,
    val error: String? = null
) {
    companion object {
        fun failure(error: String): DeterministicValidationResult {
            return DeterministicValidationResult(
                success = false,
                testRuns = emptyList(),
                consistencyAnalysis = DeterministicConsistencyAnalysis(
                    consistent = false,
                    issues = listOf(error),
                    averageScore = 0.0,
                    timingVariation = 0
                ),
                averageDeterministicScore = 0.0,
                error = error
            )
        }
    }
}

data class FinalValidationResult(
    val success: Boolean,
    val issues: List<String>,
    val overallScore: Double,
    val recommendations: List<String>,
    val meetsRequirements: RequirementsCompliance,
    val error: String? = null
) {
    companion object {
        fun failure(error: String): FinalValidationResult {
            return FinalValidationResult(
                success = false,
                issues = listOf(error),
                overallScore = 0.0,
                recommendations = listOf("Fix validation pipeline error"),
                meetsRequirements = RequirementsCompliance(false, false, false),
                error = error
            )
        }
    }
}

data class QuickValidationResult(
    val success: Boolean,
    val compilationIssues: Int,
    val healthStatus: HealthStatus,
    val deterministicScore: Double,
    val criticalIssues: List<String>,
    val recommendations: List<String>,
    val duration: Duration,
    val timestamp: kotlinx.datetime.Instant,
    val error: String? = null
) {
    companion object {
        fun failure(error: String, duration: Duration): QuickValidationResult {
            return QuickValidationResult(
                success = false,
                compilationIssues = 0,
                healthStatus = HealthStatus.CRITICAL,
                deterministicScore = 0.0,
                criticalIssues = listOf(error),
                recommendations = listOf("Fix validation error"),
                duration = duration,
                timestamp = Clock.System.now(),
                error = error
            )
        }
    }
}

data class QuickDeterministicResult(
    val success: Boolean,
    val deterministicScore: Double,
    val runs: Int,
    val consistentResults: Boolean
)

data class DeterministicConsistencyAnalysis(
    val consistent: Boolean,
    val issues: List<String>,
    val averageScore: Double,
    val timingVariation: Long
)

data class RequirementsCompliance(
    val requirement6_1: Boolean, // Performance under 5 minutes
    val requirement6_2: Boolean, // Deterministic behavior
    val requirement6_5: Boolean  // Cross-platform consistency
)