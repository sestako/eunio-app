package com.eunio.healthapp.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class UserPreferences(
    val userId: String,
    val unitSystem: UnitSystem,
    val isManuallySet: Boolean = false,
    val lastModified: Instant,
    val syncStatus: SyncStatus = SyncStatus.PENDING
)