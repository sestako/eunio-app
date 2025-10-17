package com.eunio.healthapp.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class SyncStatus {
    SYNCED,
    PENDING,
    FAILED
}