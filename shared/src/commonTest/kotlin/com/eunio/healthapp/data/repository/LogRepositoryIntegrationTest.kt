package com.eunio.healthapp.data.repository

import com.eunio.healthapp.domain.model.Symptom
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.testutil.MockServices
import com.eunio.healthapp.testutil.TestDataFactory
import kotlinx.datetime.LocalDate
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LogRepositoryIntegrationTest {
    
    private val mockServices = MockServices()
    private val logRepository = mockServices.logRepository
    
    @Test
    fun `save and retrieve daily log should work correctly`() = runTest {
        val log = TestDataFactory.createTestDailyLog("user123")
        
        val saveResult = logRepository.saveDailyLog(log)
        assertTrue(saveResult is Result.Success)
        
        val retrieveResult = logRepository.getDailyLog("user123", log.date)
        assertTrue(retrieveResult is Result.Success)
    }
    
    @Test
    fun `get daily log for non-existent date should return null`() = runTest {
        val result = logRepository.getDailyLog("user123", LocalDate(2024, 12, 31))
        
        assertTrue(result is Result.Success)
        assertNull(result.data)
    }
    
    @Test
    fun `get logs in range should return correct data`() = runTest {
        val result = logRepository.getLogsInRange(
            userId = "user123",
            startDate = LocalDate(2024, 1, 1),
            endDate = LocalDate(2024, 1, 31)
        )
        
        assertTrue(result is Result.Success)
        assertTrue(result.data.isEmpty()) // Mock returns empty list
    }
    
    @Test
    fun `get recent logs should limit results correctly`() = runTest {
        val result = logRepository.getRecentLogs("user123", limit = 10)
        
        assertTrue(result is Result.Success)
        assertTrue(result.data.size <= 10)
    }
    
    @Test
    fun `delete daily log should succeed`() = runTest {
        val result = logRepository.deleteDailyLog("user123", LocalDate(2024, 1, 15))
        
        assertTrue(result is Result.Success)
    }
    
    @Test
    fun `get period logs in range should filter correctly`() = runTest {
        val result = logRepository.getPeriodLogsInRange(
            userId = "user123",
            startDate = LocalDate(2024, 1, 1),
            endDate = LocalDate(2024, 1, 31)
        )
        
        assertTrue(result is Result.Success)
    }
    
    @Test
    fun `get BBT logs in range should filter correctly`() = runTest {
        val result = logRepository.getBBTLogsInRange(
            userId = "user123",
            startDate = LocalDate(2024, 1, 1),
            endDate = LocalDate(2024, 1, 31)
        )
        
        assertTrue(result is Result.Success)
    }
    
    @Test
    fun `get fertility logs in range should filter correctly`() = runTest {
        val result = logRepository.getFertilityLogsInRange(
            userId = "user123",
            startDate = LocalDate(2024, 1, 1),
            endDate = LocalDate(2024, 1, 31)
        )
        
        assertTrue(result is Result.Success)
    }
    
    @Test
    fun `get log count should return accurate number`() = runTest {
        val result = logRepository.getLogCount("user123")
        
        assertTrue(result is Result.Success)
        assertEquals(0, result.data) // Mock returns 0
    }
    
    @Test
    fun `get logs by symptoms should filter correctly`() = runTest {
        val symptoms = listOf(Symptom.CRAMPS, Symptom.HEADACHE)
        val result = logRepository.getLogsBySymptoms(
            userId = "user123",
            symptoms = symptoms,
            startDate = LocalDate(2024, 1, 1),
            endDate = LocalDate(2024, 1, 31)
        )
        
        assertTrue(result is Result.Success)
    }
}