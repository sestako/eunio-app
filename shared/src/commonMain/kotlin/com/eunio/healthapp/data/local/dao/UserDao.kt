package com.eunio.healthapp.data.local.dao

import com.eunio.healthapp.database.EunioDatabase
import com.eunio.healthapp.domain.model.HealthGoal
import com.eunio.healthapp.domain.model.UnitSystem
import com.eunio.healthapp.domain.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant

class UserDao(private val database: EunioDatabase) {
    
    suspend fun insertUser(user: User): Unit = withContext(Dispatchers.IO) {
        database.userQueries.insert(
            id = user.id,
            email = user.email,
            name = user.name,
            onboardingComplete = if (user.onboardingComplete) 1L else 0L,
            primaryGoal = user.primaryGoal.name,
            unitSystem = user.unitSystem.name,
            createdAt = user.createdAt.toEpochMilliseconds(),
            updatedAt = user.updatedAt.toEpochMilliseconds(),
            syncStatus = "PENDING"
        )
    }
    
    suspend fun updateUser(user: User): Unit = withContext(Dispatchers.IO) {
        database.userQueries.update(
            email = user.email,
            name = user.name,
            onboardingComplete = if (user.onboardingComplete) 1L else 0L,
            primaryGoal = user.primaryGoal.name,
            unitSystem = user.unitSystem.name,
            updatedAt = user.updatedAt.toEpochMilliseconds(),
            syncStatus = "PENDING",
            id = user.id
        )
    }
    
    suspend fun getUserById(id: String): User? = withContext(Dispatchers.IO) {
        database.userQueries.selectById(id).executeAsOneOrNull()?.let { userEntity ->
            User(
                id = userEntity.id,
                email = userEntity.email,
                name = userEntity.name,
                onboardingComplete = userEntity.onboardingComplete == 1L,
                primaryGoal = HealthGoal.valueOf(userEntity.primaryGoal),
                unitSystem = UnitSystem.valueOf(userEntity.unitSystem),
                createdAt = Instant.fromEpochMilliseconds(userEntity.createdAt),
                updatedAt = Instant.fromEpochMilliseconds(userEntity.updatedAt)
            )
        }
    }
    
    suspend fun getAllUsers(): List<User> = withContext(Dispatchers.IO) {
        database.userQueries.selectAll().executeAsList().map { userEntity ->
            User(
                id = userEntity.id,
                email = userEntity.email,
                name = userEntity.name,
                onboardingComplete = userEntity.onboardingComplete == 1L,
                primaryGoal = HealthGoal.valueOf(userEntity.primaryGoal),
                unitSystem = UnitSystem.valueOf(userEntity.unitSystem),
                createdAt = Instant.fromEpochMilliseconds(userEntity.createdAt),
                updatedAt = Instant.fromEpochMilliseconds(userEntity.updatedAt)
            )
        }
    }
    
    suspend fun getPendingSyncUsers(): List<User> = withContext(Dispatchers.IO) {
        database.userQueries.selectPendingSync().executeAsList().map { userEntity ->
            User(
                id = userEntity.id,
                email = userEntity.email,
                name = userEntity.name,
                onboardingComplete = userEntity.onboardingComplete == 1L,
                primaryGoal = HealthGoal.valueOf(userEntity.primaryGoal),
                unitSystem = UnitSystem.valueOf(userEntity.unitSystem),
                createdAt = Instant.fromEpochMilliseconds(userEntity.createdAt),
                updatedAt = Instant.fromEpochMilliseconds(userEntity.updatedAt)
            )
        }
    }
    
    suspend fun updateSyncStatus(userId: String, syncStatus: String): Unit = withContext(Dispatchers.IO) {
        database.userQueries.updateSyncStatus(syncStatus, userId)
    }
    
    suspend fun deleteUser(id: String): Unit = withContext(Dispatchers.IO) {
        database.userQueries.deleteById(id)
    }
    
    suspend fun insertOrUpdate(user: User): Unit = withContext(Dispatchers.IO) {
        val existingUser = getUserById(user.id)
        if (existingUser != null) {
            updateUser(user)
        } else {
            insertUser(user)
        }
    }
    
    suspend fun markAsSynced(userId: String): Unit = withContext(Dispatchers.IO) {
        updateSyncStatus(userId, "SYNCED")
    }
    
    // Convenience method for testing
    suspend fun insertUser(
        id: String,
        email: String,
        name: String,
        onboardingComplete: Long,
        primaryGoal: String,
        unitSystem: String,
        createdAt: Long,
        updatedAt: Long,
        syncStatus: String
    ): Unit = withContext(Dispatchers.IO) {
        database.userQueries.insert(
            id = id,
            email = email,
            name = name,
            onboardingComplete = onboardingComplete,
            primaryGoal = primaryGoal,
            unitSystem = unitSystem,
            createdAt = createdAt,
            updatedAt = updatedAt,
            syncStatus = syncStatus
        )
    }
}