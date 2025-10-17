package com.eunio.healthapp.domain.usecase.logging

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.*
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.testutil.MockServices
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class SaveDailyLogUseCaseTest {
    
    private val mockServices = MockServices()
    private val saveDailyLogUseCase = SaveDailyLogUseCase(mockServices.logRepository)
    
    @Test
    fun `save daily log with valid data should succeed`() = runTest {
        val log = DailyLog(
            id = "log123",
            userId = "user123",
            date = LocalDate(2024, 1, 15),
            bbt = 98.6,
            cervicalMucus = CervicalMucus.CREAMY,
            symptoms = listOf(Symptom.CRAMPS),
            mood = Mood.HAPPY,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val result = saveDailyLogUseCase(log)
        
        assertTrue(result is Result.Success)
    }
    
    @Test
    fun `save daily log with future date should fail`() = runTest {
        val futureDate = LocalDate(2025, 12, 31)
        val log = DailyLog(
            id = "log123",
            userId = "user123",
            date = futureDate,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val result = saveDailyLogUseCase(log)
        
        assertTrue(result is Result.Error)
        assertTrue(result.error is AppError.ValidationError)
    }
    
    @Test
    fun `save daily log with invalid BBT should fail`() = runTest {
        val log = DailyLog(
            id = "log123",
            userId = "user123",
            date = LocalDate(2024, 1, 15),
            bbt = 150.0, // Invalid temperature
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val result = saveDailyLogUseCase(log)
        
        assertTrue(result is Result.Error)
        assertTrue(result.error is AppError.ValidationError)
    }
    
    @Test
    fun `save daily log with empty user id should fail`() = runTest {
        val log = DailyLog(
            id = "log123",
            userId = "",
            date = LocalDate(2024, 1, 15),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val result = saveDailyLogUseCase(log)
        
        assertTrue(result is Result.Error)
        assertTrue(result.error is AppError.ValidationError)
    }
    
    @Test
    fun `save daily log with valid BBT range should succeed`() = runTest {
        val validTemperatures = listOf(96.0, 98.6, 100.0)
        
        for (temp in validTemperatures) {
            val log = DailyLog(
                id = "log123",
                userId = "user123",
                date = LocalDate(2024, 1, 15),
                bbt = temp,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            )
            
            val result = saveDailyLogUseCase(log)
            assertTrue(result is Result.Success, "Temperature $temp should be valid")
        }
    }
}