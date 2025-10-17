package com.eunio.health.audit

/**
 * Comprehensive Functionality Audit Framework
 * Data Models and Structures for Eunio Health App Assessment
 */

import kotlinx.datetime.Instant

// Core Audit Result Structure
data class AuditResult(
    val auditId: String,
    val timestamp: Instant,
    val overallScore: AuditScore,
    val layerAssessments: List<LayerAssessment>,
    val criticalIssues: List<CriticalIssue>,
    val remediationPlan: RemediationPlan,
    val executiveSummary: ExecutiveSummary
)

data class AuditScore(
    val overall: Double, // 0.0 to 10.0
    val infrastructure: Double,
    val businessLogic: Double,
    val dataLayer: Double,
    val presentation: Double,
    val userExperience: Double,
    val quality: Double,
    val accessibility: Double,
    val platformIntegration: Double
)

data class LayerAssessment(
    val layer: AuditLayer,
    val score: Double,
    val functionalityPercentage: Double,
    val issues: List<Issue>,
    val recommendations: List<Recommendation>,
    val detailedFindings: List<DetailedFinding>
)

enum class AuditLayer {
    INFRASTRUCTURE,
    BUSINESS_LOGIC,
    DATA_LAYER,
    PRESENTATION,
    USER_EXPERIENCE,
    PLATFORM_INTEGRATION,
    QUALITY_STANDARDS,
    ACCESSIBILITY
}

// Issue Classification and Tracking
data class Issue(
    val id: String,
    val severity: IssueSeverity,
    val category: IssueCategory,
    val title: String,
    val description: String,
    val location: String,
    val impact: String,
    val estimatedEffort: EffortEstimate,
    val blocksOtherWork: Boolean,
    val affectedFeatures: List<String>
)

enum class IssueSeverity {
    CRITICAL,    // Blocks core functionality - prevents app from working
    HIGH,        // Significantly impacts user experience - major features broken
    MEDIUM,      // Affects specific features - some functionality missing
    LOW          // Minor improvements - polish and optimization
}

enum class IssueCategory {
    DEPENDENCY_INJECTION,
    SERVICE_IMPLEMENTATION,
    DATA_PERSISTENCE,
    UI_FUNCTIONALITY,
    BUSINESS_LOGIC,
    TESTING,
    ACCESSIBILITY,
    PERFORMANCE,
    SECURITY,
    AUTHENTICATION,
    NAVIGATION,
    PLATFORM_INTEGRATION
}

data class EffortEstimate(
    val level: EffortLevel,
    val estimatedDays: IntRange,
    val complexity: ComplexityLevel,
    val dependencies: List<String>,
    val skillsRequired: List<String>
)

enum class EffortLevel {
    LOW,      // 1-3 days
    MEDIUM,   // 4-8 days  
    HIGH      // 9+ days
}

enum class ComplexityLevel {
    SIMPLE,      // Straightforward implementation, well-defined patterns
    MODERATE,    // Some complexity, standard patterns with customization
    COMPLEX      // High complexity, custom solutions, architectural changes
}

// Feature Assessment Models
data class FeatureAssessment(
    val featureName: String,
    val currentStatus: FeatureStatus,
    val functionalityPercentage: Double,
    val missingComponents: List<String>,
    val implementedComponents: List<String>,
    val userImpact: UserImpact,
    val businessValue: BusinessValue,
    val technicalDebt: TechnicalDebt
)

enum class FeatureStatus {
    NOT_IMPLEMENTED,      // 0% - No implementation
    NON_FUNCTIONAL,       // 1-19% - Structure only, no functionality
    PARTIALLY_IMPLEMENTED, // 20-79% - Some working features
    COMPLETE              // 80-100% - Fully functional
}

data class UserImpact(
    val affectedUserJourneys: List<String>,
    val workaroundAvailable: Boolean,
    val userExperienceRating: Double, // 1.0 to 5.0
    val accessibilityImpact: AccessibilityImpact
)

data class BusinessValue(
    val priority: BusinessPriority,
    val revenueImpact: RevenueImpact,
    val userRetentionImpact: RetentionImpact,
    val competitiveAdvantage: CompetitiveAdvantage
)

enum class BusinessPriority {
    CRITICAL,    // Core app functionality - app unusable without this
    HIGH,        // Important features - significantly impacts user value
    MEDIUM,      // Nice-to-have features - enhances user experience
    LOW          // Future enhancements - minimal current impact
}

enum class RevenueImpact {
    HIGH,        // Directly affects monetization or user acquisition
    MEDIUM,      // Indirectly affects business metrics
    LOW          // Minimal business impact
}

enum class RetentionImpact {
    HIGH,        // Critical for user retention
    MEDIUM,      // Affects user satisfaction
    LOW          // Minimal retention impact
}

enum class CompetitiveAdvantage {
    DIFFERENTIATOR,  // Unique feature that sets app apart
    PARITY,          // Standard feature expected by users
    NICE_TO_HAVE     // Additional value but not essential
}

// Technical Debt and Quality Assessment
data class TechnicalDebt(
    val debtLevel: DebtLevel,
    val maintainabilityScore: Double, // 1.0 to 10.0
    val testCoverage: Double,         // 0.0 to 100.0
    val codeQualityIssues: List<CodeQualityIssue>
)

enum class DebtLevel {
    LOW,     // Well-structured, maintainable code
    MEDIUM,  // Some technical debt, manageable
    HIGH     // Significant technical debt, refactoring needed
}

data class CodeQualityIssue(
    val type: CodeQualityType,
    val description: String,
    val severity: IssueSeverity,
    val location: String
)

enum class CodeQualityType {
    ARCHITECTURE_VIOLATION,
    MISSING_ERROR_HANDLING,
    POOR_SEPARATION_OF_CONCERNS,
    MISSING_TESTS,
    PERFORMANCE_ISSUE,
    SECURITY_VULNERABILITY,
    ACCESSIBILITY_VIOLATION
}

// Accessibility Assessment Models
data class AccessibilityImpact(
    val voiceOverSupport: AccessibilitySupport,
    val dynamicTypeSupport: AccessibilitySupport,
    val contrastCompliance: ContrastCompliance,
    val keyboardNavigation: AccessibilitySupport
)

enum class AccessibilitySupport {
    FULL,        // Complete implementation
    PARTIAL,     // Some support, gaps exist
    MINIMAL,     // Basic support only
    NONE         // No accessibility support
}

data class ContrastCompliance(
    val wcagLevel: WCAGLevel,
    val compliancePercentage: Double,
    val failingElements: List<String>
)

enum class WCAGLevel {
    AAA,  // Highest accessibility standard
    AA,   // Standard accessibility compliance
    A,    // Basic accessibility
    NONE  // No compliance
}

// Remediation Planning Models
data class RemediationPlan(
    val phases: List<RemediationPhase>,
    val totalEstimatedEffort: EffortEstimate,
    val criticalPath: List<String>,
    val dependencies: Map<String, List<String>>,
    val milestones: List<Milestone>,
    val riskAssessment: RiskAssessment
)

data class RemediationPhase(
    val phaseNumber: Int,
    val name: String,
    val description: String,
    val tasks: List<RemediationTask>,
    val estimatedDuration: IntRange,
    val prerequisites: List<String>,
    val deliverables: List<String>,
    val successCriteria: List<String>
)

data class RemediationTask(
    val taskId: String,
    val title: String,
    val description: String,
    val category: IssueCategory,
    val effort: EffortEstimate,
    val priority: TaskPriority,
    val assignedIssues: List<String>,
    val acceptanceCriteria: List<String>
)

enum class TaskPriority {
    BLOCKER,     // Must be completed before other work can proceed
    CRITICAL,    // High priority, affects core functionality
    HIGH,        // Important for user experience
    MEDIUM,      // Standard priority
    LOW          // Can be deferred to later phases
}

data class Milestone(
    val name: String,
    val description: String,
    val targetDate: String,
    val successCriteria: List<String>,
    val deliverables: List<String>,
    val dependentTasks: List<String>
)

data class RiskAssessment(
    val technicalRisks: List<Risk>,
    val businessRisks: List<Risk>,
    val mitigationStrategies: List<MitigationStrategy>
)

data class Risk(
    val id: String,
    val description: String,
    val probability: RiskProbability,
    val impact: RiskImpact,
    val category: RiskCategory
)

enum class RiskProbability {
    LOW,     // Unlikely to occur
    MEDIUM,  // Possible
    HIGH     // Likely to occur
}

enum class RiskImpact {
    LOW,     // Minimal impact on timeline/quality
    MEDIUM,  // Moderate impact
    HIGH     // Significant impact on project success
}

enum class RiskCategory {
    TECHNICAL,
    RESOURCE,
    TIMELINE,
    BUSINESS,
    EXTERNAL
}

data class MitigationStrategy(
    val riskId: String,
    val strategy: String,
    val owner: String,
    val timeline: String
)

// Executive Summary and Reporting
data class ExecutiveSummary(
    val overallAssessment: String,
    val keyFindings: List<String>,
    val businessImpact: String,
    val recommendedActions: List<String>,
    val timeToMarket: String,
    val investmentRequired: String
)

data class DetailedFinding(
    val component: String,
    val finding: String,
    val evidence: List<String>,
    val recommendation: String,
    val priority: TaskPriority
)

data class Recommendation(
    val title: String,
    val description: String,
    val rationale: String,
    val effort: EffortEstimate,
    val expectedBenefit: String
)

data class CriticalIssue(
    val issue: Issue,
    val blockedFeatures: List<String>,
    val userImpact: String,
    val businessImpact: String,
    val recommendedAction: String
)
