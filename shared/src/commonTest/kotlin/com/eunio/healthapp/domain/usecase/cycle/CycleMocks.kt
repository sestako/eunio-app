package com.eunio.healthapp.domain.usecase.cycle

import com.eunio.healthapp.domain.model.Cycle
import com.eunio.healthapp.domain.model.DailyLog
import com.eunio.healthapp.domain.repository.CycleRepository
import com.eunio.healthapp.domain.repository.LogRepository
import com.eunio.healthapp.domain.util.Result
import kotlinx.datetime.LocalDate

/**
 * Mock implementation of CycleRepository for testing cycle use cases.
 */
class MockCycleRepository : CycleRepository {
    val currentCycles = mutableMapOf<String, Cycle>()
    val cycleHistory = mutableMapOf<String, List<Cycle>>()
    var endCycleCalled = false
    var lastEndDate: LocalDate? = null
    
    override suspend fun getCurrentCycle(userId: String): Result<Cycle?> {
        return Result.success(currentCycles[userId])
    }
    
    override suspend fun getCycleHistory(userId: String, limit: Int): Result<List<Cycle>> {
        return Result.success(cycleHistory[userId] ?: emptyList())
    }
    
    override suspend fun startNewCycle(userId: String, startDate: LocalDate): Result<Cycle> {
        val cycle = Cycle(
            id = "cycle-${startDate}",
            userId = userId,
            startDate = startDate
        )
        currentCycles[userId] = cycle
        return Result.success(cycle)
    }
    
    override suspend fun updateCycle(cycle: Cycle): Result<Unit> {
        currentCycles[cycle.userId] = cycle
        return Result.success(Unit)
    }
    
    override suspend fun endCurrentCycle(userId: String, endDate: LocalDate): Result<Unit> {
        endCycleCalled = true
        lastEndDate = endDate
        val currentCycle = currentCycles[userId]
        if (currentCycle != null) {
            currentCycles[userId] = currentCycle.copy(endDate = endDate)
        }
        return Result.success(Unit)
    }
    
    override suspend fun getCyclesInRange(userId: String, startDate: LocalDate, endDate: LocalDate): Result<List<Cycle>> {
        return Result.success(emptyList())
    }
    
    override suspend fun getAverageCycleLength(userId: String, cycleCount: Int): Result<Double?> {
        return Result.success(null)
    }
    
    override suspend fun predictNextPeriod(userId: String): Result<LocalDate?> {
        return Result.success(null)
    }
    
    override suspend fun confirmOvulation(cycleId: String, ovulationDate: LocalDate): Result<Unit> {
        return Result.success(Unit)
    }
}

/**
 * Mock implementation of LogRepository for testing cycle use cases.
 */
class MockLogRepository : LogRepository {
    val dailyLogs = mutableMapOf<Pair<String, LocalDate>, DailyLog>()
    val logsInRange = mutableMapOf<String, List<DailyLog>>()
    val fertilityLogs = mutableMapOf<String, List<DailyLog>>()
    
    override suspend fun saveDailyLog(log: DailyLog): Result<Unit> {
        dailyLogs[log.userId to log.date] = log
        return Result.success(Unit)
    }
    
    override suspend fun getDailyLog(userId: String, date: LocalDate): Result<DailyLog?> {
        return Result.success(dailyLogs[userId to date])
    }
    
    override suspend fun getLogsInRange(userId: String, startDate: LocalDate, endDate: LocalDate): Result<List<DailyLog>> {
        return Result.success(logsInRange[userId] ?: emptyList())
    }
    
    override suspend fun getRecentLogs(userId: String, limit: Int): Result<List<DailyLog>> {
        return Result.success(emptyList())
    }
    
    override suspend fun deleteDailyLog(userId: String, date: LocalDate): Result<Unit> {
        dailyLogs.remove(userId to date)
        return Result.success(Unit)
    }
    
    override suspend fun getPeriodLogsInRange(userId: String, startDate: LocalDate, endDate: LocalDate): Result<List<DailyLog>> {
        return Result.success(emptyList())
    }
    
    override suspend fun getBBTLogsInRange(userId: String, startDate: LocalDate, endDate: LocalDate): Result<List<DailyLog>> {
        return Result.success(emptyList())
    }
    
    override suspend fun getFertilityLogsInRange(userId: String, startDate: LocalDate, endDate: LocalDate): Result<List<DailyLog>> {
        return Result.success(fertilityLogs[userId] ?: emptyList())
    }
    
    override suspend fun getLogCount(userId: String): Result<Int> {
        return Result.success(0)
    }
    
    override suspend fun getLogsBySymptoms(
        userId: String, 
        symptoms: List<com.eunio.healthapp.domain.model.Symptom>,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): Result<List<DailyLog>> {
        return Result.success(emptyList())
    }
}