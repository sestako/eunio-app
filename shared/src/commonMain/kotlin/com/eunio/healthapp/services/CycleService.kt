package com.eunio.healthapp.services

import com.eunio.healthapp.domain.model.Cycle

interface CycleService {
    suspend fun createCycle(cycle: Cycle): Result<Unit>
    suspend fun getCycle(userId: String, cycleId: String): Result<Cycle?>
    suspend fun getAllCycles(userId: String): Result<List<Cycle>>
    suspend fun updateCycle(cycle: Cycle): Result<Unit>
    suspend fun deleteCycle(userId: String, cycleId: String): Result<Unit>
}
