package com.eunio.healthapp.data.remote.dto

import com.eunio.healthapp.domain.model.Cycle
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * Data Transfer Object for Cycle entity in Firestore.
 * Handles serialization/deserialization between domain model and Firestore document.
 */
@Serializable
data class CycleDto(
    val startDate: Long, // LocalDate as epoch days
    val endDate: Long? = null,
    val predictedOvulationDate: Long? = null,
    val confirmedOvulationDate: Long? = null,
    val cycleLength: Int? = null,
    val lutealPhaseLength: Int? = null
) {
    companion object {
        /**
         * Converts domain Cycle model to Firestore DTO
         */
        fun fromDomain(cycle: Cycle): CycleDto {
            return CycleDto(
                startDate = cycle.startDate.toEpochDays().toLong(),
                endDate = cycle.endDate?.toEpochDays()?.toLong(),
                predictedOvulationDate = cycle.predictedOvulationDate?.toEpochDays()?.toLong(),
                confirmedOvulationDate = cycle.confirmedOvulationDate?.toEpochDays()?.toLong(),
                cycleLength = cycle.cycleLength,
                lutealPhaseLength = cycle.lutealPhaseLength
            )
        }
    }
    
    /**
     * Converts Firestore DTO to domain Cycle model
     */
    fun toDomain(id: String, userId: String): Cycle {
        return Cycle(
            id = id,
            userId = userId,
            startDate = LocalDate.fromEpochDays(startDate.toInt()),
            endDate = endDate?.let { LocalDate.fromEpochDays(it.toInt()) },
            predictedOvulationDate = predictedOvulationDate?.let { LocalDate.fromEpochDays(it.toInt()) },
            confirmedOvulationDate = confirmedOvulationDate?.let { LocalDate.fromEpochDays(it.toInt()) },
            cycleLength = cycleLength,
            lutealPhaseLength = lutealPhaseLength
        )
    }
}