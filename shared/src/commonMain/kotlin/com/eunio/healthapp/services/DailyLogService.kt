package com.eunio.healthapp.services

import com.eunio.healthapp.domain.model.DailyLog
import kotlinx.datetime.LocalDate

interface DailyLogService {
    suspend fun createLog(log: DailyLog): Result<Unit>
    suspend fun getLog(userId: String, logId: String): Result<DailyLog?>
    suspend fun getLogsByDateRange(userId: String, startDate: LocalDate, endDate: LocalDate): Result<List<DailyLog>>
    suspend fun updateLog(log: DailyLog): Result<Unit>
    suspend fun deleteLog(userId: String, logId: String): Result<Unit>
}
