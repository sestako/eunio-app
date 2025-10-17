package com.eunio.healthapp.domain.model

import com.eunio.healthapp.domain.util.NetworkType
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration

/**
 * Statistics about sync operations for monitoring and debugging
 */
@Serializable
data class SyncStatistics(
    val lastSyncTime: Instant? = null,
    val totalSyncs: Int = 0,
    val successfulSyncs: Int = 0,
    val failedSyncs: Int = 0,
    val pendingItems: Int = 0,
    val syncedItems: Int = 0,
    var usersSynced: Int = 0,
    var usersConflicted: Int = 0,
    var logsSynced: Int = 0,
    var logsConflicted: Int = 0,
    var cyclesSynced: Int = 0,
    var cyclesConflicted: Int = 0,
    var insightsSynced: Int = 0,
    var syncDuration: Duration? = null,
    val isOnline: Boolean = true,
    val networkType: String? = null,
    val lastError: String? = null,
    val retryCount: Int = 0,
    // Legacy fields for backward compatibility
    val pendingSyncCount: Int = pendingItems,
    val isConnected: Boolean = isOnline,
    val lastSyncAttempt: Instant = lastSyncTime ?: kotlinx.datetime.Clock.System.now()
)