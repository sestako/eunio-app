package com.eunio.health.audit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

/**
 * Unit tests for AuditConfiguration and scoring algorithms
 * Verifies task 1 implementation: audit framework configuration and scoring
 */
class AuditConfigurationTest {

    @Test
    fun `test scoring weights sum to 1_0`() {
        val weights = ScoringWeights()
        val total = weights.infrastructure + weights.businessLogic + weights.dataLayer +
                weights.presentation + weights.userExperience + weights.accessibility +
                weights.platformIntegration + weights.quality
        
        assertEquals(1.0, total, 0.001, "Scoring weights must sum to 1.0")
    }

    @Test
    fun `test scoring weights validation fails when sum is not 1_0`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            ScoringWeights(
                infrastructure = 0.5,
                businessLogic = 0.3,  // Changed from 0.5 to 0.3 so sum is 0.8, not 1.0
                dataLayer = 0.0,
                presentation = 0.0,
                userExperience = 0.0,
                accessibility = 0.0,
                platformIntegration = 0.0,
                quality = 0.0
            )
        }
        assertTrue(exception.message?.contains("Scoring weights must sum to 1.0") == true)
    }

    @Test
    fun `test default audit thresholds are correctly set`() {
        val thresholds = AuditThresholds()
        
        assertEquals(20.0, thresholds.nonFunctionalThreshold)
        assertEquals(80.0, thresholds.partiallyImplementedThreshold)
        assertEquals(70.0, thresholds.minimumUnitTestCoverage)
        assertEquals(90.0, thresholds.minimumIntegrationTestCoverage)
        assertEquals(2, thresholds.minimumE2ETestsPerFeature)
    }

    @Test
    fun `test accessibility thresholds match WCAG standards`() {
        val thresholds = AuditThresholds()
        
        assertEquals(4.5, thresholds.contrastRatioNormal, "Normal text contrast ratio should be 4.5:1")
        assertEquals(3.0, thresholds.contrastRatioLarge, "Large text contrast ratio should be 3:1")
        assertEquals(95.0, thresholds.wcagComplianceThreshold)
    }

    @Test
    fun `test infrastructure criteria weights sum to 1_0`() {
        val criteria = InfrastructureCriteria()
        val total = criteria.koinInitializationWeight + criteria.serviceImplementationWeight +
                criteria.buildConfigurationWeight + criteria.platformSetupWeight
        
        assertEquals(1.0, total, 0.001, "Infrastructure criteria weights must sum to 1.0")
    }

    @Test
    fun `test business logic criteria weights sum to 1_0`() {
        val criteria = BusinessLogicCriteria()
        val total = criteria.useCaseImplementationWeight + criteria.repositoryImplementationWeight +
                criteria.viewModelConnectivityWeight + criteria.domainModelValidationWeight
        
        assertEquals(1.0, total, 0.001, "Business logic criteria weights must sum to 1.0")
    }

    @Test
    fun `test calculateOverallScore with balanced scores`() {
        val layerScores = mapOf(
            AuditLayer.INFRASTRUCTURE to 5.0,
            AuditLayer.BUSINESS_LOGIC to 5.0,
            AuditLayer.DATA_LAYER to 5.0,
            AuditLayer.PRESENTATION to 5.0,
            AuditLayer.USER_EXPERIENCE to 5.0,
            AuditLayer.ACCESSIBILITY to 5.0,
            AuditLayer.PLATFORM_INTEGRATION to 5.0,
            AuditLayer.QUALITY_STANDARDS to 5.0
        )
        
        val overallScore = ScoringCalculator.calculateOverallScore(layerScores, ScoringWeights())
        assertEquals(5.0, overallScore, 0.001, "Overall score should be 5.0 when all layers are 5.0")
    }

    @Test
    fun `test calculateOverallScore with weighted scores`() {
        val layerScores = mapOf(
            AuditLayer.INFRASTRUCTURE to 2.0,  // 25% weight
            AuditLayer.BUSINESS_LOGIC to 8.0,  // 20% weight
            AuditLayer.DATA_LAYER to 5.0,      // 15% weight
            AuditLayer.PRESENTATION to 5.0,    // 15% weight
            AuditLayer.USER_EXPERIENCE to 5.0, // 10% weight
            AuditLayer.ACCESSIBILITY to 5.0,   // 5% weight
            AuditLayer.PLATFORM_INTEGRATION to 5.0, // 5% weight
            AuditLayer.QUALITY_STANDARDS to 5.0     // 5% weight
        )
        
        val overallScore = ScoringCalculator.calculateOverallScore(layerScores, ScoringWeights())
        
        // Expected: 2.0*0.25 + 8.0*0.20 + 5.0*0.15 + 5.0*0.15 + 5.0*0.10 + 5.0*0.05 + 5.0*0.05 + 5.0*0.05
        // = 0.5 + 1.6 + 0.75 + 0.75 + 0.5 + 0.25 + 0.25 + 0.25 = 4.85
        assertEquals(4.85, overallScore, 0.001)
    }

    @Test
    fun `test calculateFunctionalityPercentage with full implementation`() {
        val percentage = ScoringCalculator.calculateFunctionalityPercentage(10, 10)
        assertEquals(100.0, percentage, 0.001)
    }

    @Test
    fun `test calculateFunctionalityPercentage with partial implementation`() {
        val percentage = ScoringCalculator.calculateFunctionalityPercentage(5, 10)
        assertEquals(50.0, percentage, 0.001)
    }

    @Test
    fun `test calculateFunctionalityPercentage with no implementation`() {
        val percentage = ScoringCalculator.calculateFunctionalityPercentage(0, 10)
        assertEquals(0.0, percentage, 0.001)
    }

    @Test
    fun `test calculateFunctionalityPercentage with zero total components`() {
        val percentage = ScoringCalculator.calculateFunctionalityPercentage(0, 0)
        assertEquals(0.0, percentage, 0.001)
    }

    @Test
    fun `test determineFeatureStatus for NOT_IMPLEMENTED`() {
        val status = ScoringCalculator.determineFeatureStatus(0.0)
        assertEquals(FeatureStatus.NOT_IMPLEMENTED, status)
    }

    @Test
    fun `test determineFeatureStatus for NON_FUNCTIONAL`() {
        val status = ScoringCalculator.determineFeatureStatus(15.0)
        assertEquals(FeatureStatus.NON_FUNCTIONAL, status)
    }

    @Test
    fun `test determineFeatureStatus for PARTIALLY_IMPLEMENTED`() {
        val status = ScoringCalculator.determineFeatureStatus(50.0)
        assertEquals(FeatureStatus.PARTIALLY_IMPLEMENTED, status)
    }

    @Test
    fun `test determineFeatureStatus for COMPLETE`() {
        val status = ScoringCalculator.determineFeatureStatus(85.0)
        assertEquals(FeatureStatus.COMPLETE, status)
    }

    @Test
    fun `test determineFeatureStatus boundary at 20 percent`() {
        assertEquals(FeatureStatus.NON_FUNCTIONAL, ScoringCalculator.determineFeatureStatus(19.9))
        assertEquals(FeatureStatus.PARTIALLY_IMPLEMENTED, ScoringCalculator.determineFeatureStatus(20.0))
    }

    @Test
    fun `test determineFeatureStatus boundary at 80 percent`() {
        assertEquals(FeatureStatus.PARTIALLY_IMPLEMENTED, ScoringCalculator.determineFeatureStatus(79.9))
        assertEquals(FeatureStatus.COMPLETE, ScoringCalculator.determineFeatureStatus(80.0))
    }

    @Test
    fun `test calculateEffortLevel for LOW effort`() {
        assertEquals(EffortLevel.LOW, ScoringCalculator.calculateEffortLevel(1))
        assertEquals(EffortLevel.LOW, ScoringCalculator.calculateEffortLevel(3))
    }

    @Test
    fun `test calculateEffortLevel for MEDIUM effort`() {
        assertEquals(EffortLevel.MEDIUM, ScoringCalculator.calculateEffortLevel(4))
        assertEquals(EffortLevel.MEDIUM, ScoringCalculator.calculateEffortLevel(8))
    }

    @Test
    fun `test calculateEffortLevel for HIGH effort`() {
        assertEquals(EffortLevel.HIGH, ScoringCalculator.calculateEffortLevel(9))
        assertEquals(EffortLevel.HIGH, ScoringCalculator.calculateEffortLevel(15))
    }

    @Test
    fun `test determineSeverity for CRITICAL issues`() {
        val severity = ScoringCalculator.determineSeverity(
            functionalityImpact = 9.0,
            userImpact = 9.0,
            businessImpact = 9.0
        )
        assertEquals(IssueSeverity.CRITICAL, severity)
    }

    @Test
    fun `test determineSeverity for HIGH issues`() {
        val severity = ScoringCalculator.determineSeverity(
            functionalityImpact = 7.0,
            userImpact = 6.0,
            businessImpact = 7.0
        )
        assertEquals(IssueSeverity.HIGH, severity)
    }

    @Test
    fun `test determineSeverity for MEDIUM issues`() {
        val severity = ScoringCalculator.determineSeverity(
            functionalityImpact = 5.0,
            userImpact = 4.0,
            businessImpact = 5.0
        )
        assertEquals(IssueSeverity.MEDIUM, severity)
    }

    @Test
    fun `test determineSeverity for LOW issues`() {
        val severity = ScoringCalculator.determineSeverity(
            functionalityImpact = 2.0,
            userImpact = 2.0,
            businessImpact = 2.0
        )
        assertEquals(IssueSeverity.LOW, severity)
    }

    @Test
    fun `test default audit configuration is valid`() {
        val config = DEFAULT_AUDIT_CONFIG
        
        // Verify all components are initialized
        assertTrue(config.scoringWeights.infrastructure > 0)
        assertTrue(config.thresholds.nonFunctionalThreshold > 0)
        assertTrue(config.assessmentCriteria.infrastructureCriteria.koinInitializationWeight > 0)
    }

    @Test
    fun `test all audit layers are defined`() {
        val layers = AuditLayer.values()
        
        assertEquals(8, layers.size, "Should have 8 audit layers")
        assertTrue(layers.contains(AuditLayer.INFRASTRUCTURE))
        assertTrue(layers.contains(AuditLayer.BUSINESS_LOGIC))
        assertTrue(layers.contains(AuditLayer.DATA_LAYER))
        assertTrue(layers.contains(AuditLayer.PRESENTATION))
        assertTrue(layers.contains(AuditLayer.USER_EXPERIENCE))
        assertTrue(layers.contains(AuditLayer.PLATFORM_INTEGRATION))
        assertTrue(layers.contains(AuditLayer.QUALITY_STANDARDS))
        assertTrue(layers.contains(AuditLayer.ACCESSIBILITY))
    }
}
