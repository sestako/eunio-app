package com.eunio.healthapp.testutil

import com.eunio.healthapp.domain.model.*
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Instant
import kotlinx.datetime.plus
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/**
 * Efficient test data factory that creates optimized test data with minimal memory footprint
 * and supports data templates for consistent test scenarios.
 * 
 * Addresses Requirement 7.4:
 * - Implement efficient test data creation and cleanup
 * - Ensure test suite completes within reasonable time limits
 */
object EfficientTestDataFactory {
    
    /**
     * Template configurations for different test scenarios
     */
    enum class DataTemplate {
        MINIMAL,        // Bare minimum data for basic tests
        STANDARD,       // Standard test data with common fields
        COMPREHENSIVE,  // Full data for integration tests
        PERFORMANCE     // Optimized data for performance tests
    }
    
    /**
     * Configuration for test data generation
     */
    data class DataGenerationConfig(
        val template: DataTemplate = DataTemplate.STANDARD,
        val enableCaching: Boolean = true,
        val reuseImmutableData: Boolean = true,
        val generateTimestamps: Boolean = true,
        val includeOptionalFields: Boolean = true
    )
    
    // Pre-computed data for performance optimization
    private val baseTimestamp = Clock.System.now()
    private val baseDate = LocalDate(2024, 1, 15)
    
    // Cached immutable data objects
    private val cachedHealthGoals = listOf("TRACK_CYCLE", "FERTILITY", "GENERAL_HEALTH")
    private val cachedMoodLevels = (1..5).toList()
    private val cachedEnergyLevels = (1..5).toList()
    
    // Template-based data pools
    private val userTemplates = mutableMapOf<DataTemplate, User>()
    private val dailyLogTemplates = mutableMapOf<DataTemplate, DailyLog>()
    private val cycleTemplates = mutableMapOf<DataTemplate, Cycle>()
    
    init {
        initializeTemplates()
    }
    
    /**
     * Initialize data templates for efficient reuse
     */
    private fun initializeTemplates() {
        // User templates
        userTemplates[DataTemplate.MINIMAL] = createMinimalUser()
        userTemplates[DataTemplate.STANDARD] = createStandardUser()
        userTemplates[DataTemplate.COMPREHENSIVE] = createComprehensiveUser()
        userTemplates[DataTemplate.PERFORMANCE] = createPerformanceUser()
        
        // Daily log templates
        dailyLogTemplates[DataTemplate.MINIMAL] = createMinimalDailyLog()
        dailyLogTemplates[DataTemplate.STANDARD] = createStandardDailyLog()
        dailyLogTemplates[DataTemplate.COMPREHENSIVE] = createComprehensiveDailyLog()
        dailyLogTemplates[DataTemplate.PERFORMANCE] = createPerformanceDailyLog()
        
        // Cycle templates
        cycleTemplates[DataTemplate.MINIMAL] = createMinimalCycle()
        cycleTemplates[DataTemplate.STANDARD] = createStandardCycle()
        cycleTemplates[DataTemplate.COMPREHENSIVE] = createComprehensiveCycle()
        cycleTemplates[DataTemplate.PERFORMANCE] = createPerformanceCycle()
    }
    
    /**
     * Create optimized user with specified template
     */
    fun createOptimizedUser(
        userId: String = "test-user-${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}",
        config: DataGenerationConfig = DataGenerationConfig()
    ): User {
        val template = if (config.reuseImmutableData) {
            userTemplates[config.template]!!
        } else {
            createUserByTemplate(config.template)
        }
        
        return template.copy(
            id = userId,
            email = if (config.template == DataTemplate.PERFORMANCE) "test@example.com" else "$userId@test.com"
        )
    }
    
    /**
     * Create optimized daily log with specified template
     */
    fun createOptimizedDailyLog(
        userId: String = "test-user-${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}",
        date: LocalDate = baseDate,
        config: DataGenerationConfig = DataGenerationConfig()
    ): DailyLog {
        val template = if (config.reuseImmutableData) {
            dailyLogTemplates[config.template]!!
        } else {
            createDailyLogByTemplate(config.template)
        }
        
        return template.copy(
            userId = userId,
            date = date
        )
    }
    
    /**
     * Create optimized cycle with specified template
     */
    fun createOptimizedCycle(
        userId: String = "test-user-${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}",
        config: DataGenerationConfig = DataGenerationConfig()
    ): Cycle {
        val template = if (config.reuseImmutableData) {
            cycleTemplates[config.template]!!
        } else {
            createCycleByTemplate(config.template)
        }
        
        return template.copy(
            id = "cycle-${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}",
            userId = userId,
            startDate = baseDate
        )
    }
    
    /**
     * Create batch of users efficiently
     */
    fun createUserBatch(
        count: Int,
        baseUserId: String = "batch-user",
        config: DataGenerationConfig = DataGenerationConfig()
    ): List<User> {
        val template = userTemplates[config.template]!!
        
        return (1..count).map { index ->
            template.copy(
                id = "$baseUserId-$index",
                email = if (config.template == DataTemplate.PERFORMANCE) {
                    "test$index@example.com"
                } else {
                    "$baseUserId-$index@test.com"
                },
                name = if (config.template == DataTemplate.PERFORMANCE) {
                    "User $index"
                } else {
                    "Test User $index"
                },

            )
        }
    }
    
    /**
     * Create batch of daily logs efficiently
     */
    fun createDailyLogBatch(
        userId: String,
        count: Int,
        startDate: LocalDate = baseDate,
        config: DataGenerationConfig = DataGenerationConfig()
    ): List<DailyLog> {
        val template = dailyLogTemplates[config.template]!!
        
        return (0 until count).map { index ->
            template.copy(
                userId = userId,
                date = startDate.plus(kotlinx.datetime.DatePeriod(days = index))
            )
        }
    }
    
    /**
     * Create batch of cycles efficiently
     */
    fun createCycleBatch(
        userId: String,
        count: Int,
        config: DataGenerationConfig = DataGenerationConfig()
    ): List<Cycle> {
        val template = cycleTemplates[config.template]!!
        
        return (1..count).map { index ->
            template.copy(
                id = "cycle-$userId-$index",
                userId = userId,
                startDate = baseDate.plus(kotlinx.datetime.DatePeriod(days = index * 28))
            )
        }
    }
    
    // Template creation methods
    
    private fun createMinimalUser(): User {
        return TestDataFactory.createTestUser("template-user")
    }
    
    private fun createStandardUser(): User {
        return TestDataFactory.createTestUser("template-user")
    }
    
    private fun createComprehensiveUser(): User {
        return TestDataFactory.createTestUser("template-user")
    }
    
    private fun createPerformanceUser(): User {
        return TestDataFactory.createTestUser("perf-user")
    }
    
    private fun createMinimalDailyLog(): DailyLog {
        return TestDataFactory.createDailyLog("template-user")
    }
    
    private fun createStandardDailyLog(): DailyLog {
        return TestDataFactory.createDailyLog("template-user")
    }
    
    private fun createComprehensiveDailyLog(): DailyLog {
        return TestDataFactory.createDailyLog("template-user")
    }
    
    private fun createPerformanceDailyLog(): DailyLog {
        return TestDataFactory.createDailyLog("perf-user")
    }
    
    private fun createMinimalCycle(): Cycle {
        return TestDataFactory.createTestCycle("template-user")
    }
    
    private fun createStandardCycle(): Cycle {
        return TestDataFactory.createTestCycle("template-user")
    }
    
    private fun createComprehensiveCycle(): Cycle {
        return TestDataFactory.createTestCycle("template-user")
    }
    
    private fun createPerformanceCycle(): Cycle {
        return TestDataFactory.createTestCycle("perf-user")
    }
    
    private fun createUserByTemplate(template: DataTemplate): User {
        return when (template) {
            DataTemplate.MINIMAL -> createMinimalUser()
            DataTemplate.STANDARD -> createStandardUser()
            DataTemplate.COMPREHENSIVE -> createComprehensiveUser()
            DataTemplate.PERFORMANCE -> createPerformanceUser()
        }
    }
    
    private fun createDailyLogByTemplate(template: DataTemplate): DailyLog {
        return when (template) {
            DataTemplate.MINIMAL -> createMinimalDailyLog()
            DataTemplate.STANDARD -> createStandardDailyLog()
            DataTemplate.COMPREHENSIVE -> createComprehensiveDailyLog()
            DataTemplate.PERFORMANCE -> createPerformanceDailyLog()
        }
    }
    
    private fun createCycleByTemplate(template: DataTemplate): Cycle {
        return when (template) {
            DataTemplate.MINIMAL -> createMinimalCycle()
            DataTemplate.STANDARD -> createStandardCycle()
            DataTemplate.COMPREHENSIVE -> createComprehensiveCycle()
            DataTemplate.PERFORMANCE -> createPerformanceCycle()
        }
    }
    
    /**
     * Create test data for specific scenarios
     */
    object Scenarios {
        
        /**
         * Create data for authentication tests
         */
        fun createAuthTestData(): AuthTestData {
            return AuthTestData(
                validUser = createOptimizedUser(
                    "auth-user-valid",
                    DataGenerationConfig(template = DataTemplate.MINIMAL)
                ),
                invalidUser = createOptimizedUser(
                    "auth-user-invalid",
                    DataGenerationConfig(template = DataTemplate.MINIMAL)
                ).copy(email = "invalid-email"),
                newUser = createOptimizedUser(
                    "auth-user-new",
                    DataGenerationConfig(template = DataTemplate.MINIMAL)
                ).copy(onboardingComplete = false)
            )
        }
        
        data class AuthTestData(
            val validUser: User,
            val invalidUser: User,
            val newUser: User
        )
        
        /**
         * Create data for sync tests
         */
        fun createSyncTestData(userId: String): SyncTestData {
            val config = DataGenerationConfig(template = DataTemplate.STANDARD)
            
            return SyncTestData(
                user = createOptimizedUser(userId, config),
                dailyLogs = createDailyLogBatch(userId, 7, config = config),
                cycles = createCycleBatch(userId, 3, config = config)
            )
        }
        
        data class SyncTestData(
            val user: User,
            val dailyLogs: List<DailyLog>,
            val cycles: List<Cycle>
        )
        
        /**
         * Create data for performance tests
         */
        fun createPerformanceTestData(userCount: Int = 100): PerformanceTestData {
            val config = DataGenerationConfig(
                template = DataTemplate.PERFORMANCE,
                generateTimestamps = false,
                reuseImmutableData = true
            )
            
            val users = createUserBatch(userCount, "perf-user", config)
            val allLogs = users.flatMap { user ->
                createDailyLogBatch(user.id, 30, config = config)
            }
            val allCycles = users.flatMap { user ->
                createCycleBatch(user.id, 6, config = config)
            }
            
            return PerformanceTestData(
                users = users,
                dailyLogs = allLogs,
                cycles = allCycles
            )
        }
        
        data class PerformanceTestData(
            val users: List<User>,
            val dailyLogs: List<DailyLog>,
            val cycles: List<Cycle>
        )
        
        /**
         * Create data for integration tests
         */
        fun createIntegrationTestData(userId: String): IntegrationTestData {
            val config = DataGenerationConfig(template = DataTemplate.COMPREHENSIVE)
            
            return IntegrationTestData(
                user = createOptimizedUser(userId, config),
                recentLogs = createDailyLogBatch(userId, 14, config = config),
                currentCycle = createOptimizedCycle(userId, config),
                previousCycles = createCycleBatch(userId, 2, config = config)
            )
        }
        
        data class IntegrationTestData(
            val user: User,
            val recentLogs: List<DailyLog>,
            val currentCycle: Cycle,
            val previousCycles: List<Cycle>
        )
    }
    
    /**
     * Utility methods for data validation and optimization
     */
    object Utils {
        
        /**
         * Estimate memory usage of test data
         */
        fun estimateMemoryUsage(data: Any): Long {
            return when (data) {
                is User -> 1024L // 1KB estimate
                is DailyLog -> 2048L // 2KB estimate
                is Cycle -> 4096L // 4KB estimate
                is List<*> -> data.sumOf { estimateMemoryUsage(it ?: 0) }
                else -> 512L // Default estimate
            }
        }
        
        /**
         * Validate test data consistency
         */
        fun validateTestData(user: User, logs: List<DailyLog>, cycles: List<Cycle>): ValidationResult {
            val issues = mutableListOf<String>()
            
            // Check user ID consistency
            val invalidLogs = logs.filter { it.userId != user.id }
            if (invalidLogs.isNotEmpty()) {
                issues.add("${invalidLogs.size} logs have mismatched user ID")
            }
            
            val invalidCycles = cycles.filter { it.userId != user.id }
            if (invalidCycles.isNotEmpty()) {
                issues.add("${invalidCycles.size} cycles have mismatched user ID")
            }
            
            // Check date consistency
            val futureLogs = logs.filter { it.date > LocalDate(2025, 12, 31) }
            if (futureLogs.isNotEmpty()) {
                issues.add("${futureLogs.size} logs have future dates")
            }
            
            return ValidationResult(
                isValid = issues.isEmpty(),
                issues = issues
            )
        }
        
        data class ValidationResult(
            val isValid: Boolean,
            val issues: List<String>
        )
        
        /**
         * Optimize test data for performance
         */
        fun optimizeForPerformance(data: List<Any>): List<Any> {
            // Remove unnecessary fields, deduplicate, etc.
            return data.distinctBy { 
                when (it) {
                    is User -> it.id
                    is DailyLog -> "${it.userId}-${it.date}"
                    is Cycle -> it.id
                    else -> it.hashCode()
                }
            }
        }
    }
    
    /**
     * Reset templates and clear caches
     */
    fun reset() {
        userTemplates.clear()
        dailyLogTemplates.clear()
        cycleTemplates.clear()
        initializeTemplates()
    }
    
    /**
     * Get statistics about template usage
     */
    fun getTemplateStats(): TemplateStats {
        return TemplateStats(
            userTemplates = userTemplates.size,
            dailyLogTemplates = dailyLogTemplates.size,
            cycleTemplates = cycleTemplates.size,
            totalMemoryEstimate = (userTemplates.size * 1024L) + 
                                 (dailyLogTemplates.size * 2048L) + 
                                 (cycleTemplates.size * 4096L)
        )
    }
    
    data class TemplateStats(
        val userTemplates: Int,
        val dailyLogTemplates: Int,
        val cycleTemplates: Int,
        val totalMemoryEstimate: Long
    )
}

/**
 * Extension functions for easier usage
 */
fun EfficientTestDataFactory.createMinimalUser(userId: String): User {
    return createOptimizedUser(userId, EfficientTestDataFactory.DataGenerationConfig(
        template = EfficientTestDataFactory.DataTemplate.MINIMAL
    ))
}

fun EfficientTestDataFactory.createPerformanceUser(userId: String): User {
    return createOptimizedUser(userId, EfficientTestDataFactory.DataGenerationConfig(
        template = EfficientTestDataFactory.DataTemplate.PERFORMANCE,
        generateTimestamps = false
    ))
}

fun EfficientTestDataFactory.createMinimalDailyLog(userId: String, date: LocalDate): DailyLog {
    return createOptimizedDailyLog(userId, date, EfficientTestDataFactory.DataGenerationConfig(
        template = EfficientTestDataFactory.DataTemplate.MINIMAL
    ))
}

fun EfficientTestDataFactory.createPerformanceDailyLog(userId: String, date: LocalDate): DailyLog {
    return createOptimizedDailyLog(userId, date, EfficientTestDataFactory.DataGenerationConfig(
        template = EfficientTestDataFactory.DataTemplate.PERFORMANCE,
        generateTimestamps = false
    ))
}