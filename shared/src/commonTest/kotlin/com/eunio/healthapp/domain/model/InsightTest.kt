package com.eunio.healthapp.domain.model

import com.eunio.healthapp.testutil.TestDataFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InsightTest {
    
    @Test
    fun `insight creation should succeed`() {
        val insight = TestDataFactory.createTestInsight("user123")
        
        assertEquals("user123", insight.userId)
        assertEquals("This is a test insight about cycle patterns", insight.insightText)
        assertEquals(InsightType.PATTERN_RECOGNITION, insight.type)
        assertFalse(insight.isRead)
        assertEquals(0.85, insight.confidence)
    }
    
    @Test
    fun `insight with different types should be valid`() {
        val patternInsight = TestDataFactory.createTestInsight("user123").copy(
            type = InsightType.PATTERN_RECOGNITION
        )
        val warningInsight = TestDataFactory.createTestInsight("user123").copy(
            type = InsightType.EARLY_WARNING
        )
        val predictionInsight = TestDataFactory.createTestInsight("user123").copy(
            type = InsightType.CYCLE_PREDICTION
        )
        val fertilityInsight = TestDataFactory.createTestInsight("user123").copy(
            type = InsightType.FERTILITY_WINDOW
        )
        
        assertEquals(InsightType.PATTERN_RECOGNITION, patternInsight.type)
        assertEquals(InsightType.EARLY_WARNING, warningInsight.type)
        assertEquals(InsightType.CYCLE_PREDICTION, predictionInsight.type)
        assertEquals(InsightType.FERTILITY_WINDOW, fertilityInsight.type)
    }
    
    @Test
    fun `insight confidence levels should be valid`() {
        val lowConfidence = TestDataFactory.createTestInsight("user123").copy(
            confidence = 0.3
        )
        val mediumConfidence = TestDataFactory.createTestInsight("user123").copy(
            confidence = 0.6
        )
        val highConfidence = TestDataFactory.createTestInsight("user123").copy(
            confidence = 0.9
        )
        
        assertEquals(0.3, lowConfidence.confidence)
        assertEquals(0.6, mediumConfidence.confidence)
        assertEquals(0.9, highConfidence.confidence)
    }
    
    @Test
    fun `insight read status should be trackable`() {
        val unreadInsight = TestDataFactory.createTestInsight("user123")
        val readInsight = unreadInsight.copy(isRead = true)
        
        assertFalse(unreadInsight.isRead)
        assertTrue(readInsight.isRead)
    }
    
    @Test
    fun `insight with related logs should track associations`() {
        val relatedLogIds = listOf("log1", "log2", "log3")
        val insight = TestDataFactory.createTestInsight("user123").copy(
            relatedLogIds = relatedLogIds
        )
        
        assertEquals(3, insight.relatedLogIds.size)
        assertTrue(insight.relatedLogIds.containsAll(relatedLogIds))
    }
    
    @Test
    fun `actionable insight should be flagged correctly`() {
        val actionableInsight = TestDataFactory.createTestInsight("user123").copy(
            actionable = true,
            insightText = "Consider consulting your healthcare provider about irregular cycles"
        )
        val informationalInsight = TestDataFactory.createTestInsight("user123").copy(
            actionable = false,
            insightText = "Your cycle length is consistent with your historical average"
        )
        
        assertTrue(actionableInsight.actionable)
        assertFalse(informationalInsight.actionable)
    }
}