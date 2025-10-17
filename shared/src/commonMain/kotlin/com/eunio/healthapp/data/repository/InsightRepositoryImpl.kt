package com.eunio.healthapp.data.repository

import com.eunio.healthapp.data.remote.FirestoreService
import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.Insight
import com.eunio.healthapp.domain.model.InsightType
import com.eunio.healthapp.domain.repository.InsightRepository
import com.eunio.healthapp.domain.util.ErrorHandler
import com.eunio.healthapp.domain.util.Result

/**
 * Implementation of InsightRepository that manages health insight data.
 * Handles insight retrieval, user interaction tracking, and insight management.
 */
class InsightRepositoryImpl(
    private val firestoreService: FirestoreService,
    private val errorHandler: ErrorHandler
) : InsightRepository {

    override suspend fun getUnreadInsights(userId: String): Result<List<Insight>> {
        return try {
            firestoreService.getUnreadInsights(userId)
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
    }

    override suspend fun markInsightAsRead(insightId: String): Result<Unit> {
        return try {
            // Get the insight first to validate it exists and get userId
            val insightResult = firestoreService.getInsight("", insightId) // userId not needed for this operation
            if (insightResult.isError) {
                return insightResult.map { }
            }
            
            val insight = insightResult.getOrNull()
                ?: return Result.error(AppError.ValidationError("Insight not found"))
            
            firestoreService.markInsightAsRead(insight.userId, insightId)
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
    }

    override suspend fun getInsightHistory(userId: String, limit: Int): Result<List<Insight>> {
        return try {
            if (limit <= 0) {
                return Result.error(errorHandler.createValidationError("Limit must be positive", "limit"))
            }
            
            firestoreService.getInsightHistory(userId, limit)
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
    }

    override suspend fun saveInsight(insight: Insight): Result<Unit> {
        return try {
            // Validate insight data
            if (insight.insightText.isBlank()) {
                return Result.error(errorHandler.createValidationError("Insight text cannot be empty", "insightText"))
            }
            
            if (insight.confidence < 0.0 || insight.confidence > 1.0) {
                return Result.error(errorHandler.createValidationError("Confidence must be between 0.0 and 1.0", "confidence"))
            }
            
            firestoreService.saveInsight(insight)
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
    }

    override suspend fun getInsightsByType(
        userId: String,
        type: InsightType,
        limit: Int
    ): Result<List<Insight>> {
        return try {
            if (limit <= 0) {
                return Result.error(errorHandler.createValidationError("Limit must be positive", "limit"))
            }
            
            val historyResult = getInsightHistory(userId, limit * 2) // Get more to filter
            if (historyResult.isError) {
                return historyResult
            }
            
            val allInsights = historyResult.getOrThrow()
            val filteredInsights = allInsights
                .filter { it.type == type }
                .take(limit)
            
            Result.success(filteredInsights)
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
    }

    override suspend fun getActionableInsights(userId: String): Result<List<Insight>> {
        return try {
            val historyResult = getInsightHistory(userId, 100) // Get more insights to filter
            if (historyResult.isError) {
                return historyResult
            }
            
            val allInsights = historyResult.getOrThrow()
            val actionableInsights = allInsights
                .filter { it.actionable && !it.isRead }
                .sortedByDescending { it.generatedDate }
            
            Result.success(actionableInsights)
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
    }

    override suspend fun deleteInsight(insightId: String): Result<Unit> {
        return try {
            // Get the insight first to validate it exists and get userId
            val insightResult = firestoreService.getInsight("", insightId) // userId not needed for this operation
            if (insightResult.isError) {
                return insightResult.map { }
            }
            
            val insight = insightResult.getOrNull()
                ?: return Result.error(AppError.ValidationError("Insight not found"))
            
            firestoreService.deleteInsight(insight.userId, insightId)
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
    }

    override suspend fun getInsightsForLogs(userId: String, logIds: List<String>): Result<List<Insight>> {
        return try {
            if (logIds.isEmpty()) {
                return Result.success(emptyList())
            }
            
            val historyResult = getInsightHistory(userId, 200) // Get more insights to search
            if (historyResult.isError) {
                return historyResult
            }
            
            val allInsights = historyResult.getOrThrow()
            val relatedInsights = allInsights.filter { insight ->
                insight.relatedLogIds.any { logId -> logIds.contains(logId) }
            }
            
            Result.success(relatedInsights.sortedByDescending { it.generatedDate })
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
    }

    override suspend fun getInsightCount(userId: String, includeRead: Boolean): Result<Int> {
        return try {
            val historyResult = getInsightHistory(userId, 1000) // Get all insights
            if (historyResult.isError) {
                return historyResult.map { 0 }
            }
            
            val allInsights = historyResult.getOrThrow()
            val count = if (includeRead) {
                allInsights.size
            } else {
                allInsights.count { !it.isRead }
            }
            
            Result.success(count)
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
    }

    override suspend fun getHighConfidenceInsights(
        userId: String,
        minConfidence: Double,
        limit: Int
    ): Result<List<Insight>> {
        return try {
            if (minConfidence < 0.0 || minConfidence > 1.0) {
                return Result.error(errorHandler.createValidationError("Confidence must be between 0.0 and 1.0", "minConfidence"))
            }
            
            if (limit <= 0) {
                return Result.error(errorHandler.createValidationError("Limit must be positive", "limit"))
            }
            
            val historyResult = getInsightHistory(userId, limit * 3) // Get more to filter
            if (historyResult.isError) {
                return historyResult
            }
            
            val allInsights = historyResult.getOrThrow()
            val highConfidenceInsights = allInsights
                .filter { it.confidence >= minConfidence }
                .sortedByDescending { it.confidence }
                .take(limit)
            
            Result.success(highConfidenceInsights)
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
    }

    override suspend fun markAllInsightsAsRead(userId: String): Result<Unit> {
        return try {
            val unreadResult = getUnreadInsights(userId)
            if (unreadResult.isError) {
                return unreadResult.map { }
            }
            
            val unreadInsights = unreadResult.getOrThrow()
            
            // Mark each insight as read
            for (insight in unreadInsights) {
                val markResult = markInsightAsRead(insight.id)
                if (markResult.isError) {
                    // Continue with other insights even if one fails
                    continue
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
    }

    /**
     * Gets insights that match specific criteria for advanced filtering
     */
    suspend fun getInsightsByCriteria(
        userId: String,
        types: List<InsightType>? = null,
        minConfidence: Double? = null,
        actionableOnly: Boolean = false,
        unreadOnly: Boolean = false,
        limit: Int = 50
    ): Result<List<Insight>> {
        return try {
            val historyResult = getInsightHistory(userId, limit * 2)
            if (historyResult.isError) {
                return historyResult
            }
            
            var insights = historyResult.getOrThrow()
            
            // Apply filters
            if (types != null && types.isNotEmpty()) {
                insights = insights.filter { types.contains(it.type) }
            }
            
            if (minConfidence != null) {
                insights = insights.filter { it.confidence >= minConfidence }
            }
            
            if (actionableOnly) {
                insights = insights.filter { it.actionable }
            }
            
            if (unreadOnly) {
                insights = insights.filter { !it.isRead }
            }
            
            val result = insights
                .sortedByDescending { it.generatedDate }
                .take(limit)
            
            Result.success(result)
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
    }
}