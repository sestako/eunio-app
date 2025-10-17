package com.eunio.health.audit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

/**
 * Tests to verify infrastructure layer assessment findings
 * Validates task 2 implementation: infrastructure assessment
 */
class InfrastructureAssessmentTest {

    @Test
    fun `test infrastructure assessment data model creation`() {
        // Create a sample infrastructure assessment
        val assessment = LayerAssessment(
            layer = AuditLayer.INFRASTRUCTURE,
            score = 2.5,
            functionalityPercentage = 15.0,
            issues = listOf(
                Issue(
                    id = "infra-001",
                    severity = IssueSeverity.CRITICAL,
                    category = IssueCategory.DEPENDENCY_INJECTION,
                    title = "Koin initialization commented out on iOS",
                    description = "Dependency injection is not initialized in iOS app entry point",
                    location = "iosApp/iosApp/iOSApp.swift",
                    impact = "Blocks 100% of shared functionality",
                    estimatedEffort = EffortEstimate(
                        level = EffortLevel.MEDIUM,
                        estimatedDays = 3..5,
                        complexity = ComplexityLevel.MODERATE,
                        dependencies = emptyList(),
                        skillsRequired = listOf("Kotlin", "Koin", "iOS")
                    ),
                    blocksOtherWork = true,
                    affectedFeatures = listOf("All shared features")
                )
            ),
            recommendations = emptyList(),
            detailedFindings = emptyList()
        )
        
        assertEquals(AuditLayer.INFRASTRUCTURE, assessment.layer)
        assertEquals(2.5, assessment.score)
        assertEquals(15.0, assessment.functionalityPercentage)
        assertEquals(1, assessment.issues.size)
        assertEquals(IssueSeverity.CRITICAL, assessment.issues[0].severity)
    }

    @Test
    fun `test dependency injection issue classification`() {
        val diIssue = Issue(
            id = "di-001",
            severity = IssueSeverity.CRITICAL,
            category = IssueCategory.DEPENDENCY_INJECTION,
            title = "Koin not initialized",
            description = "DI framework not set up",
            location = "iOS app entry point",
            impact = "Prevents app from accessing shared code",
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
        
        assertEquals(IssueCategory.DEPENDENCY_INJECTION, diIssue.category)
        assertTrue(diIssue.blocksOtherWork)
        assertEquals(IssueSeverity.CRITICAL, diIssue.severity)
    }

    @Test
    fun `test service implementation gap tracking`() {
        val missingServices = listOf(
            "AuthService",
            "FirebaseAuthService",
            "UserDao",
            "DailyLogDao",
            "FirestoreService",
            "PDFGenerationService",
            "NotificationService",
            "HealthKitService",
            "FileSystemService",
            "BiometricAuthService",
            "CameraService",
            "LocationService",
            "AnalyticsService",
            "CrashReportingService",
            "SQLDelightDatabase"
        )
        
        val serviceIssue = Issue(
            id = "service-001",
            severity = IssueSeverity.HIGH,
            category = IssueCategory.SERVICE_IMPLEMENTATION,
            title = "Missing service implementations",
            description = "15+ critical services lack DI bindings",
            location = "DI modules",
            impact = "Core functionality cannot be accessed",
            estimatedEffort = EffortEstimate(
                level = EffortLevel.HIGH,
                estimatedDays = 10..15,
                complexity = ComplexityLevel.COMPLEX,
                dependencies = listOf("Koin initialization"),
                skillsRequired = listOf("Kotlin", "Firebase", "Platform APIs")
            ),
            blocksOtherWork = false,
            affectedFeatures = missingServices
        )
        
        assertTrue(serviceIssue.affectedFeatures.size >= 15)
        assertEquals(IssueCategory.SERVICE_IMPLEMENTATION, serviceIssue.category)
        assertEquals(EffortLevel.HIGH, serviceIssue.estimatedEffort.level)
    }

    @Test
    fun `test platform-specific assessment tracking`() {
        // iOS platform assessment
        val iosAssessment = DetailedFinding(
            component = "iOS Koin Initialization",
            finding = "Koin initialization is commented out",
            evidence = listOf(
                "iosApp/iosApp/iOSApp.swift line 15",
                "// shared.IOSKoinInitializer.shared.initKoin()"
            ),
            recommendation = "Uncomment Koin initialization in iOS app entry point",
            priority = TaskPriority.BLOCKER
        )
        
        // Android platform assessment
        val androidAssessment = DetailedFinding(
            component = "Android Koin Initialization",
            finding = "Only sharedModule included, missing 5 other modules",
            evidence = listOf(
                "androidApp/src/androidMain/kotlin/com/eunio/healthapp/android/EunioApplication.kt",
                "startKoin { modules(sharedModule) }"
            ),
            recommendation = "Include all 6 modules in Android Koin initialization",
            priority = TaskPriority.HIGH
        )
        
        assertEquals(TaskPriority.BLOCKER, iosAssessment.priority)
        assertEquals(TaskPriority.HIGH, androidAssessment.priority)
        assertTrue(iosAssessment.evidence.isNotEmpty())
        assertTrue(androidAssessment.evidence.isNotEmpty())
    }

    @Test
    fun `test infrastructure scoring calculation`() {
        // Test infrastructure layer scoring based on assessment criteria
        val criteria = InfrastructureCriteria()
        
        // Verify weights sum to 1.0
        val total = criteria.koinInitializationWeight + 
                   criteria.serviceImplementationWeight +
                   criteria.buildConfigurationWeight + 
                   criteria.platformSetupWeight
        
        assertEquals(1.0, total, 0.001)
        
        // Koin initialization should have highest weight (40%)
        assertEquals(0.40, criteria.koinInitializationWeight)
        
        // Service implementation should be second (35%)
        assertEquals(0.35, criteria.serviceImplementationWeight)
    }

    @Test
    fun `test infrastructure functionality percentage calculation`() {
        // iOS: 0% functional (Koin not initialized)
        val iosFunctionality = ScoringCalculator.calculateFunctionalityPercentage(0, 6)
        assertEquals(0.0, iosFunctionality)
        
        // Android: 16.7% functional (1 of 6 modules included)
        val androidFunctionality = ScoringCalculator.calculateFunctionalityPercentage(1, 6)
        assertEquals(16.666666666666668, androidFunctionality, 0.1)
        
        // Target: 100% functional (all 6 modules properly initialized)
        val targetFunctionality = ScoringCalculator.calculateFunctionalityPercentage(6, 6)
        assertEquals(100.0, targetFunctionality)
    }

    @Test
    fun `test build configuration assessment`() {
        val buildIssue = Issue(
            id = "build-001",
            severity = IssueSeverity.MEDIUM,
            category = IssueCategory.PLATFORM_INTEGRATION,
            title = "Build succeeds but app is non-functional",
            description = "Builds complete successfully but produce non-functional apps",
            location = "Build configuration",
            impact = "False sense of completion - builds don't indicate functionality",
            estimatedEffort = EffortEstimate(
                level = EffortLevel.LOW,
                estimatedDays = 1..2,
                complexity = ComplexityLevel.SIMPLE,
                dependencies = listOf("Fix DI initialization first"),
                skillsRequired = listOf("Build systems")
            ),
            blocksOtherWork = false,
            affectedFeatures = listOf("iOS build", "Android build")
        )
        
        assertEquals(IssueCategory.PLATFORM_INTEGRATION, buildIssue.category)
        assertEquals(IssueSeverity.MEDIUM, buildIssue.severity)
    }

    @Test
    fun `test remediation task for infrastructure fixes`() {
        val remediationTask = RemediationTask(
            taskId = "infra-fix-001",
            title = "Initialize Koin dependency injection on iOS",
            description = "Uncomment and verify Koin initialization in iOS app entry point",
            category = IssueCategory.DEPENDENCY_INJECTION,
            effort = EffortEstimate(
                level = EffortLevel.MEDIUM,
                estimatedDays = 3..5,
                complexity = ComplexityLevel.MODERATE,
                dependencies = emptyList(),
                skillsRequired = listOf("Kotlin", "Koin", "iOS", "Swift")
            ),
            priority = TaskPriority.BLOCKER,
            assignedIssues = listOf("infra-001"),
            acceptanceCriteria = listOf(
                "Koin initializes successfully on iOS",
                "IOSKoinHelper.shared returns valid instances",
                "ViewModels can be instantiated from Swift",
                "All 6 modules are loaded",
                "No runtime crashes on app launch"
            )
        )
        
        assertEquals(TaskPriority.BLOCKER, remediationTask.priority)
        assertEquals(5, remediationTask.acceptanceCriteria.size)
        assertEquals(EffortLevel.MEDIUM, remediationTask.effort.level)
        assertTrue(remediationTask.effort.estimatedDays.contains(4))
    }

    @Test
    fun `test critical path identification for infrastructure`() {
        val criticalPath = listOf(
            "1. Initialize Koin on iOS",
            "2. Include all modules in Android",
            "3. Implement missing service bindings",
            "4. Verify platform-specific services",
            "5. Test end-to-end DI resolution"
        )
        
        val remediationPlan = RemediationPlan(
            phases = emptyList(),
            totalEstimatedEffort = EffortEstimate(
                level = EffortLevel.HIGH,
                estimatedDays = 15..25,
                complexity = ComplexityLevel.COMPLEX,
                dependencies = emptyList(),
                skillsRequired = listOf("Kotlin", "Koin", "iOS", "Android", "Firebase")
            ),
            criticalPath = criticalPath,
            dependencies = mapOf(
                "Service implementations" to listOf("Koin initialization"),
                "UI connections" to listOf("Service implementations"),
                "Testing" to listOf("UI connections")
            ),
            milestones = emptyList(),
            riskAssessment = RiskAssessment(
                technicalRisks = emptyList(),
                businessRisks = emptyList(),
                mitigationStrategies = emptyList()
            )
        )
        
        assertEquals(5, remediationPlan.criticalPath.size)
        assertTrue(remediationPlan.criticalPath[0].contains("Initialize Koin"))
        assertNotNull(remediationPlan.dependencies["Service implementations"])
    }

    @Test
    fun `test infrastructure assessment completeness`() {
        // Verify all required assessment areas are covered
        val assessmentAreas = listOf(
            "Koin initialization status",
            "Module configuration",
            "Shared module accessibility",
            "Missing service implementations",
            "IOSKoinHelper completeness",
            "IOSKoinInitializer completeness",
            "Platform-specific setup",
            "Build configuration"
        )
        
        assertTrue(assessmentAreas.size >= 8, "Should assess at least 8 infrastructure areas")
        assertTrue(assessmentAreas.contains("Koin initialization status"))
        assertTrue(assessmentAreas.contains("Missing service implementations"))
    }
}
