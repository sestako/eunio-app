package com.eunio.healthapp.domain.usecase.logging

import com.eunio.healthapp.domain.model.DailyLog
import com.eunio.healthapp.domain.model.Symptom
import com.eunio.healthapp.domain.repository.LogRepository
import com.eunio.healthapp.domain.util.Result
import kotlinx.datetime.LocalDate

class MockLogRepository : LogRepository {
    // SaveDailyLog related properties
    var saveDailyLogResult: Result<Unit> = Result.success(Unit)
    var saveDailyLogCallCount = 0
    var lastSavedLog: DailyLog? = null
    
    // GetDailyLog related properties
    var getDailyLogResult: Result<DailyLog?> = Result.success(null)
    var getDailyLogCallCount = 0
    var lastUserId: String? = null
    var lastDate: LocalDate? = null
    var getDailyLogCallback: ((String, LocalDate) -> Result<DailyLog?>)? = null
    
    // GetLogsInRange related properties
    var getLogsInRangeResult: Result<List<DailyLog>> = Result.success(emptyList())
    var getLogsInRangeCallCount = 0
    var lastStartDate: LocalDate? = null
    var lastEndDate: LocalDate? = null
    
    // GetRecentLogs related properties
    var getRecentLogsResult: Result<List<DailyLog>> = Result.success(emptyList())
    var getRecentLogsCallCount = 0
    var lastLimit: Int? = null
    
    // Period logs related properties
    var getPeriodLogsInRangeResult: Result<List<DailyLog>> = Result.success(emptyList())
    var getPeriodLogsInRangeCallCount = 0
    
    // BBT logs related properties
    var getBBTLogsInRangeResult: Result<List<DailyLog>> = Result.success(emptyList())
    var getBBTLogsInRangeCallCount = 0
    
    // Fertility logs related properties
    var getFertilityLogsInRangeResult: Result<List<DailyLog>> = Result.success(emptyList())
    var getFertilityLogsInRangeCallCount = 0
    
    // Symptoms logs related properties
    var getLogsBySymptomsResult: Result<List<DailyLog>> = Result.success(emptyList())
    var getLogsBySymptomsCallCount = 0
    var lastSymptoms: List<Symptom>? = null
    
    // Log count related properties
    var getLogCountResult: Result<Int> = Result.success(0)
    var getLogCountCallCount = 0
    
    override suspend fun saveDailyLog(log: DailyLog): Result<Unit> {
        saveDailyLogCallCount++
        lastSavedLog = log
        return saveDailyLogResult
    }
    
    override suspend fun getDailyLog(userId: String, date: LocalDate): Result<DailyLog?> {
        getDailyLogCallCount++
        lastUserId = userId
        lastDate = date
        return getDailyLogCallback?.invoke(userId, date) ?: getDailyLogResult
    }
    
    override suspend fun getLogsInRange(userId: String, startDate: LocalDate, endDate: LocalDate): Result<List<DailyLog>> {
        getLogsInRangeCallCount++
        lastUserId = userId
        lastStartDate = startDate
        lastEndDate = endDate
        return getLogsInRangeResult
    }
    
    override suspend fun getRecentLogs(userId: String, limit: Int): Result<List<DailyLog>> {
        getRecentLogsCallCount++
        lastLimit = limit
        return getRecentLogsResult
    }
    
    override suspend fun deleteDailyLog(userId: String, date: LocalDate): Result<Unit> {
        TODO("Not implemented for this test")
    }
    
    override suspend fun getPeriodLogsInRange(userId: String, startDate: LocalDate, endDate: LocalDate): Result<List<DailyLog>> {
        getPeriodLogsInRangeCallCount++
        return getPeriodLogsInRangeResult
    }
    
    override suspend fun getBBTLogsInRange(userId: String, startDate: LocalDate, endDate: LocalDate): Result<List<DailyLog>> {
        getBBTLogsInRangeCallCount++
        return getBBTLogsInRangeResult
    }
    
    override suspend fun getFertilityLogsInRange(userId: String, startDate: LocalDate, endDate: LocalDate): Result<List<DailyLog>> {
        getFertilityLogsInRangeCallCount++
        return getFertilityLogsInRangeResult
    }
    
    override suspend fun getLogCount(userId: String): Result<Int> {
        getLogCountCallCount++
        return getLogCountResult
    }
    
    override suspend fun getLogsBySymptoms(userId: String, symptoms: List<Symptom>, startDate: LocalDate?, endDate: LocalDate?): Result<List<DailyLog>> {
        getLogsBySymptomsCallCount++
        lastSymptoms = symptoms
        return getLogsBySymptomsResult
    }
}