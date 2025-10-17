package com.eunio.healthapp.data.repository

import com.eunio.healthapp.data.remote.FirestoreService
import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.Cycle
import com.eunio.healthapp.domain.util.ErrorHandler
import com.eunio.healthapp.domain.util.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CycleRepositoryImplTest {
    
    private val firestoreService = mockk<FirestoreService>()
    private val errorHandler = mockk<ErrorHandler>()
    
    private val repository = CycleRepositoryImpl(
        firestoreService = firestoreService,
        errorHandler = errorHandler
    )
    
    private val testUserId = "test-user-id"
    private val testCycle = Cycle(
        id = "test-cycle-id",
        userId = testUserId,
        startDate = LocalDate(2024, 1, 1),
        endDate = LocalDate(2024, 1, 28),
        cycleLength = 28
    )
    
    @Test
    fun `getCurrentCycle returns current cycle from firestore`() = runTest {
        // Given
        coEvery { firestoreService.getCurrentCycle(testUserId) } returns Result.success(testCycle)
        
        // When
        val result = repository.getCurrentCycle(testUserId)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(testCycle, result.getOrNull())
        coVerify { firestoreService.getCurrentCycle(testUserId) }
    }
    
    @Test
    fun `getCurrentCycle returns null when no current cycle exists`() = runTest {
        // Given
        coEvery { firestoreService.getCurrentCycle(testUserId) } returns Result.success(null)
        
        // When
        val result = repository.getCurrentCycle(testUserId)
        
        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }
    
    @Test
    fun `getCycleHistory returns cycles with valid limit`() = runTest {
        // Given
        val cycles = listOf(testCycle)
        coEvery { firestoreService.getCycleHistory(testUserId, 12) } returns Result.success(cycles)
        
        // When
        val result = repository.getCycleHistory(testUserId, 12)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(cycles, result.getOrNull())
        coVerify { firestoreService.getCycleHistory(testUserId, 12) }
    }
    
    @Test
    fun `getCycleHistory fails with validation error for invalid limit`() = runTest {
        // Given
        every { errorHandler.createValidationError("Limit must be positive", "limit") } returns 
            AppError.ValidationError("Limit must be positive", "limit")
        
        // When
        val result = repository.getCycleHistory(testUserId, 0)
        
        // Then
        assertTrue(result.isError)
        assertIs<AppError.ValidationError>(result.errorOrNull())
    }
    
    @Test
    fun `startNewCycle creates new cycle and ends current one`() = runTest {
        // Given
        val currentCycle = testCycle.copy(endDate = null)
        val startDate = LocalDate(2024, 1, 29)
        
        coEvery { firestoreService.getCurrentCycle(testUserId) } returns Result.success(currentCycle)
        coEvery { firestoreService.updateCycle(any()) } returns Result.success(Unit)
        coEvery { firestoreService.saveCycle(any()) } returns Result.success(Unit)
        
        // When
        val result = repository.startNewCycle(testUserId, startDate)
        
        // Then
        assertTrue(result.isSuccess)
        val newCycle = result.getOrNull()!!
        assertEquals(testUserId, newCycle.userId)
        assertEquals(startDate, newCycle.startDate)
        
        coVerify { firestoreService.getCurrentCycle(testUserId) }
        coVerify { firestoreService.updateCycle(any()) }
        coVerify { firestoreService.saveCycle(any()) }
    }
    
    @Test
    fun `startNewCycle fails when start date is in future`() = runTest {
        // Given
        val futureDate = LocalDate(2025, 12, 31)
        every { errorHandler.createValidationError("Cycle start date cannot be in the future", "startDate") } returns 
            AppError.ValidationError("Cycle start date cannot be in the future", "startDate")
        
        // When
        val result = repository.startNewCycle(testUserId, futureDate)
        
        // Then
        assertTrue(result.isError)
        assertIs<AppError.ValidationError>(result.errorOrNull())
    }
    
    @Test
    fun `updateCycle validates cycle data before updating`() = runTest {
        // Given
        coEvery { firestoreService.updateCycle(testCycle) } returns Result.success(Unit)
        
        // When
        val result = repository.updateCycle(testCycle)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { firestoreService.updateCycle(testCycle) }
    }
    
    @Test
    fun `updateCycle fails when end date is before start date`() = runTest {
        // Given
        val invalidCycle = testCycle.copy(
            startDate = LocalDate(2024, 1, 15),
            endDate = LocalDate(2024, 1, 10)
        )
        every { errorHandler.createValidationError("End date cannot be before start date", "endDate") } returns 
            AppError.ValidationError("End date cannot be before start date", "endDate")
        
        // When
        val result = repository.updateCycle(invalidCycle)
        
        // Then
        assertTrue(result.isError)
        assertIs<AppError.ValidationError>(result.errorOrNull())
    }
    
    @Test
    fun `updateCycle fails when cycle length is invalid`() = runTest {
        // Given
        val invalidCycle = testCycle.copy(cycleLength = -5)
        every { errorHandler.createValidationError("Cycle length must be positive", "cycleLength") } returns 
            AppError.ValidationError("Cycle length must be positive", "cycleLength")
        
        // When
        val result = repository.updateCycle(invalidCycle)
        
        // Then
        assertTrue(result.isError)
        assertIs<AppError.ValidationError>(result.errorOrNull())
    }
    
    @Test
    fun `endCurrentCycle ends active cycle with calculated length`() = runTest {
        // Given
        val activeCycle = testCycle.copy(endDate = null)
        val endDate = LocalDate(2024, 1, 28)
        
        coEvery { firestoreService.getCurrentCycle(testUserId) } returns Result.success(activeCycle)
        coEvery { firestoreService.updateCycle(any()) } returns Result.success(Unit)
        
        // When
        val result = repository.endCurrentCycle(testUserId, endDate)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { 
            firestoreService.updateCycle(match { cycle ->
                cycle.endDate == endDate && cycle.cycleLength == 28
            })
        }
    }
    
    @Test
    fun `endCurrentCycle fails when no active cycle exists`() = runTest {
        // Given
        coEvery { firestoreService.getCurrentCycle(testUserId) } returns Result.success(null)
        
        // When
        val result = repository.endCurrentCycle(testUserId, LocalDate(2024, 1, 28))
        
        // Then
        assertTrue(result.isError)
        assertIs<AppError.ValidationError>(result.errorOrNull())
    }
    
    @Test
    fun `getCyclesInRange filters cycles by date range`() = runTest {
        // Given
        val cycles = listOf(
            testCycle.copy(startDate = LocalDate(2024, 1, 1)),
            testCycle.copy(startDate = LocalDate(2024, 2, 1)),
            testCycle.copy(startDate = LocalDate(2024, 3, 1))
        )
        coEvery { firestoreService.getCycleHistory(testUserId, 100) } returns Result.success(cycles)
        
        // When
        val result = repository.getCyclesInRange(
            testUserId,
            LocalDate(2024, 1, 15),
            LocalDate(2024, 2, 15)
        )
        
        // Then
        assertTrue(result.isSuccess)
        val filteredCycles = result.getOrNull()!!
        assertEquals(2, filteredCycles.size)
    }
    
    @Test
    fun `getCyclesInRange fails when end date is before start date`() = runTest {
        // Given
        every { errorHandler.createValidationError("End date cannot be before start date", "dateRange") } returns 
            AppError.ValidationError("End date cannot be before start date", "dateRange")
        
        // When
        val result = repository.getCyclesInRange(
            testUserId,
            LocalDate(2024, 2, 1),
            LocalDate(2024, 1, 1)
        )
        
        // Then
        assertTrue(result.isError)
        assertIs<AppError.ValidationError>(result.errorOrNull())
    }
    
    @Test
    fun `getAverageCycleLength calculates average from completed cycles`() = runTest {
        // Given
        val cycles = listOf(
            testCycle.copy(cycleLength = 28),
            testCycle.copy(cycleLength = 30),
            testCycle.copy(cycleLength = 26)
        )
        coEvery { firestoreService.getCycleHistory(testUserId, 6) } returns Result.success(cycles)
        
        // When
        val result = repository.getAverageCycleLength(testUserId, 6)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(28.0, result.getOrNull())
    }
    
    @Test
    fun `getAverageCycleLength returns null when no completed cycles exist`() = runTest {
        // Given
        val cycles = listOf(testCycle.copy(cycleLength = null))
        coEvery { firestoreService.getCycleHistory(testUserId, 6) } returns Result.success(cycles)
        
        // When
        val result = repository.getAverageCycleLength(testUserId, 6)
        
        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }
    
    @Test
    fun `predictNextPeriod uses average cycle length for prediction`() = runTest {
        // Given
        val currentCycle = testCycle.copy(
            startDate = LocalDate(2024, 1, 1),
            endDate = null
        )
        val historyCycles = listOf(
            testCycle.copy(cycleLength = 28),
            testCycle.copy(cycleLength = 30)
        )
        
        coEvery { firestoreService.getCurrentCycle(testUserId) } returns Result.success(currentCycle)
        coEvery { firestoreService.getCycleHistory(testUserId, 6) } returns Result.success(historyCycles)
        
        // When
        val result = repository.predictNextPeriod(testUserId)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(LocalDate(2024, 1, 30), result.getOrNull()) // 1 + 29 days (average of 28 and 30)
    }
    
    @Test
    fun `predictNextPeriod uses default length when no history exists`() = runTest {
        // Given
        val currentCycle = testCycle.copy(
            startDate = LocalDate(2024, 1, 1),
            endDate = null
        )
        
        coEvery { firestoreService.getCurrentCycle(testUserId) } returns Result.success(currentCycle)
        coEvery { firestoreService.getCycleHistory(testUserId, 6) } returns Result.success(emptyList())
        
        // When
        val result = repository.predictNextPeriod(testUserId)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(LocalDate(2024, 1, 29), result.getOrNull()) // 1 + 28 days (default)
    }
    
    @Test
    fun `confirmOvulation updates cycle with ovulation date and luteal phase`() = runTest {
        // Given
        val cycle = testCycle.copy(
            startDate = LocalDate(2024, 1, 1),
            endDate = LocalDate(2024, 1, 28)
        )
        val ovulationDate = LocalDate(2024, 1, 14)
        
        coEvery { firestoreService.getCycle("", testCycle.id) } returns Result.success(cycle)
        coEvery { firestoreService.updateCycle(any()) } returns Result.success(Unit)
        
        // When
        val result = repository.confirmOvulation(testCycle.id, ovulationDate)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { 
            firestoreService.updateCycle(match { updatedCycle ->
                updatedCycle.confirmedOvulationDate == ovulationDate && 
                updatedCycle.lutealPhaseLength == 14 // 28 - 14
            })
        }
    }
    
    @Test
    fun `confirmOvulation fails when ovulation date is before cycle start`() = runTest {
        // Given
        val cycle = testCycle.copy(startDate = LocalDate(2024, 1, 15))
        val ovulationDate = LocalDate(2024, 1, 10)
        
        coEvery { firestoreService.getCycle("", testCycle.id) } returns Result.success(cycle)
        every { errorHandler.createValidationError("Ovulation date cannot be before cycle start", "ovulationDate") } returns 
            AppError.ValidationError("Ovulation date cannot be before cycle start", "ovulationDate")
        
        // When
        val result = repository.confirmOvulation(testCycle.id, ovulationDate)
        
        // Then
        assertTrue(result.isError)
        assertIs<AppError.ValidationError>(result.errorOrNull())
    }
}