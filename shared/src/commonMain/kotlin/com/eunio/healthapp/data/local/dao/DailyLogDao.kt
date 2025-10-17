package com.eunio.healthapp.data.local.dao

import com.eunio.healthapp.database.EunioDatabase
import com.eunio.healthapp.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DailyLogDao(private val database: EunioDatabase) {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    suspend fun insertDailyLog(dailyLog: DailyLog): Unit = withContext(Dispatchers.IO) {
        database.dailyLogQueries.insert(
            id = dailyLog.id,
            userId = dailyLog.userId,
            date = dailyLog.date.toEpochDays().toLong(),
            periodFlow = dailyLog.periodFlow?.name,
            symptoms = json.encodeToString(dailyLog.symptoms.map { it.name }),
            mood = dailyLog.mood?.name,
            sexualActivity = dailyLog.sexualActivity?.let { json.encodeToString(it) },
            bbt = dailyLog.bbt,
            cervicalMucus = dailyLog.cervicalMucus?.name,
            opkResult = dailyLog.opkResult?.name,
            notes = dailyLog.notes,
            createdAt = dailyLog.createdAt.toEpochMilliseconds(),
            updatedAt = dailyLog.updatedAt.toEpochMilliseconds(),
            syncStatus = "PENDING",
            isSynced = 0, // false
            pendingSync = 1, // true
            lastSyncAttempt = null,
            syncRetryCount = 0
        )
    }
    
    suspend fun updateDailyLog(dailyLog: DailyLog): Unit = withContext(Dispatchers.IO) {
        database.dailyLogQueries.update(
            periodFlow = dailyLog.periodFlow?.name,
            symptoms = json.encodeToString(dailyLog.symptoms.map { it.name }),
            mood = dailyLog.mood?.name,
            sexualActivity = dailyLog.sexualActivity?.let { json.encodeToString(it) },
            bbt = dailyLog.bbt,
            cervicalMucus = dailyLog.cervicalMucus?.name,
            opkResult = dailyLog.opkResult?.name,
            notes = dailyLog.notes,
            updatedAt = dailyLog.updatedAt.toEpochMilliseconds(),
            syncStatus = "PENDING",
            isSynced = 0, // false - mark as not synced when updating
            pendingSync = 1, // true - mark as pending sync when updating
            lastSyncAttempt = null,
            syncRetryCount = 0,
            id = dailyLog.id
        )
    }
    
    suspend fun getDailyLogsByUserId(userId: String): List<DailyLog> = withContext(Dispatchers.IO) {
        database.dailyLogQueries.selectByUserId(userId).executeAsList().map { entity ->
            mapEntityToDailyLog(entity)
        }
    }
    
    suspend fun getDailyLogByUserIdAndDate(userId: String, date: LocalDate): DailyLog? = withContext(Dispatchers.IO) {
        database.dailyLogQueries.selectByUserIdAndDate(
            userId = userId,
            date = date.toEpochDays().toLong()
        ).executeAsOneOrNull()?.let { entity ->
            mapEntityToDailyLog(entity)
        }
    }
    
    suspend fun getDailyLogsByDateRange(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DailyLog> = withContext(Dispatchers.IO) {
        database.dailyLogQueries.selectByUserIdAndDateRange(
            userId = userId,
            startDate.toEpochDays().toLong(),
            endDate.toEpochDays().toLong()
        ).executeAsList().map { entity ->
            mapEntityToDailyLog(entity)
        }
    }
    
    suspend fun getPendingSyncLogs(): List<DailyLog> = withContext(Dispatchers.IO) {
        database.dailyLogQueries.selectPendingSync().executeAsList().map { entity ->
            mapEntityToDailyLog(entity)
        }
    }
    
    suspend fun updateSyncStatus(logId: String, syncStatus: String): Unit = withContext(Dispatchers.IO) {
        database.dailyLogQueries.updateSyncStatus(syncStatus, logId)
    }
    
    suspend fun deleteDailyLog(id: String): Unit = withContext(Dispatchers.IO) {
        database.dailyLogQueries.deleteById(id)
    }
    
    suspend fun getDailyLogById(id: String): DailyLog? = withContext(Dispatchers.IO) {
        database.dailyLogQueries.selectById(id).executeAsOneOrNull()?.let { entity ->
            mapEntityToDailyLog(entity)
        }
    }
    
    suspend fun insertOrUpdate(dailyLog: DailyLog): Unit = withContext(Dispatchers.IO) {
        val existingLog = getDailyLogById(dailyLog.id)
        if (existingLog != null) {
            updateDailyLog(dailyLog)
        } else {
            insertDailyLog(dailyLog)
        }
    }
    
    suspend fun markAsSynced(logId: String): Unit = withContext(Dispatchers.IO) {
        database.dailyLogQueries.markAsSynced(logId)
    }
    
    suspend fun markAsPendingSync(logId: String): Unit = withContext(Dispatchers.IO) {
        database.dailyLogQueries.markAsPendingSync(logId)
    }
    
    suspend fun incrementSyncRetryCount(logId: String): Unit = withContext(Dispatchers.IO) {
        val currentTimeSeconds = kotlinx.datetime.Clock.System.now().epochSeconds
        database.dailyLogQueries.incrementSyncRetryCount(
            lastSyncAttempt = currentTimeSeconds,
            id = logId
        )
    }
    
    suspend fun resetSyncRetryCount(logId: String): Unit = withContext(Dispatchers.IO) {
        database.dailyLogQueries.resetSyncRetryCount(logId)
    }
    
    suspend fun getPendingSync(): List<DailyLog> = withContext(Dispatchers.IO) {
        getPendingSyncLogs()
    }
    
    private fun mapEntityToDailyLog(entity: com.eunio.healthapp.database.DailyLog): DailyLog {
        return DailyLog(
            id = entity.id,
            userId = entity.userId,
            date = LocalDate.fromEpochDays(entity.date.toInt()),
            periodFlow = entity.periodFlow?.let { PeriodFlow.valueOf(it) },
            symptoms = entity.symptoms?.let { symptomsJson ->
                json.decodeFromString<List<String>>(symptomsJson).map { Symptom.valueOf(it) }
            } ?: emptyList(),
            mood = entity.mood?.let { Mood.valueOf(it) },
            sexualActivity = entity.sexualActivity?.let { json.decodeFromString<SexualActivity>(it) },
            bbt = entity.bbt,
            cervicalMucus = entity.cervicalMucus?.let { CervicalMucus.valueOf(it) },
            opkResult = entity.opkResult?.let { OPKResult.valueOf(it) },
            notes = entity.notes,
            createdAt = Instant.fromEpochMilliseconds(entity.createdAt),
            updatedAt = Instant.fromEpochMilliseconds(entity.updatedAt)
        )
    }
}