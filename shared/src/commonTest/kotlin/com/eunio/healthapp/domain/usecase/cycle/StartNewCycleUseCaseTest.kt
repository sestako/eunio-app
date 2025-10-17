package com.eunio.healthapp.domain.usecase.cycle

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.testutil.MockServices
import kotlinx.datetime.LocalDate
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StartNewCycleUseCaseTest {
    
    private val mockServices = MockServices()
    private val startNewCycleUseCase = StartNewCycleUseCase(mockServices.cycleRepository, mockServices.logRepository)
    
    @Test
    fun `start new cycle with valid date should succeed`() = runTest {
        val result = startNewCycleUseCase.invoke(
            userId = "user123",
            startDate = LocalDate(2024, 1, 15)
        )
        
        assertTrue(result.isSuccess)
        val cycle = result.getOrNull()!!
        assertEquals("user123", cycle.userId)
        assertEquals(LocalDate(2024, 1, 15), cycle.startDate)
    }
    
    @Test
    fun `start new cycle with future date should fail`() = runTest {
        val futureDate = LocalDate(2025, 12, 31)
        val result = startNewCycleUseCase.invoke(
            userId = "user123",
            startDate = futureDate
        )
        
        assertTrue(result.isError)
        assertTrue(result.errorOrNull() is AppError)
    }
    
    @Test
    fun `start new cycle with empty user id should fail`() = runTest {
        val result = startNewCycleUseCase.invoke(
            userId = "",
            startDate = LocalDate(2024, 1, 15)
        )
        
        assertTrue(result.isError)
        assertTrue(result.errorOrNull() is AppError)
    }
    
    @Test
    fun `start new cycle with very old date should fail`() = runTest {
        val veryOldDate = LocalDate(2020, 1, 1)
        val result = startNewCycleUseCase.invoke(
            userId = "user123",
            startDate = veryOldDate
        )
        
        assertTrue(result.isError)
        assertTrue(result.errorOrNull() is AppError)
    }
    
    @Test
    fun `start new cycle with today's date should succeed`() = runTest {
        val today = LocalDate(2024, 1, 15) // Assuming this is "today" for the test
        val result = startNewCycleUseCase.invoke(
            userId = "user123",
            startDate = today
        )
        
        assertTrue(result.isSuccess)
    }
}