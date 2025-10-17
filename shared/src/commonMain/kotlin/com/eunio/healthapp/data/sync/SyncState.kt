package com.eunio.healthapp.data.sync

/**
 * Represents the current state of synchronization
 */
enum class SyncState {
    IDLE,
    SYNCING,
    COMPLETED,
    ERROR,
    OFFLINE
}