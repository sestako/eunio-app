package com.eunio.healthapp.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class DailyLog(
    val id: String,
    val userId: String,
    val date: LocalDate,
    val periodFlow: PeriodFlow? = null,
    val symptoms: List<Symptom> = emptyList(),
    val mood: Mood? = null,
    val sexualActivity: SexualActivity? = null,
    val bbt: Double? = null,
    val cervicalMucus: CervicalMucus? = null,
    val opkResult: OPKResult? = null,
    val notes: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Serializable
enum class PeriodFlow {
    LIGHT, MEDIUM, HEAVY, SPOTTING
}

@Serializable
enum class Symptom {
    CRAMPS, HEADACHE, BLOATING, BREAST_TENDERNESS, ACNE, MOOD_SWINGS, FATIGUE, NAUSEA, BACK_PAIN, FOOD_CRAVINGS
}

@Serializable
enum class Mood {
    HAPPY, SAD, ANXIOUS, IRRITABLE, CALM, ENERGETIC, TIRED, NEUTRAL
}

@Serializable
data class SexualActivity(
    val occurred: Boolean,
    val protection: Protection? = null
)

@Serializable
enum class Protection {
    CONDOM, BIRTH_CONTROL, WITHDRAWAL, NONE
}

@Serializable
enum class CervicalMucus {
    DRY, STICKY, CREAMY, WATERY, EGG_WHITE
}

@Serializable
enum class OPKResult {
    NEGATIVE, POSITIVE, PEAK
}