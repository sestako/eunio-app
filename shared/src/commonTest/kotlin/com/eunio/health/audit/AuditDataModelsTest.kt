package com.eunio.health.audit

import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Unit tests for Audit Data Models
 * Verifies task 1 implementation: data structures for audit results and assessments
 */
class AuditDataModelsTest {

    @Test
    fun `test AuditResult creation with all required fields`() {
        val auditResult = AuditResult(
            auditId = "audit-001",
            timestamp = Clock.System.now(),
            overallScore = AuditScore(
                overall = 5.0,
                infrastructure = 3.0,
                businessLogic = 4.0,
                dataLayer = 5.0,
                presentation = 6.0,
                userExperience = 5.0,
                quality = 7.0,
                accessibility = 4.0,
                platformIntegration = 5.0
            ),
            layerAssessments = emptyList(),
            criticalIssues = emptyList(),
            remediationPlan = RemediationPlan(
                phases = emptyList(),
                totalEstimatedEffort = EffortEstimate(
                    level = EffortLevel.HIGH,
                    estimatedDays = 60..90,
                    complexity = ComplexityLevel.COMPLEX,
                    dependencies = emptyList(),
                    skillsRequired = emptyList()
                ),
                criticalPath = emptyList(),
                dependencies = emptyMap(),
                milestones = emptyList(),
                riskAssessment = RiskAssessment(
                    technicalRisks = emptyList(),
                    businessRisks = emptyList(),
                    mitigationStrategies = emptyList()
                )
            ),
            executiveSummary = ExecutiveSummary(
                overallAssessment = "Test assessment",
                keyFindings = emptyList(),
                businessImpact = "Test impact",
                recommendedActions = emptyList(),
                timeToMarket = "60-90 days",
                investmentRequired = "High"
            )
        )
        
        assertEquals("audit-001", auditResult.auditId)
        assertEquals(5.0, auditResult.overallScore.overall)
    }

    @Test
    fun `test Issue creation with all severity levels`() {
        val criticalIssue = Issue(
            id = "issue-001",
            severity = IssueSeverity.CRITICAL,
            category = IssueCategory.DEPENDENCY_INJECTION,
            title = "Koin not initialized",
            description = "Dependency injection is not configured",
            location = "IOSKoinInitializer.kt",
            impact = "Blocks all shared functionality",
            estimatedEffort = EffortEstimate(
                level = EffortLevel.MEDIUM,
                estimatedDays = 3..5,
                complexity = ComplexityLevel.MODERATE,
                dependencies = emptyList(),
                skillsRequired = listOf("Kotlin", "Koin")
            ),
            blocksOtherWork = true,
            affectedFeatures = listOf("All features")
        )
        
        assertEquals(IssueSeverity.CRITICAL, criticalIssue.severity)
        assertTrue(criticalIssue.blocksOtherWork)
    }

    @Test
    fun `test all IssueSeverity levels are defined`() {
        val severities = IssueSeverity.values()
        
        assertEquals(4, severities.size)
        assertTrue(severities.contains(IssueSeverity.CRITICAL))
        assertTrue(severities.contains(IssueSeverity.HIGH))
        assertTrue(severities.contains(IssueSeverity.MEDIUM))
        assertTrue(severities.contains(IssueSeverity.LOW))
    }

    @Test
    fun `test all IssueCategory types are defined`() {
        val categories = IssueCategory.values()
        
        assertTrue(categories.size >= 12, "Should have at least 12 issue categories")
        assertTrue(categories.contains(IssueCategory.DEPENDENCY_INJECTION))
        assertTrue(categories.contains(IssueCategory.SERVICE_IMPLEMENTATION))
        assertTrue(categories.contains(IssueCategory.DATA_PERSISTENCE))
        assertTrue(categories.contains(IssueCategory.UI_FUNCTIONALITY))
        assertTrue(categories.contains(IssueCategory.BUSINESS_LOGIC))
        assertTrue(categories.contains(IssueCategory.TESTING))
        assertTrue(categories.contains(IssueCategory.ACCESSIBILITY))
        assertTrue(categories.contains(IssueCategory.AUTHENTICATION))
        assertTrue(categories.contains(IssueCategory.NAVIGATION))
    }

    @Test
    fun `test EffortEstimate with different effort levels`() {
        val lowEffort = EffortEstimate(
            level = EffortLevel.LOW,
            estimatedDays = 1..3,
            complexity = ComplexityLevel.SIMPLE,
            dependencies = emptyList(),
            skillsRequired = listOf("Kotlin")
        )
        
        val highEffort = EffortEstimate(
            level = EffortLevel.HIGH,
            estimatedDays = 10..15,
            complexity = ComplexityLevel.COMPLEX,
            dependencies = listOf("Koin setup", "Service implementations"),
            skillsRequired = listOf("Kotlin", "Firebase", "Architecture")
        )
        
        assertEquals(EffortLevel.LOW, lowEffort.level)
        assertEquals(EffortLevel.HIGH, highEffort.level)
        assertTrue(highEffort.dependencies.isNotEmpty())
    }

    @Test
    fun `test FeatureAssessment with different statuses`() {
        val notImplemented = FeatureAssessment(
            featureName = "Data Export",
            currentStatus = FeatureStatus.NOT_IMPLEMENTED,
            functionalityPercentage = 0.0,
            missingComponents = listOf("Export service", "File handling", "Share functionality"),
            implementedComponents = emptyList(),
            userImpact = UserImpact(
                affectedUserJourneys = listOf("Data backup", "Data sharing"),
                workaroundAvailable = false,
                userExperienceRating = 1.0,
                accessibilityImpact = AccessibilityImpact(
                    voiceOverSupport = AccessibilitySupport.NONE,
                    dynamicTypeSupport = AccessibilitySupport.NONE,
                    contrastCompliance = ContrastCompliance(
                        wcagLevel = WCAGLevel.NONE,
                        compliancePercentage = 0.0,
                        failingElements = emptyList()
                    ),
                    keyboardNavigation = AccessibilitySupport.NONE
                )
            ),
            businessValue = BusinessValue(
                priority = BusinessPriority.MEDIUM,
                revenueImpact = RevenueImpact.LOW,
                userRetentionImpact = RetentionImpact.MEDIUM,
                competitiveAdvantage = CompetitiveAdvantage.PARITY
            ),
            technicalDebt = TechnicalDebt(
                debtLevel = DebtLevel.LOW,
                maintainabilityScore = 8.0,
                testCoverage = 0.0,
                codeQualityIssues = emptyList()
            )
        )
        
        assertEquals(FeatureStatus.NOT_IMPLEMENTED, notImplemented.currentStatus)
        assertEquals(0.0, notImplemented.functionalityPercentage)
        assertFalse(notImplemented.userImpact.workaroundAvailable)
    }

    @Test
    fun `test all FeatureStatus types are defined`() {
        val statuses = FeatureStatus.values()
        
        assertEquals(4, statuses.size)
        assertTrue(statuses.contains(FeatureStatus.NOT_IMPLEMENTED))
        assertTrue(statuses.contains(FeatureStatus.NON_FUNCTIONAL))
        assertTrue(statuses.contains(FeatureStatus.PARTIALLY_IMPLEMENTED))
        assertTrue(statuses.contains(FeatureStatus.COMPLETE))
    }

    @Test
    fun `test BusinessPriority levels are defined`() {
        val priorities = BusinessPriority.values()
        
        assertEquals(4, priorities.size)
        assertTrue(priorities.contains(BusinessPriority.CRITICAL))
        assertTrue(priorities.contains(BusinessPriority.HIGH))
        assertTrue(priorities.contains(BusinessPriority.MEDIUM))
        assertTrue(priorities.contains(BusinessPriority.LOW))
    }

    @Test
    fun `test AccessibilitySupport levels are defined`() {
        val supportLevels = AccessibilitySupport.values()
        
        assertEquals(4, supportLevels.size)
        assertTrue(supportLevels.contains(AccessibilitySupport.FULL))
        assertTrue(supportLevels.contains(AccessibilitySupport.PARTIAL))
        assertTrue(supportLevels.contains(AccessibilitySupport.MINIMAL))
        assertTrue(supportLevels.contains(AccessibilitySupport.NONE))
    }

    @Test
    fun `test WCAGLevel compliance levels are defined`() {
        val wcagLevels = WCAGLevel.values()
        
        assertEquals(4, wcagLevels.size)
        assertTrue(wcagLevels.contains(WCAGLevel.AAA))
        assertTrue(wcagLevels.contains(WCAGLevel.AA))
        assertTrue(wcagLevels.contains(WCAGLevel.A))
        assertTrue(wcagLevels.contains(WCAGLevel.NONE))
    }

    @Test
    fun `test RemediationPlan structure`() {
        val phase1 = RemediationPhase(
            phaseNumber = 1,
            name = "Infrastructure Setup",
            description = "Fix core infrastructure issues",
            tasks = listOf(
                RemediationTask(
                    taskId = "task-001",
                    title = "Initialize Koin",
                    description = "Set up dependency injection",
                    category = IssueCategory.DEPENDENCY_INJECTION,
                    effort = EffortEstimate(
                        level = EffortLevel.MEDIUM,
                        estimatedDays = 3..5,
                        complexity = ComplexityLevel.MODERATE,
                        dependencies = emptyList(),
                        skillsRequired = listOf("Kotlin", "Koin")
                    ),
                    priority = TaskPriority.BLOCKER,
                    assignedIssues = listOf("issue-001"),
                    acceptanceCriteria = listOf("Koin initializes successfully", "ViewModels can be instantiated")
                )
            ),
            estimatedDuration = 5..10,
            prerequisites = emptyList(),
            deliverables = listOf("Working dependency injection"),
            successCriteria = listOf("All shared modules accessible")
        )
        
        val plan = RemediationPlan(
            phases = listOf(phase1),
            totalEstimatedEffort = EffortEstimate(
                level = EffortLevel.HIGH,
                estimatedDays = 60..90,
                complexity = ComplexityLevel.COMPLEX,
                dependencies = emptyList(),
                skillsRequired = listOf("Kotlin", "KMP", "iOS", "Android")
            ),
            criticalPath = listOf("Koin setup", "Service implementations", "UI connections"),
            dependencies = mapOf("Service implementations" to listOf("Koin setup")),
            milestones = listOf(
                Milestone(
                    name = "Infrastructure Complete",
                    description = "All infrastructure issues resolved",
                    targetDate = "Week 2",
                    successCriteria = listOf("Koin working", "Services implemented"),
                    deliverables = listOf("Working DI", "Core services"),
                    dependentTasks = listOf("task-001")
                )
            ),
            riskAssessment = RiskAssessment(
                technicalRisks = emptyList(),
                businessRisks = emptyList(),
                mitigationStrategies = emptyList()
            )
        )
        
        assertEquals(1, plan.phases.size)
        assertEquals("Infrastructure Setup", plan.phases[0].name)
        assertEquals(TaskPriority.BLOCKER, plan.phases[0].tasks[0].priority)
    }

    @Test
    fun `test TaskPriority levels are defined`() {
        val priorities = TaskPriority.values()
        
        assertEquals(5, priorities.size)
        assertTrue(priorities.contains(TaskPriority.BLOCKER))
        assertTrue(priorities.contains(TaskPriority.CRITICAL))
        assertTrue(priorities.contains(TaskPriority.HIGH))
        assertTrue(priorities.contains(TaskPriority.MEDIUM))
        assertTrue(priorities.contains(TaskPriority.LOW))
    }

    @Test
    fun `test Risk assessment structure`() {
        val risk = Risk(
            id = "risk-001",
            description = "Timeline may slip due to complexity",
            probability = RiskProbability.MEDIUM,
            impact = RiskImpact.HIGH,
            category = RiskCategory.TIMELINE
        )
        
        val mitigation = MitigationStrategy(
            riskId = "risk-001",
            strategy = "Break work into smaller increments",
            owner = "Tech Lead",
            timeline = "Ongoing"
        )
        
        assertEquals(RiskProbability.MEDIUM, risk.probability)
        assertEquals(RiskImpact.HIGH, risk.impact)
        assertEquals("risk-001", mitigation.riskId)
    }

    @Test
    fun `test all RiskCategory types are defined`() {
        val categories = RiskCategory.values()
        
        assertEquals(5, categories.size)
        assertTrue(categories.contains(RiskCategory.TECHNICAL))
        assertTrue(categories.contains(RiskCategory.RESOURCE))
        assertTrue(categories.contains(RiskCategory.TIMELINE))
        assertTrue(categories.contains(RiskCategory.BUSINESS))
        assertTrue(categories.contains(RiskCategory.EXTERNAL))
    }

    @Test
    fun `test ExecutiveSummary structure`() {
        val summary = ExecutiveSummary(
            overallAssessment = "App has good architecture but lacks implementation",
            keyFindings = listOf(
                "Dependency injection not initialized",
                "15+ services not implemented",
                "0% of features fully functional"
            ),
            businessImpact = "App cannot be released in current state",
            recommendedActions = listOf(
                "Initialize Koin dependency injection",
                "Implement core services",
                "Connect UI to business logic"
            ),
            timeToMarket = "60-90 days for MVP",
            investmentRequired = "High - significant development effort needed"
        )
        
        assertEquals(3, summary.keyFindings.size)
        assertEquals(3, summary.recommendedActions.size)
        assertTrue(summary.overallAssessment.isNotEmpty())
    }

    @Test
    fun `test LayerAssessment structure`() {
        val assessment = LayerAssessment(
            layer = AuditLayer.INFRASTRUCTURE,
            score = 2.5,
            functionalityPercentage = 15.0,
            issues = listOf(
                Issue(
                    id = "infra-001",
                    severity = IssueSeverity.CRITICAL,
                    category = IssueCategory.DEPENDENCY_INJECTION,
                    title = "Koin not initialized",
                    description = "DI framework not set up",
                    location = "IOSKoinInitializer.kt",
                    impact = "Blocks all functionality",
                    estimatedEffort = EffortEstimate(
                        level = EffortLevel.MEDIUM,
                        estimatedDays = 3..5,
                        complexity = ComplexityLevel.MODERATE,
                        dependencies = emptyList(),
                        skillsRequired = listOf("Kotlin", "Koin")
                    ),
                    blocksOtherWork = true,
                    affectedFeatures = listOf("All")
                )
            ),
            recommendations = listOf(
                Recommendation(
                    title = "Initialize Koin",
                    description = "Set up dependency injection framework",
                    rationale = "Required for all shared functionality",
                    effort = EffortEstimate(
                        level = EffortLevel.MEDIUM,
                        estimatedDays = 3..5,
                        complexity = ComplexityLevel.MODERATE,
                        dependencies = emptyList(),
                        skillsRequired = listOf("Kotlin", "Koin")
                    ),
                    expectedBenefit = "Enables instantiation of ViewModels and Use Cases"
                )
            ),
            detailedFindings = listOf(
                DetailedFinding(
                    component = "Dependency Injection",
                    finding = "Koin initialization is commented out",
                    evidence = listOf("IOSKoinInitializer.kt line 15"),
                    recommendation = "Uncomment and configure Koin",
                    priority = TaskPriority.BLOCKER
                )
            )
        )
        
        assertEquals(AuditLayer.INFRASTRUCTURE, assessment.layer)
        assertEquals(2.5, assessment.score)
        assertEquals(15.0, assessment.functionalityPercentage)
        assertEquals(1, assessment.issues.size)
        assertEquals(1, assessment.recommendations.size)
    }

    @Test
    fun `test TechnicalDebt assessment`() {
        val debt = TechnicalDebt(
            debtLevel = DebtLevel.HIGH,
            maintainabilityScore = 3.0,
            testCoverage = 0.0,
            codeQualityIssues = listOf(
                CodeQualityIssue(
                    type = CodeQualityType.MISSING_TESTS,
                    description = "200+ tests exist but test non-functional code",
                    severity = IssueSeverity.HIGH,
                    location = "Test suite"
                ),
                CodeQualityIssue(
                    type = CodeQualityType.ARCHITECTURE_VIOLATION,
                    description = "Good architecture but no implementation",
                    severity = IssueSeverity.CRITICAL,
                    location = "Entire codebase"
                )
            )
        )
        
        assertEquals(DebtLevel.HIGH, debt.debtLevel)
        assertEquals(0.0, debt.testCoverage)
        assertEquals(2, debt.codeQualityIssues.size)
    }

    @Test
    fun `test all CodeQualityType categories are defined`() {
        val types = CodeQualityType.values()
        
        assertTrue(types.size >= 7)
        assertTrue(types.contains(CodeQualityType.ARCHITECTURE_VIOLATION))
        assertTrue(types.contains(CodeQualityType.MISSING_ERROR_HANDLING))
        assertTrue(types.contains(CodeQualityType.POOR_SEPARATION_OF_CONCERNS))
        assertTrue(types.contains(CodeQualityType.MISSING_TESTS))
        assertTrue(types.contains(CodeQualityType.PERFORMANCE_ISSUE))
        assertTrue(types.contains(CodeQualityType.SECURITY_VULNERABILITY))
        assertTrue(types.contains(CodeQualityType.ACCESSIBILITY_VIOLATION))
    }
}
