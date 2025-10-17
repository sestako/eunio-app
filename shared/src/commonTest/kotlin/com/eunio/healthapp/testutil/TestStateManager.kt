package com.eunio.healthapp.testutil

import com.eunio.healthapp.domain.model.User
import com.eunio.healthapp.domain.model.UserPreferences
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Test state manager for proper test isolation
 */
class TestStateManager {
    private val isolatedStates = mutableMapOf<String, TestState>()
    
    fun createIsolatedState(testName: String): TestState {
        val state = TestState(
            testName = testName,
            userId = "test-user-${Clock.System.now().toEpochMilliseconds()}",
            sessionId = "session-${Clock.System.now().toEpochMilliseconds()}",
            timestamp = Clock.System.now()
        )
        isolatedStates[testName] = state
        return state
    }
    
    fun getState(testName: String): TestState? {
        return isolatedStates[testName]
    }
    
    fun cleanupState(testName: String) {
        isolatedStates.remove(testName)
    }
    
    fun resetAllStates() {
        isolatedStates.clear()
    }
    
    fun getActiveStateCount(): Int = isolatedStates.size
}

/**
 * Test state data class for isolated test execution
 */
data class TestState(
    val testName: String,
    val userId: String,
    val sessionId: String,
    val timestamp: Instant,
    val userData: User? = null,
    val preferences: UserPreferences? = null
)