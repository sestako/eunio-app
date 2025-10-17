package com.eunio.healthapp.data.sync

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.*
import com.eunio.healthapp.domain.util.ErrorHandler
import com.eunio.healthapp.testutil.TestDataFactory
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class ConflictResolutionServiceTest {
    
    private lateinit var conflictResolutionService: ConflictResolutionService
    private lateinit var errorHandler: ErrorHandler
    
    @BeforeTest
    fun setup() {
        errorHandler = ErrorHandler()
        conflictResolutionService = ConflictResolutionService(errorHandler)
    }
    
    @Test
    fun `resolveUserConflict with LAST_WRITE_WINS should prefer more recent user`() = runTest {
        // Arrange
        val now = Clock.System.now()
        val localUser = TestDataFactory.createTestUser("user1").copy(
            name = "Local Name",
            updatedAt = now
        )
        val remoteUser = TestDataFactory.createTestUser("user1").copy(
            name = "Remote Name",
            updatedAt = now - 1.hours
        )
        
        // Act
        val result = conflictResolutionService.resolveUserConflict(
            localUser, 
            remoteUser, 
            ConflictResolutionStrategy.LAST_WRITE_WINS
        )
        
        // Assert
        assertTrue(result.isSuccess)
        val resolution = result.getOrThrow()
        assertEquals("Local Name", resolution.resolvedData.name)
        assertEquals(ConflictType.TIMESTAMP_CONFLICT, resolution.conflictType)
        assertTrue(resolution.wasAutoResolved)
    }
    
    @Test
    fun `resolveUserConflict with FIELD_LEVEL_MERGE should merge fields intelligently`() = runTest {
        // Arrange
        val now = Clock.System.now()
        val localUser = TestDataFactory.createTestUser("user1").copy(
            name = "Local Name",
            onboardingComplete = true,
            updatedAt = now
        )
        val remoteUser = TestDataFactory.createTestUser("user1").copy(
            name = "Remote Name",
            onboardingComplete = false,
            updatedAt = now - 1.hours
        )
        
        // Act
        val result = conflictResolutionService.resolveUserConflict(
            localUser, 
            remoteUser, 
            ConflictResolutionStrategy.FIELD_LEVEL_MERGE
        )
        
        // Assert
        assertTrue(result.isSuccess)
        val resolution = result.getOrThrow()
        assertEquals("Local Name", resolution.resolvedData.name) // More recent
        assertTrue(resolution.resolvedData.onboardingComplete) // Prefer true
        assertEquals(ConflictType.FIELD_CONFLICT, resolution.conflictType)
    }
    
    @Test
    fun `resolveUserConflict with USER_GUIDED should require manual resolution`() = runTest {
        // Arrange
        val localUser = TestDataFactory.createTestUser("user1")
        val remoteUser = TestDataFactory.createTestUser("user1").copy(name = "Different Name")
        
        // Act
        val result = conflictResolutionService.resolveUserConflict(
            localUser, 
            remoteUser, 
            ConflictResolutionStrategy.USER_GUIDED
        )
        
        // Assert
        assertTrue(result.isSuccess)
        val resolution = result.getOrThrow()
        assertEquals(ConflictType.USER_INTERVENTION_REQUIRED, resolution.conflictType)
        assertFalse(resolution.wasAutoResolved)
        assertNotNull(resolution.localVersion)
        assertNotNull(resolution.remoteVersion)
    }
    
    @Test
    fun `resolveDailyLogConflict with LAST_WRITE_WINS should prefer more recent log`() = runTest {
        // Arrange
        val now = Clock.System.now()
        val localLog = TestDataFactory.createTestDailyLog("user1").copy(
            bbt = 98.5,
            updatedAt = now
        )
        val remoteLog = localLog.copy(
            bbt = 98.2,
            updatedAt = now - 30.minutes
        )
        
        // Act
        val result = conflictResolutionService.resolveDailyLogConflict(
            localLog, 
            remoteLog, 
            ConflictResolutionStrategy.LAST_WRITE_WINS
        )
        
        // Assert
        assertTrue(result.isSuccess)
        val resolution = result.getOrThrow()
        assertEquals(98.5, resolution.resolvedData.bbt)
        assertTrue(resolution.wasAutoResolved)
    }
    
    @Test
    fun `resolveDailyLogConflict with FIELD_LEVEL_MERGE should merge fields intelligently`() = runTest {
        // Arrange
        val now = Clock.System.now()
        val localLog = TestDataFactory.createTestDailyLog("user1").copy(
            bbt = 98.5,
            mood = Mood.HAPPY,
            symptoms = listOf(Symptom.CRAMPS),
            notes = "Local notes",
            updatedAt = now
        )
        val remoteLog = localLog.copy(
            bbt = null,
            mood = null,
            symptoms = listOf(Symptom.BLOATING),
            notes = "Remote notes",
            updatedAt = now - 30.minutes
        )
        
        // Act
        val result = conflictResolutionService.resolveDailyLogConflict(
            localLog, 
            remoteLog, 
            ConflictResolutionStrategy.FIELD_LEVEL_MERGE
        )
        
        // Assert
        assertTrue(result.isSuccess)
        val resolution = result.getOrThrow()
        assertEquals(98.5, resolution.resolvedData.bbt) // Prefer non-null
        assertEquals(Mood.HAPPY, resolution.resolvedData.mood) // Prefer non-null
        assertEquals(2, resolution.resolvedData.symptoms.size) // Merge symptoms
        assertTrue(resolution.resolvedData.symptoms.contains(Symptom.CRAMPS))
        assertTrue(resolution.resolvedData.symptoms.contains(Symptom.BLOATING))
        assertTrue(resolution.resolvedData.notes!!.contains("Local notes")) // Merge notes
    }
    
    @Test
    fun `resolveDailyLogConflict should fail for different users`() = runTest {
        // Arrange
        val localLog = TestDataFactory.createTestDailyLog("user1")
        val remoteLog = TestDataFactory.createTestDailyLog("user2")
        
        // Act
        val result = conflictResolutionService.resolveDailyLogConflict(
            localLog, 
            remoteLog, 
            ConflictResolutionStrategy.LAST_WRITE_WINS
        )
        
        // Assert
        assertTrue(result.isError)
        assertTrue(result.errorOrNull() is AppError)
    }
    
    @Test
    fun `resolveDailyLogConflict should fail for different dates`() = runTest {
        // Arrange
        val localLog = TestDataFactory.createTestDailyLog("user1")
        val remoteLog = localLog.copy(date = kotlinx.datetime.LocalDate(2024, 1, 2)) // Different date
        
        // Act
        val result = conflictResolutionService.resolveDailyLogConflict(
            localLog, 
            remoteLog, 
            ConflictResolutionStrategy.LAST_WRITE_WINS
        )
        
        // Assert
        assertTrue(result.isError)
        assertTrue(result.errorOrNull() is AppError)
    }
    
    @Test
    fun `detectConflict should return NO_CONFLICT for identical data`() = runTest {
        // Arrange
        val now = Clock.System.now()
        val data1 = "same data"
        val data2 = "same data"
        
        // Act
        val result = conflictResolutionService.detectConflict(data1, data2, now, now)
        
        // Assert
        assertEquals(ConflictDetectionResult.NO_CONFLICT, result)
    }
    
    @Test
    fun `detectConflict should return SIMULTANEOUS_EDIT for same timestamp`() = runTest {
        // Arrange
        val now = Clock.System.now()
        val data1 = "different data 1"
        val data2 = "different data 2"
        
        // Act
        val result = conflictResolutionService.detectConflict(data1, data2, now, now)
        
        // Assert
        assertEquals(ConflictDetectionResult.SIMULTANEOUS_EDIT, result)
    }
    
    @Test
    fun `detectConflict should return NEAR_SIMULTANEOUS_EDIT for close timestamps`() = runTest {
        // Arrange
        val now = Clock.System.now()
        val data1 = "different data 1"
        val data2 = "different data 2"
        
        // Act
        val result = conflictResolutionService.detectConflict(
            data1, 
            data2, 
            now, 
            now - 5.minutes
        )
        
        // Assert
        assertEquals(ConflictDetectionResult.NEAR_SIMULTANEOUS_EDIT, result)
    }
    
    @Test
    fun `detectConflict should return TIMESTAMP_CONFLICT for distant timestamps`() = runTest {
        // Arrange
        val now = Clock.System.now()
        val data1 = "different data 1"
        val data2 = "different data 2"
        
        // Act
        val result = conflictResolutionService.detectConflict(
            data1, 
            data2, 
            now, 
            now - 30.minutes
        )
        
        // Assert
        assertEquals(ConflictDetectionResult.TIMESTAMP_CONFLICT, result)
    }
}