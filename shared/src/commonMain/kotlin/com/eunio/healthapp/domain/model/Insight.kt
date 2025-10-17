package com.eunio.healthapp.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Insight(
    val id: String,
    val userId: String,
    val generatedDate: Instant,
    val insightText: String,
    val type: InsightType,
    val isRead: Boolean = false,
    val relatedLogIds: List<String> = emptyList(),
    val confidence: Double,
    val actionable: Boolean = false
)

@Serializable
enum class InsightType {
    PATTERN_RECOGNITION,
    EARLY_WARNING,
    CYCLE_PREDICTION,
    FERTILITY_WINDOW
}