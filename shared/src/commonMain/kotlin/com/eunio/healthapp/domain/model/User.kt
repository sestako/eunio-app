package com.eunio.healthapp.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val email: String,
    val name: String,
    val onboardingComplete: Boolean = false,
    val primaryGoal: HealthGoal,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val createdAt: Instant,
    val updatedAt: Instant,
    @kotlinx.serialization.Transient
    val settings: Map<String, Any>? = null
)

@Serializable
enum class HealthGoal {
    CONCEPTION,
    CONTRACEPTION,
    CYCLE_TRACKING,
    GENERAL_HEALTH
}