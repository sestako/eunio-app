package com.eunio.healthapp.testutil

import com.eunio.healthapp.domain.model.HealthGoal
import com.eunio.healthapp.domain.model.User
import com.eunio.healthapp.domain.model.UnitSystem
import kotlinx.datetime.Clock

/**
 * Test utilities for HealthGoal enum usage and User model creation
 * Ensures consistent and correct enum usage across all tests
 */
object HealthGoalTestUtils {
    
    /**
     * All valid HealthGoal enum values for testing
     */
    val ALL_HEALTH_GOALS = listOf(
        HealthGoal.CONCEPTION,
        HealthGoal.CONTRACEPTION,
        HealthGoal.CYCLE_TRACKING,
        HealthGoal.GENERAL_HEALTH
    )
    
    /**
     * Default HealthGoal for testing
     */
    val DEFAULT_HEALTH_GOAL = HealthGoal.CYCLE_TRACKING
    
    /**
     * Creates a test User with specified HealthGoal
     */
    fun createTestUser(
        id: String = "test-user",
        email: String = "test@example.com",
        name: String = "Test User",
        onboardingComplete: Boolean = true,
        primaryGoal: HealthGoal = DEFAULT_HEALTH_GOAL,
        unitSystem: UnitSystem = UnitSystem.METRIC
    ): User {
        val now = Clock.System.now()
        return User(
            id = id,
            email = email,
            name = name,
            onboardingComplete = onboardingComplete,
            primaryGoal = primaryGoal,
            unitSystem = unitSystem,
            createdAt = now,
            updatedAt = now
        )
    }
    
    /**
     * Creates test users for each HealthGoal type
     */
    fun createUsersForAllHealthGoals(): List<User> {
        return ALL_HEALTH_GOALS.mapIndexed { index, goal ->
            createTestUser(
                id = "user-${goal.name.lowercase()}",
                email = "${goal.name.lowercase()}@example.com",
                name = "User ${goal.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }}",
                primaryGoal = goal
            )
        }
    }
    
    /**
     * Validates that a HealthGoal enum value is valid
     */
    fun isValidHealthGoal(goal: HealthGoal): Boolean {
        return goal in ALL_HEALTH_GOALS
    }
    
    /**
     * Gets the string representation of HealthGoal for database storage
     */
    fun getHealthGoalDatabaseValue(goal: HealthGoal): String {
        return goal.name
    }
    
    /**
     * Converts string to HealthGoal enum safely
     */
    fun parseHealthGoal(value: String): HealthGoal? {
        return try {
            HealthGoal.valueOf(value)
        } catch (e: IllegalArgumentException) {
            null
        }
    }
    
    /**
     * Creates test scenarios for different HealthGoal combinations
     */
    fun createHealthGoalTestScenarios(): List<Pair<String, HealthGoal>> {
        return listOf(
            "Conception tracking user" to HealthGoal.CONCEPTION,
            "Contraception user" to HealthGoal.CONTRACEPTION,
            "Cycle tracking user" to HealthGoal.CYCLE_TRACKING,
            "General health user" to HealthGoal.GENERAL_HEALTH
        )
    }
    
    /**
     * Validates HealthGoal transitions for testing
     */
    fun isValidHealthGoalTransition(from: HealthGoal, to: HealthGoal): Boolean {
        // All transitions are valid in the current implementation
        return true
    }
    
    /**
     * Gets expected behavior description for a HealthGoal
     */
    fun getHealthGoalDescription(goal: HealthGoal): String {
        return when (goal) {
            HealthGoal.CONCEPTION -> "Trying to conceive - tracks fertility indicators"
            HealthGoal.CONTRACEPTION -> "Preventing pregnancy - tracks cycle for contraception"
            HealthGoal.CYCLE_TRACKING -> "General cycle tracking - monitors menstrual health"
            HealthGoal.GENERAL_HEALTH -> "Overall health monitoring - basic health tracking"
        }
    }
}