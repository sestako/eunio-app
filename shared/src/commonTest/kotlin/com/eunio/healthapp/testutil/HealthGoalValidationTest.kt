package com.eunio.healthapp.testutil

import com.eunio.healthapp.domain.model.HealthGoal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test to validate HealthGoal enum usage and ensure all references are correct
 */
class HealthGoalValidationTest {
    
    @Test
    fun `all HealthGoal enum values are valid`() {
        val expectedValues = setOf(
            HealthGoal.CONCEPTION,
            HealthGoal.CONTRACEPTION,
            HealthGoal.CYCLE_TRACKING,
            HealthGoal.GENERAL_HEALTH
        )
        
        val actualValues = HealthGoal.values().toSet()
        assertEquals(expectedValues, actualValues, "HealthGoal enum should contain exactly the expected values")
    }
    
    @Test
    fun `HealthGoalTestUtils contains all valid enum values`() {
        val utilsValues = HealthGoalTestUtils.ALL_HEALTH_GOALS.toSet()
        val enumValues = HealthGoal.values().toSet()
        
        assertEquals(enumValues, utilsValues, "HealthGoalTestUtils should contain all enum values")
    }
    
    @Test
    fun `TestDataBuilder creates users with valid HealthGoal values`() {
        val conceptionUser = TestDataBuilder.createConceptionUser()
        assertEquals(HealthGoal.CONCEPTION, conceptionUser.primaryGoal)
        
        val contraceptionUser = TestDataBuilder.createContraceptionUser()
        assertEquals(HealthGoal.CONTRACEPTION, contraceptionUser.primaryGoal)
        
        val cycleUser = TestDataBuilder.createCycleTrackingUser()
        assertEquals(HealthGoal.CYCLE_TRACKING, cycleUser.primaryGoal)
        
        val healthUser = TestDataBuilder.createGeneralHealthUser()
        assertEquals(HealthGoal.GENERAL_HEALTH, healthUser.primaryGoal)
    }
    
    @Test
    fun `createUsersWithAllHealthGoals creates one user per HealthGoal type`() {
        val users = TestDataBuilder.createUsersWithAllHealthGoals()
        assertEquals(4, users.size, "Should create one user for each HealthGoal type")
        
        val goals = users.map { it.primaryGoal }.toSet()
        val expectedGoals = HealthGoal.values().toSet()
        assertEquals(expectedGoals, goals, "Should have users for all HealthGoal types")
    }
    
    @Test
    fun `HealthGoal string conversion works correctly`() {
        HealthGoal.values().forEach { goal ->
            val stringValue = HealthGoalTestUtils.getHealthGoalDatabaseValue(goal)
            assertEquals(goal.name, stringValue, "Database value should match enum name")
            
            val parsedGoal = HealthGoalTestUtils.parseHealthGoal(stringValue)
            assertNotNull(parsedGoal, "Should be able to parse valid enum string")
            assertEquals(goal, parsedGoal, "Parsed goal should match original")
        }
    }
    
    @Test
    fun `invalid HealthGoal strings return null`() {
        val invalidValues = listOf(
            "FERTILITY_TRACKING", // This was mentioned in the task as incorrect
            "INVALID_GOAL",
            "fertility_tracking",
            "",
            "null"
        )
        
        invalidValues.forEach { invalidValue ->
            val result = HealthGoalTestUtils.parseHealthGoal(invalidValue)
            assertEquals(null, result, "Invalid value '$invalidValue' should return null")
        }
    }
    
    @Test
    fun `all HealthGoal values have descriptions`() {
        HealthGoal.values().forEach { goal ->
            val description = HealthGoalTestUtils.getHealthGoalDescription(goal)
            assertTrue(description.isNotBlank(), "HealthGoal $goal should have a non-blank description")
        }
    }
    
    @Test
    fun `HealthGoal validation works correctly`() {
        HealthGoal.values().forEach { goal ->
            assertTrue(
                HealthGoalTestUtils.isValidHealthGoal(goal),
                "HealthGoal $goal should be valid"
            )
        }
    }
    
    @Test
    fun `HealthGoal test scenarios are complete`() {
        val scenarios = HealthGoalTestUtils.createHealthGoalTestScenarios()
        assertEquals(4, scenarios.size, "Should have scenarios for all HealthGoal types")
        
        val scenarioGoals = scenarios.map { it.second }.toSet()
        val allGoals = HealthGoal.values().toSet()
        assertEquals(allGoals, scenarioGoals, "Scenarios should cover all HealthGoal types")
    }
    
    @Test
    fun `HealthGoal transitions are valid`() {
        HealthGoal.values().forEach { from ->
            HealthGoal.values().forEach { to ->
                assertTrue(
                    HealthGoalTestUtils.isValidHealthGoalTransition(from, to),
                    "Transition from $from to $to should be valid"
                )
            }
        }
    }
}