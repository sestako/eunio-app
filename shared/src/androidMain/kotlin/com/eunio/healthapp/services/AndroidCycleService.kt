package com.eunio.healthapp.services

import com.eunio.healthapp.domain.model.Cycle
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.LocalDate

class AndroidCycleService : CycleService {
    
    private val firestore = FirebaseFirestore.getInstance()
    
    override suspend fun createCycle(cycle: Cycle): Result<Unit> {
        return try {
            val cycleMap = cycleToMap(cycle)
            firestore.collection("cycles")
                .document(cycle.userId)
                .collection("user_cycles")
                .document(cycle.id)
                .set(cycleMap)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getCycle(userId: String, cycleId: String): Result<Cycle?> {
        return try {
            val snapshot = firestore.collection("cycles")
                .document(userId)
                .collection("user_cycles")
                .document(cycleId)
                .get()
                .await()
            
            if (!snapshot.exists()) {
                return Result.success(null)
            }
            
            val cycle = mapToCycle(snapshot.data ?: return Result.success(null))
            Result.success(cycle)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getAllCycles(userId: String): Result<List<Cycle>> {
        return try {
            val snapshot = firestore.collection("cycles")
                .document(userId)
                .collection("user_cycles")
                .orderBy("startDate")
                .get()
                .await()
            
            val cycles = snapshot.documents.mapNotNull { doc ->
                doc.data?.let { mapToCycle(it) }
            }
            Result.success(cycles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateCycle(cycle: Cycle): Result<Unit> {
        return try {
            val cycleMap = cycleToMap(cycle)
            firestore.collection("cycles")
                .document(cycle.userId)
                .collection("user_cycles")
                .document(cycle.id)
                .set(cycleMap)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteCycle(userId: String, cycleId: String): Result<Unit> {
        return try {
            firestore.collection("cycles")
                .document(userId)
                .collection("user_cycles")
                .document(cycleId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun cycleToMap(cycle: Cycle): Map<String, Any?> {
        return mapOf(
            "id" to cycle.id,
            "userId" to cycle.userId,
            "startDate" to cycle.startDate.toString(),
            "endDate" to cycle.endDate?.toString(),
            "predictedOvulationDate" to cycle.predictedOvulationDate?.toString(),
            "confirmedOvulationDate" to cycle.confirmedOvulationDate?.toString(),
            "cycleLength" to cycle.cycleLength,
            "lutealPhaseLength" to cycle.lutealPhaseLength
        )
    }
    
    private fun mapToCycle(data: Map<String, Any>): Cycle {
        return Cycle(
            id = data["id"] as? String ?: "",
            userId = data["userId"] as? String ?: "",
            startDate = LocalDate.parse(data["startDate"] as? String ?: ""),
            endDate = (data["endDate"] as? String)?.let { LocalDate.parse(it) },
            predictedOvulationDate = (data["predictedOvulationDate"] as? String)?.let { LocalDate.parse(it) },
            confirmedOvulationDate = (data["confirmedOvulationDate"] as? String)?.let { LocalDate.parse(it) },
            cycleLength = (data["cycleLength"] as? Number)?.toInt(),
            lutealPhaseLength = (data["lutealPhaseLength"] as? Number)?.toInt()
        )
    }
}
