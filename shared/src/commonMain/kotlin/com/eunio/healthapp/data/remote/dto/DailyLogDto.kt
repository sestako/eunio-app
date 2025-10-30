package com.eunio.healthapp.data.remote.dto

import com.eunio.healthapp.domain.model.*
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * Data Transfer Object for DailyLog entity in Firestore.
 * Handles serialization/deserialization between domain model and Firestore document.
 * 
 * Schema version 1:
 * - logId: Document ID in format yyyy-MM-dd (UTC)
 * - dateEpochDays: Date stored as UTC epoch days (Long)
 * - createdAt/updatedAt: Timestamps stored as epoch seconds (Long)
 * - v: Schema version field for future migrations
 * 
 * Note: All parameters have default values to support Firebase Android's no-arg constructor requirement.
 */
@Serializable
data class DailyLogDto(
    val logId: String = "",               // yyyy-MM-dd in UTC
    val dateEpochDays: Long = 0L,         // UTC epoch days
    val createdAt: Long = 0L,             // Epoch seconds
    val updatedAt: Long = 0L,             // Epoch seconds
    val periodFlow: String? = null,       // Enum name or null
    val symptoms: List<String>? = null,   // List of enum names or null
    val mood: String? = null,             // Enum name or null
    val sexualActivity: SexualActivityDto? = null,
    val bbt: Double? = null,              // Temperature or null
    val cervicalMucus: String? = null,    // Enum name or null
    val opkResult: String? = null,        // Enum name or null
    val notes: String? = null,            // Free text or null
    val v: Int = 1                        // Schema version
) {
    companion object {
        /**
         * Converts domain DailyLog model to Firestore DTO
         */
        fun fromDomain(dailyLog: DailyLog): DailyLogDto {
            return DailyLogDto(
                logId = dailyLog.id,
                dateEpochDays = dailyLog.date.toEpochDays().toLong(),
                createdAt = dailyLog.createdAt.epochSeconds,
                updatedAt = dailyLog.updatedAt.epochSeconds,
                periodFlow = dailyLog.periodFlow?.name,
                symptoms = dailyLog.symptoms.takeIf { it.isNotEmpty() }?.map { it.name },
                mood = dailyLog.mood?.name,
                sexualActivity = dailyLog.sexualActivity?.let { SexualActivityDto.fromDomain(it) },
                bbt = dailyLog.bbt,
                cervicalMucus = dailyLog.cervicalMucus?.name,
                opkResult = dailyLog.opkResult?.name,
                notes = dailyLog.notes,
                v = 1
            )
        }
    }
    
    /**
     * Converts Firestore DTO to domain DailyLog model
     * @param id Optional document ID override (for backward compatibility with legacy data)
     * @param userId The user ID
     */
    fun toDomain(id: String = logId, userId: String): DailyLog {
        return DailyLog(
            id = id,
            userId = userId,
            date = LocalDate.fromEpochDays(dateEpochDays.toInt()),
            periodFlow = periodFlow?.let { PeriodFlow.valueOf(it) },
            symptoms = symptoms?.map { Symptom.valueOf(it) } ?: emptyList(),
            mood = mood?.let { Mood.valueOf(it) },
            sexualActivity = sexualActivity?.toDomain(),
            bbt = bbt,
            cervicalMucus = cervicalMucus?.let { CervicalMucus.valueOf(it) },
            opkResult = opkResult?.let { OPKResult.valueOf(it) },
            notes = notes,
            createdAt = Instant.fromEpochSeconds(createdAt),
            updatedAt = Instant.fromEpochSeconds(updatedAt)
        )
    }
}

@Serializable
data class SexualActivityDto(
    val occurred: Boolean = false,
    val protection: String? = null
) {
    companion object {
        fun fromDomain(sexualActivity: SexualActivity): SexualActivityDto {
            return SexualActivityDto(
                occurred = sexualActivity.occurred,
                protection = sexualActivity.protection?.name
            )
        }
    }
    
    fun toDomain(): SexualActivity {
        return SexualActivity(
            occurred = occurred,
            protection = protection?.let { Protection.valueOf(it) }
        )
    }
}