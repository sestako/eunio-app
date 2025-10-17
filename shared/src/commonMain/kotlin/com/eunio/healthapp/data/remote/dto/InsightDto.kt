package com.eunio.healthapp.data.remote.dto

import com.eunio.healthapp.domain.model.Insight
import com.eunio.healthapp.domain.model.InsightType
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Data Transfer Object for Insight entity in Firestore.
 * Handles serialization/deserialization between domain model and Firestore document.
 */
@Serializable
data class InsightDto(
    val generatedDate: Long,
    val insightText: String,
    val type: String,
    val isRead: Boolean = false,
    val relatedLogIds: List<String> = emptyList(),
    val confidence: Double,
    val actionable: Boolean = false
) {
    companion object {
        /**
         * Converts domain Insight model to Firestore DTO
         */
        fun fromDomain(insight: Insight): InsightDto {
            return InsightDto(
                generatedDate = insight.generatedDate.epochSeconds,
                insightText = insight.insightText,
                type = insight.type.name,
                isRead = insight.isRead,
                relatedLogIds = insight.relatedLogIds,
                confidence = insight.confidence,
                actionable = insight.actionable
            )
        }
    }
    
    /**
     * Converts Firestore DTO to domain Insight model
     */
    fun toDomain(id: String, userId: String): Insight {
        return Insight(
            id = id,
            userId = userId,
            generatedDate = Instant.fromEpochSeconds(generatedDate),
            insightText = insightText,
            type = InsightType.valueOf(type),
            isRead = isRead,
            relatedLogIds = relatedLogIds,
            confidence = confidence,
            actionable = actionable
        )
    }
}