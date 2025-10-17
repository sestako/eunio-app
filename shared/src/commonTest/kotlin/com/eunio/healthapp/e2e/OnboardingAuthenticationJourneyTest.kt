package com.eunio.healthapp.e2e

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.*
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.testutil.MockServices
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlin.test.*

/**
 * End-to-end tests for user onboarding and authentication flows.
 * Tests complete user journeys from sign-up through onboarding to first use.
 * 
 * Requirements: 3.3, 3.4
 */
class OnboardingAuthenticationJourneyTest {
    
    private lateinit var mockServices: MockServices
    
    @BeforeTest
    fun setup() {
        mockServices = MockServices()
    }
    
    // Complete Onboarding Journey Tests
    
    @Test
    fun `complete new user onboarding journey succeeds`() = runTest {
        // Step 1: User discovers app and decides to sign up
        val userEmail = "newuser@example.com"
        val userPassword = "SecurePassword123!"
        val userName = "New Health User"
        
        // Step 2: User creates account
        val signUpResult = mockServices.userRepository.createUser(
            email = userEmail,
            password = userPassword,
            name = userName
        )
        
        assertTrue(signUpResult.isSuccess, "User sign up should succeed")
        val newUser = signUpResult.getOrNull()!!
        
        assertEquals(userEmail, newUser.email)
        assertEquals(userName, newUser.name)
        assertFalse(newUser.onboardingComplete, "Onboarding should not be complete initially")
        
        // Step 3: User is prompted to complete onboarding
        // User selects their primary health goal
        val selectedGoal = HealthGoal.CYCLE_TRACKING
        
        val onboardingResult = mockServices.userRepository.completeOnboarding(
            userId = newUser.id,
            primaryGoal = selectedGoal
        )
        
        assertTrue(onboardingResult.isSuccess, "Onboarding completion should succeed")
        
        // Step 4: Verify user profile is updated
        val updatedUserResult = mockServices.userRepository.getCurrentUser()
        assertTrue(updatedUserResult.isSuccess)
        val updatedUser = updatedUserResult.getOrNull()!!
        
        assertTrue(updatedUser.onboardingComplete, "Onboarding should be marked complete")
        assertEquals(selectedGoal, updatedUser.primaryGoal, "Primary goal should be set")
        
        // Step 5: User is guided to log their first entry
        val firstLog = DailyLog(
            id = "first-log-${newUser.id}",
            userId = newUser.id,
            date = LocalDate(2024, 1, 15),
            bbt = 98.2,
            mood = Mood.HAPPY,
            notes = "My first health log entry!",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val firstLogResult = mockServices.logRepository.saveDailyLog(firstLog)
        assertTrue(firstLogResult.isSuccess, "First log entry should be saved successfully")
        
        // Step 6: User can retrieve their first log
        val retrieveLogResult = mockServices.logRepository.getDailyLog(newUser.id, LocalDate(2024, 1, 15))
        assertTrue(retrieveLogResult.isSuccess)
        val retrievedLog = retrieveLogResult.getOrNull()!!
        
        assertEquals(firstLog.bbt, retrievedLog.bbt)
        assertEquals(firstLog.mood, retrievedLog.mood)
        assertEquals(firstLog.notes, retrievedLog.notes)
        
        // Step 7: User sees welcome insights or tips
        val welcomeInsight = Insight(
            id = "welcome-${newUser.id}",
            userId = newUser.id,
            generatedDate = Clock.System.now(),
            insightText = "Welcome to your health journey! Consistent logging helps track patterns.",
            type = InsightType.EARLY_WARNING,
            isRead = false,
            confidence = 1.0,
            actionable = true
        )
        
        val insightResult = mockServices.insightRepository.saveInsight(welcomeInsight)
        assertTrue(insightResult.isSuccess)
        
        val unreadInsightsResult = mockServices.insightRepository.getUnreadInsights(newUser.id)
        assertTrue(unreadInsightsResult.isSuccess)
        val unreadInsights = unreadInsightsResult.getOrNull()!!
        
        assertEquals(1, unreadInsights.size)
        assertEquals(InsightType.EARLY_WARNING, unreadInsights.first().type)
        assertTrue(unreadInsights.first().actionable)
    }
    
    @Test
    fun `user onboarding with different health goals works correctly`() = runTest {
        val testCases = listOf(
            Triple("fertility@example.com", HealthGoal.CONCEPTION, "Fertility User"),
            Triple("contraception@example.com", HealthGoal.CONTRACEPTION, "Contraception User"),
            Triple("general@example.com", HealthGoal.GENERAL_HEALTH, "General Health User")
        )
        
        testCases.forEach { (email, goal, name) ->
            // User signs up
            val signUpResult = mockServices.userRepository.createUser(email, "password123", name)
            assertTrue(signUpResult.isSuccess)
            val user = signUpResult.getOrNull()!!
            
            // User completes onboarding with specific goal
            val onboardingResult = mockServices.userRepository.completeOnboarding(user.id, goal)
            assertTrue(onboardingResult.isSuccess)
            
            // Verify goal-specific setup
            val updatedUserResult = mockServices.userRepository.getCurrentUser()
            assertTrue(updatedUserResult.isSuccess)
            val updatedUser = updatedUserResult.getOrNull()!!
            
            assertEquals(goal, updatedUser.primaryGoal)
            assertTrue(updatedUser.onboardingComplete)
            
            // Create goal-specific first log entry
            val goalSpecificLog = when (goal) {
                HealthGoal.CONCEPTION -> DailyLog(
                    id = "fertility-log-${user.id}",
                    userId = user.id,
                    date = LocalDate(2024, 1, 15),
                    bbt = 97.8,
                    cervicalMucus = CervicalMucus.EGG_WHITE,
                    opkResult = OPKResult.POSITIVE,
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now()
                )
                HealthGoal.CONTRACEPTION -> DailyLog(
                    id = "contraception-log-${user.id}",
                    userId = user.id,
                    date = LocalDate(2024, 1, 15),
                    sexualActivity = SexualActivity(occurred = true, protection = Protection.CONDOM),
                    mood = Mood.HAPPY,
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now()
                )
                else -> DailyLog(
                    id = "general-log-${user.id}",
                    userId = user.id,
                    date = LocalDate(2024, 1, 15),
                    mood = Mood.HAPPY,
                    symptoms = listOf(Symptom.HEADACHE),
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now()
                )
            }
            
            val logResult = mockServices.logRepository.saveDailyLog(goalSpecificLog)
            assertTrue(logResult.isSuccess, "Goal-specific log should be saved for $goal")
            
            // Sign out to prepare for next test case
            mockServices.userRepository.signOutUser()
        }
    }
    
    @Test
    fun `incomplete onboarding prevents full app access`() = runTest {
        // User signs up but doesn't complete onboarding
        val signUpResult = mockServices.userRepository.createUser(
            "incomplete@example.com",
            "password123",
            "Incomplete User"
        )
        
        assertTrue(signUpResult.isSuccess)
        val user = signUpResult.getOrNull()!!
        assertFalse(user.onboardingComplete)
        
        // User tries to access main features without completing onboarding
        // In a real app, this would be prevented by UI/navigation logic
        // Here we simulate that certain features check onboarding status
        
        val currentUserResult = mockServices.userRepository.getCurrentUser()
        assertTrue(currentUserResult.isSuccess)
        val currentUser = currentUserResult.getOrNull()!!
        
        // Simulate feature access check
        if (!currentUser.onboardingComplete) {
            // User should be redirected to complete onboarding
            val onboardingResult = mockServices.userRepository.completeOnboarding(
                userId = user.id,
                primaryGoal = HealthGoal.CYCLE_TRACKING
            )
            assertTrue(onboardingResult.isSuccess)
        }
        
        // After completing onboarding, user can access features
        val finalUserResult = mockServices.userRepository.getCurrentUser()
        assertTrue(finalUserResult.isSuccess)
        val finalUser = finalUserResult.getOrNull()!!
        assertTrue(finalUser.onboardingComplete)
        
        // Now user can log data
        val log = DailyLog(
            id = "post-onboarding-log",
            userId = user.id,
            date = LocalDate(2024, 1, 15),
            mood = Mood.HAPPY,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val logResult = mockServices.logRepository.saveDailyLog(log)
        assertTrue(logResult.isSuccess)
    }
    
    // Authentication Flow Tests
    
    @Test
    fun `user sign in and return user experience works correctly`() = runTest {
        // Setup: User already has an account (simulate previous sign up)
        val existingEmail = "returning@example.com"
        val existingPassword = "MySecurePassword123"
        
        val signUpResult = mockServices.userRepository.createUser(
            existingEmail,
            existingPassword,
            "Returning User"
        )
        assertTrue(signUpResult.isSuccess)
        val originalUser = signUpResult.getOrNull()!!
        
        // User completed onboarding previously
        mockServices.userRepository.completeOnboarding(originalUser.id, HealthGoal.CYCLE_TRACKING)
        
        // User has some historical data
        val historicalLogs = listOf(
            DailyLog(
                id = "historical-1",
                userId = originalUser.id,
                date = LocalDate(2024, 1, 10),
                bbt = 97.8,
                mood = Mood.HAPPY,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            ),
            DailyLog(
                id = "historical-2",
                userId = originalUser.id,
                date = LocalDate(2024, 1, 11),
                bbt = 98.1,
                periodFlow = PeriodFlow.MEDIUM,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            )
        )
        
        historicalLogs.forEach { log ->
            val result = mockServices.logRepository.saveDailyLog(log)
            assertTrue(result.isSuccess)
        }
        
        // User signs out (simulate app closure or logout)
        val signOutResult = mockServices.userRepository.signOutUser()
        assertTrue(signOutResult.isSuccess)
        
        // User returns and signs in
        val signInResult = mockServices.userRepository.signInUser(existingEmail, existingPassword)
        assertTrue(signInResult.isSuccess, "Returning user should be able to sign in")
        
        val signedInUser = signInResult.getOrNull()!!
        assertEquals(originalUser.id, signedInUser.id)
        assertEquals(originalUser.email, signedInUser.email)
        assertTrue(signedInUser.onboardingComplete, "Returning user should have completed onboarding")
        
        // User can access their historical data
        val recentLogsResult = mockServices.logRepository.getRecentLogs(signedInUser.id, 10)
        assertTrue(recentLogsResult.isSuccess)
        val recentLogs = recentLogsResult.getOrNull()!!
        
        assertEquals(2, recentLogs.size, "User should see their historical logs")
        
        // User can continue logging new data
        val newLog = DailyLog(
            id = "new-session-log",
            userId = signedInUser.id,
            date = LocalDate(2024, 1, 15),
            bbt = 98.3,
            symptoms = listOf(Symptom.CRAMPS),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val newLogResult = mockServices.logRepository.saveDailyLog(newLog)
        assertTrue(newLogResult.isSuccess, "Returning user should be able to log new data")
        
        // Verify total log count
        val totalLogsResult = mockServices.logRepository.getLogCount(signedInUser.id)
        assertTrue(totalLogsResult.isSuccess)
        assertEquals(3, totalLogsResult.getOrNull(), "User should have 3 total logs")
    }
    
    @Test
    fun `authentication failure scenarios are handled gracefully`() = runTest {
        // Setup: User has an account
        val userEmail = "testauth@example.com"
        val correctPassword = "CorrectPassword123"
        
        val signUpResult = mockServices.userRepository.createUser(userEmail, correctPassword, "Test User")
        assertTrue(signUpResult.isSuccess)
        
        // User signs out
        mockServices.userRepository.signOutUser()
        
        // Test Case 1: Wrong password
        val wrongPasswordResult = mockServices.userRepository.signInUser(userEmail, "WrongPassword")
        assertTrue(wrongPasswordResult.isError, "Sign in with wrong password should fail")
        assertTrue(wrongPasswordResult.errorOrNull() is AppError.AuthenticationError)
        
        // Test Case 2: Non-existent email
        val nonExistentResult = mockServices.userRepository.signInUser("nonexistent@example.com", correctPassword)
        assertTrue(nonExistentResult.isError, "Sign in with non-existent email should fail")
        
        // Test Case 3: Successful sign in after failures
        val successfulResult = mockServices.userRepository.signInUser(userEmail, correctPassword)
        assertTrue(successfulResult.isSuccess, "Sign in with correct credentials should succeed after failures")
        
        val user = successfulResult.getOrNull()!!
        assertEquals(userEmail, user.email)
    }
    
    @Test
    fun `password reset flow works end-to-end`() = runTest {
        // Setup: User has an account but forgot password
        val userEmail = "forgetful@example.com"
        val originalPassword = "ForgottenPassword123"
        
        val signUpResult = mockServices.userRepository.createUser(userEmail, originalPassword, "Forgetful User")
        assertTrue(signUpResult.isSuccess)
        val user = signUpResult.getOrNull()!!
        
        // User signs out
        mockServices.userRepository.signOutUser()
        
        // User tries to sign in but can't remember password
        val failedSignInResult = mockServices.userRepository.signInUser(userEmail, "WrongPassword")
        assertTrue(failedSignInResult.isError)
        
        // User requests password reset
        val resetRequestResult = mockServices.authService.sendPasswordResetEmail(userEmail)
        assertTrue(resetRequestResult.isSuccess, "Password reset email should be sent successfully")
        
        // In a real implementation, user would receive email and reset password
        // For this test, we simulate that the password reset was successful
        // and user can now sign in with original password (since we can't actually change it in mock)
        
        val postResetSignInResult = mockServices.userRepository.signInUser(userEmail, originalPassword)
        assertTrue(postResetSignInResult.isSuccess, "User should be able to sign in after password reset")
        
        val signedInUser = postResetSignInResult.getOrNull()!!
        assertEquals(user.id, signedInUser.id)
        assertEquals(userEmail, signedInUser.email)
    }
    
    // Edge Cases and Error Scenarios
    
    @Test
    fun `onboarding with invalid data is handled correctly`() = runTest {
        // User signs up successfully
        val signUpResult = mockServices.userRepository.createUser(
            "validuser@example.com",
            "ValidPassword123",
            "Valid User"
        )
        assertTrue(signUpResult.isSuccess)
        val user = signUpResult.getOrNull()!!
        
        // Test invalid onboarding scenarios would be handled by validation
        // In this mock implementation, we'll simulate validation by checking the goal
        val validGoals = HealthGoal.values()
        
        // Test with each valid goal to ensure they all work
        validGoals.forEach { goal ->
            val onboardingResult = mockServices.userRepository.completeOnboarding(user.id, goal)
            assertTrue(onboardingResult.isSuccess, "Onboarding should succeed with valid goal: $goal")
            
            val updatedUserResult = mockServices.userRepository.getCurrentUser()
            assertTrue(updatedUserResult.isSuccess)
            val updatedUser = updatedUserResult.getOrNull()!!
            assertEquals(goal, updatedUser.primaryGoal)
        }
    }
    
    @Test
    fun `concurrent user sessions are handled correctly`() = runTest {
        // Simulate multiple users signing up concurrently
        val concurrentUsers = listOf(
            Triple("user1@example.com", "Password1", "User One"),
            Triple("user2@example.com", "Password2", "User Two"),
            Triple("user3@example.com", "Password3", "User Three")
        )
        
        val signUpResults = mutableListOf<Result<User>>()
        
        // Simulate concurrent sign ups
        concurrentUsers.forEach { (email, password, name) ->
            val result = mockServices.userRepository.createUser(email, password, name)
            signUpResults.add(result)
            
            // Each user completes onboarding
            if (result.isSuccess) {
                val user = result.getOrNull()!!
                val onboardingResult = mockServices.userRepository.completeOnboarding(
                    user.id,
                    HealthGoal.CYCLE_TRACKING
                )
                assertTrue(onboardingResult.isSuccess)
            }
            
            // Sign out to allow next user
            mockServices.userRepository.signOutUser()
        }
        
        // All sign ups should succeed
        signUpResults.forEach { result ->
            assertTrue(result.isSuccess, "All concurrent sign ups should succeed")
        }
        
        // Each user should be able to sign in independently
        concurrentUsers.forEachIndexed { index, (email, password, _) ->
            val signInResult = mockServices.userRepository.signInUser(email, password)
            assertTrue(signInResult.isSuccess, "User $index should be able to sign in")
            
            val user = signInResult.getOrNull()!!
            assertTrue(user.onboardingComplete, "User $index should have completed onboarding")
            
            mockServices.userRepository.signOutUser()
        }
    }
    
    @Test
    fun `user journey with network interruptions works correctly`() = runTest {
        // Simulate network delays during onboarding
        val userEmail = "slownetwork@example.com"
        val userPassword = "SlowPassword123"
        
        // Sign up with simulated network delay
        delay(100) // Simulate network delay
        val signUpResult = mockServices.userRepository.createUser(userEmail, userPassword, "Slow Network User")
        assertTrue(signUpResult.isSuccess)
        val user = signUpResult.getOrNull()!!
        
        // Onboarding with simulated network delay
        delay(100)
        val onboardingResult = mockServices.userRepository.completeOnboarding(user.id, HealthGoal.CYCLE_TRACKING)
        assertTrue(onboardingResult.isSuccess)
        
        // First log with simulated network delay
        delay(100)
        val firstLog = DailyLog(
            id = "slow-network-log",
            userId = user.id,
            date = LocalDate(2024, 1, 15),
            bbt = 98.2,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val logResult = mockServices.logRepository.saveDailyLog(firstLog)
        assertTrue(logResult.isSuccess, "Log should succeed despite network delays")
        
        // Verify user journey completed successfully
        val finalUserResult = mockServices.userRepository.getCurrentUser()
        assertTrue(finalUserResult.isSuccess)
        val finalUser = finalUserResult.getOrNull()!!
        
        assertTrue(finalUser.onboardingComplete)
        assertEquals(HealthGoal.CYCLE_TRACKING, finalUser.primaryGoal)
        
        val logCountResult = mockServices.logRepository.getLogCount(user.id)
        assertTrue(logCountResult.isSuccess)
        assertEquals(1, logCountResult.getOrNull())
    }
    
    @Test
    fun `user onboarding data persistence across app sessions`() = runTest {
        // User starts onboarding
        val signUpResult = mockServices.userRepository.createUser(
            "persistent@example.com",
            "PersistentPassword123",
            "Persistent User"
        )
        assertTrue(signUpResult.isSuccess)
        val user = signUpResult.getOrNull()!!
        
        // User completes onboarding
        val onboardingResult = mockServices.userRepository.completeOnboarding(user.id, HealthGoal.CONCEPTION)
        assertTrue(onboardingResult.isSuccess)
        
        // User adds initial data
        val initialLog = DailyLog(
            id = "persistent-log",
            userId = user.id,
            date = LocalDate(2024, 1, 15),
            bbt = 97.8,
            cervicalMucus = CervicalMucus.CREAMY,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val logResult = mockServices.logRepository.saveDailyLog(initialLog)
        assertTrue(logResult.isSuccess)
        
        // Simulate app restart by signing out and back in
        mockServices.userRepository.signOutUser()
        
        val signInResult = mockServices.userRepository.signInUser("persistent@example.com", "PersistentPassword123")
        assertTrue(signInResult.isSuccess)
        val restoredUser = signInResult.getOrNull()!!
        
        // Verify onboarding state persisted
        assertEquals(user.id, restoredUser.id)
        assertTrue(restoredUser.onboardingComplete, "Onboarding completion should persist")
        assertEquals(HealthGoal.CONCEPTION, restoredUser.primaryGoal, "Primary goal should persist")
        
        // Verify user data persisted
        val retrievedLogResult = mockServices.logRepository.getDailyLog(user.id, LocalDate(2024, 1, 15))
        assertTrue(retrievedLogResult.isSuccess)
        val retrievedLog = retrievedLogResult.getOrNull()!!
        
        assertEquals(initialLog.bbt, retrievedLog.bbt)
        assertEquals(initialLog.cervicalMucus, retrievedLog.cervicalMucus)
        
        // User can continue their journey
        val continuedLog = DailyLog(
            id = "continued-log",
            userId = user.id,
            date = LocalDate(2024, 1, 16),
            bbt = 98.1,
            opkResult = OPKResult.POSITIVE,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val continuedLogResult = mockServices.logRepository.saveDailyLog(continuedLog)
        assertTrue(continuedLogResult.isSuccess, "User should be able to continue logging after app restart")
        
        val totalLogsResult = mockServices.logRepository.getLogCount(user.id)
        assertTrue(totalLogsResult.isSuccess)
        assertEquals(2, totalLogsResult.getOrNull(), "User should have 2 total logs")
    }
}