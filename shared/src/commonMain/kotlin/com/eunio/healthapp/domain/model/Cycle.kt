package com.eunio.healthapp.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class Cycle(
    val id: String,
    val userId: String,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val predictedOvulationDate: LocalDate? = null,
    val confirmedOvulationDate: LocalDate? = null,
    val cycleLength: Int? = null,
    val lutealPhaseLength: Int? = null
)