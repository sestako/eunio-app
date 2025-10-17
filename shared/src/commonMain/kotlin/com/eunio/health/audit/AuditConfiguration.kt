package com.eunio.health.audit

/**
 * Audit Configuration and Scoring Algorithms
 * Defines thresholds, weights, and calculation methods for the audit
 */

data class AuditConfiguration(
    val scoringWeights: ScoringWeights,
    val thresholds: AuditThresholds,
    val assessmentCriteria: AssessmentCriteria
)

data class ScoringWeights(
    val infrastructure: Double = 0.25,      // 25% - Critical foundation
    val businessLogic: Double = 0.20,       // 20% - Core functionality
    val dataLayer: Double = 0.15,           // 15% - Data persistence and sync
    val presentation: Double = 0.15,        // 15% - User interface
    val userExperience: Double = 0.10,      // 10% - End-to-end workflows
    val accessibility: Double = 0.05,       // 5% - Accessibility compliance
    val platformIntegration: Double = 0.05, // 5% - Platform-specific features
    val quality: Double = 0.05              // 5% - Code quality and testing
) {
    init {
        val total = infrastructure + businessLogic + dataLayer + presentation + 
                   userExperience + accessibility + platformIntegration + quality
        require(kotlin.math.abs(total - 1.0) < 0.001) { 
            "Scoring weights must sum to 1.0, got $total" 
        }
    }
}

data class AuditThresholds(
    // Functionality percentage thresholds
    val nonFunctionalThreshold: Double = 20.0,      // <20% = Non-Functional
    val partiallyImplementedThreshold: Double = 80.0, // 20-79% = Partially Implemented
    // 80%+ = Complete
    
    // Score thresholds (0-10 scale)
    val criticalScoreThreshold: Double = 3.0,       // <3.0 = Critical issues
    val poorScoreThreshold: Double = 5.0,           // 3.0-5.0 = Poor
    val fairScoreThreshold: Double = 7.0,           // 5.0-7.0 = Fair
    val goodScoreThreshold: Double = 8.5,           // 7.0-8.5 = Good
    // 8.5+ = Excellent
    
    // Test coverage thresholds
    val minimumUnitTestCoverage: Double = 70.0,     // 70% minimum for business logic
    val minimumIntegrationTestCoverage: Double = 90.0, // 90% for critical flows
    val minimumE2ETestsPerFeature: Int = 2,         // 2 end-to-end tests per major feature
    
    // Accessibility thresholds
    val wcagComplianceThreshold: Double = 95.0,     // 95% WCAG AA compliance
    val contrastRatioNormal: Double = 4.5,          // 4.5:1 for normal text
    val contrastRatioLarge: Double = 3.0,           // 3:1 for large text
    
    // Performance thresholds
    val maxAuditDurationMinutes: Int = 30,          // 30 minutes max audit time
    val maxMemoryUsageGB: Double = 2.0,             // 2GB max memory usage
    val maxCpuUsagePercent: Double = 50.0           // 50% max CPU usage
)

data class AssessmentCriteria(
    val infrastructureCriteria: InfrastructureCriteria,
    val businessLogicCriteria: BusinessLogicCriteria,
    val dataLayerCriteria: DataLayerCriteria,
    val presentationCriteria: PresentationCriteria,
    val userExperienceCriteria: UserExperienceCriteria,
    val accessibilityCriteria: AccessibilityCriteria,
    val qualityCriteria: QualityCriteria
)

data class InfrastructureCriteria(
    val koinInitializationWeight: Double = 0.40,    // 40% - Critical blocker
    val serviceImplementationWeight: Double = 0.35, // 35% - Core services
    val buildConfigurationWeight: Double = 0.15,    // 15% - Build system
    val platformSetupWeight: Double = 0.10          // 10% - Platform-specific setup
)

data class BusinessLogicCriteria(
    val useCaseWeight: Double = 0.35,               // 35% - Use case functionality
    val repositoryWeight: Double = 0.30,            // 30% - Data access layer
    val viewModelWeight: Double = 0.25,             // 25% - UI-business logic bridge
    val dataFlowWeight: Double = 0.10,              // 10% - Data flow and state management
    
    // Legacy property names for backward compatibility
    val useCaseImplementationWeight: Double = useCaseWeight,
    val repositoryImplementationWeight: Double = repositoryWeight,
    val viewModelConnectivityWeight: Double = viewModelWeight,
    val domainModelValidationWeight: Double = dataFlowWeight
)

data class DataLayerCriteria(
    val localDatabaseWeight: Double = 0.30,         // 30% - Local persistence
    val remoteServiceWeight: Double = 0.30,         // 30% - Remote connectivity
    val dataSynchronizationWeight: Double = 0.25,   // 25% - Sync mechanisms
    val offlineFunctionalityWeight: Double = 0.15   // 15% - Offline capabilities
)

data class PresentationCriteria(
    val uiComponentFunctionalityWeight: Double = 0.40, // 40% - UI interactivity
    val navigationFlowWeight: Double = 0.25,         // 25% - Navigation completeness
    val stateManagementWeight: Double = 0.20,        // 20% - State synchronization
    val errorHandlingWeight: Double = 0.15           // 15% - Error display and handling
)

data class UserExperienceCriteria(
    val endToEndWorkflowWeight: Double = 0.40,       // 40% - Complete user journeys
    val featureCompletenessWeight: Double = 0.30,    // 30% - Feature implementation
    val dataPersistenceWeight: Double = 0.20,        // 20% - User data handling
    val userFeedbackWeight: Double = 0.10            // 10% - User feedback mechanisms
)

data class AccessibilityCriteria(
    val screenReaderSupportWeight: Double = 0.30,    // 30% - VoiceOver/TalkBack
    val dynamicTypeWeight: Double = 0.25,            // 25% - Text scaling
    val contrastComplianceWeight: Double = 0.25,     // 25% - Color contrast
    val keyboardNavigationWeight: Double = 0.20      // 20% - Keyboard accessibility
)

data class QualityCriteria(
    val codeArchitectureWeight: Double = 0.30,       // 30% - Architecture quality
    val testCoverageWeight: Double = 0.25,           // 25% - Test coverage
    val errorHandlingWeight: Double = 0.20,          // 20% - Error handling
    val documentationWeight: Double = 0.15,          // 15% - Documentation quality
    val performanceWeight: Double = 0.10             // 10% - Performance considerations
)

// Scoring calculation utilities
object ScoringCalculator {
    
    fun calculateOverallScore(
        layerScores: Map<AuditLayer, Double>,
        weights: ScoringWeights
    ): Double {
        return layerScores[AuditLayer.INFRASTRUCTURE]!! * weights.infrastructure +
               layerScores[AuditLayer.BUSINESS_LOGIC]!! * weights.businessLogic +
               layerScores[AuditLayer.DATA_LAYER]!! * weights.dataLayer +
               layerScores[AuditLayer.PRESENTATION]!! * weights.presentation +
               layerScores[AuditLayer.USER_EXPERIENCE]!! * weights.userExperience +
               layerScores[AuditLayer.ACCESSIBILITY]!! * weights.accessibility +
               layerScores[AuditLayer.PLATFORM_INTEGRATION]!! * weights.platformIntegration +
               layerScores[AuditLayer.QUALITY_STANDARDS]!! * weights.quality
    }
    
    fun calculateFunctionalityPercentage(
        implementedComponents: Int,
        totalComponents: Int
    ): Double {
        return if (totalComponents > 0) {
            (implementedComponents.toDouble() / totalComponents.toDouble()) * 100.0
        } else {
            0.0
        }
    }
    
    fun determineFeatureStatus(functionalityPercentage: Double): FeatureStatus {
        return when {
            functionalityPercentage == 0.0 -> FeatureStatus.NOT_IMPLEMENTED
            functionalityPercentage < 20.0 -> FeatureStatus.NON_FUNCTIONAL
            functionalityPercentage < 80.0 -> FeatureStatus.PARTIALLY_IMPLEMENTED
            else -> FeatureStatus.COMPLETE
        }
    }
    
    fun calculateEffortLevel(estimatedDays: Int): EffortLevel {
        return when {
            estimatedDays <= 3 -> EffortLevel.LOW
            estimatedDays <= 8 -> EffortLevel.MEDIUM
            else -> EffortLevel.HIGH
        }
    }
    
    fun determineSeverity(
        functionalityImpact: Double,
        userImpact: Double,
        businessImpact: Double
    ): IssueSeverity {
        val combinedImpact = (functionalityImpact + userImpact + businessImpact) / 3.0
        return when {
            combinedImpact >= 8.0 -> IssueSeverity.CRITICAL
            combinedImpact >= 6.0 -> IssueSeverity.HIGH
            combinedImpact >= 4.0 -> IssueSeverity.MEDIUM
            else -> IssueSeverity.LOW
        }
    }
}

// Default configuration for Eunio Health App audit
val DEFAULT_AUDIT_CONFIG = AuditConfiguration(
    scoringWeights = ScoringWeights(),
    thresholds = AuditThresholds(),
    assessmentCriteria = AssessmentCriteria(
        infrastructureCriteria = InfrastructureCriteria(),
        businessLogicCriteria = BusinessLogicCriteria(),
        dataLayerCriteria = DataLayerCriteria(),
        presentationCriteria = PresentationCriteria(),
        userExperienceCriteria = UserExperienceCriteria(),
        accessibilityCriteria = AccessibilityCriteria(),
        qualityCriteria = QualityCriteria()
    )
)
