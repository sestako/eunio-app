package com.eunio.healthapp.data.repository

import com.eunio.healthapp.data.remote.FirestoreService
import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.Insight
import com.eunio.healthapp.domain.model.InsightType
import com.eunio.healthapp.domain.util.ErrorHandler
import com.eunio.healthapp.domain.util.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class InsightRepositoryImplTest {
    
    private val firestoreService = mockk<FirestoreService>()
    private val errorHandler = mockk<ErrorHandler>()
    
    private val repository = InsightRepositoryImpl(
        firestoreService = firestoreService,
        errorHandler = errorHandler
    )
    
    private val testUserId = "test-user-id"
    private val testInsight = Insight(
        id = "test-insight-id",
        userId = testUserId,
        generatedDate = Clock.System.now(),
        insightText = "Your cycle appears to be regular with an average length of 28 days.",
        type = InsightType.PATTERN_RECOGNITION,
        isRead = false,
        confidence = 0.85,
        actionable = false
    )
    
    @Test
    fun `getUnreadInsights returns unread insights from firestore`() = runTest {
        // Given
        val insights = listOf(testInsight)
        coEvery { firestoreService.getUnreadInsights(testUserId) } returns Result.success(insights)
        
        // When
        val result = repository.getUnreadInsights(testUserId)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(insights, result.getOrNull())
        coVerify { firestoreService.getUnreadInsights(testUserId) }
    }
    
    @Test
    fun `markInsightAsRead marks insight as read in firestore`() = runTest {
        // Given
        coEvery { firestoreService.getInsight("", testInsight.id) } returns Result.success(testInsight)
        coEvery { firestoreService.markInsightAsRead(testUserId, testInsight.id) } returns Result.success(Unit)
        
        // When
        val result = repository.markInsightAsRead(testInsight.id)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { firestoreService.markInsightAsRead(testUserId, testInsight.id) }
    }
    
    @Test
    fun `markInsightAsRead fails when insight not found`() = runTest {
        // Given
        coEvery { firestoreService.getInsight("", testInsight.id) } returns Result.success(null)
        
        // When
        val result = repository.markInsightAsRead(testInsight.id)
        
        // Then
        assertTrue(result.isError)
        assertIs<AppError.ValidationError>(result.errorOrNull())
    }
    
    @Test
    fun `getInsightHistory returns insights with valid limit`() = runTest {
        // Given
        val insights = listOf(testInsight)
        coEvery { firestoreService.getInsightHistory(testUserId, 50) } returns Result.success(insights)
        
        // When
        val result = repository.getInsightHistory(testUserId, 50)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(insights, result.getOrNull())
        coVerify { firestoreService.getInsightHistory(testUserId, 50) }
    }
    
    @Test
    fun `getInsightHistory fails with invalid limit`() = runTest {
        // Given
        every { errorHandler.createValidationError("Limit must be positive", "limit") } returns 
            AppError.ValidationError("Limit must be positive", "limit")
        
        // When
        val result = repository.getInsightHistory(testUserId, 0)
        
        // Then
        assertTrue(result.isError)
        assertIs<AppError.ValidationError>(result.errorOrNull())
    }
    
    @Test
    fun `saveInsight validates and saves insight to firestore`() = runTest {
        // Given
        coEvery { firestoreService.saveInsight(testInsight) } returns Result.success(Unit)
        
        // When
        val result = repository.saveInsight(testInsight)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { firestoreService.saveInsight(testInsight) }
    }
    
    @Test
    fun `saveInsight fails when insight text is empty`() = runTest {
        // Given
        val invalidInsight = testInsight.copy(insightText = "")
        every { errorHandler.createValidationError("Insight text cannot be empty", "insightText") } returns 
            AppError.ValidationError("Insight text cannot be empty", "insightText")
        
        // When
        val result = repository.saveInsight(invalidInsight)
        
        // Then
        assertTrue(result.isError)
        assertIs<AppError.ValidationError>(result.errorOrNull())
    }
    
    @Test
    fun `saveInsight fails when confidence is out of range`() = runTest {
        // Given
        val invalidInsight = testInsight.copy(confidence = 1.5)
        every { errorHandler.createValidationError("Confidence must be between 0.0 and 1.0", "confidence") } returns 
            AppError.ValidationError("Confidence must be between 0.0 and 1.0", "confidence")
        
        // When
        val result = repository.saveInsight(invalidInsight)
        
        // Then
        assertTrue(result.isError)
        assertIs<AppError.ValidationError>(result.errorOrNull())
    }
    
    @Test
    fun `getInsightsByType filters insights by type`() = runTest {
        // Given
        val patternInsight = testInsight.copy(type = InsightType.PATTERN_RECOGNITION)
        val warningInsight = testInsight.copy(id = "insight2", type = InsightType.EARLY_WARNING)
        val allInsights = listOf(patternInsight, warningInsight)
        
        coEvery { firestoreService.getInsightHistory(testUserId, 40) } returns Result.success(allInsights)
        
        // When
        val result = repository.getInsightsByType(testUserId, InsightType.PATTERN_RECOGNITION, 20)
        
        // Then
        assertTrue(result.isSuccess)
        val filteredInsights = result.getOrNull()!!
        assertEquals(1, filteredInsights.size)
        assertEquals(patternInsight, filteredInsights.first())
    }
    
    @Test
    fun `getActionableInsights returns only actionable unread insights`() = runTest {
        // Given
        val actionableInsight = testInsight.copy(actionable = true, isRead = false)
        val readInsight = testInsight.copy(id = "insight2", actionable = true, isRead = true)
        val nonActionableInsight = testInsight.copy(id = "insight3", actionable = false, isRead = false)
        val allInsights = listOf(actionableInsight, readInsight, nonActionableInsight)
        
        coEvery { firestoreService.getInsightHistory(testUserId, 100) } returns Result.success(allInsights)
        
        // When
        val result = repository.getActionableInsights(testUserId)
        
        // Then
        assertTrue(result.isSuccess)
        val actionableInsights = result.getOrNull()!!
        assertEquals(1, actionableInsights.size)
        assertEquals(actionableInsight, actionableInsights.first())
    }
    
    @Test
    fun `deleteInsight removes insight from firestore`() = runTest {
        // Given
        coEvery { firestoreService.getInsight("", testInsight.id) } returns Result.success(testInsight)
        coEvery { firestoreService.deleteInsight(testUserId, testInsight.id) } returns Result.success(Unit)
        
        // When
        val result = repository.deleteInsight(testInsight.id)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { firestoreService.deleteInsight(testUserId, testInsight.id) }
    }
    
    @Test
    fun `deleteInsight fails when insight not found`() = runTest {
        // Given
        coEvery { firestoreService.getInsight("", testInsight.id) } returns Result.success(null)
        
        // When
        val result = repository.deleteInsight(testInsight.id)
        
        // Then
        assertTrue(result.isError)
        assertIs<AppError.ValidationError>(result.errorOrNull())
    }
    
    @Test
    fun `getInsightsForLogs returns insights related to specific logs`() = runTest {
        // Given
        val logIds = listOf("log1", "log2")
        val relatedInsight = testInsight.copy(relatedLogIds = listOf("log1"))
        val unrelatedInsight = testInsight.copy(id = "insight2", relatedLogIds = listOf("log3"))
        val allInsights = listOf(relatedInsight, unrelatedInsight)
        
        coEvery { firestoreService.getInsightHistory(testUserId, 200) } returns Result.success(allInsights)
        
        // When
        val result = repository.getInsightsForLogs(testUserId, logIds)
        
        // Then
        assertTrue(result.isSuccess)
        val relatedInsights = result.getOrNull()!!
        assertEquals(1, relatedInsights.size)
        assertEquals(relatedInsight, relatedInsights.first())
    }
    
    @Test
    fun `getInsightsForLogs returns empty list when no log IDs provided`() = runTest {
        // When
        val result = repository.getInsightsForLogs(testUserId, emptyList())
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.isEmpty())
    }
    
    @Test
    fun `getInsightCount returns total count including read insights`() = runTest {
        // Given
        val readInsight = testInsight.copy(isRead = true)
        val unreadInsight = testInsight.copy(id = "insight2", isRead = false)
        val allInsights = listOf(readInsight, unreadInsight)
        
        coEvery { firestoreService.getInsightHistory(testUserId, 1000) } returns Result.success(allInsights)
        
        // When
        val result = repository.getInsightCount(testUserId, includeRead = true)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull())
    }
    
    @Test
    fun `getInsightCount returns unread count only when includeRead is false`() = runTest {
        // Given
        val readInsight = testInsight.copy(isRead = true)
        val unreadInsight = testInsight.copy(id = "insight2", isRead = false)
        val allInsights = listOf(readInsight, unreadInsight)
        
        coEvery { firestoreService.getInsightHistory(testUserId, 1000) } returns Result.success(allInsights)
        
        // When
        val result = repository.getInsightCount(testUserId, includeRead = false)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())
    }
    
    @Test
    fun `getHighConfidenceInsights filters by confidence threshold`() = runTest {
        // Given
        val highConfidenceInsight = testInsight.copy(confidence = 0.9)
        val lowConfidenceInsight = testInsight.copy(id = "insight2", confidence = 0.5)
        val allInsights = listOf(highConfidenceInsight, lowConfidenceInsight)
        
        coEvery { firestoreService.getInsightHistory(testUserId, 30) } returns Result.success(allInsights)
        
        // When
        val result = repository.getHighConfidenceInsights(testUserId, 0.7, 10)
        
        // Then
        assertTrue(result.isSuccess)
        val highConfidenceInsights = result.getOrNull()!!
        assertEquals(1, highConfidenceInsights.size)
        assertEquals(highConfidenceInsight, highConfidenceInsights.first())
    }
    
    @Test
    fun `getHighConfidenceInsights fails with invalid confidence range`() = runTest {
        // Given
        every { errorHandler.createValidationError("Confidence must be between 0.0 and 1.0", "minConfidence") } returns 
            AppError.ValidationError("Confidence must be between 0.0 and 1.0", "minConfidence")
        
        // When
        val result = repository.getHighConfidenceInsights(testUserId, 1.5, 10)
        
        // Then
        assertTrue(result.isError)
        assertIs<AppError.ValidationError>(result.errorOrNull())
    }
    
    @Test
    fun `markAllInsightsAsRead marks all unread insights as read`() = runTest {
        // Given
        val unreadInsight1 = testInsight.copy(isRead = false)
        val unreadInsight2 = testInsight.copy(id = "insight2", isRead = false)
        val unreadInsights = listOf(unreadInsight1, unreadInsight2)
        
        coEvery { firestoreService.getUnreadInsights(testUserId) } returns Result.success(unreadInsights)
        coEvery { firestoreService.getInsight("", unreadInsight1.id) } returns Result.success(unreadInsight1)
        coEvery { firestoreService.getInsight("", unreadInsight2.id) } returns Result.success(unreadInsight2)
        coEvery { firestoreService.markInsightAsRead(testUserId, unreadInsight1.id) } returns Result.success(Unit)
        coEvery { firestoreService.markInsightAsRead(testUserId, unreadInsight2.id) } returns Result.success(Unit)
        
        // When
        val result = repository.markAllInsightsAsRead(testUserId)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { firestoreService.markInsightAsRead(testUserId, unreadInsight1.id) }
        coVerify { firestoreService.markInsightAsRead(testUserId, unreadInsight2.id) }
    }
    
    @Test
    fun `getInsightsByCriteria filters insights by multiple criteria`() = runTest {
        // Given
        val matchingInsight = testInsight.copy(
            type = InsightType.PATTERN_RECOGNITION,
            confidence = 0.9,
            actionable = true,
            isRead = false
        )
        val nonMatchingInsight = testInsight.copy(
            id = "insight2",
            type = InsightType.EARLY_WARNING,
            confidence = 0.5,
            actionable = false,
            isRead = true
        )
        val allInsights = listOf(matchingInsight, nonMatchingInsight)
        
        coEvery { firestoreService.getInsightHistory(testUserId, 100) } returns Result.success(allInsights)
        
        // When
        val result = repository.getInsightsByCriteria(
            userId = testUserId,
            types = listOf(InsightType.PATTERN_RECOGNITION),
            minConfidence = 0.8,
            actionableOnly = true,
            unreadOnly = true,
            limit = 50
        )
        
        // Then
        assertTrue(result.isSuccess)
        val filteredInsights = result.getOrNull()!!
        assertEquals(1, filteredInsights.size)
        assertEquals(matchingInsight, filteredInsights.first())
    }
}